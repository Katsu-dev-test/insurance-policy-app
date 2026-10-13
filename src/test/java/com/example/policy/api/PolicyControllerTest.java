package com.example.policy.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PolicyControllerTest.FixedClockConfig.class)
class PolicyControllerTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(TODAY.atStartOfDay(TOKYO).toInstant(), TOKYO);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    // ── 既存機能の回帰テスト ──────────────────────────────────────────

    @Test
    void 契約番号を指定すると契約者名を返す() throws Exception {
        mockMvc.perform(get("/api/policies/P0000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber").value("P0000001"))
                .andExpect(jsonPath("$.policyHolderName").value("山田 太郎"));
    }

    @Test
    void 満期10日後の契約は警告フラグがtrue() throws Exception {
        mockMvc.perform(get("/api/policies/P0000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryDate").value("2026-10-22"))
                .andExpect(jsonPath("$.expiringSoon").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void 満期ちょうど30日後の契約は警告フラグがtrue() throws Exception {
        mockMvc.perform(get("/api/policies/P0000002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryDate").value("2026-11-11"))
                .andExpect(jsonPath("$.expiringSoon").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void 満期90日後の契約は警告フラグがfalse() throws Exception {
        mockMvc.perform(get("/api/policies/P0000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryDate").value("2027-01-10"))
                .andExpect(jsonPath("$.expiringSoon").value(false))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void 存在しない契約番号は404を返す() throws Exception {
        mockMvc.perform(get("/api/policies/P9999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Policy not found"));
    }

    @Test
    void 契約番号の形式が不正な場合は400を返す() throws Exception {
        mockMvc.perform(get("/api/policies/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid request"));
    }

    @Test
    void エラー応答に内部情報を含めない() throws Exception {
        mockMvc.perform(get("/api/policies/invalid"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Exception"))));
    }

    // ── 検索機能のテスト ──────────────────────────────────────────────

    @Test
    void 条件未指定で全件を返す() throws Exception {
        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void 契約者名の部分一致で絞り込める() throws Exception {
        mockMvc.perform(get("/api/policies").param("holderName", "山田"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].policyNumber").value("P0000001"))
                .andExpect(jsonPath("$[1].policyNumber").value("P0000004"))
                .andExpect(jsonPath("$[2].policyNumber").value("P0000005"));
    }

    @Test
    void 同じ契約者名で複数の契約を取得できる() throws Exception {
        mockMvc.perform(get("/api/policies").param("holderName", "山田 太郎"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].policyNumber").value("P0000001"))
                .andExpect(jsonPath("$[1].policyNumber").value("P0000004"));
    }

    @Test
    void ステータスのみで絞り込める() throws Exception {
        mockMvc.perform(get("/api/policies").param("status", "CANCELED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].policyNumber").value("P0000004"))
                .andExpect(jsonPath("$[0].status").value("CANCELED"));
    }

    @Test
    void 契約者名とステータスのAND条件で絞り込める() throws Exception {
        mockMvc.perform(get("/api/policies")
                        .param("holderName", "佐藤")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].policyNumber").value("P0000002"))
                .andExpect(jsonPath("$[1].policyNumber").value("P0000007"));
    }

    @Test
    void 該当0件のときは空の一覧を返す() throws Exception {
        mockMvc.perform(get("/api/policies").param("holderName", "存在しない契約者"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 不正なステータス値は400を返す() throws Exception {
        mockMvc.perform(get("/api/policies").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid request"));
    }

    @Test
    void statusは大文字小文字を区別しない() throws Exception {
        mockMvc.perform(get("/api/policies").param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void 検索結果に満期予告フラグが含まれる() throws Exception {
        mockMvc.perform(get("/api/policies").param("holderName", "山田 太郎"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].expiringSoon").value(true))
                .andExpect(jsonPath("$[1].expiringSoon").value(false));
    }

    @Test
    void 検索結果は契約番号の昇順() throws Exception {
        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyNumber").value("P0000001"))
                .andExpect(jsonPath("$[7].policyNumber").value("P0000008"));
    }

    @Test
    void 契約者名の検索は大文字小文字を区別しない() throws Exception {
        mockMvc.perform(get("/api/policies").param("holderName", "SMITH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].policyNumber").value("P0000008"));
    }
}
