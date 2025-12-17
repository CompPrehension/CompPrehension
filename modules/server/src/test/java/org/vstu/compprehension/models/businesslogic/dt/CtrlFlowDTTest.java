package org.vstu.compprehension.models.businesslogic.dt;

import domains.ControlFlowDTDomain;
import its.reasoner.nodes.*;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Transactional
public class CtrlFlowDTTest {
    @Autowired
    DomainFactory domainFactory;
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private QuestionMetadataRepository qMetaRepo;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestionService questionService;

    private static final boolean DETAILED_TRACE = true;

    private ControlFlowDTDomain domain;
    private ExerciseEntity exercise;
    private ExerciseAttemptEntity attempt;

    @BeforeAll
    public void tearUp() {
        domain = (ControlFlowDTDomain) domainFactory.getDomain("ControlFlowDTDomain");
        exercise = StreamSupport.stream(exerciseRepository.findAll().spliterator(), false)
                .filter(e -> e.getName().equals("CtrlFlow25-Test")).findFirst().orElseThrow();
        attempt = new ExerciseAttemptEntity();
        attempt.setQuestions(List.of());
        attempt.setExercise(exercise);
        attempt.setUser(userRepository.findAll().iterator().next());
        exerciseAttemptRepository.save(attempt);
    }

    public Question loadQuestion(String questionName) {
        var metas = qMetaRepo.findByName(questionName);
        return domain.makeQuestion(metas.getFirst(), attempt, List.of(domain.getTag("Python")), Language.ENGLISH);
    }

    private String walkDecisionTreeTrace(DecisionTreeTrace trace) {
        if (trace == null || trace.isEmpty()) {
            return "none";
        }
        return trace.stream().map(this::walkDecisionTreeTraceElement).collect(Collectors.joining(" -> "));
    }

    /**
     * Собрать результаты всех листовых узлов-результатов ветвей (BranchResult) по всей трассе,
     * включая все уровни вложенности (агрегации, циклы и т.п.).
     * Формат элементов списка совпадает с BranchResult‑веткой в walkDecisionTreeTraceElement:
     * "<RESULT [skill]>".
     */
    private List<String> collectLeafBranchResults(DecisionTreeTrace trace) {
        return DecisionTreeReasonerBackend.nestedTraceElements(trace).stream()
                // Листовые элементы: у них нет вложенных трасс
                .filter(e -> Objects.requireNonNullElse(e.nestedTraces(), List.<DecisionTreeTrace>of()).isEmpty())
                // Нас интересуют только результаты ветвей
                .filter(e -> e instanceof BranchResultDecisionTreeTraceElement)
                .map(e -> (BranchResultDecisionTreeTraceElement) e)
                // Форматируем так же, как в walkDecisionTreeTraceElement
                .map(e -> "<%s [%s]>".formatted(e.getNode().getValue(), e.getNode().getMetadata().get("skill")))
                .toList();
    }

    private String walkDecisionTreeTraceElement(DecisionTreeTraceElement element) {
        return switch (element) {
            case BranchResultDecisionTreeTraceElement e -> "<%s [%s]>".formatted(e.getNode().getValue(), e.getNode().getMetadata().get("skill"));
            case AggregationDecisionTreeTraceElement e -> "%s => {\n\t%s\n}".formatted(
                        e.getNode().getClass().getSimpleName(),
                        e.nestedTraces().stream().map(n -> walkDecisionTreeTrace((DecisionTreeTrace) n)).collect(Collectors.joining(" ; "))
            );
            case WhileCycleDecisionTreeTraceElement e -> "%s => {\n\t%s\n}".formatted(
                    e.getNode().getClass().getSimpleName(),
                    e.nestedTraces().stream().map(n -> walkDecisionTreeTrace(n)).collect(Collectors.joining(" ;\n\t"))
            );
            default -> element.getNode().getClass().getSimpleName();
        };
    }

