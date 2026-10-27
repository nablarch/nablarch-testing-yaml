# task-43 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| C0/C1 が計測され、第1回からの差分と下がった箇所が挙がっている | OK | HEAD `0d1db70`（`src/` は `00fc164` と同一。`837e9c5`・`0d1db70` はいずれも報告書のみの変更）で `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes`（`Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`）→ `mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec`。`target/site/jacoco/jacoco.csv` の列を合計して **C0 = 1809/1822 = 99.29%（`INSTRUCTION_MISSED` 13）・C1 = 174/176 = 98.86%（`BRANCH_MISSED` 2）**。第1回の基準 `8eacaa7` は `git worktree add --detach` で取り出し**同じ手順で測り直し**、`Tests run: 267, Failures: 0, Errors: 0, Skipped: 1` / **C0 = 1663/1676 = 99.22%・C1 = 168/170 = 98.82%** を得た（`checks/task-33.md` の記録と一致）。クラス別（9クラス全件）の `INSTRUCTION_MISSED`／`BRANCH_MISSED` が前後で完全に一致するため**下がった箇所は無し**。未達2箇所は JaCoCo の HTML から `nc`／`bpc` の付いた行を機械抽出して `YamlFileBuilder.java:246`-`:247`（第1回は `:236`-`:237`。2-1 で `+10` ずれただけで同じ箇所）と `YamlLoader.java:60`-`:61`・`:65`-`:66`（不変）と特定。報告書 §7.1 に全件表 | | |
| converter を変更せずに実行し、着手前 656件からの差分が全件挙がっている | OK | `nablarch-testing-converter`（ブランチ `ntf-test-data-converter`、HEAD `d611bec`）は**一切変更していない**。実行前後とも `git -C ../nablarch-testing-converter status --short` が空、`git rev-parse HEAD` が `d611bec6ea1eb7039be51b0d5ea202d1dfecb8cf`。着手前の基準は `3ee39c9` を worktree で取り出して `mvn -o clean install -DskipTests` し、converter で `mvn -o clean test` → **`Tests run: 656, Failures: 0, Errors: 0, Skipped: 0`**（指示書 完了条件10 の数値を本タスクで再現）。HEAD を install し直して同コマンド → **`Tests run: 656, Failures: 3, Errors: 1, Skipped: 0`**。差分**4件**を全件、落ちた理由（フィクスチャの記述と是正内容の対応）つきで報告書 §7.2 に記載。2-3・2-5 で落ちない理由も機械抽出の結果つきで記載。基準側 install の `git-commit-id-plugin` `MissingObjectException` は `#33` と同じ symlink 回避策で通し、測定後に symlink と worktree を削除（`git worktree list` は本体1件、`.git/worktrees` 無し） | | |
| 報告書が指示書 §6 の6項目をこの順で含む | OK | `grep -n "^## " .rn/ntf-yaml/report-step4-2.md` → `## 1. 2-1〜2-5 の「着手前に特定すること」の結果` / `## 2. 指示書の記述と実測が食い違った点（着手前に判明した分）` / `## 3. 第2節7件の是正結果` / `## 4. 本体を oracle にしたテストの一覧（2-1・2-4）` / `## 5. 期待値をわざと崩す確認の結果` / `## 6. 既存テストの期待値を変えた箇所の全件` / `## 7. カバレッジ C0/C1 と converter で落ちたテストの全件` / `## 8. 決めていただきたいこと・記録`（`135958e` で改題）。§6 の6項目は §1・§3・§4・§5・§6・§7 にこの順で入っている。対応表は `report-step4-2.md:76`-`:87`（`135958e` 後）。§2 と §8 は追加節。`135958e` で `## 結論`（`:15`）と `## 指示書 §4 の完了条件10項目の充足`（`:57`）を前置し、`## 付録: コーディネータへの申し送り`（`:966`）を末尾に切り出した | | |
| `git status --short` が空、push 済み | OK | コミット2件 `837e9c5`（報告書 §3〜§8 の記入）・`0d1db70`（§4.2 の出典行の訂正）。どちらも `git add .rn/ntf-yaml/report-step4-2.md` のみで、`git add -A` / `git add .` は使っていない。`git push origin feature/ntf-yaml` → `00fc164..837e9c5` および `837e9c5..0d1db70`、`git log --oneline -1 origin/feature/ntf-yaml` が `0d1db70`。force push はしていない。push 後の `git status --short` が空。`tmp/` は残っているが `.gitignore` の対象で status に出ない。`jacoco.exec` は削除済み（`.gitignore:20` の対象でもある）。本ファイル（`checks/task-43.md`）は未追跡のまま add していない | | |

