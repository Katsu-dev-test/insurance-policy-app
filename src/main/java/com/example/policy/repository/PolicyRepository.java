package com.example.policy.repository;

import com.example.policy.domain.Policy;
import java.util.Optional;

public interface PolicyRepository {

    Optional<Policy> findByPolicyNumber(String policyNumber);
}
