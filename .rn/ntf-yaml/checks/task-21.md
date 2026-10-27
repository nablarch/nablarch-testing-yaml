# task-21 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| 全値が null／空文字の行が、先頭・中間のどちらに置かれても、setup_tables / expected_tables / list_maps の3経路すべてで行として存在しなくなる | OK | 6テストが GREEN（`YamlTableDataBuilderTest#buildTableDataList_blankValueRowLeadingExcluded` / `_blankValueRowMiddleExcluded` / `_blankValueRowLeadingInExpectedTableExcluded` / `_blankValueRowMiddleInExpectedTableExcluded` / `buildListMapRows_blankValueRowLeadingExcluded` / `_blankValueRowMiddleExcluded`）。先頭ケースは「空行のキー集合を後続行と変えた」フィクスチャで、列名解決からも除外されること（列名が後続行のキーで決まること）を検証している。レビュー修正で `expected_complete_tables` 経路（fillDefaults=true）の `buildTableDataList_blankValueRowInExpectedCompleteTableExcluded` を追加。2巡目レビュー V9 で、この経路も完了条件どおり「先頭・中間」を問う形へフィクスチャを直した（`completedTable.yaml` の `blankValueRowComplete` を「空行→通常行→空行→通常行」に変更し、2 つ目の空行を非空行に挟まれた中間位置へ）。javadoc・フィクスチャのコメント・assert メッセージ・期待件数（残り 2 行）を実データに揃えてある。3巡目レビュー V18 で、`expected_complete_tables` の `blankValueRowComplete` 末尾に空行を 1 件足して「空行→通常行→空行→通常行→空行」とし、先頭・中間・末尾の 3 位置すべてを担保した（javadoc・フィクスチャのコメント・assert メッセージ・期待件数 2 行を実データに揃えてある）。変異「最終行は常に残す」（M-lastrow）で落ちることを実測。`mvn -o clean test` で `Tests run: 207, Failures: 0, Errors: 0, Skipped: 0` | OK | 6テストの実在を行番号つきで確認（`:1109`/`:1136` setup、`:1189`/`:1216` expected、`:1265`/`:1290` list_maps）。先頭ケースはフィクスチャの空行キー集合が後続行と異なる（`blankValueRowLeading` は2キー・後続は5キー）ため、列名アサーションが列名解決からの除外まで検査していると確認 |
| 空マッピング（{}）の行が、先頭・中間のどちらに置かれても、setup_tables / expected_tables / list_maps の3経路すべてで行として存在しなくなる | OK | setup_tables 中間=`buildTableDataList_emptyRowEntrySkipped`、setup_tables 先頭=`buildTableDataList_leadingEmptyRowKeepsFollowingRows`、expected_tables 中間=新規 `buildTableDataList_emptyRowEntryInExpectedTableSkipped`、expected_tables 先頭=`buildTableDataList_leadingEmptyRowInExpectedTableKeepsFollowingRows`、list_maps 中間=期待値変更した `buildListMapRows_emptyRowEntrySkipped`（2件・いずれも通常行）、list_maps 先頭=期待値変更した `buildListMapRows_leadingEmptyRowKeepsFollowingRows`（2件・空 Map なし）。すべて GREEN。2巡目レビュー V15 で、テーブル側 `buildTableDataList_allEmptyRowsReturnsTableDataWithNoColumns` と対になる list_maps 側の「全行が行として存在しない → 空リスト」を `buildListMapRows_allBlankRowsReturnsEmptyList`（新規グループ `allBlankRowsListMap`）で固定 | OK | 6組すべての実在を確認。`list_maps` の「空 Map として残る」旧挙動が消えていることを期待値変更の差分で確認 |
| 値が1つでも非空の行は従来どおり保持される | OK | `buildTableDataList_partiallyBlankValueRowKept`（空文字カラムは空文字・null カラムは null で保持）、`buildListMapRows_partiallyBlankValueRowKept`、`YamlSectionTest#dropBlankRows_keepsRowHavingAnyNonBlankValue`、`#dropBlankRows_keepsRowHavingOnlyMarkerColumnValue` が GREEN。変異 M5（1つでも空なら空行扱い）でこれらが落ちることを確認済み。レビュー修正で空白文字（半角スペース1個）の扱いを `YamlSectionTest#dropBlankRows_keepsRowHavingOnlyWhitespaceValue` で追加固定し、変異 M-trim で落ちることを確認済み。値加工（interpret）後に全て null になる行が保持されることは `buildTableDataList_rowInterpretedToAllNullIsKept` / `buildListMapRows_rowInterpretedToAllNullIsKept` で固定し、変異 M-order で落ちることを確認済み。3巡目レビュー V17 で、マーカーカラムだけが値を持つ行の挙動（行としては残り、`list_maps` の結果は空 Map になる）を `buildListMapRows_markerOnlyRowKeptAsEmptyMap`（新規グループ `markerOnlyRowListMap`）で固定し、変異 M-marker で落ちることを実測 | OK | `_partiallyBlankValueRowKept` ×2 が `""` は空文字・`null` は null のまま保持することを検査していると確認。`dropBlankRows_keepsRowHavingAnyNonBlankValue` / `_keepsRowHavingOnlyWhitespaceValue`（trim しない）/ `_keepsRowHavingOnlyMarkerColumnValue` も確認 |
| ファイルデータ（YamlFileBuilder）の挙動が変わっていない（全フィールド "" のレコードは保持されたまま） | OK | `YamlFileBuilder.java` は未変更（`git status` に現れない）。`grep -c dropBlankRows YamlFileBuilder.java` = 0。レビュー修正で回帰ガード `YamlFileBuilderTest#buildFileList_allBlankFieldRecordIsKept` を追加（`expected_files` の `allBlankFieldsRecord` グループ、`rows: [["", "", ""]]` が 1 件のレコードとして保持されること）。3巡目レビュー V16 で、件数だけでなく各フィールドの値まで assert するよう強化した（実測して `FIELD1`／`FIELD2`／`FIELD3` がいずれも `java.lang.String` の空文字であることを確認したうえで期待値を書いた。3 フィールドすべてを明示的に空文字で書いたフィクスチャなので、値が `null` に化ける退行を検出できる）。変異 M-file / M-file-null で落ちることを確認済み。`YamlFileBuilderTest` は `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0` | OK（round 2 時点は「弱い」） | `git diff bd0dde2..14ad84a` に `YamlFileBuilder.java` が現れないことと、ガードテストの追加を確認。round 2 では「検査が件数のみ」「解説書が正典とする `- []` 記法を外している」を Finding として指摘（F3・F4）。**F4 は3巡目 V16 で解消**（各フィールドの値まで assert）。**F3 は `#22` step C が引き取る** |
| 既存フィクスチャのグループが変更されていない | OK | `git diff --numstat src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/tableData.yaml` = `129 0`（追加のみ・削除0）。`emptyRows` / `allEmptyRows` / `emptyRowMixed` / `leadingEmptyRow` / `emptyRowListMap` / `leadingEmptyRowListMap` はいずれも無変更。レビュー修正での追加分も削除 0（`fb58781` 以降の `git diff --numstat`: `tableData.yaml` = `22 0`、`completedTable.yaml` = `15 0`、`fileData.yaml` = `20 0`）。2巡目レビュー修正後も `git diff --numstat bd0dde2 -- .../YamlTableDataBuilderTest/` = `tableData.yaml 160 0` / `completedTable.yaml 18 0` で削除 0。V9 で行を足した `blankValueRowComplete` と V15 で追加した `allBlankRowsListMap` はいずれも `14ad84a` 以降に新設したグループ／今回の新規グループであり、既存グループは無変更。3巡目レビュー修正後の `git diff a5cb6dd -- .../tableData.yaml` の削除は空行 2 行のみ（V23 の空行慣行そろえ）で、既存グループの中身は無変更。`completedTable.yaml` の変更は V18 対象の `blankValueRowComplete` とその直上コメントのみ、`fileData.yaml` の変更は空行 1 行の削除のみ | OK | 3フィクスチャの差分ハンクがすべて追加行のみ（削除行ゼロ）であることを確認。新規グループは全て `group_id`/`id` 付きで `groupId=""` の既存クエリに混入しないことも確認 |
| 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている | OK | 下記「Mutation checks」に M1〜M5 と、レビュー修正で追加した M-order / M-file / M-trim の変更内容・コマンド・落ちたテスト名と失敗行を記録。追加/期待値変更した全 20 テストがいずれかの変異で落ちることを確認済み。2巡目レビュー修正分は「2巡目レビュー修正で実施した変異（M-listmaps / M1 再々実施）」に、3巡目レビュー修正分は「3巡目レビュー修正で実施した変異（M-file-null / M-marker / M-lastrow）」に記録 | OK（未コミットの注記つき） | M1〜M5・M-order・M-file・M-trim のコマンド／`Tests run:`／落ちたテスト名＋行番号があり、追加・変更した全テストが変異表で1つ以上に紐づくことを確認。本ファイルが untracked である点を F6 として指摘したが、**check ファイルはチェックオフのコミットで coordinator が入れる手順どおり**（`task-execute-workflow.md` の Check file format） |
| pom.xml / argLine が変更されておらず、他リポジトリへの書き込みが無い | OK | 3巡目レビュー修正後の `git status --short` の変更は src 配下の 7 ファイル（`YamlSection.java`・`YamlFileBuilderTest.java`・`fileData.yaml`・`YamlSectionTest.java`・`YamlTableDataBuilderTest.java`・`completedTable.yaml`・`tableData.yaml`）と未追跡の本ファイルのみ。`YamlSection.java` の差分は javadoc の削除のみ（`git diff` で確認）。`nablarch-testing` へは `sed -n` による参照読み取りのみ（`ListMapParser.java` / `HeaderLine.java`）で書き込み無し。2巡目レビュー修正後の記録は次のとおり: `git status --short` の変更は src 配下の 5 ファイル（`YamlSection.java`・`YamlSectionTest.java`・`YamlTableDataBuilderTest.java`・`completedTable.yaml`・`tableData.yaml`）と未追跡の本ファイルのみ。`YamlSection.java` の差分は javadoc のみ（`git diff` で確認）。fb58781 時点の記録は下記のとおり: `git status --short` の変更は src 配下の7ファイルのみ（`pom.xml`・`argLine`・`ntf-testdata-yaml-schema.json` はいずれも含まれない）。`/home/tie303177/work/nablarch/nablarch-testing` へは `grep` / `sed -n` による参照読み取りのみで書き込み無し。`nablarch-document` は本タスクでは一切参照・書き込みしていない | OK | `git diff --name-only bd0dde2..14ad84a` に `pom.xml` 無し。変更ファイルはすべて当リポジトリ配下であることを確認 |
| `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `[INFO] Tests run: 207, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`（3巡目レビュー修正後の再実行。内訳 `YamlTableDataBuilderTest` 51 / `YamlSectionTest` 22 / `YamlFileBuilderTest` 26 / `YamlMessageBuilderTest` 38 / `YamlTestDataParserTest` 49 / `YamlLoaderTest` 21） | OK | coordinator が単独実行した結果を採用（QA 自身は mvn 未実行） |

## Method (TDD)

- RED: フィクスチャ（新規グループのみ追加）とテスト9件追加＋既存2件の期待値変更を先に入れ、
  `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlTableDataBuilderTest -DfailIfNoTests=false`
  → `Tests run: 46, Failures: 10, Errors: 0, Skipped: 0`（例: `buildTableDataList_blankValueRowLeadingExcluded:1117 Expected: is <[PK_COL1, PK_COL2, VARCHAR2_COL, NUMBER_COL, NUMBER_COL2]> but: was <[PK_COL1, VARCHAR2_COL]>`）。
  新規10件目の `buildTableDataList_emptyRowEntryInExpectedTableSkipped` のみ #17 の修正で既に GREEN。
- GREEN: `YamlSection#dropBlankRows` を追加し、`resolveColumns`/`extractRows` より前で適用。
  `Tests run: 68, Failures: 0`（YamlTableDataBuilderTest + YamlSectionTest）→ 全体 `Tests run: 200, Failures: 0`。

