package org.vstu.compprehension.Exceptions.NotFoundEx;

public class TagNFException extends RuntimeException {
    public TagNFException(String message) {
        super(message);
    }

    public TagNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
