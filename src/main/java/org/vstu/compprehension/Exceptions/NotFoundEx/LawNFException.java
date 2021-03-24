package org.vstu.compprehension.Exceptions.NotFoundEx;

public class LawNFException extends RuntimeException {
    public LawNFException(String message) {
        super(message);
    }

    public LawNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