## Mutation checks

いずれも変更後に
`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest='YamlTableDataBuilderTest,YamlSectionTest' -DfailIfNoTests=false`
を実行し、確認後に元へ戻した。

- M1: `isBlankRow` を `return castMap(row).isEmpty();` に置換（全値空の行を落とさない）
  → Failures 9。`dropBlankRows_removesEmptyMappingAndAllBlankValueRows:438` /
  `buildTableDataList_blankValueRowLeadingExcluded:1117` / `_blankValueRowMiddleExcluded:1144` /
  `_partiallyBlankValueRowKept:1170` / `_blankValueRowLeadingInExpectedTableExcluded:1197` /
  `_blankValueRowMiddleInExpectedTableExcluded:1224` / `buildListMapRows_blankValueRowLeadingExcluded:1272` /
  `_blankValueRowMiddleExcluded:1297` / `_partiallyBlankValueRowKept:1320`
- M2: `isBlankRow` 冒頭に `if (castMap(row).isEmpty()) { return false; }` を挿入（空マッピング・非マッピング行を落とさない）
  → Failures 11。うち本タスクで追加/変更したもの: `buildTableDataList_emptyRowEntryInExpectedTableSkipped:1248` /
  `buildListMapRows_emptyRowEntrySkipped:773` / `buildListMapRows_leadingEmptyRowKeepsFollowingRows:1081` /
  `dropBlankRows_removesNonMappingRows:504` / `dropBlankRows_removesEmptyMappingAndAllBlankValueRows:438`
