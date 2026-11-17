# task-27 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `isResourceExisting` が `basePath/<クラス名>` ディレクトリの存在を答える | OK | `src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java:184`-`:185` を `new File(buildFilePath(...)).exists()` → `new File(buildContainerPath(...)).isDirectory()` に是正。入れ物パスの組み立ては `:97`-`:101`（`buildContainerPath`）。`src/main/java/nablarch/test/core/reader/YamlTestDataParser.java:112` は `YamlLoader.isResourceExisting` への委譲のまま（シグネチャ・メソッド名は不変）で、javadoc `:100`-`:110` に「判定単位は入れ物」「入れ物は `<basePath>/<入れ物名>` ディレクトリ」「読み込み単位の有無は見ない」を明記 | | |
| `getSetupTableData` の内部ガードが読み込み単位の判定に置き換わっている | OK | `YamlTestDataParser.java:126` を `if (!isResourceExisting(path, resourceName))` → `if (!YamlLoader.isDataExisting(path, resourceName))` に置換。`YamlLoader#isDataExisting`（`YamlLoader.java:200`-`:201`）は `<basePath>/<resourceName>.yaml` が通常ファイルとして存在するかを返す（Excel の `PoiXlsReader#isDataExisting`（シート単位）と同じ位置づけ）。あわせて `BasicTestDataParser.java:52`（`3c4bd2a`）と同じ debug ログを `:127`-`:128` に出す | | |
| `master_data_tool.rst:28` の挙動を押さえるテストがある | OK | `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:1009` `getSetupTableDataOnExcelMasterDataIsSilentlyEmpty`。`MasterDataSetUpper#getAllTableData`（`nablarch-testing` `3c4bd2a` の `src/main/java/nablarch/test/core/db/MasterDataSetUpper.java:199`-`:200`）が Excel 形式のマスタデータに対して渡す `<ファイル名>/<シート名>` 形式のリソース名で `getSetupTableData` を呼び、(1) 空リストが返ること (2) `writer.memlog` に `-WARN-` / `-ERROR-` / `-FATAL-` を含むログが 1 件も無いこと、を確認する。フィクスチャは `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/masterdata/master-data.yaml`（Excel 側に対応する YAML は置かない） | | |
| 是正前に落ち是正後に通るテストが存在する | OK | 4 件。`YamlLoaderTest#isResourceExisting_trueWhenReadUnitMissingButContainerExists`（`:231`）／`YamlLoaderTest#isResourceExisting_wholeNameIsContainerWhenNoSlash`（`:280`）／`YamlTestDataParserTest#isResourceExistingReturnsTrueWhenReadUnitMissingButContainerExists`（`:139`）／`YamlTestDataParserTest#getSetupTableDataReturnsEmptyWhenReadUnitNotExists`（`:933`）。是正前の実行結果 `Tests run: 239, Failures: 4, Errors: 0, Skipped: 0`、是正後 `Tests run: 239, Failures: 0, Errors: 0, Skipped: 0`（詳細は下記「Method（テストファースト）の適用」） | | |
| 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 下記「step E 変異確認」参照（変異実行 2 回。追加/変更した 15 件すべてと、複数アサーションを持つ 2 件の残りのアサーションを個別に確認） | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 239, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（着手前ベースライン 229 + 追加 10 件） | | |
| converter で落ちたテストが実測され記録されている（converter のコードは変更しない） | OK | 下記「step G converter 実測」参照。本タスク起因は 1 件（`YamlTestCoreAdapterTest.isResourceExisting_reflectsFileExistence:370`）。converter は未変更（実行前後とも `git status --short` は空） | | |

## Method（テストファースト）の適用

