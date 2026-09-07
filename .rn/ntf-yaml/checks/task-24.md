# task-24 Completion Check

`:108` のカラム省略まわりの記述の乖離を是正する。スコープは 2026-08-24 ユーザー指示の3点 (a)(b)(c)。

**経緯**: 2件とも `#21` の空行除去（全値が null／空文字の行を列名決定より前に取り除く）によって**カラム名決定行が動くようになったため顕在化した**。空行除去が入る前は「先頭行＝1行目」で固定だったため、「省略したカラム」の単位のあいまいさが表面化しにくかった。

---

## step C（波及先の特定）— 反映より前に実施

### スキーマ内の相互参照

| 箇所 | 記述 | 判定 |
| --- | --- | --- |
| `:18` `properties.expected_tables.description` | 「rows に記載したカラムのみ比較対象（省略カラムは比較しない）」 | **波及先**。`:108` の expected_tables 文と同じ主張で同じ乖離。是正対象に含めた |
| `:25` `properties.expected_complete_tables.description` | 「省略カラムにはカラム型ごとのデフォルト値（…）を補完してから全カラム比較する」 | **波及先**。`:108` が「（親 description 参照）」でここを引いている。是正対象に含めた |
| `:108` 内 `【カラム省略の注意: FK 制約違反とデフォルト値補完】` ブロック | 「setup_tables の rows でカラムを省略すると、数値型カラムには…`"0"` を補完して INSERT する」 | **波及先**（同一 description 内）。省略の単位が同じくあいまい。是正対象に含めた |
| `:108` 冒頭 `【テーブル系の rows はオブジェクト配列】record_fragment の rows は配列の配列である点に注意` | — | (b) の対になる記述。`:361` を `record_fragment` 表記に揃えると対称になる。**変更不要** |
| `:136` `$defs.list_map_data.properties.rows.description` | 空行除去の規則を `table_data` と共通と述べる | カラム省略の帰結には触れていない（`list_maps` は DB へ INSERT しないため (1)(2) の区別が無い）。**変更不要** |

調査コマンド:

```
$ for w in "ファイル系" "テーブル系" "比較対象外" "親 description" "デフォルト値"; do \
    grep -no "$w" src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json | cut -d: -f1 | sort -u | tr '\n' ' '; echo " <- $w"; done
361  <- ファイル系
108 361  <- テーブル系
108  <- 比較対象外
108  <- 親 description
25 108  <- デフォルト値
```

### 既存テストのうち文言に依存しているもの

**description の文字列を assert しているテストは0件**。

```
$ grep -rn "description" src/test --include=*.java
（ヒットは list_maps の "description" カラム名を扱う YamlTableDataBuilderTest:842,859 と、
  JSON ポインタでスキーマ箇所を指すコメント・javadoc のみ）
```

スキーマ箇所を JSON ポインタで指しているのは `YamlFileBuilderTest` の以下だけで、いずれも `$defs.record_fragment.properties.rows.description`（`:377`）と `$defs.directives.*`（本タスク対象外）を指す。`:361`（親 description）を指すテストは無い。

```
src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java:925,949,984,1019
src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java:566,567,585,622,650,682
```

**挙動の前提として是正後の文と矛盾してはならないテスト**（文字列依存ではないが、(1) の挙動を固定している）:

- `YamlTableDataBuilderTest#buildTableDataList_fillDefaultValues`（`:165-188`）— `completedTable.yaml` は行が1件で `PK_COL1`/`PK_COL2` のみ。他カラムは**全行で省略**（＝ (1)）。`NUMBER_COL` が `"0"` に補完されることを固定
- `YamlTestDataParserTest#getExpectedTableDataCompleted`（`:1171-1197`）— 同じフィクスチャで `expected_complete_tables` 経路。`NUMBER_COL="0"` / `VARCHAR2_COL=" "` を固定

いずれも (1) のケースのみで、是正後の文（(1) は補完される／(2) は null）と矛盾しない。**テストの変更は不要**。

---

## step A（(a) の帰結の実物確認）— INSERT の実行箇所まで通した

**主張**: 「カラム名決定行にあって当該行で省略したカラムは、デフォルト値補完ではなく NULL が INSERT される／比較対象から外れない」

出典チェーン（すべて実物で確認。本体は `../nablarch-testing`、`convert-testdata-excel-to-text` ブランチ `2e43786`。読み取りのみ）:

