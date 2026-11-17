# task-40 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| 2文字の `\` ＋ `r` を含む値がデータ行・ディレクティブ・制御ヘッダのいずれでもエラーになる | OK | データ行（テーブル系）: `YamlTableDataBuilder.java:155`。データ行（`list_maps`）: 同 `:204`。データ行（`record_fragment` のセル。ファイル系・電文系の両方が通る）: `YamlFileBuilder.java:253`。ディレクティブ（ファイル系・電文系の両方）: 同 `:290`。制御ヘッダ: `YamlMessageBuilder.java:326`（キー）・`:333`（値）。テストは 19 件: `YamlTableDataBuilderTest.java:637`・`:671`・`:697`・`:727`・`:759`・`:786`・`:813`、`YamlFileBuilderTest.java:1063`・`:1092`・`:1121`・`:1152`、`YamlMessageBuilderTest.java:1896`・`:1926`・`:1958`・`:1987`・`:2017`・`:2047`、`YamlSectionTest.java:271`・`:301`。今回埋めた穴: 電文 `messages` の本文データ行（`:1958`）とディレクティブ（`:1987`）、送信同期 4 セクションの `buildSendSyncList`（`:2017`）と `buildSendSyncBodies`（`:2047`） | | |
| 検査が `YamlSection` の1箇所に置かれ、`interpret` と `convertFwHeader` の両方から通っている | OK | 検査の実体は `YamlSection.java:430`-`:441` の `rejectLiteralCr(String, String)` の 1 メソッドだけ。`grep -rn "rejectLiteralCr" src/main/java` の実コードは 3 行のみ: `YamlSection.java:300`（`interpret` の中。早期 return より**前**）・`YamlMessageBuilder.java:326`・`:333`（`convertFwHeader` の中）。判定文字列は `YamlSection.java:98` の `LITERAL_CR` 1 箇所。**空インタープリタチェーンでも検査が行われる**ことを `YamlSectionTest.java:271`・`:301` が固定した（この経路は `InterpreterResolver.java:54`-`:56` の `raw()` が常に空チェーンを返し、下流の `../nablarch-testing-converter/src/main/java/nablarch/test/core/reader/YamlTestCoreAdapter.java:73` がそれを使う。実物を開いて確認）。出所文字列の組み立ても `YamlSection.java:330` の `entrySource(String, String, String)` 1 箇所に集約し、6 箇所の生成側（`YamlTableDataBuilder.java:102`・`:188`、`YamlFileBuilder.java:88`、`YamlMessageBuilder.java:133`・`:167`・`:208`）をそこへ寄せた | | |
| `"\\n"` と実際の CR（`"\r"`）が通ることのテストがある | OK | `"\\n"`（2 文字）: `YamlTableDataBuilderTest.java:671`・`YamlFileBuilderTest.java:1152`。実際の CR: `YamlTableDataBuilderTest.java:603` `buildListMapRows_yamlEscapeBecomesCr`（旧名 `..._lineSeparatorIsInterpretedOnlyByYamlParser`。「だけ」を担保していないため改名。実測は下記 Verdict 行の M7）。境界も固定した: バックスラッシュと大文字 `R` の 2 文字は**通る**（`YamlTableDataBuilderTest.java:759`）、バックスラッシュ 2 つ＋`r` の 3 文字（YAML に `"\\\\r"` と書いた値）は**拒否される**（同 `:727`）。マーカーカラムの値が検査対象外であることも同 `:786` で固定した | | |
| 例外メッセージに値と出所が入ることを assert している | OK | 異常系 12 件がいずれも `containsString("value=[...]")` と `containsString("source=<セクション> entry <table/path/id>='...'")` の 2 本を assert する。判定が部分一致（`contains`。完全一致・前方一致・後方一致への退化を検知）であることは `YamlTableDataBuilderTest.java:697`（`value=[AB\rCD]`）と `YamlFileBuilderTest.java:1092` が担保する。**例外メッセージの文面を直した**: 従来は推奨形も拒否値も画面上どちらも `\r` に見えたため、YAML ソース表記で言い分ける形（`write it in the YAML source as "\r" (one backslash inside double quotes), not as "\\r" (two backslashes).`）にした。`YamlTableDataBuilderTest.java:637` がこの文面を assert する | | |
| 既存フィクスチャ・テスト1件が解説書に合わせて直されている | OK（着手時の記録のとおり。今回は追加のみで、既存テストの期待値は変えていない） | 着手時の内訳は下記「Steps C の実測（指示との食い違い）」節のまま。今回追加したフィクスチャ: `nativeTypes.yaml:30`（`literalCrInsideTest`）・`:37`（`escapedBackslashCrTest`）・`:43`（`upperCaseRTest`）・`:49`（`markerLiteralCrTest`）、`fileData.yaml:487`（`literalCrInsideRow`）、`messageData.yaml:77`（`literalCrInMessageBody001`）・`:88`（`literalCrInMessageDirective001`）・`:185`（`literalCrSendSync`）・`:198`（`literalCrSendSyncBodies`）。`nativeTypes.yaml` の `literalCrTest` に付いていた「読み込むとエラーになるため」というコメントは**不正確だったので直した**。`YamlLoader.load` 自体は成功する（同じファイルを読む他テストが緑）。エラーになるのはそのエントリを組み立てたとき（`buildListMapRows(yaml, "literalCrTest", ...)`）である | | |
| 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 隔離コピー（`git worktree add --detach <scratchpad>/wt-t40-mut-9c4e1b HEAD` ＋ 作業ツリーの `git diff` を `git apply`）で 9 通りの変異を `mvn -o clean test` で実測した。終了後 `git worktree remove --force` 済み。**M1**（`YamlMessageBuilder.java:167`・`:208` の `source` 式をリテラルに置換）→ `buildSendSyncList_literalBackslashRInRowThrows`・`buildSendSyncBodies_literalBackslashRInRowThrows` の 2 件だけが落ちる。**M2**（`YamlSection.interpret` の `rejectLiteralCr` を空チェーン早期 return の後ろへ移す）→ `interpret_emptyInterpretersStillRejectsLiteralCr`・`interpret_nullInterpretersStillRejectsLiteralCr` の 2 件だけが落ちる。**M3**（`contains` → `equals`）→ 4 件（`buildListMapRows_literalBackslashRInsideLongerValueThrows`・`buildFileList_literalBackslashRInsideLongerValueInRowThrows`・`buildListMapRows_escapedBackslashFollowedByRThrows`・`buildMessagePool_literalBackslashRInFwHeaderKeyThrows`）が落ちる（着手前は 1 件だけだった）。**M4**（`YamlMessageBuilder.java:133` の本文組み立てへ渡す `source` だけをリテラルに置換。`fw_header` 側は無変更）→ `buildMessagePool_literalBackslashRInMessageBodyRowThrows`・`buildMessagePool_literalBackslashRInMessageDirectiveThrows` の 2 件だけが落ちる。**M5**（`value.contains` → `value.toLowerCase().contains`）→ `buildListMapRows_backslashUpperCaseRIsKeptAsIs` の 1 件だけが落ちる。**M6**（`buildListMapRows` からマーカー除外 `if (isMarker(col)) continue;` を削除）→ `buildListMapRows_literalBackslashRInMarkerColumnIsNotChecked` が `IllegalStateException` で落ちる（ほかに既存のマーカー系 5 件も落ちる）。**M7**（`unit-test.xml` の `yamlInterpreters` 先頭に `LineSeparatorInterpreter` を追加）→ 落ちるのは `YamlTestDataParserTest.yamlInterpretersAreOnlyDocumentedTwo` の **1 件だけ**で、`YamlTableDataBuilderTest` 67 件は全緑（改名した `buildListMapRows_yamlEscapeBecomesCr` を含む）。旧テスト名の「Only／だけ」が過剰主張だったことの実測。**M8**（`YamlSection.entrySource` の書式を変更）→ 22 件が落ちる。書式を集約したあとも既存 6 件の逐語 assert が生成側とつながっていることの裏づけ。**M9**（例外メッセージを旧文面へ差し戻す）→ `buildListMapRows_literalBackslashRThrowsException` の「推奨形と拒否値を YAML ソース表記で言い分けていること」だけが落ちる。殺せなかった変異は無い | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 319, Failures: 0, Errors: 0, Skipped: 1` / `[INFO] BUILD SUCCESS`（着手前 307 件から 12 件増。`Skipped: 1` は #41 担当の既存 `@Ignore`。そのまま残し、新しい `@Ignore` も足していない）。`mvn -o javadoc:javadoc` も `BUILD SUCCESS` で参照解決の警告なし。今回書いた `file:line` 参照はすべて開いて内容が主張と合うことを確認した。不一致は 1 件見つけて直した（`entrySource` の javadoc が `YamlFileBuilder` の入口を `buildFileList` と書いていたが実名は `buildDataFileList`） | | |

## 一次情報の確認（自分で開いて確かめた）

- 解説書（ピン `afa4f9e`）`ja/development_tools/testing_framework/implementation/testdata_notation.rst` の該当セル（`git show afa4f9e:<path> \| sed -n '1445p'`）:
  > YAML のパーサが制御文字に変換する。バックスラッシュと ``r`` の2文字（``"\\r"``）を含む値は書けない。Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値はテスティングフレームワークの仕様上存在せず、YAML 形式ではエラーになる。``"\\n"`` は Excel 形式と同じく2文字のまま残る
  - 節見出しの特定: `awk` で `NR<=1420` の見出しを列挙 → 直近の親節が `1309: null・空文字・改行など特殊な値を記述する`、その下の `1399: YAML形式の場合`。javadoc にはこの 2 段の見出しと引用文で出典を書いた（行番号は書いていない）
- 同 `setup/common.rst`（`sed -n '77p'`）:
  > ``yamlInterpreters`` に指定するのは、この2つだけでよい。null ・空文字・ダブルクォート・改行文字は YAML のパーサが構文として解釈するため、Excel 形式で必要な ``NullInterpreter`` ・ ``QuotationTrimmer`` ・ ``LineSeparatorInterpreter`` は指定しない。
  - 節見出しの特定: 同じ `awk` で `37: テストデータの形式をYAMLに変更する`。javadoc にはこの見出しと引用文で書いた
- 本体 `../nablarch-testing/.../LineSeparatorInterpreter.java:31` = `private static final String DEFAULT_PATTERN = "\\\\r";`、`:34` = `private static final String DEFAULT_LINE_SEP = LineSeparator.CR.toString();`、`:44`-`:48` の `interpret` が `replaceLineSeparator(orig)` を呼ぶ。本体は参照のみ（書き込みなし）
- YAML パーサが `"\\r"` をどう読むか: ダブルクォート文字列の `\\` はバックスラッシュ 1 文字にほどけるため、値は **バックスラッシュ＋`r` の 2 文字**になる。実測は変更前の既存テスト `YamlTableDataBuilderTest#buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser` が `assertThat(..., is("\\r"))` で緑だったこと（着手前ベースライン 299 件緑）。`~` のような別解釈は入らない
- 既存の例外の型とメッセージの形（揃える先）: `YamlFileBuilder.java:81`（`"Missing required field 'path' in " + sectionKey + " entry. groupId=..."`）・`YamlTableDataBuilder.java:86`（同型）・`YamlMessageBuilder.java` の `convertFwHeader`（`"fw_header in message entry id='...' must be a map, but was: ..."`）。いずれも `IllegalStateException` で、英文 1〜2 文＋`key=value` の列。本タスクの例外もこれに揃えた

