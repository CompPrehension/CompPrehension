package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;

import java.util.Optional;

@Repository
public interface EducationResourceRepository extends JpaRepository<EducationResourceEntity, Long> {
    Optional<EducationResourceEntity> findByUrlAndType(String url, EducationResourceType type);

    /**
     * Inserts a row only if no row with the same ({@link EducationResourceEntity#url}, {@link EducationResourceEntity#type}) exists.
     *
     * @return number of affected rows
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO education_resource (url, type)
            VALUES (:url, :type)
            """, nativeQuery = true)
    int createIfAbsent(@Param("url") String url, @Param("type") String type);

    /**
     * Inserts the entity only if it does not already exist.
     *
     * @return {@code true} if a new row was created, {@code false} if a row already existed
     */
    default boolean createIfAbsent(EducationResourceEntity entity) {
        if (entity.getId() != null) {
            throw new IllegalArgumentException("Use save() to update existing EducationResourceEntity");
        }
        return createIfAbsent(entity.getUrl(), entity.getType().name()) > 0;
    }
}
