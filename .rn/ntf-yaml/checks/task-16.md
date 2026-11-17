# task-16 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `grep -rn 'TODO\|FIXME\|@Ignore' src --include=*.java` が 0 件 | OK | 実行結果 0 件。`org.junit.Ignore` の import も削除済み（`YamlTableDataBuilderTest.java` に他の利用箇所なし）。残存ヒットは `YamlTestDataParserTest.java:1046` のメソッド名 `setTestDataReaderLogsInfoAndIgnores` のみで、これは `@Ignore` にも TODO/FIXME にも該当しない | | |
| 3テストが実行され PASS している（Skipped 0） | OK | `target/surefire-reports/TEST-nablarch.test.core.reader.yaml.YamlTableDataBuilderTest.xml` に `tests="33" errors="0" skipped="0"`、`<skipped` タグ 0 件。リネーム後の `<testcase name="buildTableDataList_allEmptyRowsReturnsTableDataWithNoColumns" time="0.006"/>`、`..._emptyExpectedTableReturnsTableDataWithNoColumns time="0.003"`、および名称据え置きの `..._emptyExpectedCompleteTableReturnsTableDataWithAllDbColumns time="0.002"` が failure/skipped 子要素なしで存在。`buildTableDataList_emptyRowsExcluded` も同様 | | |
| 変更した `assertThat` は列名に関するものだけで、テーブル名・件数・行数の主張は `6204d38` 時点と同一 | OK | 本ラウンドで変更したのは `setup_tables` 2件のメッセージ文字列（第1引数）のみ。`:160` と `:435` の `カラム名が 0 件であること（解決は本体側の責務）` → `カラム名が 0 件であること`（javadoc の「0 件のまま解決されない」と矛盾していたため）。`expected_tables` の `:890` は本体側で解決されるため据え置き。期待値は `is(0)` 3件（`YamlTableDataBuilderTest.java:160,435,890`）・`is(11)` 1件（`:915`）のまま。`git diff aa8f7e6 -- src` の非コメント差分はこの2行のみ | | |
| `YamlTableDataBuilder.java` のコメントが責務の所在を説明している | OK | `fillDefaults` 3分岐の説明を `buildTableData` の javadoc（`YamlTableDataBuilder.java:100-127`）へ移し、`new TableData(...)` 直前のインラインは列名 0 件になる条件と作り出さない理由の4行（`:138-141`）に絞った。記述ごとの裏取りは下記「§7 2回目の是正ラウンドの裏取り」。「FIXME」「TODO」「#17」等のタスク番号は書いていない | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | 2回目の是正ラウンドで再実行: `[INFO] Tests run: 177, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`（`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o clean test`、他プロセスと並行させず単独実行）。`Tests run:` 行の実出力を確認済み（`No tests to run.` ではない） | | |

## Evidence 詳細

### §3 前提の裏取り（すべて実物のコードで確認。参照のみ・書き込みなし）

1. **expected_tables 経路の安全性 — OK**
   `/home/tie303177/work/nablarch/nablarch-testing/src/main/java/nablarch/test/core/db/TableData.java:337-347`
   `loadData()` 冒頭で `String[] colNames = getColumnNames();`、`if (colNames.length == 0) { colNames = dbInfo.getColumns(tableName); }`（:345-346）。
   コメントにも「カラム名の宣言は『検証対象カラムの絞り込み』であって『検証するかどうか』のスイッチではないため、カラム名が0件でもDBは読む」と明記。
   よって列名0件の TableData を期待値に使っても DB 全カラムが読まれ、偽陰性にならない。