## 3 経路（データ行・ディレクティブ・制御ヘッダ）の追跡（実測）

`grep -rn "interpret(\|objectToString(\|toStr(" src/main/java` で全呼び出しを列挙し、各経路を追った。

| 経路 | 値になるまでに通るメソッド | 検査が載るか |
|---|---|---|
| データ行（`setup_tables`／`expected_tables`／`expected_complete_tables`） | `YamlTableDataBuilder.buildTableDataList` → `extractRows`（`objectToString`）→ `buildTableData` → `interpret`（`:155`） | 載る |
| データ行（`list_maps`） | `buildListMapRows`（public）→ `extractRows` → `buildListMapRows`（private）→ `interpret`（`:204`） | 載る |
| データ行（`record_fragment` の `rows`。ファイル系・電文系の両方） | `YamlFileBuilder.buildFragmentsInternal` → `interpret(objectToString(cell), ...)`（`:253`） | 載る |
| ディレクティブ（ファイル系・電文系の両方） | `mapDirectives`（`toStr`）→ `applyDirectives` → `interpret`（`:290`） | 載る |
| 制御ヘッダ（`messages` の `fw_header:`） | `convertFwHeader` → `objectToString` のみ（解釈しない） | `rejectLiteralCr` を直接呼ぶ（`:315` キー・`:322` 値） |
| 制御ヘッダ（`expected_request_header_messages` 等の 4 セクション） | ヘッダ項目も `records` の `fields`／`rows` に書く（`fw_header:` を使わない）→ 上の「データ行（`record_fragment`）」と同じ経路 | 載る |

