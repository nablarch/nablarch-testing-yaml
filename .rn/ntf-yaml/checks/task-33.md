# task-33 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| C0/C1 が計測され、下がった箇所が挙がっている | OK | HEAD `8eacaa7`: `mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes`（`Tests run: 267, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`）→ `mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec`。CSV 集計で C0 = 1663/1676 = 99.22%（`INSTRUCTION_MISSED` 13）、C1 = 168/170 = 98.82%（`BRANCH_MISSED` 2）。基準 `ab0064e` は `git worktree add` で取り出して同手順で計測（`Tests run: 226, Failures: 0, Errors: 0, Skipped: 0`）し C0 = 1598/1611 = 99.19%、C1 = 158/160 = 98.75%。9クラスすべてで `INSTRUCTION_MISSED` / `BRANCH_MISSED` が両者一致（`YamlFileBuilder` 1/1、`YamlLoader` 12/1、他7クラス 0/0）のため**下がった箇所は無し**。報告書 §5.2〜5.5。未達2箇所は #19 の承認済み到達不能で、JaCoCo の HTML から `YamlFileBuilder.java:236`-`:237`・`YamlLoader.java:60`-`:61`・`:65`-`:66` と特定（テストは足していない） | | |
| `.rn/ntf-yaml/report-step4.md` に §6 の5項目がこの順で載っている | OK | `grep -n "^## " .rn/ntf-yaml/report-step4.md` → `## 1. 第2節5件の是正結果` / `## 2. 第3節13件の結果` / `## 3. 期待値をわざと崩す確認の結果` / `## 4. 既存テストの期待値を変えた箇所の全件` / `## 5. カバレッジ C0/C1 の計測結果と converter で落ちたテスト` の順。加えて `## 6. 未是正の食い違い`・`## 7. 指示書の想定と実測が食い違った点` を別節で追加。1ファイルにまとめている | | |
| `git status --short` が空、`tmp/` と `javac.*.args` が無い | OK | コミット・push 後の `git status --short` が空。`ls -a` に `tmp` 無し（測定で生成された空ディレクトリを `rmdir` で削除）。`find . -name "javac.*.args"` が0件。`jacoco.exec` は削除済み（`.gitignore:20` の対象でもある）。`target/site/jacoco` は `.gitignore:1` の `target/` 配下。`git worktree list` は本体1件のみ（一時 worktree は `git worktree remove` 済み） | | |
| push 済み | OK | `git push origin feature/ntf-yaml` → `8eacaa7..ad373e9  feature/ntf-yaml -> feature/ntf-yaml`。`git log --oneline -1 origin/feature/ntf-yaml` が `ad373e9`。force push はしていない | | |

## Overall Verdict

- Self-check: OK

---

## step B2: converter の帰属実測

`nablarch-testing-converter`（`/home/tie303177/work/nablarch/nablarch-testing-converter`、`60d9a2d`）は**一切変更していない**（実行前後とも `git status --short` が空を確認）。

本モジュールを2つの状態で `.m2` へ install し、それぞれ `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` を実行した。

| | 集計 |
|---|---|
| 基準（本モジュール `ab0064e`、`git worktree add` で取り出して install） | `Tests run: 605, Failures: 8, Errors: 1, Skipped: 2` |
| 完了後（本モジュール `8eacaa7` を install） | `Tests run: 605, Failures: 5, Errors: 0, Skipped: 2` |

最後に HEAD `8eacaa7` を `.m2` へ install し直して終えている（`Installing .../nablarch-testing-yaml-1.0.0-SNAPSHOT.jar to /home/tie303177/.m2/...`）。

**切り分け**

- Step 4 起因で新たに落ちた: 1件（`YamlTestCoreAdapterTest#isResourceExisting_reflectsFileExistence:370`、`Expected: is <false> but: was <true>`、2-2 起因）
- Step 4 で解消した: 5件（`YamlFormatReaderScalarTest#readsOmittedValueAsJavaNull:364` / `#readsUnquotedNullAsJavaNull:352` / `#readsUnquotedNullAsJavaNullInListMapPath:572`、`YamlFormatWriterTest#roundTrip_nullAndNullStringAndNumeric_areDistinguishedThroughRealReader:660`、`RoundTripTest#nullCell_xlsConvertsToLiteralString_yamlPreservesNull:664`(Error)）
- Step 4 と無関係に落ち続け: 4件（`YamlFormatReaderInvalidInputTest#dropsAllRowsWhenFirstRowOfTableIsEmptyObject:601` / `#keepsRowCountButLosesValuesWhenFirstRowOfListMapIsEmptyObject:628`、`YamlFormatReaderScalarTest#readsEmptyStringAsIs:505` / `#readsEmptyStringAsIsInListMapPath:584`）

