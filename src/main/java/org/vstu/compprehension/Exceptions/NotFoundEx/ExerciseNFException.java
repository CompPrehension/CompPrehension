package org.vstu.compprehension.Exceptions.NotFoundEx;

public class ExerciseNFException extends RuntimeException {
    public ExerciseNFException(String message) {
        super(message);
    }

    public ExerciseNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
