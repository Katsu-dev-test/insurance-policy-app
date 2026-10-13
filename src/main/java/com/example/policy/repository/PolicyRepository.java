package com.example.policy.repository;

import com.example.policy.domain.Policy;
import com.example.policy.domain.PolicyStatus;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    /**
     * 契約者名(部分一致・大文字小文字を区別しない)とステータス(完全一致)で検索する。
     * 各条件は null の場合は絞り込みに使わない。両方指定された場合は AND 条件。
     * 結果は契約番号の昇順。
     */
    List<Policy> search(String holderNameContains, PolicyStatus status);
}
