# task-08 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `setup_tables` の description に「INSERT 前の全件 DELETE」が記載されている | OK | "rows の有無にかかわらず、NTF はまず対象テーブルを全件 DELETE してから INSERT を行う（rows が空の場合も DELETE は実行される）" | OK | 字義どおり満たしている（QA報告より） |
| `setup_tables` の description に「FK 親テーブル clear 時に子テーブルも列挙すること（NTF は子→親の順で削除する）」が記載されている | OK | "FK の親テーブルを clear（全件 DELETE）する場合は、参照元の子テーブルも setup_tables に列挙すること（NTF は子テーブル → 親テーブルの順で DELETE するため…）" | OK | 字義どおり満たしている（QA報告より） |
| `table_data.rows` の description に「FK 制約のある数値カラムを省略すると `"0"` が INSERT され FK 違反になる可能性」が記載されている | OK | "FK 制約のある数値カラム（外部キー）を省略すると `\"0\"` が INSERT され、参照先テーブルに ID=0 の行が存在しない場合は FK 制約違反になる" | OK | 字義どおり満たしている（QA報告より） |
| `table_data.rows` の description に「NULL 許容カラムを NULL にしたければ省略せず `null` を明示すること」が記載されている | OK | "NULL 許容カラムを NULL にしたい場合は省略せず `null`（クォートなし）を明示すること（省略は NULL ではなくデフォルト値の補完を意味する）" | OK | 字義どおり満たしている（QA報告より） |
| description 以外（`type` / `enum` / `required` 等の検証ルール構造）は変更されていない | OK | git diff で description のみ変更、他フィールド無変更を確認 | OK | 構造フィールド無変更確認済み（QA報告より） |
| JSON として妥当（`json.load` が通る） | OK | `python3 -c "import json; json.load(open(...))"` 通過 | OK | 確認済み（QA報告より） |
| `mvn clean test` 全 PASS | OK | Tests run: 159, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS | OK | 確認済み（QA報告より） |

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective | OK | 全 completion criteria を証拠付きで確認。JSON valid・mvn test 159件 PASS を実際に実行して確認している |

## Expert Reviews (axes the task needs)

### Craft Expert (writing)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | OK | 文の論理的正確性・prose clarity OK。2件の WARN（冗長な括弧、タイトルと末尾NULL注意の微細なスコープ差）は致命的ではなくリリース可能と判定 |
| Consistency with existing style | OK | 「NTF が〜する」体言止め・【…】表記すべて既存スタイルに準拠 |

### Verification Expert (fact-check)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked | OK | Claim A/B: DbAccessTestSupport.java 192–200行で DELETE→INSERT 順・子→親 DELETE 順を確認。Claim C/D: YamlTableDataBuilder.java・BasicDefaultValues.java・TableData.java で数値デフォルト "0"・省略≠NULL を確認 |
| Coverage | OK | 4つの事実クレームをすべてソースコードで裏付け。未検証の断言なし |

## Overall Verdict

- Self-check: OK
- QA: OK
- Design expert: N/A
- Craft expert: OK
- Verification expert: OK
- Ready to check off: Yes
