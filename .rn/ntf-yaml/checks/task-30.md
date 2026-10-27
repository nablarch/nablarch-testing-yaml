# task-30 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `:410`・`:108`・`:136`・`:365` の `description` が解説書と食い違わない（`:208` は変更していない） | OK | 下表「4件の対応表」のとおり、書き換えた文ごとに解説書の該当行を開いて突き合わせた。`$defs.message_data.properties.records.description` は「MessageParser はこの records の record_type を内部で常に "default" に置換する」のまま未変更。追加是正として `table_data.rows` の空行除去段落の1文（下節「追加是正」）も直し、`table_data.rows` と `list_map_data.rows` を通しで読み直して `NullInterpreter` 前提の帰結が他に残っていないことを確認した | | |
| `YamlSection.java:174` の javadoc に `NullInterpreter` 前提の記述が残っていない | OK | `grep -n NullInterpreter src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` が 0 件。該当文は「この順序により、値加工を通すと空になる値だけを持つ行も、行としては保持される。」に短縮 | | |
| `length: "-"` の挙動（最大バイト長への自動拡張・改行とその前後空白の除去・改行なし値の前後空白は残る）を押さえるテストがある | OK | `YamlFileBuilderTest` に 3 メソッド追加（`buildFileList_ondemandLengthExpandsToMaxByteLength` / `buildFileList_ondemandLengthRemovesLineSeparatorWithSurroundingSpaces` / `buildFileList_ondemandLengthKeepsSpacesWhenValueHasNoLineSeparator`）。フィクスチャは `YamlFileBuilderTest/fileData.yaml` の `ondemandLength` / `ondemandLineSeparator` / `ondemandKeepSpaces` グループ | | |
| 追加した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 下節「変異確認」に実行コマンドと 3 件の失敗出力を記録 | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `Tests run: 251, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（ベースライン 248 + 追加 3） | | |

## Overall Verdict

- Self-check: OK

## 4件の対応表（変更前 / 変更後 / 根拠）

### A. `$defs.field_def.properties.length.description`（変更前 `:410`）

- 変更前: `"-" フィールドの値は NTF が格納時に改行コードおよび前後空白を除去する。`
- 変更後: `"-" フィールドの値は NTF が格納時に、値に含まれる改行とその前後の空白を除去する（改行を含まない値の前後の空白は除去されない）。`
- 根拠: `implementation/testdata_notation.rst:1059`（`nablarch-document` `5b5c91e`）
  「フィールドの数だけ記載する。``"-"`` を指定すると、追加した全レコードの最大バイト長に自動拡張される。この場合、値に含まれる改行と、その前後の空白は取り除かれる」
- 実装の裏付け: `nablarch-testing`（`3c4bd2a`）`src/main/java/nablarch/test/core/file/DataFileFragment.java:76`
  `REMOVE_LS_SP_PATTERN = Pattern.compile("\\s*[\\r\\n]\\s*")` を `replaceAll("")`（同 `:160`）。
  改行を含まない値は一致箇所が無いため前後の空白が残る。

### B. `$defs.table_data.properties.rows.description`（変更前 `:108`）

**B-1（FK 制約の文言が BOOLEAN 型カラムで矛盾する）**

- 変更前（FK 段落の末尾）: `また、NULL 許容カラムを NULL にしたい場合は省略せず \`null\`（クォートなし）を明示すること（上記 (1) の省略は NULL ではなくデフォルト値の補完を意味し、上記 (2) に当たるかどうかは行の並びに依存するため）`
- 何が矛盾していたか: 同じ `:108` の前段が「Boolean 型カラムだけは例外で、…値が null になれば NULL を扱えず NullPointerException になるため、true/false のいずれかの値を明示すること（null を明示しても NullPointerException を防げない）」と述べているのに対し、FK 段落の末尾はカラム型を限定せず「NULL 許容カラムを NULL にしたい場合は `null` を明示すること」と指示していた。NULL 許容の Boolean 型カラムでは、この指示に従って `null` を明示すると NULL にはならず NullPointerException になる。同一 description 内で相反する指示が並んでいた。
- 変更後: 上記の文に続けて `。ただし Boolean 型カラムは上記の例外に該当し、\`null\` を明示しても NULL にはならず NullPointerException になるため、NULL 許容カラムであっても true/false のいずれかを明示すること` を追記
- 根拠: 前段の Boolean 例外の記述（同 description 内、既存）。実装の裏付けは `nablarch-testing`（`3c4bd2a`）`src/main/java/nablarch/test/core/db/TableData.java:162`-`:164`（`insert.setBoolean(bindIndex++, row.getBoolean(columnName) ...)`。`setBoolean(int, boolean)` へ Boolean null を渡すためアンボクシングで NPE）
- 補足: 解説書 `implementation/testdata_notation.rst:820`-`:833` の null 値表は「null（Java の null）→ アンクォートの `null`」とだけ述べ、Boolean 型の例外には触れていない。よって B-1 は解説書との食い違いではなく description 内部の矛盾の解消であり、解説書の記述（`null` はアンクォートで書く）と衝突しない形で例外を明示した。