    /**
     * Базовая реализация проверки ответа по шагам.
     * Возвращает итоговый результат интерпретации, чтобы можно было дополнительно его проанализировать.
     */
    private Domain.InterpretSentenceResult judgeCore(Question q,
                                                     List<Pair<Integer, String>> answerObjectIds,
                                                     boolean everySubTrace, boolean consideredAsCorrect,
                                                     boolean detectUnfinished
    ) {
        List<ResponseEntity> responses = new ArrayList<>();
        Domain.InterpretSentenceResult result = null;
        int i = 0;
        int last_i = answerObjectIds.size();
        for (var entry : answerObjectIds) {
            AnswerObjectEntity answerObject = q.getAnswerObject(entry.getKey());  // Allow invalid node IDs in tests (these may change after rebuild);
            responses.add(ResponseEntity.builder().leftAnswerObject(answerObject).rightAnswerObject(answerObject).build());
            i++;
            boolean is_last = i == last_i;

            if (!everySubTrace && !is_last) {
                continue;
            }

            result = judgeAtOnce(q, responses, consideredAsCorrect);
            if (result.IterationsLeft == 0 && !is_last) {
                // The end of the program but not the end of the trace.
                System.err.println("#### WARNING ####: Unexpected end of program. Last response: id=%s, cfg_node_id=%s".formatted(entry.getKey(), entry.getValue()));
                // break;  // No need to stop now.
            }
        }
        if (result != null && result.IterationsLeft > 0 && detectUnfinished) {
            Assertions.fail("Answer processing was finished, but program requires also %d interactions".formatted(result.IterationsLeft));
        }
        return result;
    }

    public void judgeAndCheck(Question q,
                              List<Pair<Integer, String>> answerObjectIds,
                              boolean everySubTrace, boolean consideredAsCorrect,
                              boolean detectUnfinished
    ) {
        judgeCore(q, answerObjectIds, everySubTrace, consideredAsCorrect, detectUnfinished);
    }

    /**
     * Расширенный вариант judge, который кроме стандартной проверки корректности
     * дополнительно проверяет, что среди листовых результатов трассы есть заданные значения.
     */
    public void judgeAndCheck(Question q,
                              List<Pair<Integer, String>> answerObjectIds,
                              boolean everySubTrace, boolean consideredAsCorrect,
                              boolean detectUnfinished,
                              List<String> expectedLeafResults
    ) {
        Domain.InterpretSentenceResult result =
                judgeCore(q, answerObjectIds, everySubTrace, consideredAsCorrect, detectUnfinished);

        if (expectedLeafResults != null && !expectedLeafResults.isEmpty()) {
            List<String> leafResults = collectLeafBranchResults(result.decisionTreeTrace);
            for (String expected : expectedLeafResults) {
                Assertions.assertTrue(
                        leafResults.contains(expected),
                        "Expected outcome not found: %s; actual outcomes: %s"
                                .formatted(expected, String.join(", ", leafResults))
                );
            }
        }
    }

    public Domain.InterpretSentenceResult judgeAtOnceByAnswerObjects(Question q, List<Pair<Integer, String>> answerObjectIds, boolean consideredAsCorrect) {
        List<ResponseEntity> responses = answerObjectIds.stream()
                .map((entry) -> AnswerObjectEntity.builder().answerId(entry.getKey())
                        .domainInfo(entry.getValue()).build())
                .map((answerObject) -> ResponseEntity.builder().leftAnswerObject(answerObject).rightAnswerObject(answerObject).build())
                .toList();
        return judgeAtOnce(q, responses, consideredAsCorrect);
    }

