package org.vstu.compprehension.moodle;


public class MoodleWsException extends RuntimeException {
    public MoodleWsException(String message) {
        super(message);
    }

    public MoodleWsException(String message, Throwable cause) {
        super(message, cause);
    }
}
