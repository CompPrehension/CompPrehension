package org.vstu.compprehension.Exceptions.NotFoundEx;

public class CourseNFException extends RuntimeException {
    public CourseNFException(String message) {
        super(message);
    }

    public CourseNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
