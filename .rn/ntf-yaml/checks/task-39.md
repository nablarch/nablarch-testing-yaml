# task-39 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `isBlankRow` が空マッピング（値を1つも持たない行）だけを真とし、Java null・`""` をどちらも非空として扱う | OK | `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:227`-`:229` が `return castMap(row).isEmpty();` の 1 行（値を一切見ない）。`castMap`（`:116`-`:121`）が Map でない値に空 Map を返すのでスカラ行も同じ判定で落ちる。判定は `isBlankRow` の 1 箇所に閉じており、`dropBlankRows`（`:205`-`:213`）も `resolveColumns`（`:247`-`:254`）も同じメソッドを呼ぶ（`grep -n 'isEmpty()' src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java` → 0 件）。単体での固定: `YamlSectionTest.java:481`（`dropBlankRows_removesOnlyEmptyMappingRow`）・`:511`（`dropBlankRows_keepsRowHavingOnlyMarkerColumnValue`）・`:532`（`dropBlankRows_removesNonMappingRows`）・`:557`（`dropBlankRows_keepsRowHavingOnlyNullValues`） | | |
| `YamlSection` の javadoc と `YamlTableDataBuilder` のコメントが是正後の挙動と食い違わない | OK | 点検した範囲（`YamlSection.java`）: **クラス javadoc `:13`-`:40`**（3 つの意味規則の関係と適用順。K3 で書き直した）・`dropBlankRows` javadoc `:154`-`:204`（`:157`-`:164` 判定の説明、`:166`-`:173` 解説書の引用、`:175`-`:184` `record_fragment` の行を除外する理由（K5 で `{@link}` 先と適用範囲を訂正）、`:186`-`:192` 値加工との順序、`:194`-`:198` 列名解決との前後関係は結果を変えないこと）・`isBlankRow` javadoc `:215`-`:226`・`resolveColumns` javadoc `:231`-`:246`。`YamlTableDataBuilder.java`: クラス javadoc `:37`-`:40`・`buildTableDataList` 内インラインコメント `:90`-`:92`・`buildListMapRows` javadoc `:169`-`:171`・`extractRows` javadoc `:213`-`:214`。<br>**機械的な洗い出しに使った grep（実行したもの）**: `grep -rn "全ての値が空文字\|空文字ではない\|非空\|値を見\|値ベース" src/main/java src/test` — ヒット 55 件を 1 件ずつ開いて是正後の挙動と照合した。是正前の述語を条件として述べていたのは `YamlSectionTest.java:565`（K4。assert メッセージ。`空文字ではない` は前ラウンドの `grep -rn "全ての値が空文字"` では掛からなかった）と `YamlSectionTest.java` の `dropBlankRows_keepsRowHavingAnyNonBlankValue`（K6。畳んで削除）と `YamlTableDataBuilderTest.java` の `*_partiallyBlankValueRowKept` 2 件の javadoc 見出し（「値が 1 つでも非空の行は保持されること」→「空文字・null を値に持つ行がいずれも保持されること」）。是正後は残ヒットすべてが整合（`非空` の残りは「非空マッピング行＝キーを持つ行」「変異／旧実装の説明」「`MessageContent` の非空 Map」「`YamlTrailingNullOracleTest` の先頭セルが非空」で、いずれも空行判定の述語を述べていない）。`grep -rn "·" src/` → 0 件（K7 で U+00B7 を U+30FB に直した後） | | |
| 本体 `BasicTestDataParser` を正解にしたテストがあり、`{}`／全値 `""`／`null` だけ／マーカーカラムだけ の4種をテーブルと `LIST_MAP` の両方で入力に含む | OK | `src/test/java/nablarch/test/core/reader/YamlBlankEntryOracleTest.java`（**10 件**）。テーブル（`setup_tables`）が `:232` T1=`{}`・`:248` T2=全値 `""`・`:265` T3=`null` だけ・`:282` T4=マーカーカラムだけ（他カラムに `""` を明示）・`:313` T5=マーカーカラムだけ（他カラムをキーごと省略）。`LIST_MAP` が `:353` L1・`:368` L2・`:384` L3・`:400` L4・`:425` L5。YAML 入力は `YamlBlankEntryOracleTest/blankEntry.yaml`（T1 `:10`・T2 `:19`・T3 `:30`・T4 `:43`・T5 `:60`・L1 `:71`・L2 `:79`・L3 `:89`・L4 `:100`・L5 `:113`）。本体側入力は `BodyExcelOracle`（#36 の既存クラス。無変更）で `.xlsx` を組み立て `BasicTestDataParser` で読む。各ケースは本体の結果を正解として YAML と突き合わせる（`YamlBlankEntryOracleTest.java:462` `assertTableCase`／同 `:487` `assertListMapCase`）だけでなく、本体の結果自体が解説書どおりであることも assert する（同 `:510` `assertTableValues`／同 `:528` `assertListMapValues`）。T5・L5 は「行が読み飛ばされないこと（件数・カラム名）は本体と一致」と「省略カラムの値は本体 `""`・YAML null」という仕様差の両方を assert する | | |
| 是正前に落ち是正後に通るテストが存在する | OK | 下記「変異 M-C（是正前の実装に戻す）」。`isBlankRow` だけを旧実装（値ベース）に戻すと `Tests run: 299, Failures: 12, Errors: 0, Skipped: 1` になり、うち `YamlBlankEntryOracleTest.getSetupTableData_allEmptyStringRowIsKept`・`getListMap_allEmptyStringRowIsKept` を含む 12 件が落ちる。是正後は `Tests run: 299, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS` | | |
| 既存テストで期待値を変えたもの・変えなかったものが件数付きで記録されている | OK | 下記「是正後に落ちた既存テスト（全件・実測）」。落ちた既存テストは **14 件**（期待値を変えた 12 件＋フィクスチャだけ変えた 2 件）。落ちなかったが記述だけ直したものが **6 件**（同節）。折り畳んで削除したテストが **2 件**（`YamlSectionTest.dropBlankRows_keepsRowHavingOnlyEmptyStringValues`（J15）と `YamlSectionTest.dropBlankRows_keepsRowHavingAnyNonBlankValue`（K6）） | | |
| 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 下記「変異確認（実測）」。`src/main` 側の変異 5 通り（M-A〜M-E）＋期待値の崩し 8 通り（M24〜M31）を隔離コピーで実測。崩したもの・変えた挙動を検知できなかったものは 0 件。M-A〜M-E は本ラウンドで**畳んだ後の状態を対象に実測し直した**（下表の `Tests run` は 299 件時点の値）。K6 の折り畳みが変異カバレッジを落としていないことは、同一の隔離コピーで「畳んだ状態」と「畳む前の状態（当該テストだけを戻したもの）」の両方に M-A・M-B・M-C を当て、**落ちたテスト名の集合が完全に一致する**（`diff` が空、18 件／3 件／12 件）ことと、**畳んだテスト自身はどの変異でも落ちなかった**ことで確かめた。隔離コピーは `git worktree add --detach <scratchpad>/mut-k39-kiyo`（ほかに `mut-k39b-kiyo`・`mut-k39c-kiyo`）で作り、終了後すべて `git worktree remove` 済み | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 299, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。着手前ベースライン（隔離コピーを `d682fbd` に切り替えて自分で実測）は `Tests run: 291, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。差 +8 ＝ 新規 `YamlBlankEntryOracleTest` 10 件 − `YamlSectionTest` の折り畳みで削除した 2 件（J15・K6）。`Skipped: 1` は `YamlTableDataBuilderTest` の既存 `@Ignore`（#41 の担当。触っていない） | | |

## 一次情報の確認（自分で開いて確かめた）

- 解説書（ピン `afa4f9e`）`ja/development_tools/testing_framework/implementation/testdata_notation.rst:1502`。親節は `:1451`「コメント・マーカーカラム・空エントリを扱う」。逐語:
  「記法として空のエントリは読み飛ばされる。Excel 形式では行の全セルが空セルの場合、YAML 形式では `rows:` 内の要素が空マッピング（`{}`）の場合である。`""` と書いた空文字は値であり、すべての値が `""` のエントリは読み飛ばされず、全カラムが空文字のエントリとして読み込まれる。（中略）この判定はマーカーカラムを除外する前に行われる。そのため、マーカーカラムだけに値があるエントリは読み飛ばされず、他のカラムがすべて空文字のエントリとして読み込まれる。」
  **YAML 形式のスキップ条件として挙げられているのは空マッピング `{}` だけである**（「全ての値が空文字」は Excel 側にも YAML 側にもスキップ条件として現れず、逆に「読み飛ばされず」と明記されている）
- 本体 `PoiXlsReader`（`../nablarch-testing/src/main/java/nablarch/test/core/reader/PoiXlsReader.java`）: `readLine` が `:93` で `isBlankLine(list)` を呼び `continue` する。`isBlankLine`（`:140`-`:146`）は各要素を `e.isEmpty()` だけで判定する
- 本体 `TestDataParsingTemplate`（同 `../nablarch-testing/src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java`）: `:180` の `isBlankLine(line)` による読み飛ばしが `:183` の `interpret(line)` より**前**にある。したがって「空行判定は値加工より前」という順序の主張は本体で成立している
- ファイルデータの行構造: `YamlFileBuilder.java:235`-`:241` が `rows:` の各要素を `rowObj instanceof List` でだけ受け付け、`List` でない行を `continue` で読み飛ばす。つまりファイルデータの行は Map ではなく配列である。`YamlSection.castMap`（`:116`-`:121`）は Map でない値に空 Map を返すため、`dropBlankRows` をファイルデータに通すと**全ての行が消える**。これがファイルデータを除外する真の理由であり、「全フィールドが空文字のレコードを保持するため」という是正前の理由付けは是正後の `dropBlankRows` には当てはまらない
- スキーマ `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:108`（`$defs.table_data.properties.rows.description`）の逐語:
  「(2) カラム名決定行にはあるが個々の行で省略したカラムは、その行でそのカラムに `null` を書いたのと同じ扱いになる（キーが無い状態ではなく値が null の状態で保持される）。」
  これが T5・L5 の「省略カラムは YAML では null になる」の根拠である（#24 の決定）

## 記述の是正（実測で真偽を確かめた）

指示書 J1〜J18 に対応する。番号は指示書のもの。

| # | 場所 | 何が偽だったか／何をしたか | 実測の根拠 |
|---|---|---|---|
| J1 | `YamlTableDataBuilder.java:90`-`:92` | 「空マッピング、および全ての値が空文字の行」を取り除くと書いてあった（是正で取り消した規則）。「値を 1 つも持たない行＝空マッピング {} だけ」に直し、`""` も Java null も残ることを明記 | `grep -rn "全ての値が空文字" src/main/java` → **0 件**（残るのは #42 担当のスキーマ JSON のみ） |
| J2 | `YamlSection.java` `dropBlankRows` javadoc | 「この順序が列名の決定を左右する」は**偽**。理由を「値加工より前」の 1 点（`:186`-`:192`）に絞り、`:194`-`:198` に「列名解決との前後関係は結果を変えない」を正しく書いた | 変異 **M-D**（`dropBlankRows` を `resolveColumns` の後ろへ移動）で `Tests run: 299, Failures: 0` = 全緑。順序は列名決定に影響しない |
| J3 | 同 javadoc `:176`-`:183` | 「全フィールドが空文字のレコードを 1 件として保持する仕様のため」は是正後の `dropBlankRows` には当てはまらない。真の理由（ファイルデータの行は Map でなく配列なので通すと全行が消える）に書き直した | `YamlFileBuilder.java:235`-`:241`（`rowObj instanceof List`）と `YamlSection.castMap:116`-`:121` を開いて確認 |
| J4 | `YamlTableDataBuilderTest.java:1281`-`:1284` | セクション見出しが「空マッピング、および全ての値が空文字の行が行として存在しないものとして扱われること」だった（直下のテスト群は正反対を assert）。「行として存在しないものとして扱われるのは空マッピングだけであり、全値 `""`・null だけ・マーカーだけの行は残ること」に直した | 直下のテスト（`:1300` 以降）の assert を読んで確認 |
| J5 | `YamlFileBuilderTest.java:517`-`:521` | 「テーブル系・`list_maps` では全ての値が空文字の行を行として存在しないものとして扱う」の前半が偽。J3 と同じ真の理由に書き直した | 同上 |
| J6 | `YamlTableDataBuilderTest.java:1582`-`:1586`、`YamlTableDataBuilderTest/tableData.yaml:140`-`:143` | 「判定を全値 `""` へ戻すとこのテストが落ちるので門番になる」は**偽**。事実の記録（値加工後に全値 `""` になっても行は残る）に留め、旧実装へ戻す変異では落ちないことを明記した | 変異 **M-C**（旧実装に戻す）で `buildTableDataList_rowInterpretedToAllBlankIsKept` は**落ちなかった**（失敗 12 件の中に無い） |
| J7 | `YamlTableDataBuilderTest.java:1653`-`:1655`、`YamlTableDataBuilderTest/tableData.yaml:277` | 「list_maps 経路でも空行判定が値を見ないことを固定する」は過大主張。同じく事実の記録に直した | 変異 **M-C** で `buildListMapRows_rowInterpretedToAllBlankIsKept` も落ちなかった |
| J8 | `YamlTableDataBuilderTest.java:1620`-`:1623`・`:1684`-`:1685` | 「解説書が定めるスキップ条件は空マッピングと全ての値が空文字の 2 つだけ」は解説書と逆。「YAML 形式のスキップ条件は空マッピング `{}` だけ」に直した | 解説書 `afa4f9e` `:1502` の原文（上記「一次情報の確認」）を自分で開いて確認 |
| J9 | `YamlTableDataBuilderTest.java:1734`-`:1735` | 「空行判定はマーカーカラムの値も対象に含めるため」は偽（判定は値を一切見ない）。「判定は値を一切見ずキーの有無だけで行う」に直した | `YamlSection.java:227`-`:229` |
| J10 | `YamlTableDataBuilderTest.java:98`-`:113`（`newBlankingBuilder` javadoc） | 「空行判定が値加工より前に行われることを確かめるための門番」は偽。「値加工後に全値 `""` になる行を作るためのビルダ」に直し、前後関係は判別できないことと M-C で落ちないことを明記した | 変異 M-C（同上） |
| J11 | `blankEntry.yaml:60` T5・`:113` L5、`YamlBlankEntryOracleTest.java:313`・`:425` | 完了条件3の「マーカーカラムだけ」が oracle の入力に無かった（T4/L4 は他カラムにも `""` を明示していた）。真のマーカーのみ行（`- "[NO]": "2"`）を T5・L5 として追加し、件数・カラム名は本体と一致すること、他カラムの値は本体 `""`・YAML null になることを両方 assert した。根拠（スキーマ `:108` の (2) の逐語と #24）を javadoc に引用文つきで書いた。既存の T4/L4 は Excel の空セルと値をそろえた対照として残し、なぜ `""` を明示するかをフィクスチャ（`blankEntry.yaml:40`-`:41`・同 `:98`-`:99`）に書いた | 変異 **M-B**（マーカーカラムを除外した後で空判定する実装）が T5・L5 を殺す（`T5: 行数が本体と一致すること`・`L5: 件数が本体と一致すること` が失敗）。追加前は同じ変異で oracle 8 件がすべて通っていた |
| J12 | `YamlColumnOmissionTest.java:172`-`:201` | `omission.yaml` の s4a/s4b に `NULL_COL` が 1 つも無く（`awk '/group_id: s4a/,/group_id: s0/' omission.yaml \| grep -c NULL_COL` → **0**）、`a.contains("NULL_COL"), is(false)` はどんな実装でも真だった。この 2 行を捨て、代わりに「先頭の `{}` が行として取り除かれデータ行が 2 件になること」（`s4a.size()`／`s4b.size()`）と「カラム名が最初にキーを持つ行のキー列と一致すること」を assert した。フィクスチャは変えていない | 変異 **M-A**（`isBlankRow` を常に false）で本テストが落ちる（`s4a: 先頭の空エントリ（{}）は行として取り除かれ、データ行は 2 件になること`）。是正前はこの変異でも通っていた |
| J13 | 本ファイル | 旧版の完了条件2の Self-check が J1 を見落として OK になっていた点、変異 M22 が空振りの assert を反転させたものだった点、J6 に対応する記述が偽だった点をすべて直した。旧 M1〜M23 の表は、行番号が今回の変更で動いたうえ M22 が証拠にならないため、実測し直した M-A〜M-E ＋ M24〜M31 の表に置き換えた | 下記「変異確認（実測）」 |
| J14 | `YamlSection.java:247`-`:254` | `isBlankRow` と `resolveColumns` が同じ判定を別々に書いていた。`resolveColumns` を `if (!isBlankRow(row))` に変え、判定を 1 箇所に閉じた | 置換前の条件は `!castMap(row).isEmpty()`、`isBlankRow` は `castMap(row).isEmpty()` なので式として同一。実測でも `mvn -o clean test` が `Tests run: 299, Failures: 0`、変異 M-A/M-B/M-C/M-D/M-E の結果も置換前と矛盾しない |
| J15 | `YamlSectionTest.java:463`-`:496` | `dropBlankRows_keepsRowHavingOnlyEmptyStringValues` を `dropBlankRows_removesOnlyEmptyMappingRow` に畳んだ（後者の入力は既に全値 `""` の行を含み、それが残ることを assert していた）。前者の javadoc が持っていた本体 Excel 側の根拠（`PoiXlsReader#isBlankLine` と `QuotationTrimmer`）は後者の javadoc `:471`-`:474` に移した | 畳んだ後も変異 **M-C** で `dropBlankRows_removesOnlyEmptyMappingRow` が落ちる（畳む前は 2 件とも同じ M-C で落ちていた）。変異カバレッジは落ちていない |
| J16 | `YamlBlankEntryOracleTest.java:124`-`:129` | `oracle.parser().setDbInfo(dbInfo)` を毎テスト呼んでいたのを `if (oracle == null)` ブロック内へ移した | `mvn -o clean test` で `YamlBlankEntryOracleTest` 10 件すべて通る |
| J17 | 同 `:505`-`:509`（`assertTableValues` javadoc）・`:523`-`:527`（`assertListMapValues` javadoc） | 「本体を正解として扱う以上…」のコメントが呼び出し側インラインとヘルパーの両方にあった。先例 `YamlTrailingNullOracleTest.java:399` に揃え、呼び出し側のインラインコメントを消してヘルパー（`assertTableValues`／`assertListMapValues`）の javadoc 1 箇所だけにした（現在この文が現れるのは `YamlBlankEntryOracleTest.java:508` と 同 `:526` の 2 箇所＝ヘルパー 2 つの javadoc のみで、インラインには無い） | `YamlTrailingNullOracleTest.java:396`-`:400` を開いて形式を確認 |
| J18 | `YamlTableDataBuilderTest.java:1390` | `buildTableDataList_blankValueRowLeadingInExpectedTableKept` を `..._blankValueRowLeadingInExpectedTableKeptAndDeterminesColumns` に改名（`setup_tables` 側と揃えた） | 参照箇所は `grep -rn` で本ファイル以外に無いことを確認済み |

