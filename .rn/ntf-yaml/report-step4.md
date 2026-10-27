# Step 4 報告書

対象: `nablarch-testing-yaml`（ブランチ `feature/ntf-yaml`）。Step 4 着手前 `ab0064e` → 指示書18件の完了時 `8eacaa7`。§6 の2件は、その後 2026-08-27 のユーザー指示（`/rn:gm` 差し戻し）で `#35` として是正した。

## 結論

指示書 第2節の5件はすべて是正した。第3節の13件はすべてテストを書き、12件が通り、1件（3-2 の負のテスト）が `@Ignore` になった。`mvn -o clean test` は `Tests run: 267, Failures: 0, Errors: 0, Skipped: 1`。

カバレッジは C0 99.22%（`INSTRUCTION_MISSED` 13）／C1 98.82%（`BRANCH_MISSED` 2）。未達は #19 で「到達不能」としてユーザーが承認済みの2箇所だけで、`ab0064e` と比べて**下がった箇所は無い**（クラス別の `INSTRUCTION_MISSED` / `BRANCH_MISSED` が両者で完全に一致）。

下流 `nablarch-testing-converter`（`60d9a2d`、未変更）で **Step 4 起因の失敗は1件**（`YamlTestCoreAdapterTest#isResourceExisting_reflectsFileExistence`、2-2 起因）。逆に Step 4 で**5件が解消した**。指示書が「2-1 起因の疑い」とした4件は `ab0064e` の時点で既に落ちており、Step 4 起因ではない（実測）。

指示書の18件の外で見つかった食い違い2件（スキーマの `sendSyncTestData` 形式の `id` 記述、`unit-test.xml` の `fileExtensions`）は、**`#35` で是正済み**（§6）。是正後の `mvn -o clean test` は `Tests run: 268, Failures: 0, Errors: 0, Skipped: 1`（門番テスト1件を追加）。

出典の書き方: 本モジュールは `<パス>:<行>`（断りが無ければ HEAD `8eacaa7`）。解説書は `nablarch-document` のピン `5b5c91e` の `<パス>:<行>`（パスは `ja/development_tools/testing_framework/` からの相対）。依存先はピン（`nablarch-testing` `3c4bd2a`／`nablarch-testing-converter` `60d9a2d`）。

---

## 1. 第2節5件の是正結果

| # | 是正 | コミット |
|---|---|---|
| 2-1 | 空行判定が Java null を空扱いしている | `013c974`・`98d7ce6` |
| 2-2 | `isResourceExisting` の判定単位を入れ物に揃える | `b510075` |
| 2-3 | 送信同期4キーで `record_type` の記載値を保持する | `1693cc1` |
| 2-4 | テスト用 `yamlInterpreters` を解説書に合わせる | `90d4b24` |
| 2-5 | スキーマ `description` 4件を解説書に合わせる | `c40ce50`・`7019093` |

### 2-1 空行判定が Java null を空扱いしている（`013c974`・`98d7ce6`）

**変更したファイルと `file:line`**

| ファイル | 何を変えたか |
|---|---|
| `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:205` | 判定を `if (str != null && !str.isEmpty())` → `if (str == null \|\| !str.isEmpty())` に是正。空文字だけを空と見なし、Java null は非空として扱う |
| 同 `:155`-`:157`（`dropBlankRows`）・`:193`-`:197`（`isBlankRow`） | javadoc を「全ての値が null または空文字」→「全ての値が空文字」に更新し、「Java null は空文字ではないため非空として扱い `COL: null` / `COL:` だけの行は残す」を明記 |
| `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:108`・`:136` | `$defs.table_data.rows`・`$defs.list_map_data.rows` の空行除去の条件を「全ての値が空文字の行」に是正 |
| `src/main/java/nablarch/test/core/reader/yaml/YamlTableDataBuilder.java:37`-`:39`・`:89`-`:92`・`:168`-`:171` | 同趣旨の説明コメント／javadoc（`98d7ce6`） |
| `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java:518`（`buildFileList_allBlankFieldRecordIsKept` の javadoc） | 同種の説明 javadoc（`98d7ce6`。当時の行番号は `:402`） |

解説書との一致: `implementation/testdata_notation.rst:1500`「空マッピング（`{}`）またはすべての値が空文字の場合にスキップされる」。

**直す前に落ちたテスト（3件）**

- `YamlSectionTest#dropBlankRows_keepsRowHavingOnlyNullValues`（`src/test/java/nablarch/test/core/reader/yaml/YamlSectionTest.java:595`）— `Expected: is <1> but: was <0>`
- `YamlTableDataBuilderTest#buildTableDataList_nullValueOnlyRowKept`（`src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java:1599`）— `Expected: is <2> but: was <1>`
- `YamlTableDataBuilderTest#buildListMapRows_nullValueOnlyRowKept`（同 `:1660`）— `Expected: is <2> but: was <1>`

是正前 `Tests run: 76, Failures: 3`（`-Dtest='YamlSectionTest,YamlTableDataBuilderTest'`）→ 是正後フル実行 `Tests run: 229, Failures: 0`。

### 2-2 `isResourceExisting` の判定単位を入れ物に揃える（`b510075`）

**変更したファイルと `file:line`**

| ファイル | 何を変えたか |
|---|---|
| `src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java:185` | `new File(buildFilePath(...)).exists()` → `new File(buildContainerPath(...)).isDirectory()` |
| 同 `:97`-`:101` | 入れ物パスを組み立てる `buildContainerPath` を追加（`/` が無い場合は `resourceName` 全体を入れ物名とする） |
| 同 `:200`-`:201` | 読み込み単位を判定する `isDataExisting` を**追加**（`<basePath>/<resourceName>.yaml` の `isFile()`） |
| `src/main/java/nablarch/test/core/reader/YamlTestDataParser.java:126` | `getSetupTableData` の内部ガードを `YamlLoader.isDataExisting` に置換（`:127`-`:128` に `BasicTestDataParser.java:52`（`3c4bd2a`）と同じ debug ログ） |
| 同 `:100`-`:110` | 判定単位が入れ物であることを javadoc に明記。`:112`-`:113` の委譲とシグネチャは不変 |

