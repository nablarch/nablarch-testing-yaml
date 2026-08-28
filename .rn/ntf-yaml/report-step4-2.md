# Step 4 第2回 報告書

対象: `nablarch-testing-yaml`（ブランチ `feature/ntf-yaml`、着手時 `3ee39c9`）
指示書: `nablarch-document@origin/ntf-yaml-support` の `.rn/20260724-ntf-yaml-support/ntf-step4-06-nablarch-testing-yaml-2.md`
参照点（ピン）: 解説書 `nablarch-document@afa4f9e`（パスは `ja/development_tools/testing_framework/…`）／`nablarch-testing@3c4bd2a`／`nablarch-testing-converter@d611bec`

着手前ベースライン（実測。`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`）:
`Tests run: 268, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。`@Test` 268件・`@Ignore` 1件。

完了時（実測。同コマンド。HEAD `00fc164`）:
`Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`。`@Test` 318件・`@Ignore` 0件。

**指示書 §6 の6項目と本報告書の節の対応**（§6 の順序どおりに並べてある。§2 は着手前調査の過程で判明した食い違いを別立てにしたもの）:

| 指示書 §6 | 本報告書 |
|---|---|
| 1. 2-1〜2-5 の「着手前に特定すること」の結果 | §1 |
| （追加） | §2 |
| 2. 第2節7件の是正結果 | §3 |
| 3. 本体を oracle にしたテストの一覧 | §4 |
| 4. 期待値をわざと崩す確認の結果 | §5 |
| 5. 既存テストの期待値を変えた箇所の全件 | §6 |
| 6. カバレッジ C0/C1 と converter で落ちたテストの全件 | §7 |
| （追加） | §8 申し送り |

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
`getMessage_fwHeaderRecordTypeIsNotSkipped`（`YamlTestDataParserTest.java:938`）、
`getMessage_legacyFwHeaderRecordCausesRecordLengthMismatch`（同 `:1109`）、
`fwHeaderSync` を使う送信同期のテスト（`getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped`。同 `:986`）。

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
| 空マッピング `{}` | 11 | 変わらない（前後とも読み飛ばす） |
| 全値が `""` | 14 | **変わる**（読み飛ばす → 残す） |
| 全値が Java null | 2 | 変わらない（前後とも残す） |
| マーカーカラムだけに値 | 1 | 変わらない（前後とも残す） |

「全値 `""`」14件の内訳（全件）:

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

参考: 空マッピング `{}` 11件は `tableData.yaml:60`・`:61`・`:71`・`:82`・`:236`・`:242`・`:298`・`:301`・`:340`・`:359` と
`completedTable.yaml:15`。全値 Java null 2件は `tableData.yaml:161`（`nullValueOnlyRow`）と `:291`（`nullValueOnlyRowListMap`）。
マーカーカラムだけに値 1件は `tableData.yaml:308`（`markerOnlyRowListMap`）。

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
   `git ls-tree -r --name-only 3ee39c9 src/test/java` の全 `.java` を `grep -nE 'poi|Workbook|XSSF|HSSF'` に掛けると2ヒットするが、
   いずれも英単語 `point` の一部（`YamlTestDataParserTest.java:1391` の `required-decimal-point`、
   `YamlLoaderTest.java:504` の `error path must point to nested location`）であり、**POI の利用は0件**である。
   （本報告書の第1版は「0ヒット」と書いていた。#43 で数え直して訂正した。結論は変わらない。）
   ただし POI 3.8（`poi`・`poi-ooxml`・`poi-ooxml-schemas`）は `nablarch-testing` 経由でテストクラスパスに載っている
   （`mvn -o dependency:build-classpath` で実測）。oracle 用の `.xlsx` を組む土台は新規に作る。作業自体は妨げられない。
2. **解説書のパスは `ja/development_tools/testing_framework/` 配下である。**
   指示書は `implementation/testdata_notation.rst`・`setup/common.rst` と短縮形で書いているが、
   `afa4f9e` での実パスは `ja/development_tools/testing_framework/implementation/testdata_notation.rst` および
   `ja/development_tools/testing_framework/setup/common.rst`。引用された行番号・本文はこのパスで全件一致した（2-1〜2-6 の8行を照合済み）。

---


## 3. 第2節7件の是正結果

**7件すべて是正済み。** `mvn -o clean test` は `Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`
（HEAD `00fc164`。`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean install` のテストフェーズで実測）。
着手前は `Tests run: 268, Failures: 0, Errors: 0, Skipped: 1`（`@Test` 268件のうち1件が `@Ignore`）。

### 3.1 「直す前に落ちたテスト」の測り方（本タスクの実測）

各是正の「直す前は落ちる」は、**HEAD のテスト一式に対し、その是正の `src/main` 変更だけを取り消して**測り直した。
手順は 5 件とも同じで、読み手が同じことをできる:

```
git worktree add --detach <scratchpad>/mut HEAD
# <scratchpad>/mut/src/main に下表「取り消し方」の 1 箇所だけを当てる
cd <scratchpad>/mut && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
```

各タスク（#36〜#40）の check ファイルには、**当時の**「実装を入れる前にテストだけを足した状態」での実測が別に記録してある。
本節の数字は**それとは別に、最終状態で測り直したもの**である。作業用 worktree は測定後 `git worktree remove --force` 済み
（`git worktree list` は本体1件のみ）。

### 3.2 是正ごとの結果

#### 2-1. 末尾フィールドの `null` を `""` に畳む

- 変更した `src/main`: `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java:262`
  `List<String> trimmedValues = NablarchTestUtils.trimTailCopy(rowValues);`（同 `:3` に import 追加）。
  結果を `:265` `addValueWithId` ／ `:267` `addValue` へ渡す。順序は `interpret`（`:252`-`:254`）→ `trimTail`（`:262`）→ `addValue`。
  **規則は手写しせず本体 `nablarch-testing` の実装をそのまま呼んでいる**
  （`../nablarch-testing/src/main/java/nablarch/test/NablarchTestUtils.java:273` `trimTailCopy`、実体は同 `:251`-`:263` `trimTail`）
- コミット: `ce81530`（`src/main` 1ファイル・`src/test` 3ファイル）
- 取り消し方: `:262` を `List<String> trimmedValues = rowValues;` に置換
- 取り消したときに落ちるテスト: **4件**（`Tests run: 318, Failures: 3, Errors: 1`）

| テスト | 落ちる要点 |
|---|---|
| `YamlTrailingNullOracleTest.getSetupFile_trailingNullsBecomeEmptyStrings:214`（F1） | `assertSameAsOracle:425` `F1: FIELD2 が本体と一致すること` |
| `YamlTrailingNullOracleTest.getSetupFile_allNullsBecomeEmptyStrings:264`（F4） | `assertSameAsOracle:425` `F4: FIELD2 が本体と一致すること` |
| `YamlTrailingNullOracleTest.getSetupFile_trailingEmptyStringAndNullBecomeEmptyStrings:298`（F6） | `assertSameAsOracle:425` `F6: FIELD3 が本体と一致すること` |
| `YamlTrailingNullOracleTest.getSendSyncMessage_trailingNullsBecomeEmptyStrings:357`（S2） | `NullPointerException`（本体 `MockMessages$MockMessage.removePadding` が null 値で落ちる） |

#### 2-2. 電文の `records` に `maxItems: 1`

- 変更した `src/main`: `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:208`（`message_data.records`）・
  `:242`（`expected_request_message_data.records`）・`:274`（`group_message_data.records`）に `"maxItems": 1,` を追加
- コミット: `389fe6d`（`src/main` 1ファイル ＋3行、`src/test` 8ファイル）
- 取り消し方: 上の3行を削除
- 取り消したときに落ちるテスト: **4件**（`Tests run: 318, Failures: 3, Errors: 1`）

| テスト | 落ちる要点 |
|---|---|
| `YamlLoaderTest.load_messagesWithMultipleRecordsIsSchemaViolation:620` | `YamlSchemaValidationException が期待される` |
| `YamlLoaderTest.load_expectedRequestMessagesWithMultipleRecordsIsSchemaViolation:653` | 同上 |
| `YamlLoaderTest.load_responseMessagesWithMultipleRecordsIsSchemaViolation:687` | 同上 |
| `YamlTestDataParserTest.getMessage_legacyFwHeaderRecordIsRejectedBySchemaValidation:1114` | `IllegalStateException`（スキーマで弾かれず旧形式が `record-length differs.` まで進む） |

#### 2-3. `fw_header` のキーを `reader.fwHeaderfields` の名前に限る

- 変更した `src/main`: `src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java:327`-`:331`
  （`if (!allowedFields.contains(key)) { throw new IllegalStateException("fw_header in " + source + " has unknown key '" + key + "'. …"); }`）。
  許可キー集合は同 `:388`-`:394` `fwHeaderFields()` が作る（`:74` `FW_HEADER_KEY = "reader.fwHeaderfields"`、
  `:81` 既定4つ `requestId`・`userId`・`resendFlag`・`resultCode`、`:393` `NablarchTestUtils.makeArray`）。
  **キー・既定値・分割の仕方は本体 `MessageParser` と同じものを使っている**
- コミット: `1b480b4`・`cfcd2ae`・`de31806`・`93fcff7`
- 取り消し方: `:327`-`:331` の `if` ブロックを削除
- 取り消したときに落ちるテスト: **8件**（`Tests run: 318, Failures: 8, Errors: 0`。すべて `YamlMessageBuilderTest`）

| テスト |
|---|
| `buildMessagePool_fwHeaderKeyNotInDefaultFieldsThrows:973` |
| `buildMessagePool_fwHeaderKeyNotInConfiguredFieldsThrows:1023` |
| `buildMessagePool_fwHeaderFieldsAreSplitByCommaWithoutTrimming:1058` |
| `buildMessagePool_unknownKeyIsCheckedOnlyForTheEntryBeingRead:1233` |
| `buildMessagePool_fwHeaderKeyIsCaseSensitive:1275` |
| `buildMessagePool_fwHeaderTildeKeyIsReadAsStringAndRejected:1313` |
| `buildMessagePool_fwHeaderNonStringKeyThrowsWithStringifiedKeyName:1354` |
| `buildMessagePool_fwHeaderNullKeyIsRejectedInDefensiveBranch:1412` |

（いずれも `IllegalStateException が期待される` で落ちる。行番号は失敗メッセージ中の assert 行）

#### 2-4. 空エントリの判定を `{}` だけに限る

- 変更した `src/main`: `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:234`-`:236`
  （`private static boolean isBlankRow(Object row) { return castMap(row).isEmpty(); }`）。
  javadoc（同 `:222`-`:233`）と `YamlTableDataBuilder.java:38`・`:215` のコメントも合わせた
- コミット: `a5a9f10`・`91d5a91`・`68a57ec`
- 取り消し方: `3ee39c9` 時点の実装（`castMap(row).values()` を回して `objectToString` が空文字なら空とみなす）に戻す
- 取り消したときに落ちるテスト: **12件**（`Tests run: 318, Failures: 12, Errors: 0`）

| テスト | 落ちる要点 |
|---|---|
| `YamlBlankEntryOracleTest.getSetupTableData_allEmptyStringRowIsKept:249`（T2） | `assertTableCase:471` `T2: 行数が本体と一致すること` |
| `YamlBlankEntryOracleTest.getListMap_allEmptyStringRowIsKept:369`（L2） | `assertListMapCase:493` `L2: 件数が本体と一致すること` |
| `YamlSectionTest.dropBlankRows_removesOnlyEmptyMappingRow:588` | `空マッピング行だけが取り除かれること` |
| `YamlTableDataBuilderTest.buildTableDataList_blankValueRowLeadingKeptAndDeterminesColumns:1498` | `列名が先頭行（全ての値が空文字の行）のキーで YAML 記述順に決まること` |
| `YamlTableDataBuilderTest.buildTableDataList_blankValueRowMiddleKept:1528` | `中間の全値空行も残り 3 行返ること` |
| `YamlTableDataBuilderTest.buildTableDataList_partiallyBlankValueRowKept:1557` | `2 行とも返ること` |
| `YamlTableDataBuilderTest.buildTableDataList_blankValueRowLeadingInExpectedTableKeptAndDeterminesColumns:1588` | `列名が先頭行（全ての値が空文字の行）のキーで YAML 記述順に決まること` |
| `YamlTableDataBuilderTest.buildTableDataList_blankValueRowMiddleInExpectedTableKept:1617` | `中間の全値空行も残り 3 行返ること` |
| `YamlTableDataBuilderTest.buildTableDataList_blankValueRowInExpectedCompleteTableKept:1672` | `先頭・中間・末尾の全値空行も残り 5 行になること` |
| `YamlTableDataBuilderTest.buildListMapRows_blankValueRowLeadingKeptAndDeterminesKeys:1704` | `先頭の全値空行も残り 2 件返ること` |
| `YamlTableDataBuilderTest.buildListMapRows_blankValueRowMiddleKept:1731` | `中間の全値空行も残り 3 件返ること` |
| `YamlTableDataBuilderTest.buildListMapRows_partiallyBlankValueRowKept:1758` | `2 件とも返ること` |

#### 2-5. バックスラッシュと `r` の2文字をエラーにする

- 変更した `src/main`: `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:430`-`:441` `rejectLiteralCr`
  （`:98` `LITERAL_CR = "\\r"`）。呼び出しは `interpret`（同 `:300`。データ行・ディレクティブがここを通る）と
  `YamlMessageBuilder.convertFwHeader`（`:326` キー・`:333` 値）の2経路。
  出所（セクション・`table`／`path`／`id`）は `YamlSection.entrySource` が組み立てる
- コミット: `476672d`・`e3a4c1f`
- 取り消し方: `rejectLiteralCr` の先頭に `if (true) { return; }` を入れて検査を無効化
- 取り消したときに落ちるテスト: **15件**（`Tests run: 318, Failures: 14, Errors: 1`）

| テスト |
|---|
| `YamlSectionTest.interpret_nullInterpretersStillRejectsLiteralCr:305` |
| `YamlSectionTest.interpret_emptyInterpretersStillRejectsLiteralCr:278` |
| `YamlTableDataBuilderTest.buildListMapRows_literalBackslashRThrowsException:643` |
| `YamlTableDataBuilderTest.buildListMapRows_literalBackslashRInsideLongerValueThrows:703` |
| `YamlTableDataBuilderTest.buildListMapRows_escapedBackslashFollowedByRThrows:733` |
| `YamlTableDataBuilderTest.buildTableDataList_literalBackslashRThrowsException:819` |
| `YamlFileBuilderTest.buildFileList_literalBackslashRInRowThrowsException:1070` |
| `YamlFileBuilderTest.buildFileList_literalBackslashRInsideLongerValueInRowThrows:1099` |
| `YamlFileBuilderTest.buildFileList_literalBackslashRInDirectiveThrowsException:1127`（`IllegalArgumentException` で Errors 側） |
| `YamlMessageBuilderTest.buildMessagePool_literalBackslashRInFwHeaderValueThrows:1903` |
| `YamlMessageBuilderTest.buildMessagePool_literalBackslashRInFwHeaderKeyThrows:1936` |
| `YamlMessageBuilderTest.buildMessagePool_literalBackslashRInMessageBodyRowThrows:1965` |
| `YamlMessageBuilderTest.buildMessagePool_literalBackslashRInMessageDirectiveThrows:1994` |
| `YamlMessageBuilderTest.buildSendSyncList_literalBackslashRInRowThrows:2024` |
| `YamlMessageBuilderTest.buildSendSyncBodies_literalBackslashRInRowThrows:2054` |

#### 2-6. `@Ignore` 1件の削除

- 削除したもの: `YamlTableDataBuilderTest#buildListMapRows_unknownCharacterTypeIsNotConverted`（`3ee39c9` の `:753`。`@Ignore` 付き）
  とフィクスチャ `charTypeUnknownTest`
