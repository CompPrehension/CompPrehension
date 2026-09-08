package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.common.Utils;
import org.vstu.compprehension.frontend.dto.*;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackViolationLawDto;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.NewInteractionData;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.data.questionoptions.OrderQuestionOptionsData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;
import org.vstu.compprehension.services.*;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.frontend.mappers.FeedbackDtoMapper;
import org.vstu.compprehension.frontend.mappers.QuestionDtoMapper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.vstu.compprehension.enums.InteractionType.REQUEST_CORRECT_ANSWER;
import static org.vstu.compprehension.enums.InteractionType.SEND_RESPONSE;

@Service
@RequiredArgsConstructor
@Log4j2
class ExerciseAttemptFrontendServiceImpl implements ExerciseAttemptFrontendService {
    private final ExerciseAttemptDataService exerciseAttemptService;
    private final ExerciseDataService exerciseService;
    private final QuestionDataService questionService;
    private final AbstractStrategyFactory strategyFactory;
    private final LocalizationService localizationService;
    private final UserDataService userService;
    private final QuestionDtoMapper questionDtoMapper;
    private final FeedbackDtoMapper feedbackDtoMapper;
    private final Mapper<AttemptSummaryData, ExerciseAttemptDto> exerciseAttemptDtoMapper;
    private final Mapper<ResponseData, AnswerDto> answerDtoMapper;

    @Override
    public void ensureCanAccessAttempt(long userId, long attemptId) {
        exerciseAttemptService.ensureCanAccessAttempt(userId, attemptId);
    }