解説書との一致: `implementation/class_unit_test/component.rst:313`「YAML 形式では `<ディレクトリ>/<ファイル名>/<読み込み単位の名前>.yaml` が読み込まれる」＝入れ物は `<basePath>/<ファイル名>` ディレクトリ。

**直す前に落ちたテスト（4件）**

- `YamlLoaderTest#isResourceExisting_trueWhenReadUnitMissingButContainerExists`（`src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest.java:231`）
- `YamlLoaderTest#isResourceExisting_wholeNameIsContainerWhenNoSlash`（同 `:280`）
- `YamlTestDataParserTest#isResourceExistingReturnsTrueWhenReadUnitMissingButContainerExists`（`src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:146`）
- `YamlTestDataParserTest#getSetupTableDataReturnsEmptyWhenReadUnitNotExists`（同 `:1136`）

是正前 `Tests run: 239, Failures: 4` → 是正後 `Tests run: 239, Failures: 0`。

### 2-3 送信同期4キーで `record_type` の記載値を保持する（`1693cc1`）

**変更したファイルと `file:line`**

| ファイル | 何を変えたか |
|---|---|
| `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:302`-`:307` | `isSendSyncMessageSectionKey` を追加。送信同期4セクションキーだけ `true` |
| `src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java:89`-`:90`・`:119`-`:120`・`:158`-`:159` | セクションキーから求めた `keepRecordType` を引き回す |
| `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java:196`-`:198` | `fragment.setRecordType(keepRecordType && recordType != null ? recordType : DEFAULT_RECORD_TYPE)` |
| 同 `:142`・`:168`・`:189` | `buildFragmentsForMessage` / `buildFragmentsForSendSync` / `buildFragmentsInternal` に `keepRecordType` を追加（いずれも package-private。公開シグネチャは不変） |

解説書との一致: `implementation/testdata_notation.rst:1163`「`MESSAGE`（`setUpMessages`・`expectedMessages`）では、記載した値は使われず、デフォルトのレコード種別（`"default"`）になる。同期応答メッセージ送信で使う4つのデータタイプ…では、記載した値がそのままレコード種別になる」。

**直す前に落ちたテスト（5件）**

- `YamlTestDataParserTest#getMessageWithoutCache_expectedRequestHeaderMessages`（`YamlTestDataParserTest.java:809`）
- `YamlTestDataParserTest#getMessageWithoutCache_recordTypeIsKeptForSendSyncDataTypes`（同 `:1034`）
- `YamlTestDataParserTest#getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped`（同 `:986`）
- `YamlMessageBuilderTest#buildMessagePool_fwHeaderRecordTypeIsNotSkipped`（`src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java:423`）
- `YamlMessageBuilderTest#buildSendSyncMessageList_recordTypeIsKeptAsIs`（同 `:486`）

是正前 `Tests run: 243, Failures: 5` → 是正後 `Tests run: 245, Failures: 0`。

### 2-4 テスト用 `yamlInterpreters` を解説書に合わせる（`90d4b24`）

**変更したファイルと `file:line`**

| ファイル | 何を変えたか |
|---|---|
| `src/test/resources/unit-test.xml:58`-`:74` | `yamlInterpreters` から `NullInterpreter`・`LineSeparatorInterpreter` を削除。`DateTimeInterpreter` と `CompositeInterpreter`→`BasicJapaneseCharacterInterpreter` の2件のみ |
| 同 `:78`-`:89` | `yamlMessagingInterpreters` を**新設**（`CompositeInterpreter`→`BasicJapaneseCharacterInterpreter` の1件のみ） |
| `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java`（`before()`） | 参照先を Excel 用 `interpreters` → `yamlMessagingInterpreters` |
| `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java`（`before()`） | 参照先を Excel 用 `interpreters` → `yamlInterpreters` |

解説書との一致: `setup/common.rst:77`（`yamlInterpreters` はこの2つだけでよい）・`:81`（`NullInterpreter` を指定してはならない）・`:260`（電文用は1つだけでよい）。

`src/main` は変更していない（この是正はテスト用コンポーネント設定の是正である）。

**直す前に落ちたテスト（4件）**

- `YamlTestDataParserTest#yamlInterpretersAreOnlyDocumentedTwo`（`YamlTestDataParserTest.java:1670`）— `Expected: is <2> but: was <4>`
- `YamlTestDataParserTest#quotedNullIsKeptAsStringAndDistinguishableFromBareNull`（同 `:1697`）— `Expected: is "null" but: was null`
- `YamlTableDataBuilderTest#buildListMapRows_quotedNullIsKeptAsString`（`YamlTableDataBuilderTest.java:542`）— `Expected: is "null" but: was null`
- `YamlTableDataBuilderTest#buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser`（同 `:591`）

是正前 `Tests run: 247, Failures: 4` → 是正後 `Tests run: 248, Failures: 0`。

設定変更後に**新たに落ちた既存テストは1件**で、期待値を解説書側へ寄せて是正した（本書 4. を参照）。

### 2-5 スキーマ `description` 4件を解説書に合わせる（`c40ce50`・`7019093`）

`ab0064e`→`8eacaa7` のスキーマ差分は **description 4行のみ**（`git diff --stat ab0064e..HEAD -- src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` が `4 insertions(+), 4 deletions(-)`）。`type` / `required` / `properties` / `enum` の構造は無変更。