- コミット: `36a8af6`
- **`src/main` は変更していない**（指示書 完了条件1 は 2-6・2-7 を除外している）ため「直す前に落ちるテスト」は無い
- 現状: `grep -rnE "^\s*@Ignore" src/` が **0件**、`grep -rn "import org.junit.Ignore" src/` も **0件**、
  `mvn -o clean test` が `Skipped: 0`（完了条件5）。
  `src/` に残る `@Ignore` の文字列は `YamlMessageBuilderTest.java:1125` の javadoc 本文1件のみ（アノテーションではない）

#### 2-7. スキーマ `description` の追随

- 変更した `src/main`: `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` の `description` のみ
  （`:108` `table_data.rows`／`:136` `list_map_data.rows`／`:209`・`:243`・`:275` の3つの `records`／
  `:216` `message_data.fw_header`／`:380` `record_fragment.rows`／`:433`・`:434` `$defs.fw_header`。
  加えて `:293` `record-separator` に 2-5 の規則を追記）
- コミット: `94f7e16`・`7daae89`
- **挙動を変えていない**ため「直す前に落ちるテスト」は無い（完了条件1 の対象外）
- スコープ拡張1件・別種の出典訂正5件を同時に行った。§8.1 に分けて記す

---

## 4. 本体を oracle にしたテストの一覧（2-1・2-4）

