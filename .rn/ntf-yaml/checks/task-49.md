# task-49 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| Q1・Q2 は〈先に落ちるテスト → 落ちることを確認 → 変更 → 緑〉の順で行われ、報告にテスト名と「変更前に落ちた」事実がある | OK | 先にテストとフィクスチャだけを追加し、スキーマ変更前に `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test -Dtest=YamlLoaderTest -DfailIfNoTests=false` を実行して 3 件が落ちることを確認した（生ログは下記「Q1・Q2 の変更前ログ」）。スキーマ変更後は `Tests run: 37, Failures: 0` で緑。報告書 `.rn/ntf-yaml/report-step4-3.md` §6「決着（2026-08-31）」と §7「第2ラウンド」にテスト名と「変更前に落ちた」事実を記載した | | |
| 構造の差分が Q1・Q2 に対応する箇所だけであることを `git diff` で確認して報告に書いてある | OK | `6175639` と作業ツリーのスキーマ JSON を `description`／`$comment` を除いてパースし機械比較した結果、差分は (a) `/$defs/record_fragment/properties/rows/minItems`（Q1）、(b) `/$defs/directives_fixed`・`/$defs/directives_variable`・`/$defs/file_data/allOf`（Q2）のみ。`git --no-pager diff --stat 6175639 -- src/` の変更ファイルはスキーマ 1・テスト 3（＋新規フィクスチャ 5）。報告書 §7「第2ラウンド」に記載 | | |
| `mvn clean test` 全件緑（320件＋新規テスト）・`git status --short` 空・push 済み | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test` → `[INFO] Tests run: 324, Failures: 0, Errors: 0, Skipped: 0` / `[INFO] BUILD SUCCESS`（324 = 320 + 新規 5 − 削除 1）。commit（`f3620fc`・`69d903a`）と push の直後、`git status --short` は空だった。`git push` は `6175639..69d903a  feature/ntf-yaml -> feature/ntf-yaml` で成功（`origin/feature/ntf-yaml` = `69d903a`）。その後、本ファイル `checks/task-49.md` を書き出したため、現在の `git status --short` は `?? .rn/ntf-yaml/checks/task-49.md` の 1 行だけである（指示どおり commit していない）。**追記（63-5 の件数是正）**: 報告書 63-5 に書いた「25 箇所」が実測と合わなかったため、`xlrd` による再実測（24 箇所。付録「Q4〜Q6 の実測の根拠」の Q5 参照）に基づき `.rn/ntf-yaml/report-step4-3.md` の当該 1 箇所のみを 24 に訂正し、数え方（`xlrd` で全 23 シートの全セルを走査し値が `ダミー` のセルを数えた）と内訳を所見に追記した。`git add` の対象は `.rn/ntf-yaml/report-step4-3.md` のみで、`src/` は無変更、63-5 の判定と Q5 の結論は変えていない | | |

## QA Expert Review

指示書 `ntf-step4-13` §6 末尾「レビューは回さない（§4 と同じく、検証はディレクターが実物で行う）」
により、QA / Design / Craft / Verification の各エキスパートは回していない。
代わりにコーディネーターが下記を実物で独立に再現・検証した。

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective | N/A（指示書 §6 によりレビュー不実施） | — |

## コーディネーター独立検証（2026-08-31）

| 確認項目 | 方法 | 結果 |
|---|---|---|
| Q1・Q2 の「変更前に落ちる」事実 | スキーマだけを `6175639` の内容に戻し（`git show 6175639:… > …`）、`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlLoaderTest` を実行。実行後にスキーマを復元し `git status --short` が追跡ファイルを含まないことを確認 | OK。`Tests run: 37, Failures: 3` で、落ちたのは `load_emptyRowsIsSchemaViolation`（`:747`）・`load_variableFileWithFixedOnlyDirectiveIsSchemaViolation`（`:815`）・`load_fixedFileWithVariableOnlyDirectiveIsSchemaViolation`（`:783`）の新規3件のみ。エキスパートの報告と完全一致 |
| 全件緑 | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` をコーディネーター自身で単独実行 | OK。`Tests run: 324, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` |
| 構造差分が Q1・Q2 に対応する箇所だけか | `git diff 6175639..HEAD -- src/main/resources/…schema.json` を全量読む | OK。構造の追加は `record_fragment.properties.rows.minItems`、`$defs.directives_fixed`、`$defs.directives_variable`、`file_data.allOf` の4点のみ。他は description・`$comment` の変更 |
| 報告書のスキーマ参照行番号 | スキーマを走査して「`description`／`$comment` ノードの JSON パス → 行番号」の対応表を作り、報告書の全 52 見出し（`### DNN \`パス\` — スキーマ :NN`）のパスと行番号を機械照合 | OK。不一致 0 件。本文中のスキーマ参照も全件、参照先の行の内容を目視で確認（`:81`・`:459`・`:390`・`:485`・`:493`・`:249` 等）して妥当 |
| Q4 の実測 | `nablarch-testing@3c4bd2a` の `FixedLengthFileFragment.java:98`（`.setName(name)`）・`DataFileFragment.java:104`-`:115`（Map キー）・`:190`-`:194`（`assertNotContainDuplicateNames`）・`:519`-`:529`（`prepareRecordDefinition` が names から生成）を自分で開いて確認 | OK。「照合」ではなく「生成」であるという結論を裏づける |
| Q6 の実測 | `DataFile.java:275`-`:284`（`createLayout`）・`:294`-`:306`（`setDirective`）・`:325`-`:334`（`convertDirectiveValue`）を自分で開いて確認 | OK。型変換して `LayoutDefinition` のディレクティブへ素通しするだけで単位を解釈しない |
| Q6 の解説書逐語 | `ed3de95f` の `testdata_notation.rst:933`-`:934` | OK。`max-record-length` の説明は「レコードの最大長」で単位の記載なし |
| Q2 の11キー・9キー | `ed3de95f` の `testdata_notation.rst:884`-`:909`（固定長11キー）・`:911`-`:936`（可変長9キー）を自分で展開して列挙 | OK。`directives_fixed`・`directives_variable` の `propertyNames.enum` と完全一致 |
| Q5 の所見の件数 | `xlrd` で `3c4bd2a` の `RequestTestingMessagingClientTest.xls` を全シート走査 | 当初 NG（報告書は「25 箇所」、実測 24 箇所）→ 実装エキスパートに差し戻し、`05c2b23` で 24 に是正・数え方を明記して解消 |
| description 件数・スキーマ行数 | `grep -c '"description"'` = 66、`wc -l` = 517 | OK。報告書 §7 の「`description` 66 件」「`:517` まで」と一致 |

