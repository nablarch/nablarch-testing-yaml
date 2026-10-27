# task-28 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| 送信同期4データタイプで `record_type` の記載値が保持される | OK | `YamlSection#isSendSyncMessageSectionKey`（`YamlSection.java:303`-`:308`）でセクションキーを判定し、`YamlMessageBuilder.java:89`-`:90`・`:119`-`:120`・`:158`-`:159` から `keepRecordType` として引き回す。`YamlFileBuilder#buildFragmentsInternal`（`YamlFileBuilder.java:196`-`:198`）が `keepRecordType && recordType != null ? recordType : DEFAULT_RECORD_TYPE`。`YamlTestDataParserTest#getMessageWithoutCache_recordTypeIsKeptForSendSyncDataTypes` が4データタイプすべて（FW_HEADER / BODY / HEADER / BODY）を確認 | | |
| `messages`（`setUpMessages`・`expectedMessages`）は `"default"` のまま | OK | `isSendSyncMessageSectionKey("messages")` が false。`YamlTestDataParserTest#getMessageWithoutCache_recordTypeIsDefaultForMessages`（DataType.MESSAGE・記載値 BODY → "default"）・`#getMessage_fwHeaderRecordTypeIsNotSkipped`（2レコードとも "default"）・`YamlMessageBuilderTest#buildMessagePool_recordTypeIsDefaultForMessages` が確認 | | |
| `record_type` を書いた既存フィクスチャについて、変更したもの・しなかったものが件数付きで記録されている | OK | 後述「step B 数え直しの全件表」。**フィクスチャ YAML の変更は 0 件**（`git diff --stat` は `.java` 7 ファイルのみ）。挙動が変わる記載は送信同期4セクションの 24 件 | | |
| 是正前に落ち是正後に通るテストが存在する | OK | 是正前（テストだけ先に書いた状態）の `mvn -o clean test`: `Tests run: 243, Failures: 5, Errors: 0, Skipped: 0` / BUILD FAILURE。失敗した5件は後述。是正後: `Tests run: 245, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS | | |
| 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 後述「step D 変異確認」（Mutation A〜E、いずれも `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` を全量実行） | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `Tests run: 245, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS` | | |

## Overall Verdict

- Self-check: OK

---

## Method（テストファースト）を適用したこと

期待する挙動を捉える落ちるテストを先に書き、production コードを触らずに `mvn -o clean test` を実行して
`Tests run: 243, Failures: 5, Errors: 0, Skipped: 0` / BUILD FAILURE を確認した。落ちた5件:

- `YamlTestDataParserTest.getMessageWithoutCache_expectedRequestHeaderMessages:713`
- `YamlTestDataParserTest.getMessageWithoutCache_recordTypeIsKeptForSendSyncDataTypes:929`
- `YamlTestDataParserTest.getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped:892`
- `YamlMessageBuilderTest.buildMessagePool_fwHeaderRecordTypeIsNotSkipped:435`
- `YamlMessageBuilderTest.buildSendSyncMessageList_recordTypeIsKeptAsIs:498`

そのあと `YamlSection`／`YamlFileBuilder`／`YamlMessageBuilder` を是正し、緑にした。
（`YamlFileBuilderTest` はシグネチャ変更を伴うため、実装と同時に更新した。）

## step B: `record_type` の数え直し（全件）

### grep 実測

| コマンド | 実測 | 指示書の件数 | 一致 |
|---|---|---|---|
| `grep -rn 'record_type: *HEADER' src/` | 7 | 7 | 一致 |
| `grep -rn 'record_type: *FW_HEADER' src/` | 17 | 16 | **不一致（+1）** |

`FW_HEADER` 17件の内訳（17件のうち **YAML フィクスチャの実データ行は 7件**、残り10件はスキーマ記述・コメント・javadoc・アサーションメッセージ）:

| # | file:line | 種別 |
|---|---|---|
| 1 | `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:208` | スキーマの description 文（フィクスチャではない） |
| 2 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/messageData.yaml:31` | フィクスチャ（`messages`） |
| 3 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/messageData.yaml:46` | フィクスチャ内の**コメント行** |
| 4 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/messageData.yaml:53` | フィクスチャ（`messages`） |
| 5 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/messageData.yaml:80` | フィクスチャ（`expected_request_header_messages`） |
| 6 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/messageData.yaml:130` | フィクスチャ（`response_body_messages`） |
| 7 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/schemaFullCoverage.yaml:196` | フィクスチャ（`expected_request_header_messages`） |
| 8 | `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest/fileData.yaml:119` | フィクスチャ（`setup_files`） |
| 9 | `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/messageData.yaml:90` | フィクスチャ（`expected_request_header_messages`） |
| 10 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:689` | Java javadoc |
| 11 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:706` | Java コメント |
| 12 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:809` | Java javadoc |
| 13 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:826` | Java コメント |
| 14 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:854` | Java javadoc |
| 15 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:891` | Java javadoc |
| 16 | `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java:413` | Java javadoc |
| 17 | `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java:433` | Java アサーションメッセージ |

（行番号はいずれも**着手時 HEAD `8e41689`** のもの。指示書の16件との差 +1 は、上記のうちどれが数え漏れかを特定できないが、
実測 17件を正として扱う。フィクスチャの実データ行だけを数えると 7 件になる。）

### `record_type` を書いたフィクスチャ全件（86行）と、今回の是正での扱い

`src/test` 配下の YAML フィクスチャに現れる `record_type:` 行は **86件**（`grep -rn 'record_type:' src/test --include=*.yaml | wc -l`。
うち1件はコメント行なので実データは 85件）。セクション別に分類すると:

| セクション | 記載件数 | 今回の挙動変化 | フィクスチャ本文の変更 |
|---|---|---|---|
| `setup_files`（21）/ `expected_files`（24） | 45 | なし（従来から記載値をそのまま使用） | 0件 |
| `messages` | 17（うちコメント1行を含む） | なし（記載値を使わず `"default"`、従来どおり） | 0件 |
| `expected_request_header_messages` | 4 | **あり**（`"default"` → 記載値） | 0件 |
| `expected_request_body_messages` | 4 | **あり** | 0件 |
| `response_header_messages` | 5 | **あり** | 0件 |
| `response_body_messages` | 11 | **あり** | 0件 |
| 合計 | 85（+コメント1） | 変化するのは **24件** | **0件** |

**フィクスチャ YAML は 1 ファイルも変更していない**（`git diff --stat` の対象は `.java` 7ファイルのみ）。
記載値をそのまま保持するのが是正後の正しい挙動であり、フィクスチャの記述はそのままで意味が正しくなるため。

挙動が変わる 24件の内訳（`file:line` → 記載値）:

- `expected_request_header_messages`（4）
  - `YamlTestDataParserTest/messageData.yaml:80` → FW_HEADER
  - `YamlTestDataParserTest/schemaFullCoverage.yaml:196` → FW_HEADER
  - `YamlMessageBuilderTest/fwHeaderMapData.yaml:64` → HEADER
  - `YamlMessageBuilderTest/messageData.yaml:90` → FW_HEADER
- `expected_request_body_messages`（4）
  - `YamlTestDataParserTest/messageData.yaml:100` → BODY
  - `YamlTestDataParserTest/schemaFullCoverage.yaml:217` → BODY
  - `YamlMessageBuilderTest/fwHeaderMapData.yaml:53` → BODY
  - `YamlMessageBuilderTest/messageData.yaml:110` → BODY
- `response_header_messages`（5）
  - `YamlTestDataParserTest/messageData.yaml:150` → HEADER
  - `YamlTestDataParserTest/schemaFullCoverage.yaml:234` → HEADER
  - `YamlTestDataParserTest/schemaFullCoverage.yaml:244` → HEADER
  - `YamlMessageBuilderTest/fwHeaderMapData.yaml:89` → HEADER
  - `YamlMessageBuilderTest/messageData.yaml:192` → HEADER
- `response_body_messages`（11）
  - `YamlTestDataParserTest/messageData.yaml:112` → BODY
  - `YamlTestDataParserTest/messageData.yaml:130` → FW_HEADER
  - `YamlTestDataParserTest/messageData.yaml:137` → BODY
  - `YamlTestDataParserTest/schemaFullCoverage.yaml:257` → BODY
  - `YamlTestDataParserTest/schemaFullCoverage.yaml:270` → BODY
  - `YamlMessageBuilderTest/fwHeaderMapData.yaml:78` → BODY
  - `YamlMessageBuilderTest/messageData.yaml:121` → BODY
  - `YamlMessageBuilderTest/messageData.yaml:137` → BODY
  - `YamlMessageBuilderTest/messageData.yaml:151` → BODY
  - `YamlMessageBuilderTest/messageData.yaml:165` → BODY
  - `YamlMessageBuilderTest/messageData.yaml:178` → BODY

### 指定された4テストの見直し結果

| テスト | 変更 | 理由 |
|---|---|---|
| `YamlFileBuilderTest#buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped` | 期待値を `"default"`／`"default"` → `"FW_HEADER"`／`"BODY"` に変更。呼び出しを `keepRecordType=true` に。javadoc 更新 | 送信同期4セクションは記載値を保持する。テスト名（読み飛ばされない）は依然として正しいので維持 |
| `YamlMessageBuilderTest#buildMessagePool_fwHeaderRecordTypeIsNotSkipped` | 期待値を `"default"` → `"FW_HEADER"` に変更。javadoc 更新 | `expected_request_header_messages` は送信同期4セクションの1つ |
| `YamlTestDataParserTest#getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped` | レコード種別のアサーション（`"FW_HEADER"`／`"BODY"`／`"BODY"`）を**追加**。javadoc 更新 | 元はレコード種別を検証していなかった |
| `YamlTestDataParserTest#getMessage_fwHeaderRecordTypeIsNotSkipped` | 期待値は `"default"` のまま**変更なし**。2レコード目のアサーションを追加し、javadoc・アサーションメッセージの文言を「固定される」→「`messages` では記載値が使われず `"default"` になる」に修正 | `messages`（MESSAGE）経路なので解説書どおり `"default"`。4件が同じ結論にはならない |