指示書 §3 のとおり、Excel に同じ意味がある 2-1・2-4 は**本体 `nablarch-testing` を正解にした**。
oracle 側は POI で同じ意味の `.xlsx` を組み、本体の公開 API `BasicTestDataParser`（`PoiXlsReader` ＋
`src/test/resources/unit-test.xml` の `interpreters`）で読む。YAML 側は `YamlTestDataParser` で同じ意味の `.yaml` を読む。
どちらのテストクラスも、**本体が返した値が解説書どおりであること**を先に固定してから、YAML の値を本体と突き合わせる
（`assertRecordValues` → `assertSameAsOracle` の2段。`YamlTrailingNullOracleTest.java:375`-`:382`）。

### 4.1 2-1: `YamlTrailingNullOracleTest`（8件）

- テスト: `src/test/java/nablarch/test/core/reader/YamlTrailingNullOracleTest.java`
- oracle 組み立て: `src/test/java/nablarch/test/core/reader/BodyExcelOracle.java`（POI で `.xlsx` を生成）
- YAML 入力: `src/test/java/nablarch/test/core/reader/YamlTrailingNullOracleTest/trailingNull.yaml`
- Excel のシート名と YAML のグループ ID／ID が F1〜F6・M1・S2 で1対1に対応する
- ファイルデータは可変長を使う（`VariableLengthFileFragment#convertValue` が恒等写像で、`""` と null を素のまま観測できるため）

| ケース | 入力（YAML の `rows` ／ Excel のセル） | 本体（Excel）の値 | YAML の値 | テスト（`YamlTrailingNullOracleTest.java`） |
|---|---|---|---|---|
| F1 | `["x", null, null]` | `x`, `""`, `""` | 同じ | `getSetupFile_trailingNullsBecomeEmptyStrings:213` |
| F2 | `["x", null, "y"]` | `x`, **null**, `y` | 同じ | `getSetupFile_nullFollowedByValueIsKept:229` |
| F3 | `[""]` | `""`, `""`, `""` | 同じ | `getSetupFile_emptyStringWithOmittedTrailingFields:247` |
| F4 | `[null, null, null]` | `""`, `""`, `""` | 同じ | `getSetupFile_allNullsBecomeEmptyStrings:263` |
| F5 | `["x"]` | `x`, `""`, `""` | 同じ | `getSetupFile_omittedTrailingFieldsBecomeEmptyStrings:280` |
| F6 | `["x", "", null]` | `x`, `""`, `""` | 同じ | `getSetupFile_trailingEmptyStringAndNullBecomeEmptyStrings:297` |
| M1 | 電文 `["x", null, null]` | `x`, `""`, `""` | 同じ | `getMessage_trailingNullsBecomeEmptyStrings:326` |
| S2 | 送信同期 `["x", null, null]` | `x`, `""`, `""` | 同じ | `getSendSyncMessage_trailingNullsBecomeEmptyStrings:353` |

指示書 §3 が要求した F1〜F6・M1・S2 を全件含む（送信同期は4データタイプのうち `response_body_messages` の1つ）。
指示書 2-1 の実測表で「yaml `3ee39c9`」が本体と食い違っていた F1・F4・F6・M1・S2 のうち、**M1 は是正前も一致していた**
（`messages` は固定長で `DataFileFragment#removePadding` を通り、値が null でも `""` が返るため）。
是正前に実際に落ちたのは F1・F4・F6・S2 の4件である（§3.2 の 2-1）。

### 4.2 2-4: `YamlBlankEntryOracleTest`（10件）

- テスト: `src/test/java/nablarch/test/core/reader/YamlBlankEntryOracleTest.java`
- YAML 入力: `src/test/java/nablarch/test/core/reader/YamlBlankEntryOracleTest/blankEntry.yaml`
- 指示書 §3 が要求した4種（`{}`／全値 `""`／`null` だけ／マーカーカラムだけに値）を、テーブル（`setup_tables`）と
  `LIST_MAP`（`list_maps`）の**両方**に置いた。各グループは先頭に値を持つ通常行を置き、2行目を判定対象にしている

| ケース | 入力の2行目 | 本体（Excel）の結果 | YAML の結果 | テスト（`YamlBlankEntryOracleTest.java`） |
|---|---|---|---|---|
| T1 | `{}`（Excel: 全セル空） | 行が消え1行 | 同じ | `getSetupTableData_emptyMappingRowIsSkipped:232` |
| T2 | 全カラム `""` | 2行。2行目は全カラム `""` | 同じ | `getSetupTableData_allEmptyStringRowIsKept:248` |
| T3 | 全カラム `null` | 2行。2行目は全カラム null | 同じ | `getSetupTableData_allNullRowIsKept:265` |
| T4 | `[NO]` に値・他は `""` を明示 | 2行。2行目は全カラム `""` | 同じ | `getSetupTableData_markerOnlyRowIsKept:282` |
| T5 | `[NO]` だけをキーに持つ（他は省略） | 2行。2行目は全カラム `""` | 行数・カラム名は一致。**2行目の値は null**（仕様差） | `getSetupTableData_markerOnlyRowWithOmittedColumnsIsKept:313` |
| L1 | `{}` | 1件 | 同じ | `getListMap_emptyMappingRowIsSkipped:353` |
| L2 | 全キー `""` | 2件。2件目は全キー `""` | 同じ | `getListMap_allEmptyStringRowIsKept:368` |
| L3 | 全キー `null` | 2件。2件目は全キー null | 同じ | `getListMap_allNullRowIsKept:384` |
| L4 | `[NO]` に値・他は `""` を明示 | 2件。2件目は全キー `""` | 同じ | `getListMap_markerOnlyRowIsKept:400` |
| L5 | `[NO]` だけをキーに持つ（他は省略） | 2件。2件目は全キー `""` | 件数・キー集合は一致。**2件目の値は null**（仕様差） | `getListMap_markerOnlyRowWithOmittedKeysIsKept:425` |

