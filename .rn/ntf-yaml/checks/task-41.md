# task-41 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `buildListMapRows_unknownCharacterTypeIsNotConverted` が削除されている | OK | 削除前の `YamlTableDataBuilderTest.java` の `:962`（javadoc 開始 `/**`）-`:987`（メソッドの閉じ括弧）＋`:988`（区切りの空行）の 27 行を削除。削除に伴い使われなくなった `import org.junit.Ignore;`（削除前 `:17`）も削除した。削除後は `grep -rn "buildListMapRows_unknownCharacterTypeIsNotConverted" src/` が 0 件。削除の境界は `git diff` で確認済みで、直前のテスト（`:958`-`:959` の `assertTrue(...)` と閉じ括弧。14 文字種のテスト `buildListMapRows_allFourteenCharacterTypesAreGenerated`）と直後のテスト `buildListMapRows_combinedCharTypeNotationKeepsSeparator`（現 `YamlTableDataBuilderTest.java:974`。javadoc は現 `:961`-`:972`、`@Test` は現 `:973`）は無傷。`git diff --numstat 0fd018e..HEAD` は `0 28 .../YamlTableDataBuilderTest.java` / `0 5 .../nativeTypes.yaml` で計 33 行削除（java 側 28 = テスト本体 27 ＋ import 1） | | |
| `charTypeUnknownTest` の参照有無が実測され、参照が無ければ削除されている | OK | 削除前に `grep -rn 'charTypeUnknownTest' . --exclude-dir=target --exclude-dir=.git` を実行。ヒットは 6 件で、内訳は (1) 削除対象テスト自身の 3 行（`YamlTableDataBuilderTest.java:969`・`:970`・`:981`）、(2) フィクスチャ定義自身（`nativeTypes.yaml:134`）、(3) コーディネータの `steering.md` のタスク指示文 2 行（計測時点で `:1410`・`:1420`。その後コーディネータが #41 の Purpose と Step C に加筆したため、再確認時点では `.rn/ntf-yaml/steering.md:1412`・`:1422` にずれている。内容は同じ 2 行）。**src/ 配下でこのフィクスチャを参照するのは削除対象テストだけ**であり、他のテスト・他のフィクスチャ・`src/main`・`src/test/resources` からの参照は 0 件。よって `nativeTypes.yaml` の `:133`（節コメント `# 3-2（負のテスト）: 列挙外の文字種名`）-`:136`（`GEN_COL` 行）＋区切りの空行を削除した。取り残しの確認として `grep -rn 'charTypeUnknown\|存在しない文字種' . --exclude-dir=target --exclude-dir=.git` を削除後に再実行し、src/ 配下 0 件（残るのは `.rn/` の過去記録と steering のみ）。`nativeTypes.yaml` 冒頭（`:1`-`:20`）および他の節コメントがこのエントリに言及していないことも確認した。隣接エントリ（`charTypeTest_外字` = 現 `:129`-`:131`、`charTypeCombinedTest` = 現 `:134`-`:136`）は無傷で、節の間の空行 1 行も従来どおり保った | | |
| アノテーションとしての `@Ignore` が src/test 全体で0件 | OK | `grep -rnE '^\s*@Ignore' src/test` → 出力 0 行（`| wc -l` = 0）。素の `grep -rn '@Ignore' src/test` は 1 件だけ残るが、それは `YamlMessageBuilderTest.java:1125` の javadoc 散文中の `{@code @Ignore}`（`* 他のどのテストも落ちない（隔離コピーでこのテストを {@code @Ignore} にして外し、`）であり、アノテーションではない。指示どおり**触っていない**。新たな `@Ignore` は 1 件も足していない | | |
| `mvn -o clean test` が BUILD SUCCESS で `Skipped: 0` | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `[INFO] Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`。着手前ベースラインは `Tests run: 319, Failures: 0, Errors: 0, Skipped: 1`（同コマンドを着手前に実行して実測）。差分は削除した 1 件（＝ Skipped だった 1 件）のみで、**実行されていたテストは 1 件も減っていない**。クラス別でも `YamlTableDataBuilderTest` が 67（Skipped 1）→ 66（Skipped 0）で、他クラスは着手前後で同数 | | |

## 削除してよい根拠（一次情報を自分で開いて確かめた）

