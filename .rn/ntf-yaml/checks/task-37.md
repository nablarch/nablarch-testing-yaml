# task-37 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| 3箇所の `records` に `maxItems: 1` が入っている | OK | `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:208`（`$defs.message_data.records`）・`:242`（`$defs.expected_request_message_data.records`）・`:274`（`$defs.group_message_data.records`）。いずれも既存の `"minItems": 1` の直後に `"maxItems": 1,` を置いた。`git diff` は 3 行追加・0 行削除のみ。`$defs.file_data.records`（`:179`）は `"minItems": 0` のままで `maxItems` を入れていない（ファイルは複数レコードレイアウトを持てるため）。根拠: `implementation/testdata_notation.rst:1153`・`:1299`（ピン `afa4f9e`） | | |
| `records` を2つ書くとスキーマ検証で落ちるテストがあり、メッセージに出所が入ることを assert している | OK | `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest.java` に 3 件追加。`load_messagesWithMultipleRecordsIsSchemaViolation`（`:616`）・`load_expectedRequestMessagesWithMultipleRecordsIsSchemaViolation`（`:649`）・`load_responseMessagesWithMultipleRecordsIsSchemaViolation`（`:683`）。いずれも `YamlSchemaValidationException`（既存のスキーマ検証テストと同じ型。新しい例外型は作っていない）を捕捉し、(1) メッセージにファイルパス、(2) メッセージに出所（`messages[0].records` ／ `expected_request_body_messages[0].records` ／ `response_body_messages[0].records`）、(3) `getErrors()` が 1 件で `getType()` が `"maxItems"`、を assert する。対照として `load_fileDataWithMultipleRecordsIsAllowed`（`:716`）が `setup_files` は `records` 2 件でもロードできることを押さえる | | |
| 是正前は通り是正後に落ちる既存テストが全件挙がり、解説書に合わせて直されている（件数付き） | OK | 落ちた既存テストは **15 件**（すべて `YamlTestDataParserTest`）。うち期待値を**変えた** **3 件**、期待値を**変えなかった** **12 件**（`messageData.yaml` のフィクスチャ是正だけで復旧）。全件は下記「スキーマ変更後に落ちた既存テスト」の節 | | |
| 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 追加 4 件・変更 3 件の計 **7 件**すべてについて期待値を崩し、1 度の実行で 7 件すべてが落ちることを確認した。下記「変異確認」の節。確認後ただちに元へ戻し、再度全件緑を確認済み | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 280, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。着手前ベースライン `Tests run: 276, Failures: 0, Errors: 0, Skipped: 1` ＋ 追加 4 件。`Skipped 1` は `YamlTableDataBuilderTest.java:751` の既存 `@Ignore`（#41 の担当。今回は触っていない） | | |

## スキーマ変更「前」に新規テストが落ちたこと（実測）

コマンド:
`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest='YamlLoaderTest#load_messagesWithMultipleRecordsIsSchemaViolation+load_expectedRequestMessagesWithMultipleRecordsIsSchemaViolation+load_responseMessagesWithMultipleRecordsIsSchemaViolation+load_fileDataWithMultipleRecordsIsAllowed'`
結果: `Tests run: 4, Failures: 3, Errors: 0, Skipped: 0` / `BUILD FAILURE`

| テスト | 落ちた要点 |
|---|---|
| `load_messagesWithMultipleRecordsIsSchemaViolation` | `YamlLoaderTest.java:620 YamlSchemaValidationException が期待される` — `maxItems` が無いため `records` 2 件でも通ってしまう |
| `load_expectedRequestMessagesWithMultipleRecordsIsSchemaViolation` | `YamlLoaderTest.java:653 YamlSchemaValidationException が期待される`（同上） |
| `load_responseMessagesWithMultipleRecordsIsSchemaViolation` | `YamlLoaderTest.java:687 YamlSchemaValidationException が期待される`（同上） |
| `load_fileDataWithMultipleRecordsIsAllowed` | 落ちない（対照。ファイルデータは変更前後とも 2 レコードで通る） |

## スキーマ変更「後」に落ちた既存テスト（全件・実測）

コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
結果: `Tests run: 280, Failures: 1, Errors: 14, Skipped: 1` / `BUILD FAILURE`

落ちたのは `nablarch.test.core.reader.YamlTestDataParserTest` の **15 件**。すべて
`YamlTestDataParserTest/messageData.yaml` を読む（同ファイルに `records` 2 件のエントリが 3 件あり、
スキーマ検証はファイル単位のためロード自体が失敗した）。

| # | テスト | 期待値を変えたか | 対応 |
|---|---|---|---|
| 1 | `getMessage` | 変えていない | `messageData.yaml` の是正だけで復旧 |
| 2 | `getMessageWithoutCache_expectedRequestHeaderMessages` | 変えていない | 同上 |
| 3 | `getMessageWithoutCache_expectedRequestBodyMessages` | 変えていない | 同上 |
| 4 | `getMessageWithoutCache_responseHeaderMessages` | 変えていない | 同上 |
| 5 | `getMessageWithoutCache_responseBodyMessages` | 変えていない | 同上 |
| 6 | `getSendSyncMessage` | 変えていない | 同上 |
| 7 | `getSendSyncMessageReturnsNullForUnknownGroupId` | 変えていない | 同上 |
| 8 | `getMessageWithoutCache_recordTypeIsKeptForSendSyncDataTypes` | 変えていない | 同上 |
| 9 | `getMessageWithoutCache_recordTypeIsDefaultForMessages` | 変えていない | 同上 |
| 10 | `getMessageReturnsNullWhenIdNotFound` | 変えていない | 同上 |
| 11 | `getMessageWithoutCacheReturnsNullWhenIdNotFound` | 変えていない | 同上 |
| 12 | `getMessage_reservedIdsSetUpMessagesAndExpectedMessages` | 変えていない | 同上 |
| 13 | `getMessage_fwHeaderRecordTypeIsNotSkipped` | **変えた** | フィクスチャ `fwHeaderRecordType001` を 2 レコード（`FW_HEADER` 10 バイト ＋ `BODY` 10 バイト）から 1 レコード（`record_type: FW_HEADER`・`HEAD_KEY` 10 バイト・値行 2 行）に畳んだ。テストが押さえる意味（「`record_type: FW_HEADER` は特別扱いされず `messages` では `"default"` になる」「`fw_header:` の値は本文に混ざらない」）は保持。変えた assert は 2 レコード目の項目名・値（`SEARCH_KEY`／`SEARCHKEY1` → `HEAD_KEY`／`HEADKEY002`）と javadoc の Given/Then（`YamlTestDataParserTest.java:941`） |
| 14 | `getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped` | **変えた** | フィクスチャ `sync001`（`group_id: fwHeaderSync`）を 2 レコード（`FW_HEADER` 値行 1 行 ＋ `BODY` 値行 2 行）から 1 レコード（`record_type: FW_HEADER`・値行 2 行）に畳んだ。テストが押さえる意味（「送信同期 4 データタイプでは記載値がそのままレコード種別になる」「連番が 1 始まりで付与される」）は保持。件数 3 → 2、`BODY_KEY` → `HEAD_KEY`、レコード種別の期待値を `"FW_HEADER"` 2 件に変更（`YamlTestDataParserTest.java:991`）。フラグメントをまたぐ連番リセットの assert は、複数レコードレイアウト自体が禁止になったため落とした |
| 15 | `getMessage_legacyFwHeaderRecordCausesRecordLengthMismatch` | **変えた**（テスト名も変更） | `getMessage_legacyFwHeaderRecordIsRejectedBySchemaValidation` に改名（`YamlTestDataParserTest.java:1111`）。旧形式は `maxItems: 1` により手前のスキーマ検証で弾かれるようになったため、期待する例外を `IllegalStateException`（`"record-length differs."`）から `YamlSchemaValidationException`（ファイルパス・`messages[0].records`・`getType()` が `"maxItems"`）に変更。テストの意味「旧形式の書き方は通らない」は保持。スキーマ検証はファイル単位でありこのエントリを `messageData.yaml` に残すと他 14 件を巻き込むため、エントリを新規フィクスチャ `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/legacyFwHeaderRecord.yaml` へ切り出した |

