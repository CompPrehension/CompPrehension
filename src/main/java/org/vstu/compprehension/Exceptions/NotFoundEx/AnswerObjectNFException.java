package org.vstu.compprehension.Exceptions.NotFoundEx;

public class AnswerObjectNFException extends RuntimeException{
    public AnswerObjectNFException(String message) {
        super(message);
    }

    public AnswerObjectNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
