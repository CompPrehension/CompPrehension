package org.vstu.compprehension.businesslogic.domains.controlflowdt;

import org.vstu.compprehension.businesslogic.domains.ControlFlowDTDomain;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.BundleLocalizationService;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.SeededRandomProvider;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.enums.Language;

import java.util.List;
import java.util.Set;

final class ControlFlowDtDomainFixture {

    static final String PLAY = "play";
    static final String QUESTION = "question";
    static final String STEP_INTO = "step-into";
    static final String STEP_OUT = "step-out";

    record BankQuestion(String file, List<String> trace, Set<String> conditions) {
        int steps() {
            return trace.size();
        }

        String action(int step) {
            return trace.get(step);
        }

        boolean isCondition(int step) {
            return conditions.contains(trace.get(step));
        }
    }

    static final BankQuestion SEQUENCE = new BankQuestion("sequence",
            List.of("global_statements_structure_action_first_ast6", "global_statements_structure_action_next_ast24"),
            Set.of());
    static final BankQuestion WHILE_NOT_ENTERED = new BankQuestion("while_not_entered",
            List.of("global_statements_structure_action_first_ast6", "global_statements_structure_action_next_ast19",
                    "global_statements_structure_action_next_ast32", "while_structure_action_cond_ast42",
                    "global_statements_structure_action_next_ast152"),
            Set.of("while_structure_action_cond_ast42"));
    static final BankQuestion IF_ELIF = new BankQuestion("if_elif",
            List.of("global_statements_structure_action_first_ast6", "global_statements_structure_action_next_ast19",
                    "if_structure_action_first_cond_ast33", "if_structure_action_next_cond_ast55",
                    "block_structure_action_first_ast64"),
            Set.of("if_structure_action_first_cond_ast33", "if_structure_action_next_cond_ast55"));
    static final BankQuestion IF_THEN = new BankQuestion("if_then",
            List.of("global_statements_structure_action_first_ast6", "global_statements_structure_action_next_ast19",
                    "if_structure_action_first_cond_ast29", "block_structure_action_first_ast43",
                    "global_statements_structure_action_next_ast98", "global_statements_structure_action_next_ast124",
                    "global_statements_structure_action_next_ast131"),
            Set.of("if_structure_action_first_cond_ast29"));
    static final BankQuestion WHILE_ONE_ITERATION = new BankQuestion("while_one_iteration",
            List.of("global_statements_structure_action_first_ast6", "global_statements_structure_action_next_ast19",
                    "while_structure_action_cond_ast29", "if_structure_action_first_cond_ast32",
                    "block_structure_action_first_ast46", "block_structure_action_next_ast80",
                    "while_structure_action_cond_ast29", "global_statements_structure_action_next_ast246"),
            Set.of("while_structure_action_cond_ast29", "if_structure_action_first_cond_ast32"));
    static final BankQuestion BREAK_IN_FOR = new BankQuestion("break_in_for",
            List.of("global_statements_structure_action_first_ast29", "global_statements_structure_action_next_ast59",
                    "global_statements_structure_action_next_ast72", "for_each_structure_action_init_ast85",
                    "for_each_structure_action_cond_ast80", "if_structure_action_first_cond_ast88",
                    "block_structure_action_first_ast97", "block_structure_action_next_ast110",
                    "global_statements_structure_action_next_ast156"),
            Set.of("for_each_structure_action_cond_ast80", "if_structure_action_first_cond_ast88"));
    static final BankQuestion NESTED_CALLS = new BankQuestion("nested_calls",
            List.of("func_call_structure_action_BEGIN_ast43", "func_call_structure_action_BEGIN_ast25",
                    "block_structure_action_first_ast9",
                    "func_call_structure_action_END_ast25,semantic_value_action_25_25_END_0",
                    "func_call_structure_action_BEGIN_ast30", "block_structure_action_first_ast9",
                    "func_call_structure_action_END_ast30,semantic_value_action_30_30_END_0",
                    "block_structure_action_first_ast32",
                    "func_call_structure_action_END_ast43,semantic_value_action_43_43_END_0",
                    "global_statements_structure_action_first_ast47"),
            Set.of());
    static final BankQuestion RECURSION_FACTORIAL = new BankQuestion("recursion_factorial",
            List.of("global_statements_structure_action_first_ast40", "func_call_structure_action_BEGIN_ast53",
                    "if_structure_action_first_cond_ast8", "func_call_structure_action_BEGIN_ast25",
                    "if_structure_action_first_cond_ast8", "func_call_structure_action_BEGIN_ast25",
                    "if_structure_action_first_cond_ast8", "block_structure_action_first_ast10",
                    "func_call_structure_action_END_ast25,semantic_value_action_25_25_END_0",
                    "block_structure_action_next_ast27",
                    "func_call_structure_action_END_ast25,semantic_value_action_25_25_END_1",
                    "block_structure_action_next_ast27",
                    "func_call_structure_action_END_ast53,semantic_value_action_53_53_END_0",
                    "global_statements_structure_action_next_ast57"),
            Set.of("if_structure_action_first_cond_ast8"));

    static final List<BankQuestion> BANK = List.of(
            SEQUENCE, WHILE_NOT_ENTERED, IF_ELIF, IF_THEN, WHILE_ONE_ITERATION, BREAK_IN_FOR,
            NESTED_CALLS, RECURSION_FACTORIAL);

    private static final String BANK_LOCATION = "org/vstu/compprehension/businesslogic/domains/controlflowdt/";
    static final List<String> BUNDLES = List.of("domains/control-flow");

    private static final class Holder {
        private static final ControlFlowDTDomain DOMAIN = new ControlFlowDTDomain(
                new SeededRandomProvider(),
                new BundleLocalizationService(BUNDLES.toArray(String[]::new)),
                null);
    }

    private ControlFlowDtDomainFixture() {
    }

    static ControlFlowDTDomain domain() {
        return Holder.DOMAIN;
    }

    static QuestionMetadataWithData bankRecord(BankQuestion bankQuestion) {
        return DomainFixtures.bankRecord(BANK_LOCATION + bankQuestion.file() + ".json");
    }

    static QuestionData bankQuestion(BankQuestion bankQuestion, Language language) {
        return QuestionData.of(domain().makeQuestion(bankRecord(bankQuestion), List.of(), language).getContent());
    }

    static QuestionData bankQuestion(BankQuestion bankQuestion) {
        return bankQuestion(bankQuestion, Language.RUSSIAN);
    }

    static AnswerObjectData action(QuestionData question, String cfgNodeId) {
        return question.getContent().getAnswerObjects().stream()
                .filter(a -> a.getDomainInfo().equals(cfgNodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Нет действия " + cfgNodeId + " среди " + question.getContent().getAnswerObjects()));
    }

    static List<AnswerObjectData> trace(QuestionData question, BankQuestion bankQuestion) {
        return bankQuestion.trace().stream().map(id -> action(question, id)).toList();
    }

    static List<AnswerObjectData> trace(QuestionData question, BankQuestion bankQuestion, int steps) {
        return trace(question, bankQuestion).subList(0, steps);
    }
}
