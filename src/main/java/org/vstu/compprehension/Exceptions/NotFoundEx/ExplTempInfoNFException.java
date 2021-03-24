package org.vstu.compprehension.Exceptions.NotFoundEx;

public class ExplTempInfoNFException extends RuntimeException{
    public ExplTempInfoNFException(String message) {
        super(message);
    }

    public ExplTempInfoNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
