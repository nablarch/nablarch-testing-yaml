# Step 4 第2回 報告書

対象: `nablarch-testing-yaml`（ブランチ `feature/ntf-yaml`、着手時 `3ee39c9`）
指示書: `nablarch-document@origin/ntf-yaml-support` の `.rn/20260724-ntf-yaml-support/ntf-step4-06-nablarch-testing-yaml-2.md`
参照点（ピン）: 解説書 `nablarch-document@afa4f9e`（パスは `ja/development_tools/testing_framework/…`）／`nablarch-testing@3c4bd2a`／`nablarch-testing-converter@d611bec`

着手前ベースライン（実測。`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`）:
`Tests run: 268, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。`@Test` 268件・`@Ignore` 1件。

完了時（実測。同コマンド。`src/` の最終コミット `00fc164`）:
`Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`。`@Test` 318件・`@Ignore` 0件。

---

## 結論

**指示書 第2節の7件はすべて是正した。** `mvn -o clean test` は `Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`。
`@Test` は 268件 → 318件、`@Ignore` は 1件 → 0件になった（§3）。7件のうち `src/main` の挙動を変えた5件（2-6・2-7 を除く）
それぞれについて、その `src/main` の変更だけを取り消すと
そのために足したテストが落ちることを最終状態で測り直してある（§3.2・§5.1。取り消しで落ちるテストは計43件）。

**カバレッジは第1回から下がった箇所が無い。** C0 99.29%（`INSTRUCTION_MISSED` 13）／C1 98.86%（`BRANCH_MISSED` 2）。
9クラスすべてで `INSTRUCTION_MISSED`・`BRANCH_MISSED` が第1回 `8eacaa7` と一致し、
未達は `#19` でユーザーが「到達不能」として承認済みの2箇所だけである（§7.1）。

**下流 `nablarch-testing-converter`（`d611bec`、未変更）で4件が新たに落ちる。** 着手前
`Tests run: 656, Failures: 0, Errors: 0, Skipped: 0` に対し、完了後は `Tests run: 656, Failures: 3, Errors: 1, Skipped: 0` /
**`BUILD FAILURE`**。内訳は 2-1 が2件・2-2 が1件・2-4 が1件で、いずれも converter 側のテストが是正前の挙動を期待して書かれているためである。
指示書 §5 が「converter を直さない」と定めているため本タスクでは直しておらず、
**共有ブランチ `ntf-test-data-converter` は現在 BUILD FAILURE のまま残っている**（§7.2）。

**決めていただきたいことが5件ある**（§8）。
(1) **本体と恒久的に食い違う仕様差を、この差のまま確定してよいか。** マーカーカラムだけに値があり他のキーを省略した行は、
行が残る点・カラム名は本体と一致するが、省略したカラムの値が**本体 `""`・YAML null** で食い違う（§4.2 の T5・L5）。
2-4 の決定の帰結として意図的に固定したものである。本体を oracle にして突き合わせた18ケース
（§4 の F1〜F6・M1・S2・T1〜T5・L1〜L5）のうち、**値が食い違うのは T5・L5 の1点だけ**である（§8.1）。
(2) converter の4件を誰がいつ直すか（§8.2）。
(3) 解説書 `testdata_notation.rst:889` の曖昧さ（`""` を「値」と数えるか）を起票するか（§8.3）。
(4) 2-5 の規則がスキーマ `description` の5箇所以上に未追随であることを、いま追随させるか（§8.4）。
(5) 行番号出典の方式を変えるか（§8.5）。

**指示書の外で行ったことが1件ある。** 2-7（スキーマ `description` の追随）に付随して、`src/` 配下の解説書出典の行番号を
**18箇所**訂正した（解説書の改版で `+2` ずれたもの13箇所と、着手前から指す先が誤っていたもの5箇所）。全件を §8.6 に挙げてある。

**出典の書き方**: 本モジュールは `<パス>:<行>`。断りが無ければ **`src/` の最終コミット `00fc164`** を指す
（`00fc164` 以降のコミットは本報告書だけを変更しており、`git diff --stat 00fc164 HEAD -- src/` は空である。
そのため `src/` については現在のブランチ HEAD と同じ内容を指す）。
**ただし §1 の `file:line` と grep 結果はすべて着手前 `3ee39c9` を指す**（同節の冒頭にも明記した）。
他の節で `3ee39c9` を指す箇所は、そのつど `3ee39c9` と書いてある。
解説書は `nablarch-document` のピン `afa4f9e` の `<パス>:<行>`（パスは `ja/development_tools/testing_framework/` からの相対）。
依存先はピン（`nablarch-testing` `3c4bd2a`／`nablarch-testing-converter` `d611bec`）。
指示書は `nablarch-document@origin/ntf-yaml-support`（`ef3a914`）の
`.rn/20260724-ntf-yaml-support/ntf-step4-06-nablarch-testing-yaml-2.md`。

---

## 指示書 §4 の完了条件10項目の充足

条件の文面は指示書 `.rn/20260724-ntf-yaml-support/ntf-step4-06-nablarch-testing-yaml-2.md:259`-`:269` からの逐語である。

| # | 完了条件（逐語） | 判定 | 根拠 |
|---|---|---|---|
| 1 | **第2節の7件がすべて是正されている。** 是正ごとに、直す前は落ちて直したあとは通るテストがあること（2-6・2-7 は除く） | 満たす | §3.2。5件それぞれの「取り消し方」と、取り消したときに落ちるテスト（4・4・8・12・15 = 計43件）を挙げてある |
| 2 | **2-1〜2-5 の「着手前に特定すること」の結果が、実装に入る前に報告されている** | **結果は満たす。「実装に入る前に報告した」ことは本報告書だけでは判断できない** | 結果は §1（2-1〜2-5 の全件）。報告した時点を示す記録は本報告書に無い |
| 3 | **足したテスト・直したテストそれぞれについて、期待値をわざと崩すと落ちることを1度確認している。** 確認したことを報告に書く | 満たす（§5.2 は本タスクでは再実行していない） | §5.1（実装側の変異5件。本タスクで実測）・§5.2（#36〜#40 の各タスクの記録）・§5.3（記録から辿れなかった11件。本タスクで実測し11件すべてが落ちた） |
| 4 | **既存テストの期待値を変えた箇所が全件挙がっている。** どれを変えどれを変えなかったかを、件数を数えたうえで報告する | 満たす | §6。251（共通）＝ 7（assert の行が変わった。うち期待値リテラルが変わったのは5件、assert の説明文だけが変わったのが2件）＋ 9（入力だけ変えた）＋ 235（テスト本体は不変。うち読むフィクスチャのエントリが変わったものが4件）。ほかに改名・統合・削除17件・新規67件 |
| 5 | **`@Ignore` が0件**（2-6 で削除し、新たに足していない） | 満たす | §3.2 の 2-6。`grep -rnE "^\s*@Ignore" src/` が0件、`mvn -o clean test` が `Skipped: 0` |
| 6 | **カバレッジ C0/C1 を計測し、結果を報告する。** `src/main` の是正で下がった箇所があれば挙げる | 満たす | §7.1。下がった箇所は無し（9クラス全件の `INSTRUCTION_MISSED`／`BRANCH_MISSED` が第1回と一致） |
| 7 | `mvn -o clean test` が緑。着手前は **267件成功・`@Ignore` 1件**（2026-08-27 ディレクター実測） | 満たす | 本報告書の冒頭。着手前 `Tests run: 268, Failures: 0, Errors: 0, Skipped: 1`（成功267・`@Ignore` 1）で指示書の数と一致。完了時 `Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` |
| 8 | `git status --short` が空。`tmp/` はテストスイート自身が作る空ディレクトリなので残ってよい | **報告書の外**（作業ツリーの状態） | `git status --short` で確認できる |
| 9 | 変更を push する | **報告書の外**（リポジトリの状態） | `git log --oneline origin/feature/ntf-yaml` で確認できる |
| 10 | **converter で落ちるテストを報告する**（直さない）。少なくとも `YamlFormatReaderScalarTest#skipsRowWhoseValuesAreAllEmpty`（`d611bec`）は 2-4 で落ちる見込み。着手前（`Tests run: 656, Failures: 0, Errors: 0, Skipped: 0`）からの差分を全件挙げる | 満たす | §7.2。着手前の 656／0 を本タスクで測り直し、完了後 `Failures: 3, Errors: 1` の全4件を挙げた。指示書が名指しした1件も実測で落ちた |

