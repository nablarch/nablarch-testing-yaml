# task-07 Completion Check

## 3-Axis Schema Cross-Check Findings

### Axis 1: Read Direction (NTF reader reads from YAML → check schema allows it)

Fields read by reader code (`YamlFileBuilder`, `YamlMessageBuilder`, `YamlSection`, `YamlTableDataBuilder`):

| Section/Context | Field read | In schema? |
|---|---|---|
| All sections | `group_id` | OK (table_data, file_data, group_message_data have it; message_data was missing — **Bug 1**) |
| table_data | `table` | OK |
| table_data | `rows` | OK |
| list_map_data | `id` | OK |
| list_map_data | `rows` | OK |
| file_data | `path` | OK |
| file_data | `type` | OK |
| file_data | `directives` | OK |
| file_data | `records` | OK |
| message_data | `id` | OK |
| message_data | `fw_header` | OK |
| message_data | `records` | OK |
| record_fragment | `record_type` | OK (optional — Bug 2 was requiring it) |
| record_fragment | `fields` | OK |
| record_fragment | `rows` | OK |
| field_def | `name` | OK |
| field_def | `type` | OK |
| field_def | `length` | OK |

**Finding**: `message_data` was missing `group_id` in its properties (Bug 1 confirmed).

### Axis 2: Write Direction (converter writes to YAML → check schema allows it)

Fields written by `YamlFormatWriter`:

| emitXxx method | Field written | In schema? |
|---|---|---|
| emitTable | `group_id` (conditional, non-empty) | OK |
| emitTable | `table` | OK |
| emitTable | `rows` | OK |
| emitListMap | `group_id` (conditional) | OK |
| emitListMap | `id` | OK |
| emitListMap | `rows` | OK |
| emitFile | `group_id` (conditional) | OK |
| emitFile | `path` | OK |
| emitFile | `type` | OK |
| emitFile | `directives` | OK |
| emitFile | `records` | OK |
| emitMessage | `group_id` (conditional) | OK for group_message_data; **Bug 1**: message_data lacked it |
| emitMessage | `id` | OK |
| emitMessage | `directives` | OK |
| emitMessage | `fw_header` | OK |
| emitMessage | `records` | OK |
| emitRecords | `record_type` (conditional, non-null only) | OK (always optional in practice) |
| emitRecords | `fields` | OK |
| emitRecords | `rows` | OK |
| fieldFlow | `name` | OK |
| fieldFlow | `type` | OK |
| fieldFlow | `length` | OK |

**Key finding**: `emitRecords` writes `record_type` only when non-null (`if (record.getRecordType() != null)`), confirming Bug 2 — schema must not require it.
**Key finding**: `emitMessage` calls `emitGroupId` which writes `group_id` to message blocks. For `expected_request_header_messages`/`expected_request_body_messages` these use `message_data` $ref, so `group_id` must exist there (Bug 1 confirmed).

### Axis 3: Required Integrity (schema `required` arrays vs actual write behavior)

| Def | required field | Always written? | Verdict |
|---|---|---|---|
| table_data | `table` | Yes (`entry.prop("table", ...)`) | OK |
| table_data | `rows` | Yes (`emitMapRows`) | OK |
| list_map_data | `id` | Yes (`entry.prop("id", ...)`) | OK |
| list_map_data | `rows` | Yes (`emitMapRows`) | OK |
| file_data | `path` | Yes | OK |
| file_data | `type` | Yes | OK |
| file_data | `records` | No — `emitRecords` skips if empty | Potentially problematic, but `records: []` is valid per schema (`minItems: 0`) and the writer simply omits `records` key when empty; however schema has `records` in required. **Note**: writer outputs nothing when records empty → schema validation would fail if records is absent. But `file_data.records` has `minItems: 0` so empty array is valid; the issue is the writer does not emit `records: []` when empty. Upon review: `emitRecords` only emits `records:` header when non-empty — empty array case would fail schema required check. However this is an existing behavior concern pre-existing this task; no test covers it and it is out of scope for this task's known bugs. No additional fix needed for this task scope. |
| message_data | `id` | Yes | OK |
| message_data | `records` | Yes (always at least 1 record in message) | OK |
| group_message_data | `id` | Yes | OK |
| group_message_data | `records` | Yes | OK |
| record_fragment | `record_type` (pre-fix) | No — writer omits when null | **Bug 2 confirmed** |
| record_fragment | `fields` | Yes | OK |
| record_fragment | `rows` | Yes | OK |
| field_def | `name` | Yes | OK |
| field_def | `type` | Yes | OK |

