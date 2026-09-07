# task-20 Completion Check

**レビュー不要のステップ**（Rules の基準: ビルド。2026-08-24 ユーザー指示）。代わりに実行コマンドと生の出力を記録する。

## step A — 「旧 State の禁止事項」の解消（2026-08-24 ユーザー判断）

旧 State（`#20` 着手前の記述）にあった禁止事項「yaml で `mvn install` しない（converter が `pom.xml:42-44` で `1.0.0-SNAPSHOT` に依存し install で壊れる）」は、**出典が確認できず解消**とする。ユーザー判断 2026-08-24。

根拠（ユーザー提示・本セッションでは実物再確認していないため「ユーザー提示」として記録）:

- この文言が初めて現れるコミットは `1bae0de`（2026-08-24）で、その親コミットの State は `Notes: —` のみ
- 全リビジョンを走査しても、禁止を記した State も Rules も存在しない
- 禁止の理由「converter が install で壊れる」はコンパイル面で成立しない。`.m2` の旧 jar（8/18 09:30 時点）と作業ツリー `target/classes` を全クラス `javap` で比較した公開 API 差分は追加1件（`YamlSection.dropBlankRows`）のみで、クラス削除・署名変更なし、`pom.xml` は完全一致

禁止の存在を前提にした記述は残さない。install 後に converter が赤くなること自体は想定内（別途こちらから converter 側へ再開指示を出す）。

## step A2 — `mvn -o clean test`

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean test
[INFO] Tests run: 226, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  30.366 s
[INFO] Finished at: 2026-08-24T16:47:07+09:00
```

レビュー役（ユーザー）が独立実行し、2026-08-24 16:39 に同じ `Tests run: 226, Failures: 0, Errors: 0, Skipped: 0` を確認済み。

## step B — `mvn install`

指示書 手順5 の記載コマンド `mvn -o install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true`（`clean` なし）をそのまま実行したところ、以下で失敗した。

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true
[INFO] --- jacoco:0.8.8:instrument (default-instrument) @ nablarch-testing-yaml ---
[INFO] BUILD FAILURE
[ERROR] Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.8:instrument (default-instrument) on project nablarch-testing-yaml: Unable to instrument file.: Error while instrumenting /home/tie303177/work/nablarch/nablarch-testing-yaml/target/classes/nablarch/test/core/reader/yaml/YamlFileBuilder.class with JaCoCo 0.8.8.202204050719/5dcf34a. Cannot process instrumented class nablarch/test/core/reader/yaml/YamlFileBuilder. Please supply original non-instrumented classes. -> [Help 1]
```

原因: Rules（steering.md）「`jacoco:restore-instrumented-classes` は prepare-package で走るため、`clean` なしの `mvn test` / `mvn install` は instrument 済みクラスが `target/classes` に残り『Cannot process instrumented class』で失敗する」のとおり。直前の step A2 が `test` フェーズ止まりで `prepare-package` に到達しておらず、`target/classes` に instrument 済みクラスが残っていた。

Rules に従い `clean` を付けて再実行した。

```
$ JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o clean install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true
[INFO] --- surefire:2.22.2:test (default-test) @ nablarch-testing-yaml ---
[INFO] Tests are skipped.
[INFO] --- jacoco:0.8.8:restore-instrumented-classes (default-restore-instrumented-classes) @ nablarch-testing-yaml ---
[INFO] --- jar:2.1:jar (default-jar) @ nablarch-testing-yaml ---
[INFO] Building jar: /home/tie303177/work/nablarch/nablarch-testing-yaml/target/nablarch-testing-yaml-1.0.0-SNAPSHOT.jar
[INFO] --- source:3.0.1:jar (default) @ nablarch-testing-yaml ---
[INFO] Building jar: /home/tie303177/work/nablarch/nablarch-testing-yaml/target/nablarch-testing-yaml-1.0.0-SNAPSHOT-sources.jar
[INFO] --- javadoc:2.10.4:jar (default) @ nablarch-testing-yaml ---
[INFO] Skipping javadoc generation
[INFO] --- install:3.1.2:install (default-install) @ nablarch-testing-yaml ---
[INFO] Installing /home/tie303177/work/nablarch/nablarch-testing-yaml/pom.xml to /home/tie303177/.m2/repository/com/nablarch/framework/nablarch-testing-yaml/1.0.0-SNAPSHOT/nablarch-testing-yaml-1.0.0-SNAPSHOT.pom
[INFO] Installing /home/tie303177/work/nablarch/nablarch-testing-yaml/target/nablarch-testing-yaml-1.0.0-SNAPSHOT.jar to /home/tie303177/.m2/repository/com/nablarch/framework/nablarch-testing-yaml/1.0.0-SNAPSHOT/nablarch-testing-yaml-1.0.0-SNAPSHOT.jar
[INFO] Installing /home/tie303177/work/nablarch/nablarch-testing-yaml/target/nablarch-testing-yaml-1.0.0-SNAPSHOT-sources.jar to /home/tie303177/.m2/repository/com/nablarch/framework/nablarch-testing-yaml/1.0.0-SNAPSHOT/nablarch-testing-yaml-1.0.0-SNAPSHOT-sources.jar
[INFO] BUILD SUCCESS
[INFO] Total time:  11.125 s
[INFO] Finished at: 2026-08-24T16:48:05+09:00
```

`pom.xml` / 実装は変更していない（`-DskipTests` 実行かつ本ステップで edit なし）。

## step C — jar タイムスタンプの確認

```
$ ls -l ~/.m2/repository/com/nablarch/framework/nablarch-testing-yaml/1.0.0-SNAPSHOT/nablarch-testing-yaml-1.0.0-SNAPSHOT.jar
# 着手前: -rw-r--r-- 1 tie303177 tie303177 32537  8月 18 09:30 ...
# install後: -rw-r--r-- 1 tie303177 tie303177 34090  8月 24 16:48 ...
```

タイムスタンプが `2026-08-18 09:30` → `2026-08-24 16:48` へ更新された。

## 完了条件の self-check

| 完了条件 | 判定 | 根拠 |
| --- | --- | --- |
| `mvn -o clean test` が BUILD SUCCESS の状態で install されている | OK | step A2（`Tests run: 226, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS）→ step B（`clean install` BUILD SUCCESS） |
| jar のタイムスタンプが更新されている | OK | 8/18 09:30 → 8/24 16:48 |