指示書の既知の観測「2-1 起因の疑い: `YamlFormatReaderInvalidInputTest` 2件・`YamlFormatReaderScalarTest` 2件」は**反証された**。この4件は基準 `ab0064e` の時点で同じメッセージで落ちており、Step 4 起因ではない。

**ビルドコマンドの逸脱1件（記録）**: 基準側の install が worktree 内で `git-commit-id-plugin:2.1.15:revision` の `MissingObjectException` で失敗したため、`.git/worktrees/wt-base/` に `objects` / `refs` / `config` の symlink を張って jgit がオブジェクトを解決できるようにしたうえで、**指示どおりの install コマンドをそのまま**実行した。symlink は `git worktree remove` の前に削除済みで、`git worktree list` にも `.git/worktrees` にも残っていない。`pom.xml` は変更していない。

**手順上の誤り1件（記録）**: 完了後の install を実行する際、同じ bash 呼び出し内の `cd` が残っていたため、1回目は converter ディレクトリで `mvn install` が走り `nablarch-testing-converter` が `.m2` へ install された（本モジュールではない）。converter リポジトリのファイルは変更していない（`git status --short` は空のまま）。直後に本モジュールのディレクトリで install をやり直し、converter のテスト実行はその後に行っている。

## 本タスクで現物に当たって確かめたもの

報告書に載せた数値・テスト名・`file:line` は、check ファイルの記述をそのまま写さず、次のとおり自分で確認した。

- 実装の `file:line`: `YamlSection.java:205`・`:302`-`:307`、`YamlLoader.java:97`-`:101`・`:185`・`:200`-`:201`、`YamlTestDataParser.java:112`-`:113`・`:126`-`:129`、`YamlFileBuilder.java:196`-`:198`、`YamlMessageBuilder.java:90`・`:120`・`:159`、`YamlTableDataBuilder.java:37`-`:39`・`:89`-`:92`・`:168`-`:171`（`sed -n` で本文を確認）
- スキーマ: `:108`・`:136`・`:365`・`:410` が HEAD の位置。`git diff --stat ab0064e..HEAD` が description 4行のみ（`4 insertions(+), 4 deletions(-)`）。`:208` は未変更。`:53`・`:200` の `sendSyncTestData/{requestId}/message` 記述（`grep -n sendSyncTestData`、`json.load` で `$defs` を展開）
- テストメソッドの存在と行番号: 報告書に載せた38メソッドすべてを `grep -rn "public void <name>("` で確認
- `@Ignore`: `grep -rn "@Ignore" src/test/java/` が1件のみ。文言は原文のまま転記
- `record_type` 件数: `git grep -h 'record_type: *FW_HEADER' <rev> -- src/ | wc -l` を `ab0064e`（17）・`8eacaa7`（18）で実行。`HEADER` は両方 7
- 2-1 の既存テスト変更: `git diff 013c974^..013c974 -- src/test` を全文読み、`assert*` の期待値変更が0件であること、フィクスチャの `null` → `""` が15箇所・11テスト分であることを数えた
- 2-3 の既存テスト変更: 各テストメソッドの本文を `git show 1693cc1^:<file>` と HEAD で突き合わせ、期待値が変わったのが3テスト・4アサーションであることを確認
- 解説書（ピン `5b5c91e`、`git show` で読取）: `setup/common.rst:77`・`:81`・`:260`・`:264`、`implementation/testdata_notation.rst:1059`・`:1151`・`:1163`・`:1313`・`:1399`・`:1500`、`implementation/class_unit_test/component.rst:313`
- 依存先（ピン `3c4bd2a`）: `BasicJapaneseCharacterInterpreter` が `m.matches()`、`CompositeInterpreter` が `while (m.find())` であること
- converter: `YamlTestCoreAdapterTest.java:364`-`:370` の本文

## 確かめていないもの（報告書に明示済み）

- **変異確認（報告書 §3）の再実行**。Scope により `src/test` を変更できないため、各 check ファイルに記録された実測をそのまま載せ、再実行していない旨を節の冒頭に明記した。対象テスト名と `file:line` は現物で確認済み
- **各是正の「直す前に落ちたテスト」の再測定**。`src/main` を変更できないため再現していない。テストメソッドの存在・位置は現物で確認済み

## 指示書の想定と実測の食い違い（報告書 §7 に記載）

1. `record_type: FW_HEADER` は 17件（指示書は16件）。`HEADER` は 7件で一致
2. 3-3 は解説書どおり通った。担っているのは `CompositeInterpreter`
3. converter の4件は 2-1 起因ではない（基準時点で既に落ちている）
4. 解説書の行番号: 指示書の `testdata_notation.rst:1149` → 現物は `:1151`、`setup/common.rst:263` → 現物は `:264`（important の本文行）