そのほか `YamlTestDataParserTest#getMessageWithoutCache_expectedRequestHeaderMessages` の
`message.getRecordType()` を `"default"` → `"FW_HEADER"` に変更した（同じく送信同期4セクション）。

### 追加したテスト（step C を含む）

- `YamlTestDataParserTest#getMessageWithoutCache_recordTypeIsKeptForSendSyncDataTypes`
  — 送信同期4データタイプすべてで記載値が保持されること（`FW_HEADER` が単に `FW_HEADER` になることを含む）
- `YamlTestDataParserTest#getMessageWithoutCache_recordTypeIsDefaultForMessages` — `messages` は `"default"`
- `YamlMessageBuilderTest#buildMessagePool_recordTypeIsDefaultForMessages` — `messages` は `"default"`
- `YamlMessageBuilderTest#buildSendSyncMessageList_recordTypeIsKeptAsIs` — 送信同期経路で記載値が保持される
- `YamlSectionTest#isSendSyncMessageSectionKey_onlySendSyncFourSectionsAreTrue` — 判定対象セクションの集合を固定
- `YamlFileBuilderTest#buildFragmentsForSendSync_recordTypeIsDefaultWhenNotKept` — `keepRecordType=false` 分岐

## step D: 変異確認

実行コマンドはいずれも `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`（全量実行）。

