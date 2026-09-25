package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.GeneratedQuestionData;
import org.vstu.compprehension.data.question.NewSupplementaryStepData;
import org.vstu.compprehension.data.question.QuestionContentData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.businesslogic.SupplementaryResponse;
import org.vstu.compprehension.businesslogic.SupplementaryStepContext;
import org.vstu.compprehension.data.question.SupplementaryStepData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.data.question.NewInteractionData;
import org.vstu.compprehension.data.question.QuestionRequestLogData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.enums.QuestionType;
import org.vstu.compprehension.repositories.data.InteractionDataRepository;
import org.vstu.compprehension.repositories.data.QuestionDataRepository;
import org.vstu.compprehension.repositories.data.SupplementaryStepDataRepository;
import org.vstu.compprehension.frontend.mappers.SupplementaryQuestionDtoMapper;
import org.vstu.compprehension.mappers.Mapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
class QuestionDataServiceImpl implements QuestionDataService {
    private final AbstractStrategyFactory strategyFactory;
    private final SupplementaryStepDataRepository supplementaryStepDataRepository;
    private final QuestionDataRepository questionDataRepository;
    private final InteractionDataRepository interactionDataRepository;
    private final ExerciseAttemptDataService exerciseAttemptService;
    private final DomainFactory domainFactory;
    private final QuestionBank questionStorage;
    private final Mapper<QuestionRequest, QuestionRequestLogData> questionRequestLogMapper;
    private final SupplementaryQuestionDtoMapper supplementaryQuestionDtoMapper;


    public QuestionData generateQuestion(long exerciseAttemptId) {
        var context = exerciseAttemptService.getGenerationContext(exerciseAttemptId);
        Domain domain = domainFactory.getDomain(context.domainId());
        AbstractStrategy strategy = strategyFactory.getStrategy(context.strategyId());

        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttemptId);
        qr = domain.ensureQuestionRequestValid(qr);

        GeneratedQuestionData generated = domain.makeQuestion(qr, context.exerciseOptions(), context.userLanguage());

        return saveQuestion(QuestionData.of(generated.getContent()), questionRequestLogMapper.map(qr), exerciseAttemptId);
    }

    public QuestionData generateQuestion(int questionMetadataId, Language lang) {
        var rawQuestion = questionStorage.loadQuestion(questionMetadataId);
        if (rawQuestion == null) {
            throw new RuntimeException("Metadata with id " + questionMetadataId + " not found");
        }
        var domain = domainFactory.getDomain(rawQuestion.getDomainShortname());
        var tags = domain.getAllTags().stream()
                .filter(t -> rawQuestion.getTagBits() != null && (rawQuestion.getTagBits() & t.getBitmask()) != 0)
                .toList();
        var generated = domain.makeQuestion(rawQuestion, tags, lang);
        return saveQuestion(QuestionData.of(generated.getContent()), null, null);
    }

    public @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(long sourceQuestionId, @NotNull ViolationData violation, Language lang) {
        val sourceQuestion = questionDataRepository.findById(sourceQuestionId);
        val domain = domainFactory.getDomain(sourceQuestion.getContent().getDomainId());
        val interactions = sourceQuestion.getInteractions();
        val latestStep = interactions.isEmpty() ? null
                : supplementaryStepDataRepository.findLatestStepOfInteraction(interactions.getLast().getId());
        val responseGen = domain.makeSupplementaryQuestion(sourceQuestion, latestStep, violation, lang);

        return switch (responseGen.getResponse()) {
            case SupplementaryResponse.Question(var question) -> {
                val supplementary = saveQuestion(
                        QuestionData.of(question.getContent()),
                        null,
                        exerciseAttemptService.findAttemptIdOfQuestion(sourceQuestionId).orElse(null));
                createStep(responseGen.getNewStep(), supplementary.getId());
                yield supplementaryQuestionDtoMapper.map(supplementary, lang);
            }
            case SupplementaryResponse.Feedback(var feedback) -> {
                createStep(responseGen.getNewStep(), null);
                yield new SupplementaryQuestionDto.Feedback(feedback);
            }
        };
    }

    public SupplementaryFeedbackDto judgeSupplementaryQuestion(long supplementaryQuestionId, List<? extends AnswerData> responses, Language language) {
        val step = supplementaryStepDataRepository.findBySupplementaryQuestionId(supplementaryQuestionId);
        if (step == null) {
            throw new IllegalArgumentException(
                    "Question with id " + supplementaryQuestionId + " isn't supplementary");
        }

        // Наводящий вопрос судится по фактам основного, поэтому сам он не поднимается.
        val mainQuestion = questionDataRepository.findById(step.getMainQuestionId());
        val stepContext = new SupplementaryStepContext(step, findInteraction(mainQuestion, step));

        Domain domain = domainFactory.getDomain(mainQuestion.getContent().getDomainId());
        val feedbackGen = domain.judgeSupplementaryQuestion(mainQuestion, stepContext, responses, language);
        createStep(feedbackGen.getNewStep(), null);
        return feedbackGen.getFeedback();
    }

    private void createStep(@Nullable NewSupplementaryStepData step, @Nullable Long supplementaryQuestionId) {
        if (step != null) {
            supplementaryStepDataRepository.create(step, supplementaryQuestionId);
        }
    }

    private static @NotNull QuestionInteractionData findInteraction(@NotNull QuestionData question,
                                                                    @NotNull SupplementaryStepData step) {
        return question.getInteractions().stream()
                .filter(i -> Objects.equals(i.getId(), step.getMainQuestionInteractionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Interaction " + step.getMainQuestionInteractionId()
                        + " is not among interactions of question " + question.getId()));
    }

    public QuestionType getQuestionType(long questionId) {
        return questionDataRepository.getQuestionType(questionId);
    }

    public List<AnswerData> resolveAnswers(long questionId, List<SubmittedAnswerData> answers) {
        return interactionDataRepository.resolveAnswers(questionId, answers);
    }

    /** Записать взаимодействие студента с вопросом. */
    public QuestionInteractionData recordInteraction(NewInteractionData interaction) {
        return interactionDataRepository.record(interaction);
    }

    /** Выставить оценку за уже записанное взаимодействие. */
    public void gradeInteraction(long interactionId, float grade) {
        interactionDataRepository.grade(interactionId, grade);
    }

    public QuestionData getQuestion(Long questionId) {
        return questionDataRepository.findById(questionId);
    }

    /** Имя домена вопроса — скалярным запросом, без подъёма всего вопроса. */
    private @NotNull String getDomainId(long questionId) {
        return questionDataRepository.getDomainId(questionId);
    }

    public QuestionData getSolvedQuestion(Long questionId) {
        val question = getQuestion(questionId);
        val content = question.getContent();
        var domain = domainFactory.getDomain(content.getDomainId());
        if (!domain.requiresSolving()) {
            return question;
        }

        QuestionContentData solved = domain.solveQuestion(content, domain.resolveTags(content.getTags()));

        // Решение дописывает в вопрос факты, и их нужно сохранить явно
        return saveQuestion(question.withContent(solved), null, null);
    }

    /**
     * Id пользователя, которому принадлежит попытка, породившая вопрос.
     */
    public Optional<Long> findQuestionOwnerUserId(Long questionId) {
        return questionDataRepository.findOwnerUserId(questionId);
    }

    public @NotNull QuestionData saveQuestion(@NotNull QuestionData question,
                                              @Nullable QuestionRequestLogData questionRequestLog,
                                              @Nullable Long exerciseAttemptId) {
        return questionDataRepository.save(question, questionRequestLog, exerciseAttemptId);
    }
}
