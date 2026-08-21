Rn version: 0.8.0

# Goal

nablarch-testing の YAML 読み込み機構（src/main 12件）とその単体テスト（src/test 52件）を
nablarch-testing-yaml リポジトリへ切り出し、`mvn test` 全 PASS の状態にする。
実装は一切変更せず、ファイルの物理コピー（パス変更）と package/import の機械的調整、
および pom 設定のみを行う。
移送後、解説書・JSON Schema との食い違いが見つかった箇所は、ユーザー確認を経たタスク（#5〜#13）として是正する。

# Acceptance criteria

- `mvn test` 全テスト PASS（yaml リポジトリ単体で緑）
- 移送3タスク（#1〜#3）の完了時点で、全移動ファイルが `worktree-agent-a79308e7e5862d004`（`d8ba387`）と package/import を除き完全一致していた（根拠: checks/task-02.md・checks/task-03.md）
- #4 以降の実装差分は、すべて steering の承認済みタスクに帰属し、タスク外の差分が無い（根拠: git log と git diff d8ba387..HEAD）
- 本体（nablarch-testing）に一切書き込みをしていない
- push 済み

# Assumptions

- nablarch-testing と nablarch-testing-yaml は同じ親ディレクトリに clone 済み（`../nablarch-testing`）
- 本体は `convert-testdata-excel-to-text` ブランチのまま（書き込み禁止・参照のみ）
- 本体テストクラスを extends する `*YamlTest` や `YamlModeTestBase` はこのリポジトリに含めない（integration 行き）
- コンパイル・テストが通らず実装変更が要ると判断したら止めてユーザーに確認する

# Rules

- 1 task = 1 commit
- 実装の変更は一切しない（物理コピー・package/import 機械的調整・pom 設定のみ許可）
- 本体（nablarch-testing）には書き込まない
- 変更が必要と判断したら **止めてユーザーに確認**（テスト PASS のために実装をいじるのは禁止）
- mvn コマンド（compile / test / install 全て）は `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn ...` で実行する（Nablarch v6 は Java 17 ターゲット。既定 java は 21 なので指定必須。親 POM は `--release` でなく `source`/`target` 指定のため 21 でビルドすると 21 専用 API が通ってしまう）
- mvn は必ず `clean` を付ける（`jacoco:restore-instrumented-classes` は prepare-package で走るため、`clean` なしの `mvn test` / `mvn install` は instrument 済みクラスが `target/classes` に残り「Cannot process instrumented class」で失敗する）
- javadoc 生成時に「モジュールが使用されていますが…java 8 api」の WARNING 1個が出るが、`maven-javadoc-plugin 2.10.4`（親 POM 固定）と Java 9+ モジュールシステムの非互換によるもので許容済み

# Tasks

### ~~#1: pom.xml の作成~~

**Purpose**: yaml リポジトリのビルドが通るよう pom.xml を作成する

**Prerequisites**: none

**Steps**:

- [x] 本体 pom.xml を参照し、親 POM・groupId・依存（nablarch-testing compile、snakeyaml-engine、JUnit）を設定した pom.xml を作成する
- [x] `mvn validate`（またはコンパイルのみ）でパース・設定エラーがないことを確認する
- [x] self-check (OK/NG per completion criterion, record in checks/task-01.md)
- [x] QA expert review (subagent)
- [x] user review

**Completion criteria**:

- pom.xml が作成されており `mvn validate` がエラーなく通る
- 親 POM に `com.nablarch:nablarch-parent` を指定している
- compile 依存に `com.nablarch.framework:nablarch-testing` が含まれている
- compile 依存に `org.snakeyaml:snakeyaml-engine:3.0.1` が含まれている
- テスト依存に JUnit が含まれている

---

### ~~#2: src/main ファイルのコピー配置（12件）~~

**Purpose**: 本体ブランチの src/main 対象 12 件を yaml リポジトリの同一パッケージパスへコピーし、package/import を機械的に調整する

**Prerequisites**: #1

**Steps**:

- [x] A. src/main（java 11件 + schema 1件）を対応パスへコピー
  - `src/main/java/nablarch/test/core/file/TestCoreFileAdapter.java`
  - `src/main/java/nablarch/test/core/reader/StubDbInfo.java`
  - `src/main/java/nablarch/test/core/reader/TestCoreReaderAdapter.java`
  - `src/main/java/nablarch/test/core/reader/YamlTestCoreAdapter.java`
  - `src/main/java/nablarch/test/core/reader/YamlTestDataParser.java`
  - `src/main/java/nablarch/test/core/reader/yaml/InterpreterResolver.java`
  - `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java`
  - `src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java`
  - `src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java`
  - `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java`
  - `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java`
  - `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json`
- [x] package/import の機械的調整（必要な場合のみ）
- [x] `mvn compile` でコンパイルエラーがないことを確認
- [x] 差分チェック: 全ファイルを本体現ブランチと diff し package/import 以外の差分がないことを確認
- [x] self-check (OK/NG per completion criterion, record in checks/task-02.md)
- [x] QA expert review (subagent)
- [x] language expert review (subagent)
- [x] software-engineering expert review (subagent)
- [x] user review

