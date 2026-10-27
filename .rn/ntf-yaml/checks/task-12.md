# task-12 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `record_type` の値が `FW_HEADER` のレコードが、`messages` 経路・送信同期メッセージ経路のいずれでも読み飛ばされずフラグメントとして構築される | OK | ユニット経路: `YamlFileBuilderTest#buildFragmentsForMessage_fwHeaderRecordTypeIsNotSkipped`（`src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java:543`）・`#buildFragmentsForMessage_fwHeaderRecordWithoutLength`（同 :582、length 未指定 × FW_HEADER）・`#buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped`（同 :624）が、公開 API（`DataFile#createLayout()` のレコード定義件数・`getTypeName()`、`DataFile#toDataRecords()` の値）だけで、2 レコードから 2 フラグメントが構築され record_type が `"default"` に固定されること、FW_HEADER レコードの値行（`requestId=0000000001`）が電文本文としてレンダリングされることを検証（リフレクション不使用）。公開 API 経路: `YamlMessageBuilderTest#buildMessagePool_fwHeaderRecordTypeIsNotSkipped`（`src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java:366`）と `YamlTestDataParserTest#getMessageWithoutCache_expectedRequestHeaderMessages`（`src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:679`）が、`record_type: FW_HEADER` を記述した fixture から `RequestTestingMessagePool#getExpectedMessageList()` で期待電文 1 件を取得し `requestId=0000000001`/`userId=testUser01`/`resendFlag=0`/`resultCode=0000` の実値まで検証。変異実験（`buildFragmentsInternal` に `if (messaging && "FW_HEADER".equals(recordType)) { continue; }` を復活）で `Tests run: 167, Failures: 5` となり、上記 5 件がすべて RED（`YamlTestDataParserTest:691`、`YamlFileBuilderTest:556/599/637`、`YamlMessageBuilderTest:378`）。分岐を除去して復元後に再度 `BUILD SUCCESS`、`git diff -- src/main/` は Javadoc 差分のみであることを確認。**修正ラウンド2で公開 API 経路の担保を追加**: `YamlTestDataParserTest#getMessage_fwHeaderRecordTypeIsNotSkipped`（`src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:806`、`messages` セクション＝`useFwHeader=true` 経路で `fw_header:` マップと `record_type: FW_HEADER` を併記した fixture から本文 2 件・`fw_header` の `requestId`/`userId` が本文に混ざらないことを検証）・`#getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped`（同 :850、`getSendSyncMessage` 公開 API 経由で FW_HEADER レコードの値行を含む 3 件を検証）。コーディネータが独立に変異実験を再実行（`if (messaging && "FW_HEADER".equals(recordType)) { continue; }` を復活）した結果 `Tests run: 173, Failures: 8` で 4 テストクラスにまたがり RED（`YamlTestDataParserTest:692/813/860/894`、`YamlFileBuilderTest:558/601/639`、`YamlMessageBuilderTest:433`）、復元後 `md5sum` 一致 | OK | 上記の変異実験をコーディネータが自ら実行し、公開 API 経路（`YamlTestDataParserTest`）3 件を含む 8 件が RED になることを実測 |
| `src/main` に `record_type` の値を特別扱いする分岐・定数が残っていない | OK | `grep -rn "FW_HEADER" src/main/java/` の残存ヒットは `FIELD_FW_HEADER = "fw_header"`（`YamlSection.java:70`）と `YamlMessageBuilder.java:15,91` のみで、いずれも `fw_header:` マップキーの定数・参照。`record_type` の値を判定する分岐は `YamlFileBuilder.java` から削除済み（定数 `YamlSection.FW_HEADER_RECORD_TYPE` および対応する static import も削除。`git diff` で確認）。あわせて `YamlFileBuilder#buildFragmentsForMessage` の Javadoc（`src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java:127-133`）を、`fw_header:` を使う経路（`messages`）と使わない経路（`expected_request_header_messages` 等、スキーマ `ntf-testdata-yaml-schema.json:223`「fw_header は使用しない」／`YamlMessageBuilder.java:90-92` の `useFwHeader` 引数）を区別した記述へ修正 | OK | `grep -rn "FW_HEADER" src/main/java/` の残存ヒットが `fw_header:` マップキー定数（`YamlSection.java:70`）とその参照 2 か所のみであることをコーディネータが再確認。修正ラウンド2の `src/main` 差分は `YamlFileBuilder`（Javadoc 8 行）と `YamlMessageBuilder`（コメント 4 行）のみで実装ロジックの変更ゼロ（`git diff src/main/` で確認） |
| FW 制御ヘッダを `fw_header:` マップから取得する経路が従来どおり動作する（既存の `fw_header` 関連テストが GREEN） | OK | `YamlMessageBuilderTest` 全 37 件 PASS（`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`）。`buildMessagePool_withFwHeader`（requestId/userId/resendFlag/resultCode の実値検証）、`buildMessagePool_fwHeaderMapReadableWithoutHeaderRecord`、`buildMessagePool_customFwHeaderFields`、`buildMessagePool_emptyFwHeaderRows`、`buildMessagePool_noFwHeaderMapReturnsEmptyFwHeader` を含む。実装 `YamlMessageBuilder#convertFwHeader` は `map.get(FIELD_FW_HEADER)` のみを読んでおり（`YamlMessageBuilder.java:91`）、`records` は参照しない | OK | 修正ラウンド2で `fw_header:` と本文の分離を固定するテストを**追加**（既存テストの置き換えではない）: `YamlMessageBuilderTest#buildMessagePool_fwHeaderMapItemsAreNotBodyFragments`（`YamlMessageBuilderTest.java:365`）。実装エキスパートが `buildMessageBodyFile` に `fw_header` の先頭 1 件を本文フラグメントとして注入する変異を入れ、本テストと `YamlTestDataParserTest:813` が RED になることを確認（復元済み） |
| `mvn clean test` が BUILD SUCCESS（Skipped は既存 `@Ignore` の 4 件のみ、Failures/Errors 0） | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test` → `Tests run: 167, Failures: 0, Errors: 0, Skipped: 4` / `BUILD SUCCESS`。Skipped 4 件は既存 `@Ignore`（`YamlTestDataParserTest.java:398`、`YamlTableDataBuilderTest.java:147,421,871`）と一致（`grep -rn "@Ignore" src/test/java` で 4 件のみ。復活させたものはない） | OK | 修正ラウンド2後にコーディネータが `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test` を自ら実行し `Tests run: 173, Failures: 0, Errors: 0, Skipped: 4` / `BUILD SUCCESS`。テスト数は 167 → 173（6 件追加）、Skipped は 4 件のまま増減なし |
| `ntf-testdata-yaml-schema.json` の検証ルール構造（`type` / `enum` / `required`）が変更されていない | OK | `git status --short` に `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` が現れず、ファイル自体を一切変更していない（コミット `0b53910` の変更ファイル一覧にも含まれない） | OK | 修正ラウンド2後も `git status --short` に同ファイルが現れないことをコーディネータが確認。QA レビューが `git diff --stat c914351..HEAD -- src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` が空であることと、`enum` 制約がファイル種別（:170）のみで `record_type` に予約値がないことを併せて確認 |
| `nablarch-testing` 本体が変更されていない | OK | `/home/tie303177/work/nablarch/nablarch-testing` で `git status --short` の出力が空（クリーン）。`nablarch-testing-converter` には本タスク開始前から `src/test/java/nablarch/test/tool/converter/yaml/YamlFormatReaderInvalidInputTest.java` の未コミット変更が存在するが、本タスクでは同リポジトリへ一切書き込んでいない | OK | コーディネータが `/home/tie303177/work/nablarch/nablarch-testing` で `git status --short` が空・HEAD が `fdf55d4` であることを確認。`nablarch-testing-converter` の未コミット変更（`.rn/ntf-test-data-converter/coverage/inventory.md`）は同リポジトリの別タスク（#25.5）に関する記述で、本タスクの成果物とは無関係（差分内容で判定。当環境では読み取りのたびに mtime が現在時刻へ前進するため mtime は判定材料にならないことを実測） |

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective (checks the right thing, not just "passed") | pass | Completion criteria 6 項目すべて OK。5 種の変異（FW_HEADER スキップ復活／メッセージ系の `record_type` 固定撤去／ファイル系を常に `default`／`addValueWithId`→`addValue`／メッセージ系の length 未指定 `"-"`→`""`）を投入し全 kill を確認、`md5sum` で復元も確認。指摘4点はすべて修正ラウンド2で対応済み: (1) `messages` セクションに FW_HEADER レコードの fixture が無い→`fwHeaderRecordType001` を追加、(2) 送信同期の公開 API 経路が盲目→`getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped` を追加、(3) 旧形式 YAML 残存時の `record-length differs.` が未固定→`getMessage_legacyFwHeaderRecordCausesRecordLengthMismatch` を追加、(4) 送信同期連番の assert が実装出力の追認に近い→フラグメント単位で 1 始まりに戻る現行挙動が消費側で無害であることを一次情報で確認のうえ現行挙動として固定。QA が 1 回だけ観測した環境ノイズ（初回 `mvn clean test` が変異1と同一の 5 件 RED、以降 6 回連続 SUCCESS）は再現せず、コーディネータの再実行でも発生していない |

## Expert Reviews (axes the task needs)

### Craft Expert (coding)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | pass | Completion criteria 6/6。指摘のうち Valid 判定分を修正ラウンド2で対応: テストクラス Javadoc の事実誤り（package-private を「公開する API」と記述）、`messages` 経路の担保の純減、不要な `throws Exception`、`messageRecord()` ヘルパの長さ暗黙結合（長さを明示引数化＋複数行オーバーロード追加）、`buildFragmentsForSendSync` Javadoc の「照合に使う」という不正確な記述 |
| Consistency with existing style | pass | 追加テストは既存の GWT コメント形式・`createLayout().getRecords()` 流儀に沿う。`MessagePool#getFwHeader` が package-private でテストが別パッケージ（`nablarch.test.core.reader`）にあるためリフレクションを 1 か所使うが、同クラス内の既存 18 か所と同じ流儀 |

