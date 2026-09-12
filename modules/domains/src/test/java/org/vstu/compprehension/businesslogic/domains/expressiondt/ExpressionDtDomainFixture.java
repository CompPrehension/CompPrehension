package org.vstu.compprehension.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.Tag;
import org.vstu.compprehension.businesslogic.domains.helpers.meaningtree.MeaningTreeOrderQuestionBuilder;
import org.vstu.compprehension.businesslogic.domains.helpers.meaningtree.QuestionDynamicDataAppender;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.data.domain.DomainOptionsData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.services.LocalizationService;
import org.vstu.compprehension.services.RandomProvider;
import org.vstu.meaningtree.SupportedLanguage;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

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

    private static final String BANK_LOCATION = "org/vstu/compprehension/businesslogic/domains/expression-dt-bank/";
    private static final String BUNDLES_PACKAGE = "org.vstu.compprehension.businesslogic.domains.";
    private static final List<String> BUNDLES = List.of(
            BUNDLES_PACKAGE + "programming-language-expression-domain-dt-messages",
            BUNDLES_PACKAGE + "programming-language-expression-domain-messages");

    private static final class Holder {
        private static final ProgrammingLanguageExpressionDTDomain DOMAIN = new ProgrammingLanguageExpressionDTDomain(
                new DomainData("ProgrammingLanguageExpressionDTDomain", "expression_dt", "1", new DomainOptionsData()),
                new ProgrammingLanguageExpressionDomain(
                        new DomainData("ProgrammingLanguageExpressionDomain", "expression", "1", new DomainOptionsData()),
                        new BundleLocalizationService(),
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
        var resource = BANK_LOCATION + bankQuestion.file() + ".json";
        try (var stream = ExpressionDtDomainFixture.class.getClassLoader().getResourceAsStream(resource)) {
            var json = JsonParser.parseReader(new InputStreamReader(
                    Objects.requireNonNull(stream, resource), StandardCharsets.UTF_8)).getAsJsonObject();
            var data = json.remove("data");
            var metadata = new Gson().fromJson(json, QuestionMetadataWithData.class);
            metadata.setData(SerializableQuestion.deserializeFromString(data.toString()));
            return metadata;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    static List<ResponseData> responses(List<AnswerObjectData> answers) {
        return answers.stream()
                .map(a -> ResponseData.builder().leftAnswerObject(a).rightAnswerObject(a).build())
                .toList();
    }

    static List<ResponseData> responses(AnswerObjectData... answers) {
        return responses(Arrays.asList(answers));
    }

    static final class BundleLocalizationService implements LocalizationService {
        @Override
        public @NotNull String getMessage(@NotNull String messageId, @NotNull Locale locale) {
            for (String bundle : BUNDLES) {
                try {
                    return ResourceBundle.getBundle(bundle, locale, ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES))
                            .getString(messageId);
                } catch (MissingResourceException ignored) {
                }
            }
            return messageId;
        }

        @Override
        public @NotNull String getMessage(@NotNull String messageId, @NotNull Language language) {
            return getMessage(messageId, Language.getLocale(language));
        }
    }

    static final class SeededRandomProvider implements RandomProvider {
        private RandomGenerator random = new SplittableRandom(0);

        @Override
        public RandomGenerator getRandom() {
            return random;
        }

        @Override
        public void reset(int seed) {
            random = new SplittableRandom(seed);
        }
    }
}
