Rn version: 0.8.0

# Goal

nablarch-testing の YAML 読み込み機構（src/main 12件）とその単体テスト（src/test 52件）を
nablarch-testing-yaml リポジトリへ切り出し、`mvn test` 全 PASS の状態にする。
実装は一切変更せず、ファイルの物理コピー（パス変更）と package/import の機械的調整、
および pom 設定のみを行う。
移送後、解説書・JSON Schema との食い違いが見つかった箇所は、ユーザー確認を経たタスク（#5〜#13）として是正する。

**Step 4（#26〜#34）**: ディレクター指示書 `nablarch-document@2101ce0` の
`.rn/20260724-ntf-yaml-support/ntf-step4-02-nablarch-testing-yaml.md` に確定済みで載っている18件
（実装の是正5件・テスト追加13件）を実施し、解説書に書いてあることをテストで押さえる。
探索も、解説書を読み比べて不一致を洗い出す作業も含まない。

# Acceptance criteria

- `mvn test` 全テスト PASS（yaml リポジトリ単体で緑）
- 移送3タスク（#1〜#3）の完了時点で、全移動ファイルが `worktree-agent-a79308e7e5862d004`（`d8ba387`）と package/import を除き完全一致していた（根拠: checks/task-02.md・checks/task-03.md）
- #4 以降の実装差分は、すべて steering の承認済みタスクに帰属し、タスク外の差分が無い（根拠: `git diff --stat 0df7407..HEAD -- src/main` の14ファイルについてファイル→タスク→checks/記録の対応を実測。未採番だった5コミットは `#25` として追番・記録済み（`checks/task-25.md`）。`git diff d8ba387..HEAD` は d8ba387 がこのリポジトリに存在せず取得不能——2026-08-24 `git cat-file -t d8ba387` で `Not a valid object name` を確認、移送元 worktree ブランチが消滅済みのため）
- 本体（nablarch-testing）に一切書き込みをしていない
- push 済み
- **Step 4**: 指示書 §4 の完了条件8項目をすべて満たす（第2節5件の是正／第3節13件のテスト存在・落ちたものは `@Ignore`＋印つき理由／足した・直したテストの変異確認／既存テスト期待値変更の全件記録／C0・C1 計測／`mvn test` 緑／`git status --short` 空・`tmp/`・`javac.*.args` 無し／push）
- **Step 4 第2回**: 指示書（`ntf-step4-06-nablarch-testing-yaml-2.md`）§4 の完了条件10項目をすべて満たす（第2節7件の是正／2-1〜2-5 の着手前調査の事前報告／足した・直したテストの変異確認／既存テスト期待値変更の全件記録／`@Ignore` 0件／C0・C1 計測／`mvn -o clean test` 緑／`git status --short` 空／push／converter で落ちたテストの全件報告）

# Assumptions

- nablarch-testing と nablarch-testing-yaml は同じ親ディレクトリに clone 済み（`../nablarch-testing`）
- 本体は `convert-testdata-excel-to-text` ブランチのまま（書き込み禁止・参照のみ）
- 本体テストクラスを extends する `*YamlTest` や `YamlModeTestBase` はこのリポジトリに含めない（integration 行き）
- コンパイル・テストが通らず実装変更が要ると判断したら止めてユーザーに確認する

# Rules

- 1 task = 1 commit
- 実装の変更は一切しない（物理コピー・package/import 機械的調整・pom 設定のみ許可）
- 本体（nablarch-testing）には書き込まない
- 変更が必要と判断したら **止めてユーザーに確認**（テスト PASS のために実装をいじるのは禁止）
- mvn コマンド（compile / test / install 全て）は `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn ...` で実行する（Nablarch v6 は Java 17 ターゲット。既定 java は 21 なので指定必須。親 POM は `--release` でなく `source`/`target` 指定のため 21 でビルドすると 21 専用 API が通ってしまう）
- mvn は必ず `clean` を付ける（`jacoco:restore-instrumented-classes` は prepare-package で走るため、`clean` なしの `mvn test` / `mvn install` は instrument 済みクラスが `target/classes` に残り「Cannot process instrumented class」で失敗する）
- javadoc 生成時に「モジュールが使用されていますが…java 8 api」の WARNING 1個が出るが、`maven-javadoc-plugin 2.10.4`（親 POM 固定）と Java 9+ モジュールシステムの非互換によるもので許容済み
- mvn は**単独実行**する（並行実行でビルドが汚染された実績あり）。`BUILD SUCCESS` だけを根拠にせず `Tests run:` の行を確認する
- **AskUserQuestion は使わない**（2026-08-24 ユーザー指示）。判断を仰ぐときは本文に書く
- **Craft/QA レビューの要否は次の基準で判断する**（2026-08-24 ユーザー指示。都度確認しない）
  - **判断単位はタスクではなく「ステップ」**（2026-08-24 訂正）。1タスク内でステップごとに要否が割れる場合は、ステップ単位で登録する
  - **必要**: CC が設計・実装するステップ（コード修正・テスト追加）
  - **不要**: ユーザーが文言・構造を逐語指定したステップ／実測・計測ステップ／ビルド・サインオフ
  - 不要としたステップでは、レビューの代わりに **実行コマンドと生の出力、適用後の全文、diff** を報告する
  - 現行タスクへの当てはめ: #21 必要 / #22 必要 / #20 不要 / #14 不要 / **#19 はステップで割れる**（step A・B = 不要／step C = 必要。詳細は #19 に記載）
  - 基準の当てはめに迷う新タスクが出たときだけユーザーに聞く
- **スキーマ `description` が対応すべき先は「実装の分岐」ではなく「スキーマ検証を通過しうる入力に対する、外から観測できる挙動」である**（2026-08-24 ユーザー裁定。`description` 全般に適用）。到達不能な内部規則は書かない。実装側の到達不能な防衛分岐（`YamlSection#castMap` の非 Map 分岐など）はそのまま残してよい
- レビュー用サブエージェントには**個別の一意な作業ディレクトリ**を割り当てる（共有 scratchpad で衝突した実績あり）

**Step 4（#26〜#34）に適用する Rules**（出典: 指示書 `nablarch-document@2101ce0` の
`.rn/20260724-ntf-yaml-support/ntf-step4-02-nablarch-testing-yaml.md`）

- **参照点（ピン）** — 解説書 `nablarch-document`: `5b5c91e`（`git show 5b5c91e:<path>` で読む。**作業ツリーの HEAD を読まない**）／本モジュール: `0db2221`（作業ツリーで作業してよい）／`nablarch-testing`: `3c4bd2a`（変更しない）／`nablarch-testing-converter`: `60d9a2d`（変更しない）
- **本モジュールの `src/main` は変更してよい**（指示書 §1。タグ0件・未リリースで後方互換の対象利用者が存在しないため）。上の Rules「実装の変更は一切しない」は移送タスク #1〜#3 に係るものであり、Step 4 には適用しない
- **解説書を直さない**（指示書 §5）。「解説書が誤っている」と判断した項目は、根拠（`file:line` と参照コミット）を添えて報告して**止める**
- **`nablarch-testing` を直さない。`nablarch-testing-converter` を直さない**（指示書 §5）。2-2 で落ちる converter のテストは報告するだけ
- **解説書に無い書き方を追いかけない。Excel の実装に合わせない**（指示書 §5）。合わせる先は解説書である
- **第2節（実装の是正5件）は直す。第3節（テスト追加13件）で落ちたものは直さず `@Ignore` にして記録する**（指示書 §1）。理由は機械的に集められる印を付ける — `@Ignore("NTF-DOC: <解説書パス>:<行> — 期待 X / 実際 Y")`。何を直すかは全モジュール分を集めてからディレクターが判断するため、**範囲の判断を持たない**
- **足したテスト・直したテストそれぞれについて、期待値をわざと崩すと落ちることを1度確認する**（指示書 §4-3）。「テストが通る」だけでは何かを押さえた証拠にならない。確認したことを報告に書く
- **Step 4 では4観点レビュー（QA / Design / Craft / Verification）を回さない**（指示書 §7）。作業が18件に確定していて探索を含まないことによる。観点D（検証の妥当性）は完了条件3「期待値をわざと崩すと落ちること」で代替し、ディレクターが担当範囲を全量読み直して独立に検証する。上の Rules「Craft/QA レビューの要否」の当てはめは Step 4 タスクには適用しない

**Step 4 第2回（#36〜#44）に適用する Rules**（出典: 指示書 `nablarch-document@origin/ntf-yaml-support` の
`.rn/20260724-ntf-yaml-support/ntf-step4-06-nablarch-testing-yaml-2.md`）

- **参照点（ピン）** — 解説書 `nablarch-document`: `afa4f9e`（`git show afa4f9e:<path>` で読む。**作業ツリーの HEAD を読まない**。`ja/` 配下は `05e57a1` と同一。パスは `ja/development_tools/testing_framework/…`）／本モジュール: `3ee39c9`（作業ツリーで作業してよい）／`nablarch-testing`: `3c4bd2a`（変更しない）／`nablarch-testing-converter`: `d611bec`（変更しない）
- **判断の軸**（2026-08-28 ユーザー確定）— 中間モデル＝NTF 仕様＝現行 Excel 実装が定める意味。「YAML で表せて Excel で表せない意味」は存在しない。YAML の記法がこの意味集合からはみ出す場合、**対応する意味があれば写す（末尾 `null` → `""`）、無ければ弾く（エラー）**。静的に決まるものはスキーマで、設定に依存するもの（`fw_header:` のキー）は実装で検査する
- **第2節の7件は直す。範囲の判断を持たない**（指示書 §1・渡し文面）。解説書が正であり、実装が追いついていない
- **既存テストが落ちたら、期待値を解説書に合わせて直す**（指示書 §1）。「変えた／変えなかった」を件数つきで報告する（完了条件4）
- **Excel 形式に同じ意味がある項目（2-1・2-4）は、本体 `nablarch-testing` を正解（oracle）にしたテストを書く**（指示書 §3）。YAML 側の実装の結果どうしを比べるテストでは規則の写し間違いを検知できない。oracle は POI で組んだ `.xlsx` を本体の `BasicTestDataParser`（`PoiXlsReader` ＋ `NullInterpreter` → `QuotationTrimmer` → `LineSeparatorInterpreter`。`DateTimeInterpreter`・`${...}` 系は掛けない）で読んだ結果
- **足したテスト・直したテストそれぞれについて、期待値をわざと崩すと落ちることを1度確認する**（指示書 §4-3）。確認したことを報告に書く
- **`@Ignore` は0件にする**（指示書 §4-5）。2-6 で既存1件を削除し、新たに足さない。第1回の「落ちたら `@Ignore`」規則は第2回には適用しない
- **解説書を直さない／`nablarch-testing` を直さない／`nablarch-testing-converter` を直さない**（指示書 §5）。converter で落ちるテストは報告するだけ
- **解説書に無い書き方を追いかけない。Excel の実装に合わせない**（指示書 §5）。合わせる先は解説書である。本体を oracle に使うのは、解説書が「形式によらず同じ」と定めた意味を確かめるためであり、本体の挙動を仕様にするためではない
- **`YamlFileBuilder` に Excel の行走査（`DataFileParser`）を通さない**（2026-08-28 ユーザー判断）。構造は YAML が明示するので判定するものが無く、足りないのは値の規則だけ
- **4観点レビュー（QA / Design / Craft / Verification）は回さない**（指示書 §7）。作業が7件に確定していて探索を含まないことによる。観点D は完了条件3（変異確認）と第3節の oracle で代替し、ディレクターが差分を全量読み直して独立に検証する

# Tasks

### ~~#1: pom.xml の作成~~

**Purpose**: yaml リポジトリのビルドが通るよう pom.xml を作成する

**Prerequisites**: none

**Steps**:

- [x] 本体 pom.xml を参照し、親 POM・groupId・依存（nablarch-testing compile、snakeyaml-engine、JUnit）を設定した pom.xml を作成する
- [x] `mvn validate`（またはコンパイルのみ）でパース・設定エラーがないことを確認する
- [x] self-check (OK/NG per completion criterion, record in checks/task-01.md)
- [x] QA expert review (subagent)
- [x] user review

**Completion criteria**:

- pom.xml が作成されており `mvn validate` がエラーなく通る
- 親 POM に `com.nablarch:nablarch-parent` を指定している
- compile 依存に `com.nablarch.framework:nablarch-testing` が含まれている
- compile 依存に `org.snakeyaml:snakeyaml-engine:3.0.1` が含まれている
- テスト依存に JUnit が含まれている

---

### ~~#2: src/main ファイルのコピー配置（12件）~~

**Purpose**: 本体ブランチの src/main 対象 12 件を yaml リポジトリの同一パッケージパスへコピーし、package/import を機械的に調整する

**Prerequisites**: #1

**Steps**:

- [x] A. src/main（java 11件 + schema 1件）を対応パスへコピー
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
- [x] package/import の機械的調整（必要な場合のみ）
- [x] `mvn compile` でコンパイルエラーがないことを確認
- [x] 差分チェック: 全ファイルを本体現ブランチと diff し package/import 以外の差分がないことを確認
- [x] self-check (OK/NG per completion criterion, record in checks/task-02.md)
- [x] QA expert review (subagent)
- [x] language expert review (subagent)
- [x] software-engineering expert review (subagent)
- [x] user review

**Completion criteria**:

- 12 件すべてが `src/main` の対応パスに配置されている
- `mvn compile` がエラーなく通る
- 全ファイルが本体現ブランチと package/import を除き完全一致

---

### ~~#3: src/test ファイルのコピー配置（52件）と mvn test 全 PASS~~

**Purpose**: 単体テスト（java 9件）とテストデータ（43件）を yaml リポジトリへコピー配置し、`mvn test` 全 PASS を確認する

**Prerequisites**: #2

**Steps**:

- [x] B. src/test java（9件）を対応パスへコピー
  - `src/test/java/nablarch/test/core/file/TestCoreFileAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/TestCoreReaderAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/YamlTestDataParserTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlFileBuilderTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlLoaderTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlMessageBuilderTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlSectionTest.java`
  - `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java`
- [x] C. テストデータ（43件）を対応パスへコピー（テストクラスと同じ相対パスを維持）
  - `YamlTestCoreAdapterTest/*`（4件）
  - `YamlTestDataParserTest/*`（16件）
  - `YamlFileBuilderTest/*`（2件）
  - `YamlLoaderTest/*`（13件）
  - `YamlMessageBuilderTest/*`（3件）
  - `YamlTableDataBuilderTest/*`（5件）
- [x] `mvn test` 実行・全 PASS 確認（落ちたら配置/pom で解決。実装変更は禁止）
- [x] 差分チェック: テスト java 9件を本体現ブランチと diff し package/import 以外の差分がないことを確認
- [x] self-check (OK/NG per completion criterion, record in checks/task-03.md)
- [x] QA expert review (subagent)
- [x] language expert review (subagent)
- [x] software-engineering expert review (subagent)
- [x] user review

**Completion criteria**:

- 単体テスト java 9件・テストデータ 43件すべてが `src/test` の対応パスに配置されている
- `mvn test` 全テスト PASS
- テスト java 9件が本体現ブランチと package/import を除き完全一致

### ~~#4: converter 専用 Adapter 群の削除~~

**Purpose**: yaml リポジトリに混入していた converter 専用クラス（4件）とその随伴テスト（3件＋データ4件）を削除し、`mvn clean test` 全 PASS・`mvn install` 成功を確認する

**Prerequisites**: #3

**Steps**:

- [x] A. 以下のファイルを `git rm` で削除
  - `src/main/java/nablarch/test/core/file/TestCoreFileAdapter.java`
  - `src/main/java/nablarch/test/core/reader/YamlTestCoreAdapter.java`
  - `src/main/java/nablarch/test/core/reader/TestCoreReaderAdapter.java`
  - `src/main/java/nablarch/test/core/reader/StubDbInfo.java`
  - `src/test/java/nablarch/test/core/file/TestCoreFileAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/TestCoreReaderAdapterTest.java`
  - `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest/`（配下4件）
- [x] B. `mvn clean test` 実行・全 PASS 確認（落ちたら止めて報告）
- [x] C. 緑確認後 commit・push
- [x] D. `mvn install` 実行・成功確認
- [x] self-check (OK/NG per completion criterion, record in checks/task-04.md)
- [x] QA expert review (subagent)
- [x] user review

**Completion criteria**:

- 削除対象 4件（src/main）＋テスト3件＋データ4件がリポジトリから消えている
- `mvn clean test` 全テスト PASS
- `mvn install` BUILD SUCCESS

---

### ~~#5: カバレッジ未達箇所の対処（テスト追加 + コメント追加）~~

**Purpose**: JaCoCo レポートで判明した未カバー箇所を NTF 仕様視点で分類し、テスト追加・コメント追加で解消する

**Prerequisites**: #4

**Steps**:

- [x] A. `YamlMessageBuilderTest` に `buildSendSyncBodies` のテストを追加（converter の仕様を満たすことを確認）
  - group_id 一致時に FixedLengthFile リストが返ること
  - group_id 不一致時に空リストが返ること
  - `stripBrackets(null)` のブランチも自然にカバー
- [x] B. `InterpreterResolver` の `raw()` のテストを追加
  - `raw().resolve("any")` が空リストを返すこと
- [x] C. `YamlLoaderTest` に末尾 "/" 付き basePath のテストを追加
  - 末尾 "/" 付きパスでも正常にロードできること
- [x] D. `YamlFileBuilder.java` 182行付近に SnakeYAML Engine の仕様を説明するコメントを追加
- [x] E. `mvn clean test` 全 PASS 確認
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-05.md)
- [x] G. QA expert review (subagent)
- [x] H. language expert review (subagent)
- [x] I. software-engineering expert review (subagent)
- [x] J. user review

**Completion criteria**:

- `buildSendSyncBodies`（converter 仕様）がテストでカバーされている
- `InterpreterResolver.raw()` がテストでカバーされている
- `YamlLoader` の末尾 "/" 分岐がテストでカバーされている
- `YamlFileBuilder` の `instanceof` ガードにコメントが追加されている
- `mvn clean test` 全 PASS

---

### ~~#6: YamlLoader への JSON Schema バリデーション組み込み~~

**Purpose**: YAML パース時に `ntf-testdata-yaml-schema.json` でバリデーションをかけ、スキーマ違反を即座に検出できるようにする。スキーマと実装の整合性を CI で継続的に担保する。

**Prerequisites**: #5

**Steps**:

- [x] A. `pom.xml` に `com.networknt:json-schema-validator:3.0.5` を compile スコープで追加
- [x] B. `YamlSchemaValidationException` を新規作成
  - `IllegalStateException` のサブクラス
  - `filePath` と `List<Error>` を保持
  - `getMessage()` をオーバーライド: ライブラリの `Error.toString()` をそのまま使い、ファイルパス + 全エラーを改行で連結
