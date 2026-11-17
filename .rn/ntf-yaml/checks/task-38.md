# task-38 Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `convertFwHeader` が既定4つ／`reader.fwHeaderfields` の名前だけを許し、それ以外を例外にする | OK | `src/main/java/nablarch/test/core/reader/yaml/YamlMessageBuilder.java:278`-`:299`（`convertFwHeader`）。ループの中で `allowedFields.contains(key)` を判定し、無ければ `IllegalStateException`（`:291`-`:295`）。許可集合は `:350`-`:356`（`fwHeaderFields()`）が返す。解説書の出典: `git -C ../nablarch-document show afa4f9e:ja/development_tools/testing_framework/implementation/testdata_notation.rst` を自分で開き、`testdata_notation.rst:1147` の節「メッセージングのデータを記述する」の中の `testdata_notation.rst:1274` 項「YAML形式の場合」に置かれた `testdata_notation.rst:1295` 「`fw_header:` に記載できるキーは、`reader.fwHeaderfields` の名前（省略時は `requestId`・`userId`・`resendFlag`・`resultCode`）だけである。それ以外のキーがあるとエラーになる。」を確認した。**3巡目**: クラス javadoc の出典（`YamlMessageBuilder.java:53`-`:67`）に親節「メッセージングのデータを記述する」を添えた（「Excel形式の場合」「YAML形式の場合」はこの rst にそれぞれ 8 回現れ節見出しだけでは一意にならない。`grep -n` で実測）。キー検査が `messages` 経路（`useFwHeader=true`）限定であることは `YamlMessageBuilder.java:135`-`:137`、これを `YamlMessageBuilderTest.java:1479`（`fwHeaderIsNotCheckedOutsideMessagesPathAndBecomesEmptyMap`）が押さえる | | |
| 集合の作り方が本体と同じ（同じキー・同じ既定4つ・同じ `makeArray`） | OK | **テストで守っている**: `YamlMessageBuilderTest.java:1099`（`fwHeaderFields_isIdenticalToMessageParserForListedConfigurations`）が本体 `MessageParser` を生成して private フィールド `fwHeaderFields`（`../nablarch-testing/src/main/java/nablarch/test/core/reader/MessageParser.java:107`-`:110`）をリフレクションで読み（ヘルパー `YamlMessageBuilderTest.java:137`-`:154`）、`YamlMessageBuilder.fwHeaderFields()` の戻り（ヘルパー `YamlMessageBuilderTest.java:158`-`:162`）と **8 通り**の設定（`null`／`""`／`" "`／`","`／`"a,b"`／`"a, b"`／`"requestId,"`／**`",requestId"`**）すべてで等しいことを assert する。実装側: `YamlMessageBuilder.java:74`（同じキー）・`:81`-`:82`（同じ既定4つ。`NablarchTestUtils.asSet("requestId","userId","resendFlag","resultCode")` の並びまで本体 `MessageParser.java:109` と同じ）・`YamlMessageBuilder.java:352`-`:355`（同じ `StringUtil.isNullOrEmpty` 判定・同じ `NablarchTestUtils.makeArray`・同じ `NablarchTestUtils.asSet`）。`makeArray` が前後の空白を取り除かないことは `../nablarch-testing/src/main/java/nablarch/test/NablarchTestUtils.java:35`（`COMMA = Pattern.compile(",")`）・`:45`-`:49` を開いて確認済み。**呼び出しごとに引く**点だけ本体と異なる（理由は `YamlMessageBuilder.java:338`-`:342` の javadoc に明記）。ヘルパー `mainFwHeaderFields()` は `NoSuchFieldException`／`NullPointerException` を捕まえて `UPSTREAM_CHANGED`（`YamlMessageBuilderTest.java:64`-`:66`）を message に持つ `AssertionError` を投げる。**3巡目**: `fail(メッセージ + e)` をやめ `throw new AssertionError(メッセージ, e)` にして cause を残した（`YamlMessageBuilderTest.java:143`-`:144`・`YamlMessageBuilderTest.java:150`-`:151`）。戻り値は**両分岐とも不変**（`YamlMessageBuilder.java:354`-`:355` の `Collections.unmodifiableSet`。`YamlMessageBuilderTest.java:1138` の `fwHeaderFields_returnsUnmodifiableSetInBothBranches` が assert） | | |
| `reader.fwHeaderfields` を設定した場合・しない場合の両方のテストがある | OK | **しない場合**: `YamlMessageBuilderTest.java:965`（`buildMessagePool_fwHeaderKeyNotInDefaultFieldsThrows`）・`:1215`（`buildMessagePool_unknownKeyIsCheckedOnlyForTheEntryBeingRead`）・`:1265`（`buildMessagePool_fwHeaderKeyIsCaseSensitive`）・`:1305`（`buildMessagePool_fwHeaderTildeKeyIsReadAsStringAndRejected`）・`:1344`（`buildMessagePool_fwHeaderNonStringKeyThrowsWithStringifiedKeyName`）・`:1402`（`buildMessagePool_fwHeaderNullKeyIsRejectedInDefensiveBranch`）。**空文字＝未設定**: `:1174`（`buildMessagePool_emptyFwHeaderFieldsBehavesAsUnset`。`setFwHeaderFields("")` で既定4つが実際に通ることを assert）。**設定した場合**: `:918`（`buildMessagePool_customFwHeaderFields`）・`:1014`（`buildMessagePool_fwHeaderKeyNotInConfiguredFieldsThrows`）・`:1049`（`buildMessagePool_fwHeaderFieldsAreSplitByCommaWithoutTrimming`）・`:1627`（`buildMessagePool_fwHeaderMapKeepsQuotedNumericAndBooleanLikeValuesAsStrings`）。**両分岐の不変性**: `:1138`。**8 通りの設定**（異常系 `" "`・`","`・末尾カンマ・先頭カンマを含む）は `:1099` のパリティテストが回す。設定は `:100`-`:102`（`setFwHeaderFields`）で行い、`:77`-`:82`（`@After`）で空文字に戻す。`reader.fwHeaderfields` は `src/test/resources` のどの設定にも無い（`grep -rn fwHeaderfields src/test/resources` が 0 件）ため、真に未設定（`SystemRepository.getString` が `null`）で走るのはクラス内で最初に走る 1 件だけである（各テストの直前に値を出力して実測。1 件が `<null>`、残り 51 件が `[]`） | | |
| 例外メッセージに電文の `id` と不正なキー名が入ることを assert している | OK | メッセージの形は `YamlMessageBuilder.java:293`-`:294` の `"fw_header in message entry id='" + id + "' has unknown key '" + key + "'. allowed keys (reader.fwHeaderfields): " + formatAllowedFields(allowedFields)`。**`id` は部分一致でなく `id='...'` の形で assert**（`YamlMessageBuilderTest.java:975`・`:1025`・`:1236`・`:1280`・`:1318`・`:1359`・`:1417`、および `must be a map` 側 `:795`）。**キー名**は `has unknown key '...'` の形（`:977`・`:1027`・`:1061`・`:1238`・`:1278`・`:1316`・`:1357`・`:1415`）。**メッセージ後半（何が許されるのか）**は `allowed keys (reader.fwHeaderfields): [...]` を丸ごと assert する 3 件（`:979`＝既定4つ／`:1029`＝設定あり／`:1066`＝空白入りの項目名）が押さえ、`TreeSet` による辞書順（＝決定性）と**各名前のクォート**（`YamlMessageBuilder.java:317`-`:326` の `formatAllowedFields`）を同時に守る。**`must be a map` 側の後半**（`:284`-`:285` の `but was: <型名>`）も `YamlMessageBuilderTest.java:797` が型名まで assert する | | |
| 既存フィクスチャ・テストで変えたもの・変えなかったものが件数付きで記録されている | OK | 着手前（`876e342`）の `YamlMessageBuilderTest` は `@Test` **41 件**、現在は **52 件**（`@Test` の直後の `public void` 名を 3 リビジョンで機械的に突き合わせた実測）。是正で落ちた既存テストは **4 件**（下記「是正後に落ちた既存テスト」）。フィクスチャの該当キーは **3 件**（削除 1・残した 2。下記「是正したフィクスチャ」）。ラウンドごとの内訳は下記「レビュー是正ラウンドの変更」「レビュー是正 2巡目の変更」「レビュー是正 3巡目の変更」に件数付きで記録した。**着手前の 41 件のうち、名前がそのまま残っているのは 38 件・名前が変わった／消えたのは 3 件**（下記「着手前テストの行方（実測）」） | | |
| 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある | OK | 初回ラウンド **8 通り**（下記「変異確認」M1-M8）＋1巡目 **13 通り**（I1-I7・M9-M14）＋2巡目 **16 通り**（N1-N16）＋**3巡目 8 通り**（下記「変異確認（レビュー是正 3巡目）」O1-O6・P1-P2）。すべて `git worktree add --detach` で作った隔離コピー（3巡目は `<scratchpad>/wt38fix`）で実施し、作業ツリーは崩していない。生き残った変異 3 件は下記「生き残っている変異（却下・実測つき）」に理由を実測つきで記録した | | |
| `mvn -o clean test` が BUILD SUCCESS | OK | `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 291, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`。着手前ベースライン（自分で取り直した実測）は同コマンドで `Tests run: 280, Failures: 0, Errors: 0, Skipped: 1`。差 +11 の内訳: 初回 +3、1巡目 +9、2巡目 -1、3巡目 ±0（改名 1 件のみ）。`Skipped 1` は `YamlTableDataBuilderTest` の既存 `@Ignore`（#41 の担当。今回触っていない） | | |

## 是正「前」に新規テストが落ちたこと（実測）

コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlMessageBuilderTest -DfailIfNoTests=false`
（`src/main` を変更する前、テストだけを追加した状態）
結果: `Tests run: 44, Failures: 3, Errors: 0, Skipped: 0` / `BUILD FAILURE`

| テスト | 落ちた要点 |
|---|---|
| `buildMessagePool_fwHeaderKeyNotInDefaultFieldsThrows` | `IllegalStateException が期待される` — 検査が無く独自キーがそのまま通っていた |
| `buildMessagePool_fwHeaderKeyNotInConfiguredFieldsThrows` | `IllegalStateException が期待される`（同上） |
| `buildMessagePool_fwHeaderFieldsAreSplitByCommaWithoutTrimming` | `IllegalStateException が期待される`（同上） |

（`fail` の message だけを写した。当時のスタックトレース行番号は、その後の是正で現在のファイルとは対応しないため載せない）

## 是正「後」に落ちた既存テスト（全件・実測）

コマンド: `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
結果: `Tests run: 283, Failures: 0, Errors: 4, Skipped: 1` / `BUILD FAILURE`
落ちたのは `nablarch.test.core.reader.yaml.YamlMessageBuilderTest` の **4 件**のみ。
他のテストクラス（`YamlTestDataParserTest` 64 件・`YamlLoaderTest` 32 件・`YamlFileBuilderTest` 33 件など）は 1 件も落ちていない。

| # | テスト | テストコードを変えたか | 対応 |
|---|---|---|---|
| 1 | `buildMessagePool_customFwHeaderFields` | **変えた** | 「フィルタ廃止で全キーが保持される」テストから「`reader.fwHeaderfields` に設定した独自キーは通る」テストへ。`setFwHeaderFields("customField,requestId")` を追加し javadoc とコメントを是正した（現在は `YamlMessageBuilderTest.java:918`）。assert（`customField`＝`CUSTOM_VALUE`・`requestId`＝`0000000001`）は変えていない |
| 2 | `buildMessagePool_fwHeaderMapAllKeysRetainedIncludingCustom` | **変えた**（テスト名も変更） | `buildMessagePool_fwHeaderMapAllDefaultKeysRetained` に改名した（このテストは 2巡目で削除。下記「レビュー是正 2巡目の変更」）。独自キー `customProjectKey` をフィクスチャから外したため、`customProjectKey` の assert を「既定 4 キーだけが設定されていること」（`fwHeader.size()` が 4）に差し替えた。既定 4 キーの値の assert 4 件は変えていない |
| 3 | `buildMessagePool_fwHeaderMapReadableWithoutHeaderRecord` | **変えていない** | 同じ `fwHeaderMapData.yaml` の `req001` を読むため巻き添えで落ちていた。フィクスチャから `customProjectKey` を外しただけで復旧した（現在は `YamlMessageBuilderTest.java:1435`） |
| 4 | `buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues` | **変えた** | 独自キー `boolFlag` を使うため `setFwHeaderFields("resendFlag,resultCode,boolFlag")` を追加し javadoc に Given を追記した（1巡目に `buildMessagePool_fwHeaderMapKeepsQuotedNumericAndBooleanLikeValuesAsStrings` へ改名。現在は `YamlMessageBuilderTest.java:1627`）。assert 3 件（`"0"`・`"1234"`・`"true"`）は変えていない |

**件数まとめ**: 落ちた既存テスト 4 件 ＝ テストコードを変えた **3 件** ＋ 変えなかった **1 件**。

この節は**是正（初回ラウンド）時点**の記録である。落ちなかった既存テスト（着手前ベースライン 280 件のうち
残り **276 件**）は、その時点では一切触っていない。ただし**最終状態では成立しない**。落ちていない既存テストにも
その後のレビュー是正で手を入れているためである。

- レビュー是正 1巡目: `buildMessagePool_malformedFwHeaderRowsThrowsException`（落ちていない）の `id` の assert を
  `containsString("malformed001")` から `containsString("id='malformed001'")` へ締めた **1 件**
  （下記「レビュー是正ラウンドの変更」の「変更したテスト 4 件」がこれを開示している）
- レビュー是正 2巡目: 変更 **6 件**・削除 **3 件**（下記「レビュー是正 2巡目の変更」）。ただしこの 9 件のうち
  **着手前（`876e342`）から存在したのは 1 件だけ**である（下記「着手前テストの行方（実測）」）

以前ここに置いていた「残り 276 件は一切触っていない」という一文は、同じファイルの下 2 つの表と矛盾していたため
上記に訂正した（レビュー 2巡目 G13）。さらに 3巡目で、2巡目の「落ちていない**既存テスト**を 6 件変更・3 件削除」という
書き方が実測と食い違うことが分かったため、上記のとおり訂正した（下記「着手前テストの行方（実測）」が内訳）。

## 着手前テストの行方（実測）

`@Test` の直後の `public void <名前>` を `876e342`（着手前）・`1b480b4`（初回ラウンド）・`cfcd2ae`（1巡目）・
`de31806`（2巡目）・現在の 5 リビジョンから機械的に抜き出して突き合わせた実測。

| リビジョン | `YamlMessageBuilderTest` の `@Test` 件数 |
|---|---|
| `876e342`（着手前） | 41 |
| `1b480b4`（初回ラウンド） | 44 |
| `cfcd2ae`（1巡目） | 53 |
| `de31806`（2巡目） | 52 |
| 現在（3巡目） | 52 |

**着手前の 41 件のうち、名前がそのまま残っているのは 38 件。**名前が変わった／消えたのは次の 3 件だけである。

| 着手前の名前 | 行方 |
|---|---|
| `buildMessagePool_fwHeaderMapAllKeysRetainedIncludingCustom` | 初回ラウンドで `buildMessagePool_fwHeaderMapAllDefaultKeysRetained` へ改名 → 2巡目で削除（`buildMessagePool_emptyFwHeaderFieldsBehavesAsUnset` と重なるため） |
| `buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues` | 1巡目で `buildMessagePool_fwHeaderMapKeepsQuotedNumericAndBooleanLikeValuesAsStrings` へ改名（現在 `YamlMessageBuilderTest.java:1627`） |
| `buildMessagePool_malformedFwHeaderRowsThrowsException` | 1巡目で `id` の assert を締め、2巡目で `buildMessagePool_nonMapFwHeaderThrowsExceptionWithTypeName` へ改名（現在 `YamlMessageBuilderTest.java:775`） |

**2巡目で変更した 6 件・削除した 3 件の出自**（`876e342` に同名が在ったかを実測）。

| 2巡目の扱い | テスト | `876e342` に在ったか |
|---|---|---|
| 変更 | `buildMessagePool_fwHeaderKeyNotInDefaultFieldsThrows` | いいえ（初回ラウンドで新規追加） |
| 変更 | `buildMessagePool_fwHeaderKeyNotInConfiguredFieldsThrows` | いいえ（初回ラウンドで新規追加） |
| 変更（改名） | `buildMessagePool_malformedFwHeaderRowsThrowsException` | **はい** |
| 変更（改名） | `fwHeaderFields_isIdenticalToMessageParserForEveryConfiguration` | いいえ（1巡目で新規追加） |
| 変更（改名） | `buildMessagePool_fwHeaderNullKeyThrowsIllegalStateExceptionNotNpe` | いいえ（1巡目で新規追加） |
| 変更 | `buildMessagePool_emptyFwHeaderFieldsBehavesAsUnset` | いいえ（1巡目で新規追加） |
| 削除 | `buildMessagePool_fwHeaderErrorMessageListsDefaultAllowedKeysInSortedOrder` | いいえ（1巡目で新規追加） |
| 削除 | `buildMessagePool_fwHeaderErrorMessageListsConfiguredAllowedKeysInSortedOrder` | いいえ（1巡目で新規追加） |
| 削除 | `buildMessagePool_fwHeaderMapAllDefaultKeysRetained` | いいえ（初回ラウンドで既存テストを改名して作った名前。元の名前は上表のとおり `..._AllKeysRetainedIncludingCustom`） |

つまり **2巡目に触れた 9 件のうち、着手前から名前が存在したのは `buildMessagePool_malformedFwHeaderRowsThrowsException` の 1 件だけ**で、
削除した 3 件に着手前から存在した名前は **0 件**である（ただし `..._AllDefaultKeysRetained` は着手前テストを改名したものであり、
着手前テストの中身をたどると上表のとおり削除に行き着く）。残りは初回ラウンド・1巡目で本タスクが自分で作ったテストの整理である。

## 是正したフィクスチャ（該当キー 3 件）

| ファイル:行（着手前） | キー | 是正 |
|---|---|---|
| 着手前の `YamlMessageBuilderTest/customFwHeaderData.yaml:9` | `customField` | **残した**。テスト側で `reader.fwHeaderfields` に `customField,requestId` を設定して通す（このフィクスチャは「設定した場合」と「設定しない場合（例外）」の両方で使う）。先頭コメントの「fwHeaderFields フィルタが廃止され、fw_header に記述した全キーが保持される」は是正後の挙動と食い違うため書き換え、解説書の出典を付けた（3巡目に節見出し形式へ揃えた。現在 `customFwHeaderData.yaml:2`-`:5`） |
| 着手前の `YamlMessageBuilderTest/fwHeaderMapData.yaml:14` | `customProjectKey` | **削除した**。この `req001` は独自キーと無関係な `buildMessagePool_fwHeaderMapReadableWithoutHeaderRecord` からも読まれるため、設定を足すより既定 4 キーだけのフィクスチャにするほうが素直と判断した。コメントも是正した（3巡目に節見出し形式へ揃えた。現在 `fwHeaderMapData.yaml:5`-`:9`） |
| 着手前の `YamlMessageBuilderTest/fwHeaderMapData.yaml:40` | `boolFlag` | **残した**。テスト側で `reader.fwHeaderfields` に `resendFlag,resultCode,boolFlag` を設定して通す（値の文字列化を確かめるテストの意味を保つため、キー名は変えない）。コメントも是正した（現在 `fwHeaderMapData.yaml:38`） |

