package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.util.List;
import java.util.Objects;

/**
 * An "interface" between a {@link Domain} and a {@link Backend},
 * defining the behaviour of their interaction and the format of the data used in it.
 * This interface should be considered a part of a {@link Domain},
 * which defines the possibilities of its interaction with a number of {@link Backend}s
 * <br>
 * The meaning of type parameters are identical to that of the {@link Backend}
 */
public abstract class DomainToBackendInterface<BackendInput, BackendOutput, Back extends Backend<BackendInput, BackendOutput>> {

    /**
     * The class of the backend this interface is defined for
     */
    protected final Class<Back> backendClass;

    public DomainToBackendInterface(Class<Back> backendClass) {
        this.backendClass = backendClass;
    }

    public Class<Back> getBackendClass() {
        return backendClass;
    }

    /**
     * Prepare data needed for the {@link Backend#judge} method using the necessary format
     */
    public abstract BackendInput prepareBackendInfoForJudge(
        Question question,
        List<ResponseEntity> responses,
        List<Tag> tags
    );

    /**
     * Interpret the results of the {@link Backend#judge} method
     * to provide user with the information on their responses
     */
    public abstract Domain.InterpretSentenceResult interpretJudgeOutput(
        Question judgedQuestion,
        BackendOutput backendOutput
    );


    /**
     * Prepare data needed for the {@link Backend#solve} method using the necessary format
     */
    public abstract BackendInput prepareBackendInfoForSolve(
        Question question,
        List<Tag> tags
    );

    /**
     * Insert the results of the {@link Backend#solve} method into the solved question
     */
    public abstract void updateQuestionAfterSolve(
        Question question,
        BackendOutput backendOutput
    );

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainToBackendInterface<?, ?, ?> that = (DomainToBackendInterface<?, ?, ?>) o;
        return Objects.equals(backendClass, that.backendClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backendClass);
    }
}
