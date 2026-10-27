# task-32 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| 3-7〜3-13 の7件すべてについてテストが存在する | OK | 3-7: `YamlTableDataBuilderTest#buildTableDataList_groupIdIsMatchedExactly`（`:1744`）、3-8: `YamlLoaderTest#load_prefixMatchedTopLevelKeyIsSchemaViolation`（`:578`）、3-9: `YamlTableDataBuilderTest#buildTableDataList_collectionIsNotTruncatedByInterleavedGroupId`（`:1779`）、3-10: `YamlTestDataParserTest#getMessage_reservedIdsSetUpMessagesAndExpectedMessages`（`:1782`）、3-11: `YamlTestDataParserTest#getMessageWithoutCache_readsMessageYamlUnderRequestIdDirectory`（`:1829`）、3-12: `YamlTestDataParserTest#getListMap_resolvesFileNameAndUnitNameToNestedYaml`（`:1867`）、3-13: `YamlTableDataBuilderTest#buildListMapRows_argsIndexKeyIsNotExcludedAsMarker`（`:1808`）。計7メソッド | | |
| 落ちたものは `@Ignore` ＋ `NTF-DOC:` 印つきの理由で記録されている（実装は直していない） | OK | 7件とも1回目の実行で通ったため、本タスクで新設した `@Ignore` は0件（`Skipped: 1` は #31 の既存 `@Ignore` 1件のみ）。`git status --porcelain -- src/main src/test/resources pom.xml` が空（実装・設定・pom は未変更） | | |
| 通った各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 下記「変異確認」のとおり、通った7件すべての期待値を同時に崩して1度実行し、7件すべてが Failure になった（`Tests run: 267, Failures: 7, Errors: 0, Skipped: 1`）。崩した箇所は実行後に元へ戻し、再実行で緑を確認 | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `BUILD SUCCESS` / `Tests run: 267, Failures: 0, Errors: 0, Skipped: 1`（着手前ベースライン `260, Skipped 1` に対し +7 テスト・Skipped 増減なし） | | |

## Overall Verdict

- Self-check: OK

## 7件の結果

| # | テスト | 結果 | `@Ignore` の理由（原文） |
|---|---|---|---|
| 3-7 | `YamlTableDataBuilderTest#buildTableDataList_groupIdIsMatchedExactly` | 通った | — |
| 3-8 | `YamlLoaderTest#load_prefixMatchedTopLevelKeyIsSchemaViolation` | 通った | — |
| 3-9 | `YamlTableDataBuilderTest#buildTableDataList_collectionIsNotTruncatedByInterleavedGroupId` | 通った | — |
| 3-10 | `YamlTestDataParserTest#getMessage_reservedIdsSetUpMessagesAndExpectedMessages` | 通った | — |
| 3-11 | `YamlTestDataParserTest#getMessageWithoutCache_readsMessageYamlUnderRequestIdDirectory` | 通った | — |
| 3-12 | `YamlTestDataParserTest#getListMap_resolvesFileNameAndUnitNameToNestedYaml` | 通った | — |
| 3-13 | `YamlTableDataBuilderTest#buildListMapRows_argsIndexKeyIsNotExcludedAsMarker` | 通った | — |

`@Ignore` にしたテストは0件のため、「`@Ignore` は変異確認の対象外」に該当するものは無い。

## 3-8 でアサートした例外

- 型: `YamlSchemaValidationException`（`YamlLoader.load` が送出）
- メッセージの要点:
  - ファイルパス（`YamlLoaderTest/schemaViolation_prefixMatchedTopLevelKey`）を含むこと
  - 違反したキー名 `setup_tables_extra` を含むこと
  - `getErrors()` が1件で、その `ValidationMessage#getType()` が `additionalProperties` であること
- 実際のエラー本文（変異実行時の surefire 出力に現れたもの）: `$: プロパティ 'setup_tables_extra' がスキーマで定義されておらず、スキーマでは追加のプロパティが許可されていません`

## 3-11 と `fileExtensions` の `sendSyncTestData` = `xls`

