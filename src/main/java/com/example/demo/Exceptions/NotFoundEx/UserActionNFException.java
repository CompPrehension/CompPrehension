package com.example.demo.Exceptions.NotFoundEx;

public class UserActionNFException extends RuntimeException {
    public UserActionNFException(String message) {
        super(message);
    }

    public UserActionNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
