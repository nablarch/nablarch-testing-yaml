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
