package org.vstu.compprehension.Service;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.collections4.ListUtils;
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
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.utils.Checkpointer;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.Mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.vstu.compprehension.models.entities.EnumData.InteractionType.REQUEST_CORRECT_ANSWER;
import static org.vstu.compprehension.models.entities.EnumData.InteractionType.SEND_RESPONSE;

@Service
@Log4j2
public class FrontendService {
    private final QuestionRepository questionRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseAttemptService exerciseAttemptService;
    private final ExerciseService exerciseService;
    private final QuestionService questionService;
    private final AbstractStrategyFactory strategyFactory;
    private final FeedbackRepository feedbackRepository;
    private final InteractionRepository interactionRepository;
    private final LocalizationService localizationService;
    private final DomainFactory domainFactory;
    private final EntityManager entityManager;
    private final UserRepository userRepository;

    public FrontendService(ExerciseAttemptRepository exerciseAttemptRepository, QuestionRepository questionRepository, ExerciseAttemptService exerciseAttemptService, ExerciseService exerciseService, EntityManager entityManager, QuestionService questionService, LocalizationService localizationService, AbstractStrategyFactory strategyFactory, DomainFactory domainFactory, UserRepository userRepository, FeedbackRepository feedbackRepository, InteractionRepository interactionRepository) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.questionRepository = questionRepository;
        this.exerciseAttemptService = exerciseAttemptService;
        this.exerciseService = exerciseService;
        this.entityManager = entityManager;
        this.questionService = questionService;
        this.localizationService = localizationService;
        this.strategyFactory = strategyFactory;
        this.domainFactory = domainFactory;
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.interactionRepository = interactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull SupplementaryFeedbackDto addSupplementaryQuestionAnswer(@NotNull InteractionDto interaction) throws Exception {
        val questionId = interaction.getQuestionId();
        val answers = interaction.getAnswers();
        val question = questionService.getQuestion(questionId);
        if (!question.isSupplementary()) {
            throw new Exception("Question with id" + questionId + " isn't supplementary");
        }

        val responses = questionService.responseQuestion(question, answers);

        return questionService.judgeSupplementaryQuestion(question, responses);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull FeedbackDto addQuestionAnswer(@NotNull InteractionDto interaction) throws Exception {
        Checkpointer ch = new Checkpointer(log);

        val questionId = interaction.getQuestionId();
        val answers = interaction.getAnswers();

        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findByQuestionId(questionId)
                .orElse(null);

        // evaluate answer
        val question = questionService.getSolvedQuestion(questionId);
        val domain = question.getDomain();
        val tags = question.getTags();
        ch.hit("solved question obtained");
        val responses = questionService.responseQuestion(question, answers);
        val newResponses = responses.stream().filter(x -> x.getCreatedByInteraction() == null).collect(Collectors.toList());
        ch.hit("responses collected");
        val judgeResult = questionService.judgeQuestion(question, responses, tags);
        ch.hit("judgeQuestion done");

        // add interaction
        val existingInteractions = question.getQuestionData().getInteractions();
        val ie = new InteractionEntity(SEND_RESPONSE, question.getQuestionData(), judgeResult.violations, judgeResult.correctlyAppliedLaws, responses, newResponses);
        existingInteractions.add(ie);
        val correctInteractionsCount = (int)existingInteractions.stream().filter(i -> i.getViolations().isEmpty()).count();
        // ch.hit("add interaction ("+correctInteractionsCount+")");

        // add feedback & decide next exercise state
        var feedback = ie.getFeedback();
        feedback.setInteractionsLeft(judgeResult.IterationsLeft);
        var grade = 1f;
        var strategyAttemptDecision = Decision.CONTINUE;
        if (attempt != null) {
            var strategy = strategyFactory.getStrategy(attempt.getExercise().getStrategyId());
            grade = strategy.grade(attempt);
            ch.hit("graded with strategy ("+grade+")");

            strategyAttemptDecision = strategy.decide(attempt);
            exerciseAttemptService.ensureAttemptStatus(attempt, strategyAttemptDecision);
            ch.hit("decide next exercise state ("+strategyAttemptDecision.name()+")");
        }
        feedback.setGrade(grade);
        feedbackRepository.save(feedback);
        ch.hit("add feedback ("+judgeResult.IterationsLeft+" interactions left)");

        // calculate error message
        val violations = judgeResult.violations.stream()
                .map(v -> FeedbackViolationLawDto.builder().name(v.getLawName()).canCreateSupplementaryQuestion(domain.needSupplementaryQuestion(v)).build())
                .filter(Objects::nonNull);
        val explanations = judgeResult.explanations.stream().map(HyperText::getText);
        val errors = Streams.zip(violations, explanations, Pair::of)
                .collect(Collectors.toList());
        val locale = getQuestionLanguage(attempt);
        val messages = !errors.isEmpty() && !judgeResult.isAnswerCorrect ? errors.stream().map(pair -> FeedbackDto.Message.Error(pair.getRight(), pair.getLeft())).toArray(FeedbackDto.Message[]::new)
                : judgeResult.IterationsLeft == 0 && judgeResult.isAnswerCorrect ? new FeedbackDto.Message[] { FeedbackDto.Message.Success(localizationService.getMessage("exercise_correct-last-question-answer", locale)) }
                : judgeResult.IterationsLeft > 0 && judgeResult.isAnswerCorrect ? new FeedbackDto.Message[] { FeedbackDto.Message.Success(localizationService.getMessage("exercise_correct-question-answer", locale)) }
                : null;
        // ch.hit("calculate error message ("+ (messages != null ? messages.length : 0) +")");

        // return result of the last correct interaction
        val correctInteraction = existingInteractions.stream()
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0) // select only interactions without mistakes
                .reduce((first, second) -> second);
        val correctAnswers = correctInteraction
                .map(InteractionEntity::getResponses).stream()
                .flatMap(Collection::stream)
                .map(Mapper::toDto)
                .toArray(AnswerDto[]::new);

        // special case for order question
        // force complete answer if the last but one answer is correct
        val isAnswerCorrect = errors.size() == 0 && judgeResult.isAnswerCorrect;
        val orderQuestionOptions = Utils.tryCast(question.getQuestionData().getOptions(), OrderQuestionOptionsEntity.class).orElse(null);
        if (isAnswerCorrect && question.getQuestionData().getQuestionType().equals(QuestionType.ORDER) &&
                orderQuestionOptions != null && !orderQuestionOptions.isMultipleSelectionEnabled() &&
                ie.getFeedback().getInteractionsLeft() == 1 && question.getQuestionData().getAnswerObjects().size() - correctAnswers.length == 1) {
            val correctAnswersIds = Arrays.stream(correctAnswers).map(a -> a.getAnswer()[0]).collect(Collectors.toSet());
            val missingAnswer = question.getQuestionData().getAnswerObjects().stream()
                    .filter(ao -> !correctAnswersIds.contains(ao.getAnswerId().longValue()))
                    .map(ao -> new AnswerDto(ao.getAnswerId().longValue(), ao.getAnswerId().longValue(), true, null))
                    .findFirst().get();
            val newAnswer = ArrayUtils.add(correctAnswers, missingAnswer);
//            ch.hit("results made");
            val res = addQuestionAnswer(new InteractionDto(questionId, newAnswer));
            ch.since_start("addOrdinaryQuestionAnswer() + fill last answer: completed in");
            return res;
        }

        ch.hit("results made");
        ch.since_start("addOrdinaryQuestionAnswer() completed in");

        return Mapper.toFeedbackDto(question,
                messages,
                correctInteractionsCount,
                (int)existingInteractions.stream().filter(i -> i.getViolations().size() > 0).count(),
                ie.getFeedback().getGrade(),
                ie.getFeedback().getInteractionsLeft(),
                correctAnswers,
                isAnswerCorrect,
                strategyAttemptDecision);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull QuestionDto generateQuestion(@NotNull Long exAttemptId) throws Exception {
        val attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));
        val question = questionService.generateQuestion(attempt);
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
        val question = questionRepository.findByIdEager(questionId)
                .orElseThrow();

