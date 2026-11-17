# task-13 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `record-separator.description` に実制御文字によるリテラル指定（`"\r\n"` 等）の推奨が残っていない | OK | 書き換え後の全文（`ntf-testdata-yaml-schema.json:290`、JSON デコード後）は「[共通] レコード区切り文字。改行コードは `NONE` / `CR` / `LF` / `CRLF` のシンボルで指定する（例: `record-separator: CRLF`）。シンボル以外の文字列を書いた場合は、その文字列自身が区切り文字になる。YAML のダブルクォート文字列に `"\r\n"` と書くと実際の制御文字に展開され、NTF が値を trim する際に除去されて区切りが空文字になる（エラーにならないため注意）」。`"\r\n"` は「書くと壊れる」という警告としてのみ登場し、推奨語は「シンボルで指定する」 | OK | QA レビュアーが JSON をパースして `repr()` で全文を確認し、推奨語が「シンボルで指定すること」であることを判定 |
| `field-separator.description` に実制御文字のタブによる指定の推奨が残っていない | OK | 書き換え後の全文（同 :330）は「[可変長専用] フィールド区切り文字。省略時はカンマ（`","`）。2文字表記の `\t` を除き、1文字でない値はエラー。タブは `field-separator: "\\t"` と記述する。YAML の `"\t"` は実際のタブ文字に展開され、NTF が値を trim する際に除去されて0文字になりエラーとなるため、バックスラッシュをエスケープする」。実タブは「エラーとなる」とのみ記載 | OK | 同上 |
| 両 description が示す記法が、実行で通ることをテストで担保されている | OK | `YamlFileBuilderTest` に 4 件追加（`buildFileList_recordSeparatorSymbolsAreConvertedToLineSeparators` :558、`..._recordSeparatorLiteralStringIsUsedAsIs`、`..._recordSeparatorControlCharBecomesEmpty`、`..._controlCharFieldSeparatorThrowsException`）。いずれも `YamlLoader.load` → `YamlFileBuilder#buildDataFileList` → `createLayout()` という実経路を YAML fixture 経由で通す。`\t` 2 文字表記は既存 `buildFileList_tabFieldSeparatorBecomesTabChar`（:512）、2 文字値のエラーは既存 `buildFileList_twoCharFieldSeparatorThrowsException`（:433）が担保 | OK | QA レビュアーが変異実験 M2（fixture の `CRLF` を実制御文字へ）・M3（`"\\t"` を実タブへ）・M4（実装 `applyDirectives` に制御文字をシンボルへ差し替える処理を注入）で計 4 件が落ちることを確認。**M1（description を旧文言へ戻す）では 1 件も落ちない**ことも確認しており、この事実はテスト側のコメントに明記した（下記「担保範囲の明示」） |
| description 以外（`type` / `enum` / `required` 等の検証ルール構造）が変更されていない | OK | 変更前（`git show HEAD:`）と現行の両方から `description` キーを再帰的に除去した構造体を Python で比較 → `structure identical: True`。差分のある description は `$defs.directives.properties.record-separator` と `.../field-separator` の 2 ノードのみ | OK | QA レビュアーが同じ方法で独立に確認（全ノード走査で差分 2 ノードのみ） |
| JSON として妥当（`json.load` が通る） | OK | `python3 -c "import json; json.load(open('src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json'))"` → 正常終了 | OK | QA レビュアーも独立に実行し `JSON valid` |
| `mvn clean test` が BUILD SUCCESS（Skipped は #11 の 4 件のみ、Failures/Errors 0） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test` → `Tests run: 177, Failures: 0, Errors: 0, Skipped: 4` / BUILD SUCCESS | OK | QA レビュアーも独立に実行し EXIT=0。Skipped 内訳は `YamlTestDataParserTest` 1 件 + `YamlTableDataBuilderTest` 3 件で、いずれも #11 由来の既存分 |
| `nablarch-testing` 本体（`DataFile#setDirective`）が変更されていない | OK | `git -C /home/tie303177/work/nablarch/nablarch-testing status --short` が空。HEAD は `fdf55d4` のまま | OK | QA レビュアーが同リポジトリの `git diff --stat -- src/main/java/nablarch/test/core/file/DataFile.java` が空であることも確認 |

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective (checks the right thing, not just "passed") | pass | criteria 7 項目すべて OK。変異 4 種（M1〜M4）を投入し、M2/M3/M4 で計 4 件が落ちること、M1 では落ちないことを確認。変異対象 4 ファイルを `md5sum -c` で復元確認、一時ファイルも削除済み。指摘 1 件（テスト Javadoc が「description の文言が戻ることを防ぐ」と過大主張していた。M1 が示すとおり description を旧文言へ戻しても 1 件も落ちない）は修正ラウンドで対応済み |