`YamlMessageBuilderTest/messageData.yaml`・`YamlTestDataParserTest/messageData.yaml`・`YamlTestDataParserTest/schemaFullCoverage.yaml`・`YamlTestDataParserTest/legacyFwHeaderRecord.yaml`・`YamlLoaderTest/schemaViolation_expectedRequest_fwHeader.yaml` は `fw_header:` のキーが既定 4 つの範囲内で該当なし（変更していない。`grep -rln fw_header src/test --include=*.yaml` の全 8 件を 1 件ずつ確認）。`YamlFileBuilderTest/fileData.yaml` の `FW_HEADER` は `record_type` の値であって `fw_header:` ではない。

## 変異確認

追加 3 件・変更 4 件について 8 通りの変異を **1 つずつ**適用し、そのつど
`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlMessageBuilderTest -DfailIfNoTests=false` を実行した。
8 通りすべてで `Tests run: 44, Failures: 1, Errors: 0, Skipped: 0` / `BUILD FAILURE`（＝崩した 1 件だけが落ちる）。
確認後ただちに元へ戻し、`mvn -o clean test` で `Tests run: 283, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS` を再確認済み。

| # | テスト | 崩した内容 | 結果 |
|---|---|---|---|
| M1 | `buildMessagePool_fwHeaderKeyNotInDefaultFieldsThrows` | 不正キー名の期待 `'customField'` → `'zzzUnknown'` | 落ちた（Failures: 1） |
| M2 | 同上 | id の期待 `req001` → `req999` | 落ちた（Failures: 1） |
| M3 | `buildMessagePool_fwHeaderKeyNotInConfiguredFieldsThrows` | 期待キー名 `'requestId'` → `'customField'`（設定にあるキー） | 落ちた（Failures: 1） |
| M4 | `buildMessagePool_fwHeaderFieldsAreSplitByCommaWithoutTrimming` | 期待 `has unknown key 'requestId'` → `has unknown key 'customField'` | 落ちた（Failures: 1） |
| M5 | `buildMessagePool_customFwHeaderFields` | 期待値 `CUSTOM_VALUE` → `OTHER` | 落ちた（Failures: 1） |
| M6 | `buildMessagePool_fwHeaderMapAllDefaultKeysRetained` | 件数 `is(4)` → `is(5)` | 落ちた（Failures: 1） |
| M7 | `buildMessagePool_fwHeaderMapReadableWithoutHeaderRecord` | 期待値 `0000000001` → `9999999999` | 落ちた（Failures: 1） |
| M8 | `buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues` | 真偽値の期待 `true` → `false` | 落ちた（Failures: 1） |

（M8 のテストはレビュー是正ラウンドで `buildMessagePool_fwHeaderMapKeepsQuotedNumericAndBooleanLikeValuesAsStrings` へ改名した。同じ変異を M14 で再確認済み）

## レビュー是正ラウンドの変更

| 区分 | 件数 | 内訳 |
|---|---|---|
| 新規テスト | **9 件** | `buildMessagePool_fwHeaderErrorMessageListsDefaultAllowedKeysInSortedOrder`・`buildMessagePool_fwHeaderErrorMessageListsConfiguredAllowedKeysInSortedOrder`・`fwHeaderFields_isIdenticalToMessageParserForEveryConfiguration`・`buildMessagePool_emptyFwHeaderFieldsBehavesAsUnset`・`buildMessagePool_validEntryIsReadableThoughSameFileHasEntryWithUnknownKey`・`buildMessagePool_entryWithUnknownKeyThrowsWhenItIsRead`・`buildMessagePool_fwHeaderKeyIsCaseSensitive`・`buildMessagePool_fwHeaderNonStringKeyThrowsWithStringifiedKeyName`・`buildMessagePool_fwHeaderNullKeyThrowsIllegalStateExceptionNotNpe`（行番号は当時のものになるため載せない。現存する名前の現在位置は Completion Criteria 表を参照） |
| 変更したテスト | **4 件** | `id` の assert を `containsString("req001")` から `containsString("id='req001'")` の形へ締めた 3 件（`buildMessagePool_fwHeaderKeyNotInDefaultFieldsThrows`・`buildMessagePool_fwHeaderKeyNotInConfiguredFieldsThrows`、および既存の `must be a map` 側 `buildMessagePool_malformedFwHeaderRowsThrowsException`）。`buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues` を `buildMessagePool_fwHeaderMapKeepsQuotedNumericAndBooleanLikeValuesAsStrings` へ改名し javadoc を実態（フィクスチャの値は**クォート済み**）に合わせた 1 件。assert する値は 4 件とも変えていない |
| 新規フィクスチャ | **1 件** | `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest/mixedFwHeaderKeysData.yaml`（不正キーの `badKey001` と正常な `goodKey001` の 2 エントリ） |
| 既存フィクスチャ | **0 件** | 追加で 1 件も変えていない |
| 追加ヘルパー | **3 件** | `mainFwHeaderFields()`・`yamlFwHeaderFields()`・`yamlWithFwHeader()`（現在位置は `YamlMessageBuilderTest.java:137`・`:158`・`:189`） |
| 実装（`YamlMessageBuilder.java`） | javadoc 3 箇所＋定数 1 箇所 | クラス javadoc に「巻き添えにしない範囲はキーの検査に限る（値の型はスキーマがロード時に検査するのでファイル全体が落ちる）」（現在 `YamlMessageBuilder.java:34`-`:44`）と「集合外キーを例外にするのは本体との意図的な差異」（現在 `YamlMessageBuilder.java:46`-`:51`。本体側の根拠は解説書 `testdata_notation.rst:1264`「名前・値の行のうち、ディレクティブ名でなく `reader.fwHeaderfields` にも無い名前の行は、フレームワーク制御ヘッダではなくフィールド名称行として読み込まれる。」を自分で開いて確認）を追記。`convertFwHeader` の javadoc の `{@link #fwHeaderFields()}` を `{@code fwHeaderFields()}` へ（現在 `YamlMessageBuilder.java:270`。private メソッドへのリンクは生成 javadoc で解決されないため）。`DEFAULT_FW_HEADER_FIELDS` を書き換え可能な `String[]` から不変 `Set` へ（現在 `YamlMessageBuilder.java:81`-`:82`。未設定時の毎回の `HashSet` 生成も消えた） |

## 変異確認（レビュー是正ラウンド）

隔離コピー（`git worktree add --detach <scratchpad>/wt38 HEAD` に作業ツリーの 3 ファイルをコピー）で
**13 通り**の変異を 1 つずつ適用し、そのつど
`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlMessageBuilderTest -DfailIfNoTests=false` を実行した。
変異なしは `Tests run: 53, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`。
各回の実行後にファイルを復元し、最後に隔離コピーの実装・テストが作業ツリーと同一であること（`diff -q` → SAME）を
確認してから `git worktree remove --force` した。作業ツリーは一度も崩していない。

**実装を崩す変異（I1-I7）**

| # | 崩した内容（`YamlMessageBuilder.java`） | 結果（落ちたテスト） |
|---|---|---|
| I1 | **F1**: `+ "allowed keys (" + FW_HEADER_KEY + "): " + new TreeSet<String>(allowedFields)` を丸ごと `+ ""` に潰す | `Failures: 2` — `...ListsDefaultAllowedKeysInSortedOrder`・`...ListsConfiguredAllowedKeysInSortedOrder` |
| I2 | **F1**: `new TreeSet<String>(allowedFields)` → `allowedFields`（`HashSet` のまま出す＝順序が非決定に） | `Failures: 2` — 同上（既定4つの `HashSet` 反復順は `[requestId, resultCode, userId, resendFlag]` で辞書順と異なる） |
| I3 | **F2**: 2 箇所の `id='" + id + "'` を `id='" + id + "XYZ'` に崩す | `Failures: 7` — `id='...'` を assert する全テスト（`entryWithUnknownKeyThrowsWhenItIsRead`・`fwHeaderKeyIsCaseSensitive`・`fwHeaderKeyNotInConfiguredFieldsThrows`・`fwHeaderKeyNotInDefaultFieldsThrows`・`fwHeaderNonStringKeyThrows...`・`fwHeaderNullKeyThrows...`・`malformedFwHeaderRowsThrowsException`） |
| I4 | **F3**: 既定4つから `"userId"` を削る | `Failures: 2, Errors: 8` — うちパリティテスト `fwHeaderFields_isIdenticalToMessageParserForEveryConfiguration` が `reader.fwHeaderfields=[null]` で落ちた |
| I5 | **F3**: `NablarchTestUtils.makeArray(configured)` を trim する版 `configured.trim().split("\\s*,\\s*")` に差し替える | `Failures: 2` — パリティテストが `reader.fwHeaderfields=[ ]` で落ちた／`fwHeaderFieldsAreSplitByCommaWithoutTrimming` |
| I6 | **F5**: `StringUtil.isNullOrEmpty(configured)` → `configured == null`（空文字を未設定扱いしない） | `Failures: 2, Errors: 9` — うち `buildMessagePool_emptyFwHeaderFieldsBehavesAsUnset` とパリティテスト（`reader.fwHeaderfields=[]`） |
| I7 | **F4**: キーの検査を遅延実行でなく全エントリ一括に変える（`buildMessageContent` のループ先頭で id 一致前に `convertFwHeader` を呼ぶ） | `Errors: 2` — `validEntryIsReadableThoughSameFileHasEntryWithUnknownKey`（＝遅延実行が守られている）ほか |

**期待値を崩す変異（M9-M14）**

