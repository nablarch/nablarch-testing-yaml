# task-17 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| 先頭 `{}` ＋後続実データ行のとき、`setup_tables` / `expected_tables` / `list_maps` の3経路すべてで後続のデータ行が保持される | OK | 3経路＋`expected_complete_tables` 経路の4テストが PASS。`buildTableDataList_leadingEmptyRowKeepsFollowingRows`（setup_tables, `YamlTableDataBuilderTest.java:959`）／`buildTableDataList_leadingEmptyRowInExpectedTableKeepsFollowingRows`（expected_tables, `:997`）／`buildTableDataList_leadingEmptyRowInExpectedCompleteTableKeepsFollowingRows`（expected_complete_tables・fillDefaults=true, `:1036`）／`buildListMapRows_leadingEmptyRowKeepsFollowingRows`（list_maps, `:1072`）。各テストは列名リストを順序込みで assert し（例: `is(Arrays.asList("PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL", "NUMBER_COL2"))`）、各データ行の全列の値（PK_COL1 / PK_COL2 / VARCHAR2_COL / NUMBER_COL / NUMBER_COL2）を assert する。`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 LANG=ja_JP.UTF-8 TZ=Asia/Tokyo "/mnt/c/Program Files/apache-maven-3.9.9/bin/mvn" -o clean test` で `Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS。 | | |
| `resolveColumns` は「先頭のキーを持つ行」を返し、「最後のキーを持つ行」や「`rows.get(0)`」では追加テストが落ちる | OK | 変異は自分専用の複製 `/tmp/claude-1000/-home-tie303177-work-nablarch-nablarch-testing-yaml/b54f3aac-63f7-4080-b2eb-b320cfb720a7/scratchpad/impl-fix17/` で実施（対象リポジトリは未変更。`git diff 93e270e -- src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` の出力行数 0）。<br>**変異A（最後のキー保持行を返す）**: `resolveColumns` のループを `last = new ArrayList<String>(rowMap.keySet());` に変えて最後まで走らせ `last` を返す。コマンド: `cd .../scratchpad/impl-fix17 && ` `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 LANG=ja_JP.UTF-8 TZ=Asia/Tokyo "/mnt/c/Program Files/apache-maven-3.9.9/bin/mvn" -o clean test`。結果 `Tests run: 187, Failures: 6, Errors: 0, Skipped: 0` / BUILD FAILURE。落ちたのは追加4テスト＋新規 `YamlSectionTest.resolveColumns_returnsKeysOfFirstKeyedRowInDeclarationOrder:291`＋既存 `YamlTestDataParserTest.trailingKeyOmittedIsNull`。代表的な失敗: `YamlTableDataBuilderTest.buildTableDataList_leadingEmptyRowKeepsFollowingRows:968 Expected: is <[PK_COL1, PK_COL2, VARCHAR2_COL, NUMBER_COL, NUMBER_COL2]> but: was <[PK_COL1, PK_COL2, VARCHAR2_COL]>`、`buildTableDataList_leadingEmptyRowInExpectedCompleteTableKeepsFollowingRows:1049 Expected: is "complete" but: was " "`。<br>**変異B（`rows.get(0)` に戻す）**: 同じ複製で `return new ArrayList<String>(castMap(rows.get(0)).keySet());` に置換。結果 `Tests run: 187, Failures: 6, Errors: 0, Skipped: 0` / BUILD FAILURE。落ちたのは追加4テスト＋`YamlSectionTest.resolveColumns_returnsKeysOfFirstKeyedRowInDeclarationOrder:291` / `resolveColumns_skipsNonMappingRows:375`。 | | |
| 非空マッピング行が1つも無い場合は従来どおり列名0件（#16 で確定した振る舞いが退行していない） | OK | `resolveColumns` はキーを持つ行が見つからなければ `new ArrayList<String>()` を返す（`YamlSection.java:152-160`、今回変更なし）。#16 の3テスト `buildTableDataList_emptyRowEntrySkipped`（`:394`）／`buildTableDataList_allEmptyRowsReturnsTableDataWithNoColumns`（`:427`）／`buildTableDataList_emptyExpectedTableReturnsTableDataWithNoColumns`（`:881`）は変更なしで PASS。同じ振る舞いを単体レベルでも固定した: `YamlSectionTest.resolveColumns_emptyRowsReturnsEmptyList`（`:305`）／`resolveColumns_allEmptyMappingRowsReturnsEmptyList`（`:323`）／`resolveColumns_allScalarRowsReturnsEmptyList`（`:344`）。`buildTableDataList_allEmptyRowsReturnsTableDataWithNoColumns` のアサーションメッセージを旧表現「先頭行が {} の場合も」から「全行が {} の場合も」に是正（`YamlTableDataBuilderTest.java:435`）。 | | |
| 変更した単位そのもの（`YamlSection#resolveColumns`）に直接の単体テストがある | OK | `YamlSectionTest` に6件追加（既存11件は未変更。`git diff 93e270e -- src/test/java/nablarch/test/core/reader/yaml/YamlSectionTest.java` の削除行は 0）。`resolveColumns_returnsKeysOfFirstKeyedRowInDeclarationOrder`（`:280`, 先頭 `{}` の後続で最初にキーを持つ行のキーが記述順で返ること）／`resolveColumns_emptyRowsReturnsEmptyList`（`:305`）／`resolveColumns_allEmptyMappingRowsReturnsEmptyList`（`:323`）／`resolveColumns_allScalarRowsReturnsEmptyList`（`:344`）／`resolveColumns_skipsNonMappingRows`（`:367`, マッピングでない行を読み飛ばすこと。YAML 経由ではスキーマ `rows.items: {"type":"object"}` が弾くためこの単体テストでのみ担保）／`resolveColumns_preservesColumnNameCase`（`:389`, 大文字小文字が保持されること）。`YamlSectionTest` の実行結果は `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`（11 + 6）。 | | |
| コメント／javadoc が「キーを持つ行が1つも無い場合」を網羅と読ませない表現になっている | OK | `YamlTableDataBuilder.java:104-105`（javadoc）と `:139-141`（インラインコメント）の「どの行もキーを持たない場合、すなわち rows が空（`rows: []`）か全行が空マッピング（`{}`）の場合」という同値列挙をやめ、`YamlSection.java:138` と同じ「キーを持つ行が 1 つも無い場合」に揃え、具体例は「例えば rows が空（`rows: []`）のとき」に留めた。根拠: `resolveColumns` はマッピングでない行（スカラ等）も読み飛ばすため、全行がスカラでも列名 0 件になる（`YamlSectionTest.resolveColumns_allScalarRowsReturnsEmptyList:344` が実測で担保）。`git diff 93e270e -- src/main/` はこの2箇所のコメントのみ（ロジック変更なし）。 | | |
| 既存のテスト・フィクスチャグループ・期待値・メソッド名が変更されていない | OK | `git diff 93e270e -- .../tableData.yaml` の変更は #17 で追加した3グループ（`leadingEmptyRow` / `leadingEmptyRowListMap` / `leadingEmptyRowExpected`）のみで、`emptyRows` / `allEmptyRows` / `emptyRowMixed` / `emptyRowListMap` / `markerColInTable` / `newGroup_emptyExpected` 等の既存グループは無変更。`completedTable.yaml` は新規グループ `leadingEmptyRowComplete` の追加のみ（既存2エントリは無変更）。`YamlTableDataBuilderTest.java` の既存メソッドへの変更はアサーションメッセージ1行（`:435`）のみ。`git diff 93e270e -- src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` は出力行数 0。 | | |
| `pom.xml` / `argLine` が変更されておらず、他リポジトリへの書き込みが無い | OK | `git diff 93e270e -- pom.xml` の出力行数 0。`git diff --stat 93e270e` の対象は `YamlTableDataBuilder.java` / `YamlSectionTest.java` / `YamlTableDataBuilderTest.java` / `completedTable.yaml` / `tableData.yaml` の5ファイルのみ。変異実験は自分専用の複製 `.../scratchpad/impl-fix17/` で行い、他リポジトリは参照のみ（nablarch-testing の sources jar 展開先もスクラッチパッド配下）。 | | |
| `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 LANG=ja_JP.UTF-8 TZ=Asia/Tokyo "/mnt/c/Program Files/apache-maven-3.9.9/bin/mvn" -o clean test` → `Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS（前回 180 + 追加7）。クラス別: YamlTestDataParserTest 49 / YamlLoaderTest 21 / YamlSectionTest 17 / YamlFileBuilderTest 25 / YamlMessageBuilderTest 38 / YamlTableDataBuilderTest 37、いずれも Failures 0・Errors 0・Skipped 0。 | | |

## 補足: `resolveColumns` の呼び出し元の洗い出し

`grep -rn 'resolveColumns' src/ --include=*.java` の結果、定義（`YamlSection.java:145`、変更後は `:152`）と static import（`YamlTableDataBuilder.java:25`）を除く呼び出しは次の2箇所のみ。この洗い出しは実装時点のもの。**修正ラウンド（`2f257ef`）で `YamlSectionTest` に `resolveColumns` の単体テスト6件を追加したため、現在はテストコードからの直接呼び出しがある**（coordinator 追記）。

- `YamlTableDataBuilder.java:88`（`buildTableDataList`。`setup_tables` / `expected_tables` / `expected_complete_tables` の3セクション共通）— 直後に `extractRows(rows, columnNames)` を呼ぶ。
- `YamlTableDataBuilder.java:179`（`buildListMapRows`）— 同じく直後に `extractRows(rows, columnNames)` を呼ぶ。

両方とも `getList(map, FIELD_ROWS)` で得た同一の `rows` を `resolveColumns` と `extractRows` に渡すため、列名の由来行が変わっても両者の齟齬は生じない。`extractRows` は空マッピング行を空リストとして保持し続けるので、`buildTableData` の `if (rawRow.isEmpty()) { continue; }` と `buildListMapRows` の空 Map 保持はいずれも従来どおり動く。

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective | OK | 被害の実体（データ行の消失）を行数だけでなく値レベルで検証。QA が旧新を同一複製上で切り替えて全入力パターンを実測比較し、振る舞いが変わるのは「先頭から連続する空マッピング／非 Map 行の後に非空 Map 行がある」場合のみと確定。 |
| 退行の網羅確認 | OK | `emptyRowMixed` / `emptyRowListMap` / マーカーのみ先頭（`{}` なし）/ 全行 `{}` / `rows: []` はいずれも旧新で完全一致（実測表）。スコープ外の「2行目以降が1行目に無いキー」も悪化なし（`{}` 無しの場合と挙動が揃っただけ）。 |
| `extractRows` との組み合わせ | OK | 修正後は列名0件が「非空 Map 行が皆無」のときだけになるため、実データ行が空 rawRow になる経路が消えた。`rawRow.isEmpty()` スキップは本物の `{}` 行にしか発火しない。 |

## Expert Reviews (axes the task needs)

### Design Expert

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Approach/structure fits | OK | 「先頭の非空マッピング行のキーを使う」は Excel 経路と整合。本体は `TestDataParsingTemplate.java:180` の `isBlankLine → continue` で空行をブロック解析前に全除去するため、空行がヘッダ行になることが構造上あり得ない。代替案（全行キーの和集合／`{}` 先頭をエラー化）は解説書の規定に反するため却下が妥当と判定。 |
| System-wide integrity | OK | 呼び出しは `YamlTableDataBuilder.java:88`（table 系3セクション共通）と `:180`（list_maps）の2箇所のみ。両者とも同一 `rows` を `resolveColumns` と `extractRows` の双方に渡すため齟齬なし。#16 が正当化した「どの行もキーを持たない」条件が、変更後に実際に網羅的になった。 |
| コメントの事実主張 | OK（初回 fail → 是正後 pass） | 初回は「すなわち rows が空か全行が空マッピング」という同値列挙が非 Map 行を落としていた。是正後は `YamlSection.java:138` と同じ「キーを持つ行が 1 つも無い場合（例えば…）」に統一。 |

### Craft Expert (coding)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | OK | `castMap` が非 Map に `Collections.emptyMap()` を返す（`:98-103`）ため `rowMap.isEmpty()` は null 安全。呼び出し元2箇所とも `getList` 経由で null は来ない（`getList` は非 List/キー不在で `Collections.emptyList()`）。`new ArrayList<String>()`・ダイヤモンド不使用で Java 6 世代のスタイルに一致。 |
| Consistency with existing style | OK | 新規フィクスチャの命名は既存語彙に沿い、PK は既存と衝突なし。インデント・引用符・グループ間の空け方は既存どおり。末尾の余分な空行なし。 |

### Verification Expert (test)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked (tests run) | OK | surefire XML を直接確認。`YamlSectionTest` 17件（既存11＋追加6）、`YamlTableDataBuilderTest` 37件、全体 `Tests run: 187, Failures: 0, Errors: 0, Skipped: 0`。追加6件が testcase 要素として存在し `<failure>`/`<error>` なし。 |
| Coverage (edge cases) | OK（初回 NG → 是正後 pass） | 初回は変異 mut4（最後のキー保持行を返す）で追加3テストが**1件も落ちず**、「先頭の」が固定されていなかった。フィクスチャを行ごとに異なるキー集合へ変更した結果、mut4 で **6件が撃墜**（追加4＋新設単体テスト1＋既存1）。局所的な列ズレ変異 mut6 も、初回は setup/expected が検出できなかったが、全列 assert 化により4経路すべてで撃墜。 |

## Coordinator Review / Triage

coordinator が自分で確認したこと: `git diff 93e270e..2f257ef -- src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` が**空**（ロジック変更ゼロ）。`src/main` の変更は `YamlTableDataBuilder.java` のコメント2箇所（4行）のみ。`resolveColumns` の全経路（`YamlSection.java:144-149` → `extractRows` の `for (String col : columnNames)` 0周 → `buildTableData` の `if (rawRow.isEmpty()) continue`）を追って不具合の機序を確認。解説書 `testdata_notation.rst:819` / `:1534` の該当箇所と、YAML 実装に「全値が空文字ならスキップ」が無いこと（空判定は `:145` `rawRow.isEmpty()` と `:222` `rowMap.isEmpty()` のみ）も実物で確認。作業ツリーが他エージェントのプローブで汚染されていないことも確認（tracked 差分ゼロ）。

| Finding | 出所 | Triage | 理由 |
|---|---|---|---|
| 追加テストが「**先頭の**」を固定できていない（mut4 で0件しか落ちない） | QA・Verification | **Valid → 修正済**（`2f257ef`） | フィクスチャを行ごとに異なるキー集合へ変更。mut4 で6件撃墜を再確認。 |
| アサーションが件数のみ／局所的な列ズレを検出できない | Craft・Verification | **Valid → 修正済**（`2f257ef`） | 順序込みのリスト比較＋各行の全列値 assert＋欠落キーの `assertNull`。 |
| `:434` の assert メッセージに旧表現「先頭行が {}」が残存 | Craft | **Valid → 修正済**（`2f257ef`） | 「全行が {}」へ。フィクスチャ実態に合致。 |
| コメントの「すなわち〜」列挙が非 Map 行を落としている | Craft・Design | **Valid → 修正済**（`2f257ef`） | `YamlSection.java:138` と同じ表現に統一。 |
| `resolveColumns` に直接の単体テストが無い（非 Map 行スキップが全レベル未検証） | QA・Craft | **Valid → 修正済**（`2f257ef`） | `YamlSectionTest` に6件追加。スキーマが `rows.items: {"type":"object"}` で弾くため、この単体テストでしか到達できない経路。 |
| `expected_complete_tables`（`fillDefaults=true`）に先頭 `{}` のテストが無い | Craft・Verification | **Valid → 修正済**（`2f257ef`） | setup 版と expected 版は同一コードを通るため、これが3本目の分岐。 |
| 解説書 `testdata_notation.rst:819`「カラム名は最初の行のキーで決まる」と実装の食い違い | Design | **他リポジトリ → ユーザー報告済** | `nablarch-document` は別リポジトリ。共通ルール「他リポジトリのファイルを変更しない／他責先は報告する」に従う。 |
| 「すべての値が空文字の行」がスキップされない（解説書 `:1534` が `{}` と同格と規定）。先頭にあると #17 と同型の消失が起きる | Design | **Valid → ユーザー判断待ち** | 起票の可否を諮り中。列名解決だけでなく行スキップ自体の実装が要る。 |
| 「先頭がマーカーのみの行」で後続の実データ行の値が全消失 | Verification | **Valid → ユーザー判断待ち** | 同上。#17 と同型。 |
| 「2行目以降が1行目に無いキーは読み込まれない」 | Design | **不具合ではない** | 解説書 `:819` に仕様として明記。 |
| `{}` ＋マーカーのみ行で 旧「0行」→ 新「値ゼロの2行」 | QA | **退行ではない** | `{}` が無い同型（マーカーのみ行が先頭）は旧から同じ挙動。既存挙動への整合。 |
| ループ変数名を `rowObj` に揃えると兄弟メソッドと一致 | Craft（nit） | **対応不要** | 実害なし。 |

## Overall Verdict

- Self-check: OK
- QA: OK
- Design expert: OK（初回 fail → 是正後 pass）
- Craft expert: OK
- Verification expert: OK（初回 Coverage NG → 是正後 pass）
- Ready to check off: Yes