    public String makeJudgeTrace(Domain.InterpretSentenceResult result, List<ResponseEntity> responses, boolean invalid) {
        StringBuilder builder = new StringBuilder();
        if (invalid) {
            builder.append("=====  !!! Invalid solution !!! ==== \n");
        } else {
            builder.append("===== Valid solution ==== \n");
        }
        builder.append("STEPS LEFT: %d\n".formatted(result.IterationsLeft));

        builder.append("Answer ID trace: %s\n".formatted(responses.stream().map(r ->
                r.getLeftAnswerObject().getAnswerId().toString()
        ).collect(Collectors.joining(" -> "))));
        builder.append("CFG Trace: %s\n".formatted(responses.stream().map(r ->
                r.getLeftAnswerObject().getDomainInfo()
        ).collect(Collectors.joining(" -> "))));
        builder.append("Judge result: %s\n".formatted(result.isAnswerCorrect));
        builder.append("Variable dump: %s\n".formatted(
                result.decisionTreeTrace.getFinalVariableSnapshot().entrySet().stream()
                        .map(varObj -> "%s = %s".formatted(varObj.getKey(), varObj.getValue()))
                        .collect(Collectors.joining("; "))
        ));
        builder.append("Interpretation trace: %s\n".formatted(walkDecisionTreeTrace(result.decisionTreeTrace)));
        builder.append("\n===== / ===== \n");
        return builder.toString();
    }

    public Domain.InterpretSentenceResult judgeAtOnce(Question q, List<ResponseEntity> responses, boolean consideredAsCorrect) {
        if (DETAILED_TRACE) {
            System.out.println("Prepared question answers (CFG ids): \n- %s\n".formatted(responses.stream().map(r ->
                    r.getLeftAnswerObject().getDomainInfo()
            ).collect(Collectors.joining("\n- "))));
        }
        var result = questionService.judgeQuestion(q, responses, List.of(domain.getTag("Python")));

        System.out.printf("Expected %s solution...%n", consideredAsCorrect? "valid" : "invalid");

        if (result.isAnswerCorrect != consideredAsCorrect) {
            // Корректность не совпадает с ожидаемой.
            Assertions.fail(makeJudgeTrace(result, responses, !result.isAnswerCorrect));
        } else if (DETAILED_TRACE) {
            System.out.println(makeJudgeTrace(result, responses, !result.isAnswerCorrect));

            // Для отладки: показываем также все листовые результаты ветвей
            List<String> leafResults = collectLeafBranchResults(result.decisionTreeTrace);
            System.out.println("Leaf branch results: " + String.join(", ", leafResults));
        }
        return result;
    }

    @Test
    public void variableSequence() {
        var question = loadQuestion("debug_6_simple_variables.py");
        var answers = List.of(
                Pair.of(0, "atom_104"),
                Pair.of(1, "atom_107"),
                Pair.of(2, "atom_111"),
                Pair.of(3, "atom_115"),
                Pair.of(4, "atom_119")
        );
        judgeAndCheck(question, answers, true, true, true);
    }


    @Test
    public void simpleCall() {
        var question = loadQuestion("debug_5_simple_function.py");
        var answers = List.of(
            Pair.of(1, "BEGIN_114"),
            Pair.of(2, "BEGIN_117"),
            Pair.of(0, "atom_104"),
            Pair.of(3, "END_118")
        );
        judgeAndCheck(question, answers, true, true, true);
    }

    @Test
    public void ex_5_inf_recursion_t1() {
        var question = loadQuestion("debug_5_inf_recursion.py");
        var answers = List.of(
            Pair.of(5, "atom_145"),
            Pair.of(6, "BEGIN_149"),
            Pair.of(7, "BEGIN_152"),
            Pair.of(0, "atom_104"),
            Pair.of(1, "atom_110"),  // true
            Pair.of(2, "BEGIN_119"),
            Pair.of(0, "atom_104"),
            Pair.of(1, "atom_110"),  // false
            Pair.of(4, "atom_134"),
            Pair.of(3, "END_120"),
            Pair.of(4, "atom_134"),
            Pair.of(8, "END_153")
        );
        judgeAndCheck(question, answers, true, true, true);
    }
    @Test
    public void ex_5_inf_recursion_t2() {
        var question = loadQuestion("debug_5_inf_recursion.py");
        var answers = List.of(
            Pair.of(5, "atom_145"),
            Pair.of(6, "BEGIN_149"),
            Pair.of(7, "BEGIN_152"),
            Pair.of(0, "atom_104"),
            Pair.of(1, "atom_110"),  // не false !!!
            Pair.of(4, "atom_134")  // неправильно!
//            Pair.of(8, "END_153")
        );
        judgeAndCheck(question, answers, false, false, false);
    }

