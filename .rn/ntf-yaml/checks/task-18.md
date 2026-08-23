# task-18 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `rows.description` に「一致しない場合は NTF がエラーを出す」が残っていない | OK | 出典3件を実物で確認: (1) 実装 `/home/tie303177/work/nablarch/nablarch-testing/src/main/java/nablarch/test/core/file/DataFileFragment.java:107` = `String value = i < line.size() ? line.get(i) : "";`（ループは 106 行目 `for (int i = 0; i < names.size(); i++)`）。(2) 解説書 `/home/tie303177/work/nablarch/nablarch-document/ja/development_tools/testing_framework/implementation/testdata_notation.rst:883` = 「データ行のセル数（Excel形式）または ``rows:`` の各要素の長さ（YAML形式）がフィールド数より少ない場合、不足したフィールドは ``""`` として補完される」。(3) スキーマ `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:377`（変更前）に「各配列の要素数が fields の件数と一致しない場合は NTF がエラーを出す」。3件とも指示書の行番号どおりで、ずれなし。変更後の 377 行は「各配列の要素数が fields の件数より少ない場合、不足したフィールドは `""` として補完される」。`grep -n "エラーを出す" src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` は record_fragment.rows に該当なし | | |
| 「多い場合」に関する記述を新たに追加していない | OK | `git diff 5fb7720` の追加2行に「多い」「無視」「余り」の語はない。追加したのは「より少ない場合」の1文のみ | | |
| `description` 以外（`type` / `items` の構造・`pattern` / `required` 等）を変更していない | OK | `git diff 5fb7720 --stat` = `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json \| 4 ++--`（1 file changed, 2 insertions, 2 deletions）。変更行は `$defs.record_fragment.properties.rows.description`（377行）と同 `items.description`（386行）の2箇所のみ | | |
| JSON として妥当 | OK | `python3 -c "import json; json.load(open('src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json', encoding='utf-8')); print('JSON OK')"` → `JSON OK` | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o clean test` → `[INFO] Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`（ベースライン 187 と一致） | | |
| （Step F）`rows:` の要素数が `fields` より少ない YAML がスキーマ検証で落ちない | OK | 自分専用の複製 `/tmp/claude-1000/-home-tie303177-work-nablarch-nablarch-testing-yaml/b54f3aac-63f7-4080-b2eb-b320cfb720a7/scratchpad/impl18/shortRows.yaml` に fields 3件・rows `["AAA","BBB","CCC"]` / `["AAA"]` / `[]` を記述し、`YamlLoader.load(base, "shortRows")` を実行 → 例外なし（標準出力 `SCHEMA_VALIDATION: PASSED (no exception)`）。実行は `java -cp target/classes:target/test-classes:<dependency:build-classpath の出力> Verify <dir>` | | |
| （Method）「少ない場合は `""` 補完」が実挙動と一致する | OK | 同 Verify プログラムで `YamlFileBuilder#buildDataFileList` → `DataFile#toDataRecords()` を実行した結果: `RECORD: {FIELD2=BBB, FIELD3=CCC, FIELD1=AAA}` / `RECORD: {FIELD2=, FIELD3=, FIELD1=AAA}` / `RECORD: {FIELD2=, FIELD3=, FIELD1=}`。不足フィールドが `""` になることを実測 | | |
| （Method）YAML 経路が補完コードに到達する | OK | `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java:240` = `fragment.addValue(rowValues);`（223行目 `for (Object rowObj : getList(record, FIELD_ROWS))` の中）。`fragment` は `DataFile#getNewFragment()` が返す `DataFileFragment` で、`addValue` は `DataFileFragment.java:102` 定義、107 行目が上記の `""` 補完 | | |

## Overall Verdict

- Self-check: OK
