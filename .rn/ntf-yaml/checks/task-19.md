# task-19 Completion Check

## step A・B（カバレッジ実測）— 2026-08-24

**レビュー不要のステップ**（Rules の基準・2026-08-24 ユーザー訂正）。代わりに実行コマンドと生の出力を記録する。

## 着手前後の状態

```
$ git log --oneline -1
8094013 fix: complete task #22 — レビュー ラウンド2 の指摘を反映し description とテストを仕上げる
$ git rev-parse --abbrev-ref HEAD
feature/ntf-yaml
$ git status --porcelain
（出力なし）
```

着手前・着手後とも同じ（実測のみで `src/main` も `pom.xml` も変更していない）。

## ベースライン差分の採り直し（`指示/00-共通ルール.md:56` が求めるもの）

base は `git merge-base HEAD main` = `c0e6d20`（Initial commit）。

```
$ git diff --stat c0e6d20...HEAD -- src/main
 .../test/core/reader/YamlTestDataParser.java       | 194 +++++++++
 .../test/core/reader/yaml/InterpreterResolver.java |  57 +++
 .../test/core/reader/yaml/MessageContent.java      |  41 ++
 .../test/core/reader/yaml/YamlFileBuilder.java     | 260 ++++++++++++
 .../nablarch/test/core/reader/yaml/YamlLoader.java | 157 ++++++++
 .../test/core/reader/yaml/YamlMessageBuilder.java  | 240 ++++++++++++
 .../reader/yaml/YamlSchemaValidationException.java |  39 ++
 .../test/core/reader/yaml/YamlSection.java         | 299 ++++++++++++++
 .../core/reader/yaml/YamlTableDataBuilder.java     | 227 +++++++++++
 .../nablarch/test/ntf-testdata-yaml-schema.json    | 434 +++++++++++++++++++++
 10 files changed, 1948 insertions(+)
```

着手前の記録は「10ファイル `+1843`」だったが、`#18`〜`#22` の作業を経て **`+1948`** になっている。java は9ファイル・全部新規のため、**対象は結果的にモジュール全体（9クラス）**である点は変わらない。

## 実行コマンドと生の出力

`jacoco.exec` はリポジトリルートに以前の実行分が残っており（209119 バイト、`.gitignore` 対象）、JaCoCo は既定で追記するため**削除してから採り直した**。

```bash
$ rm -f jacoco.exec
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean jacoco:instrument test jacoco:restore-instrumented-classes
[INFO] Tests run: 210, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

$ ls -l ./jacoco.exec
-rw-r--r-- 1 tie303177 tie303177 692  8月 24 12:41 ./jacoco.exec

$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o jacoco:report -Djacoco.dataFile=$(pwd)/jacoco.exec
[INFO] BUILD SUCCESS

$ ls -l target/site/jacoco/jacoco.csv target/site/jacoco/index.html
-rw-r--r-- 1 tie303177 tie303177 3865  8月 24 12:41 target/site/jacoco/index.html
-rw-r--r-- 1 tie303177 tie303177 1029  8月 24 12:41 target/site/jacoco/jacoco.csv
```

`pom.xml` / `argLine` は変更していない。

### jacoco.csv 全文

```
GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
nablarch-testing-yaml,nablarch.test.core.reader.yaml,YamlTableDataBuilder,0,322,0,30,0,67,0,21,0,6
nablarch-testing-yaml,nablarch.test.core.reader.yaml,YamlSection,0,201,0,44,0,50,0,36,0,12
nablarch-testing-yaml,nablarch.test.core.reader.yaml,YamlMessageBuilder,0,276,0,30,0,58,0,24,0,9
nablarch-testing-yaml,nablarch.test.core.reader.yaml,YamlSchemaValidationException,0,28,0,0,0,7,0,3,0,3
nablarch-testing-yaml,nablarch.test.core.reader.yaml,InterpreterResolver,0,11,0,0,0,2,0,4,0,4
nablarch-testing-yaml,nablarch.test.core.reader.yaml,YamlFileBuilder,1,366,1,41,1,83,1,28,0,8
nablarch-testing-yaml,nablarch.test.core.reader.yaml,YamlLoader,12,152,1,11,3,43,1,10,0,5
nablarch-testing-yaml,nablarch.test.core.reader.yaml,MessageContent,0,15,0,0,0,6,0,3,0,3
nablarch-testing-yaml,nablarch.test.core.reader,YamlTestDataParser,0,227,0,2,0,55,0,18,0,17
```