- M3: `isBlankRow` でマーカーカラム（`[COL]`）を判定対象から除外
  → Failures 1。`dropBlankRows_keepsRowHavingOnlyMarkerColumnValue:483`
- M4: `if (str != null && !str.isEmpty())` → `if (str != null && str.isEmpty())`（非空判定の反転）
  → Failures 33 + Errors 5。`dropBlankRows_removesEmptyMappingAndAllBlankValueRows:438` /
  `dropBlankRows_removesNonMappingRows:504` / `buildListMapRows_blankValueRowLeadingExcluded:1273` ほか多数
- M5: 「値が1つでも空なら空行扱い」に反転（`if (str == null || str.isEmpty()) return true;` / 末尾 `return false;`）
  → Failures 18。うち: `dropBlankRows_keepsRowHavingAnyNonBlankValue:460` /
  `dropBlankRows_keepsRowHavingOnlyMarkerColumnValue:483` / `dropBlankRows_removesEmptyMappingAndAllBlankValueRows:439` /
  `dropBlankRows_removesNonMappingRows:504` / `buildTableDataList_partiallyBlankValueRowKept:1170` /
  `buildListMapRows_partiallyBlankValueRowKept:1320`

### レビュー修正で追加した変異（M-order / M-file / M-trim）

いずれも「修正前は全テスト GREEN のまま生き残っていた」と指摘された変異である。
変異を入れて
`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
を実行し、確認後に元へ戻して `git status --short` で復元を確認した。

- M-order: 空行判定を `interpret` の後ろへずらす。`YamlTableDataBuilder#buildTableData` の
  `td.addRow(values)` と private `buildListMapRows` の `result.add(row)` を、値加工後の値が
  全て null／空文字なら追加しない形に変更（`mutantAllBlank` ヘルパーを追加）
  → `Tests run: 205, Failures: 2, Errors: 0, Skipped: 0` / `BUILD FAILURE`。
  `YamlTableDataBuilderTest.buildTableDataList_rowInterpretedToAllNullIsKept:1352 値加工後に全て null になる行も行として保持され、2 行返ること` /
  `YamlTableDataBuilderTest.buildListMapRows_rowInterpretedToAllNullIsKept:1383 値加工後に全て null になる行も行として保持され、2 件返ること`。
  復元: `git checkout -- src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java` →
  `grep -c mutantAllBlank YamlTableDataBuilder.java` = 0
