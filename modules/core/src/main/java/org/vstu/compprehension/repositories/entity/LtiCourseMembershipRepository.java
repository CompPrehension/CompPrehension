package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.external_system.LtiCourseMembershipEntity;

import java.util.List;

@Repository
public interface LtiCourseMembershipRepository extends JpaRepository<LtiCourseMembershipEntity, Long> {

    interface LtiCourseMembershipSourceView {
        Long getCourseId();
        Long getEducationResourceId();
        String getIssuer();
        String getClientId();
        String getMembershipsUrl();
    }

    @Query("""
            select m.courseId as courseId, c.educationResource.id as educationResourceId,
                   m.issuer as issuer, m.clientId as clientId, m.membershipsUrl as membershipsUrl
            from LtiCourseMembershipEntity m
            join CourseEntity c on c.id = m.courseId
            where c.educationResource.trustStatus = org.vstu.compprehension.enums.EducationResourceTrustStatus.TRUSTED
            """)
    List<LtiCourseMembershipSourceView> findSourcesOfTrustedLms();

    /** Запуски из одного курса могут прийти одновременно, поэтому вставка и обновление — одним запросом. */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            insert into lti_course_membership (course_id, issuer, client_id, memberships_url, updated_at)
            values (:courseId, :issuer, :clientId, :membershipsUrl, now()) as new
            on duplicate key update issuer = new.issuer, client_id = new.client_id,
                                    memberships_url = new.memberships_url, updated_at = new.updated_at
            """, nativeQuery = true)
    int upsert(@Param("courseId") long courseId, @Param("issuer") String issuer,
               @Param("clientId") String clientId, @Param("membershipsUrl") String membershipsUrl);
}