| `file:line`（HEAD） | 定義 | 是正の要点 | 解説書の根拠 |
|---|---|---|---|
| `ntf-testdata-yaml-schema.json:410` | `$defs.field_def.length` | `"-"` は「改行コードおよび前後空白を除去」→「**値に含まれる改行とその前後の空白**を除去（改行を含まない値の前後の空白は除去されない）」 | `implementation/testdata_notation.rst:1059` |
| 同 `:108` | `$defs.table_data.rows` | `NullInterpreter` 前提の帰結を除去（`"null"` は文字列のまま）。あわせて FK 段落末尾の「NULL 許容カラムは `null` を明示」に Boolean 型の但し書きを追記し、同一 description 内の矛盾を解消 | `setup/common.rst:81`、`implementation/testdata_notation.rst:1399` |
| 同 `:136` | `$defs.list_map_data.rows` | 同上（`"null"` / `NULL` は文字列のままマップの値になる） | `setup/common.rst:81` |
| 同 `:365` | `$defs.record_fragment.record_type` | 「メッセージング系では常に `"default"`」→「`messages` では `"default"`、**送信同期4セクションでは記載値がそのままレコード種別**」 | `implementation/testdata_notation.rst:1163` |

`:365` は指示書 2-5 の名指し3件（`:410`・`:108`・`:136`）の外だが、2-3 の是正で事実に反することになったため、2026-08-26 のユーザー判断で対象に追加した。

`$defs.message_data.records`（`:208`）は `messages` 専用の定義で是正後も正しいため**変更していない**。

あわせて `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` の javadoc から `NullInterpreter` 前提の例示を除去した（`grep -n NullInterpreter` が同ファイルで0件）。

**この是正で落ちたテストは無い**（description と javadoc の是正であり、実装挙動を変えていない）。代わりに `length: "-"` の挙動を押さえるテストを3件追加した（`YamlFileBuilderTest.java:307`・`:343`・`:378`）。

---

## 2. 第3節13件の結果

**13件すべてにテストがある。通ったもの12件、`@Ignore` にしたもの1件。**

| # | テスト | `file:line` | 結果 |
|---|---|---|---|
| 3-1 | `YamlTestDataParserTest#yaml12BooleanWordsAreStringsAsKeysAndValues` | `YamlTestDataParserTest.java:297` | 通った |
| 3-1 | `YamlTestDataParserTest#unquotedNoKeyStaysStringKey` | 同 `:330` | 通った |
| 3-2 | `YamlTableDataBuilderTest#buildListMapRows_allFourteenCharacterTypesAreGenerated` | `YamlTableDataBuilderTest.java:706` | 通った（14文字種すべて） |
| 3-2（負） | `YamlTableDataBuilderTest#buildListMapRows_unknownCharacterTypeIsNotConverted` | 同 `:753` | **`@Ignore`** |
| 3-3 | `YamlTableDataBuilderTest#buildListMapRows_combinedCharTypeNotationKeepsSeparator` | 同 `:779` | 通った |
| 3-4 | `YamlTableDataBuilderTest#buildListMapRows_escapedLfIsLineFeed` | 同 `:812` | 通った |
| 3-5 | `YamlDateNotationTest#omittedMillisIsFilledWithZero` | `YamlDateNotationTest.java:120` | 通った |
| 3-5 | `YamlDateNotationTest#omittedTimeIsFilledWithZero` | 同 `:140` | 通った |
| 3-6 | `YamlTestDataParserTest#attachNotationIsReadableAsUploadFileSpecification` | `YamlTestDataParserTest.java:363` | 通った |
| 3-7 | `YamlTableDataBuilderTest#buildTableDataList_groupIdIsMatchedExactly` | `YamlTableDataBuilderTest.java:1744` | 通った |
| 3-8 | `YamlLoaderTest#load_prefixMatchedTopLevelKeyIsSchemaViolation` | `YamlLoaderTest.java:578` | 通った |
| 3-9 | `YamlTableDataBuilderTest#buildTableDataList_collectionIsNotTruncatedByInterleavedGroupId` | `YamlTableDataBuilderTest.java:1779` | 通った |
| 3-10 | `YamlTestDataParserTest#getMessage_reservedIdsSetUpMessagesAndExpectedMessages` | `YamlTestDataParserTest.java:1782` | 通った |
| 3-11 | `YamlTestDataParserTest#getMessageWithoutCache_readsMessageYamlUnderRequestIdDirectory` | 同 `:1829` | 通った |
| 3-12 | `YamlTestDataParserTest#getListMap_resolvesFileNameAndUnitNameToNestedYaml` | 同 `:1867` | 通った |
| 3-13 | `YamlTableDataBuilderTest#buildListMapRows_argsIndexKeyIsNotExcludedAsMarker` | `YamlTableDataBuilderTest.java:1808` | 通った |

ファイル名は `src/test/java/nablarch/test/core/reader/`（`YamlTestDataParserTest`）、同 `reader/yaml/`（`YamlTableDataBuilderTest`・`YamlLoaderTest`）、同 `db/`（`YamlDateNotationTest`）配下。

### `@Ignore` にしたもの（1件）— 理由の文言（原文）

`src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java:751`:

```
@Ignore("NTF-DOC: implementation/testdata_notation.rst:1313 — 期待 列挙外の文字種名は変換されず ${存在しない文字種,3} のまま / 実際 InterpretationFailedException（原因 IllegalArgumentException: unknown charsetName. charsetName=[存在しない文字種]）")
```

`grep -rn "@Ignore" src/test/java/` のヒットはこの1件のみ。期待値は解説書どおりのまま残してあり、実装は直していない（`git diff ab0064e..HEAD -- src/main/java` に `BasicJapaneseCharacterInterpreter` 関連の変更は無い。そもそも当該クラスは依存先 `nablarch-testing` にある）。

これが `mvn -o clean test` の `Skipped: 1` の実体である。

### 3-2 の14文字種

半角英字 / 半角数字 / 半角記号 / 半角カナ / 全角英字 / 全角数字 / 全角ひらがな / 全角カタカナ / 全角漢字 / 全角記号その他 / 中国語 / サロゲートペア / 改行 / 外字。文字種ごとに別の `list_maps` エントリで評価し、失敗を集めてから一度に判定する書き方のため、1つ落ちても残りの検証が止まらない。

---

## 3. 期待値をわざと崩す確認の結果