- [x] C. `YamlLoader.load()` にバリデーションを追加
  - パース直後・キャッシュ格納前にバリデーション実行
  - `JSON_SCHEMA` はクラス変数としてシングルトンキャッシュ
  - 違反があれば `YamlSchemaValidationException` をスロー
- [x] D. `YamlLoaderTest` にバリデーション関連テストを追加（6パターン）
  - required漏れ・型違反・enum違反・深いネスト・複数同時・基本検出
- [x] E. `mvn clean test` 全 PASS 確認
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-06.md)
- [x] G. QA expert review (subagent)
- [x] H. language expert review (subagent)
- [x] I. software-engineering expert review (subagent)
- [x] J. user review

**Completion criteria**:

- `pom.xml` に `json-schema-validator:3.0.5` が追加されている
- `YamlSchemaValidationException` が存在し、`getMessage()` にファイルパスと全違反メッセージが含まれる
- `YamlLoader.load()` がスキーマ違反 YAML に対して `YamlSchemaValidationException` をスローする
- 正常な YAML は引き続き例外なく読み込まれる
- `mvn clean test` 全 PASS

---

### ~~#7: スキーマ横並びチェック（3軸）実施 + 不備2件修正~~

**Purpose**: スキーマと実装・仕様の整合を3軸で確認し、判明済み2件の不備を修正して `mvn clean test` 全 PASS・PR Ready for review へ進める

**Prerequisites**: #6

**Steps**:

- [x] A. スキーマ横並びチェックを3軸で実施（subagent）
  - 軸1（読み取り）: 実装（YamlLoader 等）が読むフィールド → スキーマに定義済みか
  - 軸2（書き込み）: converter（`YamlFormatWriter` 等）が書くフィールド → スキーマに定義済みか（`../nablarch-testing` の `convert-testdata-excel-to-text` ブランチを参照）
  - 軸3（required）: スキーマの required フィールド → NTF仕様で必須か（`ntf-impl-spec-list.md` と突き合わせ）
- [x] B. チェック結果を元に他に不備がないことを確認 → NTF仕様に基づきスキーマ再設計
  - 不備1（再設計）: `message_data`（messages専用: fw_header あり・group_id なし）と新 def `expected_request_message_data`（expected_request_*専用: fw_header なし・group_id あり）に分離
  - 不備2: `record_fragment.required` から `record_type` を外す（省略可能なフィールド）
  - 負例テスト3件追加（messages+group_id、expected_request_*+fw_header、group_id空文字）
- [x] C. `mvn clean test` 全 PASS 確認（159件）
- [x] D. commit・push（SHA: dd61c4e）
- [x] E. self-check (OK — checks/task-07.md 記録済み)
- [x] F. QA expert review (OK)
- [x] G. language expert review (OK)
- [x] H. software-engineering expert review (OK)
- [x] I. user review → PR #1 を Ready for review に変更

**Completion criteria**:

- スキーマ横並びチェック（3軸）が実施され、チェック結果が checks/task-07.md に記録されている
- `message_data` の properties に `group_id` が追加されている
- `record_fragment.required` に `record_type` が含まれていない
- 追加不備が見つかった場合はすべて修正されている（実装変更なし）
- `mvn clean test` 全テスト PASS

---

### ~~#8: スキーマ description に FK 制約の落とし穴2件を追記~~

**Purpose**: FK 制約のある環境で利用者がハマる NTF 挙動2点をスキーマ description に記載する

**Prerequisites**: #7

**Steps**:

- [x] A. `setup_tables` の description に落とし穴1を追記
  - 通常の INSERT でも対象テーブルは INSERT 前に全件 DELETE されること
  - FK の親テーブルを clear するなら子テーブルも `setup_tables` に列挙すること（NTF は子→親の順で削除する）
- [x] B. `table_data.rows` の description に落とし穴2を追記
  - FK 制約のある数値カラムを省略すると `"0"` が INSERT され、参照先に ID=0 の行が無ければ FK 違反になること
  - NULL 許容カラムを NULL にしたい場合は省略せず明示的に `null` を書くこと（省略≠NULL）
- [x] C. JSON として妥当か検証（`python3 -c "import json; json.load(open(...))"` 等）
- [x] D. `mvn clean test` 全 PASS 確認
- [x] E. commit・push
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-08.md)
- [x] G. QA expert review (subagent)
- [x] H. Craft expert review — writing (subagent)
- [x] I. Verification expert review — fact-check (subagent)

**Completion criteria**:

- `setup_tables` の description に「INSERT 前の全件 DELETE」と「FK 親テーブル clear 時に子テーブルも列挙すること（NTF は子→親の順で削除する）」が記載されている
- `table_data.rows` の description に「FK 制約のある数値カラムを省略すると `"0"` が INSERT され FK 違反になる可能性」と「NULL 許容カラムを NULL にしたければ省略せず `null` を明示すること」が記載されている
- description 以外（`type` / `enum` / `required` 等の検証ルール構造）は変更されていない
- JSON として妥当（`json.load` が通る）
- `mvn clean test` 全 PASS

---

### ~~#9: 不具合 #C — group_id 省略メッセージエントリが buildSendSyncList で取得できない~~

**Purpose**: `YamlMessageBuilder.buildSendSyncList` の `rawGroupId != null` 条件により、`group_id` を持たないエントリがグループIDなし（`""`）で取得できない不具合を修正する。

**Prerequisites**: #8

**Steps**:

- [x] A. RED: `YamlMessageBuilderTest` に `buildSendSyncMessageList_noGroupId` を追加し、失敗することを確認
  - テストデータは既存 `YamlMessageBuilderTest/` 配下に追記（新規ディレクトリ不要）
- [x] B. GREEN: `buildSendSyncList` を `YamlSection.groupMatches()` に置き換え、`groupMatches` を static import 追加
  - `stripBrackets()` が未使用になる場合は削除
  - `buildSendSyncBodies` に変換ツール専用旨の Javadoc を追記
- [x] C. `mvn test` 実行・全 PASS 確認
- [x] D. commit・push
- [x] E. self-check (OK/NG per completion criterion, record in checks/task-09.md)
- [x] F. QA expert review (subagent)
- [x] G. Craft expert review — coding (subagent)
- [x] H. Verification expert review — test (subagent)

**Completion criteria**:

- 追加した `buildSendSyncMessageList_noGroupId` テストが GREEN
- `YamlMessageBuilderTest` の既存テストが全て通っている
- `buildSendSyncList` から `FIELD_GROUP_ID` の直接比較が消えている
- `stripBrackets()` が未使用なら削除されている
- `buildSendSyncBodies` のロジックが変更されていない（Javadoc 追記のみ）

---

### ~~#10: 不具合 #F — 空の EXPECTED_TABLE が常に PASS する~~（**#11 で差し戻し済み・不具合 #F は未解決**）

**Purpose**: `expected_tables` に `rows: []` のエントリを書いたとき DB に行があってもアサーションが PASS する偽陰性を修正する。

**Prerequisites**: #9

**Steps**:

- [x] A. RED①: 偽陰性テスト — DB に 1 件以上あり `rows: []` で assertTableEquals が FAIL すること
- [x] B. RED②: `EXPECTED_COMPLETE_TABLE` NPE テスト — `rows: []` で getExpectedTableData が例外なく返りカラムが DB 全列
- [x] C. RED③: SETUP_TABLE 退行防止テスト — `rows: []` で setUpDb 後にテーブルが空になること
- [x] D. GREEN: `buildTableData()` をデフォルトコンストラクタ + setter 方式に変更、`fillDefaults && !dataColumns.isEmpty()` ガード追加
- [x] E. 既存テスト `buildTableDataList_allEmptyRowsReturnsTableDataWithZeroColumns` の期待値を「dbInfo 全列が返ること」に更新し、Javadoc から `(JE-6)` と解説書 10.5 参照を削除
- [x] F. `mvn test` 実行・全 PASS 確認
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-10.md)
- [x] I. QA expert review (subagent)
- [x] J. Craft expert review — coding (subagent)
- [x] K. Verification expert review — test (subagent)

**Completion criteria**:

- 追加した RED テスト①②③が全て GREEN
- `buildTableDataList_allEmptyRowsReturnsTableDataWithZeroColumns` の期待値が更新され GREEN
- `YamlTableDataBuilderTest` の他の既存テストが全て通っている
- YAML スキーマが変更されていない
- `nablarch-testing` 本体および converter が変更されていない

---

### ~~#11: #10 の差し戻しと本体課題としての報告~~

**Purpose**: #10 の修正が converter（DB を持たない読み込み経路）を壊したため差し戻し、事象を NTF 本体の課題としてチームへ報告する。

**Prerequisites**: #10

**Steps**:

- [x] A. `buildTableData()` を差し戻し（長さ 0 の列名で `TableData` を生成／`fillDefaults` のガードも撤去）
- [x] B. FAIL する 4 件を `@Ignore` + FIXME で保留（削除しない）
  - `YamlTestDataParserTest#emptyExpectedTable_failsWhenDbHasRows`
  - `YamlTableDataBuilderTest#buildTableDataList_emptyRowsExcluded`
  - `YamlTableDataBuilderTest#buildTableDataList_allEmptyRowsReturnsTableDataWithAllDbColumns`
  - `YamlTableDataBuilderTest#buildTableDataList_emptyExpectedTableReturnsTableDataWithAllDbColumns`
- [x] C. `mvn clean test` 全 PASS 確認（164 件 / Skipped 4）
- [x] D. commit・push（SHA: 190cc9a）
- [x] E. `mvn clean install` 実行・成功確認（`.m2` 更新）
- [x] F. converter で回帰確認（428 件 PASS・BUILD SUCCESS）
- [x] G. 事象の裏取り（実測）
  - Excel 経路でもカラム名 0 件が発生することを実 xlsx で確認（識別子行がシート末尾／空行をヘッダ位置に置く／ヘッダがマーカーのみ）
  - 準備データ（`setup_tables`）はカラム名 0 件でも 0 件初期化が成立することを実 DB で確認
  - 解説書（`nablarch-document` main / `ntf-yaml-support`）にカラム名必須の記載が無いこと、記載例はすべてカラム名の行を書いていることを確認
- [x] H. チーム報告書を作成（`.rn/ntf-yaml/report-empty-expected-table.md`。本体 main の該当行 URL 付き）

**Completion criteria**:

- `YamlTableDataBuilder` が #10 以前の形（長さ 0 の列名）に戻っている
- FAIL する 4 件が削除されず `@Ignore` + FIXME で残っている
- `mvn clean test` が BUILD SUCCESS（Skipped 4）
- converter が `Tests run: 428, Failures: 0, Errors: 0` / BUILD SUCCESS
- 報告書に本体 `main` ブランチの該当行 URL が記載されている

---

### ~~#12: YML-03 — `record_type: FW_HEADER` によるレコード読み飛ばしを廃止する~~

**Purpose**: メッセージ系経路が `record_type` の値 `FW_HEADER` でレコードを読み飛ばす旧版前提の処理を廃止し、解説書 `testdata_notation.rst:1302`（「`record_type` に特別な予約値はない」／FW 制御ヘッダは `fw_header:` マップで記述する）の仕様に一致させる。

**Prerequisites**: #11

**Steps**:

- [x] A. RED: `record_type: FW_HEADER` を含むレコードが読み飛ばされないことを確認するテストを追加し、失敗することを確認する
  - `buildFragmentsForMessage` 経路（`messages`）と `buildFragmentsForSendSync` 経路（送信同期）の両方
  - 期待値: `FW_HEADER` という値を持つレコードも他の `record_type` 値と同じくフラグメントとして構築される
- [x] B. GREEN: `YamlFileBuilder#buildFragmentsInternal` の `skipFwHeader && FW_HEADER_RECORD_TYPE.equals(recordType)` によるスキップ分岐を削除する
  - `skipFwHeader` 引数が担う他の役割（`record_type` を `"default"` 固定・length 未指定を `"-"` 扱い）は維持し、引数名・Javadoc を実態に合わせる（`skipFwHeader` → `messaging` にリネーム）
  - `YamlSection.FW_HEADER_RECORD_TYPE` が未使用になったら削除する（削除済み）
- [x] C. 旧記法（`record_type: FW_HEADER` レコード）に依存している既存テスト・テストデータを新仕様（`fw_header:` マップ）へ移行する
  - **移行しない**判断に変更した。解説書 `testdata_notation.rst:1296`/`:1302` のとおり `record_type` に予約値はなく `FW_HEADER` は現仕様でも合法な装飾値であり、スキーマにも `enum` 制約がないため、実データ経路に残すこと自体が「特別扱いしない」ことの回帰ガードになる。実際に変異実験でこれらの fixture を使うテストが RED になることを確認した（詳細は `checks/task-12.md`）
- [x] D. `mvn clean test` 全 PASS 確認（Skipped は #11 で `@Ignore` にした 4 件のみ）
  - `Tests run: 173, Failures: 0, Errors: 0, Skipped: 4` / BUILD SUCCESS
- [x] E. commit・push（`0b53910` → レビュー反映 `b91abc1` → `e9213ad`）
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-12.md)
- [x] G. QA expert review (subagent) — pass
- [x] H. Craft expert review — coding (subagent) — pass
- [x] I. Verification expert review — test (subagent) — pass

**Completion criteria**:

- `record_type` の値が `FW_HEADER` のレコードが、`messages` 経路・送信同期メッセージ経路のいずれでも読み飛ばされずフラグメントとして構築される
- `src/main` に `record_type` の値を特別扱いする分岐・定数が残っていない
- FW 制御ヘッダを `fw_header:` マップから取得する経路が従来どおり動作する（既存の `fw_header` 関連テストが GREEN）
- `mvn clean test` が BUILD SUCCESS（Skipped は #11 の 4 件のみ、Failures/Errors 0）
- `ntf-testdata-yaml-schema.json` の検証ルール構造（`type` / `enum` / `required`）が変更されていない
- `nablarch-testing` 本体が変更されていない

---

### ~~#13: YML-08 — ディレクティブ description を解説書の記法へ修正する~~

**Purpose**: `ntf-testdata-yaml-schema.json` の `record-separator` / `field-separator` の description が、`DataFile#setDirective` の `trim()` により実際には通らない「実制御文字をリテラル指定する記法」を推奨している状態を、解説書が示す記法（シンボル／エスケープ2文字表記）へ書き換える。

**Prerequisites**: #12

**Steps**:

- [x] A. RED: 現 description が推奨する記法が実際には通らないことを実行で確認するテストを追加する
  - `record-separator` に実制御文字 `"\r\n"`（YAML のダブルクォートで展開された CR+LF）を与えるとレコード区切りが空になること
  - `field-separator` に実制御文字のタブを与えると `IllegalArgumentException`（`field-separator must be one character`）になること
  - あわせて解説書が示す記法（`record-separator: CRLF` 等のシンボル）が通ることを確認する
  - RED 実行結果: `Tests run: 2, Failures: 1, Errors: 1` / BUILD FAILURE（`Expected: is "\r\n" but: was ""` / `IllegalArgumentException: field-separator must be one character.but was `）
  - シンボルは 4 種すべて（`NONE` / `CR` / `LF` / `CRLF`）を assert する形へ拡張した（fact-check レビュー指摘）
- [x] B. GREEN: `$defs.directives.properties.record-separator.description` と `field-separator.description` を解説書と一致する記法へ書き換える
  - `record-separator`: シンボル `NONE` / `CR` / `LF` / `CRLF`（解説書 `:945`、記述例 `:1114`）
  - `field-separator`: エスケープ2文字表記 `\t`（解説書 `:1078`）
  - 実制御文字を値に書く記法の記述を削除する
  - あわせて、YAML で実制御文字に展開されてしまう罠を両 description に警告として書き足した。`field-separator` 側は解説書 `testdata_examples.rst:1435` の YAML 固有の注意と同じ機序で説明する形へ揃えた
- [x] C. JSON として妥当か検証（`python3 -c "import json; json.load(open(...))"`）
- [x] D. `mvn clean test` 全 PASS 確認
- [x] E. commit・push
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-13.md)
- [x] G. QA expert review (subagent)
- [x] H. Craft expert review — writing (subagent)
- [x] I. Verification expert review — fact-check (subagent)

**Completion criteria**:

- `record-separator.description` に実制御文字によるリテラル指定（`"\r\n"` 等）の推奨が残っていない
- `field-separator.description` に実制御文字のタブによる指定の推奨が残っていない
- 両 description が示す記法が、実行で通ることをテストで担保されている
- description 以外（`type` / `enum` / `required` 等の検証ルール構造）が変更されていない
- JSON として妥当（`json.load` が通る）
- `mvn clean test` が BUILD SUCCESS（Skipped は #11 の 4 件のみ、Failures/Errors 0）
- `nablarch-testing` 本体（`DataFile#setDirective`）が変更されていない

---

### ~~#14: Evaluation sign-off~~

**Purpose**: `steering.md` の Acceptance criteria を通しで実行し、その結果をユーザーへ提示して評価ゲートの判定を受ける。

**Prerequisites**: #13

**Steps**:

- [x] A. Acceptance criteria を上から順に実行し、結果（OK/NG と根拠）をまとめる（**2026-08-24 再実行**。#20 完了により旧記録は陳腐化したため取り直し）
  - 5 項目中 5 項目 OK
    - `mvn -o clean test` 全 PASS: `Tests run: 226, Failures: 0, Errors: 0, Skipped: 0`、BUILD SUCCESS（`2026-08-24T16:51:04+09:00`）
    - 移送3タスク（#1〜#3）完了時点の完全一致: `checks/task-02.md`（12件 diff 差分ゼロ）・`checks/task-03.md`（9件 diff 差分ゼロ）に記録済み（変更なし・再取得不要）
    - #4 以降の実装差分の task 帰属: `git diff --stat 0df7407..HEAD -- src/main` の14ファイルについてファイル→タスク→`checks/` 記録の対応を実測（2026-08-24 再取得）。未採番だった5コミット（`f375fde`・`630e700`・`10feb3e`・`6ea4655`・`b309359`）は `#25` として追番・記録済み。タスク外の差分はない
    - 本体無書き込み: 本体 `../nablarch-testing` の `git status --short` 空、HEAD `2e43786`（ブランチ `convert-testdata-excel-to-text`）
    - push 済み: HEAD `5b28eb9` = `origin/feature/ntf-yaml`（`git rev-parse HEAD` と `git rev-parse origin/feature/ntf-yaml` が一致）
