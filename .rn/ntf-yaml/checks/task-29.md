# task-29 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `unit-test.xml` の `yamlInterpreters` が `DateTimeInterpreter` と `CompositeInterpreter` の2つだけ | OK | `src/test/resources/unit-test.xml:58`-`:74`。`NullInterpreter`・`LineSeparatorInterpreter` を削除し、`DateTimeInterpreter`（`systemTimeProvider` = `dateProvider`）と `CompositeInterpreter`→`BasicJapaneseCharacterInterpreter` の2件のみ。テストでも固定: `YamlTestDataParserTest#yamlInterpretersAreOnlyDocumentedTwo`（size==2 と各要素の型を検証） | | |
| 電文用パーサの有無が実測され、あれば `yamlMessagingInterpreters` 相当に揃っている | OK | 実測: `grep -rn "messagingTestDataParser\|MessagingInterpreters" src/` → 0 件（着手時点）。コンポーネントとしての電文用パーサは無い。ただし `YamlMessageBuilderTest#before`（`:56`）が電文用ビルダを **Excel 用の `interpreters`**（`NullInterpreter`・`QuotationTrimmer`・`LineSeparatorInterpreter` 込み）で別に組んでいた。解説書 `setup/common.rst:244`-`:257` に合わせて `unit-test.xml:76`-`:89` に `yamlMessagingInterpreters`（`CompositeInterpreter`→`BasicJapaneseCharacterInterpreter` の1件のみ）を新設し、`YamlMessageBuilderTest#before` をこれに差し替え。あわせて `YamlFileBuilderTest#before` も Excel 用 `interpreters` を参照していたため `yamlInterpreters` に差し替えた（YAML のファイルデータは `testDataParser` = `yamlInterpreters` 経路のため）。`YamlMessageBuilderTest#yamlMessagingInterpretersIsOnlyDocumentedOne` で1件だけであることを固定 | | |
| 是正前に落ち是正後に通るテストが存在する | OK | 是正前（`unit-test.xml` 変更前・テストのみ先行投入）の `mvn -o clean test`: `Tests run: 247, Failures: 4, Errors: 0, Skipped: 0`。落ちた4件 = `YamlTestDataParserTest.yamlInterpretersAreOnlyDocumentedTwo` / `YamlTestDataParserTest.quotedNullIsKeptAsStringAndDistinguishableFromBareNull` / `YamlTableDataBuilderTest.buildListMapRows_quotedNullIsKeptAsString` / `YamlTableDataBuilderTest.buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser`。是正後は全件通過 | | |
| 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 下記「変異確認」の表のとおり、M1〜M8 の8変異すべてで狙ったテストが落ちることを実行して確認した | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | 最終実行: `[INFO] Tests run: 248, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`（`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`） | | |

## Overall Verdict

- Self-check: OK

---

## Method を適用したこと

「先に解説書どおりの設定で期待される挙動を捉えるテストを書き、落ちることを確認してから設定を変える」を守った。

1. `unit-test.xml` を触らずに、解説書 `setup/common.rst:57`-`:81` が定める挙動を捉えるテストとフィクスチャを先に投入した。
2. `mvn -o clean test` を実行し `Tests run: 247, Failures: 4` を確認（4件はいずれも新規/是正した期待値のテスト）。
3. そのあと `unit-test.xml` の `yamlInterpreters` から `NullInterpreter`・`LineSeparatorInterpreter` を外し、`yamlMessagingInterpreters` を新設した。
4. 再実行で残った失敗1件（`setupThrowsNpeWhenBooleanColumnIsQuotedNullString`）を、解説書に照らして「期待値が `NullInterpreter` 前提だった」と判断し、期待値側を解説書に合わせた。設定は戻していない。

## 変えたテスト・フィクスチャの全件表