## Expert Reviews (axes the task needs)

### Craft Expert (writing)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | pass | 指摘 3 件（should-fix）をすべて修正: (1) `field-separator` の文が「〜場合は」「〜すると」の二重条件で主節が解決しないまま終わっていた、(2)「2文字表記」と言いつつ示す例が `"\\t"`（バックスラッシュ 2 つ）で、なぜ 2 つ書くのか（YAML のダブルクォート内でバックスラッシュをエスケープするため）が書かれておらず自己矛盾に見えた。かつ読み手が実際に犯すミスである `"\t"` が説明文に一度も出てこなかった、(3)「1文字のみ有効（2文字以上はエラー）」が 0 文字ケースを説明できず、直後の文の根拠が読み手から見えなかった。低優先の 4 件（例の係り先・結論の位置・trim の主語・「エラーにならない」の明示）も `record-separator` 側で対応 |
| Consistency with existing style | pass | `[共通]` / `[可変長専用]` の接頭辞、末尾に句点を打たない規約、バックティックでのコード表記、「〜すること」の指示形（同ファイル :136 / :386 に既存用例）はいずれも同ファイル内の他 62 個と揃っている。シンボルを `"CRLF"` から `CRLF` へクォートなしに改めた点は解説書 `testdata_notation.rst:946` の表記に近づく改善 |

### Verification Expert (fact-check)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked (tests run / claims verified / flow traced) | pass | 12 の主張すべてを、テスト classpath 上の使い捨て Java プログラム（実行後削除）と一次情報の現物確認で検証。**事実と違う主張はゼロ**。実測値: `record-separator` に実 CR+LF → 例外なし・格納値 `len=0`、`field-separator` に実タブ → `IllegalArgumentException`（メッセージ全文 `field-separator must be one character.but was `、末尾が空）、`\t`（2 文字）→ U+0009（1 文字）、`NONE`→`""` / `CR`→U+000D / `LF`→U+000A / `CRLF`→U+000D U+000A、`:` → `:`、4 文字リテラル `\r\n` → そのまま 4 文字 |
| Coverage (edge cases / claims / steps) | pass（ラウンド1で穴を塞いだ） | 指摘 1 件: `buildFileList_recordSeparatorSymbolCrlfBecomesCrLf` の Javadoc が 4 シンボルの担保を主張しながら `CRLF` しか assert していなかった。修正ラウンドで `buildFileList_recordSeparatorSymbolsAreConvertedToLineSeparators` へ改め、fixture を 4 グループに分けて 4 種すべてを assert（実測値をプローブで確認。プローブは削除済み、`grep` で残存なしを確認）。ほか 2 点は「事実誤りではないが文言が広い」との注記: `String.trim()` が除去するのは U+0000〜U+0020 のみで U+007F・C1 制御文字は残る／解説書にエスケープシーケンス記法が「ない」のはディレクティブの文脈に限る（データ値の文脈には `LineSeparatorInterpreter` 用の記述が `testdata_notation.rst:1421` / `:1473` にある。別機能） |

## Overall Verdict

- Self-check: OK
- QA: pass
- Craft expert: pass
- Verification expert: pass
- Ready to check off: Yes

---

## TDD の適用（RED → GREEN）

現 description が推奨する記法をそのまま assert する暫定テスト 2 本を先に書き、description を直す前に実行した。

コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test -Dtest=YamlFileBuilderTest#redControlCharRecordSeparatorBecomesCrLf+redControlCharFieldSeparatorBecomesTabChar`

結果: `Tests run: 2, Failures: 1, Errors: 1` / BUILD FAILURE

- `record-separator: "\r\n"`（YAML が実 CR+LF へ展開）→ `Expected: is "\r\n" but: was ""`。**例外は出ず無言で壊れる**
- `field-separator: "\t"`（YAML が実タブへ展開）→ `java.lang.IllegalArgumentException: field-separator must be one character.but was `（`but was ` の後ろが空＝`trim()` で消えている）

