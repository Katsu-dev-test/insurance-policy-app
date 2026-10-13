package com.example.policy.api;

import com.example.policy.domain.PolicyStatus;
import java.time.LocalDate;

/**
 * 契約照会レスポンス。expiringSoon は満期日まで 30 日以内(満期日当日を含む)の場合に true。
 * status は契約ステータス(満期間近は含まない)。
 */
public record PolicyResponse(
        String policyNumber,
        String policyHolderName,
        LocalDate expiryDate,
        boolean expiringSoon,
        PolicyStatus status) {
}
