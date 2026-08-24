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
- mvn は**単独実行**する（並行実行でビルドが汚染された実績あり）。`BUILD SUCCESS` だけを根拠にせず `Tests run:` の行を確認する
- **AskUserQuestion は使わない**（2026-08-24 ユーザー指示）。判断を仰ぐときは本文に書く
- **Craft/QA レビューの要否は次の基準で判断する**（2026-08-24 ユーザー指示。都度確認しない）
  - **判断単位はタスクではなく「ステップ」**（2026-08-24 訂正）。1タスク内でステップごとに要否が割れる場合は、ステップ単位で登録する
  - **必要**: CC が設計・実装するステップ（コード修正・テスト追加）
  - **不要**: ユーザーが文言・構造を逐語指定したステップ／実測・計測ステップ／ビルド・サインオフ
  - 不要としたステップでは、レビューの代わりに **実行コマンドと生の出力、適用後の全文、diff** を報告する
  - 現行タスクへの当てはめ: #21 必要 / #22 必要 / #20 不要 / #14 不要 / **#19 はステップで割れる**（step A・B = 不要／step C = 必要。詳細は #19 に記載）
  - 基準の当てはめに迷う新タスクが出たときだけユーザーに聞く
- **スキーマ `description` が対応すべき先は「実装の分岐」ではなく「スキーマ検証を通過しうる入力に対する、外から観測できる挙動」である**（2026-08-24 ユーザー裁定。`description` 全般に適用）。到達不能な内部規則は書かない。実装側の到達不能な防衛分岐（`YamlSection#castMap` の非 Map 分岐など）はそのまま残してよい
- レビュー用サブエージェントには**個別の一意な作業ディレクトリ**を割り当てる（共有 scratchpad で衝突した実績あり）

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

### ~~#12: YML-03 — `record_type: FW_HEADER` によるレコード読み飛ばしを廃止する~~

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

### ~~#13: YML-08 — ディレクティブ description を解説書の記法へ修正する~~

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
  - **Craft/QA レビューは不要**（Rules の基準: サインオフ。2026-08-24 ユーザー指示）。代わりに **実行コマンドと生の出力** を報告する

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

### ~~#17: `resolveColumns` の先頭行 `{}` によるデータ行の無言消失を塞ぐ~~

**Purpose**: `rows` の先頭要素が空マッピング（`{}`）のとき列名が0件になり、後続の実データ行が無言で捨てられる不具合を解消する。`setup_tables` ではテーブルが空にされたうえでデータが投入されず、エラーも出ない。

**Prerequisites**: #16

**根拠（2026-08-21 実測）**: `YamlSection.java:144-149` の `resolveColumns` は `rows.get(0)` のキーのみを列名にする。先頭が `{}` なら `columnNames` が空になり、`YamlTableDataBuilder.java:203-215` の `extractRows` が実データ行に対しても空の `rowValues` を作る（`for (String col : columnNames)` が0周）。その空行は `YamlTableDataBuilder.java:121-123` の `if (rawRow.isEmpty()) { continue; }` で捨てられる。

**Steps**:

- [x] A. RED: 3経路それぞれに「先頭 `{}` ＋後続に実データ行」のテストを追加し、失敗することを確認する
  - `buildTableDataList`（`setup_tables`）
  - `buildTableDataList`（`expected_tables`）
  - `buildListMapRows`（`list_maps`。`YamlTableDataBuilder.java:157` が同じ `resolveColumns` を呼ぶ）
- [x] B. フィクスチャ: 先頭 `{}` の**新規グループ**を `tableData.yaml` 等に追加する。**既存グループ `emptyRowMixed`（`tableData.yaml:63-75`。`{}` が2番目）は変更しない**
- [x] C. GREEN: `resolveColumns` を「先頭の**非空マッピング行**のキーを列名にする」実装へ変更する。非空マッピング行が1つも無ければ従来どおり空リストを返す
- [x] D. `list_maps` 経路でも同根の症状（先頭 `{}` で全行が空 Map になる）が塞がることを確認する
- [x] D2. #16 で書いた `YamlTableDataBuilder.java:111` の冒頭「列名は先頭行（`rows.get(0)`）のキーからのみ決まる」と、`YamlSection#resolveColumns` の javadoc（`:135-143`）が、変更後の実装と食い違わないよう更新する
- [x] E. 変異確認（`指示/00-共通ルール.md:62`）: 追加した各テストについて、その分岐を壊す変更を1つ入れると落ちることを実際に確認し、元に戻す。コマンドと結果を記録する
- [x] F. `mvn -o clean test` 全 PASS 確認（`Tests run:` の行を確認。`BUILD SUCCESS` だけを根拠にしない）
- [x] G. commit・push（**#16 とは別コミット**）
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-17.md)
- [x] I. QA expert review (subagent)
- [x] J. Design expert review (subagent)
- [x] K. Craft expert review — coding (subagent)
- [x] L. Verification expert review — test (subagent)

**スコープ外（2026-08-24 ユーザー判断で決着・不具合ではない）**: 「先頭行に無いキーを2行目以降が持っていても捨てられる」件は `testdata_notation.rst:819`（「後続の行に最初の行のキーにないものを追加しても、そのキーは読み込まれない」）どおりの仕様。**報告書に不具合として書かない。** 同じく「先頭がマーカーのみの行で後続の実データ行の値が全消失」も `testdata_notation.rst:819`（カラム名は先頭要素のキーで決まる）＋ `:1514`（マーカーカラムは読み込み対象外）どおりで、本体 `HeaderLine.java:87-95` が同じ除外をしており Excel も同結果。**報告書に不具合として書かない。**

**Completion criteria**:

- 先頭 `{}` ＋後続実データ行のとき、`setup_tables` / `expected_tables` / `list_maps` の3経路すべてで後続のデータ行が保持される
- 非空マッピング行が1つも無い場合は従来どおり列名0件（#16 で確定した振る舞いが退行していない）
- 既存グループ `emptyRowMixed` のフィクスチャと期待値が変更されていない
- 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている
- `pom.xml` / `argLine` が変更されておらず、他リポジトリへの書き込みが無い
- `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0）

---

### ~~#18: 手順3（XLS-42）— `record_fragment.rows` のスキーマ記述を実装と解説書に合わせる~~

**Purpose**: スキーマの `description` だけが実装・解説書と食い違っているため、実装に合わせて直す。

**Prerequisites**: #17

**Steps**:

- [x] A. 3出典を実物で再確認する（`DataFileFragment.java:107` / `testdata_notation.rst:883` / 当スキーマ）※着手時に確認済み・行番号一致
- [x] B. `$defs.record_fragment.properties.rows.description` を「fields の件数より少ない場合、不足したフィールドは `""` として補完される」趣旨へ直す
- [x] C. `items.description` の「fields の順序に完全対応」も同件数前提の書き方なら合わせて直す
- [x] D. 「多い側」には触れない（エラー化とも「余りは無視される」とも書かない）
- [x] E. JSON として妥当か検証（`python3 -c "import json; json.load(open(...))"`）
- [x] F. スキーマ検証テストが緑のままであることを確認。`rows:` の要素数が `fields` より少ない YAML がスキーマ検証で落ちるなら直す
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-18.md)
- [x] I. QA expert review (subagent) — 完了条件は全 OK、**総評 NG**（親 `:361` の未修正矛盾）。判定は `checks/task-18.md`
- [x] J. Craft expert review — writing (subagent) — **NG**（NG-1 親子矛盾／NG-2・NG-3 `:386` の重複／NG-4 `:377` の曖昧さ）
- [x] K. Verification expert review — fact-check (subagent) — **OK**（主張A〜F すべて真。旧記述「一致しない場合はエラー」は条件付きでも真でないことを確認）
- [x] L. **判断（2026-08-24 ユーザー回答: 直す。ただし言い換えではなく削除）**: 同 `$defs.record_fragment` の**親 `description`**（スキーマ `:361`）に残る「rows の各配列は fields と完全に同じ順序・**同じ件数**で値を並べること（NTF パーサが列順で対応付ける）」は、子（`:377`）と矛盾するため**第3文を丸ごと削除**する。第1文・第2文は残す。理由（ユーザー）: `:377` の第1・2文が同じ内容を件数の扱いまで含めて正確に述べており、親に言い換えを残すと同一画面内の相互参照が増えるだけで新情報がない
- [x] M. 以下3点を**1ラウンド**で実装エキスパートに修正させる（`description` 以外は触らない） — 完了（`60b2678`。3点とも指示どおり適用、`Tests run: 187, Failures: 0, Errors: 0, Skipped: 0`）
  - `:361` 第3文「rows の各配列は fields と完全に同じ順序・同じ件数で値を並べること（NTF パーサが列順で対応付ける）」を**丸ごと削除**する（直前の `\n` ごと）。第1文・第2文は変更しない
  - `:386`（`items.description`）から「fields の順序で先頭から対応付けられる。」の**一文だけを削除**する（`:377` 第2文と同内容）。「フィールド値のリスト。」は items の要素が何かを示す唯一の記述なので**残す**。「数値・真偽値も文字列（クォート付き）で記述すること」も残す
  - `:377` 第3文 →「各配列の要素数が fields の件数より少ない場合、値を指定しなかったフィールドには `""` が設定される」（Craft NG-4 = Valid: **値**の不足なのに「不足したフィールド」ではフィールド定義の不足とも読める。「補完」は `:108` で「カラム型ごとのデフォルト値」の意味に使われており二義）
  - QA 指摘「多い場合は無言で切り捨てられる旨を書く」は **Invalid**（ユーザー承認済み）。スキーマには書かず、報告書候補として `checks/task-18.md` の Triage に残す
- [x] N. 修正後、Craft(writing) と QA を再実行する（Verification は事実主張が変わらないため再実行不要）— 実行済み。**両者とも総合 fail**。完了条件5件は両者とも全 OK で、落ちた理由は完了条件の外側。判定・根拠・triage は `checks/task-18.md` の round 2 セクション
- [x] O. **判断（2026-08-24 ユーザー回答）**: 3件とも回答済み
  - **判断A: 書き戻す**（形は指定あり）。`:377` を3文構成にする — 第1文＝規範「各要素は fields と同じ順序・同じ件数で値を並べた配列（NTF は fields の順序で位置対応させる）」／第2文＝挙動「要素数が fields の件数に満たない場合、不足した末尾のフィールドは `""` として扱われる」／第3文＝記法「これを利用し、空配列 `[]` を1要素書くと全フィールドが `""` のレコード1件になる」。**第3文は必須**（第2文だけだと「末尾は省いてよい」という緩和に読める。解説書 `rst:883` は補完を緩和ではなく意図した記法の仕組みとして説明しており、第3文があって初めて `:883` と同じ意味になる）。規範の出典は `rst:1143`（ファイルデータ）と `rst:1300`（メッセージング）
  - **判断A の前提測定（実施済み・結果は「補完される」）**: 固定長ファイルでも `rows` の要素数不足が `""` 補完されることを固定長フィクスチャで実測した（下記「実測」参照）。よって第2文に「可変長ファイルでは」の限定は付けない。加えて `rst:883` が `""` 補完を「可変長ファイルでは」の節に置いている点は実装より記述範囲が狭く、⑥ nablarch-document の報告書候補に追加する
  - **判断B: 現状維持**。`:386` は「数値・真偽値も必ず文字列（クォート付き）で記述すること」だけにする（判断A で `:377` に順序の規範が戻るため、`:386` に順序を書くと同一ブロック内の二重定義になる。IDE 補完の挙動は根拠にしない）
  - **判断C: 新タスク #22 として起こす**。`#18` では実施しない
- [x] P. 判断A の回答を受け、round 2 の **Valid 4件**と合わせて**1ラウンド**で修正 — 完了（`mvn -o clean test` → `Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS）
  - `:361` 末尾の `。` を削除（schema 内 description 全64件を再集計し、`。` 終わりが 0 件になったことを確認）
  - `:377` を判断A の3文構成へ。第2文で「不足した**末尾**のフィールド」と末尾限定を明示（実装は位置対応 `DataFileFragment.java:107` = `i < line.size() ? line.get(i) : ""`。**出典は `rst:883`**。round 2 triage で引いていた `rst:787` はテーブルデータ〈Excel〉節のため差し替えた）
  - `:377`「0件も有効」→「`rows` が0件でも有効」。**括弧内「（setup_files では空ファイル、expected_files では出力なしの期待値として使用）」は削除**した（`rows: []` がファイル系でどう出力されるかは未実測。`rst:881` `rst:1146` は 0 バイト空ファイルの表現手段を `records: []` としており表現手段が違う。#15/#16 が実測した `rows: []` はテーブルデータ経路のみ。測っていない挙動は書かない）
  - `:386` に「必ず」を追加（`:108` `:136` と揃える）

**実測（2026-08-24・判断A の前提）**: 固定長・可変長の双方で、`rows` の要素数不足は `""` 補完される。

- 方法: 一時テストクラスから `YamlFileBuilder#buildDataFileList` → `DataFile#toDataRecords()` を実行（実行後に一時ファイルは削除済み）。フィクスチャは `type: fixed`（`FIELD1`/`FIELD2` 各 `半角` 長さ5）と `type: variable`（`NAME`/`VALUE`）で、いずれも `rows` に `["AAAAA"]`（要素1件）と `[]`（空配列）を並べた
- 結果: `FixedLengthFile` → `row[0]={FIELD2=, FIELD1=AAAAA}` / `row[1]={FIELD2=, FIELD1=}`、`VariableLengthFile` → `row[0]={VALUE=, NAME=tanaka}` / `row[1]={VALUE=, NAME=}`
- 裏付け: `addValue` は `DataFileFragment.java:102-115` の1箇所のみで、`grep -rn addValue nablarch/test/core/file/` の結果は `:102`（`addValue`）と `:169`（`addValueWithId`）だけ。`VariableLengthFileFragment` / `FixedLengthFileFragment` に override は無い
- 解説書との差: `rst:883` は `""` 補完を「可変長ファイルでは、…」で始まる節に置いている（同じ文の後半は「固定長ファイルの場合はスペースパディングされた定長レコードとして書き出される」と固定長にも触れており、節の切り方だけが実装より狭い）→ ⑥ nablarch-document の報告書候補