---

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
| （追加） | §8 決めていただきたいこと・記録 |

---

## 1. 2-1〜2-5 の「着手前に特定すること」の結果

**本節の `file:line`・grep 結果・件数はすべて着手前 `3ee39c9` を指す。**
HEAD で同じコマンドを打つと数が合わない（例: `grep -rn 'fwHeaderfields' src/test` は `3ee39c9` で0ヒット、HEAD では41ヒット。
`YamlSectionTest.java:473` は `3ee39c9` では `dropBlankRows_removesEmptyMappingAndAllBlankValueRows` の宣言行だが、HEAD では別の javadoc 行である）。
検算するときは `git show 3ee39c9:<パス>` または `git grep <パターン> 3ee39c9 -- <パス>` を使う。

走査対象は `src/test/**/*.yaml`（55ファイル）と `src/test/java/**/*.java` の全件。
YAML はテキスト検索ではなく PyYAML の `compose()` で構文木にしてから、セクション種別・エントリ・行を特定した。

### 2-1. 末尾に `null` を置いて `null` を期待している既存テスト — **0件**

`setup_files`・`expected_files`・`messages`・`expected_request_header_messages`・`expected_request_body_messages`・
`response_header_messages`・`response_body_messages` の全エントリの `records[].rows[]` を走査し、
末尾要素が YAML の null タグ（クォートなし `null`／`~`／値の省略）である行を探した。**該当0件。**

`nullValue()` を assert する既存テストも **10箇所**あるが、いずれもファイル・電文のデータ行ではない。
走査コマンドは `git grep -n 'nullValue()' 3ee39c9 -- src/test`（10行）。内訳は次のとおり。

- `YamlColumnOmissionTest` のテーブル値 **6件**（`:190`・`:242`・`:251`・`:335`・`:357`・`:361`）
- `YamlSectionTest` の `interpret`／`objectToString` の引数 null 2件（`:182`・`:243`）
- `YamlFileBuilderTest.java:570` の `record-length` ディレクティブ未設定1件
- `YamlTestDataParserTest.java:1423` の `list_maps` 値1件

**この走査は `nullValue()` に限ったものである。** `assertNull` を使う assert も `3ee39c9` の `src/test` に **31箇所**ある
（`git grep -n 'assertNull' 3ee39c9 -- src/test` の33行から `import` 2行を除いた数。
`YamlTestDataParserTest` 10・`YamlMessageBuilderTest` 6・`YamlTableDataBuilderTest` 15）。
31箇所すべてについて囲む `@Test` メソッド名を機械抽出して確かめたところ、対象は
`getListMap`／`list_maps` の値、テーブルの値、`buildMessagePool`／`buildSendSyncMessageList` の戻り値または `requestId` であり、
**ファイル・電文の `records[].rows[]` の末尾フィールドを見ているものは無い。**

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
- `YamlColumnOmissionTest`（2件。`omission.yaml` の `s4a`・`s4b` を読む `@Test` の全件。
  `git show 3ee39c9:…/YamlColumnOmissionTest.java` から `@Test` メソッドを機械抽出し、本体に `"s4a"` または `"s4b"` を
  含むものを取った結果、次の2件だけだった）
  - `columnNamesDependOnRowOrderAfterBlankRowRemoval`（`:174`）
  - `insertedValueDependsOnRowOrder`（`:187`）

変わらない見込みの既存テスト（`{}`・全値 null・マーカーカラムのみを扱うもの）:
`dropBlankRows_keepsRowHavingAnyNonBlankValue`（`:498`）、`dropBlankRows_keepsRowHavingOnlyMarkerColumnValue`（`:549`）、
`dropBlankRows_removesNonMappingRows`（`:570`）、`dropBlankRows_keepsRowHavingOnlyNullValues`（`:595`）、
`resolveColumns_emptyRowsReturnsEmptyList`（`:363`）、`resolveColumns_allEmptyMappingRowsReturnsEmptyList`（`:381`）、
`buildTableDataList_emptyRowEntrySkipped`（`:429`）、`buildListMapRows_emptyRowEntrySkipped`（`:947`）、
`buildTableDataList_leadingEmptyRow*`（`:1143`・`:1181`・`:1220`）、`buildListMapRows_leadingEmptyRowKeepsFollowingRows`（`:1256`）、
`buildTableDataList_emptyRowEntryInExpectedTableSkipped`（`:1423`）、`buildTableDataList_rowInterpretedToAllBlankIsKept`（`:1564`）、
`buildListMapRows_rowInterpretedToAllBlankIsKept`（`:1631`）ほか。**この一覧は全件ではない**（末尾の「ほか」）。
実測との突き合わせは §6.6 にある。2-4 起因で実際に期待値または名前が変わった既存テストは、§6.2 の5件と §6.4 の11件の計16件である。

### 2-5. 2文字の `\` ＋ `r` を値に置いている既存フィクスチャ・テスト — **フィクスチャ1件 / テスト1件**

- フィクスチャ: `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/nativeTypes.yaml:16`
  の `LITERAL_CR_COL: "\\r"`（YAML のダブルクォート内エスケープ。ロード後は `\` と `r` の2文字）
- テスト: `YamlTableDataBuilderTest#buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser`（`:591`）。
  `:603` で `is("\\r")`（Java 文字列リテラル。実体は2文字）を assert している