### Verification Expert (test)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked (tests run / claims verified / flow traced) | pass | 変異実験で公開 API 経路 2 件を含む 5 件が RED になることを確認、作業ツリー復元も確認。`mvn clean test` → `Tests run: 167, Failures: 0, Errors: 0, Skipped: 4` |
| Coverage (edge cases / claims / steps) | pass（ラウンド2で穴を塞いだ） | 報告された 6 つの穴のうち 3 つを修正: (1) 送信同期の連番が変異に耐えない（`String.valueOf(rowNo)`→`"1"` で全 GREEN のままだった）→`buildFragmentsForSendSync_rowNoIsIncrementedPerRow`（`YamlFileBuilderTest.java:666`）と公開 API 側の連番 assert を追加し、コーディネータが同変異を再投入して 2 件 RED を実測、(2) 通常ファイル経路で `record_type: FW_HEADER` がそのまま採用される非対称性が未担保→`buildFileList_fwHeaderRecordTypeIsUsedAsIsInFileRoute`（同 :713）を追加、(3) 送信同期の公開 API 経路が未担保→上記のとおり追加。残り 3 つのうち「メッセージ系の `record_type` 未指定」「`records: []`」は本タスクの不具合と独立した既存の網羅漏れ、「`record_type: ""`（空文字）」は仕様が定めていない領域でテストによる固定を見送った（誤った仕様を固めるため） |