- [x] B. 結果をユーザーへ提示し、`/rn:ty`（承認）または `/rn:gm`（差し戻し）の判定を受ける
  - **Craft/QA レビューは不要**（Rules の基準: サインオフ。2026-08-24 ユーザー指示）。代わりに **実行コマンドと生の出力** を報告する
  - **判定: OK**（2026-08-24 ユーザー承認）。**マージはしない。PR #1 は DRAFT のまま維持する**（`gh pr list` で状態 DRAFT を確認済み。ユーザー指示によりこの状態を変更しない）

**Completion criteria**:

- Acceptance criteria 全項目の実行結果が根拠つきで提示されている
- ユーザーの判定（`/rn:ty`）が得られている

---

### ~~#15: 2-1 — `#23` の修正で解消した不具合 #F の `@Ignore` を外す~~

**Purpose**: 本体 `TableData#loadData()` の `#23`（`b6e049a`）対応により不具合 #F が解消したため、保留していた偽陰性テストを復活させる。

**Prerequisites**: none

**Steps**:

- [x] A. `YamlTestDataParserTest.java:396`〜`:398` の FIXME コメントと `:399` の `@Ignore` を削除する（Given/When/Then は1文字も変えない）
- [x] B. `mvn -o clean test` で `emptyExpectedTable_failsWhenDbHasRows` が緑になることを確認する（緑にならなければ commit せず revert し、surefire レポートを添えて報告）
- [x] C. commit・push
- [x] D. self-check (OK/NG per completion criterion, record in checks/task-15.md)
- [x] E. QA expert review (subagent)
- [x] F. Verification expert review — test (subagent)

**Completion criteria**:

- `YamlTestDataParserTest` に `FIXME` / `@Ignore` が残っていない
- `emptyExpectedTable_failsWhenDbHasRows` が実行され PASS している（Skipped でない）
- テストの Given/When/Then が `0197071` 時点と同一
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#16: 2-2 / 2-3 — `rows: []` のカラム名解決の責務を本体側に置き、期待値と FIXME を実態に合わせる~~

**Purpose**: `rows: []` のときカラム名を解決するのは DB を知る `TableData#loadData()`（本体）の責務であり、YAML を読むだけの `YamlTableDataBuilder` が長さ0の列名で `TableData` を作るのは正しい、という責務の所在を期待値とコメントに反映する。

**Prerequisites**: #15

**Steps**:

- [x] A. `YamlTableDataBuilderTest` の3テスト（`:145/147`・`:419/421`・`:869/871`）で「dbInfo の全カラム数（11）が返ること」を主張する `assertThat` を、列名0件を主張する形へ直す（テーブル名・件数・行数の `assertThat` は変えない）
- [x] B. 各テストの javadoc に、なぜ列名0件が正しいのかを1文で書く（責務の所在）
- [x] C. 3件の FIXME コメントと 3件の `@Ignore` を削除する
- [x] D. `YamlTableDataBuilder.java:110` の FIXME を、長さ0の列名で `TableData` を作ってよい理由を説明する通常のコメントへ書き換える（「FIXME」「TODO」の語を残さない）
- [x] E. `mvn -o clean test` 全 PASS 確認
- [x] F. commit・push
- [x] G. self-check (OK/NG per completion criterion, record in checks/task-16.md)
- [x] H. QA expert review (subagent)
- [x] I. Craft expert review — coding (subagent)
- [x] J. Verification expert review — test (subagent)

**Completion criteria**:

- `grep -rn 'TODO\|FIXME\|@Ignore' src --include=*.java` が 0 件
- 3テストが実行され PASS している（Skipped 0）
- 変更した `assertThat` は列名に関するものだけで、テーブル名・件数・行数の主張は `0197071` 時点と同一
- `YamlTableDataBuilder.java` のコメントが責務の所在を説明している
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#17: `resolveColumns` の先頭行 `{}` によるデータ行の無言消失を塞ぐ~~

**Purpose**: `rows` の先頭要素が空マッピング（`{}`）のとき列名が0件になり、後続の実データ行が無言で捨てられる不具合を解消する。`setup_tables` ではテーブルが空にされたうえでデータが投入されず、エラーも出ない。

**Prerequisites**: #16

**根拠（2026-08-21 実測）**: `YamlSection.java:144-149` の `resolveColumns` は `rows.get(0)` のキーのみを列名にする。先頭が `{}` なら `columnNames` が空になり、`YamlTableDataBuilder.java:203-215` の `extractRows` が実データ行に対しても空の `rowValues` を作る（`for (String col : columnNames)` が0周）。その空行は `YamlTableDataBuilder.java:121-123` の `if (rawRow.isEmpty()) { continue; }` で捨てられる。

**Steps**:

- [x] A. RED: 3経路それぞれに「先頭 `{}` ＋後続に実データ行」のテストを追加し、失敗することを確認する
  - `buildTableDataList`（`setup_tables`）
  - `buildTableDataList`（`expected_tables`）
  - `buildListMapRows`（`list_maps`。`YamlTableDataBuilder.java:157` が同じ `resolveColumns` を呼ぶ）
- [x] B. フィクスチャ: 先頭 `{}` の**新規グループ**を `tableData.yaml` 等に追加する。**既存グループ `emptyRowMixed`（`tableData.yaml:63-75`。`{}` が2番目）は変更しない**
- [x] C. GREEN: `resolveColumns` を「先頭の**非空マッピング行**のキーを列名にする」実装へ変更する。非空マッピング行が1つも無ければ従来どおり空リストを返す
- [x] D. `list_maps` 経路でも同根の症状（先頭 `{}` で全行が空 Map になる）が塞がることを確認する
- [x] D2. #16 で書いた `YamlTableDataBuilder.java:111` の冒頭「列名は先頭行（`rows.get(0)`）のキーからのみ決まる」と、`YamlSection#resolveColumns` の javadoc（`:135-143`）が、変更後の実装と食い違わないよう更新する
- [x] E. 変異確認（`指示/00-共通ルール.md:62`）: 追加した各テストについて、その分岐を壊す変更を1つ入れると落ちることを実際に確認し、元に戻す。コマンドと結果を記録する
- [x] F. `mvn -o clean test` 全 PASS 確認（`Tests run:` の行を確認。`BUILD SUCCESS` だけを根拠にしない）
- [x] G. commit・push（**#16 とは別コミット**）
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-17.md)
- [x] I. QA expert review (subagent)
- [x] J. Design expert review (subagent)
- [x] K. Craft expert review — coding (subagent)
- [x] L. Verification expert review — test (subagent)

**スコープ外（2026-08-24 ユーザー判断で決着・不具合ではない）**: 「先頭行に無いキーを2行目以降が持っていても捨てられる」件は `testdata_notation.rst:819`（「後続の行に最初の行のキーにないものを追加しても、そのキーは読み込まれない」）どおりの仕様。**報告書に不具合として書かない。** 同じく「先頭がマーカーのみの行で後続の実データ行の値が全消失」も `testdata_notation.rst:819`（カラム名は先頭要素のキーで決まる）＋ `:1514`（マーカーカラムは読み込み対象外）どおりで、本体 `HeaderLine.java:87-95` が同じ除外をしており Excel も同結果。**報告書に不具合として書かない。**

**Completion criteria**:

- 先頭 `{}` ＋後続実データ行のとき、`setup_tables` / `expected_tables` / `list_maps` の3経路すべてで後続のデータ行が保持される
- 非空マッピング行が1つも無い場合は従来どおり列名0件（#16 で確定した振る舞いが退行していない）
- 既存グループ `emptyRowMixed` のフィクスチャと期待値が変更されていない
- 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている
- `pom.xml` / `argLine` が変更されておらず、他リポジトリへの書き込みが無い
- `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0）

---

### ~~#18: 手順3（XLS-42）— `record_fragment.rows` のスキーマ記述を実装と解説書に合わせる~~

**Purpose**: スキーマの `description` だけが実装・解説書と食い違っているため、実装に合わせて直す。

**Prerequisites**: #17

**Steps**:

- [x] A. 3出典を実物で再確認する（`DataFileFragment.java:107` / `testdata_notation.rst:883` / 当スキーマ）※着手時に確認済み・行番号一致
- [x] B. `$defs.record_fragment.properties.rows.description` を「fields の件数より少ない場合、不足したフィールドは `""` として補完される」趣旨へ直す
- [x] C. `items.description` の「fields の順序に完全対応」も同件数前提の書き方なら合わせて直す
- [x] D. 「多い側」には触れない（エラー化とも「余りは無視される」とも書かない）
- [x] E. JSON として妥当か検証（`python3 -c "import json; json.load(open(...))"`）
- [x] F. スキーマ検証テストが緑のままであることを確認。`rows:` の要素数が `fields` より少ない YAML がスキーマ検証で落ちるなら直す
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-18.md)
- [x] I. QA expert review (subagent) — 完了条件は全 OK、**総評 NG**（親 `:361` の未修正矛盾）。判定は `checks/task-18.md`
- [x] J. Craft expert review — writing (subagent) — **NG**（NG-1 親子矛盾／NG-2・NG-3 `:386` の重複／NG-4 `:377` の曖昧さ）
- [x] K. Verification expert review — fact-check (subagent) — **OK**（主張A〜F すべて真。旧記述「一致しない場合はエラー」は条件付きでも真でないことを確認）
- [x] L. **判断（2026-08-24 ユーザー回答: 直す。ただし言い換えではなく削除）**: 同 `$defs.record_fragment` の**親 `description`**（スキーマ `:361`）に残る「rows の各配列は fields と完全に同じ順序・**同じ件数**で値を並べること（NTF パーサが列順で対応付ける）」は、子（`:377`）と矛盾するため**第3文を丸ごと削除**する。第1文・第2文は残す。理由（ユーザー）: `:377` の第1・2文が同じ内容を件数の扱いまで含めて正確に述べており、親に言い換えを残すと同一画面内の相互参照が増えるだけで新情報がない
- [x] M. 以下3点を**1ラウンド**で実装エキスパートに修正させる（`description` 以外は触らない） — 完了（`60b2678`。3点とも指示どおり適用、`Tests run: 187, Failures: 0, Errors: 0, Skipped: 0`）
  - `:361` 第3文「rows の各配列は fields と完全に同じ順序・同じ件数で値を並べること（NTF パーサが列順で対応付ける）」を**丸ごと削除**する（直前の `\n` ごと）。第1文・第2文は変更しない
  - `:386`（`items.description`）から「fields の順序で先頭から対応付けられる。」の**一文だけを削除**する（`:377` 第2文と同内容）。「フィールド値のリスト。」は items の要素が何かを示す唯一の記述なので**残す**。「数値・真偽値も文字列（クォート付き）で記述すること」も残す
  - `:377` 第3文 →「各配列の要素数が fields の件数より少ない場合、値を指定しなかったフィールドには `""` が設定される」（Craft NG-4 = Valid: **値**の不足なのに「不足したフィールド」ではフィールド定義の不足とも読める。「補完」は `:108` で「カラム型ごとのデフォルト値」の意味に使われており二義）
  - QA 指摘「多い場合は無言で切り捨てられる旨を書く」は **Invalid**（ユーザー承認済み）。スキーマには書かず、報告書候補として `checks/task-18.md` の Triage に残す
- [x] N. 修正後、Craft(writing) と QA を再実行する（Verification は事実主張が変わらないため再実行不要）— 実行済み。**両者とも総合 fail**。完了条件5件は両者とも全 OK で、落ちた理由は完了条件の外側。判定・根拠・triage は `checks/task-18.md` の round 2 セクション
- [x] O. **判断（2026-08-24 ユーザー回答）**: 3件とも回答済み
  - **判断A: 書き戻す**（形は指定あり）。`:377` を3文構成にする — 第1文＝規範「各要素は fields と同じ順序・同じ件数で値を並べた配列（NTF は fields の順序で位置対応させる）」／第2文＝挙動「要素数が fields の件数に満たない場合、不足した末尾のフィールドは `""` として扱われる」／第3文＝記法「これを利用し、空配列 `[]` を1要素書くと全フィールドが `""` のレコード1件になる」。**第3文は必須**（第2文だけだと「末尾は省いてよい」という緩和に読める。解説書 `rst:883` は補完を緩和ではなく意図した記法の仕組みとして説明しており、第3文があって初めて `:883` と同じ意味になる）。規範の出典は `rst:1143`（ファイルデータ）と `rst:1300`（メッセージング）
  - **判断A の前提測定（実施済み・結果は「補完される」）**: 固定長ファイルでも `rows` の要素数不足が `""` 補完されることを固定長フィクスチャで実測した（下記「実測」参照）。よって第2文に「可変長ファイルでは」の限定は付けない。加えて `rst:883` が `""` 補完を「可変長ファイルでは」の節に置いている点は実装より記述範囲が狭く、⑥ nablarch-document の報告書候補に追加する
  - **判断B: 現状維持**。`:386` は「数値・真偽値も必ず文字列（クォート付き）で記述すること」だけにする（判断A で `:377` に順序の規範が戻るため、`:386` に順序を書くと同一ブロック内の二重定義になる。IDE 補完の挙動は根拠にしない）
  - **判断C: 新タスク #22 として起こす**。`#18` では実施しない
- [x] P. 判断A の回答を受け、round 2 の **Valid 4件**と合わせて**1ラウンド**で修正 — 完了（`mvn -o clean test` → `Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS）
  - `:361` 末尾の `。` を削除（schema 内 description 全64件を再集計し、`。` 終わりが 0 件になったことを確認）
  - `:377` を判断A の3文構成へ。第2文で「不足した**末尾**のフィールド」と末尾限定を明示（実装は位置対応 `DataFileFragment.java:107` = `i < line.size() ? line.get(i) : ""`。**出典は `rst:883`**。round 2 triage で引いていた `rst:787` はテーブルデータ〈Excel〉節のため差し替えた）
  - `:377`「0件も有効」→「`rows` が0件でも有効」。**括弧内「（setup_files では空ファイル、expected_files では出力なしの期待値として使用）」は削除**した（`rows: []` がファイル系でどう出力されるかは未実測。`rst:881` `rst:1146` は 0 バイト空ファイルの表現手段を `records: []` としており表現手段が違う。#15/#16 が実測した `rows: []` はテーブルデータ経路のみ。測っていない挙動は書かない）
  - `:386` に「必ず」を追加（`:108` `:136` と揃える）

**実測（2026-08-24・判断A の前提）**: 固定長・可変長の双方で、`rows` の要素数不足は `""` 補完される。

- 方法: 一時テストクラスから `YamlFileBuilder#buildDataFileList` → `DataFile#toDataRecords()` を実行（実行後に一時ファイルは削除済み）。フィクスチャは `type: fixed`（`FIELD1`/`FIELD2` 各 `半角` 長さ5）と `type: variable`（`NAME`/`VALUE`）で、いずれも `rows` に `["AAAAA"]`（要素1件）と `[]`（空配列）を並べた
- 結果: `FixedLengthFile` → `row[0]={FIELD2=, FIELD1=AAAAA}` / `row[1]={FIELD2=, FIELD1=}`、`VariableLengthFile` → `row[0]={VALUE=, NAME=tanaka}` / `row[1]={VALUE=, NAME=}`
- 裏付け: `addValue` は `DataFileFragment.java:102-115` の1箇所のみで、`grep -rn addValue nablarch/test/core/file/` の結果は `:102`（`addValue`）と `:169`（`addValueWithId`）だけ。`VariableLengthFileFragment` / `FixedLengthFileFragment` に override は無い
- 解説書との差: `rst:883` は `""` 補完を「可変長ファイルでは、…」で始まる節に置いている（同じ文の後半は「固定長ファイルの場合はスペースパディングされた定長レコードとして書き出される」と固定長にも触れており、節の切り方だけが実装より狭い）→ ⑥ nablarch-document の報告書候補

- [x] Q. 修正後、Craft(writing) と QA を再々実行する — 実行済み。**QA pass / Craft fail**。完了条件5件は両者とも全 OK。Valid は Craft F2（「各配列の」の主語補い）の1件のみで修正済み。他は却下・Invalid・報告書候補・`#22` へ帰属。判定・根拠・triage・round 3 の新実測は `checks/task-18.md` の round 3 セクション
- [x] R. **round 3 却下判断へのユーザー回答（2026-08-24）**: Craft F1（接続詞を足す）・Craft F5（`[]` のバッククォート）の**却下は支持**。新事実1・2 の扱い（1=⑥ 報告書候補へ／2=`#22` で扱う）も**両方支持**。QA の `null` 指摘（`:386` が `null` の扱いに触れていない＝ Craft F8・QA F5）への **Invalid 判定は支持**（既存の乖離で本差分の対象外・⑥ 報告書候補へ）。**新事実3 はユーザー未判断**（2026-08-24 訂正。ユーザーが判断したのは新事実1・2 の2件のみ）— 内容は報告済みで**回答待ち**。新事実3 に依存する作業は進めない。**Craft F4 の却下のみ覆る** — 「位置対応させる」に前例が無いのは事実だが、その前例を `#18` 自身が `:386` から消していた。`git show 35f70c7:src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` の `:386` = 「フィールド値のリスト。**fields の順序で先頭から対応付けられる。**数値・真偽値も文字列（クォート付き）で記述すること」を実物で確認済み。**単独ラウンドは立てず、`#22` step A0 で `:377` を「先頭から対応付ける」へ戻す**

**Completion criteria**:

- `rows.description` に「一致しない場合は NTF がエラーを出す」が残っていない
- 「多い場合」に関する記述を新たに追加していない
- `description` 以外（`type` / `items` の構造・`pattern` / `required` 等）を変更していない
- JSON として妥当
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#21: 全値が null／空文字の行がスキップされない不具合を塞ぐ~~

**Purpose**: `rows` / `list_maps` の要素が「空マッピング（`{}`）」の場合も「全ての値が null または空文字」の場合も、行として存在しないものとして扱う。列名解決からもデータ行からも除外し、位置（先頭・中間・末尾）を問わず同じ結果にする。

**Prerequisites**: #18

**根拠（出典）**:

- 解説書 `testdata_notation.rst:1534`「全要素が null または空文字のエントリは読み飛ばされる。（略）YAML では `rows:` 内の要素が空マッピング（`{}`）またはすべての値が空文字の場合にスキップされる。この空エントリの省略は（略）テーブルデータや `LIST_MAP` のエントリ自体を無いものとして扱う点で異なる。」→ #17 が塞いだのはこの一文の**前半（`{}`）だけ**
- 本体の参照実装: `PoiXlsReader.java:140` `isBlankLine`（全セル空文字で true）を `:93` で読み飛ばす。`TestDataParsingTemplate.java:407` を `:180` で同様。**いずれも `interpret` より前**に判定している
- 現状（yaml 側）: 行スキップ判定は `YamlTableDataBuilder.java:145` / `:193` / `:222` の `Map.isEmpty()` のみで、Map が空かどうかしか見ていない