**ディレクターへ報告する判断（変更していない／変更した理由を明示するもの）**:

1. **母集合 444 を据え置いた**。Q1〜Q6 で description を変更し、`$defs` が 64→66 に増えたため、
   厳密に再導出すれば主張件数は動く。エキスパートは第1ラウンドとの対照可能性を優先して 444 を据え置き、
   Q2 の `file_data` 構造制約を新規行ではなく 37-構造3 に記録した（報告書 §7 に明記）。
   再導出すべきならその指示で行える。
2. **既存テスト1件を削除した**（`YamlFileBuilderTest#buildFileList_noRowsBecomesZeroDataRecords`）。
   Q1 の `minItems: 1` により入力がスキーマ検証を通らなくなり、2026-08-24 ユーザー裁定の判定基準
   （スキーマ検証を通過しうる入力に対する挙動）の対象外になったため。期待値の修正ではなく削除である。
3. **電文側の型別限定は表現していない**（指示書 §6 Q2 が許す「その箇所だけ報告して止まる」）。
   根拠は報告書 §6 と `$defs.directives` の `$comment`。

## Overall Verdict

- Self-check: OK
- QA: N/A（指示書 §6 によりレビュー不実施。コーディネーター独立検証で代替。1件の NG は `05c2b23` で解消済み）
- Design expert: N/A
- Craft expert: N/A
- Verification expert: N/A
- Ready to check off: Yes

---

## 付録（自己点検の証跡）

### Q1・Q2 の変更前ログ（スキーマ変更前に落ちたこと）

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test -Dtest=YamlLoaderTest -DfailIfNoTests=false
[ERROR] Tests run: 37, Failures: 3, Errors: 0, Skipped: 0, Time elapsed: 0.484 s <<< FAILURE! - in nablarch.test.core.reader.yaml.YamlLoaderTest
[ERROR] load_emptyRowsIsSchemaViolation(nablarch.test.core.reader.yaml.YamlLoaderTest)  Time elapsed: 0.015 s  <<< FAILURE!
java.lang.AssertionError: YamlSchemaValidationException が期待される
	at nablarch.test.core.reader.yaml.YamlLoaderTest.load_emptyRowsIsSchemaViolation(YamlLoaderTest.java:747)

[ERROR] load_variableFileWithFixedOnlyDirectiveIsSchemaViolation(nablarch.test.core.reader.yaml.YamlLoaderTest)  Time elapsed: 0 s  <<< FAILURE!
java.lang.AssertionError: YamlSchemaValidationException が期待される
	at nablarch.test.core.reader.yaml.YamlLoaderTest.load_variableFileWithFixedOnlyDirectiveIsSchemaViolation(YamlLoaderTest.java:815)

