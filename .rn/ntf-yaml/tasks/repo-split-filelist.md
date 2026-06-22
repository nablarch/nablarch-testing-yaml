# PR#75 リポジトリ分割 方針とファイル処理一覧

- **調査対象**: `nablarch/nablarch-testing` branch `convert-testdata-excel-to-text` HEAD `18c1bbc`

- **基準**: develop `6aa6989`（このブランチの分岐元。develop へ未マージ）

- **判定**: 各ファイルの実依存（import / extends / 利用元）を全件確認。継承・補助・リソースの連鎖を 2 次まで追跡し閉鎖を確認。

## 分割方針

4 リポジトリへ分割し、NTF 解説書は nablarch-document へ反映する。

- **nablarch-testing（既存）**: 本体 core ＋ Excel ベーステスト（`getTestConfig()` フック保持）＋補助。src/main の改変は 4 ファイルのみ（キャッシュ抑止フック）。

- **nablarch-testing-yaml（新規）**: YAML 読込（src/main）＋その単体テスト。本体のみに依存。

- **nablarch-testing-converter（新規）**: 変換ツール＋形式間変換テスト。yaml・本体に依存。

- **nablarch-testing-integration（新規）**: Excel/YAML ダブルテスト（結合）。Excel ベースはコピー、YAML サブクラスは移動。converter・yaml・本体に依存。

- **nablarch-document（別管理）**: NTF 解説書（rst）。`docs/pr75/{docs,design}` をインプットに更新。

依存方向（一方向・循環なし）:

- yaml → 本体

- converter → yaml → 本体

- integration → converter, yaml, 本体

## 作業順序と CC 連携

依存の下流から構築する。順序は **yaml → converter → integration**。

- yaml が土台（本体のみ依存）。converter は yaml に依存。integration は両方＋ダブルテストに依存。先に下流を確定させないと上流が組めない。

各リポジトリの作業は **2 つの CC に分ける**。1 リポジトリ＝1 CC を守り、本体へ書き込めるのは後始末 CC のみとする。

- **構築 CC**: 新リポジトリで起動。develop からブランチを切り、対象を配置して `mvn test` 緑まで。本体は**参照のみ**（実装無改変の差分チェックに使う）。

- **後始末 CC**: 本体（nablarch-testing、現ブランチ `convert-testdata-excel-to-text`）で起動。移動済みファイルを**削除のみ**。

CC 間の連携は、私の中間リストではなく **構築 CC の実成果（その PR 差分）を正本**とする。

- 後始末 CC は「構築 CC が新リポジトリに追加したファイル ∧ 本体現ブランチに存在するファイル」の積集合を削除対象とする。新規構築物（pom 等、本体に無い）は自動的に除外される。

- 後始末 CC は構築 CC の完了（緑・差分ゼロ・push 済み）を待ってから着手する。

実装変更は全 CC で禁止。許されるのは物理移動・package/import の機械的調整・pom 設定のみ。変更が要ると判断したら止めてユーザーに確認する。

## 処理の凡例

- **残留**: nablarch-testing にそのまま残す

- **移動**: nablarch-testing から除去し、先のリポジトリへ

- **コピー**: Excel ベース／補助。本体に残しつつ integration にも複製（ダブルテストの Excel 側）

- **rst反映**: NTF 解説資料。nablarch-document の rst 更新インプット（本体にも記録として残す）

## ダブルテスト機構

- Excel ベースが `getTestConfig()` で `unit-test.xml` を返す。

- YAML サブクラスが `@Override` で `unit-test-yaml.xml` を返し、`YamlModeTestBase` が実行時に Excel→YAML 変換する。

- 同一テスト群を Excel/YAML の 2 経路で実行する。

- integration が本体非依存で完結するよう、Excel ベースはコピー、補助も随伴コピーする（test-jar 不要）。

## 処理別 件数

| 処理 | 件数 | 備考 |
|---|---|---|
| 残留（本体のみ） | 40 | |
| コピー（本体＋integration） | 31 | Excel ベース 20＋補助 11 |
| 移動 → yaml | 64 | src/main 12（java 11＋schema 1）＋単体テスト 9＋テストデータ 43 |
| 移動 → converter | 49 | |
| 移動 → integration | 44 | YAML サブ 20＋基盤＋設定＋変換生成データ |
| rst反映 → nablarch-document | 11 | 解説書 10＋構造リファレンス 1 |
| **合計** | **239** | PR 225 ＋ コピー用既存 14 |