1. `nablarch-testing-yaml` `YamlTableDataBuilder.java:219-221` — `extractRows` は `columnNames` だけを走査し、行に無いキーは `objectToString(rowMap.get(col))` = `null` を格納する（「不在」ではなく「値 null」になる）
2. 本体 `TableData.java:523-534` `addRow` — `columnNames` 全件を `map.put(columnNames[i].toUpperCase(), value)` する。値が null でも**キーは入る**
3. 本体 `TableData.java:137-177` `insertData` — INSERT の実行箇所。`:139` `getNonComputedColumns()` が返すのは **DB の全カラム**（`:325-334`。`dbInfo.getColumns(tableName)` から computed カラムだけを除いたもの）で、YAML に書いたカラムではない。`:140-141` でその全カラム分の `INSERT INTO ...(...) VALUES (?,...)` を組み立て、`:159` / `:167` で `convert(row, columnName, rowIndex)` の戻り値をバインドし、`:171-177` `addBatch` → `executeBatch` で実行する
4. 本体 `TableData.java:189-199` `convert` —
   - `:191-193` `if (!row.containsKey(columnName)) return getDefaultValue(columnName);` → **キーが無いカラムだけ**デフォルト値になる。これは (1)（全行で省略＝`columnNames` に入らないカラム）に当たる
   - `:196-199` `Object orig = row.get(columnName); if (orig == null) return null;` → 2 でキーは入っているので (2) はここを通り、**null がバインドされる**
5. 本体 `Assertion.java:249-311` `assertTableEquals` — `:256` `String[] columns = expected.getColumnNames();` を取り、`:297-302` でその全カラムを `assertEqualsAsString` で1件ずつ比較する。カラム名は列名決定行のキーで確定しているため、**(2) は null として比較され、比較対象から外れない**
6. 本体 `TableData.java:707-723` `fillDefaultValues` — `:709-712` の `omittedColumns` は `allColumns - columnNames` の差集合。`:715-719` はその差集合だけを埋める。**行単位の省略（(2)）は補完対象に入らない**

解説書も同じ結論（`#22` レビュー観点B の報告。今回は再点検のみ）: `testdata_notation.rst:658` / `:819`「`rows:` の先頭行のキーの一部を後続の行が持たない場合、そのカラムは `null` を明示的に指定したのと同じ扱いになる」。**食い違っていたのはスキーマ側**。

---

## step B（(a) の反映）

`:108` の該当3文を、省略の単位を (1)(2) に分けた3段落へ差し替えた。あわせて波及先（`:18` / `:25` / `:108` 内 FK ブロック）も同じ区別に揃えた。

### 適用後の全文（`:108` = `$defs.table_data.properties.rows.description`）