| # | ファイル | 対象 | 種別 | 何をどう直したか |
|---|---|---|---|---|
| 1 | `src/test/resources/unit-test.xml` | `yamlInterpreters` | 設定 | `NullInterpreter`・`LineSeparatorInterpreter` を削除。コメントを解説書 `:77`/`:81` の文言に合わせた |
| 2 | `src/test/resources/unit-test.xml` | `yamlMessagingInterpreters` | 設定（新設） | 解説書 `:244`-`:257` の電文用リスト（`CompositeInterpreter`→`BasicJapaneseCharacterInterpreter` の1件）を追加 |
| 3 | `YamlTestDataParserTest.java` | `yamlInterpretersAreOnlyDocumentedTwo` | テスト追加 | `yamlInterpreters` が解説書どおり2件・各要素の型であることを固定 |
| 4 | `YamlTestDataParserTest.java` | `quotedNullIsKeptAsStringAndDistinguishableFromBareNull` | テスト追加 | クォート付き `"null"`/`"NULL"` は文字列のまま、クォートなし `null` と値省略 `COL:` だけが Java null になることを公開 API 経路で固定 |
| 5 | `YamlTestDataParserTest.java` | `yamlInterpretersDoNotDoubleProcessQuotes` | javadoc 修正 | `When:` 行の列挙から `NullInterpreter`・`LineSeparatorInterpreter` を外した（挙動・期待値は変更なし） |
| 6 | `YamlTestDataParserTest/quotedValues.yaml` | 先頭コメント / `nullNotationTest` | フィクスチャ | 先頭コメントを是正後の構成に合わせ、`nullNotationTest`（`BARE_NULL`/`OMITTED_NULL`/`QUOTED_NULL`/`UPPER_QUOTED_NULL`）を追加 |
| 7 | `YamlTableDataBuilderTest.java` | `buildListMapRows_quotedNullIsJavaNull` → `buildListMapRows_quotedNullIsKeptAsString` | テスト名・期待値・javadoc | 期待値を `null` から文字列 `"null"` へ変更（解説書 `:81`）。クォートなし `null` は Java null になることを同じテストで対比 |
| 8 | `YamlTableDataBuilderTest.java` | `buildListMapRows_escapedCrIsCarriageReturn` → `buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser` | テスト名・期待値・javadoc | 期待値を「YAML のエスケープ `"\r"` だけが CR になり、バックスラッシュ+r の2文字は変換されない」に変更（解説書 `:77`） |
| 9 | `YamlTableDataBuilderTest.java` | `buildListMapRows_spaceBetweenQuotesIsSpace` | javadoc 修正 | 「`QuotationTrimmer` が外側クォートを除去して」→「YAML のパーサがクォートを構文として処理し」。期待値は変更なし |
| 10 | `YamlTableDataBuilderTest.java` | `buildTableDataList_rowInterpretedToAllNullIsKept` → `buildTableDataList_rowInterpretedToAllBlankIsKept` | テスト名・期待値・フィクスチャ | 空行判定（`dropBlankRows`）が値加工（`interpret`）より前であることの門番を維持するため、`NullInterpreter` 依存をやめ、テスト内の `BlankingInterpreter`（全ての値を空文字にする）で門番を作り直した。期待値は Java null → 空文字 |
| 11 | `YamlTableDataBuilderTest.java` | `buildListMapRows_rowInterpretedToAllNullIsKept` → `buildListMapRows_rowInterpretedToAllBlankIsKept` | 同上（list_maps 経路） | 同上 |
| 12 | `YamlTableDataBuilderTest.java` | `newBlankingBuilder()` / `BlankingInterpreter` | ヘルパー追加 | 上記2件の門番用。`yamlInterpreters` には値を空にするインタープリタが無いため、テスト内で専用に組む |
| 13 | `YamlTableDataBuilderTest.java` | `buildListMapRows_updateTimeAndSetUpTimeConverted` | テスト内の組み立て変更 | ローカルに組むインタープリタ列から `new NullInterpreter()` を外した（このテストの期待値には無関係）。import も削除 |
| 14 | `YamlTableDataBuilderTest.java` | import | 整理 | `NullInterpreter`・`QuotationTrimmer` の import を削除、`InterpretationContext` を追加 |
| 15 | `YamlTableDataBuilderTest/nativeTypes.yaml` | `interpreterTest` | フィクスチャ | `BARE_NULL: null` を追加。`CR_COL: "\\r"` を `LITERAL_CR_COL: "\\r"` と `YAML_CR_COL: "\r"` に分割 |
| 16 | `YamlTableDataBuilderTest/tableData.yaml` | `interpretedToNullRow` → `interpretedToBlankRow`、`interpretedToNullRowListMap` → `interpretedToBlankRowListMap` | フィクスチャ | 2行目の値を `"null"` から `"to_be_blanked"` に変更。コメントから `NullInterpreter` 前提の説明を除去 |
| 17 | `YamlColumnOmissionTest.java` | `setupThrowsNpeWhenBooleanColumnIsQuotedNullString` → `setupSucceedsWhenBooleanColumnIsQuotedNullString` | テスト名・期待値・javadoc | クォート付き `"null"` は Java null にならないため NPE にならず INSERT でき、Boolean カラムには `false` が入ることを固定。クォートなし `null` 明示（`setupThrowsNpeWhenBooleanColumnIsExplicitNull`）との違いを対比 |
| 18 | `YamlColumnOmissionTest/omission.yaml` | `s11` のコメント | フィクスチャ | 「`NullInterpreter` が null へ変換した後に NPE」→「`NullInterpreter` を指定しないため文字列のまま扱われ NPE にならない」 |
| 19 | `YamlMessageBuilderTest.java` | `before()` | 参照先の差し替え | `interpreters`（Excel 用）→ `yamlMessagingInterpreters` |
| 20 | `YamlMessageBuilderTest.java` | `yamlMessagingInterpretersIsOnlyDocumentedOne` | テスト追加 | `yamlMessagingInterpreters` が解説書どおり `CompositeInterpreter` 1件だけであることを固定 |
| 21 | `YamlFileBuilderTest.java` | `before()` | 参照先の差し替え | `interpreters`（Excel 用）→ `yamlInterpreters` |

