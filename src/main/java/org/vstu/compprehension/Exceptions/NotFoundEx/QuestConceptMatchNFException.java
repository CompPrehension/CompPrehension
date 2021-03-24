package org.vstu.compprehension.Exceptions.NotFoundEx;

public class QuestConceptMatchNFException extends RuntimeException {
    public QuestConceptMatchNFException(String message) {
        super(message);
    }

    public QuestConceptMatchNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
