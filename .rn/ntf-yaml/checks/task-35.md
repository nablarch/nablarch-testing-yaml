# task-35 Completion Check

対象: 2026-08-27 のユーザー指示（`/rn:gm` 差し戻し）。報告書 `.rn/ntf-yaml/report-step4.md` §6 に挙げた未是正2件の是正。

ビルドは全て `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o ...`（steering Rules）。

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| 門番テストが削除前は落ち、削除後は通る（順序が報告に書かれている） | OK | ①門番テスト `YamlTestDataParserTest#fileExtensionsHasNoSendSyncTestData` を追加しただけの状態（`unit-test.xml` の `fileExtensions` は残したまま）で `mvn -o clean test -Dtest='YamlTestDataParserTest#fileExtensionsHasNoSendSyncTestData'` → **BUILD FAILURE**、`java.lang.AssertionError: fileExtensions に sendSyncTestData キーが無いこと（setup/common.rst:264）` at `YamlTestDataParserTest.java:1712`、`Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`。②`unit-test.xml` から `fileExtensions` を削除して同コマンド → **BUILD SUCCESS**、`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。順序は報告書 §6-2 の表に記載 | | |
| `mvn -o clean test` が緑。`Skipped 1` は `@Ignore` 1件のまま | OK | `mvn -o clean test` → `BUILD SUCCESS` / `Tests run: 268, Failures: 0, Errors: 0, Skipped: 1`。267（`8eacaa7`）+ 門番テスト1件。`Skipped 1` は `YamlTableDataBuilderTest`（`Tests run: 60, ..., Skipped: 1`）＝ 3-2 の `@Ignore`。実装は直していない | | |
| 6-1 の前後で挙動テストの結果が変わらないことが示されている | OK | `mvn -o clean test -Dtest='YamlTestDataParserTest#getMessage_reservedIdsSetUpMessagesAndExpectedMessages'` を `description` 変更の前後で実行。前: `BUILD SUCCESS` / `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。後: 同じく `BUILD SUCCESS` / `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。報告書 §6-1 の表に記載 | | |
| 報告書 §6 が「是正済み」になっている | OK | `## 6. 指示書18件の外にあった食い違いの是正（2件・是正済み）` / `### 6-1. …（是正済み）` / `### 6-2. …（是正済み）` / `### 6-3. 是正後の全体テスト`。§結論の該当段落も「`#35` で是正済み」に書き換え | | |
| `git status --short` が空、push 済み | OK | 是正後の `git status --short` は変更3ファイルのみ（`ntf-testdata-yaml-schema.json` / `YamlTestDataParserTest.java` / `unit-test.xml`）。`tmp/` は空ディレクトリで git status に出ない（`unit-test.xml:167` の `basePathSettings` が `move` = `file:tmp` のため、テストスイート自身が毎回作る。2026-08-27 ユーザー確認で対応不要と決定）。`find . -name "javac.*.args"` は0件。commit `a008066`（`fix: 報告書 §6 の未是正2件を是正する（#35）`）→ `git push origin feature/ntf-yaml` → `6bdd9e8..a008066  feature/ntf-yaml -> feature/ntf-yaml`。force push はしていない。本 State 更新コミットも同様に push する | | |

## Overall Verdict

- Self-check: OK

---

## 是正の中身

### 6-1. スキーマ `description` 2箇所（`src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json`）

合わせる先は解説書 `implementation/testdata_notation.rst:1151`（`nablarch-document` ピン `5b5c91e`。`git show 5b5c91e:ja/development_tools/testing_framework/implementation/testdata_notation.rst` で現物を読んで確認）。要点は2つ。

1. データタイプ `MESSAGE` の識別子は `setUpMessages`（要求電文）・`expectedMessages`（応答電文）で、**固定である**
2. `sendSyncTestData` はコンポーネント設定ファイルでベースディレクトリに付けるキーで、読み込み単位は「リクエスト ID と同じ名前のディレクトリ配下の `message.yaml`」。**`sendSyncTestData` はデータブロックの識別子ではない**

書き換えたのは `:53`（`properties.messages`）と `:200`（`$defs.message_data.properties.id`）の `description` のみ。行番号は変わっていない。`enum` 等の制約は足していない（指示は `description` の是正のみ）。

`messages` セクションがデータタイプ `MESSAGE` に対応することは実装で確認した — `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:314`（`case MESSAGE: return KEY_MESSAGES;`）、`src/main/java/nablarch/test/core/reader/YamlTestDataParser.java:176`（`getMessage` が `YamlSection.KEY_MESSAGES` を渡す）。

根拠テストは既存の `YamlTestDataParserTest#getMessage_reservedIdsSetUpMessagesAndExpectedMessages`（`src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:1814`）。指示どおり新規の挙動テストは足していない。

### 6-2. `unit-test.xml` の `fileExtensions`（`src/test/resources/unit-test.xml`）

根拠は解説書 `setup/common.rst:264`（`.. important::` の本文。同じくピン `5b5c91e` の現物で確認）— 「`fileExtensions` には `sendSyncTestData` を設定しない」。

`filePathSetting` から `fileExtensions` プロパティを丸ごと削除した（`:170`-`:174` の5行）。残るエントリが `sendSyncTestData` の1件だけで、空の `<map/>` を残す意味が無いため。`FilePathSetting` の `fileExtensions` フィールドは初期値が空の `CaseInsensitiveMap`（`nablarch-core` `6-NEXT-SNAPSHOT` の sources jar `FilePathSetting.java:27`）で、プロパティ未指定でも `getFileExtensions()` は `null` を返さない。

`basePathSettings`（`move` = `file:tmp`）には手を入れていない（解説書が要求していないため範囲外。ユーザー指示にも明記）。

門番テストは `YamlTestDataParserTest#fileExtensionsHasNoSendSyncTestData`（`YamlTestDataParserTest.java:1703`）。2-4 の `yamlInterpretersAreOnlyDocumentedTwo` と同じく `repositoryResource.getComponent(...)` でコンポーネントを取り、`getFileExtensions()` に `sendSyncTestData` キーが無いことを表明する。

**converter への影響は無い**。`pom.xml` に `test-jar` の設定が無く（`grep -n "test-jar" pom.xml` が0件）、`src/test/resources/unit-test.xml` は成果物に入らないため、下流 `nablarch-testing-converter` からは参照されない。

## 実測メモ: `SendSyncSupport` 経路が使えない理由（報告書 §6-2 の末尾の裏取り）

- `nablarch-testing` `3c4bd2a` の `SendSyncSupport.java:346` が `filePathSetting.getBaseDirectory(SEND_SYNC_TEST_DATA_BASE_PATH)` を呼ぶ。`:49` で `SEND_SYNC_TEST_DATA_BASE_PATH = "sendSyncTestData"`、`:46` で `RESPONSE_MESSAGES_SHEET_NAME = "message"`、`:347` が `resourceName = requestId + "/" + RESPONSE_MESSAGES_SHEET_NAME`
- `nablarch-core` `6-NEXT-SNAPSHOT` の `FilePathSetting#getBaseDirectory` は `getBasePathUrl` を呼び、`basePathSettings` にキーが無いと `throw new IllegalArgumentException("Unknown basePathName: " + basePathName)`
- 本モジュールの `unit-test.xml` の `basePathSettings` は `move` の1件だけなので、この経路は例外になる。`#32` が `getMessageWithoutCache` を直接呼ぶ形を採った理由は変わらない