### 未達クラス一覧（`指示/00-共通ルール.md:104-106` の awk そのまま）

```
$ awk -F, 'NR==1{next} ($4+0)>0 || ($6+0)>0 {printf "%s.%s  INSTRUCTION_MISSED=%s BRANCH_MISSED=%s\n",$2,$3,$4,$6}' \
    target/site/jacoco/jacoco.csv | sort
nablarch.test.core.reader.yaml.YamlFileBuilder  INSTRUCTION_MISSED=1 BRANCH_MISSED=1
nablarch.test.core.reader.yaml.YamlLoader  INSTRUCTION_MISSED=12 BRANCH_MISSED=1
```

**9クラス中7クラスは C0/C1 とも達成済み**（`INSTRUCTION_MISSED=0` かつ `BRANCH_MISSED=0`）。

### 未達の行（jacoco.xml から抽出）

```
=== nablarch/test/core/reader/yaml/YamlFileBuilder.java ===
  line 227: MISSED_INSTR=0 COVERED_INSTR=3 MISSED_BRANCH=1 COVERED_BRANCH=1
  line 228: MISSED_INSTR=1 COVERED_INSTR=0 MISSED_BRANCH=0 COVERED_BRANCH=0
=== nablarch/test/core/reader/yaml/YamlLoader.java ===
  line 60: MISSED_INSTR=0 COVERED_INSTR=2 MISSED_BRANCH=1 COVERED_BRANCH=1
  line 61: MISSED_INSTR=5 COVERED_INSTR=0 MISSED_BRANCH=0 COVERED_BRANCH=0
  line 65: MISSED_INSTR=1 COVERED_INSTR=0 MISSED_BRANCH=0 COVERED_BRANCH=0
  line 66: MISSED_INSTR=6 COVERED_INSTR=0 MISSED_BRANCH=0 COVERED_BRANCH=0
```

### 該当箇所のソース

`YamlFileBuilder.java:224-229`（値行ループの防御的ガード）:

```java
224            for (Object rowObj : getList(record, FIELD_ROWS)) {
225                // SnakeYAML Engine では rows: の各要素は通常 List だが、外部入力（YAML ファイル）にマッピングや null が
226                // 混入した場合への防御的ガード。Java 言語仕様上この分岐は通常到達不能だが、堅牢性のために残す。
227                if (!(rowObj instanceof List)) {
228                    continue;
229                }
```

`YamlLoader.java:57-68`（static イニシャライザのスキーマ読み込み）:

```java
57    static {
58        try (InputStream schemaStream = YamlLoader.class.getClassLoader()
59                .getResourceAsStream(SCHEMA_RESOURCE_PATH)) {
60            if (schemaStream == null) {
61                throw new IllegalStateException("Schema file not found on classpath: " + SCHEMA_RESOURCE_PATH);
62            }
63            JSON_SCHEMA = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
64                    .getSchema(schemaStream);
65        } catch (IOException e) {
66            throw new IllegalStateException("Failed to load JSON schema from: " + SCHEMA_RESOURCE_PATH, e);
67        }
68    }
```

## step B/C ゲート

`指示/yaml-あるべき姿とカバレッジ.md:114`「**まず実測して、未達クラスの一覧を報告すること。** テストを足すのはその後。何件足すことになるかが分かってからでないとユーザーが判断できない。」に従い、**ここで停止してユーザーの指示を待つ**。step C（未達分岐を埋めるテスト追加）には勝手に進まない。

