# task-26 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `isBlankRow` が空文字のみを空と見なし、Java null を非空として扱う | OK | `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:205` の判定を `if (str != null && !str.isEmpty())` → `if (str == null \|\| !str.isEmpty())` に是正。javadoc も `:152`-`:158`（dropBlankRows）と `:194`-`:200`（isBlankRow）で「全ての値が null または空文字」→「全ての値が空文字」＋「Java null は非空として扱い `COL: null` / `COL:` だけの行は残す」に更新。`YamlTableDataBuilder.java:37`-`:39` / `:89`-`:92` / `:168`-`:171` と `YamlFileBuilderTest.java:402` の説明コメントも同じ表現に揃えた（追補参照） | | |
| スキーマ `:108`・`:136` の `description` が解説書 `:1500` と食い違わない | OK | `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:108`（table_data.rows）と `:136`（list_map_data.rows）の空行除去の条件の記述を「【全ての値が空文字の行は行として存在しない】空マッピング `{}` の行、および全ての値が空文字 `""` の行は…」へ変更し、Java null（クォートなし `null`・値省略 `COL:`）は非空として残る旨を明記。解説書 `nablarch-document` `5b5c91e` の `ja/development_tools/testing_framework/implementation/testdata_notation.rst:1500`「空マッピング（ `{}` ）またはすべての値が空文字の場合にスキップされる」と一致。FK 制約・カラム省略など同一 description 内の他の記述は未変更（`git diff` の当該 2 行のみ） | | |
| 既存テスト12件（5+5+2）について、変更したもの・しなかったものが件数付きで記録されている | OK | 下記「既存テスト12件の数え直し」参照（変更 8 件 / 未変更 4 件。うちアサーションを変更したのは 1 件のみで、残る 7 件はフィクスチャと javadoc の変更）。加えて 12 件の外で波及した 3 件も記録 | | |
| 是正前に落ち是正後に通るテストが存在する | OK | `dropBlankRows_keepsRowHavingOnlyNullValues` / `buildTableDataList_nullValueOnlyRowKept` / `buildListMapRows_nullValueOnlyRowKept` の 3 件。是正前: `Tests run: 76, Failures: 3, Errors: 0, Skipped: 0`（この 3 件が失敗）／是正後の最終フル実行: `Tests run: 229, Failures: 0, Errors: 0, Skipped: 0` | | |
| 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 下記「step E 変異確認」参照（変異実行 3 回、コマンドと結果を記載） | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 229, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（着手前ベースライン 226 + 追加 3 件） | | |

## Method（テストファースト）の適用

1. 実装を触る前に、期待する挙動を捉える落ちるテストを先に書いた。
   - `YamlSectionTest#dropBlankRows_keepsRowHavingOnlyNullValues`（新規）
   - `YamlTableDataBuilderTest#buildTableDataList_nullValueOnlyRowKept`（新規、フィクスチャ `tableData.yaml` の `setup_tables` に `nullValueOnlyRow` グループを追加。クォートなし `null` と値を省略した `PK_COL2:` の両方を置く）
   - `YamlTableDataBuilderTest#buildListMapRows_nullValueOnlyRowKept`（新規、`list_maps` に `nullValueOnlyRowListMap` を追加）
2. 是正前に実行して 3 件とも落ちることを確認した。
   - `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest='YamlSectionTest,YamlTableDataBuilderTest' -DfailIfNoTests=false`
   - 結果: `Tests run: 76, Failures: 3, Errors: 0, Skipped: 0`
     - `dropBlankRows_keepsRowHavingOnlyNullValues` — Expected: is <1> but: was <0>
     - `buildTableDataList_nullValueOnlyRowKept` — Expected: is <2> but: was <1>
     - `buildListMapRows_nullValueOnlyRowKept` — Expected: is <2> but: was <1>
3. `YamlSection#isBlankRow` を是正し、3 件が通ることを確認したうえで既存テスト・フィクスチャの期待値見直しに進んだ。

## 既存テスト12件の数え直し

### `YamlSectionTest#dropBlankRows_*`（5 件）— 変更 1 件 / 未変更 4 件

