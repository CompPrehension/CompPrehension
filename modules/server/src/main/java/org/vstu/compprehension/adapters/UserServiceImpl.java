package org.vstu.compprehension.adapters;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.EducationResourceService;
import org.vstu.compprehension.Service.ExternalAccountService;
import org.vstu.compprehension.Service.LtiContextProvider;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.businesslogic.lti.LtiContext;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.Role;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class UserServiceImpl implements UserService {
    private static final String LTI_VERSION_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/version";
    private static final String LTI_LAUNCH_PRESENTATION_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/launch_presentation";
    private static final String LTI_VERSION_1_3 = "1.3.0";

    private final UserRepository userRepository;
    private final EducationResourceService educationResourceService;
    private final ExternalAccountService externalAccountService;
    private final LtiContextProvider ltiContextProvider;
    private final CourseService courseService;
    private final AuthService authService;

    public UserServiceImpl(
            UserRepository userRepository,
            EducationResourceService educationResourceService,
            ExternalAccountService externalAccountService,
            LtiContextProvider ltiContextProvider,
            CourseService courseService,
            AuthService authService
    ) {
        this.userRepository = userRepository;
        this.educationResourceService = educationResourceService;
        this.externalAccountService = externalAccountService;
        this.ltiContextProvider = ltiContextProvider;
        this.courseService = courseService;
        this.authService = authService;
    }

    public UserEntity getCurrentUser() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var parsedIdToken = getToken(authentication);
        var externalId = getExternalId(authentication, parsedIdToken);

        var fullName = parsedIdToken.getFullName();
        var email = parsedIdToken.getEmail();
        if (email == null || email.isBlank()) {
            throw new Exception("id_token must contain non-empty email claim");
        }

        boolean isLti = LTI_VERSION_1_3.equals(parsedIdToken.getClaimAsString(LTI_VERSION_CLAIM));

        UserEntity entity = userRepository.findFirstByEmailOrderByIdAsc(email).orElseGet(UserEntity::new);

        Language language;
        if (isLti) {
            language = Optional.ofNullable(parsedIdToken.getClaimAsMap(LTI_LAUNCH_PRESENTATION_CLAIM))
                    .flatMap(x -> Optional.ofNullable(x.get("locale")))
                    .map(l -> Language.fromString(l.toString()))
                    .orElse(null);
        } else {
            language = entity.getPreferred_language();
        }

        entity.setFirstName(fullName);
        entity.setLogin(email);
        entity.setPassword(null);
        entity.setEmail(email);
        entity.setPreferred_language(Optional.ofNullable(language).orElse(Language.ENGLISH));
        entity.setExternalId(externalId);

        if (isLti) {
            entity.setExternalUserId(parsedIdToken.getSubject());
        }
        entity = userRepository.save(entity);

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toSet());

        if (isLti) {
            applyLtiRoles(entity, parsedIdToken, authorities);
        } else {
            applyKeycloakRoles(entity);
        }

        return entity;
    }

    private void applyLtiRoles(UserEntity user, OidcIdToken parsedIdToken, Set<String> ltiRoles) {
        LtiContext ctx = ltiContextProvider.getCurrentLtiContext().orElse(null);
        if (ctx == null) return;

        EducationResourceEntity eduRes = educationResourceService.getOrCreateTrusted(ctx.lmsUrl(), ctx.lmsType());

        authService.assignGlobalRole(user.getId(), Role.STUDENT);

        boolean externalAccountNonExists = externalAccountService.findByUserAndEducationResource(
                user.getId(), eduRes.getId()
        ).isEmpty();
        if (externalAccountNonExists) {
            externalAccountService.createOrGetExisting(user.getId(), eduRes.getId(), parsedIdToken.getSubject());
        }

        Role eduResRole = ltiRoles.contains("ROLE_Administrator") ? Role.EDUCATION_RESOURCE_ADMIN : null;
        authService.reconcileRoleInEducationResource(user.getId(), eduRes.getId(), eduResRole);

        CourseEntity course = courseService.resolveOrCreateFromLtiContext(ctx, eduRes.getId());
        if (course != null) {
            Role courseRole = mapLtiCourseRole(ltiRoles);
            if (courseRole != null) {
                authService.reconcileCourseRoleAssignments(
                        eduRes.getId(),
                        List.of(user.getId()),
                        List.of(new AuthService.CourseRoleAssignment(user.getId(), course.getId(), courseRole)),
                        List.of(course.getId()));
            }
        }
    }

    private void applyKeycloakRoles(UserEntity user) {
        authService.assignGlobalRole(user.getId(), Role.STUDENT);
    }

    private Role mapLtiCourseRole(Collection<String> ltiRoles) {
        if (ltiRoles.contains("ROLE_Instructor")
            || ltiRoles.contains("ROLE_ContentDeveloper")
            || ltiRoles.contains("ROLE_Mentor")) {
            return Role.TEACHER;
        }
        if (ltiRoles.contains("ROLE_TeachingAssistant")) {
            return Role.ASSISTANT;
        }
        return Role.STUDENT;
    }

    @Override
    public void setLanguage(Language language) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var parsedIdToken = getToken(authentication);
        var email = parsedIdToken.getEmail();
        if (email == null || email.isBlank()) {
            throw new Exception("id_token must contain non-empty email claim");
        }

        var entity = userRepository.findFirstByEmailOrderByIdAsc(email).orElseThrow(() -> new Exception("User not found"));
        entity.setPreferred_language(language);
        userRepository.save(entity);
    }

    @NotNull
    private static OidcIdToken getToken(Authentication authentication) throws Exception {
        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new Exception("Trying to create user within Anonymous access");
        }

        var principal = authentication.getPrincipal();
        if (!(principal instanceof OidcUser)) {
            throw new Exception("Unexpected authorized user format");
        }
        var parsedIdToken = ((OidcUser) principal).getIdToken();
        if (parsedIdToken == null) {
            throw new Exception("No id_token found");
        }
        return parsedIdToken;
    }

    private static String getExternalId(Authentication authentication, OidcIdToken token) {
        var principalName = authentication.getName();
        return token.getIssuer() + "_" + principalName;
    }
}