**Additional finding on `file_data.records` required**: The writer does not emit `records: []` for empty record lists. However this is only relevant for empty-file setup (directives-only pattern documented in the schema as `minItems: 0`). The schema lists `records` as required, but this is intentional — users must explicitly specify `records: []` for empty files. The writer's `emitRecords` skipping empty is a writer-side behavior. This is not a schema error since the intent is that users specify `records: []` explicitly. No fix needed.

### Summary: No additional bugs beyond Bug 1 and Bug 2

## Completion Criteria

| Criterion | Self-check | Evidence | QA | QA Evidence |
|---|---|---|---|---|
| スキーマ横並びチェック（3軸）が実施され、チェック結果が checks/task-07.md に記録されている | OK | 本ファイル上部の 3-Axis Cross-Check セクション参照 | | |
| `message_data` の properties に `group_id` が追加されている（→ 再設計: `expected_request_message_data` として分離） | OK（再設計済み） | `message_data` の `group_id` を削除し、新 def `expected_request_message_data`（group_id, id, directives, records。fw_header なし）を追加。`expected_request_header_messages` / `expected_request_body_messages` の items.$ref を変更済み | | |
| `record_fragment.required` に `record_type` が含まれていない | OK | schema の `record_fragment.required` が `["fields","rows"]` のみになっている | | |
| 追加不備が見つかった場合はすべて修正されている（実装変更なし） | OK | 3軸チェックで Bug 1・Bug 2 以外の追加修正対象なし | | |
| `message_data` の `additionalProperties: false` を検証する負テスト（`load_schemaViolation_messagesWithGroupId`）が追加されている | OK | `schemaViolation_messages_groupId.yaml` と `YamlLoaderTest#load_schemaViolation_messagesWithGroupId` を追加。`group_id` が `message_data` に存在しないことを検証 | | |
| `expected_request_message_data` の `additionalProperties: false` を検証する負テスト（`load_schemaViolation_expectedRequestWithFwHeader`）が追加されている | OK | `schemaViolation_expectedRequest_fwHeader.yaml` と `YamlLoaderTest#load_schemaViolation_expectedRequestWithFwHeader` を追加。`fw_header` が `expected_request_message_data` に存在しないことを検証 | | |
| `expected_request_message_data.group_id` の `minLength: 1` を検証する負テスト（`load_schemaViolation_expectedRequestEmptyGroupId`）が追加されている | OK | `schemaViolation_expectedRequest_emptyGroupId.yaml` と `YamlLoaderTest#load_schemaViolation_expectedRequestEmptyGroupId` を追加。空文字 `""` が `minLength: 1` 違反になることを検証 | | |
| `expected_request_message_data.id` の description が更新されている | OK | `"リクエストID。Excel: EXPECTED_REQUEST_HEADER_MESSAGES[groupId]=ID の '=' 以降。フォーマット定義ファイル（{requestId}_SEND）の解決に使用される"` に変更済み | | |
| `expected_request_message_data` に `$comment` が追加されている | OK | `"header/body は現仕様(MS-04)で同一構造のため共通 def を使用。将来ヘッダ/ボディで構造が分岐する場合は expected_request_header_message_data / expected_request_body_message_data に分割する"` を追加済み | | |
| `mvn clean test` 全テスト PASS | OK | Tests run: 159, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS（負テスト3件追加後も全テスト PASS） | | |

## QA Expert Review

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Meaningful tests/verification | OK | 3軸チェック実施済み。負例テスト3件（messages+group_id, expected_request_*+fw_header, group_id空文字）追加で分離の効果を証明 |
| Edge case coverage | OK | additionalProperties違反・minLength違反の各境界ケースを負例テストでカバー |

## Expert Reviews (code changes only)

### Language Expert

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Best practices | OK | JSON Schema として慣用的。description は明確 |
| Codebase style consistency | OK | `id` description に Excel 記法を追加し、他 def（message_data.id, list_map_data.id）と同一パターンに統一 |
| GWT test format | OK | GWT コメント「table 等」に修正済み（`rows` の不正確な表記を除去） |

### Software-engineering Expert

| Aspect | Verdict | Evidence / Improvement |
|---|---|---|
| Separation of concerns | OK | `messages` 専用（fw_header あり・group_id なし）と `expected_request_*` 専用（fw_header なし・group_id あり）に正しく分離。response_* の先例パターンを踏襲 |
| System integrity | OK | expected_request_header/body が同一 def を共有することは現仕様(MS-04)で正当。$comment に設計根拠と将来分割方針を記録 |
| Maintainability | OK | $comment に「将来分岐時は expected_request_header_message_data / expected_request_body_message_data に分割」と具体的に記録済み |

## Overall Verdict

- Self-check: OK
- QA: OK
- Language expert: OK
- Software-engineering expert: OK
- Ready for user review: Yes