**件数まとめ**: 落ちた既存テスト 15 件 ＝ 期待値を変えたもの **3 件** ＋ 変えなかったもの **12 件**。
落ちなかった既存テスト（着手前ベースライン 276 件のうち残り **261 件**）は一切触っていない。

## 是正したフィクスチャ（3 件・すべて `YamlTestDataParserTest/messageData.yaml`）

| 旧行 | セクション | id | 是正 |
|---|---|---|---|
| `:31` | `messages` | `fwHeaderRecordType001` | 2 レコード → 1 レコード（`record_type: FW_HEADER`・`HEAD_KEY` 10 バイト・値行 2 行）に畳んだ |
| `:53` | `messages` | `legacyFwHeaderRecord001` | `messageData.yaml` から削除し、独立ファイル `legacyFwHeaderRecord.yaml` へ 2 レコードのまま移した（スキーマ検証で弾かれることを押さえるフィクスチャとして使う） |
| `:163` | `response_body_messages` | `sync001`（`group_id: fwHeaderSync`） | 2 レコード → 1 レコード（`record_type: FW_HEADER`・`HEAD_KEY` 10 バイト・値行 2 行）に畳んだ |

`YamlTestDataParserTest/schemaFullCoverage.yaml` は電文セクションの全エントリが `records` 1 件で該当なし（変更していない）。

## 変異確認

7 件の期待値を同時に崩し、1 度実行して 7 件すべてが落ちることを確認した。
コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
結果: `Tests run: 280, Failures: 7, Errors: 0, Skipped: 1` / `BUILD FAILURE`。
確認後ただちに元へ戻し、再実行して `Tests run: 280, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS` を確認済み。

| テスト | 崩した内容 | 落ちたこと |
|---|---|---|
| `load_messagesWithMultipleRecordsIsSchemaViolation` | `errors.get(0).getType(), is("maxItems")` → `is("minItems")` | 落ちた（`YamlLoaderTest:629 レコードレイアウトの上限超過として弾かれること（maxItems 違反）: $.messages[0].records: アイテムは最大でも 1 個必要ですが、2 が見つかりました`） |
| `load_expectedRequestMessagesWithMultipleRecordsIsSchemaViolation` | 出所 `containsString("expected_request_body_messages[0].records")` → `"expected_request_header_messages[0].records"` | 落ちた（`YamlLoaderTest:659 エラーメッセージに出所（expected_request_body_messages セクションの records）が含まれること`） |
| `load_responseMessagesWithMultipleRecordsIsSchemaViolation` | `errors.size(), is(1)` → `is(2)` | 落ちた（`YamlLoaderTest:696 違反が 1 件報告されること`） |
| `load_fileDataWithMultipleRecordsIsAllowed` | `records.size(), is(2)` → `is(3)` | 落ちた（`YamlLoaderTest:726 ファイルデータは複数のレコードレイアウトを持てること`） |
| `getMessage_fwHeaderRecordTypeIsNotSkipped` | `messages.get(1).getString("HEAD_KEY"), is("HEADKEY002")` → `is("HEADKEY003")` | 落ちた（`YamlTestDataParserTest:954`） |
| `getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped` | `messages.size(), is(2)` → `is(3)` | 落ちた（`YamlTestDataParserTest:1001 FW_HEADER レコードの値行も読み飛ばされず計 2 件になること`） |
| `getMessage_legacyFwHeaderRecordIsRejectedBySchemaValidation` | 出所 `containsString("messages[0].records")` → `"messages[1].records"` | 落ちた（`YamlTestDataParserTest:1120 エラーメッセージに出所（messages セクションの records）が含まれること`） |

## 最終の実測

