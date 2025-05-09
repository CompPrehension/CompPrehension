package org.vstu.compprehension.models.businesslogic.domains;

import its.model.DomainSolvingModel;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.List;

public abstract class DecisionTreeReasoningDomain extends DomainBase {
    private DecisionTreeReasonerBackend.Interface backendInterface;

    protected DecisionTreeReasoningDomain(DomainEntity domainEntity, RandomProvider randomProvider, DecisionTreeReasonerBackend.Interface backendInterface) {
        super(domainEntity, randomProvider);

        this.backendInterface = backendInterface;
    }

    public abstract DomainSolvingModel getDomainSolvingModel();

    @NotNull
    public String getSolvingBackendId() {
        return DecisionTreeReasonerBackend.BACKEND_ID;
    }

    @NotNull
    public String getJudgingBackendId() {
        return DecisionTreeReasonerBackend.BACKEND_ID;
    }

    protected void setBackendInterface(DecisionTreeReasonerBackend.Interface backendInterface) {
        this.backendInterface = backendInterface;
    }

    public Question solveQuestion(Question question, List<Tag> tags) {
        var backend = new DecisionTreeReasonerBackend();
        backendInterface.updateQuestionAfterSolve(
            question,
            backend.solve(backendInterface.prepareBackendInfoForSolve(question, tags))
        );
        return question;
    }

    public InterpretSentenceResult judgeQuestion(Question question, List<ResponseEntity> responses, List<Tag> tags) {
        var backend = new DecisionTreeReasonerBackend();
        var output = backend.judge(backendInterface.prepareBackendInfoForJudge(question, responses, tags));
        return backendInterface.interpretJudgeOutput(question, output);
    }
}