**Completion criteria**:

- 12 件すべてが `src/main` の対応パスに配置されている
- `mvn compile` がエラーなく通る
- 全ファイルが本体現ブランチと package/import を除き完全一致

---

### ~~#3: src/test ファイルのコピー配置（52件）と mvn test 全 PASS~~

**Purpose**: 単体テスト（java 9件）とテストデータ（43件）を yaml リポジトリへコピー配置し、`mvn test` 全 PASS を確認する

**Prerequisites**: #2

**Steps**:

- [x] B. src/test java（9件）を対応パスへコピー
  - `src/test/java/nablarch/test/core/file/TestCoreFileAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/TestCoreReaderAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlSectionTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java`
- [x] C. テストデータ（43件）を対応パスへコピー（テストクラスと同じ相対パスを維持）
  - `YamlTestCoreAdapterTest/*`（4件）
  - `YamlTestDataParserTest/*`（16件）
  - `YamlFileBuilderTest/*`（2件）
  - `YamlLoaderTest/*`（13件）
  - `YamlMessageBuilderTest/*`（3件）
  - `YamlTableDataBuilderTest/*`（5件）
- [x] `mvn test` 実行・全 PASS 確認（落ちたら配置/pom で解決。実装変更は禁止）
- [x] 差分チェック: テスト java 9件を本体現ブランチと diff し package/import 以外の差分がないことを確認
- [x] self-check (OK/NG per completion criterion, record in checks/task-03.md)
- [x] QA expert review (subagent)
- [x] language expert review (subagent)
- [x] software-engineering expert review (subagent)
- [x] user review

**Completion criteria**:

- 単体テスト java 9件・テストデータ 43件すべてが `src/test` の対応パスに配置されている
- `mvn test` 全テスト PASS
- テスト java 9件が本体現ブランチと package/import を除き完全一致

### ~~#4: converter 専用 Adapter 群の削除~~

**Purpose**: yaml リポジトリに混入していた converter 専用クラス（4件）とその随伴テスト（3件＋データ4件）を削除し、`mvn clean test` 全 PASS・`mvn install` 成功を確認する

**Prerequisites**: #3

**Steps**:

- [x] A. 以下のファイルを `git rm` で削除
  - `src/main/java/nablarch/test/core/file/TestCoreFileAdapter.java`
  - `src/main/java/nablarch/test/core/reader/YamlTestCoreAdapter.java`
  - `src/main/java/nablarch/test/core/reader/TestCoreReaderAdapter.java`
  - `src/main/java/nablarch/test/core/reader/StubDbInfo.java`
  - `src/test/java/nablarch/test/core/file/TestCoreFileAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/TestCoreReaderAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest/`（配下4件）
- [x] B. `mvn clean test` 実行・全 PASS 確認（落ちたら止めて報告）
- [x] C. 緑確認後 commit・push
- [x] D. `mvn install` 実行・成功確認
- [x] self-check (OK/NG per completion criterion, record in checks/task-04.md)
- [x] QA expert review (subagent)
- [x] user review

**Completion criteria**:

- 削除対象 4件（src/main）＋テスト3件＋データ4件がリポジトリから消えている
- `mvn clean test` 全テスト PASS
- `mvn install` BUILD SUCCESS

---

### ~~#5: カバレッジ未達箇所の対処（テスト追加 + コメント追加）~~

**Purpose**: JaCoCo レポートで判明した未カバー箇所を NTF 仕様視点で分類し、テスト追加・コメント追加で解消する

**Prerequisites**: #4

**Steps**:

- [x] A. `YamlMessageBuilderTest` に `buildSendSyncBodies` のテストを追加（converter の仕様を満たすことを確認）
  - group_id 一致時に FixedLengthFile リストが返ること
  - group_id 不一致時に空リストが返ること
  - `stripBrackets(null)` のブランチも自然にカバー
- [x] B. `InterpreterResolver` の `raw()` のテストを追加
  - `raw().resolve("any")` が空リストを返すこと
- [x] C. `YamlLoaderTest` に末尾 "/" 付き basePath のテストを追加
  - 末尾 "/" 付きパスでも正常にロードできること
- [x] D. `YamlFileBuilder.java` 182行付近に SnakeYAML Engine の仕様を説明するコメントを追加
- [x] E. `mvn clean test` 全 PASS 確認
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-05.md)
- [x] G. QA expert review (subagent)
- [x] H. language expert review (subagent)
- [x] I. software-engineering expert review (subagent)
- [x] J. user review

**Completion criteria**:

- `buildSendSyncBodies`（converter 仕様）がテストでカバーされている
- `InterpreterResolver.raw()` がテストでカバーされている
- `YamlLoader` の末尾 "/" 分岐がテストでカバーされている
- `YamlFileBuilder` の `instanceof` ガードにコメントが追加されている
- `mvn clean test` 全 PASS

---

### ~~#6: YamlLoader への JSON Schema バリデーション組み込み~~

**Purpose**: YAML パース時に `ntf-testdata-yaml-schema.json` でバリデーションをかけ、スキーマ違反を即座に検出できるようにする。スキーマと実装の整合性を CI で継続的に担保する。

