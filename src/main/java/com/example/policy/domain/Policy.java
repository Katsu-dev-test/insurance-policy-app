package com.example.policy.domain;

import java.time.LocalDate;

/**
 * 契約。サンプルのため契約番号・契約者名・満期日・ステータスのみを保持する。
 */
public record Policy(
        String policyNumber,
        String policyHolderName,
        LocalDate expiryDate,
        PolicyStatus status) {
}
