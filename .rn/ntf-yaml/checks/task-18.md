# task-18 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `rows.description` に「一致しない場合は NTF がエラーを出す」が残っていない | OK | 出典3件を実物で確認: (1) 実装 `/home/tie303177/work/nablarch/nablarch-testing/src/main/java/nablarch/test/core/file/DataFileFragment.java:107` = `String value = i < line.size() ? line.get(i) : "";`（ループは 106 行目 `for (int i = 0; i < names.size(); i++)`）。(2) 解説書 `/home/tie303177/work/nablarch/nablarch-document/ja/development_tools/testing_framework/implementation/testdata_notation.rst:883` = 「データ行のセル数（Excel形式）または ``rows:`` の各要素の長さ（YAML形式）がフィールド数より少ない場合、不足したフィールドは ``""`` として補完される」。(3) スキーマ `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:377`（変更前）に「各配列の要素数が fields の件数と一致しない場合は NTF がエラーを出す」。3件とも指示書の行番号どおりで、ずれなし。変更後の 377 行は「各配列の要素数が fields の件数より少ない場合、不足したフィールドは `""` として補完される」。`grep -n "エラーを出す" src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` は record_fragment.rows に該当なし／【step M】再確認: `grep -n "エラーを出す" src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` → 該当なし（ファイル全体で 0 件） | | |
| 「多い場合」に関する記述を新たに追加していない | OK | `git diff 5fb7720` の追加2行に「多い」「無視」「余り」の語はない。追加したのは「より少ない場合」の1文のみ／【step M】`git diff -U0 <schema> \| grep "^+" \| grep -E "多い\|無視\|余り"` → 該当なし。step M の追加3行はいずれも「多い場合」に言及しない | | |
| `description` 以外（`type` / `items` の構造・`pattern` / `required` 等）を変更していない | OK | `git diff 5fb7720 --stat` = `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json \| 4 ++--`（1 file changed, 2 insertions, 2 deletions）。変更行は `$defs.record_fragment.properties.rows.description`（377行）と同 `items.description`（386行）の2箇所のみ／【step M】`git diff --stat` = `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json \| 6 +++---`（1 file changed, 3 insertions(+), 3 deletions(-)）。追加・削除行はすべて `"description":` で始まる行のみ（361 / 377 / 386）。`type` / `items` の構造 / `pattern` / `required` / `minItems` は無変更 | | |
| JSON として妥当 | OK | `python3 -c "import json; json.load(open('src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json', encoding='utf-8')); print('JSON OK')"` → `JSON OK`／【step M】同コマンド再実行 → `JSON OK` | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o clean test` → `[INFO] Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`（ベースライン 187 と一致）／【step M】`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 LANG=ja_JP.UTF-8 TZ=Asia/Tokyo mvn -o clean test` → `[INFO] Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`（ベースライン 187 と一致） | | |
| （Step F）`rows:` の要素数が `fields` より少ない YAML がスキーマ検証で落ちない | OK | 自分専用の複製 `/tmp/claude-1000/-home-tie303177-work-nablarch-nablarch-testing-yaml/b54f3aac-63f7-4080-b2eb-b320cfb720a7/scratchpad/impl18/shortRows.yaml` に fields 3件・rows `["AAA","BBB","CCC"]` / `["AAA"]` / `[]` を記述し、`YamlLoader.load(base, "shortRows")` を実行 → 例外なし（標準出力 `SCHEMA_VALIDATION: PASSED (no exception)`）。実行は `java -cp target/classes:target/test-classes:<dependency:build-classpath の出力> Verify <dir>` | | |
| （Method）「少ない場合は `""` 補完」が実挙動と一致する | OK | 同 Verify プログラムで `YamlFileBuilder#buildDataFileList` → `DataFile#toDataRecords()` を実行した結果: `RECORD: {FIELD2=BBB, FIELD3=CCC, FIELD1=AAA}` / `RECORD: {FIELD2=, FIELD3=, FIELD1=AAA}` / `RECORD: {FIELD2=, FIELD3=, FIELD1=}`。不足フィールドが `""` になることを実測 | | |
| （Method）YAML 経路が補完コードに到達する | OK | `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java:240` = `fragment.addValue(rowValues);`（223行目 `for (Object rowObj : getList(record, FIELD_ROWS))` の中）。`fragment` は `DataFile#getNewFragment()` が返す `DataFileFragment` で、`addValue` は `DataFileFragment.java:102` 定義、107 行目が上記の `""` 補完 | | |
| （step M-1）`$defs.record_fragment` 親 `description`（361行）: 第3文を削除 | OK | 変更前 = `` "レコード種別1ブロック。1つのレコードレイアウト（フィールド定義 + データ行）を表す。\n【ファイル系の rows は配列の配列】テーブル系（table_data / list_map_data）の rows はオブジェクト配列である点に注意。\nrows の各配列は fields と完全に同じ順序・同じ件数で値を並べること（NTF パーサが列順で対応付ける）" ``／変更後 = `` "レコード種別1ブロック。1つのレコードレイアウト（フィールド定義 + データ行）を表す。\n【ファイル系の rows は配列の配列】テーブル系（table_data / list_map_data）の rows はオブジェクト配列である点に注意。" ``。直前の `\n` ごと第3文のみを削除し、言い換え文は置いていない。第1文・第2文は無変更（`git diff` で確認）。失われた真の情報がないことの確認: 削除文のうち「同じ順序／列順で対応付ける」は `:377` 第1文「各要素は fields と同順の値配列」・第2文「NTF は fields の順序でフィールドと値を対応付ける」が同内容をより正確に述べている。残る「同じ件数で値を並べること」は実装（`DataFileFragment.java:106-107` = `for (int i = 0; i < names.size(); i++)` / `String value = i < line.size() ? line.get(i) : "";`）および解説書（`testdata_notation.rst:883`）と食い違う記述であり、真の情報ではない | | |
| （step M-2）`properties.rows.description`（377行）: 第3文の曖昧さを修正 | OK | 変更前の第3文 = `` 各配列の要素数が fields の件数より少ない場合、不足したフィールドは `""` として補完される。 ``／変更後の第3文 = `` 各配列の要素数が fields の件数より少ない場合、値を指定しなかったフィールドには `""` が設定される。 ``。第1文・第2文・第4文（`0件も有効（setup_files では…）`）は無変更（`git diff` の 1 行差分で確認）。「補完」は同スキーマ `:108` で「カラム型ごとのデフォルト値を入れる」意味に使われているため `rows` の文脈から除去した | | |
| （step M-3）`properties.rows.items.description`（386行）: 重複する一文を削除 | OK | 変更前 = `` "フィールド値のリスト。fields の順序で先頭から対応付けられる。数値・真偽値も文字列（クォート付き）で記述すること" ``／変更後 = `` "フィールド値のリスト。数値・真偽値も文字列（クォート付き）で記述すること" ``。削除は「fields の順序で先頭から対応付けられる。」の一文のみ。「フィールド値のリスト。」「数値・真偽値も…」は保持。失われた真の情報がないことの確認: 削除文は `:377` 第2文「NTF は fields の順序でフィールドと値を対応付ける。」と同内容（能動・主語明示でより明確）であり、items 側に固有の情報はない | | |

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective (checks the right thing, not just "passed") | NG | 完了条件5件はすべて OK。ただし総評 NG。理由: 同じ `$defs.record_fragment` の**親 description**（`ntf-testdata-yaml-schema.json:361`）に「rows の各配列は fields と完全に同じ順序・**同じ件数**で値を並べること」が残り、直した子（`:377`「少ない場合は `""` 補完」）と自己矛盾する。完了条件が `rows.description` にスコープを切っているため文言上は合格だが、タスクの目的（実装と食い違う description を直す）は未達。あわせて「description はスキーマ検証に一切効かないため、この変更の正しさを固定する回帰テストは本リポジトリに存在しない」（`YamlFileBuilderTest.java` の `rows` 使用箇所 `:269`/`:311`/`:740`/`:914-921` はいずれも過不足なしのケースのみ）と指摘 |

