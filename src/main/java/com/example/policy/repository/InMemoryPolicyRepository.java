package com.example.policy.repository;

import com.example.policy.domain.Policy;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * サンプル用のインメモリ実装。実運用では DB アクセスの実装に置き換える。
 * 満期日は起動時の本日からの相対日数で生成し、いつ実行しても警告の有無が変わらないようにしている。
 */
@Repository
public class InMemoryPolicyRepository implements PolicyRepository {

    private final Map<String, Policy> policies;

    public InMemoryPolicyRepository(Clock clock) {
        LocalDate today = LocalDate.now(clock);
        this.policies = List.of(
                new Policy("P0000001", "山田 太郎", today.plusDays(10)),
                new Policy("P0000002", "佐藤 花子", today.plusDays(30)),
                new Policy("P0000003", "鈴木 一郎", today.plusDays(90)))
            .stream()
            .collect(Collectors.toUnmodifiableMap(Policy::policyNumber, Function.identity()));
    }

    @Override
    public Optional<Policy> findByPolicyNumber(String policyNumber) {
        return Optional.ofNullable(policies.get(policyNumber));
    }
}
