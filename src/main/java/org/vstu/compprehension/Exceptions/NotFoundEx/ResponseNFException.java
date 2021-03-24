package org.vstu.compprehension.Exceptions.NotFoundEx;

public class ResponseNFException extends RuntimeException{
    public ResponseNFException(String message) {
        super(message);
    }

    public ResponseNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