2. **setup_tables 経路の安全性 — OK（空化は機能する）**
   - `DbAccessTestSupport.java:175-203`: `setUpDb` は `TableDataSorter.reversed(...)` の順に `tableToDelete.deleteData(conn)` → `TableDataSorter.sort(...)` の順に `tableToInsert.insertData(conn)`。
   - `TableData.java:127-130` `deleteData()`: `connection.prepareStatement("DELETE FROM " + tableName).executeUpdate()`。**列名を一切参照しない**ため、列名0件でもテーブルは DELETE される。
   - `TableData.java:137-178` `insertData()`: 列は `getNonComputedColumns()`（:327-333）から取得し、その実体は `dbInfo.getColumns(tableName)`。つまり `getColumnNames()` ではなく **dbInfo を直接読む**ので列名0件の影響を受けない。行ループは `contents.size()`（rows: [] のとき 0）なのでバインドは1件も起きず、`insert.executeBatch()`（:177）が0件バッチで走るだけ。
   - `TableDataSorter.java:173`: 比較は `getIndex(t1.getTableName()) - getIndex(t2.getTableName())` で、列名に依存しない。
   → 列名0件でも `setup_tables` の「テーブルを空にする」指示は DELETE として正しく機能する。停止条件には該当しない。

3. **converter 経路 — OK（ただし前版の「DbInfo を持たない」は誤り。正しくは「`getColumns` が呼べない」）**
   - 変換ツールは DbInfo を**持つ**。`nablarch-testing-converter/src/main/java/nablarch/test/core/reader/StubDbInfo.java:37-39` の `getColumnType` は `Types.VARCHAR` を返し、`:48-50` の `getColumns` は `UnsupportedOperationException`（`:31-34` の `notOnReadPath`）を投げる。
   - よって「DbInfo が無いから列名解決できない」ではなく「`getColumns` が使えない DbInfo 実装がある」が正確。ビルダが `dbInfo.getColumns()` で列名解決すると、この経路が `UnsupportedOperationException` で壊れる。
   - なお `YamlTableDataBuilder.java` の `buildTableData` は `dbInfo` を `new TableData(dbInfo, ...)` へ渡すだけだが、`fillDefaults=true` のときは同メソッド内の `fillDefaultValues()`（:138-140）経由で `TableData#fillDefaultValues()` が `dbInfo.getColumns(tableName)` を呼ぶ。前版の「本ビルダは DB を参照しない」は**この経路によって反証される**。

### §5 変異実験（「意味のあるテスト」の担保）

コマンド（変異前・変異後とも同一）:
```
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 LANG=ja_JP.UTF-8 TZ=Asia/Tokyo \
  "/mnt/c/Program Files/apache-maven-3.9.9/bin/mvn" -o clean test
```

- 変異前（本コミットの実装）: `[INFO] Tests run: 177, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS
- 変異後（`buildTableData()` を `dataColumns.isEmpty() ? dbInfo.getColumns(tableName) : ...` に一時変更）:
  `[ERROR] Tests run: 177, Failures: 3, Errors: 0, Skipped: 0` / BUILD FAILURE
  失敗3件はいずれも `カラム名が 0 件であること（解決は本体側の責務）`:
  `buildTableDataList_emptyRowsExcluded:160` / `buildTableDataList_allEmptyRowsReturnsTableDataWithAllDbColumns:433` / `buildTableDataList_emptyExpectedTableReturnsTableDataWithAllDbColumns:886`
- 復元後の再実行: `[INFO] Tests run: 177, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS
  （`git diff --stat` は変更2ファイルのみ。変異コードは残っていない）

### §6 コメント是正ラウンドの裏取り（`ac08429` → HEAD。すべて実物のコードで確認・参照のみ）

書き直したコメントは `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:110-125`。記述ごとの出典:

