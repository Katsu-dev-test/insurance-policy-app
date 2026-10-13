package com.example.policy.domain;

/**
 * 契約ステータス。「満期間近」は含めない(満期日から判定する expiringSoon で扱う)。
 */
public enum PolicyStatus {
    /** 有効 */
    ACTIVE,
    /** 解約 */
    CANCELED,
    /** 失効 */
    LAPSED,
    /** 満期 */
    EXPIRED
}
