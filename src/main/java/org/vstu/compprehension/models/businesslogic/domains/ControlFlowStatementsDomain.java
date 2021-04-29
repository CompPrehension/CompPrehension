package org.vstu.compprehension.models.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.HyperText;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ControlFlowStatementsDomain extends Domain {
    static final String EXECUTION_ORDER_QUESTION_TYPE = "OrderActs";
    static final String EXECUTION_ORDER_SUPPLEMENTARY_QUESTION_TYPE = "OrderActsSupplementary";
    static final String DEFINE_TYPE_QUESTION_TYPE = "DefineType";
//    static final String LAWS_CONFIG_PATH = "file:c:/D/Work/YDev/CompPr/c_owl/jena/domain_laws.json";
    static final String LAWS_CONFIG_PATH = "org/vstu/compprehension/models/businesslogic/domains/control-flow-statements-domain-laws.json";

    static final String VOCAB_SCHEMA_PATH = "c:\\D\\Work\\YDev\\CompPr\\world_onto\\domain_schema.ttl";
    private static DomainVocabulary vocab = null;

    static List<Question> QUESTIONS;

    public ControlFlowStatementsDomain() {
        super();
        name = "ControlFlowStatementsDomain";
        fillConcepts();
        readLaws(this.getClass().getClassLoader().getResourceAsStream(LAWS_CONFIG_PATH));
    }

    private static void initVocab() {
        if (vocab == null) {
            vocab = new DomainVocabulary(VOCAB_SCHEMA_PATH);
        }
    }

    private void fillConcepts() {
        concepts = new ArrayList<>();
        initVocab();
        concepts.addAll(vocab.readConcepts());
    }

    private void readLaws(InputStream inputStream) {
        Objects.requireNonNull(inputStream);
        positiveLaws = new ArrayList<>();
        negativeLaws = new ArrayList<>();

        RuntimeTypeAdapterFactory<Law> runtimeTypeAdapterFactory =
                RuntimeTypeAdapterFactory
                        .of(Law.class, "positive")
                        .registerSubtype(PositiveLaw.class, "true")
                        .registerSubtype(NegativeLaw.class, "false");
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

        Law[] lawForms = gson.fromJson(
                new InputStreamReader(inputStream),
                Law[].class);

        for (Law lawForm : lawForms) {
            if (lawForm.isPositiveLaw()) {
                positiveLaws.add((PositiveLaw) lawForm);
            } else {
                negativeLaws.add((NegativeLaw) lawForm);
            }
        }
    }


    @Override
    public void update() {
    }

    @Override
    public List<HyperText> getFullSolutionTrace(Question question) {
        return null;
    }

    @Override
    public ExerciseForm getExerciseForm() {
        return null;
    }

    @Override
    public ExerciseEntity processExerciseForm(ExerciseForm ef) {
        return null;
    }

    @Override
    public Question makeQuestion(QuestionRequest questionRequest, List<Tag> tags, Language userLanguage) {
        return null;
    }

    /**
     * Generate explanation of violations
     *
     * @param violations   list of student violations
     * @param feedbackType TODO: use feedbackType or delete it
     * @return explanation for each violation in random order
     */
    @Override
    public ArrayList<HyperText> makeExplanation(List<ViolationEntity> violations, FeedbackType feedbackType) {
        return null;
    }

    @Override
    public List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags) {
        return null;
    }

    @Override
    public List<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags) {
        return null;
    }

    @Override
    public List<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return null;
    }

    @Override
    public List<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts) {
        return null;
    }

    @Override
    public List<BackendFactEntity> responseToFacts(String questionDomainType, List<ResponseEntity> responses,
                                                   List<AnswerObjectEntity> answerObjects) {
        return null;
    }

    @Override
    public InterpretSentenceResult interpretSentence(List<BackendFactEntity> violations) {
        return null;
    }

    @Override
    public Question makeSupplementaryQuestion(InterpretSentenceResult interpretSentenceResult,
                                              ExerciseAttemptEntity exerciseAttemptEntity) {
        return null;
    }

    @Override
    public ProcessSolutionResult processSolution(List<BackendFactEntity> solution) {
        return null;
    }

    @Override
    public CorrectAnswer getAnyNextCorrectAnswer(Question q) {
        return null;
    }

    @Override
    protected List<Question> getQuestionTemplates() {
        return null;
    }


    public static void main(String[] args) {
        new ControlFlowStatementsDomain();
    }
}
