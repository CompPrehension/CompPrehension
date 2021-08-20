package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseAttemptRepository extends CrudRepository<ExerciseAttemptEntity, Long> {
    Optional<ExerciseAttemptEntity> findFirstByExerciseIdAndUserIdAndAttemptStatusOrderByIdDesc(Long exerciseId, Long userId, AttemptStatus status);
    List<ExerciseAttemptEntity> findAllByExerciseIdAndUserIdAndAttemptStatusOrderByIdDesc(Long exerciseId, Long userId, AttemptStatus status);
}