## Overall Verdict

- Self-check: OK
- QA: pass
- Craft expert: pass
- Verification expert: pass
- Ready to check off: Yes

---

## TDD の適用（RED → GREEN）

先に 2 本のテストを追加し、実装を変更する前に実行して赤を確認した。

コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test -Dtest=YamlFileBuilderTest`

出力（要点）:

```
[ERROR] Tests run: 18, Failures: 2, Errors: 0, Skipped: 0 - in nablarch.test.core.reader.yaml.YamlFileBuilderTest
[ERROR] buildFragmentsForMessage_fwHeaderRecordTypeIsNotSkipped
java.lang.AssertionError:
FW_HEADER レコードも読み飛ばされず 2 フラグメント構築されること
Expected: is <2>
     but: was <1>
[ERROR] buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped
java.lang.AssertionError:
FW_HEADER レコードも読み飛ばされず 2 フラグメント構築されること
Expected: is <2>
     but: was <1>
BUILD FAILURE
```

その後 `YamlFileBuilder#buildFragmentsInternal` のスキップ分岐を削除し（GREEN）、全体で `Tests run: 166, Failures: 0, Errors: 0, Skipped: 4` / `BUILD SUCCESS`。

### レビュー指摘反映後の変異実験（公開 API 経路の担保確認）

