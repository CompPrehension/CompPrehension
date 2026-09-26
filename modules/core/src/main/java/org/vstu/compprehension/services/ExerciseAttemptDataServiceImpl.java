package org.vstu.compprehension.services;

import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.enums.AttemptStatus;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.data.exerciseattempt.AttemptGenerationContextData;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.data.exerciseattempt.AttemptOwnerData;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.data.question.ExerciseAttemptContextData;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;
import org.vstu.compprehension.repositories.data.ExerciseAttemptDataRepository;

import java.util.Optional;

@Service
class ExerciseAttemptDataServiceImpl implements ExerciseAttemptDataService {
    private final LtiContextProvider ltiContextProvider;
    private final GradePassbackService gradePassbackService;
    private final ExerciseAttemptDataRepository exerciseAttemptDataRepository;

    public ExerciseAttemptDataServiceImpl(LtiContextProvider ltiContextProvider,
                                          GradePassbackService gradePassbackService,
                                          ExerciseAttemptDataRepository exerciseAttemptDataRepository) {
        this.ltiContextProvider = ltiContextProvider;
        this.gradePassbackService = gradePassbackService;
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
        return exerciseAttemptDataRepository.findAttemptContextByQuestionId(questionId)
                .map(attempt -> new QuestionAttemptContextData(attempt, resolveQuestionStage(attempt, questionId)));
    }

    private @NotNull ExerciseStageData resolveQuestionStage(@NotNull ExerciseAttemptContextData attempt, long questionId) {
        var stages = attempt.stages();
        if (stages.isEmpty()) {
            throw new IllegalStateException(
                    "Exercise of attempt " + attempt.attemptId() + " has no stages, question " + questionId);
        }

        long questionNumber = exerciseAttemptDataRepository
                .countQuestionsUpTo(attempt.attemptId(), questionId);
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
        return exerciseAttemptDataRepository.findAttemptContextByQuestionId(questionId)
                .map(ExerciseAttemptContextData::userLanguage)
                .orElse(Language.RUSSIAN);
    }

    @Transactional(readOnly = true)
    public Optional<Long> findAttemptIdOfQuestion(long questionId) {
        return exerciseAttemptDataRepository.findAttemptContextByQuestionId(questionId)
                .map(ExerciseAttemptContextData::attemptId);
    }

    @Transactional(readOnly = true)
    public Optional<AttemptOwnerData> findOwnerByAttemptId(long attemptId) {
        return exerciseAttemptDataRepository.findOwnerByAttemptId(attemptId);
    }

    @Transactional(readOnly = true)
    public Optional<AttemptOwnerData> findOwnerByQuestionId(long questionId) {
        return exerciseAttemptDataRepository.findOwnerByQuestionId(questionId);
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