### Mutation A — `YamlSection#isSendSyncMessageSectionKey` を `return false;` に

`Tests run: 245, Failures: 6, Errors: 0, Skipped: 0` / BUILD FAILURE

- `YamlTestDataParserTest.getMessageWithoutCache_expectedRequestHeaderMessages:713`
- `YamlTestDataParserTest.getMessageWithoutCache_recordTypeIsKeptForSendSyncDataTypes:929`
- `YamlTestDataParserTest.getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped:892`
- `YamlMessageBuilderTest.buildMessagePool_fwHeaderRecordTypeIsNotSkipped:435`
- `YamlMessageBuilderTest.buildSendSyncMessageList_recordTypeIsKeptAsIs:498`
- `YamlSectionTest.isSendSyncMessageSectionKey_onlySendSyncFourSectionsAreTrue:107`

### Mutation B — 同メソッドを `return true;` に

`Tests run: 245, Failures: 4, Errors: 0, Skipped: 0` / BUILD FAILURE

- `YamlTestDataParserTest.getMessageWithoutCache_recordTypeIsDefaultForMessages:966`
- `YamlTestDataParserTest.getMessage_fwHeaderRecordTypeIsNotSkipped:836`
- `YamlMessageBuilderTest.buildMessagePool_recordTypeIsDefaultForMessages:468`
- `YamlSectionTest.isSendSyncMessageSectionKey_onlySendSyncFourSectionsAreTrue:117`