- 阻まれた。ただし直接の障害は `fileExtensions` の手前にある。`src/test/resources/unit-test.xml:163`-`:175` の `filePathSetting` は `basePathSettings` に `move` しか持たず `sendSyncTestData` を持たない。このため `SendSyncSupport`（`nablarch-testing` `3c4bd2a` の `SendSyncSupport.java:346`・`:348`）が呼ぶ `FilePathSetting#getBaseDirectory("sendSyncTestData")` / `#getFileIfExists("sendSyncTestData", requestId)` は、いずれも `IllegalArgumentException: Unknown basePathName: sendSyncTestData` を送出する（`unit-test-yaml.xml` を読ませた使い捨てのプローブテストで実測。実測後に削除）。仮に `basePathSettings` を足したとしても、`unit-test.xml:170`-`:174` の `fileExtensions` が `sendSyncTestData` = `xls` を設定しているため `getFileIfExists` は `<base>/<リクエストID>.xls` を探しに行き、YAML 形式のディレクトリを返さない（解説書 `setup/common.rst:263` の important が禁じている設定）。いずれも設定ファイルの変更が要るため、指示どおり設定は変えていない。
- 回避: `SendSyncSupport` を経由せず、`SendSyncSupport` が組み立てるのと同じ引数の組で `YamlTestDataParser#getMessageWithoutCache` を直接呼んだ。`SendSyncSupport.java:347` が `resourceName = リクエストID + "/" + "message"` を組み立て、`:393` がその `basePath`・`resourceName` で `getMessageWithoutCache` を呼ぶ。テストも `getMessageWithoutCache(<base>, "RM21AA0101/message", RESPONSE_BODY_MESSAGES, "RM21AA0101")` を呼ぶ。
- 「`<リクエストID>.yaml` のような配置では読まれない」ことは囮ファイルで押さえた。`sendSyncTestData/RM21AA0101/message.yaml`（`RESULT_KEY: "FROM_DIR01"`）と `sendSyncTestData/RM21AA0101.yaml`（`RESULT_KEY: "FROM_FILE1"`）の両方を置き、返る値が `FROM_DIR01` であることを表明する。囮ファイルの存在自体もテスト内で `File#isFile()` により表明しており、囮が消えると空振りになる前に落ちる。
- `@Ignore` にはしていない（テストは通っている）。

## 変異確認

- コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
- 通った7メソッドの期待値を同時に崩して1度だけ実行した
- 結果: `Tests run: 267, Failures: 7, Errors: 0, Skipped: 1` / `BUILD FAILURE`。崩した7件がすべて Failure になり、他は増えなかった
- 実行後に崩した箇所を元へ戻し、再実行で `Tests run: 267, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS` を確認した

| # | 崩した期待値 | 落ちたテスト（surefire の出力） |
|---|---|---|
| 3-7 | `exact.size(), is(1)` → `is(2)` | `YamlTableDataBuilderTest.buildTableDataList_groupIdIsMatchedExactly:1752 case01 に前方一致する case010 は収集されず 1 件だけ返ること` |
| 3-8 | `errors.get(0).getType(), is("additionalProperties")` → `is("MUTATED")` | `YamlLoaderTest.load_prefixMatchedTopLevelKeyIsSchemaViolation:591 スキーマに定義されていないトップレベルキーとして弾かれること（additionalProperties 違反）: $: プロパティ 'setup_tables_extra' がスキーマで定義されておらず、スキーマでは追加のプロパティが許可されていません` |
| 3-9 | `result.size(), is(2)` → `is(1)` | `YamlTableDataBuilderTest.buildTableDataList_collectionIsNotTruncatedByInterleavedGroupId:1787 別の group_id を挟んでも打ち切られず 2 件とも収集されること` |
| 3-10 | `getString("REQUEST_KEY"), is("SETUPKEY01")` → `is("MUTATED")` | `YamlTestDataParserTest.getMessage_reservedIdsSetUpMessagesAndExpectedMessages:1790 setUpMessages の電文本文が記述どおりであること` |
| 3-11 | `getString("RESULT_KEY"), is("FROM_DIR01")` → `is("FROM_FILE1")`（囮ファイルの値） | `YamlTestDataParserTest.getMessageWithoutCache_readsMessageYamlUnderRequestIdDirectory:1845 <リクエストID>/message.yaml が読み込み単位になること（囮の <リクエストID>.yaml が読まれると FROM_FILE1 になる）` |
| 3-12 | `result.size(), is(2)` → `is(1)`（囮ファイルの行数） | `YamlTestDataParserTest.getListMap_resolvesFileNameAndUnitNameToNestedYaml:1878 <ディレクトリ>/<ファイル名>/<読み込み単位の名前>.yaml が読まれること（囮の <ディレクトリ>/<ファイル名>.yaml が読まれると 1 件になる）` |
| 3-13 | `row.get("args[0]"), is("arg0Value")` → `is("MUTATED")` | `YamlTableDataBuilderTest.buildListMapRows_argsIndexKeyIsNotExcludedAsMarker:1820 args[0] の値が取得できること` |

## 追加したフィクスチャ