**T5・L5 の仕様差は意図して固定してある。** 「マーカーカラムだけに値があるエントリ」を YAML で素直に書くと他のキーを省略することになり、
省略したカラムは YAML では null になる（`{}` だけを空とみなす 2-4 の決定の帰結）。行が残る点は本体と一致するが値は食い違う。
値までそろえたい場合は T4・L4 のように `""` を明示する。テストは「本体は `""`・YAML は null」を両方 assert して差を固定している
（`YamlBlankEntryOracleTest.java:329`-`:334`・`:443`-`:448`）。

---

## 5. 期待値をわざと崩す確認の結果

### 5.1 本タスクで測り直したもの（実装側の変異・5件）

§3.1 の手順で、是正 5 件それぞれの `src/main` 変更だけを取り消し、`mvn -o clean test` を実行した。
**5件すべてで、その是正のために足した／直したテストが落ちる。**

| # | 変異（取り消した `src/main`） | 結果 | 落ちたテスト |
|---|---|---|---|
| R1 | `YamlFileBuilder.java:262` の `trimTailCopy` を素通しにする | `Tests run: 318, Failures: 3, Errors: 1` | §3.2 の 2-1 の表（4件） |
| R2 | スキーマの `"maxItems": 1,` 3行を削除 | `Tests run: 318, Failures: 3, Errors: 1` | §3.2 の 2-2 の表（4件） |
| R3 | `YamlMessageBuilder.java:327`-`:331` の許可キー検査を削除 | `Tests run: 318, Failures: 8, Errors: 0` | §3.2 の 2-3 の表（8件） |
| R4 | `YamlSection.isBlankRow` を `3ee39c9` の実装に戻す | `Tests run: 318, Failures: 12, Errors: 0` | §3.2 の 2-4 の表（12件） |
| R5 | `YamlSection.rejectLiteralCr` の先頭に `if (true) { return; }` | `Tests run: 318, Failures: 14, Errors: 1` | §3.2 の 2-5 の表（15件） |

R4 で `*_rowInterpretedToAllBlankIsKept` の2件が落ちないのは、`dropBlankRows` が `interpret` より前に走るためで、意図どおりである。

### 5.2 各タスクで実測した「期待値の崩し」（本タスクでは再実行していない）

追加・変更した個々のテストについて期待値リテラルを崩す確認は、**#36〜#40 の各タスクで実測済み**である。
本タスクでは再実行していない（**未再実行**と明示する）。対象テスト名と `file:line` は本タスクで現物を開いて存在を確認した。

| タスク | 崩した対象 | 件数 | 記録の場所 |
|---|---|---|---|
| #36（2-1） | `YamlTrailingNullOracleTest` の期待値リテラル（F1〜F6・M1・S2） | 8 | `.rn/ntf-yaml/checks/task-36.md` 「変異確認」 |
| #37（2-2） | `YamlLoaderTest` 4件・`YamlTestDataParserTest` 3件の期待値 | 7 | `.rn/ntf-yaml/checks/task-37.md` 「変異確認」 |
| #38（2-3） | `YamlMessageBuilderTest` の追加3件・変更4件（M1〜M8）ほかレビュー是正3ラウンド分 | 8＋ | `.rn/ntf-yaml/checks/task-38.md` 「変異確認」（3節） |
| #39（2-4） | `src/main` 側 5通り（M-A〜M-E）と、期待値を変えた／新規テスト 8通り（M24〜M31） | 13 | `.rn/ntf-yaml/checks/task-39.md` 「変異確認（実測）」 |
| #40（2-5） | 検査の無効化・定数の差し替え・出所／値の欠落・検査位置の入れ替え・フィクスチャの CR（M1〜M6） | 6 | `.rn/ntf-yaml/checks/task-40.md` 「変異確認（実測）」 |

#41（2-6）はテスト削除のみ、#42（2-7）は `description` と出典コメントのみで、崩す対象となる期待値を作っていない。

---

## 6. 既存テストの期待値を変えた箇所の全件

**測り方**: `3ee39c9` と HEAD `00fc164` の `src/test/java` から `@Test` メソッドを機械抽出し、
クラス名＋メソッド名の集合と、各メソッド本体の行（空行を除く）を突き合わせた。
「期待値を変えた」は、本体のうち `assert*(`／`fail(`／`is(`／`containsString(`／`hasItem` を含む行の並びが変わったものとした。

### 6.1 総数

| | 件数 |
|---|---|
| `3ee39c9` の `@Test` | 268（うち `@Ignore` 1） |
| HEAD の `@Test` | 318（`@Ignore` 0） |
| 両方に同じ名前で在る | 251 |
| `3ee39c9` にあり HEAD に無い（改名・統合・削除） | 17 |
| HEAD で新規に足した | 67 |

268 − 17 ＋ 67 ＝ 318（整合）。

### 6.2 同じ名前のまま**期待値を変えた** — 7件

| テスト | 是正 | 変えた内容 |
|---|---|---|
| `YamlColumnOmissionTest#columnNamesDependOnRowOrderAfterBlankRowRemoval` | 2-4 | 全値 `""` の行が残るようになったため、カラム名・件数の assert を差し替えた |
| `YamlSectionTest#dropBlankRows_keepsRowHavingOnlyMarkerColumnValue` | 2-4 | 判定がマーカーカラム除外の**前**に行われることを assert の根拠に合わせた |
| `YamlSectionTest#dropBlankRows_keepsRowHavingOnlyNullValues` | 2-4 | スキップ条件を「空マッピングだけ」に合わせた |
| `YamlTableDataBuilderTest#buildTableDataList_partiallyBlankValueRowKept` | 2-4 | 行数 1 → 2 |
| `YamlTableDataBuilderTest#buildListMapRows_partiallyBlankValueRowKept` | 2-4 | 件数 1 → 2 |
| `YamlTestDataParserTest#getMessage_fwHeaderRecordTypeIsNotSkipped` | 2-2 | フィクスチャを1レコードに畳んだのに伴い、2レコード目の項目名・値の assert を差し替えた |
| `YamlTestDataParserTest#getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped` | 2-2 | 同上。フラグメントをまたぐ連番リセットの assert は、複数レコードレイアウト自体が禁止になったため落とした |

### 6.3 同じ名前のまま**期待値は変えず**、入力側だけ変えた — 9件

