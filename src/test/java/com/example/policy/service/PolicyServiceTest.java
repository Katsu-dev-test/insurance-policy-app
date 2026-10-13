package com.example.policy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.policy.domain.Policy;
import com.example.policy.domain.PolicyNotFoundException;
import com.example.policy.repository.PolicyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PolicyServiceTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(TOKYO).toInstant(), TOKYO);

    private final PolicyRepository repository = policyNumber ->
            "P0000001".equals(policyNumber)
                    ? Optional.of(new Policy("P0000001", "山田 太郎", TODAY.plusDays(10)))
                    : Optional.empty();

    private final PolicyService service = new PolicyService(repository, clock);

    private Policy policyExpiringIn(long days) {
        return new Policy("P0000009", "テスト 太郎", TODAY.plusDays(days));
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
        // UTC では 2026-10-11 15:00 だが、東京では 2026-10-12 00:00 になる
        Clock utcLateEvening = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), TOKYO);
        PolicyService s = new PolicyService(repository, utcLateEvening);
        // 東京基準で今日が 10/12 なので、11/11 はちょうど 30 日後
        assertTrue(s.isExpiringSoon(new Policy("P0000009", "テスト 太郎", LocalDate.of(2026, 11, 11))));
        assertFalse(s.isExpiringSoon(new Policy("P0000009", "テスト 太郎", LocalDate.of(2026, 11, 12))));
    }
}
