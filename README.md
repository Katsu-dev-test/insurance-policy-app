# insurance-policy-app

契約番号を指定して契約者名を返却する、Spring Boot 製の契約照会 API サンプルです。
契約満期日が 30 日以内の契約には、API の警告フラグと照会画面の警告表示で注意を促します(Issue #1)。

## 画面

起動後に `http://127.0.0.1:8080/` を開き、契約番号を入力して照会します。満期が 30 日以内の契約では警告バナーが表示されます。

## 前提

- JDK 25
- Maven 3.9 以上
- Spring Boot 4.1.1

## 実行

```bash
mvn spring-boot:run
```

サービスは `127.0.0.1:8080` のみで待ち受けます(外部公開しません)。

## テスト

```bash
mvn verify
```

## API

### `GET /api/policies/{policyNumber}`

契約番号は英大文字 1 文字 + 数字 7 桁(例: `P0000001`)。

```bash
curl http://127.0.0.1:8080/api/policies/P0000001
```

```json
{"policyNumber":"P0000001","policyHolderName":"山田 太郎","expiryDate":"2026-10-22","expiringSoon":true}
```

| ステータス | 条件 | 本文 |
|---|---|---|
| 200 | 契約が存在する | 契約番号、契約者名、満期日、満期予告フラグ |
| 400 | 契約番号の形式が不正 | `{"detail":"Invalid request", ...}` |
| 404 | 契約が存在しない | `{"detail":"Policy not found", ...}` |

エラー応答は汎用メッセージのみで、例外名やスタックトレースは含みません。

### 満期予告フラグ(`expiringSoon`)

- 満期日が本日から **0〜30 日後**(30 日後・満期日当日を含む)の場合に `true`。
- 満期日を過ぎた契約は `false`。
- 基準日は Asia/Tokyo の暦日。
- 30 日の閾値は `PolicyService.EXPIRY_WARNING_DAYS` で定義しています(固定値)。

### サンプルデータ

満期日は起動日からの相対日数で生成するため、いつ起動しても下表のとおり警告の有無は変わりません。

| 契約番号 | 契約者名 | 満期日 | expiringSoon |
|---|---|---|---|
| P0000001 | 山田 太郎 | 起動日 + 10 日 | true |
| P0000002 | 佐藤 花子 | 起動日 + 30 日 | true |
| P0000003 | 鈴木 一郎 | 起動日 + 90 日 | false |

## 構成

```
com.example.policy
├── api         PolicyController, PolicyResponse, GlobalExceptionHandler
├── config      ClockConfig(基準日の Clock)
├── service     PolicyService(満期予告の判定)
├── repository  PolicyRepository, InMemoryPolicyRepository(サンプル用)
└── domain      Policy, PolicyNotFoundException
```

実運用では `PolicyRepository` の実装を DB アクセスに差し替えます。
このサンプルには認証・認可がありません。個人情報を扱う本番利用の前に、OpenID Connect などによる認証と RBAC を追加してください。
