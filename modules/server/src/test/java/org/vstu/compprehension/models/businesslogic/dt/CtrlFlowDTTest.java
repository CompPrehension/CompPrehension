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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("dev")
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

    public void judge(Question q,
                      List<Pair<Integer, String>> answerObjectIds,
                      boolean everySubTrace, boolean consideredAsCorrect,
                      boolean detectUnfinished
    ) {
        List<ResponseEntity> responses = new ArrayList<>();
        Domain.InterpretSentenceResult result = null;
        int i = 0;
        int last_i = answerObjectIds.size();
        for (var entry : answerObjectIds) {
            AnswerObjectEntity answerObject = AnswerObjectEntity
                    .builder().answerId(entry.getKey())
                    .domainInfo(entry.getValue()).build();
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
            System.out.println("Prepared question answers (CFG ids): %s\n".formatted(responses.stream().map(r ->
                    r.getLeftAnswerObject().getDomainInfo()
            ).collect(Collectors.joining("\n"))));
        }
        var result = questionService.judgeQuestion(q, responses, List.of(domain.getTag("Python")));

        System.out.printf("Expected %s solution...%n", consideredAsCorrect? "valid" : "invalid");

        if (result.isAnswerCorrect != consideredAsCorrect) {
            // Корректность не совпадает с ожидаемой.
            Assertions.fail(makeJudgeTrace(result, responses, !result.isAnswerCorrect));
        } else if (DETAILED_TRACE) {
            System.out.println(makeJudgeTrace(result, responses, !result.isAnswerCorrect));
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
        judge(question, answers, true, true, true);
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
        judge(question, answers, true, true, true);
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
        judge(question, answers, true, true, true);
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
        judge(question, answers, false, false, false);
    }

}
