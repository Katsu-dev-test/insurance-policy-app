package com.example.policy.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.policy.domain.Policy;
import com.example.policy.domain.PolicyNotFoundException;
import com.example.policy.domain.PolicyStatus;
import com.example.policy.service.PolicyService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PolicyControllerUnitTest {

    private static final LocalDate EXPIRY = LocalDate.of(2026, 10, 22);

    private final PolicyService policyService = mock(PolicyService.class);
    private final PolicyController controller = new PolicyController(policyService);

    @Test
    void 契約の内容をレスポンスに詰め替える() {
        Policy policy = new Policy("P0000001", "山田 太郎", EXPIRY, PolicyStatus.ACTIVE, false);
        when(policyService.getPolicy("P0000001")).thenReturn(policy);
        when(policyService.isExpiringSoon(policy)).thenReturn(true);

        PolicyResponse response = controller.getPolicy("P0000001");

        assertEquals("P0000001", response.policyNumber());
        assertEquals("山田 太郎", response.policyHolderName());
        assertEquals(EXPIRY, response.expiryDate());
        assertTrue(response.expiringSoon());
        assertEquals(PolicyStatus.ACTIVE, response.status());
    }

    @Test
    void 満期予告対象外ならフラグはfalse() {
        Policy policy = new Policy("P0000003", "鈴木 一郎", EXPIRY, PolicyStatus.ACTIVE, false);
        when(policyService.getPolicy("P0000003")).thenReturn(policy);
        when(policyService.isExpiringSoon(policy)).thenReturn(false);

        assertFalse(controller.getPolicy("P0000003").expiringSoon());
    }

    @Test
    void 契約が存在しない場合はサービスの例外をそのまま伝播する() {
        when(policyService.getPolicy("P9999999")).thenThrow(new PolicyNotFoundException("P9999999"));

        assertThrows(PolicyNotFoundException.class, () -> controller.getPolicy("P9999999"));
    }
}
