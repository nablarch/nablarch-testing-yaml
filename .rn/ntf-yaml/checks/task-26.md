# task-26 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| step C の実測結果（実行コマンドと生の出力）が記録されており、`TestDataParsingTemplate.isBlankLine` 経路の発火有無が「未確認」でなく実測で判定されている | OK | `report-task-26-isblankrow.md` §3.2／§3.3／§3.4。実行コマンドと生の出力を全文掲載。判定は「メソッドは発火する（5,852 行中 1 行）が、Java null は 147,632 セル中 0 件のため `StringUtil.isNullOrEmpty(Collection)` の null 扱いの枝は踏まれない」。Excel 61 ブック / 246 シート全件走査。 | | |
| 3ケースを区別する案が、判別条件のレベルで書かれている | OK | 同 §4.1 に判別条件2つ（値が0個 / 全値が「Java null でない空文字」）と実装コードを提示。§4.2 の表で (a) `{}` 落ちる・(b) 全 `""` 落ちる・(c) 全 Java null 残る を実測出力付きで対応付け。(c1) アンクォート `null` と (c2) キーのみ値省略が SnakeYAML で同一の `{A=null,B=null}` になることを §2.3 の実測で提示。 | | |
| `resolveColumns` への波及の見立てと、解説書 `:818` との整合の見立てがある | OK | 同 §5.1 に 4 パターンの実測出力（現行 columns vs 案 columns）、§5.2 に `testdata_notation.rst:818`（`76e6e61`）逐語と整合の見立て（矛盾しない・整合が改善する）、§5.3 に追随が要る既存テスト一覧。 | | |
| `src/main` が変更されていない（報告のみのタスク） | OK | `git status --porcelain` の全件が `.rn/ntf-yaml/` 配下のみ。`src/main` 配下に差分・未追跡ファイルなし。実測用コードはすべて scratchpad（`/tmp/claude-1000/.../task26-impl/`）に置いた。 | | |

## レビュー（本指示書 §6 の4観点。rn 既定レビューとは別物・2026-08-26 ユーザー明示）

各観点は別サブエージェント。全プロンプトに §6 の3点（実測コマンドで裏付けよ・推測で書くな／付属の検証スクリプトを正解として使わず独立に組め／敵対的にレビューせよ）と「本指示書自体もレビュー対象」を明記。rn 既定レビュー（QA / Design / Craft / Verification）は #26 では回していない（案の方針の妥当性は §5 の user 検証ゲートが担うため。steering.md の Rules に判断を記録）。

### ラウンド1 の結果

| 観点 | 総合判定 | must | should | nit |
|---|---|---|---|---|
| A 充足 | pass | 1 | 2 | 3 |
| B 整合 | pass | 1 | 3 | 2 |
| C 規約 | pass | 0 | 4 | 4 |
| D 検証の妥当性 | **fail** | 2 | 3 | 1 |

**4観点すべてが、報告書 §3.2／§3.3 の実測を独立に再現した**（`cells=147632 nullCells=0` / `isBlankLine fired=1 ofWhichContainedJavaNull=0` / サンプル `RequestTestingSendSyncBatchTest.xls#testPaddingRemoved`）。観点D は本物の `parse`→`readTestData` を駆動（リフレクション不使用）、観点B は実 `.xls` を HSSF で生成して本物の `ListMapParser`+`NullInterpreter` に通し、いずれも同じ結果を得た。

### Completion criteria への観点判定

| Criterion | A | B | C | D |
|---|---|---|---|---|
| step C の実測結果が記録され、`isBlankLine` 経路の発火有無が実測で判定されている | OK | OK | OK | OK |
| 3ケースを区別する案が判別条件のレベルで書かれている | OK | OK | OK | OK |
| `resolveColumns` への波及の見立てと解説書 `:818` との整合の見立てがある | OK（穴あり） | OK（欠落あり） | OK | OK（内容に誤り → D-1） |
| `src/main` が変更されていない | OK | OK | OK | OK |

### must（#27 着手前に解消が要る）

| # | 観点 | 指摘 | 状態 |
|---|---|---|---|
| M1 | A・B・D | §5.3 の「落ちるテスト」一覧が不足。`YamlTableDataBuilderTest.java:1443 buildListMapRows_allBlankRowsReturnsEmptyList` が漏れ（10件 → 11件）。fixture 出典も不正確で、`:1272` の fixture は `completedTable.yaml:29`-`:41` にあり報告書が挙げた `tableData.yaml` には無い。fixture 判定が変わるエントリは実測12件 | **未対応**（修正ラウンド1で対応予定） |
| M2 | D | §5.1「変わるのは『先頭側の行が全 Java null』のときだけ」は偽。**空文字と Java null の混在行が先頭に来るとカラムが消失する。**実 fixture で発生（`tableData.yaml` `group_id=blankValueRowLeading` で `[PK_COL1, PK_COL2, VARCHAR2_COL, NUMBER_COL, NUMBER_COL2]` → `[PK_COL1, VARCHAR2_COL]`、`list_maps` `blankValueRowLeadingListMap` で `[KEY1, KEY2]` → `[KEY9, KEY8]`）。マーカーのみ null 行も `[[NO]]` だけになる | **ユーザー判断待ち**（2026-08-26 報告済み。仕様の帰結として受け入れるか） |

### should

