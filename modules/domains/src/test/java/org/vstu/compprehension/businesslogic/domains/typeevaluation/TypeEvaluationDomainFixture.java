package org.vstu.compprehension.businesslogic.domains.typeevaluation;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.domains.Domain.InterpretSentenceResult;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.BundleLocalizationService;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.SeededRandomProvider;
import org.vstu.compprehension.businesslogic.domains.TypeEvaluationDTDomain;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.FeedbackData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.Language;

import java.util.ArrayList;
import java.util.List;

final class TypeEvaluationDomainFixture {

    static final List<String> BUNDLES = List.of("domains/type-evaluation");
    private static final String BANK_LOCATION = "org/vstu/compprehension/businesslogic/domains/typeevaluation/";

    /** Вопрос банка и его решение: операции в порядке вычисления с эталонными типами. */
    record BankQuestion(String file, List<Step> solution) {
    }

    record Step(String operation, String type) {
    }

    static final BankQuestion AVERAGE_OF_GRADES = new BankQuestion("average_of_grades",
            List.of(new Step("op_len", "t_int"), new Step("op_div", "t_float")));
    static final BankQuestion EMPTY_NAME_OR_NAMES = new BankQuestion("empty_name_or_names",
            List.of(new Step("op_or", "t_list_str"), new Step("op_len", "t_int")));
    static final BankQuestion STUDENT_FIRST_GRADE = new BankQuestion("student_first_grade",
            List.of(new Step("op_key", "t_list_int"), new Step("op_first", "t_int")));
    static final BankQuestion FIRST_CHAR_PLUS_ONE = new BankQuestion("first_char_plus_one",
            List.of(new Step("op_first", "t_str"), new Step("op_add", "t_error")));

    static final List<BankQuestion> BANK = List.of(
            AVERAGE_OF_GRADES, EMPTY_NAME_OR_NAMES, STUDENT_FIRST_GRADE, FIRST_CHAR_PLUS_ONE);

    private static final class Holder {
        private static final TypeEvaluationDTDomain DOMAIN = new TypeEvaluationDTDomain(
                new SeededRandomProvider(),
                new BundleLocalizationService(BUNDLES.toArray(String[]::new)),
                null);
    }

    private TypeEvaluationDomainFixture() {
    }

    static @NotNull TypeEvaluationDTDomain domain() {
        return Holder.DOMAIN;
    }

    static @NotNull QuestionData question(@NotNull BankQuestion bankQuestion) {
        var record = DomainFixtures.bankRecord(BANK_LOCATION + bankQuestion.file() + ".json");
        return QuestionData.of(domain().makeQuestion(record, List.of(), Language.RUSSIAN).getContent());
    }

    /** Ответ студента: в слот операции выбран тип. */
    static @NotNull AnswerData answer(@NotNull QuestionData question, @NotNull String operation, @NotNull String type) {
        return new AnswerData.Pair(answerObject(question, operation, false), answerObject(question, type, true));
    }

    static @NotNull List<AnswerData> solution(@NotNull QuestionData question, @NotNull BankQuestion bankQuestion, int steps) {
        return bankQuestion.solution().subList(0, steps).stream()
                .map(step -> answer(question, step.operation(), step.type()))
                .toList();
    }

    static @NotNull InterpretSentenceResult judge(@NotNull QuestionData question, @NotNull List<AnswerData> responses) {
        return domain().judgeQuestion(question, responses,
                domain().resolveTags(question.getContent().getTags()), Language.RUSSIAN);
    }

    /** Вопрос, в котором уже принято верное взаимодействие с данными ответами. */
    static @NotNull QuestionData withCorrectAnswers(@NotNull QuestionData question, @NotNull List<AnswerData> answers,
                                                    int interactionsLeft) {
        var responses = answers.stream()
                .map(answer -> ResponseData.builder().answer(answer).build())
                .toList();
        return question.withInteraction(QuestionInteractionData.builder()
                .id(1L)
                .interactionType(InteractionType.SEND_RESPONSE)
                .responses(new ArrayList<>(responses))
                .violations(new ArrayList<>())
                .feedback(FeedbackData.builder().interactionsLeft(interactionsLeft).build())
                .build());
    }

    private static @NotNull AnswerObjectData answerObject(@NotNull QuestionData question, @NotNull String domainInfo,
                                                          boolean isRightCol) {
        return question.getContent().getAnswerObjects().stream()
                .filter(answer -> answer.isRightCol() == isRightCol && answer.getDomainInfo().equals(domainInfo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Нет объекта ответа " + domainInfo));
    }
}
