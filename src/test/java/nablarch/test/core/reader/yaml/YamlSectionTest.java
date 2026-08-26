package nablarch.test.core.reader.yaml;

import nablarch.test.core.reader.DataType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * {@link YamlSection} の静的ユーティリティメソッドに対する単体テスト。
 *
 * <p>
 * DB 不要 — @RunWith なし。純粋なロジック検証のみ。
 * </p>
 */
public class YamlSectionTest {

    // ========================================================================
    // castMap: 非 Map を渡した場合は空 Map が返ること
    // ========================================================================

    /**
     * [YamlSection] castMap: String を渡した場合は空 Map が返ること。
     *
     * <p>
     * Given: castMap に String "hello" を渡す（Map ではない）<br>
     * When:  castMap("hello") を呼ぶ<br>
     * Then:  空 Map が返ること
     * </p>
     */
    @Test
    public void castMap_nonMapReturnsEmptyMap() {
        // When
        java.util.Map<String, Object> result = YamlSection.castMap("hello");

        // Then
        assertTrue("非 Map を渡した場合は空 Map が返ること", result.isEmpty());
    }

    /**
     * [YamlSection] castMap: Integer を渡した場合は空 Map が返ること。
     *
     * <p>
     * Given: castMap に Integer 42 を渡す（Map ではない）<br>
     * When:  castMap(42) を呼ぶ<br>
     * Then:  空 Map が返ること
     * </p>
     */
    @Test
    public void castMap_integerReturnsEmptyMap() {
        // When
        java.util.Map<String, Object> result = YamlSection.castMap(42);

        // Then
        assertTrue("Integer を渡した場合は空 Map が返ること", result.isEmpty());
    }

    // ========================================================================
    // dataTypeToSectionKey: MESSAGE → "messages"
    // ========================================================================

    /**
     * [YamlSection] dataTypeToSectionKey: DataType.MESSAGE → "messages" が返ること。
     *
     * <p>
     * Given: DataType.MESSAGE<br>
     * When:  YamlSection.dataTypeToSectionKey(DataType.MESSAGE) を呼ぶ<br>
     * Then:  "messages" が返ること
     * </p>
     */
    @Test
    public void dataTypeToSectionKey_messageMapsToMessages() {
        // When
        String key = YamlSection.dataTypeToSectionKey(DataType.MESSAGE);

        // Then
        assertThat("DataType.MESSAGE は 'messages' キーにマップされること", key, is("messages"));
    }

    // ========================================================================
    // isSendSyncMessageSectionKey: 送信同期4セクションだけが true になること
    // ========================================================================

    /**
     * [YamlSection] isSendSyncMessageSectionKey: 同期応答メッセージ送信で使う 4 セクションのみ
     * {@code true} が返ること。
     *
     * <p>
     * この 4 セクションでは {@code record_type} の記載値がそのままレコード種別になり、
     * {@code messages} では記載値が使われず "default" になる。その振り分けの判定を固定する<br>
     * Given: 送信同期 4 セクションのキー・{@code messages}・ファイル系セクションキー・null<br>
     * When:  YamlSection.isSendSyncMessageSectionKey を呼ぶ<br>
     * Then:  送信同期 4 セクションのみ true、それ以外は false が返ること
     * </p>
     */
    @Test
    public void isSendSyncMessageSectionKey_onlySendSyncFourSectionsAreTrue() {
        // When / Then: 送信同期4セクション
        assertThat("expected_request_header_messages は送信同期セクションであること",
                YamlSection.isSendSyncMessageSectionKey("expected_request_header_messages"), is(true));
        assertThat("expected_request_body_messages は送信同期セクションであること",
                YamlSection.isSendSyncMessageSectionKey("expected_request_body_messages"), is(true));
        assertThat("response_header_messages は送信同期セクションであること",
                YamlSection.isSendSyncMessageSectionKey("response_header_messages"), is(true));
        assertThat("response_body_messages は送信同期セクションであること",
                YamlSection.isSendSyncMessageSectionKey("response_body_messages"), is(true));

        // When / Then: それ以外
        assertThat("messages は送信同期セクションではないこと",
                YamlSection.isSendSyncMessageSectionKey("messages"), is(false));
        assertThat("setup_files は送信同期セクションではないこと",
                YamlSection.isSendSyncMessageSectionKey("setup_files"), is(false));
        assertThat("expected_files は送信同期セクションではないこと",
                YamlSection.isSendSyncMessageSectionKey("expected_files"), is(false));
        assertThat("null は送信同期セクションではないこと",
                YamlSection.isSendSyncMessageSectionKey(null), is(false));
    }

