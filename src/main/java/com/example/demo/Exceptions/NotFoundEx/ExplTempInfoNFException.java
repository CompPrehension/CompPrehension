package com.example.demo.Exceptions.NotFoundEx;

public class ExplTempInfoNFException extends RuntimeException{
    public ExplTempInfoNFException(String message) {
        super(message);
    }

    public ExplTempInfoNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
