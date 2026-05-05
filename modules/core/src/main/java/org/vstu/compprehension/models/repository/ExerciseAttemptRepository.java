package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseAttemptRepository extends CrudRepository<ExerciseAttemptEntity, Long> {
    @Query("select a from ExerciseAttemptEntity a " +
            "inner join fetch a.exercise " +
            "left join fetch a.questions q " +
            "left join fetch a.user " +
            "where a.id IN (select q.exerciseAttempt.id from QuestionEntity q where q.id = ?1 and q.exerciseAttempt is not null)")
    Optional<ExerciseAttemptEntity> findByQuestionId(long questionId);
    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user left join fetch a.course where a.id = ?1")
    Optional<ExerciseAttemptEntity> getById(Long attemptId);
    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user where a.exercise.id = ?1 and a.user.id = ?2 and a.attemptStatus = ?3")
    Optional<ExerciseAttemptEntity> getLastWithStatus(Long exerciseId, Long userId, AttemptStatus status);
    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user left join fetch a.course where a.exercise.id = ?1 and a.course.id = ?2 and a.user.id = ?3 and a.attemptStatus = ?4")
    Optional<ExerciseAttemptEntity> getLastWithStatusByCourse(Long exerciseId, Long courseId, Long userId, AttemptStatus status);
    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user where a.exercise.id = ?1 and a.user.id = ?2 and a.attemptStatus = ?3")
    List<ExerciseAttemptEntity> getAllByStatus(Long exerciseId, Long userId, AttemptStatus status);
    @Modifying
    @Query(value = "UPDATE ExerciseAttemptEntity SET attemptStatus = :newStatus WHERE exercise.id = :exerciseId AND user.id = :userId AND attemptStatus = :oldStatus")
    int changeExistingAttemptsStatus(@Param("exerciseId") Long exerciseId, @Param("userId") Long userId, @Param("oldStatus") AttemptStatus oldStatus, @Param("newStatus") AttemptStatus newStatus);
    @Modifying
    @Query(value = "UPDATE ExerciseAttemptEntity SET attemptStatus = :newStatus WHERE exercise.id = :exerciseId AND course.id = :courseId AND user.id = :userId AND attemptStatus = :oldStatus")
    int changeExistingAttemptsStatusByCourse(@Param("exerciseId") Long exerciseId, @Param("courseId") Long courseId, @Param("userId") Long userId, @Param("oldStatus") AttemptStatus oldStatus, @Param("newStatus") AttemptStatus newStatus);

    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user left join fetch a.course where a.exercise.id = ?1 and a.course.id = ?2")
    List<ExerciseAttemptEntity> getAllByExerciseAndCourse(Long exerciseId, Long courseId);

    @Query("""
        select cast(f.grade as double)
        from InteractionEntity i
        left join i.feedback f
        where i.id = (
            select max(i2.id) from InteractionEntity i2
            where i2.question.id = (
                select max(q.id) from QuestionEntity q
                where q.exerciseAttempt.id = :attemptId
            )
        )
        """)
    Optional<Double> calculateFinalGrade(@Param("attemptId") Long attemptId);
}
