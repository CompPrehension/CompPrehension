package org.vstu.compprehension.models.businesslogic.backend;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.models.businesslogic.DomainToBackendInterface;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.domains.Domain;

import java.util.List;

/**
 * A reasoning backend that determines the process a question gets judged/solved.
 * Is used via {@link QuestionService} to judge/solve a question,
 * which produces output that can then be interpreted by a {@link Domain}.<br>
 * A single {@link Domain} may interact with multiple Backends by way of {@link DomainToBackendInterface}s
 * @param <BackendInput> determines the format the information about a question is fed into the backend
 * @param <BackendOutput> determines the format of the backend's judge/solve output to be interpreted
 */
public interface Backend<BackendInput, BackendOutput> {

    /**
     * A name-like id to represent this backend and retrieve it by
     */
    @NotNull String getBackendId();

    /**
     * Judge a question, providing information on if the user answered it correctly
     * It is assumed that the argument of this method contains all the neccessary information
     * about the question and the user's response(s)
     * (being prepared by the {@link DomainToBackendInterface#prepareBackendInfoForJudge} method)
     * The output of this is then passed into {@link DomainToBackendInterface#interpretJudgeOutput}
     */
    BackendOutput judge(BackendInput questionData);

    /**
     * Solve a question, adding some helpful information to it
     * which can then be stored or used later for the {@link #judge}
     * It is assumed that the argument of this method contains all the neccessary information about the question
     * (being prepared by the {@link DomainToBackendInterface#prepareBackendInfoForSolve} method)
     * The output of this is then passed into {@link DomainToBackendInterface#updateQuestionAfterSolve}
     */
    BackendOutput solve(BackendInput questionData);


    /**
     * If this backend is a decorator, then return the most nested backend (the actual object doing the reasoning)
     * Otherwise, return this
     */
    default Backend<BackendInput, BackendOutput> getActualBackend(){
        return this;
    }
}
