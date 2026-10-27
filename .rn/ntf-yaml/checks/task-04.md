# Task #4 Self-check: converter 専用 Adapter 群の削除

実施日: 2026-06-22

## Completion Criteria

### 削除対象ファイルがリポジトリから消えている

**src/main（4件）:**
- OK: `src/main/java/nablarch/test/core/file/TestCoreFileAdapter.java` — 削除済み
- OK: `src/main/java/nablarch/test/core/reader/YamlTestCoreAdapter.java` — 削除済み
- OK: `src/main/java/nablarch/test/core/reader/TestCoreReaderAdapter.java` — 削除済み
- OK: `src/main/java/nablarch/test/core/reader/StubDbInfo.java` — 削除済み

**src/test（3件＋データ4件）:**
- OK: `src/test/java/nablarch/test/core/file/TestCoreFileAdapterTest.java` — 削除済み
- OK: `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest.java` — 削除済み
- OK: `src/test/java/nablarch/test/core/reader/TestCoreReaderAdapterTest.java` — 削除済み
- OK: `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest/files.yaml` — 削除済み
- OK: `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest/messages.yaml` — 削除済み
- OK: `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest/sendSync.yaml` — 削除済み
- OK: `src/test/java/nablarch/test/core/reader/YamlTestCoreAdapterTest/tables.yaml` — 削除済み

### `mvn clean test` 全テスト PASS

- OK: Tests run: 140, Failures: 0, Errors: 0, Skipped: 0
- OK: BUILD SUCCESS (Total time: 11.259 s)

### `mvn install` BUILD SUCCESS

- OK: `mvn clean install -Dmaven.javadoc.skip=true` BUILD SUCCESS
  - `mvn install` 単体では JaCoCo の二重インスツルメント（already instrumented）エラーが発生
  - `mvn clean install` で解消、BUILD SUCCESS (Total time: 9.549 s)

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| 削除対象ファイルの正確性 | OK | git diff --cached --name-only で11件のみ staging、過不足なし |
| 残留コンパイル参照なし | OK | 残留 .java ファイルに import 文なし |
| Javadoc {link} dangling 参照 | NG→OK | 4件検出(InterpreterResolver/YamlLoader/YamlMessageBuilder/YamlSection)→修正済み |
| mvn clean test 全 PASS | OK | 140 tests run, 0 failures |
| mvn install BUILD SUCCESS | OK | mvn clean install -Dmaven.javadoc.skip=true BUILD SUCCESS |

## Overall Verdict

- Self-check: OK
- QA: OK（Javadoc dangling 4件 → 修正後 OK）
- Language expert: N/A（削除のみ）
- Software-engineering expert: N/A（削除のみ）
- Ready for user review: Yes

## 備考
- `mvn install`（clean なし）では JaCoCo が `target/classes` 内の計測済みクラスを再計測しようとして失敗する。
  これは削除作業と無関係な既存の現象（`mvn clean` なしで連続実行した場合の副作用）。
  `mvn clean install` では問題なく成功する。
