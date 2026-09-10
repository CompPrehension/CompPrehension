package org.vstu.compprehension.services;

import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.enums.AttemptStatus;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.data.exerciseattempt.AttemptGenerationContextData;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.data.exerciseattempt.AttemptOwnerData;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;
import org.vstu.compprehension.repositories.data.ExerciseAttemptDataRepository;

import java.util.Optional;

@Service
class ExerciseAttemptDataServiceImpl implements ExerciseAttemptDataService {
    private final LtiContextProvider ltiContextProvider;
    private final GradePassbackService gradePassbackService;
    private final CourseDataService courseService;
    private final AuthService authService;
    private final AuthScopeFactory authScopes;
    private final ExerciseAttemptDataRepository exerciseAttemptDataRepository;

    public ExerciseAttemptDataServiceImpl(LtiContextProvider ltiContextProvider,
                                          GradePassbackService gradePassbackService,
                                          CourseDataService courseService,
                                          AuthService authService,
                                          AuthScopeFactory authScopes,
                                          ExerciseAttemptDataRepository exerciseAttemptDataRepository) {
        this.ltiContextProvider = ltiContextProvider;
        this.gradePassbackService = gradePassbackService;
        this.courseService = courseService;
        this.authService = authService;
        this.authScopes = authScopes;
        this.exerciseAttemptDataRepository = exerciseAttemptDataRepository;
    }

    @Transactional(readOnly = true)
    public @NotNull ExerciseAttemptWithQuestionsData getAttemptWithQuestions(long attemptId) {
        return exerciseAttemptDataRepository.getAttemptWithQuestions(attemptId);
    }

    @Transactional(readOnly = true)
    public @NotNull AttemptGenerationContextData getGenerationContext(long attemptId) {
        return exerciseAttemptDataRepository.getGenerationContext(attemptId);
    }

    @Transactional(readOnly = true)
    public Optional<QuestionAttemptContextData> findQuestionContext(long questionId) {
        return exerciseAttemptDataRepository.findQuestionAttemptContext(questionId)
                .map(context -> {
                    context.setQuestionStage(resolveQuestionStage(context, questionId));
                    return context;
                });
    }

    private @NotNull ExerciseStageData resolveQuestionStage(@NotNull QuestionAttemptContextData context, long questionId) {
        var stages = context.getStages();
        if (stages.isEmpty()) {
            throw new IllegalStateException(
                    "Exercise of attempt " + context.getAttemptId() + " has no stages, question " + questionId);
        }

        long questionNumber = exerciseAttemptDataRepository
                .countQuestionsUpTo(context.getAttemptId(), questionId);
        int questionsPassed = 0;
        ExerciseStageData stage = stages.getFirst();
        for (int i = 0; i < stages.size() && questionsPassed < questionNumber; i++) {
            stage = stages.get(i);
            questionsPassed += stage.getNumberOfQuestions();
        }
        return stage;
    }

    @Transactional(readOnly = true)
    public Language findUserLanguageForQuestion(long questionId) {
        return exerciseAttemptDataRepository.findQuestionAttemptContext(questionId)
                .map(QuestionAttemptContextData::getUserLanguage)
                .orElse(Language.RUSSIAN);
    }

    @Transactional(readOnly = true)
    public Optional<Long> findAttemptIdOfQuestion(long questionId) {
        return exerciseAttemptDataRepository.findQuestionAttemptContext(questionId)
                .map(QuestionAttemptContextData::getAttemptId);
    }

    @Transactional(readOnly = true)
    public void ensureCanAccessAttempt(long userId, long attemptId) {
        AttemptOwnerData owner = exerciseAttemptDataRepository.findOwnerByAttemptId(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("No attempt with id " + attemptId));
        ensureOwnerOrPrivileged(userId, owner, attemptId);
    }

    @Transactional(readOnly = true)
    public void ensureCanAccessQuestion(long userId, long questionId) {
        AttemptOwnerData owner = exerciseAttemptDataRepository.findOwnerByQuestionId(questionId)
                .orElse(null);
        ensureOwnerOrPrivileged(userId, owner, questionId);
    }

    private void ensureOwnerOrPrivileged(long userId, @Nullable AttemptOwnerData owner, long targetId) {
        if (owner != null && owner.userId() != null && owner.userId() == userId) {
            authService.ensureAuthorized(userId, SystemPermission.SOLVE_EXERCISE, authScopes.courseOrGlobal(owner.courseId()));
            return;
        }
        if (authService.isAuthorized(userId, SystemPermission.EDIT_EXERCISE, authScopes.courseOrGlobal(owner != null ? owner.courseId() : null))) {
            return;
        }
        throw new SecurityException(String.format(
                "User %s is not allowed to access attempt data %s", userId, targetId));
    }

    @Transactional(readOnly = true)
    public Optional<AttemptSummaryData> findSummary(long attemptId) {
        return exerciseAttemptDataRepository.findSummary(attemptId);
    }

    @Transactional(readOnly = true)
    public Optional<AttemptSummaryData> findIncompleteAttempt(long exerciseId, long userId,
                                                              @Nullable Long courseId) {
        return exerciseAttemptDataRepository.findSummaryWithStatus(
                exerciseId, userId, courseId, AttemptStatus.INCOMPLETE);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull AttemptSummaryData createNewAttempt(long exerciseId, long userId,
                                                        @Nullable Long courseId) {
        if (courseId != null) {
            courseService.ensureExerciseInCourse(exerciseId, courseId);
        }
        var lti = ltiContextProvider.getCurrentLtiContext().orElse(null);
        return exerciseAttemptDataRepository.create(
                exerciseId, userId, courseId,
                lti == null ? null : lti.lineitemUrl(),
                lti == null || lti.course() == null ? null : lti.course().courseId());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void ensureAttemptStatus(long attemptId, Decision decision) {
        if (decision != Decision.FINISH) {
            return;
        }
        if (!exerciseAttemptDataRepository.finishIfIncomplete(attemptId)) {
            return;
        }
        gradePassbackService.passGrade(attemptId, exerciseAttemptDataRepository.getFinalGrade(attemptId));
    }
}
