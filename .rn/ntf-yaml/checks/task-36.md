# task-36 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `YamlFileBuilder` が `addValue`／`addValueWithId` の直前で本体の `NablarchTestUtils.trimTailCopy` を通している（規則の手写しをしていない） | OK | `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java:253` で `NablarchTestUtils.trimTailCopy(rowValues)` を呼び、その結果を `:256`（`addValueWithId`）／`:258`（`addValue`）へ渡す。追加した処理はこの 1 行のみで、末尾畳み込みの規則は自前で書いていない（`git diff src/main/java` は 11 行追加・2 行削除、うち 8 行はコメント）。順序は `interpret`（`:245`-`:247`）→ `trimTail`（`:253`）→ `addValue`（`:256`／`:258`）で本体 `DataFileParser.java:68` と同じ | | |
| 本体 `BasicTestDataParser` を正解にしたテストがあり、F1〜F6・M1・S2 を入力に含む | OK | `src/test/java/nablarch/test/core/reader/YamlTrailingNullOracleTest.java`（テスト 8 件）。oracle 側は `BodyExcelOracle`（`src/test/java/nablarch/test/core/reader/BodyExcelOracle.java`）が POI で `target/test-oracle/YamlTrailingNullOracleTest.xlsx` を組み立て、`BasicTestDataParser`（`PoiXlsReader` ＋ リポジトリの `interpreters`＝`NullInterpreter`・`QuotationTrimmer`・`LineSeparatorInterpreter` を含む）で読む。YAML 側は `YamlTestDataParser`（`yamlInterpreters`）で `src/test/java/nablarch/test/core/reader/YamlTrailingNullOracleTest/trailingNull.yaml` を読む。シート名とグループ ID／ID が F1・F2・F3・F4・F5・F6・M1・S2 で 1 対 1 対応 | | |
| 是正前に落ち是正後に通るテストが存在する | OK | 是正前（`JAVA_HOME=... mvn -o clean test -Dtest=YamlTrailingNullOracleTest`）: `Tests run: 8, Failures: 3, Errors: 1`。落ちたのは下記「是正前に落ちたテスト」の 4 件。是正後の同コマンド: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` | | |
| 既存テストで期待値を変えたもの・変えなかったものが件数付きで記録されている | OK | 変えたもの **0 件**、変えなかったもの **268 件**（着手前ベースラインの全件）。下記「既存テスト」の節を参照 | | |
| 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 追加した 8 件すべてについて期待値を崩し、1 度の実行で全 8 件が落ちることを確認した。下記「変異確認」の節を参照 | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 276, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。`Skipped 1` は `YamlTableDataBuilderTest.java:751` の既存 `@Ignore`（#41 の担当。今回は触っていない） | | |

## 是正前に落ちたテスト（実測）

コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlTrailingNullOracleTest`
結果: `Tests run: 8, Failures: 3, Errors: 1, Skipped: 0`

| テスト | 失敗の要点 |
|---|---|
| `getSetupFile_trailingNullsBecomeEmptyStrings`（F1） | `F1: FIELD2 が本体と一致すること Expected: is "" but: was null` |
| `getSetupFile_allNullsBecomeEmptyStrings`（F4） | `F4: FIELD2 が本体と一致すること Expected: is "" but: was null` |
| `getSetupFile_trailingEmptyStringAndNullBecomeEmptyStrings`（F6） | `F6: FIELD3 が本体と一致すること Expected: is "" but: was null` |
| `getSendSyncMessage_trailingNullsBecomeEmptyStrings`（S2） | `java.lang.NullPointerException: Cannot invoke "Object.equals(Object)" because "value" is null`（`nablarch-testing` の `MockMessages$MockMessage.removePadding`（`MockMessages.java:64`）が null 値で落ちる） |

F2・F3・F5・M1 は是正の前後で結果が変わらない対照ケースであり、是正前も通っていた。
M1 が対照になるのは、`messages` が固定長で `DataFileFragment#removePadding` を通り、値が null でも `""` が返るためである（実測。是正前の probe で `FIELD2=[]`・`FIELD3=[]` を確認）。
null と `""` の差が `DataRecord` に現れるのは `MockMessages` を通る送信同期電文（S2）の経路であり、そちらが電文側の判別役を担う。

## 是正後（実測）

- `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlTrailingNullOracleTest`
  → `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`
- `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
  → `Tests run: 276, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`
  （着手前ベースライン `Tests run: 268, Failures: 0, Errors: 0, Skipped: 1` ＋ 追加 8 件）

## 変異確認

崩したのは各テストが本体（Excel）の結果に対して置いている期待値リテラルである。
コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlTrailingNullOracleTest`
結果: `Tests run: 8, Failures: 8, Errors: 0, Skipped: 0`（8 件すべてが落ちた）。確認後ただちに元へ戻し、再度全件緑を確認済み。

