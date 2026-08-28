# Step 4 第2回 報告書

対象: `nablarch-testing-yaml`（ブランチ `feature/ntf-yaml`、着手時 `3ee39c9`）
指示書: `nablarch-document@origin/ntf-yaml-support` の `.rn/20260724-ntf-yaml-support/ntf-step4-06-nablarch-testing-yaml-2.md`
参照点（ピン）: 解説書 `nablarch-document@afa4f9e`（パスは `ja/development_tools/testing_framework/…`）／`nablarch-testing@3c4bd2a`／`nablarch-testing-converter@d611bec`

着手前ベースライン（実測。`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`）:
`Tests run: 268, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。`@Test` 268件・`@Ignore` 1件。

---

## 1. 2-1〜2-5 の「着手前に特定すること」の結果

走査対象は `src/test/**/*.yaml`（55ファイル）と `src/test/java/**/*.java` の全件。
YAML はテキスト検索ではなく PyYAML の `compose()` で構文木にしてから、セクション種別・エントリ・行を特定した。

### 2-1. 末尾に `null` を置いて `null` を期待している既存テスト — **0件**

`setup_files`・`expected_files`・`messages`・`expected_request_header_messages`・`expected_request_body_messages`・
`response_header_messages`・`response_body_messages` の全エントリの `records[].rows[]` を走査し、
末尾要素が YAML の null タグ（クォートなし `null`／`~`／値の省略）である行を探した。**該当0件。**

`nullValue()` を assert する既存テストも10箇所あるが、いずれもファイル・電文のデータ行ではない
（`YamlColumnOmissionTest` のテーブル値7件、`YamlSectionTest` の `interpret`/`objectToString` の引数 null 2件、
`YamlFileBuilderTest.java:570` の `record-length` ディレクティブ未設定1件、`YamlTestDataParserTest.java:1423` の `list_maps` 値1件）。

→ 2-1 の是正で期待値を変える既存テストは、見込みとして0件。是正後の実測で確かめる。

### 2-2. 電文の1エントリに `records` を2つ以上書いているフィクスチャ — **3エントリ**

| ファイル | 行 | セクション | id | records 数 |
|---|---|---|---|---|
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/messageData.yaml` | 31 | `messages` | `fwHeaderRecordType001` | 2 |
| 同上 | 53 | `messages` | `legacyFwHeaderRecord001` | 2 |
| 同上 | 163 | `response_body_messages` | `sync001`（`group_id: fwHeaderSync`） | 2 |

`YamlTestDataParserTest/schemaFullCoverage.yaml` は電文セクションの全エントリが `records` 1つで、該当なし（確認済み）。

**波及**: 3件とも同一ファイル `YamlTestDataParserTest/messageData.yaml` にあり、スキーマ検証は
`YamlLoader.load` でファイル単位に走る。このファイルは `YamlTestDataParserTest` の**16箇所**から読まれるため、
`maxItems: 1` を入れた時点でロード自体が失敗し、16箇所すべてが落ちる。フィクスチャの書き換えが必要。

直接に意味が変わるテスト（3件）:
`getMessage_fwHeaderRecordTypeIsNotSkipped`（`YamlTestDataParserTest.java:940`）、
`getMessage_legacyFwHeaderRecordCausesRecordLengthMismatch`（同 `:1112`）、
`fwHeaderSync` を使う送信同期のテスト（同 `:990`）。

### 2-3. `fw_header:` の既定4つ以外のキー — **キー3件 / 落ちる見込みのテスト4件**

| ファイル | 行 | id | キー |
|---|---|---|---|
| `…/yaml/YamlMessageBuilderTest/customFwHeaderData.yaml` | 9 | `req001` | `customField` |
| `…/yaml/YamlMessageBuilderTest/fwHeaderMapData.yaml` | 14 | `req001` | `customProjectKey` |
| 同上 | 40 | `numericValues001` | `boolFlag` |

`reader.fwHeaderfields` を設定しているテストは **0件**（`grep -rn 'fwHeaderfields' src/test` が0ヒット）。
したがって上のキーを持つエントリを組み立てるテストは是正後に落ちる。

落ちる見込みのテスト（4件。いずれも `YamlMessageBuilderTest`）:
`buildMessagePool_customFwHeaderFields`（`:792`。`customField` の保持を assert）、
`buildMessagePool_fwHeaderMapAllKeysRetainedIncludingCustom`（`:824`。`customProjectKey` の保持を assert）、
`buildMessagePool_fwHeaderMapReadableWithoutHeaderRecord`（`:854`。`req001` を組むため巻き込まれる）、
`buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues`（`:985`。`boolFlag` の保持を assert）。