## 是正後に落ちた既存テスト（全件・実測）

`isBlankRow` だけを是正し、テストは一切直していない状態で測った（前回の実測を再掲）。
落ちた既存テストは **14 件**で、指示書 Steps D が挙げた 14 件と完全に一致した。

| # | テスト（現在の名前） | 変えたもの |
|---|---|---|
| 1 | `YamlSectionTest.dropBlankRows_removesOnlyEmptyMappingRow`（現 `:481`） | **期待値＋テスト名**。残る件数を 1→2 に。残った 2 件の中身も assert |
| 2 | `YamlSectionTest.dropBlankRows_keepsRowHavingOnlyEmptyStringValues`（旧） | **期待値＋テスト名**。是正後は値の中身が判定に無関係になるため作り替えた。今回さらに #1 に畳んで**削除**した（J15） |
| 3 | `YamlTableDataBuilderTest.buildTableDataList_blankValueRowLeadingKeptAndDeterminesColumns`（現 `:1300`） | **期待値＋テスト名**。列名が先頭行の 2 キー、行数 1→2 |
| 4 | `YamlTableDataBuilderTest.buildTableDataList_blankValueRowMiddleKept`（現 `:1330`） | **期待値＋テスト名**。行数 2→3 |
| 5 | `YamlTableDataBuilderTest.buildTableDataList_partiallyBlankValueRowKept`（現 `:1359`） | **期待値のみ**（名前は据え置き）。行数 1→2 |
| 6 | `YamlTableDataBuilderTest.buildTableDataList_blankValueRowLeadingInExpectedTableKeptAndDeterminesColumns`（現 `:1390`） | **期待値＋テスト名**（今回 J18 でさらに改名） |
| 7 | `YamlTableDataBuilderTest.buildTableDataList_blankValueRowMiddleInExpectedTableKept`（現 `:1419`） | **期待値＋テスト名** |
| 8 | `YamlTableDataBuilderTest.buildTableDataList_blankValueRowInExpectedCompleteTableKept`（現 `:1474`） | **期待値＋テスト名**。行数 2→5、全カラム 11 は不変 |
| 9 | `YamlTableDataBuilderTest.buildListMapRows_blankValueRowLeadingKeptAndDeterminesKeys`（現 `:1507`） | **期待値＋テスト名**。件数 1→2 |
| 10 | `YamlTableDataBuilderTest.buildListMapRows_blankValueRowMiddleKept`（現 `:1534`） | **期待値＋テスト名**。件数 2→3 |
| 11 | `YamlTableDataBuilderTest.buildListMapRows_partiallyBlankValueRowKept`（現 `:1561`） | **期待値のみ**（名前は据え置き）。件数 1→2 |
| 12 | `YamlTableDataBuilderTest.buildListMapRows_allEmptyMappingRowsReturnsEmptyList`（現 `:1719`） | **フィクスチャ＋テスト名**（期待値＝空リストは不変） |
| 13 | `YamlColumnOmissionTest.columnNamesDependOnRowOrderAfterBlankRowRemoval`（現 `:183`） | **フィクスチャのみ**（当時）。今回さらに **assert を差し替えた**（J12） |
| 14 | `YamlColumnOmissionTest.insertedValueDependsOnRowOrder`（現 `:206`） | **フィクスチャのみ**（期待値・テスト名は不変） |

