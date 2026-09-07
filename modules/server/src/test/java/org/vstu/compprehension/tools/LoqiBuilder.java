package org.vstu.compprehension.tools;

import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.data.exercise.ExerciseStageData;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.businesslogic.*;

import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.services.QuestionDataService;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.businesslogic.domains.helpers.meaningtree.MeaningTreeOrderQuestionBuilder;
import org.vstu.compprehension.businesslogic.domains.helpers.meaningtree.MeaningTreeRDFTransformer;
import org.vstu.compprehension.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.entities.ExerciseEntity;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository;
import org.vstu.compprehension.repositories.entity.DomainRepository;
import org.vstu.compprehension.repositories.entity.ExerciseRepository;
import org.vstu.compprehension.repositories.entity.UserRepository;
import org.vstu.meaningtree.SupportedLanguage;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@Log4j2
public class LoqiBuilder extends AbstractIntegrationTest {
    @Autowired
    DomainFactory domainFactory;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestionDataService questionService;

    private ExerciseAttemptEntity attempt;
    private ExerciseEntity exercise;
    private ProgrammingLanguageExpressionDTDomain domain;

    public static final String domainId = "ProgrammingLanguageExpressionDTDomain";

    private static final String RESOURCES_LOCATION = "org/vstu/compprehension/businesslogic/domains/";
    private static final String DOMAIN_MODEL_LOCATION = RESOURCES_LOCATION + "programming-language-expression-domain-model/";
    private final DomainSolvingModel domainSolvingModel = new DomainSolvingModel(
            this.getClass().getClassLoader().getResource(DOMAIN_MODEL_LOCATION),
            DomainSolvingModel.BuildMethod.LOQI
    );

    @BeforeAll
    public void tearUp() {
        domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
        exercise = new ExerciseEntity();
        exercise.setDomain(domainRepository.findById(domain.getName()).orElseThrow());
        exercise.setBackendId("DTReasoner");
        exercise.setTags("");
        exercise.setOptions(new ExerciseOptionsData(null, true,
                true, true, true, true,
                true, 7, null, null));
        exercise.setName("test");
        exercise.setStages(Collections.singletonList(new ExerciseStageData()));
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

    @SneakyThrows
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
            List<ResponseData> responses = new ArrayList<>();
            for (Integer response : sequence) {
                AnswerObjectData answerObject = AnswerObjectData
                        .builder().answerId(response)
                        .domainInfo("token_" + response).build();
                responses.add(ResponseData.builder().leftAnswerObject(answerObject).rightAnswerObject(answerObject).build());
            }
            DomainModel model = MeaningTreeRDFTransformer.questionToDomainModel(
                    domainSolvingModel, q.getStatementFacts(), responses, List.of(domain.getTag(outLangStr))
            );
            var tempDir = Files.createTempDirectory("loqi").toFile();
            var filename = new File(tempDir, q.getQuestionName() + ".loqi");
            MeaningTreeRDFTransformer.dumpModelLoqi(model, filename);
            log.info("Saved to {}", filename.getAbsolutePath());
        }
        return allPassed;
    }

    @Test
    public void buildLoqi() {
        generate("a + b + c * x", SupportedLanguage.PYTHON, SupportedLanguage.CPP, List.of(1, 5, 3));
    }
}
