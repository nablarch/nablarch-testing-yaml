# method-rename Completion Check

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| `grep -rn "rs[0-9][0-9]_" src --include=*.java` が 0 件 | OK | コマンド実行結果: 出力なし（0件） | OK | QA が実機再確認し 0件を確認 |
| `mvn test` が 162件 GREEN | OK | `Tests run: 162, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS` | OK | surefire XML でメソッド名レベルまで確認、新名称が全件出現 |
| `feature/ntf-yaml` に push 済み | OK | git push origin feature/ntf-yaml を実行済み（STEP 4 完了後） | OK | `Your branch is up to date with 'origin/feature/ntf-yaml'` を確認 |

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Verification approach meaningful to the objective (checks the right thing, not just "passed") | OK | grep による残存プレフィックス確認と surefire 件数確認はいずれも目的（プレフィックス除去完了・既存テスト無破壊）に直結。surefire XML でリネーム後メソッド名の出現を確認し、ゴム判ではないことを確認。 |

## Expert Reviews (axes the task needs)

### Craft Expert (coding)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Medium-specific best practice | OK | 10件すべて camelCase・振る舞い記述型。rsXX_ という仕様ID埋め込みプレフィックスの除去は「名前は実装の何をするかを表す」原則に沿う改善 |
| Consistency with existing style | OK | ファイル内の多数派スタイル（camelCase, 例: getSetupTableDataWithGroupId）と完全一致。テスト本体・アサーション・Javadoc は無変更 |

### Verification Expert (test)

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Artifact actually checked (tests run / claims verified / flow traced) | OK | git show ae843e2 で diff 直接確認。grep 独自実行で 0件確認。`feature/ntf-yaml` は origin と同期済み |
| Coverage (edge cases / claims / steps) | OK | 全10件（rs01×1, rs03×1, rs04×1, rs05×2, rs06×2, rs07×1, rs08×2）が diff に存在確認。テスト件数減少なし（リネームのみ） |

## Overall Verdict

- Self-check: OK
- QA: OK
- Design expert: N/A
- Craft expert: OK
- Verification expert: OK
- Ready to check off: Yes