    // ========================================================================
    // toStr: 非 null 値の toString が返ること
    // ========================================================================

    /**
     * [YamlSection] toStr: 非 null 値を渡した場合はその toString が返ること。
     *
     * <p>
     * Given: 非 null の String "hello"<br>
     * When:  YamlSection.toStr("hello") を呼ぶ<br>
     * Then:  "hello" が返ること
     * </p>
     */
    @Test
    public void toStr_nonNullReturnsToString() {
        // When
        String result = YamlSection.toStr("hello");

        // Then
        assertThat("非 null 値の toString が返ること", result, is("hello"));
    }

    /**
     * [YamlSection] toStr: Integer を渡した場合はその文字列表現が返ること。
     *
     * <p>
     * Given: Integer 42<br>
     * When:  YamlSection.toStr(42) を呼ぶ<br>
     * Then:  "42" が返ること
     * </p>
     */
    @Test
    public void toStr_integerReturnsStringRepresentation() {
        // When
        String result = YamlSection.toStr(42);

        // Then
        assertThat("Integer の toStr は '42' を返すこと", result, is("42"));
    }

    /**
     * [YamlSection] toStr: null を渡した場合は null が返ること。
     *
     * <p>
     * Given: null<br>
     * When:  YamlSection.toStr(null) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void toStr_nullReturnsNull() {
        // When
        String result = YamlSection.toStr(null);

        // Then
        assertThat("null を渡した場合は null が返ること", result, nullValue());
    }

    // ========================================================================
    // interpret: interps が null/空のとき value がそのまま返ること
    // ========================================================================

    /**
     * [YamlSection] interpret: interpreters リストが null の場合は value がそのまま返ること。
     *
     * <p>
     * Given: value="test", interps=null<br>
     * When:  YamlSection.interpret("test", null) を呼ぶ<br>
     * Then:  "test" がそのまま返ること
     * </p>
     */
    @Test
    public void interpret_nullInterpretersReturnsValueAsIs() {
        // When
        String result = YamlSection.interpret("test", null);

        // Then
        assertThat("interpreters が null のとき value がそのまま返ること", result, is("test"));
    }

    /**
     * [YamlSection] interpret: interpreters リストが空の場合は value がそのまま返ること。
     *
     * <p>
     * Given: value="hello", interps=空リスト<br>
     * When:  YamlSection.interpret("hello", emptyList) を呼ぶ<br>
     * Then:  "hello" がそのまま返ること
     * </p>
     */
    @Test
    public void interpret_emptyInterpretersReturnsValueAsIs() {
        // Given
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> emptyList = Collections.emptyList();

        // When
        String result = YamlSection.interpret("hello", emptyList);

        // Then
        assertThat("interpreters が空のとき value がそのまま返ること", result, is("hello"));
    }

    /**
     * [YamlSection] interpret: value が null の場合は null が返ること。
     *
     * <p>
     * Given: value=null, interps=何らかのリスト<br>
     * When:  YamlSection.interpret(null, emptyList) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void interpret_nullValueReturnsNull() {
        // When
        String result = YamlSection.interpret(null, Collections.<nablarch.test.core.util.interpreter.TestDataInterpreter>emptyList());

        // Then
        assertThat("value が null のとき null が返ること", result, nullValue());
    }

    // ========================================================================
    // addBinaryFileInterpreter: interpreters が null の場合も BinaryFileInterpreter のみのリストが返ること
    // ========================================================================

    // ========================================================================
    // isMarker: null column は false を返すこと
    // ========================================================================

    /**
     * [YamlSection] isMarker: null を渡した場合は false が返ること。
     *
     * <p>
     * Given: column=null<br>
     * When:  YamlSection.isMarker(null) を呼ぶ<br>
     * Then:  false が返ること
     * </p>
     */
    @Test
    public void isMarker_nullReturnsFalse() {
        // When
        boolean result = YamlSection.isMarker(null);

        // Then
        assertThat("null を渡した場合は false が返ること", result, is(false));
    }

