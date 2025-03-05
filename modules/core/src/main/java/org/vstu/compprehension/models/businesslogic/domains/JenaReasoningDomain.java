package org.vstu.compprehension.models.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.backend.FactBackend;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.List;

public abstract class JenaReasoningDomain extends DomainBase {
    private final FactBackend.Interface<JenaBackend> backendInterface;

    protected JenaReasoningDomain(DomainEntity domainEntity, RandomProvider randomProvider) {
        super(domainEntity, randomProvider);

        this.backendInterface = new FactBackend.Interface<>(JenaBackend.class, this, JenaBackend.BackendId);
    }

    @NotNull
    public String getSolvingBackendId() {
        return JenaBackend.BackendId;
    }

    /**
     * Get domain-defined backend id, which determines the backend used to JUDGE this domain's questions. By default, the same as solving domain.
     */
    @NotNull
    public String getJudgingBackendId() {
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

    public InterpretSentenceResult judgeQuestion(Question question, List<ResponseEntity> responses, List<Tag> tags) {
        var backend = new JenaBackend();
        var output = backend.judge(backendInterface.prepareBackendInfoForJudge(question, responses, tags));
        return backendInterface.interpretJudgeOutput(question, output);
    }
}
