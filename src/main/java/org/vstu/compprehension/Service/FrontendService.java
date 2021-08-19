package org.vstu.compprehension.Service;

import com.google.common.collect.Iterables;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.vstu.compprehension.dto.ExerciseAttemptDto;
import org.vstu.compprehension.dto.ExerciseStatisticsItemDto;
import org.vstu.compprehension.dto.InteractionDto;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.Ordering;
import org.vstu.compprehension.models.businesslogic.SingleChoice;
import org.vstu.compprehension.models.businesslogic.Strategy;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.ViolationEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.FeedbackRepository;
import org.vstu.compprehension.models.repository.ViolationRepository;
import org.vstu.compprehension.utils.DomainAdapter;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.Mapper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.vstu.compprehension.models.entities.EnumData.InteractionType.REQUEST_CORRECT_ANSWER;
import static org.vstu.compprehension.models.entities.EnumData.InteractionType.SEND_RESPONSE;

@Service
public class FrontendService {
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;

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
    private ViolationRepository violationRepository;

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

        val responses = questionService.responseQuestion(question, answers);
        val judgeResult = questionService.judgeSupplementaryQuestion(question, responses, attempt);
        val violations = judgeResult.violations.stream().map(ViolationEntity::getLawName).toArray(String[]::new);
        val messages = judgeResult.isAnswerCorrect
                ? new FeedbackDto.Message[] { FeedbackDto.Message.Success("Correct!", violations) }
                : new FeedbackDto.Message[] { FeedbackDto.Message.Error("Incorrect", violations) };
        return FeedbackDto.builder()
                .messages(messages)
                .build();
    }

    private @NotNull FeedbackDto addOrdinaryQuestionAnswer(@NotNull InteractionDto interaction) throws Exception {
        Long exAttemptId = interaction.getAttemptId();
        Long questionId = interaction.getQuestionId();
        Long[][] answers = interaction.getAnswers();

        ExerciseAttemptEntity attempt = exerciseAttemptRepository.findById(exAttemptId)
                .orElseThrow(() -> new Exception("Can't find attempt with id " + exAttemptId));

        // evaluate answer
        val tags = attempt.getExercise().getTags();
        val question = questionService.getSolvedQuestion(questionId);
        val responses = questionService.responseQuestion(question, answers);
        val judgeResult = questionService.judgeQuestion(question, responses, tags);

        // add interaction
        val existingInteractions = question.getQuestionData().getInteractions();
        val ie = new InteractionEntity(SEND_RESPONSE, question.getQuestionData(), judgeResult.violations, judgeResult.correctlyAppliedLaws, responses);
        existingInteractions.add(ie);
        val correctInteractionsCount = (int)existingInteractions.stream().filter(i -> i.getViolations().size() == 0).count();

        // add feedback
        val grade = strategy.grade(attempt);
        ie.getFeedback().setInteractionsLeft(judgeResult.IterationsLeft);
        ie.getFeedback().setGrade(grade);
        feedbackRepository.save(ie.getFeedback());

        // calculate error message
        val violationIds = judgeResult.violations.stream().map(ViolationEntity::getLawName)
                .filter(Objects::nonNull);
        val explanations = questionService.explainViolations(question, judgeResult.violations).stream().map(HyperText::getText);
        val errors = Streams.zip(violationIds, explanations, Pair::of)
                .collect(Collectors.toList());
        val messages = errors.size() > 0 && !judgeResult.isAnswerCorrect ? errors.stream().map(pair -> FeedbackDto.Message.Error(pair.getRight(), new String[] { pair.getKey() })).toArray(FeedbackDto.Message[]::new)
                : judgeResult.IterationsLeft == 0 && judgeResult.isAnswerCorrect ? new FeedbackDto.Message[] { FeedbackDto.Message.Success("All done!", new String[0]) }
                : judgeResult.IterationsLeft > 0 && judgeResult.isAnswerCorrect ? new FeedbackDto.Message[] { FeedbackDto.Message.Success("Correct, keep doing...", new String[0]) }
                : null;

        // remove incorrect answers from feedback
        val correctAnswers = errors.size() > 0
                ? Arrays.copyOf(answers, answers.length - 1)
                : answers;

        return Mapper.toFeedbackDto(question,
                ie,
                messages,
                correctInteractionsCount,
                (int)existingInteractions.stream().filter(i -> i.getViolations().size() > 0).count(),
                correctAnswers);
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
        val correctAnswerDto = Mapper.toDto(correctAnswer);

        // evaluate new answer
        val tags = question.getQuestionData().getExerciseAttempt().getExercise().getTags();
        val responses = questionService.responseQuestion(question, correctAnswerDto.getAnswers());
        val judgeResult = questionService.judgeQuestion(question, responses, tags);

        // add interaction
        val existingInteractions = question.getQuestionData().getInteractions();
        val ie = new InteractionEntity(REQUEST_CORRECT_ANSWER, question.getQuestionData(), judgeResult.violations, judgeResult.correctlyAppliedLaws, responses);
        existingInteractions.add(ie);
        val correctInteractionsCount = (int)existingInteractions.stream().filter(i -> i.getViolations().size() == 0).count();

        // add feedback
        val grade = strategy.grade(question.getQuestionData().getExerciseAttempt());
        ie.getFeedback().setInteractionsLeft(judgeResult.IterationsLeft);
        ie.getFeedback().setGrade(grade);
        feedbackRepository.save(ie.getFeedback());

        // build feedback message
        val messages = new FeedbackDto.Message[] { FeedbackDto.Message.Success(correctAnswerDto.getExplanation(), new String[0]) };

        return Mapper.toFeedbackDto(question,
                ie,
                messages,
                correctInteractionsCount,
                (int)existingInteractions.stream().filter(i -> i.getViolations().size() > 0).count(),
                correctAnswerDto.getAnswers());
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
        return existingAttempt
                .map(Mapper::toDto)
                .orElse(null);
    }

    public @NotNull ExerciseAttemptDto createExerciseAttempt(@NotNull Long exerciseId, @NotNull Long userId) throws Exception {
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
