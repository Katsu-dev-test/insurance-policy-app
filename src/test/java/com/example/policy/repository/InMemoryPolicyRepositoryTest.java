package com.example.policy.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.policy.domain.Policy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
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
}