- M-file: `YamlFileBuilder#buildFragmentsInternal` の値行に「全要素が null／空文字ならスキップ」を注入
  → `Tests run: 205, Failures: 1, Errors: 0, Skipped: 0` / `BUILD FAILURE`。
  `YamlFileBuilderTest.buildFileList_allBlankFieldRecordIsKept:424 全フィールドが "" のレコードも 1 件として保持されること`。
  復元: `git checkout -- src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java` →
  `grep -c mutantAllBlank YamlFileBuilder.java` = 0
- M-trim: `YamlSection#isBlankRow` の `!str.isEmpty()` を `!str.trim().isEmpty()` に変更（値を trim してから空判定）
  → `Tests run: 205, Failures: 1, Errors: 0, Skipped: 0` / `BUILD FAILURE`。
  `YamlSectionTest.dropBlankRows_keepsRowHavingOnlyWhitespaceValue:491 空白文字だけの値は trim されずに非空として扱われ、行が残ること`。
  復元: 同行を `!str.isEmpty()` へ戻し、`git diff YamlSection.java` の変更が javadoc 行のみであることを確認
- M1 再実施（V5 で追加した `expected_complete_tables` のテストを落とすことの確認）:
  `YamlSection#isBlankRow` を `return castMap(row).isEmpty();` に置換（全値空の行を落とさない）
  → `Tests run: 205, Failures: 11, Errors: 0, Skipped: 0` / `BUILD FAILURE`。うち新規分は
  `YamlTableDataBuilderTest.buildTableDataList_blankValueRowInExpectedCompleteTableExcluded:1414 先頭・末尾の全値空行が除外されて 1 行のみ残ること` /
  `YamlSectionTest.dropBlankRows_keepsRowHavingOnlyWhitespaceValue:491`。
  復元: 同メソッドを元の実装へ戻し、`git diff YamlSection.java` の変更が javadoc 行のみであることを確認

### 2巡目レビュー修正で実施した変異（M-listmaps / M1 再々実施）

いずれも変異を入れて `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` を実行し、
確認後に `git checkout -- <path>` で戻して `git status --short` で復元を確認した。

- M-listmaps（V15 で追加したテストの門番確認）: `YamlTableDataBuilder#buildListMapRows` の
  `dropBlankRows(getList(map, FIELD_ROWS))` を `getList(map, FIELD_ROWS)` に退行させ、list_maps 経路で
  行の除去を行わないようにする
  → `Tests run: 206, Failures: 7, Errors: 0, Skipped: 0` / `BUILD FAILURE`。対象テストは
  `YamlTableDataBuilderTest.buildListMapRows_allBlankRowsReturnsEmptyList:1412 全行が行として存在しないものの場合は空リストが返ること`
  （ほかに `_blankValueRowLeadingExcluded:1274` / `_blankValueRowMiddleExcluded:1299` /
  `_emptyRowEntrySkipped:774` / `_leadingEmptyRowKeepsFollowingRows:1083` / `_nonMapRowSkipped:808` /
  `_partiallyBlankValueRowKept:1322` も落ちた）。
  復元: `git checkout -- src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java` →
  `git status --short` に同ファイルが現れないことと `grep -n "dropBlankRows(getList(map, FIELD_ROWS))"` が 2 箇所返ることを確認
- M1 再々実施（V9 でフィクスチャを変えたテストが引き続き門番として機能することの確認）:
  `YamlSection#isBlankRow` を `return castMap(row).isEmpty();` に置換
  → `Tests run: 206, Failures: 12, Errors: 0, Skipped: 0` / `BUILD FAILURE`。対象テストは
  `YamlTableDataBuilderTest.buildTableDataList_blankValueRowInExpectedCompleteTableExcluded:1439 先頭・中間の全値空行が除外されて通常行 2 行のみ残ること`。
  復元: `git checkout -- src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` 後に
  javadoc 修正（V11〜V13）を再適用し、`git diff src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` の
  変更が javadoc 行のみであることを確認

### 3巡目レビュー修正で実施した変異（M-file-null / M-marker / M-lastrow）

いずれも変異を入れて実行し、確認後に退避しておいたコピーで元へ戻して
`git diff <path>` に意図した変更だけが残っていることを確認した
（`YamlSection.java` には V21 の javadoc 修正が乗っているため `git checkout --` は使わず、
変異前のファイルを scratchpad へ退避してから戻している）。