**件数まとめ**: 落ちた既存テスト **14 件** ＝ 期待値を変えた **12 件** ＋ フィクスチャだけ変えて期待値は変えなかった **2 件**（#13・#14）。
うち #2 は今回 #1 に畳んで削除した（残 13 件）。

### 落ちなかったが記述だけ直したもの: 6 件

| テスト | 直した理由 |
|---|---|
| `YamlSectionTest.dropBlankRows_keepsRowHavingOnlyMarkerColumnValue`（現 `:511`） | javadoc と assert メッセージの根拠を解説書の「判定はマーカーカラムを除外する前に行われる」に直した |
| `YamlSectionTest.dropBlankRows_keepsRowHavingOnlyNullValues`（現 `:557`） | javadoc のスキップ条件の説明を「空マッピングだけ」に直した |
| `YamlTableDataBuilderTest.buildTableDataList_rowInterpretedToAllBlankIsKept`（現 `:1594`） | J6。「門番になる」を事実の記録に直した |
| `YamlTableDataBuilderTest.buildListMapRows_rowInterpretedToAllBlankIsKept`（現 `:1663`） | J7。同上 |
| `YamlTableDataBuilderTest.buildTableDataList_nullValueOnlyRowKept`（現 `:1630`） | J8。解説書のスキップ条件の説明を直した |
| `YamlTableDataBuilderTest.buildListMapRows_nullValueOnlyRowKept`（現 `:1692`） | J8。同上 |

