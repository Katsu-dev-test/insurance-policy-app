package com.example.policy.repository;

import com.example.policy.domain.Policy;
import com.example.policy.domain.PolicyStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
                new Policy("P0000001", "山田 太郎", today.plusDays(10), PolicyStatus.ACTIVE, false),
                new Policy("P0000002", "佐藤 花子", today.plusDays(30), PolicyStatus.ACTIVE, false),
                new Policy("P0000003", "鈴木 一郎", today.plusDays(90), PolicyStatus.ACTIVE, false),
                // 同じ契約者名で複数契約・全ステータスを網羅する検索用データ
                new Policy("P0000004", "山田 太郎", today.minusDays(60), PolicyStatus.CANCELED, false),
                new Policy("P0000005", "山田 花子", today.minusDays(30), PolicyStatus.LAPSED, false),
                new Policy("P0000006", "田中 次郎", today.minusDays(1), PolicyStatus.EXPIRED, false),
                new Policy("P0000007", "佐藤 花子", today.plusDays(200), PolicyStatus.ACTIVE, false),
                new Policy("P0000008", "Smith John", today.plusDays(120), PolicyStatus.ACTIVE, false),
                // 更新済みで満期が近い契約(満期予告の対象外になることの確認用)
                new Policy("P0000009", "高橋 三郎", today.plusDays(15), PolicyStatus.ACTIVE, true))
            .stream()
            .collect(Collectors.toUnmodifiableMap(Policy::policyNumber, Function.identity()));
    }

    @Override
    public Optional<Policy> findByPolicyNumber(String policyNumber) {
        return Optional.ofNullable(policies.get(policyNumber));
    }

    @Override
    public List<Policy> search(String holderNameContains, PolicyStatus status) {
        String keyword = holderNameContains == null ? null : holderNameContains.toLowerCase(Locale.ROOT);
        return policies.values().stream()
                .filter(p -> keyword == null
                        || p.policyHolderName().toLowerCase(Locale.ROOT).contains(keyword))
                .filter(p -> status == null || p.status() == status)
                .sorted(Comparator.comparing(Policy::policyNumber))
                .toList();
    }
}
