# task-51 Completion Check

由来: 指示書 `/home/tie303177/work/cowork/nablarch/ntf-doc-renewal/指示/ntf-step4-18-schema-excel-parity.md` §1・§2。

## 着手前の全件基準

`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test`
→ `[INFO] Tests run: 325, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`（指示書の期待 325 件と一致）。

## 変更（構造 8 箇所）

| # | 箇所 | 変更前 | 変更後 |
|---|---|---|---|
| 1-A | `$defs.record_fragment.properties.rows.minItems` | `1` | 削除 |
| 1-B | `$defs.directives.properties.record-length.type` | `"integer"` | `["integer","string"]` |
| 1-B | `$defs.directives.properties.required-decimal-point.type` | `"boolean"` | `["boolean","string"]` |
| 1-B | `$defs.directives.properties.fixed-sign-position.type` | `"boolean"` | `["boolean","string"]` |
| 1-B | `$defs.directives.properties.required-plus-sign.type` | `"boolean"` | `["boolean","string"]` |
| 1-B | `$defs.directives.properties.ignore-blank-lines.type` | `"boolean"` | `["boolean","string"]` |
| 1-B | `$defs.directives.properties.requires-title.type` | `"boolean"` | `["boolean","string"]` |
| 1-B | `$defs.directives.properties.max-record-length.type` | `"integer"` | `["integer","string"]` |

`description` の変更（構造差分ではない）: `rows` の「データは1件以上記述する（…）」を指示書 §1-A の文へ差し替え。
1-B の 7 キーに指示書 §1-B の一文を追記（末尾の句点のみ当方で補った）。

`src/main/java` は変更していない（指示書 §1-B「実装の変更は不要」のとおり）。

## 先に落ちるテスト（RED）

スキーマ変更前に `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest='YamlLoaderTest,YamlFileBuilderTest,YamlMessageBuilderTest'` を実行し、
追加・復元した 4 件がすべて `YamlSchemaValidationException` で落ちることを確認した。

| テスト | RED の理由（実際のエラー本文） |
|---|---|
| `YamlLoaderTest#load_emptyRowsIsAllowed` | `$.setup_files[0].records[0].rows: 少なくとも 1 個の項目が必要ですが、0 が見つかりました` / `$.expected_request_header_messages[0].records[0].rows:` 同上 |
| `YamlLoaderTest#load_directiveValuesCanBeQuotedStrings` | `record-length: string が見つかりました、integer が予期されました` ほか 7 キーすべてが型違反として報告された |
| `YamlFileBuilderTest#buildFileList_noRowsBecomesZeroDataRecords` | フィクスチャ `fileData.yaml` の `noRows` グループが `minItems` 違反でロードできない |
| `YamlMessageBuilderTest#buildMessagePool_emptyRowsBecomesEmptyExpectedMessageList` | フィクスチャ `messageData.yaml` の `noSend001` が `minItems` 違反でロードできない |

`YamlFileBuilderTest#buildFileList_noRowsBecomesZeroDataRecords` とフィクスチャ `noRows` グループは、
`#49` の作業コミット `f3620fc` の直前の内容から復元した（`git show f3620fc` の削除分と同一。
javadoc が引用していたスキーマ description の文言だけ、新しい description の文言に合わせて書き換えた）。

## 削除したテスト

`YamlLoaderTest#load_emptyRowsIsSchemaViolation` とフィクスチャ `YamlLoaderTest/schemaViolation_emptyRows.yaml` を削除した。

**理由**: このテストは `rows: []` が `minItems` 違反で弾かれることを担保していた。`#51` で仕様が逆になり
（`rows: []` は「レコード定義はあるがデータ行が 0 件」の正規の記述）、担保すべき挙動が反転したため。
`#49` A（`f3620fc` の `minItems: 1` 追加。user 確定 2026-08-31）を user 判断 2026-09-07 で**上書き**する。

**削除で失った担保はない**（観点 D の確認結果）:

- 「`rows: []` を弾く」担保は仕様として撤回された（上書き）
- 反転後の挙動は新規 3 件（`load_emptyRowsIsAllowed`／`buildFileList_noRowsBecomesZeroDataRecords`／
  `buildMessagePool_emptyRowsBecomesEmptyExpectedMessageList`）が担保する
