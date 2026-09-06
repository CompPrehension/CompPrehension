package org.vstu.compprehension.Service;

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
import org.vstu.compprehension.dto.*;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.feedback.FeedbackViolationLawDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.Explanation;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.models.data.AttemptSummaryData;
import org.vstu.compprehension.models.data.ExerciseStageData;
import org.vstu.compprehension.models.data.NewInteractionData;
import org.vstu.compprehension.models.data.QuestionAttemptContextData;
import org.vstu.compprehension.models.data.ResponseData;
import org.vstu.compprehension.models.data.SubmittedAnswerData;
import org.vstu.compprehension.models.data.ViolationData;
import org.vstu.compprehension.models.data.questionoptions.OrderQuestionOptionsData;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.utils.Checkpointer;
import org.vstu.compprehension.utils.Mapper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.vstu.compprehension.models.entities.EnumData.InteractionType.REQUEST_CORRECT_ANSWER;
import static org.vstu.compprehension.models.entities.EnumData.InteractionType.SEND_RESPONSE;

@Service
@RequiredArgsConstructor
@Log4j2
public class FrontendService {
    private final ExerciseAttemptService exerciseAttemptService;
    private final ExerciseService exerciseService;
    private final QuestionService questionService;
    private final AbstractStrategyFactory strategyFactory;
    private final LocalizationService localizationService;

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull SupplementaryFeedbackDto addSupplementaryQuestionAnswer(@NotNull InteractionDto interaction) throws Exception {
        val questionId = interaction.getQuestionId();
        val question = questionService.getQuestion(questionId);
        if (!question.isSupplementary()) {
            throw new Exception("Question with id" + questionId + " isn't supplementary");
        }

        // Ответ на вспомогательный вопрос в БД не попадает: цепочка ведёт своё состояние
        // шагами, а взаимодействия у неё нет. Раньше строки ответов всё равно писались
        // и оставались ни с чем не связанными.
        val responses = questionService.resolveAnswers(questionId, toSubmittedAnswers(interaction.getAnswers()));

        return questionService.judgeSupplementaryQuestion(question, responses);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull FeedbackDto addQuestionAnswer(@NotNull InteractionDto interaction) throws Exception {
        Checkpointer ch = new Checkpointer(log);

        val questionId = interaction.getQuestionId();
        val answers = toSubmittedAnswers(interaction.getAnswers());
        val context = exerciseAttemptService.findQuestionContext(questionId).orElse(null);

        // evaluate answer
        val question = questionService.getSolvedQuestion(questionId);
        val domain = question.getDomain();
        val tags = question.getTags();
        ch.hit("solved question obtained");
        val responses = questionService.resolveAnswers(questionId, answers);
        ch.hit("responses collected");
        val judgeResult = questionService.judgeQuestion(question, responses, tags);
        ch.hit("judgeQuestion done");

        // add interaction
        val recorded = questionService.recordInteraction(new NewInteractionData(
                questionId,
                SEND_RESPONSE,
                List.of(),
                answers,
                orEmpty(judgeResult.violations),
                orEmpty(judgeResult.correctlyAppliedLaws),
                judgeResult.IterationsLeft));

        // Оценка считается после записи: стратегия смотрит на историю попытки, и это
        // взаимодействие обязано быть её частью — иначе решение принимается по
        // предыдущему ответу.
        val outcome = gradeAndDecide(context, judgeResult);
        questionService.gradeInteraction(recorded.interactionId(), outcome.getLeft());
        ch.hit("graded with strategy (" + outcome.getLeft() + ")");
        if (context != null) {
            exerciseAttemptService.ensureAttemptStatus(context.attemptId(), outcome.getRight());
            ch.hit("decide next exercise state (" + outcome.getRight().name() + ")");
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
                        .map(Mapper::toDto)
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
            ch.since_start("addOrdinaryQuestionAnswer() + fill last answer: completed in");
            return res;
        }

        ch.hit("results made");
        ch.since_start("addOrdinaryQuestionAnswer() completed in");

        return Mapper.toFeedbackDto(question,
                messages,
                recorded.correctInteractionsCount(),
                recorded.erroneousInteractionsCount(),
                outcome.getLeft(),
                judgeResult.IterationsLeft,
                correctAnswers,
                isAnswerCorrect,
                outcome.getRight());
    }

    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull QuestionDto generateQuestion(@NotNull Long exAttemptId) {
        val question = questionService.generateQuestion(exAttemptId);
        return Mapper.toDto(question);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull QuestionDto generateQuestionByMetadata(Integer metadataId, Language lang) throws Exception {
        if (metadataId == null) {
            throw new Exception("Metadata id is null");
        }
        val question = questionService.generateQuestion(metadataId, lang);
        return Mapper.toDto(question);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(@NotNull Long questionId, @NotNull String[] violationLaws) throws Exception {
        val violation = new ViolationData(); //TODO: make normal choice
        violation.setLawName(violationLaws[0]);

        var language = exerciseAttemptService.findUserLanguageForQuestion(questionId);
        return questionService.generateSupplementaryQuestion(questionId, violation, language);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull QuestionDto getQuestion(@NotNull Long questionId) throws Exception {
        val question = questionService.getQuestion(questionId);
        return Mapper.toDto(question);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull FeedbackDto generateNextCorrectAnswer(@NotNull Long questionId) {
        // get next correct answer
        val question = questionService.getSolvedQuestion(questionId);
        val context = exerciseAttemptService.findQuestionContext(questionId).orElse(null);
        val correctAnswer = questionService.getNextCorrectAnswer(question);

        // Подсказка достраивает уже данные студентом ответы, а не начинает решение
        // заново: ответы последнего верного взаимодействия переезжают в это.
        val carried = questionService.findLatestCorrectInteraction(questionId).orElse(null);
        val carriedResponses = carried == null ? List.<ResponseData>of() : carried.responses();
        val newAnswers = correctAnswer.answers.stream()
                .map(x -> new SubmittedAnswerData(x.getLeft().getAnswerId(), x.getRight().getAnswerId(), null))
                .toList();

        // evaluate new answer
        val responses = Stream.concat(
                carriedResponses.stream(),
                questionService.resolveAnswers(questionId, newAnswers).stream()).toList();
        val judgeResult = questionService.judgeQuestion(question, responses, question.getTags());

        // add interaction
        val recorded = questionService.recordInteraction(new NewInteractionData(
                questionId,
                REQUEST_CORRECT_ANSWER,
                carriedResponses.stream().map(ResponseData::getId).toList(),
                newAnswers,
                orEmpty(judgeResult.violations),
                orEmpty(judgeResult.correctlyAppliedLaws),
                judgeResult.IterationsLeft));

        val outcome = gradeAndDecide(context, judgeResult);
        questionService.gradeInteraction(recorded.interactionId(), outcome.getLeft());
        if (context != null) {
            exerciseAttemptService.ensureAttemptStatus(context.attemptId(), outcome.getRight());
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

        return Mapper.toFeedbackDto(question,
                messages,
                recorded.correctInteractionsCount(),
                recorded.erroneousInteractionsCount(),
                outcome.getLeft(),
                judgeResult.IterationsLeft,
                recorded.responses().stream().map(Mapper::toDto).toArray(AnswerDto[]::new),
                /*true*/ judgeResult.violations.isEmpty() && judgeResult.isAnswerCorrect,
                outcome.getRight());
    }

    /**
     * Оценка за взаимодействие и решение стратегии о судьбе попытки.
     * <p>
     * У вопроса вне попытки стратегии нет: он оценивается единицей и продолжается,
     * как было и раньше.
     */
    private @NotNull Pair<Float, Decision> gradeAndDecide(@Nullable QuestionAttemptContextData context,
                                                          @NotNull Domain.InterpretSentenceResult judgeResult) {
        if (context == null) {
            return Pair.of(1f, Decision.CONTINUE);
        }
        var strategy = strategyFactory.getStrategy(context.strategyId());
        float grade = strategy.grade(context.attemptId(), judgeResult);
        return Pair.of(grade, strategy.decide(context.attemptId()));
    }

    /**
     * Списки разбора, которые домен вправе оставить пустыми.
     * <p>
     * {@code InterpretSentenceResult} — обычный класс с полями без инициализации, и
     * часть доменов их не заполняет. {@code *Data} такого не допускает, поэтому пустота
     * приводится к пустому списку прямо на границе.
     */
    private static <T> @NotNull List<T> orEmpty(@Nullable List<T> values) {
        return values == null ? List.of() : values;
    }

    /** Язык вопроса; вне попытки — русский, как было и раньше. */
    private static @NotNull Language questionLanguage(@Nullable QuestionAttemptContextData context) {
        return context == null ? Language.RUSSIAN/*ENGLISH*/ : context.userLanguage();
    }

    /** Ответы с фронта в вид слоя доступа к данным: номера вариантов и их происхождение. */
    private static @NotNull List<SubmittedAnswerData> toSubmittedAnswers(@Nullable AnswerDto[] answers) {
        return answers == null ? List.of() : Arrays.stream(answers)
                .map(answer -> new SubmittedAnswerData(
                        answer.getAnswer()[0].intValue(),
                        answer.getAnswer()[1].intValue(),
                        answer.getCreatedByInteraction()))
                .toList();
    }

    public @Nullable ExerciseAttemptDto getExerciseAttempt(@NotNull Long attemptId) throws Exception {
        return exerciseAttemptService.findSummary(attemptId)
                .map(Mapper::toDto)
                .orElse(null);
    }

    public @Nullable ExerciseAttemptDto getExistingExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId, @Nullable Long courseId) throws Exception {
        val result = exerciseAttemptService.findIncompleteAttempt(exerciseId, userId, courseId)
                .map(Mapper::toDto)
                .orElse(null);
        log.info("Is course attempt exists: {}", result != null);

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull ExerciseAttemptDto createExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId, @Nullable Long courseId) {
        var ea = exerciseAttemptService.createNewAttempt(exerciseId, userId, courseId);
        return Mapper.toDto(ea);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull ExerciseAttemptDto createSolvedExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId, @Nullable Long courseId) throws Exception {
        var ea = exerciseAttemptService.createNewAttempt(exerciseId, userId, courseId);
        return createSolvedExerciseAttempt(ea);
    }

    private @NotNull ExerciseAttemptDto createSolvedExerciseAttempt(AttemptSummaryData ea) throws Exception {
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
        return Mapper.toDto(exerciseAttemptService.findSummary(ea.attemptId()).orElseThrow());
    }
}