### Mutation C — 同メソッドから `KEY_RESPONSE_HEADER_MESSAGES` の項を削除

`Tests run: 245, Failures: 2, Errors: 0, Skipped: 0` / BUILD FAILURE

- `YamlTestDataParserTest.getMessageWithoutCache_recordTypeIsKeptForSendSyncDataTypes:937`（`response_header_messages` の `HEADER`）
- `YamlSectionTest.isSendSyncMessageSectionKey_onlySendSyncFourSectionsAreTrue:111`

### Mutation D — `YamlFileBuilder#buildFragmentsInternal` が `keepRecordType` を無視し常に記載値を採用

`Tests run: 245, Failures: 6, Errors: 0, Skipped: 0` / BUILD FAILURE

- `YamlTestDataParserTest.getMessageWithoutCache_recordTypeIsDefaultForMessages:966`
- `YamlTestDataParserTest.getMessage_fwHeaderRecordTypeIsNotSkipped:836`
- `YamlFileBuilderTest.buildFragmentsForMessage_fwHeaderRecordTypeIsNotSkipped:746`
- `YamlFileBuilderTest.buildFragmentsForMessage_fwHeaderRecordWithoutLength:791`
- `YamlFileBuilderTest.buildFragmentsForSendSync_recordTypeIsDefaultWhenNotKept:880`
- `YamlMessageBuilderTest.buildMessagePool_recordTypeIsDefaultForMessages:468`

### Mutation E — `buildFragmentsForSendSync` が `keepRecordType` を捨てて常に `false` を渡す

`Tests run: 245, Failures: 3, Errors: 0, Skipped: 0` / BUILD FAILURE

- `YamlTestDataParserTest.getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped:892`
- `YamlFileBuilderTest.buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped:836`
- `YamlMessageBuilderTest.buildSendSyncMessageList_recordTypeIsKeptAsIs:498`

各変異のあと production コードを元に戻し、`grep -rn MUTATION src/main src/test` が 0 件であることを確認した。

## 判断が必要な事項（スキーマと解説書の食い違い）

`src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:363`（`$defs.record_fragment.record_type` の description）:

> レコード種別名。ファイルデータ（setup_files / expected_files）では複数レコードレイアウトを持つファイルの識別に使われる。メッセージング系（messages / expected_request_\* / response_\*）では NTF 内部で常に "default" に置換されるため実行時の挙動に影響しない（可読性のために任意の名前を記述してよい。FW_HEADER のような予約値はない）

は、解説書 `nablarch-document` `5b5c91e` の
`ja/development_tools/testing_framework/implementation/testdata_notation.rst:1163` が定める
「同期応答メッセージ送信で使う4つのデータタイプ…では、記載した値がそのままレコード種別になる」と食い違う。

Scope により**スキーマは触っていない**。是正が必要と判断する。

なお同ファイル `:208`（`$defs.message_data.records` の description、「MessageParser はこの records の
record_type を内部で常に "default" に置換する」）は `messages` エントリ限定の記述なので、解説書と整合している。

## コーディネーター独立レビュー

Step 4 では4観点レビューを回さない（指示書 §7）。コーディネーターがコミット済み差分を独立に読み、ビルドを自分で実行し、参照点の一次情報を自分で確認して検証した。

