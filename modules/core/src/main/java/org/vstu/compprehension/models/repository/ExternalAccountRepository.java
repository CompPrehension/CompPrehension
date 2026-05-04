package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.ExternalAccountEntity;
import org.vstu.compprehension.models.entities.course.ExternalAccountId;

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

    /**
     * Inserts the entity only if it does not already exist.
     *
     * @return {@code true} if a new row was created, {@code false} if a row already existed
     */
    default boolean createIfAbsent(ExternalAccountEntity account) {
        if (account.getId() != null) {
            throw new IllegalArgumentException("Use save() to update existing ExternalAccountEntity");
        }
        return createIfAbsent(
                account.getUser().getId(),
                account.getEducationResource().getId(),
                account.getExternalId()
        ) > 0;
    }
}
