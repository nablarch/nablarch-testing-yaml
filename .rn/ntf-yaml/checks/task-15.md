# task-15 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `YamlTestDataParserTest` に `FIXME` / `@Ignore` が残っていない | OK | `grep -n "FIXME\|@Ignore\|org.junit.Ignore" src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java` → 該当なし（出力 0 行）。削除したのは `:396`〜`:398` の FIXME 3 行、`:399` の `@Ignore(...)` 1 行、および他に `@Ignore` が残らなくなった `import org.junit.Ignore;`（`:24`）の計 5 行。 | OK | `grep -rn "FIXME\|@Ignore"` → ヒット 0。`Ignore` の唯一のヒットはメソッド名 `setTestDataReaderLogsInfoAndIgnores()`（`:1046`）で偽陽性。3レビュアが独立に同じ確認。 |
| `emptyExpectedTable_failsWhenDbHasRows` が実行され PASS している（Skipped でない） | OK | `target/surefire-reports/TEST-nablarch.test.core.reader.YamlTestDataParserTest.xml` に `<testcase name="emptyExpectedTable_failsWhenDbHasRows" classname="nablarch.test.core.reader.YamlTestDataParserTest" time="0.058"/>` — 子要素 `<skipped>` / `<failure>` / `<error>` なし＝実行され PASS。同 `.txt` は `Tests run: 49, Failures: 0, Errors: 0, Skipped: 0 - in nablarch.test.core.reader.YamlTestDataParserTest`。全体の Skipped 3 件は `YamlTableDataBuilderTest` の 3 件（`buildTableDataList_emptyExpectedTableReturnsTableDataWithAllDbColumns` / `buildTableDataList_allEmptyRowsReturnsTableDataWithAllDbColumns` / `buildTableDataList_emptyRowsExcluded`）＝別タスク #16 の対象で、本タスク対象外。 | OK | QA・Verification が独立に surefire XML を parse し `<testcase …/>` に `skipped`/`failure`/`error` 子要素なしを確認。クラス計 `Tests run: 49, Failures: 0, Errors: 0, Skipped: 0`。 |
| テストの Given/When/Then が `0197071` 時点と同一 | OK | `git diff 0197071 -- src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java` の全出力が上記 5 行の削除（`-` 行）のみ。追加行（`+`）ゼロ、テストメソッド本体・javadoc・アサーション・テストデータへの変更なし。 | OK | `git diff 0197071 058a11f -- <該当ファイル> --numstat` → `0 5`（追加ゼロ）。メソッド本体 `:396-417` を `0197071` 版と切り出し `diff` して差分ゼロ（Verification・Craft が独立に確認）。 |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o clean test` → `[WARNING] Tests run: 177, Failures: 0, Errors: 0, Skipped: 3` / `[INFO] BUILD SUCCESS`。`-o` は外していない。ベースライン `177/0/0/Skipped 4` に対し Skipped が 4→3 に減少（想定どおり）。 | OK | `Tests run: 177, Failures: 0, Errors: 0, Skipped: 3` / `BUILD SUCCESS`。Verification の1回目 BUILD FAILURE は QA と同時に `mvn clean` が走った並行実行の人工物で、単独再実行で成功を確認済み。 |

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective (checks the right thing, not just "passed") | OK | 変異実験2本で実証。(1) 本体 `TableData#loadData()` を pre-#23 実装（`-24.jar` 相当）へ戻した複製で実行 → `Tests run: 49, Failures: 1` で**このテストだけが FAIL**（`DB に行があるのに assertTableEquals が通り抜けた（偽陰性）`）。他48件は #23 を検出できておらず、本テストが唯一の砦。(2) Given の `insert`（`:398`）を無効化 → 同じく FAIL。DB セットアップは load-bearing。実際に解決される jar が修正版であることも `mvn dependency:build-classpath` + `javap -c`（`arraylength / ifne → DbInfo.getColumns`）で確認。変異はすべて scratchpad の複製で実施し、本体・対象リポジトリとも非改変。 |
| 実行順序依存・データ汚染 | OK（潜在リスクは finding B） | `@BeforeClass` の `VariousDbTestHelper.createTable` が先頭で `dropTable` を呼ぶ（`javap -c` 確認）ためクラス跨ぎ汚染はリセットされる。`TEST_TABLE` を参照するのはモジュール内3ファイルのみで、DB の行内容を読むテストは他に存在しない。現状無害。 |
| Skipped 4→3 の減少がこのテストの復活によるものか | OK | pristine 複製に `@Ignore` だけを戻して全量実行 → `Skipped: 4`（parser 1 + builder 3）。現行は parser `Skipped: 0` / 合計 3、総数は両方 177 で不変。差分1件はこのテストの復活そのもの。 |