- [x] Q. 修正後、Craft(writing) と QA を再々実行する — 実行済み。**QA pass / Craft fail**。完了条件5件は両者とも全 OK。Valid は Craft F2（「各配列の」の主語補い）の1件のみで修正済み。他は却下・Invalid・報告書候補・`#22` へ帰属。判定・根拠・triage・round 3 の新実測は `checks/task-18.md` の round 3 セクション
- [x] R. **round 3 却下判断へのユーザー回答（2026-08-24）**: Craft F1（接続詞を足す）・Craft F5（`[]` のバッククォート）の**却下は支持**。新事実1・2 の扱い（1=⑥ 報告書候補へ／2=`#22` で扱う）も**両方支持**。QA の `null` 指摘（`:386` が `null` の扱いに触れていない＝ Craft F8・QA F5）への **Invalid 判定は支持**（既存の乖離で本差分の対象外・⑥ 報告書候補へ）。**新事実3 はユーザー未判断**（2026-08-24 訂正。ユーザーが判断したのは新事実1・2 の2件のみ）— 内容は報告済みで**回答待ち**。新事実3 に依存する作業は進めない。**Craft F4 の却下のみ覆る** — 「位置対応させる」に前例が無いのは事実だが、その前例を `#18` 自身が `:386` から消していた。`git show 35f70c7:src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` の `:386` = 「フィールド値のリスト。**fields の順序で先頭から対応付けられる。**数値・真偽値も文字列（クォート付き）で記述すること」を実物で確認済み。**単独ラウンドは立てず、`#22` step A0 で `:377` を「先頭から対応付ける」へ戻す**

**Completion criteria**:

- `rows.description` に「一致しない場合は NTF がエラーを出す」が残っていない
- 「多い場合」に関する記述を新たに追加していない
- `description` 以外（`type` / `items` の構造・`pattern` / `required` 等）を変更していない
- JSON として妥当
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#21: 全値が null／空文字の行がスキップされない不具合を塞ぐ~~

**Purpose**: `rows` / `list_maps` の要素が「空マッピング（`{}`）」の場合も「全ての値が null または空文字」の場合も、行として存在しないものとして扱う。列名解決からもデータ行からも除外し、位置（先頭・中間・末尾）を問わず同じ結果にする。

**Prerequisites**: #18

**根拠（出典）**:

- 解説書 `testdata_notation.rst:1534`「全要素が null または空文字のエントリは読み飛ばされる。（略）YAML では `rows:` 内の要素が空マッピング（`{}`）またはすべての値が空文字の場合にスキップされる。この空エントリの省略は（略）テーブルデータや `LIST_MAP` のエントリ自体を無いものとして扱う点で異なる。」→ #17 が塞いだのはこの一文の**前半（`{}`）だけ**
- 本体の参照実装: `PoiXlsReader.java:140` `isBlankLine`（全セル空文字で true）を `:93` で読み飛ばす。`TestDataParsingTemplate.java:407` を `:180` で同様。**いずれも `interpret` より前**に判定している
- 現状（yaml 側）: 行スキップ判定は `YamlTableDataBuilder.java:145` / `:193` / `:222` の `Map.isEmpty()` のみで、Map が空かどうかしか見ていない

**適用範囲の境界**: テーブルデータ（`setup_tables` / `expected_tables` / `expected_complete_tables`）と `list_maps` のみ。**ファイルデータには適用しない** — `testdata_notation.rst:883` が「この扱いは、テーブルデータの空行のスキップとは異なる仕組みである」と明記しており、ファイルデータでは全フィールド `""` のレコードとして保持するのが正。

**Steps**:

- [x] A. RED: 3経路それぞれに「全値が空文字の行を**先頭**に置く」「**中間**に置く」テストを追加し、失敗することを確認する
  - `buildTableDataList`（`setup_tables`）／`buildTableDataList`（`expected_tables`）／`buildListMapRows`（`list_maps`）
  - 中間位置は現状「値が全部 `""` のデータ行」として投入されるため、消えることを固定する
- [x] A2. RED: `list_maps` の `{}` 行についても、既存テスト `buildListMapRows_emptyRowIncludedAsEmptyMap`（`:765-777`）の期待値を「2件返り、いずれも通常行」へ書き換え、失敗することを確認する。**テストメソッド名と javadoc の Given/Then も書き換える**（現状の挙動を指す文言のため）。**フィクスチャ `emptyRowListMap` は変更しない**
- [x] B. フィクスチャ: **新規グループ**を足す。**既存グループ（`emptyRows` / `allEmptyRows` / `emptyRowMixed` / `leadingEmptyRow` / `emptyRowListMap` / `leadingEmptyRowListMap`）は変更しない**
- [x] C. GREEN: `resolveColumns` / `extractRows` を呼ぶ**前**に「空マッピング、または全ての値が null／空文字」の行を取り除く。本体（`isBlankLine` → `interpret`）と順序を揃える
- [x] D. `null` と `""` の双方が空とみなされること、値が1つでも非空なら残ることを確認する
- [x] E. javadoc（`YamlSection#resolveColumns` `:135-151`、`YamlTableDataBuilder` クラス javadoc `:36-37`・`:104-107`・`:139-142`・`:146`・`:192`・`:211-212`）を変更後の実装と食い違わないよう更新する
- [x] F. 変異確認（`指示/00-共通ルール.md:62`）: 追加した各テストについて、その分岐を壊す変更を1つ入れると落ちることを実際に確認し、元に戻す。**コマンドと結果を報告に含める**
- [x] G. `mvn -o clean test` 全 PASS 確認（`Tests run:` の行を確認）
- [x] H. commit・push
- [x] I. self-check (OK/NG per completion criterion, record in checks/task-21.md)
- [x] J. QA expert review (subagent)
- [x] K. Design expert review (subagent)
- [x] L. Craft expert review — coding (subagent)
- [x] M. Verification expert review — test (subagent)