**B-2（`NullInterpreter` 前提の記述）**

- 変更前①: `値を Java null にする書き方は2系統ある。クォートなしの小文字 \`null\` と、キーだけ書いて値を省略した \`COL:\` はロード時点で null になる。クォート付きの \`"null"\` や大文字を含む \`NULL\` / \`Null\` は文字列としてロードされ、NullInterpreter が null へ変換する（大文字・小文字は区別しない）。`
- 変更後①: `値を Java null にする書き方は2つある。クォートなしの小文字 \`null\` と、キーだけ書いて値を省略した \`COL:\` で、いずれもロード時点で null になる。クォート付きの \`"null"\` や大文字を含む \`NULL\` / \`Null\` は文字列としてロードされ、文字列のまま扱われる（YAML 経路では NullInterpreter を指定しないため null へは変換されない）。`
- 変更前②（Boolean の文の中）: `原因が (2) の行ごとの省略であるか、クォートなし小文字 \`null\`・\`COL:\` の値省略・クォート付き \`"null"\`（NullInterpreter 変換後）のいずれであるかを問わず、`
- 変更後②: `原因が (2) の行ごとの省略であるか、クォートなし小文字 \`null\`・\`COL:\` の値省略のいずれであるかを問わず、`
- 根拠: `setup/common.rst:81`（`5b5c91e`）「``NullInterpreter`` を指定してはならない。指定すると、文字列として記述した ``"null"`` も Java の null になり、両者を区別できなくなる。」
  および `implementation/testdata_notation.rst:1399`「Java の null にしたい場合のみクォートなしで ``null`` と記述する（``"null"`` とクォートした場合は文字列になる）」

**空行除去の条件（#26 で是正済み）の現状確認**: `:108` の【全ての値が空文字の行は行として存在しない】の段落はそのまま。今回の変更は上記 3 箇所のみで、空行除去の条件には触れていない。

### C. `$defs.list_map_data.properties.rows.description`（変更前 `:136`）

- 変更前: `Java null（クォートなしの \`null\`・値を省略した \`COL:\`）と \`"null"\` / \`NULL\` は、いずれも空文字ではないため非空として扱われ、これだけの行は残り、マップの値が Java null になる。`
- 変更後: `Java null（クォートなしの \`null\`・値を省略した \`COL:\`）と、文字列としてロードされる \`"null"\` / \`NULL\` は、いずれも空文字ではないため非空として扱われ、これだけの行は残る（YAML 経路では NullInterpreter を指定しないため、前者はマップの値が Java null になり、後者は文字列のままマップの値になる）。`
- 根拠: `setup/common.rst:81`（`5b5c91e`）
- 空行除去の条件（#26 で是正済み）は未変更。

### D. `$defs.record_fragment.properties.record_type.description`（変更前 `:365`）