## Expert Reviews (axes the task needs)

### Craft Expert (coding)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | OK | `import org.junit.Ignore;` の削除は妥当（`@Ignore` アノテーションはファイル内に残っておらず、"Ignore" を含む `:1046` メソッド名・`:1080` javadoc の `ignore-blank-lines` と取り違えていない）。残る全 import の使用箇所を1つずつ実コードで確認済み。指摘: `java.util.Collections`（`:28`）が未使用だが `0197071` 時点でも未使用の既存持ち越しで、本コミット起因ではない。 |
| Consistency with existing style | OK | `git diff --check` クリーン。削除跡は javadoc の `*/`（`:392`）直後に `@Test`（`:393`）が続き、余分な空行・取り残しなし。`[BUG-F]` javadoc（Given/When/Then を含む `:385-392`）は完全に保存され、巻き添え削除なし。 |

### Verification Expert (test)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked (tests run) | OK | surefire XML を直接 parse して確認（報告の「BUILD SUCCESS だった」を根拠にしていない）。suite 属性 `tests=49 failures=0 errors=0 skipped=0`。 |
| Coverage (edge cases) | OK（未担保分はすべて criteria 範囲外） | 例外の発生源を隔離ランナーで実測特定: メッセージ `空テーブル検証 an unexpected record is included in the table of [TEST_TABLE]. PK=[PK_COL1=00001,PK_COL2=1]`、スタック先頭 `Assertion.fail(Assertion.java:578)` ← `Assertion.java:311`（「DB にあって期待値になかったデータ」分岐）＝意図どおり。ネガティブコントロール（DB を空にすると `AssertionError` が出ない）も実測。`expected.get(0).getColumnNames()` は長さ0で、フォールバックは本体 `TableData.java:345-347` 側で効いていることを確認。未担保: `EXPECTED_COMPLETE_TABLE`+`rows: []`（実害なしと実測）、`SETUP_TABLE`+`rows: []`（#16 対象）、`rows: []` 複数エントリ — いずれも criteria がファイルスコープのため範囲外。 |

## Coordinator Review / Triage

coordinator が `git diff 681e468..HEAD` を自分で確認: 削除のみ5行・追加ゼロ、Given/When/Then 無変更、scope 内。本体 `nablarch-testing/src/main/java/nablarch/test/core/db/TableData.java:339-347` に `colNames.length == 0` → `dbInfo.getColumns(tableName)` フォールバックが存在することも自分で確認。

| Finding | 出所 | Triage | 理由 |
|---|---|---|---|
| A/F-2: `catch (AssertionError e)`（`:412`）がメッセージ・発生源を検証していない | QA・Verification | **Invalid（scope 外）** | 修正には Then の変更が要る。Completion criterion「テストの Given/When/Then が `0197071` 時点と同一」および指示書 2-1「テストの中身（Given / When / Then）は1文字も変えない」に抵触する。ユーザーへ所見として報告する。 |
| B/F-4: `VariousDbTestHelper.insert`（`:398`）の後始末がなく、挿入行がクラス末尾まで残る | QA・Craft・Verification | **Invalid（scope 外）→ ユーザー報告** | `@After` 追加は本タスク（FIXME/`@Ignore` の削除のみ）の scope を広げる。現状無害であることは3レビュアが独立に確認済み（`createTable` の `dropTable`／DB 行を読むテストが他にない）。scope 拡大の可否はユーザー判断。 |
| C/F-3: `YamlTableDataBuilderTest` の FIXME 3件と `YamlTableDataBuilder.java:110` の FIXME が現時点で事実と食い違う | QA・Verification | **Valid だが #16 の対象** | steering `#16` の Steps C・D がそのまま該当。本タスクでは対応しない。 |
| F-1: #23 の修正はローカル install の SNAPSHOT jar にのみ存在し、リモート取得版 `-24.jar` には無い | Verification | **Invalid（scope 外）→ ユーザー報告** | 5リポジトリを順に直して install していく計画（`指示/00-共通ルール.md`）に内在する前提であり、本タスクで解消できない。 |
| 未使用 import `java.util.Collections`（`:28`） | Craft | **Invalid（scope 外・既存）** | `0197071` 時点でも未使用。本コミット起因ではない。 |

## Overall Verdict

- Self-check: OK
- QA: OK
- Design expert: N/A（構造・方針を作らない削除のみのタスク）
- Craft expert: OK
- Verification expert: OK
- Ready to check off: Yes