**実施記録（2026-08-24）**:

- コミット3本: `fb58781`（実装本体）→ `14ad84a`（レビュー修正1）→ `a5cb6dd`（レビュー修正2）→ `d75c79c`（レビュー修正3）。すべて push 済み・force-push なし
- 実装: 判定を `YamlSection#isBlankRow` の1箇所に集約し、`dropBlankRows` を `resolveColumns` / `extractRows` より**前**に適用（本体 `PoiXlsReader#readLine` / `TestDataParsingTemplate#readTestData` と同じ順序）。前段で除去するため到達不能になった防衛分岐4件は削除。`resolveColumns` の読み飛ばしは public API の単体担保として残した
- マーカーカラム（`[COL]`）の値も空行判定の対象に含める。本体 `PoiXlsReader#isBlankLine` が行の全セルを対象とするのに合わせたもの
- レビュー3ラウンド（上限3）を消化。**Verification round 3 = 9変異すべて死亡・生存変異ゼロ・空振りテスト0件**
- 最終: `mvn -o clean test` → `Tests run: 207, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS（coordinator が単独実行して確認）
- 判定・Finding・triage・変異表は `checks/task-21.md`

**未解決（`#21` の完了条件の外側・ユーザー判断待ち）**: スキーマ `ntf-testdata-yaml-schema.json` の `$defs.table_data.properties.rows`（`:108`）と `$defs.list_map_data.properties.rows`（`:135`）の description に、空行スキップの規範が書かれていない。3レビュアーが独立に指摘（Design D / QA F1 / Craft F1）。とくに `:108` は「`null`（クォートなし）および `"null"`（クォートあり）はともに NullInterpreter により Java null に変換される」と等価性を明言しているが、**全値がそれだけの行**では分岐する（裸 `null` のみ → 行ごと消える／`"null"` のみ → 行は残り値が null）。**この分岐は本体 Excel と一致しており実装は正しい**（空セルは `isBlankLine` で落ち、文字列 `null` のセルは非空で残る）ので、直すべきはスキーマの description。coordinator 推奨は **`#22` step A1 として足すこと**（`#22` は step A0 で既に同じファイルに触り、`#18` で確立した「スキーマ description を実装に合わせる」作業と同種）

**確定スコープ（2026-08-24 ユーザー判断）**:

- **対象ケースは2つ** — 「全ての値が null または空文字の行」と「空マッピング（`{}`）の行」の両方
- **対象経路は2つ** — `table_data`（`setup_tables` / `expected_tables` / `expected_complete_tables`）と `list_maps` の両方。`{}` は #17 で `table_data` 側だけ塞いだ状態で、`list_maps` 側は未対応（`YamlTableDataBuilder.java:192-193, 202` が空 Map をそのまま結果に残す）
- **完了条件は `testdata_notation.rst:1534` の一文が「両経路 × 両ケース」で満たされていること**
- **ファイルデータには適用しない** — `:1534` が「別の仕組み」と明記（下の「適用範囲の境界」参照）

**既存テストの期待値変更（同判断）**: `YamlTableDataBuilderTest#buildListMapRows_emptyRowIncludedAsEmptyMap`（`:765-777`）は `:1534` に反する現在の実装を固定しているテストであるため、**期待値を「2件返り、いずれも通常行」へ変更する**。テストメソッド名と javadoc の Given/Then も現状固定の文言なので書き換える。**フィクスチャは変えない**。

**Completion criteria**:

- 全値が null／空文字の行が、先頭・中間のどちらに置かれても、`setup_tables` / `expected_tables` / `list_maps` の3経路すべてで行として存在しなくなる
- 空マッピング（`{}`）の行が、先頭・中間のどちらに置かれても、`setup_tables` / `expected_tables` / `list_maps` の3経路すべてで行として存在しなくなる（`list_maps` は現状 空 Map として結果に残っている）
- 値が1つでも非空の行は従来どおり保持される
- ファイルデータ（`YamlFileBuilder`）の挙動が変わっていない（全フィールド `""` のレコードは保持されたまま）
- 既存フィクスチャのグループが変更されていない
- 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている
- `pom.xml` / `argLine` が変更されておらず、他リポジトリへの書き込みが無い
- `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0）

---

### ~~#22: `record_fragment.rows` の description が述べる挙動を実経路で固定するテストを追加する~~

**Purpose**: `#18` で書き直した `$defs.record_fragment.properties.rows.description` が述べる挙動を、YAML ファイルを経由した実際の経路で実測して固定する。

**Prerequisites**: #18

**方針（2026-08-24 ユーザー判断）**: `YamlFileBuilderTest.java:527-543` のヘッダコメントの方針をそのまま踏襲する。すなわち **description の文字列自体は参照しない**。固定するのは文言が拠り所とする実挙動のみで、文言が変わってもテストは落ちない。

**Steps**:

- [x] A1. **（2026-08-24 ユーザー承認済み）** `#21` から回ってきたスキーマ description の追随。`$defs.table_data.properties.rows`（`:108`）と `$defs.list_map_data.properties.rows`（`:135`）に、`fb58781` で入れた「空マッピング（`{}`）の行および全値が `null` または空文字の行は、行として存在しないものとして扱う」旨を書く。**文言は `YamlSection#dropBlankRows` の実装から起こす。ただし実装と1対1で対応させない**（2026-08-24 ユーザーが当初指示を取り下げ。Rules の規範のとおり、スキーマ検証を通過しうる入力に対する観測可能な挙動だけを書く）。`"null"`（クォートあり）は値として非空のため行は残り、値が Java null になる点を含める。`:108` の既存記述「空配列 `[]` は `setup_tables` において全件 DELETE のみ」との関係（**どちらが先に効くか**）も書き分ける。**`record_fragment` の rows には適用しないことも、誤読を防ぐため明示する**（「ファイル系」と等値にしない — V5）
- [x] A0. **`#18` step R の F4 適用**（**A1 と同一ラウンドで処理する** — 2026-08-24 ユーザー支持）: `$defs.record_fragment.properties.rows.description`（スキーマ `:377`）の「（NTF は fields の順序で**位置対応させる**）」を「（NTF は fields の順序で**先頭から対応付ける**）」へ戻す。根拠は削除前の `:386`（`git show 35f70c7:src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` で確認済み = 「fields の順序で先頭から対応付けられる。」）。`description` 以外は触らない。JSON 妥当性を `python3 -c "import json; json.load(open(...))"` で確認する
- [x] A. `YamlFileBuilderTest` に、`#13` の先例（`:527-543` のヘッダコメント）と同じ体裁のセクションを起こす
- [x] B. フィクスチャ（`YamlFileBuilderTest/fileData.yaml`）に**新規グループ**を足す。既存グループは変更しない
- [x] C. 以下3件を最低限の対象として固定する（`YamlFileBuilder#buildDataFileList` → `DataFile#toDataRecords()` の経路）
  - 要素数が `fields` より少ない行 → **末尾のフィールドが `""` になる**こと
  - `rows: [[]]` → **全フィールドが `""` のレコード1件**になること（解説書 `rst:883`）
  - `rows: []` → **データ行0件**になること

  **期待値 `""` の出典（2026-08-24 ユーザー訂正）**: 判断A でも新事実3 でもなく、依存先 nablarch-testing（`2e43786`、作業ツリー clean）の `DataFileFragment.java:107`
  `String value = i < line.size() ? line.get(i) : "";`（`addValueWithId` は `:175` に同一。実物で確認済み）。`names.size()` 分だけ回し、行データが尽きた位置に `""` を入れる。固定長／可変長・型によらない共通処理。判断A も新事実3 もこの1行の系である。**指示書・報告にはこの出典を使う。**

  **期待値を固定する層（同ユーザー条件・coordinator 決定）**: **`toDataRecords()`（DataRecord 層）で固定し、フィクスチャは可変長（`VariableLengthFile`）を使う。** 可変長経路は変換が恒等写像なので、DataRecord 層の値がそのまま `:107` の `""` になる。出典チェーン（すべて `2e43786` で実物確認済み）:
  - `DataFile.java:155-161` `toDataRecords()` → 各 fragment の `toDataRecords()` を連結
  - `DataFileFragment.java:385-392` → `:401-405` `toDataRecord()` → `convertForDataRecord(value)` の結果を `putAll`
  - `VariableLengthFileFragment.java:31-38` `convertForDataRecord` → `:43-45` `convertValue` = `return stringExpression;`（**恒等**）
  - 値の生成元は `DataFileFragment.java:107`

  **固定長（`FixedLengthFile`）で DataRecord 層の `""` を期待値に書かない**。固定長は `FixedLengthFileFragment.java:45-59` の `convertForDataRecord` が `convertValue`（`:67-88`）に加えて `removePadding(key, converted, dummy)`（`:55`）を通し、最終的に `DataFileFragment.java:484` `dataType.removePadding(value)` に落ちる。`removePadding("")` が `""` を返す根拠は nablarch-core-dataformat 側にあるが、当リポジトリが依存する `6-NEXT-SNAPSHOT` には sources jar が無く（`~/.m2/repository/com/nablarch/framework/nablarch-core-dataformat/6-NEXT-SNAPSHOT/` に `*-sources.jar` 無し。旧版 1.3.5 / 2.0.3 の sources を現行版の根拠にはしない）、**クラス・行番号を示せないため未確認**。示せない以上、固定長で `""` を断定しない
- [x] D. 変異確認（`指示/00-共通ルール.md:62`）: 追加した各テストについて、その分岐を壊す変更を1つ入れると落ちることを実際に確認し、元に戻す。**コマンドと結果を報告に含める**
- [x] E. `mvn -o clean test` 全 PASS 確認（`Tests run:` の行を確認）
- [x] F. commit・push
- [x] G. self-check (OK/NG per completion criterion, record in checks/task-22.md)
- [x] H. QA expert review (subagent)
- [x] I. Craft expert review — coding (subagent)
- [x] J. Verification expert review — test (subagent)

**レビュー ラウンド1（step A0 + A1）の結果と 2026-08-24 ユーザー裁定** — 判定表・全 Finding は `checks/task-22.md`。ラウンド1の実装は `ee4a55e`。4観点（A 充足／B 整合／C 規約／D 検証の妥当性）を独立サブエージェントで実施し全観点 fail。

**step A0（`:377` の1語差し替え）は4観点とも OK**。裁定・修正の対象は step A1（`:108` / `:136` の description）のみ。

**ユーザー裁定3件（2026-08-24・確定）**:

1. **判断1 = 案A**。「マッピングでない行（スカラ等）」を除去対象の列挙から**落とす**。ユーザーの「実装と1対1で対応させる」という当初指示が誤りだったと明示的に取り下げられた。スカラ行は `$defs.table_data.properties.rows.items` = `{"type":"object"}`（`:109-117`）／`list_map_data` 同（`:137-145`）と `YamlLoader.java:121-125` のロード毎 `JSON_SCHEMA.validate` により `YamlSchemaValidationException` で弾かれ、description が説明すべき挙動ではない。**`castMap` / `isBlankRow` の実装は変更しない**（到達不能な防衛分岐として残す）。以後の規範は Rules に登録済み
2. **判断2 = 承認（1句足す）**。`$defs.table_data` は setup_tables / expected_tables / expected_complete_tables の3セクション共通であり、全行除去で 0 件になったとき expected 系では「対象テーブルに行が存在しないこと」の検証になる。根拠 `TableData.java:341-347`（`if (colNames.length == 0) { colNames = dbInfo.getColumns(tableName); }`）。**文面は逐語指定しない**（逐語指定の失敗が判断1のため）。実物から起こしてレビューで見る
3. **判断3 = 別タスク（`#24`）へ送る**。`#22` では直さない。起票条件は `#24` に記載

**裁定不要・修正ラウンドで直す Valid 指摘**（すべて coordinator が実物で裏取り済み。ユーザーは「9件（V2・V4・V5・V7・V8・V9・V10・V14 ほか）」と数えた。「ほか」に当たる1件は判断2 の expected 系0行の追記として処理する）:

| # | 指摘 | 出典 |
|---|---|---|
| V2 | 「`null` も `"null"` もともに **NullInterpreter により** 変換される」は機構として誤り。クォートなし `null` は `interpret` が `value == null` で早期 return するためチェーンに入らない | `YamlSection.java:247-249` |
| V4 | 「カラム名は除去後に残った行から決まる」→「残った**先頭の行**のキー。後続行にしか無いキーは無視される」 | `YamlSection.java:227-235`（`resolveColumns`）/ `YamlTableDataBuilder.java:214-221`（`extractRows` は `columnNames` のキーしか読まない） |
| V5 | 「ファイル系（`record_fragment`）」は誤り。`record_fragment` を使う `$defs` は `file_data` / `message_data` / `expected_request_message_data` / `group_message_data` の4つで、到達するトップレベルは `setup_files` / `expected_files` / `messages` / `expected_request_header_messages` / `expected_request_body_messages` / `response_header_messages` / `response_body_messages` の7セクション | schema `:184` `:210` `:243` `:274` と `properties` の `$ref`（python で実測） |
| V7 | 同一ファイル内で「テーブル系」の定義が矛盾。`:361`「テーブル系（table_data / list_map_data）」に対し `:108` は「テーブル系と list_maps」＝排他扱い | schema `:361` / `:108` / `:136` |
| V8 | `:136` に `testShots` の 0 件エラーとの接続が無い（`:132` は「0件はエラー」と記載） | `AbstractHttpRequestTestTemplate.java:225-229`（`testCases.isEmpty()` で `IllegalStateException`） |
| V9 | `NullInterpreter` は `equalsIgnoreCase` なので `NULL` / `Null` も Java null になる | `NullInterpreter.java:15` |
| V10 | **一部誤り（2026-08-24 実測で訂正）**。「クォートなし `null` には `~` も含まれる」は**偽**。当リポジトリの `snakeyaml-engine 3.0.1` は既定 `JsonSchema` で解決するため `~` は文字列 `~` になる。null になるのは `null`（クォートなし）と**値を省略した `COL:`** の2つ | 実測: `LoadSettings.builder().setAllowDuplicateKeys(false).build()`（`YamlLoader.java:104-106` と同一）で `A: null`→null / `B: ~`→String「~」/ `C:`→null / `D: "null"`→String / `E: NULL`→String / `F: Null`→String / `G: ""`→String「」。`settings.getSchema()` = `org.snakeyaml.engine.v2.schema.JsonSchema` |
| V14 | 文章面: `:136` が `:108` の写しで固有差分が薄い／同じ事実の二重記述／「裸の `null`」という第三の表記／`:108` が 890→1430字で膨張／「列名解決」「値加工」が実装由来の語で「カラム」統一から逸脱／「空配列 []」だけバッククォート無し | schema 実測・`checks/task-22.md` Craft 欄 |

**修正ラウンドで守る制約（`指示/doc-記載漏れの是正.md:25` の明文方針）**: 「**ダメなケースを数え上げて書き並べない。** 数え上げは終わらないうえ、書いた分だけ『書いていないケースは許される』という誤読を生む。」→ null にならない表記（`~` 等）は列挙せず、**null になる表記を肯定形で書く**にとどめる。

**却下（Invalid）と理由**:

- 「`:377` に戻した文言に前例が無い」（観点C）… 事実誤り。`git show 35f70c7:...` の `:386` に「fields の順序で**先頭から対応付けられる**」がある（coordinator が実物確認）。観点C の `git log -S'先頭から対応付ける'` は語尾違いで拾えていない
- A0 の括弧を削る提案（観点C）… 文言はユーザー逐語指定。範囲外
- 「半角スペースだけの値は残る」の明記（観点A・B）… 「空文字 `""`」は字義どおりで読み違えの余地がない。3観点のうち2つが「導出可能」判定。膨張を避ける
- 「`fields` より要素数が多い場合の余剰切り捨てが `:377` に無記載」（観点D）… **YML-14 として 2026-08-21 のユーザー判断で「対応不要（不正入力）」として閉じている**（`指示/doc-記載漏れの是正.md:23`）。範囲の問題ではなく方針として閉じた件（2026-08-24 ユーザー指示により却下理由を差し替え）

**レビュー要否（2026-08-24 ユーザー指示・ステップ単位）**:

- **step A0 + A1（スキーマ description）… 回す**。外から観測できる公開本文に新しい規範が入り、文言を CC 側が起こすため。**ラウンド1は4観点**（A 充足／B 整合／C 規約／D 検証の妥当性）。**対象にはユーザーの指示文そのものを含める**（「指示された案に反例がないか」）。観点B は `dropBlankRows` の実装と description の一致を**実物で照合**する。観点D は「その description で、実装が持つ全ケースを言い切れているか」を見る
- **step C（テスト追加）… 回す**。テストの設計が CC 側にあるため。「意味のあるテスト」の条件（`指示/00-共通ルール.md`）を満たすこと。壊す変更を入れたら落ちることを実際に確認し、**コマンドと結果を報告に書く**

**参考（`#18` step P で実測済み。テストの期待値の裏取りに使える）**: 固定長・可変長の双方で要素数不足は `""` 補完される（`FixedLengthFile` → `row[0]={FIELD2=, FIELD1=AAAAA}` / `row[1]={FIELD2=, FIELD1=}`）。`addValue` は `DataFileFragment.java:102-115` の1箇所のみで override 無し。

**Completion criteria**:

- 上記3件がテストとして存在し、`description` の文字列を参照していない
- 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている
- 既存フィクスチャのグループが変更されていない
- `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0）

---

### #24: `:108` のカラム省略まわりの記述の乖離を是正する

**Purpose**: `#22` のレビュー ラウンド1 で見つかった、`$defs.table_data.properties.rows` の**既存**記述と実装の乖離を潰す。あわせて `:361` の「ファイル系」表記を直す。いずれも `#21` の空行除去によってカラム名決定行が動くようになったため顕在化した。

**Prerequisites**: #22

**注**: 番号 `#23` は本体 nablarch-testing の課題番号として `#15` の表題で既出のため、衝突を避けて欠番とする。

**確定スコープ（2026-08-24 ユーザー指示。3点）**:

- **(a)** `:108` の「各オブジェクトに含まれないカラム（省略したカラム）」**3文**の是正。setup_tables の1文だけでなく expected_tables / expected_complete_tables の2文も対象（観点D の指摘）。文面は逐語指定しない。「省略」が「全行で省略」なのか「その行で省略」なのかを読み手が区別できる書き方にし、空行除去でカラム名決定行が動く点との関係も書く
- **(b)** `:361` の「ファイル系」を「record_fragment」に差し替える。反例が見つかったら反映せず報告する
- **(c)** (a)(b) の波及先の特定を、**反映より前**の作業として行う。スキーマ内の相互参照と、既存テストのうち文言に依存しているものを洗い出し、**完了条件の対象に含める**

**Steps**:

- [x] C. **波及先の特定（反映より前）** — 実施済み。スキーマ内の相互参照3件（`:18` / `:25` / `:108` 内 FK ブロック）を是正対象に追加。description の文字列を assert するテストは**0件**のためテスト変更は不要。詳細は `checks/task-24.md`
- [x] A. **(a) の帰結を実物で確認する** — 実施済み。`#22` レビューの出典チェーンを再点検し、**INSERT の実行箇所まで通した**（本体 `TableData.java:137-177` `insertData` と `:325-334` `getNonComputedColumns` = DB 全カラム。よって YAML に無いカラムも INSERT 文に載り `convert` の `!containsKey` 分岐でデフォルト値になる）。詳細は `checks/task-24.md` の step A
- [x] B. **(a) の反映** — `:108` の該当3文を「(1) 全行で省略 / (2) その行だけ省略」の区別に建て直し、波及先 `:18` / `:25` / `:108` 内 FK ブロックも同じ区別に揃えた
- [x] B2. **(b) の反映** — `:361` の見出しを「【record_fragment の rows は配列の配列】」へ。着手前に独立検証し反例なし（`$ref` 元4つ・到達するトップレベル7セクション）
- [x] D. JSON 妥当性確認と `mvn -o clean test` 全 PASS（`Tests run: 210, Failures: 0, Errors: 0, Skipped: 0`）
- [x] E. commit・push
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-24.md)
- [ ] G. レビュー **必要**（description を CC が起こすため。Rules の基準）
  - **ラウンド1 = 2観点のみ**（**B 整合** / **D 検証の妥当性**）。**A 充足 と C 規約は回さない**（2026-08-24 ユーザー指示）。理由: `#24` は既存 description の是正であり実装変更が無く、外から観測できるものに新しいものが入らない。観点A は対象が (a) の3文と (b) の1語に確定済みで抜けが生じない。観点C は既存 description の文体に揃えるだけで実害が小さい
  - **B 整合** — CC が起こした文が実装と矛盾しないか。次の2つを**別々に**見る
    - **B-1 成果物**: `:108` と `:361` の新しい文言が、`nablarch-testing` の実物と一致するか
    - **B-2 指示文そのもの**: レビュー役の指示文に反例がないか。とくに (b) の「ファイル系 → record_fragment」案と、(a) の根拠として挙げた `Assertion.java:256` / `:297`、`TableData.java:193`、`TableData.java:707-720`。**反例が出たら反映せず報告する**
  - **D 検証の妥当性** — 新しい文言が正しいことの確認手段は、本当に欠陥を検知できるか。実装との読み比べで済ませていないか。`#22` と同じく、文言が述べる挙動を**実経路のテストで固定**し、そのテストが「文言どおりでない実装」を実際に落とすことを確認しているか。確認したコマンドと結果を見る
  - 各担当は別サブエージェントで実施し、プロンプトに次の3点を必ず入れる: 実測で裏付ける／付属の検証スクリプトを正解として使わず独立に組む／敵対的に見る
  - **ラウンド2以降は差分限定2観点**（是正が指示範囲に収まっているか／是正が新しい欠陥を生んでいないか）。**上限3回**。各ラウンドの指摘件数と観点を記録する
  - レビュー用サブエージェントには個別の一意な作業ディレクトリを割り当てる

**レビュー記録**:

| ラウンド | 観点 | 指摘件数 | 判定 |
| --- | --- | --- | --- |
| 1 | B 整合（B-1 / B-2） | Valid 4（V-1〜V-4）・要確認1 | B-1 NG・B-2 反例あり |
| 1 | D 検証の妥当性 | Valid 3（D-1〜D-3）・要確認1 | fail |

**ラウンド1 是正内容**: Valid のみ反映。(1) の定義をカラム名決定行基準に一本化、(2) の Boolean 型 NPE 例外を追記、`:18` にマーカーカラム除外を補い、FK ブロック等の波及表現を揃えた。V-3（(a) 根拠3点目「補完値で比較される」）は偽と判定されたため反映していない。あわせて `YamlColumnOmissionTest`（14件）を新規追加し、`:18` / `:25` / `:108` の主張を実経路＋5系統の変異確認で固定（`checks/task-24.md` 参照）。`mvn -o clean test` = `Tests run: 224, Failures: 0, Errors: 0, Skipped: 0`

**範囲外の欠陥（`#24` では直さず課題として起票）**:

- **X-1**: 旧 step C（マーカーカラム `[COL]` だけが非空の行がカラム名決定行になったときの帰結。`dataColumns` が0件になり全デフォルト値の1行が INSERT される／`list_maps` では空 Map が1件渡る）は、スコープ確定で `#24` から外れた。**未実施・未検証**。別タスクとして起票が必要
- **O-D1（未確認・本体側）**: `:108` 末尾の助言「NULL 許容カラムを NULL にしたい場合は省略せず `null`（クォートなし）を明示すること」は、**BOOLEAN 型カラムでは NPE になる疑い**がある。本体 `TableData.java:162-163` が `insert.setBoolean(bindIndex++, row.containsKey(col) ? row.getBoolean(col) : (Boolean) getDefaultValue(col))` で、`SqlRow#getBoolean` が null を返すと `setBoolean(int, boolean)` の unboxing で落ちると読める。**静的読解のみで実行未確認**。本体 Excel 経路と共通のため YAML 側が持ち込んだ乖離ではない
- **O-D2（軽微）**: `list_maps` がテストコードへ渡す Map は `TreeMap`（キー昇順）である（`YamlTableDataBuilder.java:193`）。`:136` の「そのまま渡す」は記述順を保つとは書いていないので誤りではないが、キー順は観測されうる
- **O-D3（軽微）**: `testShots` が空のとき web 経路は旧 ID `testCases` へフォールバックし、両方空のときだけ例外になる（`AbstractHttpRequestTestTemplate.java:220-229`）。`:136` の「エラーになる」は既存 `:132` と同じ簡略化で、ファイル内では整合している

**Completion criteria**:

- (a) の帰結が実物の出典（ファイル:行番号）つきで確定している
- `:108` の記述が (a)(b) いずれについても実装と食い違わない
- step C で特定した波及先が是正されている
- `description` 以外のスキーマ要素が変更されていない
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#19: 手順4 — 変更差分のカバレッジ実測と未達分岐を埋めるテスト追加~~

**Purpose**: このブランチが base から変更した `src/main`（10ファイル `+1843`・全部新規＝結果的にモジュール全体）について C0/C1 100% を満たす。

**Prerequisites**: #21