加えて `buildListMapRows_markerOnlyRowKeptAsEmptyMap`（現 `:1746`）の javadoc を J9 で直し、
`YamlFileBuilderTest.buildFileList_allBlankFieldRecordIsKept`（現 `:532`）の javadoc を J5 で直した。
フィクスチャのコメントも同じ理由で直した（`tableData.yaml` の 2 箇所）。

## 変異確認（実測）

隔離コピー（`git worktree add --detach <scratchpad>/mut-k39-kiyo`・`mut-k39b-kiyo`・`mut-k39c-kiyo`）に作業ツリーの `src` を複写して実施した。
M-A〜M-E は K6 の折り畳み後の状態で本ラウンドに測り直した値である（`Tests run` は 299 件）。
コマンドは `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`（M24 以降は `-Dtest=<Class>#<method>`）。

### `src/main` 側の変異

| # | 変異 | 結果 | 意味 |
|---|---|---|---|
| M-A | `isBlankRow` → `return false;`（一切除去しない） | `Tests run: 299, Failures: 17, Errors: 1, Skipped: 1` | `YamlColumnOmissionTest.columnNamesDependOnRowOrderAfterBlankRowRemoval` を含む 18 件が落ちる。J12 の是正でこのテストが変異を殺せるようになった（是正前は通っていた） |
| M-B | マーカーカラムを除外した**後**で空判定する（`isMarker` でないキーが 1 つも無ければ空とみなす） | `Tests run: 299, Failures: 3, Errors: 0, Skipped: 1` | `getSetupTableData_markerOnlyRowWithOmittedColumnsIsKept`・`getListMap_markerOnlyRowWithOmittedKeysIsKept`・`buildListMapRows_markerOnlyRowKeptAsEmptyMap` が落ちる。J11 の是正で oracle 側が変異を殺せるようになった（追加前は `buildListMapRows_markerOnlyRowKeptAsEmptyMap` 1 件だけだった） |
| M-C | `isBlankRow` を旧実装（全ての値が空文字なら真）に戻す | `Tests run: 299, Failures: 12, Errors: 0, Skipped: 1` | 落ちた 12 件: `YamlBlankEntryOracleTest` 2 件（`getSetupTableData_allEmptyStringRowIsKept`・`getListMap_allEmptyStringRowIsKept`）、`YamlSectionTest` 1 件（`dropBlankRows_removesOnlyEmptyMappingRow`。J15 で畳んだ後も殺せる）、`YamlTableDataBuilderTest` 9 件。**`*_rowInterpretedToAllBlankIsKept` の 2 件は落ちない**（J6・J7 の根拠） |
| M-D | `dropBlankRows` を `resolveColumns` の**後ろ**へ移動（`buildTableDataList`・`buildListMapRows` の両方） | `Tests run: 299, Failures: 0, Errors: 0, Skipped: 1` | 順序は列名決定に影響しない（J2 の根拠）。J14 で述語を 1 箇所に閉じたので構造的にそうなる |
| M-E | `extractRows` で行に無いカラムを null でなく `""` にする | `Tests run: 299, Failures: 16, Errors: 0, Skipped: 1` | T5・L5 の「省略カラムは null」assert が落ちる（`expected null, but was:<>`）。J11 の仕様差 assert が効いている |

