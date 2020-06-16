package com.example.demo.Exceptions;

import java.util.Map;

public class ExerciseFormException extends Exception {

    private Map<String, String> errors;

    public ExerciseFormException(String message) {
        super(message);
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
}