    @Test
    public void ex_1_onefunc_while_11_t1() {
        var question = loadQuestion("debug_1_onefunc_while.py_1+1");
        var answers = List.of(
            Pair.of(3, "atom_136"),
            Pair.of(4, "atom_140"),
            Pair.of(5, "atom_147"),  // false
            Pair.of(6, "atom_153"),
            Pair.of(5, "atom_147"),
            Pair.of(7, "atom_165"),
            Pair.of(8, "BEGIN_169"),
            Pair.of(9, "BEGIN_172"), // (call)
            Pair.of(0, "atom_107"),  // false
            Pair.of(1, "atom_113"),
            Pair.of(0, "atom_107"),
            Pair.of(2, "atom_124"),
            Pair.of(10, "END_173")
        );
        judgeAndCheck(question, answers, true, true, true);
    }

    @Test
    public void ex_1_onefunc_while_11_t2() {
        var question = loadQuestion("debug_1_onefunc_while.py_1+1");
        var answers = List.of(
            Pair.of(3, "atom_136"),
            Pair.of(4, "atom_139"),
            Pair.of(5, "atom_146"),  // false
            Pair.of(7, "atom_164"),
            Pair.of(8, "BEGIN_168"),
            //
            // Pair.of(9, "BEGIN_171"), // (call)
            // Pair.of(0, "atom_107"),  // false
            // Pair.of(2, "atom_124"),
             Pair.of(10, "END_172")
        );
        judgeAndCheck(question, answers, false, false, true);
    }

    @Test
    public void ex_1_onefunc_while_11_t3() {
        var question = loadQuestion("debug_1_onefunc_while.py_1+1");
        var answers = List.of(
            Pair.of(3, "atom_136"),
            Pair.of(4, "atom_139"),
            Pair.of(5, "atom_146"),  // false
            Pair.of(6, "atom_152")  // ложный заход в цикл!
            // Pair.of(7, "atom_164"),
            // Pair.of(8, "BEGIN_168"),
            // Pair.of(9, "BEGIN_171"), // (call)
            // Pair.of(0, "atom_107"),  // false
            // Pair.of(2, "atom_124"),
            // Pair.of(10, "END_172")
        );
        judgeAndCheck(question, answers, false, false, false);
    }

    @Test
    public void ex_1_onefunc_while_22_t1() {
        var question = loadQuestion("debug_1_onefunc_while.py_2+2");
        var answers = List.of(
            Pair.of(3, "atom_136"),
            Pair.of(4, "atom_140"),
            Pair.of(5, "atom_147"),  // true
            Pair.of(6, "atom_153"),
            Pair.of(5, "atom_147"),  // false
            Pair.of(7, "atom_165"),
            Pair.of(8, "BEGIN_169"),
            Pair.of(9, "BEGIN_172"), // (call)
            Pair.of(0, "atom_107"),  // true
            Pair.of(1, "atom_113"),
            Pair.of(0, "atom_107"),  // false
            Pair.of(2, "atom_124"),
            Pair.of(10, "END_173")
        );
        judgeAndCheck(question, answers, true, true, true);
    }