**Prerequisites**: #5

**Steps**:

- [x] A. `pom.xml` に `com.networknt:json-schema-validator:3.0.5` を compile スコープで追加
- [x] B. `YamlSchemaValidationException` を新規作成
  - `IllegalStateException` のサブクラス
  - `filePath` と `List<Error>` を保持
  - `getMessage()` をオーバーライド: ライブラリの `Error.toString()` をそのまま使い、ファイルパス + 全エラーを改行で連結
- [x] C. `YamlLoader.load()` にバリデーションを追加
  - パース直後・キャッシュ格納前にバリデーション実行
  - `JSON_SCHEMA` はクラス変数としてシングルトンキャッシュ
  - 違反があれば `YamlSchemaValidationException` をスロー
- [x] D. `YamlLoaderTest` にバリデーション関連テストを追加（6パターン）
  - required漏れ・型違反・enum違反・深いネスト・複数同時・基本検出
- [x] E. `mvn clean test` 全 PASS 確認
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-06.md)
- [x] G. QA expert review (subagent)
- [x] H. language expert review (subagent)
- [x] I. software-engineering expert review (subagent)
- [x] J. user review

**Completion criteria**:

- `pom.xml` に `json-schema-validator:3.0.5` が追加されている
- `YamlSchemaValidationException` が存在し、`getMessage()` にファイルパスと全違反メッセージが含まれる
- `YamlLoader.load()` がスキーマ違反 YAML に対して `YamlSchemaValidationException` をスローする
- 正常な YAML は引き続き例外なく読み込まれる
- `mvn clean test` 全 PASS

---

### ~~#7: スキーマ横並びチェック（3軸）実施 + 不備2件修正~~

**Purpose**: スキーマと実装・仕様の整合を3軸で確認し、判明済み2件の不備を修正して `mvn clean test` 全 PASS・PR Ready for review へ進める

**Prerequisites**: #6

**Steps**:

- [x] A. スキーマ横並びチェックを3軸で実施（subagent）
  - 軸1（読み取り）: 実装（YamlLoader 等）が読むフィールド → スキーマに定義済みか
  - 軸2（書き込み）: converter（`YamlFormatWriter` 等）が書くフィールド → スキーマに定義済みか（`../nablarch-testing` の `convert-testdata-excel-to-text` ブランチを参照）
  - 軸3（required）: スキーマの required フィールド → NTF仕様で必須か（`ntf-impl-spec-list.md` と突き合わせ）
- [x] B. チェック結果を元に他に不備がないことを確認 → NTF仕様に基づきスキーマ再設計
  - 不備1（再設計）: `message_data`（messages専用: fw_header あり・group_id なし）と新 def `expected_request_message_data`（expected_request_*専用: fw_header なし・group_id あり）に分離
  - 不備2: `record_fragment.required` から `record_type` を外す（省略可能なフィールド）
  - 負例テスト3件追加（messages+group_id、expected_request_*+fw_header、group_id空文字）
- [x] C. `mvn clean test` 全 PASS 確認（159件）
- [x] D. commit・push（SHA: dd61c4e）
- [x] E. self-check (OK — checks/task-07.md 記録済み)
- [x] F. QA expert review (OK)
- [x] G. language expert review (OK)
- [x] H. software-engineering expert review (OK)
- [x] I. user review → PR #1 を Ready for review に変更

**Completion criteria**:

- スキーマ横並びチェック（3軸）が実施され、チェック結果が checks/task-07.md に記録されている
- `message_data` の properties に `group_id` が追加されている
- `record_fragment.required` に `record_type` が含まれていない
- 追加不備が見つかった場合はすべて修正されている（実装変更なし）
- `mvn clean test` 全テスト PASS

---

### ~~#8: スキーマ description に FK 制約の落とし穴2件を追記~~

**Purpose**: FK 制約のある環境で利用者がハマる NTF 挙動2点をスキーマ description に記載する

**Prerequisites**: #7

**Steps**:

- [x] A. `setup_tables` の description に落とし穴1を追記
  - 通常の INSERT でも対象テーブルは INSERT 前に全件 DELETE されること
  - FK の親テーブルを clear するなら子テーブルも `setup_tables` に列挙すること（NTF は子→親の順で削除する）
- [x] B. `table_data.rows` の description に落とし穴2を追記
  - FK 制約のある数値カラムを省略すると `"0"` が INSERT され、参照先に ID=0 の行が無ければ FK 違反になること
  - NULL 許容カラムを NULL にしたい場合は省略せず明示的に `null` を書くこと（省略≠NULL）
- [x] C. JSON として妥当か検証（`python3 -c "import json; json.load(open(...))"` 等）
- [x] D. `mvn clean test` 全 PASS 確認
- [x] E. commit・push
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-08.md)
- [x] G. QA expert review (subagent)
- [x] H. Craft expert review — writing (subagent)
- [x] I. Verification expert review — fact-check (subagent)

**Completion criteria**:

- `setup_tables` の description に「INSERT 前の全件 DELETE」と「FK 親テーブル clear 時に子テーブルも列挙すること（NTF は子→親の順で削除する）」が記載されている
- `table_data.rows` の description に「FK 制約のある数値カラムを省略すると `"0"` が INSERT され FK 違反になる可能性」と「NULL 許容カラムを NULL にしたければ省略せず `null` を明示すること」が記載されている
- description 以外（`type` / `enum` / `required` 等の検証ルール構造）は変更されていない
- JSON として妥当（`json.load` が通る）
- `mvn clean test` 全 PASS

---

### ~~#9: 不具合 #C — group_id 省略メッセージエントリが buildSendSyncList で取得できない~~

**Purpose**: `YamlMessageBuilder.buildSendSyncList` の `rawGroupId != null` 条件により、`group_id` を持たないエントリがグループIDなし（`""`）で取得できない不具合を修正する。

**Prerequisites**: #8

**Steps**:

- [x] A. RED: `YamlMessageBuilderTest` に `buildSendSyncMessageList_noGroupId` を追加し、失敗することを確認
  - テストデータは既存 `YamlMessageBuilderTest/` 配下に追記（新規ディレクトリ不要）
- [x] B. GREEN: `buildSendSyncList` を `YamlSection.groupMatches()` に置き換え、`groupMatches` を static import 追加
  - `stripBrackets()` が未使用になる場合は削除
  - `buildSendSyncBodies` に変換ツール専用旨の Javadoc を追記
- [x] C. `mvn test` 実行・全 PASS 確認
- [x] D. commit・push
- [x] E. self-check (OK/NG per completion criterion, record in checks/task-09.md)
- [x] F. QA expert review (subagent)
- [x] G. Craft expert review — coding (subagent)
- [x] H. Verification expert review — test (subagent)

**Completion criteria**:

- 追加した `buildSendSyncMessageList_noGroupId` テストが GREEN
- `YamlMessageBuilderTest` の既存テストが全て通っている
- `buildSendSyncList` から `FIELD_GROUP_ID` の直接比較が消えている
- `stripBrackets()` が未使用なら削除されている
- `buildSendSyncBodies` のロジックが変更されていない（Javadoc 追記のみ）

---

### ~~#10: 不具合 #F — 空の EXPECTED_TABLE が常に PASS する~~（**#11 で差し戻し済み・不具合 #F は未解決**）

**Purpose**: `expected_tables` に `rows: []` のエントリを書いたとき DB に行があってもアサーションが PASS する偽陰性を修正する。

**Prerequisites**: #9

**Steps**:

- [x] A. RED①: 偽陰性テスト — DB に 1 件以上あり `rows: []` で assertTableEquals が FAIL すること
- [x] B. RED②: `EXPECTED_COMPLETE_TABLE` NPE テスト — `rows: []` で getExpectedTableData が例外なく返りカラムが DB 全列
- [x] C. RED③: SETUP_TABLE 退行防止テスト — `rows: []` で setUpDb 後にテーブルが空になること
- [x] D. GREEN: `buildTableData()` をデフォルトコンストラクタ + setter 方式に変更、`fillDefaults && !dataColumns.isEmpty()` ガード追加
- [x] E. 既存テスト `buildTableDataList_allEmptyRowsReturnsTableDataWithZeroColumns` の期待値を「dbInfo 全列が返ること」に更新し、Javadoc から `(JE-6)` と解説書 10.5 参照を削除
- [x] F. `mvn test` 実行・全 PASS 確認
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-10.md)
- [x] I. QA expert review (subagent)
- [x] J. Craft expert review — coding (subagent)
- [x] K. Verification expert review — test (subagent)

**Completion criteria**:

- 追加した RED テスト①②③が全て GREEN
- `buildTableDataList_allEmptyRowsReturnsTableDataWithZeroColumns` の期待値が更新され GREEN
- `YamlTableDataBuilderTest` の他の既存テストが全て通っている
- YAML スキーマが変更されていない
- `nablarch-testing` 本体および converter が変更されていない

---

### ~~#11: #10 の差し戻しと本体課題としての報告~~

**Purpose**: #10 の修正が converter（DB を持たない読み込み経路）を壊したため差し戻し、事象を NTF 本体の課題としてチームへ報告する。

**Prerequisites**: #10

**Steps**:

- [x] A. `buildTableData()` を差し戻し（長さ 0 の列名で `TableData` を生成／`fillDefaults` のガードも撤去）
- [x] B. FAIL する 4 件を `@Ignore` + FIXME で保留（削除しない）
  - `YamlTestDataParserTest#emptyExpectedTable_failsWhenDbHasRows`
  - `YamlTableDataBuilderTest#buildTableDataList_emptyRowsExcluded`
  - `YamlTableDataBuilderTest#buildTableDataList_allEmptyRowsReturnsTableDataWithAllDbColumns`
  - `YamlTableDataBuilderTest#buildTableDataList_emptyExpectedTableReturnsTableDataWithAllDbColumns`
