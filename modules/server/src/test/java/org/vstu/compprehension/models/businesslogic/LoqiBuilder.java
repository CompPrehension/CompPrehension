package org.vstu.compprehension.models.businesslogic;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
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
                true, true, true,
                true));
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
                .skipRuntimeValueGeneration(true)  // позволяет отключить генерацию значений для логических операторов, по умолчанию она отключена, иначе сохранятся все возможные варианты (если есть логические)
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
                    new File("c:/Temp2/loqi", q.getQuestionName() + ".loqi"));
        }
        return allPassed;
    }

//    @Test
    public void buildLoqi() {
        generate("a + b + c * x", SupportedLanguage.PYTHON, SupportedLanguage.CPP, List.of(1, 5, 3));
    }

    @Test
    public void buildLoqi_many() {
        var expr_list = new ArrayList< Pair<String, List<Integer>> >();
//        expr_list.add(new ImmutablePair<>("+ b", List.of(0)));
//        expr_list.add(new ImmutablePair<>("a + b", List.of(1)));
//        expr_list.add(new ImmutablePair<>("var [ var1 ]", List.of(1, 3)));
//        expr_list.add(new ImmutablePair<>("var [ ( var ) ]", List.of(1)));
        expr_list.add(new ImmutablePair<>("x + ( var -- -- ) --", List.of(1, 4, 5, 7)));
        expr_list.add(new ImmutablePair<>("func ( ( ( 1 ) ) , 2 )", List.of(0, 7)));
        expr_list.add(new ImmutablePair<>("+ - ~ -- ++ & * ! var1 . var2", List.of(0, 1, 2, 3, 4, 5, 6, 7, 9)));
        expr_list.add(new ImmutablePair<>("var6 %= var7 /= var8 *= var9 -= var10 += var11 = var12", List.of(1, 3, 5, 7, 9, 11)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , ! var --", List.of(1, 4, 7, 11, 13, 14, 16)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , var1 . var2 . var3", List.of(1, 4, 7, 11, 13, 15, 17)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , var & var2 ^ var3 | var4", List.of(1, 4, 7, 11, 13, 15, 17, 19)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , var [ var1 + var2 ] * -- var3", List.of(1, 4, 7, 11, 13, 15, 17, 19, 20, 21)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , + - ~ -- ++ & * ! var1 . var2", List.of(1, 4, 7, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21, 23)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , var6 %= var7 /= var8 *= var9 -= var10 += var11 = var12", List.of(1, 4, 7, 11, 13, 15, 17, 19, 21, 23, 25)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , var ^= var2 |= var3 &= var4 || var5 <<= var6 , ! var --", List.of(1, 4, 7, 11, 13, 15, 17, 19, 21, 23, 25, 26, 28)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , var ^= var2 |= var3 &= var4 || var5 <<= var6 , var1 . var2 ). var3", List.of(1, 4, 7, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , var ^= var2 |= var3 &= var4 || var5 <<= var6 , var & var2 )^ var3 | var4", List.of(1, 4, 7, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31)));
        expr_list.add(new ImmutablePair<>("var1 * ( var2 || ( var3 + var4 ) ) , var5 , var ^= var2 |= var3 &= var4 || var5 <<= var6 , var [ var1 )+ var2 ] * -- var3", List.of(1, 4, 7, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 32, 33)));
//        expr_list.add("");

        for (var tokens_ansobjs : expr_list) {
            try {
                generate(tokens_ansobjs.getLeft(), SupportedLanguage.PYTHON, SupportedLanguage.CPP, tokens_ansobjs.getRight());
            } catch (org.vstu.meaningtree.exceptions.UnsupportedParsingException ex) {
                System.out.println("ERROR in expression: " + tokens_ansobjs.getLeft());
            }
        }
    }
}