### 期待値の崩し（追加/変更したテスト）

| # | テスト | 崩し方 | 結果 |
|---|---|---|---|
| M24 | `YamlBlankEntryOracleTest.getSetupTableData_markerOnlyRowWithOmittedColumnsIsKept` | `actual.size(), is(expected.size())` → `is(expected.size() + 1)` | FAILED |
| M25 | 〃 | `assertNull(actual.getValue(1, column))` → `getValue(0, column)` | FAILED |
| M26 | 〃 | 本体期待値 `expected.getValue(1, column), is("")` → `is("x")` | FAILED |
| M27 | `YamlBlankEntryOracleTest.getListMap_markerOnlyRowWithOmittedKeysIsKept` | `actual.size(), is(expected.size())` → `is(expected.size() + 1)` | FAILED |
| M28 | 〃 | `assertNull(actual.get(1).get(key))` → `actual.get(0).get(key)` | FAILED |
| M29 | 〃 | 本体期待値 `expected.get(1).get(key), is("")` → `is("x")` | FAILED |
| M30 | `YamlColumnOmissionTest.columnNamesDependOnRowOrderAfterBlankRowRemoval` | `s4a.size(), is(2)` → `is(3)` | FAILED |
| M31 | 〃 | 期待カラム名リストから `TIMESTAMP_COL` を落とす | FAILED |