    /**
     * [YamlSection] addBinaryFileInterpreter: interpreters が null の場合、
     * BinaryFileInterpreter のみを含む 1 件のリストが返ること。
     *
     * <p>
     * Given: interpreters=null<br>
     * When:  YamlSection.addBinaryFileInterpreter("somePath", null) を呼ぶ<br>
     * Then:  サイズ 1 のリストが返り、先頭要素が BinaryFileInterpreter であること
     * </p>
     */
    @Test
    public void addBinaryFileInterpreter_nullInterpretersReturnsSingletonList() {
        // When
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> result =
                YamlSection.addBinaryFileInterpreter("src/test/java/", null);

        // Then
        assertThat("interpreters が null でも BinaryFileInterpreter を 1 件含むリストが返ること",
                result.size(), is(1));
        assertTrue("先頭要素が BinaryFileInterpreter であること",
                result.get(0) instanceof nablarch.test.core.util.interpreter.BinaryFileInterpreter);
    }
    // ========================================================================
    // resolveColumns: 先頭のキーを持つ行のキーをカラム名として決定すること
    // ========================================================================

    /**
     * 値を指定して記述順を保つ行（{@link LinkedHashMap}）を組み立てる。
     * SnakeYAML のマッピングロード結果に合わせる。
     *
     * @param keyValues キーと値を交互に並べたもの
     */
    private static Map<String, Object> rowOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /**
     * 全ての値を {@code "v"} とした行を {@link #rowOf(Object...)} で組み立てる。
     * 値そのものが問われないテスト（列名解決等）で使う。
     * 空マッピング（{@code {}}）の行は本メソッドではなく {@link #rowOf(Object...)} を引数なしで
     * 呼んで組み立てる（戻り値は同じだが、キーを持たない行はキーを列挙しない方の呼び方に揃える、
     * という本テストクラス内の表記規約）。
     */
    private static Map<String, Object> rowWithKeys(String... keys) {
        Object[] keyValues = new Object[keys.length * 2];
        for (int i = 0; i < keys.length; i++) {
            keyValues[i * 2] = keys[i];
            keyValues[i * 2 + 1] = "v";
        }
        return rowOf(keyValues);
    }

    /**
     * [YamlSection] resolveColumns: 先頭が空マッピングでも、後続で最初にキーを持つ行のキーが記述順で返ること。
     *
     * <p>
     * Given: 空マッピング行・3 キーの行・2 キーの行 からなる rows<br>
     * When:  YamlSection.resolveColumns(rows) を呼ぶ<br>
     * Then:  2 番目の行の 3 キーが YAML 記述順で返ること（後続行のキーは影響しないこと）
     * </p>
     */
    @Test
    public void resolveColumns_returnsKeysOfFirstKeyedRowInDeclarationOrder() {
        // Given
        List<Object> rows = Arrays.<Object>asList(
                rowOf(),
                rowWithKeys("COL_C", "COL_A", "COL_B"),
                rowWithKeys("COL_A"));

        // When
        List<String> result = YamlSection.resolveColumns(rows);

        // Then
        assertThat("先頭のキーを持つ行のキーが記述順で返ること",
                result, is(Arrays.asList("COL_C", "COL_A", "COL_B")));
    }