- [x] C. `mvn clean test` 全 PASS 確認（164 件 / Skipped 4）
- [x] D. commit・push（SHA: 190cc9a）
- [x] E. `mvn clean install` 実行・成功確認（`.m2` 更新）
- [x] F. converter で回帰確認（428 件 PASS・BUILD SUCCESS）
- [x] G. 事象の裏取り（実測）
  - Excel 経路でもカラム名 0 件が発生することを実 xlsx で確認（識別子行がシート末尾／空行をヘッダ位置に置く／ヘッダがマーカーのみ）
  - 準備データ（`setup_tables`）はカラム名 0 件でも 0 件初期化が成立することを実 DB で確認
  - 解説書（`nablarch-document` main / `ntf-yaml-support`）にカラム名必須の記載が無いこと、記載例はすべてカラム名の行を書いていることを確認
- [x] H. チーム報告書を作成（`.rn/ntf-yaml/report-empty-expected-table.md`。本体 main の該当行 URL 付き）

**Completion criteria**:

- `YamlTableDataBuilder` が #10 以前の形（長さ 0 の列名）に戻っている
- FAIL する 4 件が削除されず `@Ignore` + FIXME で残っている
- `mvn clean test` が BUILD SUCCESS（Skipped 4）
- converter が `Tests run: 428, Failures: 0, Errors: 0` / BUILD SUCCESS
- 報告書に本体 `main` ブランチの該当行 URL が記載されている

---

### #12: YML-03 — `record_type: FW_HEADER` によるレコード読み飛ばしを廃止する

**Purpose**: メッセージ系経路が `record_type` の値 `FW_HEADER` でレコードを読み飛ばす旧版前提の処理を廃止し、解説書 `testdata_notation.rst:1302`（「`record_type` に特別な予約値はない」／FW 制御ヘッダは `fw_header:` マップで記述する）の仕様に一致させる。

**Prerequisites**: #11

**Steps**:

- [x] A. RED: `record_type: FW_HEADER` を含むレコードが読み飛ばされないことを確認するテストを追加し、失敗することを確認する
  - `buildFragmentsForMessage` 経路（`messages`）と `buildFragmentsForSendSync` 経路（送信同期）の両方
  - 期待値: `FW_HEADER` という値を持つレコードも他の `record_type` 値と同じくフラグメントとして構築される
- [x] B. GREEN: `YamlFileBuilder#buildFragmentsInternal` の `skipFwHeader && FW_HEADER_RECORD_TYPE.equals(recordType)` によるスキップ分岐を削除する
  - `skipFwHeader` 引数が担う他の役割（`record_type` を `"default"` 固定・length 未指定を `"-"` 扱い）は維持し、引数名・Javadoc を実態に合わせる（`skipFwHeader` → `messaging` にリネーム）
  - `YamlSection.FW_HEADER_RECORD_TYPE` が未使用になったら削除する（削除済み）
- [x] C. 旧記法（`record_type: FW_HEADER` レコード）に依存している既存テスト・テストデータを新仕様（`fw_header:` マップ）へ移行する
  - **移行しない**判断に変更した。解説書 `testdata_notation.rst:1296`/`:1302` のとおり `record_type` に予約値はなく `FW_HEADER` は現仕様でも合法な装飾値であり、スキーマにも `enum` 制約がないため、実データ経路に残すこと自体が「特別扱いしない」ことの回帰ガードになる。実際に変異実験でこれらの fixture を使うテストが RED になることを確認した（詳細は `checks/task-12.md`）
- [x] D. `mvn clean test` 全 PASS 確認（Skipped は #11 で `@Ignore` にした 4 件のみ）
  - `Tests run: 173, Failures: 0, Errors: 0, Skipped: 4` / BUILD SUCCESS
- [x] E. commit・push（`0b53910` → レビュー反映 `b91abc1` → `e9213ad`）
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-12.md)
- [x] G. QA expert review (subagent) — pass
- [x] H. Craft expert review — coding (subagent) — pass
- [x] I. Verification expert review — test (subagent) — pass

**Completion criteria**:

- `record_type` の値が `FW_HEADER` のレコードが、`messages` 経路・送信同期メッセージ経路のいずれでも読み飛ばされずフラグメントとして構築される
- `src/main` に `record_type` の値を特別扱いする分岐・定数が残っていない
- FW 制御ヘッダを `fw_header:` マップから取得する経路が従来どおり動作する（既存の `fw_header` 関連テストが GREEN）
- `mvn clean test` が BUILD SUCCESS（Skipped は #11 の 4 件のみ、Failures/Errors 0）
- `ntf-testdata-yaml-schema.json` の検証ルール構造（`type` / `enum` / `required`）が変更されていない
- `nablarch-testing` 本体が変更されていない

---

### #13: YML-08 — ディレクティブ description を解説書の記法へ修正する

**Purpose**: `ntf-testdata-yaml-schema.json` の `record-separator` / `field-separator` の description が、`DataFile#setDirective` の `trim()` により実際には通らない「実制御文字をリテラル指定する記法」を推奨している状態を、解説書が示す記法（シンボル／エスケープ2文字表記）へ書き換える。

**Prerequisites**: #12

**Steps**:

- [x] A. RED: 現 description が推奨する記法が実際には通らないことを実行で確認するテストを追加する
  - `record-separator` に実制御文字 `"\r\n"`（YAML のダブルクォートで展開された CR+LF）を与えるとレコード区切りが空になること
  - `field-separator` に実制御文字のタブを与えると `IllegalArgumentException`（`field-separator must be one character`）になること
  - あわせて解説書が示す記法（`record-separator: CRLF` 等のシンボル）が通ることを確認する
  - RED 実行結果: `Tests run: 2, Failures: 1, Errors: 1` / BUILD FAILURE（`Expected: is "\r\n" but: was ""` / `IllegalArgumentException: field-separator must be one character.but was `）
  - シンボルは 4 種すべて（`NONE` / `CR` / `LF` / `CRLF`）を assert する形へ拡張した（fact-check レビュー指摘）
- [x] B. GREEN: `$defs.directives.properties.record-separator.description` と `field-separator.description` を解説書と一致する記法へ書き換える
  - `record-separator`: シンボル `NONE` / `CR` / `LF` / `CRLF`（解説書 `:945`、記述例 `:1114`）
  - `field-separator`: エスケープ2文字表記 `\t`（解説書 `:1078`）
  - 実制御文字を値に書く記法の記述を削除する
  - あわせて、YAML で実制御文字に展開されてしまう罠を両 description に警告として書き足した。`field-separator` 側は解説書 `testdata_examples.rst:1435` の YAML 固有の注意と同じ機序で説明する形へ揃えた
- [x] C. JSON として妥当か検証（`python3 -c "import json; json.load(open(...))"`）
- [x] D. `mvn clean test` 全 PASS 確認
- [x] E. commit・push
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-13.md)
- [x] G. QA expert review (subagent)
- [x] H. Craft expert review — writing (subagent)
- [x] I. Verification expert review — fact-check (subagent)

**Completion criteria**:

- `record-separator.description` に実制御文字によるリテラル指定（`"\r\n"` 等）の推奨が残っていない
- `field-separator.description` に実制御文字のタブによる指定の推奨が残っていない
- 両 description が示す記法が、実行で通ることをテストで担保されている
- description 以外（`type` / `enum` / `required` 等の検証ルール構造）が変更されていない
- JSON として妥当（`json.load` が通る）
- `mvn clean test` が BUILD SUCCESS（Skipped は #11 の 4 件のみ、Failures/Errors 0）
- `nablarch-testing` 本体（`DataFile#setDirective`）が変更されていない

---

### #14: Evaluation sign-off

**Purpose**: `steering.md` の Acceptance criteria を通しで実行し、その結果をユーザーへ提示して評価ゲートの判定を受ける。

**Prerequisites**: #13

**Steps**:

- [x] A. Acceptance criteria を上から順に実行し、結果（OK/NG と根拠）をまとめる
  - 5 項目中 5 項目 OK
    - `mvn clean test` 全 PASS: 177 tests / 0 failures / 0 errors / Skipped 4、BUILD SUCCESS
    - 移送3タスク（#1〜#3）完了時点の完全一致: `checks/task-02.md`（12件 diff 差分ゼロ）・`checks/task-03.md`（9件 diff 差分ゼロ）に記録済み。複製元は `worktree-agent-a79308e7e5862d004`（`d8ba387`）。旧基準が挙げていた `convert-testdata-excel-to-text`（`fdf55d4`）は `reader/yaml` 配下のファイルを1件も含まない（`git ls-tree -r fdf55d4` で確認）ため、この基準は元々別ブランチを指しており誤りだった
    - #4 以降の実装差分の task 帰属: 本リポジトリの `git log` は #4〜#13 各タスクの commit（`docs(steering): complete task #N` を含む）と1対1で対応しており、タスク外の差分はない
    - 本体無書き込み: `git status` 空、HEAD `fdf55d4`（本体側）
    - push 済み: HEAD `99376b1` = `origin/feature/ntf-yaml`
- [ ] B. 結果をユーザーへ提示し、`/rn:ty`（承認）または `/rn:gm`（差し戻し）の判定を受ける
  - **保留（2026-08-21）**: 同一ブランチに #15〜#20 を追加したため step A の実行結果は陳腐化した。sign-off は #20 完了後に step A を再実行してから受ける。

**Completion criteria**:

- Acceptance criteria 全項目の実行結果が根拠つきで提示されている
- ユーザーの判定（`/rn:ty`）が得られている

---

### ~~#15: 2-1 — `#23` の修正で解消した不具合 #F の `@Ignore` を外す~~

**Purpose**: 本体 `TableData#loadData()` の `#23`（`b6e049a`）対応により不具合 #F が解消したため、保留していた偽陰性テストを復活させる。

**Prerequisites**: none

**Steps**:

- [x] A. `YamlTestDataParserTest.java:396`〜`:398` の FIXME コメントと `:399` の `@Ignore` を削除する（Given/When/Then は1文字も変えない）
- [x] B. `mvn -o clean test` で `emptyExpectedTable_failsWhenDbHasRows` が緑になることを確認する（緑にならなければ commit せず revert し、surefire レポートを添えて報告）
- [x] C. commit・push
- [x] D. self-check (OK/NG per completion criterion, record in checks/task-15.md)
- [x] E. QA expert review (subagent)
- [x] F. Verification expert review — test (subagent)

