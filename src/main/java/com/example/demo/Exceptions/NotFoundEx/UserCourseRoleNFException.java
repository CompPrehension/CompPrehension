package com.example.demo.Exceptions.NotFoundEx;

public class UserCourseRoleNFException extends RuntimeException {
    public UserCourseRoleNFException(String message) {
        super(message);
    }

    public UserCourseRoleNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
