# insurance-policy-app

契約番号を指定して契約者名を返却する、Spring Boot 製の契約照会 API サンプルです。
有効(`ACTIVE`)かつ未更新の契約の満期日が 30 日以内の場合、API の警告フラグと照会画面の警告表示で注意を促します(Issue #1, #6)。
契約者名と契約ステータスで契約を検索できます(Issue #3)。

## 画面

起動後に `http://127.0.0.1:8080/` を開き、契約番号を入力して照会します。有効かつ未更新の契約で満期が 30 日以内の場合は警告バナーが表示されます。更新済みかどうか(`renewed`)は画面に表示しません。

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
{"policyNumber":"P0000001","policyHolderName":"山田 太郎","expiryDate":"2026-10-22","expiringSoon":true,"status":"ACTIVE","renewed":false}
```

| ステータス | 条件 | 本文 |
|---|---|---|
| 200 | 契約が存在する | 契約番号、契約者名、満期日、満期予告フラグ、契約ステータス、更新済みフラグ |
| 400 | 契約番号の形式が不正 | `{"detail":"Invalid request", ...}` |
| 404 | 契約が存在しない | `{"detail":"Policy not found", ...}` |

エラー応答は汎用メッセージのみで、例外名やスタックトレースは含みません。

### `GET /api/policies`(契約検索)

契約者名と契約ステータスで契約を検索し、**契約の一覧**を返します。

| パラメータ | 必須 | 内容 |
|---|---|---|
| `holderName` | 任意 | 契約者名。部分一致、大文字小文字を区別しない。前後の空白は除去し、空白のみは未指定扱い。100 文字まで。 |
| `status` | 任意 | 契約ステータス。単一選択。大文字小文字を区別しない。 |

- 両方指定した場合は AND 条件。
- 両方省略した場合は全件を返す。
- 結果は契約番号の昇順。該当がなければ空の配列 `[]`(200)。
- `status` に定義にない値、または `holderName` が 100 文字超の場合は 400。

```bash
curl "http://127.0.0.1:8080/api/policies?holderName=山田&status=ACTIVE"
```

### 契約ステータス(`status`)

| 値 | 意味 |
|---|---|
| `ACTIVE` | 有効 |
| `CANCELED` | 解約 |
| `LAPSED` | 失効 |
| `EXPIRED` | 満期 |

「満期間近」はステータスに含めません。満期日から判定する `expiringSoon` で扱います。
`expiringSoon` は `ACTIVE` の契約だけが対象です。`CANCELED` / `LAPSED` / `EXPIRED` は、満期日が近くても `false` になります(Issue #1 の仕様確認による)。

### 更新済みフラグ(`renewed`)

- 更新手続きが済んだ契約で `true`。契約番号照会・契約検索のレスポンスに含まれます(Issue #6)。
- ステータスは変わりません。更新済みの旧契約も `ACTIVE` のままで、満期日を過ぎると `EXPIRED` になる想定です。
- 満期予告(`expiringSoon`)の判定にのみ使います。検索条件にはできず、画面にも表示しません。
- 更新前後の契約を関連付ける情報は持ちません。

### 満期予告フラグ(`expiringSoon`)

次の条件をすべて満たす場合のみ `true` です。

- ステータスが `ACTIVE`
- 満期日が本日から **0〜30 日後**(30 日後・満期日当日を含む)
- `renewed` が `false`(未更新)

これ以外は `false` です(`ACTIVE` 以外のステータス、更新済み、満期日を過ぎた契約)。

- 基準日は Asia/Tokyo の暦日。
- 30 日の閾値は `PolicyService.EXPIRY_WARNING_DAYS` で定義しています(固定値)。

### サンプルデータ

満期日は起動日からの相対日数で生成するため、いつ起動しても下表のとおり警告の有無は変わりません。

| 契約番号 | 契約者名 | ステータス | 満期日 | renewed | expiringSoon |
|---|---|---|---|---|---|
| P0000001 | 山田 太郎 | ACTIVE | 起動日 + 10 日 | false | true |
| P0000002 | 佐藤 花子 | ACTIVE | 起動日 + 30 日 | false | true |
| P0000003 | 鈴木 一郎 | ACTIVE | 起動日 + 90 日 | false | false |
| P0000004 | 山田 太郎 | CANCELED | 起動日 − 60 日 | false | false |
| P0000005 | 山田 花子 | LAPSED | 起動日 − 30 日 | false | false |
| P0000006 | 田中 次郎 | EXPIRED | 起動日 − 1 日 | false | false |
| P0000007 | 佐藤 花子 | ACTIVE | 起動日 + 200 日 | false | false |
| P0000008 | Smith John | ACTIVE | 起動日 + 120 日 | false | false |
| P0000009 | 高橋 三郎 | ACTIVE | 起動日 + 15 日 | true | false |

## 構成

```
com.example.policy
├── api         PolicyController, PolicyResponse, GlobalExceptionHandler, InvalidStatusException
├── config      ClockConfig(基準日の Clock)
├── service     PolicyService(満期予告の判定、契約検索)
├── repository  PolicyRepository, InMemoryPolicyRepository(サンプル用)
└── domain      Policy, PolicyStatus, PolicyNotFoundException
```

実運用では `PolicyRepository` の実装を DB アクセスに差し替えます。

## 既知の未対応

- **認証・認可がありません。** 比較用サンプルのためスコープ外としています。契約検索 API は複数の契約者名・満期日を一括で返すため、個人情報を扱う本番利用の前に、OpenID Connect などによる認証と RBAC の追加が必須です。
- **検索結果の件数上限がありません。** 同上の理由でスコープ外です。本番ではページングと上限が必要です。
- **更新の履歴をたどれません。** `renewed` は更新済みかどうかだけを表し、更新前後の契約の関連は持ちません(Issue #6 の仕様確認で、現時点では不要とされています)。
