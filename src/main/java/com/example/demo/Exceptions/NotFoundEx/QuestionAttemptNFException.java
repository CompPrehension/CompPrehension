package com.example.demo.Exceptions.NotFoundEx;

public class QuestionAttemptNFException extends RuntimeException {
    public QuestionAttemptNFException(String message) {
        super(message);
    }

    public QuestionAttemptNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
