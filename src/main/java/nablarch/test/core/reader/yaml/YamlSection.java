package nablarch.test.core.reader.yaml;

import nablarch.test.core.reader.DataType;
import nablarch.test.core.util.interpreter.BinaryFileInterpreter;
import nablarch.test.core.util.interpreter.InterpretationContext;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * YAML セクションキー定数・共通ヘルパーメソッド、およびデータ行の意味規則。
 *
 * <p>
 * {@code nablarch.test.core.reader.yaml} パッケージ内のビルダ（{@code Yaml*Builder}）・
 * {@link nablarch.test.core.reader.YamlTestDataParser} から使用する。
 * </p>
 *
 * <p>
 * 定数と値変換ヘルパーに加えて、テーブル系セクション（{@code setup_tables}／{@code expected_tables}／
 * {@code expected_complete_tables}）と {@code list_maps} における次の 3 つの意味規則を持つ。
 * </p>
 * <ul>
 *   <li>何を行とみなすか — {@link #dropBlankRows(List)}</li>
 *   <li>カラム名をどの行から決めるか — {@link #resolveColumns(List)}</li>
 *   <li>どのカラム名を DB 操作対象外のマーカーカラムとみなすか — {@link #isMarker(String)}</li>
 * </ul>
 * <p>
 * {@code dropBlankRows} は値加工（{@link #interpret(String, List, String)}）より前に適用する。
 * {@code resolveColumns} との前後は、どちらも同じ判定で行を読み飛ばすため結果を変えない
 * （いずれも根拠は {@link #dropBlankRows(List)} の javadoc に記す）。
 * {@code isMarker} は列名が決まった後に適用する。
 * {@code record_fragment} の行を組み立てる {@link YamlFileBuilder}（ファイル系・電文系の両方が使う）は
 * いずれの規則も使わない。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlSection {

    // ========================================================================
    // セクションキー定数
    // ========================================================================

    public static final String KEY_SETUP_TABLES = "setup_tables";
    public static final String KEY_EXPECTED_TABLES = "expected_tables";
    public static final String KEY_EXPECTED_COMPLETE_TABLES = "expected_complete_tables";
    public static final String KEY_LIST_MAPS = "list_maps";
    public static final String KEY_SETUP_FILES = "setup_files";
    public static final String KEY_EXPECTED_FILES = "expected_files";
    public static final String KEY_MESSAGES = "messages";
    public static final String KEY_EXPECTED_REQUEST_HEADER_MESSAGES = "expected_request_header_messages";
    public static final String KEY_EXPECTED_REQUEST_BODY_MESSAGES = "expected_request_body_messages";
    public static final String KEY_RESPONSE_HEADER_MESSAGES = "response_header_messages";
    public static final String KEY_RESPONSE_BODY_MESSAGES = "response_body_messages";

    // ========================================================================
    // フィールドキー定数
    // ========================================================================

    public static final String FIELD_GROUP_ID = "group_id";
    public static final String FIELD_ID = "id";
    public static final String FIELD_TABLE = "table";
    public static final String FIELD_ROWS = "rows";
    public static final String FIELD_PATH = "path";
    /** "fixed" / "variable" またはフィールド型 */
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_DIRECTIVES = "directives";
    public static final String FIELD_RECORDS = "records";
    public static final String FIELD_RECORD_TYPE = "record_type";
    public static final String FIELD_FIELDS = "fields";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_LENGTH = "length";

    // ========================================================================
    // ファイル種別定数
    // ========================================================================

    public static final String FILE_TYPE_FIXED = "fixed";

    // ========================================================================
    // メッセージ系定数
    // ========================================================================

    /** messages エントリ直下の FW 制御ヘッダマップキー */
    public static final String FIELD_FW_HEADER = "fw_header";

    /** レコードタイプ名。record_type が未指定の場合のフォールバック、および {@code messages} で記載値の代わりに使用する。 */
    public static final String DEFAULT_RECORD_TYPE = "default";

    // ========================================================================
    // 値の検査
    // ========================================================================

    /** 値に書けないバックスラッシュと {@code r} の 2 文字（Java 文字列としては {@code "\\r"}） */
    private static final String LITERAL_CR = "\\r";

    // ========================================================================
    // ユーティリティメソッド
    // ========================================================================

    private YamlSection() {
    }

    /**
     * YAML Map から指定キーのリストを取得する。値が null またはキー不在の場合は空リストを返す。
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) {
            return (List<Object>) val;
        }
        return Collections.emptyList();
    }

    /**
     * Object を {@code Map<String, Object>} にキャストする。Map でない場合は空 Map を返す。
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Collections.emptyMap();
    }

    /**
     * YAML Map のキー値（パス・ファイル種別・フィールド名等の設定値）を文字列に変換する（null の場合は null）。
     *
     * <p>
     * 設定値取得用。テストデータのセルデータ変換には {@link #objectToString(Object)} を使うこと。
     * </p>
     */
    public static String toStr(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * YAML のテストデータ値を文字列に変換する。
     *
     * <ul>
     * <li>null → null</li>
     * <li>Boolean → "true"/"false"</li>
     * <li>数値 → 数字文字列</li>
     * <li>その他 → {@code toString()}</li>
     * </ul>
     *
     * <p>
     * テストデータのセル値変換用。設定値取得には {@link #toStr(Object)} を使うこと。
     * 現在の実装は {@link #toStr(Object)} と同一だが、将来 YAML ネイティブ型の変換仕様が
     * 変わった場合はこちらのみ変更すること（例: 数値フォーマットの変更、null 表現の変換等）。
     * </p>
     */
    public static String objectToString(Object value) {
        return toStr(value);
    }

    /**
     * データ行のうち、行として存在しないものを取り除く。
     *
     * <p>
     * 値を 1 つも持たない行（空マッピング {@code {}}）だけを、Excel の全セル空行と同じく
     * 行が無いものとして扱う。空文字 {@code ""} も Java null も値であり、どちらも非空として扱うため、
     * 全ての値が {@code ""} の行や {@code COL: null}・{@code COL:}（値の省略）だけの行は残る。
     * マッピングでない行（スカラ等）は構造を持たないためここで取り除く
     * （{@link #castMap(Object)} が Map でない値に対して空 Map を返すので、空マッピングと同じ判定で
     * 扱える）。
     * </p>
     *
     * <p>
     * 出典は解説書（{@code nablarch-document} リポジトリの
     * {@code ja/development_tools/testing_framework/implementation/testdata_notation.rst}）
     * 「コメント・マーカーカラム・空エントリを扱う」節である。行番号は改版で腐るため引用文で示す。
     * 「記法として空のエントリは読み飛ばされる。Excel 形式では行の全セルが空セルの場合、YAML 形式では
     * {@code rows:} 内の要素が空マッピング（{@code {}}）の場合である。{@code ""} と書いた空文字は値であり、
     * すべての値が {@code ""} のエントリは読み飛ばされず、全カラムが空文字のエントリとして読み込まれる。」
     * </p>
     *
     * <p>
     * テーブル系セクション（{@code setup_tables}／{@code expected_tables}／
     * {@code expected_complete_tables}）と {@code list_maps} の行に適用する。{@code record_fragment} の
     * {@code rows}（ファイル系セクションと電文系セクションのレコードレイアウトが持つ行）には適用しない。
     * その行はカラム名をキーに持つマッピングではなく値だけを並べた配列（{@link List}）であり
     * （{@link YamlFileBuilder} の {@code buildFragmentsInternal} が各行を {@code List} としてのみ扱い、
     * {@code List} でない行は読み飛ばす。ファイル系・電文系のどの入口もこのメソッドを通る）、
     * {@link #castMap(Object)} が Map でない値に対して空 Map を返すため、本メソッドを通すと
     * 値の有無にかかわらず全ての行が取り除かれてしまう。
     * </p>
     *
     * <p>
     * 値加工（{@link #interpret(String, List, String)}）より前に適用すること。依存先 nablarch-testing では、
     * {@code PoiXlsReader#readLine} が読み込み段階で空行（{@code PoiXlsReader#isBlankLine}）を
     * そのまま読み飛ばす。値加工（{@code interpret}）を持つのは
     * {@code TestDataParsingTemplate#readTestData} の方で、こちらも {@code isBlankLine} による
     * 読み飛ばしを {@code interpret} より前に行う。本メソッドはこの順序に揃える。
     * </p>
     *
     * <p>
     * 列名解決（{@link #resolveColumns(List)}）との前後関係は結果を変えない。
     * {@code resolveColumns} は本メソッドと同じ {@code isBlankRow} で行を読み飛ばすため、
     * 本メソッドを通す前と後のどちらで呼んでも決まる列名は同じである。
     * </p>
     *
     * @param rows データ行のリスト（null 不可）。呼び出し側は {@link #getList(Map, String)} の戻り値を
     *             渡すこと。キー不在・値 null は {@code getList} が空リストとして吸収するため、
     *             本メソッドでは null を受け付けない
     * @return 行として存在しないものを取り除いたリスト
     */
    public static List<Object> dropBlankRows(List<Object> rows) {
        List<Object> result = new ArrayList<Object>(rows.size());
        for (Object row : rows) {
            if (!isBlankRow(row)) {
                result.add(row);
            }
        }
        return result;
    }

    /**
     * 行として存在しないもの（値を 1 つも持たない行）か判定する。
     *
     * <p>
     * 該当するのは空マッピング（{@code {}}）と、マッピングでない行（スカラ等。
     * {@link #castMap(Object)} が空 Map を返すため空マッピングと同じ判定になる）だけである。
     * 空文字 {@code ""} も Java null も値であり、どちらも非空として扱うため、全ての値が {@code ""} の行、
     * {@code COL: null} や {@code COL:}（値の省略）だけの行、マーカーカラム（{@code [COL]}）だけに
     * 値がある行は、いずれも残す。マーカーカラムを除外するのは列名が決まった後
     * （{@link #isMarker(String)} の適用時）であり、本判定はそれより前に行われる。
     * </p>
     */
    private static boolean isBlankRow(Object row) {
        return castMap(row).isEmpty();
    }

    /**
     * 先頭のキーを持つ行のキーをカラム名（マーカー含む・YAML 記述順・大文字小文字保持）として決定する。
     * キーを持つ行が 1 つも無い場合は空リストを返す。
     *
     * <p>
     * 読み飛ばす行の判定は {@link #dropBlankRows(List)} と同じ {@code isBlankRow} に委ねる。
     * 判定を 1 箇所に閉じることで、両者の条件が食い違わないようにする。
     * 本パッケージのビルダは {@link #dropBlankRows(List)} で除去済みの行を渡すためこの読み飛ばしには
     * 到達しないが、除去を行わない呼び出しでも列名が 0 件に倒れないようにこの判定を残す。
     * </p>
     *
     * <p>
     * SnakeYAML はマッピングを {@link java.util.LinkedHashMap} でロードするため、{@code keySet} の
     * 順序は YAML 記述順と一致する。
     * </p>
     */
    public static List<String> resolveColumns(List<Object> rows) {
        for (Object row : rows) {
            if (!isBlankRow(row)) {
                return new ArrayList<String>(castMap(row).keySet());
            }
        }
        return new ArrayList<String>();
    }

    /**
     * マーカーカラム（{@code [COL]} 形式）か判定する。
     */
    public static boolean isMarker(String column) {
        return column != null && column.startsWith("[") && column.endsWith("]");
    }

    /**
     * インタープリタチェーンを適用して値を変換する。
     *
     * <p>
     * 変換の前に {@link #rejectLiteralCr(String, String)} で値を検査する。インタープリタが 1 つも
     * 渡されない場合も検査は行う。
     * </p>
     *
     * @param value   変換する値（null 可。null の場合は検査も変換もせず null を返す）
     * @param interps 適用するインタープリタチェーン（null・空可）
     * @param source  検査に失敗したときに例外メッセージへ出す出所
     *                （{@link #rejectLiteralCr(String, String)} の {@code source} 引数）
     * @return 変換後の値
     */
    public static String interpret(String value, List<TestDataInterpreter> interps, String source) {
        if (value == null) {
            return null;
        }
        rejectLiteralCr(value, source);
        if (interps == null || interps.isEmpty()) {
            return value;
        }
        InterpretationContext ctx = new InterpretationContext(value, interps);
        return ctx.invokeNext();
    }

    /**
     * バックスラッシュと {@code r} の 2 文字を含む値を拒否する。
     *
     * <p>
     * 値のどこかにこの 2 文字が現れたら {@link IllegalStateException} を投げる。
     * YAML のダブルクォート文字列に {@code "\r"} と書いてパーサが展開した実際の CR（0x0D）と、
     * バックスラッシュと {@code n} の 2 文字は通す。
     * </p>
     *
     * <p>
     * 出典は解説書（{@code nablarch-document} リポジトリの
     * {@code ja/development_tools/testing_framework/implementation/testdata_notation.rst}）
     * 「null・空文字・改行など特殊な値を記述する」節の「YAML形式の場合」項にある表の「改行文字」の行である
     * （「YAML形式の場合」という見出しはこの解説書に 8 回現れるため、親節を添えて一意にする）。
     * 行番号は改版で腐るため引用文で示す。
     * 「YAML のパーサが制御文字に変換する。バックスラッシュと {@code r} の2文字（{@code "\\r"}）を含む値は
     * 書けない。Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値はテスティング
     * フレームワークの仕様上存在せず、YAML 形式ではエラーになる。{@code "\\n"} は Excel 形式と同じく
     * 2文字のまま残る」
     * </p>
     *
     * <p>
     * Excel 形式でこの2文字が必ず CR になるのは、依存先 nablarch-testing の
     * {@link nablarch.test.core.util.interpreter.LineSeparatorInterpreter} が既定の置換パターン
     * （{@code DEFAULT_PATTERN}。Java 文字列 {@code "\\\\r"}、正規表現としてはリテラルのバックスラッシュ＋{@code r}）を
     * 既定の改行コード（{@code DEFAULT_LINE_SEP}。{@code LineSeparator.CR}）へ置換するためである。
     * YAML 形式ではこのインタープリタを使わない。出典は解説書
     * {@code ja/development_tools/testing_framework/setup/common.rst} の
     * 「テストデータの形式をYAMLに変更する」節である。
     * 「{@code yamlInterpreters} に指定するのは、この2つだけでよい。null ・空文字・ダブルクォート・改行文字は
     * YAML のパーサが構文として解釈するため、Excel 形式で必要な {@code NullInterpreter} ・
     * {@code QuotationTrimmer} ・ {@code LineSeparatorInterpreter} は指定しない。」
     * </p>
     *
     * <p>
     * 検査はこのメソッド 1 箇所に置き、値が通る 2 つの経路から呼ぶ。
     * データ行とディレクティブは {@link #interpret(String, List, String)} を通り、
     * FW 制御ヘッダ（{@code fw_header:}）は解釈を行わないため
     * {@code YamlMessageBuilder#convertFwHeader} から直接呼ぶ。
     * </p>
     *
     * <p>
     * マーカーカラム（{@link #isMarker(String)}）の値はこの検査を通らない。
     * {@link YamlTableDataBuilder} が {@code interpret} を呼ぶ前にマーカーカラムを除外するためである。
     * マーカーカラムの値は DB 操作にも突合にも使われず捨てられるので、検査の対象外でよい。
     * {@code type:} ・ {@code record_type:} ・ {@code path:} ・ {@code table:} ・ {@code id:} などの
     * スキーマ構造値も、値ではなく設定値であるため対象外である（{@link #toStr(Object)} を通り
     * {@code interpret} を通らない）。
     * </p>
     *
     * @param value  検査する値（null 可。null の場合は何もしない）
     * @param source 例外メッセージへ出す出所。セクションキーと、エントリを特定する
     *               {@code table}／{@code path}／{@code id} を含む文字列
     *               （例: {@code "setup_tables entry table='USER'"}）
     */
    static void rejectLiteralCr(String value, String source) {
        if (value == null || !value.contains(LITERAL_CR)) {
            return;
        }
        throw new IllegalStateException(
                "A value containing a backslash followed by 'r' cannot be written in YAML format. "
                        + "In Excel format this 2-character sequence is always converted to CR, "
                        + "so such a value does not exist in the testing framework. "
                        + "Write an actual CR with the YAML escape \"\\r\" instead. "
                        + "value=[" + value + "], source=" + source);
    }

    /**
     * {@link BinaryFileInterpreter} をリストの先頭に積んで返す。
     */
    public static List<TestDataInterpreter> addBinaryFileInterpreter(String path,
                                                               List<TestDataInterpreter> interpreters) {
        BinaryFileInterpreter fileInterpreter = new BinaryFileInterpreter(path);
        List<TestDataInterpreter> result = new ArrayList<TestDataInterpreter>(
                (interpreters != null ? interpreters.size() : 0) + 1);
        result.add(fileInterpreter);
        if (interpreters != null) {
            result.addAll(interpreters);
        }
        return result;
    }

    /**
     * 整形済みグループ ID（{@code "[xxx]"} または {@code ""}）と生のグループ ID が一致するか。
     *
     * @param rawGroupId        YAML エントリの {@code group_id} 値（null の場合はグループなし）
     * @param requestedFormatted 呼び出し側が保持する整形済みグループ ID（{@code "[xxx]"} または {@code ""}）
     * @return 一致する場合 {@code true}
     */
    public static boolean groupMatches(String rawGroupId, String requestedFormatted) {
        String formatted = rawGroupId != null ? "[" + rawGroupId + "]" : "";
        return formatted.equals(requestedFormatted);
    }

    /**
     * 指定されたセクションキーが同期応答メッセージ送信で使う 4 セクション
     * （{@code expected_request_header_messages}／{@code expected_request_body_messages}／
     * {@code response_header_messages}／{@code response_body_messages}）のいずれかかを判定する。
     *
     * <p>
     * 電文のレコード種別の扱いはセクションによって異なる。この 4 セクションでは
     * {@code record_type} に記載した値がそのままレコード種別になる（{@code "FW_HEADER"} のような
     * 予約値はなく、記載どおりのレコード種別として扱われる）。
     * 一方 {@code messages} では記載した値は使われず、デフォルトのレコード種別
     * （{@link #DEFAULT_RECORD_TYPE}）になる。
     * </p>
     *
     * @param sectionKey セクションキー
     * @return 同期応答メッセージ送信の 4 セクションのいずれかの場合 {@code true}
     */
    public static boolean isSendSyncMessageSectionKey(String sectionKey) {
        return KEY_EXPECTED_REQUEST_HEADER_MESSAGES.equals(sectionKey)
                || KEY_EXPECTED_REQUEST_BODY_MESSAGES.equals(sectionKey)
                || KEY_RESPONSE_HEADER_MESSAGES.equals(sectionKey)
                || KEY_RESPONSE_BODY_MESSAGES.equals(sectionKey);
    }

    /**
     * {@link DataType} から YAML セクションキーへ変換する。
     */
    public static String dataTypeToSectionKey(DataType dataType) {
        switch (dataType) {
            case MESSAGE:                          return KEY_MESSAGES;
            case EXPECTED_REQUEST_HEADER_MESSAGES: return KEY_EXPECTED_REQUEST_HEADER_MESSAGES;
            case EXPECTED_REQUEST_BODY_MESSAGES:   return KEY_EXPECTED_REQUEST_BODY_MESSAGES;
            case RESPONSE_HEADER_MESSAGES:         return KEY_RESPONSE_HEADER_MESSAGES;
            case RESPONSE_BODY_MESSAGES:           return KEY_RESPONSE_BODY_MESSAGES;
            default:
                throw new IllegalArgumentException("Unsupported DataType for messaging: " + dataType);
        }
    }
}