    @Test
    public void ex_1_onefunc_while_22_t2() {
        var question = loadQuestion("debug_1_onefunc_while.py_2+2");
        var answers = List.of(
            Pair.of(3, "atom_136"),
            Pair.of(4, "atom_139"),
            Pair.of(5, "atom_146"),  // true
            Pair.of(6, "atom_152"),
            // Pair.of(5, "atom_146"),  // false  --- no last cond chacked
            Pair.of(7, "atom_164")
//            Pair.of(8, "BEGIN_168"),
//            Pair.of(9, "BEGIN_171"), // (call)
//            Pair.of(0, "atom_107"),  // true
//            Pair.of(1, "atom_113"),
//            Pair.of(0, "atom_107"),  // false
//            Pair.of(2, "atom_124"),
//            Pair.of(10, "END_172")
        );
        judgeAndCheck(question, answers, false, false, false);
    }

    @Test
    public void ex_1_onefunc_while_22_t3() {
        var question = loadQuestion("debug_1_onefunc_while.py_2+2");
        var answers = List.of(
            Pair.of(3, "atom_136"),
            Pair.of(4, "atom_139"),
            Pair.of(5, "atom_146"),  // true
            Pair.of(6, "atom_152"),
            Pair.of(5, "atom_146"),  // false
            Pair.of(7, "atom_164"),
            Pair.of(8, "BEGIN_168"),
            Pair.of(9, "BEGIN_171"), // (call)
            Pair.of(0, "atom_107"),  // true
            Pair.of(1, "atom_113"),
            Pair.of(0, "atom_107"),  // false
            // Pair.of(2, "atom_124"), // no return
            Pair.of(10, "END_172") // (end of call)
        );
        judgeAndCheck(question, answers, false, false, false);
    }

    @Test
    public void ex_1_onefunc_while_22_t4() {
        var question = loadQuestion("debug_1_onefunc_while.py_2+2");
        var answers = List.of(
                /* 1*/  Pair.of(3, "atom_136"),
                /* 2*/  Pair.of(4, "atom_139"),
                /* 3*/  Pair.of(5, "atom_146"),  // true
                /* 4*/  Pair.of(6, "atom_152"),
                /* 5*/  Pair.of(5, "atom_146"),  // false
                /* 6*/  Pair.of(7, "atom_164"),
                /* 7*/  Pair.of(8, "BEGIN_168"),
                /* 8*/  Pair.of(9, "BEGIN_171"), // (call)
                /* 9*/  Pair.of(0, "atom_107"),  // true
                /*10*/  Pair.of(1, "atom_113"),
                /*11*/  Pair.of(0, "atom_107"),  // false
                /*12*/  Pair.of(1, "atom_113")  // body again!
                /*13*/  // Pair.of(2, "atom_124") // (return)
                /*14*/  // Pair.of(10, "END_172") // (end of call)
        );
        judgeAndCheck(
                question,
                answers,
                false,
                false,
                false,
                List.of("<ERROR [condition_value_allows_transition]>")
        );
    }

    @Test
    public void ex_1_onefunc_while_22_t5() {
        var question = loadQuestion("debug_1_onefunc_while.py_2+2");
        var answers = List.of(
          /* 1*/  Pair.of(3, "atom_136"),
          /* 2*/  Pair.of(4, "atom_139"),
          /* 3*/  Pair.of(5, "atom_146"),  // true
          /* 4*/  Pair.of(6, "atom_152"),
          /* 5*/  Pair.of(5, "atom_146"),  // false
          /* 6*/  Pair.of(7, "atom_164"),
          /* 7*/  // Pair.of(8, "BEGIN_168"), // (stmt with calls)
          /* 8*/  Pair.of(9, "BEGIN_171") // (call)
          /* 9*/  // Pair.of(0, "atom_107"),  // true
          /*10*/  // Pair.of(1, "atom_113"),
          /*11*/  // Pair.of(0, "atom_107"),  // false
          /*13*/  // Pair.of(2, "atom_124") // (return)
          /*14*/  // Pair.of(10, "END_172") // (end of call)
        );
        judgeAndCheck(
                question,
                answers,
                false,
                false,
                false
                // , List.of("<ERROR [many_actions_in_require_selection]>")
        );
    }