        val violation = new ViolationEntity(); //TODO: make normal choice
        violation.setLawName(violationLaws[0]);

        var language = getQuestionLanguage(question);
        return questionService.generateSupplementaryQuestion(question, violation, language);
    }

    private Language getQuestionLanguage(QuestionEntity question) {
        return getQuestionLanguage(question.getExerciseAttempt());
    }

    private Language getQuestionLanguage(ExerciseAttemptEntity attempt) {
        return attempt != null
            ? attempt.getUser().getPreferred_language()
            : Language.ENGLISH;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull QuestionDto getQuestion(@NotNull Long questionId) throws Exception {
        val question = questionService.getQuestion(questionId);
        return Mapper.toDto(question);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull FeedbackDto generateNextCorrectAnswer(@NotNull Long questionId) throws Exception {
        // get next correct answer
        val question = questionService.getSolvedQuestion(questionId);
        val correctAnswer = questionService.getNextCorrectAnswer(question);
        val correctAnswerResponses = correctAnswer.answers.stream()
                .map(x -> ResponseEntity.builder().leftAnswerObject(x.getLeft()).rightAnswerObject(x.getRight()).build())
                .collect(Collectors.toList());

        // get last correct interaction responses
        val lastCorrectInteraction = question.getQuestionData().getInteractions().stream()
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0)
                .reduce((first, second) -> second);
        val lastCorrectInteractionResponses = lastCorrectInteraction
                .map(InteractionEntity::getResponses)
                .orElseGet(ArrayList::new);

        // concat last correct interaction responses with new correct answers
        val responses = ListUtils.union(lastCorrectInteractionResponses, correctAnswerResponses);
        //responseRepository.saveAll(responses);

        // evaluate new answer
        val attempt = question.getQuestionData().getExerciseAttempt();
        val tags = question.getTags();
        val newResponses = responses.stream().filter(x -> x.getCreatedByInteraction() == null).collect(Collectors.toList());
        val judgeResult = questionService.judgeQuestion(question, responses, tags);

        // add interaction
        var existingInteractions = question.getQuestionData().getInteractions();
        val ie = new InteractionEntity(REQUEST_CORRECT_ANSWER, question.getQuestionData(), judgeResult.violations, judgeResult.correctlyAppliedLaws, responses, newResponses);
        existingInteractions.add(ie);
        val correctInteractionsCount = (int)existingInteractions.stream().filter(i -> i.getViolations().size() == 0).count();

        // add feedback & decide next exercise state
        var feedback = ie.getFeedback();
        feedback.setInteractionsLeft(judgeResult.IterationsLeft);
        var grade = 1f;
        var strategyAttemptDecision = Decision.CONTINUE;
        if (attempt != null) {
            var strategy = strategyFactory.getStrategy(attempt.getExercise().getStrategyId());
            grade = strategy.grade(attempt);

            strategyAttemptDecision = strategy.decide(attempt);
            exerciseAttemptService.ensureAttemptStatus(attempt, strategyAttemptDecision);
        }
        feedback.setGrade(grade);
        feedbackRepository.save(feedback);

        // build feedback message
        val messages = new FeedbackDto.Message[] { FeedbackDto.Message.Success(correctAnswer.explanation.toString()) };

        return Mapper.toFeedbackDto(question,
                messages,
                correctInteractionsCount,
                (int)existingInteractions.stream().filter(i -> i.getViolations().size() > 0).count(),
                ie.getFeedback().getGrade(),
                ie.getFeedback().getInteractionsLeft(),
                ie.getResponses().stream().map(Mapper::toDto).toArray(AnswerDto[]::new),
                /*true*/ judgeResult.violations.isEmpty() && judgeResult.isAnswerCorrect,
                strategyAttemptDecision);
    }

    public @NotNull ExerciseStatisticsItemDto[] getExerciseStatistics(@NotNull Long exerciseId) {
        val exercise = exerciseService.getExercise(exerciseId);

        val result = exercise.getExerciseAttempts().stream()
                .map(att -> {
                    val questionsCount = att.getQuestions().size();
                    val totalInteractionsCount = att.getQuestions().stream()
                            .filter(q -> q.getInteractions() != null)
                            .flatMap(q -> q.getInteractions().stream()).count();
                    val totalInteractionsWithErrorsCount = att.getQuestions().stream()
                            .filter(q -> q.getInteractions() != null)
                            .flatMap(q -> q.getInteractions().stream())
                            .filter(i -> i.getViolations().size() > 0).count();
                    double avgGrade = att.getQuestions().stream()
                            .filter(q -> q.getInteractions() != null && q.getInteractions().size() > 0)
                            .mapToDouble(q -> {
                                val last = q.getInteractions().stream().reduce((f, s) -> s);
                                val grade = last.map(l -> l.getFeedback()).map(f -> f.getGrade()).orElse(0f);
                                return grade;
                            })
                            .average()
                            .orElse(0d);

                    return ExerciseStatisticsItemDto.builder()
                            .attemptId(att.getId())
                            .averageGrade(avgGrade)
                            .questionsCount(questionsCount)
                            .totalInteractionsCount((int)totalInteractionsCount)
                            .totalInteractionsWithErrorsCount((int)totalInteractionsWithErrorsCount)
                            .build();
                })
                .toArray(ExerciseStatisticsItemDto[]::new);

        return result;
    }

    public @Nullable ExerciseAttemptDto getExerciseAttempt(@NotNull Long attemptId) throws Exception {
        val existingAttempt = exerciseAttemptRepository
                .getById(attemptId);
        return existingAttempt
                .map(Mapper::toDto)
                .orElse(null);
    }

    public @Nullable ExerciseAttemptDto getExistingExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId) throws Exception {
        val existingAttempt = exerciseAttemptRepository
                .getLastWithStatus(exerciseId, userId, AttemptStatus.INCOMPLETE);
        val result = existingAttempt
                .map(Mapper::toDto)
                .orElse(null);
        log.info("Is attempt exists: {}", result != null);

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull ExerciseAttemptDto createExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId) throws Exception {
        var ea = createNewAttempt(exerciseId, userId);
        return Mapper.toDto(ea);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull ExerciseAttemptDto createSolvedExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId) throws Exception {
        var ea = createNewAttempt(exerciseId, userId);
        var strategy = strategyFactory.getStrategy(ea.getExercise().getStrategyId());
        var targetQuestionCount = strategy.getOptions().isMultiStagesEnabled()
                ? ea.getExercise().getStages().stream()
                    .map(ExerciseStageEntity::getNumberOfQuestions)
                    .reduce(Integer::sum)
                    .orElse(1)
                : 1;

        for (int idx = 0; idx < targetQuestionCount; ++idx) {
            var currentQuestion = generateQuestion(ea.getId());
            // var question = questionService.getSolvedQuestion(currentQuestion.getQuestionId());
            /*
            var allCorrectAnswers = domain.getAllAnswersOfSolvedQuestion(question);
            var allAnswers = allCorrectAnswers.stream()
                    .flatMap(x -> x.answers.stream())
                    .map(x -> AnswerDto.builder().answer(new Long[]{ (long)x.getLeft().getAnswerId(), (long)x.getRight().getAnswerId() }).build())
                    .toArray(AnswerDto[]::new);
            addOrdinaryQuestionAnswer(InteractionDto.builder()
                    .attemptId(ea.getId())
                    .questionId(currentQuestion.getQuestionId())
                    .answers(allAnswers)
                    .build());
            */

            entityManager.refresh(ea);

            // debug delay for massive question generation
            if (false) {
                // sleep
                Thread.sleep(500);  // sleep
                // sleep
            }
        }

        return Mapper.toDto(ea);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExerciseAttemptEntity createNewAttempt(@NotNull Long exerciseId, @NotNull Long userId) {
        // complete all incompleted attempts
        val incompletedAttempts = exerciseAttemptRepository.getAllByStatus(exerciseId, userId, AttemptStatus.INCOMPLETE);
        log.info("Found {} existing attempt to complete", incompletedAttempts.size());
        for(val att : incompletedAttempts) {
            att.setAttemptStatus(AttemptStatus.COMPLETED_BY_SYSTEM);
            log.info("Attempt {} completed successfully", att.getId());
        }
        exerciseAttemptRepository.saveAll(incompletedAttempts);

        var exercise = exerciseService.getExercise(exerciseId);
        var user = userRepository.findById(userId).orElseThrow();

        var ea = new ExerciseAttemptEntity();
        ea.setExercise(exercise);
        ea.setUser(user);
        ea.setAttemptStatus(AttemptStatus.INCOMPLETE);
        ea.setQuestions(new ArrayList<>(0));
        exerciseAttemptRepository.save(ea);

        return ea;
    }
}
