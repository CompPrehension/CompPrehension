package com.example.demo.Exceptions.NotFoundEx;

public class GroupNFException extends RuntimeException {
    public GroupNFException(String message) {
        super(message);
    }

    public GroupNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
