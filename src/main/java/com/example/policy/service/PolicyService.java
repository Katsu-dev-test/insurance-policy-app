package com.example.policy.service;

import com.example.policy.domain.Policy;
import com.example.policy.domain.PolicyNotFoundException;
import com.example.policy.domain.PolicyStatus;
import com.example.policy.repository.PolicyRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
     * 契約者名(部分一致)とステータスで契約を検索する。
     * 契約者名が null・空文字・空白のみの場合は未指定として扱う。ステータスが null なら全ステータスが対象。
     */
    public List<Policy> search(String holderName, PolicyStatus status) {
        String keyword = (holderName == null || holderName.isBlank()) ? null : holderName.strip();
        return policyRepository.search(keyword, status);
    }

    /**
     * ACTIVE の契約で、満期日が本日から 0〜30 日後(両端含む)なら true。
     * 満期日を過ぎた契約、および CANCELED / LAPSED / EXPIRED の契約は対象外。
     */
    public boolean isExpiringSoon(Policy policy) {
        if (policy.status() != PolicyStatus.ACTIVE) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(clock), policy.expiryDate());
        return days >= 0 && days <= EXPIRY_WARNING_DAYS;
    }
}