```
データ行。各要素がレコード1件（キー=カラム名、値=セル値）。
【テーブル系の rows はオブジェクト配列】record_fragment の rows は配列の配列である点に注意。
数値・真偽値も必ず文字列（クォート付き）で記述すること（例: AGE: "30"、FLAG: "true"）。
値を Java null にする書き方は2系統ある。クォートなしの小文字 `null` と、キーだけ書いて値を省略した `COL:` はロード時点で null になる。クォート付きの `"null"` や大文字を含む `NULL` / `Null` は文字列としてロードされ、NullInterpreter が null へ変換する（大文字・小文字は区別しない）。日付型カラムに `""` を指定すると null 扱いになる（文字型の `""` は空文字のまま INSERT される）。
【全ての値が空の行は行として存在しない】空マッピング `{}` の行、および全ての値が null または空文字 `""` の行は、行が無いものとして取り除かれる。判定は行の全ての値を対象とし、マーカーカラム（`[COL]` のように `[` と `]` で囲んだ、DB 操作の対象外となるカラム）の値も含める。文字列としてロードされる null 表記は非空のため、これだけの行は残り、値が null になる。この除去はカラム名の決定より前に行われるため、カラム名は残った先頭の行のキーで決まる（後続の行にしか無いキーは無視される）。除去の対象はテーブル系（table_data / list_map_data）の rows だけで、record_fragment の rows には適用されない（全フィールドが空文字のレコードは1件として保持される）。
rows が 0 行のとき（空配列 `[]` を書いた場合と、上記の除去で全行が取り除かれた場合）、setup_tables では対象テーブルの全件 DELETE のみ実行され（INSERT なし）、expected_tables / expected_complete_tables では対象テーブルに行が存在しないことの検証になる。
【カラムの省略は2種類あり扱いが異なる】(1) 全ての行で省略したカラム（＝カラム名決定行に無いカラム）はカラム名の集合に入らない。(2) カラム名決定行にはあるが個々の行で省略したカラムは、その行でそのカラムに `null` を書いたのと同じ扱いになる（キーが無い状態ではなく値が null の状態で保持される）。カラム名は上記の空行除去のあとに残った先頭の行のキーだけで決まるため、同じカラムが (1) と (2) のどちらに当たるかは行の並びによって変わる。
(1) の挙動はセクションにより異なる: setup_tables では INSERT 時にカラム型ごとのデフォルト値が補完される（主キーは省略しないこと）。expected_tables では比較対象に入らない。expected_complete_tables では型ごとのデフォルト値（親 description 参照）を補完したうえで DB の全カラムが比較される。
(2) の挙動は3セクション共通で、いずれも null として扱われる。setup_tables ではデフォルト値の補完は行われず NULL が INSERT される（NOT NULL 制約のあるカラムでは INSERT が失敗する）。expected_tables / expected_complete_tables では期待値 null として比較される（比較対象から外れることはない）。
【カラム省略の注意: FK 制約違反とデフォルト値補完】setup_tables の rows であるカラムを全ての行で省略すると（上記 (1)）、数値型カラムには NTF がデフォルト値 `"0"` を補完して INSERT する。FK 制約のある数値カラム（外部キー）を全ての行で省略すると `"0"` が INSERT され、参照先テーブルに ID=0 の行が存在しない場合は FK 制約違反になる。FK カラムは必ず明示的に値を記述すること。また、NULL 許容カラムを NULL にしたい場合は省略せず `null`（クォートなし）を明示すること（上記 (1) の省略は NULL ではなくデフォルト値の補完を意味し、上記 (2) に当たるかどうかは行の並びに依存するため）
```

### 適用後の全文（`:18` / `:25`）

```
expected_tables (:18):
テスト実行後に NTF が DB の実際の状態と照合するデータ。比較対象になるカラムは、空行除去後の先頭行（カラム名決定行）に現れたカラムだけである（全ての行で省略したカラムは比較されない。カラム名決定行にあり個々の行で省略したカラムは null として比較される。詳細は $defs.table_data の rows を参照）。同一 group_id を持つ複数エントリはすべて収集されそれぞれ照合される。期待行と DB 行の対応付けは DB の主キーで行われる（主キーカラムは省略しないこと）。対象テーブルに存在する DB 行は全件を rows に列挙すること（列挙されていない行が DB にあると照合エラーになる。部分検証は不可）。主キーが自動採番のテーブルでは期待側に主キー値を書けないため複数行の検証が成立しない

expected_complete_tables (:25):
テスト実行後に NTF が DB と照合するデータ。省略カラムにはカラム型ごとのデフォルト値（数値型="0"、固定長文字=半角スペース×カラム長、可変長文字=" "、日付型=epoch 起点の JVM タイムゾーン依存値、バイナリ型=10バイトゼロ列の HexString、Boolean="false"）を補完してから全カラム比較する。補完されるのは全ての行で省略したカラム（＝カラム名決定行に無いカラム）だけで、カラム名決定行にあり個々の行で省略したカラムは null のまま比較される（詳細は $defs.table_data の rows を参照）。期待行と DB 行の対応付けは DB の主キーで行われる（主キーカラムは省略しないこと）
```

---

## step B2（(b) の反映）

`:361` `$defs.record_fragment.description` の見出し「【**ファイル系**の rows は配列の配列】」→「【**record_fragment** の rows は配列の配列】」。

**着手前の独立検証（反例なし）**:

```
$ python3 -c "import json;d=json.load(open('src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json'));
  print({k:(v.get('items',{}).get('\$ref')) for k,v in d['properties'].items()})"
setup_tables            -> #/$defs/table_data
expected_tables         -> #/$defs/table_data
expected_complete_tables-> #/$defs/table_data
list_maps               -> #/$defs/list_map_data
setup_files             -> #/$defs/file_data
expected_files          -> #/$defs/file_data
messages                -> #/$defs/message_data
expected_request_header_messages -> #/$defs/expected_request_message_data
expected_request_body_messages   -> #/$defs/expected_request_message_data
response_header_messages -> #/$defs/group_message_data
response_body_messages   -> #/$defs/group_message_data

$ grep -n '"\$ref": "#/\$defs/record_fragment"' src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json
184  (file_data)
210  (message_data)
243  (expected_request_message_data)
274  (group_message_data)
```

