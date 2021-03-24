package org.vstu.compprehension.Exceptions.NotFoundEx;

public class DomainNFException extends RuntimeException {
    public DomainNFException(String message) {
        super(message);
    }

    public DomainNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