**結論**: Steps A の「`interpret` と `convertFwHeader` の両方から通す」で 3 経路すべてを覆えた。`interpret` の 4 呼び出し（`YamlTableDataBuilder:155`・`:204`、`YamlFileBuilder:253`・`:290`）がデータ行とディレクティブを、`convertFwHeader` が FW 制御ヘッダを覆う。

**覆っていない値（意図的。javadoc `YamlSection.java:337`-`:344` に記載）**:

- **マーカーカラム（`[COL]`）の値**。`YamlTableDataBuilder.java:141`-`:146` が `interpret` の前に非マーカー列だけを `dataColumnIndexes` に集め、`:201`-`:203` が `isMarker(col)` で `continue` するため、マーカー列の値は `interpret` に渡らない。マーカー列の値は DB 操作にも突合にも使われず捨てられるので対象外でよい
- **スキーマ構造値**（`type:`・`record_type:`・`path:`・`table:`・`id:`・`group_id:`・`fields` の `name`/`type`/`length`）。これらは `toStr` を通り `interpret` を通らない。解説書が「値」として扱っていない設定値であり、本タスクの対象（データ行・ディレクティブ・制御ヘッダの値）ではない

## 設計の判断（例外に出所を入れるために何を渡すか）

- **採った案**: 出所を組み立てるのは**エントリのループ 1 箇所**とし、組み立てた `String source` を 1 引数として下へ通す。`interpret(String, List, String)` に第 3 引数を足し、`YamlFileBuilder.buildFragmentsForFile`／`buildFragmentsForMessage`／`buildFragmentsForSendSync`／`buildFragmentsInternal`／`applyDirectives`、`YamlMessageBuilder.buildMessageBodyFile`／`buildSendSyncBodyFile`／`buildSendSyncFile`、`YamlTableDataBuilder.buildTableData`／`buildListMapRows`(private) に同じ引数を足した（計 11 シグネチャ）
- **理由**: セクションキーは `interpret` の呼び出し地点には無く、エントリのループにしか無い。セクションキーを落とすと `setup_files` と `expected_files` に同じ `path` を書いた場合などに出所が一意にならない。また既存の例外（`YamlFileBuilder.java:81`・`YamlTableDataBuilder.java:86`）がいずれもセクションキーを入れており、揃える先がそうなっている。引数の追加は既に `interps` を同じ形で通しているのと同じ流儀で、増えるのは 1 つだけ
- **検討して採らなかった案**: (a) `file.getPath()`（ファイルは `path`、電文は `id` が入っている）と `tableName` をその場で使い引数を増やさない案 — 引数は増えないがセクションキーが落ちる。(b) `interps` と `source` を 1 つの値型にまとめる案 — シグネチャ数は減るが新しいクラスが増え、既存の `interps` を通す流儀から外れる
- `convertFwHeader` は `id` 引数を `source` 引数に**置き換えた**（両方は持たせていない）。既存の 2 つの例外メッセージも `"fw_header in " + source + " ..."` に変わったが、既存テストの assert は `containsString("must be a map, but was: X")`・`containsString("has unknown key 'K'")`・`containsString("id='req001'")` であり、いずれも新しいメッセージにも含まれるため既存テストは変更していない（`YamlMessageBuilderTest` は 52 → 54 件で、増分は新規 2 件だけ）
- `convertFwHeader` のキー検査は許可キー判定の**前**に置いた。この 2 文字を含むキーは許可キー（既定は `requestId`・`userId`・`resendFlag`・`resultCode`）に一致しえないため、後ろに置くと到達しない。変異 M5 で実測（下記）

