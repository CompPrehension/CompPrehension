package org.vstu.compprehension.Exceptions.NotFoundEx;

public class ExAttemptNFException extends RuntimeException {
    public ExAttemptNFException(String message) {
        super(message);
    }

    public ExAttemptNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
