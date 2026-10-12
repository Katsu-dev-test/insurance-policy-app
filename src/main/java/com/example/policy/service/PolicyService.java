package com.example.policy.service;

import com.example.policy.domain.Policy;
import com.example.policy.domain.PolicyNotFoundException;
import com.example.policy.repository.PolicyRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    /** 満期予告の対象とする日数(満期日までこの日数以内)。 */
    static final long EXPIRY_WARNING_DAYS = 30;

    private final PolicyRepository policyRepository;
    private final Clock clock;

    public PolicyService(PolicyRepository policyRepository, Clock clock) {
        this.policyRepository = policyRepository;
        this.clock = clock;
    }

    public Policy getPolicy(String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new PolicyNotFoundException(policyNumber));
    }

    /**
     * 満期日が本日から 0〜30 日後(両端含む)なら true。満期日を過ぎた契約は対象外。
     */
    public boolean isExpiringSoon(Policy policy) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(clock), policy.expiryDate());
        return days >= 0 && days <= EXPIRY_WARNING_DAYS;
    }
}