`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
→ `Tests run: 280, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`

## スコープ遵守

- `src/main/java/**` は 1 文字も変更していない（`git status --short` に現れない）。
- スキーマの変更は `maxItems: 1` の 3 行追加のみ。`description` は触っていない（`:209` の
  「MessageParser はこの records の record_type を内部で常に "default" に置換する」という
  実態と食い違う文言は #42 の担当のため残してある）。
- `../nablarch-testing`・`../nablarch-testing-converter`・`../nablarch-document` は読むだけで変更なし。
- `@Ignore` は新たに足していない。
- `.rn/ntf-yaml/steering.md`・`report-step4-2.md`・`unit-test.xml`・`unit-test-yaml.xml` は変更なし。

## 指示書と実測の食い違い

- 指示書 §5 は「`YamlTestDataParserTest` の16箇所すべてが落ちる」と書くが、
  `grep -c 'YamlTestDataParserTest/messageData'` が数えるのは**出現箇所**で 16、
  **テストメソッド**は 15 件である（`YamlTestDataParserTest.java:1822` と `:1834` が
  同一メソッド `getMessage_reservedIdsSetUpMessagesAndExpectedMessages` 内の 2 箇所）。
  実測でも落ちたのは 15 件（`Failures: 1, Errors: 14`）。

## QA / Expert Review

**Step 4 では4観点レビュー（QA / Design / Craft / Verification）を回さない**（指示書 §7。steering の
「Step 4 第2回に適用する Rules」）。代わりにコーディネータが下記を独立に実測した。

## コーディネータの独立検証（2026-08-28）

| 確かめたこと | 方法 | 結果 |
|---|---|---|
| 追加した検証が実際に効いている | スキーマの `"maxItems": 1,` 3行を削除して `mvn -o clean test -Dtest=YamlLoaderTest,YamlTestDataParserTest` を実行 | `Tests run: 96, Failures: 3, Errors: 1`。落ちたのは新規3件（`load_messagesWithMultipleRecordsIsSchemaViolation` / `load_expectedRequestMessagesWithMultipleRecordsIsSchemaViolation` / `load_responseMessagesWithMultipleRecordsIsSchemaViolation`）と変更した `getMessage_legacyFwHeaderRecordIsRejectedBySchemaValidation`。対照の `load_fileDataWithMultipleRecordsIsAllowed` は削除前後とも通った。確認後 `git checkout` で復元 |
| `maxItems` を入れた範囲が正しい | `git show 389fe6d -- …schema.json` | 追加は3行のみ。`$defs.message_data.records`（`:208`）・`$defs.expected_request_message_data.records`（`:242`）・`$defs.group_message_data.records`（`:274`）。**`$defs.file_data.records`（`:179`）は未変更**（ファイルは複数レコードレイアウトを持てる）。`description` は3箇所とも未変更（#42 の担当） |
| `src/main/java` を触っていない | `git show --stat 389fe6d \| grep src/main/java` | 該当なし。検証はスキーマだけで完結している |
| フィクスチャ是正が意味を保っているか | `git show 389fe6d -- …/messageData.yaml …/legacyFwHeaderRecord.yaml` | `fwHeaderRecordType001`・`sync001` は「`record_type: FW_HEADER` は予約値でなく読み飛ばされない」という担保を、1レコード＋値行2行に畳んで保持。`legacyFwHeaderRecord001` は2レコードのまま独立ファイルへ切り出し、**そのロードが弾かれること自体**を担保に変えている（スキーマ検証はファイル単位のため、同居させると他14テストを巻き込む） |
| 全件緑 | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` | `Tests run: 280, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`（#36 時点 276 ＋ 追加4） |

**記録しておく副作用（欠陥ではない）**: `getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped` が持っていた
「連番（`FIRST_FIELD_NO`）はフラグメントごとに 1 から振り直される」という担保は、電文のレコードレイアウトが
1つに限られたことで**電文経路では観測できなくなった**（フラグメントが常に1つになるため）。
`YamlFileBuilder` の `rowNo` はレコード単位に初期化されるが、`withId=true` の経路は送信同期電文だけであり、
そこは今後 `records` 1件に限られる。解説書が「レコードレイアウトは1つ」と定めている以上これは仕様どおりであり、
失われた担保は解説書に無い挙動である。

## Overall Verdict

- Self-check: OK
- QA: N/A（Step 4 は4観点レビューを回さない。指示書 §7）
- Design expert: N/A（同上）
- Craft expert: N/A（同上）
- Verification expert: N/A（同上）
- コーディネータの独立検証: OK
- Ready to check off: Yes