### QA — round 2（step N・step M 適用後の再実行）

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective | NG | 完了条件5件はすべて OK（`grep -c "エラーを出す"` → 0／`$defs.record_fragment` に「多い」「無視」「切り捨て」「余」なし／`description`・`$comment` を除いた木で `5fb7720` 版と HEAD 版が完全一致／`JSON OK`／`Tests run: 187, Failures: 0, Errors: 0, Skipped: 0`）。総評 NG の理由は2件。**F2**: 変更した同じ文に新しい誤読余地が生じた — 直前文が「各配列の要素数」を主語にしたため「0件も有効」が「要素数0の配列」と読める。そう読むと `rows: [[]]` が空ファイルになるが、実挙動は全フィールド `""` のレコード1件（`rst:883`）で正反対。変更前は「一致しない場合はエラー」だったためこの誤読は成立しなかった。**F1**: `description` はスキーマ検証に効かず `mvn test` はこの変更を検証していない。同リポジトリに先例 `YamlFileBuilderTest.java:526-542`（#13 で追加、「description が述べる挙動を実経路で実測して固定する」）がありながら踏襲していない。依存が `6-NEXT-SNAPSHOT` のため上流変更で無検知の再乖離が起きうる。**F3**（低）: `:386` から順序記述が消え、IDE 補完はカーソル位置ノードの description を出すため値行編集中に順序情報が届かない（※IDE 挙動は coordinator 未確認）。**F4**（低）: 「値を指定しなかったフィールド」は実装が位置対応のため末尾限定。**F5**: 完了条件5件はすべて形式検査で、「新しい文が実挙動と一致しているか」を問う条件が無い |

