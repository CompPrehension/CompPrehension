package org.vstu.compprehension.Exceptions.NotFoundEx;

public class MistakeNFException extends RuntimeException{
    public MistakeNFException(String message) {
        super(message);
    }

    public MistakeNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
