package com.example.demo.Exceptions.NotFoundEx;

public class ExerciseNFException extends RuntimeException {
    public ExerciseNFException(String message) {
        super(message);
    }

    public ExerciseNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