指摘（公開 API 経路に回帰テストがない）を受けてテストを追加・強化したあと、
`buildFragmentsInternal` にスキップ分岐を一時的に復活させて実行した。

コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test`

一時的に挿入したコード:

```java
String recordType = toStr(record.get(FIELD_RECORD_TYPE));
if (messaging && "FW_HEADER".equals(recordType)) {
    continue;
}
```

出力（要点）:

```
[ERROR]   YamlTestDataParserTest.getMessageWithoutCache_expectedRequestHeaderMessages:691 FW_HEADER レコードも読み飛ばされず期待電文が 1 件取得できること
[ERROR]   YamlFileBuilderTest.buildFragmentsForMessage_fwHeaderRecordTypeIsNotSkipped:556 FW_HEADER レコードも読み飛ばされず 2 フラグメント構築されること
[ERROR]   YamlFileBuilderTest.buildFragmentsForMessage_fwHeaderRecordWithoutLength:599 length 未指定の FW_HEADER レコードも 1 フラグメント構築されること
[ERROR]   YamlFileBuilderTest.buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped:637 FW_HEADER レコードも読み飛ばされず 2 フラグメント構築されること
[ERROR]   YamlMessageBuilderTest.buildMessagePool_fwHeaderRecordTypeIsNotSkipped:378 record_type: FW_HEADER のレコードもフラグメントになること
[ERROR] Tests run: 167, Failures: 5, Errors: 0, Skipped: 4
BUILD FAILURE
```

修正前（`0b53910` 時点）は落ちるのが `YamlFileBuilderTest` の新規 2 件だけで、
公開 API 経路（`YamlTestDataParserTest` / `YamlMessageBuilderTest`）は全 GREEN のままだった。
反映後は公開 API 経路の 2 件も RED になり、担保が効いていることを確認した。

分岐を除去して復元後、`git diff -- src/main/` の差分が `buildFragmentsForMessage` の Javadoc のみであること、
および `mvn clean test` が `Tests run: 167, Failures: 0, Errors: 0, Skipped: 4` / `BUILD SUCCESS` に戻ることを確認した。

期待値は「今の実装の出力」ではなく解説書
（`nablarch-document` ブランチ `ntf-yaml-support` の
`ja/development_tools/testing_framework/implementation/testdata_notation.rst:1302` important ディレクティブ:
「``record_type`` に特別な予約値はない」、および :1296 付近「``record_type`` の値は…常に ``"default"`` に置き換えられる」）
に合わせて記述した。

## カバレッジ（JaCoCo）

本プロジェクトの `mvn clean test` は `target/site/jacoco/` を生成しない
（親 POM `nablarch-parent` の jacoco-maven-plugin の実行は `instrument` と
`restore-instrumented-classes` のみで `report` ゴールがない。`mvn help:effective-pom` で確認）。
そのためオフライン計測で生成されるプロジェクト直下の `jacoco.exec`（`.gitignore:20` で除外済み）を退避し、
`mvn clean compile -Djacoco.skip=true` で非 instrument クラスを再生成したうえで
`mvn org.jacoco:jacoco-maven-plugin:0.8.8:report -Djacoco.dataFile=<退避先>` によりレポートを生成した。

| クラス | 行カバレッジ | 分岐カバレッジ |
|---|---|---|
| `YamlFileBuilder` | 83/84 (98.8%) | 41/42 (97.6%) |
| `YamlMessageBuilder` | 58/58 (100%) | 30/30 (100%) |
| `YamlSection` | 35/35 (100%) | 32/32 (100%) |

未カバーは `YamlFileBuilder.java:224` の `continue` 1 行のみ（`rows:` の要素が List でない場合の防御的ガード。
コード上「Java 言語仕様上この分岐は通常到達不能」とコメント済みの箇所であり、本タスクの変更対象外）。
レビュー指摘反映後も同じ数値であることを再計測して確認した。

## テストデータの `record_type: FW_HEADER` を残す判断

`0b53910` では既存テストデータの `record_type: FW_HEADER` を `record_type: default` へ書き換えたが、
レビュー指摘を受けて **`FW_HEADER` のまま残す**方針へ戻した。

理由は、解説書のとおり `record_type` に特別な予約値はなく
（`testdata_notation.rst:1302`「``record_type`` に特別な予約値はない」、
`testdata_notation.rst:1296`「任意の値を装飾的に記述できるが、実行時の挙動には影響しない」）、
スキーマの `record_type` にも `enum` 制約がないため `FW_HEADER` は現仕様でも合法な装飾値であり、
実データ経路に `FW_HEADER` を残しておくことが「特別扱いしない」ことの回帰ガードになるため。

- `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/messageData.yaml:24`
- `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/messageData.yaml:90`
- `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest/schemaFullCoverage.yaml:196`

なお `messages` セクションのテストデータは `fw_header:` マップ記法で記述されており、
`record_type: FW_HEADER` は使っていない。

## リフレクションから公開 API への置き換え

`YamlFileBuilderTest` から `DataFile#all` / `DataFileFragment#recordType` / `DataFileFragment#values` を
リフレクションで覗くヘルパ（`fragmentsOf` / `recordTypeOf` / `valuesOf`）と、
`buildFileList_multipleRecordLayouts` のインラインリフレクションを撤去し、
公開 API（`DataFile#createLayout()`・`RecordDefinition#getTypeName()`・`FieldDefinition#getPosition()`・
`DataFile#toDataRecords()`）による検証へ統一した。同クラス内に既にあった
`createLayout().getRecords()` 流儀（現在の `YamlFileBuilderTest.java:283` / `:392`）と揃う。