## Steps C の実測（指示との食い違い）

指示は「該当はフィクスチャ1件・テスト1件」。**フィクスチャは 1 件で一致したが、テストは 3 件落ちた**。

`grep -rn '\\\\r' src/test/ src/main/` の全ヒット（3 ファイル）:

1. `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/nativeTypes.yaml:16` の `LITERAL_CR_COL: "\\r"` — **該当**
2. `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java` の 5 行（`:590`・`:591`・`:593`・`:606`・`:609`）— javadoc と assert。うち値の期待値は `:609` の `is("\\r")` 1 件
3. `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:293` — JSON 文字列 `"\"\\r\\n\""`。JSON をほどくと `"\r\n"` という**説明文**であり、テストデータの値ではない。スキーマは #42 の担当なので触っていない

加えて、YAML の素のスカラでバックスラッシュ＋`r` を 1 個だけ書いた箇所が無いことも `grep -rn '\\r' src/test/ --include=*.yaml --include=*.yml` で確認した（ヒットは `nativeTypes.yaml:16`・`:17` と、`"\r\n"`（実 CR+LF に展開される）の 4 件だけ）。

**落ちた既存テスト（実測・全件 3 件）**。3 件とも `nativeTypes.yaml` の `interpreterTest` エントリ（`LITERAL_CR_COL` を含む同じ 1 行）を読むため巻き添えで落ちた:

| テスト | 落ちた理由 | 対応 |
|---|---|---|
| `YamlTableDataBuilderTest.buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser`（旧 `:597`） | `LITERAL_CR_COL` を期待値に持っていた（旧 `:609` の `is("\\r")`） | **期待値を変えた（1 件）**。`LITERAL_CR_COL` の assert を削除し、`YAML_CR_COL` が CR 1 文字であることだけを固定する形に直した。エラーになることの担保は新設の `buildListMapRows_literalBackslashRThrowsException` が持つ |
| `YamlTableDataBuilderTest.buildListMapRows_quotedNullIsKeptAsString`（`:553`） | 同じエントリを読むだけで、`LITERAL_CR_COL` は assert していない | **期待値を変えていない**。フィクスチャから `LITERAL_CR_COL` を別エントリ（`literalCrTest`）へ切り出しただけで緑に戻った |
| `YamlTableDataBuilderTest.buildListMapRows_spaceBetweenQuotesIsSpace`（`:578`） | 同上 | **期待値を変えていない**（同上） |

**変えた/変えなかった内訳**: 落ちた 3 件のうち、期待値を変えたのは 1 件、フィクスチャの切り出しだけで緑に戻したのが 2 件。

## フィクスチャの変更（実測）

| ファイル | 変更 |
|---|---|
| `YamlTableDataBuilderTest/nativeTypes.yaml` | `interpreterTest` から `LITERAL_CR_COL: "\\r"` を削除（`:16`）。単独エントリ `literalCrTest`（`:23`-`:25`）と `literalLfTest`（`:28`-`:30`）を新設。エラーになるエントリを他テストと同居させないため |
| `YamlTableDataBuilderTest/tableData.yaml` | `setup_tables` に `group_id: literalCrInTable`（`:192`-）を追加（`VARCHAR2_COL: "\\r"`） |
| `YamlFileBuilderTest/fileData.yaml` | `expected_files` に `literalCrInRow`（`:472`-）・`literalCrInDirective`（`:487`-）・`literalLfInRow`（`:502`-）の 3 グループを追加 |
| `YamlMessageBuilderTest/fwHeaderMapData.yaml` | `messages` に `literalCrInFwHeaderValue001`（`:57`-）・`literalCrInFwHeaderKey001`（`:70`-）の 2 エントリを追加 |

## 追加したテスト（8 件）と変更したテスト（1 件）