- M-file-null（V16 の強化テストの門番確認）: `YamlFileBuilder#buildFragmentsInternal` の値行で、
  値が空文字のとき `null` を積むように変更（`rowValues.add(mutantValue != null && mutantValue.isEmpty() ? null : mutantValue)`）
  → `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest='YamlFileBuilderTest' -DfailIfNoTests=false`
  で `Tests run: 26, Failures: 1, Errors: 0, Skipped: 0` / `BUILD FAILURE`。
  `YamlFileBuilderTest.buildFileList_allBlankFieldRecordIsKept:425 FIELD1 が空文字のまま保持されること`。
  復元: 退避コピーを書き戻し、`git diff -- .../YamlFileBuilder.java` が空（HEAD と同一）であることと
  `grep -c mutantValue YamlFileBuilder.java` = 0 を確認
- M-marker（V17 で追加したテストの門番確認。M3 と同種だが対象テストが増えたため再実施）:
  `YamlSection#isBlankRow` をキーでループする形に変え、`isMarker(key)` の列を判定対象から除外
  → `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` で
  `Tests run: 207, Failures: 2, Errors: 0, Skipped: 0` / `BUILD FAILURE`。
  `YamlSectionTest.dropBlankRows_keepsRowHavingOnlyMarkerColumnValue:519 マーカーカラムの値も空行判定の対象になるため残ること` /
  `YamlTableDataBuilderTest.buildListMapRows_markerOnlyRowKeptAsEmptyMap:1478 マーカーカラムだけが値を持つ行も行としては残ること`。
  復元: 退避コピーを書き戻し、`git diff -- .../YamlSection.java` の変更が javadoc の削除のみであることを確認
- M-lastrow（V18 で末尾へ足した空行の門番確認）: `YamlSection#dropBlankRows` を
  「最終行は空行判定にかかわらず常に残す」形へ変更（`if (i == rows.size() - 1 || !isBlankRow(row))`）
  → `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` で
  `Tests run: 207, Failures: 6, Errors: 0, Skipped: 0` / `BUILD FAILURE`。V18 の対象テストは
  `YamlTableDataBuilderTest.buildTableDataList_blankValueRowInExpectedCompleteTableExcluded:1281 先頭・中間・末尾の全値空行が除外されて通常行 2 行のみ残ること`
  （ほかに `YamlSectionTest.dropBlankRows_keepsRowHavingOnlyWhitespaceValue:494` /
  `YamlTableDataBuilderTest.buildListMapRows_allBlankRowsReturnsEmptyList:1451` /
  `_partiallyBlankValueRowKept:1361` / `buildTableDataList_allEmptyRowsReturnsTableDataWithNoColumns:438` /
  `buildTableDataList_partiallyBlankValueRowKept:1173` も落ちた）。
  復元: 退避コピーを書き戻し、`git diff -- .../YamlSection.java` の変更が javadoc の削除のみであることを確認

追加/期待値変更した全テストと、それを落とす変異の対応:

| テスト | 落とす変異 |
|---|---|
| buildTableDataList_blankValueRowLeadingExcluded | M1 |
| buildTableDataList_blankValueRowMiddleExcluded | M1 |
| buildTableDataList_partiallyBlankValueRowKept | M1, M5 |
| buildTableDataList_blankValueRowLeadingInExpectedTableExcluded | M1 |
| buildTableDataList_blankValueRowMiddleInExpectedTableExcluded | M1 |
| buildTableDataList_emptyRowEntryInExpectedTableSkipped | M2 |
| buildListMapRows_blankValueRowLeadingExcluded | M1 |
| buildListMapRows_blankValueRowMiddleExcluded | M1 |
| buildListMapRows_partiallyBlankValueRowKept | M1, M5 |
| buildListMapRows_emptyRowEntrySkipped（期待値変更） | M2 |
| buildListMapRows_leadingEmptyRowKeepsFollowingRows（期待値変更） | M2 |
| dropBlankRows_removesEmptyMappingAndAllBlankValueRows | M1, M2, M5 |
| dropBlankRows_keepsRowHavingAnyNonBlankValue | M5 |
| dropBlankRows_keepsRowHavingOnlyMarkerColumnValue | M3, M5 |
| dropBlankRows_removesNonMappingRows | M2, M5 |
| dropBlankRows_keepsRowHavingOnlyWhitespaceValue（レビュー修正で追加） | M-trim |
| buildTableDataList_rowInterpretedToAllNullIsKept（レビュー修正で追加） | M-order |
| buildListMapRows_rowInterpretedToAllNullIsKept（レビュー修正で追加） | M-order |
| buildFileList_allBlankFieldRecordIsKept（レビュー修正で追加） | M-file |
| buildTableDataList_blankValueRowInExpectedCompleteTableExcluded（レビュー修正で追加・V9 でフィクスチャ変更） | M1（再実施・再々実施して実測） |
| buildListMapRows_allBlankRowsReturnsEmptyList（2巡目レビュー V15 で追加） | M-listmaps |
| buildFileList_allBlankFieldRecordIsKept（3巡目レビュー V16 で値の assert を追加） | M-file-null |
| buildListMapRows_markerOnlyRowKeptAsEmptyMap（3巡目レビュー V17 で追加） | M-marker |
| buildTableDataList_blankValueRowInExpectedCompleteTableExcluded（3巡目レビュー V18 で末尾ケースを追加） | M-lastrow |

