package org.vstu.compprehension.Service;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.ArrayList;

@Service
public class ExerciseAttemptService {
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseService exerciseService;
    private final UserRepository userRepository;
    private final LtiContextProvider ltiContextProvider;
    private final GradePassbackService gradePassbackService;

    public ExerciseAttemptService(ExerciseAttemptRepository exerciseAttemptRepository,
                                  ExerciseService exerciseService,
                                  UserRepository userRepository,
                                  LtiContextProvider ltiContextProvider,
                                  GradePassbackService gradePassbackService) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.exerciseService = exerciseService;
        this.userRepository = userRepository;
        this.ltiContextProvider = ltiContextProvider;
        this.gradePassbackService = gradePassbackService;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ExerciseAttemptEntity createNewAttempt(@NotNull Long exerciseId, @NotNull Long userId) {
        exerciseAttemptRepository.changeExistingAttemptsStatus(exerciseId, userId, AttemptStatus.INCOMPLETE, AttemptStatus.COMPLETED_BY_SYSTEM);

        var exercise = exerciseService.getExercise(exerciseId);
        var user = userRepository.findById(userId).orElseThrow();

        var ea = new ExerciseAttemptEntity();
        ea.setExercise(exercise);
        ea.setUser(user);
        ea.setAttemptStatus(AttemptStatus.INCOMPLETE);
        ea.setQuestions(new ArrayList<>());

        ltiContextProvider.getCurrentLtiContext().ifPresent(ctx -> {
            ea.setLtiLineitemUrl(ctx.lineitemUrl());
            ea.setLtiContextId(ctx.contextId());
        });

        exerciseAttemptRepository.save(ea);
        return ea;
    }

    public void ensureAttemptStatus(ExerciseAttemptEntity attempt, Decision decision) {
        if (decision == Decision.FINISH && attempt.getAttemptStatus() == AttemptStatus.INCOMPLETE) {
            attempt.setAttemptStatus(AttemptStatus.COMPLETED_BY_USER);
            exerciseAttemptRepository.save(attempt);
            double grade = exerciseAttemptRepository.calculateFinalGrade(attempt.getId())
                    .orElse(0.0);
            gradePassbackService.passGrade(attempt, grade);
        } else {
            exerciseAttemptRepository.save(attempt);
        }
    }
}