| # | テスト | 崩した内容 | 結果 |
|---|---|---|---|
| M9 | `buildMessagePool_entryWithUnknownKeyThrowsWhenItIsRead` | 期待キー名 `'customField'` → `'requestId'` | 落ちた（Failures: 1） |
| M10 | `buildMessagePool_fwHeaderKeyIsCaseSensitive` | 期待 `'" + key + "'` → 固定の `'requestId'` | 落ちた（Failures: 1） |
| M11 | `buildMessagePool_fwHeaderNonStringKeyThrowsWithStringifiedKeyName` | 数値キーの期待 `"1234"` → `"9999"` | 落ちた（Failures: 1） |
| M12 | `buildMessagePool_fwHeaderNullKeyThrowsIllegalStateExceptionNotNpe` | 期待 `'null'` → `'NULL'` | 落ちた（Failures: 1） |
| M13 | `buildMessagePool_validEntryIsReadableThoughSameFileHasEntryWithUnknownKey` | 期待値 `0000000001` → `9999999999` | 落ちた（Failures: 1） |
| M14 | `buildMessagePool_fwHeaderMapKeepsQuotedNumericAndBooleanLikeValuesAsStrings` | 真偽値の期待 `true` → `false` | 落ちた（Failures: 1） |

（`...ListsDefaultAllowedKeysInSortedOrder`・`...ListsConfiguredAllowedKeysInSortedOrder`・
`fwHeaderFields_isIdenticalToMessageParserForEveryConfiguration`・`buildMessagePool_emptyFwHeaderFieldsBehavesAsUnset`・
`id` の assert を締めた 3 件は、実装を崩す I1-I6 で落ちることを確認済みのため期待値側の変異は行っていない）

## レビュー是正 2巡目の変更

レビュー 4 軸の 2巡目で出た指摘 G1-G13 への対応。

| 区分 | 件数 | 内訳 |
|---|---|---|
| 新規テスト | **3 件** | `fwHeaderFields_returnsUnmodifiableSetInBothBranches`（現在 `YamlMessageBuilderTest.java:1138`。G2・G3）・`fwHeaderIsNotCheckedOutsideMessagesPathAndBecomesEmptyMap`（現在 `:1479`。G5）・`buildMessagePool_fwHeaderTildeKeyIsReadAsStringAndRejected`（現在 `:1305`。G1） |
| 変更したテスト | **6 件** | `buildMessagePool_fwHeaderKeyNotInDefaultFieldsThrows`（現在 `:965`。`allowed keys` の assert を吸収。G9-2）・`buildMessagePool_fwHeaderKeyNotInConfiguredFieldsThrows`（現在 `:1014`。設定値を `"customField"` から `"userId,customField"` へ変え `allowed keys` の assert を吸収。G9-2）・`buildMessagePool_malformedFwHeaderRowsThrowsException` → `buildMessagePool_nonMapFwHeaderThrowsExceptionWithTypeName`（現在 `:775`。改名し、型名の assert とスカラ 2 ケースを追加、ヘルパー `yamlWithFwHeader` を適用。G4・G9-4）・`fwHeaderFields_isIdenticalToMessageParserForEveryConfiguration` → `..._ForListedConfigurations`（現在 `:1099`。改名し設定を 7→8 通りへ。G6・G8）・`buildMessagePool_fwHeaderNullKeyThrowsIllegalStateExceptionNotNpe` → `..._fwHeaderNullKeyIsStringifiedInDefensiveBranch`（3巡目にさらに `..._fwHeaderNullKeyIsRejectedInDefensiveBranch` へ改名。現在 `:1402`。G1）・`buildMessagePool_emptyFwHeaderFieldsBehavesAsUnset`（現在 `:1174`。MS-04 の観点を javadoc に吸収。G9-1） |
| 削除したテスト | **3 件** | `buildMessagePool_fwHeaderErrorMessageListsDefaultAllowedKeysInSortedOrder`・`..._ListsConfiguredAllowedKeysInSortedOrder`（G9-2。assert を既存 2 件へ移した）・`buildMessagePool_fwHeaderMapAllDefaultKeysRetained`（G9-1。`emptyFwHeaderFieldsBehavesAsUnset` と同じフィクスチャ `fwHeaderMapData` の同じ id `req001` に対し、同じ期待値を見る assert が 5 件重なっていた。**逐語ではない**: assert のメッセージ文言は 5 件中 4 件が違い（「requestId が設定されていること」対「requestId が通ること」など）、`fwHeader` の取り出しも直接リフレクション対ヘルパー `getFwHeader` で違う。同一なのは 5 件目「既定 4 キーだけが設定されていること」の文言と、5 件すべての期待値・比較対象である） |
| 畳み込んだテスト | **1 件** | `buildMessagePool_entryWithUnknownKeyThrowsWhenItIsRead` を `buildMessagePool_validEntryIsReadableThoughSameFileHasEntryWithUnknownKey` へ畳み、`buildMessagePool_unknownKeyIsCheckedOnlyForTheEntryBeingRead`（現在 `:1215`）に改名（G9-3）。**assert は 1 つも捨てていない**（両テストの assert 5 件がそのまま 1 メソッドに並ぶ） |
| 追加ヘルパー・定数 | **2 件** | `UPSTREAM_CHANGED`（現在 `:64`-`:66`。G7）・`assertUnmodifiable`（3巡目にヘルパーブロックへ移動。現在 `:165`-`:178`。G2） |
| フィクスチャ | **1 件** | `YamlMessageBuilderTest/mixedFwHeaderKeysData.yaml`。先頭コメントを解説書の引用文つきに改め、**前提**（スキーマ `$defs.fw_header` がキー名を制限していないから遅延実行を示せる）を明記（G12）。エントリ `tildeKey001`（`fw_header:` のキーが `~`）を追加（G1） |
| 実装（`YamlMessageBuilder.java`） | **4 箇所** | クラス javadoc の解説書引用を行番号から節見出し＋引用文へ（現在 `YamlMessageBuilder.java:53`-`:67`。G11）・`formatAllowedFields` を追加し各名前をクォート（現在 `YamlMessageBuilder.java:301`-`:326`・`YamlMessageBuilder.java:294`。G10）・`fwHeaderFields()` の設定あり分岐も `Collections.unmodifiableSet` で包み javadoc に契約を明記（現在 `YamlMessageBuilder.java:341`-`:346`・`YamlMessageBuilder.java:354`-`:355`。G3）・`convertFwHeader` 周辺の既存 javadoc は変更なし |
| 変更していないもの | — | スキーマ `ntf-testdata-yaml-schema.json`（#42 Step B の担当）・`.rn/ntf-yaml/steering.md`・`../nablarch-testing`・`../nablarch-document` |

### 実測で確かめた事実（G1・G4・G5・G6）

隔離コピー（`git worktree add --detach`）に使い捨ての `ProbeTest` を置いて実行した結果。

| 調べたこと | 実測結果 |
|---|---|
| `fw_header:` のキーに `~` と書いたとき | `YamlLoader.load` 後のキーは `java.lang.String` の `"~"`。**null ではない**。`convertFwHeader` に届き `has unknown key '~'` になる（実 YAML から到達可能） |
| `fw_header:` のキーに `null` と書いたとき | 真の Java `null` キーになるが `convertFwHeader` に**届かない**。`YamlLoader.java:151` の `OBJECT_MAPPER.valueToTree` が `IllegalArgumentException: Null key for a Map not allowed in JSON (use a converting NullKeySerializer?)` を投げ、スキーマ検証の手前でファイル全体のロードが失敗する |
| `fw_header: "NOT_A_MAP"`（スカラ） | `YamlSchemaValidationException`。スキーマ `$defs.fw_header` の `"type": "object"` がロード時に弾く。**マップ以外は実 YAML から到達不能** |
| 非 `messages` セクションに `fw_header:` を書く | `YamlSchemaValidationException`（`$.response_body_messages[0]: プロパティ 'fw_header' がスキーマで定義されておらず、スキーマでは追加のプロパティが許可されていません`）。`expected_request_body_messages` も同じ。**到達不能** |
| `reader.fwHeaderfields=",requestId"` のときの本体の挙動 | 本体 `MessageParser` の `fwHeaderFields` は `[, requestId]`。private `isFrameworkHeader("")` をリフレクション実行して `true`、`isFrameworkHeader("userId")` は `false`。**YAML 側の素通りと同じ挙動** |

### 判断（G5・G6・G9）

- **G5**: 実 YAML では書けない（上表）。ただし「書けないから足さない」ではなく、スキーマ検証を通さない合成 Map で
  `useFwHeader=false` の経路と `buildSendSyncList` の経路を**直接呼ぶ**テストを足した（現在 `YamlMessageBuilderTest.java:1479`）。指摘の変異
  （`useFwHeader` が false でも `convertFwHeader` を通す）が殺せることを N4 で実測した
- **G6**: パリティテストの設定に `",requestId"` を足した（現在 `YamlMessageBuilderTest.java:1099`）。**空文字キーが通ることを個別のテストで
  仕様固定はしない**と判断した。根拠は、素通りは「集合が本体と一致すること」の帰結であって選択した仕様ではないため
  （上表のとおり本体の `isFrameworkHeader("")` も `true` を返す）。固定すべきは集合の一致であり、そこはパリティ
  テストが押さえる。この判断は現在 `YamlMessageBuilderTest.java:1099` の javadoc にも実測つきで書いた。追加した 1 要素が効いていることは、
  `","` 単独では差が出ず `",requestId"` でだけ差が出る変異（N11）で確かめた
- **G9**: 4 件すべて解消した。削除・畳み込みで変異カバレッジが落ちないことを N5（`allowed keys` を潰す）・
  N6（`TreeSet` をやめる）・N8（`isNullOrEmpty` を `== null` に）・N9（キー検査を一括実行に）・N16（既定キーを 5 つに）
  で実測した。いずれも削除前と同じく落ちる。G9-3 は assert を 1 つも捨てずに 1 メソッドへ並べたため、
  構成上カバレッジは変わらない

