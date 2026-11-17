# nablarch-testing-yaml

## このリポジトリの目的

Nablarch Testing Framework（NTF）で、**Excel 形式の代わりに YAML 形式のテストデータを読み込んで**テストを実行できるようにするリポジトリです。

このリポジトリが提供するパーサ `YamlTestDataParser` を `testDataParser` に設定すると、**テストコードを変更せずに** YAML 形式のテストデータでテストを実行できます。テストデータの構成（データタイプ・グループID・特殊記法など）は Excel 形式と共通で、書式だけが異なります。

既存の Excel 形式のテストデータは、[nablarch-testing-converter](https://github.com/nablarch/nablarch-testing-converter) で YAML 形式に変換できます。

## 利用方法

利用するプロジェクトで、次の 3 つを行います。

| 手順 | 対象ファイル | 内容 |
|---|---|---|
| 1 | `pom.xml` | 依存関係に `nablarch-testing-yaml` を追加する |
| 2 | `src/test/resources/unit-test.xml`<br>（`diConfig` で再初期化が発生するテストでは `diConfig` に指定したファイルも） | `testDataParser` を `YamlTestDataParser` に差し替える |
| 3 | テストデータ | テストクラスと同名のディレクトリに YAML ファイルを置く |

> [!IMPORTANT]
> `testDataParser` は 1 つのコンポーネントのため、**1 つのプロジェクトで Excel 形式と YAML 形式を混在させることはできません**。

### 1. 依存関係を追加する

`pom.xml` の `<dependencies>` に追加します（`<build>` の `<plugin>` や `<configuration>` ではありません）。

```xml
<dependencies>
  <!-- ...既存の依存関係... -->

  <dependency>
    <groupId>com.nablarch.framework</groupId>
    <artifactId>nablarch-testing-yaml</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

### 2. testDataParser を差し替える

テスト用のコンポーネント設定ファイル `src/test/resources/unit-test.xml` に、`testDataParser` の定義を追加します。

`testDataParser` は、デフォルト設定 `nablarch/test/test-data.xml` に Excel 形式用の `BasicTestDataParser` として定義されています。同じ名前で上書きするため、**`nablarch/test/test-data.xml` の import より後に**記述してください。既存の記述は変更不要です。

```xml
<component-configuration ...>

  <!-- 既存の記述（変更不要） -->
  <import file="nablarch/test/test-data.xml"/>
  <!-- ...既存の他の記述... -->

  <!-- ここから追加 -->
  <component name="testDataParser" class="nablarch.test.core.reader.YamlTestDataParser">
    <property name="dbInfo" ref="dbInfo"/>
    <property name="interpreters">
      <list>
        <component-ref name="dateTimeInterpreter"/>
        <component-ref name="compositeInterpreter"/>
      </list>
    </property>
  </component>
  <!-- ここまで追加 -->

</component-configuration>
```

Excel 形式の定義（`BasicTestDataParser`）とは、次のプロパティが異なります。

| プロパティ | 設定内容 |
|---|---|
| `interpreters` | `nablarch/test/test-data.xml` が定義している `dateTimeInterpreter`・`compositeInterpreter` の 2 つだけを、この順で指定する |
| `testDataReader` | 指定不要。Excel 形式では Excel ファイルを読み込むために必須（`nablarch/test/test-data.xml` では `PoiXlsReader` を指定）だが、`YamlTestDataParser` は YAML ファイルを直接読み込むため使用しない |

#### diConfig で再初期化が発生するテストの場合

`diConfig` で再初期化が発生する以下のテストの場合は、**`diConfig` に指定したファイルにも**、上と同じ `testDataParser` の設定が必要です。

- 同期応答メッセージ送信または HTTP メッセージ送信を行う、バッチ・メッセージングのリクエスト単体テスト

これらのテストでは、応答電文のテストデータが `diConfig` 側の `testDataParser` で読み込まれるためです。例えば `diConfig: "ss21AC01/B21AC01.xml"` と記載している場合は、`ss21AC01/B21AC01.xml` に記述します。

### 3. テストデータを配置する

テストクラスと同じディレクトリに、テストクラスと同名のディレクトリを作り、その中に YAML ファイルを置きます。

```
src/test/java/com/example/
├── FooTest.java
└── FooTest/              ← テストクラスと同名のディレクトリ（Excel 形式の FooTest.xlsx に相当）
    ├── testInsert.yaml   ← Excel 形式のシート testInsert に相当
    └── testUpdate.yaml
```

```yaml
# src/test/java/com/example/FooTest/testInsert.yaml
setup_tables:
  - table: USER_MASTER
    rows:
      - USER_ID: "0000000001"
        USER_NAME: "山田太郎"
```

Excel 形式との対応は次のとおりです。

| 階層 | Excel 形式 | YAML 形式 |
|---|---|---|
| データコンテナ（テストクラス 1 つ分） | テストクラスと同名の Excel ファイル | テストクラスと同名のディレクトリ |
| データセクション（読み込み単位） | シート | YAML ファイル（`<シート名>.yaml`） |
| データブロック（1 用途分のデータ） | データタイプで始まる矩形範囲 | トップレベルキー（`setup_tables` など）の下の 1 エントリ |

- データ値は必ずダブルクォートで囲んでください。
- YAML ファイルは読み込み時に、jar に同梱した JSON Schema（`nablarch/test/ntf-testdata-yaml-schema.json`）で検証されます。
- `src/test/java` 以外に置く場合は、テスト用の環境設定ファイル（`unit-test.xml` から `config-file` 要素で読み込む `.properties` ファイル）に `nablarch.test.resource-root` を設定します。

## 実行方法

リポジトリのルートディレクトリ（`pom.xml` のあるディレクトリ）で、次のコマンドを実行します。

| 目的 | コマンド |
|---|---|
| テスト | `mvn test` |
| BUILD | `mvn clean package` |

## 依存関係

| ライブラリ | 役割 |
|---|---|
| `nablarch-testing` | NTF 本体（`YamlTestDataParser` が継承する `BasicTestDataParser` などのテストインフラ） |
| `snakeyaml-engine` | YAML（1.2）の読み込み |
| `json-schema-validator` | YAML テストデータの JSON Schema 検証 |