| テスト | 経路 | 期待 |
|---|---|---|
| `YamlTableDataBuilderTest.java:625` `buildListMapRows_literalBackslashRThrowsException` | データ行（`list_maps`） | `IllegalStateException` ／ `value=[\r]` ／ `source=list_maps entry id='literalCrTest'` |
| `YamlTableDataBuilderTest.java:680` `buildTableDataList_literalBackslashRThrowsException` | データ行（テーブル系。`list_maps` とは別の `interpret` 呼び出し） | `IllegalStateException` ／ `value=[\r]` ／ `source=setup_tables entry table='TEST_TABLE'` |
| `YamlTableDataBuilderTest.java:655` `buildListMapRows_literalBackslashNIsKeptAsIs` | データ行（正常系） | エラーにならず `is("\\n")` |
| `YamlFileBuilderTest.java:1063` `buildFileList_literalBackslashRInRowThrowsException` | データ行（`record_fragment`） | `IllegalStateException` ／ `value=[\r]` ／ `source=expected_files entry path='dummy/literal_cr_row.csv'` |
| `YamlFileBuilderTest.java:1092` `buildFileList_literalBackslashRInDirectiveThrowsException` | ディレクティブ | `IllegalStateException` ／ `value=[\r]` ／ `source=expected_files entry path='dummy/literal_cr_directive.csv'` |
| `YamlFileBuilderTest.java:1123` `buildFileList_literalBackslashNInRowIsKeptAsIs` | データ行（正常系） | エラーにならず `getString("FIELD1")` が `is("\\n")` |
| `YamlMessageBuilderTest.java:1896` `buildMessagePool_literalBackslashRInFwHeaderValueThrows` | 制御ヘッダ（値） | `IllegalStateException` ／ `value=[\r]` ／ `source=messages entry id='literalCrInFwHeaderValue001'` |
| `YamlMessageBuilderTest.java:1926` `buildMessagePool_literalBackslashRInFwHeaderKeyThrows` | 制御ヘッダ（キー） | `IllegalStateException` ／ `value=[req\rId]` ／ `source=messages entry id='literalCrInFwHeaderKey001'` ／ `has unknown key` **でない**こと |
| `YamlTableDataBuilderTest.java:597` `buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser`（**変更**） | 実 CR が通ること | `YAML_CR_COL` が `is("\r")` |

`YamlSectionTest` の `interpret` 3 件（`interpret_nullInterpretersReturnsValueAsIs`／`interpret_emptyInterpretersReturnsValueAsIs`／`interpret_nullValueReturnsNull`）と `YamlFileBuilderTest` の `buildFragmentsFor*` 5 呼び出しは、シグネチャ変更に伴う引数追加のみ（期待値は変えていない）。

## 変異確認（実測）

隔離コピーで実行した。`git worktree add --detach <scratchpad>/ntf40-mutation-wt HEAD` → 作業ツリーの `src/` を `rsync` で写す → 変異を当てて `mvn -o clean test`。終了後 `git worktree remove --force` 済み（`git worktree list` は本体 1 件のみ）。

| 変異 | 内容 | 結果（実測） |
|---|---|---|
| M1 | `YamlSection.rejectLiteralCr` の先頭に `if (true) { return; }` を入れ検査を無効化 | `Tests run: 307, Failures: 5, Errors: 1` — 異常系 6 件すべてが落ちる（`buildFileList_literalBackslashRInRowThrowsException`・`buildFileList_literalBackslashRInDirectiveThrowsException`・`buildMessagePool_literalBackslashRInFwHeaderValueThrows`・`buildMessagePool_literalBackslashRInFwHeaderKeyThrows`・`buildListMapRows_literalBackslashRThrowsException`・`buildTableDataList_literalBackslashRThrowsException`） |
| M2 | `LITERAL_CR` を `"\\r"` → `"\\n"` に変える | `Failures: 5, Errors: 3` — 異常系 6 件に加え、正常系の `buildListMapRows_literalBackslashNIsKeptAsIs`・`buildFileList_literalBackslashNInRowIsKeptAsIs` の 2 件も落ちる（合計 8 件＝追加テスト全件） |
| M3 | 出所の組み立てから `sectionKey`／`KEY_LIST_MAPS` を落とす（6 箇所） | `Failures: 6` — 異常系 6 件が**「出所…がメッセージに含まれること」の assert で**落ちる |
| M4 | 例外メッセージから `value=[" + value + "], ` を落とす | `Failures: 6` — 異常系 6 件が**「値がメッセージに含まれること」の assert で**落ちる |
| M5 | `convertFwHeader` のキー検査を許可キー判定の**後ろ**へ移す | `Failures: 1` — `buildMessagePool_literalBackslashRInFwHeaderKeyThrows` だけが落ちる（`has unknown key` の例外になり `value=[req\rId]` が出ない）。「許可キー判定より前に置く」という主張の裏づけ |
| M6 | `nativeTypes.yaml` の `YAML_CR_COL: "\r"` を `"\n"` に変える | `Failures: 1` — 変更したテスト `buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser` だけが落ちる（`YAML のエスケープ "\r" は CR 文字になること`）。残した assert が効いていることの裏づけ |

追加 8 件は M1／M2 で、変更 1 件は M6 で落ちる。出所・値の assert はそれぞれ M3・M4 で落ちる。

## カバレッジ（JaCoCo・実測）

`task-38.md` と同じ手順で計測した。

