# task-25 Completion Check

`#14` step A の根拠取り直し（2026-08-24）で判明した、PR #1 レビュー中に発生したが番号未採番だった実装差分5件を追認する。5件とも `checks/` にも steering.md の見出しにも記録が無かった（`git log --oneline 0df7407..HEAD` の目視確認だけでは見つからない）。

## 対象コミット

| コミット | 日時 | ファイル | 内容 |
|---|---|---|---|
| `f375fde` | 2026-06-24 15:22:57 +0900 | `ntf-testdata-yaml-schema.json` | `field_def.length` にダミーフィールド用の `"0"` を許容（`minimum: 1→0`、pattern `^([1-9][0-9]*\|-)$` → `^([0-9]+\|-)$`）。NTF実装が `parseInt("0")=0` として動作することに合わせた制約緩和 |
| `630e700` | 2026-06-24 17:54:44 +0900 | `YamlTestDataParser.java` | `setTestDataReader` を、DI で同名コンポーネント上書き時に発生する `UnsupportedOperationException` throw から INFO ログ出力＋無視に変更 |
| `10feb3e` | 2026-06-24 18:20:53 +0900 | `YamlLoader.java`／`YamlSchemaValidationException.java` | 依存 `json-schema-validator` を 3.0.5→1.5.9 に降格。tools.jackson:3.x と com.fasterxml.jackson:2.x の混在を解消し Nablarch 他モジュールと同じ Jackson 2.x 系に統一 |
| `6ea4655` | 2026-06-25 14:33:56 +0900 | `ntf-testdata-yaml-schema.json` | 全セクションの description を「構造の説明」から「NTF が実行時に何をするか」へ書き換え。rows:[] の全件DELETE、null/"null"変換、group_id有無での収集差、testShots空エラー等の実行時挙動を追記 |
| `b309359` | 2026-07-15 08:43:53 +0900 | `ntf-testdata-yaml-schema.json` | `expected_tables`/`expected_complete_tables` の description に、DB行との対応付けが主キーで行われること・DB全行列挙が必須なこと（部分検証不可）・自動採番主キーの制約を追記 |

## 実物確認（本タスクでの検証）

- `f375fde`: `git show f375fde -- src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` で diff を確認。`length` の `anyOf` が `minimum: 0` / パターン `^([0-9]+|-)$` になっており、現在の `ntf-testdata-yaml-schema.json` にもこの制約がそのまま残っている（後続タスクで巻き戻されていない）
- `630e700`: `git show 630e700` で diff を確認。`setTestDataReader` が例外を投げず INFO ログのみになっている変更が、現在の `YamlTestDataParser.java` にそのまま残っている
- `10feb3e`: `git show 10feb3e` で pom.xml 差分を確認。`json-schema-validator` のバージョンが 1.5.9 のまま現在に至る（後続コミットで変更されていない。`git log --oneline -- pom.xml` に該当バージョン変更なし）
- `6ea4655`／`b309359`: `git show` で diff を確認。両方とも description 文字列のみの変更で `type`/`enum`/`required` 等の検証ルール構造に変更なし（後続タスク #7〜#24 の各 checks/ が同ファイルの description をさらに上書きしているが、両コミットの追記内容自体と矛盾する記録はない）

## Completion Criteria

| Criterion | Self-check | Evidence |
|---|---|---|
| 5コミットの内容が commit message の主張どおりであることを diff で確認した | OK | 上記「実物確認」参照 |
| 5コミットの変更が現在のコードに反映されたまま残っている（後続タスクで無断で巻き戻されていない） | OK | 上記「実物確認」参照。`6ea4655`／`b309359` の記述箇所は後続タスクがさらに上書きしているが、追記した論点（実行時挙動・主キー対応）自体は現行の description にも含まれている（`ntf-testdata-yaml-schema.json` を目視確認） |
| `mvn test` 全体が PASS の状態に含まれている | OK | `#14` step A（2026-08-24 再実行）で `Tests run: 226, Failures: 0, Errors: 0, Skipped: 0` を確認済み。5件はこの回帰テスト対象に含まれる |
| 実装の意図（なぜこの変更が必要だったか）が commit message に明記されている | OK | 5件とも commit message 本文に理由が記載されている（上記表参照） |

## 判定

- Self-check: OK
- 個別の Craft/QA/Language expert レビューは実施していない（既にマージ・テスト済みの過去差分5件の追認であり新規実装ではないと判断し、2026-08-24 ユーザーへ提案のうえ簡易追認での即時記録を承認された）
- 目的: `#14` Acceptance criteria「#4以降の実装差分はすべて承認済みタスクに帰属」を実物と一致させること
