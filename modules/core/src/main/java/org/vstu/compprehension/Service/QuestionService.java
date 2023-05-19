package org.vstu.compprehension.Service;

import its.questions.gen.states.*;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.dto.AnswerDto;
import org.vstu.compprehension.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.dto.feedback.FeedbackDto;
import org.vstu.compprehension.dto.feedback.FeedbackViolationLawDto;
import org.vstu.compprehension.dto.question.QuestionDto;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.BackendFactory;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.businesslogic.domains.DecisionTreeBasedDomain;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.QuestionOptions.MatchingQuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.SingleChoiceOptionsEntity;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.utils.HyperText;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.utils.Mapper;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerObjectRepository answerObjectRepository;
    private final AbstractStrategyFactory strategyFactory;
    private final BackendFactory backendFactory;
    private final DomainService domainService;
    private final InteractionRepository interactionRepository;
    private final ResponseRepository responseRepository;
    private final SupplementaryStepRepository supplementaryStepRepository;
    private final DomainFactory domainFactory;
    private final QuestionRequestLogRepository questionRequestLogRepository;
    private final QuestionMetadataRepository questionMetadataRepository;

    @Autowired
    public QuestionService(QuestionRepository questionRepository, AnswerObjectRepository answerObjectRepository, AbstractStrategyFactory strategyFactory, BackendFactory backendFactory, DomainService domainService, InteractionRepository interactionRepository, ResponseRepository responseRepository, SupplementaryStepRepository supplementaryStepRepository, DomainFactory domainFactory, QuestionRequestLogRepository questionRequestLogRepository, QuestionMetadataRepository questionMetadataRepository) {
        this.questionRepository = questionRepository;
        this.answerObjectRepository = answerObjectRepository;
        this.strategyFactory = strategyFactory;
        this.backendFactory = backendFactory;
        this.domainService = domainService;
        this.interactionRepository = interactionRepository;
        this.responseRepository = responseRepository;
        this.supplementaryStepRepository = supplementaryStepRepository;
        this.domainFactory = domainFactory;
        this.questionRequestLogRepository = questionRequestLogRepository;
        this.questionMetadataRepository = questionMetadataRepository;
    }


    public Question generateQuestion(ExerciseAttemptEntity exerciseAttempt) {
        Domain domain = domainFactory.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        AbstractStrategy strategy = strategyFactory.getStrategy(exerciseAttempt.getExercise().getStrategyId());
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        saveQuestionRequest(qr);
        Question question = domain.makeQuestion(qr, exerciseAttempt.getExercise().getTags(), exerciseAttempt.getUser().getPreferred_language());
        question.getQuestionData().setDomainEntity(domainService.getDomainEntity(domain.getName()));
        saveQuestion(question.getQuestionData());
        return question;
    }

    private void saveQuestionRequest(QuestionRequest qr) {
        // fill empty lists
        if (qr.getDeniedQuestionMetaIds().isEmpty())
            qr.getDeniedQuestionMetaIds().add(0);
        if (qr.getDeniedQuestionTemplateIds().isEmpty())
            qr.getDeniedQuestionTemplateIds().add(0);

        val qrl = qr.getLogEntity();

        Map<String, Object> res = questionMetadataRepository.countQuestions(qr);
        int questionsFound = ((BigInteger)res.getOrDefault("number", -2)).intValue();
        qrl.setFoundCount(questionsFound);
        qrl.setCreatedDate(new Date());
        questionRequestLogRepository.save(qrl);
    }


    public @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(@NotNull QuestionEntity sourceQuestion, @NotNull ViolationEntity violation, Language lang) {
        val domain = domainFactory.getDomain(sourceQuestion.getExerciseAttempt().getExercise().getDomain().getName());
        Question question = null;
        if(!(domain instanceof DecisionTreeBasedDomain)){
            question = domain.makeSupplementaryQuestion(sourceQuestion, violation, lang);
            if (question != null) {
                question.getQuestionData().setDomainEntity(domainService.getDomainEntity(domain.getName()));
                saveQuestion(question.getQuestionData());
            }
            return questionAsSupplementaryQuestionDto(question);
        }
        else {
            val out = ((DecisionTreeBasedDomain) domain).makeSupplementaryQuestionDT(sourceQuestion, lang);
            SupplementaryStepEntity s = out.getSecond();
            if(out.getFirst() instanceof its.questions.gen.states.Question){
                question = transformQuestionFormats((its.questions.gen.states.Question) out.getFirst(), domain, sourceQuestion.getExerciseAttempt());
                question.getQuestionData().setDomainEntity(domainService.getDomainEntity(domain.getName()));
                saveQuestion(question.getQuestionData());
                s.setSupplementaryQuestion(question.getQuestionData());
            }
            supplementaryStepRepository.save(out.getSecond());
            if(question != null)
                return questionAsSupplementaryQuestionDto(question);
            else
                return SupplementaryQuestionDto.FromMessage(stateChangeAsSupplementaryFeedbackDto((QuestionStateChange) out.getFirst()));
        }
    }

    public SupplementaryFeedbackDto judgeSupplementaryQuestion(Question question, List<ResponseEntity> responses, ExerciseAttemptEntity exerciseAttempt, LocalizationService localizationService) {
        Domain domain = domainFactory.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        if(!(domain instanceof DecisionTreeBasedDomain)){
            assert responses.size() == 1;
            val judgeResult = domain.judgeSupplementaryQuestion(question, responses.get(0).getLeftAnswerObject());
            val violation = judgeResult.violations.stream()
                    .map(v -> FeedbackViolationLawDto.builder().name(v.getLawName()).canCreateSupplementaryQuestion(domain.needSupplementaryQuestion(v)).build())
                    .findFirst()
                    .orElse(null);
            val locale = exerciseAttempt.getUser().getPreferred_language().toLocale();
            val message = judgeResult.isAnswerCorrect
                    ? FeedbackDto.Message.Success(localizationService.getMessage("exercise_correct-sup-question-answer", locale), violation)
                    : FeedbackDto.Message.Error(localizationService.getMessage("exercise_wrong-sup-question-answer", locale), violation);
            return new SupplementaryFeedbackDto(
                    message,
                    judgeResult.isAnswerCorrect ? SupplementaryFeedbackDto.Action.ContinueAuto : SupplementaryFeedbackDto.Action.ContinueManual);
        }
        else {
            val supplementaryInfo = supplementaryStepRepository.findBySupplementaryQuestion(question.getQuestionData());
            val out = ((DecisionTreeBasedDomain) domain).judgeSupplementaryQuestionDT(supplementaryInfo, responses);
            supplementaryStepRepository.save(out.getSecond());
            return stateChangeAsSupplementaryFeedbackDto(out.getFirst());
        }
    }

    public static final int aggregationPadding = 5; //FIXME используется потому, что из вопросов-сопоставлений можно отправить неполный ответ
    public static final int aggregationShift = 2;
    public static final Map<String, Integer> aggregationMatching = Map.of("Верно", 1, "Неверно", -1, "Не имеет значения", 0);
    public static Question transformQuestionFormats(its.questions.gen.states.Question q, Domain domain, ExerciseAttemptEntity exerciseAttempt){
        QuestionEntity generated = new QuestionEntity();
        generated.setQuestionText(q.getText());
        //generated.setQuestionName(String.valueOf(creatorStateId));    //FIXME?
        generated.setQuestionDomainType(domain.getDefaultQuestionType(true));
        generated.setExerciseAttempt(exerciseAttempt);
        generated.setAnswerObjects(q.getOptions().stream().map((opt) -> {
            AnswerObjectEntity ans = new AnswerObjectEntity();
            ans.setAnswerId(opt.getSecond());
            ans.setHyperText(opt.getFirst());
            return ans;
        }).collect(Collectors.toList()));
        if(q.isAggregation() ){
            generated.setQuestionType(QuestionType.MATCHING);
            List<AnswerObjectEntity> answers = generated.getAnswerObjects();
            for(AnswerObjectEntity a : answers){
                a.setAnswerId(a.getAnswerId() + aggregationShift); //чтобы избежать пересечения с answerId ответов в aggregationMathching
            }
            for(Map.Entry<String, Integer> m : aggregationMatching.entrySet()){
                AnswerObjectEntity ans = new AnswerObjectEntity();
                ans.setAnswerId(m.getValue());
                ans.setHyperText(m.getKey());
                ans.setRightCol(true);
                answers.add(ans);
            }
            generated.setAnswerObjects(answers);
            val opt = new MatchingQuestionOptionsEntity();
            opt.setShowSupplementaryQuestions(true);
            opt.setDisplayMode(MatchingQuestionOptionsEntity.DisplayMode.COMBOBOX);
            generated.setOptions(opt);
            return new Matching(generated, domain);
        }
        else {
            generated.setQuestionType(QuestionType.SINGLE_CHOICE);
            val opt = new SingleChoiceOptionsEntity();
            opt.setShowSupplementaryQuestions(true);
            opt.setDisplayMode(SingleChoiceOptionsEntity.DisplayMode.RADIO);
            generated.setOptions(opt);
            return new SingleChoice(generated, domain);
        }
    }
    public static SupplementaryQuestionDto questionAsSupplementaryQuestionDto(Question supQuestion){
        QuestionDto questionDto = supQuestion != null ? Mapper.toDto(supQuestion) : null;
        return questionDto != null && questionDto.getAnswers().length > 0 ? SupplementaryQuestionDto.FromQuestion(Mapper.toDto(supQuestion))
                : questionDto != null ? SupplementaryQuestionDto.FromMessage(new SupplementaryFeedbackDto(FeedbackDto.Message.Success(questionDto.getText().replaceAll("<[^>]*>", "")), SupplementaryFeedbackDto.Action.Finish))
                : SupplementaryQuestionDto.Empty();
    }
    public static SupplementaryFeedbackDto stateChangeAsSupplementaryFeedbackDto(QuestionStateChange change){
        Explanation expl = change.getExplanation();
        if(expl != null)
            return new SupplementaryFeedbackDto(
                    new FeedbackDto.Message(expl.getType() == ExplanationType.Error ? FeedbackDto.MessageType.ERROR : FeedbackDto.MessageType.SUCCESS, expl.getText(), new FeedbackViolationLawDto("", true)),
                    change.getNextState() == null || change.getNextState() instanceof EndQuestionState
                            ? SupplementaryFeedbackDto.Action.Finish
                            : expl.getShouldPause() ? SupplementaryFeedbackDto.Action.ContinueManual : SupplementaryFeedbackDto.Action.ContinueAuto
            );
        else
            return new SupplementaryFeedbackDto(
                    FeedbackDto.Message.Success("...", new FeedbackViolationLawDto("", true)),
                    change.getNextState() == null || change.getNextState() instanceof EndQuestionState
                            ? SupplementaryFeedbackDto.Action.Finish
                            : SupplementaryFeedbackDto.Action.ContinueAuto);
    }
    public static SupplementaryQuestionDto stateResultAsSupplementaryQuestionDto(QuestionStateResult q, Domain domain, ExerciseAttemptEntity exerciseAttempt){
        if(q instanceof its.questions.gen.states.Question){
            Question supQuestion = transformQuestionFormats((its.questions.gen.states.Question) q, domain, exerciseAttempt);
            return questionAsSupplementaryQuestionDto(supQuestion);
        }
        else {
            QuestionStateChange change = ((QuestionStateChange) q);
            return SupplementaryQuestionDto.FromMessage(stateChangeAsSupplementaryFeedbackDto(change));
        }
    }

    public Question solveQuestion(Question question, List<Tag> tags) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        Backend backend = backendFactory.getBackend(question.getQuestionData().getExerciseAttempt().getExercise().getBackendId());

        // use reasoner to solve question
        Collection<Fact> solution = backend.solve(
                /*new ArrayList<>*/(domain.getQuestionLaws(question.getQuestionDomainType(), tags)),
                question.getStatementFactsWithSchema(),
                new ReasoningOptions(
                        false,
                        domain.getSolutionVerbs(question.getQuestionDomainType(), question.getStatementFacts()),
                        question.getQuestionUniqueTemplateName()
                ));

        List<BackendFactEntity> storedSolution = question.getQuestionData().getSolutionFacts();
        if (storedSolution != null && !storedSolution.isEmpty()) {
            // add anything set as solution before
            solution.addAll(Fact.entitiesToFacts(storedSolution));
        }
        // save facts to question
        question.getQuestionData().setSolutionFacts(Fact.factsToEntities(solution));

        // don't save solution into DB
        // // saveQuestion(question.getQuestionData());

        return question;
    }

    /*
    public List<ResponseEntity> responseQuestion(Question question, List<Integer> responses) {
        val result = new ArrayList<ResponseEntity>();
        for (val answerId : responses) {
            result.add(makeResponse(question.getAnswerObject(answerId)));
        }
        return result;
    }
    */

    public List<ResponseEntity> responseQuestion(Question question, AnswerDto[] answers) {
        val result = new ArrayList<ResponseEntity>();
        for (val answer: answers) {
            val left = question.getAnswerObject(answer.getAnswer()[0].intValue());
            val right = question.getAnswerObject(answer.getAnswer()[1].intValue());
            val createdByInteraction = Optional.ofNullable(answer.getCreatedByInteraction())
                    .flatMap(id -> interactionRepository.findById(id))
                    .orElse(null);
            val response = makeResponse(left, right, createdByInteraction);
            result.add(response);
        }
        return result;
    }

    public Domain.InterpretSentenceResult judgeQuestion(Question question, List<ResponseEntity> responses, List<Tag> tags) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        Collection<Fact> responseFacts = question.responseToFacts(responses);
        Backend backend = backendFactory.getBackend(question.getQuestionData().getExerciseAttempt().getExercise().getBackendId());
        Collection<Fact> violations = backend.judge(
                new ArrayList<>(domain.getQuestionNegativeLaws(question.getQuestionDomainType(), tags)),
                question.getStatementFactsWithSchema(),
                Fact.entitiesToFacts(question.getSolutionFacts()),
                responseFacts,
                new ReasoningOptions(
                        false,
                        domain.getViolationVerbs(question.getQuestionDomainType(), question.getStatementFacts()),
                        question.getQuestionUniqueTemplateName())
        );
        return domain.interpretSentence(violations);
    }

    public List<HyperText> explainViolations(Question question, List<ViolationEntity> violations) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        Language lang;
        try {
            lang = question.getQuestionData().getExerciseAttempt().getUser().getPreferred_language(); // The language currently selected in UI
        } catch (NullPointerException e) {
            lang = Language.ENGLISH;  // fallback if it cannot be figured out
        }
        return domain.makeExplanation(violations, FeedbackType.EXPLANATION, lang);
    }

    public Question getQuestion(Long questionId) {
        var rawQuestion = questionRepository.findByIdEager(questionId).orElseThrow();
        Question question = generateBusinessLogicQuestion(rawQuestion);
        return question;
    }

    public Question getSolvedQuestion(Long questionId) {
        val question = getQuestion(questionId);
        return solveQuestion(question, question.getQuestionData().getExerciseAttempt().getExercise().getTags());
    }

    public QuestionEntity getQuestionEntity(Long questionId) {
        return questionRepository.findById(questionId).get();
    }

    public void saveQuestion(QuestionEntity question) {
        if (question.getAnswerObjects() != null) {
            for (AnswerObjectEntity answerObject : question.getAnswerObjects()) {
                if (answerObject.getId() == null) {
                    answerObject.setQuestion(question);
                }
            }
            answerObjectRepository.saveAll(question.getAnswerObjects().stream().filter(a -> a.getId() == null)::iterator);
        }

        if (question.getInteractions() != null) {
            for (val interactionEntity : question.getInteractions()) {
                if (interactionEntity.getQuestion() == null) {
                    interactionEntity.setQuestion(question);
                }
            }
        }
        questionRepository.save(question);
    }

    public Domain.CorrectAnswer getNextCorrectAnswer(Question question) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        return domain.getAnyNextCorrectAnswer(question);
    }

    /*
    public Question generateBusinessLogicQuestion(ExerciseAttemptEntity exerciseAttempt) {
        
        //Генерируем вопрос
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        Language userLanguage = exerciseAttempt.getUser().getPreferred_language();
        Domain domain = core.getDomain(
                exerciseAttempt.getExercise().getDomain().getName());
        Question newQuestion =
                domain.makeQuestion(qr, exerciseAttempt.getExercise().getTags(), userLanguage);
        
        saveQuestion(newQuestion.getQuestionData());
        
        return newQuestion;
    }
    */

    public Question generateBusinessLogicQuestion(
            QuestionEntity question) {

        Domain domain = domainFactory.getDomain(question.getExerciseAttempt().getExercise().getDomain().getName());
        if (question.getQuestionType() == QuestionType.MATCHING) {
            return new Matching(question, domain);
        } else if (question.getQuestionType() == QuestionType.ORDER) {
            return new Ordering(question, domain);
        } else if (question.getQuestionType() == QuestionType.MULTI_CHOICE) {
            return new MultiChoice(question, domain);
        } else {
            return new SingleChoice(question, domain);
        }
    }

    private ResponseEntity makeResponse(AnswerObjectEntity answer) {
        ResponseEntity response = new ResponseEntity();
        response.setLeftAnswerObject(answer);
        response.setRightAnswerObject(answer);
        responseRepository.save(response);
        return response;
    }

    private ResponseEntity makeResponse(AnswerObjectEntity answerL, AnswerObjectEntity answerR, InteractionEntity createdByInteraction) {
        ResponseEntity response = new ResponseEntity();
        response.setLeftAnswerObject(answerL);
        response.setRightAnswerObject(answerR);
        response.setCreatedByInteraction(createdByInteraction);
        responseRepository.save(response);
        return response;
    }
}