| # | テスト | 変更 | 内容 |
|---|---|---|---|
| 1 | `dropBlankRows_removesEmptyMappingAndAllBlankValueRows` | 変更 | Given から `rowOf("COL_A", null, "COL_B", null)`（全値 null の行）を削除し、`{}` 行・全値空文字行・値を持つ行の 3 件に。javadoc の「null／空文字」→「空文字」。全値 null の行は新規テストが担当 |
| 2 | `dropBlankRows_keepsRowHavingAnyNonBlankValue` | 未変更 | Given は (null, "", "v")。是正前後とも「残る」で期待値が変わらない |
| 3 | `dropBlankRows_keepsRowHavingOnlyWhitespaceValue` | 未変更 | null を含まない（半角スペースと空文字のみ） |
| 4 | `dropBlankRows_keepsRowHavingOnlyMarkerColumnValue` | 未変更 | null を含まない |
| 5 | `dropBlankRows_removesNonMappingRows` | 未変更 | スカラ行の除去。null 値を含まない |

セクション見出しコメント（`YamlSectionTest.java:421`）の「全値が null／空文字」→「全値が空文字」も更新（テスト本体ではないため上表の件数外）。

### `YamlTableDataBuilderTest#buildTableDataList_blankValueRow*`（5 件）— 変更 5 件 / 未変更 0 件

いずれも「空行」を表すフィクスチャ行に Java null が含まれており、是正後はその行が残ってしまうため、当該行の `null` を `""` に置き換えた。**テストコードのアサーション（期待件数・期待値）は 5 件とも変更していない**（変更したのは javadoc の文言とフィクスチャのみ）。

| # | テスト | 変更 | 内容 |
|---|---|---|---|
| 1 | `buildTableDataList_blankValueRowLeadingExcluded` | 変更 | `tableData.yaml` `setup_tables` の `blankValueRowLeading`: `VARCHAR2_COL: null` → `""`。javadoc「全て null／空文字」→「全て空文字」 |
| 2 | `buildTableDataList_blankValueRowMiddleExcluded` | 変更 | `blankValueRowMiddle` の空行: `PK_COL2: null` / `NUMBER_COL: null` → `""`。javadoc 同上 |
| 3 | `buildTableDataList_blankValueRowLeadingInExpectedTableExcluded` | 変更 | `expected_tables` の `blankValueRowLeadingExpected`: `VARCHAR2_COL: null` → `""`。javadoc 同上 |
| 4 | `buildTableDataList_blankValueRowMiddleInExpectedTableExcluded` | 変更 | `blankValueRowMiddleExpected` の空行: `PK_COL2: null` / `NUMBER_COL: null` → `""`。javadoc 同上 |
| 5 | `buildTableDataList_blankValueRowInExpectedCompleteTableExcluded` | 変更 | `completedTable.yaml` の `blankValueRowComplete` の空行 3 件（先頭・中間・末尾）の `null` → `""`（`VARCHAR2_COL: null` / `PK_COL1: null` / `PK_COL2: null`）。フィクスチャ冒頭コメントも「全ての値が空文字の行」に更新 |

### `YamlTableDataBuilderTest#buildListMapRows_blankValueRow*`（2 件）— 変更 2 件 / 未変更 0 件

| # | テスト | 変更 | 内容 |
|---|---|---|---|
| 1 | `buildListMapRows_blankValueRowLeadingExcluded` | 変更 | `blankValueRowLeadingListMap`: `KEY8: null` → `""`。javadoc「全て null／空文字」→「全て空文字」。アサーションは未変更 |
| 2 | `buildListMapRows_blankValueRowMiddleExcluded` | 変更 | `blankValueRowMiddleListMap` の空行: `KEY2: null` → `""`。javadoc 同上。アサーションは未変更 |

**12 件の合計: 変更 8 件（うちアサーション変更は 1 件のみ）／未変更 4 件。**
（内訳: YamlSectionTest 変更 1・未変更 4、buildTableDataList_blankValueRow* 変更 5、buildListMapRows_blankValueRow* 変更 2）

### 12 件の外で波及したテスト（3 件）

是正後のフル実行で落ちたため同じ方針（空行を表す `null` → `""`）で直した。アサーションは未変更。