**注記**: 本タスクは Scope により `src/test` を変更できないため、変異確認を**再実行していない**。以下は各 check ファイル（`.rn/ntf-yaml/checks/task-26.md` 〜 `task-32.md`）に記録された実測である。対象テスト名と `file:line` は本タスクで現物に当たって確認した。

| 是正/追加 | 崩した内容 | 実測結果 |
|---|---|---|
| 2-1 変異1 | `YamlSection#isBlankRow` を是正前（Java null を空扱い）のまま新規3テストを実行 | `Tests run: 76, Failures: 3` — `dropBlankRows_keepsRowHavingOnlyNullValues` / `buildTableDataList_nullValueOnlyRowKept` / `buildListMapRows_nullValueOnlyRowKept` |
| 2-1 変異2 | 是正後、既存フィクスチャの空行に Java null が残る状態（`""` を `null` に戻した状態と同値） | `Tests run: 76, Failures: 11` — 変更した8件＋波及3件 |
| 2-1 変異3 | 期待件数を崩す4件（`is(1)`→`is(0)` / `is(1)`→`is(2)` / `is(2)`→`is(1)` ×2） | `Tests run: 76, Failures: 4` — 崩した4件が過不足なく失敗 |
| 2-1 変異4 | 「null が null のまま保持される」期待を崩す3件（`rowOf(...,null)`→`rowOf(...,"")`、`assertNull`→`assertNotNull` ×2） | `Tests run: 76, Failures: 3` |
| 2-2 変異1 | 追加/変更した15件の主アサーションを一括反転（`is(true)`↔`is(false)`、`assertTrue`↔`assertFalse`、`is(0)`→`is(1)`、`"0000009001"`→`"9999999999"`） | `Tests run: 239, Failures: 15` — 崩した15件が過不足なく失敗 |
| 2-2 変異2 | 複数アサーションを持つ2件の残り側（`isResourceExisting(DIR,"NoSuchContainer")`→`is(true)`、ログ検証 `not(containsString("-WARN-"))`→`not(containsString("-DEBUG-"))`） | `Tests run: 239, Failures: 2` |
| 2-3 変異A | `YamlSection#isSendSyncMessageSectionKey` を `return false;` に | `Tests run: 245, Failures: 6` |
| 2-3 変異B | 同メソッドを `return true;` に | `Tests run: 245, Failures: 4` |
| 2-3 変異C | 同メソッドから `KEY_RESPONSE_HEADER_MESSAGES` の項を削除 | `Tests run: 245, Failures: 2` |
| 2-3 変異D | `YamlFileBuilder#buildFragmentsInternal` が `keepRecordType` を無視し常に記載値を採用 | `Tests run: 245, Failures: 6` |
| 2-3 変異E | `buildFragmentsForSendSync` が `keepRecordType` を捨てて常に `false` を渡す | `Tests run: 245, Failures: 3` |
| 2-4 M1 | `yamlInterpreters` に `NullInterpreter` を戻す | `Tests run: 248, Failures: 3, Errors: 1` |
| 2-4 M2 | `yamlInterpreters` に `LineSeparatorInterpreter` を戻す | `Tests run: 248, Failures: 2` |
| 2-4 M3 | `yamlMessagingInterpreters` に `NullInterpreter` を足す | `Tests run: 248, Failures: 1`（`yamlMessagingInterpretersIsOnlyDocumentedOne`） |
| 2-4 M4a/M4b | 門番フィクスチャ `interpretedToBlankRow` / `...ListMap` の値を全て `""` にする | それぞれ `Failures: 1` |
| 2-4 M5 | `nativeTypes.yaml` の `YAML_CR_COL: "\r"` を `"\\r"`（2文字）に | `Failures: 1` |
| 2-4 M6 | `omission.yaml` の `s11` の `BOOL_COL: "null"` をクォートなし `null` に | `Errors: 1`（NPE） |
| 2-4 M7 | `quotedValues.yaml` の `QUOTED_NULL: "null"` をクォートなし `null` に | `Failures: 1` |
| 2-4 M8 | `nativeTypes.yaml` の `UPDATE_COL: "${updateTime}"` を `"${updateTimeX}"` に | `Failures: 1` |
| 2-5 | `length: "-"` の3テストの期待値を同時に崩す（FIELD2 開始位置 `is(6)`→`is(4)`、値 `is("ABCD")`→`is("AB  CD")`、FIELD2 開始位置 `is(7)`→`is(3)`） | `Tests run: 33, Failures: 3` — 崩した3件が過不足なく失敗 |
| 3-1〜3-6 | 通った8メソッドの期待値を同時に崩す（`"no"`→`"false"`、`"1"`→`"9"`、`${attach:...}` に `X` 付加、コードポイント数 `3`→`4`、長さ `7`→`8`、LF→CR、ミリ秒 `.000`→`.001`、時刻 `00:00:00`→`00:00:01`） | `Tests run: 260, Failures: 8` — 崩した8件が過不足なく失敗 |
| 3-7〜3-13 | 通った7メソッドの期待値を同時に崩す（`is(1)`→`is(2)`、`"additionalProperties"`→`"MUTATED"`、`is(2)`→`is(1)`、`"SETUPKEY01"`→`"MUTATED"`、`"FROM_DIR01"`→`"FROM_FILE1"`、`is(2)`→`is(1)`、`"arg0Value"`→`"MUTATED"`） | `Tests run: 267, Failures: 7` — 崩した7件が過不足なく失敗 |

変異を当てていない変更点は2-4 の2箇所のみで、いずれも javadoc の文言修正（期待値・挙動は不変）と `YamlFileBuilderTest#before` の参照先差し替え（`yamlInterpreters` 側の内容は M1・M2 で守られている）。

`@Ignore` にした1件は通っていないため変異確認の対象外である。

---

## 4. 既存テストの期待値を変えた箇所の全件

「期待値」をアサーション（`assertThat` / `assertNull` などの期待側）に限って数え、区別のため Given（テストコード内のデータ・フィクスチャ YAML）の変更も併記する。

### 2-1（`013c974`・`98d7ce6`）— **アサーションの変更は 0 件**。Given の変更が 11 件

