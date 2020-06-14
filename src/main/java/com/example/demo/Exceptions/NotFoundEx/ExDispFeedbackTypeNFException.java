package com.example.demo.Exceptions.NotFoundEx;

public class ExDispFeedbackTypeNFException extends RuntimeException {
    public ExDispFeedbackTypeNFException(String message) {
        super(message);
    }

    public ExDispFeedbackTypeNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