既存のまま据え置いたテスト（T1〜T4・L1〜L4 の 8 件、`YamlSectionTest`・`YamlTableDataBuilderTest` の
空エントリ系）については、前回タスクで期待値の崩し（M1〜M21・M23 相当）が実測済みであり、
今回の変更で期待値を触っていない。今回崩し直したのは**期待値を変えた／新規に書いたもの**だけである。
前回の M22（`a.contains("NULL_COL"), is(false)` → `is(true)`）は、その assert 自体が空振り
（s4a/s4b ブロックに `NULL_COL` が 1 つも無い）で挙動変化の検知にならないため、**証拠から外した**。
代わりが M-A と M30・M31 である。

隔離コピーは実施後に `git worktree remove` 済み（`git worktree list` は本体 1 件のみ）。

## 申し送り

- **#42 Step A へ**: スキーマ `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:108`・`:136` の
  「空マッピング `{}` の行、および全ての値が空文字 `""` の行は、行が無いものとして取り除かれる」は
  **本タスクでは直していない**（#42 Step A の担当）。現時点でスキーマの `description` は実装と食い違っている。
  なお、この `description` を典拠として引用している既存テストがある。#42 で文言を直すときは併せて見直すこと。
  - `src/test/java/nablarch/test/core/db/YamlColumnOmissionTest.java:34`（クラス javadoc。`$defs.table_data.properties.rows`（:108）の description が述べる「カラムの省略」の挙動を固定する、と宣言している）
  - 同 `:310`（`:108` の FK・NULL 許容カラムの助言を引用している）
  - `src/test/java/nablarch/test/core/reader/YamlBlankEntryOracleTest.java:297`-`:300`（本タスクで追加。`:108` の (2) の逐語を引用）
  - `:136`（`$defs.list_map_data.properties.rows.description`）を名指しで参照しているテストが 1 件ある。
    `src/test/java/nablarch/test/core/reader/YamlBlankEntryOracleTest.java:410`-`:416`（L5 の javadoc、K1 で書き直したもの）で、
    参照の趣旨は「`:136` には**カラム（キー）省略の扱いを述べた記述が無い**」という不在の指摘である。
    #42 で `:136` を書き換えるときにカラム省略の記述を足すなら、この javadoc も併せて直すこと
    （足さないなら現在の記述のままで正しい）
  - 【全ての値が空文字の行は行として存在しない】段落**そのもの**（`:108`・`:136` 共通）を典拠として
    引用しているテストは、`grep -rn "全ての値が空文字の行は行として存在しない\|list_map_data\|table_data.properties.rows" src/test` の
    範囲では**無い**（`YamlFileBuilderTest.java:1151` が引用しているのは
    `$defs.record_fragment.properties.rows.description` であり、#42 Step A の対象外）
- **#43 Step C へ**: 下流 `nablarch-testing-converter` の
  `YamlFormatReaderScalarTest.skipsRowWhoseValuesAreAllEmpty` が本是正で落ちる見込みである。
  **#43 で実測して確認すること**。本タスクでは converter リポジトリを触っていない（未確認）

## スコープ外として触っていないもの

- #40（2 文字の `\` ＋ `r`）
- #41 — `YamlTableDataBuilderTest` の既存 `@Ignore` はそのまま（`Skipped: 1` が維持されている）
- #42 — 上記「申し送り」のとおり
- #43 — 上記「申し送り」のとおり
- 完了条件外として足さなかったテスト: `buildTableDataList` 経路のスカラ行、`expected_tables` の
  `COL: null` だけの行、全値 `""` の `setup_tables` 行が実際に INSERT されることの DB レベルのテスト