## 全ファイル一覧

並び順: 残留 → コピー → yaml → converter → integration → rst反映。

| # | ファイルパス | 状態 | 処理 | 先 | 区分 |
|---|---|---|---|---|---|
| 1 | `.gitignore` | M | 残留 | nablarch-testing | 本体(ビルド) |
| 2 | `docs/pr75/adrs/*（2件）` | A | 残留 | nablarch-testing | 本体(docs記録) |
| 3 | `docs/pr75/checks/*（20件）` | A | 残留 | nablarch-testing | 本体(docs記録) |
| 4 | `docs/pr75/ntf-impl-spec-list.md` | A | 残留 | nablarch-testing | 本体(docs記録) |
| 5 | `docs/pr75/steering.md` | A | 残留 | nablarch-testing | 本体(docs記録) |
| 6 | `pom.xml` | M | 残留 | nablarch-testing | 本体(ビルド) |
| 7 | `src/main/java/nablarch/test/core/reader/DataFileParser.java` | M | 残留 | nablarch-testing | 本体(キャッシュフック改変) |
| 8 | `src/main/java/nablarch/test/core/reader/ListMapParser.java` | M | 残留 | nablarch-testing | 本体(キャッシュフック改変) |
| 9 | `src/main/java/nablarch/test/core/reader/TableDataParser.java` | M | 残留 | nablarch-testing | 本体(キャッシュフック改変) |
| 10 | `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java` | M | 残留 | nablarch-testing | 本体(キャッシュフック改変) |
| 11 | `src/test/java/nablarch/test/core/entity/BeanValidationTestStrategyTest.java` | M | 残留 | nablarch-testing | 本体(テスト改変) |
| 12 | `src/test/java/nablarch/test/core/entity/CharsetTestVariationTest.java` | M | 残留 | nablarch-testing | 本体(テスト改変) |
| 13 | `src/test/java/nablarch/test/core/entity/NablarchValidationTestStrategyTest.java` | M | 残留 | nablarch-testing | 本体(テスト改変) |
| 14 | `src/test/java/nablarch/test/core/file/FixedLengthFileFragmentTest.java` | M | 残留 | nablarch-testing | 本体(テスト改変) |
| 15 | `src/test/java/nablarch/test/core/http/TestCaseInfoTest.java` | M | 残留 | nablarch-testing | 本体(テスト改変) |
| 16 | `src/test/java/nablarch/test/core/util/interpreter/QuotationTrimmerTest.java` | M | 残留 | nablarch-testing | 本体(テスト改変) |
| 17 | `src/test/resources/test-common.xml` | M | 残留 | nablarch-testing | 本体(設定改変) |
| 18 | `src/test/resources/unit-test-dbless.xml` | M | 残留 | nablarch-testing | 本体(設定改変) |
| 19 | `src/test/resources/unit-test.config` | M | 残留 | nablarch-testing | 本体(設定改変) |
| 20 | `src/test/resources/unit-test.xml` | M | 残留 | nablarch-testing | 本体(設定改変) |
| 21 | `src/test/java/nablarch/test/TestSupportTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 22 | `src/test/java/nablarch/test/Trap.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 23 | `src/test/java/nablarch/test/core/MultiResourceDataSetUpTest.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 24 | `src/test/java/nablarch/test/core/batch/BatchRequestTestSupportTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 25 | `src/test/java/nablarch/test/core/batch/DBtoDBBatchSample.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 26 | `src/test/java/nablarch/test/core/batch/DBtoDBBatchSampleTest.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 27 | `src/test/java/nablarch/test/core/batch/FileToFileBatchSampleTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 28 | `src/test/java/nablarch/test/core/batch/SimpleBatchSample.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 29 | `src/test/java/nablarch/test/core/batch/SimpleBatchSampleTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 30 | `src/test/java/nablarch/test/core/db/DbAccessTestSupportTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 31 | `src/test/java/nablarch/test/core/db/EntityTestSupportTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 32 | `src/test/java/nablarch/test/core/db/HogeTable.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 33 | `src/test/java/nablarch/test/core/db/HogeTableSsdMaster.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 34 | `src/test/java/nablarch/test/core/db/TableDataSorterTest.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 35 | `src/test/java/nablarch/test/core/db/TestTable.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 36 | `src/test/java/nablarch/test/core/entity/TestBean.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 37 | `src/test/java/nablarch/test/core/entity/TestBeanTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 38 | `src/test/java/nablarch/test/core/entity/TestEntity.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 39 | `src/test/java/nablarch/test/core/entity/TestEntityTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 40 | `src/test/java/nablarch/test/core/file/FileSupportTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 41 | `src/test/java/nablarch/test/core/file/FileSupportWithDbLessTestDataParserTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 42 | `src/test/java/nablarch/test/core/file/SimpleWriter.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 43 | `src/test/java/nablarch/test/core/http/AbstractHttpRequestTestTemplateTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 44 | `src/test/java/nablarch/test/core/http/AbstractHttpRequestTestTemplateTest2.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 45 | `src/test/java/nablarch/test/core/http/MockHttpRequestTestTemplate.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 46 | `src/test/java/nablarch/test/core/messaging/MessagingReceiveTestSupportTest.java` | 既存 | コピー | nablarch-testing ＋ integration | 本体既存資産(Excel/補助) |
| 47 | `src/test/java/nablarch/test/core/messaging/MessagingRequestTestSupportTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 48 | `src/test/java/nablarch/test/core/messaging/RequestTestingMessagingClientTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 49 | `src/test/java/nablarch/test/core/messaging/RequestTestingMessagingContextTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 50 | `src/test/java/nablarch/test/core/messaging/RequestTestingSendSyncBatchTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 51 | `src/test/java/nablarch/test/core/messaging/RequestTestingSendSyncSupportTest.java` | M | コピー | nablarch-testing ＋ integration | 本体(c継承元・改変) |
| 52 | `src/main/java/nablarch/test/core/file/TestCoreFileAdapter.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 53 | `src/main/java/nablarch/test/core/reader/StubDbInfo.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 54 | `src/main/java/nablarch/test/core/reader/TestCoreReaderAdapter.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 55 | `src/main/java/nablarch/test/core/reader/YamlTestCoreAdapter.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 56 | `src/main/java/nablarch/test/core/reader/YamlTestDataParser.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 57 | `src/main/java/nablarch/test/core/reader/yaml/InterpreterResolver.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 58 | `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 59 | `src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 60 | `src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 61 | `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 62 | `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 63 | `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 64 | `src/test/java/nablarch/test/core/file/TestCoreFileAdapterTest.java` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 65 | `src/test/java/nablarch/test/core/reader/TestCoreReaderAdapterTest.java` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 66 | `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest.java` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 67 | `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest/*（4件）` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 68 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 69 | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/*（16件）` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 70 | `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 71 | `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest/*（2件）` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 72 | `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 73 | `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest/*（13件）` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 74 | `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 75 | `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/*（3件）` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 76 | `src/test/java/nablarch/test/core/reader/yaml/YamlSectionTest.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 77 | `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java` | A | 移動 | nablarch-testing-yaml | yaml(main) |
| 78 | `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/*（5件）` | A | 移動 | nablarch-testing-yaml | yaml(test/a) |
| 79 | `src/main/java/nablarch/test/tool/converter/ConversionRequest.java` | A | 移動 | nablarch-testing-converter | converter |
| 80 | `src/main/java/nablarch/test/tool/converter/ConverterException.java` | A | 移動 | nablarch-testing-converter | converter |
| 81 | `src/main/java/nablarch/test/tool/converter/ConverterFileFilter.java` | A | 移動 | nablarch-testing-converter | converter |
| 82 | `src/main/java/nablarch/test/tool/converter/ConverterPathResolver.java` | A | 移動 | nablarch-testing-converter | converter |
| 83 | `src/main/java/nablarch/test/tool/converter/DataFormat.java` | A | 移動 | nablarch-testing-converter | converter |
| 84 | `src/main/java/nablarch/test/tool/converter/FormatHandler.java` | A | 移動 | nablarch-testing-converter | converter |
| 85 | `src/main/java/nablarch/test/tool/converter/TestDataConverter.java` | A | 移動 | nablarch-testing-converter | converter |
| 86 | `src/main/java/nablarch/test/tool/converter/TestDataFormatReader.java` | A | 移動 | nablarch-testing-converter | converter |
| 87 | `src/main/java/nablarch/test/tool/converter/TestDataFormatWriter.java` | A | 移動 | nablarch-testing-converter | converter |
| 88 | `src/main/java/nablarch/test/tool/converter/XlsFormatHandler.java` | A | 移動 | nablarch-testing-converter | converter |
| 89 | `src/main/java/nablarch/test/tool/converter/YamlFormatHandler.java` | A | 移動 | nablarch-testing-converter | converter |
| 90 | `src/main/java/nablarch/test/tool/converter/model/ColumnRowDataBlock.java` | A | 移動 | nablarch-testing-converter | converter |
| 91 | `src/main/java/nablarch/test/tool/converter/model/FieldDef.java` | A | 移動 | nablarch-testing-converter | converter |
| 92 | `src/main/java/nablarch/test/tool/converter/model/FileDataBlock.java` | A | 移動 | nablarch-testing-converter | converter |
| 93 | `src/main/java/nablarch/test/tool/converter/model/ListMapBlock.java` | A | 移動 | nablarch-testing-converter | converter |
| 94 | `src/main/java/nablarch/test/tool/converter/model/MessageDataBlock.java` | A | 移動 | nablarch-testing-converter | converter |
| 95 | `src/main/java/nablarch/test/tool/converter/model/RecordLayout.java` | A | 移動 | nablarch-testing-converter | converter |
| 96 | `src/main/java/nablarch/test/tool/converter/model/TableDataBlock.java` | A | 移動 | nablarch-testing-converter | converter |
| 97 | `src/main/java/nablarch/test/tool/converter/model/TestDataBlock.java` | A | 移動 | nablarch-testing-converter | converter |
| 98 | `src/main/java/nablarch/test/tool/converter/model/TestDataContainer.java` | A | 移動 | nablarch-testing-converter | converter |
| 99 | `src/main/java/nablarch/test/tool/converter/model/TestDataSection.java` | A | 移動 | nablarch-testing-converter | converter |
| 100 | `src/main/java/nablarch/test/tool/converter/xls/ExcelFormatConfig.java` | A | 移動 | nablarch-testing-converter | converter |
| 101 | `src/main/java/nablarch/test/tool/converter/xls/XlsFormatReader.java` | A | 移動 | nablarch-testing-converter | converter |
| 102 | `src/main/java/nablarch/test/tool/converter/xls/XlsFormatWriter.java` | A | 移動 | nablarch-testing-converter | converter |
| 103 | `src/main/java/nablarch/test/tool/converter/yaml/ValidationError.java` | A | 移動 | nablarch-testing-converter | converter |
| 104 | `src/main/java/nablarch/test/tool/converter/yaml/YamlFormatReader.java` | A | 移動 | nablarch-testing-converter | converter |
| 105 | `src/main/java/nablarch/test/tool/converter/yaml/YamlFormatWriter.java` | A | 移動 | nablarch-testing-converter | converter |
| 106 | `src/main/java/nablarch/test/tool/converter/yaml/YamlTestDataValidator.java` | A | 移動 | nablarch-testing-converter | converter |
| 107 | `src/test/java/nablarch/test/tool/converter/ConversionRequestTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 108 | `src/test/java/nablarch/test/tool/converter/ConverterExceptionTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 109 | `src/test/java/nablarch/test/tool/converter/ConverterFileFilterTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 110 | `src/test/java/nablarch/test/tool/converter/ConverterPathResolverTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 111 | `src/test/java/nablarch/test/tool/converter/DataFormatTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 112 | `src/test/java/nablarch/test/tool/converter/RoundTripTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 113 | `src/test/java/nablarch/test/tool/converter/TestDataConverterTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 114 | `src/test/java/nablarch/test/tool/converter/model/FieldDefTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 115 | `src/test/java/nablarch/test/tool/converter/model/FileDataBlockTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 116 | `src/test/java/nablarch/test/tool/converter/model/ListMapBlockTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 117 | `src/test/java/nablarch/test/tool/converter/model/MessageDataBlockTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 118 | `src/test/java/nablarch/test/tool/converter/model/ModelSealedHierarchyTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 119 | `src/test/java/nablarch/test/tool/converter/model/RecordLayoutTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 120 | `src/test/java/nablarch/test/tool/converter/model/TableDataBlockTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 121 | `src/test/java/nablarch/test/tool/converter/model/TestDataContainerTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 122 | `src/test/java/nablarch/test/tool/converter/xls/XlsFormatReaderTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 123 | `src/test/java/nablarch/test/tool/converter/xls/XlsFormatWriterTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 124 | `src/test/java/nablarch/test/tool/converter/yaml/ValidationErrorTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 125 | `src/test/java/nablarch/test/tool/converter/yaml/YamlFormatReaderTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 126 | `src/test/java/nablarch/test/tool/converter/yaml/YamlFormatWriterTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 127 | `src/test/java/nablarch/test/tool/converter/yaml/YamlTestDataValidatorTest.java` | A | 移動 | nablarch-testing-converter | converter |
| 128 | `src/test/java/nablarch/test/TestSupportYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 129 | `src/test/java/nablarch/test/core/MultiResourceDataSetUpYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 130 | `src/test/java/nablarch/test/core/batch/BatchRequestTestSupportYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 131 | `src/test/java/nablarch/test/core/batch/DBtoDBBatchSampleYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 132 | `src/test/java/nablarch/test/core/batch/FileToFileBatchSampleYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 133 | `src/test/java/nablarch/test/core/batch/SimpleBatchSampleYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 134 | `src/test/java/nablarch/test/core/db/DbAccessTestSupportYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 135 | `src/test/java/nablarch/test/core/db/EntityTestSupportYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 136 | `src/test/java/nablarch/test/core/entity/TestBeanYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 137 | `src/test/java/nablarch/test/core/entity/TestEntityYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 138 | `src/test/java/nablarch/test/core/file/FileSupportWithDbLessTestDataParserYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 139 | `src/test/java/nablarch/test/core/file/FileSupportYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 140 | `src/test/java/nablarch/test/core/http/AbstractHttpRequestTestTemplateTest2YamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 141 | `src/test/java/nablarch/test/core/http/AbstractHttpRequestTestTemplateYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 142 | `src/test/java/nablarch/test/core/messaging/MessagingReceiveTestSupportYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 143 | `src/test/java/nablarch/test/core/messaging/MessagingRequestTestSupportYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 144 | `src/test/java/nablarch/test/core/messaging/RequestTestingMessagingClientYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 145 | `src/test/java/nablarch/test/core/messaging/RequestTestingMessagingContextYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 146 | `src/test/java/nablarch/test/core/messaging/RequestTestingSendSyncBatchYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 147 | `src/test/java/nablarch/test/core/messaging/RequestTestingSendSyncSupportYamlTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 148 | `src/test/java/nablarch/test/core/reader/BasicTestDataParserTest/*（8件）` | A | 移動 | nablarch-testing-integration | 結合(c)変換生成データ |
| 149 | `src/test/java/nablarch/test/core/reader/ExcelToYamlEquivalenceTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 150 | `src/test/java/nablarch/test/core/reader/FormatAwareTestDataParser.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 151 | `src/test/java/nablarch/test/core/reader/TestDataFormat.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 152 | `src/test/java/nablarch/test/core/reader/YamlSchemaValidationTest.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 153 | `src/test/java/nablarch/test/core/reader/yaml/YamlModeTestBase.java` | A | 移動 | nablarch-testing-integration | 結合(c) |
| 154 | `src/test/resources/nablarch/test/core/db/DbAccessTestSupportYamlTest.xml` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 155 | `src/test/resources/nablarch/test/core/http/http-test-configuration-format-aware.xml` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 156 | `src/test/resources/nablarch/test/core/http/http-test-configuration-with-htmlcheck-format-aware.xml` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 157 | `src/test/resources/nablarch/test/core/http/http-test-configuration-yaml.xml` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 158 | `src/test/resources/nablarch/test/core/messaging/XmlAssertAsStringTest-yaml.xml` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 159 | `src/test/resources/nablarch/test/core/messaging/web/web-component-configuration-request-testing-yaml.xml` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 160 | `src/test/resources/unit-test-format-aware.config` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 161 | `src/test/resources/unit-test-format-aware.xml` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 162 | `src/test/resources/unit-test-yaml-dbless.xml` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 163 | `src/test/resources/unit-test-yaml.config` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 164 | `src/test/resources/unit-test-yaml.xml` | A | 移動 | nablarch-testing-integration | 結合(c)設定 |
| 165 | `docs/pr75/design/*（1件）` | A | rst反映 | nablarch-document | 本体(docs記録) |
| 166 | `docs/pr75/docs/*（10件）` | A | rst反映 | nablarch-document | 本体(docs記録) |