---

## 裁定（2026-08-24 ユーザー判断）— 未達2箇所は「到達不能」として承認

**判断**: 未達2箇所（`YamlFileBuilder:227-228` / `YamlLoader:60-61` `:65-66`）は**到達不能として承認する**。**テストを足さない**。**step C は実施しない**。リフレクション等で通すテストは書かない（「壊す変更で落ちること」を満たさない — 壊れ方そのものが現実に起こらないため）。

これにより完了条件「`INSTRUCTION_MISSED` が 0、`BRANCH_MISSED` が 0（**到達不能としてユーザーが承認した箇所を除く**）」を満たす。

### 根拠1: `YamlFileBuilder.java:227-228`（`!(rowObj instanceof List)` の true 側）

`record_fragment.rows` の各要素が非 List の YAML は、**この行に到達する前にスキーマ検証で落ちる**。

- `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json:375-386` — `$defs.record_fragment.properties.rows` の `items` が `"type": "array"`（`:378-379`）。よって `rows` の要素はスキーマ上 配列に限定される（実物で確認）
- `YamlLoader.java:97-127` — `load()` はキャッシュ未ヒットのとき必ず `JSON_SCHEMA.validate(jsonNode)` を実行し（`:121`）、違反があれば `YamlSchemaValidationException` を投げる（`:122-124`）。検証を迂回する分岐は無い（実物で確認）
- `YamlFileBuilder` が扱う Map の出所は `YamlTestDataParser.java:112,120,133,140,148,156,163,173` の `YamlLoader.load(...)` のみ（`grep -rn "YamlLoader\." src/main/java` で確認。他の生成経路は無い）

つまり「非 List 要素を含む YAML」は `:227` へ到達しない。到達させるにはロード経路を迂回して `YamlFileBuilder` を直接叩く必要があり、それは実運用で起こる壊れ方ではない。

### 根拠2: `YamlLoader.java:57-68`（static イニシャライザの2分岐）

- `schemaStream == null`（`:60-61`）— スキーマ `ntf-testdata-yaml-schema.json` は**自モジュールの `src/main/resources/nablarch/test/` に同梱**されており（`ls -l` で確認）、`SCHEMA_RESOURCE_PATH`（`:45`）と一致する。クラスパスから消えるのはクラスパスを細工した場合だけ
- `IOException`（`:65-66`）— 同梱リソースの `InputStream#close` / 読み取りで IOException を起こすにも、同様にクラスパス細工が要る

いずれも「実装を壊す変更」ではなく「実行環境を壊す細工」でしか再現できないため、到達不能として扱う。

### 未達値（採取済み・変化なし）

```
nablarch.test.core.reader.yaml.YamlFileBuilder  INSTRUCTION_MISSED=1 BRANCH_MISSED=1
nablarch.test.core.reader.yaml.YamlLoader  INSTRUCTION_MISSED=12 BRANCH_MISSED=1
```

### 完了条件の self-check

| 完了条件 | 判定 | 根拠 |
| --- | --- | --- |
| 差分対象クラスの `INSTRUCTION_MISSED` が 0、`BRANCH_MISSED` が 0（到達不能としてユーザーが承認した箇所を除く） | OK | 9クラス中7クラスが 0/0。残る2クラスの未達は上の裁定で到達不能として承認済み |
| 追加した各テストについて「壊す変更で落ちた」確認コマンドと結果が記録されている | N/A | テストを追加していない（step C 不実施） |
| 追加テストの javadoc に「何を担保するか」が1文で書かれている | N/A | 同上 |
| `pom.xml` / `argLine` が変更されていない | OK | `git diff --stat` に `pom.xml` なし |
| `mvn -o clean test` が BUILD SUCCESS | OK | 下記 |

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
[INFO] Tests run: 210, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  10.981 s
[INFO] Finished at: 2026-08-24T13:07:47+09:00
```

**レビューは回さない**（実装変更が無いため。step C 不実施によりレビュー必要ステップが消えた）。
