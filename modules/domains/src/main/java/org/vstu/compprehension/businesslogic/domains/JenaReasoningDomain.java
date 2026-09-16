package org.vstu.compprehension.businesslogic.domains;

import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.NegativeLaw;
import org.vstu.compprehension.businesslogic.PositiveLaw;
import org.vstu.compprehension.services.RandomProvider;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.services.ExerciseAttemptDataService;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.data.question.QuestionContentData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.businesslogic.Tag;
import org.vstu.compprehension.businesslogic.backend.FactBackend;
import org.vstu.compprehension.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.enums.Language;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class JenaReasoningDomain extends DomainBase {
    private final FactBackend.Interface<JenaBackend> backendInterface;

    protected JenaReasoningDomain(DomainData domainData, RandomProvider randomProvider, DomainStructure structure) {
        super(domainData, randomProvider, structure);

        this.backendInterface = new FactBackend.Interface<>(this);
    }

    protected static List<Law> readLawsJson(InputStream inputStream) {
        Objects.requireNonNull(inputStream);
        var gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .registerTypeAdapterFactory(RuntimeTypeAdapterFactory
                        .of(Law.class, "positive")
                        .registerSubtype(PositiveLaw.class, "true")
                        .registerSubtype(NegativeLaw.class, "false"))
                .create();
        return Arrays.asList(gson.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), Law[].class));
    }

    @NotNull
    public String getBackendId() {
        return JenaBackend.BackendId;
    }

    public QuestionContentData solveQuestion(QuestionContentData question, List<Tag> tags) {
        var backend = new JenaBackend();
        return backendInterface.updateQuestionAfterSolve(
                question,
                backend.solve(backendInterface.prepareBackendInfoForSolve(question, tags))
        );
    }

    public InterpretSentenceResult judgeQuestion(QuestionData question, List<? extends AnswerData> responses, List<Tag> tags, Language language) {
        var backend = new JenaBackend();
        var output = backend.judge(backendInterface.prepareBackendInfoForJudge(question, responses, tags));
        return backendInterface.interpretJudgeOutput(question, output, language);
    }
}
