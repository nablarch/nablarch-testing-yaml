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

---

# ラウンド2（2026-08-24）— 裁定反映後の修正ラウンドと step A〜E

## step A0 + A1（スキーマ description）の再レビュー — 4観点

実装: `954f20b`（`:108` / `:136`）＋ 本ラウンドの観点C 反映（`:11` / `:108` / `:136`）。

| 観点 | 判定 | 内容 |
|---|---|---|
| A（充足） | NG 1件 | Finding は **`#24` の (a) のみ**。新設文そのものへの指摘は無し |
| B（整合） | NG 1件 | **新設19文はすべて実装と一致**（照合表）。NG 理由は A と同じ `#24` の (a) |
| C（規約・文章） | NG 4件 | すべて文章面。本ラウンドで3件反映・1件はユーザー判断へ（下記） |
| D（検証の妥当性） | **OK** | 網羅表 23＋11 ケース。「誤って言っている」と判定できたのは既存文（`#24` 対象）だけ |

**A・B・D が挙げた唯一の実質 NG は `#24` の (a)** であり、ユーザー裁定により `#22` では直さない。

### 観点C の反映

| # | 指摘 | 対応 |
|---|---|---|
| C-1 | `:108` の「後者の」が【】ブロックをまたぐ指示語。対の `:136` は明示列挙しており不揃い | 「後者の」を削除 |
| C-2 | `:136` の1段落417字がファイル内最長。対の `:108` は同内容を2段落に割っている | `:136` を2段落に分割（`:108` と同じ段落境界） |
| C-4 | `:108` L6 に同一 referent への指示語が2通り。スコープ注記が規則本体から離れている | スコープ注記を除去規則の段落（L5）末尾へ移動。重複した指示語を解消 |
| C-6 | `:11` と `:108` の重複箇所で更新が `:108` 側だけに入り、`:11` を読んだ人は 0 行の同値性を知れない | `:11` を「rows が 0 行の場合（`rows: []` の指定、および空行の除去で 0 行になった場合）」へ拡張 |
| C-3 | `record_fragment` の呼称が `:108`（record_fragment）と `:361`（ファイル系）で割れた | **`:361` は今回の変更対象外。ユーザー判断へ回す**（下記「ユーザー判断が要る1件」） |
| C-5 | 28字の重複削減（任意） | 見送り（対比を具体化する働きがあるため） |

**文字数の推移**（python3 で JSON をパースして実測）:

| | `1e1e83d` | `ee4a55e`（ラウンド1） | 本ラウンド |
|---|---|---|---|
| `$defs.table_data...rows.description` | 841字 | 1385字 | **1428字** |
| `$defs.list_map_data...rows.description` | 85字 | 465字 | **505字** |

増分の内訳は事実の追加（判断2 の expected 系 0 行・V4 の後続行キー無視・V2/V9/V10 の null 書き分け）で、観点C の実測でも「削る余地は増分592字の約5%」。判断1 で1項目（スカラ行）が落ちたぶんは織り込み済み。

**差分の範囲**: `git show HEAD` と現在の JSON を python で再帰比較し、VALUE 差分3件（`/properties/setup_tables/description`、`/$defs/table_data/properties/rows/description`、`/$defs/list_map_data/properties/rows/description`）のみ。KEYS / TYPE / LEN 差分 0。

## step A〜E（テスト追加）のレビュー — 3観点

| 観点 | 判定 | 内容 |
|---|---|---|
| QA | NG 3件 | 変異が担保対象を殺していない／javadoc が未実行の変異を断定／確認コマンド未記録 |
| Craft（coding） | NG 2件＋軽微1件 | javadoc に `description が述べる挙動:` ブロックが無い／既存テストとの相互参照が無い／初回 assert のメッセージ欠落 |
| Verification（test） | **OK**（Finding 2件） | 期待値 `""` の出典チェーンは一次情報で裏取り済み。指摘は変異の網羅 |

### 期待値 `""` の出典チェーン（Verification 観点が実物で確認）

依存先 `/home/tie303177/work/nablarch/nablarch-testing` の HEAD は `2e43786`、`git status --porcelain` は空（clean）。

