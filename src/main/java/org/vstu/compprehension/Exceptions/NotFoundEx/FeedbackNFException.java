package org.vstu.compprehension.Exceptions.NotFoundEx;

public class FeedbackNFException extends RuntimeException {
    public FeedbackNFException(String message) {
        super(message);
    }

    public FeedbackNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