落ちない見込み（5件。既定4つのみ、または `fw_header` を持たないエントリを組む）:
`buildMessagePool_expectedRequestBodyMessagesReturnsEmptyFwHeader`、
`buildMessagePool_responseBodyMessagesReturnsEmptyFwHeader`、
`buildMessagePool_expectedRequestHeaderMessagesReturnsEmptyFwHeader`、
`buildMessagePool_responseHeaderMessagesReturnsEmptyFwHeader`、
`buildMessagePool_messagesWithoutFwHeaderMapReturnsEmptyFwHeader`。

なお `fw_header` を持つのはスキーマ上 `$defs.message_data` だけである
（`expected_request_message_data`・`group_message_data` は `group_id`・`id`・`directives`・`records` のみ）。
つまり検査を足す先は `YamlMessageBuilder.convertFwHeader` の1箇所でよい。

### 2-4. `isBlankRow`／`dropBlankRows` の挙動を期待値に書いた既存テスト

まず現行の実装を実測した。`YamlSection.isBlankRow`（`:202`-`:209`）は
`objectToString(value)` が `null` を返す（＝Java null）か、空でない文字列なら非空と判定する。
したがって現行の扱いは **`{}` → 読み飛ばす／全値 `""` → 読み飛ばす／全値 Java null → 残す／マーカーカラムだけに値 → 残す**。
是正後は **`{}` だけ読み飛ばす**ので、**変わるのは「全値 `""`」の行だけ**である。

フィクスチャの走査結果（テーブル系・`list_maps` の全 `rows` 要素、28件）:

| 種別 | 件数 | 是正で挙動が変わるか |
|---|---|---|
| 空マッピング `{}` | 10 | 変わらない（前後とも読み飛ばす） |
| 全値が `""` | 15 | **変わる**（読み飛ばす → 残す） |
| 全値が Java null | 2 | 変わらない（前後とも残す） |
| マーカーカラムだけに値 | 1 | 変わらない（前後とも残す） |

「全値 `""`」15件の内訳:

| ファイル | 行 | セクション | エントリ |
|---|---|---|---|
| `…/yaml/YamlTableDataBuilderTest/tableData.yaml` | 97 | `setup_tables` | `blankValueRowLeading` |
| 同上 | 114 | `setup_tables` | `blankValueRowMiddle` |
| 同上 | 134 | `setup_tables` | `partiallyBlankValueRow` |
| 同上 | 251 | `list_maps` | `blankValueRowLeadingListMap` |
| 同上 | 261 | `list_maps` | `blankValueRowMiddleListMap` |
| 同上 | 272 | `list_maps` | `partiallyBlankValueRowListMap` |
| 同上 | 299 | `list_maps` | `allBlankRowsListMap` |
| 同上 | 371 | `expected_tables` | `blankValueRowLeadingExpected` |
| 同上 | 388 | `expected_tables` | `blankValueRowMiddleExpected` |
| `…/yaml/YamlTableDataBuilderTest/completedTable.yaml` | 29 | `expected_complete_tables` | `blankValueRowComplete` |
| 同上 | 34 | `expected_complete_tables` | `blankValueRowComplete` |
| 同上 | 40 | `expected_complete_tables` | `blankValueRowComplete` |
| `…/db/YamlColumnOmissionTest/omission.yaml` | 55 | `setup_tables` | `s4a` |
| 同上 | 73 | `setup_tables` | `s4b` |
| `…/yaml/YamlTableDataBuilderTest/tableData.yaml` | 161 は全値 null のため対象外 | — | — |

（表の最終行は対象外の注記。「全値 `""`」は14行＋`completedTable.yaml:31`/`:37` を除く。是正時に実測で確定する）

挙動が変わる見込みの既存テスト（**14件**）:

- `YamlSectionTest`（Java で行を組み立てているもの。2件）
  - `dropBlankRows_removesEmptyMappingAndAllBlankValueRows`（`:473`。全値 `""` の行が消えることを assert → 残る）
  - `dropBlankRows_keepsRowHavingOnlyWhitespaceValue`（`:522`。半角スペース行だけが残ることを assert → 空文字行も残る）
- `YamlTableDataBuilderTest`（10件）
  - `buildTableDataList_blankValueRowLeadingExcluded`（`:1292`）
  - `buildTableDataList_blankValueRowMiddleExcluded`（`:1319`）
  - `buildTableDataList_partiallyBlankValueRowKept`（`:1345`）
  - `buildTableDataList_blankValueRowLeadingInExpectedTableExcluded`（`:1372`）
  - `buildTableDataList_blankValueRowMiddleInExpectedTableExcluded`（`:1399`）
  - `buildTableDataList_blankValueRowInExpectedCompleteTableExcluded`（`:1453`）
  - `buildListMapRows_blankValueRowLeadingExcluded`（`:1486`）
  - `buildListMapRows_blankValueRowMiddleExcluded`（`:1511`）
  - `buildListMapRows_partiallyBlankValueRowKept`（`:1534`）
  - `buildListMapRows_allBlankRowsReturnsEmptyList`（`:1687`）
