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
     * 記述順を保つ行（{@link LinkedHashMap}）を組み立てる。SnakeYAML のマッピングロード結果に合わせる。
     */
    private static Map<String, Object> row(String... keys) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (String key : keys) {
            map.put(key, "v");
        }
        return map;
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
                row(),
                row("COL_C", "COL_A", "COL_B"),
                row("COL_A"));

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
        List<Object> rows = Arrays.<Object>asList(row(), row());

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
        List<Object> rows = Arrays.<Object>asList("scalar", 42, row("COL_X", "COL_Y"));

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
        List<Object> rows = Arrays.<Object>asList(row("pkCol1", "PK_COL2", "Varchar2Col"));

        // When
        List<String> result = YamlSection.resolveColumns(rows);

        // Then
        assertThat("大文字小文字が保持されること",
                result, is(Arrays.asList("pkCol1", "PK_COL2", "Varchar2Col")));
    }
}
