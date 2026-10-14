package com.example.policy.domain;

import java.time.LocalDate;

/**
 * 契約。サンプルのため契約番号・契約者名・満期日・ステータス・更新済みフラグのみを保持する。
 * renewed は更新手続きが済んだ契約で true。ステータスは変わらず(ACTIVE のまま)、満期予告の判定にのみ使う。
 */
public record Policy(
        String policyNumber,
        String policyHolderName,
        LocalDate expiryDate,
        PolicyStatus status,
        boolean renewed) {
}