    /**
     * [YamlSection] resolveColumns: rows が空の場合は空リストが返ること。
     *
     * <p>
     * Given: 空の rows<br>
     * When:  YamlSection.resolveColumns(emptyList) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void resolveColumns_emptyRowsReturnsEmptyList() {
        // When
        List<String> result = YamlSection.resolveColumns(Collections.emptyList());

        // Then
        assertTrue("rows が空の場合は空リストが返ること", result.isEmpty());
    }

    /**
     * [YamlSection] resolveColumns: 全行が空マッピングの場合は空リストが返ること。
     *
     * <p>
     * Given: 空マッピング行のみ 2 件からなる rows<br>
     * When:  YamlSection.resolveColumns(rows) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void resolveColumns_allEmptyMappingRowsReturnsEmptyList() {
        // Given
        List<Object> rows = Arrays.<Object>asList(rowOf(), rowOf());

        // When
        List<String> result = YamlSection.resolveColumns(rows);

        // Then
        assertTrue("キーを持つ行が 1 つも無い場合は空リストが返ること", result.isEmpty());
    }

    /**
     * [YamlSection] resolveColumns: 全行がマッピングでない値（スカラ）の場合は空リストが返ること。
     *
     * <p>
     * Given: String と Integer だけからなる rows<br>
     * When:  YamlSection.resolveColumns(rows) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void resolveColumns_allScalarRowsReturnsEmptyList() {
        // Given
        List<Object> rows = Arrays.<Object>asList("scalar", 42);

        // When
        List<String> result = YamlSection.resolveColumns(rows);

        // Then
        assertTrue("キーを持つ行が 1 つも無い場合は空リストが返ること", result.isEmpty());
    }

    /**
     * [YamlSection] resolveColumns: マッピングでない行（スカラ等）を読み飛ばして後続の行から列名を決めること。
     *
     * <p>
     * YAML ファイル経由ではスキーマ（{@code rows.items} が {@code {"type":"object"}}）が
     * スカラ行を弾くため、この経路はこの単体テストでしか担保できない。<br>
     * Given: String 行・Integer 行・2 キーの行 からなる rows<br>
     * When:  YamlSection.resolveColumns(rows) を呼ぶ<br>
     * Then:  スカラ行が読み飛ばされ、3 番目の行の 2 キーが返ること
     * </p>
     */
    @Test
    public void resolveColumns_skipsNonMappingRows() {
        // Given
        List<Object> rows = Arrays.<Object>asList("scalar", 42, rowWithKeys("COL_X", "COL_Y"));

        // When
        List<String> result = YamlSection.resolveColumns(rows);

        // Then
        assertThat("マッピングでない行を読み飛ばして後続の行から列名を決めること",
                result, is(Arrays.asList("COL_X", "COL_Y")));
    }

    /**
     * [YamlSection] resolveColumns: カラム名の大文字小文字がそのまま保持されること。
     *
     * <p>
     * Given: 大文字小文字が混在するキーを持つ行 1 件からなる rows<br>
     * When:  YamlSection.resolveColumns(rows) を呼ぶ<br>
     * Then:  キーが大文字小文字を変換されずに返ること
     * </p>
     */
    @Test
    public void resolveColumns_preservesColumnNameCase() {
        // Given
        List<Object> rows = Arrays.<Object>asList(rowWithKeys("pkCol1", "PK_COL2", "Varchar2Col"));

        // When
        List<String> result = YamlSection.resolveColumns(rows);

        // Then
        assertThat("大文字小文字が保持されること",
                result, is(Arrays.asList("pkCol1", "PK_COL2", "Varchar2Col")));
    }

    // ========================================================================
    // dropBlankRows: 行として存在しないもの（空マッピング・全値が空文字）を取り除くこと
    // ========================================================================

    /**
     * [YamlSection] dropBlankRows: 空マッピング行と全ての値が空文字の行が取り除かれること。
     *
     * <p>
     * Given: 空マッピング行・全ての値が空文字の行・値を持つ行 からなる rows<br>
     * When:  YamlSection.dropBlankRows(rows) を呼ぶ<br>
     * Then:  値を持つ行 1 件のみが記述順で返ること
     * </p>
     */
    @Test
    public void dropBlankRows_removesEmptyMappingAndAllBlankValueRows() {
        // Given
        List<Object> rows = Arrays.<Object>asList(
                rowOf(),
                rowOf("COL_A", "", "COL_B", ""),
                rowOf("COL_A", "v", "COL_B", ""));

        // When
        List<Object> result = YamlSection.dropBlankRows(rows);

        // Then
        assertThat("値を持つ行のみ残ること", result.size(), is(1));
        assertThat("残った行が値を持つ行であること", result.get(0), is((Object) rowOf("COL_A", "v", "COL_B", "")));
    }

    /**
     * [YamlSection] dropBlankRows: 値が 1 つでも非空なら行が残ること（null と空文字が混在していても同じ）。
     *
     * <p>
     * Given: null・空文字・非空文字 が混在する行 1 件からなる rows<br>
     * When:  YamlSection.dropBlankRows(rows) を呼ぶ<br>
     * Then:  その行が残ること
     * </p>
     */
    @Test
    public void dropBlankRows_keepsRowHavingAnyNonBlankValue() {
        // Given
        List<Object> rows = Arrays.<Object>asList(rowOf("COL_A", null, "COL_B", "", "COL_C", "v"));

        // When
        List<Object> result = YamlSection.dropBlankRows(rows);

        // Then
        assertThat("値が 1 つでも非空なら残ること", result.size(), is(1));
    }