その後 description を書き換え、暫定テストを実挙動を固定する形へ整えて GREEN。

## 不具合の機序（一次情報）

- `/home/tie303177/work/nablarch/nablarch-testing/src/main/java/nablarch/test/core/file/DataFile.java:304`
  `Object value = convertDirectiveValue(directive, stringValue.trim());`
  → 値は変換前に `trim()` される。制御文字だけの値は空文字になる
- 同 `VariableLengthFile.java:17`（`TAB_EXPRESSION = "\\t"`）・`:29`（既定値 `","`）・`:68-79`（`convertDirectiveValue`）
  → `\t`（バックスラッシュ + t の 2 文字）と等しいときだけタブへ変換。それ以外で長さが 1 でなければ `IllegalArgumentException`
- 同 `LineSeparator.java:11-17`（enum 定義）・`:57-65`（`evaluate`）
  → `NONE` / `CR` / `LF` / `CRLF` は実際の改行コードへ、それ以外は与えられた文字列自身をそのまま返す

## 解説書との整合（一次情報）

`/home/tie303177/work/nablarch/nablarch-document` ブランチ `ntf-yaml-support`（HEAD `7f5659e`）

- `ja/development_tools/testing_framework/implementation/testdata_notation.rst:946`
  「レコード区切り。``NONE``\ / ``CR``\ / ``LF``\ / ``CRLF``\ または任意のリテラル文字列が有効」
- 同 `:948`「フィールド区切り文字。デフォルトは ``","``\ 。1文字のみ有効であり、2文字以上はエラーになる」
- 同 `:1078`（tip、Excel 形式の節）「例えば区切り文字をタブにしたい場合は ``field-separator=\t``\ と指定する」— バックスラッシュと t の 2 文字表記
- 同 `:1114`（記述例）`record-separator CRLF`
- `ja/development_tools/testing_framework/implementation/testdata_examples.rst:1435`（**YAML形式の場合** の節）
  「タブ文字は ``"\\t"``\ と記述する。\ YAML\ の ``"\t"``\ は実際のタブ文字になってしまうため、バックスラッシュをエスケープする」

`field-separator.description` は最後の 1 件（YAML 固有の説明）と同じ機序で説明する形へ揃えた。

## 担保範囲の明示（QA 指摘への対応）

QA レビュアーの変異実験 M1 が示すとおり、`schema.json` の description を旧（誤った）文言へ戻しても**テストは 1 件も落ちない**。テストが読むのは fixture と実行時の挙動だけで、description 文字列を参照する経路は存在しない（スキーマ検証に description は使われない。`grep -rn "ntf-testdata-yaml-schema" src/` の唯一の実コード参照は `YamlLoader.java:45` の検証パス）。

当初のテストコメントは「description がその記法を推奨する状態へ戻ることを防ぐ」と書いており、これは事実と違った。`YamlFileBuilderTest.java:527-543` のブロックコメントを、担保が

- description が示す記法（シンボル指定・`\t` の 2 文字表記）が実行で通り、期待どおりの値になること
- description が示していない記法（YAML のエスケープシーケンスによる実制御文字の指定）が実行で通らないこと

の 2 点であること、および「description の文字列自体を参照しないため文言が変わってもテストは落ちない。文言と挙動の整合を保つのは文言を書く側の責務で、ここで固定するのは文言が拠り所とする実挙動のみである」と明示する形へ書き換えた。

description 文字列そのものを assert するテストは**追加しない**判断とした。QA レビュアーも「費用対効果が低い」と同意見。

## スコープ外として送らなかった指摘（ユーザー判断待ち）

解説書側で、YAML 利用者がハマる罠のうち**改行コード側だけ無防備**である。

- タブ: `testdata_examples.rst:1435` が YAML 固有の注意（`"\t"` は実タブになる／バックスラッシュをエスケープする）を書いている
- 改行コード: 該当する注意が解説書に**一切ない**。`testdata_notation.rst:946` は有効値の列挙のみ、`:1114` と `testdata_examples.rst:1444` の記述例はどちらも `record-separator: CRLF`

今回はスキーマ側の description で警告する形とした。解説書 `testdata_examples.rst` の YAML 節にタブと同じ調子で 1 文足す価値はあるが、別リポジトリであり本タスクのスコープ外のため触っていない。