- 変更前: `メッセージング系（messages / expected_request_* / response_*）では NTF 内部で常に "default" に置換されるため実行時の挙動に影響しない（可読性のために任意の名前を記述してよい。FW_HEADER のような予約値はない）`
- 変更後: `messages では記載した値は使われず、NTF 内部で常に "default" になるため実行時の挙動に影響しない（可読性のために任意の名前を記述してよい）。同期応答メッセージ送信で使う4セクション（expected_request_header_messages / expected_request_body_messages / response_header_messages / response_body_messages）では、記載した値がそのままレコード種別になる。いずれのセクションでも FW_HEADER のような予約値はない`
- 根拠: `implementation/testdata_notation.rst:1163`（`5b5c91e`）
  「``MESSAGE``（``setUpMessages``・``expectedMessages``）では、記載した値は使われず、デフォルトのレコード種別（``"default"``）になる。同期応答メッセージ送信で使う4つのデータタイプ（``EXPECTED_REQUEST_HEADER_MESSAGES``・``EXPECTED_REQUEST_BODY_MESSAGES``・``RESPONSE_HEADER_MESSAGES``・``RESPONSE_BODY_MESSAGES``）と取引単体テストのモックアップクラスの電文では、記載した値がそのままレコード種別になる。」
- セクション名は `ntf-testdata-yaml-schema.json` の `properties` キー（`expected_request_header_messages` / `expected_request_body_messages` / `response_header_messages` / `response_body_messages`）に合わせた。

### 変更しないもの（確認）

- `$defs.message_data.properties.records.description`（変更前 `:208`）は未変更。現在の値:
  `電文本文のレコード定義。FW 制御ヘッダは fw_header に記述するため records には含めない（旧形式の record_type: FW_HEADER は廃止）。MessageParser はこの records の record_type を内部で常に "default" に置換する`
- スキーマの構造（`type` / `required` / `properties` / `enum`）は未変更（差分は description 4 行のみ）。
- `python3 -c "import json,io; json.load(io.open('src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json','r',encoding='utf-8'))"` → `JSON OK`

## `length: "-"` の除去処理の所在

- 本モジュール（`YamlFileBuilder#buildFragmentsInternal`）は `length` の記載値（`"-"` を含む）をそのまま `DataFileFragment#setLengths` へ渡し、値行を `addValue` / `addValueWithId` へ渡すだけである（`src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java:218`-`:250`）。
- 実際の除去と長さ拡張は依存先 `nablarch-testing`（`3c4bd2a`）の `DataFileFragment` にある。
  - `setLengths`（`:286`-`:293`）で `"-"` のフィールドを `isOndemandCalcFieldSizeList` に記録
  - `addValue`（`:102`-`:115`）／`addValueWithId`（`:169`-`:182`）が該当フィールドについて `removeLineSeparatorWithTrim`（`:158`-`:161`。`Pattern "\s*[\r\n]\s*"` を空文字へ置換）を適用し、`replaceFieldSize`（`:137`-`:152`。既存値より長い場合のみ更新＝最大バイト長を保持）でフィールド長を拡張

## 追加したテスト

| テスト | 押さえた点 | 検証方法 |
|---|---|---|
| `buildFileList_ondemandLengthExpandsToMaxByteLength` | 全レコードの**最大バイト長**への自動拡張 | `ondemandLength` グループ（FIELD1=半角/`"-"`: "A"/"ABCDE"/"ABC"、FIELD2=全角/`"-"`: "あ"/"あいう"/"あい"、FIELD3=半角/2）。`createLayout()` のフィールド位置が 1 / 6 / 12、`record-length` が 13（=5+6+2）。最終行の長さ（3・4）ではなく最大（5・6）であること、および FIELD2 が文字数（3）ではなくバイト数（6）であることを固定 |
| `buildFileList_ondemandLengthRemovesLineSeparatorWithSurroundingSpaces` | **改行とその前後の空白**の除去 | `ondemandLineSeparator` グループ（FIELD1=半角/`"-"`、値 `"AB \r\n CD"`）。`toDataRecords().get(0).getString("FIELD1")` が `"ABCD"`、`record-length` が 4 |
| `buildFileList_ondemandLengthKeepsSpacesWhenValueHasNoLineSeparator` | **改行なし値の前後空白は残る** | `ondemandKeepSpaces` グループ（FIELD1=半角/`"-"`、値 `"  AB  "`、FIELD2=半角/1、値 "Z"）。FIELD2 の位置が 7、`record-length` が 7 → FIELD1 は 6 バイトのまま（除去されていれば 2 バイト）。加えて `getString("FIELD1")` が `"  AB"`（先頭の空白は残る。末尾側は半角（X）データタイプの読み出し時パディング除去で落ちるため、バイト長で担保） |

