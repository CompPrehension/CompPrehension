package org.vstu.compprehension.models.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.Service.SupplementaryStepService;
import org.vstu.compprehension.models.data.ResponseData;
import org.vstu.compprehension.Service.ExerciseAttemptService;
import org.vstu.compprehension.models.data.DomainData;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.backend.FactBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.entities.DomainEntity;
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

    public InterpretSentenceResult judgeQuestion(Question question, List<ResponseData> responses, List<Tag> tags) {
        var backend = new JenaBackend();
        var output = backend.judge(backendInterface.prepareBackendInfoForJudge(question, responses, tags));
        return backendInterface.interpretJudgeOutput(question, output);
    }
}
