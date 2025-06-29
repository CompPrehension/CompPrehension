package org.vstu.compprehension.models.businesslogic;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeOrderQuestionBuilder;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeRDFTransformer;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseOptionsEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.meaningtree.SupportedLanguage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Transactional
public class LoqiBuilder {
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

    private static final String RESOURCES_LOCATION = "org/vstu/compprehension/models/businesslogic/domains/";
    private static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "programming-language-expression-domain-model/";
    private final DomainSolvingModel domainSolvingModel = new DomainSolvingModel(
            this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION),
            DomainSolvingModel.BuildMethod.LOQI
    );

    @BeforeAll
    public void tearUp() {
        domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
        exercise = new ExerciseEntity();
        exercise.setDomain(domain.getDomainEntity());
        exercise.setBackendId("DTReasoner");
        exercise.setTags("");
        exercise.setOptions(new ExerciseOptionsEntity(null, true,
                true, true, true, true,
                true, 7));
        exercise.setName("test");
        exercise.setStages(Collections.singletonList(new ExerciseStageEntity()));
        exercise.setStrategyId("StaticStrategy");
        exercise.getStages().getFirst();
        exerciseRepository.save(exercise);
        attempt = new ExerciseAttemptEntity();
        attempt.setQuestions(List.of());
        attempt.setExercise(exercise);
        attempt.setUser(userRepository.findAll().iterator().next());
        exerciseAttemptRepository.save(attempt);
        exerciseRepository.save(exercise);
    }

    @AfterAll
    public void tearDown() {
        exerciseAttemptRepository.delete(attempt);
        exerciseRepository.delete(exercise);
    }

    public boolean generate(String expression, SupportedLanguage inLang, SupportedLanguage outLang, List<Integer> sequence) {
        List<Question> questions = MeaningTreeOrderQuestionBuilder
                .newQuestion(domain)
                .expression(expression, inLang)
                .questionOrigin("test", "MIT")
                .skipRuntimeValueGeneration(true)
                .buildQuestions(outLang);
        String outLangStr = outLang.toString().substring(0, 1).toUpperCase() + outLang.toString().substring(1);

        boolean allPassed = true;
        for (Question q : questions) {
            List<ResponseEntity> responses = new ArrayList<>();
            for (Integer response : sequence) {
                AnswerObjectEntity answerObject = AnswerObjectEntity
                        .builder().answerId(response)
                        .domainInfo("token_" + response).build();
                responses.add(ResponseEntity.builder().leftAnswerObject(answerObject).rightAnswerObject(answerObject).build());
            }
            DomainModel model = MeaningTreeRDFTransformer.questionToDomainModel(
                    domainSolvingModel, q.getStatementFacts(), responses, List.of(domain.getTag(outLangStr))
            );
            MeaningTreeRDFTransformer.dumpModelLoqi(model,
                    new File("D:/", q.getQuestionName() + ".loqi"));
        }
        return allPassed;
    }

    @Test
    public void buildLoqi() {
        generate("a + b + c * x", SupportedLanguage.PYTHON, SupportedLanguage.CPP, List.of(1, 5, 3));
    }
}
