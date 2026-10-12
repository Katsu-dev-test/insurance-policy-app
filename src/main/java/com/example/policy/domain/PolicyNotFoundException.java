package com.example.policy.domain;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String policyNumber) {
        super("Policy not found: " + policyNumber);
    }
}
