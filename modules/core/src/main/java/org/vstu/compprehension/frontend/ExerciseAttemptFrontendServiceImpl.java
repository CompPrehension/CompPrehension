package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.common.Utils;
import org.vstu.compprehension.enums.RoleInExercise;
import org.vstu.compprehension.frontend.dto.*;
import org.vstu.compprehension.frontend.dto.feedback.ClarificationAnswerDto;
import org.vstu.compprehension.frontend.dto.feedback.ClarificationFeedbackDto;
import org.vstu.compprehension.frontend.dto.feedback.FeedbackDto;
import org.vstu.compprehension.frontend.dto.question.QuestionDto;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.domains.Judgement;
import org.vstu.compprehension.businesslogic.domains.Reasoning;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.businesslogic.strategies.AnswerReaction;
import org.vstu.compprehension.businesslogic.strategies.StrategyDecision;
import org.vstu.compprehension.data.exerciseattempt.AttemptSummaryData;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.AnswerFeedbackData;
import org.vstu.compprehension.data.question.CountedKnowledgeData;
import org.vstu.compprehension.data.question.HypothesisClarificationData;
import org.vstu.compprehension.data.question.InteractionReasoningData;
import org.vstu.compprehension.data.question.NewInteractionAnswerData;
import org.vstu.compprehension.data.question.NewInteractionData;
import org.vstu.compprehension.data.question.QuestionAttemptContextData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.data.questionoptions.OrderQuestionOptionsData;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;
import org.vstu.compprehension.services.*;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.mappers.UpdateMapper;
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
    private final DomainFactory domainFactory;
    private final AbstractStrategyFactory strategyFactory;
    private final LocalizationService localizationService;
    private final UserDataService userService;
    private final QuestionDtoMapper questionDtoMapper;
    private final FeedbackDtoMapper feedbackDtoMapper;
    private final RandomProvider randomProvider;
    private final Mapper<AttemptSummaryData, ExerciseAttemptDto> exerciseAttemptDtoMapper;
    private final Mapper<ResponseData, NewInteractionAnswerData> carriedAnswerMapper;
    private final Mapper<SubmittedAnswerData, NewInteractionAnswerData> submittedAnswerMapper;
    private final UpdateMapper<Reasoning, InteractionReasoningData> reasoningMapper;
    private final UpdateMapper<Judgement.Verdict, InteractionReasoningData> verdictMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull SupplementaryFeedbackDto addSupplementaryQuestionAnswer(@NotNull InteractionDto interaction) {
        val questionId = interaction.getQuestionId();

        var currentUser = userService.getCurrentUser();
        var language = currentUser.language();

        val answers = toSubmittedAnswers(questionService.getQuestionType(questionId), interaction.getAnswers());
        val responses = questionService.resolveAnswers(questionId, answers);

        return questionService.judgeSupplementaryQuestion(questionId, responses, language);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull FeedbackDto addQuestionAnswer(@NotNull InteractionDto interaction) {
        var questionId = interaction.getQuestionId();
        var question = questionService.getSolvedQuestion(questionId);
        return addQuestionAnswer(question, toSubmittedAnswers(question.getContent().getQuestionType(), interaction.getAnswers()));
    }

    private @NotNull FeedbackDto addQuestionAnswer(long questionId, @NotNull List<SubmittedAnswerData> answers) {
        var question = questionService.getSolvedQuestion(questionId);
        return addQuestionAnswer(question, answers);
    }

    private @NotNull FeedbackDto addQuestionAnswer(QuestionData question, @NotNull List<SubmittedAnswerData> answers) {
        var questionId = question.getId();
        if (questionId == null) {
            throw new IllegalArgumentException("Question Id is null");
        }

        var context = exerciseAttemptService.findQuestionContext(questionId).orElse(null);

        var currentUser = userService.getCurrentUser();
        var language = currentUser.language();

        // evaluate answer
        val domain = domainFactory.getDomain(question.getContent().getDomainId());
        val tags = domain.resolveTags(question.getContent().getTags());
        val responses = questionService.resolveAnswers(questionId, answers);
        val judgement = domain.judgeAnswer(question, responses, tags, language);
        Explanation explanation;
        List<InteractionReasoningData> reasonings;
        HypothesisClarificationData clarification;
        switch (judgement) {
            // Ход мысли не установлен: выбирать нечего, неверный ответ объясняется самим вердиктом.
            case Judgement.Verdict verdict -> {
                explanation = verdict.isAnswerCorrect() ? Explanation.empty(Explanation.Type.ERROR) : verdict.explanation();
                reasonings = List.of(toReasoningData(verdict));
                clarification = null;
            }
            case Judgement.Reasoned reasoned -> {
                var reaction = context == null
                        ? reactToDebuggedAnswer(reasoned)
                        : strategyFactory.getStrategy(context.strategyId()).reactToAnswer(context.attemptId(), reasoned);
                explanation = explainAnswer(reasoned, reaction.reply());
                reasonings = toReasoningData(reasoned, reaction.probable());
                clarification = toClarification(reasoned, reaction.reply());
            }
        }
        muteDeniedExplanations(explanation, context);

        // add interaction
        val graded = recordAndGrade(question, context, SEND_RESPONSE, submittedAnswerMapper.mapAll(answers), judgement,
                reasonings, clarification);
        question = graded.question();
        val strategyDecision = graded.decision();

        val locale = questionLanguage(context);
        // calculate error message
        val violations = graded.interaction().getViolations().stream()
                .map(v -> new AnswerFeedbackData.Knowledge(v.getKnowledgeName(), domain.needSupplementaryQuestion(v.getKnowledgeName(), v.getInteractionType())))
                .toList();
        Collection<Explanation> explanationSource = explanation.getRawMessage().isEmpty() ? explanation.getChildren() : List.of(explanation);
        val errors = explanationSource.stream().map(e -> Pair.of(
                violations.stream().filter(v -> e.getKnowledgeNames().contains(v.name())).toList(),
                e.toHyperText(locale).getText())).toList();
        // The last correct answer has no message of its own: the question being solved is the message.
        val messages = !errors.isEmpty() && !judgement.isAnswerCorrect() ? errors.stream().map(pair -> AnswerFeedbackData.Message.error(pair.getRight(), pair.getLeft())).toList()
                : judgement.stepsLeft() > 0 && judgement.isAnswerCorrect() ? List.of(AnswerFeedbackData.Message.success(localizationService.getMessage("exercise_correct-question-answer", locale), violations))
                : null;

        // return result of the last correct interaction
        val correctAnswers = question.latestCorrectResponses();

        // special case for order question
        // force complete answer if the last but one answer is correct
        val isAnswerCorrect = errors.isEmpty() && judgement.isAnswerCorrect();
        val orderQuestionOptions = Utils.tryCast(question.getContent().getOptions(), OrderQuestionOptionsData.class).orElse(null);
        if (isAnswerCorrect && question.getContent().getQuestionType().equals(QuestionType.ORDER) &&
                orderQuestionOptions != null && !orderQuestionOptions.isMultipleSelectionEnabled() &&
                judgement.stepsLeft() == 1 && question.getContent().getAnswerObjects().size() - correctAnswers.size() == 1) {
            val correctAnswersIds = correctAnswers.stream().map(r -> r.getAnswer().left().getAnswerId()).collect(Collectors.toSet());
            val missingAnswer = question.getContent().getAnswerObjects().stream()
                    .filter(ao -> !correctAnswersIds.contains(ao.getAnswerId()))
                    .<SubmittedAnswerData>map(ao -> new SubmittedAnswerData.Pair(ao.getAnswerId(), ao.getAnswerId(), null))
                    .findFirst().get();
            val completedAnswer = Stream.concat(
                    correctAnswers.stream().<SubmittedAnswerData>map(r -> new SubmittedAnswerData.Pair(
                            r.getAnswer().left().getAnswerId(), r.getAnswer().right().getAnswerId(), r.getCreatedByInteractionId())),
                    Stream.of(missingAnswer)).toList();
            return addQuestionAnswer(questionId, completedAnswer);
        }

        return feedbackDtoMapper.map(new AnswerFeedbackData(question, messages, correctAnswers, isAnswerCorrect,
                judgement.stepsLeft(), strategyDecision.grade(), strategyDecision.decision()), language);
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
    public @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(@NotNull Long questionId, @NotNull String[] violatedKnowledge) {
        val violation = new ViolationData(); //TODO: make normal choice
        violation.setKnowledgeName(violatedKnowledge[0]);

        var language = exerciseAttemptService.findUserLanguageForQuestion(questionId);
        return questionService.generateSupplementaryQuestion(questionId, violation, language);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull QuestionDto getQuestion(@NotNull Long questionId) {
        val question = questionService.getQuestion(questionId);
        return questionDtoMapper.map(question, userService.getCurrentUser().language());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull ClarificationFeedbackDto answerClarification(@NotNull ClarificationAnswerDto answer) {
        var question = questionService.getQuestion(answer.questionId());
        var interaction = question.findInteractionAwaitingClarification().orElseThrow(() -> new IllegalStateException(
                "Question " + answer.questionId() + " has no clarification awaiting an answer"));
        var chosen = answer.option() == null ? null : Objects.requireNonNull(interaction.getClarification()).content()
                .options().stream()
                .filter(option -> option.reasoning() == answer.option())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reasoning " + answer.option() + " is not among the clarification options"));
        questionService.answerClarification(interaction.getId(), chosen == null ? null : chosen.reasoning());
        return new ClarificationFeedbackDto(chosen == null ? null : chosen.explanation());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public @NotNull FeedbackDto generateNextCorrectAnswer(@NotNull Long questionId) {
        // get next correct answer
        var question = questionService.getSolvedQuestion(questionId);
        var context = exerciseAttemptService.findQuestionContext(questionId).orElse(null);
        var domain = domainFactory.getDomain(question.getContent().getDomainId());
        var currentUser = userService.getCurrentUser();
        var language = currentUser.language();
        val correctAnswer = domain.getAnyNextCorrectAnswer(question, language);
        muteDeniedExplanations(correctAnswer.explanation, context);

        // Подсказка достраивает уже данные студентом ответы, а не начинает решение
        // заново: ответы последнего верного взаимодействия переезжают в это.
        val alreadyGiven = question.latestCorrectResponses();
        val nextAnswers = correctAnswer.answers.stream()
                .map(ExerciseAttemptFrontendServiceImpl::toSubmittedAnswer)
                .toList();

        // evaluate new answer
        val responses = Stream.concat(
                alreadyGiven.stream().map(ResponseData::getAnswer),
                questionService.resolveAnswers(questionId, nextAnswers).stream()).toList();
        val judgement = domain.judgeAnswer(question, responses,
                domain.resolveTags(question.getContent().getTags()), language);

        // add interaction
        val reasonings = switch (judgement) {
            case Judgement.Verdict verdict -> List.of(toReasoningData(verdict));
            case Judgement.Reasoned reasoned -> toReasoningData(reasoned, reasoned.collectReasoningIds());
        };
        val graded = recordAndGrade(question, context, REQUEST_CORRECT_ANSWER,
                Stream.concat(
                        carriedAnswerMapper.mapAll(alreadyGiven).stream(),
                        submittedAnswerMapper.mapAll(nextAnswers).stream()).toList(),
                judgement, List.of(toSystemReasoning(judgement.isAnswerCorrect(), reasonings)), null);
        question = graded.question();
        val recorded = graded.interaction();
        val strategyDecision = graded.decision();

        // build feedback message
        val locale = questionLanguage(context);
        val messages = correctAnswer.explanation.getChildren().stream()
                .map(e -> AnswerFeedbackData.Message.success(e.toHyperText(locale).getText(),
                        e.getKnowledgeNames().stream().map(knowledge -> new AnswerFeedbackData.Knowledge(knowledge, false)).toList()))
                .toList();

        return feedbackDtoMapper.map(new AnswerFeedbackData(question, messages, recorded.getResponses(),
                /*true*/ recorded.isCorrect(),
                judgement.stepsLeft(), strategyDecision.grade(), strategyDecision.decision()), language);
    }

    /** Взаимодействие, записанное и оценённое стратегией, вместе с обновлённым вопросом. */
    private record GradedInteraction(@NotNull QuestionData question,
                                     @NotNull QuestionInteractionData interaction,
                                     @NotNull StrategyDecision decision) {
    }

    /**
     * Записать взаимодействие студента с вопросом, выставить за него оценку стратегии
     * и, если стратегия так решила, закрыть попытку.
     */
    private @NotNull GradedInteraction recordAndGrade(@NotNull QuestionData question,
                                                      @Nullable QuestionAttemptContextData context,
                                                      @NotNull InteractionType interactionType,
                                                      @NotNull List<NewInteractionAnswerData> answers,
                                                      @NotNull Judgement judgement,
                                                      @NotNull List<InteractionReasoningData> reasonings,
                                                      @Nullable HypothesisClarificationData clarification) {
        val recorded = questionService.recordInteraction(new NewInteractionData(
                question.getId(),
                interactionType,
                answers,
                judgement.isAnswerCorrect(),
                reasonings,
                withShuffledOptions(clarification),
                judgement.stepsLeft()));

        val decision = context == null
                ? new StrategyDecision(1f, Decision.CONTINUE)
                : strategyFactory.getStrategy(context.strategyId())
                        .gradeAndDecide(context.attemptId());
        questionService.gradeInteraction(recorded.getId(), decision.grade());
        if (context != null) {
            exerciseAttemptService.ensureAttemptStatus(context.attemptId(), decision.decision());
        }

        return new GradedInteraction(question.withInteraction(recorded), recorded, decision);
    }

    private @NotNull List<InteractionReasoningData> toReasoningData(@NotNull Judgement.Reasoned judgement,
                                                                   @NotNull Set<Integer> probable) {
        return judgement.reasonings().stream()
                .map(reasoning -> {
                    var data = new InteractionReasoningData();
                    reasoningMapper.apply(reasoning, data);
                    data.setProbable(probable.contains(reasoning.id()));
                    return data;
                })
                .toList();
    }

    // Вердикт — единственное, что известно об ответе, поэтому он допущен.
    private @NotNull InteractionReasoningData toReasoningData(@NotNull Judgement.Verdict verdict) {
        var data = new InteractionReasoningData();
        verdictMapper.apply(verdict, data);
        data.setProbable(true);
        return data;
    }

    // Вне попытки вопрос отлаживают по банку: допускаются все рассуждения, и о любом ответе спрашивают, если вариантов
    // несколько, — так видны все гипотезы.
    private static @NotNull AnswerReaction reactToDebuggedAnswer(@NotNull Judgement.Reasoned judgement) {
        var options = judgement.reasonings().stream()
                .filter(reasoning -> reasoning.reason() != null)
                .map(Reasoning::id)
                .toList();
        AnswerReaction.Reply reply = options.size() > 1 ? new AnswerReaction.Reply.Clarify(options)
                : !judgement.isAnswerCorrect() && judgement.reasonings().size() == 1
                ? new AnswerReaction.Reply.Explain(judgement.reasonings().getFirst().id())
                : new AnswerReaction.Reply.Acknowledge();
        return new AnswerReaction(judgement.collectReasoningIds(), reply);
    }

    private static @Nullable HypothesisClarificationData toClarification(@NotNull Judgement.Reasoned judgement,
                                                                         @NotNull AnswerReaction.Reply reply) {
        if (!(reply instanceof AnswerReaction.Reply.Clarify clarify)) {
            return null;
        }
        return new HypothesisClarificationData(judgement.inquiry().prompt(), clarify.options().stream()
                .map(judgement::getReasoning)
                .map(reasoning -> {
                    if (reasoning.reason() == null) {
                        throw new IllegalStateException("Reasoning " + reasoning.id() + " has no reason to offer");
                    }
                    return new HypothesisClarificationData.Option(reasoning.id(), reasoning.explanation().getRawMessage().getText());
                })
                .toList());
    }

    // Верный ответ объяснять не нужно, каким бы рассуждением студент к нему ни пришёл. Пока рассуждение неверного
    // ответа не известно, студент видит только вердикт.
    private static @NotNull Explanation explainAnswer(@NotNull Judgement.Reasoned judgement, @NotNull AnswerReaction.Reply reply) {
        if (judgement.isAnswerCorrect()) {
            if (reply instanceof AnswerReaction.Reply.Explain) {
                throw new IllegalStateException("A correct answer is not explained by a reasoning");
            }
            return Explanation.empty(Explanation.Type.ERROR);
        }
        return reply instanceof AnswerReaction.Reply.Explain explain
                ? judgement.getReasoning(explain.reasoning()).explanation()
                : judgement.inquiry().statement();
    }

    // Подсказку дала система: рассуждения, которыми к её ответу мог бы прийти студент, о нём ничего не говорят.
    private static @NotNull InteractionReasoningData toSystemReasoning(boolean isAnswerCorrect,
                                                                       @NotNull List<InteractionReasoningData> reasonings) {
        var counted = new CountedKnowledgeData(isAnswerCorrect, reasonings, null);
        return new InteractionReasoningData(0, List.of(), true, isAnswerCorrect, null, counted.getViolations(),
                counted.getAppliedKnowledge());
    }

    // Порядок вариантов влияет на выбор студента, поэтому он случайный; сохраняется показанный порядок.
    private @Nullable HypothesisClarificationData withShuffledOptions(@Nullable HypothesisClarificationData clarification) {
        if (clarification == null) {
            return null;
        }
        var options = new ArrayList<>(clarification.options());
        Collections.shuffle(options, randomProvider.getRandom());
        return new HypothesisClarificationData(clarification.prompt(), options);
    }

    private static void muteDeniedExplanations(@Nullable Explanation explanation, @Nullable QuestionAttemptContextData context) {
        if (explanation == null || context == null) {
            return;
        }
        var deniedSkills = context.questionStage().getSkills().stream()
                .filter(skill -> RoleInExercise.FORBIDDEN.equals(skill.getKind()))
                .map(ExerciseSkillDto::getName)
                .toList();
        explanation.muteDeniedSkills(deniedSkills);
    }

    private static @NotNull Language questionLanguage(@Nullable QuestionAttemptContextData context) {
        return context == null ? Language.RUSSIAN/*ENGLISH*/ : context.userLanguage();
    }

    private static @NotNull List<SubmittedAnswerData> toSubmittedAnswers(@NotNull QuestionType questionType,
                                                                      @Nullable AnswerDto[] answers) {
        return answers == null ? List.of() : Arrays.stream(answers)
                .map(answer -> toSubmittedAnswer(questionType, answer))
                .toList();
    }

    private static @NotNull SubmittedAnswerData toSubmittedAnswer(@NotNull AnswerData answer) {
        return switch (answer) {
            case AnswerData.Pair pair -> new SubmittedAnswerData.Pair(pair.left().getAnswerId(), pair.right().getAnswerId(), null);
            case AnswerData.Choice choice -> new SubmittedAnswerData.Choice(choice.left().getAnswerId(), choice.value(), null);
        };
    }

    private static @NotNull SubmittedAnswerData toSubmittedAnswer(@NotNull QuestionType questionType,
                                                                  @NotNull AnswerDto answer) {
        var left = answer.getAnswer()[0].intValue();
        var right = answer.getAnswer()[1].intValue();
        return questionType == QuestionType.MULTI_CHOICE
                ? new SubmittedAnswerData.Choice(left, right, answer.getCreatedByInteraction())
                : new SubmittedAnswerData.Pair(left, right, answer.getCreatedByInteraction());
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
