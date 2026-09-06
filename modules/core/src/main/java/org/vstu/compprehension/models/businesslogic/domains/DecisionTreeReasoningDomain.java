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
    protected DecisionTreeReasoningDomain(DomainEntity domainEntity, RandomProvider randomProvider) {
        super(domainEntity, randomProvider);
    }

    public abstract List<DomainSolvingModel> getDomainSolvingModels();
    
    public abstract DecisionTreeReasonerBackend.Interface getBackendInterface();

    @NotNull
    public String getBackendId() {
        return DecisionTreeReasonerBackend.BACKEND_ID;
    }

    public Question solveQuestion(Question question, List<Tag> tags) {
        var backend = new DecisionTreeReasonerBackend();
        var backendInterface = getBackendInterface();
        backendInterface.updateQuestionAfterSolve(
            question,
            backend.solve(backendInterface.prepareBackendInfoForSolve(question, tags))
        );
        return question;
    }

    public InterpretSentenceResult judgeQuestion(Question question, List<ResponseEntity> responses, List<Tag> tags) {
        var backend = new DecisionTreeReasonerBackend();
        var backendInterface = getBackendInterface();
        var output = backend.judge(backendInterface.prepareBackendInfoForJudge(question, responses, tags));
        return backendInterface.interpretJudgeOutput(question, output);
    }
}