    /**
     * [YamlSection] dropBlankRows: 値が空白文字（半角スペース）だけの行も残ること。
     *
     * <p>
     * 空判定は {@code String.isEmpty()} で行い、値を trim してからは判定しない。依存先 nablarch-testing の
     * {@code PoiXlsReader#isBlankLine} が各セルを {@code String#isEmpty()} で判定し trim しないのに
     * 合わせるため、半角スペース 1 個は「値がある」として扱う。<br>
     * Given: 半角スペース 1 個だけを値に持つ行と、空文字の値を持つ行 からなる rows<br>
     * When:  YamlSection.dropBlankRows(rows) を呼ぶ<br>
     * Then:  半角スペースの行だけが残ること
     * </p>
     */
    @Test
    public void dropBlankRows_keepsRowHavingOnlyWhitespaceValue() {
        // Given
        List<Object> rows = Arrays.<Object>asList(
                rowOf("COL_A", " ", "COL_B", ""),
                rowOf("COL_A", "", "COL_B", ""));

        // When
        List<Object> result = YamlSection.dropBlankRows(rows);

        // Then
        assertThat("空白文字だけの値は trim されずに非空として扱われ、行が残ること", result.size(), is(1));
        assertThat("残ったのが半角スペースを持つ行であること",
                result.get(0), is((Object) rowOf("COL_A", " ", "COL_B", "")));
    }

    /**
     * [YamlSection] dropBlankRows: マーカーカラムだけが値を持つ行も残ること。
     *
     * <p>
     * 依存先 nablarch-testing の空行判定は行の全セルを対象とするため、マーカーカラムの値も
     * 空行判定の対象に含める。<br>
     * Given: "[NO]" のみ値を持ち他は空文字の行 1 件からなる rows<br>
     * When:  YamlSection.dropBlankRows(rows) を呼ぶ<br>
     * Then:  その行が残ること
     * </p>
     */
    @Test
    public void dropBlankRows_keepsRowHavingOnlyMarkerColumnValue() {
        // Given
        List<Object> rows = Arrays.<Object>asList(rowOf("[NO]", "1", "COL_A", ""));

        // When
        List<Object> result = YamlSection.dropBlankRows(rows);

        // Then
        assertThat("マーカーカラムの値も空行判定の対象になるため残ること", result.size(), is(1));
    }

    /**
     * [YamlSection] dropBlankRows: マッピングでない行（スカラ等）が取り除かれること。
     *
     * <p>
     * Given: String 行・Integer 行・値を持つ行 からなる rows<br>
     * When:  YamlSection.dropBlankRows(rows) を呼ぶ<br>
     * Then:  値を持つ行 1 件のみが返ること
     * </p>
     */
    @Test
    public void dropBlankRows_removesNonMappingRows() {
        // Given
        List<Object> rows = Arrays.<Object>asList("scalar", 42, rowOf("COL_A", "v"));

        // When
        List<Object> result = YamlSection.dropBlankRows(rows);

        // Then
        assertThat("マッピングでない行が取り除かれること", result.size(), is(1));
        assertThat(result.get(0), is((Object) rowOf("COL_A", "v")));
    }

    /**
     * [YamlSection] dropBlankRows: 値が全て Java null の行は残ること。
     *
     * <p>
     * スキップ条件は空マッピング（{@code {}}）と全ての値が空文字の 2 つだけであり、Java null は
     * 空文字ではないため行として残る。YAML ではクォートなしの {@code null} とキーだけ書いた
     * {@code COL:} がロード時点で Java null になるため、どちらの書き方の行もここで残る。<br>
     * Given: 全ての値が Java null の行 1 件からなる rows<br>
     * When:  YamlSection.dropBlankRows(rows) を呼ぶ<br>
     * Then:  その行が残ること
     * </p>
     */
    @Test
    public void dropBlankRows_keepsRowHavingOnlyNullValues() {
        // Given
        List<Object> rows = Arrays.<Object>asList(rowOf("COL_A", null, "COL_B", null));

        // When
        List<Object> result = YamlSection.dropBlankRows(rows);

        // Then
        assertThat("Java null は空文字ではないため行が残ること", result.size(), is(1));
        assertThat("残った行が全ての値が null の行であること",
                result.get(0), is((Object) rowOf("COL_A", null, "COL_B", null)));
    }
}