## 設計判断のメモ（V21 で public javadoc から移した内容）

`YamlSection#resolveColumns` の javadoc に書いていた次の記述を、3巡目レビュー V21 の指摘を受けて
ここへ移した。他クラスの private メソッドの実装状況に依存する記述であり、どちらか一方が動くと
腐るため public API の javadoc には置かない。javadoc には
「`dropBlankRows` を通していない入力を渡されても列名が 0 件に倒れないよう、この判定を残す」
という趣旨の 1 文だけを残してある。

> 同種の判定を残すか否かの線引きは可視性で決めている。本メソッドは public であり
> `dropBlankRows(List)` を通していない行を渡されても単体で正しく振る舞う必要があるため、
> この判定を残す。一方 `YamlTableDataBuilder#extractRows` は private かつ本メソッドと同じ
> 呼び出し元からしか到達しないため、判定を持たせず `dropBlankRows` に一本化してある。

## Coverage (JaCoCo)

```
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec
```

- `YamlSection`: line 50/50 (100.0%), branch 44/44 (100.0%)
- `YamlTableDataBuilder`: line 67/67 (100.0%), branch 30/30 (100.0%)

レビュー修正では本体の実行コードを変更していない（`YamlSection.java` の変更は javadoc のみ、
`YamlTableDataBuilder.java` / `YamlFileBuilder.java` は無変更）ため、再測定していない。

## QA Expert Review

対象コミットごとに 2 回実施した（round 1 = `fb58781`、round 2 = `14ad84a`）。round 3（`a5cb6dd` / `d75c79c`）は Craft と Verification のみ再実行した。

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective (checks the right thing, not just "passed") | OK（round 2） | round 1 は **NG 相当の Finding F1** を出した — 「空行判定を `interpret` より前に行う」というこの変更の設計上の要を守るテストが 1 本も無く、判定を `interpret` 後へずらしても 200 テストが全 GREEN のまま通る。既存フィクスチャの `null` は 13 箇所すべてクォート無しで門番にならないことも走査で確認していた。round 2 では追加された `_rowInterpretedToAllNullIsKept` ×2 によりこの穴が塞がれたことを確認し **OK**。境界（マーカーのみ／空白文字／非マッピング／全行空／`expected_complete_tables`）も検査ありと確認。依存先との整合は `PoiXlsReader#isBlankLine`（行の全セル対象・trim 無し）と一致していることを確認 |

### QA の Finding と triage

| Finding | 判定 | 根拠 |
|---|---|---|
| round 1 F1: 「空行判定は `interpret` より前」を守るテストが無い | **Valid → 修正済（V2）** | Verification も独立に同じ変異（M-order）が生存することを実測。`_rowInterpretedToAllNullIsKept` ×2 で塞ぎ、M-order が 2 件だけを狙い撃ちで落とすことを確認 |
| round 1 F3 / round 2 F3: ファイルデータ不変の回帰ガードが無い | **Valid → 修正済（V3・V16）** | 完了条件4 が正面から要求している。`buildFileList_allBlankFieldRecordIsKept` を追加し、V16 で値まで assert |
| round 2 F3: ガードが `- []`（`rst:883` が正典とする記法）を使っていない | **`#22` へ帰属** | `#22` step C が `rows: [[]]` → 全フィールド `""` のレコード 1 件を固定する。#21 の完了条件は「全フィールド `""` のレコードが保持される」であり `- ["", "", ""]` で満たす |
| round 2 F1: スキーマ `:108` の「裸 `null` と `"null"` はともに Java null」が行の存在レベルで成り立たなくなった | **Escalation（ユーザー判断待ち）** | 挙動自体は本体 Excel と一致しており実装は正しい（空セル＝空行として落ちる／文字列 `null` のセル＝非空で残る）。直すべきはスキーマの description。#21 の完了条件外のため `#22` step A1 として足すことを推奨し、ユーザーへ escalate 済み |
| round 2 F2: 解説書 `rst:819`「カラム名は最初の行のキーで決まる」が実装と食い違う | **⑥ 報告書候補** | 実装は「最初の**非空**行のキー」。coordinator が `rst:819` を実物で確認。ただしこの乖離は `#17`（先頭 `{}` 行の読み飛ばし）の時点で既に生じており、`#21` はその範囲を広げただけ。別リポジトリのため本タスクでは直せない |
| round 2 F5: `expected_complete_tables` テストの javadoc が「先頭・中間」だがフィクスチャは先頭・末尾 | **Valid → 修正済（V9・V18）** | Craft F2・Verification F-1 も独立に指摘。フィクスチャ側を直して「先頭・中間・末尾」の 3 位置を担保する形にした |
| round 2 F6: `checks/task-21.md` が untracked | **Invalid（手順どおり）** | check ファイルは implementation expert が書き、coordinator がチェックオフのコミットで入れる（`task-execute-workflow.md` の Check file format）。Craft・Verification も同じ指摘をしたが同様に却下 |
| round 2 F7: `resolveColumns` の空行スキップは production から到達不能 | **Invalid（差分の欠陥ではない）** | javadoc が理由を明記済み。Verification が M-4（当該ガードを削除）で `YamlSectionTest` の 2 本が落ちることを実測しており、public API の単体担保として門番は存在する |
| round 2 F8: マーカー列のみ値を持つ行の end-to-end 検査が無い | **Valid → 修正済（V17）** | `buildListMapRows_markerOnlyRowKeptAsEmptyMap` を追加。挙動が本体（`ListMapParser#onReadLine` が `getMapExcludingMarkerColumns` の結果を無条件に積む／`HeaderLine#getMapExcludingMarkerColumns` は全列マーカーなら空 Map を返す）と一致することを coordinator が実物で確認済み |

