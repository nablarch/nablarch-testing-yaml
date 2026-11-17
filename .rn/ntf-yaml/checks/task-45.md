# task-45 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence |
|---|---|---|
| `mvn -o clean test` 緑（318 件＋T6/L6） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `BUILD SUCCESS` / `Tests run: 320, Failures: 0, Errors: 0, Skipped: 0` |
| `@Ignore` 0 件 | OK | `grep -rnE '^\s*@Ignore' src/test` → 0 件 |
| `git grep -nE '\.rst\|nablarch-document\|解説書\|出典' -- src/` が 0 件 | OK | 0 件。あわせて `git grep -n '根拠:' -- src/` も 0 件 |
| `git status --short` 空、push 済み | OK | 下記「コミット」のとおり |
| 報告書に §9 が追記されている | OK | `.rn/ntf-yaml/report-step4-2.md:994` から §9（9.0〜9.6） |

## Overall Verdict

- Self-check: OK

## 実施内容（指示書 §8 の 1〜4 ＋ §8.2 の訂正3件）

| 項目 | 結果 |
|---|---|
| O. Rules のピン取り直し（解説書 `afa4f9e` → `a6da1f6`） | `steering.md:69` の1行。他3つのピンは変更なし |
| A. `src/` から解説書への参照を除去 | 122 行・28 ファイル。他リポジトリを指す `path:line` 10 箇所はクラス名だけに。本モジュール自身の `YamlLoader.java:151` は残置 |
| B. 2-5 の規則をスキーマ `description` 5 箇所へ追記 | `:108`・`:136`・`:216`・`:380`・`:433`。文言は `:293` から切り出して統一 |
| C. T5/L5 の Javadoc 書き直しと T6/L6 の追加 | テスト 2 件・oracle シート 2 枚・フィクスチャ 2 エントリを追加。変異確認 1 度実施（`Failures: 2`） |
| D. `checks/task-31.md` への注記 | `:8`・`:9`・`:23`（＋`:7` に短い注記を追加。前提として報告書 §9.4 に記載） |

## 変異確認

T6・L6 の期待値を `{null, null, null}` → `{"", "", ""}` に変えて
`mvn -o clean test -Dtest=YamlBlankEntryOracleTest` を実行 →
`Tests run: 12, Failures: 2, Errors: 0, Skipped: 0`。
落ちたのは `getSetupTableData_markerOnlyRowWithOmittedColumnsMatchesExplicitNull` と
`getListMap_markerOnlyRowWithOmittedKeysMatchesExplicitNull`（いずれも `Expected: is "" but: was null`）。
崩した箇所は元に戻し、再実行で緑を確認した。

## テストの動作・期待値

既存テストの動作・期待値は 1 件も変えていない。変えたのは Javadoc・テストコメント・フィクスチャのコメント、
スキーマの `description`、`steering.md` のピン行、`checks/task-31.md` の注記である。
足したのは T6・L6 の 2 件とその入力だけである。
