package com.example.policy.api;

import com.example.policy.domain.Policy;
import com.example.policy.service.PolicyService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
@Validated
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    /** 契約番号から契約者名・満期日・満期予告フラグを返却する。契約番号は英大文字 1 文字 + 数字 7 桁。 */
    @GetMapping("/{policyNumber}")
    public PolicyResponse getPolicy(
            @PathVariable @Pattern(regexp = "^[A-Z][0-9]{7}$") String policyNumber) {
        Policy policy = policyService.getPolicy(policyNumber);
        return new PolicyResponse(
                policy.policyNumber(),
                policy.policyHolderName(),
                policy.expiryDate(),
                policyService.isExpiringSoon(policy));
    }
}
