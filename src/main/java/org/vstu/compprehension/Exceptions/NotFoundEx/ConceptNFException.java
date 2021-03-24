package org.vstu.compprehension.Exceptions.NotFoundEx;

public class ConceptNFException extends RuntimeException {
    public ConceptNFException(String message) {
        super(message);
    }

    public ConceptNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