## Overall Verdict

- Self-check: OK
- Craft (writing): `0d1db70` で **fail**（誤った数値2件・第1回から落ちた書式2件）→ `135958e` で全件是正。約250行の圧縮提案はスコープ外とした
- Design: 投入せず（本タスクに構造上の判断が無いため）
- Verification (fact-check): **pass**（文言訂正4件）。converter 656→`3F/1E` と C0/C1 を第1回基準を含めて隔離コピーで独立実測し、報告書の数値が全件一致 → `135958e` で4件是正
- QA: `0d1db70` で **fail**（報告書の誤った事実記述4件。実装・計測は独立実測で全件再現）→ `135958e` で全件是正
- コーディネータの独立検証: 指摘全件を実物で再現。`135958e` で `mvn -o clean test` = `Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` を自分で実行。`src/` は `00fc164` から不変（`git diff --stat 00fc164 HEAD -- src/` が空）。訂正箇所の `file:line` 25件以上を機械検証し**新たな誤りは無し**
- Ready to check off: **Yes**（`135958e`）

修正ラウンド: 1回（三軸の指摘を1回にまとめて投入。上限3回のうち1回で収束）。

---

## 指示 §8「コミット前に必ずやること」1〜5 の結果

| # | 内容 | 結果 |
|---|---|---|
| 1 | 報告書に書いたすべての数値・`file:line` 参照を機械的に検証。不一致0件 | **OK**（下記「§8-1 の機械検証」） |
| 2 | 報告書に書いたすべての事実主張を列挙し、確かめ方を1行で示す | **OK**（下記「事実主張と確かめ方」） |
| 3 | `mvn -o clean test` が BUILD SUCCESS | **OK**。`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `[INFO] Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS` |
| 4 | `git -C ../nablarch-testing-converter status --short` が空 | **OK**（空。HEAD も `d611bec` のまま） |
| 5 | `git status --short` が空 | **OK**（push 後に空） |

### §8-1 の機械検証（Python で全件抽出して突き合わせ）

| 検証 | 対象 | 結果 |
|---|---|---|
| テストメソッド名の実在 | 報告書中の `Class#method` / `Class.method` 71件 | HEAD の作業ツリー・`3ee39c9`・converter `d611bec` のいずれかに**全件実在**（未解決0件） |
| `src/…` と `../nablarch-testing/…` の `file:line` | 29件 | ファイルが存在し、指す行が範囲内かつ**非空**。問題0件 |
| `Class.java:N` 形式の参照 | 27件（重複除去後） | 1件ずつ本文を出力して目視照合。`3ee39c9` を指すもの（§1・§2）と HEAD を指すもの（§3〜§8）を区別して確認。不一致0件 |
| §1 のフィクスチャ行番号 | `tableData.yaml` 22件・`completedTable.yaml` 4件・`omission.yaml` 2件 | PyYAML の `compose()` で `3ee39c9` の各 `rows` 要素の開始行と種別（`{}`／全値 `""`／全値 null／その他）を出し、**28件すべて**行番号・エントリ名・種別が一致 |
| §1 の 2-2／2-3／2-5 のフィクスチャ行番号 | 7件 | `git show 3ee39c9:<file>` の該当行を出力して照合（`messageData.yaml:31`・`:53`・`:163`、`customFwHeaderData.yaml:9`、`fwHeaderMapData.yaml:14`・`:40`、`nativeTypes.yaml:16`）。全件一致 |
| §1 のテストメソッド行番号 | 33件（`3ee39c9`） | 機械抽出と突き合わせ、**3件の不一致**（`YamlTestDataParserTest` の `:940`→`:938`・`:1112`→`:1109`・`:990`→`:986`）を発見し報告書を訂正。訂正後は不一致0件 |
| §3 の `src/main` 行範囲 | 8件（HEAD） | `awk` で該当行を出力して照合。**4件の不一致**（`YamlMessageBuilder.java:328`-`:332`→`:327`-`:331`、`YamlFileBuilder` の `:268`→`:267`・`:245`-`:252`→`:252`-`:254`、`YamlSection` の `:196`-`:233`→`:222`-`:233`・`:430`-`:443`→`:430`-`:441`）を発見し訂正。訂正後は不一致0件 |
| §8.1 の出典訂正の件数 | 18件 | `cb82f3b` と HEAD の `src/` 全ファイルから `*.rst:N` 形式の出典を機械抽出して差分を取り、**18箇所**（`+2` ずれ13・別種5）を確定。**引き継ぎの「別種3件」は誤りで実測は5件** |

