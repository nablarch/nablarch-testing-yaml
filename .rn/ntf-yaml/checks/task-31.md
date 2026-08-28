# task-31 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| 3-1〜3-6 の6件すべてについてテストが存在する | OK | 3-1: `YamlTestDataParserTest#yaml12BooleanWordsAreStringsAsKeysAndValues` / `#unquotedNoKeyStaysStringKey`、3-2: `YamlTableDataBuilderTest#buildListMapRows_allFourteenCharacterTypesAreGenerated` / `#buildListMapRows_unknownCharacterTypeIsNotConverted`（負）、3-3: `YamlTableDataBuilderTest#buildListMapRows_combinedCharTypeNotationKeepsSeparator`、3-4: `YamlTableDataBuilderTest#buildListMapRows_escapedLfIsLineFeed`、3-5: `YamlDateNotationTest#omittedMillisIsFilledWithZero` / `#omittedTimeIsFilledWithZero`、3-6: `YamlTestDataParserTest#attachNotationIsReadableAsUploadFileSpecification`（計9メソッド） | | |
| 落ちたものは `@Ignore` ＋ `NTF-DOC:` 印つきの理由で記録されている（実装は直していない） | OK | 落ちたのは1件のみ。`YamlTableDataBuilderTest.java:751` に `@Ignore("NTF-DOC: implementation/testdata_notation.rst:1313 — 期待 列挙外の文字種名は変換されず ${存在しない文字種,3} のまま / 実際 InterpretationFailedException（原因 IllegalArgumentException: unknown charsetName. charsetName=[存在しない文字種]）")`。期待値は解説書どおりのまま残してある。`git status --porcelain -- src/main src/test/resources pom.xml` が空（実装・設定は未変更） | | |
| 3-2 の負のテスト（列挙外の文字種名は変換されない）が書かれている | OK | `YamlTableDataBuilderTest#buildListMapRows_unknownCharacterTypeIsNotConverted`（`${存在しない文字種,3}` がそのまま残ることを期待）。実行して落ちたため `@Ignore` 済み | | |
| 通った各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 下記「変異確認」のとおり、通った8件すべてを同時に崩して1度実行し、8件すべてが Failure になった（`Tests run: 260, Failures: 8, Errors: 0, Skipped: 1`）。崩した箇所は実行後に元へ戻し、再実行で緑を確認 | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `BUILD SUCCESS` / `Tests run: 260, Failures: 0, Errors: 0, Skipped: 1`（着手前ベースライン 251, Skipped 0 に対し +9 テスト・Skipped +1） | | |

## Overall Verdict

- Self-check: OK

## 6件の結果

| # | テスト | 結果 | `@Ignore` の理由（原文） |
|---|---|---|---|
| 3-1 | `YamlTestDataParserTest#yaml12BooleanWordsAreStringsAsKeysAndValues`<br>`YamlTestDataParserTest#unquotedNoKeyStaysStringKey` | 通った | — |
| 3-2 | `YamlTableDataBuilderTest#buildListMapRows_allFourteenCharacterTypesAreGenerated` | 通った（14文字種すべて） | — |
| 3-2（負） | `YamlTableDataBuilderTest#buildListMapRows_unknownCharacterTypeIsNotConverted` | `@Ignore` | `NTF-DOC: implementation/testdata_notation.rst:1313 — 期待 列挙外の文字種名は変換されず ${存在しない文字種,3} のまま / 実際 InterpretationFailedException（原因 IllegalArgumentException: unknown charsetName. charsetName=[存在しない文字種]）` |
| 3-3 | `YamlTableDataBuilderTest#buildListMapRows_combinedCharTypeNotationKeepsSeparator` | 通った | — |
| 3-4 | `YamlTableDataBuilderTest#buildListMapRows_escapedLfIsLineFeed` | 通った | — |
| 3-5 | `YamlDateNotationTest#omittedMillisIsFilledWithZero`<br>`YamlDateNotationTest#omittedTimeIsFilledWithZero` | 通った | — |
| 3-6 | `YamlTestDataParserTest#attachNotationIsReadableAsUploadFileSpecification` | 通った | — |

### 3-2 の14文字種の内訳