`buildFileList_multipleRecordLayouts` の fixture（`YamlFileBuilderTest/fileData.yaml` の `multiRecord`）は、
HEADER が 6 バイト・DATA が 40 バイトでレコード長が不一致だったため、
`FixedLengthFile#createLayout()` / `#toDataRecords()` が
`IllegalStateException: record-length differs.`（`nablarch-testing` の `FixedLengthFile.java:111`）を投げ、
公開 API では一切検証できなかった。固定長ファイルは全レコードレイアウトのレコード長が一致している必要があるため、
HEADER に `FILLER`（半角 34 バイト）を追加して DATA と同じ 40 バイトに揃えた。

## テストクラス Javadoc の整合（`YamlFileBuilderTest`）

追加した `buildFragmentsForMessage` / `buildFragmentsForSendSync` のテストは
`YamlFileBuilderTest` に置いたまま、クラス Javadoc をファイル系メソッド限定の宣言から
「`YamlFileBuilder` のテストクラス（ファイル系＋メッセージ系レコードレイアウト組み立て）」へ改めた。

`YamlMessageBuilderTest` へ移す案も検討したが、テストクラスは SUT と 1 対 1 に対応させるのが
本プロジェクトの既存の構成（`YamlLoaderTest` / `YamlSectionTest` / `YamlTableDataBuilderTest` …）であるため、
クラス配置ではなく Javadoc の側を実態に合わせた。

