# task-22 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `:377` の「位置対応させる」が「先頭から対応付ける」に戻っている | OK | `git show 35f70c7:src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` の `:386` を自分で実行して確認（=「フィールド値のリスト。fields の順序で**先頭から対応付けられる**。…」）。修正後 `:377` は「（NTF は fields の順序で先頭から対応付ける）」。ファイル内に「位置対応」は 0 件（grep）。 | OK | 4観点とも OK。coordinator も `git show 35f70c7:...` の `:386`「fields の順序で先頭から対応付けられる」を実物で確認。観点C の「前例が無い」は語尾違いの `git log -S` による取りこぼしで却下 |
| `:108` と `:135`（実際の description 行は `:136`）の description に、`dropBlankRows` の実装が持つ全ケースが漏れなく書かれている | OK | 実装 `YamlSection.java` の `dropBlankRows`（`:182-190`）・`isBlankRow`（`:201-209`）を実読。対応表 — (a) 空マッピング `{}` → `isBlankRow` の for が 1 度も回らず true（`:202-208`）／javadoc `:155-156`。(b) 全ての値が `null` または空文字 → `:203-204` `str != null && !str.isEmpty()` が全値で偽。(c) マッピングでない行（スカラ等） → `:202` `castMap(row)`、`castMap` は非 Map に `Collections.emptyMap()` を返す（`:113-118`）／javadoc `:156-158`。(d) マーカーカラム `[COL]` の値も判定対象 → `:202` が `values()` 全件を回しマーカー除外をしない／javadoc `:196-198`。`[COL]` の書式は `isMarker`（`:239-241`、`startsWith("[") && endsWith("]")`）、「DB 操作対象外」はクラス javadoc `:29`、list_maps でマップから除外されるのは `YamlTableDataBuilder.java:195-197`。(e) `"null"`（クォートあり）は非空 → `objectToString("null")` = `"null"` で `:204` が false を返し行が残る、値は NullInterpreter で Java null（javadoc `:172-174`／テスト `YamlTableDataBuilderTest.java:1395-1399` と list_maps 版 `:1427-1428`）。5 ケースすべてを `:108` と `:136` の両方に記載。 | NG | 4観点すべてが独立に NG。(1) 「マッピングでない行（スカラ等）」はスキーマ自身の `items: {"type":"object"}`（`:109-117` / `:137-145`）と `YamlLoader.java:121-125` の検証により到達不能で、実際は `YamlSchemaValidationException`（`YamlTableDataBuilderTest.java:791` が「スキーマ検証で弾かれるためフィクスチャでは書けない」と明記）。(2) 0 件になった後の帰結が setup_tables のみ（`TableData.java:339-346` により expected 系では「テーブルが空であること」の検証になる。`YamlTestDataParserTest.java:396`）。(3) 「残った行から決まる」は正しくは「残った先頭の行のキー」（`YamlSection.java:227-235`） |
| `:108` で `rows: []`（全件 DELETE のみ）と空行除去の先後関係が書き分けられている | OK | `YamlTableDataBuilder.java:91-99` を実読 — `dropBlankRows`（`:91`）が `resolveColumns`（`:92`）より先に走り、空になっても `buildTableData` が `:99` で呼ばれ 0 行の `TableData` になる（`:95-98` のコメントが「rows が空（rows: []）のテーブルも 0 行の TableData として生成する」と明記）。`:108` に「この除去は列名解決・値加工より前に行われるため、カラム名は除去後に残った行から決まる。全行が取り除かれて 0 件になった rows は、上記の空配列 [] と同じ 0 行として扱われる（setup_tables では全件 DELETE のみ）。」と記載し、除去が先・その結果が `[]` と同じ、という順序を明示。 | NG | 先後関係そのものは OK（`YamlTableDataBuilder.java:91-99`）。ただし帰結が setup_tables に限定されており、expected_tables / expected_complete_tables で 0 行が何を意味するかが書かれていない |
| ファイル系（`record_fragment`）に適用しないことが明示されている | OK | 根拠は `YamlSection.java:33`「ファイルデータ（`YamlFileBuilder`）はいずれの規則も使わない。」と `:161-165` の javadoc。加えて `grep -rn "dropBlankRows" src/main/java/` の呼び出し元が `YamlTableDataBuilder.java:91`（テーブル系）と `:181`（list_maps）の 2 箇所のみで `YamlFileBuilder` に無いことを確認。`:108` に「この規則が適用されるのはテーブル系と list_maps の rows だけで、ファイル系（record_fragment）の rows には適用されない（全フィールドが空文字のレコードは1件のレコードとして保持される）。」、`:136` に「テーブル系（table_data）の rows と同じ規則で、ファイル系（record_fragment）の rows には適用されない」と記載。 | NG | 明示はあるが用語が誤り。`record_fragment` は `file_data` のほか `message_data` / `expected_request_message_data` / `group_message_data` からも `$ref` される（schema `:184` `:210` `:243` `:274`）。「ファイル系」と等値ではない。加えて `:361` が「テーブル系（table_data / list_map_data）」と定義しているのに `:108` は「テーブル系と list_maps」と排他扱いしており、同一ファイル内で用語が矛盾 |
| `:108` の `null` / `"null"` 等価性の一文が行レベルの差に触れる形になっている | OK | 旧文「`null`（クォートなし）および `\"null\"`（クォートあり）はともに NullInterpreter により Java null に変換される。」を「値としては `null`（クォートなし）も `\"null\"`（クォートあり）もともに NullInterpreter により Java null に変換されるが、行として存在するかの判定（後述）では等価でない: 全ての値が裸の `null` だけの行は行ごと取り除かれ、全ての値が `\"null\"` だけの行は行が残って値が Java null になる。」へ変更。裸 `null` 側の根拠は `isBlankRow`（`:203-204`、`objectToString(null)` が null）、`"null"` 側は同 `:204` が非空で false を返すこと＋javadoc `:172-174`。 | NG | 行レベルの差には触れているが前段の機構帰属が誤り。`YamlSection.java:248-250` が `value == null` で早期 return するため、裸 `null` は `NullInterpreter` を通らない。また `NullInterpreter.java:15` は `equalsIgnoreCase` なので `"NULL"` / `"Null"` も対象 |
| `description` 以外のスキーマ要素が変更されていない | OK | 変更後の JSON と `git show HEAD:...` の JSON を Python で再帰比較。差分は `/$defs/table_data/properties/rows/description`、`/$defs/list_map_data/properties/rows/description`、`/$defs/record_fragment/properties/rows/description` の 3 つの VALUE DIFF のみで、KEYS DIFF・TYPE DIFF・LEN DIFF は 0 件。 | OK | coordinator が `1e1e83d` と `ee4a55e` の JSON を再帰比較。差分は 3 つの description の値のみ（KEYS / TYPE / LEN 差分 0）。4観点とも同結論 |
| JSON として妥当（`python3 -c "import json; json.load(...)"` が通る） | OK | `python3 -c "import json; json.load(open('src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json')); print('JSON OK')"` を実行し `JSON OK` を出力（例外なし）。 | OK | coordinator が `json.load` で確認。4観点とも OK |
| `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` を単独実行。`[INFO] Tests run: 207, Failures: 0, Errors: 0, Skipped: 0` および `[INFO] BUILD SUCCESS`。 | OK | 実装エキスパートが単独実行し `Tests run: 207, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS。レビュー役は衝突回避のため未実行 |

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective | NG | 検証の宛先を取り違えている。description の読み手はスキーマ検証を通す YAML を書く人であり、担保すべきは「スキーマ検証を通過しうる入力に対する観測可能な挙動」。実装の分岐と 1 対 1 に対応させた結果、スキーマ自身が `items: {"type":"object"}` で禁止している入力（スカラ行）の挙動を説明してしまっている（`YamlLoader.java:121-125`）。また `$defs.table_data` が 3 セクション共通であることを踏まえた検証になっておらず、expected 系での帰結が抜けた |

## Expert Reviews

### Design Expert

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Approach/structure fits | NG | 規範を `rows` の description に置くこと自体は妥当。ただしマーカーカラムの定義がこのファイルで初出なのに空行判定パラグラフの括弧内に 2 箇所重複して置かれており、責務の置き場所が不適切（マーカーはテーブル系 3 セクション＋`list_maps` に横断する規則。`YamlTableDataBuilder.java:138-143`, `196-198`） |
| System-wide integrity | NG | (1) 同一ファイル内で「テーブル系」の定義が矛盾（`:361` vs `:108` / `:136`）。(2) `record_fragment` を「ファイル系」と等値に扱っているが `$ref` 元は 4 箇所。(3) `:136` が `:108` の写しで、`list_maps` 固有の帰結（`testShots` は 0 件でエラー。`:132` と `AbstractHttpRequestTestTemplate.java:224-229`）と接続していない。(4) 解説書 `testdata_notation.rst:1534` は「すべての値が空文字」までで裸 `null`・マーカー値を含めておらず、スキーマだけ更新した結果 2 箇所で食い違う（nablarch-document 側・⑥ 報告書候補） |

### Craft Expert (writing)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | NG | 同じ事実の二重記述（`"null"` は残る、が第 4 段落と第 6 段落に重複）。前方参照「（後述）」と後方参照「上記の空配列 []」が混在。`:108` が 841→1385 字（+65%）、追加した単一段落 442 字はファイル内のどの description より長い |
| Consistency with existing style | NG | (1) 「列名解決」「値加工」は実装メソッド由来の語で、このファイルは「カラム」で統一（`:18/:25/:60/:67/:100/:162/:229`）。同一文内で「列名解決…カラム名は…」と混在。(2) 「裸の `null`」は同一文で既出の「`null`（クォートなし）」に対する第三の表記でファイル内に用例なし。(3) 見出し【行として存在しないものの除去】は既存の断定文型（`:361`【ファイル系の rows は配列の配列】）と型が違う。(4) 「空配列 []」だけバッククォート無し（`:377` は `[]` 付き） |

### Verification Expert (fact-check)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked | OK | 16 ケースの網羅表を実装出典つきで作成。A0 の裏取りも `DataFileFragment.java:102-115` で取れている（`names` の先頭から添字対応、不足末尾は `""`） |
| Coverage | NG | 16 ケース中 6 件が NG。誤導 1 件（スカラ行）、沈黙 4 件（expected_tables / expected_complete_tables の 0 行の意味、マーカーのみ非空の行の帰結、後続行固有キーの黙殺）、既存文との矛盾 1 件（列名決定行にあって当該行で省略したカラムは NULL が INSERT される。`TableData.java:189-193`）。`fields` より要素数が多い場合の余剰切り捨て（`DataFileFragment.java:105`）も `:377` に無記載 |

## Overall Verdict

- Self-check: OK
- QA: NG
- Design expert: NG
- Craft expert: NG
- Verification expert: NG
- Ready to check off: No — ラウンド1（step A0 + A1）で 4 観点すべて fail。Valid 8 件は修正ラウンドで直す。判断 1（スカラ行の記述）・判断 2（expected 系での 0 行の意味）・判断 3（範囲外の乖離 2 件の送り先）は**ユーザー裁定待ち**。裁定後に修正ラウンドを回し、その後 step A〜E（テスト追加）へ進む