[ERROR] load_fixedFileWithVariableOnlyDirectiveIsSchemaViolation(nablarch.test.core.reader.yaml.YamlLoaderTest)  Time elapsed: 0.003 s  <<< FAILURE!
java.lang.AssertionError: YamlSchemaValidationException が期待される
	at nablarch.test.core.reader.yaml.YamlLoaderTest.load_fixedFileWithVariableOnlyDirectiveIsSchemaViolation(YamlLoaderTest.java:783)

[ERROR] Tests run: 37, Failures: 3, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

この 3 件が落ちたことは、検証ライブラリ（`com.networknt:json-schema-validator:1.5.9`。`pom.xml:37`-`:39`）が
`if`／`then` と `propertyNames` を実際に評価していることの確認にもなっている（サポート外なら
スキーマ変更後も落ち続ける）。変更後は同じコマンドで `Tests run: 37, Failures: 0` となった。

### 変更後の全件実行（生の出力）

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn clean test
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.932 s - in nablarch.test.core.db.YamlDateNotationTest
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.235 s - in nablarch.test.core.db.YamlColumnOmissionTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.659 s - in nablarch.test.core.reader.YamlBlankEntryOracleTest
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.163 s - in nablarch.test.core.reader.YamlTestDataParserTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.092 s - in nablarch.test.core.reader.YamlTrailingNullOracleTest
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.037 s - in nablarch.test.core.reader.yaml.YamlLoaderTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s - in nablarch.test.core.reader.yaml.YamlSectionTest
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.15 s - in nablarch.test.core.reader.yaml.YamlFileBuilderTest
[INFO] Tests run: 58, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.097 s - in nablarch.test.core.reader.yaml.YamlMessageBuilderTest
[INFO] Tests run: 66, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.136 s - in nablarch.test.core.reader.yaml.YamlTableDataBuilderTest
[INFO] Tests run: 324, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

324 件の内訳: 320 件（#46 完了時）＋ 新規 5 件 − 削除 1 件。

- 新規: `YamlLoaderTest#load_emptyRowsIsSchemaViolation`（Q1）、
  `#load_fixedFileWithVariableOnlyDirectiveIsSchemaViolation`・
  `#load_variableFileWithFixedOnlyDirectiveIsSchemaViolation`（Q2。先に落ちるテスト）、
  `#load_fileDirectivesOfMatchingTypeAreAllowed`・`#load_messageDirectivesAreNotTypeRestricted`（対照）
- 削除: `YamlFileBuilderTest#buildFileList_noRowsBecomesZeroDataRecords`。C8 で是正した
  「rows が0件でも有効」を担保していたテストであり、Q1 の `minItems: 1` により `rows: []` が
  スキーマ検証を通らなくなったため、判定基準（2026-08-24 ユーザー裁定「スキーマ検証を通過しうる
  入力に対する、外から観測できる挙動」）の対象外になった。フィクスチャ
  `YamlFileBuilderTest/fileData.yaml` の `noRows` グループも併せて削除した

### カバレッジ（JaCoCo）

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes
[INFO] Tests run: 324, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec
[INFO] BUILD SUCCESS
（target/site/jacoco/jacoco.csv を集計）
C0 1809/1822  INSTRUCTION_MISSED=13
C1 174/176    BRANCH_MISSED=2
  YamlFileBuilder IM=1 BM=1
  YamlLoader      IM=12 BM=1