**Completion criteria**:

- `YamlTestDataParserTest` に `FIXME` / `@Ignore` が残っていない
- `emptyExpectedTable_failsWhenDbHasRows` が実行され PASS している（Skipped でない）
- テストの Given/When/Then が `0197071` 時点と同一
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#16: 2-2 / 2-3 — `rows: []` のカラム名解決の責務を本体側に置き、期待値と FIXME を実態に合わせる~~

**Purpose**: `rows: []` のときカラム名を解決するのは DB を知る `TableData#loadData()`（本体）の責務であり、YAML を読むだけの `YamlTableDataBuilder` が長さ0の列名で `TableData` を作るのは正しい、という責務の所在を期待値とコメントに反映する。

**Prerequisites**: #15

**Steps**:

- [x] A. `YamlTableDataBuilderTest` の3テスト（`:145/147`・`:419/421`・`:869/871`）で「dbInfo の全カラム数（11）が返ること」を主張する `assertThat` を、列名0件を主張する形へ直す（テーブル名・件数・行数の `assertThat` は変えない）
- [x] B. 各テストの javadoc に、なぜ列名0件が正しいのかを1文で書く（責務の所在）
- [x] C. 3件の FIXME コメントと 3件の `@Ignore` を削除する
- [x] D. `YamlTableDataBuilder.java:110` の FIXME を、長さ0の列名で `TableData` を作ってよい理由を説明する通常のコメントへ書き換える（「FIXME」「TODO」の語を残さない）
- [x] E. `mvn -o clean test` 全 PASS 確認
- [x] F. commit・push
- [x] G. self-check (OK/NG per completion criterion, record in checks/task-16.md)
- [x] H. QA expert review (subagent)
- [x] I. Craft expert review — coding (subagent)
- [x] J. Verification expert review — test (subagent)

**Completion criteria**:

- `grep -rn 'TODO\|FIXME\|@Ignore' src --include=*.java` が 0 件
- 3テストが実行され PASS している（Skipped 0）
- 変更した `assertThat` は列名に関するものだけで、テーブル名・件数・行数の主張は `0197071` 時点と同一
- `YamlTableDataBuilder.java` のコメントが責務の所在を説明している
- `mvn -o clean test` が BUILD SUCCESS

---

### #17: `resolveColumns` の先頭行 `{}` によるデータ行の無言消失を塞ぐ

**Purpose**: `rows` の先頭要素が空マッピング（`{}`）のとき列名が0件になり、後続の実データ行が無言で捨てられる不具合を解消する。`setup_tables` ではテーブルが空にされたうえでデータが投入されず、エラーも出ない。

**Prerequisites**: #16

**根拠（2026-08-21 実測）**: `YamlSection.java:144-149` の `resolveColumns` は `rows.get(0)` のキーのみを列名にする。先頭が `{}` なら `columnNames` が空になり、`YamlTableDataBuilder.java:203-215` の `extractRows` が実データ行に対しても空の `rowValues` を作る（`for (String col : columnNames)` が0周）。その空行は `YamlTableDataBuilder.java:121-123` の `if (rawRow.isEmpty()) { continue; }` で捨てられる。

**Steps**:

- [ ] A. RED: 3経路それぞれに「先頭 `{}` ＋後続に実データ行」のテストを追加し、失敗することを確認する
  - `buildTableDataList`（`setup_tables`）
  - `buildTableDataList`（`expected_tables`）
  - `buildListMapRows`（`list_maps`。`YamlTableDataBuilder.java:157` が同じ `resolveColumns` を呼ぶ）
- [ ] B. フィクスチャ: 先頭 `{}` の**新規グループ**を `tableData.yaml` 等に追加する。**既存グループ `emptyRowMixed`（`tableData.yaml:63-75`。`{}` が2番目）は変更しない**
- [ ] C. GREEN: `resolveColumns` を「先頭の**非空マッピング行**のキーを列名にする」実装へ変更する。非空マッピング行が1つも無ければ従来どおり空リストを返す
- [ ] D. `list_maps` 経路でも同根の症状（先頭 `{}` で全行が空 Map になる）が塞がることを確認する
- [ ] D2. #16 で書いた `YamlTableDataBuilder.java:111` の冒頭「列名は先頭行（`rows.get(0)`）のキーからのみ決まる」と、`YamlSection#resolveColumns` の javadoc（`:135-143`）が、変更後の実装と食い違わないよう更新する
- [ ] E. 変異確認（`指示/00-共通ルール.md:62`）: 追加した各テストについて、その分岐を壊す変更を1つ入れると落ちることを実際に確認し、元に戻す。コマンドと結果を記録する
- [ ] F. `mvn -o clean test` 全 PASS 確認（`Tests run:` の行を確認。`BUILD SUCCESS` だけを根拠にしない）
- [ ] G. commit・push（**#16 とは別コミット**）
- [ ] H. self-check (OK/NG per completion criterion, record in checks/task-17.md)
- [ ] I. QA expert review (subagent)
- [ ] J. Design expert review (subagent)
- [ ] K. Craft expert review — coding (subagent)
- [ ] L. Verification expert review — test (subagent)

