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
import org.vstu.compprehension.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.data.cource.CreateCourseData;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.data.user.UserAccountUpdateData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.repositories.data.UserDataRepository;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
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
    private final LtiContextProvider ltiContextProvider;
    private final CourseDataService courseService;
    private final RoleAssignmentService roleAssignmentService;
    private final Mapper<UserAccountData, UserData> currentUserMapper;

    public UserServiceImpl(
            UserDataRepository users,
            EducationResourceService educationResourceService,
            LtiContextProvider ltiContextProvider,
            CourseDataService courseService,
            RoleAssignmentService roleAssignmentService,
            Mapper<UserAccountData, UserData> currentUserMapper
    ) {
        this.users = users;
        this.educationResourceService = educationResourceService;
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

        var email = parsedIdToken.getEmail();
        if (email == null || email.isBlank()) {
            throw new Exception("id_token must contain non-empty email claim");
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toSet());

        return isLti(parsedIdToken)
                ? signInFromLti(parsedIdToken, email, authorities)
                : signInFromIdp(parsedIdToken, email, authorities);
    }

    private UserAccountData signInFromIdp(OidcIdToken token, String email, Set<String> idpRoles) {
        var existing = users.findByIdpIdentity(getIssuer(token), token.getSubject());
        if (existing.isPresent()) {
            var account = existing.get();
            return users.updateProfile(account.id(),
                    new UserAccountUpdateData(email, token.getFullName(), account.language()));
        }

        var account = users.createIdpUser(getIssuer(token), token.getSubject(),
                new UserAccountUpdateData(email, token.getFullName(), Language.ENGLISH));
        applyIdpRoles(account.id(), idpRoles);
        return account;
    }

    private UserAccountData signInFromLti(OidcIdToken token, String email, Set<String> ltiRoles) {
        LtiContext ctx = getLtiContext();
        long eduResId = findTrustedEducationResourceId(ctx);
        var profile = new UserAccountUpdateData(email, token.getFullName(),
                Optional.ofNullable(getLtiLanguage(token)).orElse(Language.ENGLISH));

        var externalId = token.getSubject();
        if (externalId == null) {
            throw new IllegalStateException("LTI token without required sub claim");
        }

        var existing = users.findByEducationResourceUser(eduResId, externalId);
        UserAccountData account;
        if (existing.isPresent()) {
            users.updateEducationResourceUserProfile(eduResId, externalId, profile);
            account = users.updateProfile(existing.get().id(), profile);
        } else {
            account = users.createEducationResourceUser(eduResId, externalId, profile);
        }

        applyLtiRoles(account.id(), eduResId, ctx, ltiRoles);
        return account;
    }

    private static @Nullable Language getLtiLanguage(@NotNull OidcIdToken parsedIdToken) {
        return Optional.ofNullable(parsedIdToken.getClaimAsMap(LTI_LAUNCH_PRESENTATION_CLAIM))
                .flatMap(x -> Optional.ofNullable(x.get("locale")))
                .map(l -> Language.fromString(l.toString()))
                .orElse(null);
    }

    private LtiContext getLtiContext() {
        return ltiContextProvider.getCurrentLtiContext()
                .orElseThrow(() -> new IllegalStateException("LTI id_token without LTI launch context"));
    }

    private long findTrustedEducationResourceId(LtiContext ctx) {
        educationResourceService.ensureTrusted(ctx.educationResourceId());
        return ctx.educationResourceId();
    }

    private void applyLtiRoles(long userId, long eduResId, LtiContext ctx, Set<String> ltiRoles) {
        // roleAssignmentService.assignGlobalRole(userId, SystemRole.STUDENT);

        Role eduResRole = ltiRoles.contains("ROLE_Administrator") ? SystemRole.EDUCATION_RESOURCE_ADMIN : null;
        roleAssignmentService.reconcileRoleInEducationResource(userId, eduResId, eduResRole);

        LtiCourseContext ltiCourse = ctx.course();
        if (ltiCourse != null && ltiCourse.courseId() != null) {
            long courseId = courseService.getOrCreate(new CreateCourseData(eduResId, ltiCourse.courseId(), ltiCourse.courseName()));
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

    private void applyIdpRoles(long userId, Set<String> idpRoles) {
        roleAssignmentService.assignGlobalRole(userId, SystemRole.STUDENT);
        if (idpRoles.contains("ROLE_Administrator")) {
            roleAssignmentService.assignRootRole(userId, SystemRole.ADMIN);
        } else if (idpRoles.contains("ROLE_Teacher")) {
            roleAssignmentService.assignGlobalRole(userId, SystemRole.GLOBAL_EXERCISE_AUTHOR);
        }
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
        var token = getToken(authentication);
        var account = isLti(token)
                ? users.findByEducationResourceUser(findTrustedEducationResourceId(getLtiContext()), token.getSubject())
                : users.findByIdpIdentity(getIssuer(token), token.getSubject());
        users.setLanguage(account.orElseThrow(() -> new NoSuchElementException("Signed-in user not found")).id(), language);
    }

    private static String getIssuer(OidcIdToken token) {
        return token.getIssuer().toString();
    }

    private static boolean isLti(OidcIdToken token) {
        return LTI_VERSION_1_3.equals(token.getClaimAsString(LTI_VERSION_CLAIM));
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
}