## 変異確認（レビュー是正 2巡目）

隔離コピー（`git worktree add --detach <scratchpad>/wt38 HEAD` に作業ツリーの 3 ファイルをコピー）で
**16 通り**の変異を 1 つずつ適用し、そのつど
`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test -Dtest=YamlMessageBuilderTest -DfailIfNoTests=false` を実行した。
変異なしは `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`。
各回の実行後にファイルを復元し、最後に隔離コピーの 3 ファイルが作業ツリーと同一（`diff -q` → SAME）であることを
確認してから `git worktree remove --force` した。作業ツリーは一度も崩していない。

**実装を崩す変異（N1-N11・N16）**

| # | 崩した内容（`YamlMessageBuilder.java`） | 結果（落ちたテスト） |
|---|---|---|
| N1 | `Collections.unmodifiableSet(...)` を**両分岐**から外す（G2・G3） | `Failures: 3` — `fwHeaderFields_returnsUnmodifiableSetInBothBranches`（`add` できてしまう）ほか、共有の既定集合が汚染されて `fwHeaderKeyNotInDefaultFieldsThrows`・パリティテストも落ちた |
| N2 | `but was: " + fwHeaderObj.getClass().getSimpleName()` を固定文字列 `but was: List` にする（G4） | `Failures: 1` — `buildMessagePool_nonMapFwHeaderThrowsExceptionWithTypeName` |
| N3 | `!(fwHeaderObj instanceof Map)` を `fwHeaderObj instanceof List` にする（G4） | `Errors: 1` — 同上（スカラが検査をすり抜け `ClassCastException` になる） |
| N4 | `useFwHeader ? convertFwHeader(...) : emptyMap()` を無条件 `convertFwHeader(...)` にする（G5） | `Errors: 1` — `fwHeaderIsNotCheckedOutsideMessagesPathAndBecomesEmptyMap` |
| N5 | `allowed keys (...) + formatAllowedFields(...)` を丸ごと `+ ""` に潰す（G9-2 の削除が安全かの確認） | `Failures: 2` — `fwHeaderKeyNotInDefaultFieldsThrows`・`fwHeaderKeyNotInConfiguredFieldsThrows`（削除した専用 2 件と同じだけ落ちる） |
| N6 | `new TreeSet<String>(allowedFields)` を `allowedFields` にする（辞書順をやめる。同上） | `Failures: 2` — 同上 |
| N7 | `formatAllowedFields(...)` をやめ `new TreeSet<String>(allowedFields)` の `toString()` に戻す（G10 のクォートを外す） | `Failures: 2` — 同上 |
| N8 | `StringUtil.isNullOrEmpty(configured)` を `configured == null` にする（G9-1 の削除が安全かの確認） | `Failures: 2, Errors: 8` — うち `emptyFwHeaderFieldsBehavesAsUnset`・パリティテスト（`reader.fwHeaderfields=[]`） |
| N9 | キー検査を遅延実行でなく全エントリ一括にする（G9-3 の畳み込みが安全かの確認） | `Failures: 1, Errors: 2` — `unknownKeyIsCheckedOnlyForTheEntryBeingRead`・`fwHeaderTildeKeyIsReadAsStringAndRejected` ほか |
| N10 | `makeArray` を空要素を捨てる版（`configured.replaceAll("^,","").split(",")`）に差し替える | `Failures: 1` — パリティテストが `reader.fwHeaderfields=[,]` で落ちた |
| N11 | 先頭カンマ**だけ**を落とす（`configured.startsWith(",") ? configured.substring(1) : configured`。`","` 単独では差が出ない変異） | `Failures: 1` — パリティテストが `reader.fwHeaderfields=[,requestId]` で落ちた（**G6 で足した 1 要素が効いていることの確認**） |
| N16 | 既定 4 つに 5 つ目 `"extra"` を足す（G9-1 の削除が安全かの確認） | `Failures: 2` — `fwHeaderKeyNotInDefaultFieldsThrows`・パリティテスト |

**テスト側を崩す変異（N12-N15）**

| # | テスト | 崩した内容 | 結果 |
|---|---|---|---|
| N12 | `mainFwHeaderFields()` ヘルパー（G7） | 本体のフィールド名が変わった状況を模して `getDeclaredField("fwHeaderFields")` を `"fwHeaderFieldsXYZ"` にする | `Failures: 1`。素の `NoSuchFieldException` ではなく `本体 ../nablarch-testing/src/main/java/nablarch/test/core/reader/MessageParser.java:107-:110 の集合生成が変わった可能性がある。YamlMessageBuilder.fwHeaderFields() を本体に合わせ直すこと。 （private フィールド fwHeaderFields が見つからない: java.lang.NoSuchFieldException: fwHeaderFieldsXYZ）` で落ちた |
| N13 | `buildMessagePool_fwHeaderTildeKeyIsReadAsStringAndRejected` | 期待キー `'~'` → `'x'` | 落ちた（`Failures: 1`） |
| N14 | `fwHeaderIsNotCheckedOutsideMessagesPathAndBecomesEmptyMap` | 空 Map の期待 `is(0)` → `is(1)` | 落ちた（`Failures: 1`） |
| N15 | `buildMessagePool_nonMapFwHeaderThrowsExceptionWithTypeName` | 文字列ケースの期待型名 `"String"` → `"ArrayList"` | 落ちた（`Failures: 1`） |

## レビュー是正 3巡目の変更

レビュー 3巡目で出た指摘 H1-H17 への対応。**新規テスト 0 件・削除 0 件**（`@Test` は 52 件のまま）。

| 区分 | 件数 | 内訳 |
|---|---|---|
| 改名したテスト | **1 件** | `buildMessagePool_fwHeaderNullKeyIsStringifiedInDefensiveBranch` → `buildMessagePool_fwHeaderNullKeyIsRejectedInDefensiveBranch`（現在 `YamlMessageBuilderTest.java:1402`。H2。旧名は「`objectToString` が `"null"` に文字列化する」という誤りを名前に固定していた） |
| 合成 Map から実フィクスチャへ移したテスト | **2 件** | `buildMessagePool_fwHeaderKeyIsCaseSensitive`（現在 `YamlMessageBuilderTest.java:1265`）・`buildMessagePool_fwHeaderNonStringKeyThrowsWithStringifiedKeyName`（現在 `YamlMessageBuilderTest.java:1344`）。どちらも `mixedFwHeaderKeysData.yaml` の実エントリを読む形にした（H16） |
| assert を足したテスト | **1 件** | `buildMessagePool_fwHeaderFieldsAreSplitByCommaWithoutTrimming`（現在 `YamlMessageBuilderTest.java:1049`）に `allowed keys` の全文 assert を 1 行追加（現在 `YamlMessageBuilderTest.java:1066`）。期待値 `[' requestId', 'customField']` は実行して得た実際の出力（H8） |
| javadoc・コメントの是正 | **7 箇所** | H1（`YamlMessageBuilderTest.java:994`-`:1002` の「`userId` が通ることは押さえていない」）・H2（`YamlMessageBuilderTest.java:1369`-`:1375`）・H5（`<p>` の二重開始 2 箇所を除去。`cfcd2ae` 時点 0 箇所・`de31806` 時点 2 箇所を `awk` で実測）・H12（セクションコメントの出典 `YamlMessageBuilderTest.java:938`-`:941` を節見出し形式へ）・H13（`YamlMessageBuilderTest.java:1123`-`:1128`）・H14（`YamlMessageBuilderTest.java:1158`-`:1163`）・javadoc 内の本体参照 `MessageParser.java:107` → `MessageParser.java:108`（`isNullOrEmpty` の実際の行）・javadoc 内のフィクスチャ参照 `fwHeaderMapData.yaml:38`-`:40` → `fwHeaderMapData.yaml:41`-`:43` |
| ヘルパーの整理 | **2 件** | `mainFwHeaderFields()` の `fail(メッセージ + e)` を `throw new AssertionError(メッセージ, e)` にして cause を残し、デッドコード `throw e;` を削除（現在 `YamlMessageBuilderTest.java:143`-`:144`・`YamlMessageBuilderTest.java:150`-`:151`。H10）・`assertUnmodifiable` をテストメソッド群からヘルパーブロックへ移動（現在 `YamlMessageBuilderTest.java:165`-`:178`。H11） |
| フィクスチャ | **3 件** | `mixedFwHeaderKeysData.yaml` にエントリ 4 件追加（`mixedFwHeaderKeysData.yaml:58` lowerCaseKey001・`mixedFwHeaderKeysData.yaml:70` upperCaseKey001・`mixedFwHeaderKeysData.yaml:85` numericKey001・`mixedFwHeaderKeysData.yaml:97` booleanKey001。H16）。`customFwHeaderData.yaml:2`-`:5`・`fwHeaderMapData.yaml:5`-`:9` の出典を行番号形式から節見出し形式へ（H12） |
| 実装（`YamlMessageBuilder.java`） | **3 箇所** | クラス javadoc の解説書出典に親節「メッセージングのデータを記述する」を添えて一意化（`YamlMessageBuilder.java:53`-`:67`。H7）・`formatAllowedFields` の javadoc の「空白が見えない」を実測した出力（`[ requestId, customField]` と `[' requestId', 'customField']`）に基づく「見分けにくい」へ（`YamlMessageBuilder.java:304`-`:312`。H9）・`fwHeaderFields()` の javadoc の不変性の理由づけを反実仮想へ（`YamlMessageBuilder.java:341`-`:346`。H6） |
| 変更していないもの | — | スキーマ `ntf-testdata-yaml-schema.json`（#42 Step B の担当）・`.rn/ntf-yaml/steering.md`・`../nablarch-testing`・`../nablarch-document` |

### 実測で確かめた事実（3巡目）