なお修正ラウンド2で、この Javadoc に事実誤りがあったことが 3 名のレビュアー全員から指摘された。
`buildFragmentsForMessage` / `buildFragmentsForSendSync` は「`YamlFileBuilder` が公開する API」ではなく
package-private であり（`src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java:139,158`）、
同一パッケージの本テストクラスからのみ直接呼び出せる。この点を明示し、
利用者が実際に通る公開 API 経路の担保は `YamlTestDataParserTest`、
`YamlMessageBuilder` 経由の結合検証は `YamlMessageBuilderTest` が担う旨をあわせて記述する形へ修正した。

## 修正ラウンド2（レビュー指摘の反映）

QA・Craft・Verification の 3 レビューはいずれも Completion criteria 6/6 を pass としたうえで、
担保の穴とドキュメントの事実誤りを指摘した。Valid と判定した指摘を以下のとおり修正した。

| # | 指摘 | 対応 |
|---|---|---|
| 1 | テストクラス Javadoc が package-private メソッドを「公開する API」と記述（3 名一致） | 上記のとおり修正 |
| 2 | `messages` 経路で `fw_header:` の項目が本文フラグメントにならない担保が、旧テスト転用により純減 | `YamlMessageBuilderTest#buildMessagePool_fwHeaderMapItemsAreNotBodyFragments`（`YamlMessageBuilderTest.java:365`）を**追加**（既存テストは残置） |
| 3 | `messages` セクションに `record_type: FW_HEADER` の fixture が 1 件も無く criterion 1 の文言を満たしきっていない | `YamlTestDataParserTest/messageData.yaml` に `fwHeaderRecordType001` を追加し `YamlTestDataParserTest.java:806` で検証 |
| 4 | 送信同期の FW_HEADER 非スキップが公開 API 経路（`getSendSyncMessage`）で未担保 | 同 YAML の `response_body_messages` に `fwHeaderSync` を追加し `YamlTestDataParserTest.java:850` で検証 |
| 5 | 送信同期の連番が変異に耐えない（`String.valueOf(rowNo)` → `"1"` で全 GREEN） | `YamlFileBuilderTest.java:666` と公開 API 側に連番の assert を追加 |
| 6 | 通常ファイル経路で `record_type: FW_HEADER` がそのまま採用される非対称性が未担保 | `YamlFileBuilderTest.java:713` を追加 |
| 7 | `buildFragmentsForSendSync` の Javadoc「`RequestTestingMessagingProvider` が要求/応答電文の照合に使う」が不正確 | 一次情報で確認のうえ修正（下記） |
| 8 | 旧形式 YAML が `messages` に残った場合の挙動が未固定 | `legacyFwHeaderRecord001` fixture と `YamlTestDataParserTest.java:890` を追加 |

### #7 の一次情報

`FIRST_FIELD_NO` は電文の照合には使われない。消費側は突合前に `remove()` しており、
期待電文と実電文はリストの位置（インデックス）で対応付ける。取り出した連番は失敗時メッセージ
（`test no=[...]`）にのみ使われる。

