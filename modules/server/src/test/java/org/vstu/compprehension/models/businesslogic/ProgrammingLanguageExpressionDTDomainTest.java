package org.vstu.compprehension.models.businesslogic;

import jakarta.transaction.Transactional;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeDefaultExpressionConfig;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeOrderQuestionBuilder;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeRDFHelper;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.nodes.Node;
import org.vstu.meaningtree.serializers.rdf.RDFDeserializer;
import org.vstu.meaningtree.utils.NodeLabel;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Transactional
public class ProgrammingLanguageExpressionDTDomainTest {
    @Autowired
    DomainFactory domainFactory;
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestionService questionService;

    private ExerciseAttemptEntity attempt;
    private ExerciseEntity exercise;
    private ProgrammingLanguageExpressionDTDomain domain;

    public static final String domainId = "ProgrammingLanguageExpressionDTDomain";

    @BeforeAll
    public void init() {
        domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
        exercise = StreamSupport.stream(
                exerciseRepository.findAll().spliterator(), false
        ).filter((ExerciseEntity exercise) -> exercise.getDomain().getShortName().equals("expression_dt")).toList().getFirst();
        exercise.setStrategyId("StaticStrategy");
        exercise.getStages().getFirst();
        exerciseRepository.save(exercise);
    }

    public boolean generateAndSolve(String expression, SupportedLanguage inLang, SupportedLanguage outLang, List<Integer> sequence) {
        List<Question> questions = MeaningTreeOrderQuestionBuilder.newQuestion(expression, inLang, domain).buildQuestion(outLang);
        String outLangStr = outLang.toString().substring(0, 1).toUpperCase() + outLang.toString().substring(1);

        boolean allPassed = true;
        for (Question q : questions) {
            // Check metadata
            Assert.isTrue(q.getMetadata() != null
                    && q.getMetadata().getIntegralComplexity() >= 0
                    && q.getMetadata().getIntegralComplexity() <= 1, String.format(
                            "Invalid integral complexity %f, possibleErrors=%d, solutionLength=%d",
                    q.getMetadata().getIntegralComplexity(),
                    q.getMetadata().getDistinctErrorsCount(),
                    q.getMetadata().getSolutionSteps()));

            List<ResponseEntity> responses = new ArrayList<>();
            for (Integer response : sequence) {
                AnswerObjectEntity answerObject = AnswerObjectEntity
                        .builder().answerId(response)
                        .domainInfo("token_" + response).build();
                responses.add(ResponseEntity.builder().leftAnswerObject(answerObject).rightAnswerObject(answerObject).build());
                var result = questionService.judgeQuestion(q, responses, List.of(domain.getTag(outLangStr)));
                allPassed = allPassed && result.isAnswerCorrect;
                if (!result.isAnswerCorrect) {
                    Assertions.fail(String.format("%s: %s", responses.stream()
                            .map(ResponseEntity::getLeftAnswerObject)
                            .map(AnswerObjectEntity::getDomainInfo).toList(), result.explanations
                            .stream().map(HyperText::getText)
                            .collect(Collectors.joining("\n"))));
                }
                if (result.IterationsLeft == 0) {
                    break;
                }
            }
        }
        return allPassed;
    }

    @Test
    public void testConversionQuestionToPython() {
        testConversion(SupportedLanguage.PYTHON, List.of("operator_subscript", "operator_function_call"));
        testConversion(SupportedLanguage.PYTHON, List.of("operator_.", "operator_?"));
    }

    @Test
    public void testConversionQuestionToCpp() {
        testConversion(SupportedLanguage.CPP, List.of("operator_subscript", "operator_function_call"));
        testConversion(SupportedLanguage.CPP, List.of("operator_.", "operator_?"));
    }

    @Test
    public void testConversionQuestionToJava() {
        testConversion(SupportedLanguage.JAVA, List.of("operator_subscript", "operator_function_call"));
        testConversion(SupportedLanguage.JAVA, List.of("operator_.", "operator_?"));
    }