| コメントの記述 | 裏付け（ファイル:行） |
|---|---|
| 列名は先頭行 `rows.get(0)` のキーからのみ決まる | `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:145-150`（`rows.isEmpty()` なら空リスト、そうでなければ `castMap(rows.get(0)).keySet()`） |
| rows が空、または先頭行が空マッピング `{}` のとき dataColumns が空になる | `YamlSection.java:146-149` と `YamlTableDataBuilder.java:102-109`（`{}` の場合 `keySet()` が空→ `cols` が空→ `dataColumns` も空） |
| 変換ツールの StubDbInfo は `getColumns` で `UnsupportedOperationException` を投げる | `/home/tie303177/work/nablarch/nablarch-testing-converter/src/main/java/nablarch/test/core/reader/StubDbInfo.java:48-50`（`getColumnType` は `:37-39` で `Types.VARCHAR`） |
| `fillDefaults` は setup_tables=false / expected_tables=false / expected_complete_tables=true | `src/main/java/nablarch/test/core/reader/YamlTestDataParser.java:113`（setup, false）、`:122-123`（expected, false）、`:124-125`（complete, true） |
| `TableData#loadData()` は列名 0 件のとき `dbInfo.getColumns(tableName)` を取得対象カラムにする | `/home/tie303177/work/nablarch/nablarch-testing/src/main/java/nablarch/test/core/db/TableData.java:337-347`（`:345-346` が `if (colNames.length == 0) { colNames = dbInfo.getColumns(tableName); }`） |
| この前提が崩れれば `emptyExpectedTable_failsWhenDbHasRows` が落ちる | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:385-396`（DB に 1 件ある状態で `rows: []` の期待値と `assertTableEquals` し `AssertionError` を要求） |
| `TableData#deleteData()` は `"DELETE FROM " + tableName` で列名を使わない | `nablarch-testing/.../TableData.java:127-130` |
| `TableData#insertData()` が使うのは dbInfo 由来の自動計算カラム除外のカラムで、TableData の列名ではない | `TableData.java:137-140`（`getNonComputedColumns()`）と `:325-334`（`dbInfo.getColumns(tableName)` から `dbInfo.isComputedColumn(...)` を除外） |
| `fillDefaults=true` では `TableData#fillDefaultValues()` が `dbInfo.getColumns(tableName)` を列名として設定する | `TableData.java:707-723`（`:709` で取得、`:722` で `setColumnNames(allColumns)`） |

テスト側の変更（機構の詳細はビルダのコメント1箇所に集約し、テストは「なぜ列名 0 件が正しいか」に留める）:

- リネーム: `buildTableDataList_allEmptyRowsReturnsTableDataWithNoColumns`（`YamlTableDataBuilderTest.java:426`）、`buildTableDataList_emptyExpectedTableReturnsTableDataWithNoColumns`（`:882`）。`..._emptyExpectedCompleteTableReturnsTableDataWithAllDbColumns`（`:907`）は名前・期待値とも据え置き。
- 旧名の残存確認: `grep -rn` で `src/` に 0 件（ヒットは `.rn/ntf-yaml/steering.md:379-380`、`.rn/ntf-yaml/checks/task-10.md:8`、`task-15.md:8` の記録文書のみ。いずれも本タスクの Scope 外）。
- javadoc 是正: `:143-147`（setup_tables は「解決される」でなく「必要にならない」）、`:418-423`（先頭行キー由来の限定を追加し他2件と粒度を統一）、`:871-879`（`loadData()` フォールバックを依存先の振る舞いとして記述し、兄弟テストが 11 カラムを期待する理由＝`fillDefaults=true` に言及）。

`git diff ac08429 -- src` の内容はコメント・javadoc・メソッド名のみ。`assertThat` の追加・削除行は 0 件。変異実験は本ラウンドでは行っていない。

### §7 2回目の是正ラウンドの裏取り（`aa8f7e6` → HEAD。すべて実物のコードで確認・参照のみ）

前提として、依存先 `nablarch-testing` は `pom.xml` の親から `6-NEXT-SNAPSHOT` が解決される（`mvn -o dependency:tree` の
`com.nablarch.framework:nablarch-testing:jar:6-NEXT-SNAPSHOT:compile`）。以下の出典は
`~/.m2/repository/com/nablarch/framework/nablarch-testing/6-NEXT-SNAPSHOT/nablarch-testing-6-NEXT-SNAPSHOT-sources.jar`
に含まれる `nablarch/test/core/db/TableData.java` を展開して確認した。

