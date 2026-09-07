# CC① 作業指示 — nablarch-testing-yaml 構築

## 前提

- すべてのリポジトリは同じ親ディレクトリに clone 済み（例: `<parent>/nablarch-testing`, `<parent>/nablarch-testing-yaml`）。

- **NTF 本体（nablarch-testing）はブランチ `convert-testdata-excel-to-text` のまま**。本作業の移動元はこのブランチにある。

- このCCは **nablarch-testing-yaml で起動**し、yaml リポジトリのみを変更する。**本体（nablarch-testing）は参照のみ。一切書き込まない。**

- 本体からのファイル削除は別CC（CC②）が行う。このCCはやらない。

## 絶対禁止（最優先）

- **実装の変更は一切しない**。移動するコードのロジック・シグネチャ・振る舞いを変えてはいけない。

- 許されるのは「ファイルの物理コピー（パス変更）」と、それに伴う **package 宣言・import の機械的な調整**、および **pom の依存設定**のみ。

- コンパイル・テストが通らず実装変更が要ると判断したら、**勝手に直さず作業を止めてユーザーに確認**する。

- 「テスト PASS」を通すために実装をいじるのは事故。PASS しない＝設計・依存・配置の問題として扱い、止めて報告する。これは完了条件より優先。

- 本体（nablarch-testing）には書き込まない（読み取り専用）。

## 目的

NTF の YAML 読み込み機構（src/main）とその単体テストを yaml リポジトリへ切り出し、`mvn test` 全 PASS の状態にする。移動元（本体現ブランチ）との差分が無い（実装無改変）ことを確認する。

## 依存（pom 設定の指針・本体 pom を参考）

- 本体 `com.nablarch.framework:nablarch-testing` を **compile 依存**に追加（yaml は本体 core の `core.util.interpreter` / `core.file.*` / `core.db.*` / `core.messaging.*` / `core.reader.DataType` 等を import）。

- YAML ライブラリ: `org.snakeyaml:snakeyaml-engine:3.0.1`（本体 pom と同一）。

- テスト: JUnit（本体 pom 踏襲）。

- 親 POM: `com.nablarch:nablarch-parent`、groupId は本体に合わせる。

## 移動対象（本体現ブランチ → yaml。本体側パスと同一パッケージで配置）

### A. src/main（12件）
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

### B. src/test java 単体テスト（9件）
- `src/test/java/nablarch/test/core/file/TestCoreFileAdapterTest.java`
- `src/test/java/nablarch/test/core/reader/TestCoreReaderAdapterTest.java`
- `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest.java`
- `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java`
- `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java`
- `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest.java`
- `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java`
- `src/test/java/nablarch/test/core/reader/yaml/YamlSectionTest.java`
- `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java`

### C. src/test テストデータ（43件・ディレクトリ単位）
- `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest/*` 配下一式
- `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/*` 配下一式
- `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest/*` 配下一式
- `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest/*` 配下一式
- `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/*` 配下一式
- `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/*` 配下一式

## 手順

1. `nablarch-testing-yaml` で develop から作業ブランチを作成。

2. pom.xml を作成（上記「依存」に従い本体 pom を参考に）。

3. A（src/main）を yaml の `src/main/java`・`src/main/resources` の同一パッケージパスへコピー配置。

4. B（単体テスト）・C（テストデータ）を `src/test` の対応パスへコピー配置。テストデータはテストクラスと同じ相対パスを維持。

5. `mvn test` を実行し **全テスト PASS** を確認。落ちたら **配置（pom 依存・リソースのパス）**で解決する。**コードは変更しない**。配置で解決せず実装変更が要るなら **止めてユーザーに確認**。

6. **差分チェック（実装無改変の証明）**: 移動した各ファイルを、本体現ブランチの対応ファイルと 1 件ずつ diff し、**package 宣言・import 以外の差分が無い**ことを確認する。

   - 例: `diff <(git -C ../nablarch-testing show convert-testdata-excel-to-text:src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java) src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java`

   - 1 行でも実装差分があれば実装を変えている＝NG。**止めて報告**。

7. 全対象で差分ゼロを確認後、commit・push。

## 完了条件

- yaml リポジトリ `mvn test` 全 PASS。

- 全移動ファイルが本体現ブランチと（package/import 除き）完全一致＝実装無改変を確認済み。

- push 済み。本体には一切書き込んでいない。

## 注意

- 本体テストクラスを `extends` するもの（`*YamlTest`）や `YamlModeTestBase` は **このリポジトリに含めない**（integration 行き。converter を実行時に呼ぶため）。上記移動対象に含まれていないことを確認すること。
