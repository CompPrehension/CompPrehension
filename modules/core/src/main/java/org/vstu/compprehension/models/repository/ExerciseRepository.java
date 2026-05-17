package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExerciseRepository extends JpaRepository<ExerciseEntity, Long> {
    @Query("select e.id from ExerciseEntity e")
    List<Long> findAllIds();

    @Query("select new org.vstu.compprehension.dto.ExerciseDto(e.id, e.name, e.isPublic, e.guid) from ExerciseEntity e")
    List<ExerciseDto> getAllExerciseItems();

    List<ExerciseEntity> findAllByIsPublicTrue();

    @Query("""
            select e from ExerciseEntity e
            join ExerciseCourseLinkEntity l on l.exercise.id = e.id
            where l.course.id = :courseId
            order by l.linkedAt desc
            """)
    List<ExerciseEntity> findAllByCourseId(@Param("courseId") long courseId);

    List<ExerciseEntity> findAllByGuid(UUID guid);
}