- `/home/tie303177/work/nablarch/nablarch-testing/src/main/java/nablarch/test/core/messaging/RequestTestingMessagingProvider.java:344-373`（インデックスループ。`Assertion.assertEquals` の前に `remove()`。連番の用途は :355 / :365 / :371 のメッセージのみ）
- 同 `nablarch/test/core/messaging/RequestTestingMessagingClient.java:379-386`（同じくインデックスループで、:385-386 で `remove()`。ヘッダ側は :380/:383-384 がコメントアウトされており本文側のみ有効）
- `/home/tie303177/work/nablarch/nablarch-testing/src/main/java/nablarch/test/core/reader/SendSyncMessageParser.java:134`（Excel 経路が値行先頭の No 列を切り離している箇所: `currentFragment.addValueWithId(temp, temp.remove(NO_COLUMN_NUMBER));`）

この事実に基づき `YamlFileBuilder.java` の Javadoc と `YamlMessageBuilder.java:170-176` のコメントを修正した。
`src/main` の差分はこの 2 件のドキュメントのみで、実装ロジックの変更はゼロ（`git diff src/main/` で確認）。

### 修正しない と判断した指摘

| 指摘 | 判断理由 |
|---|---|
| `boolean messaging` / `withId` を列挙型 `FragmentMode` へ置き換える | 本タスクのスコープ外。Completion criteria は不具合修正を定めており、引数受け渡し構造の再設計は含まない |
| `MessagePool` に対するリフレクション参照 18 か所の解消 | 本差分で変更していない既存事情 |
| 旧形式検出時の診断メッセージ／警告ログの新設 | 機能追加でありスコープ外 |
| `record_type: ""`（空文字）の扱いをテストで固定 | 仕様が定めていない領域であり、テストで固定すると誤った仕様を固めることになる |

### 修正ラウンド2 後の変異実験（コーディネータが独立に実行）

実装エキスパートの報告を追認せず、コーディネータ自身が 2 種の変異を投入して確認した。

**変異 A: 送信同期の連番を固定**（`fragment.addValueWithId(rowValues, String.valueOf(rowNo))` → `"1"`）

```
[ERROR]   YamlTestDataParserTest.getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped:870 BODY レコードの 2 行目の連番が "2" にインクリメントされること
[ERROR]   YamlFileBuilderTest.buildFragmentsForSendSync_rowNoIsIncrementedPerRow:681 2 行目の連番が "2" にインクリメントされること
[ERROR] Tests run: 173, Failures: 2, Errors: 0, Skipped: 4
```

修正ラウンド2 の前はこの変異で 173 件すべて GREEN のままだった（Verification レビュアーが実測）。

**変異 B: FW_HEADER 読み飛ばしを復活**

```
[ERROR]   YamlTestDataParserTest.getMessageWithoutCache_expectedRequestHeaderMessages:692
[ERROR]   YamlTestDataParserTest.getMessage_fwHeaderRecordTypeIsNotSkipped:813
[ERROR]   YamlTestDataParserTest.getMessage_legacyFwHeaderRecordCausesRecordLengthMismatch:894
[ERROR]   YamlTestDataParserTest.getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped:860
[ERROR]   YamlFileBuilderTest.buildFragmentsForMessage_fwHeaderRecordTypeIsNotSkipped:558
[ERROR]   YamlFileBuilderTest.buildFragmentsForMessage_fwHeaderRecordWithoutLength:601
[ERROR]   YamlFileBuilderTest.buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped:639
[ERROR]   YamlMessageBuilderTest.buildMessagePool_fwHeaderRecordTypeIsNotSkipped:433
[ERROR] Tests run: 173, Failures: 8, Errors: 0, Skipped: 4
```

ラウンド1 時点では 5 件・2 クラスだった RED が、8 件・4 クラス（公開 API 経路 3 件を含む）へ広がった。
両変異とも投入前後で `md5sum` が一致することを確認して復元し、
`mvn clean test` が `Tests run: 173, Failures: 0, Errors: 0, Skipped: 4` / `BUILD SUCCESS` に戻ることを確認した。
