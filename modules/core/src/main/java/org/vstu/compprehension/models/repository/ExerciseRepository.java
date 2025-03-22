package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends CrudRepository<ExerciseEntity, Long> {
    @Query("select e.id from ExerciseEntity e")
    List<Long> findAllIds();

    @Query("select new org.vstu.compprehension.dto.ExerciseDto(e.id, e.name) from ExerciseEntity e")
    List<ExerciseDto> getAllExerciseItems();

    //@Query("select new org.vstu.compprehension.dto.ExerciseCardDto(e.id, e.name, e.domain.name, e.strategyId, e.backendId) from ExerciseEntity e where e.id = ?1")
    //Optional<ExerciseCardDto> getExerciseCard(long id);
}