隔離コピー（`git worktree add --detach <scratchpad>/wt38fix`）に使い捨ての `ProbeTest` を置いて実行した結果。

| 調べたこと | 実測結果 |
|---|---|
| `fw_header:` のキーにクォートなしの数値 `1234` を書いたとき | ロードは成功し、キーは `java.lang.Integer` の `1234`。**実 YAML から到達可能**（Jackson の `valueToTree` が JSON 化でキーを文字列にするためスキーマを通過し、Java 側の Map ではキーは `Integer` のまま残る） |
| `fw_header:` のキーにクォートなしの真偽値 `true` を書いたとき | 同上。キーは `java.lang.Boolean` の `true`。**到達可能** |
| `fw_header:` のキーに `requestid`／`REQUESTID` を書いたとき | ロードは成功し、キーは `java.lang.String`。**到達可能** |
| `fw_header:` の**値**に `~` を書いたとき | ロードは成功し、値は `java.lang.String` の `"~"`。**null ではない**（キーの場合と同じ） |
| `fw_header:` の**値**を省略したとき（`userId:`） | `YamlSchemaValidationException`（`$.messages[0].fw_header.userId: null が見つかりました、string が予期されました`）。**Java の null 値は実 YAML から到達不能** |
| `fw_header:` の**値**にクォートなしの数値を書いたとき（`requestId: 1234`） | `YamlSchemaValidationException`（`$.messages[1].fw_header.requestId: integer が見つかりました、string が予期されました`） |
| `fw_header: "NOT_A_MAP"`（スカラ） | `YamlSchemaValidationException`（`$.messages[0].fw_header: string が見つかりました、object が予期されました`）。**マップ以外は到達不能**（2巡目の実測を再確認） |
| `fw_header:` のキーに `null` と書いたとき | `IllegalArgumentException: Null key for a Map not allowed in JSON (use a converting NullKeySerializer?) (through reference chain: ...LinkedHashMap["fw_header"]->java.util.LinkedHashMap["null"])`。**真の null キーは到達不能**（2巡目の実測を再確認） |
| `reader.fwHeaderfields` の設定 `"customField, requestId"` に対する出力 | 許可集合の `Set#toString()` は `[ requestId, customField]`、`formatAllowedFields` の出力は `[' requestId', 'customField']`（H8・H9 の根拠） |
| `reader.fwHeaderfields` が真に未設定（`null`）で走るテストの件数 | `@Before` で値を出力して実測。**1 件**（クラス内で最初に走る `buildMessagePool_expectedRequestBodyMessages`）が `<null>`、残り 51 件が `[]`。`src/test/resources` にこのキーの定義は無い（`grep -rn fwHeaderfields src/test/resources` が 0 件） |

## 変異確認（レビュー是正 3巡目）

隔離コピー（`git worktree add --detach <scratchpad>/wt38fix HEAD` に作業ツリーのファイルをコピー）で
**8 通り**の変異を 1 つずつ適用し、そのつど `mvn -o clean test`（O1-O6 は `-Dtest=YamlMessageBuilderTest`）を実行した。
変異なしは `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`（クラス単体）／
`Tests run: 291, Failures: 0, Errors: 0, Skipped: 1`（全体）で BUILD SUCCESS。
各回の実行後にファイルを復元し、最後に `git worktree remove` した。作業ツリーは一度も崩していない。

| # | 崩した内容 | 結果 |
|---|---|---|
| O1 | `formatAllowedFields` のクォート（`sb.append('\'')…`）を外す | `Failures: 3` — `fwHeaderFieldsAreSplitByCommaWithoutTrimming`・`fwHeaderKeyNotInConfiguredFieldsThrows`・`fwHeaderKeyNotInDefaultFieldsThrows` |
| O2 | `formatAllowedFields` で項目名を `field.trim()` にする（**空白入りの名前だけが差になる変異**） | `Failures: 1` — `fwHeaderFieldsAreSplitByCommaWithoutTrimming` のみ。**H8 で足した 1 行が唯一これを殺す**ことの確認 |
| O3 | キーの照合を `equalsIgnoreCase` に変える | `Failures: 1` — `buildMessagePool_fwHeaderKeyIsCaseSensitive`（実フィクスチャ版が実 YAML 経由で殺せることの確認） |
| O4 | `buildMessagePool_fwHeaderNonStringKeyThrowsWithStringifiedKeyName` の期待値 `"1234"` → `"9999"` | `Failures: 1` |
| O5 | `buildMessagePool_fwHeaderKeyIsCaseSensitive` の期待値 `"REQUESTID"` → `"requestId"` | `Failures: 1` |
| O6 | `mainFwHeaderFields()` の `getDeclaredField("fwHeaderFields")` → `"fwHeaderFieldsXYZ"` | `Failures: 1`。`java.lang.AssertionError: 本体 …MessageParser.java:107-:110 の集合生成が変わった可能性がある。… （private フィールド fwHeaderFields が見つからない）` に続けて `Caused by: java.lang.NoSuchFieldException: fwHeaderFieldsXYZ` が出た（**H10 の cause 保存の確認**） |
| P1 | `Collections.unmodifiableSet(...)` を**両分岐**から外し、かつ `fwHeaderFields_returnsUnmodifiableSetInBothBranches` を `@Ignore` にする | `Tests run: 291, Failures: 0, Errors: 0, Skipped: 2` / **BUILD SUCCESS**（＝このテストが無ければ他のどのテストも落ちない。H13 の前半） |
| P2 | P1 と同じ実装変異のまま、`@Ignore` を外す | `Tests run: 291, Failures: 3` — `fwHeaderFields_returnsUnmodifiableSetInBothBranches`（`add` できてしまう）・`fwHeaderFields_isIdenticalToMessageParserForListedConfigurations`・`buildMessagePool_fwHeaderKeyNotInDefaultFieldsThrows`（この `add` が共有の既定集合を汚染するため）。H13 の後半 |

## 生き残っている変異（却下・実測つき）

次の 3 つの変異は隔離コピーで実際に適用したが、`mvn -o clean test` が
`Tests run: 291, Failures: 0, Errors: 0, Skipped: 1` / BUILD SUCCESS のままだった（3 件とも自分で実行して確認）。
いずれもテストを足して殺すのではなく、**却下**する。理由は下表のとおり実測で確かめた。

| # | 変異 | 生き残る理由（実測） |
|---|---|---|
| S1 | 値側 `objectToString(kv.getValue())` → `String.valueOf(kv.getValue())`（`YamlMessageBuilder.java:296`） | 両者の結果が違う唯一の入力は Java の `null` 値（`objectToString` は `null`、`String.valueOf` は `"null"`）。実 YAML では `fw_header:` の値を省略すると `YamlSchemaValidationException`（`null が見つかりました、string が予期されました`）でロードが落ち、`~` と書いた場合は文字列 `"~"` になる。**Java の null 値は実 YAML から到達不能**なので同値 |
| S2 | キー側 `objectToString(kv.getKey())` → `String.valueOf(kv.getKey())`（`:290`） | 同じく違いが出るのは null キーのみ。実 YAML では `YamlLoader` がロード時に `IllegalArgumentException: Null key for a Map not allowed in JSON` で落ちるため**到達不能**。さらに、合成 Map で到達させても例外メッセージは変わらない（`allowedFields.contains(null)` も `contains("null")` も `false` で、メッセージは文字列連結によりどちらも `unknown key 'null'`）ため、テストからは原理的に区別できない |
| S3 | `new LinkedHashMap<String, String>()` → `new TreeMap<String, String>()`（`:288`） | 変わるのは `fwHeader` の格納順だけである。順序は仕様ではない — 本体 `MessageParser` の同じ役割のフィールドが `private final Map<String, String> fwHeader = new HashMap<String, String>();`（`../nablarch-testing/src/main/java/nablarch/test/core/reader/MessageParser.java:30`）で、順序を保証していないことを開いて確認した。YAML 側が `LinkedHashMap` で記述順を保つのは本体より強い保証であり、それを仕様として固定する理由が無いため、順序の assert は置かない |

## 別タスク候補（本タスクでは直さない・実測つき）

`YamlLoader.load` は、YAML のマップに真の `null` キーがあると Jackson の生の
`IllegalArgumentException: Null key for a Map not allowed in JSON (use a converting NullKeySerializer?)` を
そのまま漏らす（`YamlLoader.java:151` の `OBJECT_MAPPER.valueToTree`）。`YamlSchemaValidationException` や
`IllegalStateException("Failed to ... file: " + filePath)` と違い、**どのファイルで起きたかが分からない**
（Jackson の reference chain にファイルパスは入らない）。隔離コピーの `ProbeTest` で
`fw_header: {null: "V"}` を書いた YAML をロードして実測した。`YamlLoader` の課題であり #38 のスコープ外のため
直していない。

## カバレッジ（JaCoCo・実測）

プロジェクトの `pom.xml` に JaCoCo の設定は無く（`grep -c jacoco pom.xml` → `0`）、親 `nablarch-parent:6-NEXT-SNAPSHOT` が
`instrument`／`restore-instrumented-classes` の 2 ゴールだけを定義している（`report` は無く、`jacoco.exec` は
オフライン計測のため**プロジェクト直下**に出る）。そのため次の順で計測した。

