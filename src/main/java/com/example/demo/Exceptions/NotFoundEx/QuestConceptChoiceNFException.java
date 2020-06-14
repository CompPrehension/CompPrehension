package com.example.demo.Exceptions.NotFoundEx;

public class QuestConceptChoiceNFException extends RuntimeException{
    public QuestConceptChoiceNFException(String message) {
        super(message);
    }

    public QuestConceptChoiceNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