期待値は実行前に解説書 `implementation/testdata_notation.rst:1059` と除去パターン `\s*[\r\n]\s*` から算出して記述し、その後に実行した。

## 変異確認（step C）

実行コマンド:

```
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlFileBuilderTest
```

崩した内容（3 件を同時に適用して 1 回実行）:

1. `buildFileList_ondemandLengthExpandsToMaxByteLength`: FIELD2 の開始位置の期待値 `is(6)` → `is(4)`（最大バイト長 5 ではなく最終行 "ABC" の 3 バイトを期待）
2. `buildFileList_ondemandLengthRemovesLineSeparatorWithSurroundingSpaces`: 値の期待値 `is("ABCD")` → `is("AB  CD")`（改行だけ除去され前後の空白は残る、を期待）
3. `buildFileList_ondemandLengthKeepsSpacesWhenValueHasNoLineSeparator`: FIELD2 の開始位置の期待値 `is(7)` → `is(3)`（前後の空白も除去される、を期待）

結果（3 件すべて FAILURE）:

```
[ERROR] Tests run: 33, Failures: 3, Errors: 0, Skipped: 0, Time elapsed: 1.943 s <<< FAILURE! - in nablarch.test.core.reader.yaml.YamlFileBuilderTest
[ERROR]   YamlFileBuilderTest.buildFileList_ondemandLengthExpandsToMaxByteLength:319 FIELD1 が全レコードの最大バイト長（"ABCDE" の 5 バイト）へ拡張されること
Expected: is <4>
     but: was <6>
[ERROR]   YamlFileBuilderTest.buildFileList_ondemandLengthKeepsSpacesWhenValueHasNoLineSeparator:390 改行を含まない値の前後の空白は除去されず、FIELD1 が 6 バイトのままになること
Expected: is <3>
     but: was <7>
[ERROR]   YamlFileBuilderTest.buildFileList_ondemandLengthRemovesLineSeparatorWithSurroundingSpaces:358 値から改行と、その前後の空白が取り除かれること
Expected: is "AB  CD"
     but: was "ABCD"
```

崩した箇所を元に戻したうえで再実行し、`Tests run: 33, Failures: 0, Errors: 0, Skipped: 0` を確認した。

## 最終確認（step D）