**適用範囲の境界**: テーブルデータ（`setup_tables` / `expected_tables` / `expected_complete_tables`）と `list_maps` のみ。**ファイルデータには適用しない** — `testdata_notation.rst:883` が「この扱いは、テーブルデータの空行のスキップとは異なる仕組みである」と明記しており、ファイルデータでは全フィールド `""` のレコードとして保持するのが正。

**Steps**:

- [x] A. RED: 3経路それぞれに「全値が空文字の行を**先頭**に置く」「**中間**に置く」テストを追加し、失敗することを確認する
  - `buildTableDataList`（`setup_tables`）／`buildTableDataList`（`expected_tables`）／`buildListMapRows`（`list_maps`）
  - 中間位置は現状「値が全部 `""` のデータ行」として投入されるため、消えることを固定する
- [x] A2. RED: `list_maps` の `{}` 行についても、既存テスト `buildListMapRows_emptyRowIncludedAsEmptyMap`（`:765-777`）の期待値を「2件返り、いずれも通常行」へ書き換え、失敗することを確認する。**テストメソッド名と javadoc の Given/Then も書き換える**（現状の挙動を指す文言のため）。**フィクスチャ `emptyRowListMap` は変更しない**
- [x] B. フィクスチャ: **新規グループ**を足す。**既存グループ（`emptyRows` / `allEmptyRows` / `emptyRowMixed` / `leadingEmptyRow` / `emptyRowListMap` / `leadingEmptyRowListMap`）は変更しない**
- [x] C. GREEN: `resolveColumns` / `extractRows` を呼ぶ**前**に「空マッピング、または全ての値が null／空文字」の行を取り除く。本体（`isBlankLine` → `interpret`）と順序を揃える
- [x] D. `null` と `""` の双方が空とみなされること、値が1つでも非空なら残ることを確認する
- [x] E. javadoc（`YamlSection#resolveColumns` `:135-151`、`YamlTableDataBuilder` クラス javadoc `:36-37`・`:104-107`・`:139-142`・`:146`・`:192`・`:211-212`）を変更後の実装と食い違わないよう更新する
- [x] F. 変異確認（`指示/00-共通ルール.md:62`）: 追加した各テストについて、その分岐を壊す変更を1つ入れると落ちることを実際に確認し、元に戻す。**コマンドと結果を報告に含める**
- [x] G. `mvn -o clean test` 全 PASS 確認（`Tests run:` の行を確認）
- [x] H. commit・push
- [x] I. self-check (OK/NG per completion criterion, record in checks/task-21.md)
- [x] J. QA expert review (subagent)
- [x] K. Design expert review (subagent)
- [x] L. Craft expert review — coding (subagent)
- [x] M. Verification expert review — test (subagent)

**実施記録（2026-08-24）**:

- コミット3本: `fb58781`（実装本体）→ `14ad84a`（レビュー修正1）→ `a5cb6dd`（レビュー修正2）→ `d75c79c`（レビュー修正3）。すべて push 済み・force-push なし
- 実装: 判定を `YamlSection#isBlankRow` の1箇所に集約し、`dropBlankRows` を `resolveColumns` / `extractRows` より**前**に適用（本体 `PoiXlsReader#readLine` / `TestDataParsingTemplate#readTestData` と同じ順序）。前段で除去するため到達不能になった防衛分岐4件は削除。`resolveColumns` の読み飛ばしは public API の単体担保として残した
- マーカーカラム（`[COL]`）の値も空行判定の対象に含める。本体 `PoiXlsReader#isBlankLine` が行の全セルを対象とするのに合わせたもの
- レビュー3ラウンド（上限3）を消化。**Verification round 3 = 9変異すべて死亡・生存変異ゼロ・空振りテスト0件**
- 最終: `mvn -o clean test` → `Tests run: 207, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS（coordinator が単独実行して確認）
- 判定・Finding・triage・変異表は `checks/task-21.md`

**未解決（`#21` の完了条件の外側・ユーザー判断待ち）**: スキーマ `ntf-testdata-yaml-schema.json` の `$defs.table_data.properties.rows`（`:108`）と `$defs.list_map_data.properties.rows`（`:135`）の description に、空行スキップの規範が書かれていない。3レビュアーが独立に指摘（Design D / QA F1 / Craft F1）。とくに `:108` は「`null`（クォートなし）および `"null"`（クォートあり）はともに NullInterpreter により Java null に変換される」と等価性を明言しているが、**全値がそれだけの行**では分岐する（裸 `null` のみ → 行ごと消える／`"null"` のみ → 行は残り値が null）。**この分岐は本体 Excel と一致しており実装は正しい**（空セルは `isBlankLine` で落ち、文字列 `null` のセルは非空で残る）ので、直すべきはスキーマの description。coordinator 推奨は **`#22` step A1 として足すこと**（`#22` は step A0 で既に同じファイルに触り、`#18` で確立した「スキーマ description を実装に合わせる」作業と同種）

**確定スコープ（2026-08-24 ユーザー判断）**:

- **対象ケースは2つ** — 「全ての値が null または空文字の行」と「空マッピング（`{}`）の行」の両方
- **対象経路は2つ** — `table_data`（`setup_tables` / `expected_tables` / `expected_complete_tables`）と `list_maps` の両方。`{}` は #17 で `table_data` 側だけ塞いだ状態で、`list_maps` 側は未対応（`YamlTableDataBuilder.java:192-193, 202` が空 Map をそのまま結果に残す）
- **完了条件は `testdata_notation.rst:1534` の一文が「両経路 × 両ケース」で満たされていること**
- **ファイルデータには適用しない** — `:1534` が「別の仕組み」と明記（下の「適用範囲の境界」参照）

**既存テストの期待値変更（同判断）**: `YamlTableDataBuilderTest#buildListMapRows_emptyRowIncludedAsEmptyMap`（`:765-777`）は `:1534` に反する現在の実装を固定しているテストであるため、**期待値を「2件返り、いずれも通常行」へ変更する**。テストメソッド名と javadoc の Given/Then も現状固定の文言なので書き換える。**フィクスチャは変えない**。

**Completion criteria**:

- 全値が null／空文字の行が、先頭・中間のどちらに置かれても、`setup_tables` / `expected_tables` / `list_maps` の3経路すべてで行として存在しなくなる
- 空マッピング（`{}`）の行が、先頭・中間のどちらに置かれても、`setup_tables` / `expected_tables` / `list_maps` の3経路すべてで行として存在しなくなる（`list_maps` は現状 空 Map として結果に残っている）
- 値が1つでも非空の行は従来どおり保持される
- ファイルデータ（`YamlFileBuilder`）の挙動が変わっていない（全フィールド `""` のレコードは保持されたまま）
- 既存フィクスチャのグループが変更されていない
- 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている
- `pom.xml` / `argLine` が変更されておらず、他リポジトリへの書き込みが無い
- `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0）

---

### ~~#22: `record_fragment.rows` の description が述べる挙動を実経路で固定するテストを追加する~~

**Purpose**: `#18` で書き直した `$defs.record_fragment.properties.rows.description` が述べる挙動を、YAML ファイルを経由した実際の経路で実測して固定する。

**Prerequisites**: #18

**方針（2026-08-24 ユーザー判断）**: `YamlFileBuilderTest.java:527-543` のヘッダコメントの方針をそのまま踏襲する。すなわち **description の文字列自体は参照しない**。固定するのは文言が拠り所とする実挙動のみで、文言が変わってもテストは落ちない。

**Steps**:

- [x] A1. **（2026-08-24 ユーザー承認済み）** `#21` から回ってきたスキーマ description の追随。`$defs.table_data.properties.rows`（`:108`）と `$defs.list_map_data.properties.rows`（`:135`）に、`fb58781` で入れた「空マッピング（`{}`）の行および全値が `null` または空文字の行は、行として存在しないものとして扱う」旨を書く。**文言は `YamlSection#dropBlankRows` の実装から起こす。ただし実装と1対1で対応させない**（2026-08-24 ユーザーが当初指示を取り下げ。Rules の規範のとおり、スキーマ検証を通過しうる入力に対する観測可能な挙動だけを書く）。`"null"`（クォートあり）は値として非空のため行は残り、値が Java null になる点を含める。`:108` の既存記述「空配列 `[]` は `setup_tables` において全件 DELETE のみ」との関係（**どちらが先に効くか**）も書き分ける。**`record_fragment` の rows には適用しないことも、誤読を防ぐため明示する**（「ファイル系」と等値にしない — V5）
- [x] A0. **`#18` step R の F4 適用**（**A1 と同一ラウンドで処理する** — 2026-08-24 ユーザー支持）: `$defs.record_fragment.properties.rows.description`（スキーマ `:377`）の「（NTF は fields の順序で**位置対応させる**）」を「（NTF は fields の順序で**先頭から対応付ける**）」へ戻す。根拠は削除前の `:386`（`git show 35f70c7:src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` で確認済み = 「fields の順序で先頭から対応付けられる。」）。`description` 以外は触らない。JSON 妥当性を `python3 -c "import json; json.load(open(...))"` で確認する
- [x] A. `YamlFileBuilderTest` に、`#13` の先例（`:527-543` のヘッダコメント）と同じ体裁のセクションを起こす
- [x] B. フィクスチャ（`YamlFileBuilderTest/fileData.yaml`）に**新規グループ**を足す。既存グループは変更しない
- [x] C. 以下3件を最低限の対象として固定する（`YamlFileBuilder#buildDataFileList` → `DataFile#toDataRecords()` の経路）
  - 要素数が `fields` より少ない行 → **末尾のフィールドが `""` になる**こと
  - `rows: [[]]` → **全フィールドが `""` のレコード1件**になること（解説書 `rst:883`）
  - `rows: []` → **データ行0件**になること

  **期待値 `""` の出典（2026-08-24 ユーザー訂正）**: 判断A でも新事実3 でもなく、依存先 nablarch-testing（`2e43786`、作業ツリー clean）の `DataFileFragment.java:107`
  `String value = i < line.size() ? line.get(i) : "";`（`addValueWithId` は `:175` に同一。実物で確認済み）。`names.size()` 分だけ回し、行データが尽きた位置に `""` を入れる。固定長／可変長・型によらない共通処理。判断A も新事実3 もこの1行の系である。**指示書・報告にはこの出典を使う。**

  **期待値を固定する層（同ユーザー条件・coordinator 決定）**: **`toDataRecords()`（DataRecord 層）で固定し、フィクスチャは可変長（`VariableLengthFile`）を使う。** 可変長経路は変換が恒等写像なので、DataRecord 層の値がそのまま `:107` の `""` になる。出典チェーン（すべて `2e43786` で実物確認済み）:
  - `DataFile.java:155-161` `toDataRecords()` → 各 fragment の `toDataRecords()` を連結
  - `DataFileFragment.java:385-392` → `:401-405` `toDataRecord()` → `convertForDataRecord(value)` の結果を `putAll`
  - `VariableLengthFileFragment.java:31-38` `convertForDataRecord` → `:43-45` `convertValue` = `return stringExpression;`（**恒等**）
  - 値の生成元は `DataFileFragment.java:107`

  **固定長（`FixedLengthFile`）で DataRecord 層の `""` を期待値に書かない**。固定長は `FixedLengthFileFragment.java:45-59` の `convertForDataRecord` が `convertValue`（`:67-88`）に加えて `removePadding(key, converted, dummy)`（`:55`）を通し、最終的に `DataFileFragment.java:484` `dataType.removePadding(value)` に落ちる。`removePadding("")` が `""` を返す根拠は nablarch-core-dataformat 側にあるが、当リポジトリが依存する `6-NEXT-SNAPSHOT` には sources jar が無く（`~/.m2/repository/com/nablarch/framework/nablarch-core-dataformat/6-NEXT-SNAPSHOT/` に `*-sources.jar` 無し。旧版 1.3.5 / 2.0.3 の sources を現行版の根拠にはしない）、**クラス・行番号を示せないため未確認**。示せない以上、固定長で `""` を断定しない
- [x] D. 変異確認（`指示/00-共通ルール.md:62`）: 追加した各テストについて、その分岐を壊す変更を1つ入れると落ちることを実際に確認し、元に戻す。**コマンドと結果を報告に含める**
- [x] E. `mvn -o clean test` 全 PASS 確認（`Tests run:` の行を確認）
- [x] F. commit・push
- [x] G. self-check (OK/NG per completion criterion, record in checks/task-22.md)
- [x] H. QA expert review (subagent)
- [x] I. Craft expert review — coding (subagent)
- [x] J. Verification expert review — test (subagent)

**レビュー ラウンド1（step A0 + A1）の結果と 2026-08-24 ユーザー裁定** — 判定表・全 Finding は `checks/task-22.md`。ラウンド1の実装は `ee4a55e`。4観点（A 充足／B 整合／C 規約／D 検証の妥当性）を独立サブエージェントで実施し全観点 fail。

**step A0（`:377` の1語差し替え）は4観点とも OK**。裁定・修正の対象は step A1（`:108` / `:136` の description）のみ。

**ユーザー裁定3件（2026-08-24・確定）**:

1. **判断1 = 案A**。「マッピングでない行（スカラ等）」を除去対象の列挙から**落とす**。ユーザーの「実装と1対1で対応させる」という当初指示が誤りだったと明示的に取り下げられた。スカラ行は `$defs.table_data.properties.rows.items` = `{"type":"object"}`（`:109-117`）／`list_map_data` 同（`:137-145`）と `YamlLoader.java:121-125` のロード毎 `JSON_SCHEMA.validate` により `YamlSchemaValidationException` で弾かれ、description が説明すべき挙動ではない。**`castMap` / `isBlankRow` の実装は変更しない**（到達不能な防衛分岐として残す）。以後の規範は Rules に登録済み
2. **判断2 = 承認（1句足す）**。`$defs.table_data` は setup_tables / expected_tables / expected_complete_tables の3セクション共通であり、全行除去で 0 件になったとき expected 系では「対象テーブルに行が存在しないこと」の検証になる。根拠 `TableData.java:341-347`（`if (colNames.length == 0) { colNames = dbInfo.getColumns(tableName); }`）。**文面は逐語指定しない**（逐語指定の失敗が判断1のため）。実物から起こしてレビューで見る
3. **判断3 = 別タスク（`#24`）へ送る**。`#22` では直さない。起票条件は `#24` に記載

**裁定不要・修正ラウンドで直す Valid 指摘**（すべて coordinator が実物で裏取り済み。ユーザーは「9件（V2・V4・V5・V7・V8・V9・V10・V14 ほか）」と数えた。「ほか」に当たる1件は判断2 の expected 系0行の追記として処理する）:

| # | 指摘 | 出典 |
|---|---|---|
| V2 | 「`null` も `"null"` もともに **NullInterpreter により** 変換される」は機構として誤り。クォートなし `null` は `interpret` が `value == null` で早期 return するためチェーンに入らない | `YamlSection.java:247-249` |
| V4 | 「カラム名は除去後に残った行から決まる」→「残った**先頭の行**のキー。後続行にしか無いキーは無視される」 | `YamlSection.java:227-235`（`resolveColumns`）/ `YamlTableDataBuilder.java:214-221`（`extractRows` は `columnNames` のキーしか読まない） |
| V5 | 「ファイル系（`record_fragment`）」は誤り。`record_fragment` を使う `$defs` は `file_data` / `message_data` / `expected_request_message_data` / `group_message_data` の4つで、到達するトップレベルは `setup_files` / `expected_files` / `messages` / `expected_request_header_messages` / `expected_request_body_messages` / `response_header_messages` / `response_body_messages` の7セクション | schema `:184` `:210` `:243` `:274` と `properties` の `$ref`（python で実測） |
| V7 | 同一ファイル内で「テーブル系」の定義が矛盾。`:361`「テーブル系（table_data / list_map_data）」に対し `:108` は「テーブル系と list_maps」＝排他扱い | schema `:361` / `:108` / `:136` |
| V8 | `:136` に `testShots` の 0 件エラーとの接続が無い（`:132` は「0件はエラー」と記載） | `AbstractHttpRequestTestTemplate.java:225-229`（`testCases.isEmpty()` で `IllegalStateException`） |
| V9 | `NullInterpreter` は `equalsIgnoreCase` なので `NULL` / `Null` も Java null になる | `NullInterpreter.java:15` |
| V10 | **一部誤り（2026-08-24 実測で訂正）**。「クォートなし `null` には `~` も含まれる」は**偽**。当リポジトリの `snakeyaml-engine 3.0.1` は既定 `JsonSchema` で解決するため `~` は文字列 `~` になる。null になるのは `null`（クォートなし）と**値を省略した `COL:`** の2つ | 実測: `LoadSettings.builder().setAllowDuplicateKeys(false).build()`（`YamlLoader.java:104-106` と同一）で `A: null`→null / `B: ~`→String「~」/ `C:`→null / `D: "null"`→String / `E: NULL`→String / `F: Null`→String / `G: ""`→String「」。`settings.getSchema()` = `org.snakeyaml.engine.v2.schema.JsonSchema` |
| V14 | 文章面: `:136` が `:108` の写しで固有差分が薄い／同じ事実の二重記述／「裸の `null`」という第三の表記／`:108` が 890→1430字で膨張／「列名解決」「値加工」が実装由来の語で「カラム」統一から逸脱／「空配列 []」だけバッククォート無し | schema 実測・`checks/task-22.md` Craft 欄 |

**修正ラウンドで守る制約（`指示/doc-記載漏れの是正.md:25` の明文方針）**: 「**ダメなケースを数え上げて書き並べない。** 数え上げは終わらないうえ、書いた分だけ『書いていないケースは許される』という誤読を生む。」→ null にならない表記（`~` 等）は列挙せず、**null になる表記を肯定形で書く**にとどめる。

**却下（Invalid）と理由**:

- 「`:377` に戻した文言に前例が無い」（観点C）… 事実誤り。`git show 35f70c7:...` の `:386` に「fields の順序で**先頭から対応付けられる**」がある（coordinator が実物確認）。観点C の `git log -S'先頭から対応付ける'` は語尾違いで拾えていない
- A0 の括弧を削る提案（観点C）… 文言はユーザー逐語指定。範囲外
- 「半角スペースだけの値は残る」の明記（観点A・B）… 「空文字 `""`」は字義どおりで読み違えの余地がない。3観点のうち2つが「導出可能」判定。膨張を避ける
- 「`fields` より要素数が多い場合の余剰切り捨てが `:377` に無記載」（観点D）… **YML-14 として 2026-08-21 のユーザー判断で「対応不要（不正入力）」として閉じている**（`指示/doc-記載漏れの是正.md:23`）。範囲の問題ではなく方針として閉じた件（2026-08-24 ユーザー指示により却下理由を差し替え）

**レビュー要否（2026-08-24 ユーザー指示・ステップ単位）**:

- **step A0 + A1（スキーマ description）… 回す**。外から観測できる公開本文に新しい規範が入り、文言を CC 側が起こすため。**ラウンド1は4観点**（A 充足／B 整合／C 規約／D 検証の妥当性）。**対象にはユーザーの指示文そのものを含める**（「指示された案に反例がないか」）。観点B は `dropBlankRows` の実装と description の一致を**実物で照合**する。観点D は「その description で、実装が持つ全ケースを言い切れているか」を見る
- **step C（テスト追加）… 回す**。テストの設計が CC 側にあるため。「意味のあるテスト」の条件（`指示/00-共通ルール.md`）を満たすこと。壊す変更を入れたら落ちることを実際に確認し、**コマンドと結果を報告に書く**

**参考（`#18` step P で実測済み。テストの期待値の裏取りに使える）**: 固定長・可変長の双方で要素数不足は `""` 補完される（`FixedLengthFile` → `row[0]={FIELD2=, FIELD1=AAAAA}` / `row[1]={FIELD2=, FIELD1=}`）。`addValue` は `DataFileFragment.java:102-115` の1箇所のみで override 無し。

**Completion criteria**:

- 上記3件がテストとして存在し、`description` の文字列を参照していない
- 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている
- 既存フィクスチャのグループが変更されていない
- `mvn -o clean test` が `Tests run:` 出力つきで BUILD SUCCESS（Failures/Errors/Skipped すべて 0）

---

### ~~#24: `:108` のカラム省略まわりの記述の乖離を是正する~~

**Purpose**: `#22` のレビュー ラウンド1 で見つかった、`$defs.table_data.properties.rows` の**既存**記述と実装の乖離を潰す。あわせて `:361` の「ファイル系」表記を直す。いずれも `#21` の空行除去によってカラム名決定行が動くようになったため顕在化した。

**Prerequisites**: #22

**注**: 番号 `#23` は本体 nablarch-testing の課題番号として `#15` の表題で既出のため、衝突を避けて欠番とする。

**確定スコープ（2026-08-24 ユーザー指示。3点）**:

- **(a)** `:108` の「各オブジェクトに含まれないカラム（省略したカラム）」**3文**の是正。setup_tables の1文だけでなく expected_tables / expected_complete_tables の2文も対象（観点D の指摘）。文面は逐語指定しない。「省略」が「全行で省略」なのか「その行で省略」なのかを読み手が区別できる書き方にし、空行除去でカラム名決定行が動く点との関係も書く
- **(b)** `:361` の「ファイル系」を「record_fragment」に差し替える。反例が見つかったら反映せず報告する
- **(c)** (a)(b) の波及先の特定を、**反映より前**の作業として行う。スキーマ内の相互参照と、既存テストのうち文言に依存しているものを洗い出し、**完了条件の対象に含める**

**Steps**:

- [x] C. **波及先の特定（反映より前）** — 実施済み。スキーマ内の相互参照3件（`:18` / `:25` / `:108` 内 FK ブロック）を是正対象に追加。description の文字列を assert するテストは**0件**のためテスト変更は不要。詳細は `checks/task-24.md`
- [x] A. **(a) の帰結を実物で確認する** — 実施済み。`#22` レビューの出典チェーンを再点検し、**INSERT の実行箇所まで通した**（本体 `TableData.java:137-177` `insertData` と `:325-334` `getNonComputedColumns` = DB 全カラム。よって YAML に無いカラムも INSERT 文に載り `convert` の `!containsKey` 分岐でデフォルト値になる）。詳細は `checks/task-24.md` の step A
- [x] B. **(a) の反映** — `:108` の該当3文を「(1) 全行で省略 / (2) その行だけ省略」の区別に建て直し、波及先 `:18` / `:25` / `:108` 内 FK ブロックも同じ区別に揃えた
- [x] B2. **(b) の反映** — `:361` の見出しを「【record_fragment の rows は配列の配列】」へ。着手前に独立検証し反例なし（`$ref` 元4つ・到達するトップレベル7セクション）
- [x] D. JSON 妥当性確認と `mvn -o clean test` 全 PASS（`Tests run: 210, Failures: 0, Errors: 0, Skipped: 0`）
- [x] E. commit・push
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-24.md)
- [x] G. レビュー **必要**（description を CC が起こすため。Rules の基準）
  - **ラウンド1 = 2観点のみ**（**B 整合** / **D 検証の妥当性**）。**A 充足 と C 規約は回さない**（2026-08-24 ユーザー指示）。理由: `#24` は既存 description の是正であり実装変更が無く、外から観測できるものに新しいものが入らない。観点A は対象が (a) の3文と (b) の1語に確定済みで抜けが生じない。観点C は既存 description の文体に揃えるだけで実害が小さい
  - **B 整合** — CC が起こした文が実装と矛盾しないか。次の2つを**別々に**見る
    - **B-1 成果物**: `:108` と `:361` の新しい文言が、`nablarch-testing` の実物と一致するか
    - **B-2 指示文そのもの**: レビュー役の指示文に反例がないか。とくに (b) の「ファイル系 → record_fragment」案と、(a) の根拠として挙げた `Assertion.java:256` / `:297`、`TableData.java:193`、`TableData.java:707-720`。**反例が出たら反映せず報告する**
  - **D 検証の妥当性** — 新しい文言が正しいことの確認手段は、本当に欠陥を検知できるか。実装との読み比べで済ませていないか。`#22` と同じく、文言が述べる挙動を**実経路のテストで固定**し、そのテストが「文言どおりでない実装」を実際に落とすことを確認しているか。確認したコマンドと結果を見る
  - 各担当は別サブエージェントで実施し、プロンプトに次の3点を必ず入れる: 実測で裏付ける／付属の検証スクリプトを正解として使わず独立に組む／敵対的に見る
  - **ラウンド2以降は差分限定2観点**（是正が指示範囲に収まっているか／是正が新しい欠陥を生んでいないか）。**上限3回**。各ラウンドの指摘件数と観点を記録する
  - レビュー用サブエージェントには個別の一意な作業ディレクトリを割り当てる

**レビュー記録**:

| ラウンド | 観点 | 指摘件数 | 判定 |
| --- | --- | --- | --- |
| 1 | B 整合（B-1 / B-2） | Valid 4（V-1〜V-4）・要確認1 | B-1 NG・B-2 反例あり |
| 1 | D 検証の妥当性 | Valid 3（D-1〜D-3）・要確認1 | fail |
| 2 | 観点1（差分限定範囲） | 1件 | Invalid（:361 は 2f060e8 の承認済みスコープ、比較範囲の誤りによる誤指摘） |
| 2 | 観点2（新しい欠陥） | 1件 | Valid（Boolean NPE 例外文が原因を行内省略に限定して読める書き方だった） |
| 3 | 観点1（差分限定範囲） | 0件 | 範囲内 |
| 3 | 観点2（新しい欠陥） | 0件 | Invalid（新しい欠陥なし。実経路カバレッジを1件補強） |

**ラウンド1 是正内容**: Valid のみ反映。(1) の定義をカラム名決定行基準に一本化、(2) の Boolean 型 NPE 例外を追記、`:18` にマーカーカラム除外を補い、FK ブロック等の波及表現を揃えた。V-3（(a) 根拠3点目「補完値で比較される」）は偽と判定されたため反映していない。あわせて `YamlColumnOmissionTest`（14件）を新規追加し、`:18` / `:25` / `:108` の主張を実経路＋5系統の変異確認で固定（`checks/task-24.md` 参照）。`mvn -o clean test` = `Tests run: 224, Failures: 0, Errors: 0, Skipped: 0`

**ラウンド2 是正内容**: `:108` (2) 段落の Boolean NPE 例外文を、原因の経路（行内省略／クォートなし `null`／`COL:` 省略／クォート付き `"null"`）を問わず「値が null になれば」NPE になる旨へ書き替え、「null を明示しても防げない」ことを明記。実経路テストを1件追加（クォートなし `null` の明示、mutation で NPE が実際に発生・防止すると FAIL することを確認）。`mvn -o clean test` = `Tests run: 225`

**ラウンド3 是正内容**: description の書き替えは無し（新しい欠陥なし）。(2) 段落の4系統のうちクォート付き `"null"` 経由が未固定だったため実経路テストを1件補強。`mvn -o clean test` = `Tests run: 226, Failures: 0, Errors: 0, Skipped: 0`。ラウンド3で新しい欠陥が出なかったため step G 完了

**範囲外の欠陥（`#24` では直さず課題として起票）**:

- **X-1**: 旧 step C（マーカーカラム `[COL]` だけが非空の行がカラム名決定行になったときの帰結。`dataColumns` が0件になり全デフォルト値の1行が INSERT される／`list_maps` では空 Map が1件渡る）は、スコープ確定で `#24` から外れた。**未実施・未検証**。別タスクとして起票が必要
- **O-D1（ラウンド2/3 で実経路確認済み・本体側の記述、未修正）**: `:108` 末尾の FK ブロックの助言「NULL 許容カラムを NULL にしたい場合は省略せず `null`（クォートなし）を明示すること」は、**BOOLEAN 型カラムでは NPE になる**（本体 `TableData.java:163-164` の `setBoolean` unboxing。ラウンド2/3 で `YamlColumnOmissionTest` に実経路テスト3系統を追加し実測確認済み）。この文言自体は `c56207d` 以前から存在し `#24` の対象コミットが持ち込んだものではないため、`#24` では修正していない。別課題として起票が必要。本体 Excel 経路と共通のため YAML 側が持ち込んだ乖離ではない
- **O-D2（軽微）**: `list_maps` がテストコードへ渡す Map は `TreeMap`（キー昇順）である（`YamlTableDataBuilder.java:193`）。`:136` の「そのまま渡す」は記述順を保つとは書いていないので誤りではないが、キー順は観測されうる
- **O-D3（軽微）**: `testShots` が空のとき web 経路は旧 ID `testCases` へフォールバックし、両方空のときだけ例外になる（`AbstractHttpRequestTestTemplate.java:220-229`）。`:136` の「エラーになる」は既存 `:132` と同じ簡略化で、ファイル内では整合している

**Completion criteria**:

- (a) の帰結が実物の出典（ファイル:行番号）つきで確定している
- `:108` の記述が (a)(b) いずれについても実装と食い違わない
- step C で特定した波及先が是正されている
- `description` 以外のスキーマ要素が変更されていない
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#25: PR #1 レビュー中の未採番修正5件の追認~~

**Purpose**: `#14` step A の根拠取り直し（2026-08-24）で判明した、PR #1 レビュー中に発生したが番号未採番だった実装差分5件（`f375fde`・`630e700`・`10feb3e`・`6ea4655`・`b309359`）を追番・記録し、「#4以降の実装差分はすべて承認済みタスクに帰属」を実物と一致させる

**Prerequisites**: #24

**Steps**:

- [x] A. 5コミットの diff を実物で確認し、内容が commit message の主張どおりであることを検証（`checks/task-25.md`）
- [x] B. 5コミットの変更が現在のコードに反映されたまま残っていることを確認
- [x] C. self-check を `checks/task-25.md` に記録

**Completion criteria**:

- `checks/task-25.md` に5コミットの内容・実物確認結果が記録されている
- 5コミットの変更が現在のコードから失われていないことが確認されている

---

### ~~#19: 手順4 — 変更差分のカバレッジ実測と未達分岐を埋めるテスト追加~~

**Purpose**: このブランチが base から変更した `src/main`（10ファイル `+1843`・全部新規＝結果的にモジュール全体）について C0/C1 100% を満たす。

**Prerequisites**: #21

**着手時のベースライン（2026-08-24 実測・要再取得）**: `Tests run: 187, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS。`TODO`/`FIXME`/`@Ignore` は 0 件

**Steps**:

- [x] A. `mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes` → `mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec` でカバレッジを採取する（`pom.xml` / `argLine` は変更しない）
- [x] B. awk で未達クラス一覧を出し、**テストを足す前にユーザーへ提示する**（実測結果は `checks/task-19.md`）
- [x] B/C ゲート（**ユーザー判断・2026-08-24 明示指示**）: step A・B の実測結果（実行コマンドと生の出力、未達クラス一覧）を報告したら**そこで止まる**。どの分岐を埋めるかの指示を待つ。**勝手に step C へ進まない**。根拠: `指示/yaml-あるべき姿とカバレッジ.md:114`「**まず実測して、未達クラスの一覧を報告すること。** テストを足すのはその後。何件足すことになるかが分かってからでないとユーザーが判断できない。」
- [x] C. ユーザーの合図後、未達分岐を埋める「意味のあるテスト」を追加する → **不実施**（2026-08-24 ユーザー裁定: 未達2箇所は到達不能として承認。テストを足さない）
- [x] D. 追加テストごとに、その行・分岐を壊す変更を1つ入れると落ちることを実際に確認し、コマンドと結果を記録する → **N/A**（step C 不実施）
- [x] E. 到達不能と判断した行・分岐は、`@ExcludeFromCoverage` の類を足さず、理由をコードの行番号付きで報告する → 済（`checks/task-19.md` の「裁定」節。除外アノテーションは追加していない）
- [x] F. 再計測して `INSTRUCTION_MISSED` / `BRANCH_MISSED` が 0 であることを確認する → **再計測不要**（`src/main` も `src/test` も変更していないため step A の実測値が現状値。承認済み到達不能2箇所を除き 0）
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-19.md)
- [x] I. **レビュー要否はステップで割れる**（2026-08-24 ユーザー訂正）— step C 不実施により**レビュー必要ステップが消えた**ため、`#19` ではレビューを回さない（実装変更ゼロ）
  - **step A・B（カバレッジ実測）＝ レビュー不要**。代わりに **実行コマンドと生の出力** を報告する
  - **step C（未達分岐を埋めるテスト追加）＝ レビュー必要**。QA / Craft(coding) / Verification(test) を回す
  - step D・E・F（変異確認・到達不能の報告・再計測）は step C の成果物に付随するため step C のレビューに含める

**裁定（2026-08-24 ユーザー判断）**: 未達2箇所は**到達不能として承認**。テストを足さず step C は実施しない。根拠は `checks/task-19.md` の「裁定」節（要約: ①`YamlFileBuilder:227` の非 List 分岐は、スキーマ `:378-379` の `items.type = array` と `YamlLoader:121` の全ロード検証により到達前に落ちる。②`YamlLoader:60-61` `:65-66` はスキーマを自モジュールの `src/main/resources` に同梱しているため、クラスパス細工でしか再現できない）。リフレクション等で通すテストは書かない（壊れ方そのものが現実に起こらず「壊す変更で落ちること」を満たさないため）。

**Completion criteria**:

- 差分対象クラスの `INSTRUCTION_MISSED` が 0、`BRANCH_MISSED` が 0（到達不能としてユーザーが承認した箇所を除く）
- 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている
- 追加テストの javadoc に「何を担保するか」が1文で書かれている
- `pom.xml` / `argLine` が変更されていない
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#20: 手順5 — `mvn install`（下流 converter の前提）~~

**Purpose**: 順序3番目の `nablarch-testing-converter` が着手できるよう、緑になった成果物を `.m2` へ配置する。

**Prerequisites**: #19

**Steps**:

- [x] A. **解消（2026-08-24 ユーザー判断）**: 旧 State の禁止事項「yaml で `mvn install` しない（converter が `pom.xml:42-44` で `1.0.0-SNAPSHOT` に依存し install で壊れる）」は、**出典が確認できず解消**とする。この文言が初めて現れるコミット `1bae0de` の親コミットの State は `Notes: —` のみで、全リビジョンを走査しても禁止を記した State も Rules も存在しない。禁止理由「converter が install で壊れる」もコンパイル面で成立しない（`.m2` 旧 jar と作業ツリー `target/classes` の全クラス `javap` 比較で公開 API 差分は追加1件 `YamlSection.dropBlankRows` のみ、クラス削除・署名変更なし、`pom.xml` 完全一致）。禁止の存在を前提にした記述は残さない
- [x] A2. すべて緑であることを確認する — `Tests run: 226, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS（レビュー役が独立実行し 2026-08-24 16:39 に同結果を確認済み）
- [x] B. `mvn -o install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true` を実行する — 指示書どおりのコマンド（`clean` なし）は Rules（`jacoco:restore-instrumented-classes` は prepare-package で走る）どおり `Cannot process instrumented class` で BUILD FAILURE。Rules に従い `clean` を付けて再実行し BUILD SUCCESS
- [x] C. `~/.m2/.../nablarch-testing-yaml-1.0.0-SNAPSHOT.jar` のタイムスタンプが実行時刻へ更新されたことを確認する（着手前は 2026-08-18 09:30） — `2026-08-24 16:48` へ更新確認済み
- [x] D. self-check (OK/NG per completion criterion, record in checks/task-20.md)
- [x] E. **Craft/QA レビューは不要**（Rules の基準: ビルド。2026-08-24 ユーザー指示）。代わりに **実行コマンドと生の出力** を報告する — `checks/task-20.md` に記録済み

**Completion criteria**:

- `mvn -o clean test` が BUILD SUCCESS の状態で install されている
- jar のタイムスタンプが更新されている

---

### ~~#26: 2-1 — 空行判定が Java null を空扱いしている~~

**Purpose**: 解説書（`5b5c91e` の `implementation/testdata_notation.rst:1500`）が定めるスキップ条件は「空マッピング `{}`」と「すべての値が空文字」の2つだけである。`YamlSection#isBlankRow` が Java null も空として扱っているため、`COL: null` や `COL:` だけの行が消える。解説書に合わせる。

**Prerequisites**: none

**Steps**:

- [x] A. `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:201`-`:208` の `isBlankRow` を、空文字だけを空と見なし Java null を非空として扱うよう是正する（javadoc も合わせる）
- [x] B. 波及: `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:108` と `:136` の `description` から「全ての値が null または」を落とし、解説書 `:1500` の文言に合わせる
- [x] C. 既存テストの期待値見直し — `YamlSectionTest#dropBlankRows_*`（5件）・`YamlTableDataBuilderTest#buildTableDataList_blankValueRow*`（5件）・同 `#buildListMapRows_blankValueRow*`（2件）を**全件数え直し**、どれを変えどれを変えなかったかを記録する
- [x] D. 是正を押さえるテストを足す（`COL: null` だけの行・`COL:` だけの行が残ること／空マッピング `{}` と全値空文字はスキップされること）
- [x] E. **変異確認**: A の是正前は落ち是正後は通るテストを特定し、さらに追加/変更した各テストについて期待値をわざと崩すと落ちることを1度実行して確認し、コマンドと結果を記録する
- [x] F. `mvn -o clean test` 緑を確認（`Tests run:` 行を読む）
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-26.md)

**Completion criteria**:

- `isBlankRow` が空文字のみを空と見なし、Java null を非空として扱う
- スキーマ `:108`・`:136` の `description` が解説書 `:1500` と食い違わない
- 既存テスト12件（5+5+2）について、変更したもの・しなかったものが件数付きで記録されている
- 是正前に落ち是正後に通るテストが存在する
- 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#27: 2-2 — `isResourceExisting` の判定単位を Excel と揃える~~

**Purpose**: `TestDataParser#isResourceExisting` の呼び出し元（`TestSupport#getPathResourceExisting`・`RestTestSupport`）は**入れ物単位**（Excel の `basePath/<クラス名>.xls`）の意味で使っているが、YAML 実装は読み込み単位（シート相当）の存在を答えている。YAML の入れ物は `basePath/<クラス名>` ディレクトリである（`implementation/class_unit_test/component.rst:313`）。入れ物単位に揃える。

**Prerequisites**: none

**Steps**:

- [x] A. `YamlLoader.java:142`-`:143` / `YamlTestDataParser.java:103` を入れ物単位（`basePath/<クラス名>` ディレクトリの存在）に是正する
- [x] B. `YamlTestDataParser.java:109`（`getSetupTableData` の内部ガード）を、Excel が同位置で使う `isDataExisting` 相当（読み込み単位＝ファイルの存在）の判定に置き換える。`BasicTestDataParser.java:52`（`3c4bd2a`）参照
- [x] C. `tools/master_data_tool.rst:28` が述べる挙動（Excel 形式のファイルに YAML 用パーサを設定すると投入0件になり、例外も警告も出ない）が壊れていないことをテストで確かめる
- [x] D. 入れ物単位／読み込み単位それぞれを押さえるテストを足す（`setUpDb.yaml` を置いていないクラスで入れ物が真になること／存在しない入れ物が偽になること）
- [x] E. **変異確認**: 是正前に落ち是正後に通るテストの特定、および追加/変更した各テストの期待値を崩すと落ちることの確認
- [x] F. `mvn -o clean test` 緑を確認
- [x] G. **converter は直さない**。`nablarch-testing-converter@60d9a2d` の `YamlTestCoreAdapterTest.java:365`-`:370`（`isResourceExisting_reflectsFileExistence`）が落ちることを実測し、落ちたテスト名と理由を記録する（本モジュール外のため完了条件の対象外）
- [x] H. commit・push
- [x] I. self-check (OK/NG per completion criterion, record in checks/task-27.md)

**Completion criteria**:

- `isResourceExisting` が `basePath/<クラス名>` ディレクトリの存在を答える
- `getSetupTableData` の内部ガードが読み込み単位の判定に置き換わっている
- `master_data_tool.rst:28` の挙動を押さえるテストがある
- 是正前に落ち是正後に通るテストが存在する
- 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS
- converter で落ちたテストが実測され記録されている（converter のコードは変更しない）

---

### ~~#28: 2-3 — 送信同期4キーで `record_type` の記載値を保持する~~

**Purpose**: 解説書 `implementation/testdata_notation.rst:1163`（`5b5c91e` で改訂済み）は、`MESSAGE`（`setUpMessages`・`expectedMessages`）では記載値を使わず `"default"` に、同期応答メッセージ送信で使う4データタイプでは記載値をそのままレコード種別にすると定める。現行は `YamlFileBuilder.java:187`-`:189` が `messaging` 経路すべてで `"default"` に固定している。

**Prerequisites**: none

**Steps**:

- [x] A. `YamlFileBuilder#buildFragmentsInternal`（`:187`-`:189`）を、送信同期4キー（`EXPECTED_REQUEST_HEADER_MESSAGES`・`EXPECTED_REQUEST_BODY_MESSAGES`・`RESPONSE_HEADER_MESSAGES`・`RESPONSE_BODY_MESSAGES`）では記載値を保持し、`messages` では `"default"` のままとするよう是正する。`getMessage` と `getMessageWithoutCache` はどちらも `YamlMessageBuilder#buildMessagePool` を通るため**セクションキーで区別**する（`YamlTestDataParser.java:157`・`:164`-`:166`）
- [x] B. 既存テストの期待値見直し — `record_type: HEADER` を書いたフィクスチャ・`record_type: FW_HEADER` を書いたフィクスチャを**全件数え直し**、どれを変えどれを変えなかったかを記録する。特に `YamlFileBuilderTest#buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped`・`YamlMessageBuilderTest#buildMessagePool_fwHeaderRecordTypeIsNotSkipped`・`YamlTestDataParserTest#getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped`・`YamlTestDataParserTest#getMessage_fwHeaderRecordTypeIsNotSkipped` の4件は名前どおりの意味が変わる
- [x] C. `implementation/testdata_notation.rst:1299`-`:1301`（`record_type` に予約値はない）は変わらない。`FW_HEADER` が送信同期4キーで単に `FW_HEADER` というレコード種別になることを押さえるテストを足す
- [x] D. **変異確認**: 是正前に落ち是正後に通るテストの特定、および追加/変更した各テストの期待値を崩すと落ちることの確認
- [x] E. `mvn -o clean test` 緑を確認
- [x] F. commit・push
- [x] G. self-check (OK/NG per completion criterion, record in checks/task-28.md)

**Completion criteria**:

- 送信同期4データタイプで `record_type` の記載値が保持される
- `messages`（`setUpMessages`・`expectedMessages`）は `"default"` のまま
- `record_type` を書いた既存フィクスチャについて、変更したもの・しなかったものが件数付きで記録されている
- 是正前に落ち是正後に通るテストが存在する
- 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#29: 2-4 — テスト用 `yamlInterpreters` を解説書に合わせる~~

**Purpose**: 解説書 `setup/common.rst:77` は `yamlInterpreters` に指定するのは `DateTimeInterpreter` と `CompositeInterpreter`→`BasicJapaneseCharacterInterpreter` の2つだけと定め、`:81`（important）は `NullInterpreter` を指定してはならないと定める。現行 `src/test/resources/unit-test.xml:56`-`:76` は `NullInterpreter` と `LineSeparatorInterpreter` を含む。

**Prerequisites**: #26

**Steps**:

- [x] A. `src/test/resources/unit-test.xml` の `yamlInterpreters` から `NullInterpreter`・`LineSeparatorInterpreter` を外す
- [x] B. 解説書 `setup/common.rst:244`-`:257` が新設した `yamlMessagingInterpreters`（`CompositeInterpreter`→`BasicJapaneseCharacterInterpreter` のみ）に照らし、本モジュールのテストが電文用パーサを別に組んでいるかを実測し、組んでいればそちらも合わせる
- [x] C. 設定変更で挙動が変わるテストを洗い出し、期待値を解説書側に合わせる（文字列 `"null"` が Java null にならないこと等）
- [x] D. **変異確認**: 是正前に落ち是正後に通るテストの特定、および追加/変更した各テストの期待値を崩すと落ちることの確認
- [x] E. `mvn -o clean test` 緑を確認
- [x] F. commit・push
- [x] G. self-check (OK/NG per completion criterion, record in checks/task-29.md)

**Completion criteria**:

- `unit-test.xml` の `yamlInterpreters` が `DateTimeInterpreter` と `CompositeInterpreter` の2つだけ
- 電文用パーサの有無が実測され、あれば `yamlMessagingInterpreters` 相当に揃っている
- 是正前に落ち是正後に通るテストが存在する
- 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#30: 2-5 — スキーマ `description` 4件を解説書に合わせる~~

**Purpose**: `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` の `description` は SSoT の適用範囲である（2026-08-25 ユーザー確定）。解説書と食い違う4件（指示書 2-5 の名指し3件＋2-3 の波及先1件）を是正する。

**Prerequisites**: #26

**Steps**:

- [x] A. `:410`（`length`）— 「`"-"` フィールドの値は NTF が格納時に改行コードおよび前後空白を除去する」を、除去されるのが**改行と、その前後の空白**であり改行を含まない値の前後空白は残ることが分かる文言へ是正する（`implementation/testdata_notation.rst:1059`）
- [x] B. `:108`（`rows`。テーブル系）— FK 制約の文言が BOOLEAN 型カラムで矛盾する点を是正する（`implementation/testdata_notation.rst:820`-`:833`）。空行除去の条件は #26 で是正済みであることを確認する
- [x] C. `:136`（`rows`。`list_map`）— 空行除去の条件が #26 で是正済みであることを確認する
- [x] B2. `:108`・`:136` の `description` に残る `NullInterpreter` 前提の記述を #29（2-4）の是正後の挙動に合わせる。`:108`「クォート付きの `"null"` や大文字を含む `NULL` / `Null` は文字列としてロードされ、NullInterpreter が null へ変換する」「クォート付き `"null"`（NullInterpreter 変換後）」、`:136`「`"null"` / `NULL` は…マップの値が Java null になる」は、解説書 `setup/common.rst:81` が `NullInterpreter` を禁じているため YAML 経路では成立しない（#29 で `YamlColumnOmissionTest` の期待値が実際に反転した）。**`:108`・`:136` は指示書 2-5 の名指し3件に含まれる**
- [x] B3. `src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:174` の javadoc が「値加工を通すと空になる値（例えば `NullInterpreter` が Java null へ変換する `"null"`）」と `NullInterpreter` を例に挙げている。#29 の是正で YAML 経路では成立しないため直す（`:365` と同じく、自分の是正で事実に反することになった記述）
- [x] C2. `:365`（`$defs.record_fragment.record_type`）— 「メッセージング系（messages / expected_request_* / response_*）では NTF 内部で常に `"default"` に置換されるため実行時の挙動に影響しない」を #28 の是正後の挙動に合わせる。`$defs.record_fragment` は `messages` と送信同期4セクションの両方から参照される共用定義であり、送信同期4セクションでは記載値がそのままレコード種別になる（`implementation/testdata_notation.rst:1163`）。`:208`（`$defs.message_data.records`）は `messages` 専用の定義に付いており是正後も正しいため**変更しない**。**指示書 2-5 の名指し3件の外**であり、2-3 の波及先として 2026-08-26 ユーザー判断で追加した
- [x] D. `notation.rst:1059` の `fields[].length: "-"`（全レコードの最大バイト長への自動拡張と、値中の改行とその前後の空白の除去）の挙動を押さえるテストを足す（`schemaFullCoverage.yaml:87` にデータはあるがテストが無い。A の根拠として同時に押さえる）
- [x] E. **変異確認**: 追加した各テストについて期待値を崩すと落ちることの確認
- [x] F. `mvn -o clean test` 緑を確認
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-30.md)

**Completion criteria**:

- `:410`・`:108`・`:136`・`:365` の `description` が解説書と食い違わない（`:365` は 2-3 の波及先。`:208` は変更しない）
- `length: "-"` の挙動（最大バイト長への自動拡張・改行とその前後空白の除去・改行なし値の前後空白は残る）を押さえるテストがある
- 追加した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#31: 3-1〜3-6 — 記法・特殊記法のテスト追加（6件）~~

**Purpose**: 解説書に記述があり既存テスト226件が押さえていない記法6件をテストで押さえる。**落ちたものは直さず `@Ignore` にして記録する。**

**Prerequisites**: #26, #27, #28, #29, #30

**Steps**:

- [x] 3-1. YAML 1.2 Core Schema で解釈されるため、クォートなしの `no`・`yes`・`on`・`off` がキーでも値でも文字列のままになる（`notation.rst:92`・`:1399`、`implementation/deal_unit_test/batch.rst:352` の実例 `- no: "1"`）
- [x] 3-2. `${<文字種>,3}` が14文字種それぞれで該当文字種3文字になる（サロゲートペアは3コードポイント）。**列挙外の文字種名は変換されないという負のテストも必ず書く**（`notation.rst:1313`-`:1320`）
  （#41 で失効: この負のテストは解説書に無い「あるべき姿」を追っていたため #41 で削除した。根拠は `nablarch-document@09779f6`「docs: 限定列挙に付けた「それ以外はエラー」を落とす」が当該文から `（それ以外を指定するとエラーになる）` を意図的に削除していること。14種類が使えること自体は `YamlTableDataBuilderTest.buildListMapRows_allFourteenCharacterTypesAreGenerated` が引き続き担保する）
- [x] 3-3. `"${半角数字,2}-${半角数字,4}"` が7文字になり3文字目が `-` のまま残る（`notation.rst:1322`）
- [x] 3-4. `"\n"` が LF 1文字（`U+000A`）になる（`notation.rst:1441`-`:1443`）
- [x] 3-5. `"20210123123456"` が `2021-01-23 12:34:56.000`、`"20210123"` が `2021-01-23 00:00:00.000` に評価される（`notation.rst:1326`-`:1331`）
- [x] 3-6. `"${attach:ファイルパス}"` がアップロードファイルの指定として読める（`notation.rst:1337`）
- [x] X. 落ちたものは実装を直さず `@Ignore("NTF-DOC: <解説書パス>:<行> — 期待 X / 実際 Y")` にして記録する。**範囲の判断を持たない**
- [x] Y. **変異確認**: 通った各テストについて期待値をわざと崩すと落ちることを1度確認し、コマンドと結果を記録する
- [x] Z. `mvn -o clean test` 緑を確認・commit・push・self-check（`checks/task-31.md`）

**Completion criteria**:

- 3-1〜3-6 の6件すべてについてテストが存在する
- 落ちたものは `@Ignore` ＋ `NTF-DOC:` 印つきの理由で記録されている（実装は直していない）
- 3-2 の負のテスト（列挙外の文字種名は変換されない）が書かれている
  （#41 で失効。上と同じ理由。当時の実測記録としては真だが、現在の src/ には該当テストは存在しない）
- 通った各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#32: 3-7〜3-13 — キー解決・グループ・電文配置のテスト追加（7件）~~

**Purpose**: 解説書に記述があり既存テスト226件が押さえていない挙動7件をテストで押さえる。**落ちたものは直さず `@Ignore` にして記録する。**

**Prerequisites**: #31

**Steps**:

- [x] 3-7. グループIDは完全一致で突合される。`case01` を指定したとき `case010` を持つエントリは収集されない（`notation.rst:255`-`:269`）
- [x] 3-8. `setup_tables_extra` のような前方一致するトップレベルキーは `setup_tables` として読まれず、スキーマ違反になる（`notation.rst:205`）
- [x] 3-9. `expected_tables` に group_id `a`・`b`・`a` の順で並べても group_id `a` の収集結果は2件になる（Excel のように1件で打ち切られない）（`notation.rst:339`）
- [x] 3-10. `messages` の `id` に予約値 `setUpMessages`・`expectedMessages` を書いて取得できる（`notation.rst:1149`）
- [x] 3-11. モックアップクラスの電文は、リクエストIDと同じ名前のディレクトリ配下の固定名 `message.yaml` が読み込み単位になる。`<リクエストID>.yaml` では読まれない（`implementation/deal_unit_test/mom.rst:72`）
  - 申し送り（#29 実測）: `src/test/resources/unit-test.xml:170`-`:174` の `filePathSetting` が `fileExtensions` に `sendSyncTestData` = `xls` を設定している。解説書 `setup/common.rst:263`（important）は「`fileExtensions` には `sendSyncTestData` を設定しない。YAML 形式ではリクエストIDと同じ名前のディレクトリを参照するため、拡張子を設定するとテストデータが見つからず、テストの実行時に例外が発生する」と定める。**18件のいずれにも該当しないため #29 では未変更。** 3-11 がこれに阻まれる場合は、設定を変えずに済む書き方（`TestDataParser` を直接使う等）でテストを書き、阻まれた事実を報告する。**設定を変える判断はしない**
- [x] 3-12. `TestDataParser` を直接使うとき、第2引数 `<ファイル名>/<読み込み単位の名前>` が `<ディレクトリ>/<ファイル名>/<読み込み単位の名前>.yaml` に解決される（`implementation/class_unit_test/component.rst:313`）
- [x] 3-13. `rows:` に `args[0]: "x"` と書くと返る Map のキーが文字列 `"args[0]"` になる（`[` `]` を含むキーがマーカーカラムとして除外されない）（`notation.rst:503`-`:507`）
- [x] X. 落ちたものは実装を直さず `@Ignore("NTF-DOC: <解説書パス>:<行> — 期待 X / 実際 Y")` にして記録する。**範囲の判断を持たない**
- [x] Y. **変異確認**: 通った各テストについて期待値をわざと崩すと落ちることを1度確認し、コマンドと結果を記録する
- [x] Z. `mvn -o clean test` 緑を確認・commit・push・self-check（`checks/task-32.md`）

**Completion criteria**:

- 3-7〜3-13 の7件すべてについてテストが存在する
- 落ちたものは `@Ignore` ＋ `NTF-DOC:` 印つきの理由で記録されている（実装は直していない）
- 通った各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#33: カバレッジ C0/C1 計測と Step 4 報告書の作成~~

**Purpose**: 指示書「4. 完了条件」5 と「6. 報告」を満たす。

**Prerequisites**: #32

**Steps**:

- [x] A. `mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes` → `mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec` で C0/C1 を計測する（`pom.xml` / `argLine` は変更しない）
- [x] B. `src/main` の是正でカバレッジが下がった箇所があれば挙げる
- [x] B2. converter（`60d9a2d`。**変更しない**）の失敗テストを帰属付きで実測する。Step 4 着手前（`ab0064e`）の本モジュールを `.m2` へ install した状態と、Step 4 完了後の状態とで converter の `mvn -o clean test` を実行し、差分から「Step 4 起因の失敗」を切り分ける。#26（空行判定）起因の失敗が `YamlFormatReaderInvalidInputTest` 2件・`YamlFormatReaderScalarTest` 2件として観測されている（#27 実測。要再確認）ため、2-2 起因の1件と合わせて全件を理由付きで挙げる
- [x] C. 報告書を `.rn/ntf-yaml/report-step4.md` に1ファイルでまとめる。順序は指示書 §6 のとおり — ①第2節5件の是正結果（変更ファイルと `file:line`、直す前に落ちたテスト名）②第3節13件の結果（通った／`@Ignore` の内訳。`@Ignore` は理由の文言をそのまま載せる）③期待値をわざと崩す確認の結果（対象テスト名と崩した内容）④既存テストの期待値を変えた箇所の全件（2-1・2-3 それぞれ件数を数えて）⑤カバレッジ C0/C1 の計測結果と converter で落ちたテスト
- [x] D. 後始末 — `git status --short` が空。`tmp/` と `javac.*.args` を残さない。一時ファイル・作業用スクリプト・ログを消す
- [x] E. commit・push
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-33.md)

**Completion criteria**:

- C0/C1 が計測され、下がった箇所が挙がっている
- `.rn/ntf-yaml/report-step4.md` に §6 の5項目がこの順で載っている
- `git status --short` が空、`tmp/` と `javac.*.args` が無い
- push 済み

