package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserEntity, Long> {
    Optional<UserEntity> findUserByEmail(String email);
    Optional<UserEntity> findUserByLogin(String login);
    Optional<UserEntity> findByExternalId(String externalId);
}