半角英字 / 半角数字 / 半角記号 / 半角カナ / 全角英字 / 全角数字 / 全角ひらがな / 全角カタカナ / 全角漢字 / 全角記号その他 / 中国語 / サロゲートペア / 改行 / 外字 の14種すべてで `${<文字種>,3}` が変換され、3コードポイントになった（落ちた文字種は0件）。文字種ごとに別の `list_maps` エントリにし、結果をリストに集めてから一度に判定しているため、1つ落ちても残りの検証は行われる。

## 変異確認

- コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
- 通った8メソッドの期待値を同時に崩して1度だけ実行した（`@Ignore` にした `buildListMapRows_unknownCharacterTypeIsNotConverted` は対象外）
- 結果: `Tests run: 260, Failures: 8, Errors: 0, Skipped: 1` / `BUILD FAILURE`。崩した8件がすべて Failure になり、他は増えなかった

| 崩した期待値 | 落ちたテスト（surefire の出力） |
|---|---|
| `row.get("no")` の期待を `"no"` → `"false"`（YAML 1.1 の結果に）| `YamlTestDataParserTest.yaml12BooleanWordsAreStringsAsKeysAndValues:303 値 no が文字列 "no" になること` |
| `row.get("no")` の期待を `"1"` → `"9"` | `YamlTestDataParserTest.unquotedNoKeyStaysStringKey:335` |
| `${attach:<path>}` の期待に `X` を付加 | `YamlTestDataParserTest.attachNotationIsReadableAsUploadFileSpecification:367 ${attach:ファイルパス} の表記のまま取得できること` |
| コードポイント数の期待を `3` → `4` | `YamlTableDataBuilderTest.buildListMapRows_allFourteenCharacterTypesAreGenerated:735`（14文字種すべてが失敗リストに載り、全文字種が検証されていることも同時に確認できた） |
| 組み合わせ記法の長さの期待を `7` → `8` | `YamlTableDataBuilderTest.buildListMapRows_combinedCharTypeNotationKeepsSeparator:789 半角数字2文字 + "-" + 半角数字4文字で 7 文字になること: [53-5410]` |
| `"\n"` の期待を LF → CR | `YamlTableDataBuilderTest.buildListMapRows_escapedLfIsLineFeed:823 "\n" は LF（U+000A）になること` |
| 期待タイムスタンプを `2021-01-23 12:34:56.000` → `.001` | `YamlDateNotationTest.omittedMillisIsFilledWithZero:125 ミリ秒を省略した場合はミリ秒0として扱われること` |
| 期待タイムスタンプを `2021-01-23 00:00:00.000` → `00:00:01.000` | `YamlDateNotationTest.omittedTimeIsFilledWithZero:145 時刻全部を省略した場合は0時0分0秒000として扱われること` |

- 崩した箇所を元に戻したうえで再実行し、`BUILD SUCCESS` / `Tests run: 260, Failures: 0, Errors: 0, Skipped: 1` を確認した

## 触ったファイル

- `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java`（3-1・3-6 のテスト追加）
- `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/nativeTypes.yaml`（3-1・3-6 のフィクスチャ追加）
- `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java`（3-2・3-3・3-4 のテスト追加、`@Ignore` 1件）
- `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/nativeTypes.yaml`（3-2・3-3・3-4 のフィクスチャ追加）
- `src/test/java/nablarch/test/core/db/YamlDateNotationTest.java`（新規。3-5）
- `src/test/java/nablarch/test/core/db/YamlDateNotationTest/date.yaml`（新規。3-5 のフィクスチャ）

`src/main`・`src/test/resources`・`pom.xml` は未変更（`git status --porcelain -- src/main src/test/resources pom.xml` が空）。

## コーディネーター独立レビュー

Step 4 では4観点レビューを回さない（指示書 §7）。コーディネーターがコミット済み差分を独立に読み、ビルドを自分で実行して検証した。

