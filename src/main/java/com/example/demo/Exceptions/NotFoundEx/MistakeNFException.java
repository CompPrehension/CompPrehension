package com.example.demo.Exceptions.NotFoundEx;

public class MistakeNFException extends RuntimeException{
    public MistakeNFException(String message) {
        super(message);
    }

    public MistakeNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
