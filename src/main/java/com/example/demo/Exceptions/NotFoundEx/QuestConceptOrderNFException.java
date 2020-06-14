package com.example.demo.Exceptions.NotFoundEx;

public class QuestConceptOrderNFException extends RuntimeException {
    public QuestConceptOrderNFException(String message) {
        super(message);
    }

    public QuestConceptOrderNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
