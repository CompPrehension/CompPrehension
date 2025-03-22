package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
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
    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user where a.id = ?1")
    Optional<ExerciseAttemptEntity> getById(Long attemptId);
    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user where a.exercise.id = ?1 and a.user.id = ?2 and a.attemptStatus = ?3")
    Optional<ExerciseAttemptEntity> getLastWithStatus(Long exerciseId, Long userId, AttemptStatus status);
    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user where a.exercise.id = ?1 and a.user.id = ?2 and a.attemptStatus = ?3")
    List<ExerciseAttemptEntity> getAllByStatus(Long exerciseId, Long userId, AttemptStatus status);
}