| テスト | 是正 | 変えた内容 |
|---|---|---|
| `YamlMessageBuilderTest#buildMessagePool_customFwHeaderFields` | 2-3 | `setFwHeaderFields("customField,requestId")` を Given に足した。assert は不変 |
| `YamlSectionTest#interpret_nullInterpretersReturnsValueAsIs` | 2-5 | `interpret` のシグネチャ変更に伴う引数追加のみ |
| `YamlSectionTest#interpret_emptyInterpretersReturnsValueAsIs` | 2-5 | 同上 |
| `YamlSectionTest#interpret_nullValueReturnsNull` | 2-5 | 同上 |
| `YamlFileBuilderTest#buildFragmentsForMessage_fwHeaderRecordTypeIsNotSkipped` | 2-5 | `buildFragmentsFor*` のシグネチャ変更に伴う引数追加のみ |
| `YamlFileBuilderTest#buildFragmentsForMessage_fwHeaderRecordWithoutLength` | 2-5 | 同上 |
| `YamlFileBuilderTest#buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped` | 2-5 | 同上 |
| `YamlFileBuilderTest#buildFragmentsForSendSync_recordTypeIsDefaultWhenNotKept` | 2-5 | 同上 |
| `YamlFileBuilderTest#buildFragmentsForSendSync_rowNoIsIncrementedPerRow` | 2-5 | 同上 |

### 6.4 改名・統合・削除した既存テスト — 17件（全件）

| `3ee39c9` のテスト（行） | 是正 | HEAD での行方（行） |
|---|---|---|
| `YamlSectionTest#dropBlankRows_removesEmptyMappingAndAllBlankValueRows`（`:473`） | 2-4 | 改名 `dropBlankRows_removesOnlyEmptyMappingRow`（`:577`）。残る件数 1 → 2 |
| `YamlSectionTest#dropBlankRows_keepsRowHavingOnlyWhitespaceValue`（`:522`） | 2-4 | 上へ統合し**削除** |
| `YamlSectionTest#dropBlankRows_keepsRowHavingAnyNonBlankValue`（`:498`） | 2-4 | 上へ統合し**削除** |
| `YamlTableDataBuilderTest#buildTableDataList_blankValueRowLeadingExcluded`（`:1292`） | 2-4 | 改名 `..._blankValueRowLeadingKeptAndDeterminesColumns`（`:1489`）。行数 1 → 2 |
| `YamlTableDataBuilderTest#buildTableDataList_blankValueRowMiddleExcluded`（`:1319`） | 2-4 | 改名 `..._blankValueRowMiddleKept`（`:1519`）。行数 2 → 3 |
| `YamlTableDataBuilderTest#buildTableDataList_blankValueRowLeadingInExpectedTableExcluded`（`:1372`） | 2-4 | 改名 `..._blankValueRowLeadingInExpectedTableKeptAndDeterminesColumns`（`:1579`） |
| `YamlTableDataBuilderTest#buildTableDataList_blankValueRowMiddleInExpectedTableExcluded`（`:1399`） | 2-4 | 改名 `..._blankValueRowMiddleInExpectedTableKept`（`:1608`） |
| `YamlTableDataBuilderTest#buildTableDataList_blankValueRowInExpectedCompleteTableExcluded`（`:1453`） | 2-4 | 改名 `..._blankValueRowInExpectedCompleteTableKept`（`:1663`）。行数 2 → 5 |
| `YamlTableDataBuilderTest#buildListMapRows_blankValueRowLeadingExcluded`（`:1486`） | 2-4 | 改名 `..._blankValueRowLeadingKeptAndDeterminesKeys`（`:1696`）。件数 1 → 2 |
| `YamlTableDataBuilderTest#buildListMapRows_blankValueRowMiddleExcluded`（`:1511`） | 2-4 | 改名 `..._blankValueRowMiddleKept`（`:1723`）。件数 2 → 3 |
| `YamlTableDataBuilderTest#buildListMapRows_allBlankRowsReturnsEmptyList`（`:1687`） | 2-4 | 改名 `..._allEmptyMappingRowsReturnsEmptyList`（`:1908`）。フィクスチャを `{}` に変え、期待値（空リスト）は不変 |
| `YamlMessageBuilderTest#buildMessagePool_fwHeaderMapAllKeysRetainedIncludingCustom`（`:824`） | 2-3 | 改名後、レビュー是正で**削除**（独自キーを扱うテストが別に立ったため） |
| `YamlMessageBuilderTest#buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues`（`:985`） | 2-3 | 改名 `..._fwHeaderMapKeepsQuotedNumericAndBooleanLikeValuesAsStrings`（`:1628`）。`setFwHeaderFields` を Given に追加。値の assert 3件は不変 |
| `YamlMessageBuilderTest#buildMessagePool_malformedFwHeaderRowsThrowsException`（`:648`） | 2-3 | 改名 `..._nonMapFwHeaderThrowsExceptionWithTypeName`（`:776`）。`id` の assert を `containsString("id='malformed001'")` へ締めた |
| `YamlTestDataParserTest#getMessage_legacyFwHeaderRecordCausesRecordLengthMismatch`（`:1109`） | 2-2 | 改名 `..._legacyFwHeaderRecordIsRejectedBySchemaValidation`（`:1111`）。期待する例外を `IllegalStateException` から `YamlSchemaValidationException` へ |
| `YamlTableDataBuilderTest#buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser`（`:591`） | 2-5 | 改名 `buildListMapRows_yamlEscapeBecomesCr`（`:602`）。2文字の `\`＋`r` が残ることを assert していた行を落とし、実 CR の assert だけを残した |
| `YamlTableDataBuilderTest#buildListMapRows_unknownCharacterTypeIsNotConverted`（`:753`） | 2-6 | **削除**（`@Ignore` 付き。解説書に無い期待を追っていたため） |

### 6.5 期待値を変えなかった既存テスト

251 − 7（6.2）− 9（6.3）＝ **235件**は本体も入力も触っていない。
17件（6.4）のうち期待値を実質的に変えたのは 11件（改名だけで期待値が不変のもの・統合されたもの・削除したものを除く）。

### 6.6 §1 の「見込み」と実測の突き合わせ

| §1 の見込み | 実測 | 差の理由 |
|---|---|---|
| 2-1: 期待値を変える既存テストは0件 | **0件**（§6.2〜6.4 に 2-1 起因のものは無い） | 一致 |
| 2-2: `messageData.yaml` を読む**16箇所**が落ちる | 落ちた**テストは15件**（`.rn/ntf-yaml/checks/task-37.md` の実測） | 16 は読み出しの**箇所**の数（`git show 3ee39c9:…/YamlTestDataParserTest.java \| grep -n messageData` の 17 ヒットから javadoc 1件を除いた数）。うち `:1816` と `:1828` が同一テストメソッド内にあるため、テスト数では15になる |
| 2-3: 落ちる見込み4件 | **4件**（`.rn/ntf-yaml/checks/task-38.md` の実測） | 一致。4件はすべて §6 に現れる（改名2件・Given 変更1件・フィクスチャ変更のみ1件） |
| 2-4: 挙動が変わる見込み14件 | **14件**（`.rn/ntf-yaml/checks/task-39.md` の実測） | 一致 |
| 2-5: フィクスチャ1件・テスト1件 | フィクスチャ1件（`nativeTypes.yaml:16`）・テスト1件（改名して assert を落とした。§6.4） | 一致 |

（2-2〜2-4 の「落ちた件数」は各タスクの実測記録であり、**本タスクでは再実行していない**。
本タスクで測り直したのは §3.2・§5.1 の「最終状態で `src/main` の是正を取り消したときに落ちるテスト」である。）

### 6.7 フィクスチャだけを変えたもの

期待値を書き換えずフィクスチャ側で復旧させたものが以下にある（テスト本体は不変）。

