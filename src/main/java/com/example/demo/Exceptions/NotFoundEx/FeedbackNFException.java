package com.example.demo.Exceptions.NotFoundEx;

public class FeedbackNFException extends RuntimeException {
    public FeedbackNFException(String message) {
        super(message);
    }

    public FeedbackNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