1. 実装を触る前に、期待する挙動を捉えるテストを先に書いた（`YamlLoaderTest` に 8 件、`YamlTestDataParserTest` に 7 件）。
2. `YamlLoader#isDataExisting` が未定義でテストがコンパイルできないため、まず **判定ロジックを変えずに** `isDataExisting` だけを追加（`isResourceExisting` は `.exists()` のまま）して赤を実測した。
   - `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
   - 結果: `Tests run: 239, Failures: 4, Errors: 0, Skipped: 0`
     - `YamlLoaderTest.isResourceExisting_trueWhenReadUnitMissingButContainerExists:236`
     - `YamlLoaderTest.isResourceExisting_wholeNameIsContainerWhenNoSlash:282`
     - `YamlTestDataParserTest.isResourceExistingReturnsTrueWhenReadUnitMissingButContainerExists:141`
     - `YamlTestDataParserTest.getSetupTableDataReturnsEmptyWhenReadUnitNotExists:935`（Given の `assertTrue(sut.isResourceExisting(...))` で失敗）
3. `isResourceExisting` を入れ物単位（`buildContainerPath` + `isDirectory()`）に、`getSetupTableData` の内部ガードを `YamlLoader.isDataExisting` に是正し、4 件が通ることを確認した。
   - `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 239, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`

## `resourceName` に `/` が無い場合の扱い

入れ物名は `resourceName` の**最後の `/` より前**の部分とし、`/` を含まない場合は `resourceName` 全体を入れ物名として扱う（`YamlLoader.java:97`-`:101`）。javadoc は `YamlLoader.java:165`-`:183`（`isResourceExisting`）と `YamlTestDataParser.java:100`-`:110` に記載。

- Excel（`PoiXlsReader#splitLastResourceName`、`3c4bd2a` の `PoiXlsReader.java:304`）は `/` が無いと `name.substring(0, -1)` で `StringIndexOutOfBoundsException` になるが、**Excel の実装には合わせていない**。合わせる先は解説書 `component.rst:313` が定める `<ファイル名>/<読み込み単位の名前>` という形であり、その形から外れた入力に対しては例外ではなく `false`／`true` を返す全域的な判定にした。
- 「入れ物名を空にして `basePath` 自身を見る」案は採らなかった。`basePath` はほぼ常に存在するため `TestSupport#getPathResourceExisting`（`3c4bd2a` の `TestSupport.java:308`-`:315`）が最初の候補パスを無条件に返してしまい、判定として機能しなくなる。
- 読み込み単位（`isDataExisting`）は `/` の有無にかかわらず `<basePath>/<resourceName>.yaml` を見る。`MasterDataSetUpper#getAllTableData`（`3c4bd2a` の `MasterDataSetUpper.java:193`）が YAML 形式のマスタデータを `/` の無いリソース名で問い合わせるため、この経路を `YamlTestDataParserTest#getSetupTableDataLoadsMasterDataFileWithoutSlash`（`:978`）で押さえた。

## step E 変異確認

### 変異 1 — 追加/変更した 15 件の主アサーションを一括で崩す

期待値をすべて反転（`is(true)`↔`is(false)`、`assertTrue`↔`assertFalse`、`is(0)`→`is(1)`、`"0000009001"`→`"9999999999"`）して実行。

- `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
- 結果: `Tests run: 239, Failures: 15, Errors: 0, Skipped: 0` — 崩した 15 件が**過不足なく**失敗した。

| 崩した期待値 | 落ちたテスト |
|---|---|
| `isResourceExisting(DIR, "YamlLoaderTest/simple")` → `is(false)` | `YamlLoaderTest.isResourceExisting_trueWhenContainerDirectoryExists:217` |
| `isResourceExisting(DIR, "YamlLoaderTest/noSuchFile")` → `is(false)` | `YamlLoaderTest.isResourceExisting_trueWhenReadUnitMissingButContainerExists:236` |
| `isResourceExisting(DIR, "NoSuchContainer/simple")` → `is(true)` | `YamlLoaderTest.isResourceExisting_falseWhenContainerDirectoryNotExists:251` |
| `isResourceExisting(DIR, "YamlLoaderTest.java/simple")` → `is(true)` | `YamlLoaderTest.isResourceExisting_falseWhenContainerIsNotDirectory:266` |
| `isResourceExisting(DIR, "YamlLoaderTest")` → `is(false)` | `YamlLoaderTest.isResourceExisting_wholeNameIsContainerWhenNoSlash:282` |
| `isDataExisting(DIR, "YamlLoaderTest/simple")` → `is(false)` | `YamlLoaderTest.isDataExisting_trueWhenYamlFileExists:298` |
| `isDataExisting(DIR, "YamlLoaderTest/noSuchFile")` → `is(true)` | `YamlLoaderTest.isDataExisting_falseWhenYamlFileNotExists:313` |
| `isDataExisting(DIR, "YamlLoaderTest")` → `is(true)` | `YamlLoaderTest.isDataExisting_falseWhenOnlySameNamedDirectoryExists:328` |
| `assertTrue(...existingForTest)` → `assertFalse` | `YamlTestDataParserTest.isResourceExistingReturnsTrueWhenContainerExists:120` |
| `assertTrue(...setUpDb)` → `assertFalse` | `YamlTestDataParserTest.isResourceExistingReturnsTrueWhenReadUnitMissingButContainerExists:141` |
| `assertFalse(...NoSuchTestClass/existingForTest)` → `assertTrue` | `YamlTestDataParserTest.isResourceExistingReturnsFalseWhenContainerNotExists:156` |
| `result.size()` `is(0)` → `is(1)`（読み込み単位なし） | `YamlTestDataParserTest.getSetupTableDataReturnsEmptyWhenReadUnitNotExists:941` |
| `result.size()` `is(0)` → `is(1)`（入れ物なし） | `YamlTestDataParserTest.getSetupTableDataReturnsEmptyWhenContainerNotExists:959` |
| `PK_COL1` `"0000009001"` → `"9999999999"` | `YamlTestDataParserTest.getSetupTableDataLoadsMasterDataFileWithoutSlash:988` |
| `result.size()` `is(0)` → `is(1)`（Excel マスタデータ） | `YamlTestDataParserTest.getSetupTableDataOnExcelMasterDataIsSilentlyEmpty:1018` |

### 変異 2 — 複数アサーションを持つ 2 件の、変異 1 で報告されなかった側を崩す

- `YamlLoaderTest#isResourceExisting_wholeNameIsContainerWhenNoSlash` の 2 番目 `isResourceExisting(DIR, "NoSuchContainer")` → `is(true)`
- `YamlTestDataParserTest#getSetupTableDataOnExcelMasterDataIsSilentlyEmpty` のログ検証 `not(containsString("-WARN-"))` → `not(containsString("-DEBUG-"))`（ログ検証ループが実際にメッセージを走査していること、＝空振りでないことの確認）