- 本体 `../nablarch-testing` への書き込みなし

## QA / Expert Review

2 ラウンド（`a5a9f10` / `91d5a91`）について、QA・Design・Craft・Verification の 4 軸を独立した
サブエージェントとしてそれぞれ実施した（Craft の 2 巡目は API エラーで 1 度落ちたため再実行、計 9 回）。
各エキスパートには成果物と完了条件とチェックリストだけを渡し、このチェックファイル・実装エキスパートの
サマリ・他軸の判定は渡していない。変異確認は一意な名前の隔離ワークツリーで行うことを義務づけた。

| 軸 | 1巡目（`a5a9f10`） | 2巡目（`91d5a91`） |
|---|---|---|
| QA | **fail** | **fail**（L5 の出典 1 件） |
| Design | **fail** | **fail**（3 件） |
| Craft | **fail**（虚偽記述 9 件） | **fail**（新規 2 件＋退役述語 1 件＋表記ゆれ 1 件） |
| Verification | **fail** | pass（非ブロッキング 2 件） |

### 指摘の型

**実装ロジック・テストの守備範囲・変異カバレッジは、2 巡とも 4 軸すべてが OK と判定した。**
落ちた理由はすべて **javadoc・コメント・assert メッセージ・チェックファイルの記述の正確さ**である。
是正が「値を見る判定」から「キーの有無だけを見る判定」へ述語を入れ替えたため、旧述語を前提に書かれた
記述が広範に偽になり、その掃き出しに 2 ラウンドを要した。

| 巡 | 主な虚偽記述 | 実測した反証 |
|---|---|---|
| 1 | `YamlTableDataBuilder.java` の `dropBlankRows` 呼び出し直上のコメントが「全ての値が空文字の行を取り除く」のまま（**4 軸すべてが独立に検出**） | `isBlankRow` は値を一切見ない |
| 1 | 「`dropBlankRows` と `resolveColumns` の順序が列名の決定を左右する」 | 両者の述語が同一のため、順序を入れ替えても全緑（Craft・Verification が独立に変異で実証） |
| 1 | 「このテストは値ベース判定への逆戻りを防ぐ門番になる」（3 箇所） | 旧実装へ戻す変異で当該テストは落ちない |
| 1 | 「解説書が定めるスキップ条件は空マッピングと全ての値が空文字の 2 つ」 | 解説書（ピン `afa4f9e`）は逆を定める |
| 1 | oracle の入力に「マーカーカラムだけに値がある行」が実在しない（完了条件 3 の逐語未達） | マーカー除外後に空判定する変異で oracle 8 件が全通過 |
| 1 | `YamlColumnOmissionTest` の `NULL_COL` の assert が空振り | フィクスチャ変更で `NULL_COL` が当該ブロックから消え、どんな実装でも真になっていた |
| 2 | L5 の出典としたスキーマ `$defs.list_map_data...rows.description` にカラム省略の記述が無い（**QA・Design が独立に検出**） | JSON をパースして全文確認。「共通」と述べる唯一の文は空行除去規則＝#39 が是正した当の規則 |
| 2 | `{@link YamlFileBuilder#buildDataFileList} が List でない行を読み飛ばす` | 当該ガードは `YamlFileBuilder.java:237`（private `buildFragmentsInternal`）にあり、リンク先には存在しない |
| 2 | assert メッセージ「Java null は空文字ではないため行が残ること」 | 判定は値を見ない。同テストの javadoc は正しく直っており、同一テスト内で矛盾していた |

### 軸ごとの評価

| 軸 | 観点 | 判定 | 根拠 |
|---|---|---|---|
| QA | 検証のやり方が目的に対して意味を持つか | OK | oracle が本物であることを 3 方向（本体側期待値・POI の `.xlsx`・YAML フィクスチャ）の変異で確認。反実仮想（T5/L5 追加前の `a5a9f10` に同じ変異を当てると oracle 8 件が全通過）まで測り、完了条件 3 のギャップが実際に埋まったことを確認 |
| Design | アプローチ・構造が適切か／責務の分離 | OK | 判定が `isBlankRow` の 1 箇所に閉じていること、`dropBlankRows` → `resolveColumns` → `interpret` の順序が本体（`PoiXlsReader.java:93`・`TestDataParsingTemplate.java:180`／`:183`）と揃うことを本体の実物で確認。J14（`resolveColumns` が `isBlankRow` を呼ぶ）で述語の二重定義を解消したことにより、順序入れ替え変異が「等価であることを構造から導ける」形になった |
| Design | システム全体の整合性 | OK | `dropBlankRows` の呼び出し元が `YamlTableDataBuilder.java:95`・`:186` の 2 箇所だけで、ファイル・電文経路に影響が無いことを確認。公開 API の宣言行を `d682fbd` と `diff` して差分ゼロ。下流 `nablarch-testing-converter` の `YamlFormatReader.java:502` が `resolveColumns` を除去なしで呼ぶが、述語が是正前後で同値のため挙動不変であることを確認 |
| Craft | 慣行・エラー処理・null・命名・重複 | OK | `isBlankRow` の可視性が `private static` のまま不変。既存の流儀（出典は節見出し＋引用文、Given/When/Then、ヘルパーの配置）に一致。`mvn -o clean javadoc:javadoc` の警告数が着手前と同数 |
| Verification | 成果物が実際に検査されているか | OK | 2 巡合計で 30 通り超の変異を隔離コピーで実測。生き残るのは順序入れ替えの等価変異 1 件のみで、これは J14 により等価性が構造から導ける |
| Verification | 網羅 | OK | `{}`／全値 `""`／`null` だけ／マーカーのみ（`""` 明示と キー省略 の 2 通り）／スカラ行／`rows: []`／全行 `{}`／先頭行が全値 `""`（列名決定の変化）を網羅。実行順（`reversealphabetical`／`random`）と単独実行でも緑 |