## Expert Reviews (axes the task needs)

### Craft Expert (writing)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | NG | (NG-1) 親 `:361` と子 `:377` の矛盾を本コミットが作り込んだ。修正前は親子とも「同順・同件数」で一致していた。しかも `:361` は「〜すること」（規範）、`:377` は「〜される」（挙動）で、規則なのかサポートされた書き方なのか判別できない。(NG-4) `:377`「不足したフィールドは `""` として補完される」は**値**が不足しているのにフィールド定義の不足とも読める。さらに「補完」は `:108`（`table_data.rows`）で「カラム型ごとのデフォルト値を入れる」意味で使われており、同一スキーマ内で二義。代替文「値を指定しなかったフィールドには `""` が設定される」 |
| Consistency with existing style | NG | (NG-2/NG-3) `:386` は書き換えの結果、`:377` 第2文と同内容になり新情報がゼロ。加えて受動・動作主省略で、隣接する「NTF は〜対応付ける」（能動・主語明示）と声が反転。代替は言い換えではなく第1文の削除。／OK 判定: バッククォート用法（`:108` と一致）・「〜すること」の使い分け・全角半角・「NTF は」の先例（`:136`）はいずれも既存文体と整合 |

### Craft Expert (writing) — round 2（step N・step M 適用後の再実行）

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | NG | **(1a) 規範が消え挙動だけが残った（重）** — `#18` で `:377` から「同件数」を、`:361` から第3文を落とした結果、`$defs.record_fragment` に順序・件数の規範が1文も残らない。解説書は規範を維持している（`testdata_notation.rst:1143`「``rows:`` の各行は配列形式で、``fields:`` と同じ順序・同じ件数で値を並べる」／`:1300`「``fields:`` と同じ順序・同じ件数の値だけを並べる」）。寛容な挙動のみを書いた結果「末尾は省いてよい」と読める。**(1b) 「値を指定しなかったフィールド」は誤読を招く（重）** — 実装は位置対応（`DataFileFragment.java:107`）で省けるのは末尾のみ。親 description が直前で「テーブル系の rows はオブジェクト配列」と対比を張っており、テーブル系（`:108`）はキー単位で任意カラムを省略できるため、任意フィールドを飛ばせると誤読しうる。解説書 `rst:787` は「記述しなかった分のカラムには空文字が設定されたものとして扱われる」。**(1c)** 「各要素は fields と同順の値配列」と「NTF は fields の順序で…対応付ける」は同一事実の二重記述。**(1d)** 「NTF は…対応付ける」（能動・主語明示）の直後が「`""` が設定される」（主語省略の受動）で主体が消える。`:108` は「NTF がデフォルト値 `"0"` を補完して INSERT する」と主体を明示。**(1e)** 「同順」はスキーマ内1件（この行のみ）・解説書0件で、解説書は「同じ順序」2件 |
| Consistency with existing style | NG | **(2a) 末尾句点（重・本差分が持ち込んだ）** — schema 内 description 64件のうち `。` で終わるのは `$defs/record_fragment`（`:361`）ただ1件。差分前は `…列順で対応付ける）` で終わっており、`）`22件・`る`16件ほか、他はいずれも文末句点を打たない。**(2b) 「必ず」の欠落（軽）** — `:386` は「数値・真偽値も文字列（クォート付き）で記述すること」だが、兄弟の `:108` `:136` はいずれも「数値・真偽値も**必ず**文字列（クォート付き）で記述すること」。**(2c)** 1文内で配列側を「要素数」、fields 側を「件数」と使い分けている（軽） |
| 親・子・孫の情報配分 | OK | 親＝ブロック定義＋形状の対比、子＝順序/件数/0件、孫＝値の記法という分担は妥当で、重複削除の方向も正しい。欠落は (1a) の「規範を担う階層が無くなった」点のみ |

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