1. `rm -f jacoco.exec`
2. `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
3. `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o org.jacoco:jacoco-maven-plugin:0.8.8:restore-instrumented-classes org.jacoco:jacoco-maven-plugin:0.8.8:report -Djacoco.dataFile=jacoco.exec`
4. `target/site/jacoco/jacoco.xml` を機械的に集計

`src/main` 全体（9 クラス）: **行 99.0%（408/412）・分岐 98.9%（174/176）・命令 99.3%（1797/1810）**

| クラス | 行 | 分岐 |
|---|---|---|
| **`YamlSection`（検査の置き場）** | **52/52（100%）** | **50/50（100%）** |
| **`YamlMessageBuilder`（変更先）** | **85/85（100%）** | **38/38（100%）** |
| **`YamlTableDataBuilder`（変更先）** | **67/67（100%）** | **30/30（100%）** |
| **`YamlFileBuilder`（変更先）** | **85/86** | **41/42** |
| `YamlTestDataParser` | 56/56 | 2/2 |
| `YamlLoader` | 48/51 | 13/14 |
| `YamlSchemaValidationException` | 7/7 | 分岐なし |
| `InterpreterResolver` | 2/2 | 分岐なし |
| `MessageContent` | 6/6 | 分岐なし |

未到達は 2 箇所だけで、いずれも本タスクと無関係な既存の防御的コードである（`jacoco.xml` の `mi>0 or mb>0` を機械的に抽出）。

- `YamlFileBuilder.java:246`-`:247` — `if (!(rowObj instanceof List)) { continue; }`。コメントに「Java 言語仕様上この分岐は通常到達不能」と既に書かれている防御的ガード
- `YamlLoader.java:60`-`:61`・`:65`-`:66` — スキーマファイルがクラスパスに無い場合／スキーマのロードに失敗した場合の分岐

`rejectLiteralCr` は行・分岐とも全到達（`YamlSection` が 52/52・50/50）。

## javadoc の検証

`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o javadoc:javadoc` → `BUILD SUCCESS`、警告・エラーなし（`grep -E "警告:|エラー|error:"` のヒットは「名前のないモジュール」の 1 件のみで、これは既存の環境由来）。今回書いた `{@link}`（`YamlSection#rejectLiteralCr(String, String)`・`YamlSection#interpret(String, List, String)`・`nablarch.test.core.util.interpreter.LineSeparatorInterpreter`・`IllegalStateException`・`YamlSection#isMarker(String)`・`YamlSection#toStr(Object)`）はすべて解決している。今回 `file:line` 形式の参照は 1 つも書いていない（出典はすべて節見出し＋引用文）。

## スコープ外として触っていないもの

- #41（既存 `@Ignore` 1 件の削除）— `YamlTableDataBuilderTest` の `@Ignore` はそのまま。最終ビルドの `Skipped: 1` がそれ。新しい `@Ignore` も足していない
- #42（スキーマ `description` の追随）— `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` は無変更（`git status --short` に出ない）
- `.rn/ntf-yaml/steering.md` — 無変更（着手前から `M` の状態。コーディネータの持ち物）
- 本体 `../nablarch-testing` — 参照のみ（`LineSeparatorInterpreter.java`・`DataFile.java` を読んだだけ）

## QA / Expert Review

`476672d` について QA・Design・Craft・Verification の 4 軸を独立したサブエージェントとして実施した。
各エキスパートには成果物と完了条件とチェックリストだけを渡し、このチェックファイル・実装エキスパートの
サマリ・他軸の判定は渡していない。変異確認は一意な名前の隔離ワークツリーで行うことを義務づけた。

| 軸 | 判定 | 指摘 |
|---|---|---|
| QA | **pass** | 非ブロッキング 4 件（F1-F4） |
| Design | **pass** | 生き残る変異 2 件（M-A・M-B） |
| Craft | **pass** | 非ブロッキング 8 件（F1-F8） |
| Verification | **pass** | 生き残る変異 1 件（X9）＋カバレッジの抜け 7 件 |

**4 軸すべてが「実測で偽と判明した記述は 0 件」と報告した。**#38（3 ラウンド）・#39（2 ラウンド）で
繰り返し落ちた「修正が新しい虚偽記述を混入させる」型は、このタスクでは 1 件も起きていない。
落ちた軸が無いため、残る指摘はすべて **「実装は正しいがテストが固定していない」** 型である。

### 4 軸が独立に収束した点

| 指摘 | 検出した軸 | 実測した反証 |
|---|---|---|
| `interpret` の javadoc「インタープリタが 1 つも渡されない場合も検査は行う」を固定するテストが無い | Design（M-B）・QA（F2）・Craft（F2） | 検査を空チェーン早期 return の後ろへ移しても全 307 件緑。しかも `InterpreterResolver.raw()` は常に空チェーンを返し、下流の変換ツールがこれを使う**生きた経路** |
| `contains`（部分一致）を固定しているのが `fw_header` キーのテスト 1 件だけ | 4 軸すべて | `contains` を `equals`／`startsWith`／`endsWith` のいずれに退化させても、落ちるのは同じ 1 件（値が `req\rId` で埋め込みだから）。異常系の他 5 件は値がちょうど 2 文字なので通ってしまう |
| 送信同期 2 経路の `source` が完全に無検査 | Verification（X9） | `YamlMessageBuilder` の該当 2 行の `source` 式を意味のないリテラルに置き換えても全 307 件緑 |
| テスト名 `..._lineSeparatorIsInterpretedOnlyByYamlParser` が過剰主張 | Craft（F1） | 「Only」を担っていた assert を削ったため、`yamlInterpreters` に `LineSeparatorInterpreter` を足す変異でもこのテストは落ちない（落ちるのは別テスト 1 件） |
| キー検査の非対称（`fw_header` キーは検査するがカラム名・ディレクティブキーは検査しない）が javadoc に無い | QA（F3）・Verification | `list_maps` に `"A\rB": "ok"` と書くと素通りすることを QA が実測 |