Java ソース中の `"\\r"` リテラルも全走査した。`git grep -nF '\\r' 3ee39c9 -- 'src/*.java'` は**5行**返すが、
うち `:584`・`:585`・`:587` は javadoc のコメントで Java 文字列リテラルではない。
リテラルは上記テストの `:600`（assert の説明文）と `:603`（期待値）の**2箇所のみ**である。
`src/main` では `ntf-testdata-yaml-schema.json:290` の `description` 本文に `"\r\n"` の説明が出てくるが、これは値ではなく説明文。
実際の CR（`"\r"`。1文字）は `nativeTypes.yaml:17` の `YAML_CR_COL`、`schemaFullCoverage.yaml:67` と
`YamlFileBuilderTest/fileData.yaml:171`・`:369` の `record-separator` にあり、いずれも**対象外**（2文字の `\`＋`r` ではない）。

---

## 2. 指示書の記述と実測が食い違った点（着手前に判明した分）

1. **指示書 §3「`YamlSectionTest` が既に POI を使っている」は、ピン `3ee39c9` では成り立たない。**
   `git ls-tree -r --name-only 3ee39c9 src/test/java` の全 `.java` を `grep -nE 'poi|Workbook|XSSF|HSSF'` に掛けると2ヒットするが、
   いずれも英単語 `point` の一部（`YamlTestDataParserTest.java:1391` の `required-decimal-point`、
   `YamlLoaderTest.java:504` の `error path must point to nested location`）であり、**POI の利用は0件**である。
   ただし POI 3.8（`poi`・`poi-ooxml`・`poi-ooxml-schemas`）は `nablarch-testing` 経由でテストクラスパスに載っている
   （`mvn -o dependency:build-classpath` で実測）。oracle 用の `.xlsx` を組む土台は新規に作る。作業自体は妨げられない。
2. **解説書のパスは `ja/development_tools/testing_framework/` 配下である。**
   指示書は `implementation/testdata_notation.rst`・`setup/common.rst` と短縮形で書いているが、
   `afa4f9e` での実パスは `ja/development_tools/testing_framework/implementation/testdata_notation.rst` および
   `ja/development_tools/testing_framework/setup/common.rst`。引用された行番号・本文はこのパスで全件一致した（2-1〜2-6 の8行を照合済み）。

---


## 3. 第2節7件の是正結果

**7件すべて是正済み。** `mvn -o clean test` は `Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`
（`src/` の最終コミット `00fc164`。`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean install` のテストフェーズで実測）。
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
- スコープ拡張1件・別種の出典訂正5件を同時に行った。§8.6 に分けて記す

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
**§4 の18ケース（F1〜F6・M1・S2・T1〜T5・L1〜L5）のうち値が食い違うのはこの T5・L5 だけであり、
この差は恒久的に残る。承認/差し戻しを分ける材料として、判断事項を §8.1 に立てた。**

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

### 5.2 各タスクの check ファイルに記録がある「期待値の崩し」（本タスクでは再実行していない）

下表は `.rn/ntf-yaml/checks/task-3*.md`・`task-4*.md` の「変異確認」節に**実測が記録されている**分である。
**本タスクでは再実行していない（未再実行）。** 対象テスト名と `file:line` は本タスクで現物を開いて存在を確認した。

| タスク | 崩した対象 | 件数 | 記録の場所 |
|---|---|---|---|
| #36（2-1） | `YamlTrailingNullOracleTest` の期待値リテラル（F1〜F6・M1・S2） | 8 | `.rn/ntf-yaml/checks/task-36.md` 「変異確認」 |
| #37（2-2） | `YamlLoaderTest` 4件・`YamlTestDataParserTest` 3件の期待値 | 7 | `.rn/ntf-yaml/checks/task-37.md` 「変異確認」 |
| #38（2-3） | `YamlMessageBuilderTest` の追加3件・変更4件（M1〜M8）ほかレビュー是正3ラウンド分 | 8＋ | `.rn/ntf-yaml/checks/task-38.md` 「変異確認」（3節） |
| #39（2-4） | `src/main` 側 5通り（M-A〜M-E）と、期待値を変えた／新規テスト 8通り（M24〜M31） | 13 | `.rn/ntf-yaml/checks/task-39.md` 「変異確認（実測）」 |
| #40（2-5） | 検査の無効化・定数の差し替え・出所／値の欠落・検査位置の入れ替え・フィクスチャの CR（M1〜M6） | 6 | `.rn/ntf-yaml/checks/task-40.md` 「変異確認（実測）」 |

#41（2-6）はテスト削除のみ、#42（2-7）は `description` と出典コメントのみで、崩す対象となる期待値を作っていない。

### 5.3 記録から辿れなかった11件（本タスクで実測した）

**突き合わせ**: HEAD で新規に足した67件（§6.1）のテスト名を、(a) `.rn/ntf-yaml/checks/*.md` の見出しに「変異」を含む節の本文と、
(b) §3.2／§5.1 で `src/main` を取り消したときに落ちる43件、の両方に機械照合した。
**どちらにも現れないものが11件**残った。§5.2 の記録だけでは、この11件の期待値を崩す確認が済んでいるかを辿れない。

**そこで本タスクで実測した。** 隔離コピー（`git worktree add --detach <scratchpad>/mut43 HEAD`）を作り、
11件それぞれの期待値リテラルを1つずつ崩して（変異はすべて別々のテストメソッド内で閉じており互いに干渉しない）
`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` を1度実行した。
結果は **`Tests run: 318, Failures: 11, Errors: 0, Skipped: 0` / `BUILD FAILURE`** で、
落ちたのは**狙った11件だけ**である。作業ツリーは崩していない（測定後 `git worktree remove --force` 済み。
`git worktree list` は本体1件のみ、`.git/worktrees` も残っていない）。

| # | テスト | 崩した内容 | 落ちた assert（失敗メッセージの行と要点） |
|---|---|---|---|
| M-a | `YamlBlankEntryOracleTest#getSetupTableData_emptyMappingRowIsSkipped`（`:232`） | 期待行を1行から2行（`{"", "", ""}` を追加）へ | `:233`→`assertTableValues:514` `T1 本体（Excel）: 行数`（`is <2>` / `was <1>`） |
| M-b | `YamlBlankEntryOracleTest#getSetupTableData_allNullRowIsKept`（`:265`） | 2行目の期待値 `{null, null, null}` → `{"", "", ""}` | `:266`→`assertTableValues:517` `T3 本体（Excel）: 1 行目の PK_COL1`（`is ""` / `was null`） |
| M-c | `YamlBlankEntryOracleTest#getSetupTableData_markerOnlyRowIsKept`（`:282`） | 2行目の期待値 `{"", "", ""}` → `{null, null, null}` | `:283`→`assertTableValues:517` `T4 本体（Excel）: 1 行目の PK_COL1`（`is null` / `was ""`） |
| M-d | `YamlBlankEntryOracleTest#getListMap_emptyMappingRowIsSkipped`（`:353`） | 期待件数を1件から2件（`{"", "", ""}` を追加）へ | `:354`→`assertListMapValues:530` `L1 本体（Excel）: 件数`（`is <2>` / `was <1>`） |
| M-e | `YamlBlankEntryOracleTest#getListMap_allNullRowIsKept`（`:384`） | 2件目の期待値 `{null, null, null}` → `{"", "", ""}` | `:385`→`assertListMapValues:535` `L3 本体（Excel）: 1 件目の KEY1`（`is ""` / `was null`） |
| M-f | `YamlBlankEntryOracleTest#getListMap_markerOnlyRowIsKept`（`:400`） | 2件目の期待値 `{"", "", ""}` → `{null, null, null}` | `:401`→`assertListMapValues:535` `L4 本体（Excel）: 1 件目の KEY1`（`is null` / `was ""`） |
| M-g | `YamlTableDataBuilderTest#buildListMapRows_yamlEscapeBecomesCr`（`:602`） | `YAML_CR_COL` の期待値 `is("\r")`（実 CR 1文字）→ `is("\\r")`（2文字） | `:611` `YAML のエスケープ "\r" は CR 文字になること` |
| M-h | `YamlTableDataBuilderTest#buildListMapRows_backslashUpperCaseRIsKeptAsIs`（`:758`） | `UPPER_R_COL` の期待値 `is("\\R")` → `is("\r")` | `:767`（`is "\r"` / `was "\R"`） |
| M-i | `YamlTableDataBuilderTest#buildListMapRows_literalBackslashRInMarkerColumnIsNotChecked`（`:785`） | `DATA_COL` の期待値 `is("ok")` → `is("ng")` | `:794`（`is "ng"` / `was "ok"`） |
| M-j | `YamlTableDataBuilderTest#buildListMapRows_allEmptyMappingRowsReturnsEmptyList`（`:1908`） | `assertTrue(…, result.isEmpty())` → `assertTrue(…, result.size() == 1)` | `:1916` `全行が空マッピングの場合は空リストが返ること` |
| M-k | `YamlSectionTest#entrySource_formatsSectionFieldAndValue`（`:330`） | `is("setup_tables entry table='USER'")` → `is("…table='OTHER'")` | `:331` `table 形式`（`is "…'OTHER'"` / `was "…'USER'"`） |

なお M-g のテストは `3ee39c9` の `buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser` を改名したもの（§6.4）で、
改名前の名前でなら `#40` の変異 M6（フィクスチャの `YAML_CR_COL: "\r"` を `"\n"` に変える）が同じ assert を落としている
（`.rn/ntf-yaml/checks/task-40.md:113`）。突き合わせを名前で行ったため上の11件に残った。

---

## 6. 既存テストの期待値を変えた箇所の全件

**測り方**: `3ee39c9` と `00fc164`（`src/` の最終コミット）の `src/test/java` から `@Test` メソッドを機械抽出し、
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

### 6.2 同じ名前のまま assert の行が変わった — 7件（うち**期待値リテラルが変わったのは5件**、**assert の説明文だけが変わったのが2件**）

上の機械判定（`assert*(`／`fail(`／`is(`／`containsString(`／`hasItem` を含む行の並びが変わったもの）は7件を拾うが、
そのうち2件は**期待値のリテラルが `is(1)` のまま変わっておらず、assert の第1引数（説明文）だけが変わっている**。
指示書 完了条件4 が問う「期待値を変えた箇所」は残り**5件**である。両者を「種別」列で区別した。

| テスト | 是正 | 種別 | 変えた内容 |
|---|---|---|---|
| `YamlColumnOmissionTest#columnNamesDependOnRowOrderAfterBlankRowRemoval` | 2-4 | **期待値** | 空エントリ `{}` の行が取り除かれることを前提に、行数（`is(2)`）とカラム名リストの assert を足し、`contains("NULL_COL"), is(false)` を差し替えた |
| `YamlSectionTest#dropBlankRows_keepsRowHavingOnlyMarkerColumnValue` | 2-4 | 説明文のみ | `result.size(), is(1)` は不変。説明文を「マーカーカラムの値も空行判定の対象になるため残ること」→「マーカーカラムを除外する前に判定するため行が残ること」に変えた |
| `YamlSectionTest#dropBlankRows_keepsRowHavingOnlyNullValues` | 2-4 | 説明文のみ | `result.size(), is(1)` は不変。説明文を「Java null は空文字ではないため行が残ること」→「値が全て Java null でもキーを持つ行であるため残ること」に変えた |
| `YamlTableDataBuilderTest#buildTableDataList_partiallyBlankValueRowKept` | 2-4 | **期待値** | 行数 1 → 2。2行目が全カラム空文字であることの assert を2行足した |
| `YamlTableDataBuilderTest#buildListMapRows_partiallyBlankValueRowKept` | 2-4 | **期待値** | 件数 1 → 2。2件目の `KEY1` が `""` であることの assert を足した |
| `YamlTestDataParserTest#getMessage_fwHeaderRecordTypeIsNotSkipped` | 2-2 | **期待値** | フィクスチャを1レコードに畳んだのに伴い、`getString("SEARCH_KEY"), is("SEARCHKEY1")` → `getString("HEAD_KEY"), is("HEADKEY002")` |
| `YamlTestDataParserTest#getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped` | 2-2 | **期待値** | 件数 3 → 2、`BODY_KEY` の assert を `HEAD_KEY` へ、レコード種別 `BODY` → `FW_HEADER`。フラグメントをまたぐ連番リセットの assert は、複数レコードレイアウト自体が禁止になったため落とした |

（「説明文のみ」の2件は `.rn/ntf-yaml/checks/task-39.md:77`-`:82` でも
「落ちなかったが記述だけ直したもの: 6 件」に分類してある。）

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

251 − 7（6.2）− 9（6.3）＝ **235件**は**テスト本体**を触っていない
（`@Test` メソッド本体の行が、空行を除いて `3ee39c9` と `00fc164` で完全に一致する）。

**ただし「入力も変わっていない」わけではない。** 235件のうち **139件**は §6.7 の変更フィクスチャのいずれかを読んでいる。
さらに、変更フィクスチャの中で**記述が変わったエントリ**（コメントの書き換えでも、別エントリの追加・削除でもないもの）に絞ると、
それを読むテストは次の **4件**である（全件）。

| テスト（`00fc164`） | 読むエントリと、その変更 |
|---|---|
| `YamlColumnOmissionTest#insertedValueDependsOnRowOrder`（`:206`） | `…/db/YamlColumnOmissionTest/omission.yaml` の `s4a`（先頭行 `:55`）・`s4b`（先頭行 `:73`）。先頭行が `NULL_COL: ""` → `{}` |
| `YamlMessageBuilderTest#buildMessagePool_fwHeaderMapReadableWithoutHeaderRecord`（`:1436`） | `…/yaml/YamlMessageBuilderTest/fwHeaderMapData.yaml` の `messages`／`req001` から独自キー `customProjectKey` を外した |
| `YamlTableDataBuilderTest#buildListMapRows_quotedNullIsKeptAsString`（`:547`） | `…/yaml/YamlTableDataBuilderTest/nativeTypes.yaml` の `interpreterTest` から `LITERAL_CR_COL: "\\r"` を外した |
| `YamlTableDataBuilderTest#buildListMapRows_spaceBetweenQuotesIsSpace`（`:572`） | 同上 |

測り方: `git diff --name-status 3ee39c9 00fc164 -- src/test` から変更（`M`）のフィクスチャ12件を取り、
その全差分を1件ずつ読んで「記述が変わったエントリ」を特定したうえで、235件の本体からそのエントリ ID を機械検索した。
残る135件が読むフィクスチャの変更は、コメント（出典・説明文）の書き換えか、**別の**エントリの追加・削除だけである。

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
| `YamlMessageBuilderTest/customFwHeaderData.yaml` | 2-3 | 冒頭コメントを「`fwHeaderFields` フィルタが廃止され全キーが保持される」から「`reader.fwHeaderfields` の名前だけ書ける」へ差し替えた（**データ本体は不変**。`customField` を通すための設定はテスト側の Given に置いた。§6.3） |
| `YamlColumnOmissionTest/omission.yaml` | 2-4 | `s4a`（`:55`）・`s4b`（`:73`）の先頭行を `NULL_COL: ""` から `{}` に変えた（全値 `""` の行が残るようになったため、「先頭に空行を置く」意図を空エントリで書き直した） |
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

| | 第1回 `8eacaa7` | 第2回 `00fc164`（`src/` の最終コミット） |
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
| 3 | `yaml.YamlFormatReaderScalarTest#skipsRowWhoseValuesAreAllEmpty:596`（FAILURE） | **2-4** | フィクスチャ（同ファイル `:583`-`:591`。`YamlFixture.read` に渡す YAML 文字列）は `setup_tables` に `- {}` と `- K: "" / V: ""` の2行を置き、**両方が読み飛ばされる**ことを期待している。javadoc（`:569`-`:572`）が引く出典は `testdata_notation.rst:1500`（`5783b35` 時点）の旧文「空マッピング（`{}`）**または**すべての値が空文字の場合にスキップされる」であり、`afa4f9e` の `:1502` で「`""` は値であり読み飛ばされない」に改訂された箇所そのものである。実測メッセージ: `Expected: is <[[x, 1]]> but: was <[[x, 1], [, ]]>` |
| 4 | `yaml.YamlFormatReaderRealFileTest#keepsFwHeaderNamedRecordInSendSyncFromRealYaml:640`（ERROR） | **2-2** | フィクスチャ（同ファイル `:640`-`:653`）は `response_body_messages` の1エントリに `records` を2件（`record_type: "FW_HEADER"` の1件と本文1件）書き、**2件とも残る**ことを期待している。是正後はスキーマ検証で弾かれる。実測メッセージ: `YamlSchemaValidationException: $.response_body_messages[0].records: アイテムは最大でも 1 個必要ですが、2 が見つかりました` |

**2-3・2-5 で落ちなかった理由（実測）**

- 2-3: converter の `src/test` にある `fw_header` のキーは、機械抽出の結果 `requestId`・`userId`・`resendFlag`・`dateSent` の4種のみ。
  `dateSent` は `YamlFormatWriterModelTest.java:762` にあるが、これは converter 側のモデルを直接組むテストで
  本モジュールの `YamlMessageBuilder.convertFwHeader` を通らない。実 YAML を読む経路（`YamlFormatReaderRealFileTest.java:817`-`:818`、
  `YamlTestCoreAdapterTest/messages.yaml:4`-`:5`、`YamlFormatWriterTest.java:235`-`:236`、`YamlTestDataValidatorTest.java:438`）は
  すべて既定4つの範囲内である
- 2-5: converter の `src/test` に Java 文字列として `\\r`（2文字）を書いた行は **12 行・14 箇所**ある
  （`git grep -nF '\\r' d611bec -- src/test`。`SpecialNotationRoundTripTest.java:312` と
  `XlsFormatWriterCellTypeTest.java:544` は1行に2箇所）。**YAML の値として本モジュールの `interpret` に
  2文字のまま渡るものは無い。** 14箇所の全件は次の3つに分かれる:
  - **YAML のダブルクォート内に書かれ、パーサが実 CR に展開するもの（4箇所）**:
    `xls/SpecialNotationRoundTripTest.java:312` の第2引数（YAML 記法 `"a\rb"`）と同 `:385`（`BODY: "1行目\r2行目"`）、
    `yaml/YamlFormatReaderInvalidInputTest.java:979`（`record-separator: "\r\n"`）、
    `yaml/YamlFormatWriterTest.java:337`（`- V: "a\"b\\c\n\r\t\x01"`）
  - **Excel のセル値・Excel 記法の文字列（YAML を通らない。7箇所）**:
    `xls/SpecialNotationRoundTripTest.java:312` の第1引数・同 `:376`、
    `xls/XlsFormatWriterCellTypeTest.java:544` の期待値・同 `:547`、
    `xls/XlsNotationSymmetryTest.java:200`・`:239`・`:293`
  - **assert の説明文（値ではない。3箇所）**:
    `xls/XlsFormatWriterCellTypeTest.java:544` の第1引数・同 `:546`、`xls/XlsNotationSymmetryTest.java:206`

**指示書 完了条件10 の見込みとの対応**: 指示書は「少なくとも `YamlFormatReaderScalarTest#skipsRowWhoseValuesAreAllEmpty` は 2-4 で落ちる」と書いており、
実測でもそのとおり落ちた（上表の #3）。加えて 2-1 起因が2件・2-2 起因が1件あった。

第1回の報告書（`.rn/ntf-yaml/report-step4.md:11`）は converter の結果を「新たに落ちた／解消した／無関係に落ち続けている」の
3分類で報告したが、今回は1つ目しか無い。**着手前が `Failures: 0, Errors: 0` だったため、残る2分類は該当0件だからである。**

**是正の主体（事実と推奨）**

- **事実**: `nablarch-testing-converter` の共有ブランチ `ntf-test-data-converter`（HEAD `d611bec`、未変更）は、
  本モジュールの是正後の jar を `.m2` へ install した状態で `mvn -o clean test` が
  `Tests run: 656, Failures: 3, Errors: 1, Skipped: 0` / **`BUILD FAILURE`** になる。
  指示書 §5 が「`nablarch-testing-converter` を直さない。落ちるテストは報告するだけ」と定めているため本タスクでは直しておらず、
  **共有ブランチはこの状態のまま残っている。**
- **推奨**: **4件とも converter 側で直す。本モジュール側で直すものは無い。** 直し方は2種類に分かれる。
  - **期待値の書き換えで済むもの（3件）**: 上表 #1（`is(Arrays.asList("a", null, ""))` → `("a", "", "")`）、
    #2（`is(nullValue())` → `is("")`。または末尾に値のあるフィールドを足して「末尾の null」でなくする）、
    #3（読み飛ばされる行を `- {}` の1行だけにし、javadoc が引く出典を `afa4f9e` の `:1502` に差し替える）
  - **テストの前提そのものを組み替えるもの（1件）**: 上表 #4。電文の1エントリに `records` を2つ書くこと自体が 2-2 で
    書けなくなったため、`FW_HEADER` を名乗るレコードが落とされないことは `records` 1件の形で確かめ直す必要がある
- **理由**: 4件はいずれも解説書 `afa4f9e` が定める挙動（2-1・2-2・2-4）に converter 側のテストが追随していないだけであり、
  本モジュールの実装を戻すと解説書に反する。#3 に至っては、converter の javadoc が引く出典が
  `testdata_notation.rst:1500`（`5783b35` 時点）の**改訂前**の文であることを、そのテスト自身が明記している

---
## 8. 決めていただきたいこと・記録

§8.1〜§8.5 は判断を仰ぐ項目である。各項を〈事実 / 選択肢 / 推奨 / 理由〉で書いた。
§8.6 は指示書の外で行ったことの記録である。

### 8.1 本体と恒久的に食い違う仕様差（§4.2 の T5・L5）を、この差のまま確定してよいか

- **事実**: マーカーカラム（`[NO]`）だけに値を持ち、他のカラムをキーごと省略した行は、
  **行が残る点とカラム名は本体（Excel）と一致するが、省略したカラムの値が本体 `""` に対し YAML では Java null になる。**
  これは 2-4 の決定（空とみなすのは `{}` だけ）と、#24 で確立した「省略したカラムは `null` を書いたのと同じ」という
  仕様の帰結で、意図して固定してある（`YamlBlankEntryOracleTest.java:329`-`:334`・`:443`-`:448` が
  本体の値と YAML の値の両方を assert して差を明示している）。
  本体を oracle にして突き合わせた18ケース（§4 の F1〜F6・M1・S2・T1〜T5・L1〜L5）のうち、
  **値が食い違うのはこの T5・L5 だけ**である
- **選択肢**:
  1. この差のまま確定する（現状）。値までそろえたい利用者は §4.2 の T4・L4 のように `""` を明示して書く
  2. 省略したカラムを `""` で埋めて本体に合わせる（`YamlTableDataBuilder` の行展開を変える）
  3. 他のカラムを省略した行をエラーにする
- **推奨**: **1（この差のまま確定する）**
- **理由**: 「カラム名決定行にはあるが個々の行で省略したカラムは、その行でそのカラムに `null` を書いたのと同じ扱いになる」は
  スキーマ `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:108`
  （`$defs.table_data.properties.rows.description`）が定めた本モジュールの仕様である。
  2 を採ると「省略＝null」と「省略＝`""`」がマーカーカラムの有無で分岐し、利用者が覚える規則が1つ増える。
  また `#39` の変異 M-E（行展開で省略カラムを `null` でなく `""` にする）は 16 件のテストを落とすことが実測されており
  （`.rn/ntf-yaml/checks/task-39.md:106`）、2 の影響範囲はそこに出ている。
  3 は既存の書き方（`YamlColumnOmissionTest` が担保しているカラムの省略）を壊す

### 8.2 converter で新たに落ちる4件を、誰がいつ直すか

- **事実**: `nablarch-testing-converter` の共有ブランチ `ntf-test-data-converter`（HEAD `d611bec`、未変更）は、
  本モジュールの是正後の jar を `.m2` へ install した状態で `mvn -o clean test` が
  `Tests run: 656, Failures: 3, Errors: 1, Skipped: 0` / **`BUILD FAILURE`** になり、**現在その状態のまま残っている**。
  4件の内訳・原因・失敗メッセージは §7.2 の表にある
- **選択肢**: 1. converter 側で4件を直す ／ 2. 本モジュールの是正を戻す ／ 3. 落ちたまま置く
- **推奨**: **1（converter 側で直す）。** 3件は期待値の書き換えで済み、1件（§7.2 の #4）はテストの前提の組み替えが要る（§7.2 の「是正の主体」）
- **理由**: 4件はいずれも解説書 `afa4f9e` が定める挙動に converter 側のテストが追随していないだけで、本モジュールの実装に誤りは無い。
  2 は解説書に反する。3 は下流のビルドが赤のまま固定され、次に converter を触る人が原因の切り分けからやり直すことになる

### 8.3 解説書 `testdata_notation.rst:889` の曖昧さを起票するか

- **事実**: `afa4f9e` の `ja/development_tools/testing_framework/implementation/testdata_notation.rst:889` は
  「末尾のフィールドに ``null``\ と記述した場合は、形式によらず\ ``""``\ になる。後ろに値のあるフィールドがあれば\ null\ のまま保持される」
  と書くが、`""` を「値」と数えるかが曖昧である。実装（本体 `nablarch-testing` の
  `../nablarch-testing/src/main/java/nablarch/test/NablarchTestUtils.java:251`-`:263` `trimTail`）は
  末尾から `StringUtil.hasValue` が偽の要素を連続して取り除き、`hasValue` は **null と `""` の両方**で偽になる。
  依存クラスパスで `NablarchTestUtils.trimTailCopy` を直接呼んで実測した結果は次のとおり:

  | 入力 | `trimTailCopy` の結果 |
  |---|---|
  | `["x", null, ""]` | `["x"]` ← **`null` が保持されない** |
  | `["x", "", null]` | `["x"]` |
  | `["x", null, "y"]` | `["x", null, "y"]` |
  | `["x", null, null]` | `["x"]` |
  | `["x", "", "y"]` | `["x", "", "y"]` |

  `["x", null, ""]` では `null` の後ろに `""` というフィールドが**書かれている**。
  2-4（`afa4f9e` の `:1502`「`""` と書いた空文字は値であり」）に従えば `""` は値なので、`:889` の文からは
  「`null` のまま保持される」と読めるが、実際は保持されない。
  **本モジュールは本体の `trimTailCopy` をそのまま呼んでいるので実装は本体と一致しており、是正は不要である。**
  問題は解説書の文言だけである
- **選択肢**: 1. `.rn/ntf-yaml/report-nablarch-document-discrepancies.md` に追記して起票する ／
  2. 起票せず、`:889` の「値のあるフィールド」は「`""` でない値」の意だと了解して閉じる
- **推奨**: **1（起票する）**
- **理由**: この一文は 2-1 の是正の根拠そのものであり、`""` を値と数えるかは 2-4 の決定と正面から関わる。
  読み手が `:889` と `:1502` を並べると矛盾して読める状態が残る。
  起票の書式は同ファイルの既存項目に合わせられる。**同ファイルは本タスクでは書き換えていない**

### 8.4 2-5 の規則をスキーマ `description` に追随させるか

- **事実**: 2-5 の規則（バックスラッシュと `r` の2文字はエラー）について、
  解説書（`afa4f9e` の `testdata_notation.rst:1445`）・実装（`YamlSection.rejectLiteralCr`）・
  テスト（§3.2 の 2-5 の15件）の三者は揃っている。
  一方、スキーマ `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` で
  **2-5 の規則に触れているのは `:293`（`record-separator`）の1箇所だけ**である。
  `grep -n 'バックスラッシュ' src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` は2件返すが、
  もう1件の `:333`（`field-separator`）は「YAML の `"\t"` は実際のタブ文字に展開されるためバックスラッシュをエスケープする」という
  **別件**の注意で、2-5 の規則ではない。`:293` は `#42` で「ディレクティブ値に実害がある1箇所」として追記したものである。
  規則は**値全般**（データ行・ディレクティブ・制御ヘッダ）に掛かるため、少なくとも
  `:108`（`table_data.rows`）・`:136`（`list_map_data.rows`）・`:380`（`record_fragment.rows`）・
  `:216`／`:433`（`fw_header`）にも同じ注意が要る。**5箇所以上が未追随である**
- **選択肢**: 1. いま5箇所以上に追記する ／ 2. 次の指示書に回す ／ 3. 追記しない（`:293` だけで足りるとする）
- **推奨**: **2（次の指示書に回す）**
- **理由**: 指示書 2-7 の表は 2-1〜2-4 の4件を挙げており 2-5 の追随を求めていない。
  `#42` はこれを「指示書 2-7 の対象（指示書 `:226`-`:229` の表が `3ee39c9` の行番号で挙げる
  `:108`／`:136`／`:213`-`:215`／`:424`-`:430`／`:208`・`:241`・`:272`／`:377`）の外」として
  スコープ外にした。いま足すと指示書の範囲を再び越える。
  一方 3 は採れない。規則は値全般に掛かるのに `record-separator` にしか書かれていない状態は、
  スキーマの `description` を読んで書く利用者に対して不完全である

### 8.5 行番号出典の方式を変えるか

- **事実**: 行番号出典は解説書の改版のたびに壊れる。今回それが 18 箇所で起きた（§8.6）。
  本タスクの実測では、`src/` 配下に解説書の**リビジョンのピンは1つも書かれていない**
  （`grep -rl "afa4f9e\|nablarch-document@" src/` が **0件**）。
  一方、行番号を含む出典は **60 箇所・16 ファイル**にある
  （`grep -rEo '[a-z_/]+\.rst:[0-9]+' src/ | wc -l` が 60、`grep -rEl '\.rst:[0-9]+' src/ | wc -l` が 16）。
  したがって**壊れたことに気づく仕組みが無い**。今回も `#41` のレビューが拾って初めて分かった
- **選択肢**:
  1. ピン付きの解説書チェックアウトを前提に `src/` 配下の全出典を解決し、
     「指し先が範囲内・非空・記録した引用文を含む」ことを突き合わせる機械検証を1本入れる
  2. 出典を「節見出し＋逐語引用」にし、行番号は補助として必ずピン付きで書く（例: `testdata_notation.rst@afa4f9e:1328`）
  3. 現状のまま（行番号のみ・ピンなし）
- **推奨**: **1 を先に、2 を次に。** 1 は `#42` と本タスクで使ったスクリプトと同じ発想で作れ、差分が小さい。
  2 は本モジュールに既に先例がある:
  `src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java:56`-`:66`、
  `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:176`-`:179`、
  `src/test/java/nablarch/test/core/reader/YamlBlankEntryOracleTest.java`（行番号出典 0件）、
  `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/mixedFwHeaderKeysData.yaml:1`-`:13`（フィクスチャのコメント）
- **理由**: 3 は採れない。18 箇所が壊れた事実がすでにあり、うち5箇所は着手前から誤っていて誰も気づいていなかった（§8.6 の (2)）。
  1 を先に置くのは、2 の方式へ移す作業自体が 60 箇所の書き換えになり、その正しさを確かめる手段が先に要るからである。
  **本タスクではどちらも実施していない**（指示書の範囲外）

### 8.6 スコープ拡張（`#42` で実施。指示書には無い）

**指示書 §2 の 2-7 は「スキーマ `description` を解説書に合わせる」だけを求めているが、`#42` では `src/` 配下の解説書出典の行番号も訂正した。**
本タスクで `cb82f3b`（`#42` 直前）と `00fc164` の全出典を機械抽出して突き合わせ、**変わったのは 18 箇所**であることを確認した。
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

引き継ぎ資料は「別種の誤りは3件」としていたが、**実測では5件**である。
`7daae89` で直した `:1149` → `:1151` の2件が数えられていなかった。この2件は `:1299` 未満を指すため `+2` ずれの13箇所にも含まれない。
第1回の報告書（`.rn/ntf-yaml/report-step4.md`）が記録しているのは「**指示書**の `testdata_notation.rst:1149` が現物では `:1151` である」ことであり、
`src/` 側の出典が同じ誤りを持っていたことは記録されていない。両者は別の箇所を指している。

---

## 付録: コーディネータへの申し送り

この付録は宛先がコーディネータであり、承認/差し戻しの判断材料ではない。

### A. `#41` で `@Ignore` 付きテスト1件を削除した結果、`#31` の記録2箇所が前提を失った

削除したのは `YamlTableDataBuilderTest#buildListMapRows_unknownCharacterTypeIsNotConverted`（`3ee39c9` の `:753`）。
現在の `src/` に該当テストは無い（`grep -rn "buildListMapRows_unknownCharacterTypeIsNotConverted" src/` が **0件**）。

| 失効した記録 | 逐語 | 現状 |
|---|---|---|
| `.rn/ntf-yaml/steering.md:1140`（`#31` Step 3-2） | 「**列挙外の文字種名は変換されないという負のテストも必ず書く**」 | 該当テストは存在しない。前半（14文字種）は `YamlTableDataBuilderTest#buildListMapRows_allFourteenCharacterTypesAreGenerated` が引き続き担保 |
| `.rn/ntf-yaml/steering.md:1154`（`#31` Completion criteria） | 「3-2 の負のテスト（列挙外の文字種名は変換されない）が書かれている」 | 同上 |

`steering.md` の上記2箇所には、コーディネータが「`#41` で失効」の注記を追記済みであることを本タスクで確認した（`:1141`・`:1155`）。
**`.rn/ntf-yaml` 以下の他の記録（`checks/task-31.md`）への注記は未了**である。
`checks/task-31.md` の3箇所（`:8`・`:9`・`:23`）が「負のテストが `@Ignore` 付きで存在する」という当時の状態を記録しており、
現在の `src/` とは一致しない（当時の実測記録としては真）。注記を入れるかどうかは未決である。

`#41` の完了条件「`@Ignore` が `src/test` 全体で0件」と `#31` の完了条件「落ちたものは `@Ignore` で記録されている」は字面上両立しないが、
**`#31` が記録した `@Ignore` 1件を `#41` がテストごと削除したため、両者は同じ状態（`@Ignore` 0件・負のテスト無し）を指している。**

### B. §8.3 の起票先

`.rn/ntf-yaml/report-nablarch-document-discrepancies.md` は本タスクでは書き換えていない。

---

## 9. `#45`: 解説書への参照の除去・2-5 のスキーマ追随・T6/L6 の追加

指示書 `nablarch-document@origin/ntf-yaml-support` の
`.rn/20260724-ntf-yaml-support/ntf-step4-06-nablarch-testing-yaml-2.md` §8（承認文面と確認2件への回答・訂正3件）による。

### 9.0 Rules の参照点（ピン）の取り直し

`steering.md:69` の「参照点（ピン）」の解説書だけを `afa4f9e` → `a6da1f6` に取り直した
（本モジュール `3ee39c9`・`nablarch-testing` `3c4bd2a`・`nablarch-testing-converter` `d611bec` は変えていない）。
`git -C ../nablarch-document diff --stat afa4f9e a6da1f6 -- ja/` は 2 ファイル 3 挿入 3 削除で、行番号は変わらない。
あわせて、ピン行にあった「`ja/` 配下は `05e57a1` と同一」の注記を落とした。
`git diff --stat 05e57a1 a6da1f6 -- ja/` が 2 ファイル 3 挿入 3 削除を返し、`a6da1f6` では成り立たないためである。

### 9.1 `src/` から取り除いた解説書への参照 — 122 行・28 ファイル

抽出方法は `git grep -nE '\.rst|nablarch-document|解説書|出典|根拠:' -- src`（着手前・`58c7bc1` 時点）。
パターン別の出現行数（重複あり。`git grep -ncE '<pat>' -- src` の合計）は
`.rst` 77 行（26 ファイル）／`nablarch-document` 9 行／「解説書」52 行／「出典」28 行／「根拠:」37 行で、
ユニークな行の合計が 122 行・28 ファイルである。`.rst` 77 行・26 ファイルと
「解説書／出典／根拠:」108 行は `aac55ad` の実測と一致する。

| ファイル | 件数 | 取り除いた行（着手前の行番号） |
|---|---|---|
| `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java` | 1 | 261 |
| `src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java` | 3 | 54, 55, 57 |
| `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` | 12 | 174, 175, 275, 276, 344, 345, 347, 360, 361, 377, 406, 421 |
| `src/test/java/nablarch/test/core/db/YamlDateNotationTest.java` | 3 | 30, 113, 133 |
| `src/test/java/nablarch/test/core/db/YamlDateNotationTest/date.yaml` | 1 | 1 |
| `src/test/java/nablarch/test/core/reader/YamlBlankEntryOracleTest.java` | 16 | 32, 33, 187, 223, 240, 256, 273, 297, 459, 460, 484, 485, 506, 508, 524, 526 |
| `src/test/java/nablarch/test/core/reader/YamlBlankEntryOracleTest/blankEntry.yaml` | 1 | 2 |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java` | 16 | 290, 318, 321, 324, 356, 932, 981, 1102, 1205, 1658, 1662, 1702, 1718, 1813, 1855, 1897 |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/legacyFwHeaderRecord.yaml` | 1 | 4 |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/messageData.yaml` | 3 | 24, 43, 122 |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/nativeTypes.yaml` | 3 | 17, 25, 33 |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/otherDir/CommonTestData/employees.yaml` | 1 | 3 |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/quotedValues.yaml` | 1 | 1 |
| `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/sendSyncTestData/RM21AA0101/message.yaml` | 1 | 4 |
| `src/test/java/nablarch/test/core/reader/YamlTrailingNullOracleTest.java` | 15 | 29, 31, 45, 131, 205, 221, 237, 255, 271, 288, 309, 342, 373, 397, 399 |
| `src/test/java/nablarch/test/core/reader/YamlTrailingNullOracleTest/trailingNull.yaml` | 1 | 1 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java` | 4 | 65, 1052, 1085, 1142 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest/fileData.yaml` | 1 | 468 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest.java` | 5 | 570, 608, 641, 675, 709 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest/schemaViolation_prefixMatchedTopLevelKey.yaml` | 1 | 4 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java` | 4 | 73, 939, 1884, 2090 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/customFwHeaderData.yaml` | 1 | 3 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/fwHeaderMapData.yaml` | 2 | 7, 54 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/mixedFwHeaderKeysData.yaml` | 1 | 5 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlSectionTest.java` | 2 | 563, 598 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java` | 14 | 56, 619, 660, 687, 912, 914, 922, 967, 1000, 1479, 1809, 1957, 1993, 2022 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/nativeTypes.yaml` | 4 | 19, 83, 88, 133 |
| `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/tableData.yaml` | 4 | 169, 189, 326, 414 |

Javadoc・テストの説明は、引用と出典表記をやめて「何を確かめるか」を自分の言葉で書き直した。
既存の Given/When/Then と本体クラス名への言及は残している。

**他リポジトリのソースを指す `path:line` は、パスと行番号を落としてクラス名だけ残した（10 箇所）。**

| 箇所 | 前 | 後 |
|---|---|---|
| `YamlSection.java`（`:384`） | `../nablarch-testing/src/main/java/nablarch/test/core/util/interpreter/LineSeparatorInterpreter.java:31` | `LineSeparatorInterpreter` |
| `YamlSection.java`（`:407`） | `../nablarch-testing/src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:183` | `TestDataParsingTemplate` |
| `YamlMessageBuilderTest.java`（`:66`・`:124`・`:1078`） | `../nablarch-testing/src/main/java/nablarch/test/core/reader/MessageParser.java:107`-`:110` | `MessageParser` |
| `YamlMessageBuilderTest.java`（`:1155`） | `MessageParser.java:108` | `MessageParser` |
| `YamlSectionTest.java`（`:262`） | `../nablarch-testing-converter/src/main/java/nablarch/test/core/reader/YamlTestCoreAdapter.java:73` | `YamlTestCoreAdapter` |
| `YamlTableDataBuilderTest.java`（`:748`） | `../nablarch-testing/src/main/java/nablarch/test/core/util/interpreter/LineSeparatorInterpreter.java:31` | `LineSeparatorInterpreter` |
| `YamlTestDataParserTest.java`（`:1857`） | `SendSyncSupport.java:347`・`:393` | `SendSyncSupport#createTestDataInfo` |
| `YamlTrailingNullOracleTest.java`（`:317`） | `nablarch-testing` の `MockMessages.java:64` | `MockMessages$MockMessage#removePadding` |

本モジュール自身を指す `YamlLoader.java:151`（`YamlMessageBuilderTest.java:1385`）は指示どおり対象外で、そのまま残している。

**確認**（作業後・`git grep` は追跡ファイルに対して実行）:

- `git grep -nE '\.rst|nablarch-document|解説書|出典' -- src/` → **0 件**（指示書の式）
- `git grep -n '根拠:' -- src/` → **0 件**
- `git grep -nE '\.\./nablarch-|[A-Za-z]+\.java:[0-9]+' -- src` → **1 件**（上記の `YamlLoader.java:151` のみ）

### 9.2 2-5 の規則をスキーマ `description` 5 箇所へ追記

文言は `record-separator`（`:293`。`#42` で追記）の既存文をそのまま切り出して揃えた。

| 追記先 | 行（追記後） | 追記した文 |
|---|---|---|
| `$defs.table_data.properties.rows` | `:108` | 【値に書けない2文字】バックスラッシュと `r` の2文字（`"\\r"`）を含む値はエラーになる（Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値はテスティングフレームワークの仕様上存在しない） |
| `$defs.list_map_data.properties.rows` | `:136` | 同上 |
| `$defs.message_data.properties.fw_header` | `:216` | バックスラッシュと `r` の2文字（`"\\r"`）を含むキー名・値はエラーになる（以下同文） |
| `$defs.record_fragment.properties.rows` | `:380` | バックスラッシュと `r` の2文字（`"\\r"`）を含む値はエラーになる（以下同文） |
| `$defs.fw_header` | `:433` | バックスラッシュと `r` の2文字（`"\\r"`）を含むキー名・値はエラーになる（以下同文） |

**前提（指示書 §8 に無いため、ここに書いて進めた）**: `fw_header` の 2 箇所だけ「キー名・値」とした。
`YamlMessageBuilder`（`convertFwHeader`）がキーと値の両方に `YamlSection#rejectLiteralCr` を掛けており、
スキーマ検証を通過した入力に対して外から観測できる挙動だからである（`$defs.fw_header` は
`additionalProperties: {"type":"string"}` でキー名を制限しないため、この 2 文字を含むキーはスキーマでは止まらない）。
残る 3 箇所は値だけを対象にした。テーブル系・`list_maps` のカラム名は検査を通らず素通りするためである。
JSON として壊れていないことは `json.load` で確認した。

### 9.3 `YamlBlankEntryOracleTest` の T5/L5 の書き直しと T6/L6 の追加

T5/L5 の Javadoc から「仕様差」の枠組みを外し、「キーを省略したカラムは `null` を明示したのと同じ扱いになる。
Excel の空セルは `""` なので入力が非等価であり、値だけが分かれる」という説明に書き直した
（`YamlBlankEntryOracleTest.java` の T5・L5 の Javadoc と、本文コメント 2 箇所の「（仕様差）」→「（入力が非等価なため）」）。
クラス Javadoc の「マーカーカラムだけに値がある行」の節も、2 通り → 3 通り（T4/L4・T5/L5・T6/L6）に書き直した。

**足したケース**:

| ケース | 本体（Excel）側の入力 | YAML 側の入力 | 本体の値 | YAML の値 |
|---|---|---|---|---|
| T6（`setup_tables`） | カラム名行 `[NO]`,`PK_COL1`,`VARCHAR2_COL`,`NULL_COL`。通常行 `1`,`00001`,`v1`,`n1`。判定対象行 `2`,`null`,`null`,`null` | 通常行 `"[NO]": "1"` ＋ 3 カラム。判定対象行は `- "[NO]": "2"` のみ（他はキーごと省略） | 1 行目 `00001`／`v1`／`n1`、2 行目 `null`／`null`／`null` | 本体と同一（カラム名・行数・全カラムの値が一致） |
| L6（`list_maps`） | キー名行 `[NO]`,`KEY1`,`KEY2`,`KEY3`。通常行 `1`,`v1`,`v2`,`v3`。判定対象行 `2`,`null`,`null`,`null` | 通常行 `"[NO]": "1"` ＋ 3 キー。判定対象行は `- "[NO]": "2"` のみ | 1 件目 `v1`／`v2`／`v3`、2 件目 `null`／`null`／`null` | 本体と同一（キー集合・件数・全キーの値が一致） |

Excel 側の `null` が Java `null` になるのは、本体の `interpreters` に含まれる `NullInterpreter` による。
これにより「キーの省略」と「`null` の明示」が等価な入力になり、T5/L5 の食い違いが形式間の仕様差ではなく
入力の非等価によることが示される。

**期待値を崩すと落ちることの確認（1 度）**: T6・L6 の期待値を `{null, null, null}` → `{"", "", ""}` に変え、
`mvn -o clean test -Dtest=YamlBlankEntryOracleTest` を実行して
`Tests run: 12, Failures: 2, Errors: 0, Skipped: 0` を得た。落ちた 2 件は
`getSetupTableData_markerOnlyRowWithOmittedColumnsMatchesExplicitNull`（`Expected: is "" but: was null`。
`assertTableValues` の「T6 本体（Excel）: 1 行目の PK_COL1」）と
`getListMap_markerOnlyRowWithOmittedKeysMatchesExplicitNull`（同じく「L6 本体（Excel）: 1 件目の KEY1」）である。
崩した箇所は元に戻し、再実行で緑を確認した。

### 9.4 `checks/task-31.md` への「#41 で削除」注記

指示の 3 箇所（`:8`・`:9`・`:23`）に「**【#41 で削除】このテストは `#41`（2-6）でテストごと削除済み。
現在の `src/` に該当メソッドは無く、`@Ignore` は 0 件である。以下は当時の実測記録。**」を入れた。
`:23` は表のセルのため「**#41 で削除**」と「`@Ignore`（**#41 で削除済み**）」の短い形にしてある。

**前提（指示書 §8 に無いため、ここに書いて進めた）**: `:7` にも同じテスト名が出るため、
`（負）` → `（負。**#41 で削除**）` の短い注記を 1 箇所足した（指示の 3 箇所に加えて 4 箇所目）。
`:94` の「失効した記述」の表は `:8`・`:9`・`:23` の失効をすでに記録しているため、手を入れていない。

### 9.5 完了条件の確認

| 完了条件 | 結果 | 根拠 |
|---|---|---|
| `mvn -o clean test` 緑（318 件＋T6/L6） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `BUILD SUCCESS` / `Tests run: 320, Failures: 0, Errors: 0, Skipped: 0`（318 + T6/L6 の 2 件） |
| `@Ignore` 0 件 | OK | `grep -rnE '^\s*@Ignore' src/test` → 0 件 |
| 9.1 の grep が 0 件 | OK | `git grep -nE '\.rst|nablarch-document|解説書|出典' -- src/` → 0 件。「根拠:」も 0 件 |
| `git status --short` 空・push | OK | 9.6 のとおり |

### 9.6 テストの動作・期待値を変えていないこと

`#45` で変えたのは Javadoc・テストコメント・フィクスチャのコメントと、スキーマの `description`・
`steering.md` のピン行・`checks/task-31.md` の注記である。**既存テストの動作・期待値は 1 件も変えていない。**
足したのは T6・L6 の 2 件と、それに対応する oracle シート 2 枚（`buildOracleBook`）・フィクスチャの
2 エントリ（`blankEntry.yaml` の `T6`・`L6`）だけである。