## 設定変更で落ちたテストと直し方

是正前（テスト先行投入時、`Tests run: 247, Failures: 4`）に落ちた4件は、いずれも今回追加/是正したテスト自身であり、設定を解説書どおりに直したことで通った。

設定変更後に **新たに落ちた既存テストは1件**。

| テスト | 落ちた理由 | 直し方 |
|---|---|---|
| `YamlColumnOmissionTest.setupThrowsNpeWhenBooleanColumnIsQuotedNullString` | `NullInterpreter` を外したことで `BOOL_COL: "null"` が Java null にならず、NPE が起きなくなった | 期待値を解説書側に合わせた（テスト名・javadoc・フィクスチャのコメントも是正）。`setupSucceedsWhenBooleanColumnIsQuotedNullString` に改名し、「NPE にならずに INSERT でき、値は `false` になる」を固定 |

## 是正前に落ちて是正後に通ったテスト

`Tests run: 247, Failures: 4, Errors: 0, Skipped: 0`（`unit-test.xml` 是正前）

- `YamlTestDataParserTest.yamlInterpretersAreOnlyDocumentedTwo`（`Expected: is <2> but: was <4>`）
- `YamlTestDataParserTest.quotedNullIsKeptAsStringAndDistinguishableFromBareNull`（`Expected: is "null" but: was null`）
- `YamlTableDataBuilderTest.buildListMapRows_quotedNullIsKeptAsString`（`Expected: is "null" but: was null`）
- `YamlTableDataBuilderTest.buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser`（バックスラッシュ+r の2文字が CR に変換されてしまう）

是正後: `Tests run: 248, Failures: 0, Errors: 0, Skipped: 0`

## 変異確認（step D）

