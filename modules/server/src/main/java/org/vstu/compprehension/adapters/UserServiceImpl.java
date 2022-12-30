package org.vstu.compprehension.adapters;

import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity getCurrentUser() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new Exception("Trying to create user within Anonymous access");
        }

        var principal = authentication.getPrincipal();
        if (!(principal instanceof OidcUser)) {
            throw new Exception("Unexpected authorized user format");
        }
        var parsedIdToken = ((OidcUser)principal).getIdToken();

        var principalName = authentication.getName();
        var fullName = parsedIdToken.getFullName();
        var email = parsedIdToken.getEmail();
        var roles = authentication.getAuthorities().stream()
                .map(r -> r.getAuthority().split("ROLE_"))
                .flatMap(Arrays::stream)
                .filter(r -> r.length() > 0).distinct()
                .collect(Collectors.toList());
        var externalId = parsedIdToken.getIssuer() + "_" + principalName;

        val entity = userRepository.findByExternalId(externalId).orElseGet(UserEntity::new);
        entity.setFirstName(fullName);
        entity.setLogin(email);
        entity.setPassword("undefined");
        entity.setEmail(email);
        if (entity.getPreferred_language() == null) {
            entity.setPreferred_language(Language.ENGLISH);
        }
        entity.setRoles(fromLtiRoles(roles));
        entity.setExternalId(externalId);

        return userRepository.save(entity);
    }

    private HashSet<Role> fromLtiRoles(Collection<String> roles) {
        if (roles.contains("Administrator")) {
            return new HashSet<>(Arrays.asList(Role.values().clone()));
        }

        val teacherRoles = Arrays.asList("Instructor", "TeachingAssistant", "ContentDeveloper", "Mentor");
        if (CollectionUtils.containsAny(roles, teacherRoles)) {
            return new HashSet<>(List.of(Role.TEACHER, Role.STUDENT));
        }

        return new HashSet<>(List.of(Role.STUDENT));
    }

    private HashSet<Role> fromKeycloakRoles(Collection<String> roles) {
        if (roles.contains("Administrator")) {
            return new HashSet<>(Arrays.asList(Role.values().clone()));
        }
        if (roles.contains("Teacher")) {
            return new HashSet<>(Arrays.asList(Role.TEACHER, Role.STUDENT));
        }
        return new HashSet<>(List.of(Role.STUDENT));
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