| 直した記述 | 直した理由（実物の出典） |
|---|---|
| 「YAML に列名を書く場所が無い」の適用範囲 | `YamlSection.java:145-150` の `resolveColumns` は `rows.isEmpty()` なら空、そうでなければ `castMap(rows.get(0)).keySet()`。先頭行が `{}` でも後続行にキーがあれば YAML には列名が書かれているため、旧記述は成立しない。よって「どの行もキーを持たない場合（`rows: []`／全行が `{}`）」に限定した（`YamlTableDataBuilder.java:138-141`、`YamlTableDataBuilderTest.java:144-146` / `:420-422` / `:872-874`） |
| 下流の `StubDbInfo` を名指ししていた点 | 依存方向は converter → yaml であり、`nablarch-testing-converter/src/main/java/nablarch/test/core/reader/StubDbInfo.java:28` は package-private（`final class StubDbInfo`）で本モジュールから参照できない。実装名をやめ「`getColumns` を提供しない `DbInfo` 実装と組み合わせて読み込み専用に使われうる」という契約の記述に置き換えた（`YamlTableDataBuilder.java:140-141`） |
| 「`getColumns` に依存しない」と「`fillDefaults=true` では `getColumns` を使う」が矛盾して見える点 | 読み込み専用経路は `fillDefaults=false` で呼ばれる。`nablarch-testing-converter/src/main/java/nablarch/test/core/reader/YamlTestCoreAdapter.java:122` が `buildTableDataList(yaml, tableSectionKey(type), groupId, false, path)` を渡しており、converter の `main` 配下に `buildTableDataList` の呼び出しは他に無い（`grep -rn` で1件）。よって `fillDefaultValues()` に到達しない旨の一句を javadoc 末尾（`YamlTableDataBuilder.java:122-126`）に足した |
| `:160` / `:435` のアサーションメッセージ | 同テストの javadoc が「setup_tables では列名は 0 件のまま解決されない」と述べており、`（解決は本体側の責務）` と矛盾していた。文言から括弧書きを削除。`expected_tables` の `:890` は `TableData.java:337-347` の `loadData()` フォールバックにより本体側で解決されるため据え置き |
| `:412` 付近の因果 | カラム名が 0 件になる因果は「空マッピングのスキップ」ではなく `resolveColumns` が先頭行（`{}`）のキーを取ること（`YamlSection.java:145-150`）。行データ 0 件（`YamlTableDataBuilder.java:144-146` の `rawRow.isEmpty()` continue）とカラム名 0 件を別々の原因として書き分けた |

javadoc へ移した3分岐の記述は §6 の表と同じ出典で再確認した（`TableData.java:127-130` deleteData／`:137-140` と `:325-334` insertData／`:337-347` loadData／`:707-723` fillDefaultValues）。

書式面: `//（` 始まりを解消、em ダッシュを廃止、箇条書きは javadoc の `<ul><li>` に移行、参照するテストを
`nablarch.test.core.reader.YamlTestDataParserTest#emptyExpectedTable_failsWhenDbHasRows` と完全修飾（本ファイルは
`nablarch.test.core.reader.yaml` パッケージ）。

`git diff aa8f7e6 -- src` の非コメント差分は `assertThat` 第1引数の2行のみ（上記）。コード行・期待値・メソッド名の変更は 0 件。

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective | OK | 「失敗したテストを通すために期待値だけ書き換えた」のではないことを、変異実験4本で実証。(A) 列名解決をビルダ側へ戻すと対象3テストだけが FAIL。(B) フィクスチャの `group_id` を改名すると「サイズ 1 のリストが返ること」が先に落ちる＝グループ不一致は捕捉済み。(C) `emptyRows` に実データ行を入れると列名の主張が落ちる＝0 は YAML 内容に追随。(D) `rows:` キー自体を削除すると16件が `YamlSchemaValidationException` で error＝「キー不在」が `rows: []` に化けない。実際に解決される jar が修正版であることも `mvn dependency:build-classpath` + `javap -c` で確認。 |
| 3経路（setup / expected / expected_complete）の整合 | OK | 差は section ではなく `fillDefaults`。`TableData#fillDefaultValues()` が末尾で `setColumnNames(allColumns)`（`TableData.java:722`）するため `expected_complete_tables` のみ 11 件になる。変異 A 下でも当該テストは 11 のまま PASS。 |
| `setup_tables` で列名0件でもテーブルが空になるか | OK | `TableData.java:127-130` `deleteData()` は `"DELETE FROM " + tableName`（jar のバイトコードでも `columnNames` フィールドアクセス無しを確認）。`insertData()` は `getNonComputedColumns()` 由来で `columnNames` に依存せず、0 行なのでバインドループは0周。`DbAccessTestSupport.setUpDb:184` の早期 return はリストが空のときのみで、`rows: []` は TableData 1件を生むため delete は走る。 |