実行と結果:

- `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
- 結果: `Tests run: 239, Failures: 2, Errors: 0, Skipped: 0`
  - `YamlLoaderTest.isResourceExisting_wholeNameIsContainerWhenNoSlash:283`
  - `YamlTestDataParserTest.getSetupTableDataOnExcelMasterDataIsSilentlyEmpty:1022`

変異はすべて元に戻し、最終実行で `Tests run: 239, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` を確認済み。

## step G converter 実測

`/home/tie303177/work/nablarch/nablarch-testing-converter`（`60d9a2d`、**未変更**）。本モジュールを `.m2` に配置してから `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` を実行した。

### 是正後（本タスクの変更を `install` した状態）

`Tests run: 605, Failures: 5, Errors: 0, Skipped: 2`

### ベースライン（本タスクの変更を `git stash` して `install` した状態＝ HEAD `0602b39`）

`Tests run: 605, Failures: 4, Errors: 0, Skipped: 2`

- `YamlFormatReaderInvalidInputTest.dropsAllRowsWhenFirstRowOfTableIsEmptyObject:601`
- `YamlFormatReaderInvalidInputTest.keepsRowCountButLosesValuesWhenFirstRowOfListMapIsEmptyObject:628`
- `YamlFormatReaderScalarTest.readsEmptyStringAsIs:505->readValue:106->readValueLine:172`
- `YamlFormatReaderScalarTest.readsEmptyStringAsIsInListMapPath:584->readListMapValue:192`

この 4 件は着手時 HEAD の時点ですでに落ちており（task #26 の空行判定の是正に追随していない）、本タスク起因ではない。

### 本タスク起因で新たに落ちた 1 件

- テスト: `nablarch.test.core.reader.YamlTestCoreAdapterTest.isResourceExisting_reflectsFileExistence`（`src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest.java:364`-`:370`）
- 失敗メッセージ:

  ```
  java.lang.AssertionError:

  Expected: is <false>
       but: was <true>
  	at nablarch.test.core.reader.YamlTestCoreAdapterTest.isResourceExisting_reflectsFileExistence(YamlTestCoreAdapterTest.java:370)
  ```

- 理由: converter の `YamlTestCoreAdapter#isResourceExisting`（`src/main/java/nablarch/test/core/reader/YamlTestCoreAdapter.java:102`）は `YamlLoader.isResourceExisting` へ透過委譲しており、テストは `assertThat(sut.isResourceExisting(DIR, "YamlTestCoreAdapterTest/noSuchFile"), is(false))` と**読み込み単位**（`noSuchFile.yaml` の不存在）を期待している。是正後は入れ物単位の判定になり、入れ物ディレクトリ `YamlTestCoreAdapterTest` が存在するため `true` を返す。converter 側が読み込み単位の判定を意図しているなら `YamlLoader.isDataExisting` に切り替えるのが対応方針になるが、**本タスクでは converter を変更していない**。

- converter リポジトリの `git status --short` は実行前後とも空（`target/` は `.gitignore` 済み）。

## 参考: 触っていないもの