## Expert Reviews (axes the task needs)

### Design Expert

`fb58781` に対して 1 回実施。以降 `src/main` の変更は javadoc のみのため再実行していない。

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Approach/structure fits | OK | 処理順が本体と一致することを Design 自身が一次情報で確認（`PoiXlsReader#readLine` は `isBlankLine` で捨ててから返す／`TestDataParsingTemplate#readTestData` は `isBlankLine` → `interpret` の順）。削除された防衛分岐 3 件がいずれも到達不能かつ相互に整合していることを検算（旧コードで `rawRow.size() != cols.size()` になり得たのは `extractRows` の空リスト枝だけで、下流 2 つの `isEmpty()` ガードはそのための IndexOutOfBounds 防止。3 件同時削除で不変条件が回復） |
| System-wide integrity (interfaces, cross-doc consistency) | OK（Finding D を除く） | `YamlSection` の非定数メソッドをパッケージ外から使っている箇所は無し。`YamlMessageBuilder` は `rows` を扱わず、`YamlFileBuilder` の `rows` は配列の配列で構造が別。解説書 `rst:1534` と適用範囲は一致。**Finding D（スキーマ description が追随していない）のみ NG** |

Design の Finding A/B/C（`dropBlankRows` を public にしたことによる時間的結合、`resolveColumns` に防衛判定を残した非対称性、新経路が増えたときの失敗が静かになる点）は **却下**。Design 自身が「`castMap`／`getList`／`interpret` 等も同様に不要な public であり、本変更が新たに作った問題ではなく既存パターンの踏襲」「現状のままでも実害は無く、判断としては受け入れ可能」と記述しており、可視性の整理はこの差分の範囲を超える。Finding E（`rst:1534` の YAML 節が `null` に触れていない）は ⑥ 報告書候補。Finding F（クラス javadoc 未更新）は **Valid → 修正済（V6・V13）**。

### Craft Expert (coding)

`fb58781` / `14ad84a` / `a5cb6dd` に対して 3 回実施。

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | OK | 3 巡とも「実装コードに欠陥は発見できなかった」。判定は 1 箇所に集約、Java 言語水準（ダイヤモンド不使用）・`import static` 集約・全 static で状態を持たずスレッド安全、`castMap(null)` 経由で null 要素も安全に落ちることを検算済み |
| Consistency with existing style | OK（3 巡目の V19〜V23 反映後） | round 3 でテストヘルパーの名前（`row` / `rowOf` が取り違え可能）・テストセクションの見出しと並び・フィクスチャの空行慣行の逸脱を指摘。V19・V20・V22・V23 で解消 |
| javadoc・コメントの事実性 | OK（3 巡目の V16〜V18・V21 反映後） | round 1〜3 で出典の誤りを 3 件検出しすべて修正 — `TestDataParsingTemplate#readLines`（**依存先に存在しない**。実在は `#readTestData`）、`PoiXlsReader#isBlankLine` が `StringUtil.isNullOrEmpty` で判定するという記述（**実物は `String#isEmpty` で trim しない**）、`PoiXlsReader#readLine` も値加工より前に空行判定するという記述（**`PoiXlsReader` に `interpret` は 0 件**）。いずれも coordinator が一次情報で裏を取ったうえで修正 |

Craft の Finding のうち **却下**したもの: F7（`dropBlankRows` がスカラ行も捨てるので名前が実態と合わない）— javadoc が「マッピングでない行（スカラ等）も構造を持たないためここで取り除く」と既に明記しており、public API の再改名は割に合わない。F8（`dropBlankRows` の null 非防御）・F11（`isBlankRow` に `@param`/`@return` が無い）— Craft 自身が「`resolveColumns` も同様で既存スタイルとは一貫している」「パッケージ内の慣行は割れている」と認めている。F10（`YamlTableDataBuilder` の javadoc が `TableData#deleteData()` / `#insertData()` を無引数のように書いている）— **指摘は事実**（実物は `void deleteData(AppDbConnection)` = `TableData.java:127` / `void insertData(AppDbConnection)` = `:137`。coordinator が確認）だが、`bd0dde2` 時点から存在する既存の誤りで本差分は当該行に触れていないため**範囲外**。後続で拾えるよう本ファイルに記録する。F1（スキーマ description）は QA round 2 F1 と同じもので **Escalation**。

