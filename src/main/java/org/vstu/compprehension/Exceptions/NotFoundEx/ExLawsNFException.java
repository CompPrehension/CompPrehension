package org.vstu.compprehension.Exceptions.NotFoundEx;

public class ExLawsNFException extends RuntimeException {
    public ExLawsNFException(String message) {
        super(message);
    }

    public ExLawsNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