| ファイル | 用途 |
|---|---|
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/tableData.yaml`（追記） | 3-7（`setup_tables` の `group_id: case01` / `case010`）、3-9（`expected_tables` の `interleavedA` / `interleavedB` / `interleavedA`）、3-13（`list_maps` の `argsColumnTest`） |
| `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest/schemaViolation_prefixMatchedTopLevelKey.yaml`（新規） | 3-8（トップレベルキー `setup_tables_extra` のみ） |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/messageData.yaml`（追記） | 3-10（`messages` の `id: setUpMessages` / `id: expectedMessages`） |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/sendSyncTestData/RM21AA0101/message.yaml`（新規） | 3-11（読み込み単位） |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/sendSyncTestData/RM21AA0101.yaml`（新規） | 3-11（囮。読まれないこと） |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/otherDir/CommonTestData/employees.yaml`（新規） | 3-12（読み込み単位） |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/otherDir/CommonTestData.yaml`（新規） | 3-12（囮。読まれないこと） |

## 解説書が誤っていると判断した項目

なし。7件とも解説書の記述どおりの挙動だった。

## コーディネーター独立レビュー

Step 4 では4観点レビューを回さない（指示書 §7）。コーディネーターがコミット済み差分を独立に読み、ビルドを自分で実行して検証した。

| 観点 | 判定 | 根拠 |
|---|---|---|
| 差分がタスクの範囲に収まっている | OK | `git diff 96ab7c8..bb94be5 --stat` は `src/test/java/` 配下10ファイル、433 insertions / 0 deletions。`git status --porcelain -- src/main src/test/resources pom.xml` が空であることをコーディネーターも確認 |
| 7件すべてにテストがある | OK | 3-7〜3-13 の7メソッド。3-1〜3-6（#31 の担当）には触れていない |
| 実装で直していない | OK | `src/main` の差分0。設定ファイル（`src/test/resources/`）の差分も0 |
| 新規 `@Ignore` が無いこと | OK | `grep -rn '@Ignore' src/test/java/` のヒットは1件のみ（#31 の既存分）。7件とも通ったため新設なし |
| 3-11 が設定を変えずに押さえられている | OK | `unit-test.xml` の `filePathSetting` は `basePathSettings` に `sendSyncTestData` を持たず、`SendSyncSupport`（`nablarch-testing@3c4bd2a` `:346`・`:348`）経由では `IllegalArgumentException: Unknown basePathName` になる。`SendSyncSupport.java:347` と同じ `resourceName = <リクエストID>/message` で `getMessageWithoutCache` を直接呼ぶ形にして回避。囮 `sendSyncTestData/RM21AA0101.yaml` を置き「`<リクエストID>.yaml` では読まれない」ことも押さえている。設定は未変更 |
| 3-8 が「何か例外が出た」で終わっていない | OK | 型 `YamlSchemaValidationException`、`getErrors()` 1件、`ValidationMessage#getType()` が `additionalProperties`、メッセージにファイルパスと違反キー `setup_tables_extra` を含むことまでアサート |
| 変異確認が実施されている | OK | 7件同時に期待値を崩して `Tests run: 267, Failures: 7, Errors: 0, Skipped: 1`。崩した7件だけが落ち、復元後に緑を再確認 |
| ビルド（コーディネーター自身の実行） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `BUILD SUCCESS`。`target/surefire-reports/*.txt` の集計で `Tests run: 267, Failures: 0, Errors: 0, Skipped: 1`（2026-08-26 コーディネーターが独立実行） |

### 未是正の食い違い（#33 の報告へ引き継ぐ。ユーザー判断待ち）

`src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` の2箇所が、解説書 `5b5c91e` の `implementation/testdata_notation.rst:1149` と食い違う（コーディネーターが `$defs` を展開して現物を確認）。

- `$defs.message_data.id` の `description`: 「メッセージID。`sendSyncTestData/{requestId}/message` 形式で指定する（末尾の `message` は固定のパスセグメント）」
- `properties.messages` の `description`: 「…id は `sendSyncTestData/{requestId}/message` 形式で指定する」

解説書 `:1149` は「データタイプ `MESSAGE` の識別子として `setUpMessages`（要求電文）・`expectedMessages`（応答電文）を指定する。**これらの識別子は固定である**」と定め、`sendSyncTestData/{requestId}/message` は**読み込み単位のパス**であって「**`sendSyncTestData` はデータブロックの識別子ではない**」と明記している。本タスクの 3-10（`getMessage_reservedIdsSetUpMessagesAndExpectedMessages`）が、`messages` の `id` に `setUpMessages`・`expectedMessages` を書いて取得できることを実測で通しており、スキーマの記述と正面から食い違う。

**指示書 2-5 の名指し（`:410`・`:108`・`:136`）にも、2-3 の波及先として追加した `:365` にも該当しない。** また Step 4 の是正が falsify したものでもなく、着手前から存在する食い違いである。**範囲の判断を持たないため、直さずユーザー判断を仰ぐ。** 本タスクでは未変更。

## Overall Verdict（コーディネーター）

- コーディネーター独立レビュー: OK
- Ready to check off: Yes（スキーマ2箇所はユーザー判断待ちとして別建て。#32 の Completion criteria には含まれない）
