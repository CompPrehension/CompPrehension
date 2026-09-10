package org.vstu.compprehension.businesslogic.domains;

import io.brookite.termannotations.DomainTermDictionary;
import its.model.DomainSolvingModel;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.QuestionContentData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.businesslogic.Tag;
import org.vstu.compprehension.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.services.RandomProvider;

import java.util.List;
import java.util.Optional;

public abstract class DecisionTreeReasoningDomain extends DomainBase {

    protected DecisionTreeReasoningDomain(DomainData domainData, RandomProvider randomProvider) {
        super(domainData, randomProvider);
    }

    public abstract List<DomainSolvingModel> getDomainSolvingModels();
    
    public abstract DecisionTreeReasonerBackend.Interface getBackendInterface();

    @NotNull
    public String getBackendId() {
        return DecisionTreeReasonerBackend.BACKEND_ID;
    }

    @Override
    public boolean requiresSolving() {
        return false;
    }

    public QuestionContentData solveQuestion(QuestionContentData question, List<Tag> tags) {
        // Дерево решений считает всё при оценке ответа, отдельного решения вопроса нет
        return question;
    }

    public InterpretSentenceResult judgeQuestion(QuestionData question, List<? extends AnswerData> responses, List<Tag> tags, Language language) {
        var backend = new DecisionTreeReasonerBackend();
        var backendInterface = getBackendInterface();
        var output = backend.judge(backendInterface.prepareBackendInfoForJudge(question, responses, tags));
        return backendInterface.interpretJudgeOutput(question, output, language);
    }

    public Optional<DomainTermDictionary> getTermDictionary() {
        return Optional.empty();
    }
}
