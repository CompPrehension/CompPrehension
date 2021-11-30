package org.vstu.compprehension.Service;

import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.dto.AnswerDto;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.ExerciseStatisticsItemDto;
import org.vstu.compprehension.dto.InteractionDto;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.feedback.FeedbackViolationLawDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.Strategy;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.OrderQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.models.entities.ViolationEntity;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.utils.DomainAdapter;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.Mapper;
import org.vstu.compprehension.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.vstu.compprehension.models.entities.EnumData.InteractionType.REQUEST_CORRECT_ANSWER;
import static org.vstu.compprehension.models.entities.EnumData.InteractionType.SEND_RESPONSE;

@Service
@Log4j2
public class FrontendService {
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;

    @Autowired
    private ExerciseAttemptService exerciseAttemptService;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private Strategy strategy;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ResponseRepository responseRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ViolationRepository violationRepository;

    @Autowired
    private LocalizationService localizationService;

    public @NotNull FeedbackDto addQuestionAnswer(@NotNull InteractionDto interaction) throws Exception {
        val questionId = interaction.getQuestionId();
        val question = questionService.getQuestion(questionId);
        return question.isSupplementary()
                ? addSupplementaryQuestionAnswer(interaction)
                : addOrdinaryQuestionAnswer(interaction);
    }