## Expert Reviews (axes the task needs)

### Design Expert

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Approach/structure fits | OK（初回 fail → 是正後 pass） | 初回は「本ビルダは DB を参照しない」が `:132-134` の `fillDefaultValues()` で反証され fail。是正後は `fillDefaults` による3分岐を実際のとおり記述し、`getColumns` を提供しない `DbInfo` 実装と組み合わせる読み込み専用経路は `fillDefaults=false` で呼ぶため `fillDefaultValues()` に到達しない、という契約で矛盾を解消。 |
| System-wide integrity | OK | 下流クラス名 `StubDbInfo` の名指しを撤去（`grep -rn "StubDbInfo" src/` = 0件）。依存方向 converter → yaml の逆転を解消。DbInfo なしで builder を構築する経路は `YamlTestCoreAdapter.java:75` の1箇所のみで、その `buildTableDataList` 呼び出しは `:122` の1箇所・`false` 固定であることを確認。`fillDefaults=true` を渡す DB なし経路は存在しない。 |
| 事実主張の一次情報照合 | OK | 3分岐の各主張を `TableData.java:127-130` / `:137-140`+`:325-334` / `:337-347` / `:707-723` で照合。引用テストが FQCN どおり `YamlTestDataParserTest.java:396` に実在し、前提が崩れれば落ちる関係も成立。 |

### Craft Expert (coding)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | OK（初回 fail → 是正後 pass） | 初回 fail の主因は、既知不具合（先頭 `{}` ＋後続実データ行）を「正しい設計」として文書化していた点。是正後は理由づけが「どの行もキーを持たない場合（`rows: []`／全行が `{}`）」に限定され、`YamlSection.java:145-149` と整合する真の含意になった。未使用 import なし、`import org.junit.Ignore;` の削除も妥当（"Ignore" を含む他のヒットはメソッド名・設定名で取り違えなし）。 |
| Consistency with existing style | OK | 16行のメソッド内コメントを `buildTableData` の javadoc（`<ul>`/`<li>`×3）へ移し、インラインは4行に。`//（` 行頭書式と em ダッシュ対を廃止（両ファイルで出現0件）。テスト javadoc から本体機構の写しを削り `{@code YamlTableDataBuilder#buildTableData}` への参照に一本化（テストファイル内の `DELETE FROM` 出現0件）。参照テストを別パッケージのため完全修飾。 |

### Verification Expert (test)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked (tests run) | OK | surefire XML を直接 parse。`tests="33" errors="0" skipped="0" failures="0"`、3件が実測時間つきで `<testcase>` に存在。リネーム後も `@Test` 数 33・全体 177 で不変＝実行対象から外れていない。 |
| Coverage (edge cases) | OK | 変異実験を独立に再現: base `177/0/0/0` BUILD SUCCESS、mut `Tests run: 177, Failures: 3` BUILD FAILURE。落ちたのは対象3件のみ（`Expected: is <0> but: was <11>`）、他5クラス144件は無傷。副次的発見として、同じ変異でも `emptyExpectedTable_failsWhenDbHasRows` は PASS するため、**列名0件の契約を守っているのはこの3テストだけ**（＝変換ツール経路を守る唯一の砦）。 |

## Coordinator Review / Triage

coordinator が自分で確認したこと: `git diff 6204d38..eb23c39 -- src` の全量目視（非コメント差分は `assertThat` 第1引数2件のみ、コード行・期待値・メソッド名の変更 0 件）。本体 `TableData.java:339-347` のフォールバック実在。`YamlTableDataBuilder.java:132-134` → `TableData.java:707-723` による `fillDefaults=true` 経路の DB 参照。converter `YamlTestCoreAdapter.java:122` が `false` を渡すこと。`StubDbInfo` の `getColumns` が throw・`getColumnType` が `Types.VARCHAR` を返すこと。`mvn -o javadoc:javadoc -Dshow=private` が BUILD SUCCESS で、既知の1件（モジュール／java8 api）以外に警告が出ないこと。