1. `YamlFileBuilder.java:224` — `getList(record, FIELD_ROWS)` を回す（`rows: []` は0回）
2. `YamlFileBuilder.java:232-235` — `rowValues` は**行の要素数ぶんだけ**。ここでは補完しない
3. `YamlFileBuilder.java:240` — `fragment.addValue(rowValues)`
4. **`DataFileFragment.java:107`** — `String value = i < line.size() ? line.get(i) : "";` ← `""` の唯一の生成点。ループは `names.size()` で回る（`:106`）
5. `DataFileFragment.java:108` → `isOndemandCalcFieldSize(i)` は当フィクスチャでは常に false（`length` 未指定かつ `messaging=false` で `setLengths` が呼ばれず、リストが null のまま）。よって加工されない
6. `DataFile.java:155-160` → `DataFileFragment.java:386-392` → `:401-405` `convertForDataRecord`
7. `VariableLengthFileFragment.java:31-38` → `MapCollector.java:43-53`（`skip()` を呼ばないので全キー保存）
8. **`VariableLengthFileFragment.java:43-45`** — `return stringExpression;`（**恒等**）

**固定長を選ばなかった理由が事実であることも確認済み**: `FixedLengthFileFragment.java:51-58`（`:55` で `removePadding`）→ `DataFileFragment.java:470-484` → `dataType.removePadding(value)`。`~/.m2/repository/com/nablarch/framework/nablarch-core-dataformat/6-NEXT-SNAPSHOT/` には sources jar が無く（`.jar` / `.pom` / metadata のみ）、`~/work/nablarch/` 配下にもチェックアウトが無い。旧版 1.3.5 / 2.0.3 の sources は現行版の根拠にしない。

### 変異確認（step D）— 実行コマンドと結果

すべて `/home/tie303177/work/nablarch/nablarch-testing-yaml` で単独実行。共通コマンド:

```bash
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlFileBuilderTest
```

各変異は `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java` に1つだけ入れ、確認後に元へ戻した（`git diff --stat src/main/java/` が空であることを確認）。

| 変異 | 挿入内容と位置 | 結果 |
|---|---|---|
| MUTATION-1 | `:236`（`if (withId) {`）の直前に `rowValues.add(0, "");` | `Tests run: 29, Failures: 6, Errors: 1`。`buildFileList_shortRowFillsTrailingFieldsWithEmptyString:966 先頭から対応付けられること` が FAILURE |
| MUTATION-2 | 同位置に `if (rowValues.isEmpty()) { continue; }` | `Tests run: 29, Failures: 1, Errors: 0`。`buildFileList_emptyRowBecomesOneRecordOfEmptyStrings:993 空配列 1 件がレコード 1 件になること` が FAILURE |
| MUTATION-3 | 値行ループ直後に `if (rowNo == 1) { fragment.addValue(new ArrayList<String>()); }` | `Tests run: 29, Failures: 1, Errors: 0`。`buildFileList_noRowsBecomesZeroDataRecords:1023 データ行が 0 件であること` が FAILURE |
| **MUTATION-4** | 同 `:236` の直前に `while (rowValues.size() < names.size()) { rowValues.add(null); }` | `Tests run: 29, Failures: 2, Errors: 0`。`buildFileList_emptyRowBecomesOneRecordOfEmptyStrings:996 FIELD1 が "" になること` と `buildFileList_shortRowFillsTrailingFieldsWithEmptyString:967 不足した 2 番目のフィールドが "" になること` が FAILURE |
| **MUTATION-5** | `:183`（`Map<String, Object> record = castMap(recordObj);`）の直後に `if (getList(record, FIELD_ROWS).isEmpty()) { continue; }` | `Tests run: 29, Failures: 1, Errors: 1`。`buildFileList_noRowsBecomesZeroDataRecords:1021 レコード定義は生成されること` が FAILURE |

**MUTATION-4 と MUTATION-5 は QA 観点と Verification 観点が独立に指摘した追加分**。MUTATION-1〜3 だけでは、テストの中心である `""` 埋め（`DataFileFragment.java:107` の三項演算子の偽側）を殺す変異が1つも無く、`""` の assert は JUnit が先行 assert で停止するため評価すらされていなかった。MUTATION-4 でこれが load-bearing になり、MUTATION-5 で `noRows` の「レコード定義は生成されること」も裏付いた。

### レビュー指摘の反映