### トリアージ

有効と判定して直したもの: 1 巡目 18 件（J1〜J18）・2 巡目 9 件（K1〜K9）。
実装エキスパートは 2 巡目に、指示に無かった 2 件（`*_partiallyBlankValueRowKept` の javadoc 見出し）を
自ら機械的な洗い出しで検出して直した。

却下したもの（根拠つき）:

| 指摘 | 却下の根拠 |
|---|---|
| スキーマ `ntf-testdata-yaml-schema.json:108`・`:136` の `description` が実装と矛盾（4 軸すべてが指摘） | `steering.md` の #42 Step A が明示的に担当（Prerequisites に #39）。#39 の完了条件に含まれない |
| `buildTableDataList` 経路のスカラ行のテスト／`expected_tables` の `COL: null` だけの行のテスト／全値 `""` の `setup_tables` 行が実際に INSERT されることの DB レベルのテスト | 完了条件外 |
| マーカーのみ行が `expected_tables`／`expected_complete_tables` で未検証 | 完了条件は「テーブルと `LIST_MAP` の両方」であり、セクション単位の網羅は求めていない |
| `YamlFileBuilderTest` の javadoc 見出しが `buildFileList:` を名乗るが実体は `buildDataFileList` | 着手前からある記述で本差分の範囲外 |

### コーディネータの独立検証（`68a57ec`）

エキスパートの報告を鵜呑みにせず、自分で次を実測した。

- `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 299, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`
- 解説書（ピン `afa4f9e`）の節見出し `:1451`「コメント・マーカーカラム・空エントリを扱う」と `:1502` の該当文を開き、実装が SSoT と一致することを確認
- `grep -rn "全ての値が空文字" src/main/java` → 0 件。`grep -rn "空文字ではない" src/` の残り 2 件がいずれも #42 Step A 担当のスキーマ `:108`・`:136` であることを確認
- `grep -rn "·" src/` → 0 件（K7）
- チェックファイル中の `file:line` 参照 35 件を機械抽出し、リポジトリ内 34 件がすべて実在行の範囲内・範囲外 0 件（残り 1 件は解説書への外部参照で別途確認済み）
- `YamlSection.java` の `{@link}` 14 種を列挙し、K5 が `{@link YamlFileBuilder}`（クラス）＋`{@code buildFragmentsInternal}` に直っていることを確認。`instanceof List` のガードが `YamlFileBuilder.java:237` にあり `buildDataFileList`（`:70`-`:92`）に無いことも実物で確認
- K1 の書き直しが検証可能な形になっていることを確認（スキーマに記述が無い旨を明記し、実際の機構として `extractRows` が `:95`（テーブル系）と `:186`（`list_maps`）の両方から呼ばれる共通経路であることを実物で確認）
- K3 のクラス javadoc が「値加工より前」と「`resolveColumns` との前後は結果を変えない」を区別して書けていることを確認
- K6 で畳んだ `dropBlankRows_keepsRowHavingAnyNonBlankValue` の参照が 0 件であることを確認
- T5/L5 が仕様差（本体 `""` / YAML null）を隠さず明示的に assert していることを確認

### #42・#43 への申し送り

- **#42 Step A**: スキーマ `:108`・`:136` は未修正で実装と食い違ったまま。`:136` を書き換えると `YamlBlankEntryOracleTest` の L5 javadoc が参照する記述が消えるため、併せて見直すこと
- **#43 Step C**: 下流 `nablarch-testing-converter` の `YamlFormatReaderScalarTest.skipsRowWhoseValuesAreAllEmpty` がこの是正で落ちる見込み（静的走査による見込みで**未確認**）
- **#43 報告書**: #26 は誤った決定ではなく、当時ピン留めされていた解説書（`5b5c91e`）の本文に忠実だった。文言が変わったのは `6bfc058` であり、#39 は**解説書の改版への追随**である

## Overall Verdict

- Self-check: **PASS**（完了条件 7 件すべて OK。K1〜K9 の記述訂正を反映し、`file:line` 参照と `{@link}` 先を全件開いて再検証済み）
- QA: OK（2巡目の指摘は L5 の出典 1 件。K1 で是正し、コーディネータが実物で確認）
- Design expert: OK（2巡目の指摘 3 件を K1・K2・K3 で是正）
- Craft expert: OK（2巡目の新規 2 件を K4・K5、退役述語を K6、表記ゆれを K7 で是正）
- Verification expert: OK（2巡目 pass。新規 survivor なし。残る 1 件は J14 により等価性が構造から導ける）
- コーディネータの独立検証: OK（上記「コーディネータの独立検証」節）
- Ready to check off: Yes（完了条件 7 項目すべて OK。有効な指摘 27 件を 2 ラウンドで是正、却下 4 件は根拠を記録。`mvn -o clean test` 緑をコーディネータが独立に実測）
