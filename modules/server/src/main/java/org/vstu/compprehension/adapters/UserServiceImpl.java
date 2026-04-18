package org.vstu.compprehension.adapters;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.educationresource.MoodleEducationResourceEntity;
import org.vstu.compprehension.models.entities.externalaccount.MoodleAccountEntity;
import org.vstu.compprehension.models.repository.MoodleAccountRepository;
import org.vstu.compprehension.models.repository.MoodleEducationResourceRepository;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class UserServiceImpl implements UserService {
    private static final String LTI_VERSION_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/version";
    private static final String LTI_LAUNCH_PRESENTATION_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/launch_presentation";
    private static final String LTI_VERSION_1_3 = "1.3.0";

    private final UserRepository userRepository;
    private final MoodleEducationResourceRepository moodleEducationResourceRepository;
    private final MoodleAccountRepository moodleAccountRepository;

    public UserServiceImpl(UserRepository userRepository,
                           MoodleEducationResourceRepository moodleEducationResourceRepository,
                           MoodleAccountRepository moodleAccountRepository) {
        this.userRepository = userRepository;
        this.moodleEducationResourceRepository = moodleEducationResourceRepository;
        this.moodleAccountRepository = moodleAccountRepository;
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

        Set<Role> roles;
        Language language;
        if (isLti) {
            roles = fromLtiRoles(authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet()));
            language = Optional.ofNullable(parsedIdToken.getClaimAsMap(LTI_LAUNCH_PRESENTATION_CLAIM))
                    .flatMap(x -> Optional.ofNullable(x.get("locale")))
                    .map(l -> Language.fromString(l.toString()))
                    .orElse(null);
        } else {
            roles = fromKeycloakRoles(authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(r -> !r.isEmpty())
                    .collect(Collectors.toSet()));
            language = entity.getPreferred_language();
        }

        entity.setFirstName(fullName);
        entity.setLogin(email);
        entity.setPassword(null);
        entity.setEmail(email);
        entity.setPreferred_language(Optional.ofNullable(language).orElse(Language.ENGLISH));
        entity.setRoles(roles);
        entity.setExternalId(externalId);
        entity = userRepository.save(entity);

        if (isLti) {
            ensureMoodleAccount(entity, parsedIdToken);
        }
        return entity;
    }

    private void ensureMoodleAccount(UserEntity user, OidcIdToken token) {
        var resource = getOrCreateMoodleResource(token.getIssuer().toString());

        Long moodleUserId;
        try {
            // LTI sub для Moodle — числовой внутренний user_id
            moodleUserId = Long.parseLong(token.getSubject());
        } catch (NumberFormatException e) {
            log.warn("LTI sub '{}' не числовой — MoodleAccount не создаётся (возможно, не Moodle LMS)",
                    token.getSubject());
            return;
        }

        // UNIQUE(user_id, education_resource_id): если запись уже есть — ничего не делаем.
        // moodle_user_id стабильный для пары (user, Moodle-инсталляция) и меняться не должен.
        if (moodleAccountRepository.findByUserIdAndEducationResourceId(user.getId(), resource.getId()).isEmpty()) {
            var acc = new MoodleAccountEntity();
            acc.setUser(user);
            acc.setEducationResource(resource);
            acc.setMoodleUserId(moodleUserId);
            moodleAccountRepository.save(acc);
        }
    }

    /**
     * TECH DEBT: автоматическое создание {@link MoodleEducationResourceEntity} — временное решение.
     * В дальнейшем записи {@code education_resource} будет заполнять администратор,
     * а при LTI launch с неизвестного ресурса будет бросаться исключение «обращение с невалидного
     * ресурса» вместо создания записи на лету.
     *
     * <p>try/catch вокруг save защищает от race condition: при одновременном LTI login'е
     * нескольких пользователей с одной и той же неизвестной ещё Moodle-инсталляции оба потока
     * выполнят findByUrl → empty и попытаются вставить. Один из них получит
     * {@link DataIntegrityViolationException} из-за UNIQUE(url, type) — в этом случае
     * перечитываем запись, созданную конкурирующим потоком.
     */
    private MoodleEducationResourceEntity getOrCreateMoodleResource(String url) {
        return moodleEducationResourceRepository.findByUrl(url)
                .orElseGet(() -> {
                    var r = new MoodleEducationResourceEntity();
                    r.setUrl(url);
                    try {
                        return moodleEducationResourceRepository.saveAndFlush(r);
                    } catch (DataIntegrityViolationException e) {
                        return moodleEducationResourceRepository.findByUrl(url)
                                .orElseThrow(() -> new IllegalStateException(
                                        "MoodleEducationResource '" + url + "' must exist after UNIQUE violation", e));
                    }
                });
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
        var parsedIdToken = ((OidcUser)principal).getIdToken();
        if (parsedIdToken == null) {
            throw new Exception("No id_token found");
        }
        return parsedIdToken;
    }

    private static String getExternalId(Authentication authentication, OidcIdToken token) {
        var principalName = authentication.getName();
        return token.getIssuer() + "_" + principalName;
    }

    private HashSet<Role> fromLtiRoles(Collection<String> roles) {
        if (roles.contains("ROLE_Administrator")) {
            return new HashSet<>(Arrays.asList(Role.values().clone()));
        }

        var teacherRoles = Arrays.asList("ROLE_Instructor", "ROLE_TeachingAssistant", "ROLE_ContentDeveloper", "ROLE_Mentor");
        if (CollectionUtils.containsAny(roles, teacherRoles)) {
            return new HashSet<>(List.of(Role.TEACHER, Role.STUDENT));
        }

        return new HashSet<>(List.of(Role.STUDENT));
    }

    private Set<Role> fromKeycloakRoles(Collection<String> roles) {
        if (roles.contains("ROLE_Administrator")) {
            return Arrays.stream(Role.values()).collect(Collectors.toSet());
        }
        if (roles.contains("ROLE_Teacher")) {
            return Set.of(Role.TEACHER, Role.STUDENT);
        }
        return Set.of(Role.STUDENT);
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