| テスト | 崩した内容 | 落ちたこと |
|---|---|---|
| `getSetupFile_trailingNullsBecomeEmptyStrings`（F1） | `assertFileCase("F1", "x", "", "")` → `("F1", "x", null, "")` | 落ちた（`F1 本体（Excel）: FIELD2`） |
| `getSetupFile_nullFollowedByValueIsKept`（F2） | `("F2", "x", null, "y")` → `("F2", "x", "", "y")` | 落ちた（`F2 本体（Excel）: FIELD2`） |
| `getSetupFile_emptyStringWithOmittedTrailingFields`（F3） | `("F3", "", "", "")` → `("F3", "x", "", "")` | 落ちた（`F3 本体（Excel）: FIELD1`） |
| `getSetupFile_allNullsBecomeEmptyStrings`（F4） | `("F4", "", "", "")` → `("F4", null, "", "")` | 落ちた（`F4 本体（Excel）: FIELD1`） |
| `getSetupFile_omittedTrailingFieldsBecomeEmptyStrings`（F5） | `("F5", "x", "", "")` → `("F5", "x", null, "")` | 落ちた（`F5 本体（Excel）: FIELD2`） |
| `getSetupFile_trailingEmptyStringAndNullBecomeEmptyStrings`（F6） | `("F6", "x", "", "")` → `("F6", "x", "", null)` | 落ちた（`F6 本体（Excel）: FIELD3`） |
| `getMessage_trailingNullsBecomeEmptyStrings`（M1） | `assertRecordValues("M1 本体（Excel）", expected, "x", "", "")` → `"x", "", "y"` | 落ちた（`M1 本体（Excel）: FIELD3`） |
| `getSendSyncMessage_trailingNullsBecomeEmptyStrings`（S2） | `assertRecordValues("S2 本体（Excel）", expected, "x", "", "")` → `"x", "", "y"` | 落ちた（`S2 本体（Excel）: FIELD3`） |

なお oracle との突合部分（`assertSameAsOracle`）が生きていることは、是正前の実行で F1・F4・F6・S2 がその行（`YamlTrailingNullOracleTest.java` の `assertSameAsOracle`）で落ちたことが示している。

## 既存テスト

- 期待値を**変えたもの**: **0 件**。
- 期待値を**変えなかったもの**: **268 件**（着手前ベースラインの全件。是正後も `Tests run: 276`（＝268 ＋ 追加 8）で Failures 0・Errors 0）。
- 理由: 着手前調査のとおり、ファイルデータ・電文のフィクスチャに末尾 `null` を持つ行は 0 件だった（`src/test/java/**/*.yaml` の `setup_files`／`expected_files`／`messages`／同期応答 4 セクションを走査して確認）。`YamlTestDataParserTest/trailingNulls.yaml` は `list_maps`（#39 の担当）のフィクスチャであり、今回の変更経路（`YamlFileBuilder`）を通らない。

## 被覆状況

`mvn -o clean test` では JaCoCo のレポートは生成されない（`target/site/jacoco/` は作られない）。
`mvn -o jacoco:report -Djacoco.dataFile=jacoco.exec` を試したが、`target/classes` がオフライン計装済みのため
`Cannot process instrumented class nablarch/test/core/reader/yaml/YamlFileBuilder` で失敗し、行・分岐被覆は取得できなかった（未取得）。
代替として、追加した行（`YamlFileBuilder.java:253`）が実行されていることは、当該行の追加前に F1・F4・F6・S2 が落ち、追加後に通ることで確認している。

## QA / Expert Review

**Step 4 では4観点レビュー（QA / Design / Craft / Verification）を回さない**（指示書 §7。steering の
「Step 4 第2回に適用する Rules」）。観点D（検証の妥当性）は完了条件3（変異確認）と第3節の oracle で代替し、
ディレクターが差分を全量読み直して独立に検証する。代わりにコーディネータが下記を独立に実測した。

## コーディネータの独立検証（2026-08-28）

| 確かめたこと | 方法 | 結果 |
|---|---|---|
| oracle との突合が実際に効いている（期待値リテラルだけで通っていない） | `YamlFileBuilder.java:253` の `NablarchTestUtils.trimTailCopy(rowValues)` を `rowValues` に戻して `mvn -o clean test -Dtest=YamlTrailingNullOracleTest` を実行 | `Tests run: 8, Failures: 3, Errors: 1`。落ちた4件の失敗箇所はいずれも `assertSameAsOracle:425`（F1 `FIELD2` / F4 `FIELD2` / F6 `FIELD3`）と S2 の `NullPointerException`。**期待値リテラル側（`assertRecordValues`）ではなく oracle 突合側で落ちている**ことを確認。確認後ただちに元へ戻した |
| 全件緑 | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` | `Tests run: 276, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。`Skipped 1` は `YamlTableDataBuilderTest.java:751` の既存 `@Ignore`（#41 の担当） |
| `src/main` の変更が最小で、規則の手写しが無い | `git show ce81530 -- src/main/…/YamlFileBuilder.java` | 追加は `import` 1行・コメント7行・`trimTailCopy` 呼び出し1行のみ。`addValueWithId`／`addValue` への引数差し替え2行。末尾畳み込みの規則は自前で書いていない |
| 送信同期経路で本体と等価か（本体は連番セルを含む行全体に `trimTail` を掛ける） | 本体 `DataFileFragment.addValueWithId`（`3c4bd2a` の `:169`-`:183`）を確認 | 連番は `FIRST_FIELD_NO` として別に `put` され、値は `names.size()` まで `""` で埋められる。連番セルは非空なので、値だけに `trimTail` を掛けるのと等価。実装は正しい |
| 既存テストへの波及が本当に無いか | `YamlTestDataParserTest/trailingNulls.yaml` の内容を確認 | セクションは `list_maps` のみ。`YamlFileBuilder` の経路を通らないため今回の変更の対象外。着手前調査（末尾 `null` のファイル・電文フィクスチャ 0件）と整合 |

**未取得**: C0/C1 の被覆率。`target/classes` がオフライン計装済みのため `jacoco:report` が
「Cannot process instrumented class」で失敗する。**#43（カバレッジ計測）で計測方法ごと解決する**
（第1回の `#33` は計測に成功しているので、その手順を踏襲すること）。

## Overall Verdict

- Self-check: OK
- QA: N/A（Step 4 は4観点レビューを回さない。指示書 §7）
- Design expert: N/A（同上）
- Craft expert: N/A（同上）
- Verification expert: N/A（同上）
- コーディネータの独立検証: OK
- Ready to check off: Yes