`record_fragment` の `$ref` 元は4つ、そこから到達するトップレベルは **7セクション**（`setup_files` / `expected_files` / `messages` / `expected_request_header_messages` / `expected_request_body_messages` / `response_header_messages` / `response_body_messages`）。「ファイル系」ではメッセージング系5つを取りこぼす。**反例は見つからなかった**ため案どおり反映した。

適用後（`:361`）:

```
レコード種別1ブロック。1つのレコードレイアウト（フィールド定義 + データ行）を表す。
【record_fragment の rows は配列の配列】テーブル系（table_data / list_map_data）の rows はオブジェクト配列である点に注意
```

これで `:108` の「【テーブル系の rows はオブジェクト配列】record_fragment の rows は配列の配列である点に注意」と対になる。

---

## step D（JSON 妥当性・テスト）

```
$ python3 -c "import json;json.load(open('src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json',encoding='utf-8'));print('JSON OK')"
JSON OK

$ git diff --stat
 src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json | 8 ++++----
 1 file changed, 4 insertions(+), 4 deletions(-)

$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
[INFO] Tests run: 210, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`description` 以外（`type` / `items` / `required` / `enum` / `pattern` 等）は変更していない（diff は description 行4本のみ）。

---

## 完了条件の self-check

| 完了条件 | 判定 | 根拠 |
| --- | --- | --- |
| (a) の帰結が実物の出典（ファイル:行番号）つきで確定している | OK | step A の6点。INSERT の実行箇所（`TableData.java:137-177`）と `getNonComputedColumns`（`:325-334`）まで通した |
| `:108` の記述が (a)(b) いずれについても実装と食い違わない | OK | step B / B2 |
| step C で特定した波及先が是正されている | OK | `:18` / `:25` / `:108` 内 FK ブロック。既存テストは文言非依存のため変更不要 |
| `description` 以外のスキーマ要素が変更されていない | OK | `git diff` は description 行4本のみ |
| `mvn -o clean test` が BUILD SUCCESS | OK | 210 tests / 0 failures |

---

## 範囲外の欠陥（`#24` では直さない。課題として起票）

- **X-1（`#24` 対象外・スコープ外）**: steering `#24` の旧 step C（マーカーカラム `[COL]` だけが非空の行がカラム名決定行になったときの帰結 = `dataColumns` が0件になり全デフォルト値の1行が INSERT される／`list_maps` では空 Map が1件渡る）は、2026-08-24 のユーザー指示でスコープが3点に確定した際に外れた。**未実施・未検証**。別タスクとして起票が必要
- **O-D1（既出・本体側）**: `:108` 末尾の「NULL 許容カラムを NULL にしたい場合は省略せず `null` を明示すること」は BOOLEAN 型カラムでは NPE になる疑い（本体 `TableData.java:162-163` の `row.getBoolean(columnName)` が null を返すと `setBoolean(int, boolean)` の unboxing で落ちると読める）。**静的読解のみ・実行未確認**。本体 Excel 経路と共通の挙動

---

## レビュー ラウンド1（2観点: B 整合 / D 検証の妥当性）

**観点A（充足）・観点C（規約）は回さない**（2026-08-24 ユーザー指示）。理由: `#24` は既存 description の是正であり実装変更が無く、外から観測できるものに新しいものが入らない。観点A は対象が (a) の3文と (b) の1語に確定済みで抜けが生じない。観点C は既存 description の文体に揃えるだけで実害が小さい。

各担当は個別の作業ディレクトリ（`scratchpad/rev24-B` / `scratchpad/rev24-D`）で独立に実施。指示に「実測で裏付ける／付属の検証スクリプトを正解として使わず独立に組む／敵対的に見る」を明記。

### D（検証の妥当性）— 総合 fail

**指摘（Valid）**:
- **D-1**: `:108` に書いた INSERT・比較まわりの主張7点は、コミット当初（`2f060e8`）は実行で確かめられていなかった（INSERT 経路に到達する既存テストが0件）。`#22` が確立した「実経路テスト＋変異確認」の水準を満たしていなかった
- **D-2**: 主張はすべて実経路テストで固定可能（D 自身が12件のテストと5系統の変異確認で実証済み）
- **D-3**: `(1)` の定義「全ての行で省略したカラム（＝カラム名決定行に無いカラム）」の「＝」が成り立たない場合がある。後続の行にだけ書いたキーも `(1)` として扱われ、値が捨てられる