### 事実主張と確かめ方（報告書の節ごと）

| # | 事実主張 | 何を実行/参照して確かめたか |
|---|---|---|
| 1 | 着手前ベースライン `Tests run: 268 / Skipped: 1`、`@Test` 268件・`@Ignore` 1件 | `3ee39c9` の `src/test/java` から `@Test` 直後の `public void` を機械抽出 → 268件。`@Ignore` は `grep` で1件 |
| 2 | 完了時 `Tests run: 318 / Skipped: 0`、`@Test` 318件・`@Ignore` 0件 | `mvn -o clean test` の実行結果。HEAD の作業ツリーから同じ機械抽出 → 318件。`grep -rnE "^\s*@Ignore" src/` が0件、`grep -rn "import org.junit.Ignore" src/` が0件 |
| 3 | §1 2-1「末尾 `null` を置いた既存フィクスチャは0件」 | `3ee39c9` の `.yaml` 55ファイルを PyYAML でロードし、ファイル・電文7セクションの `records[].rows[]` の末尾要素が Python `None` である行を数えた → 0件 |
| 4 | §1 2-2「`records` を2つ以上書いた電文エントリは3件」 | 同スキャン → 3件。ファイル名・セクション・`id` まで一致 |
| 5 | §1 2-2「`messageData.yaml` は16箇所から読まれる」 | `git show 3ee39c9:…/YamlTestDataParserTest.java \| grep -n messageData` → 17ヒット。うち `:1027` は javadoc なので読み出しは16箇所。うち `:1816`・`:1828` が同一テスト内のため落ちるテストは15件（#37 の実測と一致） |
| 6 | §1 2-3「既定4つ以外のキーは3件、`reader.fwHeaderfields` の設定は0件」 | 同スキャン → `customField`・`customProjectKey`・`boolFlag` の3件。`3ee39c9` の `src/test` 全ファイルを `grep -n fwHeaderfields` → 0ヒット |
| 7 | §1 2-4「`{}` 11件・全値 `""` 14件・全値 null 2件・マーカーのみ1件」 | 同スキャン（テーブル系4セクション＋`list_maps`）→ 11 / 14 / 2 / 1。全値 `""` 14件はファイル・セクション・エントリ名まで報告書の表と一致 |
| 8 | §1 2-5「フィクスチャ1件・テスト1件」 | `3ee39c9` の `.yaml` を PyYAML でロードして値に `\` ＋ `r` の2文字を含むものを探索 → `nativeTypes.yaml` の `LITERAL_CR_COL` 1件。`.java` を `grep -F '\\r'` → `YamlTableDataBuilderTest.java` の5行（うち assert は `:603`） |
| 9 | §2 1「POI の利用は0件（`grep` の2ヒットは `point` の一部）」 | `3ee39c9` の `src/test/java` 全ファイルを `grep -nE 'poi\|Workbook\|XSSF\|HSSF'` → 2ヒット。本文を出力して `required-decimal-point`・`must point to` であることを確認 |
| 10 | §3.2 の各是正の `src/main` 変更箇所（`file:line` と本文） | `awk` で HEAD の該当行を出力して照合（上表「§3 の `src/main` 行範囲」） |
| 11 | §3.2 の「取り消したときに落ちるテスト」5表（4・4・8・12・15件） | `git worktree add --detach <scratchpad>/mut HEAD` に `src` を複写し、是正1件ぶんの `src/main` 変更だけを取り消して `mvn -o clean test` を5回実行。surefire の `[ERROR] Failures:` / `Errors:` 節をそのまま転記。worktree は `git worktree remove --force` 済み |
| 12 | §3.2 2-1「本体の実装をそのまま呼んでいる」 | `../nablarch-testing/src/main/java/nablarch/test/NablarchTestUtils.java:245`-`:280` を開いて `trimTailCopy`（`:273`）と `trimTail`（`:251`-`:263`）を確認 |
| 13 | §3.2 2-6「`@Ignore` 0件」 | `grep -rnE "^\s*@Ignore" src/` が0件、`grep -rn "import org.junit.Ignore" src/` が0件、`mvn -o clean test` が `Skipped: 0`。`src/` に残る `@Ignore` 文字列は `YamlMessageBuilderTest.java:1125` の javadoc 1件のみ（`grep -rn "@Ignore" src/`） |
| 14 | §4.1 の oracle 表（F1〜F6・M1・S2 の入力と本体の値） | フィクスチャ `YamlTrailingNullOracleTest/trailingNull.yaml` を全文読み、テスト側の `assertFileCase("F1", "x", "", "")` 等 8 行を `YamlTrailingNullOracleTest.java` から抽出。`assertFileCase`（`:375`-`:382`）が `assertRecordValues`（本体が解説書どおりか）→ `assertSameAsOracle`（YAML と本体の突合）の2段であることをソースで確認 |
| 15 | §4.1「M1 は是正前も一致していた」 | §3.2 2-1 の実測（`trimTailCopy` を取り消しても M1 のテストは落ちない）。落ちたのは F1・F4・F6・S2 の4件 |
| 16 | §4.2 の oracle 表（T1〜T5・L1〜L5） | フィクスチャ `YamlBlankEntryOracleTest/blankEntry.yaml` と `YamlBlankEntryOracleTest.java` の `assertTableCase` / `assertListMapCase` の引数、T5・L5 の個別 assert（`:329`-`:334`・`:443`-`:448`）を読んだ |
| 17 | §5.1 の R1〜R5 | #11 と同じ実測（同一の worktree 手順） |
| 18 | §5.2 の各タスクの変異確認 | **本タスクでは再実行していない。**報告書本文に「未再実行」と明記した。対象テスト名と `file:line` は HEAD の現物で存在を確認済み（上表「テストメソッド名の実在」） |
| 19 | §6.1 の総数（268 / 318 / 251 / 17 / 67） | `3ee39c9` と HEAD の `@Test` メソッドを機械抽出して集合演算。268 − 17 ＋ 67 ＝ 318 |
| 20 | §6.2〜6.3（期待値変更7件・入力のみ変更9件） | 両リビジョンの各メソッド本体（空行除去）を比較し、`assert*(` / `fail(` / `is(` / `containsString(` / `hasItem` を含む行の並びが変わったものを「期待値変更」、変わらないものを「入力のみ」とした。**この基準による7件のうち2件は `is(1)` が不変で assert の説明文だけが変わっている**（QA が指摘）。`135958e` で見出しを「7件（期待値リテラル5件・説明文のみ2件）」に改め、表に種別列を足した |
| 21 | §6.4 の17件の行方 | 削除側17件それぞれについて HEAD 側の後継を特定（`git show 3ee39c9:<file>` と HEAD のソースを突き合わせ）。`malformedFwHeaderRowsThrowsException` → `nonMapFwHeaderThrowsExceptionWithTypeName`（`:776`）、`lineSeparatorIsInterpretedOnlyByYamlParser` → `yamlEscapeBecomesCr`（`:602`）はソース本文で確認 |
| 22 | §6.6 の突き合わせ表 | 2-2 の 16/15 の差は #5 のとおり。2-3 の4件・2-4 の14件は `checks/task-38.md`・`task-39.md` の実測記録（**本タスクでは再実行していない**旨を本文に明記） |
| 23 | §7.1 のカバレッジ数値と未達2箇所 | #1 のとおり。未達行は JaCoCo HTML の `<span class="nc\|pc bpc">` を正規表現で抽出 |
| 24 | §7.1「`YamlSection` の分岐が 52→50 に減ったのはコードから分岐が消えたため」 | HEAD の `YamlSection.java:234`-`:236` が `return castMap(row).isEmpty();` の1行、`3ee39c9` は `for` ＋ 2条件の分岐であることをソースで確認。`BRANCH_MISSED` は前後とも0 |
| 25 | §7.2 の converter 4件と落ちた理由 | #2 の実行結果と、converter の該当テスト・フィクスチャ（`YamlFormatReaderInvalidInputTest.java:743`-`:765`、`YamlFormatReaderScalarTest.java:224`-`:239`・`:582`-`:598`（**報告書に書いたフィクスチャの範囲 `:582`-`:590` は1行ずれで、正しくは `:583`-`:591`**。Verification が指摘し `135958e` で訂正）、`YamlFormatReaderRealFileTest.java:638`-`:665`）を開いて確認。失敗メッセージは surefire の出力をそのまま転記 |
| 26 | §7.2「2-3 で落ちない理由」 | converter の `src/test` 全 `.java`／`.yaml` から `fw_header` 直下のキーを機械抽出 → `requestId`・`userId`・`resendFlag`・`dateSent` の4種。`dateSent` の箇所（`YamlFormatWriterModelTest.java:762`）がモデル直組みで本モジュールを通らないことをソースで確認 |
| 27 | §7.2「2-5 で落ちない理由」 | converter を `grep -rn -F '\\r' src/` → 13件（うち `src/main` 1件、`src/test` は12行）。1件ずつ本文を読み、YAML のダブルクォート内エスケープ（パーサが実制御文字に展開）か Excel 経路か assert の説明文かを判別。**当初この3分類が `YamlFormatWriterTest.java:337` を取りこぼしていた**（Verification が指摘）。`135958e` で `src/test` 12行・**14箇所**（`:312` と `:544` は1行に2箇所）の全件を3群で閉じた |
| 28 | §8.1 の18箇所（`+2` ずれ13・別種5） | 上表「§8.1 の出典訂正の件数」。原因側は `git -C ../nablarch-document show 6ba3c83 -- '*testdata_notation.rst'` のハンクヘッダ（`@@ -1150,7 +1150,7 @@` の同数置換と `@@ -1296,6 +1296,8 @@` の2行挿入）で確認。`git merge-base --is-ancestor 6ba3c83 afa4f9e` が真 |
| 29 | §8.2 の `trimTailCopy` の実測表 | `mvn -o dependency:build-classpath` で得たクラスパスに対し `NablarchTestUtils.trimTailCopy` を直接呼ぶ 12 行の Java を書いて実行。`["x", null, ""]` → `["x"]` を確認（`null` は保持されない）。解説書 `:889` の逐語は `git -C ../nablarch-document show afa4f9e:<path>` を行番号つきに展開して確認 |
| 30 | §8.4（旧 §8.3）「スキーマで規則に触れているのは `:293` の1箇所だけ」 | **当初この欄と報告書に「`grep` の結果は `:293` の1件のみ」と書いたが偽であった。**実測は2件（`:293` `record-separator`／`:333` `field-separator`）。`:333` は「YAML の `"\t"` は実タブに展開されるためバックスラッシュをエスケープする」の話で 2-5 の規則ではないため、**主張（規則に触れているのは `:293` だけ）は真**。`135958e` で根拠の書き方を訂正した。規則が掛かるべき `:108`・`:136`・`:216`・`:380`・`:433` の本文を出力して未記載であることを確認 |
| 31 | §8.4「ピンは0件・行番号出典は60箇所16ファイル」 | `grep -rl "afa4f9e\|nablarch-document@" src/ \| wc -l` → 0。`grep -rEo '[a-z_/]+\.rst:[0-9]+' src/ \| wc -l` → 60、`grep -rEl '\.rst:[0-9]+' src/ \| wc -l` → 16。節見出し方式の先例4件はソースを開いて確認 |
| 32 | §8.5「該当テストは存在しない・steering.md に注記済み」 | `grep -rn "buildListMapRows_unknownCharacterTypeIsNotConverted" src/` → 0件。`.rn/ntf-yaml/steering.md:1138`-`:1158` を読み、`:1141`・`:1155` に「#41 で失効」の注記があることを確認 |

### 確かめていないもの（報告書に明示済み）

- **§5.2 の「期待値の崩し」の再実行**（#36〜#40 の各タスクの実測記録。本タスクでは未再実行）。代替として §5.1 に実装側の変異5件を本タスクで測り直した
- **§6.6 の 2-2〜2-4 の「当時落ちた件数」**（#37〜#39 の実測記録。本タスクでは未再実行）

## スコープ遵守

- 変更したのは `.rn/ntf-yaml/report-step4-2.md` 1ファイルのみ（`git show --stat 837e9c5`・`0d1db70`・`135958e`）
- `src/main`・`src/test`・`pom.xml` は未変更（`git diff --stat 00fc164 HEAD -- src/` が空）
- `nablarch-testing`・`nablarch-testing-converter`・`nablarch-document` はいずれも未変更（3リポジトリとも `git status --short` が空）
- `.rn/ntf-yaml/steering.md` に触れていない。`.rn/ntf-yaml/report-nablarch-document-discrepancies.md` にも触れていない（§8.2 は報告書側に書き、起票の判断はコーディネータに委ねた）
- 本ファイル（`checks/task-43.md`）はコミットしていない

## QA / Expert Review

投入した軸: QA（常時）／Craft (writing)（成果物が文書のため）／Verification (fact-check)（報告書の数値がユーザーの #44 判定材料になるため）。
Design は本タスクに構造上の判断が無いため投入していない。いずれも `0d1db70` に対する敵対的レビューで、隔離コピー（`git worktree add --detach`／`git clone --no-hardlinks`）での独立実測を課した。

### 各軸の判定（`0d1db70` 時点）

| 軸 | 判定 | 要旨 |
|---|---|---|
| Verification (fact-check) | **pass**（文言訂正4件） | converter 656→`Failures: 3, Errors: 1` と C0/C1 を**第1回基準 `8eacaa7` を含めて**隔離コピーで測り直し、報告書の数値が全件一致。`file:line` 全件検証で範囲外・無関係は1件のみ |
| QA | **fail** | 実装・計測は独立実測で全件再現。差し戻し理由は報告書の誤った事実記述4件 |
| Craft (writing) | **fail** | 第1回にあって第2回で落ちた書式（`## 結論`・出典規約の宣言）に実害。誤った数値・grep 結果 |

### レビューが挙げ、コーディネータが実物で再現した指摘（全件）

誤った記述（`135958e` で全件訂正）:

| # | 箇所（`0d1db70`） | 指摘 | コーディネータの再現 |
|---|---|---|---|
| 1 | `:39`-`:41` | `nullValue()` の内訳 7+2+1+1=11 が本文「10箇所」と矛盾。`YamlColumnOmissionTest` は6件 | `git grep -n 'nullValue()' 3ee39c9 -- src/test` → 全体10行、同クラス6行（`:190`・`:242`・`:251`・`:335`・`:357`・`:361`）。7 は import 行 `:26` を数えた値 |
| 2 | `:718` | 「`grep -n 'バックスラッシュ'` は `:293` の1件のみ」が偽 | 同コマンドで2件（`:293`・`:333`）。主張自体は真、根拠が偽 |
| 3 | `:505` | 「235件は本体も入力も触っていない」が偽 | `git diff 3ee39c9 HEAD -- .../omission.yaml` で `s4a`(`:55`)・`s4b`(`:73`) の先頭行が `NULL_COL: ""`→`{}`。`insertedValueDependsOnRowOrder` は本体不変でこれを読む。`checks/task-39.md` の表 #14 が同じことを記録済みで §6.7 とも自己矛盾 |
| 4 | `:455` | 「期待値を変えた7件」のうち2件は `is(1)` 不変・説明文のみ | `git diff 3ee39c9 HEAD -- .../YamlSectionTest.java` で `keepsRowHavingOnlyMarkerColumnValue`・`keepsRowHavingOnlyNullValues` を確認 |
| 5 | `:422` | 「#36〜#40 の各タスクで実測済み」の根拠が辿れない。11件が check ファイルにも43件にも現れない | `grep -rn "M21\b\|M20\b\|M19\b" .rn/` のヒットは `task-39.md:122` のその文自身のみ。参照先の記録が `.rn/` に存在しない |
| 6 | §7.2 #3 | converter フィクスチャの行範囲 `:582`-`:590` が1行ずれ | `git show d611bec:.../YamlFormatReaderScalarTest.java` で `:582` = `// Given / When`、YAML 文字列は `:583`-`:591` |
| 7 | §7.2 | 「2-5 で落ちなかった理由」の3分類が `YamlFormatWriterTest.java:337` を取りこぼし | `git grep -nF '\\r' d611bec -- src/test` → 12行 |
| 8 | `:10`・`:192`・`:553` | 「HEAD `00fc164`」が実 HEAD と食い違う | `git rev-parse HEAD` = `0d1db70`。`00fc164..HEAD` の2コミットは報告書のみ |

構造の是正（`135958e` で実施）:

| # | 指摘 | 実害の再現 |
|---|---|---|
| B1 | `## 結論` 節が無い（第1回は `report-step4.md:5`-`:15`） | 承認判断の最大材料である converter 回帰4件が `:588`（763行中77%地点）、恒久的仕様差 T5・L5 が `:396` にあった |
| B2 | 出典規約の宣言が無い（第1回は `report-step4.md:16`） | 本文「`grep -rn 'fwHeaderfields' src/test` が0ヒット」は HEAD では **41ヒット**（`3ee39c9` なら正しい）。`YamlSectionTest.java:473` も HEAD では無関係な javadoc |
| B3 | 指示書 §4 の完了条件10項目の充足表が無い | 冒頭の対応表は §6（報告の6項目）だけが対象 |
| B4 | `:157`「件数は是正後の実測で確定する」が未回収。`:147`-`:148` の「2件相当」「ほか」が閉じていないのに `:515` で「実測14件。一致」と断定 | — |
| C1 | converter 4件の是正主体の推奨と「下流が BUILD FAILURE のまま」の明示が無い | — |
| C2 | §8 が本文6箇所で「判断はコーディネータ」と書き、発注者に判断材料を返さず宛先を転送している | — |

**スコープ外とした指摘**: Craft が提案した約250行の圧縮（§1 の着手前メモ143行、§3.2 の全件表、§5.1 の重複表、§7.1・§6.4 の表）。誤りではなく好みの範囲であり、大規模な組み替えは新たな誤りを生むリスクの方が大きいと判断した。

### 実装エキスパートがレビュー指摘を超えて自ら見つけた誤り（4件）

1. §6.7 の `customFwHeaderData.yaml` の記述が誤り。実際の差分は冒頭コメントのみでデータ本体は不変（設定は §6.3 のテスト側 Given）
2. §1 2-5「Java ソース中の `\r` リテラルは2箇所」は grep では検算できない（`git grep -nF '\\r' 3ee39c9 -- 'src/*.java'` は5行で、`:584`・`:585`・`:587` は javadoc。主張自体は真）
3. A5 の11件のうち `buildListMapRows_yamlEscapeBecomesCr` は改名前の名前でなら `#40` の M6 が実測済み（`task-40.md:113`）。名前照合のため残ったもの
4. A7 の `YamlFormatWriterTest.java:337` は YAML エスケープ群の3件目ではなく4件目（`:312` の第2引数も YAML 記法）

### 修正後（`135958e`）のコーディネータ独立検証

| 検証 | 結果 |
|---|---|
| `mvn -o clean test`（自分で実行） | `Tests run: 318, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` |
| `git diff --stat 00fc164 HEAD -- src/` | 空。`src/` は `00fc164` から不変 |
| `git diff --name-only 0d1db70 HEAD` | `.rn/ntf-yaml/report-step4-2.md` 1ファイルのみ |
| `git rev-parse HEAD` / `origin/feature/ntf-yaml` | ともに `135958e`。`git worktree list` は本体1件 |
| 誤り1（`nullValue()` 6件・`assertNull` 31箇所） | `git grep` で 10 / 6 を確認。`assertNull` は33行−import 2行=31、クラス別 `YamlTestDataParserTest` 10・`YamlMessageBuilderTest` 6・`YamlTableDataBuilderTest` 15 が実測と一致 |
| 誤り2（grep 2件・`:333` の内容） | 報告書 `:885`-`:886` が2件返すことと `:333` が field-separator である旨を明記していることを確認 |
| 誤り6（`:583`-`:591`） | converter `d611bec` の現物で `:582` = `// Given / When`、`:583` = `YamlFixture.read(...)`、`:591` = `V: ""` を確認 |
| 誤り7（12行・14箇所） | `git grep -nF '\\r' d611bec -- src/test` → 12行 |
| §5.3 の11件の `file:line` | `YamlBlankEntryOracleTest.java` の `:232`・`:265`・`:282`・`:353`・`:384`・`:400` と assert ヘルパ `:514`・`:517`・`:530`・`:535`、`YamlTableDataBuilderTest.java:1908`／`:1916`、`YamlSectionTest.java:330`／`:331` を `sed -n` で全件確認 |
| §6.5 の4件の `file:line` | `YamlColumnOmissionTest.java:206`、`YamlMessageBuilderTest.java:1436`、`YamlTableDataBuilderTest.java:547`・`:572` を全件確認 |
| §1 の `:174`・`:187`（B4 の閉じた2件） | `3ee39c9` の宣言行であることを確認（HEAD ではなく §1 の宣言どおり `3ee39c9` 基準） |
| §1 の `omission.yaml:55` | `3ee39c9` では `NULL_COL: ""`、HEAD では `{}`。§1（`3ee39c9`）と §6.5／§6.7（`00fc164`）で同じ行番号を使うのは差分が1対1置換のため。両方とも正しい |
| 指示書 §4 の10項目の逐語 | `nablarch-document@origin/ntf-yaml-support`（`ef3a914`）の `:259`-`:269` と逐語一致（項目10のみ末尾を省略） |
| converter の共有ブランチ名 | `git -C ../nablarch-testing-converter branch` → `* ntf-test-data-converter`、HEAD `d611bec`、`git status --short` 空 |

**新たな誤りは見つからなかった。** `#38` で3ラウンド続いた「修正のたびに新しい偽の文が入る」パターンは、今回は再現していない。

