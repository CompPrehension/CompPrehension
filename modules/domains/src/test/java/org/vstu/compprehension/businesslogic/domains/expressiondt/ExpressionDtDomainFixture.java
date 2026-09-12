package org.vstu.compprehension.businesslogic.domains.expressiondt;

import org.vstu.compprehension.businesslogic.Tag;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.BundleLocalizationService;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.SeededRandomProvider;
import org.vstu.compprehension.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.businesslogic.domains.helpers.meaningtree.MeaningTreeOrderQuestionBuilder;
import org.vstu.compprehension.businesslogic.domains.helpers.meaningtree.QuestionDynamicDataAppender;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.data.domain.DomainOptionsData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.enums.Language;
import org.vstu.meaningtree.SupportedLanguage;

import java.util.List;

final class ExpressionDtDomainFixture {

    static final String END_TOKEN = "end_token";
    static final String CPP_TAG = "C++";

    record BankQuestion(String file, String expression, List<String> evaluationOrder) {
        int steps() {
            return evaluationOrder.size();
        }
    }

    static final BankQuestion MEMBER_ACCESS_PLUS = new BankQuestion("member_access_plus", "wp->sx + sb_w", List.of("->", "+"));
    static final BankQuestion MODULO_PLUS = new BankQuestion("modulo_plus", "byte % 10 + '0'", List.of("%", "+"));
    static final BankQuestion MUL_PLUS_MINUS = new BankQuestion("mul_plus_minus", "10 * z + c - '0'", List.of("*", "+", "-"));
    static final BankQuestion ASSIGN_UNARY_MINUS_PLUS = new BankQuestion("assign_unary_minus_plus", "pre = -pre + 2", List.of("-", "+", "="));
    static final BankQuestion PARENTHESES_AND_UNARY_MINUS = new BankQuestion("parentheses_and_unary_minus", "(i & -i)", List.of("-", "&"));

    static final List<BankQuestion> BANK = List.of(
            MEMBER_ACCESS_PLUS, MODULO_PLUS, MUL_PLUS_MINUS, ASSIGN_UNARY_MINUS_PLUS, PARENTHESES_AND_UNARY_MINUS);

    private static final String BANK_LOCATION = "org/vstu/compprehension/businesslogic/domains/expressiondt/";
    static final List<String> BUNDLES = List.of(
            "org/vstu/compprehension/businesslogic/domains/programming-language-expression-domain-dt-messages",
            "org/vstu/compprehension/businesslogic/domains/programming-language-expression-domain-messages");

    private static final class Holder {
        private static final ProgrammingLanguageExpressionDTDomain DOMAIN = new ProgrammingLanguageExpressionDTDomain(
                new DomainData("ProgrammingLanguageExpressionDTDomain", "expression_dt", "1", new DomainOptionsData()),
                new ProgrammingLanguageExpressionDomain(
                        new DomainData("ProgrammingLanguageExpressionDomain", "expression", "1", new DomainOptionsData()),
                        new BundleLocalizationService(BUNDLES.toArray(String[]::new)),
                        new SeededRandomProvider(),
                        null));
    }

    private ExpressionDtDomainFixture() {
    }

    static ProgrammingLanguageExpressionDTDomain domain() {
        return Holder.DOMAIN;
    }

    static List<Tag> cppTags() {
        return List.of(domain().getTag(CPP_TAG));
    }

    static QuestionMetadataWithData bankRecord(BankQuestion bankQuestion) {
        return DomainFixtures.bankRecord(BANK_LOCATION + bankQuestion.file() + ".json");
    }

    static QuestionData bankQuestion(BankQuestion bankQuestion, Language language) {
        return QuestionData.of(domain().makeQuestion(bankRecord(bankQuestion), cppTags(), language).getContent());
    }

    static QuestionData bankQuestion(BankQuestion bankQuestion) {
        return bankQuestion(bankQuestion, Language.RUSSIAN);
    }

    static QuestionData expressionQuestion(String expression) {
        var generated = MeaningTreeOrderQuestionBuilder.newQuestion(domain())
                .skipMutations(true)
                .questionOrigin("test", "MIT")
                .expression(expression, SupportedLanguage.CPP)
                .buildQuestions(SupportedLanguage.CPP)
                .getLast();
        var withAnswers = QuestionDynamicDataAppender.appendQuestionData(generated, null, SupportedLanguage.CPP, domain(), Language.RUSSIAN);
        return QuestionData.of(withAnswers.getContent());
    }

    static List<AnswerObjectData> operators(QuestionData question) {
        return question.getContent().getAnswerObjects().stream()
                .filter(a -> !END_TOKEN.equals(a.getDomainInfo()))
                .toList();
    }

    static AnswerObjectData operator(QuestionData question, String text) {
        return operators(question).stream()
                .filter(a -> a.getHyperText().equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Нет оператора " + text + " среди " + operators(question)));
    }

    static List<AnswerObjectData> operatorsInOrder(QuestionData question, BankQuestion bankQuestion) {
        return bankQuestion.evaluationOrder().stream().map(text -> operator(question, text)).toList();
    }

    static AnswerObjectData endToken(QuestionData question) {
        return question.getContent().getAnswerObjects().stream()
                .filter(a -> END_TOKEN.equals(a.getDomainInfo()))
                .findFirst()
                .orElseThrow();
    }
}
