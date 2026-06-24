package nablarch.test.core.reader.yaml;

import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.DataFileFragment;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static nablarch.test.core.reader.yaml.YamlSection.DEFAULT_RECORD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_DIRECTIVES;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_FIELDS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_LENGTH;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_NAME;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_PATH;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_RECORDS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_RECORD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ROWS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.FILE_TYPE_FIXED;
import static nablarch.test.core.reader.yaml.YamlSection.FW_HEADER_RECORD_TYPE;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.groupMatches;
import static nablarch.test.core.reader.yaml.YamlSection.interpret;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML のファイル系セクション（{@code setup_files}／{@code expected_files}）から、本体の器
 * （{@link DataFile}）を直接組み立てるビルダ。
 *
 * <p>
 * YAML トップレベル Map を走査し、ディレクティブ適用・レコードレイアウト／データ行の組み立て・
 * 特殊記法の解釈・グループ ID 絞り込みを行う。レコードレイアウト組み立て
 * （{@link #buildFragmentsForFile}、{@link #buildFragmentsForMessage}、{@link #buildFragmentsForSendSync}）と
 * ディレクティブ写し取り（{@link #mapDirectives}）は
 * メッセージ系（{@link YamlMessageBuilder}）からも再利用する。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlFileBuilder {

    private final InterpreterResolver interpreterResolver;

    /**
     * コンストラクタ。
     *
     * @param interpreterResolver basePath ごとに値加工インタープリタチェーンを解決する戦略
     */
    public YamlFileBuilder(InterpreterResolver interpreterResolver) {
        this.interpreterResolver = interpreterResolver;
    }

    /**
     * ファイル系セクションから指定グループの {@link DataFile} 群を組み立てる。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー（例: {@code "setup_files"}）
     * @param groupId    整形済みグループ ID
     * @param basePath   インタープリタ用ベースパス
     * @return DataFile リスト
     */
    public List<DataFile> buildDataFileList(Map<String, Object> yaml, String sectionKey,
                                            String groupId, String basePath) {
        List<DataFile> result = new ArrayList<DataFile>();
        List<TestDataInterpreter> interps = interpreterResolver.resolve(basePath);
        for (Object entry : getList(yaml, sectionKey)) {
            Map<String, Object> map = castMap(entry);
            if (!groupMatches(toStr(map.get(FIELD_GROUP_ID)), groupId)) {
                continue;
            }
            String path = toStr(map.get(FIELD_PATH));
            if (path == null) {
                throw new IllegalStateException(
                        "Missing required field 'path' in " + sectionKey + " entry. groupId=" + groupId
                                + ", basePath=" + basePath);
            }
            DataFile file = FILE_TYPE_FIXED.equals(toStr(map.get(FIELD_TYPE)))
                    ? new FixedLengthFile(path)
                    : new VariableLengthFile(path);
            applyDirectives(file, mapDirectives(map), interps);
            buildFragmentsForFile(file, getList(map, FIELD_RECORDS), interps);
            result.add(file);
        }
        return result;
    }

    /**
     * エントリの {@code directives:} マップを未加工で写し取る（YAML 順保持）。
     */
    static Map<String, String> mapDirectives(Map<String, Object> entry) {
        Map<String, String> directives = new LinkedHashMap<String, String>();
        Object directivesObj = entry.get(FIELD_DIRECTIVES);
        if (directivesObj == null) {
            return directives;
        }
        for (Map.Entry<String, Object> e : castMap(directivesObj).entrySet()) {
            directives.put(e.getKey(), toStr(e.getValue()));
        }
        return directives;
    }

    /**
     * 通常ファイル用（{@code setup_files}／{@code expected_files}）にレコード群から
     * {@link DataFileFragment} を組み立てる。
     *
     * <p>FW_HEADER レコードを含め、すべての {@code record_type} をそのまま使用する。</p>
     *
     * @param file    ファイル
     * @param records 生のレコードレイアウト Map 群（YAML 順）
     * @param interps 使用するインタープリタリスト
     */
    static void buildFragmentsForFile(DataFile file, List<Object> records,
                                      List<TestDataInterpreter> interps) {
        buildFragmentsInternal(file, records, false, false, interps);
    }

    /**
     * 受信メッセージ用にレコード群から {@link DataFileFragment} を組み立てる。
     *
     * <p>FW_HEADER レコードをスキップし、{@code record_type} を {@code "default"} に固定し、
     * 長さ未指定フィールドを {@code "-"}（動的計算）として扱う。値行に連番は付与しない。</p>
     *
     * @param file    ファイル
     * @param records 生のレコードレイアウト Map 群（YAML 順）
     * @param interps 使用するインタープリタリスト
     */
    static void buildFragmentsForMessage(DataFile file, List<Object> records,
                                         List<TestDataInterpreter> interps) {
        buildFragmentsInternal(file, records, true, false, interps);
    }

    /**
     * 送信同期メッセージ用にレコード群から {@link DataFileFragment} を組み立てる。
     *
     * <p>FW_HEADER レコードをスキップし、{@code record_type} を {@code "default"} に固定し、
     * 長さ未指定フィールドを {@code "-"}（動的計算）として扱う。
     * 各値行に連番（1 始まりの行インデックス）を {@link DataFileFragment#FIRST_FIELD_NO} として付与する。
     * 送信同期メッセージは本体パーサ（{@code SendSyncMessageParser}）が値行先頭セルの連番を
     * {@code FIRST_FIELD_NO} に隔離して保持し、{@code RequestTestingMessagingProvider} が
     * 要求/応答電文の照合に使うため、YAML 経路でも連番を補う必要がある。</p>
     *
     * @param file    ファイル
     * @param records 生のレコードレイアウト Map 群（YAML 順）
     * @param interps 使用するインタープリタリスト
     */
    static void buildFragmentsForSendSync(DataFile file, List<Object> records,
                                          List<TestDataInterpreter> interps) {
        buildFragmentsInternal(file, records, true, true, interps);
    }

    /**
     * レコード群から {@link DataFileFragment} を組み立てる共通実装。
     *
     * @param file         ファイル
     * @param records      生のレコードレイアウト Map 群（YAML 順）
     * @param skipFwHeader true の場合 FW_HEADER レコードをスキップし、record_type を {@code "default"} に固定し、
     *                     長さ未指定フィールドを {@code "-"}（動的計算）として扱う（メッセージ系）
     * @param withId       true の場合、各値行に連番（1 始まりの行インデックス）を
     *                     {@link DataFileFragment#FIRST_FIELD_NO} として付与する（送信同期メッセージのみ）
     * @param interps      使用するインタープリタリスト
     */
    private static void buildFragmentsInternal(DataFile file, List<Object> records,
                                               boolean skipFwHeader, boolean withId,
                                               List<TestDataInterpreter> interps) {
        for (Object recordObj : records) {
            Map<String, Object> record = castMap(recordObj);
            String recordType = toStr(record.get(FIELD_RECORD_TYPE));
            if (skipFwHeader && FW_HEADER_RECORD_TYPE.equals(recordType)) {
                continue;
            }

            DataFileFragment fragment = file.getNewFragment();
            fragment.setRecordType(skipFwHeader
                    ? DEFAULT_RECORD_TYPE
                    : (recordType != null ? recordType : DEFAULT_RECORD_TYPE));

            List<Object> fields = getList(record, FIELD_FIELDS);
            List<String> names = new ArrayList<String>(fields.size());
            List<String> types = new ArrayList<String>(fields.size());
            List<String> lengths = new ArrayList<String>(fields.size());
            boolean hasLength = false;
            for (Object fieldObj : fields) {
                Map<String, Object> field = castMap(fieldObj);
                names.add(toStr(field.get(FIELD_NAME)));
                types.add(toStr(field.get(FIELD_TYPE)));
                String length = toStr(field.get(FIELD_LENGTH));
                if (length != null) {
                    hasLength = true;
                    lengths.add(length);
                } else {
                    lengths.add(null);
                }
            }

            fragment.setNames(names);
            fragment.setTypes(types);

            // メッセージファイル（skipFwHeader=true）は常に固定長のため setLengths が必要。
            // それ以外は length フィールドが 1 件以上ある場合のみ setLengths を呼ぶ。
            if (skipFwHeader || hasLength) {
                List<String> cleanedLengths = new ArrayList<String>(lengths.size());
                for (String l : lengths) {
                    // skipFwHeader=true（メッセージ）の場合 length 未指定フィールドを "-"（動的計算）として扱う。
                    cleanedLengths.add(l != null ? l : (skipFwHeader ? "-" : ""));
                }
                fragment.setLengths(cleanedLengths);
            }

            int rowNo = 1;
            for (Object rowObj : getList(record, FIELD_ROWS)) {
                // SnakeYAML Engine では rows: の各要素は通常 List だが、外部入力（YAML ファイル）にマッピングや null が
                // 混入した場合への防御的ガード。Java 言語仕様上この分岐は通常到達不能だが、堅牢性のために残す。
                if (!(rowObj instanceof List)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                List<Object> rowList = (List<Object>) rowObj;
                List<String> rowValues = new ArrayList<String>(rowList.size());
                for (Object cell : rowList) {
                    rowValues.add(interpret(objectToString(cell), interps));
                }
                if (withId) {
                    // 送信同期メッセージ：値行先頭の連番（本体は値行先頭セル）を 1 始まりの行インデックスで補う。
                    fragment.addValueWithId(rowValues, String.valueOf(rowNo));
                } else {
                    fragment.addValue(rowValues);
                }
                rowNo++;
            }
        }
    }

    /**
     * ディレクティブ Map を {@link DataFile} に適用する。
     */
    static void applyDirectives(DataFile file, Map<String, String> directives, List<TestDataInterpreter> interps) {
        for (Map.Entry<String, String> e : directives.entrySet()) {
            // ディレクティブ値にも渡されたインタープリタを適用する。
            // YAML 経路では yamlInterpreters（QuotationTrimmer なし）が渡されるため
            // YAML パーサが処理済みのクォートを二重処理することはない。
            // Excel 経路では interpreters（QuotationTrimmer 含む）が渡される。
            file.setDirective(e.getKey(), interpret(e.getValue(), interps));
        }
    }

}
