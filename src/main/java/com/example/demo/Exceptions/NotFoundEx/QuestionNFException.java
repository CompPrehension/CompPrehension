package com.example.demo.Exceptions.NotFoundEx;

public class QuestionNFException extends RuntimeException {
    public QuestionNFException(String message) {
        super(message);
    }

    public QuestionNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
