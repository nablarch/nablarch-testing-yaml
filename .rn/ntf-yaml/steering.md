# Goal

nablarch-testing の YAML 読み込み機構（src/main 12件）とその単体テスト（src/test 52件）を
nablarch-testing-yaml リポジトリへ切り出し、`mvn test` 全 PASS の状態にする。
実装は一切変更せず、ファイルの物理コピー（パス変更）と package/import の機械的調整、
および pom 設定のみを行う。

# Acceptance criteria

- `mvn test` 全テスト PASS（yaml リポジトリ単体で緑）
- 全移動ファイルが本体ブランチ `convert-testdata-excel-to-text` と package/import を除き完全一致（実装無改変）
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

# Decisions

## D-1: ブランチを develop から切り develop へマージ
- **Issue**: マージ先ブランチの選択
- **Conclusion**: develop へ PR・マージ
- **Rationale**: 作業指示に従い develop を統合先とする
- **Evidence**: cc1-yaml-build.md「develop から作業ブランチを作成」
- **Sources**: .rn/ntf-yaml/tasks/cc1-yaml-build.md

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

# State

- **Status**: paused
- **Date**: 2026-06-24
- **Last completed**: #6（全タスク完了）
- **Next**: スキーマ横並びチェックの軸を拡張し、判明した2件のスキーマ不備を修正する
- **Notes**: |
    ## 現状

    全6タスク完了。integration テストで converter 側からスキーマ不備が2件発覚。
    PR #1 はまだ DRAFT のまま（Ready for review にしていない）。

    ## 発覚したスキーマ不備

    ### 不備1: `message_data` に `group_id` が未定義
    - `expected_request_header_messages` / `expected_request_body_messages` は
      `buildSendSyncList` / `buildSendSyncBodies` 経由で `group_id` を読む
    - スキーマの `message_data` には `additionalProperties: false` があり `group_id` が未定義
      → converterが `group_id` を書いたYAMLがバリデーションで弾かれる
    - NTF仕様根拠: `ntf-testdata-doc-examples-messaging.md` §7.2 の例に
      `expected_request_header_messages: - group_id: case1` が明示されている
    - **修正**: `message_data` の properties に `group_id` を追加する

    ### 不備2: `record_fragment.record_type` が `required` になっている
    - converter の `YamlFormatWriter.emitRecords` は `record.getRecordType() != null` の
      場合のみ `record_type` を出力する（省略あり得る）
    - NTF仕様根拠: 解説書 §7.10「`record_type:` に任意の値を記述できる（可読性のためだけ）」
      → 省略可能。§6.8 のエラーケースにも `record_type` 省略は含まれない
    - **修正**: `record_fragment.required` から `record_type` を外す

    ## 横並びチェックの根本問題

    今回のチェックは「読み取り方向（実装が読むフィールドがスキーマに定義されているか）」のみで、
    以下2軸が欠けていた：
    - **書き込み方向**: converterが書くフィールドがスキーマに定義されているか
    - **required整合**: スキーマの `required` がNTF仕様と一致しているか

    ## 次のアクション（再開後すぐ実施: 選択肢A）

    1. スキーマ横並びチェックを3軸に拡張して実施する
       - 軸1（読み取り）: 実装が読むフィールド → スキーマに定義済みか（既存）
       - 軸2（書き込み）: converterが書くフィールド → スキーマに定義済みか（新規）
         → `nablarch-testing` の `YamlFormatWriter` / `YamlFormatReader` が出力するフィールドを列挙
       - 軸3（required）: スキーマの required フィールド → NTF仕様で必須か（新規）
         → `ntf-impl-spec-list.md` の仕様と突き合わせ
    2. 横並びチェックで他に不備がないことを確認してから2件を修正・コミット
    3. `mvn clean test` 全PASS確認
    4. PR #1 を Ready for review に変更