各変異は「適用 → `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → 復元」で実施した。

| 変異 | 崩した内容 | 結果 |
|---|---|---|
| M1 | `unit-test.xml` の `yamlInterpreters` に `NullInterpreter` を戻す | `Tests run: 248, Failures: 3, Errors: 1`。落ちた: `yamlInterpretersAreOnlyDocumentedTwo` / `quotedNullIsKeptAsStringAndDistinguishableFromBareNull` / `buildListMapRows_quotedNullIsKeptAsString` / `setupSucceedsWhenBooleanColumnIsQuotedNullString`（NullPointerException） |
| M2 | `unit-test.xml` の `yamlInterpreters` に `LineSeparatorInterpreter` を戻す | `Tests run: 248, Failures: 2`。落ちた: `yamlInterpretersAreOnlyDocumentedTwo` / `buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser` |
| M3 | `unit-test.xml` の `yamlMessagingInterpreters` に `NullInterpreter` を足す | `Tests run: 248, Failures: 1`。落ちた: `yamlMessagingInterpretersIsOnlyDocumentedOne` |
| M4a | `tableData.yaml` の `interpretedToBlankRow` 2行目の値を全て `""` にする（門番の前提を崩す） | `Tests run: 248, Failures: 1`。落ちた: `buildTableDataList_rowInterpretedToAllBlankIsKept`（行が空行判定で消えて 2 行 → 1 行） |
| M4b | `tableData.yaml` の `interpretedToBlankRowListMap` 2件目の値を全て `""` にする | `Tests run: 248, Failures: 1`。落ちた: `buildListMapRows_rowInterpretedToAllBlankIsKept` |
| M5 | `nativeTypes.yaml` の `YAML_CR_COL: "\r"` を `"\\r"`（2文字）にする | `Tests run: 248, Failures: 1`。落ちた: `buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser` |
| M6 | `omission.yaml` の `s11` の `BOOL_COL: "null"` をクォートなし `null` にする | `Tests run: 248, Failures: 0, Errors: 1`。落ちた: `setupSucceedsWhenBooleanColumnIsQuotedNullString`（NullPointerException） |
| M7 | `quotedValues.yaml` の `QUOTED_NULL: "null"` をクォートなし `null` にする | `Tests run: 248, Failures: 1`。落ちた: `quotedNullIsKeptAsStringAndDistinguishableFromBareNull` |
| M8 | `nativeTypes.yaml` の `UPDATE_COL: "${updateTime}"` を `"${updateTimeX}"` にする | `Tests run: 248, Failures: 1`。落ちた: `buildListMapRows_updateTimeAndSetUpTimeConverted` |

変異を当てていない変更点（理由つき）:

- 表 #5・#9（javadoc の文言修正のみ。期待値・挙動は変えていない）
- 表 #21 `YamlFileBuilderTest#before` の参照先差し替え。`interpreters` に戻しても落ちるテストは無い（この差し替えを守るテストは無い）。`yamlInterpreters` 側の内容は M1・M2 で守られている

## `NullInterpreter` に言及したスキーマ description の洗い出し（**変更していない**）

対象: `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json`（全 434 行）。`grep -in "nullinterpreter"` の結果は **`:108` の1行のみ**（`:136` には `NullInterpreter` の語は無い）。該当行は `properties.rows`（テーブル系）の `description` で、`NullInterpreter` に言及する文は次の2つ。

- `:108`「クォート付きの `\"null\"` や大文字を含む `NULL` / `Null` は文字列としてロードされ、NullInterpreter が null へ変換する（大文字・小文字は区別しない）。」
- `:108`「ただし Boolean 型カラムだけは例外で、原因が (2) の行ごとの省略であるか、クォートなし小文字 `null`・`COL:` の値省略・クォート付き `\"null\"`（NullInterpreter 変換後）のいずれであるかを問わず、値が null になれば NULL を扱えず NullPointerException になるため、true/false のいずれかの値を明示すること（null を明示しても NullPointerException を防げない）。」

関連（`NullInterpreter` の語は無いが同じ主張をしている箇所。あわせて未変更）:

- `:136`（`list_maps` の `rows` の description）「Java null（クォートなしの `null`・値を省略した `COL:`）と `\"null\"` / `NULL` は、いずれも空文字ではないため非空として扱われ、これだけの行は残り、マップの値が Java null になる。」

いずれも本タスクでは変更していない（指示書 2-5 の対象は `:410`・`:108`・`:136` の3件であり、`NullInterpreter` への言及の扱いは範囲外）。

## `src/main` に残った `NullInterpreter` 前提の記述（**変更していない**）

- `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:174` — `dropBlankRows` の javadoc「値加工を通すと空になる値（例えば `NullInterpreter` が Java null へ変換する `\"null\"`）だけを持つ行も、行としては保持される」。YAML 経路では `NullInterpreter` を指定しないため、この例は成立しなくなった。本タスクは `src/main` を変更しない指示のため未変更

## コーディネーター独立レビュー

