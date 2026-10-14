package com.example.policy.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.policy.domain.Policy;
import com.example.policy.domain.PolicyStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryPolicyRepositoryTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    private final InMemoryPolicyRepository repository =
            new InMemoryPolicyRepository(Clock.fixed(TODAY.atStartOfDay(TOKYO).toInstant(), TOKYO));

    @Test
    void 登録済みの契約番号で契約を取得できる() {
        Policy policy = repository.findByPolicyNumber("P0000001").orElseThrow();

        assertEquals("P0000001", policy.policyNumber());
        assertEquals("山田 太郎", policy.policyHolderName());
        assertEquals(PolicyStatus.ACTIVE, policy.status());
    }

    @Test
    void 未登録の契約番号は空を返す() {
        assertTrue(repository.findByPolicyNumber("P9999999").isEmpty());
    }

    @Test
    void 満期日は基準日からの相対日数で生成される() {
        assertEquals(TODAY.plusDays(10), repository.findByPolicyNumber("P0000001").orElseThrow().expiryDate());
        assertEquals(TODAY.plusDays(30), repository.findByPolicyNumber("P0000002").orElseThrow().expiryDate());
        assertEquals(TODAY.plusDays(90), repository.findByPolicyNumber("P0000003").orElseThrow().expiryDate());
    }

    @Test
    void 条件なしで全件を返す() {
        assertEquals(9, repository.search(null, null).size());
    }

    @Test
    void 契約者名の部分一致で絞り込む() {
        List<Policy> result = repository.search("山田", null);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(p -> p.policyHolderName().contains("山田")));
    }

    @Test
    void ステータスで絞り込む() {
        List<Policy> result = repository.search(null, PolicyStatus.CANCELED);
        assertEquals(1, result.size());
        assertEquals(PolicyStatus.CANCELED, result.get(0).status());
    }

    @Test
    void 契約者名とステータスのAND絞り込み() {
        List<Policy> result = repository.search("佐藤", PolicyStatus.ACTIVE);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.policyHolderName().contains("佐藤")));
        assertTrue(result.stream().allMatch(p -> p.status() == PolicyStatus.ACTIVE));
    }

    @Test
    void 該当0件は空リストを返す() {
        assertTrue(repository.search("存在しない", null).isEmpty());
    }

    @Test
    void 検索結果は契約番号の昇順() {
        List<Policy> result = repository.search(null, null);
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).policyNumber().compareTo(result.get(i + 1).policyNumber()) < 0);
        }
    }

    @Test
    void 契約者名の大文字小文字を区別しない() {
        assertEquals("P0000008", repository.search("smith", null).get(0).policyNumber());
        assertEquals("P0000008", repository.search("SMITH", null).get(0).policyNumber());
        assertEquals("P0000008", repository.search("sMiTh jOhN", null).get(0).policyNumber());
    }
}