```

**#46 完了時の基準（C0 1809/1822・C1 174/176・`INSTRUCTION_MISSED` 13・`BRANCH_MISSED` 2）と完全に一致した。増減なし。**
未達 2 箇所は #46 でユーザーが到達不能として承認済みの箇所（`YamlFileBuilder`・`YamlLoader`）であり、
新規・削除いずれのテストもこの 2 箇所に触れていない。削除した
`buildFileList_noRowsBecomesZeroDataRecords` は `YamlFileBuilder` の既存経路を通るだけで、
その経路は他のテストでも通るため未達は増えなかった。

### Q4〜Q6 の実測の根拠

参照コミット: 本体 `nablarch-testing@3c4bd2a`、解説書 `nablarch-document@ed3de95f`、
本モジュールは作業ツリー（`6175639` 時点）。

- **Q4（`field_def.name`）— 「照合する」挙動は実在しない。削除して実測どおりの記述に置換した**
  - NTF はテストデータの `name` から `FieldDefinition` を生成する: `FixedLengthFileFragment.java:98`（`.setName(name)`）、
    `VariableLengthFileFragment.java:53`、`DataFileFragment.java:519`-`:529`
  - レイアウト定義は毎回新規生成される: `DataFile.java:275`-`:284`
  - 期待値ファイルの読み込みも同じ生成レイアウトを使う: `DataFile.java:198`-`:224`、本体 `FileSupport.java:159`-`:177`
    → 名前の一致を検査する経路は原理的に無い
  - `name` の他の用途: `DataRecord` の Map キー（`DataFileFragment.java:104`-`:115`）、
    同一レコード種別内の重複検査（`DataFileFragment.java:190`-`:194`・`:354`-`:362`）
  - `.fmt` を `FilePathSetting.getFile("format", ...)` で読む経路は messaging のみ
    （`MockMessagingClient.java:166`・`:194`、`RequestTestingMessagingClient.java:539`）で、
    いずれもアプリが送信した `DataRecord` のシリアライズ用であり、テストデータの `name` は渡らない
  - 本モジュール側は素通し: `YamlFileBuilder.java:217`・`:228`
- **Q5（`field_def.length` の `"0"`）— 「ダミーフィールド」の根拠は取れなかった。削除した**
  - 解説書 `ed3de95f`: `git show ed3de95f:<notation.rst> | grep -n 'ダミー\|プレースホルダ\|placeholder'` → 0 件。
    `testdata_examples.rst` も 0 件
  - 本体 `3c4bd2a`: `git grep -n 'ダミーフィールド\|プレースホルダ' 3c4bd2a -- src` → 0 件
  - 長さ `0` を特別扱いする実装の分岐は無い: `FixedLengthFileFragment.java:95`-`:101`（`Integer.parseInt("0")` を
    そのまま長さに使う）、`DataFileFragment.java:311`-`:317`（レコード長に `0` を加算）
  - 用途の実例は本体のテストリソース `RequestTestingMessagingClientTest.xls`（`3c4bd2a`）の
    電文セクションに **24 箇所**あるが、ファイルデータ（`SETUP_FIXED` 等）には 0 件。
    実装でも解説書でもないため description の根拠にはしなかった（この事実は報告書 63-5 に所見として残した）
  - **（是正）当初この件数を「25 箇所」と書いていたが、実測は 24 箇所だった。** 実測方法と結果:
    `git show 3c4bd2a:src/test/java/nablarch/test/core/messaging/RequestTestingMessagingClientTest.xls`
    （`/home/tie303177/work/nablarch/nablarch-testing` で実行）で `.xls` を取り出し、`xlrd` で
    全 23 シートの全セルを走査して値が `ダミー` のセルを数えた → **24**（`'ダミー' in v` で数えても 24）。
    出現は 22 シートで、`testLessStatusCode` と `testSendDifferentRequestIds` が各 2 箇所、
    残る 20 シートが各 1 箇所（20 + 2 + 2 = 24）。24 箇所すべてがフィールド名セル（B列）で、
    2 行下のフィールド長セルは `0`、上位の見出しは `EXPECTED_REQUEST_HEADER_MESSAGES[...]` または
    `RESPONSE_HEADER_MESSAGES[...]`。したがって「長さ `0`・フィールド名ダミー」「電文セクションのみ」
    という構造の主張は正しく、誤っていたのは件数だけだった。走査スクリプト:

    ```python
    import xlrd
    b = xlrd.open_workbook('RequestTestingMessagingClientTest.xls')
    n = 0
    for sh in b.sheets():
        for r in range(sh.nrows):
            for c in range(sh.ncols):
                v = sh.cell_value(r, c)
                if isinstance(v, str) and v.strip() == 'ダミー':
                    n += 1
    print(n)  # -> 24
    ```
- **Q6（`max-record-length` の「（バイト数）」）— 根拠は取れなかった。削除した**
  - 本体 `3c4bd2a`: `git grep -n 'max-record-length\|MAX_RECORD_LENGTH' 3c4bd2a` のヒットは
    `docs/pr75/docs/ntf-testdata-doc.md:643`・`docs/pr75/ntf-impl-spec-list.md:177` のみで `src/` は 0 件。
    本モジュールの `src/` も 0 件
  - NTF は値を型変換して `LayoutDefinition` のディレクティブへ素通しするだけで単位を解釈しない:
    `DataFile.java:294`-`:306`（`setDirective`）・`:325`-`:334`（`convertDirectiveValue`）・`:275`-`:284`（`createLayout`）
  - 依存先 `nablarch-core-dataformat` の `VariableLengthDataRecordFormatter` は `BufferedReader#read()` で
    数える**文字数**として扱う（同 2.0.3 sources の `:139`-`:140`・`:795`-`:796`・`:882`-`:888`。
    実際に使う 6-NEXT-SNAPSHOT は sources 未配布のため `javap -p` と `strings` で同じフィールド構成と
    メッセージ `the number of the read characters exceeded the upper limit.` を確認）。
    本モジュールから観測できる挙動ではないため description には書かず、解説書 `notation.rst:934`
    「レコードの最大長」の逐語に合わせて「最大レコード長」とした