| 観点 | 判定 | 根拠 |
|---|---|---|
| 差分がタスクの範囲に収まっている | OK | `git diff d55c5bc..c9f6020 --stat` は `src/test/java/` 配下6ファイル（新規クラス `YamlDateNotationTest` とそのフィクスチャを含む）のみ、513 insertions / 0 deletions。`git status --porcelain -- src/main src/test/resources pom.xml` が空であることをコーディネーターも確認 |
| 6件すべてにテストがある | OK | 3-1（2メソッド）・3-2（14文字種＋負のテスト）・3-3・3-4・3-5（2メソッド）・3-6。3-7〜3-13 には手を付けていない |
| 落ちたものを実装で直していない | OK | `src/main` の差分0。落ちた1件は `@Ignore` として残置 |
| `@Ignore` の書式が印つき | OK | `YamlTableDataBuilderTest.java:751` の1件のみ。`@Ignore("NTF-DOC: implementation/testdata_notation.rst:1313 — 期待 … / 実際 …")` の書式に従っている。`grep -rn '@Ignore' src/test/java/` のヒットはこの1件だけ |
| 3-2 の負のテストが書かれている | OK | `buildListMapRows_unknownCharacterTypeIsNotConverted`。期待値を実際の挙動（例外）に書き換えず、解説書どおりの期待値を残したまま `@Ignore` にしている |
| 3-2 が「1つ落ちると残りが検証されない」書き方になっていない | OK | 文字種ごとに別エントリで評価し、失敗を集めてから一度に判定。変異確認で14件すべてが失敗リストに載ることを実測している |
| 変異確認が実施されている | OK | 通った8メソッドの期待値を同時に崩して `Tests run: 260, Failures: 8, Errors: 0, Skipped: 1`。崩した8件が過不足なく失敗し、復元後に緑を確認。`@Ignore` の1件は対象外である旨も記録 |
| ビルド（コーディネーター自身の実行） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `BUILD SUCCESS`。`target/surefire-reports/*.txt` の集計で `Tests run: 260, Failures: 0, Errors: 0, Skipped: 1`（2026-08-26 コーディネーターが独立実行） |

### 指示書の想定と実測が食い違った点（#33 の報告へ引き継ぐ）

3-3（組み合わせ記法 `"${半角数字,2}-${半角数字,4}"`）は指示書が既存0件として挙げた項目だが、**解説書どおりに通った**。担っているのは `unit-test.xml` の `CompositeInterpreter`（`${...}` を1要素ずつ切り出して委譲する）であり、`BasicJapaneseCharacterInterpreter` 単体は完全一致（`m.matches()`）でしか変換しない。**通ったこと自体は問題ではない**が、押さえている経路が単体クラスではなく合成側である点を報告に残す。

## Overall Verdict（コーディネーター）

- コーディネーター独立レビュー: OK
- Ready to check off: Yes

## 後続タスクによる失効（2026-08-29 コーディネータ追記）

本記録は **#31 完了時点（Step 4 第1回）の実測**であり、その後の是正で次の2点が失効した。
記録そのものは当時の事実として残し、書き換えていない。

| 失効した記述 | 失効させたタスク | 現在の事実 |
|---|---|---|
| 完了条件「落ちたものは `@Ignore` ＋ `NTF-DOC:` 印つきの理由で記録されている」（`:8`）、および 3-2 の負のテストに関する記述（`:9`・`:23`） | **#41** | 当該テスト `buildListMapRows_unknownCharacterTypeIsNotConverted` は #41 で削除済み。`grep -rnE '^\s*@Ignore' src/test` は 0 件、`mvn -o clean test` は `Skipped: 0` |

**削除の根拠**（#41 で実測。詳細は `checks/task-41.md`）:

- 解説書は列挙外の文字種名を書いたときの挙動を定めていない。`nablarch-document@09779f6`
  「docs: 限定列挙に付けた「それ以外はエラー」を落とす」が当該文から `（それ以外を指定するとエラーになる）`
  を意図的に削除している（その親 `3e01b69` の時点では在った）。
- 挙動は名前の字種に依存し一律ではない。`BasicJapaneseCharacterInterpreter.java:24` のパターン
  `\$\{(\W+)\s*,\s*([0-9]+)\}` は文字種名部が `\W+` のため、`${存在しない文字種,3}` は例外になるが
  `${abc,3}` はマッチせずそのまま残る。この不一貫さ自体が「解説書が定めていない」ことの裏づけである。
- 14 種類が使えること自体は `YamlTableDataBuilderTest#buildListMapRows_allFourteenCharacterTypesAreGenerated`
  が引き続き担保する。

`steering.md` の #31 該当箇所（Step 3-2 と Completion criteria）にも同趣旨の注記を入れてある。