`git diff 013c974^..013c974 -- src/test/java` に `assert*` の期待値を変えた行は無い（変わったのは javadoc とアサーションメッセージの文言のみ）。是正で挙動が変わるのは「空行を表す行に Java null が混じっていた Given」であり、直したのはその Given である。

**Given を変えた既存テスト（11件）**

| # | テスト | 変えた Given |
|---|---|---|
| 1 | `YamlSectionTest#dropBlankRows_removesEmptyMappingAndAllBlankValueRows` | Java コード内の `rowOf("COL_A", null, "COL_B", null)` の行を削除（`assertThat(result.size(), is(1))` は不変） |
| 2 | `YamlTableDataBuilderTest#buildTableDataList_blankValueRowLeadingExcluded` | `tableData.yaml` `blankValueRowLeading`: `VARCHAR2_COL: null` → `""`（1箇所） |
| 3 | `YamlTableDataBuilderTest#buildTableDataList_blankValueRowMiddleExcluded` | `blankValueRowMiddle`: `PK_COL2` / `NUMBER_COL` → `""`（2箇所） |
| 4 | `YamlTableDataBuilderTest#buildTableDataList_partiallyBlankValueRowKept` | `partiallyBlankValueRow` の2行目: `PK_COL2` / `NUMBER_COL` → `""`（2箇所）。1行目の `NUMBER_COL: null` は「null が null のまま保持される」担保のため残置 |
| 5 | `YamlTableDataBuilderTest#buildTableDataList_blankValueRowLeadingInExpectedTableExcluded` | `blankValueRowLeadingExpected`: `VARCHAR2_COL` → `""`（1箇所） |
| 6 | `YamlTableDataBuilderTest#buildTableDataList_blankValueRowMiddleInExpectedTableExcluded` | `blankValueRowMiddleExpected`: `PK_COL2` / `NUMBER_COL` → `""`（2箇所） |
| 7 | `YamlTableDataBuilderTest#buildTableDataList_blankValueRowInExpectedCompleteTableExcluded` | `completedTable.yaml` `blankValueRowComplete`: `VARCHAR2_COL` / `PK_COL1` / `PK_COL2` → `""`（3箇所） |
| 8 | `YamlTableDataBuilderTest#buildListMapRows_blankValueRowLeadingExcluded` | `blankValueRowLeadingListMap`: `KEY8` → `""`（1箇所） |
| 9 | `YamlTableDataBuilderTest#buildListMapRows_blankValueRowMiddleExcluded` | `blankValueRowMiddleListMap`: `KEY2` → `""`（1箇所） |
| 10 | `YamlTableDataBuilderTest#buildListMapRows_partiallyBlankValueRowKept` | `partiallyBlankValueRowListMap` の2件目: `KEY2` → `""`（1箇所）。1件目の `KEY3: null` は残置 |
| 11 | `YamlTableDataBuilderTest#buildListMapRows_allBlankRowsReturnsEmptyList` | `allBlankRowsListMap` の中間行: `KEY2` → `""`（1箇所） |

**件数**: アサーション変更 **0件** / Given を変えた既存テスト **11件**（YAML フィクスチャの値の書き換え **15箇所**＋Java コード内 Given 1箇所）。javadoc・コメントのみの変更は上表の件数外。

指示書は「既存12件（5+5+2）」を名指ししていた。実測ではそのうち **8件** に Given の変更が要り、**4件** は変更不要（`dropBlankRows_keepsRowHavingAnyNonBlankValue` / `..._keepsRowHavingOnlyWhitespaceValue` / `..._keepsRowHavingOnlyMarkerColumnValue` / `..._removesNonMappingRows`。いずれも Java null を含まない）。名指しの12件の外で **3件**（上表 #4・#10・#11）に波及した。8 + 3 = 11 が上表の件数である。

### 2-3（`1693cc1`）— **アサーションの変更は 3テスト・4箇所**。フィクスチャ YAML の変更は 0 件

| # | テスト | `file:line`（HEAD） | 変更前 → 変更後 |
|---|---|---|---|
| 1 | `YamlTestDataParserTest#getMessageWithoutCache_expectedRequestHeaderMessages` | `YamlTestDataParserTest.java:824` | `message.getRecordType()` の期待 `"default"` → `"FW_HEADER"` |
| 2 | `YamlFileBuilderTest#buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped` | `YamlFileBuilderTest.java:953` | `layoutRecords.get(0).getTypeName()` の期待 `"default"` → `"FW_HEADER"` |
| 3 | 同上 | 同 `:955` | `layoutRecords.get(1).getTypeName()` の期待 `"default"` → `"BODY"` |
| 4 | `YamlMessageBuilderTest#buildMessagePool_fwHeaderRecordTypeIsNotSkipped` | `YamlMessageBuilderTest.java:437` | `layout.getRecords().get(0).getTypeName()` の期待 `"default"` → `"FW_HEADER"` |

**期待値が変わらなかった既存テスト（アサーションメッセージの文言だけ直したもの）**

- `YamlTestDataParserTest#getMessage_fwHeaderRecordTypeIsNotSkipped`（`:947`。`messages` 経路なので `"default"` のまま。`:949` に2レコード目の検証を**追加**）
- `YamlFileBuilderTest#buildFragmentsForMessage_fwHeaderRecordTypeIsNotSkipped`（`:863`・`:865`。`"default"` のまま）
- `YamlFileBuilderTest#buildFragmentsForMessage_fwHeaderRecordWithoutLength`（`"default"` のまま）
- `YamlTestDataParserTest#getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped`（レコード種別の検証を**追加**。既存アサーションの期待値は不変）

**フィクスチャ YAML の変更は 0 件**。`git diff --stat 1693cc1^..1693cc1` の対象は `.java` 7ファイルのみ。送信同期4セクションの `record_type` 記載24件は、記載値をそのまま保持するのが是正後の正しい挙動なので、本文を変えずに意味が正しくなる。

### 参考: 2-4（`90d4b24`）で新たに落ち、期待値を解説書側へ寄せた既存テスト（1件）

