package org.vstu.compprehension.Service;


import org.vstu.compprehension.Exceptions.NotFoundEx.ExerciseNFException;
import org.vstu.compprehension.Exceptions.NotFoundEx.UserNFException;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService {
    @Autowired
    private ExerciseRepository exerciseRepository;

    public ExerciseEntity getExercise(long exerciseId) {
        try {
            return exerciseRepository.findById(exerciseId).orElseThrow(()->
                    new ExerciseNFException("Exercise with id: " + exerciseId + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-exercise to Model-exercise", e);
        }
    }
}