### Verification Expert (test)

`fb58781` / `14ad84a` / `a5cb6dd` に対して 3 回実施。いずれも Verification 自身が `mvn` と変異注入を実行している。

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked (tests run / claims verified) | OK（round 2・3） | **round 1 は fail** — 完了条件4「ファイルデータの挙動が変わっていない」に対する検査が存在せず、3 変異（M-order / M-file / M-trim）がいずれも生存した（200 テスト全 GREEN のまま）。round 2 で 3 変異とも死亡を確認、round 3 では 9 変異すべてが死亡し**生存変異ゼロ・空振りテスト 0 件** |
| Coverage (edge cases) | OK | round 3 で M-4（`resolveColumns` の防衛ガード削除）・M-8（最終行を常に残す）・M-9（`expected_complete_tables` だけ `dropBlankRows` を適用しない）など経路を分離する変異を投入し、各経路が独立に担保されていることを実測。「列名解決を `dropBlankRows` の前へ移す」変異では 3 経路＋`expected_complete_tables` の 4 テストが落ち、完了条件の中核（列名解決からも除外される）が固定されていることを確認 |

Verification の Finding のうち **却下**したもの: round 3 の「`expected_tables` / `expected_complete_tables` の末尾位置が無担保」— と思われたが V18 で `expected_complete_tables` に末尾ケースを足して解消した。`dropBlankRows(null)` の契約を検証するテストが無い点は、呼び出し元が `getList` の戻り値のみを渡す設計で実害が無く、Craft F8 と同じ理由で却下。

## 後続で拾う記録

- `YamlTableDataBuilder.java:120-121` の javadoc が `TableData#deleteData()` / `#insertData()` を無引数のように書いている。実物は `void deleteData(AppDbConnection)`（`nablarch-testing` の `TableData.java:127`）/ `void insertData(AppDbConnection)`（`:137`）。`bd0dde2` 時点から存在する既存の誤りで、`#21` の差分は当該行に触れていない
- ⑥ nablarch-document 報告書候補: 解説書 `rst:819`「カラム名は、最初の行（`rows:` の先頭要素）のキーで決まる」が実装（最初の**非空**行のキー）と食い違う。乖離は `#17` の時点で発生
- ⑥ nablarch-document 報告書候補: 解説書 `rst:1534` の YAML 節が「空マッピング（`{}`）または**すべての値が空文字**の場合にスキップ」で `null` に触れていない（同じ段落の第 1 文は「全要素が null または空文字」）。実装は本体 `StringUtil.isNullOrEmpty` と一致しており実装側が正

## Overall Verdict


- Self-check: OK（レビュー指摘 V1〜V7、2巡目 V8〜V15 に続き 3巡目の V16〜V23 を反映後に再確認。
  V16 はファイルデータ回帰テストを実測してから各フィールドの値まで assert する形へ強化、
  V17 は `list_maps` でマーカーカラムだけ値を持つ行の挙動（行は残り結果は空 Map）をテストで固定、
  V18 は `blankValueRowComplete` の末尾に空行を足して先頭・中間・末尾の 3 位置を担保、
  V19 はテストヘルパー `row` を `rowWithKeys` へ改名、V20 はそのヘルパー javadoc を規約の書き方へ修正、
  V21 は public javadoc から他クラスの private メソッドに関する記述を本ファイルへ移動、
  V22 はテストセクションの見出しを両ケースを含む表現へ広げ `expected_complete_tables` のテストを
  expected 群の直後へ移動、V23 はフィクスチャの空行を慣行（グループ間 1 行）へそろえた。
  `mvn -o clean test` → `Tests run: 207, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`。
  変異 M-file-null / M-marker / M-lastrow で V16・V17・V18 のテストが門番として機能することを実測。
  2巡目までの記録: V8・V10・V11 は一次情報を開いて訂正、V9 はフィクスチャを「先頭・中間」へ直して記述と実データを一致、V12・V13 は javadoc に方針・意味規則を追記、V14 はテストヘルパーを `rowOf()` に統一、V15 は list_maps の全行空ケースのテストを追加。`mvn -o clean test` → `Tests run: 206, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`。変異 M-listmaps / M1 再々実施で新規・変更テストが門番として機能することを実測）
- QA: OK（round 2 = pass。round 1 の F1「順序を守るテストが無い」は Valid として V2 で修正済）
- Design expert: OK（round 1 = pass。Finding D はスキーマの話で `#21` の完了条件外・Escalation）
- Craft expert: OK（round 3 の指摘 V16〜V23 を反映後。round 1〜3 で検出した出典の誤り 3 件はすべて一次情報で裏を取って修正済み。未解決は F1 = スキーマ description のみで Escalation）
- Verification expert: OK（round 3 = pass。9 変異すべて死亡、生存変異ゼロ、空振りテスト 0 件）
- Ready to check off: **Yes**（完了条件 8 件すべて OK。未解決の Valid 指摘はスキーマ description の 1 件のみで、`#21` の完了条件の外側でありユーザー判断待ちとして `#22` step A1 の候補に載せてある）
