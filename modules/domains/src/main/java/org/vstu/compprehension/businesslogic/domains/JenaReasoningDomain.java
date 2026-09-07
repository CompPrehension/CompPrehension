package org.vstu.compprehension.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.services.SupplementaryStepService;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.services.ExerciseAttemptService;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.businesslogic.Question;
import org.vstu.compprehension.businesslogic.Tag;
import org.vstu.compprehension.businesslogic.backend.FactBackend;
import org.vstu.compprehension.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.data.enums.Language;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.List;

public abstract class JenaReasoningDomain extends DomainBase {
    private final FactBackend.Interface<JenaBackend> backendInterface;

    protected JenaReasoningDomain(DomainData domainData, RandomProvider randomProvider,
            ExerciseAttemptService exerciseAttemptService,
            SupplementaryStepService supplementaryStepService) {
        super(domainData, randomProvider, exerciseAttemptService, supplementaryStepService);

        this.backendInterface = new FactBackend.Interface<>(this);
    }

    @NotNull
    public String getBackendId() {
        return JenaBackend.BackendId;
    }

    public Question solveQuestion(Question question, List<Tag> tags) {
        var backend = new JenaBackend();
        backendInterface.updateQuestionAfterSolve(
                question,
                backend.solve(backendInterface.prepareBackendInfoForSolve(question, tags))
        );
        return question;
    }

    public InterpretSentenceResult judgeQuestion(Question question, List<ResponseData> responses, List<Tag> tags, Language language) {
        var backend = new JenaBackend();
        var output = backend.judge(backendInterface.prepareBackendInfoForJudge(question, responses, tags));
        return backendInterface.interpretJudgeOutput(question, output, language);
    }
}