| フィクスチャ | 是正 | 変えた内容 |
|---|---|---|
| `YamlTestDataParserTest/messageData.yaml` | 2-2 | `records` 2件のエントリ3件を、1件へ畳む（2件）／別ファイル `legacyFwHeaderRecord.yaml` へ切り出す（1件） |
| `YamlMessageBuilderTest/fwHeaderMapData.yaml` | 2-3 | 独自キー `customProjectKey`・`boolFlag` を外した |
| `YamlMessageBuilderTest/customFwHeaderData.yaml` | 2-3 | `customField` の扱いを `reader.fwHeaderfields` 設定つきに変えた |
| `YamlColumnOmissionTest/omission.yaml` | 2-4 | `s4a`・`s4b` の全値 `""` の行の扱いに合わせて調整 |
| `YamlTableDataBuilderTest/tableData.yaml`・`completedTable.yaml` | 2-4 | 全値 `""` の行が残る前提へコメントと構成を合わせた |
| `YamlTableDataBuilderTest/nativeTypes.yaml` | 2-5・2-6 | `LITERAL_CR_COL: "\\r"` を `interpreterTest` から外し単独エントリへ／`charTypeUnknownTest` を削除 |

---

## 7. カバレッジ C0/C1 と converter で落ちたテストの全件

### 7.1 カバレッジ C0/C1

**結論: 第1回から下がった箇所は無い。** 未達は第1回と同じ2箇所（`#19` で承認済みの到達不能）だけである。

測定コマンド（第1回・第2回とも同じ。JaCoCo 0.8.8）:

```
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec
```

第1回の基準 `8eacaa7` は `git worktree add --detach <scratchpad>/wt-8eacaa7 8eacaa7` で取り出し、
**本タスクで同じ手順で測り直した**（`.rn/ntf-yaml/checks/task-33.md` の記録を写していない）。
worktree は測定後 `git worktree remove --force` 済み。集計は `target/site/jacoco/jacoco.csv` の列を合計したもの。

| | 第1回 `8eacaa7` | 第2回 `00fc164` |
|---|---|---|
| テスト | `Tests run: 267, Failures: 0, Errors: 0, Skipped: 1` | `Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` |
| C0（命令） | 1663 / 1676 = **99.22%**（`INSTRUCTION_MISSED` 13） | 1809 / 1822 = **99.29%**（`INSTRUCTION_MISSED` 13） |
| C1（分岐） | 168 / 170 = **98.82%**（`BRANCH_MISSED` 2） | 174 / 176 = **98.86%**（`BRANCH_MISSED` 2） |

クラス別（`INSTRUCTION_MISSED` / `BRANCH_MISSED`。9クラス全件）:

| クラス | 第1回 IM/BM | 第2回 IM/BM | 命令数（覆） 第1回 → 第2回 | 分岐数（覆） 第1回 → 第2回 |
|---|---|---|---|---|
| `YamlTableDataBuilder` | 0 / 0 | 0 / 0 | 322 → 332 | 30 → 30 |
| `YamlSection` | 0 / 0 | 0 / 0 | 221 → 221 | 52 → 50 |
| `YamlMessageBuilder` | 0 / 0 | 0 / 0 | 285 → 406 | 30 → 38 |
| `YamlSchemaValidationException` | 0 / 0 | 0 / 0 | 28 → 28 | 0 → 0 |
| `InterpreterResolver` | 0 / 0 | 0 / 0 | 11 → 11 | 0 → 0 |
| `YamlFileBuilder` | 1 / 1 | 1 / 1 | 367 → 382 | 41 → 41 |
| `YamlLoader` | 12 / 1 | 12 / 1 | 182 → 182 | 13 → 13 |
| `MessageContent` | 0 / 0 | 0 / 0 | 15 → 15 | 0 → 0 |
| `YamlTestDataParser` | 0 / 0 | 0 / 0 | 232 → 232 | 2 → 2 |

**下がった箇所: 無し。** 9クラスすべてで `INSTRUCTION_MISSED` と `BRANCH_MISSED` が第1回と一致する。
`YamlSection` の分岐総数が 52 → 50 に減っているのは、2-4 で `isBlankRow` が `castMap(row).isEmpty()` の1行になり
**分岐そのものが2つ消えた**ためで、覆えていない分岐が増えたのではない（`BRANCH_MISSED` は前後とも 0）。

未達2箇所（JaCoCo の HTML で `nc`／`bpc` の付いた行を機械抽出して特定）:

| 箇所 | 第1回の位置 | 第2回の位置 | 内容 |
|---|---|---|---|
| `YamlFileBuilder.java` | `:236`-`:237` | `:246`-`:247` | `if (!(rowObj instanceof List)) { continue; }`。SnakeYAML Engine では `rows:` の各要素は常に `List` になるため到達しない防御的ガード（コメント `:244`-`:245` に明記） |
| `YamlLoader.java` | `:60`-`:61`・`:65`-`:66` | 同じ（変わらず） | スキーマがクラスパスに無い場合と読み込みが `IOException` になる場合。`static` 初期化子で、通常は到達しない |

`YamlFileBuilder` の位置が `+10` ずれているのは 2-1 で `trimTailCopy` の呼び出しとコメントを足したためで、指している箇所は同じである。

### 7.2 converter で落ちたテストの全件

**結論: 着手前 `Tests run: 656, Failures: 0, Errors: 0, Skipped: 0` から、4件が落ちるようになった。**
内訳は 2-1 が2件・2-2 が1件・2-4 が1件で、いずれも converter のフィクスチャが**是正前の挙動を期待して書かれている**ためである。
2-3・2-5 起因の失敗は無い。

`nablarch-testing-converter` は**一切変更していない**（ブランチ `ntf-test-data-converter`、HEAD `d611bec`。
実行の前後とも `git -C ../nablarch-testing-converter status --short` が空であることを確認した）。

測り方（着手前・完了後とも本モジュールを `.m2` へ install してから converter を実行した）:

```
# 着手前の基準
git worktree add --detach <scratchpad>/wt-3ee39c9 3ee39c9
cd <scratchpad>/wt-3ee39c9 && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean install -DskipTests
cd ../nablarch-testing-converter && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
#   -> Tests run: 656, Failures: 0, Errors: 0, Skipped: 0   （指示書 完了条件10 の数値と一致）

# 完了後
cd <本モジュール> && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean install
cd ../nablarch-testing-converter && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
#   -> Tests run: 656, Failures: 3, Errors: 1, Skipped: 0
```

着手前の 656 件は**本タスクで測り直した**（指示書の記載を写していない）。
基準側の install は worktree 内で `git-commit-id-plugin:2.1.15:revision` が `MissingObjectException` になったため、
`.git/worktrees/wt-3ee39c9/` に `objects`／`refs`／`config` の symlink を張って jgit がオブジェクトを解決できるようにしたうえで、
**指示どおりの install コマンドをそのまま**実行した（第1回 `#33` と同じ回避策）。symlink は測定後に削除し、
worktree も `git worktree remove --force` 済み（`git worktree list` は本体1件、`.git/worktrees` も残っていない）。
最後に HEAD `00fc164` を `.m2` へ install し直して終えている。

