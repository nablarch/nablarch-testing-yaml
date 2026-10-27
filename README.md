# nablarch-testing-yaml

## このリポジトリの目的

Nablarch Testing Framework（NTF）で、**Excel 形式の代わりに YAML 形式のテストデータを読み込んで**テストを実行できるようにするリポジトリです。

NTF はデフォルトでは Excel 形式（`.xls` / `.xlsx`）のテストデータを読み込みます。このリポジトリが提供するパーサ `YamlTestDataParser` を `testDataParser` に設定すると、**テストコードを変更せずに** YAML 形式のテストデータでテストを実行できます。テストデータの構成（データタイプ・グループID・特殊記法など）は Excel 形式と共通で、書式だけが異なります。

既存の Excel 形式のテストデータは、[nablarch-testing-converter](https://github.com/nablarch/nablarch-testing-converter) で YAML 形式に変換できます。

## テストデータの構造

Excel 形式と YAML 形式は、次のように対応します。

| 階層 | Excel 形式 | YAML 形式 |
|---|---|---|
| データコンテナ（テストクラス 1 つ分） | テストクラスと同名の Excel ファイル | テストクラスと同名のディレクトリ |
| データセクション（読み込み単位） | シート | YAML ファイル（`<シート名>.yaml`） |
| データブロック（1 用途分のデータ） | データタイプで始まる矩形範囲 | トップレベルキー（`setup_tables` など）の下の 1 エントリ |

テストデータは、テストクラスと同じディレクトリ（`src/test/java` 配下のテストクラスのパッケージのディレクトリ）に置きます。

```
src/test/java/com/example/
├── FooTest.java
└── FooTest/              ← テストクラスと同名のディレクトリ
    ├── testInsert.yaml   ← データセクション（Excel 形式のシート testInsert に相当）
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

YAML ファイルは読み込み時に、jar に同梱した JSON Schema（`nablarch/test/ntf-testdata-yaml-schema.json`）で検証されます。データ値は必ずダブルクォートで囲んでください。

## 利用方法

利用するプロジェクトで、次のファイルに設定を追加します。

| ファイル | 設定内容 |
|---|---|
| `pom.xml` | `<dependencies>` に `nablarch-testing-yaml` を追加する |
| `src/test/resources/unit-test.xml`（テスト用のコンポーネント設定ファイル） | `testDataParser` を `YamlTestDataParser` に差し替える |
| テストショット一覧の `diConfig` に指定したファイル（同期応答メッセージ送信または HTTP メッセージ送信を行うバッチ・メッセージングのリクエスト単体テストのみ） | `unit-test.xml` と同じ `testDataParser` を記述する |

### pom.xml：依存関係の追加

`pom.xml` の `<dependencies>` に追加します（`<build>` の `<plugin>` や `<configuration>` ではありません）。

```xml
<dependencies>
  <!-- 既存の依存関係 -->

  <dependency>
    <groupId>com.nablarch.framework</groupId>
    <artifactId>nablarch-testing-yaml</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

### unit-test.xml：testDataParser の差し替え

テスティングフレームワークは、テスト実行時にクラスパス直下の `unit-test.xml`（通常は `src/test/resources/unit-test.xml`）を読み込みます。このファイルの `<component-configuration>` 直下に、`testDataParser` を定義します。

`testDataParser` は、デフォルト設定 `nablarch/test/test-data.xml` に Excel 形式用の `BasicTestDataParser` として定義されています。同じ名前で定義し直して上書きするため、**`nablarch/test/test-data.xml` の import より後に**記述してください。既存の記述は変更せず、追加するのは `testDataParser` の定義だけです。

```xml
<component-configuration ...>

  <!-- 既存の記述（変更不要）。書く位置の目印として、test-data.xml の import だけを示す -->
  <import file="nablarch/test/test-data.xml"/>
  <!-- ...既存の他の記述... -->

  <!-- ここから追加：YAML 形式のテストデータを解析するコンポーネント（import より後に書く） -->
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

| プロパティ | 設定内容 |
|---|---|
| `interpreters` | `nablarch/test/test-data.xml` が定義している `dateTimeInterpreter`・`compositeInterpreter` の 2 つだけを、この順で指定する |
| `testDataReader` | 指定不要。Excel 形式（`BasicTestDataParser`）では Excel ファイルを読み込むために必須のプロパティ（`nablarch/test/test-data.xml` では `PoiXlsReader` を指定）だが、`YamlTestDataParser` は YAML ファイルを直接読み込むため使用しない |

### diConfig に指定したファイル：testDataParser の追加

**同期応答メッセージ送信または HTTP メッセージ送信を行うバッチ・メッセージングのリクエスト単体テスト**では、テストショット一覧（`testShots`）の `diConfig` に指定したファイルにも、`unit-test.xml` と同じ `testDataParser` を記述します。これらのテストは `diConfig` のファイルで実行され、応答電文のテストデータもそのファイルの `testDataParser` で読み込まれるためです。上記以外のテストでは不要です。

```yaml
# テストショット一覧の例：この場合は ss21AC01/B21AC01.xml に記述する
list_maps:
  - id: testShots
    rows:
      - no: "1"
        diConfig: "ss21AC01/B21AC01.xml"
```

### 注意点

- `testDataParser` は 1 つのコンポーネントのため、**1 つのプロジェクトで Excel 形式と YAML 形式を混在させることはできません**。
- テストデータを `src/test/java` 以外に置く場合は、テスト用の環境設定ファイル（`unit-test.xml` から `config-file` 要素で読み込む `.properties` ファイル）に `nablarch.test.resource-root` を設定します。

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