## Scope 遵守

- `src/main`・`src/test`・`pom.xml`・解説書・`nablarch-testing`・`nablarch-testing-converter` はいずれも未変更（`git diff ad373e9^..ad373e9 --stat` は `.rn/ntf-yaml/report-step4.md` の1ファイルのみ）
- #34（Evaluation sign-off）には着手していない
- コミットしたのは `.rn/ntf-yaml/report-step4.md` のみ。`git add -A` / `git add .` は使っていない。本ファイル（`task-33.md`）はステージ・コミットしていない

## コーディネーター独立レビュー

Step 4 では4観点レビューを回さない（指示書 §7）。コーディネーターがコミット済み差分と報告書を独立に読み、後始末と参照点を自分で確認して検証した。

| 観点 | 判定 | 根拠 |
|---|---|---|
| 差分がタスクの範囲に収まっている | OK | `ad373e9` は `.rn/ntf-yaml/report-step4.md` の1ファイルのみ。`src/main`・`src/test`・`pom.xml` の差分なし |
| 報告書に §6 の5項目がこの順で載っている | OK | `## 1. 第2節5件の是正結果` → `## 2. 第3節13件の結果` → `## 3. 期待値をわざと崩す確認の結果` → `## 4. 既存テストの期待値を変えた箇所の全件` → `## 5. カバレッジ C0/C1 の計測結果と converter で落ちたテスト`。加えて `## 6. 未是正の食い違い` `## 7. 指示書の想定と実測が食い違った点` |
| カバレッジが下がっていない | OK | 9クラスすべてで `INSTRUCTION_MISSED` / `BRANCH_MISSED` が `ab0064e` と一致。未達は #19 で承認済みの2箇所のみ。カバレッジを埋めるテストは足していない |
| converter の帰属実測が正しい方法で行われている | OK | 基準（`ab0064e`）と完了後（`8eacaa7`）の2点で `mvn -o clean test` を実行して差分を取っている。基準側は `git worktree add` で取得し `git worktree remove` で片付け済み（`git worktree list` が本体1件のみであることをコーディネーターも確認） |
| 後始末 | OK | `git status --short` は未追跡の `checks/task-33.md` のみ（指示どおり未コミット）。`tmp/`・`javac.*.args`・`jacoco.exec` はいずれも存在しないことをコーディネーターも確認 |
| 解説書の行番号の補正が正しい | OK | コーディネーターが写しで実測。`testdata_notation.rst:1151` が `setUpMessages`・`expectedMessages` の識別子を述べる文（指示書の `:1149` は節冒頭の段落）。`setup/common.rst:264` が `fileExtensions` の important の本文（`:262` が `.. important::`、`:263` は空行）。いずれも指示先のずれであって内容の誤りではない。指示書が引く `:1163`（レコード種別）・`:1500`（空行除去）は現物と完全一致 |

### 指示書の既知観測が反証された件（重要）

指示書 2-2 の周辺および #27 の check に「#26（空行判定）起因で converter の `YamlFormatReaderInvalidInputTest` 2件・`YamlFormatReaderScalarTest` 2件が落ちている」と記録したが、**本タスクの帰属実測で反証された。** これら4件は Step 4 着手前の `ab0064e` の時点で同じメッセージで落ちており、Step 4 起因ではない。#27 の測定が基準にした `0602b39` が #26 の是正**後**のコミットだったことが誤った見立ての原因である。

**Step 4 起因の converter の失敗は `YamlTestCoreAdapterTest#isResourceExisting_reflectsFileExistence`（2-2 起因）の1件のみ。** 逆に Step 4 で5件が解消した（基準 `Failures: 8, Errors: 1` → 完了後 `Failures: 5, Errors: 0`）。この訂正は報告書 §5.6 と §7-3 に反映されている。

### 手順上の事象（記録）

(a) worktree 内の install が `git-commit-id-plugin` の jgit エラーで失敗したため、`.git/worktrees/wt-base/` に `objects`/`refs`/`config` の symlink を張って実行した（symlink は worktree 削除前に除去済み、`pom.xml` は無変更）。(b) 完了後 install の1回目が converter ディレクトリで走り `nablarch-testing-converter` が `.m2` へ install された（converter のファイルは無変更）。直後に本モジュールで install をやり直してから converter のテストを実行している。**いずれも成果物には影響しない**が、`.m2` の状態が通常と異なるため記録する。

## Overall Verdict（コーディネーター）

- コーディネーター独立レビュー: OK
- Ready to check off: Yes