    private @NotNull FeedbackDto addSupplementaryQuestionAnswer(@NotNull InteractionDto interaction) throws Exception {
        val exAttemptId = interaction.getAttemptId();
        val questionId = interaction.getQuestionId();
        val answers = interaction.getAnswers();

        val attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));
        val question = questionService.getQuestion(questionId);
        if (!question.isSupplementary()) {
            throw new Exception("Question with id" + questionId + " isn't supplementary");
        }

        val domain = DomainAdapter.getDomain(attempt.getExercise().getDomain().getClassPath());
        assert domain != null;

        val responses = questionService.responseQuestion(question, answers);
        val judgeResult = questionService.judgeSupplementaryQuestion(question, responses, attempt);
        val violation = judgeResult.violations.stream()
                .map(v -> FeedbackViolationLawDto.builder().name(v.getLawName()).canCreateSupplementaryQuestion(domain.needSupplementaryQuestion(v)).build())
                .findFirst()
                .orElse(null);
        val locale = attempt.getUser().getPreferred_language().toLocale();
        val messages = judgeResult.isAnswerCorrect
                ? new FeedbackDto.Message[] { FeedbackDto.Message.Success(localizationService.getMessage("exercise_correct-sup-question-answer", locale), violation) }
                : new FeedbackDto.Message[] { FeedbackDto.Message.Error(localizationService.getMessage("exercise_wrong-sup-question-answer", locale), violation) };
        return FeedbackDto.builder()
                .messages(messages)
                .isCorrect(judgeResult.isAnswerCorrect)
                .build();
    }

    private @NotNull FeedbackDto addOrdinaryQuestionAnswer(@NotNull InteractionDto interaction) throws Exception {
        val exAttemptId = interaction.getAttemptId();
        val questionId = interaction.getQuestionId();
        val answers = interaction.getAnswers();

        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));

        // evaluate answer
        val tags = attempt.getExercise().getTags();
        val question = questionService.getSolvedQuestion(questionId);
        val responses = questionService.responseQuestion(question, answers);
        val newResponses = responses.stream().filter(x -> x.getCreatedByInteraction() == null).collect(Collectors.toList());
        val judgeResult = questionService.judgeQuestion(question, responses, tags);

        // add interaction
        val existingInteractions = question.getQuestionData().getInteractions();
        val ie = new InteractionEntity(SEND_RESPONSE, question.getQuestionData(), judgeResult.violations, judgeResult.correctlyAppliedLaws, responses, newResponses);
        existingInteractions.add(ie);
        val correctInteractionsCount = (int)existingInteractions.stream().filter(i -> i.getViolations().size() == 0).count();

        // add feedback
        val grade = strategy.grade(attempt);
        ie.getFeedback().setInteractionsLeft(judgeResult.IterationsLeft);
        ie.getFeedback().setGrade(grade);
        feedbackRepository.save(ie.getFeedback());

        // decide next exercise state
        val strategyAttemptDecision = strategy.decide(attempt);
        exerciseAttemptService.ensureAttemptStatus(attempt, strategyAttemptDecision);

        // calculate error message
        val domain = DomainAdapter.getDomain(attempt.getExercise().getDomain().getClassPath()); assert domain != null;
        val violations = judgeResult.violations.stream()
                .map(v -> FeedbackViolationLawDto.builder().name(v.getLawName()).canCreateSupplementaryQuestion(domain.needSupplementaryQuestion(v)).build())
                .filter(Objects::nonNull);
        val explanations = questionService.explainViolations(question, judgeResult.violations).stream().map(HyperText::getText);
        val errors = Streams.zip(violations, explanations, Pair::of)
                .collect(Collectors.toList());
        val locale = attempt.getUser().getPreferred_language().toLocale();
        val messages = errors.size() > 0 && !judgeResult.isAnswerCorrect ? errors.stream().map(pair -> FeedbackDto.Message.Error(pair.getRight(), pair.getLeft())).toArray(FeedbackDto.Message[]::new)
                : judgeResult.IterationsLeft == 0 && judgeResult.isAnswerCorrect ? new FeedbackDto.Message[] { FeedbackDto.Message.Success(localizationService.getMessage("exercise_correct-last-question-answer", locale)) }
                : judgeResult.IterationsLeft > 0 && judgeResult.isAnswerCorrect ? new FeedbackDto.Message[] { FeedbackDto.Message.Success(localizationService.getMessage("exercise_correct-question-answer", locale)) }
                : null;

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
                ie.getFeedback().getInteractionsLeft() == 1) {
            val correctAnswersIds = Arrays.stream(correctAnswers).map(a -> a.getAnswer()[0]).collect(Collectors.toSet());
            val missingAnswer = question.getQuestionData().getAnswerObjects().stream()
                    .filter(ao -> !correctAnswersIds.contains(ao.getAnswerId().longValue()))
                    .map(ao -> new AnswerDto(ao.getAnswerId().longValue(), ao.getAnswerId().longValue(), true, null))
                    .findFirst().get();
            val newAnswer = ArrayUtils.add(correctAnswers, missingAnswer);
            return addOrdinaryQuestionAnswer(new InteractionDto(exAttemptId, questionId, newAnswer));
        }


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

    public @NotNull QuestionDto generateQuestion(@NotNull Long exAttemptId) throws Exception {
        val attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));
        val question = questionService.generateQuestion(attempt);
        return Mapper.toDto(question);
    }

    public @Nullable QuestionDto generateSupplementaryQuestion(@NotNull Long exAttemptId, @NotNull Long questionId, @NotNull String[] violationLaws) throws Exception {
        val attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));
        val question = attempt.getQuestions().stream().filter(q -> q.getId().equals(questionId)).findFirst()
                .orElseThrow(() -> new Exception("Can't find question with id " + questionId));
        val lastInteraction = Iterables.getLast(question.getInteractions());

        val violation = new ViolationEntity(); //TODO: make normal choice
        violation.setLawName(violationLaws[0]);

        val supQuestion = questionService.generateSupplementaryQuestion(question, violation, question.getExerciseAttempt().getUser().getPreferred_language());
        return supQuestion != null ? Mapper.toDto(supQuestion) : null;
    }

    public @NotNull QuestionDto getQuestion(@NotNull Long questionId) throws Exception {
        val question = questionService.getQuestion(questionId);
        return Mapper.toDto(question);
    }

    public @NotNull FeedbackDto generateNextCorrectAnswer(@NotNull Long questionId) throws Exception {
        // get next correct answer
        val question = questionService.getSolvedQuestion(questionId);
        val correctAnswer = questionService.getNextCorrectAnswer(question);
        val correctAnswerResponses = correctAnswer.answers.stream()
                .map(x -> ResponseEntity.builder().leftAnswerObject(x.getLeft()).rightAnswerObject(x.getRight()).build())
                .collect(Collectors.toList());

        // get last correct interaction responses
        val lastCorrectInteraction = Optional.ofNullable(question.getQuestionData().getInteractions()).stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getFeedback().getInteractionsLeft() >= 0 && i.getViolations().size() == 0)
                .reduce((first, second) -> second);
        val lastCorrectInteractionResponses = lastCorrectInteraction
                .map(InteractionEntity::getResponses)
                .orElseGet(ArrayList::new);

        // concat last correct interaction responses with new correct answers
        val responses = ListUtils.union(lastCorrectInteractionResponses, correctAnswerResponses);
        //responseRepository.saveAll(responses);

        // evaluate new answer
        val exerciseAttempt = question.getQuestionData().getExerciseAttempt();
        val tags = exerciseAttempt.getExercise().getTags();
        val newResponses = responses.stream().filter(x -> x.getCreatedByInteraction() == null).collect(Collectors.toList());
        val judgeResult = questionService.judgeQuestion(question, responses, tags);

        // add interaction
        val existingInteractions = question.getQuestionData().getInteractions();
        val ie = new InteractionEntity(REQUEST_CORRECT_ANSWER, question.getQuestionData(), judgeResult.violations, judgeResult.correctlyAppliedLaws, responses, newResponses);
        existingInteractions.add(ie);
        val correctInteractionsCount = (int)existingInteractions.stream().filter(i -> i.getViolations().size() == 0).count();

        // add feedback
        val grade = strategy.grade(exerciseAttempt);
        ie.getFeedback().setInteractionsLeft(judgeResult.IterationsLeft);
        ie.getFeedback().setGrade(grade);
        //feedbackRepository.save(ie.getFeedback());
        interactionRepository.save(ie);

        // decide next exercise state
        val strategyAttemptDecision = strategy.decide(exerciseAttempt);
        exerciseAttemptService.ensureAttemptStatus(exerciseAttempt, strategyAttemptDecision);

        // build feedback message
        val messages = new FeedbackDto.Message[] { FeedbackDto.Message.Success(correctAnswer.explanation.toString()) };

        return Mapper.toFeedbackDto(question,
                messages,
                correctInteractionsCount,
                (int)existingInteractions.stream().filter(i -> i.getViolations().size() > 0).count(),
                ie.getFeedback().getGrade(),
                ie.getFeedback().getInteractionsLeft(),
                ie.getResponses().stream().map(Mapper::toDto).toArray(AnswerDto[]::new),
                true,
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

    public @Nullable ExerciseAttemptDto getExistingExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId) throws Exception {
        val existingAttempt = exerciseAttemptRepository
                .findFirstByExerciseIdAndUserIdAndAttemptStatusOrderByIdDesc(exerciseId, userId, AttemptStatus.INCOMPLETE);
        val result = existingAttempt
                .map(Mapper::toDto)
                .orElse(null);
        log.info("Is attempt exists: {}", result != null);

        return result;
    }

    public @NotNull ExerciseAttemptDto createExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId) throws Exception {
        // complete all incompleted attempts
        val incompletedAttempts = exerciseAttemptRepository.findAllByExerciseIdAndUserIdAndAttemptStatusOrderByIdDesc(exerciseId, userId, AttemptStatus.INCOMPLETE);
        log.info("Found {} existing attempt to complete", incompletedAttempts.size());
        for(val att : incompletedAttempts) {
            att.setAttemptStatus(AttemptStatus.COMPLETED_BY_SYSTEM);
            log.info("Attempt {} completed successfully", att.getId());
        }
        exerciseAttemptRepository.saveAll(incompletedAttempts);

        val exercise = exerciseService.getExercise(exerciseId);
        val user = userService.getUser(userId);

        val ea = new ExerciseAttemptEntity();
        ea.setExercise(exercise);
        ea.setUser(user);
        ea.setAttemptStatus(AttemptStatus.INCOMPLETE);
        ea.setQuestions(new ArrayList<>(0));
        exerciseAttemptRepository.save(ea);

        return Mapper.toDto(ea);
    }
}