- 付随して担保していた「例外メッセージにファイルパスと JSON パスの出所が入り、`getErrors()` が 1 件で
  `getType()` が違反種別を返す」形は、残る 4 件のスキーマ違反テスト（`:604` additionalProperties、
  `:640`・`:673`・`:706` maxItems）が同じ形で担保している
- `records` の `minItems` は無傷（`file_data` = 0、電文 3 セクション = 1）。`fields` の `minItems: 1` も無傷。
  「レコード定義を持たない 0 バイトの空ファイル（`records: []`）」と「データ行 0 件（`rows: []`）」の
  区別は保たれている

## 変更後の全件

`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
→ `[INFO] Tests run: 328, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`
（328 = 基準 325 − 削除 1 ＋ 追加 4）。

## 構造差分の機械比較

変更前（`git show HEAD:…schema.json`。`5a713fb` の内容）と変更後を、`description`／`$comment` を除いて
パースし、パス→値のフラット辞書にして比較した（`checks/task-49.md` と同じ方法）。

- 変更前のみ 8 パス: 上表の 8 箇所（`rows.minItems` と 7 キーの `type`）
- 変更後のみ 14 パス: 7 キーの `type[0]`・`type[1]`
- 値が変わったパス: 0

構造差分は 1-A の 1 箇所と 1-B の 7 箇所だけである。

## レビュー（指示書 §2-6・差分限定 2 観点）

指摘 2 件・採用 1 件・不採用 0 件・報告のみ 1 件。

### 観点 B（整合）— 指示書 §1 の根拠 `file:line` を実物で突き合わせた

| 主張と出典 | 実物での確認 | 判定 |
|---|---|---|
| `RequestTestingMessagingClient.java:337`・`:357`・`:366`@ae989ec が期待ヘッダ件数と実送信件数を突き合わせる | `:337` = `List<DataRecord> expectedHeaderRecords = expectedHeaderPool.getExpectedMessageList();`、`:357` = `if (sendingMessages.size() > expectedHeaderRecords.size())`、`:366` = `if (sendingMessages.size() < expectedHeaderRecords.size())` | 一致 |
| `SendSyncSupport.java:67-69`・`:82-84`@ae989ec — 取引単体テストではレイアウトだけが要求電文のパースに使われる | `:67-69` = `createTestDataInfo(EXPECTED_REQUEST_HEADER_MESSAGES, requestId)` ＋ `headerInfo.messagePool.getFormatter()`、`:82-84` = 同じ形の BODY 版。データ行は参照していない | 一致 |
| 本体のテストデータ `src/test/java/nablarch/test/core/messaging/data/*.xls`@ae989ec は 27 ファイル | `git ls-tree -r --name-only ae989ec …｜grep -c '\.xls$'` = 27 | 一致（44 ブロックの内訳は未検証。xls をパースしていない） |
| `PoiXlsReader.java:123`@ae989ec は `cell.toString()` | `:123` = `String cellValue = cell == null ? "" : cell.toString();` | 一致 |
| `DataFile.java:294-306`・`:325-334`@ae989ec が文字列から型へ変換する | `:294` = `setDirective(String, String)`、`:306` = `directives.put(...)`、`:325` = `convertDirectiveValue(Directive, String)`、`:334` = `Builder.valueOf(valueType, stringValue)` | 一致 |
| `YamlFormatWriter.java:354-362`@878ef9a はディレクティブの値を必ずクォート付きで出す | `:354` = `emitMap(...)`、`:361` が `q(e.getValue())`。`q` は `SCALAR_DUMP`（`DumpSettings…setDefaultScalarStyle(ScalarStyle.DOUBLE_QUOTED)`）で直列化する | 一致 |
| `YamlFileBuilder.java:99-109`@5a713fb が `toStr` で文字列に戻すので実装の変更は不要 | `:99` = `static Map<String, String> mapDirectives(...)`、`:107` = `directives.put(e.getKey(), toStr(e.getValue()))`。`YamlSection.toStr` は `value.toString()` | 一致 |
| `ntf-testdata-yaml-schema.json:459`@5a713fb の `minItems: 1`、および 1-B の 7 キーの行番号（`:340-341`・`:360`・`:364`・`:368`・`:380`・`:384`・`:388-389`） | 全 8 箇所とも指定行に該当する記述があった | 一致 |
| 解説書 `testdata_notation.rst:907`@a0ca03f7 の「データ（1件以上）」 | `:907` = `→ データ（1件以上）` | 一致 |
| 解説書のディレクティブ節 `:964`-`:1017`@a0ca03f7 は値の型に何も定めていない（DIFF ではない） | 全文を読んだ。キーの一覧と各キーの意味のみで、値の YAML スカラ型についての定めはない | 一致 |

**指摘 B-1（報告のみ・仕様は指示書どおり実施）**: 指示書 §1-B は 7 キーすべての description に
「クォート付きの文字列（例 `"10"`・`"true"`）でも記述できる」を足すよう逐語で指定しており、そのとおり適用した。
結果として boolean の 5 キー（`required-decimal-point`・`fixed-sign-position`・`required-plus-sign`・
`ignore-blank-lines`・`requires-title`）にも例 `"10"` が、integer の 2 キー（`record-length`・
`max-record-length`）にも例 `"true"` が載る。利用者が読む仕様文としては、型ごとに例を出し分けた方が読みやすい
（integer は `"10"`、boolean は `"true"`）。**指示書の逐語指定を勝手に変えないため実施はしていない。**
ディレクターの判断を仰ぐ。
**→ 2026-09-07 に採用となり、型ごとに出し分けた**（integer の 2 キーは例 `"10"`、boolean の 5 キーは例 `"true"`）。
`description` のみの変更で構造差分は 0（機械比較で変更前のみ・変更後のみ・値が変わったパスとも 0 件）。

### 観点 D（検証の妥当性）

| 確認 | 結果 |
|---|---|
| 追加した 4 件は変更前に落ちたか | 落ちた（上記「先に落ちるテスト（RED）」の実際のエラー本文を参照）。4 件とも「スキーマ検証で入力がロードできない」ことによる RED であり、スキーマ変更が原因であることが特定できる |
| 1-B の 7 キーが 1 件のテストでまとめて担保されているか | 担保されている。RED 時のエラー本文に 7 キーすべてが個別に列挙された。スキーマ変更が 1 キーでも漏れれば当該キーが再び型違反として報告される |
| 新規テストの変異確認 | `buildMessagePool_emptyRowsBecomesEmptyExpectedMessageList` の対象を `noSend001`（`rows: []`）から `req001`（データ行 1 件）へ差し替えて実行 → `Tests run: 1, Failures: 1`（`期待電文リストが 0 件であること`）。0 件であることを実際に見ていることを確認し、テストは元に戻した |
| `load_emptyRowsIsSchemaViolation` の削除で失った担保 | なし（上記「削除したテスト」を参照） |

**指摘 D-1（採用）**: 指示書 §2-2 は文字列版のフィクスチャについて `record-length: "10"`・`requires-title: "true"`
の 2 キーだけを挙げているが、それでは変更した 7 キーのうち 5 キーがテストで押さえられない。
フィクスチャ `directiveValuesAsStrings.yaml` を 7 キーすべての文字列表記を含む形にし、
テストも 7 キーすべての値を検証する形にした（指示の 2 キーは含む）。

## Completion criteria

| Criterion | Self-check | Evidence |
|---|---|---|
| 追加テストが変更前に落ち、変更後に通る | OK | RED は上表のとおり 4 件。変更後は `Tests run: 328, Failures: 0, Errors: 0` |
| `mvn clean test` 全件緑・`git status --short` 空・push 済み | OK | 328 件緑。commit・push は下記 |
| 構造差分が 1-A の 1 箇所と 1-B の 7 箇所だけであることを機械比較で確認済み | OK | 上記「構造差分の機械比較」 |
| `src/` に解説書への参照を書いていない | OK | 追加したテスト・フィクスチャに `.rst` のパス・行番号・節見出し・逐語引用はない |
| `~/.m2` へ install していない | OK | `mvn install` は実行していない（`mvn clean test` のみ） |
| `src/main/java` を変更していない | OK | `git diff --stat` の変更対象は `src/main/resources` のスキーマ 1 件とテスト・フィクスチャのみ |