    @Override
    public void ensureCanAccessQuestion(long userId, long questionId) {
        exerciseAttemptService.ensureCanAccessQuestion(userId, questionId);
    }

    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull SupplementaryFeedbackDto addSupplementaryQuestionAnswer(@NotNull InteractionDto interaction) {
        val questionId = interaction.getQuestionId();
        val question = questionService.getQuestion(questionId);
        if (!question.isSupplementary()) {
            throw new Exception("Question with id" + questionId + " isn't supplementary");
        }

        var currentUser = userService.getCurrentUser();
        var language = currentUser.language();

        val responses = questionService.resolveAnswers(questionId, toSubmittedAnswers(interaction.getAnswers()));

        return questionService.judgeSupplementaryQuestion(question, responses, language);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull FeedbackDto addQuestionAnswer(@NotNull InteractionDto interaction) {
        val questionId = interaction.getQuestionId();
        val answers = toSubmittedAnswers(interaction.getAnswers());
        val context = exerciseAttemptService.findQuestionContext(questionId)
                .orElseThrow();
        
        var currentUser = userService.getCurrentUser();
        var language = currentUser.language();

        // evaluate answer
        val question = questionService.getSolvedQuestion(questionId);
        val domain = question.getDomain();
        val tags = question.getTags();
        val responses = questionService.resolveAnswers(questionId, answers);
        val judgeResult = domain.judgeQuestion(question, responses, tags, language);

        // add interaction
        val recorded = questionService.recordInteraction(new NewInteractionData(
                questionId,
                SEND_RESPONSE,
                List.of(),
                answers,
                orEmpty(judgeResult.violations),
                orEmpty(judgeResult.correctlyAppliedLaws),
                judgeResult.IterationsLeft));

        var strategy = strategyFactory.getStrategy(context.strategyId());
        var strategyDecision = strategy.gradeAndDecide(context.attemptId(), judgeResult);
        questionService.gradeInteraction(recorded.interactionId(), strategyDecision.grade());
        if (context != null) {
            exerciseAttemptService.ensureAttemptStatus(context.attemptId(), strategyDecision.decision());
        }

        val locale = questionLanguage(context);
        // calculate error message
        val violations = judgeResult.violations.stream()
                .map(v -> FeedbackViolationLawDto.builder().name(v.getLawName()).canCreateSupplementaryQuestion(domain.needSupplementaryQuestion(v.getLawName(), v.getInteractionType())).build())
                .filter(Objects::nonNull).toList();
        Collection<Explanation> explanationSource = judgeResult.explanation.getRawMessage().isEmpty() ? judgeResult.explanation.getChildren() : List.of(judgeResult.explanation);
        val errors = explanationSource.stream().map(e -> Pair.of(
                violations.stream().filter(v -> e.getDomainLawNames().contains(v.getName())).toList(),
                e.toHyperText(locale).getText())).toList();
        val messages = !errors.isEmpty() && !judgeResult.isAnswerCorrect ? errors.stream().map(pair -> FeedbackDto.Message.Error(pair.getRight(), pair.getLeft())).toArray(FeedbackDto.Message[]::new)
                : judgeResult.IterationsLeft == 0 && judgeResult.isAnswerCorrect ? new FeedbackDto.Message[] { FeedbackDto.Message.Success(localizationService.getMessage("exercise_correct-last-question-answer", locale), violations) }
                : judgeResult.IterationsLeft > 0 && judgeResult.isAnswerCorrect ? new FeedbackDto.Message[] { FeedbackDto.Message.Success(localizationService.getMessage("exercise_correct-question-answer", locale), violations) }
                : null;

        // return result of the last correct interaction
        val correctAnswers = recorded.latestCorrectInteraction() == null
                ? new AnswerDto[0]
                : recorded.latestCorrectInteraction().responses().stream()
                        .map(answerDtoMapper::map)
                        .toArray(AnswerDto[]::new);

        // special case for order question
        // force complete answer if the last but one answer is correct
        val isAnswerCorrect = errors.isEmpty() && judgeResult.isAnswerCorrect;
        val orderQuestionOptions = Utils.tryCast(question.getQuestionData().getOptions(), OrderQuestionOptionsData.class).orElse(null);
        if (isAnswerCorrect && question.getQuestionData().getQuestionType().equals(QuestionType.ORDER) &&
                orderQuestionOptions != null && !orderQuestionOptions.isMultipleSelectionEnabled() &&
                judgeResult.IterationsLeft == 1 && question.getQuestionData().getAnswerObjects().size() - correctAnswers.length == 1) {
            val correctAnswersIds = Arrays.stream(correctAnswers).map(a -> a.getAnswer()[0]).collect(Collectors.toSet());
            val missingAnswer = question.getQuestionData().getAnswerObjects().stream()
                    .filter(ao -> !correctAnswersIds.contains(ao.getAnswerId().longValue()))
                    .map(ao -> new AnswerDto(ao.getAnswerId().longValue(), ao.getAnswerId().longValue(), true, null))
                    .findFirst().get();
            val newAnswer = ArrayUtils.add(correctAnswers, missingAnswer);
            val res = addQuestionAnswer(new InteractionDto(questionId, newAnswer));
            return res;
        }

        return feedbackDtoMapper.map(question,
                messages,
                recorded.correctInteractionsCount(),
                recorded.erroneousInteractionsCount(),
                strategyDecision.grade(),
                judgeResult.IterationsLeft,
                correctAnswers,
                isAnswerCorrect,
                strategyDecision.decision(),
                language);
    }

    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull QuestionDto generateQuestion(@NotNull Long exAttemptId) {
        val question = questionService.generateQuestion(exAttemptId);
        return questionDtoMapper.map(question, userService.getCurrentUser().language());
    }

    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull QuestionDto generateQuestionByMetadata(Integer metadataId, Language lang) {
        if (metadataId == null) {
            throw new Exception("Metadata id is null");
        }
        val question = questionService.generateQuestion(metadataId, lang);
        return questionDtoMapper.map(question, userService.getCurrentUser().language());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(@NotNull Long questionId, @NotNull String[] violationLaws) {
        val violation = new ViolationData(); //TODO: make normal choice
        violation.setLawName(violationLaws[0]);

        var language = exerciseAttemptService.findUserLanguageForQuestion(questionId);
        return questionService.generateSupplementaryQuestion(questionId, violation, language);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull QuestionDto getQuestion(@NotNull Long questionId) {
        val question = questionService.getQuestion(questionId);
        return questionDtoMapper.map(question, userService.getCurrentUser().language());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull FeedbackDto generateNextCorrectAnswer(@NotNull Long questionId) {
        // get next correct answer
        val question = questionService.getSolvedQuestion(questionId);
        val context = exerciseAttemptService.findQuestionContext(questionId).orElseThrow();
        var domain = question.getDomain();
        var currentUser = userService.getCurrentUser();
        var language = currentUser.language();
        val correctAnswer = domain.getAnyNextCorrectAnswer(question, language);

        // Подсказка достраивает уже данные студентом ответы, а не начинает решение
        // заново: ответы последнего верного взаимодействия переезжают в это.
        val carried = questionService.findLatestCorrectInteraction(questionId).orElse(null);
        val carriedResponses = carried == null ? List.<ResponseData>of() : carried.responses();
        val newAnswers = correctAnswer.answers.stream()
                .map(x -> new SubmittedAnswerData(x.getLeft().getAnswerId(), x.getRight().getAnswerId(), null))
                .toList();

        // evaluate new answer
        val responses = Stream.<AnswerData>concat(
                carriedResponses.stream(),
                questionService.resolveAnswers(questionId, newAnswers).stream()).toList();
        val judgeResult = domain.judgeQuestion(question, responses, question.getTags(), language);

        // add interaction
        val recorded = questionService.recordInteraction(new NewInteractionData(
                questionId,
                REQUEST_CORRECT_ANSWER,
                carriedResponses.stream().map(ResponseData::getId).toList(),
                newAnswers,
                orEmpty(judgeResult.violations),
                orEmpty(judgeResult.correctlyAppliedLaws),
                judgeResult.IterationsLeft));

        var strategy = strategyFactory.getStrategy(context.strategyId());
        var strategyDecision = strategy.gradeAndDecide(context.attemptId(), judgeResult);
        questionService.gradeInteraction(recorded.interactionId(), strategyDecision.grade());
        if (context != null) {
            exerciseAttemptService.ensureAttemptStatus(context.attemptId(), strategyDecision.decision());
        }

        // build feedback message
        val locale = questionLanguage(context);
        val messages = correctAnswer.explanation.getChildren().stream()
                .map(e -> FeedbackDto.Message.Success(e.toHyperText(locale).getText(),
                        e.getDomainLawNames().stream().map(law ->
                                FeedbackViolationLawDto.builder()
                                        .name(law)
                                        .canCreateSupplementaryQuestion(false).build()).toList()))
                .toList().toArray(new FeedbackDto.Message[0]);

        return feedbackDtoMapper.map(question,
                messages,
                recorded.correctInteractionsCount(),
                recorded.erroneousInteractionsCount(),
                strategyDecision.grade(),
                judgeResult.IterationsLeft,
                recorded.responses().stream().map(answerDtoMapper::map).toArray(AnswerDto[]::new),
                /*true*/ judgeResult.violations.isEmpty() && judgeResult.isAnswerCorrect,
                strategyDecision.decision(),
                language);
    }

    private static <T> @NotNull List<T> orEmpty(@Nullable List<T> values) {
        return values == null ? List.of() : values;
    }

    private static @NotNull Language questionLanguage(@Nullable QuestionAttemptContextData context) {
        return context == null ? Language.RUSSIAN/*ENGLISH*/ : context.userLanguage();
    }

    private static @NotNull List<SubmittedAnswerData> toSubmittedAnswers(@Nullable AnswerDto[] answers) {
        return answers == null ? List.of() : Arrays.stream(answers)
                .map(answer -> new SubmittedAnswerData(
                        answer.getAnswer()[0].intValue(),
                        answer.getAnswer()[1].intValue(),
                        answer.getCreatedByInteraction()))
                .toList();
    }

    public @Nullable ExerciseAttemptDto getExerciseAttempt(@NotNull Long attemptId) {
        return exerciseAttemptService.findSummary(attemptId)
                .map(exerciseAttemptDtoMapper::map)
                .orElse(null);
    }

    public @Nullable ExerciseAttemptDto getExistingExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId, @Nullable Long courseId) {
        val result = exerciseAttemptService.findIncompleteAttempt(exerciseId, userId, courseId)
                .map(exerciseAttemptDtoMapper::map)
                .orElse(null);
        log.info("Is course attempt exists: {}", result != null);

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull ExerciseAttemptDto createExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId, @Nullable Long courseId) {
        var ea = exerciseAttemptService.createNewAttempt(exerciseId, userId, courseId);
        return exerciseAttemptDtoMapper.map(ea);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull ExerciseAttemptDto createSolvedExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId, @Nullable Long courseId) {
        var ea = exerciseAttemptService.createNewAttempt(exerciseId, userId, courseId);
        return generateQuestions(ea);
    }

    @SneakyThrows
    private @NotNull ExerciseAttemptDto generateQuestions(AttemptSummaryData ea) {
        var exercise = exerciseService.getExercise(ea.exerciseId());
        var strategy = strategyFactory.getStrategy(exercise.strategyId());
        var targetQuestionCount = strategy.getOptions().isMultiStagesEnabled()
                ? exercise.stages().stream()
                    .map(ExerciseStageData::getNumberOfQuestions)
                    .reduce(Integer::sum)
                    .orElse(1)
                : 1;

        for (int idx = 0; idx < targetQuestionCount; ++idx) {
            var currentQuestion = generateQuestion(ea.attemptId());
            // var question = questionService.getSolvedQuestion(currentQuestion.getQuestionId());
            /*
            var allCorrectAnswers = domain.getAllAnswersOfSolvedQuestion(question);
            var allAnswers = allCorrectAnswers.stream()
                    .flatMap(x -> x.answers.stream())
                    .map(x -> AnswerDto.builder().answer(new Long[]{ (long)x.getLeft().getAnswerId(), (long)x.getRight().getAnswerId() }).build())
                    .toArray(AnswerDto[]::new);
            addOrdinaryQuestionAnswer(InteractionDto.builder()
                    .attemptId(ea.attemptId())
                    .questionId(currentQuestion.getQuestionId())
                    .answers(allAnswers)
                    .build());
            */

            // debug delay for massive question generation
            if (false) {
                // sleep
                Thread.sleep(500);  // sleep
                // sleep
            }
        }

        // Сводка перечитывается: за время цикла у попытки появились вопросы.
        return exerciseAttemptDtoMapper.map(exerciseAttemptService.findSummary(ea.attemptId()).orElseThrow());
    }
}