**要確認**: NOT NULL の断定形が全 DB で成り立つか（H2 でのみ実測）

### B（整合: B-1 成果物 / B-2 指示文）— B-1 NG・B-2 反例あり

**B-1（Valid）**:
- **V-1**: `(2)` の「NULL が INSERT される」は Boolean 型カラムでは成り立たない。`SqlRow#getBoolean` の戻り値 `Boolean` を `setBoolean(int, boolean)` へ渡す際の unboxing で `NullPointerException` になる（実測: `java.lang.NullPointerException`）。NULL 許容カラムでも起きるため「NOT NULL 制約のあるカラムでは失敗する」の限定にも当てはまらない
- **V-2**: D-3 と同一の指摘（(1) の「＝」の不正確さ）

**B-2（指示文の反例）**:
- **V-3 / V-4**: (a) の根拠3点目「`TableData.java:193` `row.containsKey` … → カラム名決定行にあって当該行で省略したカラムは、**補完値で比較される**」は**偽**（行番号も191への2行ずれ）。実装は逆で、比較経路は `convert` を通らず期待値 null のまま比較される。**この指摘は反映していない**（コミット済みの `:108` はもともと「期待値 null として比較される」で正しかった）
- (a) の残り3点と (b) 案は真。行番号もすべて一致

**要確認**: `:18` の「カラム名決定行に現れたカラムだけ」はマーカーカラムという例外が1つある

### 反映（Valid のみ）

1. `:108` の `(1)` 定義をカラム名決定行基準に一本化（V-2 / D-3）
2. `:108` の `(2)` 挙動に Boolean 型の NPE 例外を追記（V-1）
3. `:108` 内 FK ブロックの「全ての行で省略」表現を `(1)` の新定義に揃えた
4. `:18` に「マーカーカラムは DB 操作の対象外なので除く」を補った（Q-1）
5. `:25` の「全ての行で省略」表現も同様に揃えた
6. **テストを新規追加**: `src/test/java/nablarch/test/core/db/YamlColumnOmissionTest.java`（14件）。`:18` / `:25` / `:108` の主張を `YamlLoader.load` → `YamlTableDataBuilder` → 本体 `TableData#insertData` / `Assertion#assertTableEquals` の実経路で固定。Boolean NPE（V-1）と `null` 明示との同値性の2件も追加
7. V-3 は反映しない（指摘が偽のため）

### 変異確認（5系統・全14テストが少なくとも1系統で死亡）

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlColumnOmissionTest
```

| 変異 | 内容 | 殺したテスト数 |
| --- | --- | --- |
| M1 | `buildTableData` の値ループで `interpret` の null を `" "` に強制置換 | 8/14 |
| M2 | `YamlSection#resolveColumns` を「先頭行のキー」から「全行のキーの和集合」へ | 3/14 |
| M3 | `if (fillDefaults)` を `if (true)` に固定 | 5/14 |
| M4 | `if (fillDefaults)` を `if (false)` に固定 | 2/14 |
| M5 | `buildTableData` の `dataColumns` を反転（値と列名の対応をずらす） | 12/14 |

союз（和集合）で 14/14 が少なくとも1系統で FAILURE/ERROR。生存変異ゼロ。各変異後は該当ファイルを元のバックアップへ復元し、`git diff --stat src/main` が `ntf-testdata-yaml-schema.json` のみであることを確認した。

### 反映後の確認

```
$ python3 -c "import json;json.load(open('src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json',encoding='utf-8'));print('JSON OK')"
JSON OK

$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.53 s - in nablarch.test.core.db.YamlColumnOmissionTest
...
[INFO] Tests run: 224, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

（209 → 224 は `#24` で追加した `YamlColumnOmissionTest` 14件分。#19 完了時点の210件から起算すると +14 = 224 で一致）

`description` 以外のスキーマ要素は未変更（`git diff` はスキーマ4描述＋新規テストクラス・フィクスチャのみ）。

---

## レビュー ラウンド2（差分限定2観点: 是正が指示範囲に収まっているか／是正が新しい欠陥を生んでいないか）