`YamlColumnOmissionTest#setupThrowsNpeWhenBooleanColumnIsQuotedNullString` → `#setupSucceedsWhenBooleanColumnIsQuotedNullString`（`src/test/java/nablarch/test/core/db/YamlColumnOmissionTest.java:311`）。`NullInterpreter` を外したことで `BOOL_COL: "null"` が Java null にならず NPE が起きなくなったため、「NPE にならずに INSERT でき、値は `false` になる」に是正した。根拠は `setup/common.rst:81`。

---

## 5. カバレッジ C0/C1 の計測結果と converter で落ちたテスト

### 5.1 計測方法

```
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec
```

`pom.xml` / `argLine` は変更していない。HEAD `8eacaa7` のテスト実行は `Tests run: 267, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。

基準 `ab0064e` は `git worktree add` で取り出して同じ手順で計測し（`Tests run: 226, Failures: 0, Errors: 0, Skipped: 0`）、計測後に `git worktree remove` で片付けた。

### 5.2 全体値

| | `ab0064e`（Step 4 着手前） | `8eacaa7`（HEAD） |
|---|---|---|
| C0（命令） | 1598 / 1611 = **99.19%**（`INSTRUCTION_MISSED` 13） | 1663 / 1676 = **99.22%**（`INSTRUCTION_MISSED` 13） |
| C1（分岐） | 158 / 160 = **98.75%**（`BRANCH_MISSED` 2） | 168 / 170 = **98.82%**（`BRANCH_MISSED` 2） |

### 5.3 `src/main` 各クラスの `INSTRUCTION_MISSED` / `BRANCH_MISSED`（HEAD `8eacaa7`）

| クラス | `INSTRUCTION_MISSED` | `BRANCH_MISSED` |
|---|---|---|
| `nablarch.test.core.reader.YamlTestDataParser` | 0 | 0 |
| `nablarch.test.core.reader.yaml.YamlTableDataBuilder` | 0 | 0 |
| `nablarch.test.core.reader.yaml.YamlSection` | 0 | 0 |
| `nablarch.test.core.reader.yaml.YamlMessageBuilder` | 0 | 0 |
| `nablarch.test.core.reader.yaml.YamlSchemaValidationException` | 0 | 0 |
| `nablarch.test.core.reader.yaml.InterpreterResolver` | 0 | 0 |
| `nablarch.test.core.reader.yaml.MessageContent` | 0 | 0 |
| **`nablarch.test.core.reader.yaml.YamlFileBuilder`** | **1** | **1** |
| **`nablarch.test.core.reader.yaml.YamlLoader`** | **12** | **1** |

9クラス中7クラスが 0 / 0。

### 5.4 未達2箇所の内訳（#19 でユーザーが「到達不能」として承認済み）

| `file:line`（HEAD） | 内容 | 未達 |
|---|---|---|
| `src/main/java/nablarch/test/core/reader/yaml/YamlFileBuilder.java:236`-`:237` | `if (!(rowObj instanceof List)) { continue; }` の true 側 | 命令1・分岐1 |
| `src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java:60`-`:61` | static イニシャライザの `schemaStream == null` の true 側 | 分岐1 |
| `src/main/java/nablarch/test/core/reader/yaml/YamlLoader.java:65`-`:66` | 同 `catch (IOException e)` | 命令（`:61` と合わせて計12） |

`YamlFileBuilder` の未達は #19 当時 `:227`-`:228` だったもので、Step 4 の変更で行番号が `:236`-`:237` へ動いただけである（`YamlLoader` の行番号は不変）。指示に従い、この2箇所にテストは足していない。裁定は `.rn/ntf-yaml/checks/task-19.md` の「裁定」節。

### 5.5 カバレッジが下がった箇所

**無し。**

`ab0064e` と `8eacaa7` の CSV を比較すると、9クラスすべてで `INSTRUCTION_MISSED` / `BRANCH_MISSED` が完全に一致する（`YamlFileBuilder` 1/1、`YamlLoader` 12/1、他7クラス 0/0）。網羅した命令は 1598→1663（+65）、分岐は 158→168（+10）に増えている。

**未達として挙げるべき箇所は無い**（残る2箇所は承認済みの到達不能）。

### 5.6 converter（`nablarch-testing-converter` `60d9a2d`）で落ちたテスト

converter は**一切変更していない**（実行前後とも `git status --short` が空）。本モジュールを2つの状態で `.m2` へ install し、それぞれ `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` を実行した。基準側の install は `git worktree add` で `ab0064e` を取り出して行い、最後に HEAD を install し直した。

| | 基準（本モジュール `ab0064e`） | 完了後（本モジュール `8eacaa7`） |
|---|---|---|
| 集計 | `Tests run: 605, Failures: 8, Errors: 1, Skipped: 2` | `Tests run: 605, Failures: 5, Errors: 0, Skipped: 2` |

#### Step 4 起因で**新たに**落ちたテスト（1件）

| テスト | 失敗メッセージ | 原因の是正 |
|---|---|---|
| `nablarch.test.core.reader.YamlTestCoreAdapterTest#isResourceExisting_reflectsFileExistence`（`src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest.java:370`） | `Expected: is <false>` / `but: was <true>` | **2-2**（`#27`）。converter の `YamlTestCoreAdapter#isResourceExisting` は `YamlLoader.isResourceExisting` へ透過委譲しており、テストは `isResourceExisting(DIR, "YamlTestCoreAdapterTest/noSuchFile")` に**読み込み単位**（`noSuchFile.yaml` の不存在）を期待している。是正後は入れ物単位の判定になり、入れ物ディレクトリ `YamlTestCoreAdapterTest` が存在するため `true` を返す。converter 側が読み込み単位を意図しているなら `YamlLoader.isDataExisting` への切り替えが対応方針になるが、本タスクの範囲外 |

#### Step 4 で**解消した**失敗（5件）

いずれも 2-1（`#26` 空行判定の是正）で通るようになった。基準では落ち、完了後では落ちていない。

