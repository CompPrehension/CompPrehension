package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.external_system.EducationResourceUserEntity;
import org.vstu.compprehension.entities.external_system.EducationResourceUserId;

import java.util.List;

@Repository
public interface EducationResourceUserRepository extends JpaRepository<EducationResourceUserEntity, EducationResourceUserId> {

    @Modifying(clearAutomatically = true)
    @Query("""
            update EducationResourceUserEntity eru
            set eru.fullName = :fullName, eru.email = :email
            where eru.id.educationResourceId = :educationResourceId and eru.externalId = :externalId
            """)
    int updateProfile(@Param("educationResourceId") long educationResourceId,
                      @Param("externalId") String externalId,
                      @Param("fullName") String fullName,
                      @Param("email") String email);

    /** Учётная запись пользователя в образовательном ресурсе. */
    interface EducationResourceUserView {
        Long getUserId();
        String getExternalId();
        Long getEducationResourceId();
    }

    @Query("""
            select eru.id.userId as userId,
                   eru.externalId as externalId,
                   eru.id.educationResourceId as educationResourceId
            from EducationResourceUserEntity eru
            where eru.id.educationResourceId = :educationResourceId
            """)
    List<EducationResourceUserView> findUsersByEducationResourceId(@Param("educationResourceId") long educationResourceId);
}
