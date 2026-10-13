package com.example.policy.api;

public class InvalidStatusException extends RuntimeException {

    public InvalidStatusException(String value) {
        super("Invalid status value: " + value);
    }
}
