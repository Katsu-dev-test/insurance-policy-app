package com.example.policy.api;

import java.time.LocalDate;

/**
 * 契約照会レスポンス。expiringSoon は満期日まで 30 日以内(満期日当日を含む)の場合に true。
 */
public record PolicyResponse(
        String policyNumber,
        String policyHolderName,
        LocalDate expiryDate,
        boolean expiringSoon) {
}
