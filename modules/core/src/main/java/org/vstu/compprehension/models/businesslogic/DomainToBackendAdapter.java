package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.util.List;

/**
 * An "interface" between a {@link Domain} and a {@link Backend},
 * defining the behaviour of their interaction and the format of the data used in it.
 * This interface should be considered a part of a {@link Domain},
 * which defines the possibilities of its interaction with a number of {@link Backend}s
 * <br>
 * The meaning of type parameters are identical to that of the {@link Backend}
 */
public interface DomainToBackendAdapter<BackendInput, BackendOutput, Back extends Backend<BackendInput, BackendOutput>> {
    /**
     * Prepare data needed for the {@link Backend#judge} method using the necessary format
     */
    BackendInput prepareBackendInfoForJudge(
        Question question,
        List<ResponseEntity> responses,
        List<Tag> tags
    );

    /**
     * Interpret the results of the {@link Backend#judge} method
     * to provide user with the information on their responses
     */
    Domain.InterpretSentenceResult interpretJudgeOutput(
        Question judgedQuestion,
        BackendOutput backendOutput
    );


    /**
     * Prepare data needed for the {@link Backend#solve} method using the necessary format
     */
    BackendInput prepareBackendInfoForSolve(
        Question question,
        List<Tag> tags
    );

    /**
     * Insert the results of the {@link Backend#solve} method into the solved question
     */
    void updateQuestionAfterSolve(
        Question question,
        BackendOutput backendOutput
    );
}