### 軸ごとの評価

| 軸 | 観点 | 判定 | 根拠 |
|---|---|---|---|
| QA | 検証のやり方が目的に対して意味を持つか | OK | 探査テストで**値の入口 12 経路すべて**を叩き、各経路で `IllegalStateException` と出所文字列を実測。境界 16 パターンの実挙動も実測して仕様と突き合わせた |
| Design | アプローチ・構造が適切か／責務の分離 | OK | 11 メソッドへの `source` 引き回しを、代替案（例外を上位で捕まえて出所を足す／値オブジェクト導入）と比較して妥当と判定。検査を**インタープリタ適用の前**に置いたことが本質で、後ろに置くと `${binaryFile:}` が読んだファイルの中身や `${半角英字,10}` の生成値まで検査対象になる |
| Design | システム全体の整合性 | OK | 公開 API の破壊的変更（`interpret` の引数追加）が下流に影響しないことを、依存 4 リポジトリ全件の `grep` で確認（`YamlSection.interpret` の呼び出しは 0 件）。**オーバーロードで旧シグネチャを残すのは誤り**（検査を通らない経路が公開 API に残るため）と判定。スキーマ全 437 行を走査し、この 2 文字について述べた `description` が 1 つも無い＝#42 に追加項目は生じないことを確認 |
| Craft | 慣行・エラー処理・null・命名・重複 | OK | `src/main/java` の `throw new` を全 12 箇所収集し、新規メッセージが既存の流儀（英語・`IllegalStateException`・末尾に `key=value` の文脈）に揃っていることを確認。解説書・`common.rst`・本体 `LineSeparatorInterpreter` からの引用が**すべて逐語一致**することを確認 |
| Craft | 既存コードベースの流儀との一貫性 | OK | 出典の書き方（節見出し＋引用文）が先例と一致。`mvn -o clean javadoc:javadoc` の警告数が着手前と同数（隔離コピー 2 本で比較実測） |
| Verification | 成果物が実際に検査されているか | OK | 20 通りの変異を実測し 19 が殺された。生存 1 件（X9）は実装が正しいことをプローブで確認済み |
| Verification | 網羅 | OK | 境界 16 パターン・経路 12 種を実測。実装はすべて解説書どおりに振る舞う |

### トリアージ

有効と判定して直したもの: 13 件（N1〜N13）。内訳はテストの穴 6 件（N1〜N6）と記述の精度 7 件（N7〜N13）。

却下したもの（根拠つき）:

| 指摘 | 却下の根拠 |
|---|---|
| スキーマの `description` との食い違い | Design が全 437 行を走査し、この 2 文字について述べた `description` は 1 つも無いことを確認。#42 に追加項目は生じない |
| `buildFragmentsInternal` の boolean 3 連引数を搬送型にまとめる | 着手前からある負債で完了条件外 |
| `interpret` をパッケージプライベートに落とす | 今回の破壊的変更のついでに可能だが、完了条件外 |

### コーディネータの独立検証（`e3a4c1f`）

エキスパートと実装エキスパートの報告を鵜呑みにせず、自分で次を実測した。

- `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 319, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`（着手前 299 → +20）
- **N2 の変異を自分で当て直した**: 隔離ワークツリーで `rejectLiteralCr` の呼び出しを空チェーン早期 return の後ろへ移すと、`interpret_emptyInterpretersStillRejectsLiteralCr` と `interpret_nullInterpretersStillRejectsLiteralCr` の**ちょうど 2 件**が落ちる（是正前は全緑だった）
- 公開 API の破壊的変更が `YamlSection.interpret` の 1 件だけであることを `git diff` の宣言行抽出で確認。下流 `nablarch-testing-converter` が `YamlSection` から使うのが `castMap`／`dataTypeToSectionKey`／`getList`／`groupMatches`／`isMarker`／`resolveColumns`／`toStr` と定数だけで、`interpret` を呼んでいないことを確認
- N11 の `entrySource` が 6 箇所すべてを集約していることを確認（`YamlSection.java:330` に定義、呼び出しは `YamlFileBuilder.java:88`・`YamlMessageBuilder.java:133`／`:167`／`:208`・`YamlTableDataBuilder.java:102`／`:188`）
- N12 の例外メッセージが YAML ソース表記で言い分ける形（`(one backslash inside double quotes), not as "\\r" (two backslashes)`）になっていることを確認
- N8 の判断が実物に基づいて説明され、未検査のカラム名も明示的に開示されていることを確認
- 解説書 `testdata_notation.rst:1445`・`setup/common.rst:77`（ピン `afa4f9e`）と本体 `LineSeparatorInterpreter.java:31` を開き、実装が SSoT と一致することを確認

### #42・#43 への申し送り

