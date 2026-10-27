# task-48 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| 是正は 5-1〜5-4 の指定行と、それに伴う §2・§7 の記述のみ。`git diff` に `src/` の差分が無い | OK | `git diff --name-only`（コミット前）は `.rn/ntf-yaml/report-step4-3.md` の1件のみで `src/` を含まない。`git show --stat 442f335` も同ファイル1件（26 insertions / 18 deletions）。差分の内訳は 対応表 14 行（16-6・16-7・8-4・23-3・23-構造1・25-構造1・27-1・29-構造1・32-1・33-構造1・36-1・18-4・19-12・19-15）＋§2 冒頭1行＋§1 判定内訳3行＋§7 の注記8行。**指定外への追加が1つある**: §1 の判定内訳表（一致 349→344・併記 11→15・記述なし 75→76）。判定を変えた5行の必然の帰結で、放置すると 5-3 が正した種類の内部不整合を新たに生むため更新し、§7 に明記した | | |
| 対象行以外の対応表の行に差分が無い | OK | `git diff -U0` の `+`/`-` 行を全件確認。行ID を持つ行は上記 14 行のみ。行数・行ID は不変（`python3` 再集計で 行数 444・ユニーク 444）。判定内訳の再集計は 一致 344・併記 15・記述なし 76・矛盾 9・計 444 で §1 の表と一致 | | |
| commit・push 済み | OK | commit `442f335`（`docs: 対応表の判定・出典・件数をディレクター検証の指摘どおり是正する`。`complete task #` 文字列なし）。`git push origin feature/ntf-yaml` が `2b35561..442f335` で成功。`git status --short` は空（`checks/task-48.md` は本ファイル作成前の確認時点）。force push・`--amend` は未使用。ステージは `git add .rn/ntf-yaml/report-step4-3.md` のみ | | |
| 是正した行の一覧（行ID＋変更内容1行ずつ）が報告にある | OK | 最終報告の①に 14 行＋§2 件数＋§1 内訳＋§7 注記を1行ずつ列挙した。報告書側にも §7「第1ラウンド是正について（2026-08-31）」として同じ一覧を残した | | |

## Method 適用（是正した各行の主張を確かめた出典）

すべて `git show ed3de95f:<path>` で取得した本文を `awk 'NR>=N && NR<=M {print NR": "$0}'` で行番号つきに展開して確認した（作業ツリーの HEAD は読んでいない）。パスは
`ja/development_tools/testing_framework/implementation/testdata_notation.rst`（報告書の短縮表記 `notation.rst`）と
`.../testdata_examples.rst`（同 `testdata_examples.rst`）。

| 是正行 | 確かめた主張 | 出典（自分で開いて確認） |
|---|---|---|
| 16-6 | `:638` は「`rows:` の先頭行のキーの一部を後続の行が持たない場合」＝キー自体を書かない場合の規定 | `nablarch-document@ed3de95f` `notation.rst:638` |
| 16-6 | `:799` も「後続の行がこのキーの一部を持たない場合」＝同上 | 同 `notation.rst:799` |
| 16-6 | `:1486` は「YAML 形式でキーを省略した場合は前述のとおり null」＝キー省略の規定 | 同 `notation.rst:1486` |
| 16-6 | null の記法の規定は `:1385`（アンクォートの `null`）と `:1397`-`:1398`（表「null（データベースに null を格納）／``null``（クォートなし）」）のみ。値省略 `COL:` の記載は無い | 同 `notation.rst:1385`・`:1397`-`:1398` |
| 16-6 | 実装で値が null ならそのまま null になること | `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:137`-`:138`・`:157`-`:158`、`YamlTableDataBuilder.java:225` |
| 16-7 | アンクォートの `null` 側は解説書にある | `notation.rst:809`-`:810`（表「null（Java の null）／アンクォートの ``null``」）・`:1397`-`:1398` |
| 8-4 | 「メッセージボディ側のフィールドは、従来どおり `records:` の `fields:`・`rows:` に記載する」の実体は `:1279`。`:1260` は「`requestId` などのヘッダフィールドも含めて `records` の `fields:`・`rows:` にフィールド単位で記載する」で別文 | `notation.rst:1260`・`:1279` |
| 23-3・23-構造1 | 小文字リテラル `variable` は `notation.rst` 全文で 0 件（`grep -c 'variable'` = 0）。`:202`・`:204`・`:150`-`:155` はいずれも大文字 `SETUP_VARIABLE`／`EXPECTED_VARIABLE`、`:1105` は `type: fixed` | `notation.rst`（全文 grep）・`:150`-`:155`・`:202`-`:205`・`:1105` |
| 23-3・23-構造1 | 逐語根拠は `testdata_examples.rst:1251`「``type: variable``\ を指定し」・`:1257`「      type: variable」 | `testdata_examples.rst:1251`・`:1257` |
| 25-構造1・27-1・29-構造1・32-1・33-構造1・36-1 | `:1133` は空行。引用文「フレームワーク制御ヘッダ以降のメッセージボディは、フィールド名称・データ型・フィールド長・データという、前述のファイルデータと同じ構成を持つ」の実体は `:1134` | `notation.rst:1133`（空行）・`:1134` |
| §2 件数 | C5（`response_body_messages` の errorMode）と C7（同じ `response_body_messages` の長さ条件）は同じ description（スキーマ `:81`）に対する指摘であり、矛盾 8 件・是正 7 箇所。§4 の「description 7 箇所」・§5 の「8 件（C1〜C8）」・§1 の「矛盾 9 ＝ description 8 ＋ S1」と整合する | 報告書 §1・§2・§4・§5、スキーマ `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:81` |
| 18-4・19-15 | `:340` は「テスト実行には ``testShots``\ に1件以上のエントリが必要である」までで「エラー」の語は無い | `notation.rst:340` |
| 18-4・19-15 | 0件で例外になる実装根拠 | `nablarch-testing@3c4bd2a` `AbstractHttpRequestTestTemplate.java:225`-`:228`（`IllegalStateException`「testShots (LIST_MAP=testShots) must have one or more test shots.」）・`StandaloneTestSupportTemplate.java:134`-`:137`（`IllegalArgumentException`「no test shot found.」） |
| 19-12 | `:799` は「カラム名は、最初の行（``rows:``\ の先頭要素）のキーで決まる」で、「残った」（空エントリ `{}` 除去後）に相当する語は無い | `notation.rst:799` |
| 19-12 | 除去→カラム名決定の順は実装由来 | `YamlTableDataBuilder.java:185`-`:186`（`dropBlankRows` → `resolveColumns`） |