| テスト | 基準での失敗メッセージ |
|---|---|
| `YamlFormatReaderScalarTest#readsOmittedValueAsJavaNull:364` | `Expected: is <[V]>` / `but: was <[]>` |
| `YamlFormatReaderScalarTest#readsUnquotedNullAsJavaNull:352` | `Expected: is <[V]>` / `but: was <[]>` |
| `YamlFormatReaderScalarTest#readsUnquotedNullAsJavaNullInListMapPath:572` | `Expected: is <1>` / `but: was <0>` |
| `YamlFormatWriterTest#roundTrip_nullAndNullStringAndNumeric_areDistinguishedThroughRealReader:660` | `Expected: is <[null]>` / `but: was <[null]>` |
| `RoundTripTest#nullCell_xlsConvertsToLiteralString_yamlPreservesNull:664`（Error） | `IndexOutOfBounds` |

#### Step 4 と無関係に落ち続けている失敗（4件）

基準・完了後の両方で同じメッセージで落ちる。**Step 4 起因ではない。**

| テスト | 失敗メッセージ |
|---|---|
| `YamlFormatReaderInvalidInputTest#dropsAllRowsWhenFirstRowOfTableIsEmptyObject:601` | `columnNames が空であること` / `Expected: is <[]>` / `but: was <[A]>` |
| `YamlFormatReaderInvalidInputTest#keepsRowCountButLosesValuesWhenFirstRowOfListMapIsEmptyObject:628` | 同上 |
| `YamlFormatReaderScalarTest#readsEmptyStringAsIs:505` | `Expected: is <[V]>` / `but: was <[]>` |
| `YamlFormatReaderScalarTest#readsEmptyStringAsIsInListMapPath:584` | `Expected: is <1>` / `but: was <0>` |

指示書はこの4件を「2-1 起因の疑い」としていたが、**基準 `ab0064e` の時点で既に同じメッセージで落ちている**ため、Step 4 起因ではない。`#27` の check ファイルが基準にした `0602b39` は `#26` の是正後のコミットであり、その差が「2-1 起因の疑い」という見立てを生んだと考えられる。

---

## 6. 指示書18件の外にあった食い違いの是正（2件・是正済み）

2026-08-27 のユーザー指示（`/rn:gm` 差し戻し）で `#35` として是正した。**この節に限り、本モジュールの行番号は是正後の状態を指す**（他の節は HEAD `8eacaa7`）。

### 6-1. スキーマの `id` 記述が解説書と食い違っていた（是正済み）

**是正前**（`8eacaa7`）— `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` の2箇所:

- `:53`（`properties.messages` の `description`）— 「…id は `sendSyncTestData/{requestId}/message` 形式で指定する」
- `:200`（`$defs.message_data.properties.id` の `description`）— 「メッセージID。`sendSyncTestData/{requestId}/message` 形式で指定する（末尾の `message` は固定のパスセグメント）」

**合わせる先** — 解説書 `implementation/testdata_notation.rst:1151`（ピン `5b5c91e`）:

> データタイプ `MESSAGE` の識別子として `setUpMessages`（要求電文）・`expectedMessages`（応答電文）を指定し、テストの入力データ・期待値となる電文を記述する。**これらの識別子は固定である。**（中略）取引単体テストのモックアップクラスが読む同期応答メッセージ送信のテストデータは、コンポーネント設定ファイルで `sendSyncTestData` というキーに設定したベースディレクトリの配下に置く。読み込み単位の名前はリクエスト ID ごとに決まっており、Excel 形式ではリクエスト ID と同じ名前のファイルの `message` シート、YAML 形式ではリクエスト ID と同じ名前のディレクトリ配下の `message.yaml` である（`message` は固定の名前）。（中略）**`sendSyncTestData` はデータブロックの識別子ではない。**

**是正後** — 両方の `description` を次の2点に書き換えた（行番号は `:53`・`:200` のまま）:

1. id はデータタイプ `MESSAGE` の識別子であり、`setUpMessages`（要求電文）・`expectedMessages`（応答電文）という固定値を指定する
2. `sendSyncTestData` は、取引単体テストのモックアップクラスが読む同期応答メッセージ送信のテストデータのベースディレクトリに付けるコンポーネント設定のキーであり、`sendSyncTestData/{requestId}/message` は**読み込み単位を指すパス**であって、この `id` に書く値ではない（`:200` では「データブロックの識別子ではない」と明記）

`messages` セクションがデータタイプ `MESSAGE` に対応することは実装で確認できる（`src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:314` の `case MESSAGE: return KEY_MESSAGES;`、`src/main/java/nablarch/test/core/reader/YamlTestDataParser.java:176` が `getMessage` から `KEY_MESSAGES` を渡す）。

**挙動テスト** — 指示どおり新規テストは足していない。根拠テストは既存の `YamlTestDataParserTest#getMessage_reservedIdsSetUpMessagesAndExpectedMessages`（`src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:1814`）。`description` の是正なので、**変更の前後で結果が変わらない**ことを実測した（いずれも `mvn -o clean test -Dtest='YamlTestDataParserTest#getMessage_reservedIdsSetUpMessagesAndExpectedMessages'`）。

| 順 | 状態 | 結果 |
|---|---|---|
| 1 | `description` 変更**前**（6-2 の是正のみ適用済み） | BUILD SUCCESS — `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` |
| 2 | `description` 変更**後** | BUILD SUCCESS — `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` |

### 6-2. `unit-test.xml` の `fileExtensions` が解説書の禁止に反していた（是正済み）

**是正前**（`8eacaa7` の `src/test/resources/unit-test.xml:170`-`:174`）:

```xml
<property name="fileExtensions">
  <map>
    <entry key="sendSyncTestData" value="xls"/>
  </map>
</property>
```

**根拠** — 解説書 `setup/common.rst:264`（`.. important::` の本文、ピン `5b5c91e`）:

> `fileExtensions` には `sendSyncTestData` を設定しない。YAML 形式ではリクエストIDと同じ名前のディレクトリを参照するため、拡張子を設定するとテストデータが見つからず、テストの実行時に例外が発生する。