1. `rm -f jacoco.exec`（過去の実行分の追記を避ける）
2. `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
3. `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o org.jacoco:jacoco-maven-plugin:0.8.8:restore-instrumented-classes org.jacoco:jacoco-maven-plugin:0.8.8:report -Djacoco.dataFile=jacoco.exec` → `target/site/jacoco/`

`src/main` 全体（9 クラス）: **行 99.0%（404/408）・分岐 98.9%（176/178）・命令 99.3%（1758/1771）**
（レビュー是正 2巡目の後に取り直した実測。`target/site/jacoco/jacoco.xml` を機械的に集計）。

| クラス | 行 | 分岐 |
|---|---|---|
| **`YamlMessageBuilder`（本タスクの変更先）** | **80/80（100%）** | **38/38（100%）** |
| `YamlSection` | 54/54 | 52/52 |
| `YamlTableDataBuilder` | 67/67 | 30/30 |
| `YamlTestDataParser` | 56/56 | 2/2 |
| `YamlFileBuilder` | 84/85 | 41/42 |
| `YamlLoader` | 48/51 | 13/14 |
| `YamlSchemaValidationException` | 7/7 | 分岐なし |
| `InterpreterResolver` | 2/2 | 分岐なし |
| `MessageContent` | 6/6 | 分岐なし |

未到達は 2 箇所だけで、いずれも本タスクと無関係な既存の防御的コードである（`target/site/jacoco/jacoco.xml` の
`mi`/`mb` が非 0 の行を機械的に列挙して特定）。

- `YamlFileBuilder.java:237`-`:238` — `rows:` の要素が `List` でない場合の `continue`。コメント（`:235`-`:236`）が
  「Java 言語仕様上この分岐は通常到達不能」と明記している防御ガード
- `YamlLoader.java:60`-`:61`・`:65`-`:66` — `static` 初期化子でスキーマがクラスパスに無い／`IOException` の場合の
  `IllegalStateException`。クラスパスを壊さない限り到達しない

追加した検査（`YamlMessageBuilder.java:291`-`:295`）・集合生成の 2 分岐（`:352`-`:355` の設定あり／なし）・
2巡目で足した `formatAllowedFields`（`:317`-`:326`。空集合／非空集合の 2 分岐）はいずれも到達しており、
`YamlMessageBuilder` は行・分岐とも 100% である（行 71→80・分岐 34→38 は 2巡目の実装追加による。
3巡目の変更は javadoc だけなので 80/80・38/38 のまま。3巡目に取り直した実測）。

### 100% という数字の読み方（#43 の報告書で使う数字）

**`YamlMessageBuilder` の行・分岐 100% のうち、1 分岐・2 行は合成 Map（スキーマ検証を通さない直接呼び出し）
からしか到達しない。**合成 Map を使う 3 件のテスト
（`buildMessagePool_nonMapFwHeaderThrowsExceptionWithTypeName`・
`buildMessagePool_fwHeaderNullKeyIsRejectedInDefensiveBranch`・
`fwHeaderIsNotCheckedOutsideMessagesPathAndBecomesEmptyMap`）を隔離コピーで `@Ignore` にして計測したところ、
`YamlMessageBuilder` は **分岐 37/38・行 78/80** に下がり、未到達になったのは次の 1 箇所だけだった。

- `YamlMessageBuilder.java:282` の `if (!(fwHeaderObj instanceof Map))` の **true 側**（`mb=1`）と、
  その中の `:283`・`:285`（`must be a map, but was: <型名>` を投げる 2 行）

実 YAML では、スキーマ `$defs.fw_header` の `"type": "object"` がロード時に弾くためこの分岐に入れない
（隔離コピーで `fw_header: "NOT_A_MAP"` を書いた YAML をロードし、
`YamlSchemaValidationException: ... $.messages[0].fw_header: string が見つかりました、object が予期されました` を実測）。

残りの分岐は 3巡目までに実 YAML から到達させてある。とくに 3巡目では、キーの境界値
（大文字小文字違い・数値・真偽値）を合成 Map から実フィクスチャ `mixedFwHeaderKeysData.yaml` へ移した
（数値キーは `java.lang.Integer`、真偽値キーは `java.lang.Boolean` のまま `convertFwHeader` に届くことを実測。
下記「レビュー是正 3巡目の変更」）。null キーだけは実 YAML から到達しないため合成 Map のままだが、
これは分岐を増やしていない（上記の計測で `:291` の分岐は 3 件を `@Ignore` にしても覆われたままだった）。

したがって **100% を「実運用で入り得る入力すべてに対する防御度」と読み替えてはならない**。
到達不能な防御分岐を 1 つ含んだうえでの 100% である。

## スコープ遵守

- 変更したのは 5 ファイルのみ: `src/main/java/.../YamlMessageBuilder.java`、
  `src/test/java/.../YamlMessageBuilderTest.java`、`.../YamlMessageBuilderTest/customFwHeaderData.yaml`、
  `.../YamlMessageBuilderTest/fwHeaderMapData.yaml`、および新規追加の
  `.../YamlMessageBuilderTest/mixedFwHeaderKeysData.yaml`（レビュー是正 1巡目で追加、2巡目・3巡目でエントリを追加）
- スキーマ `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` は触っていない。`ntf-testdata-yaml-schema.json:216` と `ntf-testdata-yaml-schema.json:433` の
  「記述したキーはすべて FW 制御ヘッダとして NTF に渡される」、および `ntf-testdata-yaml-schema.json:434` の「任意のヘッダ名を許容する」は是正後の挙動と
  食い違うが、これは **#42 Step B の担当**のため残した
- 隣接タスク（#39 空行判定・#40 `\`+`r`・#41 `@Ignore` 削除・#42 スキーマ `description`）には手を出していない
- `.rn/ntf-yaml/steering.md` は変更なし。`../nablarch-testing`・`../nablarch-document` は読むだけで変更なし
- `@Ignore` は新たに足していない

## 指示（Steps C）と実物の照合

- **キー 3 件**: 一致した（いずれも着手前 `876e342` の行番号を自分で開いて確認）。着手前の `customFwHeaderData.yaml:9` `customField`、
  着手前の `fwHeaderMapData.yaml:14` `customProjectKey`、着手前の `fwHeaderMapData.yaml:40` `boolFlag`。加えて
  `grep -rln fw_header src/test --include=*.yaml` の 8 ファイルを全件確認し、他に該当キーが無いことを確かめた
- **テスト 4 件**: 一致した（行番号はいずれも着手前 `876e342` の `YamlMessageBuilderTest.java`）。
  `buildMessagePool_customFwHeaderFields`（着手前 `YamlMessageBuilderTest.java:792`）・
  `buildMessagePool_fwHeaderMapAllKeysRetainedIncludingCustom`（着手前 `YamlMessageBuilderTest.java:824`）・
  `buildMessagePool_fwHeaderMapReadableWithoutHeaderRecord`（着手前 `YamlMessageBuilderTest.java:854`）・
  `buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues`（着手前 `YamlMessageBuilderTest.java:985`）。行番号・名前とも着手前の実物と一致し、
  是正後の実測でもこの 4 件だけが落ちた
- **食い違い（軽微）**: Steps A は `convertFwHeader` を「233 行目から 246 行目」とするが、実物は
  メソッド宣言 着手前の `YamlMessageBuilder.java:233` から閉じ括弧 着手前の `YamlMessageBuilder.java:247` までである（同ファイルは着手前 248 行）。
  挙動に影響はない
- **記録しておく点（本タスクでは直していない）**: `buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues` は
  「クォートなしの数値・真偽値」を謳うが、フィクスチャ `fwHeaderMapData.yaml` の値（着手前の `fwHeaderMapData.yaml:38`-着手前の `fwHeaderMapData.yaml:40`／現在 `fwHeaderMapData.yaml:41`-`:43`）は着手前から
  `"0"`・`"1234"`・`"true"` と**クォート済み**である（スキーマ `$defs.fw_header` の
  `additionalProperties.type: "string"`（`ntf-testdata-yaml-schema.json:430`-`:432`）がクォートなしの数値・真偽値を
  弾くため）。テスト名・javadoc とフィクスチャのこの食い違いは着手前からあったが、レビュー是正ラウンドで
  テスト名を `buildMessagePool_fwHeaderMapKeepsQuotedNumericAndBooleanLikeValuesAsStrings` へ改め、javadoc も
  実態（クォート済みの値が文字列のまま素通しされること）に合わせて解消した（現在 `YamlMessageBuilderTest.java:1627`）。
  フィクスチャの値はスキーマが弾くためクォート済みのまま変えていない

## 最終の実測

`JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test`
→ `Tests run: 291, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`
（初回ラウンド終了時点は `Tests run: 283`。レビュー是正 1巡目で +9 件、2巡目で -1 件
＝ 新規 3 件・削除 3 件・畳み込み -1 件。3巡目は ±0 件＝改名 1 件のみ）

## QA / Expert Review

3 ラウンド（`1b480b4` / `cfcd2ae` / `de31806`）について、QA・Design・Craft・Verification の 4 軸を
独立したサブエージェントとしてそれぞれ 1 回ずつ、計 12 回実施した。各エキスパートには成果物と完了条件と
チェックリストだけを渡し、このチェックファイル・実装エキスパートのサマリ・他軸の判定は渡していない。
2 巡目以降は変異確認を隔離ワークツリーで行うことを義務づけた（1 巡目で共有ツリーの同時書き換えにより
再現不能な結果が出たため）。

| 軸 | 1巡目（`1b480b4`） | 2巡目（`cfcd2ae`） | 3巡目（`de31806`） |
|---|---|---|---|
| QA | pass | pass（要修正 2） | **fail**（記録の件数・参照行） |
| Design | pass（must-fix 1） | **fail**（`~` の事実誤り） | pass |
| Craft | pass（推奨 2） | **fail**（`~`・不変性未検査・重複 4） | **fail**（`userId` の虚偽記述） |
| Verification | pass（survivor 6） | pass（survivor 6） | pass（survivor 3） |

### 3 ラウンドを通じた指摘の型

3 巡続けて **「修正ラウンド自体が新しい虚偽記述を混入させる」** 形で落ちた。実装とテストの守備範囲に
誤りが出たことは一度もなく、落ちた理由はすべて javadoc・コメント・チェックファイルの**記述の正確さ**である。

| 巡 | 混入した虚偽記述 | 実測した反証 |
|---|---|---|
| 1 | 「`fw_header:` のキーが null（YAML の `~`）の場合」 | `~: "V"` のキーは `java.lang.String` の `"~"`。真の null キー（`null: "V"`）は `YamlLoader.java:151` の `OBJECT_MAPPER.valueToTree` が `IllegalArgumentException: Null key for a Map not allowed in JSON` で落とすため `convertFwHeader` に到達しない |
| 2 | 「設定に既定キー `userId` を混ぜてあるのは…通ることを同時に示すため」 | フィクスチャ `customFwHeaderData.yaml` の `fw_header:` は `customField`・`requestId` の 2 キーのみ。`convertFwHeader` は最初の不許可キー `requestId` で throw するため `userId` は評価されない |
| 2 | 「`objectToString` が `"null"` に文字列化して」 | `YamlSection.java:139`-`:149` の `objectToString` は `toStr`（`:127`-`:129`）へ委譲し、`toStr` は `value != null ? value.toString() : null` で Java の `null` を返す。文字列 `null` は例外メッセージの `+` 連結が作っている |
| 2 | チェックファイル「2巡目: 落ちていない既存テストを 6 件変更・3 件削除」 | 着手前（`876e342`）から存在したのは 1 件のみ（`buildMessagePool_malformedFwHeaderRowsThrowsException`）、削除 3 件のうち着手前から存在したものは 0 件 |
| 2 | チェックファイルの assert 参照行 12 箇所 | `id` 側と キー名側 が入れ替わっているものを含め、その行に無いものを指していた |

3 巡目の指示では「書く文はすべて実物を開くか実行して確かめ、確認方法を 1 行で添えること」を明示し、
`file:line` 参照の機械検証を完了条件に加えた。

### 軸ごとの評価

| 軸 | 観点 | 判定 | 根拠 |
|---|---|---|---|
| QA | 検証のやり方が目的に対して意味を持つか（形だけの検査になっていないか） | OK | 実装側の変異を独立に実施（1巡目 11 通り・2巡目 12 通り・3巡目 7 通り）。パリティテストがハードコード期待値でなく本体を読んでいることを、本体側と等価な変異で確認。削除 3 テストの保護が移管先で生きていることも変異で確認 |
| Design | アプローチ・構造が適切か／責務の分離 | OK | 検査を `convertFwHeader` に置いた判断を `YamlSection`（定数と行の意味規則のみ）・`YamlLoader`（静的スキーマ＋キャッシュ）の責務と突き合わせて妥当と判定。本体の private 定数は再利用不能（`MessageParser.java:33` が `private`、既定 4 つはフィールド初期化子のインラインリテラル）であることを確認したうえで、再定義＋パリティテストという構成を妥当と判定 |
| Design | システム全体の整合性（インターフェース契約・API 互換・ドキュメント間の一貫性） | OK | `876e342` と最終版の `public`/`protected` 宣言行を抽出して `diff` した結果、差分ゼロ。`fw_header:` を書ける経路（`messages` / `expected_request_*` / `response_*` / 送信同期）を機械的に展開し、書ける経路がすべてスキーマか新検査のどちらかで塞がれていることを確認 |
| Craft | 言語・フレームワークの慣行、エラー処理、null・スレッド安全、命名、重複 | OK | 例外型・メッセージの形が同パッケージの既存（`YamlLoader.java` 他）と一致。`objectToString(null)` が `HashSet.contains(null)` で NPE にならないことを実行して確認。`formatAllowedFields` の区切り判定が空文字の項目名でも正しいことを実行して確認。重複 4 箇所は 2 巡目で解消 |
| Craft | 既存コードベースの流儀との一貫性 | OK | import の並べ方・javadoc の形・定数の置き方・ヘルパーの配置・テストの Given/When/Then 形式が既存と一致。`mvn -o clean javadoc:javadoc` の警告数が着手前（`876e342`）と同数であることを実測 |
| Verification | 成果物が実際に検査されているか（テストが実行され、崩すと落ちるか） | OK | 3 巡合計で実装側 30 通り超の変異を隔離コピーで実測。1 巡目の survivor 2 件（`id + "XYZ"`・`allowed keys` 削除）、2 巡目の survivor 4 件（`unmodifiableSet`・`useFwHeader` ガード・`instanceof List`・型名固定）はいずれも後続ラウンドで殺されたことを確認 |
| Verification | 網羅（境界・エラー・空・最大・型変換） | OK | 既定 4 つ／設定値／空文字／空白のみ／カンマのみ／末尾カンマ／先頭カンマ／大文字小文字／非文字列キー／null キー／空マップ／マップ以外 を網羅。実行順（`reversealphabetical` / `random` / `alphabetical`）と各テストの単独実行でも緑であることを実測 |

### トリアージ

有効と判定して直したもの: 1 巡目 9 件（F1-F9）・2 巡目 13 件（G1-G13）・3 巡目 17 件（H1-H17）。

却下したもの（根拠つき）:

| 指摘 | 却下の根拠 |
|---|---|
| スキーマ `ntf-testdata-yaml-schema.json` の `fw_header` の `description`・`$comment` が実装と矛盾（4 軸すべてが指摘） | `steering.md` の #42 Step B が明示的に担当。#38 の完了条件に含まれない。**なお #42 Step B の行番号は #36・#37 のスキーマ編集で腐っていたため、コーディネータが実測値（`:216` / `:433` / `:434`）へ訂正し、`additionalProperties` の構造を締めないという制約も追記した** |
| 例外メッセージに `basePath` を足す | #38 Step A が「同メソッドの既存の `IllegalStateException` と同じ形」と定め、既存の形は `id` のみ |
| `useFwHeader` 引数を `sectionKey` からの内部導出に置き換える | 既存 API のリファクタで完了条件外 |
| 不正キーを全件まとめて報告する | 好みの問題で完了条件外 |
| `response_*` にスキーマ負テストを足す | `expected_request_*` と同一 `$def` で機構が同じ。完了条件外 |
| `YamlLoader` が真の null キーで生の Jackson 例外を漏らす | `YamlLoader` の課題。「別タスク候補」節に実測つきで記録 |
| 生き残る変異 3 件（値側・キー側の `String.valueOf`、`TreeMap`） | 差が出る入力がスキーマ／ローダで到達不能。「生き残っている変異」節に実測つきで記録 |
| `@After` の後始末を各テストの try/finally に局所化 | 指摘した Craft 自身が「現状のままでも誤りではない」と評価 |

### コーディネータの独立検証（`93fcff7`）

エキスパートの報告を鵜呑みにせず、自分で次を実測した。

- `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` → `Tests run: 291, Failures: 0, Errors: 0, Skipped: 1` / `BUILD SUCCESS`
- チェックファイル中の `file:line` 参照 105 件を機械抽出し、リポジトリ内 93 件はすべて実在行の範囲内・範囲外 0 件（残り 12 件は `nablarch-testing` / `nablarch-document` への外部参照）
- 2 巡目に取り違えていた assert 参照 16 箇所（`id` 側 8・キー名側 8）を 1 行ずつ開き、`id='...'` と `has unknown key '...'` が入れ替わりなく正しい行を指すことを確認
- 外部参照の抜き取り: 本体 `MessageParser.java:108` が `isNullOrEmpty(SystemRepository.getString(FW_HEADER_KEY))` の行であること、解説書（ピン `afa4f9e`）の親節 `:1147`「メッセージングのデータを記述する」がファイル内で 1 回だけ現れること、引用 2 文が `:1264`・`:1295` に逐語で実在すること
- 3 巡で混入した虚偽記述 2 件（`userId`・`objectToString`）の該当箇所を開き、実態に沿う記述へ直っていることを確認
- `<p>` の二重開始が 0 箇所であることを走査で確認
- 合成 Map を使うテストが 3 件（非マップ `fw_header`・null キー・非 `messages` 経路）だけに減り、実 YAML から到達可能な入力（数値キー・真偽値キー・大文字小文字違い・`~`）が `mixedFwHeaderKeysData.yaml` の実フィクスチャ経路へ移っていることを確認
- 解説書 `testdata_notation.rst:1295`（ピン `afa4f9e`）を開き、実装の挙動が SSoT の記述と一致することを確認

## Overall Verdict

- Self-check: OK（レビュー是正 3巡目 H1-H17 まで反映。3巡目は改名 1・実フィクスチャへの移動 2・assert 追加 1・javadoc/コメント是正 7 箇所・ヘルパー整理 2・フィクスチャ 3・実装 javadoc 3 箇所で、`@Test` 件数は 52 件のまま。変異 8 通り（O1-O6・P1-P2）を隔離コピーで実測し、生き残る 3 変異の却下理由も実測つきで記録した。`file:line` 参照はチェックファイル 218 件・変更したソース 9 件の計 227 件を、その行の内容を出力するスクリプトで機械的に検証し不一致 0 件）
- QA: OK（3巡目の指摘は記録の件数と参照行の 2 件。いずれも 3 巡目で是正し、コーディネータが機械検証で不一致 0 件を確認）
- Design expert: OK（3巡目 pass。実測した 14 件の事実主張がすべて真、公開 API 差分ゼロ、削除テストの保護は全件が移管先で存続）
- Craft expert: OK（3巡目の指摘は `userId` の虚偽記述 1 件。H1 で是正し、コーディネータが該当箇所を開いて確認）
- Verification expert: OK（3巡とも pass。前 2 巡の survivor 6 件はすべて後続ラウンドで殺された。残る 3 件は実 YAML から到達不能な同値クラスで、却下理由を実測つきで記録）
- コーディネータの独立検証: OK（上記「コーディネータの独立検証」節）
- Ready to check off: Yes（完了条件 7 項目すべて OK。有効な指摘 39 件を 3 ラウンドで是正、却下 8 件は根拠を記録。`mvn -o clean test` 緑をコーディネータが独立に実測）