    @Test
    public void ex_5_inf_recursion2_t1() {
        var question = loadQuestion("debug_5_inf_recursion2.py");
        var answers = List.of(
                Pair.of(8, "atom_172"),
                Pair.of(9, "BEGIN_176"),
                Pair.of(10, "BEGIN_179"),
                Pair.of(0, "atom_104"),
                Pair.of(1, "atom_110"),
                Pair.of(2, "BEGIN_119"),
                Pair.of(0, "atom_104"),
                Pair.of(1, "atom_110"),
                Pair.of(4, "atom_137"),
                Pair.of(7, "atom_161"),
                Pair.of(3, "END_120"),
                Pair.of(4, "atom_137"),
                Pair.of(7, "atom_161"),
                Pair.of(11, "END_180"),
                Pair.of(12, "atom_186")
        );
        judgeAndCheck(
                question,
                answers,
                true,
                true,
                true
                 , List.of("<CORRECT [selected_transition_without_any_constraint]>")
        );
    }

    @Test
    public void ex_5_inf_recursion2_t2() {
        var question = loadQuestion("debug_5_inf_recursion2.py");
        var answers = List.of(
                Pair.of(8, "atom_172"),
                Pair.of(9, "BEGIN_176"),
                Pair.of(10, "BEGIN_179"),
                Pair.of(0, "atom_104"),
                Pair.of(1, "atom_110"),
                Pair.of(2, "BEGIN_119"),
                Pair.of(0, "atom_104"),
                Pair.of(1, "atom_110"),
                Pair.of(4, "atom_137"),
                Pair.of(7, "atom_161"),
                Pair.of(3, "END_120"),
                Pair.of(4, "atom_137"),
                Pair.of(7, "atom_161"),
                // Pair.of(11, "END_180"),
                Pair.of(3, "END_120")  // wrong: (earlier call ended again)
                // Pair.of(12, "atom_186")
        );
        judgeAndCheck(
                question,
                answers,
                false,
                false,
                false
                 , List.of("<ERROR [is_function_call_jumping_correct]>")
        );
    }

    @Test
    public void ex_5_inf_recursion2_t3() {
        var question = loadQuestion("debug_5_inf_recursion2.py");
        var answers = List.of(
                Pair.of(8, "atom_172"),
                Pair.of(9, "BEGIN_176"),
                Pair.of(10, "BEGIN_179"),
                Pair.of(0, "atom_104"),
                Pair.of(1, "atom_110"),
                Pair.of(2, "BEGIN_119"),
                Pair.of(0, "atom_104"),
                Pair.of(1, "atom_110"),
                Pair.of(4, "atom_137"),
                Pair.of(7, "atom_161"),
                Pair.of(3, "END_120"),
                Pair.of(4, "atom_137"),
                Pair.of(7, "atom_161"),
                // Pair.of(11, "END_180"),
                Pair.of(6, "END_147")  // wrong: (earlier call ended)
                // Pair.of(12, "atom_186")
        );
        judgeAndCheck(
                question,
                answers,
                false,
                false,
                false
                 , List.of("<ERROR [is_function_call_jumping_correct]>")
        );
    }

    @Test
    public void ex_3_recursion_short_t1() {
        var question = loadQuestion("debug_3_recursion.py_short");
        var answers = List.of(
            Pair.of(9, "atom_199"),
            Pair.of(10, "BEGIN_203"),
            Pair.of(11, "BEGIN_206"),
            Pair.of(0, "atom_107"),
            Pair.of(1, "atom_113"),
            Pair.of(12, "END_207")
        );
        judgeAndCheck(
                question,
                answers,
                true,
                true,
                true
                , List.of("<CORRECT [selected_transition_without_any_constraint]>")
        );
    }

}