| # | テスト（converter・`d611bec`） | 起因 | 落ちる理由（フィクスチャの記述と、是正で何が変わったか） |
|---|---|---|---|
| 1 | `yaml.YamlFormatReaderInvalidInputTest#fillsMissingRecordFragmentValuesWithEmptyStringInsteadOfNull:763`（FAILURE） | **2-1** | フィクスチャ（同ファイル `:745`-`:756`）はフィールド3件の固定長レコードに `rows: - ["a", null]` を書き、`is(Arrays.asList("a", null, ""))` を期待している。是正後は `trimTailCopy` が末尾の `null` を落として `["a"]` にし、`DataFileFragment#addValue` がフィールド数まで `""` で埋めるため `[a, , ]` になる。実測メッセージ: `Expected: is <[a, null, ]> but: was <[a, , ]>` |
| 2 | `yaml.YamlFormatReaderScalarTest#readsUnquotedNullAsJavaNullInRecordFragmentPath:650`（FAILURE） | **2-1** | ヘルパ `readRecordFragmentValue`（同ファイル `:224`-`:239`）がフィールド1件の固定長レコードに `rows: - [null]` を書き、`is(nullValue())` を期待している。是正後は末尾の `null` が落ちて `""` で埋められる。実測メッセージ: `Expected: is null but: was ""` |
| 3 | `yaml.YamlFormatReaderScalarTest#skipsRowWhoseValuesAreAllEmpty:596`（FAILURE） | **2-4** | フィクスチャ（同ファイル `:582`-`:590`）は `setup_tables` に `- {}` と `- K: "" / V: ""` の2行を置き、**両方が読み飛ばされる**ことを期待している。javadoc（`:569`-`:572`）が引く出典は `testdata_notation.rst:1500`（`5783b35` 時点）の旧文「空マッピング（`{}`）**または**すべての値が空文字の場合にスキップされる」であり、`afa4f9e` の `:1502` で「`""` は値であり読み飛ばされない」に改訂された箇所そのものである。実測メッセージ: `Expected: is <[[x, 1]]> but: was <[[x, 1], [, ]]>` |
| 4 | `yaml.YamlFormatReaderRealFileTest#keepsFwHeaderNamedRecordInSendSyncFromRealYaml:640`（ERROR） | **2-2** | フィクスチャ（同ファイル `:640`-`:653`）は `response_body_messages` の1エントリに `records` を2件（`record_type: "FW_HEADER"` の1件と本文1件）書き、**2件とも残る**ことを期待している。是正後はスキーマ検証で弾かれる。実測メッセージ: `YamlSchemaValidationException: $.response_body_messages[0].records: アイテムは最大でも 1 個必要ですが、2 が見つかりました` |

**2-3・2-5 で落ちなかった理由（実測）**

- 2-3: converter の `src/test` にある `fw_header` のキーは、機械抽出の結果 `requestId`・`userId`・`resendFlag`・`dateSent` の4種のみ。
  `dateSent` は `YamlFormatWriterModelTest.java:762` にあるが、これは converter 側のモデルを直接組むテストで
  本モジュールの `YamlMessageBuilder.convertFwHeader` を通らない。実 YAML を読む経路（`YamlFormatReaderRealFileTest.java:817`-`:818`、
  `YamlTestCoreAdapterTest/messages.yaml:4`-`:5`、`YamlFormatWriterTest.java:235`-`:236`、`YamlTestDataValidatorTest.java:438`）は
  すべて既定4つの範囲内である
- 2-5: converter の `src/test` に Java 文字列として `\\r`（2文字）を書いた箇所は 12 件あるが、
  YAML の値として本モジュールの `interpret` に渡るものは無い。`SpecialNotationRoundTripTest.java:385` の
  `BODY: "1行目\r2行目"` や `YamlFormatReaderInvalidInputTest.java:979` の `record-separator: "\r\n"` は
  **YAML のダブルクォート内エスケープ**であり、パーサが実 CR／LF に展開する（2文字のままにはならない）。
  残りは Excel 経路（`XlsNotationSymmetryTest`・`XlsFormatWriterCellTypeTest`）と assert の説明文である

**指示書 完了条件10 の見込みとの対応**: 指示書は「少なくとも `YamlFormatReaderScalarTest#skipsRowWhoseValuesAreAllEmpty` は 2-4 で落ちる」と書いており、
実測でもそのとおり落ちた（上表の #3）。加えて 2-1 起因が2件・2-2 起因が1件あった。

---

## 8. 申し送り

### 8.1 スコープ拡張（#42 で実施。指示書には無い）

**指示書 §2 の 2-7 は「スキーマ `description` を解説書に合わせる」だけを求めているが、#42 では `src/` 配下の解説書出典の行番号も訂正した。**
本タスクで `cb82f3b`（#42 直前）と HEAD の全出典を機械抽出して突き合わせ、**変わったのは 18 箇所**であることを確認した。
これは**原因の異なる2種**に分かれる。

**(1) 解説書の改版で `+2` ずれた出典 — 13箇所**（コミット `94f7e16`）

原因は `nablarch-document@6ba3c83`（`docs(ntf): 交互記述は警告して変換、電文のレコードレイアウトは1つ`。`afa4f9e` の祖先であることを確認済み）が
`testdata_notation.rst` の `:1296` 以降に2行を挿入したこと。**`:1299` 以降を指す出典がすべて `+2` ずれた。**

| 訂正した箇所 | 前 → 後 |
|---|---|
| `src/test/java/nablarch/test/core/db/YamlDateNotationTest.java:30`・`:113`・`:133` | `:1326` → `:1328` |
| `src/test/java/nablarch/test/core/db/YamlDateNotationTest/date.yaml:1` | `:1326-:1331` → `:1328-:1333` |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:356` | `:1337` → `:1339` |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/nativeTypes.yaml:33` | `:1337` → `:1339` |
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java:56`・`:914` | `:1313-:1320` → `:1315-:1322` |
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/nativeTypes.yaml:88` | `:1313-:1320` → `:1315-:1322` |
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java:967` | `:1322` → `:1324` |
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/nativeTypes.yaml:133` | `:1322` → `:1324` |
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java:1000` | `:1441-:1443` → `:1443-:1445` |
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/nativeTypes.yaml:83` | `:1441-:1443` → `:1443-:1445` |

**(2) `+2` ずれとは無関係な出典の誤り — 5箇所**（`94f7e16` に3件、`7daae89` に2件）

こちらは `6ba3c83` の挿入とは無関係で、**着手前から指す先が誤っていた**。全件検証の過程で見つかったものである。

| 箇所 | 前 → 後 | 誤りの内容（`afa4f9e` で確認） |
|---|---|---|
| `YamlTrailingNullOracleTest.java:131` | `:857` → `:856` | 主張は「可変長はフィールド長の行を持たない」。`:857` は code-block 内の「→ データ（1件以上）」で無関係。`:856` が「→ フィールド長（固定長のみ）」 |
| `YamlTrailingNullOracleTest.java:237` | `:887` → `:882` | **`:887` は空行**。引用文「いずれか1つのフィールドに `""` と記述した行」は `:882` にある |
| `YamlTrailingNullOracleTest.java:271` | `:886` → `:882` | `:886` は「データ行の空セルの扱いには、次の2点の注意がある。」で引用文と無関係。引用文は `:882` の冒頭 |
| `YamlTestDataParserTest.java:1855` | `:1149` → `:1151` | `:1149` は電文の節の導入文。引用対象（`setUpMessages`／`expectedMessages`／`sendSyncTestData`）は `:1151` |
| `…/YamlTestDataParserTest/sendSyncTestData/RM21AA0101/message.yaml:4` | `:1149` → `:1151` | 同上 |