## Triage（coordinator）— round 2

| Finding | 出所 | 判定 | 理由（coordinator が一次情報で確認した事実を含む） |
|---|---|---|---|
| `:361` 末尾の `。` が文体違反 | Craft 2a | **Valid → 修正する** | 確認済み: schema 内 description 全64件のうち `。` 終わりは `:361` の1件のみ（`json.load` して全 description の末尾文字を集計。`）`22 / `る`16 / `止`4 ほか）。差分前は `）` 終わりで、本差分が持ち込んだ |
| `:377`「値を指定しなかったフィールド」は末尾限定 | Craft 1b・QA F4 | **Valid → 修正する** | 確認済み: `DataFileFragment.java:107` = `String value = i < line.size() ? line.get(i) : "";` の位置対応。出典は当初 `rst:787` を引いていたが、**`rst:787` はテーブルデータ（Excel）節**（直前 `:774`「ヘッダ行（2行目）」・`:771`「日付型カラムの空文字」）であり `record_fragment` はファイル系・メッセージ系のブロックなので誤り。**`rst:883` に差し替えた**（2026-08-24 ユーザー指摘）。指摘内容（末尾限定）自体は `DataFileFragment.java:107` で確認済みで妥当。ユーザーは `:377` の曖昧さ修正を承認済み |
| `:377`「0件も有効」の係り先が曖昧になった | QA F2 | **Valid → 修正する** | 確認済み: `rst:883` は「YAML形式では ``rows:`` に空配列 ``[]`` を記載した行を書けば、全フィールドが ``""`` のレコードとして保持される」。「要素数0の配列」と読むと実挙動と正反対になる。本差分が直前文を書き換えたことで生じた曖昧さ |
| `:386` に「必ず」が無い | Craft 2b | **Valid → 修正する** | 確認済み: `grep -n 数値・真偽値` → `:108` `:136` は「必ず」あり、`:386` のみ無し。本差分が編集した文字列そのもの |
| 順序・件数の**規範**がスキーマから消えた | Craft 1a | **Escalation → ユーザー判断待ち（step O・判断A）** | 確認済み: `rst:1143`「``rows:`` の各行は配列形式で、``fields:`` と同じ順序・同じ件数で値を並べる」／`rst:1300`「``fields:`` と同じ順序・同じ件数の値だけを並べる」。規範を書き戻すのは、ユーザーが `:361` 削除時に述べた「`:377` が件数の扱いまで含めて正確に述べている」という前提の再検討にあたるため、方向の変更はユーザーの判断 |
| `:386` から順序記述が消えたのは情報の純減 | QA F3 | **Escalation → ユーザー判断待ち（step O・判断B）** | `:386` の一文削除はユーザーが明示的に指示した内容。反証は「IDE 補完はカーソル位置ノードの description を出す」だが**この IDE 挙動は coordinator 未確認**。coordinator 推奨は現状維持 |
| description が述べる挙動を固定するテストが無い | QA F1 | **Escalation → ユーザー判断待ち（step O・判断C）** | 確認済み: 先例 `YamlFileBuilderTest.java:526-542`（#13 で追加）が「description が述べている挙動を、YAML ファイルを経由した実際の経路で実測して固定する」と明記。テスト追加は `#18` の完了条件（description のみ）の外でスコープ拡大にあたる。coordinator 推奨は新タスク化 |
| 冗長（1c）／主語の一貫性（1d）／「同順」の語（1e）／「要素数」と「件数」の混在（2c） | Craft | **Invalid（見送り）** | いずれも軽微な文体の指摘で、事実誤りでも完了条件違反でもない。「同順」は本差分以前から存在（`5fb7720` 版の `:377` にも「同順・同件数」）で本差分が持ち込んだものではない。報告書候補として記録 |
| 完了条件が実挙動との一致を問うていない | QA F5 | **Invalid（指摘は妥当だが本差分の欠陥ではない）** | 完了条件セットの質の問題。目的の達成自体は Method と self-check で実経路を走らせて実測済み（`YamlFileBuilder#buildDataFileList` → `DataFile#toDataRecords()` で不足フィールドが `""` になることを確認）。報告書候補として記録 |

