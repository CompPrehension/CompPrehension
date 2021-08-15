package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends CrudRepository<ExerciseEntity, Long> {
    @Query("select e.id from ExerciseEntity e")
    List<Long> findAllIds();
}
