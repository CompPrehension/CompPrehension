package com.example.demo.Exceptions.NotFoundEx;

public class QuestConceptMatchNFException extends RuntimeException {
    public QuestConceptMatchNFException(String message) {
        super(message);
    }

    public QuestConceptMatchNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