**是正後** — `filePathSetting` から `fileExtensions` プロパティを削除した（`src/test/resources/unit-test.xml:163`-`:170`）。残るエントリが `sendSyncTestData` の1件だけで、空の `<map/>` を残す意味が無いため、プロパティごと落としている。`FilePathSetting` の `fileExtensions` フィールドは初期値が空の `CaseInsensitiveMap` で、プロパティ未指定でも `getFileExtensions()` は `null` を返さない（`nablarch-core` `6-NEXT-SNAPSHOT` の `FilePathSetting.java:27`）。`basePathSettings`（`move` = `file:tmp`）には手を入れていない（解説書が要求していないため範囲外）。

**門番テスト**（1件追加） — `YamlTestDataParserTest#fileExtensionsHasNoSendSyncTestData`（`src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java:1703`）。2-4 の `yamlInterpretersAreOnlyDocumentedTwo` と同じ形で、リポジトリから `filePathSetting` を取得し `getFileExtensions()` に `sendSyncTestData` キーが無いことを表明する。

**削除前 → 削除後の順序と結果**（いずれも `mvn -o clean test -Dtest='YamlTestDataParserTest#fileExtensionsHasNoSendSyncTestData'`）:

| 順 | 状態 | 結果 |
|---|---|---|
| 1 | 門番テストを足しただけ（`fileExtensions` は**残したまま**） | **BUILD FAILURE** — `java.lang.AssertionError: fileExtensions に sendSyncTestData キーが無いこと（setup/common.rst:264）` at `YamlTestDataParserTest.java:1712`。`Tests run: 1, Failures: 1, Errors: 0, Skipped: 0` |
| 2 | `fileExtensions` を**削除した後** | **BUILD SUCCESS** — `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` |

3-11（同期応答メッセージ送信のテストデータ配置）を `SendSyncSupport` 経由で押さえられない事情は、この是正では変わらない。`basePathSettings` に `sendSyncTestData` が無いため、`SendSyncSupport#createTestDataInfo`（`nablarch-testing` `3c4bd2a` の `SendSyncSupport.java:346`）が呼ぶ `FilePathSetting#getBaseDirectory("sendSyncTestData")` は、`getBasePathUrl` の中で `IllegalArgumentException: Unknown basePathName: sendSyncTestData` になる（`nablarch-core` `6-NEXT-SNAPSHOT` の `FilePathSetting.java` の `getBasePathUrl`）。`#32` が採った、`SendSyncSupport.java:347` と同じ引数（`resourceName = <リクエストID> + "/" + "message"`）で `YamlTestDataParser#getMessageWithoutCache` を直接呼ぶ形はそのまま維持している。

（指示書はこの important を `setup/common.rst:263` としているが、ピン `5b5c91e` の現物では `.. important::` が `:262`、本文が **`:264`** である。本書は現物の行番号を採る。）

### 6-3. 是正後の全体テスト

`mvn -o clean test`: **`Tests run: 268, Failures: 0, Errors: 0, Skipped: 1`**（`8eacaa7` の 267 件 + 門番テスト1件）。`Skipped 1` は 3-2 の `@Ignore` 1件のままで、実装は直していない。`git status --short` は空。

---

## 7. 指示書の想定と実測が食い違った点

### 7-1. `record_type: FW_HEADER` の出現件数

指示書は16件としていたが、実測は**17件**（Step 4 着手前 `ab0064e`）。`record_type: HEADER` は**7件**で一致。

| コミット | `record_type: *FW_HEADER` | `record_type: *HEADER` |
|---|---|---|
| `ab0064e`（Step 4 着手前） | 17 | 7 |
| `8eacaa7`（HEAD） | 18 | 7 |

（コマンド: `git grep -h 'record_type: *FW_HEADER' <rev> -- src/ \| wc -l`。HEAD の18件目は `#28`（`1693cc1`）が追加したテストの javadoc/アサーションメッセージであり、フィクスチャのデータ行ではない。）

17件のうち **YAML フィクスチャの実データ行は7件**で、残る10件はスキーマの description・フィクスチャ内のコメント・Java の javadoc／コメント／アサーションメッセージである。

### 7-2. 3-3（組み合わせ記法）は「既存0件」ではなく解説書どおり通った

指示書は 3-3 を既存テスト0件の未確認項目として挙げたが、`YamlTableDataBuilderTest#buildListMapRows_combinedCharTypeNotationKeepsSeparator`（`src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java:779`）は**解説書どおりに通った**。

ただし担っているのは `unit-test.xml:66`-`:73` の `CompositeInterpreter` であり、`BasicJapaneseCharacterInterpreter` 単体ではない。

- `CompositeInterpreter`（`nablarch-testing` `3c4bd2a`）は `Pattern.compile("\\$\\{[^\\}]+\\}")` を `while (m.find())` で走査し、`${...}` を1要素ずつ切り出して委譲する
- `BasicJapaneseCharacterInterpreter`（同）は `Pattern.compile("\\$\\{(\\W+)\\s*,\\s*([0-9]+)\\}")` を `m.matches()`（完全一致）で判定するため、`"${半角数字,2}-${半角数字,4}"` のような値全体には一致しない

通ったこと自体は問題ではないが、押さえている経路が単体クラスではなく合成側である点を記録しておく。

### 7-3. converter で落ちる4件の帰属

「2-1（`#26` 空行判定）起因の疑い」とされた `YamlFormatReaderInvalidInputTest` 2件・`YamlFormatReaderScalarTest` 2件は、基準 `ab0064e` の時点で既に同じメッセージで落ちており、**Step 4 起因ではない**（5.6 参照）。

### 7-4. 解説書の行番号

指示書が挙げた `implementation/testdata_notation.rst:1149` と `setup/common.rst:263` は、ピン `5b5c91e` の現物ではそれぞれ **`:1151`** と **`:264`** である（6-1・6-2 の括弧書き）。指す内容は同じ。