| # | 観点 | 指摘 | 状態 |
|---|---|---|---|
| S1 | A・B・C・D | Javadoc 是正箇所が7箇所でなく9箇所。`src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:108`／`:136` が漏れ。是正は #30 の担当だが依存として申し送りが要る | 未対応（修正ラウンド1） |
| S2 | B | `resolveColumns` の本モジュール外の呼び出し元 `nablarch-testing-converter/src/main/java/nablarch/test/tool/converter/yaml/YamlFormatReader.java:498` が未検討。案でむしろ reader と converter のカラム集合の乖離が解消し、converter fixture への影響は0件 | 未対応（修正ラウンド1） |
| S3 | D | §3.2 の「判定」が実測の射程を超えている。`PoiXlsReader` は Java null を返しうる（`<t>` を持たない共有文字列 `<si/>` で `PoiXlsReader.java:125` が NPE。実 xlsx で再現）。母集合のセル種別は NUMERIC/STRING/BLANK のみで FORMULA/BOOLEAN/ERROR は0件 | **ユーザー判断待ち**（`nablarch-testing` 側の既存欠陥。別途起票か） |
| S4 | A | 非 String キーの行（`- 1:` `- true:`）が案で残り `extractRows` で `ClassCastException`。スキーマ検証は通過する。潜在バグ自体は既存だが案が到達経路を広げる | **ユーザー判断待ち**（別課題起票か #27 で対処か） |
| S5 | C | §3 の実測が報告書だけでは追試できない。プローブ本体が `/tmp` にありセッション消滅で失われる | **対応済み**（`probes/task-26/` に保全。下記） |
| S6 | D | §3.4 の Excel 突合がスタブリーダ。指示書 §4-1 やること4は「Excel 形式でも組み、両形式の結果が一致することを実測で示す」を逐語で求める | **ユーザー判断待ち**（#27 の完了条件2 でやり直す方針を提案済み） |
| S7 | A・B・C | §7「その他すべて実測と一致」が誤り。指示書 §4-1 の `notation.rst:828`-`:833` の要約が不正確 — 解説書は「空文字 → `""`」と「**日付型カラムの**空文字 → `""`（null 扱い）」を別行に書き分けており、指示書は日付型限定の但し書きを空文字全般へ広げている | 未対応（修正ラウンド1。指示書への反例として追加） |
| S8 | C | §6 に javadoc 是正後の文案（`{@code}`／`<p>` 込み）が無く、#27 が書式を推測することになる | 未対応（修正ラウンド1） |

### nit

`~` が Java null でなく文字列 `"~"` になる点の記録（実装エキスパートが自主記録）／§4.1 の `rowMap.isEmpty()` ガードは挙動上冗長で「無いと保証が崩れる」という書きぶりが不正確（観点C が差分ゼロを実測）／§4.2 の表に空文字キー・`- ` 単独・`- []`・非 String キーが無い／§3.3 の「通過セル数 81,873」の数え方が本文に無い／§3.3 のリフレクションは同一パッケージにクラスを置けば不要。

### 却下した指摘

| 観点 | 指摘 | 却下理由 |
|---|---|---|
| C | `checks/task-26.md` が未コミット・steering 未更新でリポジトリの完了コミット規約と揃っていない | **invalid（前提が事実と異なる）。** rn の `task-execute-workflow.md` は「The expert does not commit it. The coordinator ... commits the file as part of its ledger — on the post-Verify steering check-off commit」と定めており、実装エキスパートが check ファイルをコミットしないのは規定どおり。steering の check-off も Verify 完了後 |

### 本指示書（`c6559eb`）への反例（レビューで確定した全件）

1. §4-1「`TestDataParsingTemplate.isBlankLine` のこの経路は実際には発火しないと考えられる」は不正確。メソッド自体は発火する（5,852行中1行）。発火しないのは `StringUtil.isNullOrEmpty(Collection)` の Java null 枝（実装エキスパート＋4観点すべてが独立に確認）
2. §4-1 の `notation.rst:828`-`:833` の要約が解説書と不一致（S7。A・B・C が独立に検出）
3. §4-1 の3ケース分類 (a)(b)(c) が入力空間を覆っていない。「一部が空文字・一部が Java null」の混在行がどれにも入らないが、実 fixture に存在し案の採否で挙動が変わる主戦場になっている（観点A）
4. §4-1「同ファイルの Javadoc 2箇所」は範囲が狭い。実際は `YamlSection` 3箇所・`YamlTableDataBuilder` 4箇所・スキーマ2箇所（観点B・C）
5. §4-1 の「`PoiXlsReader` が返す要素は Java null にならない」は反例が構成できる（S3）。ただし指示書は「確認した範囲では」と留保付きのため指示書の誤りではなく、留保なしの「判定」へ格上げした報告書側の問題（観点D）

## Overall Verdict

- Self-check: OK
- 観点A 充足: pass（must 1・should 2・nit 3）
- 観点B 整合: pass（must 1・should 3・nit 2）
- 観点C 規約: pass（must 0・should 4・nit 4）
- 観点D 検証の妥当性: **fail**（must 2・should 3・nit 1）
- rn 既定レビュー（QA / Design / Craft / Verification）: N/A（#26 では回さない判断。steering.md の Rules に記録）
- Ready to check off: **No** — 観点D が fail。must 2件のうち M2 と should 3件（S3・S4・S6）がユーザー判断待ちで、判断が案の中身を変えうるため修正ラウンド1を保留している

## 証拠の保全

報告書 §3 が引用する実測プローブのソースを `.rn/ntf-yaml/probes/task-26/` に保全した（観点C の指摘 S5。原本は `/tmp` 配下でセッション消滅とともに失われるため）。読み手が同じ場所を開いて追試できる状態にした。
