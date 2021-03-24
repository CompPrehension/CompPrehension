package org.vstu.compprehension.Exceptions.NotFoundEx;

public class UserNFException extends RuntimeException {
    public UserNFException(String message) {
        super(message);
    }

    public UserNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
