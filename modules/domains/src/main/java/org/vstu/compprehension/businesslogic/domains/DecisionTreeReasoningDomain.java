package org.vstu.compprehension.businesslogic.domains;

import its.model.DomainSolvingModel;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.services.RandomProvider;
import org.vstu.compprehension.services.SupplementaryStepDataService;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.services.ExerciseAttemptDataService;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.businesslogic.Question;
import org.vstu.compprehension.businesslogic.Tag;
import org.vstu.compprehension.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.enums.Language;

import java.util.List;

public abstract class DecisionTreeReasoningDomain extends DomainBase {
    protected DecisionTreeReasoningDomain(DomainData domainData, RandomProvider randomProvider,
            ExerciseAttemptDataService exerciseAttemptService,
            SupplementaryStepDataService supplementaryStepService) {
        super(domainData, randomProvider, exerciseAttemptService, supplementaryStepService);
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

    public InterpretSentenceResult judgeQuestion(Question question, List<ResponseData> responses, List<Tag> tags, Language language) {
        var backend = new DecisionTreeReasonerBackend();
        var backendInterface = getBackendInterface();
        var output = backend.judge(backendInterface.prepareBackendInfoForJudge(question, responses, tags));
        return backendInterface.interpretJudgeOutput(question, output, language);
    }
}