**スコープ外（今回直さない。報告書へ回す）**: 「先頭行に無いキーを2行目以降が持っていても捨てられる」件（2026-08-21 ユーザー判断）。

**Completion criteria**:

- 先頭 `{}` ＋後続実データ行のとき、`setup_tables` / `expected_tables` / `list_maps` の3経路すべてで後続のデータ行が保持される
- 非空マッピング行が1つも無い場合は従来どおり列名0件（#16 で確定した振る舞いが退行していない）
- 既存グループ `emptyRowMixed` のフィクスチャと期待値が変更されていない
- 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている
- `pom.xml` / `argLine` が変更されておらず、他リポジトリへの書き込みが無い
- `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0）

---

### #18: 手順3（XLS-42）— `record_fragment.rows` のスキーマ記述を実装と解説書に合わせる

**Purpose**: スキーマの `description` だけが実装・解説書と食い違っているため、実装に合わせて直す。

**Prerequisites**: #17

**Steps**:

- [ ] A. 3出典を実物で再確認する（`DataFileFragment.java:107` / `testdata_notation.rst:883` / 当スキーマ）※着手時に確認済み・行番号一致
- [ ] B. `$defs.record_fragment.properties.rows.description` を「fields の件数より少ない場合、不足したフィールドは `""` として補完される」趣旨へ直す
- [ ] C. `items.description` の「fields の順序に完全対応」も同件数前提の書き方なら合わせて直す
- [ ] D. 「多い側」には触れない（エラー化とも「余りは無視される」とも書かない）
- [ ] E. JSON として妥当か検証（`python3 -c "import json; json.load(open(...))"`）
- [ ] F. スキーマ検証テストが緑のままであることを確認。`rows:` の要素数が `fields` より少ない YAML がスキーマ検証で落ちるなら直す
- [ ] G. commit・push
- [ ] H. self-check (OK/NG per completion criterion, record in checks/task-18.md)
- [ ] I. QA expert review (subagent)
- [ ] J. Craft expert review — writing (subagent)
- [ ] K. Verification expert review — fact-check (subagent)

**Completion criteria**:

- `rows.description` に「一致しない場合は NTF がエラーを出す」が残っていない
- 「多い場合」に関する記述を新たに追加していない
- `description` 以外（`type` / `items` の構造・`pattern` / `required` 等）を変更していない
- JSON として妥当
- `mvn -o clean test` が BUILD SUCCESS

---

### #19: 手順4 — 変更差分のカバレッジ実測と未達分岐を埋めるテスト追加

**Purpose**: このブランチが base から変更した `src/main`（10ファイル `+1843`・全部新規＝結果的にモジュール全体）について C0/C1 100% を満たす。

**Prerequisites**: #18

**Steps**:

- [ ] A. `mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes` → `mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec` でカバレッジを採取する（`pom.xml` / `argLine` は変更しない）
- [ ] B. awk で未達クラス一覧を出し、**テストを足す前にユーザーへ提示する**
- [ ] C. ユーザーの合図後、未達分岐を埋める「意味のあるテスト」を追加する
- [ ] D. 追加テストごとに、その行・分岐を壊す変更を1つ入れると落ちることを実際に確認し、コマンドと結果を記録する
- [ ] E. 到達不能と判断した行・分岐は、`@ExcludeFromCoverage` の類を足さず、理由をコードの行番号付きで報告する
- [ ] F. 再計測して `INSTRUCTION_MISSED` / `BRANCH_MISSED` が 0 であることを確認する
- [ ] G. commit・push
- [ ] H. self-check (OK/NG per completion criterion, record in checks/task-19.md)
- [ ] I. QA expert review (subagent)
- [ ] J. Verification expert review — test (subagent)

**Completion criteria**:

- 差分対象クラスの `INSTRUCTION_MISSED` が 0、`BRANCH_MISSED` が 0（到達不能としてユーザーが承認した箇所を除く）
- 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている
- 追加テストの javadoc に「何を担保するか」が1文で書かれている
- `pom.xml` / `argLine` が変更されていない
- `mvn -o clean test` が BUILD SUCCESS

---

### #20: 手順5 — `mvn install`（下流 converter の前提）

**Purpose**: 順序3番目の `nablarch-testing-converter` が着手できるよう、緑になった成果物を `.m2` へ配置する。

**Prerequisites**: #19

**Steps**:

- [ ] A. すべて緑であることを確認する
- [ ] B. `mvn -o install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true` を実行する
- [ ] C. `~/.m2/.../nablarch-testing-yaml-1.0.0-SNAPSHOT.jar` のタイムスタンプが実行時刻へ更新されたことを確認する（着手前は 2026-08-18 09:30:03）
- [ ] D. self-check (OK/NG per completion criterion, record in checks/task-20.md)

**Completion criteria**:

- `mvn -o clean test` が BUILD SUCCESS の状態で install されている
- jar のタイムスタンプが更新されている

---

# State

- **Status**: not suspended
- **Date**: 2026-08-21
- **Last completed**: #16
- **Next**: #17
- **Notes**: —
