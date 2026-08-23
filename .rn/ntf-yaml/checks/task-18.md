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

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective (checks the right thing, not just "passed") | NG | 完了条件5件はすべて OK。ただし総評 NG。理由: 同じ `$defs.record_fragment` の**親 description**（`ntf-testdata-yaml-schema.json:361`）に「rows の各配列は fields と完全に同じ順序・**同じ件数**で値を並べること」が残り、直した子（`:377`「少ない場合は `""` 補完」）と自己矛盾する。完了条件が `rows.description` にスコープを切っているため文言上は合格だが、タスクの目的（実装と食い違う description を直す）は未達。あわせて「description はスキーマ検証に一切効かないため、この変更の正しさを固定する回帰テストは本リポジトリに存在しない」（`YamlFileBuilderTest.java` の `rows` 使用箇所 `:269`/`:311`/`:740`/`:914-921` はいずれも過不足なしのケースのみ）と指摘 |

## Expert Reviews (axes the task needs)

### Craft Expert (writing)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | NG | (NG-1) 親 `:361` と子 `:377` の矛盾を本コミットが作り込んだ。修正前は親子とも「同順・同件数」で一致していた。しかも `:361` は「〜すること」（規範）、`:377` は「〜される」（挙動）で、規則なのかサポートされた書き方なのか判別できない。(NG-4) `:377`「不足したフィールドは `""` として補完される」は**値**が不足しているのにフィールド定義の不足とも読める。さらに「補完」は `:108`（`table_data.rows`）で「カラム型ごとのデフォルト値を入れる」意味で使われており、同一スキーマ内で二義。代替文「値を指定しなかったフィールドには `""` が設定される」 |
| Consistency with existing style | NG | (NG-2/NG-3) `:386` は書き換えの結果、`:377` 第2文と同内容になり新情報がゼロ。加えて受動・動作主省略で、隣接する「NTF は〜対応付ける」（能動・主語明示）と声が反転。代替は言い換えではなく第1文の削除。／OK 判定: バッククォート用法（`:108` と一致）・「〜すること」の使い分け・全角半角・「NTF は」の先例（`:136`）はいずれも既存文体と整合 |

### Verification Expert (fact-check)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked (claims verified) | OK | 主張A〜F をすべて一次情報で照合し真と判定。A/B/E: `YamlFileBuilder.java:196-209` + `DataFileFragment.java:105-113`（インデックス対応、名前突合コードなし）。C: `DataFileFragment.java:107`。`addValue` は抽象基底のみ定義で `FixedLengthFileFragment`/`VariableLengthFileFragment` ともオーバーライドせず、**固定長・可変長の双方で成立**。固定長の書き出しも `数値`→`Z`（`BasicDataTypeMapping.java:47`）で `DecimalHelper` が `""` を ZERO 扱い、`バイナリ` は `FixedLengthFileFragment.java:127-129` で 0 埋めとなりエラーにならないことを確認。D: `rows: []` → `DataFileFragment.java:574-588` が 1 レコードも書かず `DataFile.java:108-121` が 0 バイトファイルを作る。F: `schema:377-385` の `items.type` が `["string","null"]`、解説書 `rst:1431` |
| Coverage (claims) | OK | 削除した旧記述「一致しない場合は NTF がエラーを出す」は**条件付きでも真ではない**ことを確認（少ない側は補完、多い側は超過要素を無言で破棄。件数を検証するコードは YAML 経路 `YamlFileBuilder.java:224-243`・Excel 経路 `DataFileParser.java:193-200` のいずれにも無く、スキーマにも `minItems`/`maxItems` が無い）。件数不一致でエラーになるのは `names` と `types`/`lengths` の不一致（`DataFileFragment.java:339-346`, `543-547`）で `rows` とは別物。よって削除で失われた真の条件は無い。YAML 経路と Excel 経路は同じ `addValue`/`addValueWithId` に合流し挙動同一。未確認として「`rows` の要素数が fields より少ない/多い場合」「`rows: []` が 0 バイトファイルを生む」を直接固定する既存テストは見つからなかった旨を報告 |

## Triage（coordinator）

| Finding | 出所 | 判定 | 理由 |
|---|---|---|---|
| 親 `:361` の「同じ件数」が子 `:377` と矛盾（未修正） | QA・Craft・Verification の3本すべて | **Escalation → ユーザー判断待ち（#18 step L）** | #18 の完了条件は `rows.description` にスコープを切っており、この差分としては合格。一方でタスクの Purpose（実装と食い違う description を直す）は未達。スコープを広げる判断はユーザーのもの |
| `:386`（`items.description`）が `:377` と重複し新情報ゼロ。第1文を削除すべき | Craft NG-2/NG-3 | **Escalation（上と同じ修正ラウンドで扱う）** | 事実誤りでも完了条件違反でもなく、文章上の趣味の領域。`:361` を直す判断が出れば同じ編集で片付くため、単独では動かさない |
| `:377`「不足したフィールドは」が曖昧、「補完」が `:108` と二義 | Craft NG-4 | **Valid → 修正する** | `rows.description` はタスクのスコープ内。事実は変えず明確さのみ改善し、「多い場合」の記述も増やさない |
| 「多い場合は無言で切り捨てられる」を書くべき | QA 指摘2 | **Invalid（スコープ境界）** | 完了条件が「『多い場合』に関する記述を新たに追加していない」と明示し、steering #18 step D も「多い側には触れない（エラー化とも『余りは無視される』とも書かない）」と定めている。事実としては報告書へ回す候補 |
| `""` 補完値は `interpret()` を通らない | QA 指摘3 | **Invalid（記述の誤りではない）** | `YamlFileBuilder.java:233-235` の事実だが、変更後の description はインタープリタ適用について何も主張していない |
| 固定長で数値型が `""` 補完されたときの出力が未確認 | QA 指摘3 | **解決済み** | Verification が `DecimalHelper`（`nablarch-core-dataformat` の class 逆アセンブル）と `FixedLengthFileFragment.java:127-129` で確認し、エラーにならないと判定 |
| 「0件も有効（setup_files では…／expected_files では…）」が `message_data` 等 3 つの def からの `$ref` を説明できていない／解説書 `rst:1146` は空ファイルの正規記法を `records: []` としている | Craft 参考・Verification 主張D注記 | **スコープ外（既存記述・本差分の変更対象外）** | 変更前から同文言。報告書または後続タスクの候補として記録 |

## Overall Verdict

- Self-check: OK
- QA: NG（完了条件は全 OK。総評 NG の理由は親 `:361` の未修正矛盾）
- Design expert: N/A（構造・方式を変えないため）
- Craft expert: NG
- Verification expert: OK
- Ready to check off: No — 親 `:361` を #18 の範囲で直すかのユーザー判断（step L）と、Craft NG-4 の修正が未了
