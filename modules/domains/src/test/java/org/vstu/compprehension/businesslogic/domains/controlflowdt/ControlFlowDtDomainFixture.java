package org.vstu.compprehension.businesslogic.domains.controlflowdt;

import org.vstu.compprehension.businesslogic.domains.ControlFlowDTDomain;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.BundleLocalizationService;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.SeededRandomProvider;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.data.domain.DomainOptionsData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.enums.Language;

import java.util.List;
import java.util.Set;

final class ControlFlowDtDomainFixture {

    static final String PLAY = "play";
    static final String QUESTION = "question";

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
            List.of("atom_104", "atom_107"),
            Set.of());
    static final BankQuestion WHILE_NOT_ENTERED = new BankQuestion("while_not_entered",
            List.of("atom_104", "atom_107", "atom_111", "atom_118", "atom_140"),
            Set.of("atom_118"));
    static final BankQuestion IF_ELIF = new BankQuestion("if_elif",
            List.of("atom_104", "atom_107", "atom_114", "atom_126", "atom_135"),
            Set.of("atom_114", "atom_126"));
    static final BankQuestion WHILE_ONE_ITERATION = new BankQuestion("while_one_iteration",
            List.of("atom_104", "atom_107", "atom_114", "atom_123", "atom_129", "atom_132", "atom_114", "atom_163"),
            Set.of("atom_114", "atom_123"));
    static final BankQuestion BREAK_IN_FOR = new BankQuestion("break_in_for",
            List.of("atom_104", "atom_107", "atom_111", "atom_118", "atom_121", "atom_131", "atom_121", "atom_131",
                    "atom_121", "atom_131", "atom_137", "atom_140", "atom_186"),
            Set.of("atom_121", "atom_131"));

    static final List<BankQuestion> BANK = List.of(
            SEQUENCE, WHILE_NOT_ENTERED, IF_ELIF, WHILE_ONE_ITERATION, BREAK_IN_FOR);

    private static final String BANK_LOCATION = "org/vstu/compprehension/businesslogic/domains/controlflowdt/";
    static final List<String> BUNDLES = List.of("domains/control-flow");

    private static final class Holder {
        private static final ControlFlowDTDomain DOMAIN = new ControlFlowDTDomain(
                new DomainData("ControlFlowDTDomain", "ctrl_flow_dt25", "2", new DomainOptionsData()),
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