    public void testConversion(SupportedLanguage language, List<String> concepts) {
        ProgrammingLanguageExpressionDTDomain domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
        String langStr = language.toString().substring(0, 1).toUpperCase() + language.toString().substring(1);

        List<String> tagsString = List.of(langStr);
        List<Tag> tags = tagsString.stream().map(domain::getTag).toList();

        exercise.setTags(langStr);
        exerciseRepository.save(exercise);

        ExerciseAttemptEntity attempt = new ExerciseAttemptEntity();
        attempt.setQuestions(List.of());
        attempt.setExercise(exercise);
        attempt.setUser(userRepository.findAll().iterator().next());
        exerciseAttemptRepository.save(attempt);

        QuestionRequest r = QuestionRequest.builder()
                .domainShortname("expression")
                .targetConcepts(concepts.stream().map(domain::getConcept).toList())
                .stepsMin(2)
                .targetTags(Stream.of("C++").map(domain::getTag).toList())
                .targetLaws(List.of())
                .deniedLaws(List.of())
                .stepsMax(10)
                .complexity(0.8f)
                .build();
        Question q = domain.makeQuestion(attempt, r, tags, Language.ENGLISH);

        // Check tree correctness
        Model m = MeaningTreeRDFHelper.backendFactsToModel(q.getStatementFacts());
        RDFDeserializer deserializer = new RDFDeserializer();
        MeaningTree mt = new MeaningTree(deserializer.deserialize(m));

        // Check metadata
        Assert.isTrue(q.getMetadata() != null
                && q.getMetadata().getIntegralComplexity() >= 0
                && q.getMetadata().getIntegralComplexity() <= 1, String.format("Invalid integral complexity %f", q.getMetadata().getIntegralComplexity()));
    }

    public void testStrictOrderConversion(SupportedLanguage language) {
        ProgrammingLanguageExpressionDTDomain domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
        String langStr = language.toString().substring(0, 1).toUpperCase() + language.toString().substring(1);

        List<String> tagsString = List.of(langStr);
        List<Tag> tags = tagsString.stream().map(domain::getTag).toList();

        exercise.setTags(langStr);
        exerciseRepository.save(exercise);

        ExerciseAttemptEntity attempt = new ExerciseAttemptEntity();
        attempt.setQuestions(List.of());
        attempt.setExercise(exercise);
        attempt.setUser(userRepository.findAll().iterator().next());
        exerciseAttemptRepository.save(attempt);

        QuestionRequest r = QuestionRequest.builder()
                .domainShortname("expression")
                .targetConcepts(Stream.of("operator_&&", "operator_||")
                        .map(domain::getConcept).toList())
                .stepsMin(2)
                .targetTags(Stream.of("C++").map(domain::getTag).toList())
                .targetLaws(List.of())
                .deniedLaws(List.of())
                .stepsMax(10)
                .complexity(0.8f)
                .build();
        Question q = domain.makeQuestion(attempt, r, tags, Language.ENGLISH);

        // Check tree correctness
        Model m = MeaningTreeRDFHelper.backendFactsToModel(q.getStatementFacts());
        RDFDeserializer deserializer = new RDFDeserializer();
        MeaningTree mt = new MeaningTree(deserializer.deserialize(m));
        try {
            System.out.println(
                    SupportedLanguage.CPP.createTranslator(new MeaningTreeDefaultExpressionConfig()).getCode(mt)
            );
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // Check values
        boolean hasValues = false;
        for (Node.Info info : mt) {
            if (info.node().hasLabel(NodeLabel.VALUE)) {
                hasValues = true;
                break;
            }
        }
        Assert.isTrue(hasValues, "Values not found");


        // Check metadata
        Assert.isTrue(q.getMetadata() != null
                && q.getMetadata().getIntegralComplexity() >= 0
                && q.getMetadata().getIntegralComplexity() <= 1, String.format("Invalid integral complexity %f", q.getMetadata().getIntegralComplexity()));

    }

    @Test
    public void testStrictOrderConversionCPP() {
        testStrictOrderConversion(SupportedLanguage.CPP);
    }

    @Test
    public void testStrictOrderConversionJava() {
        testStrictOrderConversion(SupportedLanguage.JAVA);
    }

    @Test
    public void testStrictOrderConversionPython() {
        testStrictOrderConversion(SupportedLanguage.PYTHON);
    }

    @Test
    public void testSolveGeneratedPythonQuestion()  {
        generateAndSolve("a && b && (c || d);",
                SupportedLanguage.CPP, SupportedLanguage.PYTHON,
                List.of(1, 3, 6));
        generateAndSolve("a[i + 3 * b] = b * 4 + 5;", SupportedLanguage.CPP,
                SupportedLanguage.PYTHON, List.of(5, 3, 1, 10, 12));
        generateAndSolve("x = a < ty(e, t, f) + 4 * (a && v || c);", SupportedLanguage.CPP,
                        SupportedLanguage.PYTHON, List.of(5, 17, 14, 12));
        generateAndSolve("a + b if a > c else 11", SupportedLanguage.PYTHON,
                SupportedLanguage.PYTHON, List.of(5, 3));
        generateAndSolve("x = a > c ? a + b : 11;", SupportedLanguage.CPP,
                // x = a + b if a > c else 11
                SupportedLanguage.PYTHON, List.of(7, 5));
    }

}