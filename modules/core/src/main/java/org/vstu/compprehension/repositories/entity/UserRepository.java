package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.vstu.compprehension.entities.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Query("select u.id from UserEntity u order by u.id")
    List<Long> findAllIds();

    Optional<UserEntity> findFirstByEmailOrderByIdAsc(String email);
    Optional<UserEntity> findUserByEmail(String email);
    Optional<UserEntity> findUserByLogin(String login);
    Optional<UserEntity> findByExternalId(String externalId);
}