- 解説書（`nablarch-document`）・`nablarch-testing`・`nablarch-testing-converter` は未変更。
- `pom.xml` / `argLine` / `ntf-testdata-yaml-schema.json` は未変更。
- 既存の公開メソッドの削除・シグネチャ変更は無し（`YamlLoader#isDataExisting` の**追加**のみ）。

## Overall Verdict

- Self-check: OK

## コーディネーター独立レビュー

Step 4 では4観点レビューを回さない（指示書 §7）。コーディネーターがコミット済み差分を独立に読み、ビルドを自分で実行し、参照点の一次情報を自分で確認して検証した。

| 観点 | 判定 | 根拠 |
|---|---|---|
| 差分がタスクの範囲に収まっている | OK | `git diff 0602b39..b510075` は `YamlLoader.java`・`YamlTestDataParser.java`・`YamlLoaderTest.java`・`YamlTestDataParserTest.java`・新規フィクスチャ `masterdata/master-data.yaml` の5件。スキーマ・`pom.xml`・解説書・`nablarch-testing`・`nablarch-testing-converter` への書き込みなし |
| 入れ物単位の実装が解説書と一致 | OK | `YamlLoader#isResourceExisting` が `new File(buildContainerPath(...)).isDirectory()`。解説書 `5b5c91e` の `component.rst:313`「YAML 形式では `<ディレクトリ>/<ファイル名>/<読み込み単位の名前>.yaml` が読み込まれる」＝入れ物は `<basePath>/<ファイル名>` ディレクトリ、と一致 |
| 読み込み単位ガードが Excel の同位置と同じ役割 | OK | `YamlTestDataParser#getSetupTableData` が `YamlLoader.isDataExisting`（`<path>/<resourceName>.yaml` の `isFile()`）で判定し、`BasicTestDataParser.java:52`（`3c4bd2a`）と同じ debug ログを出して空リストを返す。`LOGGER` は `YamlTestDataParser.java:45` に既存 |
| 既存の公開 API を壊していない | OK | `isResourceExisting` のシグネチャ不変。`isDataExisting` は**追加**。削除・シグネチャ変更なし |
| `/` 無し resourceName の扱い | OK | resourceName 全体を入れ物名とし javadoc に明記。Excel の `PoiXlsReader#splitLastResourceName`（`3c4bd2a` の `:304`-`:313`）は `/` 無しで `substring(0, -1)` となり `StringIndexOutOfBoundsException` になる（コーディネーターが実物で確認）ため合わせていない。`MasterDataSetUpper.java:193`（`3c4bd2a`）が非 Excel 経路で `/` 無しの `fileNameWithoutSuffix` を `getSetupTableData` に渡すことも実物で確認済み |
| `master_data_tool.rst:28` の挙動を押さえている | OK | `getSetupTableDataOnExcelMasterDataIsSilentlyEmpty` が (1) 0件 (2) `-WARN-`/`-ERROR-`/`-FATAL-` が1件も出ないこと の両方を検証。`MasterDataSetUpper.java:196`-`:201`（`3c4bd2a`）が Excel 経路で渡す `<ファイル名>/<シート名>` 形式と一致 |
| 変異確認が実施されている | OK | 変異1（追加/変更15件の主アサーション一括反転 → `Failures: 15`）・変異2（複数アサーションを持つ2件の残り側 → `Failures: 2`）。ログ検証が空振りでないことも確認済み |
| ビルド（コーディネーター自身の実行） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 239, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`（2026-08-26 コーディネーターが独立実行） |
| converter を変更していない | OK | converter リポジトリの `git status --short` が空（コーディネーターが確認） |

### 指示書の想定を超えた観測（#33 の報告へ引き継ぐ）

指示書 2-2 は converter で落ちるテストを `YamlTestCoreAdapterTest#isResourceExisting_reflectsFileExistence` 1件と見込んでいたが、**#26（空行判定の是正）起因で `YamlFormatReaderInvalidInputTest` 2件・`YamlFormatReaderScalarTest` 2件が着手時 HEAD `0602b39` の時点で既に落ちている**と実測された（本タスクの実装担当による測定）。コーディネーターは未再現。**#33 step B2 で、Step 4 着手前（`ab0064e`）を基準にした帰属実測をやり直して確定させる。** converter は直さない。

### 後始末（コーディネーター実施）

`tmp/`（6月22日付の空ディレクトリ・未追跡・未 ignore）を `rmdir` で削除した。本セッション由来ではないが、指示書の後始末条件「`tmp/` を残さないこと」に従った。

## Overall Verdict（コーディネーター）

- コーディネーター独立レビュー: OK
- Ready to check off: Yes