| 観点 | 判定 | 根拠 |
|---|---|---|
| 差分がタスクの範囲に収まっている | OK | `git diff 8e41689..1693cc1` は `YamlFileBuilder.java`・`YamlMessageBuilder.java`・`YamlSection.java` とテスト4ファイル。スキーマ・`pom.xml`・解説書・`nablarch-testing`・`nablarch-testing-converter` への書き込みなし |
| セクションキーで判定している | OK | `YamlSection#isSendSyncMessageSectionKey`（`:303`-`:308`）が4つのセクションキー定数で判定し、`YamlMessageBuilder` の `buildMessageContent`・`buildSendSyncList`・`buildSendSyncBodies` が受け取り済みのセクションキーから求めて引き回す。組み立てメソッドの別ではなくセクションキーで決まるため、`getMessageWithoutCache` が送信同期4キーで `buildFragmentsForMessage` を通る経路でも記載値が保持される |
| 解説書と一致 | OK | `fragment.setRecordType(keepRecordType && recordType != null ? recordType : DEFAULT_RECORD_TYPE)`。`messages` は `keepRecordType=false` で常に `"default"`、送信同期4キーは記載値保持。解説書 `5b5c91e` の `testdata_notation.rst:1163` と一致 |
| ファイルデータ経路の挙動が変わっていない | OK | `buildFragmentsForFile` は `keepRecordType=true` を渡す。是正前の非 messaging 分岐 `recordType != null ? recordType : DEFAULT_RECORD_TYPE` と等価 |
| `withId`（連番付与）の条件を変えていない | OK | `buildFragmentsForSendSync` のみ `withId=true`。差分上も第5引数の位置に変更なし |
| 既存の公開 API を壊していない | OK | `buildFragmentsForMessage`・`buildFragmentsForSendSync` は package-private。converter が使う `buildMessageContent`・`buildSendSyncBodies` の公開シグネチャは不変 |
| 変異確認が実施されている | OK | 変異A〜E（判定メソッドの `false` 固定／`true` 固定／キー1つ削除／`keepRecordType` 無視／送信同期経路で常に `false`）を実施。すべて意図した件数が落ちた。`grep -rn MUTATION src/main src/test` = 0件で復元済み |
| ビルド（コーディネーター自身の実行） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 245, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（2026-08-26 コーディネーターが独立実行） |

### 指示書の件数との不一致（実測を正とした）

`record_type: FW_HEADER` の出現は **17件**（指示書は16件）。`record_type: HEADER` は **7件**（指示書と一致）。17件の内訳は実装担当が全件挙げており、コーディネーターも `grep -rn 'record_type: *FW_HEADER' src/` で17件を確認済み。フィクスチャ YAML の変更は0件（送信同期4セクションの24件は記載値をそのまま保持するのが正しい挙動のため、本文はそのままで意味が正しくなる）。

### ユーザー判断待ち: スキーマ `record_fragment.record_type` の description（指示書 2-5 の3件の外）

`src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:365`（`$defs.record_fragment.record_type` の `description`）:

> レコード種別名。…**メッセージング系（messages / expected_request_\* / response_\*）では NTF 内部で常に `"default"` に置換されるため実行時の挙動に影響しない**（可読性のために任意の名前を記述してよい。FW_HEADER のような予約値はない）

コーディネーターが実物で確認: `$defs.record_fragment` は `messages` と送信同期4セクションの両方から参照される共用定義である（`$defs` は `table_data` / `list_map_data` / `file_data` / `message_data` / `expected_request_message_data` / `group_message_data` / `directives` / `record_fragment` / `field_def` / `fw_header` の10件で、`message_data` は `messages` 専用）。**#28 の是正により、送信同期4セクションについてこの記述は事実に反する。**

一方 `:208`（`$defs.message_data.records`。「MessageParser はこの records の record_type を内部で常に `"default"` に置換する」）は `messages` 専用の定義に付いており、是正後も正しい。

指示書 2-5 が名指ししたスキーマ description は `:410`・`:108`・`:136` の3件であり `:365` は含まれない。**範囲の判断を持たないため、直さずユーザー判断を仰ぐ。** 本タスクでは未変更のまま。

## Overall Verdict（コーディネーター）

- コーディネーター独立レビュー: OK
- Ready to check off: Yes（`:365` はユーザー判断待ちとして別建て。#28 の Completion criteria には含まれない）
