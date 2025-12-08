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
import java.util.Map;
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
                      boolean consideredAsCorrect,
                      boolean detectUnfinished
    ) {
        List<ResponseEntity> responses = new ArrayList<>();
        Domain.InterpretSentenceResult result = null;
        for (var entry : answerObjectIds) {
            AnswerObjectEntity answerObject = AnswerObjectEntity
                    .builder().answerId(entry.getKey())
                    .domainInfo(entry.getValue()).build();
            responses.add(ResponseEntity.builder().leftAnswerObject(answerObject).rightAnswerObject(answerObject).build());
            result = judgeAtOnce(q, responses, consideredAsCorrect);
            if (result.IterationsLeft == 0) {
                System.err.println("#### WARNING ####: Unexpected end of program. Last response: id=%s, cfg_node_id=%s".formatted(entry.getKey(), entry.getValue()));
                break;
            }
        }
        if (result != null && result.IterationsLeft > 0 && detectUnfinished) {
            Assertions.fail("Answer processing was finished, but program requires also %d interactions".formatted(result.IterationsLeft));
        }
    }

    public Domain.InterpretSentenceResult judgeAtOnce(Question q, Map<Integer, String> answerObjectIds, boolean consideredAsCorrect) {
        List<ResponseEntity> responses = answerObjectIds.entrySet().stream()
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
            builder.append("===== Solution ==== \n");
        }
        builder.append("Answer ID trace: %s\n".formatted(responses.stream().map(r ->
                r.getLeftAnswerObject().getAnswerId().toString()
        ).collect(Collectors.joining("\n"))));
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
        builder.append("===== / ===== \n");
        return builder.toString();
    }

    public Domain.InterpretSentenceResult judgeAtOnce(Question q, List<ResponseEntity> responses, boolean consideredAsCorrect) {
        if (DETAILED_TRACE) {
            System.out.println("Prepared question answers (CFG ids): %s\n".formatted(responses.stream().map(r ->
                    r.getLeftAnswerObject().getDomainInfo()
            ).collect(Collectors.joining("\n"))));
        }
        var result = questionService.judgeQuestion(q, responses, List.of(domain.getTag("Python")));

        if (
                (!result.isAnswerCorrect && consideredAsCorrect) ||
                        (result.isAnswerCorrect && !consideredAsCorrect)
        ) {
            Assertions.fail(makeJudgeTrace(result, responses, true));
        } else if (DETAILED_TRACE) {
            System.out.println(makeJudgeTrace(result, responses, false));
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
        judge(question, answers, true, true);
    }

}
