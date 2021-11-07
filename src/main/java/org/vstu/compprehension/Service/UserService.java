package org.vstu.compprehension.Service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.repository.UserRepository;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;


    public UserEntity createOrUpdateFromAuthentication() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new Exception("Trying to create user within Anonymous access");
        }

        val currentPrincipalName = authentication.getName();
        val roles = authentication.getAuthorities().stream()
                .map(r -> r.getAuthority().split("ROLE_"))
                .flatMap(Arrays::stream)
                .filter(r -> r.length() > 0).distinct()
                .collect(Collectors.toList());
        authentication.getDetails();
        val externalId = "local_" + currentPrincipalName;

        val entity = userRepository.findByExternalId(externalId).orElseGet(UserEntity::new);
        entity.setFirstName(currentPrincipalName);
        entity.setLogin(currentPrincipalName);
        entity.setPassword("undefined");
        if (entity.getPreferred_language() == null) {
            entity.setPreferred_language(Language.ENGLISH);
        }
        entity.setRoles(fromLtiRoles(roles));
        entity.setExternalId(externalId);

        return userRepository.save(entity);
    }


    /**
     * Creates or updates user entity from LTI launch params
     * @param params LTI launch params
     * @return user
     */
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

    private List<Role> fromLtiRoles(List<String> roles) {
        if (roles.contains("Administrator")) {
            return new ArrayList<>(Arrays.asList(Role.values().clone()));
        }

        val teacherRoles = Arrays.asList("Instructor", "TeachingAssistant", "ContentDeveloper", "Mentor");
        if (CollectionUtils.containsAny(roles, teacherRoles)) {
            return new ArrayList<>(Arrays.asList(Role.TEACHER, Role.STUDENT));
        }

        return new ArrayList<>(Arrays.asList(Role.STUDENT));
    }

    public UserEntity getUserByEmail(String email){
        return userRepository.findUserByEmail(email).orElseThrow(()->
                new NoSuchElementException("User with email: " + email + " not Found"));
    }
    
    public UserEntity getUserByLogin(String login) {
        return userRepository.findUserByLogin(login).orElseThrow(()->
                new NoSuchElementException("User with login: " + login + " not Found"));
    }


    public UserEntity getUser(long userId) {
        return userRepository.findById(userId).orElseThrow(()->
                new NoSuchElementException("User with id: " + userId + " not Found"));
    }

    public UserEntity getUser(String login) {
        return userRepository.findUserByLogin(login).orElseThrow(()->
                new NoSuchElementException("User with login: " + login + " not Found"));
    }

    /**
     * Добавить пользователя на курс (по умолчанию добавляется пользователь
     * с ролью - студент)
     * @param userId - id пользователя, которого будем добавлять
     * @param courseId - id курса, куда будем добавлять пользователя
     */
    public void addToCourse(long userId, long courseId) {

    }
}
