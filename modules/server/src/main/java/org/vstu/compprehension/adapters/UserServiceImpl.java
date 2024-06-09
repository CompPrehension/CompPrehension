package org.vstu.compprehension.adapters;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.vstu.compprehension.Service.*;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects;
import org.vstu.compprehension.models.businesslogic.auth.SystemRole;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;
import org.vstu.compprehension.models.entities.role.PermissionScopeEntity;
import org.vstu.compprehension.models.entities.role.RoleEntity;
import org.vstu.compprehension.models.entities.role.RoleUserAssignmentEntity;
import org.vstu.compprehension.models.repository.RoleRepository;
import org.vstu.compprehension.models.repository.UserRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PermissionScopeService permissionScopeService;
    private final RoleRepository roleRepository;
    private final CourseService courseService;
    private final EducationResourceService educationResourceService;
    private final RoleUserAssignmentService roleUserAssignmentService;

    public UserServiceImpl(UserRepository userRepository, PermissionScopeService permissionScopeService, RoleRepository roleRepository, CourseService courseService, EducationResourceService educationResourceService, RoleUserAssignmentService roleUserAssignmentService) {
        this.userRepository = userRepository;
        this.permissionScopeService = permissionScopeService;
        this.roleRepository = roleRepository;
        this.courseService = courseService;
        this.educationResourceService = educationResourceService;
        this.roleUserAssignmentService = roleUserAssignmentService;
    }

    public UserEntity getCurrentUser() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var parsedIdToken = getToken(authentication);

        var principalName = authentication.getName();
        var fullName = parsedIdToken.getFullName();
        var email = parsedIdToken.getEmail();
        var externalId = parsedIdToken.getIssuer() + "_" + principalName;

        var entity = userRepository.findByExternalId(externalId).orElseGet(UserEntity::new);

        HashSet<SystemRole> roles;
        Language language = null;
        CourseEntity course = null;
        PermissionScopeEntity permissionScopeEntity;

        if ("1.3.0".equals(parsedIdToken.getClaimAsString("https://purl.imsglobal.org/spec/lti/claim/version"))) {
            roles = fromLtiRoles(authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet()));

            language = Optional.ofNullable(parsedIdToken.getClaimAsMap("https://purl.imsglobal.org/spec/lti/claim/launch_presentation"))
                    .flatMap(x -> Optional.ofNullable(x.get("locale")))
                    .map(l -> Language.fromString(l.toString()))
                    .orElse(null);

            course = courseService.getCurrentCourse();

            if (course == null) {
                var educationResource = fromLtiEducationResource(parsedIdToken.getIssuer().toString());
                permissionScopeEntity = permissionScopeService.getOrCreatePermissionScope(PermissionScopeKind.EDUCATION_RESOURCE, Optional.ofNullable(educationResource.getId()));
            } else {
                permissionScopeEntity = permissionScopeService.getOrCreatePermissionScope(PermissionScopeKind.COURSE, Optional.ofNullable(course.getId()));
            }
        } else {
            permissionScopeEntity = null;
            var preparedRoles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(r -> r.length() > 0)
                    .collect(Collectors.toSet());
            roles = fromKeycloakRoles(preparedRoles);
            language = entity.getPreferred_language();
        }

        var roleEntities = getRoles(roles).stream().filter(Objects::nonNull).collect(Collectors.toSet());

        entity.setFirstName(fullName);
        entity.setLogin(email);
        entity.setPassword(null);
        entity.setEmail(email);
        entity.setPreferred_language(Optional.ofNullable(language).orElse(Language.ENGLISH));
        entity.setExternalId(externalId);

        var user = userRepository.save(entity);

        if (permissionScopeEntity == null) {
            return user;
        }

        var roleUserAssignments = roleEntities.stream().map(role -> new RoleUserAssignmentEntity(user, role, permissionScopeEntity)).collect(Collectors.toSet());
        roleUserAssignmentService.saveIfDoesNotExist(roleUserAssignments);
        return user;
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

    private HashSet<SystemRole> fromLtiRoles(Collection<String> roles) {
        if (roles.contains("ROLE_SystemAdministrator")) {
            return new HashSet<>(List.of(AuthObjects.Roles.GlobalAdmin));
        }

        if (roles.contains("ROLE_Administrator")) {
            return new HashSet<>(List.of(AuthObjects.Roles.EducationResourceAdmin));
        }

        if (roles.contains("ROLE_TeachingAssistant")) {
            return new HashSet<>(List.of(AuthObjects.Roles.Assistant));
        }

        var teacherRoles = Arrays.asList("ROLE_Instructor", "ROLE_ContentDeveloper", "ROLE_Mentor");

        if (CollectionUtils.containsAny(roles, teacherRoles)) {
            return new HashSet<>(List.of(AuthObjects.Roles.Teacher));
        }

        if (roles.contains("ROLE_Learner")) {
            return new HashSet<>(List.of(AuthObjects.Roles.Student));
        }

        return new HashSet<>(List.of(AuthObjects.Roles.Guest));
    }

    private HashSet<SystemRole> fromKeycloakRoles(Collection<String> roles) {
        if (roles.contains("ROLE_SystemAdministrator")) {
            return new HashSet<>(List.of(AuthObjects.Roles.GlobalAdmin));
        }
        if (roles.contains("ROLE_Administrator")) {
            return new HashSet<>(List.of(AuthObjects.Roles.EducationResourceAdmin));
        }
        if (roles.contains("ROLE_Teacher")) {
            return new HashSet<>(List.of(AuthObjects.Roles.Teacher));
        }
        if (roles.contains("ROLE_Learner")) {
            return new HashSet<>(List.of(AuthObjects.Roles.Student));
        }
        return new HashSet<>(List.of(AuthObjects.Roles.Guest));
    }

    private EducationResourceEntity fromLtiEducationResource(String fullUrlString) {
        URL fullUrl;
        try {
            fullUrl = new URL(fullUrlString);
        } catch (MalformedURLException e) {
            return null;
        }

        String mainUrl = fullUrl.getProtocol() + "://" + fullUrl.getAuthority();

        String hostName = fullUrl.getHost().split("\\.")[0];
        return educationResourceService.getOrCreateEducationResource(mainUrl, hostName);
    }

    private List<RoleEntity> getRoles(Set<SystemRole> role) {
        return roleRepository.findAllByNameIn(role.stream().map(SystemRole::Name).toList());
    }


    /**
     * Creates or updates user entity from LTI launch params
     * @param params LTI launch params
     * @return user
     */
    /*
    public UserEntity createOrUpdateFromLti(Map<String, String> params) {
        val externalId = params.get("tool_consumer_instance_guid") + "_" + params.get("user_id");
        val email = params.get("lis_person_contact_email_primary");
        val firstName = params.get("lis_person_name_given");
        val lastName = params.get("lis_person_name_family");
        val roles = Stream.of(params.get("roles").split(","))
                .map(String::trim)
                .filter(s -> s.length() > 0 && s.matches("^\\w+$"))
                //.map(s -> s.substring(s.lastIndexOf('/') + 1))
                .distinct()
                .collect(Collectors.toList());
        val locale = params.get("launch_presentation_locale").split("-")[0].toUpperCase();

        val entity = userRepository.findByExternalId(externalId).orElseGet(UserEntity::new);
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setEmail(email);
        entity.setLogin(email);
        entity.setPassword("undefined");
        entity.setPreferred_language(Language.fromString(locale));
        entity.setRoles(fromLtiRoles(roles));
        entity.setExternalId(externalId);

        return userRepository.save(entity);
    }
    */
}