## step P 適用（round 3・2026-08-24 ユーザー判断を反映）

**判断A の前提測定**: 固定長ファイルでも `rows` の要素数不足は `""` 補完される。

- 方法: 一時テストクラスから `YamlFileBuilder#buildDataFileList` → `DataFile#toDataRecords()` を実行（実行後に一時ファイルは削除済み）。フィクスチャは `type: fixed`（`FIELD1`/`FIELD2` 各 `半角` 長さ5）と `type: variable`（`NAME`/`VALUE`）で、いずれも `rows` に `["AAAAA"]`（要素1件）と `[]`（空配列）を並べた
- 結果: `FixedLengthFile` → `row[0]={FIELD2=, FIELD1=AAAAA}` / `row[1]={FIELD2=, FIELD1=}`、`VariableLengthFile` → `row[0]={VALUE=, NAME=tanaka}` / `row[1]={VALUE=, NAME=}`
- 裏付け: `addValue` の定義は `DataFileFragment.java:102-115` の1箇所のみ。`grep -rn addValue src/main/java/nablarch/test/core/file/` は `:102`（`addValue`）と `:169`（`addValueWithId`）のみを返し、`VariableLengthFileFragment` / `FixedLengthFileFragment` に override は無い
- 結論: `:377` 第2文に「可変長ファイルでは」の限定は付けない

**適用した4点**

| 対象 | 変更後 | 出典・根拠 |
|---|---|---|
| `:361` | 末尾の `。` を削除 | schema 内 description 全64件を再集計し `。` 終わりが 0 件になったことを確認（`json.load` して末尾文字を集計） |
| `:377` | 判断A の3文構成へ（規範／挙動／記法）。第2文は「不足した**末尾**のフィールド」 | 規範の出典 `rst:1143`・`rst:1300`。末尾限定は `DataFileFragment.java:107` = `i < line.size() ? line.get(i) : ""`。`""` 補完と `[]` 記法は `rst:883` |
| `:377` | 「0件も有効」→「`rows` が0件でも有効」。**括弧内は削除** | 括弧内「setup_files では空ファイル、expected_files では出力なしの期待値として使用」は未実測。`rst:881`「0バイトの空ファイルは、レコード定義を持たないファイルデータブロックとして表現する」・`rst:1146`「0バイトの空ファイルを表現するには、`records:` に空配列 `[]` を記載する」と表現手段が違う。`#15`/`#16` が実測した `rows: []` はテーブルデータ経路のみ（`checks/task-16.md:26` `:104`）でファイル系は未測定。測っていない挙動は書かない |
| `:386` | 「必ず」を追加 | `:108` `:136` と揃える |

**確認**: `python3 -c "import json; json.load(...)"` → JSON OK。`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS。

**⑥ nablarch-document への報告書候補（追加）**

| 指摘 | 根拠 |
|---|---|
| `rst:883` が `""` 補完を「可変長ファイルでは、…」の節に置いているが、実装では固定長ファイルでも同じく補完される（記述範囲が実装より狭い） | 上記実測（`FixedLengthFile` で `row[1]={FIELD2=, FIELD1=}`）と `DataFileFragment.java:102-115`（override 無し）。なお `:883` の同一文の後半は「（固定長ファイルの場合はスペースパディングされた定長レコードとして書き出される）」と固定長にも触れており、節の切り方だけが狭い |

## QA / Craft(writing) — round 3（step Q・step P 適用後の再々実行）

**QA: pass**（完了条件5件すべて OK）。ただし QA チェックリスト（検証が目的に対して意味を持っているか）は **NG** — `mvn -o clean test` はこの差分を1バイトも検証していない（`description` は JSON Schema のアノテーションで検証に効かない）。この穴は `#22` が引き取る。QA は新記述の事実主張3件を自前フィクスチャで実行して真であることを確認した。