| テスト | 内容 |
|---|---|
| `buildTableDataList_partiallyBlankValueRowKept` | `partiallyBlankValueRow` の 2 行目（空行）の `PK_COL2: null` / `NUMBER_COL: null` → `""`。1 行目（保持される行）の `NUMBER_COL: null` は「null が null のまま保持される」担保のため残した |
| `buildListMapRows_partiallyBlankValueRowKept` | `partiallyBlankValueRowListMap` の 2 行目（空行）の `KEY2: null` → `""`。1 行目の `KEY3: null` は残した |
| `buildListMapRows_allBlankRowsReturnsEmptyList` | `allBlankRowsListMap` の中間行 `KEY2: null` → `""`。フィクスチャコメントも「空マッピング／全ての値が空文字」に更新 |

## step E 変異確認

### 変異1: 是正（実装）を入れる前の状態

- コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest='YamlSectionTest,YamlTableDataBuilderTest' -DfailIfNoTests=false`
- 崩した内容: `YamlSection#isBlankRow` が未是正（Java null を空扱い）の状態で新規 3 テストを実行
- 結果: `Tests run: 76, Failures: 3, Errors: 0, Skipped: 0` — `dropBlankRows_keepsRowHavingOnlyNullValues`（Expected is <1> but was <0>）、`buildTableDataList_nullValueOnlyRowKept`（is <2> but was <1>）、`buildListMapRows_nullValueOnlyRowKept`（is <2> but was <1>）が失敗

### 変異2: 是正後・既存フィクスチャが未修正の状態（フィクスチャ側の門番確認）

- コマンド: 同上
- 崩した内容: 是正を入れた直後、既存フィクスチャの空行がまだ Java null を含む状態（＝空行の `""` を `null` に戻した状態と同値）
- 結果: `Tests run: 76, Failures: 11, Errors: 0, Skipped: 0` — 上記「変更 8 件のうちアサーションを持つ 8 件」＋波及 3 件が失敗。内訳: `dropBlankRows_removesEmptyMappingAndAllBlankValueRows`、`buildTableDataList_blankValueRowLeadingExcluded` / `..._blankValueRowMiddleExcluded` / `..._blankValueRowLeadingInExpectedTableExcluded` / `..._blankValueRowMiddleInExpectedTableExcluded` / `..._blankValueRowInExpectedCompleteTableExcluded` / `..._partiallyBlankValueRowKept`、`buildListMapRows_blankValueRowLeadingExcluded` / `..._blankValueRowMiddleExcluded` / `..._partiallyBlankValueRowKept` / `..._allBlankRowsReturnsEmptyList`
- 補足: 空行を表す値に Java null が混ざっているとテストが落ちる（＝フィクスチャの `""` が効いている）ことの確認になっている

### 変異3: 追加/変更した各テストの期待件数をわざと崩す

- コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest='YamlSectionTest,YamlTableDataBuilderTest' -DfailIfNoTests=false`
- 崩した内容（4 件同時、実行後に元へ戻した）:
  - `dropBlankRows_keepsRowHavingOnlyNullValues`: 期待 `result.size()` を `is(1)` → `is(0)`
  - `dropBlankRows_removesEmptyMappingAndAllBlankValueRows`（変更した既存テスト）: 期待 `result.size()` を `is(1)` → `is(2)`
  - `buildTableDataList_nullValueOnlyRowKept`: 期待 `result.get(0).size()` を `is(2)` → `is(1)`
  - `buildListMapRows_nullValueOnlyRowKept`: 期待 `result.size()` を `is(2)` → `is(1)`
- 結果: `Tests run: 76, Failures: 4, Errors: 0, Skipped: 0` — 崩した 4 件がすべて失敗（`YamlSectionTest.dropBlankRows_keepsRowHavingOnlyNullValues:564`、`YamlSectionTest.dropBlankRows_removesEmptyMappingAndAllBlankValueRows:445`、`YamlTableDataBuilderTest.buildListMapRows_nullValueOnlyRowKept:1483`、`YamlTableDataBuilderTest.buildTableDataList_nullValueOnlyRowKept:1425`）

### 変異4: 追加テストの「null が null のまま保持される」期待を崩す

- コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest='YamlSectionTest,YamlTableDataBuilderTest' -DfailIfNoTests=false`
- 崩した内容（3 件同時、実行後に元へ戻した）:
  - `dropBlankRows_keepsRowHavingOnlyNullValues`: 残った行の期待を `rowOf("COL_A", null, "COL_B", null)` → `rowOf("COL_A", "", "COL_B", "")`
  - `buildTableDataList_nullValueOnlyRowKept`: `assertNull(... getValue(1, "PK_COL2"))` → `assertNotNull(...)`
  - `buildListMapRows_nullValueOnlyRowKept`: `assertNull(... get("KEY2"))` → `assertNotNull(...)`