| Finding | 出所 | Triage | 理由 |
|---|---|---|---|
| コメントの「本ビルダは DB を参照しない」が `fillDefaultValues()` で反証される | Design・Verification | **Valid → 修正済**（`aa8f7e6`） | 3分岐を実際のとおりに記述し直した。 |
| 「YAML に列名を書く場所が無い」が先頭 `{}` ＋後続実データ行にも及ぶ書き方で、既知不具合を正しい設計として固定化 | Craft・Design | **Valid → 修正済**（`eb23c39`） | 「どの行もキーを持たない場合」に限定。挙動そのものの修正は #17。 |
| 下流 package-private の `StubDbInfo` を名指し（依存方向の逆転） | Design | **Valid → 修正済**（`eb23c39`） | 契約の表現に置換。 |
| 「`getColumns` に依存しない」と `fillDefaults=true` の記述が矛盾して見える | Design | **Valid → 修正済**（`eb23c39`） | 読み込み専用経路は `fillDefaults=false` である旨を追記。 |
| 「挿入は全カラム」「DbInfo を持たない経路」「`rows: []` に限る」の不正確さ | Design・Craft | **Valid → 修正済**（`aa8f7e6`） | それぞれ自動計算カラム除外／`getColumns` が呼べない／全行 `{}` を含む、へ是正。 |
| `:160` `:435` のアサーションメッセージが同テストの javadoc と矛盾 | Verification | **Valid → 修正済**（`eb23c39`） | 括弧書きを削除。 |
| `:412` の因果（スキップが原因で列名0件）が誤り | Verification | **Valid → 修正済**（`eb23c39`） | 行データ0件と列名0件を別原因として書き分け。 |
| 16行のメソッド内コメントが長すぎる／`//（`・em ダッシュ・箇条書き記号の作法 | Craft | **Valid → 修正済**（`eb23c39`） | javadoc へ移設し4行に圧縮、書式を既存作法へ。 |
| テスト javadoc に本体機構が重複記述され、本体変更時に最大4箇所更新になる | Design・Craft | **Valid → 修正済**（`eb23c39`） | 結論＋参照に一本化。 |
| `buildTableDataList_emptyRowsExcluded` の名前が中身（`size()` が 1）と矛盾 | Craft・Verification | **Invalid（ユーザー指示）** | ユーザーがリネーム対象を2件に限定（2026-08-21）。据え置きが正しい。 |
| 先頭 `{}` ＋後続実データ行でデータ行が無言消失する（既存不具合） | QA | **Valid → 別タスク #17** | ユーザー判断でタスク化済み。#16 では挙動を変えない。 |
| `getColumns` が例外を投げる `DbInfo` を渡す保護テスト、`setup_tables` の `rows: []` end-to-end テストが本リポジトリに無い | Design・Verification | **Valid → #19（カバレッジ）で扱う** | scope 拡大のため本タスクでは対応しない。 |
| `:104` の「条件は下の…コメント」が2条件を網羅と読ませうる | Craft・Design（いずれも非ブロッキング） | **対応不要** | #17 で `resolveColumns` が先頭の非空マッピング行を見るようになると、当該2条件が実際に網羅になる。 |
| 本体フォールバックがローカル install の SNAPSHOT にのみ存在（リモート版 `-24.jar` には無い） | Craft・Design・Verification | **Invalid（scope 外）→ ユーザー報告済** | 5リポジトリを順に直して install する計画に内在。一時的事情のためコメントには書かない。 |
| 未使用 import `java.util.Collections` / `nullValue` | Craft | **Invalid（既存）** | `6204d38` 時点から未使用。本変更起因ではない。 |

## Overall Verdict

- Self-check: OK（2回目の是正ラウンド。列名 0 件の根拠を「どの行もキーを持たない場合」に限定し、下流クラス名の名指しを契約の記述へ置換、3分岐の説明を javadoc へ移設、アサーションメッセージ2件と因果1件を是正。コード行・期待値・メソッド名の変更は 0 件。`mvn -o clean test` = Tests run: 177, Failures: 0, Errors: 0, Skipped: 0 / BUILD SUCCESS）
- QA: OK
- Design expert: OK（初回 fail → 2度の是正ラウンド後 pass）
- Craft expert: OK（初回 fail → 2度の是正ラウンド後 pass）
- Verification expert: OK
- Ready to check off: Yes
