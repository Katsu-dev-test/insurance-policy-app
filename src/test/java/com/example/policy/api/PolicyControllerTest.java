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

    /** 実行日に依存しないよう、基準日を固定する。 */
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
                .andExpect(jsonPath("$.expiringSoon").value(true));
    }

    @Test
    void 満期ちょうど30日後の契約は警告フラグがtrue() throws Exception {
        mockMvc.perform(get("/api/policies/P0000002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryDate").value("2026-11-11"))
                .andExpect(jsonPath("$.expiringSoon").value(true));
    }

    @Test
    void 満期90日後の契約は警告フラグがfalse() throws Exception {
        mockMvc.perform(get("/api/policies/P0000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryDate").value("2027-01-10"))
                .andExpect(jsonPath("$.expiringSoon").value(false));
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
}
