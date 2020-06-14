package com.example.demo.Exceptions.NotFoundEx;

public class UserActExNFException extends RuntimeException {
    public UserActExNFException(String message) {
        super(message);
    }

    public UserActExNFException(String message, Throwable cause) {
        super(message, cause);
    }
}