削除の理由は「解説書に書かれていない期待を追っているテストだから」である。4 点を自分で実測した。

1. **解説書は列挙外の文字種名について何も定めていない。** ピン `afa4f9e` の `ja/development_tools/testing_framework/implementation/testdata_notation.rst:1315` は逐語で「\`\`${文字種,文字数}\`\`\\ で使用できる文字種は、以下の14種類に限定される。」であり、続く `:1317`-`:1322` の list-table が 14 種類を列挙するだけである（`git -C /home/tie303177/work/nablarch/nablarch-document show afa4f9e:<path> | awk 'NR>=1311 && NR<=1325'` で実測）。列挙外の名前を書いたときにどうなるか（そのまま残るのか、エラーになるのか）は**一言も書かれていない**。削除したテストの期待「`${存在しない文字種,3}` のまま残る」は解説書に出典を持たない。
2. **解説書はこの文から「それ以外はエラー」を意図的に落としている（一次証拠）。** `nablarch-document` のコミット `09779f6`（2026-08-25 13:06:13 +0900、`docs: 限定列挙に付けた「それ以外はエラー」を落とす`。`git -C ../nablarch-document merge-base --is-ancestor 09779f6 afa4f9e` が真＝ピンに含まれる）が、`testdata_notation.rst` の当該文から `（それ以外を指定するとエラーになる）` を削除している。差分は逐語で `-``${文字種,文字数}``\ で使用できる文字種は、以下の14種類に限定される（それ以外を指定するとエラーになる）。` / `+``${文字種,文字数}``\ で使用できる文字種は、以下の14種類に限定される。` である（`git -C ../nablarch-document show 09779f6 -- '*testdata_notation.rst'` で実測）。コミットメッセージに理由が書かれている: 「許容値を全件列挙したうえで『それ以外を指定するとエラーになる』と添えるのは、列挙そのものが言っていることの言い換えである。利用者は列挙から選ぶので、外れた場合の帰結を書く必要がない。」「『限定される』は残した。列挙が網羅であって例示ではないことを示すため。」同コミットは同じ書き換えを 4 箇所（固定長ファイルのディレクティブキー11個・可変長ファイルのディレクティブキー9個・フィールドのデータ型・`${文字種,文字数}` の文字種14種類）にまとめて適用している。指示書のいう「2026-08-25 ユーザー確定の基準」は、この commit という一次情報で裏が取れる。<br>
   さらに、その**直前の版（`3e01b69`）には `（それ以外を指定するとエラーになる）` が在った**（`git -C ../nablarch-document show 3e01b69:ja/development_tools/testing_framework/implementation/testdata_notation.rst | grep -n '文字種は、以下の14種類に限定される'` → `1350:``${文字種,文字数}``\ で使用できる文字種は、以下の14種類に限定される（それ以外を指定するとエラーになる）。`）。つまり削除したテストの期待「変換されず `${存在しない文字種,3}` のまま」は、**旧版の解説書とも正面から矛盾していた**。旧版に照らせば偽、現行版に照らせば出典が無い。どちらの版でも根拠を持たない。
3. **実際の挙動は文字種名の字種に依存し、一律ではない。** `../nablarch-testing/src/main/java/nablarch/test/core/util/interpreter/BasicJapaneseCharacterInterpreter.java:24` のパターンは `Pattern PTN = Pattern.compile("\\$\\{(\\W+)\\s*,\\s*([0-9]+)\\}")` で、文字種名部が `\W+`（＝ ASCII 英数字と `_` を**含まない**）に限られる。`:31` の `m.matches()` に一致すれば `:34` の `return delegate.generate(type, length);` で本体の生成クラスへ渡り、外れれば `:36` の `return context.invokeNext();` に落ちて**そのまま残る**。渡った先の `../nablarch-testing/src/main/java/nablarch/test/core/util/generator/CharacterGeneratorBase.java:53`-`:59` の `generate(String, int)` が `generators.get(charsetName)` の null 判定で `throw new IllegalArgumentException("unknown charsetName. charsetName=[" + charsetName + "]")` を実行する（`:54` が `get`、`:55` が `if (generator == null) {`、`:56` が `throw`）。<br>
   プロジェクトの依存クラスパス（`mvn -o dependency:build-classpath`）で `CompositeInterpreter` 相当の鎖（`InterpretationContext(value, [new BasicJapaneseCharacterInterpreter()])` に `invokeNext()`）へ 5 つの値を通し、次を実測した。

   | 入力 | 結果 |
   |---|---|
   | `${存在しない文字種,3}` | `InterpretationContext$InterpretationFailedException: interpretation failed. value=[${存在しない文字種,3}] interpreter=[nablarch.test.core.util.interpreter.BasicJapaneseCharacterInterpreter]`、cause `java.lang.IllegalArgumentException: unknown charsetName. charsetName=[存在しない文字種]` |
   | `${abc,3}` | **不変**（`${abc,3}` のまま） |
   | `${unknownType,3}` | **不変** |
   | `${半角英字X,3}` | **不変** |
   | `${半角英字,3}` | 変換される（例: `DSW`） |

   したがって「本体が `IllegalArgumentException` を投げる」は**今回の入力 `${存在しない文字種,3}` については真だが、列挙外の文字種名一般については偽**である。名前が ASCII 英数字を1文字でも含めばパターンに一致せず、そのまま残る。**挙動が入力依存で一貫しないこと自体が、解説書がこの挙動を定めていないことの裏づけである。** 旧 `@Ignore` の理由文（`実際 InterpretationFailedException（原因 IllegalArgumentException: unknown charsetName...）`）は、そのテストの入力に限れば正しい。
4. **Excel でも YAML でも同じ経路を通る。** `${文字種,文字数}` を解釈するのは上記 `BasicJapaneseCharacterInterpreter` の 1 クラスだけである。このクラスは本リポジトリの `src/test/resources/unit-test.xml` の 3 つの鎖の**いずれにも同じクラスとして**入っている: Excel 形式の鎖 `<list name="interpreters">`（`:28`。中の `CompositeInterpreter` が `:43`、その `interpreters` プロパティが `:44`、当該クラスの `<component>` が `:46`-`:47`）、YAML 形式の `<list name="yamlInterpreters">`（`:58`。当該クラスは `:69`-`:70`）、YAML 電文用の `<list name="yamlMessagingInterpreters">`（`:78`。当該クラスは `:84`-`:85`）。**`testDataInterpreters` という名前は `unit-test.xml` に存在しない**（`grep -n "testDataInterpreters" src/test/resources/unit-test.xml` → 0 件。以前の記述の誤りを訂正した）。Excel 用の鎖は `:21` の `<property name="interpreters" ref="interpreters"/>` から参照される。よって列挙外の文字種名の扱いは形式によらず同じであり、YAML 固有の担保対象ではない。

以上より、このテストは「間違えたときにどうなるか」を解説書に無い形で固定していたもので、2026-08-25 にユーザーが確定した「間違えたときにどうなるかは解説書に書かない」基準（`nablarch-document@09779f6` で裏が取れる）に照らして削除が妥当である。なお、14 種類が使えること自体（正の担保）は 14 文字種の網羅テスト `YamlTableDataBuilderTest.java:928` `buildListMapRows_allFourteenCharacterTypesAreGenerated`（javadoc は `:908` から、出典行は `:914`）が引き続き担保しており、今回の削除で失われた担保は無い。

## カバレッジ（JaCoCo・実測）

`task-40.md` と同じ手順（`rm -f jacoco.exec` → `mvn -o clean test` → `mvn -o org.jacoco:jacoco-maven-plugin:0.8.8:restore-instrumented-classes org.jacoco:jacoco-maven-plugin:0.8.8:report -Djacoco.dataFile=jacoco.exec` → `target/site/jacoco/jacoco.xml` を機械集計）で、**着手前と着手後の 2 回**計測した。

| | 行 | 分岐 | 命令 |
|---|---|---|---|
| 着手前（`0fd018e`） | 413/417 | 174/176 | 1809/1822 |
| 着手後 | 413/417 | 174/176 | 1809/1822 |
| 差分 | **±0** | **±0** | **±0** |

クラス単位でも全 9 クラスが着手前後で同値である（`YamlTableDataBuilder` 行 69/69・分岐 30/30、`YamlSection` 53/53・50/50、`YamlMessageBuilder` 87/87・38/38、`YamlFileBuilder` 85/86・41/42、`YamlLoader` 48/51・13/14、`YamlTestDataParser` 56/56・2/2、`YamlSchemaValidationException` 7/7、`InterpreterResolver` 2/2、`MessageContent` 6/6）。

**下がらなかった理由**: 削除したテストは `@Ignore` が付いており JUnit が実行していなかった（着手前の `Skipped: 1` がこの 1 件）。実行されていないテストは 1 行も到達させていないため、削除しても到達行・到達分岐は変わらない。到達不能になった分岐は無い。未到達 4 行 2 分岐の内訳は着手前と同じで、いずれも本タスクと無関係な既存箇所（`YamlFileBuilder` 1 行 1 分岐、`YamlLoader` 3 行 1 分岐）である。

## コミット前の確認（指示 §7 の 1〜4）

1. **`file:line` 参照の再確認**: 本ファイルに書いた参照を**すべて機械的に開き直した（2 巡目・2026-08-29）**。内訳は次のとおりで、**不一致 0 件**。
   - 解説書（ピン `afa4f9e`、`git -C ../nablarch-document show afa4f9e:ja/development_tools/testing_framework/implementation/testdata_notation.rst` を行番号つきで表示して照合）: `:1315`・`:1317`-`:1322`・`:1324`・`:1328`-`:1333`・`:1339`・`:1443`-`:1445`
   - 解説書の履歴: `09779f6`（本文・差分・親コミット `3e01b69` の `:1350`）、`6ba3c83`（差分ハンク `@@ -1296,6 +1296,8 @@`）、両者がピンの祖先であること
   - 本体（参照のみ）: `CharacterGeneratorBase.java:53`-`:59`（`:54` `get`・`:55` null 判定・`:56` `throw`）、`BasicJapaneseCharacterInterpreter.java:24`（`PTN`）・`:31`（`m.matches()`）・`:34`（`delegate.generate`）・`:36`（`invokeNext`）
   - 本リポジトリ（現 HEAD）: `unit-test.xml:21`・`:28`・`:43`・`:44`・`:46`-`:47`・`:58`・`:69`-`:70`・`:78`・`:84`-`:85`、`YamlTableDataBuilderTest.java:56`・`:908`・`:914`・`:928`・`:958`-`:959`・`:961`-`:974`・`:967`・`:1000`、`nativeTypes.yaml:83`・`:88`・`:129`-`:131`・`:133`-`:136`、`YamlMessageBuilderTest.java:1125`（パスは `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java`）、`YamlDateNotationTest.java:30`・`:113`・`:133`、`date.yaml:1`、`YamlTestDataParserTest.java:356`、`YamlTestDataParserTest/nativeTypes.yaml:33`
   - 削除前（`git show 0fd018e:<path>` 側で確認）: `YamlTableDataBuilderTest.java:17`（`import org.junit.Ignore;`）・`:962`-`:988`（`:969`・`:970`・`:981` が `charTypeUnknownTest` 参照、`:974` が `@Ignore`）、`nativeTypes.yaml:133`-`:137`（`:134` が `id: charTypeUnknownTest`）
   - `.rn/`（本ファイル内で言及したもの）: `steering.md:1131`・`:1140`・`:1141`・`:1154`・`:1155`・`:1412`・`:1422`、`checks/task-31.md:8`・`:9`・`:23`
2. **事実主張と確認手段**: 解説書の文面 → `git -C ../nablarch-document show afa4f9e:...` / `show 09779f6` / `show 3e01b69:...` / `show 6ba3c83 -- '*testdata_notation.rst'` / `merge-base --is-ancestor` を実行。挙動 → `mvn -o dependency:build-classpath` で得たクラスパス上で `InterpretationContext` に 5 つの値を通す単体プログラムを `javac`／`java` で実行し、例外クラス名・メッセージ・不変かどうかを取得（結果は上表）。Excel/YAML 共通 → `BasicJapaneseCharacterInterpreter.java` 全文と `unit-test.xml` の `interpreters`／`yamlInterpreters`／`yamlMessagingInterpreters` の 3 リストを開いて確認、`grep -n "testDataInterpreters"` が 0 件であることも実測。参照有無 → `grep -rn` を削除前後に実行。出典行番号のずれ → `grep -rnoE 'testdata_notation\.rst:[0-9]+(-:?[0-9]+)*' src/` の全 50 件を数え直し。`@Ignore` 0 件 → `grep -rnE '^\s*@Ignore' src/test`。テスト結果・カバレッジ → `mvn` を実際に実行して出力を取得。
3. **`grep -rnE '^\s*@Ignore' src/test`**: 0 件。
4. **`mvn -o clean test`**: `Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`。

## 申し送り: #41 で #31 の記録が失効した

#41 で削除したテストは #31 が要求して書いたものである。#31（`.rn/ntf-yaml/steering.md:1131` の `### ~~#31: 3-1〜3-6 — 記法・特殊記法のテスト追加（6件）~~`。`[x]` 済）の次の 2 箇所が、#41 をもって前提を失った。**本タスクでは指示どおり `steering.md` と `checks/task-31.md` に触っていない。**

| 場所 | 逐語 | 失効理由 |
|---|---|---|
| `.rn/ntf-yaml/steering.md:1140`（Step 3-2） | `- [x] 3-2. ${<文字種>,3} が14文字種それぞれで該当文字種3文字になる（サロゲートペアは3コードポイント）。**列挙外の文字種名は変換されないという負のテストも必ず書く**（notation.rst:1313-:1320）` | 太字の要求（負のテスト）が #41 で削除された。前半（14 文字種）は `buildListMapRows_allFourteenCharacterTypesAreGenerated`（`YamlTableDataBuilderTest.java:928`）が引き続き担保 |
| `.rn/ntf-yaml/steering.md:1154`（Completion criteria） | `- 3-2 の負のテスト（列挙外の文字種名は変換されない）が書かれている` | 同上。現在の src/ に該当テストは存在しない（`grep -rn "buildListMapRows_unknownCharacterTypeIsNotConverted" src/` → 0 件） |

なお、コーディネータが既に `steering.md:1141` と `:1155` に「#41 で失効」の注記を追記済みであることを確認した（本タスク着手時点の作業ツリー）。

`.rn/ntf-yaml/checks/task-31.md` 側は未注記である。次の 3 箇所が「負のテストが `@Ignore` 付きで存在する」という当時の状態を記録しており、現在の src/ とは一致しない（当時の実測記録としては真）。

- `checks/task-31.md:8` — 完了条件「落ちたものは `@Ignore` ＋ `NTF-DOC:` 印つきの理由で記録されている」の Evidence（`YamlTableDataBuilderTest.java:751` の `@Ignore(...)` を挙げている）
- `checks/task-31.md:9` — 完了条件「3-2 の負のテスト（列挙外の文字種名は変換されない）が書かれている」の行
- `checks/task-31.md:23` — 記法別一覧表の `3-2（負）` 行

**コーディネータへ**: `checks/task-31.md` の上記 3 箇所にも失効注記を入れるかどうかを判断されたい。#41 の完了条件「アノテーションとしての `@Ignore` が src/test 全体で 0 件」は、#31 の完了条件「落ちたものは `@Ignore` で記録されている」と字面上は両立しない（#31 の時点では 1 件在り、#41 でその 1 件ごと削除した）。

## スコープ外として触っていないもの

- `YamlMessageBuilderTest.java:1125` の javadoc 中の `{@code @Ignore}`（#38 の変異実験の記録。指示どおり不変）
- スキーマ `description` の追随（#42）
- **観測（未対応・#41 の範囲外）: 解説書の出典行番号が src 配下 13 箇所で 2 行ずれている。**

  **件数**: `grep -rnoE 'testdata_notation\.rst:[0-9]+(-:?[0-9]+)*' src/` で全 50 箇所を機械的に列挙し、`:1299` 以降を指すものを数え直した結果は **13 箇所**（以前この節に書いた「4 箇所」は過小で、誤り）。残る 37 箇所（`:92`・`:205`・`:255`-`:269`・`:339`・`:503`-`:507`・`:857`・`:886`・`:887`・`:889`・`:1149`・`:1153`・`:1155`）は挿入位置より前なので影響なし。

  **原因**: `nablarch-document` のコミット `6ba3c83`（2026-08-28、`docs(ntf): 交互記述は警告して変換、電文のレコードレイアウトは1つ`）が `testdata_notation.rst` の `:1296` の直後に 2 行（``records:``\ のレコードレイアウトは1つとする文＋空行）を挿入した（`git -C ../nablarch-document show 6ba3c83 -- '*testdata_notation.rst'` のハンク `@@ -1296,6 +1296,8 @@` で実測）。これにより `:1299` 以降を指す出典がすべて **+2** ずれた。`6ba3c83` はピン `afa4f9e` の祖先である（`git -C ../nablarch-document merge-base --is-ancestor 6ba3c83 afa4f9e` が真）。

  **13 箇所と正しい行番号**（正しい側はピン `afa4f9e` の本文を開いて内容を突き合わせて確認した）:

  | ファイル:行 | 現在の記述 | ピン `afa4f9e` での正しい行 | `afa4f9e` のその位置の内容 |
  |---|---|---|---|
  | `src/test/java/nablarch/test/core/db/YamlDateNotationTest.java:30`・`:113`・`:133` | `:1326`（`}-{@code :1331` と続く） | `:1328`-`:1333` | `:1328` 「日付は以下の形式で記述できる。」、`:1330`-`:1331` 2 形式の箇条書き、`:1333` ミリ秒／時刻の後置0埋め省略 |
  | `src/test/java/nablarch/test/core/db/YamlDateNotationTest/date.yaml:1` | `:1326-:1331` | `:1328`-`:1333` | 同上 |
  | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:356` | `:1337` | `:1339` | 「HTTP\ リクエストパラメータの値にアップロードファイルを指定したい場合は、値に ``${attach:ファイルパス}``\ と記述する。」 |
  | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/nativeTypes.yaml:33` | `:1337` | `:1339` | 同上 |
  | `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java:56`・`:914` | `:1313-:1320` | `:1315`-`:1322` | `:1315` 「14種類に限定される」の文、`:1317`-`:1322` 14 文字種の list-table |
  | 同 `:967` | `:1322` | `:1324` | 「本記法は単独でも組み合わせても使用できる。…」 |
  | 同 `:1000` | `:1441-:1443` | `:1443`-`:1445` | 改行文字の list-table 行（`* - 改行文字` / `"\r"`・`"\n"` / YAML パーサが制御文字に変換する旨） |
  | `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/nativeTypes.yaml:83` | `:1441-:1443` | `:1443`-`:1445` | 同上 |
  | 同 `:88` | `:1313-:1320` | `:1315`-`:1322` | 14 種類の文＋list-table |
  | 同 `:133` | `:1322` | `:1324` | 組み合わせ記法の文 |

  **今回直さない理由**: #41 のスコープ外（削除とは無関係の既存のずれ）。加えて Craft の指摘のとおり、**4 箇所だけ直すと同一ファイル内で矛盾する**。`nativeTypes.yaml:88` を `:1315`-`:1322` に直すと、同じファイルの `:133` に残る `:1322` が「組み合わせ記法の文」を指したまま `:88` の `:1322` は「14 文字種の list-table 行」を指すことになり、同じ番号が 1 ファイル内で 2 つの意味を持つ。**13 箇所を一括で直すこと。**

## QA / Expert Review

`36a8af6` について **QA と Craft の 2 軸**を独立したサブエージェントとして実施した。

**Design と Verification を回さなかった理由**（コーディネータ判断）:
- **Design**: 33 行の純粋な削除で、構造・approach を一切変更していない（`git diff --stat` はテスト 1 件・フィクスチャ 1 エントリ・未使用 import 1 行の削除のみ、`src/main` の変更ゼロ）
- **Verification**: 削除したテストは `@Ignore` が付いており実行されていなかった（着手前の `Skipped: 1` がこれ）。
  未実行のテストは本体コードを 1 行も到達させないため、削除してもカバレッジは動かない。
  実際 Craft が着手前後で JaCoCo を独立に再計測し、行 413/417・分岐 174/176・命令 1809/1822 が
  **全 9 クラスとも同値**であることを確認した。新たに変異を当てる対象も無い

| 軸 | 判定 | 指摘 |
|---|---|---|
| QA | **pass** | チェックファイルの偽記述 2 件、#31 の記録の失効 1 件 |
| Craft | **pass** | チェックファイルの偽記述 1 件、出典行番号のずれの範囲が過小 1 件 |

### 2 軸が持ち込んだ実質的な発見

いずれも**指示書にもチェックファイルにも無かった一次情報**で、削除の根拠を強め、あるいは記述の誤りを正した。

| 発見 | 検出した軸 | 内容 |
|---|---|---|
| **削除の根拠となる決定的な一次証拠** | QA | `nablarch-document@09779f6`（2026-08-25、`docs: 限定列挙に付けた「それ以外はエラー」を落とす`）が当該文から `（それ以外を指定するとエラーになる）` を**意図的に削除**している。コミットメッセージに理由が明記されており、指示書が言う「2026-08-25 ユーザー確定の基準」がこの commit で裏づけられる。さらに親 `3e01b69` の時点では在ったため、削除したテストの期待は**旧版の解説書とも正面から矛盾**していた |
| **「本体が `IllegalArgumentException` を投げる」は一般には偽** | QA | `BasicJapaneseCharacterInterpreter.java:24` のパターン `\$\{(\W+)\s*,\s*([0-9]+)\}` は文字種名部が `\W+`（ASCII 英数字と `_` を含まない）のため、`${存在しない文字種,3}` は例外になるが `${abc,3}`・`${unknownType,3}`・`${半角英字X,3}` は**マッチせずそのまま残る**。挙動は名前の字種に依存し一律ではない。**この不一貫さ自体が「解説書が定めていない」ことのより強い裏づけ**になる |
| **出典行番号のずれは 4 箇所ではなく 13 箇所** | Craft | 原因は `nablarch-document@6ba3c83` が `testdata_notation.rst` に 2 行挿入したことによる **+2 ずれ**。`:1299` 以降を指す出典がすべて影響を受ける（`:1149`・`:1153`・`:1155` は挿入位置より前で影響なし）。**4 箇所だけ直すと同一ファイル内で `:1322` が「14 種類の list-table 行」と「組み合わせ記法の文」の両方を指す形になり、かえって読み手を誤らせる** |
| **#31 の記録が失効した** | QA | `steering.md` の #31（`[x]` 済）の Step 3-2 と Completion criteria、および `checks/task-31.md:8`・`:9`・`:23` が、削除したテストの存在を前提にしている |

### 軸ごとの評価

| 軸 | 観点 | 判定 | 根拠 |
|---|---|---|---|
| QA | 検証のやり方が目的に対して意味を持つか（＝**消してよかったのか**） | OK | 解説書のピンと**その履歴**（`09779f6` とその親 `3e01b69`）まで遡って削除の正当性を確認。本体の例外を依存クラスパス上で実際に実行して確認し、指示書の主張が一般には偽であることまで突き止めた |
| QA | 削除で失われた保護 | OK | 実行されていた保護はゼロ（`@Ignore` で未実行）。14 種類が使えることは `buildListMapRows_allFourteenCharacterTypesAreGenerated` が担保し、その 14 件が解説書 `afa4f9e:testdata_notation.rst:1322` の列挙と**逐語一致**することを突き合わせて確認。`@Ignore` の理由文も過去記録に残っており復元可能 |
| Craft | 削除の取り残し | OK | 識別子・import・ヘルパー・定数の残骸ゼロを `grep` で全件確認。節コメントの番号飛びも `git show 0fd018e:` と比較して**発生していない**ことを確認（削除したのは重複ラベル `3-2（負のテスト）` であり、`3-2` は別行に残る） |
| Craft | 既存コードベースの流儀との一貫性 | OK | 空行の重複・欠落なし（`git diff --check` 無警告）、EOF の改行も保持。クラス javadoc・周辺コメントに削除したテストへの言及なし |

### トリアージ

有効と判定して直したもの: 7 件（Q1〜Q7）。**すべてチェックファイルの記述のみで、`src/` の変更はゼロ**（コードは 2 軸とも pass）。

コーディネータが直したもの（`steering.md` はコーディネータの持ち物）:
- #41 Purpose の過度な一般化を実測に基づいて訂正（`${abc,3}` は例外にならない旨と、`09779f6` の存在を追記）
- #31 の Step 3-2 と Completion criteria に「#41 で失効」の注記を追加
- `checks/task-31.md` に失効の節を追加（記録そのものは書き換えず、失効の事実と理由を追記）

却下したもの（根拠つき）:

| 指摘 | 却下の根拠 |
|---|---|
| 13 箇所の出典行番号を直す | #41 の完了条件外。**#42 に明示的な追加ステップとして入れる**（#42 が解説書との追随を担うタスクであるため）。#44 の報告でスコープ拡張として報告する |
| `YamlMessageBuilderTest.java:1125` の javadoc の実測値（`Tests run: 291 ... Skipped: 2`）が現在は再現しない | #38 の変異実験の記録として当時は真。#41 の責任範囲外 |
| `.rn/ntf-yaml/report-step4.md` が削除済みテストを現存として記述している | Step 4 第 1 回の**時点報告**。時点のスナップショットとして据え置く（#43 の報告書は第 2 回の時点で別に作る） |

### コーディネータの独立検証（`36a8af6`）

- `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`
- `grep -rnE '^\s*@Ignore' src/test` → **0 件**、`grep -rn 'charTypeUnknown\|存在しない文字種' src/` → **0 件**
- `git diff --stat 0fd018e..HEAD` が 33 行削除のみ（`src/main` の変更ゼロ）であることを確認。差分を読み、前後のテストを巻き込んでいないこと・未使用 import が除去されていることを確認
- **QA の 2 つの発見を自分で再現**: `BasicJapaneseCharacterInterpreter.java:24` のパターンが `\W+` であること、`nablarch-document@09779f6` が当該文から `（それ以外を指定するとエラーになる）` を削除していることを実物で確認
- **Craft の 13 箇所を自分で数え直した**: `grep -rnoE 'testdata_notation\.rst:1[0-9]{3}(-:[0-9]+)?' src/` を `:1299` 以上で絞り、**ちょうど 13 件**であることを確認

## Overall Verdict

- Self-check: **PASS**（完了条件 4 件すべて OK。`Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS を 2026-08-29 に再実行して確認。カバレッジは行・分岐・命令とも着手前から増減なし。`src/` は 2 軸レビューとも pass のため未変更で、`git diff --numstat 0fd018e..HEAD` は 33 行削除のまま）<br>
  レビュー指摘を受けた本ファイルの記述訂正（コードは無変更）: (1) `unit-test.xml` の存在しないリスト名 `testDataInterpreters` を実在の `interpreters` に訂正、(2) 「本体が `IllegalArgumentException` を投げる」を「今回の入力については真・列挙外一般については偽」と実測つきで書き直し、挙動が入力依存で一貫しないことを削除根拠として位置づけ直し、(3) `nablarch-document@09779f6` と親 `3e01b69` を一次証拠として追記、(4) 直後テストのメソッド名を `buildListMapRows_combinedCharTypeNotationKeepsSeparator` に訂正、(5) 出典行番号のずれを 4 箇所 → **13 箇所**に数え直し、原因（`6ba3c83` の 2 行挿入による +2）・正しい行番号・部分修正が矛盾を生むことを記録、(6) #31 の記録失効の申し送りを追加、(7) 全 `file:line` 参照を 2 巡目で機械的に再検証（不一致 0 件）
- QA: OK（pass。削除の根拠を指示書より強い一次証拠で裏づけ、指示書の過度な一般化も検出。指摘 3 件は Q1〜Q3・Q6 で是正）
- Design expert: N/A（純粋な削除で構造・approach の変更なし。上記「Design と Verification を回さなかった理由」参照）
- Craft expert: OK（pass。取り残しゼロを全件 grep で確認。出典行番号のずれの正確な範囲 13 箇所と原因コミットを特定。指摘 2 件は Q4・Q5 で是正）
- Verification expert: N/A（`@Ignore` 付きテストは未実行でカバレッジ寄与ゼロ。Craft が着手前後の JaCoCo を独立に再計測し全 9 クラス同値を確認済み）
- コーディネータの独立検証: OK（上記「コーディネータの独立検証」節。QA の 2 発見と Craft の 13 箇所を自分で再現）
- Ready to check off: Yes（完了条件 4 項目すべて OK。2 軸とも pass、有効な指摘 7 件を是正、却下 3 件は根拠を記録。`mvn -o clean test` 緑・`Skipped: 0` をコーディネータが独立に実測）