| # | 指摘 | 対応 |
|---|---|---|
| QA-F1 / Verify-F1 | 変異が担保対象の分岐を殺していない | MUTATION-4 / MUTATION-5 を実行し、本欄に記録 |
| QA-F2 / Verify-F2 | javadoc が未実行の変異を「落ちる」と断定 | 3テストとも、実行して確認した変異だけを述べる文言へ修正し、記録先（本ファイル）を示した |
| QA-F3 | 確認コマンドが記録されていない | 本欄に実行コマンドと結果を記録 |
| Craft-F1 | 先例（`:559-575` のセクション）は4/4テストの javadoc に `description が述べる挙動:` ＋該当一文の引用を置いているのに、新規3テストは 0/3 | 3テストとも追加（shortRow=「不足した末尾のフィールドは `""` として扱われる」、emptyRow=「空配列 `[]` を1要素書くと全フィールドが `""` のレコード1件になる」、noRows=「rows が0件でも有効」） |
| Craft-F2 | 新規 `emptyRow` と既存 `allBlankFieldRecordIsKept` は結論が同一で入力表記だけが違うのに相互参照が無い | 双方向に `{@link}` を張り、「要素数 0 の配列」と「空文字を明示的に並べた配列」の書き分けを javadoc とフィクスチャコメントに明記 |
| Craft-F3 | 初回 `assertThat` だけメッセージ無し（踏襲元 `:636` `:668` は付いている） | 3テストとも「1 件取得できること」を付与 |

## `#24` へ送った／スコープを広げた項目

- **(a)**: 観点A・観点B が独立に確認し、coordinator も一次情報で裏取り（`TableData.java:196-199`）。`#24` step A に出典チェーンを記録
- **観点D の F-D1**: `:108` の是正対象は setup_tables の1文だけでなく **expected_tables / expected_complete_tables の2文も同じ理由で実装と食い違う**。coordinator が実物確認（`Assertion.java:256` `:297-302` / `TableData.java:709-712`）。`#24` step B のスコープを3文へ拡張
- **観点D の別枠 O-D1 / O-D2 / O-D3**: `#24` に記録のみ

## ユーザー判断が要る1件（`#22` の完了を塞がない）

**観点C の C-3 — `:361` の「ファイル系」をどうするか。** `954f20b` で `:108` の「ファイル系（record_fragment）」という等値表現を外した結果、ファイル全体で「ファイル系」の出現は `:361` の見出し【ファイル系の rows は配列の配列】1箇所だけになった。`:108` と `:361` は互いを名指しする対の記述なので非対称が目立つ。方向としては `:108` の変更が正しい（`record_fragment` の `$ref` 元は `file_data` / `message_data` / `expected_request_message_data` / `group_message_data` の4つで、到達するトップレベルは `setup_files` / `expected_files` / `messages` / `expected_request_header_messages` / `expected_request_body_messages` / `response_header_messages` / `response_body_messages` の7セクション。「ファイル系」はメッセージング系を取りこぼす）。**`:361` は A0・A1 いずれの対象でもないため、直すかどうかはユーザー判断。** 直す場合は `#24` に相乗りさせるのが自然。

## nablarch-document 側への報告候補（当リポジトリの範囲外。coordinator が実物確認済み）

- **`testdata_notation.rst:1534`**: 「YAML では `rows:` 内の要素が空マッピング（`{}`）または**すべての値が空文字**の場合にスキップされる」— 直前の文が「全要素が null または空文字」と書いているのに、YAML の説明が空文字だけになっており、**全ての値が null（クォートなし `null` / 値省略 `COL:`）の行も除去される**ことが読み取れない。実装は `YamlSection.java:203-206` で null と空文字を同列に扱う
- **`testdata_notation.rst:830` / `:1443-1445`**: 「アンクォートの `null`（`"null"` でも同じ結果）」— 最終的な値としては同じだが、**空行判定では等価でない**（クォートなし `null` はロード時点で Java null → 行ごと除去／`"null"` は文字列 → 行が残る）。`:1534` の空行スキップ規則と併読すると、全値が `null` の行と全値が `"null"` の行で結果が変わる点が現状の記述からは導けない
- **既出**: `checks/task-18.md`（5件・`rst:883` の2件はセット）、`checks/task-21.md`（`rst:819` と `rst:1534`）

**なお `rst:658` と `rst:819` は正しい**。「`rows:` の先頭行のキーの一部を後続の行が持たない場合、そのカラムは `null` を明示的に指定したのと同じ扱いになる」と実装どおり書かれている。**食い違っているのはスキーマ側（`#24` の対象）だけ**であり、解説書側の報告候補ではない。

## Overall Verdict（ラウンド2）

- step A0 + A1: 観点D が OK、観点A・B の NG は `#24` へ送った1件のみ、観点C の指摘は3件反映・1件をユーザー判断へ
- step A〜E: Verification が OK、QA・Craft の指摘はすべて反映済み（追加変異2件を実行して記録）
- `mvn -o clean test`: `Tests run: 210, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS
- Ready to check off: **Yes**（`#24` と C-3 は `#22` の完了条件の外）