- **#42**: このタスクによるスキーマ `description` の追加是正項目は**無い**（Design が全 437 行を走査して確認）
- **#43 Step C**: converter のフィクスチャに `\` ＋ `r` の 2 文字は 1 件も無く（`grep -rn -F` で 0 件）、Excel → YAML 変換もこの 2 文字を出力しえない（`XlsFormatReader` が全データセルに `LineSeparatorInterpreter` を掛けるため）。**この是正で converter が落ちる要因にはならない見込み**（静的走査による見込みで未確認）
- **#43 Step C（converter 側の既存の乖離。この是正の責任ではない）**: converter の `stripQuotes` はディレクティブ値に `QuotationTrimmer` しか掛けないのに対し、本体の Excel 経路は行の全セルに `interpret` を掛ける。よって「`\r` と書かれた Excel のディレクティブセル」は本体では CR になるのに converter では 2 文字のまま YAML に書き出される。今回の是正はその読み戻しを**黙って誤変換していた状態から明示的なエラーに変える**（改善方向）。現行フィクスチャに該当が無いのでテストは落ちない

## Overall Verdict

- Self-check: **PASS**（完了条件 7 件すべて OK。`Tests run: 319, Failures: 0, Errors: 0, Skipped: 1` / BUILD SUCCESS。M1〜M9 の 9 変異をすべて隔離コピーで実測し、殺せなかったものは無い）
  - **N8 の判断（キー検査を残すか外すか）: `fw_header` のキー検査を残す。** 根拠は 3 点で、すべて実物で確認した。(1) Excel 形式ではキーも値も同じセルであり、本体 `../nablarch-testing/src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:183` の `List<String> interpret = interpret(line);` が読み込んだ行の全セルをまとめて `interpret` に通す。名前を書いたセルもここを通るため、Excel 形式ではキーに書いたこの 2 文字も必ず CR になる。よって値と同じ理由でキーにも書けない。(2) `fw_header` のキーはスキーマ `$defs.fw_header` が `additionalProperties: {"type":"string"}` で任意キーを許すためスキーマ検証では止まらず、さらに許可キー集合は `reader.fwHeaderfields` で差し替えられる（`NablarchTestUtils.makeArray` = `COMMA.split(str)` で空白トリムなし。`../nablarch-testing/src/main/java/nablarch/test/NablarchTestUtils.java:45`-`:49`）。この 2 文字を含む名前を許可キーに設定できてしまうので、止められるのはこの検査だけである。(3) ディレクティブのキーは検査しない。スキーマ `$defs.directives` が `additionalProperties: false` で閉じており（`directives:` が現れる 4 箇所すべてがこの `$ref` を使うことを機械的に確認）、この 2 文字を含むキーは `YamlLoader.load` のスキーマ検証で落ちるため、Java 側に検査を置いても到達しない。カラム名（テーブル系・`list_maps` の `rows` のキー）は**意図的に対象外**とした。解説書がこの 2 文字について定めているのは「値」であり、完了条件もそれに合わせているため。素通りすること自体は QA の実測どおりで、既知の非対称として `YamlSection.java` の `rejectLiteralCr` javadoc に明記した（同 javadoc に上記 (1)〜(3) も書き、`YamlMessageBuilder.convertFwHeader` の javadoc からその節を指している）
  - **`convertFwHeader` の既存例外文が変わった件（`fw_header in message entry id='X'` → `fw_header in messages entry id='X'`）: 意図的な変更として維持する。** 根拠は、本リポジトリの他の例外文がすべてセクションキーを逐語で使っていること（`source=list_maps entry id='...'`・`setup_tables entry table='...'`・`expected_files entry path='...'`）。旧文の `message` はどのセクションキーとも一致しない散文の語であり、`convertFwHeader` が呼ばれるのは `messages` セクション経路だけ（`YamlMessageBuilder.java:137`-`:139` の `useFwHeader` 分岐）なので、`messages` の方が事実として正確でもある。他の例外文と揃う方向の変更である。既存テストの assert は `containsString("id='req001'")` 等で、いずれも新文面にも含まれるため破れていない
  - **`\R`（大文字）が通り `\\r`（3 文字）が拒否される根拠**: 大文字は本体 `../nablarch-testing/src/main/java/nablarch/test/core/util/interpreter/LineSeparatorInterpreter.java:31` の `private static final String DEFAULT_PATTERN = "\\\\r";` が小文字の `r` だけを対象にしており、Excel 形式でも CR にならず 2 文字のまま残るため、YAML 形式でも書けてよい。3 文字（YAML に `"\\\\r"` と書いた値 = バックスラッシュ 2 つ＋`r`）は判定が部分一致で後ろ 2 文字が一致するため拒否される。どちらも先に実挙動をテストで確かめてから javadoc を書いた（`YamlTableDataBuilderTest.java:759`・`:727`）
- QA: OK（pass。偽の記述 0 件。値の入口 12 経路すべてで実測。指摘 4 件は N3・N7・N8 で是正）
- Design expert: OK（pass。11 メソッドへの `source` 引き回しと公開 API の破壊的変更をいずれも妥当と判定。生き残る変異 2 件は N2・N3 で是正）
- Craft expert: OK（pass。**3 タスク目にして初めて虚偽記述 0 件**。指摘 8 件は N7・N9〜N13 で是正）
- Verification expert: OK（pass。20 変異中 19 が殺された。生存 1 件（送信同期の `source`）は N1 で是正）
- コーディネータの独立検証: OK（上記「コーディネータの独立検証」節。N2 の変異を自分で当て直して 2 件が殺されることを実測）
- Ready to check off: Yes（完了条件 7 項目すべて OK。4 軸とも pass、有効な指摘 13 件を 1 ラウンドで是正、却下 3 件は根拠を記録。`mvn -o clean test` 緑をコーディネータが独立に実測）
