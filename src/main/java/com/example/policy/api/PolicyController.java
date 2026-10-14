package com.example.policy.api;

import com.example.policy.domain.Policy;
import com.example.policy.domain.PolicyStatus;
import com.example.policy.service.PolicyService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
@Validated
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    /** 契約番号から契約者名・満期日・満期予告フラグ・ステータスを返却する。契約番号は英大文字 1 文字 + 数字 7 桁。 */
    @GetMapping("/{policyNumber}")
    public PolicyResponse getPolicy(
            @PathVariable @Pattern(regexp = "^[A-Z][0-9]{7}$") String policyNumber) {
        Policy policy = policyService.getPolicy(policyNumber);
        return toResponse(policy);
    }

    /**
     * 契約者名(部分一致・大文字小文字区別なし)とステータスで契約を検索する。
     * 両方とも省略可能。両方省略した場合は全件を返す。
     * ステータスは ACTIVE / CANCELED / LAPSED / EXPIRED のいずれか。
     */
    @GetMapping
    public List<PolicyResponse> searchPolicies(
            @RequestParam(required = false) @Size(max = 100) String holderName,
            @RequestParam(required = false) String status) {
        PolicyStatus policyStatus = parseStatus(status);
        return policyService.search(holderName, policyStatus)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PolicyResponse toResponse(Policy policy) {
        return new PolicyResponse(
                policy.policyNumber(),
                policy.policyHolderName(),
                policy.expiryDate(),
                policyService.isExpiringSoon(policy),
                policy.status(),
                policy.renewed());
    }

    /**
     * status パラメータを PolicyStatus に変換する。null・空白のみなら未指定として null を返す。
     * 定義にない値は InvalidStatusException を投げ、GlobalExceptionHandler が 400 を返す。
     */
    private static PolicyStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PolicyStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException(status);
        }
    }
}