**Craft(writing): fail**（完了条件5件は OK。落ちたのは「明快さ」「既存文体との一貫性」の2観点）。

## Triage（coordinator）— round 3

coordinator が一次情報で確認した事実のみを根拠に判定した。実測はいずれも一時テストクラスを作って実行し、実行後に削除している（`git status` clean を確認済み）。

| Finding | 出所 | 判定 | 理由（coordinator の一次確認） |
|---|---|---|---|
| 「要素数が」の主語欠落（`rows` の要素数とも読める） | Craft F2 | **Valid → 修正済** | 変更前の `:377` は「**各配列の**要素数が」と主語を明示していた（`git show 5fb7720` の当該行）。末尾に「rows が0件でも有効」が続くため誤読の余地が実在する。「各配列の」を補った。意味は変えていない |
| `[]` の意味が多義（`rows: []` と要素としての `[]`）／規範と記法の推奨が同一文中で衝突。接続詞（「原則として…ただし」）を足すべき | Craft F1・QA F1 | **却下（現状維持）** | ユーザーは判断A でこの緊張を明示的に weigh したうえで3文構成を指定し、「第2文だけだと『末尾は省いてよい』という緩和に読める」ため第3文を必須とした。第1文に「原則として」を足すと規範が弱まり、判断A が復活させた目的と逆になる。解説書も規範（`rst:1143` `:1300`）を無条件で書き、記法（`rst:883`）を別の場所に置いている。ユーザーの明示指定を coordinator の判断で変えない |
| 「位置対応させる」は本ファイルに前例のない造語／使役が不自然 | Craft F4 | **却下（現状維持）** | 前例が無いのは事実（`対応` を含む description 6件を全走査。既存は「対応付け」「対応する」）。ただし「位置対応」は**位置で対応付ける**という情報を持ち、代替案の「対応付ける」ではその情報が落ちる。この語はユーザーが判断A で逐語指定した文言 |
| `[]` のバッククォートが本ファイルの慣行から逸脱 | Craft F5 | **却下（現状維持）** | 事実は確認済み: `[]` を含む description は4件で、`setup_tables` / `table_data.rows` / `file_data.records` の3件は裸、バッククォート付きは `record_fragment.rows` のみ。ただし `` `""` `` はバッククォート付きの前例が `table_data.rows` にあり、同一 description 内で `` `""` `` と裸の `[]` を混在させる方が不統一になる。ユーザーが判断A で逐語指定した表記でもある |
| 第3文「これを利用し」がメッセージング系（`$ref` 4箇所のうち `message_data` 等）でも成立するか未確認 | Craft F3・F7 | **Invalid（実測で反証）** | メッセージ系経路で実測: `YamlFileBuilder.buildFragmentsForMessage`（`fields` = `P1`/`P2` 各 `半角` 長さ5、`rows` = `[["A"], []]`）→ `row[0] P1=[A]{String} P2=[]{String}` / `row[1] P1=[]{String} P2=[]{String}`。ファイル系と同一挙動。`addValue` が `DataFileFragment.java:102-115` の1箇所のみで共有されているという構造どおり |
| 要素**過多**の行が無言で切り捨てられる旨が書かれていない | QA F2 | **Invalid（完了条件のスコープ境界）** | 完了条件が「『多い場合』に関する記述を新たに追加していない」と明示。round 1 でユーザー承認済み。報告書候補として継続記録 |
| 固定長での出力形（スペースパディング）が落ちている | Craft F6 | **Invalid（かつ前提が不正確）** | 下記「新事実」のとおり `数値` 型はスペースではなくゼロ埋めになる。`rst:883` の括弧書き自体が `数値` について不正確なので、これをスキーマへ写すと誤りを持ち込む。報告書候補へ回す |
| `:386` が `null` の扱いに触れていない | Craft F8・QA F5 | **Invalid（既存の乖離・本差分の対象外）** | `items.type` は本差分で無変更。`table_data.rows`（`:108`）には `null` の記述があり `record_fragment` には無いという乖離は差分前から存在する。報告書候補へ回す |
| 削除した括弧「（setup_files では空ファイル…）」は実は真だった | QA F4 | **却下（現状維持）＋新事実として記録** | coordinator も実測で確認（下記「新事実」）。ただしユーザーは「測っていない挙動を書かないこと」として削除を指示しており、いま復活させると `expected_files では出力なしの期待値として使用` の側が未実測のまま片側だけ戻ることになる。`#22` で両方を固定してから扱う |

