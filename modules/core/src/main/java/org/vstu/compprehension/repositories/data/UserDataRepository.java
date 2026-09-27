package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.data.user.UserAccountUpdateData;
import org.vstu.compprehension.entities.UserEntity;
import org.vstu.compprehension.entities.external_system.EducationResourceUserEntity;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.mappers.UpdateMapper;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;
import org.vstu.compprehension.repositories.entity.EducationResourceUserRepository;
import org.vstu.compprehension.repositories.entity.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserDataRepository {

    private final UserRepository userRepository;
    private final EducationResourceRepository educationResourceRepository;
    private final EducationResourceUserRepository educationResourceUserRepository;
    private final Mapper<UserEntity, UserAccountData> userAccountMapper;
    private final UpdateMapper<UserAccountUpdateData, UserEntity> userProfileMapper;
    private final UpdateMapper<UserAccountUpdateData, EducationResourceUserEntity> educationResourceUserProfileMapper;

    @Transactional(readOnly = true)
    public @NotNull List<Long> findAllIds() {
        return userRepository.findAllIds();
    }

    @Transactional(readOnly = true)
    public @NotNull UserAccountData getById(long id) {
        return userRepository.findById(id)
                .map(userAccountMapper::map)
                .orElseThrow(() -> new NoSuchElementException("User " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<UserAccountData> findByIdpIdentity(@NotNull String issuer, @NotNull String subject) {
        return userRepository.findByIdpIssuerAndIdpSubject(issuer, subject).map(userAccountMapper::map);
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<UserAccountData> findByEducationResourceUser(long educationResourceId,
                                                                         @NotNull String externalId) {
        return userRepository.findByEducationResourceUser(educationResourceId, externalId).map(userAccountMapper::map);
    }

    @Transactional
    public @NotNull UserAccountData createIdpUser(@NotNull String issuer, @NotNull String subject,
                                                 @NotNull UserAccountUpdateData profile) {
        var entity = new UserEntity();
        entity.setIdpIssuer(issuer);
        entity.setIdpSubject(subject);
        userProfileMapper.apply(profile, entity);
        return userAccountMapper.map(userRepository.save(entity));
    }

    @Transactional
    public @NotNull UserAccountData createEducationResourceUser(long educationResourceId, @NotNull String externalId,
                                                               @NotNull UserAccountUpdateData profile) {
        var user = new UserEntity();
        userProfileMapper.apply(profile, user);
        userRepository.save(user);

        var educationResourceUser = new EducationResourceUserEntity(
                user, educationResourceRepository.getReferenceById(educationResourceId), externalId);
        educationResourceUserProfileMapper.apply(profile, educationResourceUser);
        educationResourceUserRepository.save(educationResourceUser);
        return userAccountMapper.map(user);
    }

    @Transactional
    public @NotNull UserAccountData updateProfile(long userId, @NotNull UserAccountUpdateData profile) {
        var entity = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User " + userId + " not found"));
        userProfileMapper.apply(profile, entity);
        return userAccountMapper.map(userRepository.saveAndFlush(entity));
    }

    @Transactional
    public void updateEducationResourceUserProfile(long educationResourceId, @NotNull String externalId,
                                                   @NotNull UserAccountUpdateData profile) {
        if (educationResourceUserRepository.updateProfile(
                educationResourceId, externalId, profile.fullName(), profile.email()) != 1) {
            throw new NoSuchElementException(
                    "User " + externalId + " of education resource " + educationResourceId + " not found");
        }
    }

    @Transactional
    public void setLanguage(long userId, @NotNull Language language) {
        var entity = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User " + userId + " not found"));
        entity.setPreferred_language(language);
        userRepository.saveAndFlush(entity);
    }
}