---

### ~~#35: 報告書 §6 の未是正2件を是正する~~

**Purpose**: 2026-08-27 のユーザー指示（`/rn:gm` 差し戻し）。#34 の判定を仰いだ結果、報告書 §6 に挙げた「指示書18件の外の食い違い」2件を直してから #34 を再提示することになった。

**Prerequisites**: #33

**Steps**:

- [x] A. 6-2 の門番テスト `YamlTestDataParserTest#fileExtensionsHasNoSendSyncTestData` を先に足し、**削除前に落ちること**を実測する
- [x] B. `src/test/resources/unit-test.xml` の `filePathSetting` から `fileExtensions`（`sendSyncTestData` = `xls`）を削除する。根拠は `setup/common.rst:264`（ピン `5b5c91e`）。`basePathSettings` には手を入れない
- [x] C. 門番テストが**削除後に通ること**を実測する
- [x] D. 6-1 — `ntf-testdata-yaml-schema.json:53`・`:200` の `description` を `implementation/testdata_notation.rst:1151`（ピン `5b5c91e`）に合わせる。id は `setUpMessages`・`expectedMessages` の固定値であること、`sendSyncTestData/{requestId}/message` は読み込み単位のパスでデータブロックの識別子ではないことを書く。新しい挙動テストは足さない（根拠テストは既存の `getMessage_reservedIdsSetUpMessagesAndExpectedMessages`）
- [x] E. D の**前後で挙動テストの結果が変わらない**ことを実測する
- [x] F. `mvn -o clean test` が緑（`Tests run: 268, Failures: 0, Errors: 0, Skipped: 1`。`Skipped 1` は 3-2 の `@Ignore` のまま）
- [x] G. 報告書 `.rn/ntf-yaml/report-step4.md` の §6 を「是正済み」に書き換える
- [x] H. `git status --short` が空。commit・push
- [x] I. self-check (OK/NG per completion criterion, record in checks/task-35.md)

**Completion criteria**:

- 門番テストが削除前は落ち、削除後は通る（順序が報告に書かれている）
- `mvn -o clean test` が緑。`Skipped 1` は `@Ignore` 1件のまま
- 6-1 の前後で挙動テストの結果が変わらないことが示されている
- 報告書 §6 が「是正済み」になっている
- `git status --short` が空、push 済み

---

### ~~#34: Evaluation sign-off（Step 4）~~

**Purpose**: 指示書「4. 完了条件」8項目を実測で通し、ユーザーの評価ゲートを取る。

**Prerequisites**: #33、#35

**Steps**:

- [x] A. 指示書 §4 の完了条件8項目を1つずつ実測し、結果をユーザーに提示する（2026-08-26 提示済み。8項目すべて OK）
- [x] B. 1回目の判定は `/rn:gm`（差し戻し）。報告書 §6 の未是正2件の是正を指示された → `#35` で対応
- [x] C. `#35` の完了条件を実測し、#34 を再提示する（2026-08-27）
- [x] D. `/rn:ty`（承認）または `/rn:gm`（差し戻し）の判定を受ける（2026-08-27 **`/rn:ty` 承認**）

**Completion criteria**:

- 完了条件8項目の実測結果が提示されている
- ユーザーの判定が出ている

---

### ~~#36: 2-1 — ファイル・電文の末尾フィールドの `null` が `null` のまま残る~~

**Purpose**: 解説書 `implementation/testdata_notation.rst:889`（ファイル）・`:1155`（電文）（ピン `afa4f9e`）は、末尾のフィールドに `null` と記述した場合は形式によらず `""` になり、後ろに値のあるフィールドがあれば `null` のまま保持されると定める。本体は `DataFileParser.java:68` が `NablarchTestUtils.trimTailCopy` を掛けてから `DataFileFragment.addValue`（名前の数まで `""` で埋める）に渡す。現行 `YamlFileBuilder.java:243`-`:249` は `trimTail` 相当を持たない。本体の実装をそのまま使って追随する（規則を手写ししない）。

**Prerequisites**: none

**Steps**:

- [x] A. 本体を oracle にしたテストを**先に**書き、是正前に落ちることを実測する。入力は指示書 §3 の F1〜F6・M1・S2（送信同期は4種のうち1つ以上）。oracle は POI で組んだ `.xlsx` を本体 `BasicTestDataParser`（`PoiXlsReader` ＋ `NullInterpreter` → `QuotationTrimmer` → `LineSeparatorInterpreter`）で読み、`DataFile#toDataRecords()` の値を比べる
- [x] B. `YamlFileBuilder.buildFragmentsInternal` で `rowValues` を `addValueWithId`（`:247`）／`addValue`（`:249`）に渡す直前に `NablarchTestUtils.trimTailCopy` を通す。`interpret` → `trimTail` → `addValue` の順は本体と同じ
- [x] C. A のテストが是正後に通ることを実測する
- [x] D. 既存テストの期待値見直し — 着手前調査で末尾 `null` のフィクスチャは**0件**と実測済み。是正後に落ちた既存テストがあれば全件挙げ、解説書に合わせて直す。変えた／変えなかったを件数つきで記録する
- [x] E. **変異確認**: 追加/変更した各テストについて、期待値をわざと崩すと落ちることを1度実行して確認し、コマンドと結果を記録する
- [x] F. `mvn -o clean test` 緑を確認（`Tests run:` 行を読む）
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-36.md)

**Completion criteria**:

- `YamlFileBuilder` が `addValue`／`addValueWithId` の直前で本体の `NablarchTestUtils.trimTailCopy` を通している（規則の手写しをしていない）
- 本体 `BasicTestDataParser` を正解にしたテストがあり、F1〜F6・M1・S2 を入力に含む
- 是正前に落ち是正後に通るテストが存在する
- 既存テストで期待値を変えたもの・変えなかったものが件数付きで記録されている
- 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#37: 2-2 — 電文の `records:` に2つ以上のレコードレイアウトを書ける~~

**Purpose**: 解説書 `implementation/testdata_notation.rst:1153`・`:1299`（ピン `afa4f9e`）は、電文のレコードレイアウトは1つであり2つ以上記述するとエラーになると定める。本体 `MessageParser.java:70`-`:76` は名前行への切り替えを持たず複数レイアウトを表す記法が無い。現行スキーマは3箇所の `records` に上限が無い。静的に決まるためスキーマ検証で弾く。

**Prerequisites**: none

**Steps**:

- [x] A. `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` の `message_data.records`（`:205`-`:208`）・`expected_request_message_data.records`（`:238`-`:240`）・`group_message_data.records`（`:269`-`:271`）に `maxItems: 1` を加える
- [x] B. スキーマ検証（`YamlLoader`）で落ちることを押さえるテストを足す。例外の型と、メッセージに出所（セクション・`id`／パス）が入ることを assert する
- [x] C. 既存フィクスチャの是正 — 着手前調査で該当は**3エントリ**（`YamlTestDataParserTest/messageData.yaml` の `messages/fwHeaderRecordType001`（`:31`）・`messages/legacyFwHeaderRecord001`（`:53`）・`response_body_messages/sync001`（`:163`））。同ファイルは `YamlTestDataParserTest` の16箇所から読まれるためロード自体が失敗する。各エントリが押さえていた意味（`record_type: FW_HEADER` が特別扱いされないこと）を保ったまま、レコード1つの記法へ書き換える
- [x] D. 既存テストの期待値見直し — `getMessage_fwHeaderRecordTypeIsNotSkipped`（`YamlTestDataParserTest.java:940`）・`getMessage_legacyFwHeaderRecordCausesRecordLengthMismatch`（同 `:1112`）・`fwHeaderSync` を使うテスト（同 `:990`）を全件数え直し、どれを変えどれを変えなかったかを記録する
- [x] E. **変異確認**: 追加/変更した各テストについて期待値を崩すと落ちることを確認する
- [x] F. `mvn -o clean test` 緑を確認
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-37.md)

**Completion criteria**:

- 3箇所の `records` に `maxItems: 1` が入っている
- `records` を2つ書くとスキーマ検証で落ちるテストがあり、メッセージに出所が入ることを assert している
- 是正前は通り是正後に落ちる既存テストが全件挙がり、解説書に合わせて直されている（件数付き）
- 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#38: 2-3 — `fw_header:` に `reader.fwHeaderfields` に無いキーを書いても通る~~

**Purpose**: 解説書 `implementation/testdata_notation.rst:1295`（ピン `afa4f9e`）は、`fw_header:` に記載できるキーは `reader.fwHeaderfields` の名前（省略時は `requestId`・`userId`・`resendFlag`・`resultCode`）だけであり、それ以外のキーがあるとエラーになると定める。本体 `MessageParser.java:33`・`:102`-`:110` と同じ集合の作り方に揃える。設定に依存するため実装（`YamlMessageBuilder.convertFwHeader`）で検査する。

**Prerequisites**: none

**Steps**:

- [x] A. `YamlMessageBuilder.convertFwHeader`（`:233`-`:246`）で、キーが集合に無ければ例外にする。集合は本体と同じく `SystemRepository.getString("reader.fwHeaderfields")` が空なら既定4つ、あれば `NablarchTestUtils.makeArray`（カンマ分割・前後空白を取り除かない）で作る。例外には電文の `id` と不正なキー名を含める（同メソッドの既存の `IllegalStateException` と同じ形）
- [x] B. テストを足す — `reader.fwHeaderfields` を設定した場合・しない場合の両方。例外の型と、メッセージに `id` と不正キー名が入ることを assert する
- [x] C. 既存フィクスチャ・テストの是正 — 着手前調査で該当は**キー3件・テスト4件**。キー: `YamlMessageBuilderTest/customFwHeaderData.yaml:9` `customField`、`YamlMessageBuilderTest/fwHeaderMapData.yaml:14` `customProjectKey`、同 `:40` `boolFlag`。テスト（いずれも `reader.fwHeaderfields` を設定していないため是正後に落ちる）: `buildMessagePool_customFwHeaderFields`（`YamlMessageBuilderTest.java:792`）・`buildMessagePool_fwHeaderMapAllKeysRetainedIncludingCustom`（同 `:824`）・`buildMessagePool_fwHeaderMapReadableWithoutHeaderRecord`（同 `:854`）・`buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues`（同 `:985`）。どれを変えどれを変えなかったかを件数つきで記録する
- [x] D. **変異確認**: 追加/変更した各テストについて期待値を崩すと落ちることを確認する
- [x] E. `mvn -o clean test` 緑を確認
- [x] F. commit・push
- [x] G. self-check (OK/NG per completion criterion, record in checks/task-38.md)

**Completion criteria**:

- `convertFwHeader` が既定4つ／`reader.fwHeaderfields` の名前だけを許し、それ以外を例外にする
- 集合の作り方が本体と同じ（同じキー・同じ既定4つ・同じ `makeArray`）
- `reader.fwHeaderfields` を設定した場合・しない場合の両方のテストがある
- 例外メッセージに電文の `id` と不正なキー名が入ることを assert している
- 既存フィクスチャ・テストで変えたもの・変えなかったものが件数付きで記録されている
- 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#39: 2-4 — 空エントリの判定が「すべての値が空文字」を含む（第1回 2-1 の上書き）~~

**Purpose**: 解説書 `implementation/testdata_notation.rst:1502`（ピン `afa4f9e`。`6bfc058` で改訂済み）は、読み飛ばされるのは `rows:` 内の要素が空マッピング（`{}`）の場合だけであり、`""` と書いた空文字は値であって、すべての値が `""` のエントリは全カラムが空文字のエントリとして読み込まれると定める。Excel では `""` と書いたセルは空セルではない（本体 `PoiXlsReader` の `isBlankLine` は生セルの `isEmpty()` だけを見る。`""` は2文字のため非空で、`QuotationTrimmer` が後段で空文字にする）。**第1回 #26 の決定（空文字だけを空と見なす）を上書きする。**

**Prerequisites**: #36（oracle テスト基盤を共用する）

**Steps**:

- [x] A. 本体を oracle にしたテストを**先に**書き、是正前に落ちることを実測する。入力は指示書 §3 の必須4種（`{}` の行／全値 `""` の行／`null` だけの行／マーカーカラムだけに値がある行）を**テーブルと `LIST_MAP` の両方**で。oracle は POI で組んだ `.xlsx` を本体 `BasicTestDataParser` で読んだ行の値
- [x] B. `YamlSection.isBlankRow`（`:202`-`:209`）を「値を1つも持たない行（空マッピング `{}`）」だけ真にする。Java null・`""` はどちらも非空。`YamlSection` の javadoc（`:169`-`:181`・`:193`-`:201`・`:219`-`:221`）と `YamlTableDataBuilder.java:169`-`:171` のコメントも合わせる
- [x] C. A のテストが是正後に通ることを実測する
- [x] D. 既存テストの期待値見直し — 着手前調査で挙動が変わる既存テストは**14件**（`YamlSectionTest`: `dropBlankRows_removesEmptyMappingAndAllBlankValueRows`（`:473`）・`dropBlankRows_keepsRowHavingOnlyWhitespaceValue`（`:522`）の2件／`YamlTableDataBuilderTest`: `buildTableDataList_blankValueRowLeadingExcluded`（`:1292`）・`buildTableDataList_blankValueRowMiddleExcluded`（`:1319`）・`buildTableDataList_partiallyBlankValueRowKept`（`:1345`）・`buildTableDataList_blankValueRowLeadingInExpectedTableExcluded`（`:1372`）・`buildTableDataList_blankValueRowMiddleInExpectedTableExcluded`（`:1399`）・`buildTableDataList_blankValueRowInExpectedCompleteTableExcluded`（`:1453`）・`buildListMapRows_blankValueRowLeadingExcluded`（`:1486`）・`buildListMapRows_blankValueRowMiddleExcluded`（`:1511`）・`buildListMapRows_partiallyBlankValueRowKept`（`:1534`）・`buildListMapRows_allBlankRowsReturnsEmptyList`（`:1687`）の10件／`YamlColumnOmissionTest`: `columnNamesDependOnRowOrderAfterBlankRowRemoval`（`:174`）ほか `omission.yaml` の `s4a`（`:55`）・`s4b`（`:73`）を使うテスト）。**変わらないと見込むもの**（`dropBlankRows_keepsRowHavingAnyNonBlankValue`・`dropBlankRows_keepsRowHavingOnlyMarkerColumnValue`・`dropBlankRows_removesNonMappingRows`・`dropBlankRows_keepsRowHavingOnlyNullValues`・`{}` だけを使うテスト群）も**実測で確かめて**件数つきで記録する。テスト名が挙動と食い違うものは名前も直す
- [x] E. **変異確認**: 追加/変更した各テストについて期待値を崩すと落ちることを確認する
- [x] F. `mvn -o clean test` 緑を確認
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-39.md)

**Completion criteria**:

- `isBlankRow` が空マッピング（値を1つも持たない行）だけを真とし、Java null・`""` をどちらも非空として扱う
- `YamlSection` の javadoc と `YamlTableDataBuilder` のコメントが是正後の挙動と食い違わない
- 本体 `BasicTestDataParser` を正解にしたテストがあり、`{}`／全値 `""`／`null` だけ／マーカーカラムだけ の4種をテーブルと `LIST_MAP` の両方で入力に含む
- 是正前に落ち是正後に通るテストが存在する
- 既存テストで期待値を変えたもの・変えなかったものが件数付きで記録されている
- 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#40: 2-5 — 2文字の `\` ＋ `r` を含む値が読める~~

**Purpose**: 解説書 `implementation/testdata_notation.rst:1445`（ピン `afa4f9e`。`04b9405` で改訂済み）は、バックスラッシュと `r` の2文字（`"\\r"`）を含む値は書けず YAML 形式ではエラーになると定める。Excel では `LineSeparatorInterpreter.java:31`・`:34` がこの2文字を必ず CR に変換するため、この2文字を含む値は NTF の仕様上存在しない。`setup/common.rst:77` は YAML 形式に `LineSeparatorInterpreter` を指定しないと定めるため「CR として解釈する」は採れない。

**Prerequisites**: none

**Steps**:

- [x] A. 検査を `YamlSection` の1箇所に置き、`interpret`（`:266`）と `YamlMessageBuilder.convertFwHeader`（`:290` のキー・`:296` の値の `objectToString`）の両方から通す。値（データ行・ディレクティブ・制御ヘッダ）に2文字の `\` ＋ `r` が含まれていればエラーにする。例外には値と、分かる範囲で出所（セクション・`id`／`path`）を含める
- [x] B. テストを足す — 2文字の `\` ＋ `r` がデータ行・ディレクティブ・制御ヘッダのそれぞれでエラーになること、`"\\n"`（2文字のまま残る）と実際の CR（`"\r"`）は通ること。例外の型と、メッセージに出所が入ることを assert する
- [x] C. 既存フィクスチャ・テストの是正 — 着手前調査で該当は**フィクスチャ1件・テスト1件**（`YamlTableDataBuilderTest/nativeTypes.yaml:16` の `LITERAL_CR_COL: "\\r"` と、それを期待値に書く `buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser`（`YamlTableDataBuilderTest.java:597`。`:609` で `is("\\r")` を assert））。解説書に合わせて「エラーになること」へ直す
- [x] D. **変異確認**: 追加/変更した各テストについて期待値を崩すと落ちることを確認する
- [x] E. `mvn -o clean test` 緑を確認
- [x] F. commit・push
- [x] G. self-check (OK/NG per completion criterion, record in checks/task-40.md)

**Completion criteria**:

- 2文字の `\` ＋ `r` を含む値がデータ行・ディレクティブ・制御ヘッダのいずれでもエラーになる
- 検査が `YamlSection` の1箇所に置かれ、`interpret` と `convertFwHeader` の両方から通っている
- `"\\n"` と実際の CR（`"\r"`）が通ることのテストがある
- 例外メッセージに値と出所が入ることを assert している
- 既存フィクスチャ・テスト1件が解説書に合わせて直されている
- 追加/変更した各テストについて、期待値を崩すと落ちることを確認した記録がある
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#41: 2-6 — `@Ignore` 1件の削除~~

**Purpose**: `YamlTableDataBuilderTest.java:976` `buildListMapRows_unknownCharacterTypeIsNotConverted` は「列挙外の文字種名は変換されず `${存在しない文字種,3}` のまま」を期待するが、解説書 `implementation/testdata_notation.rst:1315`（ピン `afa4f9e`）は「使用できる文字種は14種類に限定される」としか書かない。列挙外の名前の扱いは Excel でも YAML でも同じである（`${文字種,文字数}` を解釈するのは `BasicJapaneseCharacterInterpreter` 1クラスだけで、`unit-test.xml` の Excel 用 `interpreters`・`yamlInterpreters`・`yamlMessagingInterpreters` の3つの鎖すべてに同じクラスが入る）。ただし**挙動は名前の字種に依存し一律ではない**（コーディネータ実測 2026-08-29）: 同クラスのパターン `\$\{(\W+)\s*,\s*([0-9]+)\}`（`BasicJapaneseCharacterInterpreter.java:24`）は文字種名部が `\W+` のため、`${存在しない文字種,3}` はマッチして `CharacterGeneratorBase.generate` に届き `IllegalArgumentException`（`CharacterGeneratorBase.java:56`）になるが、`${abc,3}` のような ASCII 英数字の名前はマッチせず**そのまま残る**。この一貫しない挙動こそ解説書が定めていないことの裏づけである。なお解説書側は `nablarch-document@09779f6`「docs: 限定列挙に付けた「それ以外はエラー」を落とす」で当該文から `（それ以外を指定するとエラーになる）` を意図的に削除している。「間違えたときにどうなるか」は解説書に書かない基準（2026-08-25 ユーザー確定）どおりであり、このテストは解説書に無い「あるべき姿」を追っている。

**Prerequisites**: none

**Steps**:

- [x] A. テストメソッド `buildListMapRows_unknownCharacterTypeIsNotConverted`（`:962`-`:987` の javadoc・`@Ignore`・`@Test` を含む）を削除する
- [x] B. フィクスチャ `charTypeUnknownTest`（`YamlTableDataBuilderTest/nativeTypes.yaml:134`）が他から参照されていないことを実測し、参照が無ければ併せて削除する
- [x] C. **アノテーションとしての** `@Ignore` が src/test 全体で0件になったことを実測する。**他に `@Ignore` を足さない**
  （コーディネータ訂正 2026-08-29: 素の `grep -rn '@Ignore' src/test` は0件にならない。`YamlMessageBuilderTest.java:1125` に `{@code @Ignore}` が **javadoc の散文**として存在するため（#38 の変異実験の記録。アノテーションではない）。指示書 §4 の完了条件が求めているのは「読み飛ばされるテストが無いこと」＝アノテーションなので、検証は `grep -rnE '^\s*@Ignore' src/test` が0件、かつ `mvn -o clean test` が `Skipped: 0` で行う。javadoc の言及は妥当な記録なので削除しない）
- [x] D. `mvn -o clean test` 緑を確認（`Skipped: 0` になること）
- [x] E. commit・push
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-41.md)

**Completion criteria**:

- `buildListMapRows_unknownCharacterTypeIsNotConverted` が削除されている
- `charTypeUnknownTest` の参照有無が実測され、参照が無ければ削除されている
- アノテーションとしての `@Ignore` が src/test 全体で0件（`grep -rnE '^\s*@Ignore' src/test` が0件。javadoc 散文中の `{@code @Ignore}` は対象外）
- `mvn -o clean test` が BUILD SUCCESS で `Skipped: 0`

---

### ~~#42: 2-7 — スキーマ `description` の追随~~

**Purpose**: スキーマ `description` も SSoT の適用範囲である（2026-08-25 ユーザー確定）。指示書 2-7 の表が挙げる4箇所を、2-1〜2-4 の是正後の解説書の文言に合わせる。**`description` の文言は解説書に合わせる。実装の挙動を写さない。**

**Prerequisites**: #36、#37、#38、#39

**Steps**:

- [x] A. `:108`（`table_data.rows`）・`:136`（`list_map_data.rows`）の「全ての値が空文字 `""` の行は、行が無いものとして取り除かれる」を `notation.rst:1502` に合わせる（`{}` だけ。2-4）
- [x] B. `:216`（`message_data.fw_header` の `description`）・`:433`（`$defs.fw_header` の `description`）・`:434`（同 `$comment`）の「記述したキーはすべて FW 制御ヘッダとして NTF に渡される（値でのフィルタリングは行われない）」「任意のヘッダ名を許容する」を `notation.rst:1295` に合わせる（`reader.fwHeaderfields` の名前だけ。他はエラー。2-3）。**`additionalProperties: {"type": "string"}`（`:429`-`:432`）の構造は締めないこと** — 許可集合は `reader.fwHeaderfields` に依存するため静的スキーマでは表現できず（#38 が実装検査を選んだ前提）、締めると `YamlMessageBuilderTest/mixedFwHeaderKeysData.yaml` が成立しなくなり「誤記エントリが他エントリを巻き添えにしない」遅延実行の性質も消える
- [x] C. `:209`（`message_data.records`）・`:243`（`expected_request_message_data.records`）・`:275`（`group_message_data.records`）の `description` を `notation.rst:1153`・`:1299` に合わせる（レコードレイアウトは1つ。2-2）。`:182`（`file_data.records`）は対象外（ファイルは複数レコードレイアウトを持てる）
- [x] D. `:380`（`record_fragment.rows` の `description`）の「不足した末尾のフィールドは `""` として扱われる」に末尾の `null` の扱いを加える（`notation.rst:889`・`:1155`。2-1）
- [x] D2. **【コーディネータ追加 2026-08-29・指示書には無い】** `src/` 配下の解説書出典のうち、ピン `afa4f9e` に対して行番号が **+2 ずれている 13 箇所**を実測値へ訂正する。原因は `nablarch-document@6ba3c83`「docs(ntf): 交互記述は警告して変換、電文のレコードレイアウトは1つ」が `testdata_notation.rst` に 2 行挿入したことで、`:1299` 以降を指す出典がすべてずれた（`:1149`・`:1153`・`:1155` を指すものは挿入位置より前で影響なし）。**4 箇所だけ直すと同一ファイル内で `:1322` が「14 種類の list-table 行」と「組み合わせ記法の文」の両方を指す形になり、かえって読み手を誤らせるため、13 箇所まとめて直すこと。**
  対象と訂正後（コーディネータと #41 のレビューが実測済み。**着手時に自分で数え直して確かめること**）:
  `YamlDateNotationTest.java:30`・`:113`・`:133` と `YamlDateNotationTest/date.yaml:1` の `:1326`(-`:1331`) → `:1328`-`:1333`／`YamlTestDataParserTest.java:356` と `YamlTestDataParserTest/nativeTypes.yaml:33` の `:1337` → `:1339`／`YamlTableDataBuilderTest.java:56`・`:914` と `YamlTableDataBuilderTest/nativeTypes.yaml:88` の `:1313-:1320` → `:1315`-`:1322`／`YamlTableDataBuilderTest.java:967` と 同 `nativeTypes.yaml:133` の `:1322` → `:1324`／`YamlTableDataBuilderTest.java:1000` と 同 `nativeTypes.yaml:83` の `:1441-:1443` → `:1443`-`:1445`
  **理由**: 出典は読み手が同じ場所を開いて確かめられなければ意味を持たない（案件の原則）。#42 が解説書との追随を担うタスクであるため、その一部として扱う。**#44 の報告でスコープ拡張として明示的に報告すること。**
- [x] D3. **【同上】** 行番号出典は解説書の改版のたびに壊れる。13 箇所を直したうえで、**今後の出典は行番号ではなく節見出し＋引用文で書く**方針を `src/` 配下に適用できたか（できない箇所があればその理由）を記録する。先例は #38 の `YamlMessageBuilder` クラス javadoc と #39 の `YamlSection.dropBlankRows` javadoc。**ただし 13 箇所すべてを節見出し方式へ書き換えるのは #42 の範囲を超えるので、まず +2 の訂正を確実に行い、方式の切り替えは判断と根拠の記録に留めてよい。**
- [x] E. 是正の前後で挙動テストの結果が変わらないことを実測する（`description` は挙動を変えないため）
- [x] F. `mvn -o clean test` 緑を確認
- [x] G. commit・push
- [x] H. self-check (OK/NG per completion criterion, record in checks/task-42.md)

**Completion criteria**:

- 指示書 2-7 の表の4行（`:108`／`:136`、`:213`-`:215`／`:424`-`:430`、`:208`／`:241`／`:272`、`:377`）が解説書の該当行と食い違わない
- 文言が解説書に沿っており、実装の挙動の写しになっていない
- 是正の前後で挙動テストの結果が変わらないことが示されている
- `mvn -o clean test` が BUILD SUCCESS

---

### ~~#43: カバレッジ C0/C1 計測・converter 実測・報告書の作成~~

**Purpose**: 指示書 §4 の完了条件6・10 と §6 の報告6項目を満たす。

**Prerequisites**: #36〜#42

**Steps**:

- [x] A. JaCoCo で `src/main` の C0（命令）/C1（分岐）を計測し、第1回（`#33` 記録）からの差分を出す。是正で下がった箇所があれば挙げる
- [x] B. `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean install` で本モジュールを `.m2` に入れる
- [x] C. `nablarch-testing-converter@d611bec`（**変更しない**）で `mvn -o clean test` を実行し、着手前（`Tests run: 656, Failures: 0, Errors: 0, Skipped: 0`）からの差分を全件挙げる。落ちたテスト名と理由を記録する
- [x] D. 報告書 `.rn/ntf-yaml/report-step4-2.md` を指示書 §6 の6項目の順で書く（1. 着手前調査の結果／2. 第2節7件の是正結果／3. 本体を oracle にしたテストの一覧／4. 変異確認の結果／5. 既存テストの期待値を変えた箇所の全件／6. カバレッジ C0/C1 と converter で落ちたテストの全件）
- [x] E. `git status --short` が空（`tmp/` はテストスイート自身が作る空ディレクトリなので残ってよい）。commit・push
- [x] F. self-check (OK/NG per completion criterion, record in checks/task-43.md)

**Completion criteria**:

- C0/C1 が計測され、第1回からの差分と下がった箇所が挙がっている
- converter を変更せずに実行し、着手前 656件からの差分が全件挙がっている
- 報告書が指示書 §6 の6項目をこの順で含む
- `git status --short` が空、push 済み

---

### ~~#44: Evaluation sign-off（Step 4 第2回）~~

**Purpose**: 指示書「4. 完了条件」10項目を実測で通し、ユーザーの評価ゲートを取る。

**Prerequisites**: #43

**Steps**:

- [x] A. 指示書 §4 の完了条件10項目を1つずつ実測し、結果をユーザーに提示する
- [x] B. `/rn:ty`（承認）または `/rn:gm`（差し戻し）の判定を受ける

**Completion criteria**:

- 完了条件10項目の実測結果が提示されている
- ユーザーの判定が出ている

**ユーザー判定（2026-08-29・`/rn:ty` で承認）**: ディレクターの独立検証に合格したため Step 4 第2回を締める。
検証の内訳は scratchpad の clone で `mvn -o clean test` 318件緑・`@Ignore` 0件、`src/main` の差分5ファイルの全量読み、
ミューテーション7件（2-1 `trimTailCopy` 無効／2-3 未知キー素通し／2-3 設定値無視／2-4 旧判定／2-5 検査無効／
2-5 `fw_header` 経路だけ未検査／2-5 判定を過剰に）がすべて検知されること、converter `d611bec` で同じ4件
（`656 / Failures: 3, Errors: 1`）が落ちることの再現。完了条件 #2 は満たすと判定（特定結果は `7480453` で最初の実装
`ce81530` に先行して記録されており、タスクごとの停止は #38 以降ユーザーが免除した）。#42 の出典訂正18箇所（§8.6）は受け入れ。

**報告書 §8 の未決5件に対するユーザーの判定（2026-08-29）**:

- **§8.1 仕様差ではない。** 解説書 `testdata_notation.rst:818` が「後続の行がこのキーの一部を持たない場合、そのカラムは null を明示的に指定したのと同じ扱いになる」と既に定めている。T5/L5 は入力が非等価（Excel の空セル＝`""`、YAML のキー省略＝null）なだけ。`:1502` の「他のカラムがすべて空文字のエントリとして読み込まれる」はこれと矛盾していたため、`nablarch-document@a6da1f6` で「他のカラムの値は通常どおり読み込まれる（Excel 形式の空セルは `""`、YAML 形式でキーを省略した場合は前述のとおり null）」に改訂済み
- **§8.2 converter 側で直す。** ディレクター作成済みの converter 第2回の指示書で扱う。yaml 側では何もしない
- **§8.3 起票不要。** `:889` を `a6da1f6` で「後ろに空文字でも null でもないフィールドがあれば null のまま保持される（末尾側に並んだ `""` と `null` は、まとめて `""` になる）」に改訂済み
- **§8.4 追随する。** #45 の B で扱う
- **§8.5 ソースコメントから解説書への参照をすべて取り除く**（#45 の A）。解説書を指す行番号も節見出しも逐語引用も
  ソースには書かない。根拠の追跡は `.rn/` の報告書・台帳で行う。機械検証（§8.5 の案1）も作らない。
  リリース済みの `nablarch-testing`・`nablarch-testing-rest`・`nablarch-testing-junit5` の `src/` には解説書への参照が
  1件も無い（ディレクター実測）

**解説書の新しいピン**: `nablarch-document@a6da1f6`。`ja/` は `afa4f9e` から
`implementation/testdata_notation.rst:889`・`:1502` と `tools/testdata_converter.rst:63` の3行だけが変わり、
行番号は変わっていない（`git -C ../nablarch-document diff --stat afa4f9e a6da1f6 -- ja/` で実測: 2ファイル 3挿入 3削除）。

---

### #45: 解説書への参照を `src/` から取り除き、2-5 の規則をスキーマへ追随させる

**Purpose**: 報告書 §8.4 の追随と §8.5 のユーザー判断（2026-08-29）を実施する。解説書の出典は `src/` に置かず
`.rn/` の報告書・台帳で追跡する方式へ移す。あわせて §8.1 の判定を T5/L5 の Javadoc に反映し、
付録 A の失効記録に注記を入れる。

**出典**: 指示書 `nablarch-document@origin/ntf-yaml-support` の
`.rn/20260724-ntf-yaml-support/ntf-step4-06-nablarch-testing-yaml-2.md` §8（§8.1 承認文面の全文・§8.2 確認2件への回答と訂正3件）。
報告書 `.rn/ntf-yaml/report-step4-2.md` の §8.1・§8.4・§8.5・付録 A。

**Prerequisites**: #44

**Steps**:

- [ ] O. Rules の「参照点（ピン）」の解説書だけを `afa4f9e` → `a6da1f6` に取り直す（本モジュール・`nablarch-testing`・
      converter のピンは変えない）。理由: `afa4f9e` の `testdata_notation.rst:1502` は「他のカラムがすべて空文字の
      エントリとして読み込まれる」のままで、C の前提と矛盾する。`a6da1f6` との差は `testdata_notation.rst:889`・`:1502` と
      `testdata_converter.rst:63` の3行で行番号は変わらない（指示書 §8.2-2）
- [ ] A. `src/main`・`src/test`（フィクスチャの YAML とそのコメントを含む）から解説書への参照を取り除く。
      対象は `.rst` のパス（行番号の有無を問わない）・`nablarch-document`・「解説書」「出典」「根拠:」として解説書を
      指す記述・解説書の節見出し・逐語引用。Javadoc とテストの説明は**何を確かめるかを自分の言葉で書く**
      （既存の Given/When/Then と本体クラス名への言及は残してよい）。
      **他リポジトリのソースを指す箇所は行番号とパスを落とし、クラス名だけ残す**（指示書 §8.2 の訂正）。
      対象はフルパスの7箇所（`YamlSection.java:384`・`:407`／`YamlMessageBuilderTest.java:66`・`:124`・`:1078`／
      `YamlSectionTest.java:262`／`YamlTableDataBuilderTest.java:748`）に加え、パス無しで他リポジトリの行番号を指す3箇所
      （`YamlTestDataParserTest.java:1857` の `SendSyncSupport.java:347`／`YamlTrailingNullOracleTest.java:317` の
      `MockMessages.java:64`／`YamlMessageBuilderTest.java:1155` の `MessageParser.java:108`）。
      本モジュール自身を指す `YamlLoader.java:151`（`YamlMessageBuilderTest.java:1385`）は対象外で残す。
      着手前に取り除く行の全件（`file:line`）を機械抽出して件数を報告する
- [ ] B. 2-5 の規則（バックスラッシュと `r` の2文字を含む値はエラー）を、スキーマ `description` の5箇所に1文ずつ追記する。
      `table_data.rows`（`:108`）／`list_map_data.rows`（`:136`）／`message_data.fw_header`（`:216`）／
      `record_fragment.rows`（`:380`）／`$defs.fw_header`（`:433`）。行番号は `ef1fc63` のスキーマ（指示書 §8.2-1）。
      文言は `record-separator` の既存文（#42。`:293`）と揃える。**実装の挙動を写さない**
- [ ] C. `YamlBlankEntryOracleTest` の T5/L5 の Javadoc から「仕様差」の枠組みを外し、「キーを省略したカラムは null を
      明示したのと同じ。Excel の空セルは `""` なので入力が非等価」と自分の言葉で書く（A のとおり解説書は引かない）。
      あわせて、等価な入力（Excel 側は他のセルに `null` と記述、YAML 側はキー省略）で本体と YAML が一致することを
      oracle で示すケースを T6/L6 として足す。足したテストは期待値を崩すと落ちることを1度確認する
- [ ] D. `.rn/ntf-yaml/checks/task-31.md` の3箇所（`:8`・`:9`・`:23`）に「#41 で削除」の注記を入れる
- [ ] E. 報告書 `.rn/ntf-yaml/report-step4-2.md` に §9 として追記する（A の件数と抽出方法、B〜D の変更箇所の
      `file:line`、T6/L6 の本体の値と YAML の値、崩す確認の結果）
- [ ] F. `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test` 緑。`git status --short` 空。commit・push
- [ ] G. self-check (OK/NG per completion criterion, record in checks/task-45.md)

**Completion criteria**:

- `mvn -o clean test` 緑（318件＋T6/L6）
- `@Ignore` が0件
- `git grep -nE '\.rst|nablarch-document|解説書|出典' -- src/` が0件（指示書の式）。
  併せて「根拠:」も0件であることを報告に書く（A の除去対象に含めるため）
- `git status --short` 空、push 済み
- 報告書に §9 が追記されている

**やらないこと**: 解説書・`nablarch-testing`・`nablarch-testing-converter` を直さない。テストの動作・期待値を変えない
（A で変えるのはコメントとフィクスチャのコメントだけ。ただし C の T6/L6 は足す）。変えたら報告に挙げる。
解説書に無い書き方を追いかけない。§8.5 の機械検証（案1）は作らない。

---

# State

(written by /rn:dn, read and reset to this placeholder by /rn:up. `Status` is `paused` while a
session is suspended — the signal /rn:up and /rn:dn search for — and resets to `not suspended` here,
so only a genuinely suspended session reads `paused`.)

- **Status**: not suspended
- **Date**: -
- **Last completed**: -
- **Next**: -
- **Notes**: -
