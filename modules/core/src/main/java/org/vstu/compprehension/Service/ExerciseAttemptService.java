package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.data.ExerciseStageData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.data.AttemptGenerationContextData;
import org.vstu.compprehension.models.data.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.models.data.AttemptOwnerData;
import org.vstu.compprehension.models.data.AttemptSummaryData;
import org.vstu.compprehension.models.data.QuestionAttemptContextData;
import org.vstu.compprehension.models.repository.data.ExerciseAttemptDataRepository;

import java.util.Optional;

@Service
public class ExerciseAttemptService {
    private final LtiContextProvider ltiContextProvider;
    private final GradePassbackService gradePassbackService;
    private final CourseService courseService;
    private final AuthService authService;
    private final AuthScopeFactory authScopes;
    private final ExerciseAttemptDataRepository exerciseAttemptDataRepository;

    public ExerciseAttemptService(LtiContextProvider ltiContextProvider,
                                  GradePassbackService gradePassbackService,
                                  CourseService courseService,
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

    /** Попытка со всеми вопросами и взаимодействиями — для стратегий. */
    @Transactional(readOnly = true)
    public @NotNull ExerciseAttemptWithQuestionsData getAttemptWithQuestions(long attemptId) {
        return exerciseAttemptDataRepository.getAttemptWithQuestions(attemptId);
    }

    /** Всё, что нужно для генерации очередного вопроса попытки. */
    @Transactional(readOnly = true)
    public @NotNull AttemptGenerationContextData getGenerationContext(long attemptId) {
        return exerciseAttemptDataRepository.getGenerationContext(attemptId);
    }

    /**
     * Контекст попытки, в которой задан вопрос.
     *
     * @return пусто, если вопрос задан вне попытки
     */
    @Transactional(readOnly = true)
    public Optional<QuestionAttemptContextData> findQuestionContext(long questionId) {
        return exerciseAttemptDataRepository.findQuestionAttemptContext(questionId);
    }

    /**
     * Этап упражнения, на котором задан вопрос.
     * <p>
     * Контекст попытки и порядковый номер вопроса — два запроса; сам разбор по этапам
     * остаётся здесь, потому что это правило упражнения, а не форма хранения.
     *
     * @return пусто, если вопрос не привязан к попытке или у упражнения нет этапов
     */
    @Transactional(readOnly = true)
    public Optional<ExerciseStageData> findStageForQuestion(long questionId) {
        var context = exerciseAttemptDataRepository.findQuestionAttemptContext(questionId).orElse(null);
        if (context == null || context.stages().isEmpty()) {
            return Optional.empty();
        }
        var stages = context.stages();

        long questionNumber = exerciseAttemptDataRepository
                .countQuestionsUpTo(context.attemptId(), questionId);
        int questionsPassed = 0;
        ExerciseStageData stage = stages.getFirst();
        for (int i = 0; i < stages.size() && questionsPassed < questionNumber; i++) {
            stage = stages.get(i);
            questionsPassed += stage.getNumberOfQuestions();
        }
        return Optional.ofNullable(stage);
    }

    /**
     * Язык, выбранный автором попытки, породившей вопрос.
     *
     * @return {@code RUSSIAN}, если вопрос не привязан к попытке — как было и раньше
     */
    @Transactional(readOnly = true)
    public Language findUserLanguageForQuestion(long questionId) {
        return exerciseAttemptDataRepository.findQuestionAttemptContext(questionId)
                .map(QuestionAttemptContextData::userLanguage)
                .orElse(Language.RUSSIAN);
    }

    /** Идентификатор попытки, в рамках которой задан вопрос. */
    @Transactional(readOnly = true)
    public Optional<Long> findAttemptIdOfQuestion(long questionId) {
        return exerciseAttemptDataRepository.findQuestionAttemptContext(questionId)
                .map(QuestionAttemptContextData::attemptId);
    }

    /**
     * Включён ли для упражнения этого вопроса режим вспомогательных вопросов
     * на дереве решений. Для вопроса вне попытки — да, как было и раньше.
     */
    @Transactional(readOnly = true)
    public boolean prefersDecisionTreeSupplementary(long questionId) {
        return exerciseAttemptDataRepository.findQuestionAttemptContext(questionId)
                .map(QuestionAttemptContextData::preferDecisionTreeSupplementary)
                .orElse(true);
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

    /** Попытка в объёме, который уезжает на фронт; пусто, если попытки нет. */
    @Transactional(readOnly = true)
    public Optional<AttemptSummaryData> findSummary(long attemptId) {
        return exerciseAttemptDataRepository.findSummary(attemptId);
    }

    /** Незавершённая попытка пользователя по упражнению; пусто, если такой нет. */
    @Transactional(readOnly = true)
    public Optional<AttemptSummaryData> findIncompleteAttempt(long exerciseId, long userId,
                                                              @Nullable Long courseId) {
        return exerciseAttemptDataRepository.findSummaryWithStatus(
                exerciseId, userId, courseId, AttemptStatus.INCOMPLETE);
    }

    /**
     * Завести попытку, закрыв незавершённые попытки того же пользователя по упражнению.
     * <p>
     * Здесь остаются две вещи, которых нет у слоя доступа к данным: проверка, что курс
     * и упражнение связаны, и LTI-контекст текущего запроса.
     */
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

    /**
     * Отметить попытку завершённой, если стратегия так решила, и выставить оценку.
     * <p>
     * Прежняя версия в остальных случаях звала {@code save} на управляемой сущности —
     * при открытой транзакции это ничего не делало.
     */
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
