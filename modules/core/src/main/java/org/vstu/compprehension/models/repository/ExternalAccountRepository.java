package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.external_system.ExternalAccountEntity;
import org.vstu.compprehension.models.entities.external_system.ExternalAccountId;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalAccountRepository extends JpaRepository<ExternalAccountEntity, ExternalAccountId> {

    /**
     * Inserts a row only if no row with the same ({@link ExternalAccountId#userId}, {@link ExternalAccountId#educationResourceId}) exists.
     *
     * @return number of affected rows
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO external_account (user_id, education_resource_id, external_id)
            VALUES (:userId, :educationResourceId, :externalId)
            """, nativeQuery = true)
    int createIfAbsent(
            @Param("userId") Long userId,
            @Param("educationResourceId") Long educationResourceId,
            @Param("externalId") String externalId
    );

    /** Идентификатор пользователя во внешней системе; пусто, если связи нет. */
    @Query("""
            select ea.externalId from ExternalAccountEntity ea
            where ea.id.userId = :userId and ea.id.educationResourceId = :educationResourceId
            """)
    Optional<String> findExternalId(@Param("userId") long userId,
                                    @Param("educationResourceId") long educationResourceId);

    @Query("""
            select ea from ExternalAccountEntity ea
            where ea.educationResource.id = :educationResourceId
            """)
    List<ExternalAccountEntity> findByEducationResourceId(@Param("educationResourceId") Long educationResourceId);
}
