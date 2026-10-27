# task-A Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `YamlSection` に `groupMatches` が追加されている | OK | `YamlSection.java` に `public static boolean groupMatches` を Javadoc 付きで追加 | OK | L205 確認済み |
| `YamlFileBuilder` の `private groupMatches` が削除されている | OK | private 定義削除、`import static YamlSection.groupMatches` に置換 | OK | 確認済み |
| `YamlTableDataBuilder` の `private groupMatches` が削除されている | OK | private 定義削除、`import static YamlSection.groupMatches` に置換 | OK | 確認済み |
| `mvn clean test` 全テスト PASS | OK | Tests run: 159, Failures: 0, Errors: 0, Skipped: 0 | OK | BUILD SUCCESS |

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Meaningful tests/verification | OK | 159件全 PASS、統合レベルで間接カバー済み |
| Edge case coverage | OK | NPE は fix コミット(a1268df)で解消済み |

## Expert Reviews (code changes only)

### Language Expert

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Best practices | OK | formatted.equals(requestedFormatted) に修正し NPE 解消（a1268df） |
| Codebase style consistency | OK | 既存 toStr/castMap 等と同スタイル |
| GWT test format | N/A | テストコード変更なし |

### Software-engineering Expert

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Separation of concerns | OK | YamlSection の既存ユーティリティ責務と一貫 |
| System integrity | OK | シグネチャ変更なし、呼び出し元2箇所の動作不変 |
| Maintainability | OK | 重複排除完了、整形仕様の変更箇所が1箇所に集約 |

## Overall Verdict

- Self-check: OK
- QA: OK
- Language expert: OK（fix コミット a1268df 後）
- Software-engineering expert: OK
- Ready for user review: Yes