- `YamlColumnOmissionTest`（2件相当）
  - `columnNamesDependOnRowOrderAfterBlankRowRemoval`（`:174`）ほか `omission.yaml` の `s4a`・`s4b` を使うテスト

変わらない見込みの既存テスト（`{}`・全値 null・マーカーカラムのみを扱うもの）:
`dropBlankRows_keepsRowHavingAnyNonBlankValue`（`:498`）、`dropBlankRows_keepsRowHavingOnlyMarkerColumnValue`（`:549`）、
`dropBlankRows_removesNonMappingRows`（`:570`）、`dropBlankRows_keepsRowHavingOnlyNullValues`（`:595`）、
`resolveColumns_emptyRowsReturnsEmptyList`（`:363`）、`resolveColumns_allEmptyMappingRowsReturnsEmptyList`（`:381`）、
`buildTableDataList_emptyRowEntrySkipped`（`:429`）、`buildListMapRows_emptyRowEntrySkipped`（`:947`）、
`buildTableDataList_leadingEmptyRow*`（`:1143`・`:1181`・`:1220`）、`buildListMapRows_leadingEmptyRowKeepsFollowingRows`（`:1256`）、
`buildTableDataList_emptyRowEntryInExpectedTableSkipped`（`:1423`）、`buildTableDataList_rowInterpretedToAllBlankIsKept`（`:1564`）、
`buildListMapRows_rowInterpretedToAllBlankIsKept`（`:1631`）ほか。件数は是正後の実測で確定する。

### 2-5. 2文字の `\` ＋ `r` を値に置いている既存フィクスチャ・テスト — **フィクスチャ1件 / テスト1件**

- フィクスチャ: `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/nativeTypes.yaml:16`
  の `LITERAL_CR_COL: "\\r"`（YAML のダブルクォート内エスケープ。ロード後は `\` と `r` の2文字）
- テスト: `YamlTableDataBuilderTest#buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser`（`:591`）。
  `:603` で `is("\\r")`（Java 文字列リテラル。実体は2文字）を assert している

Java ソース中の `"\\r"` リテラルも全走査した。上記テストの `:600`（assert の説明文）と `:603`（期待値）の2箇所のみ。
`src/main` では `ntf-testdata-yaml-schema.json:290` の `description` 本文に `"\r\n"` の説明が出てくるが、これは値ではなく説明文。
実際の CR（`"\r"`。1文字）は `nativeTypes.yaml:17` の `YAML_CR_COL`、`schemaFullCoverage.yaml:67` と
`YamlFileBuilderTest/fileData.yaml:171`・`:369` の `record-separator` にあり、いずれも**対象外**（2文字の `\`＋`r` ではない）。

---

## 2. 指示書の記述と実測が食い違った点（着手前に判明した分）

1. **指示書 §3「`YamlSectionTest` が既に POI を使っている」は、ピン `3ee39c9` では成り立たない。**
   `grep -rn 'poi\|Workbook\|XSSF\|HSSF' src/test/java` は0ヒットで、本モジュールのテストは POI を使っていない。
   ただし POI 3.8（`poi`・`poi-ooxml`・`poi-ooxml-schemas`）は `nablarch-testing` 経由でテストクラスパスに載っている
   （`mvn -o dependency:build-classpath` で実測）。oracle 用の `.xlsx` を組む土台は新規に作る。作業自体は妨げられない。
2. **解説書のパスは `ja/development_tools/testing_framework/` 配下である。**
   指示書は `implementation/testdata_notation.rst`・`setup/common.rst` と短縮形で書いているが、
   `afa4f9e` での実パスは `ja/development_tools/testing_framework/implementation/testdata_notation.rst` および
   `ja/development_tools/testing_framework/setup/common.rst`。引用された行番号・本文はこのパスで全件一致した（2-1〜2-6 の8行を照合済み）。

---

## 3. 第2節7件の是正結果

（#36〜#42 の完了後に記入する）

## 4. 本体を oracle にしたテストの一覧（2-1・2-4）

（#36・#39 の完了後に記入する）

## 5. 期待値をわざと崩す確認の結果

（#36〜#42 の完了後に記入する）

## 6. 既存テストの期待値を変えた箇所の全件

（#36〜#42 の完了後に記入する）

## 7. カバレッジ C0/C1 と converter で落ちたテストの全件

（#43 で記入する）
