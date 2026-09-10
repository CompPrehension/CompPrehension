package org.vstu.compprehension.adapters;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.vstu.compprehension.services.*;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.businesslogic.auth.Role;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.data.user.UserAccountUpdateData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.repositories.data.UserDataRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class UserServiceImpl implements UserDataService {
    private static final String LTI_VERSION_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/version";
    private static final String LTI_LAUNCH_PRESENTATION_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/launch_presentation";
    private static final String LTI_VERSION_1_3 = "1.3.0";

    private final UserDataRepository users;
    private final EducationResourceService educationResourceService;
    private final ExternalAccountService externalAccountService;
    private final LtiContextProvider ltiContextProvider;
    private final CourseDataService courseService;
    private final RoleAssignmentService roleAssignmentService;
    private final Mapper<UserAccountData, UserData> currentUserMapper;

    public UserServiceImpl(
            UserDataRepository users,
            EducationResourceService educationResourceService,
            ExternalAccountService externalAccountService,
            LtiContextProvider ltiContextProvider,
            CourseDataService courseService,
            RoleAssignmentService roleAssignmentService,
            Mapper<UserAccountData, UserData> currentUserMapper
    ) {
        this.users = users;
        this.educationResourceService = educationResourceService;
        this.externalAccountService = externalAccountService;
        this.ltiContextProvider = ltiContextProvider;
        this.courseService = courseService;
        this.roleAssignmentService = roleAssignmentService;
        this.currentUserMapper = currentUserMapper;
    }

    @SneakyThrows
    @Override
    public UserData getCurrentUser() {
        return currentUserMapper.map(signIn());
    }

    private UserAccountData signIn() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var parsedIdToken = getToken(authentication);
        var externalId = getExternalId(authentication, parsedIdToken);

        var email = parsedIdToken.getEmail();
        if (email == null || email.isBlank()) {
            throw new Exception("id_token must contain non-empty email claim");
        }

        boolean isLti = LTI_VERSION_1_3.equals(parsedIdToken.getClaimAsString(LTI_VERSION_CLAIM));
        var existing = users.findByEmail(email).orElse(null);
        boolean isNewUser = existing == null;

        Language language = isLti
                ? getLtiLanguage(parsedIdToken)
                : (existing == null ? null : existing.language());
        String externalUserId = isLti
                ? parsedIdToken.getSubject()
                : (existing == null ? null : existing.externalUserId());

        var account = users.save(new UserAccountUpdateData(
                email,
                parsedIdToken.getFullName(),
                Optional.ofNullable(language).orElse(Language.ENGLISH),
                externalId,
                externalUserId));

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toSet());

        if (isLti) {
            applyLtiRoles(account.id(), parsedIdToken.getSubject(), authorities);
        } else if (isNewUser) {
            applyKeycloakRoles(account.id(), authorities);
        }

        return account;
    }

    private static @Nullable Language getLtiLanguage(@NotNull OidcIdToken parsedIdToken) {
        return Optional.ofNullable(parsedIdToken.getClaimAsMap(LTI_LAUNCH_PRESENTATION_CLAIM))
                .flatMap(x -> Optional.ofNullable(x.get("locale")))
                .map(l -> Language.fromString(l.toString()))
                .orElse(null);
    }

    private void applyLtiRoles(long userId, String ltiSubject, Set<String> ltiRoles) {
        LtiContext ctx = ltiContextProvider.getCurrentLtiContext().orElse(null);
        if (ctx == null) return;

        long eduResId = educationResourceService.getOrCreateTrustedId(ctx.lmsUrl(), ctx.lmsType());

        // roleAssignmentService.assignGlobalRole(userId, SystemRole.STUDENT);

        externalAccountService.createIfAbsent(userId, eduResId, ltiSubject);

        Role eduResRole = ltiRoles.contains("ROLE_Administrator") ? SystemRole.EDUCATION_RESOURCE_ADMIN : null;
        roleAssignmentService.reconcileRoleInEducationResource(userId, eduResId, eduResRole);

        Long courseId = courseService.resolveOrCreateIdFromLtiContext(ctx, eduResId).orElse(null);
        if (courseId != null) {
            Role courseRole = mapLtiCourseRole(ltiRoles);
            if (courseRole != null) {
                roleAssignmentService.reconcileCourseRoleAssignments(
                        eduResId,
                        List.of(userId),
                        List.of(new RoleAssignmentService.CourseRoleAssignment(userId, courseId, courseRole)),
                        List.of(courseId));
            }
        }
    }

    private void applyKeycloakRoles(long userId, Set<String> keycloakRoles) {
        roleAssignmentService.assignGlobalRole(userId, SystemRole.STUDENT);
        Role privilegedRole = mapKeycloakGlobalRole(keycloakRoles);
        if (privilegedRole != null) {
            roleAssignmentService.assignGlobalRole(userId, privilegedRole);
        }
    }

    private Role mapKeycloakGlobalRole(Collection<String> keycloakRoles) {
        if (keycloakRoles.contains("ROLE_Administrator")) {
            return SystemRole.GLOBAL_ADMIN;
        }
        if (keycloakRoles.contains("ROLE_Teacher")) {
            return SystemRole.GLOBAL_EXERCISE_AUTHOR;
        }
        return null;
    }

    private Role mapLtiCourseRole(Collection<String> ltiRoles) {
        if (ltiRoles.contains("ROLE_Instructor")
            || ltiRoles.contains("ROLE_ContentDeveloper")
            || ltiRoles.contains("ROLE_Mentor")) {
            return SystemRole.TEACHER;
        }
        if (ltiRoles.contains("ROLE_TeachingAssistant")) {
            return SystemRole.ASSISTANT;
        }
        return SystemRole.STUDENT;
    }

    @SneakyThrows
    @Override
    public void setLanguage(Language language) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var parsedIdToken = getToken(authentication);
        var email = parsedIdToken.getEmail();
        if (email == null || email.isBlank()) {
            throw new Exception("id_token must contain non-empty email claim");
        }
        users.setLanguage(email, language);
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