Step 4 では4観点レビューを回さない（指示書 §7）。コーディネーターがコミット済み差分を独立に読み、ビルドを自分で実行し、解説書をピンで読んで検証した。

| 観点 | 判定 | 根拠 |
|---|---|---|
| 差分がタスクの範囲に収まっている | OK | `git diff 91c89f8..90d4b24` は `src/test/` 配下10ファイルのみ。`src/main`・スキーマ・`pom.xml`・解説書・`nablarch-testing`・`nablarch-testing-converter` への書き込みなし |
| `yamlInterpreters` が解説書どおり | OK | `unit-test.xml:58`-`:74` が `DateTimeInterpreter` と `CompositeInterpreter`→`BasicJapaneseCharacterInterpreter` の2件のみ。解説書 `5b5c91e` の `setup/common.rst:57`-`:68` の設定例および `:77`「この2つだけでよい」と一致。`:81` の禁止（`NullInterpreter`）も満たす |
| 電文用パーサの実測と是正 | OK | `messagingTestDataParser` は本モジュールに存在しない（コンポーネント定義0件）が、`YamlMessageBuilderTest#before` が Excel 用 `interpreters`（`NullInterpreter` 込み）で電文用ビルダを組んでいた。`unit-test.xml:76`-`:89` に `yamlMessagingInterpreters`（`CompositeInterpreter` 1件）を新設して参照させた。解説書 `setup/common.rst:244`-`:257`・`:259`「この1つだけでよい」と一致 |
| 期待値を実装側でなく解説書側に寄せている | OK | `YamlColumnOmissionTest` の Boolean カラム＋クォート付き `"null"` のテストを「NPE になる」→「NPE にならず `false` が入る」に反転。解説書 `setup/common.rst:81`（`NullInterpreter` を指定してはならない）から導かれる帰結であり、実装に合わせたのではない |
| `NullInterpreter` 前提の門番を失っていない | OK | 空行判定が値加工より前であることの門番（`buildTableDataList_rowInterpretedToAllNullIsKept` ほか）を、テスト内で定義する `BlankingInterpreter`（どんな値も空文字にする）に置き換えて維持。解説書の2つでは値を空にできないための代替であり、担保している性質は変わらない |
| 変異確認が実施されている | OK | M1〜M8 の8件。`yamlInterpreters` に `NullInterpreter`/`LineSeparatorInterpreter` を戻す、`yamlMessagingInterpreters` に `NullInterpreter` を足す、門番フィクスチャ・期待値を崩す、をすべて実行し狙ったテストが落ちた |
| ビルド（コーディネーター自身の実行） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 248, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（2026-08-26 コーディネーターが独立実行） |

### #30 へ引き継いだ波及（2-4 の是正で事実に反することになった記述）

- スキーマ `:108`（「クォート付きの `"null"` … は文字列としてロードされ、NullInterpreter が null へ変換する」「クォート付き `"null"`（NullInterpreter 変換後）」）と `:136`（「`"null"` / `NULL` は…マップの値が Java null になる」）。**`:108`・`:136` は指示書 2-5 の名指し3件に含まれる**ため、#30 step B2 として登録した
- `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:174` の javadoc（`NullInterpreter` を例示）。#30 step B3 として登録した

### 報告のみ（範囲外・未変更）

`src/test/resources/unit-test.xml:170`-`:174` の `filePathSetting` が `fileExtensions` に `sendSyncTestData` = `xls` を設定している。解説書 `5b5c91e` の `setup/common.rst:263`（important）は「`fileExtensions` には `sendSyncTestData` を設定しない。YAML 形式ではリクエストIDと同じ名前のディレクトリを参照するため、拡張子を設定するとテストデータが見つからず、テストの実行時に例外が発生する」と定める（コーディネーターがピンで確認）。指示書 2-4 の「やること」は `yamlInterpreters` に限られており、この項目は18件のいずれにも該当しないため未変更。**#32 の 3-11（`implementation/deal_unit_test/mom.rst:72`。リクエストIDと同じ名前のディレクトリ配下の `message.yaml`）に影響しうる**ため、#32 の作業指示に申し送る。

## Overall Verdict（コーディネーター）

- コーディネーター独立レビュー: OK
- Ready to check off: Yes
