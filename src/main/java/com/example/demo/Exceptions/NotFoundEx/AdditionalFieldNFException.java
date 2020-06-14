package com.example.demo.Exceptions.NotFoundEx;

public class AdditionalFieldNFException extends RuntimeException {
    public AdditionalFieldNFException(String message) {
        super(message);
    }

    public AdditionalFieldNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
