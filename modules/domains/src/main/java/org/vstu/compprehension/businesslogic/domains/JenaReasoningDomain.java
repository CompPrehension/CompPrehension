package org.vstu.compprehension.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
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

import java.util.List;

public abstract class JenaReasoningDomain extends DomainBase {
    private final FactBackend.Interface<JenaBackend> backendInterface;

    protected JenaReasoningDomain(DomainData domainData, RandomProvider randomProvider) {
        super(domainData, randomProvider);

        this.backendInterface = new FactBackend.Interface<>(this);
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