**着手時のベースライン（2026-08-24 実測・要再取得）**: `Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS。`TODO`/`FIXME`/`@Ignore` は 0 件

**Steps**:

- [x] A. `mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes` → `mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec` でカバレッジを採取する（`pom.xml` / `argLine` は変更しない）
- [x] B. awk で未達クラス一覧を出し、**テストを足す前にユーザーへ提示する**（実測結果は `checks/task-19.md`）
- [x] B/C ゲート（**ユーザー判断・2026-08-24 明示指示**）: step A・B の実測結果（実行コマンドと生の出力、未達クラス一覧）を報告したら**そこで止まる**。どの分岐を埋めるかの指示を待つ。**勝手に step C へ進まない**。根拠: `指示/yaml-あるべき姿とカバレッジ.md:114`「**まず実測して、未達クラスの一覧を報告すること。** テストを足すのはその後。何件足すことになるかが分かってからでないとユーザーが判断できない。」
- [x] C. ユーザーの合図後、未達分岐を埋める「意味のあるテスト」を追加する → **不実施**（2026-08-24 ユーザー裁定: 未達2箇所は到達不能として承認。テストを足さない）
- [x] D. 追加テストごとに、その行・分岐を壊す変更を1つ入れると落ちることを実際に確認し、コマンドと結果を記録する → **N/A**（step C 不実施）
- [x] E. 到達不能と判断した行・分岐は、`@ExcludeFromCoverage` の類を足さず、理由をコードの行番号付きで報告する → 済（`checks/task-19.md` の「裁定」節。除外アノテーションは追加していない）
- [x] F. 再計測して `INSTRUCTION_MISSED` / `BRANCH_MISSED` が 0 であることを確認する → **再計測不要**（`src/main` も `src/test` も変更していないため step A の実測値が現状値。承認済み到達不能2箇所を除き 0）
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-19.md)
- [x] I. **レビュー要否はステップで割れる**（2026-08-24 ユーザー訂正）— step C 不実施により**レビュー必要ステップが消えた**ため、`#19` ではレビューを回さない（実装変更ゼロ）
  - **step A・B（カバレッジ実測）＝ レビュー不要**。代わりに **実行コマンドと生の出力** を報告する
  - **step C（未達分岐を埋めるテスト追加）＝ レビュー必要**。QA / Craft(coding) / Verification(test) を回す
  - step D・E・F（変異確認・到達不能の報告・再計測）は step C の成果物に付随するため step C のレビューに含める

**裁定（2026-08-24 ユーザー判断）**: 未達2箇所は**到達不能として承認**。テストを足さず step C は実施しない。根拠は `checks/task-19.md` の「裁定」節（要約: ①`YamlFileBuilder:227` の非 List 分岐は、スキーマ `:378-379` の `items.type = array` と `YamlLoader:121` の全ロード検証により到達前に落ちる。②`YamlLoader:60-61` `:65-66` はスキーマを自モジュールの `src/main/resources` に同梱しているため、クラスパス細工でしか再現できない）。リフレクション等で通すテストは書かない（壊れ方そのものが現実に起こらず「壊す変更で落ちること」を満たさないため）。

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

- [ ] A. **判断待ち（着手前にユーザー回答が要る）**: 旧 State の禁止事項「yaml で `mvn install` しない（converter が `pom.xml:42-44` で `1.0.0-SNAPSHOT` に依存し install で壊れる）」と、指示書 手順5 の install 要求が衝突する。共通ルールの順序（yaml → converter）を前提に「converter は直後に自分の指示書で直す」と読んで実行してよいかを確認する
- [ ] A2. すべて緑であることを確認する
- [ ] B. `mvn -o install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true` を実行する
- [ ] C. `~/.m2/.../nablarch-testing-yaml-1.0.0-SNAPSHOT.jar` のタイムスタンプが実行時刻へ更新されたことを確認する（着手前は 2026-08-18 09:30:03）
- [ ] D. self-check (OK/NG per completion criterion, record in checks/task-20.md)
- [ ] E. **Craft/QA レビューは不要**（Rules の基準: ビルド。2026-08-24 ユーザー指示）。代わりに **実行コマンドと生の出力** を報告する

**Completion criteria**:

- `mvn -o clean test` が BUILD SUCCESS の状態で install されている
- jar のタイムスタンプが更新されている

---

# State

- **Status**: in progress
- **Date**: 2026-08-24
- **Last completed**: #19（未達2箇所を到達不能として承認・テスト追加なし）
- **Next**: `#24` の **step G（レビュー）**。ラウンド1（4観点 A/B/C/D を別担当）を実行中。結果を triage し、Valid のみ1ラウンドで反映する。ラウンド2以降は差分限定2観点・上限3回
- **Notes**:
  - ブランチ `feature/ntf-yaml`、push 済み（HEAD `2f060e8`）。PR なし
  - **`#24` の step A〜F は完了**（`2f060e8`）。スキーマ `description` 4本のみ変更（`:18` / `:25` / `:108` / `:361`）。`mvn -o clean test` = `Tests run: 210, Failures: 0, Errors: 0, Skipped: 0`。判定・根拠・波及先の洗い出しは `checks/task-24.md`
  - **`#19` は完了**。ユーザー裁定で未達2箇所（`YamlFileBuilder:227-228` / `YamlLoader:60-61` `:65-66`）を到達不能として承認し、step C（テスト追加）は不実施。根拠は `checks/task-19.md` の「裁定」節
  - **ユーザー判断待ち1件**: **#20 step A の install 判断**（旧 State の禁止事項「yaml で `mvn install` しない」と指示書 手順5 の install 要求が衝突。内容は #20 に記載済み）
  - **新規起票が要るもの（`#24` スコープ外）**: **X-1** — マーカーカラム `[COL]` だけが非空の行がカラム名決定行になったときの帰結（`dataColumns` が0件になり全デフォルト値の1行が INSERT される／`list_maps` では空 Map が1件渡る）。**未実施・未検証**。`#24` の旧 step C だったがスコープ確定（3点）で外れた
  - 本体 `../nablarch-testing` の HEAD が `fdf55d4` → **`2e43786`** に進んでいる（#14 step A の記録時点と異なる）。yaml 側からの書き込みは無い（参照のみ）が、#14 の再実行時に確認すること
  - #14（Evaluation sign-off）step B は #20 完了後に step A を再実行してから受ける
  - ⑥ nablarch-document への報告書候補: `checks/task-18.md`（5件・`rst:883` の2件はセット）、`checks/task-21.md`（`rst:819` と `rst:1534`）、`checks/task-22.md`（`rst:1534` の全値 null 欠落と、`rst:830`／`:1443-1445` の null 等価性）。**`rst:658` と `rst:819` は正しく、報告候補ではない**（食い違っていたのはスキーマ側＝`#24` で是正済み）
  - user-deferred paths: なし
