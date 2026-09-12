package org.vstu.compprehension.businesslogic.domains;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.FeedbackData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.services.LocalizationService;
import org.vstu.compprehension.services.RandomProvider;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

public final class DomainFixtures {

    private DomainFixtures() {
    }

    public static QuestionMetadataWithData bankRecord(String resource) {
        try (var stream = DomainFixtures.class.getClassLoader().getResourceAsStream(resource)) {
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

    public static List<ResponseData> responses(List<AnswerObjectData> answers) {
        return answers.stream()
                .map(a -> ResponseData.builder().leftAnswerObject(a).rightAnswerObject(a).build())
                .toList();
    }

    public static List<ResponseData> responses(AnswerObjectData... answers) {
        return responses(Arrays.asList(answers));
    }

    public static ViolationData violation(String lawName) {
        var violation = new ViolationData();
        violation.setLawName(lawName);
        return violation;
    }

    public static QuestionInteractionData interaction(long id, List<AnswerObjectData> answers,
                                                      List<ViolationData> violations, int interactionsLeft) {
        var responses = answers.stream()
                .map(a -> ResponseData.builder()
                        .leftAnswerObject(a)
                        .rightAnswerObject(a)
                        .interactionHasViolations(!violations.isEmpty())
                        .build())
                .toList();
        return QuestionInteractionData.builder()
                .id(id)
                .interactionType(InteractionType.SEND_RESPONSE)
                .responses(new ArrayList<>(responses))
                .violations(new ArrayList<>(violations))
                .feedback(FeedbackData.builder().interactionsLeft(interactionsLeft).build())
                .build();
    }

    public static QuestionData withCorrectSteps(QuestionData question, List<AnswerObjectData> given, int totalSteps) {
        var result = question;
        for (int step = 1; step <= given.size(); step++) {
            result = result.withInteraction(interaction(step, given.subList(0, step), List.of(), totalSteps - step));
        }
        return result;
    }

    public static final class BundleLocalizationService implements LocalizationService {
        private final List<String> bundles;

        public BundleLocalizationService(String... bundles) {
            this.bundles = List.of(bundles);
        }

        @Override
        public @NotNull String getMessage(@NotNull String messageId, @NotNull Locale locale) {
            for (String bundle : bundles) {
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

    public static final class SeededRandomProvider implements RandomProvider {
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
