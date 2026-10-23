package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.entities.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Query("select u.id from UserEntity u order by u.id")
    List<Long> findAllIds();

    Optional<UserEntity> findByIdpIssuerAndIdpSubject(String idpIssuer, String idpSubject);

    @Query("""
            select eru.user from EducationResourceUserEntity eru
            where eru.id.educationResourceId = :educationResourceId and eru.externalId = :externalId
            """)
    Optional<UserEntity> findByEducationResourceUser(@Param("educationResourceId") long educationResourceId,
                                                     @Param("externalId") String externalId);
}