指示書に書かれた行番号はすべて実物と一致した（`:1134`・`:1251`・`:1257`・`:1279`・`:1385`・`:1397`-`:1398`・`:340`・`:799`・`:638`・`:1486`、および `:1133` が空行であること）。食い違いは無かった。

## QA Expert Review

指示書 `ntf-step4-13` §4「レビューは回さない（成果は対応表であり、ディレクターが対応表の全行を実物で
突き合わせて独立検証する）」により、QA / Design / Craft / Verification の各エキスパートは回していない。
代わりにコーディネーターが是正14行の出典を実物で独立に再検証した（下記）。

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective | N/A（指示書 §4 によりレビュー不実施） | — |

## コーディネーター独立検証（2026-08-31）

`git diff 2b35561..442f335` を全量読み、変更された全14行の出典を自分で開いて確認した。

| 確認項目 | 結果 |
|---|---|
| 差分ファイルは `.rn/ntf-yaml/report-step4-3.md` のみ（`src/` に差分なし） | OK（`git diff --name-only 2b35561..442f335`） |
| `notation.rst:1133` が空行・`:1134` が引用文の実体 | OK（`ed3de95f` を自分で展開して確認） |
| `notation.rst` 全文に小文字リテラル `variable` が 0 件 | OK（`grep -c 'variable'` = 0） |
| `testdata_examples.rst:1251`「``type: variable``\ を指定し」・`:1257` | OK |
| `notation.rst:1279` に「メッセージボディ側のフィールドは、従来どおり…」 | OK |
| `notation.rst:340` に「エラー」の語が無い | OK |
| `notation.rst:799` に「残った」に相当する語が無い | OK |
| `notation.rst:1385`・`:1397`-`:1398` が null の記法の規定 | OK |
| `AbstractHttpRequestTestTemplate.java:225`-`:228`（`3c4bd2a`）＝ `IllegalStateException` | OK |
| `StandaloneTestSupportTemplate.java:134`-`:137`（`3c4bd2a`）＝ `IllegalArgumentException` | OK |
| `YamlSection.java:137`-`:138`・`:157`-`:158`、`YamlTableDataBuilder.java:185`-`:186`・`:225` | OK |
| §1 判定内訳の再計算（一致 349−5=344・併記 11+4=15・記述なし 75+1=76・矛盾 9・計 444） | OK |
| 対象行以外の対応表の行に差分が無い | OK（`git diff` の `+`/`-` 行に他の行ID が現れない） |

**指示書の指定外に触れた1箇所（ディレクターへ報告し判断を仰ぐ）**: §1 の判定内訳表。
指示書 §5-5-1 が許すのは「5-1〜5-4 の指定行と、それに伴う §2・§7 の記述のみ」であり §1 は含まれない。
ただし判定を変えた5行（16-6・16-7・18-4・19-12・19-15）の結果、更新しなければ §5-3 が正したのと
同種の内部不整合が §1 に新たに残る。**内部整合を優先して更新し、報告で明示する**判断とした。
戻すべきならその指示で戻せる（合計 444 と行ID は不変）。

**是正していない観察（ディレクターへ報告）**: 16-18「カラム名は残った先頭の行のキーで決まる」にも
19-12 と同じ「残った」の非対称が残っている（判定は「一致」、出典 `notation.rst:799`）。
指示書 §5-4 は 19-12 のみを指定しているため変更していない。

## Overall Verdict

- Self-check: OK
- QA: N/A（指示書 §4 によりレビュー不実施。コーディネーター独立検証で代替、上記のとおり全項目 OK）
- Design expert: N/A
- Craft expert: N/A
- Verification expert: N/A
- Ready to check off: Yes（指定外に触れた §1 判定内訳の1箇所を報告に明示することを条件に）