## 新事実（round 3 で coordinator が実測。`#22` の入力および ⑥ 報告書候補）

方法: 一時テストクラスから `YamlFileBuilder#buildDataFileList` → `DataFile#toDataRecords()` / `DataFile#write()` を実行し、`od -c` で出力バイトを確認（実行後に一時ファイル・出力ファイルとも削除済み）。

1. **`数値` 型の不足フィールドはスペースではなくゼロ埋めで書き出される。** 固定長（`F1` `半角` 5 / `F2` `半角` 5 / `F3` `数値` 3）で `rows` に `["AAAAA"]` と `[]` を並べた結果、`toDataRecords()` は `F1=[AAAAA]{String} F2=[]{String} F3=[0]{BigDecimal}` / `F1=[]{String} F2=[]{String} F3=[0]{BigDecimal}`、出力バイトは `od -c` で `A A A A A ␣ ␣ ␣ ␣ ␣ 0 0 0 \n` + `␣×10 0 0 0 \n`。断片層では `""` だが、`数値` 型では出力が `000` になる
   - `:377` の「`""` として扱われる」は断片層の記述として誤りではない（`rst:883` と同じ粒度）。型ごとの出力形は `:108`（`expected_complete_tables` のデフォルト値）の役割と重なるため `:377` には書かない
   - **⑥ nablarch-document 報告書候補**: `rst:883` の括弧書き「（固定長ファイルの場合はスペースパディングされた定長レコードとして書き出される）」は `数値` 型について不正確
2. **`setup_files` + `rows: []` は 0 バイトファイルを生成する。** `write()` 後のファイルサイズ 0 を実測（`stat -c %s` → `0`）。`records: []`（`rst:1146`）とは別経路で同じ結果になる
3. **`半角` 型の不足フィールドは `null` ではなく空文字。** `toDataRecords()` の値は `[]{String}`（`String` の空文字）であり `null` ではない

## ⑥ nablarch-document への報告書候補（round 3 時点の一覧）

| 指摘 | 根拠 |
|---|---|
| `rst:883` が `""` 補完を「可変長ファイルでは、…」の節に置いているが、実装では固定長でも同じく補完される（節の切り方が実装より狭い） | step P の実測（`FixedLengthFile` で `row[1]={FIELD2=, FIELD1=}`）と `DataFileFragment.java:102-115`（override 無し）。`:883` 同一文の後半は固定長にも触れている |
| `rst:883` の括弧書き「スペースパディングされた定長レコード」は `数値` 型について不正確（実際はゼロ埋め） | 上記「新事実」1（`od -c` で `0 0 0`） |
| 要素**過多**の行は例外も警告もなく超過分が捨てられる（解説書に記述なし） | `DataFileFragment.java:105-113` のループが `names.size()` 回で、`line` の余りを読まない |
| 完了条件が実挙動との一致を問うていない（`mvn test` は `description` を検証しない） | QA F5（round 2）・QA チェックリスト NG（round 3）。`#22` が引き取り |
| Craft の軽微指摘（冗長・主語・「同順」・「要素数/件数」・`[]` のバッククォート・「位置対応」の造語） | round 2・round 3 の Craft 指摘のうち却下したもの |

## Overall Verdict

- Self-check: OK（step P 適用後。完了条件5件すべて OK）
- QA: OK（round 3 = pass。完了条件5件 OK。QA チェックリストのみ NG で、これは `#18` の完了条件の外側であり `#22` が引き取る）
- Design expert: N/A（構造・方式を変えないため）
- Craft expert: NG（round 3 = fail。ただし Valid は F2 の1件のみで修正済。他は却下または Invalid — 理由は round 3 triage）
- Verification expert: OK（round 1 で実施。事実主張は round 3 で coordinator が再実測し、いずれも真であることを確認）
- Ready to check off: Yes — 完了条件5件すべて OK（JSON 妥当、`mvn -o clean test` → `Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS）。Valid 判定の指摘はすべて修正済み。残る指摘は Invalid・却下・報告書候補・`#22` へ帰属