**コーディネータへの申し送りとの差**: 引き継ぎでは「別種の誤りは3件」とされていたが、**実測では5件**である。
`7daae89` で直した `:1149` → `:1151` の2件が数えられていなかった。この2件は `:1299` 未満を指すため `+2` ずれの13箇所にも含まれない。
なお第1回の報告書（`.rn/ntf-yaml/report-step4.md`）は「指示書の `testdata_notation.rst:1149` → 現物は `:1151`」を既に記録しており、
`src/` 側の出典が直っていなかったことが今回の全件検証で分かった、という経緯である。

### 8.2 解説書側への起票候補（判断はコーディネータ）

**`testdata_notation.rst:889`（ピン `afa4f9e`）の「後ろに値のあるフィールドがあれば `null` のまま保持される」は、`""` を「値」と数えるかが曖昧である。**

- 解説書の逐語（`afa4f9e` の `ja/development_tools/testing_framework/implementation/testdata_notation.rst:889`）:
  「末尾のフィールドに ``null``\ と記述した場合は、形式によらず\ ``""``\ になる。後ろに値のあるフィールドがあれば\ null\ のまま保持される」
- 実装の逐語（`../nablarch-testing/src/main/java/nablarch/test/NablarchTestUtils.java:251`-`:263` `trimTail`）:
  末尾から `StringUtil.hasValue` が偽の要素を連続して取り除く。`hasValue` は **null と `""` の両方**で偽になる
- **実測**（依存クラスパスで `NablarchTestUtils.trimTailCopy` を直接呼んだ）:

  | 入力 | `trimTailCopy` の結果 |
  |---|---|
  | `["x", null, ""]` | `["x"]` ← **`null` が保持されない** |
  | `["x", "", null]` | `["x"]` |
  | `["x", null, "y"]` | `["x", null, "y"]` |
  | `["x", null, null]` | `["x"]` |
  | `["x", "", "y"]` | `["x", "", "y"]` |

- 食い違いの中身: `["x", null, ""]` では `null` の後ろに `""` というフィールドが**書かれている**。
  2-4（`afa4f9e` の `:1502`「`""` と書いた空文字は値であり」）に従えば `""` は値なので、`:889` の文からは
  「`null` のまま保持される」と読めるが、実際は保持されない。**`:889` の「値のあるフィールド」は「`""` でない値」を指す**、
  と読めるように書き分ける必要がある
- 本モジュールの扱い: **本体の `trimTailCopy` をそのまま呼んでいるので実装は本体と一致しており、是正は不要**である。
  問題は解説書の文言だけである
- 書式は `.rn/ntf-yaml/report-nablarch-document-discrepancies.md` に合わせて起票できる。
  **同ファイルは本タスクでは書き換えていない**（コーディネータの判断事項）

### 8.3 未是正として残る食い違い

**2-5 の規則（バックスラッシュと `r` の2文字はエラー）が、スキーマの `description` にほぼ書かれていない。**

- 解説書（`afa4f9e` の `testdata_notation.rst:1445`）・実装（`YamlSection.rejectLiteralCr`）・テスト（§3.2 の 2-5 の15件）の三者は揃っている
- スキーマ `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` で規則に触れているのは
  **`:293`（`record-separator`）の1箇所だけ**（`grep -n 'バックスラッシュ'` の結果は `:293` の1件のみ）。
  #42 で「ディレクティブ値に実害がある1箇所」として追記したものである
- 規則は**値全般**（データ行・ディレクティブ・制御ヘッダ）に掛かるため、少なくとも
  `:108`（`table_data.rows`）・`:136`（`list_map_data.rows`）・`:380`（`record_fragment.rows`）・
  `:216`／`:433`（`fw_header`）にも同じ注意が要る。**5箇所以上が未追随である**
- #42 は「全体への追記は指示書 2-7 の対象（`:108`／`:136`／`:213`-`:215`／`:424`-`:430`／3つの `records`／`:377`）の外」として
  スコープ外にした。指示書 2-7 の表は 2-1〜2-4 の4件を挙げており、2-5 の追随を求めていない。**判断は未了である**

### 8.4 構造的な問題としての推奨

**行番号出典は解説書の改版のたびに壊れる。今回それが 18 箇所で起きた（§8.1）。**

- 現状（本タスクで実測）: `src/` 配下に解説書の**リビジョンのピンは1つも書かれていない**
  （`grep -rl "afa4f9e\|nablarch-document@" src/` が **0件**）。一方、行番号を含む出典は **60 箇所・16 ファイル**にある
  （`grep -rEo '[a-z_/]+\.rst:[0-9]+' src/ | wc -l` が 60、`grep -rEl '\.rst:[0-9]+' src/ | wc -l` が 16）
- したがって**壊れたことに気づく仕組みが無い**。今回も `#41` のレビューが偶然拾っただけである
- 推奨（優先順）:
  1. **機械検証を1本入れる。** ピン付きの解説書チェックアウトを前提に `src/` 配下の全出典を解決し、
     「指し先が範囲内・非空・記録した引用文を含む」ことを突き合わせる。#42 と本タスクで使ったスクリプトと同じ発想で、
     CI か手動チェックリストに載せれば `:887` が空行を指すような誤りは着手前に出る
  2. **出典を「節見出し＋逐語引用」にし、行番号は補助として必ずピン付きで書く**（例: `testdata_notation.rst@afa4f9e:1328`）。
     本モジュールには既に先例がある: `src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java:56`-`:66`、
     `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:176`-`:179`、
     `src/test/java/nablarch/test/core/reader/YamlBlankEntryOracleTest.java`（行番号出典 0件）、
     `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/mixedFwHeaderKeysData.yaml:1`-`:13`（フィクスチャのコメント）
- **本タスクではどちらも実施していない。** 方式の切り替えは差分が大きく、コーディネータ／ユーザーの判断事項である

### 8.5 第1回の記録のうち第2回で失効したもの

**#41 で `@Ignore` 付きテスト1件を削除した結果、#31 の記録2箇所が前提を失った。**

削除したのは `YamlTableDataBuilderTest#buildListMapRows_unknownCharacterTypeIsNotConverted`（`3ee39c9` の `:753`）。
現在の `src/` に該当テストは無い（`grep -rn "buildListMapRows_unknownCharacterTypeIsNotConverted" src/` が **0件**）。

| 失効した記録 | 逐語 | 現状 |
|---|---|---|
| `.rn/ntf-yaml/steering.md:1140`（#31 Step 3-2） | 「**列挙外の文字種名は変換されないという負のテストも必ず書く**」 | 該当テストは存在しない。前半（14文字種）は `YamlTableDataBuilderTest#buildListMapRows_allFourteenCharacterTypesAreGenerated` が引き続き担保 |
| `.rn/ntf-yaml/steering.md:1154`（#31 Completion criteria） | 「3-2 の負のテスト（列挙外の文字種名は変換されない）が書かれている」 | 同上 |

`steering.md` の上記2箇所には、コーディネータが「#41 で失効」の注記を追記済みであることを本タスクで確認した
（`:1141`・`:1155`）。**`.rn/ntf-yaml` 以下の他の記録（`checks/task-31.md`）への注記は未了**である。
`checks/task-31.md` の3箇所（`:8`・`:9`・`:23`）が「負のテストが `@Ignore` 付きで存在する」という当時の状態を記録しており、
現在の `src/` とは一致しない（当時の実測記録としては真）。注記を入れるかどうかはコーディネータの判断事項である。

なお #41 の完了条件「`@Ignore` が `src/test` 全体で0件」と #31 の完了条件「落ちたものは `@Ignore` で記録されている」は字面上両立しない。
#31 の時点では1件在り、#41 でその1件ごと削除した、という経緯である。