対象差分: `c56207d`（ラウンド1の指摘反映コミット）と `52611b1`（wip・非デリバラブル）。各担当は個別サブエージェントで実施し、指示に「実測で裏付ける／付属の検証スクリプトを正解として使わず独立に組む／敵対的に見る」を明記。

### 観点1（差分が指示範囲に収まっているか）— Invalid

**指摘**: `c56207d` の変更が `:361`（`$defs.record_fragment.description` の「ファイル系」→「record_fragment」）にも及んでおり指示範囲（`:18`/`:25`/`:108`＋新規テスト）外である、という指摘。

**判定: Invalid（事実誤認）**。コーディネーターが独立に `git diff c56207d~1 c56207d -- src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` を実行して確認したところ、`c56207d` 単体の変更は `:18`/`:25`/`:108` の3つの `description` 値のみ（3 insertions, 3 deletions、`git show --stat` の `1 file changed, 3 insertions(+), 3 deletions(-)` と一致）。`:361` の変更は `2f060e8`（#24 step B2、2026-08-24 ユーザー承認済みスコープ (b)）に属し、`c56207d`/`52611b1` の差分には含まれない。担当サブエージェントが `2f060e8~1..c56207d` という誤った比較範囲を使ったために生じた誤指摘。

### 観点2（是正が新しい欠陥を生んでいないか）— Valid 1件

**指摘（Valid）**: `c56207d` で `:108` の `(2)` 段落に追記した Boolean 型 NPE の例外文「行ごとに省略せず値を明示すること」は、NPE の原因を「行ごとの省略」に限定して読める書き方になっている。実際の原因は「値が null であること」であり、(1)（カラム名決定行に無い＝全行省略）でも、クォートなし小文字 `null` を明示した場合でも同じ NPE になる。この文言は読み手がクォートなし `BOOL_COL: null` と明示することで「対処した」と誤認させ、実際には NPE を防げない。

**実物確認（コーディネーターが独立に再確認、一致）**:
- `../nablarch-testing` `TableData.java:163-164`（`insert.setBoolean(bindIndex++, row.containsKey(columnName) ? row.getBoolean(columnName) : (Boolean) getDefaultValue(columnName));`）— 前回指摘の行番号「162-164」から163-164への訂正が正しいことを `nl -ba` で確認
- `nablarch-testing-yaml` `YamlSection.java:247-249`（`interpret` の null チェック）— 行番号一致を確認
- `SqlRow#getBoolean` の null 伝播: 依存 jar が `2.2.0` ではなく `6-NEXT-SNAPSHOT` である点の訂正を含め、`javap -c` によるバイトコード確認で null 伝播を独立に検証済み（報告どおり）

**反映**:
1. `:108` の `(2)` 段落の当該文を、原因の経路（行内省略／クォートなし null／`COL:` 省略／クォート付き `"null"`）を問わず「値が null になれば」NPE になる旨へ書き替え、「null を明示しても防げない」ことを明記
2. `YamlColumnOmissionTest.java` に `setupThrowsNpeWhenBooleanColumnIsExplicitNull`（明示 null 系統）を新規追加、フィクスチャ `omission.yaml` に `s10` を追加
3. 変異確認: `TableData.java` の Boolean 分岐を「値が null なら getDefaultValue へフォールバック（NPE を投げない実装）」に変異させ、独立にコンパイル・classpath 差し替えで実行 → コーディネーターが自分のコマンドで再実行し **`Tests run: 15, Failures: 2`**（新規テストと既存の行内省略テストの両方が期待どおり FAIL）を確認。報告値と一致

**範囲外として記録のみ（修正しない）**: `:108` 内 FK ブロックの「NULL 許容カラムを NULL にしたい場合は省略せず `null`（クォートなし）を明示すること」は Boolean 型カラムに対して同様に誤誘導になるが、この文は `c56207d` 以前（`2f060e8` 時点）から存在し、ラウンド2の対象（`c56207d`/`52611b1` が新たに生んだ欠陥）に当たらないため今回は直さない。既出の `O-D1` と同一事象。

### 反映後の確認

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
[INFO] Tests run: 225, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

（224 → 225 は `setupThrowsNpeWhenBooleanColumnIsExplicitNull` 1件分）

`description` 以外のスキーマ要素・実装コードは未変更（`git diff` はスキーマ `:108` の description 1本＋テストファイル2本のみ）。

### ラウンド2 総括