```
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
...
[INFO] Tests run: 251, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 解説書の誤りと判断した項目

なし。

## 追加是正（コーディネーターのレビュー指摘）

`$defs.table_data.properties.rows.description` の【全ての値が空文字の行は行として存在しない】段落に、`NullInterpreter` 前提の帰結が1文残っていた（初回コミットでは未変更だった箇所）。

- 変更前: `Java null（クォートなしの \`null\`・値を省略した \`COL:\`）と文字列としてロードされる null 表記は、いずれも空文字ではないため非空として扱われ、これだけの行は残り、値が null になる。`
- 変更後: `Java null（クォートなしの \`null\`・値を省略した \`COL:\`）と、文字列としてロードされる null 表記（クォート付きの \`"null"\`・\`NULL\` / \`Null\`）は、いずれも空文字ではないため非空として扱われ、これだけの行は残る（YAML 経路では NullInterpreter を指定しないため、前者は値が null になり、後者は文字列のまま値になる）。`
- 何が問題だったか: 「値が null になる」が Java null と文字列 null 表記の両方に係っており、後者では成立しない。`NullInterpreter` を指定しない YAML 経路では文字列のままである
- 根拠: `setup/common.rst:81`（`nablarch-document` `5b5c91e`）。`$defs.list_map_data.rows` の同趣旨の文と結論を揃えた
- 通し読みの結果: `table_data.rows` と `list_map_data.rows` を全文読み直し、他に `NullInterpreter` 前提の帰結は残っていない。両 description に残る `NullInterpreter` の言及は「YAML 経路では指定しないため変換されない」という否定形の注記のみ（`table_data.rows` に2箇所、`list_map_data.rows` に1箇所）
- JSON 妥当性: `python3 -c "import json,io; json.load(io.open('src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json','r',encoding='utf-8'))"` → OK
- 再実行: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 251, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`
- `:208`（`$defs.message_data.records`）は引き続き未変更

## コーディネーター独立レビュー

Step 4 では4観点レビューを回さない（指示書 §7）。コーディネーターがコミット済み差分を独立に読み、ビルドを自分で実行し、解説書と依存先の一次情報を自分で確認して検証した。

| 観点 | 判定 | 根拠 |
|---|---|---|
| 差分がタスクの範囲に収まっている | OK | `git diff 1cb35d4..7019093` は `ntf-testdata-yaml-schema.json`（`description` 5行のみ）・`YamlSection.java`（javadoc 1箇所）・`YamlFileBuilderTest.java`・`YamlFileBuilderTest/fileData.yaml` の4件。スキーマの構造（`type`/`required`/`properties`/`enum`）は無変更。`:208` は無変更 |
| A（`length`）の書き換えが解説書と一致 | OK | 「値に含まれる改行とその前後の空白を除去する（改行を含まない値の前後の空白は除去されない）」。解説書 `5b5c91e` の `testdata_notation.rst:1059`「`"-"` を指定すると、追加した全レコードの最大バイト長に自動拡張される。この場合、値に含まれる改行と、その前後の空白は取り除かれる」と一致 |
| B（`table_data.rows`）の矛盾の特定と解消 | OK | 矛盾は同一 `description` 内。前段が「Boolean 型カラムだけは例外で…null を明示しても NullPointerException を防げない」と述べる一方、FK 段落末尾がカラム型を限定せず「NULL 許容カラムを NULL にしたい場合は省略せず `null`（クォートなし）を明示すること」と指示していた。FK 段落末尾に Boolean の但し書きを追記して解消。裏づけの `nablarch-testing`(`3c4bd2a`) `TableData.java:162`-`:164`（`insert.setBoolean(..., row.getBoolean(columnName) ...)` の Boolean null アンボクシング）はコーディネーターも実物で確認 |
| B/C の `NullInterpreter` 前提の除去 | OK | `table_data.rows`・`list_map_data.rows` の両方で「文字列としてロードされる null 表記は文字列のまま」に統一。指摘した「値が null になる」の残存1文も `7019093` で解消し、両者の結論が揃った |
| D（`record_fragment.record_type`）が解説書と一致 | OK | 「messages では記載した値は使われず常に `"default"`」「同期応答メッセージ送信で使う4セクション…では、記載した値がそのままレコード種別になる」。解説書 `testdata_notation.rst:1163` と一致。「FW_HEADER のような予約値はない」も `:1299`-`:1301` に沿って維持 |
| `YamlSection.java` の javadoc | OK | `NullInterpreter` の語が同ファイルから消え、担保している内容（値加工より前に空行判定を行う順序）は変わっていない。実装コードは無変更 |
| `length: "-"` のテスト3点 | OK | 自動拡張（最終行でなく最大バイト長・全角はバイト数）／改行とその前後空白の除去／改行なし値の前後空白は残る、をそれぞれ別テストで固定。除去処理の所在（`nablarch-testing`(`3c4bd2a`) `DataFileFragment#removeLineSeparatorWithTrim`）はコーディネーターも実物で確認 |
| 変異確認が実施されている | OK | 3件同時に期待値を崩して `Tests run: 33, Failures: 3`。崩した3件が過不足なく失敗し、復元後 `Failures: 0` |
| JSON 妥当性 | OK | `json.load` が通ることをコーディネーターが確認 |
| ビルド（コーディネーター自身の実行） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 251, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（2026-08-26 コーディネーターが独立実行） |

**指摘1件・対応済み**: `$defs.table_data.rows` の空行除去の段落に「値が null になる」が Java null と文字列 null 表記の両方に係る文が残り、`list_map_data.rows` 側の直し方と結論が食い違っていた → `7019093` で解消。

## Overall Verdict（コーディネーター）

- コーディネーター独立レビュー: OK
- Ready to check off: Yes