- 結果: `Tests run: 76, Failures: 3, Errors: 0, Skipped: 0` — 崩した 3 件がすべて失敗

### 最終確認

- コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
- 結果: `Tests run: 229, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`

## 追補（コーディネーター指摘への対応）

是正後の挙動と食い違う説明コメント（実装コードは未変更）を追加コミットで直した。

- `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:37`-`:39`（クラス javadoc）／`:89`-`:92`（`buildTableDataList` 内コメント）／`:168`-`:171`（`buildListMapRows` javadoc）— 「全ての値が null／空文字の行」→「全ての値が空文字の行」に修正し、`YamlSection` と同じ表現で「Java null は空文字ではないため非空として扱い、その行は残る」を補った
- `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java:402` — 同種の食い違い（`buildFileList_allBlankFieldRecordIsKept` の javadoc）を grep で検出し「全ての値が空文字の行」に修正
- grep（`null／空文字` / `null または空文字` / `null・空文字`）の残存ヒット 3 件（`tableData.yaml:125`・`:244`、`YamlSectionTest.java:453`）は空行判定の規則ではなく Given データの内容（実際に null と空文字を含む行）を述べたものであり、是正後も正しいため未変更
- 修正後のフル実行: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 229, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`

## Overall Verdict

- Self-check: OK

## コーディネーター独立レビュー

Step 4 では4観点レビュー（QA/Design/Craft/Verification）を回さない（指示書 §7）。代わりにコーディネーターがコミット済み差分を独立に読み、ビルドを自分で実行して検証した。

| 観点 | 判定 | 根拠 |
|---|---|---|
| 差分がタスクの範囲に収まっている | OK | `git diff 8d996a4..98d7ce6` は `YamlSection.java`（判定1行＋javadoc）・`ntf-testdata-yaml-schema.json`（`:108`・`:136` の空行除去の条件の記述のみ）・`YamlTableDataBuilder.java`（コメント/javadoc のみ）・テスト4ファイル。解説書・`nablarch-testing`・`nablarch-testing-converter` への書き込みなし |
| 実装が解説書と一致している | OK | `isBlankRow` が `str == null \|\| !str.isEmpty()` で非空を返す＝空文字のみを空と見なす。解説書 `5b5c91e` の `testdata_notation.rst:1500`「空マッピング（`{}`）またはすべての値が空文字の場合にスキップされる」と一致 |
| スキーマ description の変更が最小 | OK | `git diff` 上の変更は `:108`・`:136` の各1行のみ。同一 description 内の FK 制約・カラム省略の記述は未変更（`:410` は #30 の担当） |
| 是正前に落ち是正後に通るテストがある | OK | 新規3件（`dropBlankRows_keepsRowHavingOnlyNullValues` / `buildTableDataList_nullValueOnlyRowKept` / `buildListMapRows_nullValueOnlyRowKept`）が是正前 `Failures: 3` |
| 変異確認が実施されている | OK | 変異1〜4を実施。期待件数・null 保持の期待の双方を崩して落ちることを確認済み |
| ビルド（コーディネーター自身の実行） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 229, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（2026-08-26 コーディネーターが独立実行） |

**指摘1件・対応済み**: `YamlTableDataBuilder.java:38`・`:89`・`:168` の説明コメントが是正前の文言のまま残存 → `98d7ce6` で修正（`YamlFileBuilderTest.java:402` も同種として併せて修正）。

## Overall Verdict（コーディネーター）

- コーディネーター独立レビュー: OK
- Ready to check off: Yes