- 観点1: 指摘1件・Invalid（事実誤認と判定、却下）
- 観点2: 指摘1件・Valid（反映済み）
- ラウンド2で新しい欠陥（Valid）が出たため、指示書の完了条件「ラウンド2で新しい欠陥が出なければ完了」に該当せず、**ラウンド3が必要**（上限3回のうち残り1回）

---

## レビュー ラウンド3（差分限定2観点。上限3回のうち最終回）

対象差分: `a99e373`（ラウンド2の指摘反映コミット）単体。

### 観点1（差分が指示範囲に収まっているか）— 範囲内

`git diff --numstat a99e373~1 a99e373` をコーディネーターが独立に再実行し確認: `checks/task-24.md`（+46/-0）・スキーマ（+1/-1、`:108` の description 1行のみ）・`YamlColumnOmissionTest.java`（+16/-0）・`omission.yaml`（+13/-0）の4ファイルのみ。`:18`/`:25`/`:361` を含め他の description・構造要素・実装コード（`src/main/java`）に変更なし。指摘なし。

### 観点2（是正が新しい欠陥を生んでいないか）— 欠陥なし（Invalid）

以下を実測で検証し、いずれも矛盾・誤りなしと判定:

1. **クォート付き `"null"` → NPE の主張**: `:108` の (2) 段落が挙げる4系統（行内省略／クォートなし `null`／`COL:` 省略／クォート付き `"null"`）のうち、クォート付き `"null"` 系統だけが実経路テストで未固定だった。`NullInterpreter`（大文字小文字を区別せず `"null"` を null へ変換）が `yamlInterpreters` に実際に登録されていることを確認したうえで、`YamlColumnOmissionTest#setupThrowsNpeWhenBooleanColumnIsQuotedNullString`（フィクスチャ `s11`、`BOOL_COL: "null"`）を新規追加し、実経路で NullPointerException になることを固定した
2. **FK ブロックとの字面矛盾**: FK ブロックの「NULL 許容カラムを NULL にしたい場合は `null`（クォートなし）を明示すること」は Boolean 型カラムに対しては字面上矛盾する（明示しても NPE になるため）。ただしこの文言は `c56207d` 以前から存在し、`a99e373` が新たに生んだ／悪化させたものではない（既出 `O-D1` と同一事象）。**範囲外として報告のみ、修正しない**
3. **(1) の定義との整合**: 既存テストで Boolean カラムが (1)（カラム名決定行に無い）に該当する場合は `Boolean.FALSE` が補完され NPE にならないことを確認（`:25` の記述と整合）。(2) 段落の NPE 例外文は (2) の原因列挙に明示的に限定されており、(1) との混同は無い

**反映**: `YamlColumnOmissionTest.java` に `setupThrowsNpeWhenBooleanColumnIsQuotedNullString` を追加、`omission.yaml` に `s11` を追加（スキーマの description は変更なし）。

### 反映後の確認

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
[INFO] Tests run: 226, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

（225 → 226 は `setupThrowsNpeWhenBooleanColumnIsQuotedNullString` 1件分。コーディネーターが独立に再実行し一致を確認）

### ラウンド3 総括

- 観点1: 指摘0件（範囲内）
- 観点2: 指摘0件（Invalid＝新しい欠陥なし）。ただし主張の実経路カバレッジを1件補強（テスト追加のみ、description は無変更）
- ラウンド3で新しい欠陥が出なかったため、指示書の完了条件を満たし **`#24` step G 完了**

---

## `#24` step G 総括（ラウンド1〜3）

| ラウンド | 観点1（範囲） | 観点2（新欠陥） | 反映内容 |
| --- | --- | --- | --- |
| 1 | 観点A/B/C/D の4観点構成（初回のみ） | — | B（(1)定義・Boolean NPE 例外・:18 マーカー補足）と D（実経路テスト14件）を反映 |
| 2 | Invalid（:361 は 2f060e8 の承認済みスコープ、誤指摘） | Valid（Boolean NPE 例外文の原因限定を是正） | `:108` (2) 段落の書き替え＋テスト1件 |
| 3 | 範囲内 | Invalid（欠陥なし。カバレッジ補強のみ） | テスト1件追加（クォート付き `"null"` 系統） |

最終 `mvn -o clean test`: **Tests run: 226, Failures: 0, Errors: 0, Skipped: 0**（#24 開始時 210件 → step D で+14=224 → ラウンド2で+1=225 → ラウンド3で+1=226）。
