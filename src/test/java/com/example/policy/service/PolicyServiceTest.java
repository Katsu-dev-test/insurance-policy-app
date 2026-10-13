package com.example.policy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.policy.domain.Policy;
import com.example.policy.domain.PolicyNotFoundException;
import com.example.policy.domain.PolicyStatus;
import com.example.policy.repository.PolicyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PolicyServiceTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(TOKYO).toInstant(), TOKYO);

    private final PolicyRepository repository = new PolicyRepository() {
        @Override
        public Optional<Policy> findByPolicyNumber(String policyNumber) {
            return "P0000001".equals(policyNumber)
                    ? Optional.of(new Policy("P0000001", "山田 太郎", TODAY.plusDays(10), PolicyStatus.ACTIVE))
                    : Optional.empty();
        }

        @Override
        public List<Policy> search(String holderNameContains, PolicyStatus status) {
            return List.of(
                    new Policy("P0000001", "山田 太郎", TODAY.plusDays(10), PolicyStatus.ACTIVE),
                    new Policy("P0000004", "山田 太郎", TODAY.minusDays(60), PolicyStatus.CANCELED));
        }
    };

    private final PolicyService service = new PolicyService(repository, clock);

    private Policy policyExpiringIn(long days) {
        return new Policy("P0000009", "テスト 太郎", TODAY.plusDays(days), PolicyStatus.ACTIVE);
    }

    @Test
    void 存在する契約を取得できる() {
        assertEquals("山田 太郎", service.getPolicy("P0000001").policyHolderName());
    }

    @Test
    void 存在しない契約は例外を投げる() {
        assertThrows(PolicyNotFoundException.class, () -> service.getPolicy("P0000002"));
    }

    @Test
    void 満期日が30日後なら警告対象() {
        assertTrue(service.isExpiringSoon(policyExpiringIn(30)));
    }

    @Test
    void 満期日が31日後なら警告対象外() {
        assertFalse(service.isExpiringSoon(policyExpiringIn(31)));
    }

    @Test
    void 満期日が29日後なら警告対象() {
        assertTrue(service.isExpiringSoon(policyExpiringIn(29)));
    }

    @Test
    void 満期日が当日なら警告対象() {
        assertTrue(service.isExpiringSoon(policyExpiringIn(0)));
    }

    @Test
    void 満期日を過ぎた契約は警告対象外() {
        assertFalse(service.isExpiringSoon(policyExpiringIn(-1)));
    }

    @Test
    void 基準日は東京時間の暦日で判定する() {
        Clock utcLateEvening = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), TOKYO);
        PolicyService s = new PolicyService(repository, utcLateEvening);
        assertTrue(s.isExpiringSoon(new Policy("P0000009", "テスト 太郎", LocalDate.of(2026, 11, 11), PolicyStatus.ACTIVE)));
        assertFalse(s.isExpiringSoon(new Policy("P0000009", "テスト 太郎", LocalDate.of(2026, 11, 12), PolicyStatus.ACTIVE)));
    }

    @Test
    void 解約済み契約でも満期日が近ければ警告対象() {
        Policy canceled = new Policy("P0000004", "山田 太郎", TODAY.plusDays(5), PolicyStatus.CANCELED);
        assertTrue(service.isExpiringSoon(canceled));
    }

    @Test
    void searchがリポジトリの結果を返す() {
        List<Policy> result = service.search("山田", null);
        assertEquals(2, result.size());
    }

    @Test
    void searchの契約者名が空白のみなら全件対象になる() {
        List<Policy> result = service.search("  ", null);
        assertEquals(2, result.size());
    }
}
