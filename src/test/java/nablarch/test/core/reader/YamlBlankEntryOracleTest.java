package nablarch.test.core.reader;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * 「記法として空のエントリ」の判定を、本体（{@code nablarch-testing}）を正解（oracle）として
 * 突き合わせるテストクラス。
 *
 * <p>
 * 押さえる規則は次の 3 つである。(1) 記法として空のエントリは読み飛ばされ、その条件は Excel 形式では
 * 行の全セルが空セル、YAML 形式では {@code rows:} の要素が空マッピング {@code {}} であること。
 * (2) {@code ""} と書いた空文字は値であり、すべての値が {@code ""} の行は読み飛ばされない。
 * (3) この判定はマーカーカラムを除外する前に行われるため、マーカーカラムだけに値がある行も
 * 読み飛ばされない（他のカラムの値は形式ごとの記法どおりに読み込まれる）。
 * </p>
 * <p>
 * 本体がこの規則を満たすのは、{@code PoiXlsReader#readLine} の空行判定
 * （{@code PoiXlsReader#isBlankLine}）が生セル（{@code Cell#toString()}）を
 * {@code String#isEmpty()} だけで見るためである。{@code ""} と書いたセルは 2 文字の文字列であり
 * 非空なので、その行は読み飛ばされない。セルから引用符を外すのは
 * {@code QuotationTrimmer} であり、これは {@code TestDataParsingTemplate#readTestData} が
 * 空行判定より後に呼ぶ値加工（{@code interpret}）の一部である。
 * </p>
 * <p>
 * 本テストは規則を手写しした期待値ではなく、同じ意味の入力を本体に読ませた実行結果と突き合わせる。
 * 本体側の入力は {@link BodyExcelOracle} が POI で {@code .xlsx} を組み立て、本体の
 * {@link BasicTestDataParser}（{@link PoiXlsReader} と、{@code NullInterpreter}・
 * {@code QuotationTrimmer} を含む {@code interpreters}）で読む。YAML 側の入力は
 * {@code YamlBlankEntryOracleTest/blankEntry.yaml} に手書きし、{@link YamlTestDataParser}
 * （{@code yamlInterpreters}）で読む。インタープリタが異なるのは、YAML ではクォート無しの
 * {@code null} と {@code ""} を YAML パーサ自身が Java の {@code null}・空文字にするためである。
 * </p>
 * <p>
 * 4 種（{@code {}} の行／全値 {@code ""} の行／{@code null} だけの行／マーカーカラムだけに値がある行）を
 * テーブルデータ（{@code setup_tables}、ケース T1〜T6）と {@code LIST_MAP}（{@code list_maps}、
 * ケース L1〜L6）の双方で確かめる。是正の前後で結果が変わるのは T2・L2 である
 * （是正前は「全ての値が空文字」の行も読み飛ばしていたため）。他は結果が
 * 変わらない対照ケースで、是正が {@code {}}・Java null・マーカーカラムの扱いを壊していないことを
 * 固定する（マーカーカラムだけに値がある行は、是正前の判定でもマーカーの値を非空と見て残していた）。
 * </p>
 * <p>
 * 「マーカーカラムだけに値がある行」は 3 通りの書き方で置く。いずれも行が読み飛ばされない点は同じで、
 * 違うのは「他のカラムに何を書いたか」だけである。
 * </p>
 * <ul>
 * <li>T4・L4: 他のカラムに {@code ""} を明示した行。Excel の空セルは {@code ""} として読み込まれるため、
 *     Excel 側の「他のセルは空セル」と入力の意味がそろい、値まで一致する。</li>
 * <li>T5・L5: 他のカラムをキーごと省略した行。YAML における素直な書き方である。キーを省略したカラムは
 *     その行で {@code null} を明示したのと同じ扱いになるので、Excel の空セル（{@code ""}）とは
 *     入力の意味が異なる。したがって結果も本体が {@code ""}・YAML が Java {@code null} と分かれる。
 *     これは形式間の仕様差ではなく、非等価な入力を与えた結果である。
 *     {@link #getSetupTableData_markerOnlyRowWithOmittedColumnsIsKept()} と
 *     {@link #getListMap_markerOnlyRowWithOmittedKeysIsKept()} がこの食い違いを明示的に固定する。</li>
 * <li>T6・L6: T5・L5 と同じくキーを省略した行を YAML に置き、Excel 側は他のセルに {@code null} と
 *     記述した行にそろえたもの。入力の意味が等価になるため、値まで含めて本体と一致する。
 *     {@link #getSetupTableData_markerOnlyRowWithOmittedColumnsMatchesExplicitNull()} と
 *     {@link #getListMap_markerOnlyRowWithOmittedKeysMatchesExplicitNull()} が固定する。</li>
 * </ul>
 *
 * @author kiyotis
 */
@RunWith(DatabaseTestRunner.class)
public class YamlBlankEntryOracleTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    /** YAML 側テストデータのベースディレクトリ。 */
    private static final String YAML_DIR = "src/test/java/nablarch/test/core/reader/";

    /** YAML 側テストデータのリソース名。 */
    private static final String YAML_RESOURCE = "YamlBlankEntryOracleTest/blankEntry";

    /**
     * oracle 用ブック名。本体側の静的キャッシュ（テストデータ・ブック・解析結果）は
     * ディレクトリ名とリソース名をキーに持つため、他のテストと衝突しない名前にする。
     */
    private static final String ORACLE_BOOK = "YamlBlankEntryOracleTest";

    /** テーブルデータのカラム名。値が文字列型のカラムだけを使う。 */
    private static final String[] COLUMNS = {"PK_COL1", "VARCHAR2_COL", "NULL_COL"};

    /** マーカーカラムを先頭に置いたカラム名の行。 */
    private static final String[] MARKED_COLUMNS = {"[NO]", "PK_COL1", "VARCHAR2_COL", "NULL_COL"};

    /** {@code LIST_MAP} のキー名。 */
    private static final String[] KEYS = {"KEY1", "KEY2", "KEY3"};

    /** マーカーカラムを先頭に置いた {@code LIST_MAP} のキー名の行。 */
    private static final String[] MARKED_KEYS = {"[NO]", "KEY1", "KEY2", "KEY3"};

    /** 本体（Excel 経路）の oracle。1 度だけ組み立てて全テストで共有する。 */
    private static BodyExcelOracle oracle;

    private YamlTestDataParser sut;

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @Before
    public void before() {
        DbInfo dbInfo = repositoryResource.getComponent("dbInfo");
        if (oracle == null) {
            oracle = buildOracleBook(
                    repositoryResource.<List<TestDataInterpreter>>getComponent("interpreters"));
            // TableData はカラム型の解決に DbInfo を使うため、本体パーサにも設定する。
            oracle.parser().setDbInfo(dbInfo);
        }
        sut = new YamlTestDataParser();
        sut.setDbInfo(dbInfo);
        sut.setDefaultValues(new BasicDefaultValues());
        sut.setInterpreters(
                repositoryResource.<List<TestDataInterpreter>>getComponent("yamlInterpreters"));
    }

    @After
    public void after() {
        YamlTestDataParser.clearCacheForTest();
    }

    /**
     * 本体に読ませる {@code .xlsx} を組み立てて書き出す。
     *
     * <p>
     * シート名はケース名（T1〜T6・L1〜L6）と一致させ、{@code blankEntry.yaml} の
     * グループ ID／ID と 1 対 1 に対応させる。
     * </p>
     *
     * @param interpreters 本体の Excel 経路で使うインタープリタリスト
     * @return 書き出し済みの oracle
     */
    private static BodyExcelOracle buildOracleBook(List<TestDataInterpreter> interpreters) {
        BodyExcelOracle book = new BodyExcelOracle(ORACLE_BOOK, interpreters);
        // テーブルデータ（SETUP_TABLE）。
        dataSheet(book, "SETUP_TABLE[T1]=TEST_TABLE", "T1", COLUMNS,
                row("00001", "v1", "n1"), row("", "", ""));
        dataSheet(book, "SETUP_TABLE[T2]=TEST_TABLE", "T2", COLUMNS,
                row("00001", "v1", "n1"), row("\"\"", "\"\"", "\"\""));
        dataSheet(book, "SETUP_TABLE[T3]=TEST_TABLE", "T3", COLUMNS,
                row("00001", "v1", "n1"), row("null", "null", "null"));
        dataSheet(book, "SETUP_TABLE[T4]=TEST_TABLE", "T4", MARKED_COLUMNS,
                row("1", "00001", "v1", "n1"), row("2", "", "", ""));
        // T5 の Excel は T4 と同じ内容（他のセルは空セル）。YAML 側だけがカラムごと省略した書き方であり、
        // 入力が等価でないために生じる値の食い違いを
        // getSetupTableData_markerOnlyRowWithOmittedColumnsIsKept が固定する。
        dataSheet(book, "SETUP_TABLE[T5]=TEST_TABLE", "T5", MARKED_COLUMNS,
                row("1", "00001", "v1", "n1"), row("2", "", "", ""));
        // T6 は T5 の YAML（カラムごと省略）と入力が等価になるよう、Excel 側の他のセルに null と書く。
        // NullInterpreter が Java null に変換するため、キー省略と同じ意味の入力になる。
        dataSheet(book, "SETUP_TABLE[T6]=TEST_TABLE", "T6", MARKED_COLUMNS,
                row("1", "00001", "v1", "n1"), row("2", "null", "null", "null"));
        // LIST_MAP。
        dataSheet(book, "LIST_MAP=L1", "L1", KEYS,
                row("v1", "v2", "v3"), row("", "", ""));
        dataSheet(book, "LIST_MAP=L2", "L2", KEYS,
                row("v1", "v2", "v3"), row("\"\"", "\"\"", "\"\""));
        dataSheet(book, "LIST_MAP=L3", "L3", KEYS,
                row("v1", "v2", "v3"), row("null", "null", "null"));
        dataSheet(book, "LIST_MAP=L4", "L4", MARKED_KEYS,
                row("1", "v1", "v2", "v3"), row("2", "", "", ""));
        // L5 の Excel は L4 と同じ内容（T5 と同じ趣旨）。
        dataSheet(book, "LIST_MAP=L5", "L5", MARKED_KEYS,
                row("1", "v1", "v2", "v3"), row("2", "", "", ""));
        // L6 は T6 と同じ趣旨。Excel 側の他のセルに null と書き、YAML のキー省略と入力を等価にする。
        dataSheet(book, "LIST_MAP=L6", "L6", MARKED_KEYS,
                row("1", "v1", "v2", "v3"), row("2", "null", "null", "null"));
        return book.write();
    }

    /**
     * カラム名の行を持つデータブロック（テーブルデータ・{@code LIST_MAP}）のシートを組み立てる。
     *
     * <p>
     * Excel 形式では識別子行の次の行がカラム名の行として読み込まれるため、識別子行・カラム名の行・
     * データ行の順に積む。{@code LIST_MAP} もカラム名の行を持つデータタイプであり、同じ並びでよい
     * （マーカーカラムが効くのもカラム名の行を持つこの 2 つのデータタイプである）。
     * </p>
     *
     * @param book      組み立て先
     * @param typeCell  識別子行の先頭セル（{@code データタイプ[ID]=識別子の値}）
     * @param name      シート名（＝ケース名）
     * @param columns   カラム名の行
     * @param normalRow 値を持つ通常のデータ行
     * @param blankRow  判定対象のデータ行
     */
    private static void dataSheet(BodyExcelOracle book, String typeCell, String name,
                                  String[] columns, String[] normalRow, String[] blankRow) {
        book.row(name, typeCell);
        book.row(name, columns);
        book.row(name, normalRow);
        book.row(name, blankRow);
    }

    /** セルの並びをそのまま配列にする（可読性のためのシュガー）。 */
    private static String[] row(String... cells) {
        return cells;
    }

    // ========================================================================
    // テーブルデータ（setup_tables）
    // ========================================================================

    /**
     * [YamlTestDataParser] getSetupTableData: 空マッピング {@code &#123;&#125;} の行が読み飛ばされること（T1）。
     *
     * <p>
     * 読み飛ばしの条件は、Excel 形式では行の全セルが空セル、YAML 形式では {@code rows:} の要素が
     * 空マッピング {@code {}} であることである。是正の前後で結果が変わらない対照ケースである。<br>
     * Given: 通常行 1 件と、空マッピング（Excel は全セル空）の行 1 件<br>
     * When:  YAML と本体（Excel）の双方を {@code getSetupTableData} で読む<br>
     * Then:  いずれも通常行 1 件だけになること
     * </p>
     */
    @Test
    public void getSetupTableData_emptyMappingRowIsSkipped() {
        assertTableCase("T1", COLUMNS, new String[][]{{"00001", "v1", "n1"}});
    }

    /**
     * [YamlTestDataParser] getSetupTableData: すべての値が空文字の行が読み飛ばされないこと（T2）。
     *
     * <p>
     * {@code ""} と書いた空文字は値であるため、すべての値が {@code ""} の行は読み飛ばされず、
     * 全カラムが空文字の行として読み込まれる。<br>
     * Given: 通常行 1 件と、全カラムに {@code ""} を書いた行 1 件<br>
     * When:  YAML と本体（Excel）の双方を {@code getSetupTableData} で読む<br>
     * Then:  いずれも 2 件になり、2 件目の全カラムが空文字であること
     * </p>
     */
    @Test
    public void getSetupTableData_allEmptyStringRowIsKept() {
        assertTableCase("T2", COLUMNS, new String[][]{{"00001", "v1", "n1"}, {"", "", ""}});
    }

    /**
     * [YamlTestDataParser] getSetupTableData: すべての値が null の行が読み飛ばされないこと（T3）。
     *
     * <p>
     * 読み飛ばしの条件は「全セルが空セル」（Excel）と「空マッピング {@code {}}」（YAML）だけであり、
     * null はそのどちらでもないため値として残る。是正の前後で結果が変わらない対照ケースである。<br>
     * Given: 通常行 1 件と、全カラムが null（Excel は全セル {@code null}）の行 1 件<br>
     * When:  YAML と本体（Excel）の双方を {@code getSetupTableData} で読む<br>
     * Then:  いずれも 2 件になり、2 件目の全カラムが null であること
     * </p>
     */
    @Test
    public void getSetupTableData_allNullRowIsKept() {
        assertTableCase("T3", COLUMNS, new String[][]{{"00001", "v1", "n1"}, {null, null, null}});
    }

    /**
     * [YamlTestDataParser] getSetupTableData: マーカーカラムだけに値がある行が読み飛ばされないこと（T4）。
     *
     * <p>
     * 空行の判定はマーカーカラムを除外する前に行われるため、マーカーカラムだけに値がある行も
     * 読み飛ばされない。ここでは他のカラムに {@code ""} を明示し、Excel の空セルと入力の意味をそろえる。<br>
     * Given: マーカーカラム {@code [NO]} を持つカラム名の行と、通常行 1 件・
     *        {@code [NO]} だけに値がある行 1 件<br>
     * When:  YAML と本体（Excel）の双方を {@code getSetupTableData} で読む<br>
     * Then:  いずれもカラム名からマーカーカラムが除かれ、2 件になり、2 件目の全カラムが空文字であること
     * </p>
     */
    @Test
    public void getSetupTableData_markerOnlyRowIsKept() {
        assertTableCase("T4", COLUMNS, new String[][]{{"00001", "v1", "n1"}, {"", "", ""}});
    }

    /**
     * [YamlTestDataParser] getSetupTableData: マーカーカラム以外をキーごと省略した行も読み飛ばされず、
     * 省略したカラムの値だけが本体（{@code ""}）と YAML（Java {@code null}）で食い違うこと（T5）。
     *
     * <p>
     * 「マーカーカラムだけに値があるエントリ」の YAML における素直な書き方は、他のカラムをキーごと
     * 省略した {@code - "[NO]": "2"} である。キーを省略したカラムは、その行でそのカラムに {@code null} を
     * 書いたのと同じ扱いになる（キーが無い状態ではなく、値が {@code null} の状態で保持される）。
     * 一方 Excel の空セルは {@code ""} として読み込まれる。つまり両者は入力として等価ではなく、
     * 行が読み飛ばされない点（件数・カラム名）は一致する一方で、省略したカラムの値だけが
     * 本体 {@code ""}・YAML {@code null} と分かれる。形式間の仕様差ではないが、
     * 素直な書き方どうしを並べると値が変わることは分かりにくいため、隠さずここで固定する。
     * </p>
     * <p>
     * 値までそろえたい場合は 2 通りある。他のカラムに {@code ""} を明示して Excel の空セルにそろえるか
     * （T4。{@link #getSetupTableData_markerOnlyRowIsKept()}）、Excel 側に {@code null} と記述して
     * キー省略にそろえるか（T6。{@link #getSetupTableData_markerOnlyRowWithOmittedColumnsMatchesExplicitNull()}）
     * である。<br>
     * Given: マーカーカラム {@code [NO]} を持つカラム名の行と、通常行 1 件・{@code [NO]} だけを
     *        キーに持つ行 1 件（Excel は {@code [NO]} だけに値がある行）<br>
     * When:  YAML と本体（Excel）の双方を {@code getSetupTableData} で読む<br>
     * Then:  行数とカラム名は本体と一致し、本体の 2 行目は全カラム {@code ""}、
     *        YAML の 2 行目は全カラム {@code null} になること
     * </p>
     */
    @Test
    public void getSetupTableData_markerOnlyRowWithOmittedColumnsIsKept() {
        TableData expected = single(
                oracle.parser().getSetupTableData(oracle.dir(), oracle.resource("T5"), "T5"), "T5");
        TableData actual = single(sut.getSetupTableData(YAML_DIR, YAML_RESOURCE, "T5"), "T5");

        assertTableValues("T5 本体（Excel）", expected, COLUMNS,
                new String[][]{{"00001", "v1", "n1"}, {"", "", ""}});

        // 行が読み飛ばされないこと・カラム名が本体と一致すること。
        assertThat("T5: 行数が本体と一致すること", actual.size(), is(expected.size()));
        assertThat("T5: カラム名が本体と一致すること",
                Arrays.asList(actual.getColumnNames()), is(Arrays.asList(expected.getColumnNames())));
        assertThat("T5: 1 行目は本体と同じであること",
                rowValuesOf(actual, 0), is(rowValuesOf(expected, 0)));

        // 省略したカラムの値だけが食い違う（入力が非等価なため）。
        for (String column : COLUMNS) {
            assertThat("T5: 本体（Excel）の 2 行目の " + column + " は空文字であること",
                    expected.getValue(1, column), is((Object) ""));
            assertNull("T5: YAML の 2 行目の " + column + " は省略により null になること",
                    actual.getValue(1, column));
        }
    }

    /**
     * [YamlTestDataParser] getSetupTableData: マーカーカラム以外をキーごと省略した行は、他のカラムに
     * {@code null} と記述した Excel の行と結果まで一致すること（T6）。
     *
     * <p>
     * T5 が示すとおり、キーを省略したカラムは {@code null} を明示したのと同じ扱いになる。したがって
     * Excel 側の他のセルに {@code null} と記述すれば（{@code NullInterpreter} が Java {@code null} に
     * 変換する）、両者は入力として等価になり、カラム名・行数だけでなく値まで一致するはずである。
     * T5 の食い違いが形式間の仕様差ではなく入力の非等価によることを、この対照で示す。<br>
     * Given: マーカーカラム {@code [NO]} を持つカラム名の行と、通常行 1 件・{@code [NO]} だけを
     *        キーに持つ行 1 件（Excel は他のセルに {@code null} と記述した行）<br>
     * When:  YAML と本体（Excel）の双方を {@code getSetupTableData} で読む<br>
     * Then:  カラム名・行数・全カラムの値が本体と一致し、2 行目は双方とも全カラム {@code null} であること
     * </p>
     */
    @Test
    public void getSetupTableData_markerOnlyRowWithOmittedColumnsMatchesExplicitNull() {
        assertTableCase("T6", COLUMNS, new String[][]{{"00001", "v1", "n1"}, {null, null, null}});
    }

    // ========================================================================
    // LIST_MAP（list_maps）
    // ========================================================================

    /**
     * [YamlTestDataParser] getListMap: 空マッピング {@code &#123;&#125;} の行が読み飛ばされること（L1）。
     *
     * <p>
     * 根拠は {@link #getSetupTableData_emptyMappingRowIsSkipped()} と同じ。
     * 是正の前後で結果が変わらない対照ケースである。<br>
     * Given: 通常行 1 件と、空マッピング（Excel は全セル空）の行 1 件<br>
     * When:  YAML と本体（Excel）の双方を {@code getListMap} で読む<br>
     * Then:  いずれも通常行 1 件だけになること
     * </p>
     */
    @Test
    public void getListMap_emptyMappingRowIsSkipped() {
        assertListMapCase("L1", KEYS, new String[][]{{"v1", "v2", "v3"}});
    }

    /**
     * [YamlTestDataParser] getListMap: すべての値が空文字の行が読み飛ばされないこと（L2）。
     *
     * <p>
     * 根拠は {@link #getSetupTableData_allEmptyStringRowIsKept()} と同じ。<br>
     * Given: 通常行 1 件と、全キーに {@code ""} を書いた行 1 件<br>
     * When:  YAML と本体（Excel）の双方を {@code getListMap} で読む<br>
     * Then:  いずれも 2 件になり、2 件目の全キーの値が空文字であること
     * </p>
     */
    @Test
    public void getListMap_allEmptyStringRowIsKept() {
        assertListMapCase("L2", KEYS, new String[][]{{"v1", "v2", "v3"}, {"", "", ""}});
    }

    /**
     * [YamlTestDataParser] getListMap: すべての値が null の行が読み飛ばされないこと（L3）。
     *
     * <p>
     * 根拠は {@link #getSetupTableData_allNullRowIsKept()} と同じ。
     * 是正の前後で結果が変わらない対照ケースである。<br>
     * Given: 通常行 1 件と、全キーが null（Excel は全セル {@code null}）の行 1 件<br>
     * When:  YAML と本体（Excel）の双方を {@code getListMap} で読む<br>
     * Then:  いずれも 2 件になり、2 件目の全キーの値が null であること
     * </p>
     */
    @Test
    public void getListMap_allNullRowIsKept() {
        assertListMapCase("L3", KEYS, new String[][]{{"v1", "v2", "v3"}, {null, null, null}});
    }

    /**
     * [YamlTestDataParser] getListMap: マーカーカラムだけに値がある行が読み飛ばされないこと（L4）。
     *
     * <p>
     * 根拠は {@link #getSetupTableData_markerOnlyRowIsKept()} と同じ。<br>
     * Given: マーカーカラム {@code [NO]} を持つキー名の行と、通常行 1 件・
     *        {@code [NO]} だけに値がある行 1 件<br>
     * When:  YAML と本体（Excel）の双方を {@code getListMap} で読む<br>
     * Then:  いずれもキーからマーカーカラムが除かれ、2 件になり、2 件目の全キーの値が空文字であること
     * </p>
     */
    @Test
    public void getListMap_markerOnlyRowIsKept() {
        assertListMapCase("L4", KEYS, new String[][]{{"v1", "v2", "v3"}, {"", "", ""}});
    }

    /**
     * [YamlTestDataParser] getListMap: マーカーカラム以外をキーごと省略した行も読み飛ばされず、
     * 省略したキーの値だけが本体（{@code ""}）と YAML（Java {@code null}）で食い違うこと（L5）。
     *
     * <p>
     * 趣旨は {@link #getSetupTableData_markerOnlyRowWithOmittedColumnsIsKept()} と同じで、
     * キーを省略した行と Excel の空セルの行は入力として等価ではない。{@code list_maps} 経路も
     * テーブル系と同じ {@code YamlTableDataBuilder#extractRows} を通り、そこで {@code rowMap.get(col)} が
     * 省略したキーに対して {@code null} を返すため、「キーを省略したカラムは {@code null} を明示したのと
     * 同じ扱いになる」がそのまま当てはまる。値までそろえた対照は L4（{@code ""} を明示）と
     * L6（Excel 側に {@code null} と記述。{@link #getListMap_markerOnlyRowWithOmittedKeysMatchesExplicitNull()}）
     * である。<br>
     * Given: マーカーカラム {@code [NO]} を持つキー名の行と、通常行 1 件・{@code [NO]} だけを
     *        キーに持つ行 1 件<br>
     * When:  YAML と本体（Excel）の双方を {@code getListMap} で読む<br>
     * Then:  件数とキー集合は本体と一致し、本体の 2 件目は全キーが {@code ""}、
     *        YAML の 2 件目は全キーが {@code null} になること
     * </p>
     */
    @Test
    public void getListMap_markerOnlyRowWithOmittedKeysIsKept() {
        List<Map<String, String>> expected =
                oracle.parser().getListMap(oracle.dir(), oracle.resource("L5"), "L5");
        List<Map<String, String>> actual = sut.getListMap(YAML_DIR, YAML_RESOURCE, "L5");

        assertListMapValues("L5 本体（Excel）", expected, KEYS,
                new String[][]{{"v1", "v2", "v3"}, {"", "", ""}});

        // 行が読み飛ばされないこと・キー集合が本体と一致すること。
        assertThat("L5: 件数が本体と一致すること", actual.size(), is(expected.size()));
        for (int i = 0; i < expected.size(); i++) {
            assertThat("L5: " + i + " 件目のキー集合が本体と一致すること",
                    new ArrayList<String>(actual.get(i).keySet()),
                    is(new ArrayList<String>(expected.get(i).keySet())));
        }
        assertThat("L5: 1 件目は本体と同じであること", actual.get(0), is(expected.get(0)));

        // 省略したキーの値だけが食い違う（入力が非等価なため）。
        for (String key : KEYS) {
            assertThat("L5: 本体（Excel）の 2 件目の " + key + " は空文字であること",
                    expected.get(1).get(key), is(""));
            assertNull("L5: YAML の 2 件目の " + key + " は省略により null になること",
                    actual.get(1).get(key));
        }
    }

    /**
     * [YamlTestDataParser] getListMap: マーカーカラム以外をキーごと省略した行は、他のキーに
     * {@code null} と記述した Excel の行と結果まで一致すること（L6）。
     *
     * <p>
     * 趣旨は {@link #getSetupTableData_markerOnlyRowWithOmittedColumnsMatchesExplicitNull()} と同じ。<br>
     * Given: マーカーカラム {@code [NO]} を持つキー名の行と、通常行 1 件・{@code [NO]} だけを
     *        キーに持つ行 1 件（Excel は他のセルに {@code null} と記述した行）<br>
     * When:  YAML と本体（Excel）の双方を {@code getListMap} で読む<br>
     * Then:  キー集合・件数・全キーの値が本体と一致し、2 件目は双方とも全キーが {@code null} であること
     * </p>
     */
    @Test
    public void getListMap_markerOnlyRowWithOmittedKeysMatchesExplicitNull() {
        assertListMapCase("L6", KEYS, new String[][]{{"v1", "v2", "v3"}, {null, null, null}});
    }

    // ========================================================================
    // ヘルパー
    // ========================================================================

    /**
     * テーブルデータ 1 ケースについて、本体（Excel）の結果を正解として YAML の結果と突き合わせる。
     *
     * @param caseName        ケース名（＝ Excel のシート名 ＝ YAML のグループ ID）
     * @param expectedColumns 本体が返すはずのカラム名
     * @param expectedRows    本体が返すはずの各行の値
     */
    private void assertTableCase(String caseName, String[] expectedColumns, String[][] expectedRows) {
        TableData expected = single(
                oracle.parser().getSetupTableData(oracle.dir(), oracle.resource(caseName), caseName), caseName);
        TableData actual = single(
                sut.getSetupTableData(YAML_DIR, YAML_RESOURCE, caseName), caseName);

        assertTableValues(caseName + " 本体（Excel）", expected, expectedColumns, expectedRows);
        assertThat(caseName + ": カラム名が本体と一致すること",
                Arrays.asList(actual.getColumnNames()), is(Arrays.asList(expected.getColumnNames())));
        assertThat(caseName + ": 行数が本体と一致すること", actual.size(), is(expected.size()));
        for (int i = 0; i < expected.size(); i++) {
            for (String column : expected.getColumnNames()) {
                assertThat(caseName + ": " + i + " 行目の " + column + " が本体と一致すること",
                        actual.getValue(i, column), is(expected.getValue(i, column)));
            }
        }
    }

    /**
     * {@code LIST_MAP} 1 ケースについて、本体（Excel）の結果を正解として YAML の結果と突き合わせる。
     *
     * @param caseName     ケース名（＝ Excel のシート名 ＝ YAML の ID）
     * @param expectedKeys 本体が返すはずのキー名
     * @param expectedRows 本体が返すはずの各行の値
     */
    private void assertListMapCase(String caseName, String[] expectedKeys, String[][] expectedRows) {
        List<Map<String, String>> expected =
                oracle.parser().getListMap(oracle.dir(), oracle.resource(caseName), caseName);
        List<Map<String, String>> actual = sut.getListMap(YAML_DIR, YAML_RESOURCE, caseName);

        assertListMapValues(caseName + " 本体（Excel）", expected, expectedKeys, expectedRows);
        assertThat(caseName + ": 件数が本体と一致すること", actual.size(), is(expected.size()));
        for (int i = 0; i < expected.size(); i++) {
            assertThat(caseName + ": " + i + " 件目のキー集合が本体と一致すること",
                    new ArrayList<String>(actual.get(i).keySet()),
                    is(new ArrayList<String>(expected.get(i).keySet())));
            for (Map.Entry<String, String> entry : expected.get(i).entrySet()) {
                assertThat(caseName + ": " + i + " 件目の " + entry.getKey() + " が本体と一致すること",
                        actual.get(i).get(entry.getKey()), is(entry.getValue()));
            }
        }
    }

    /**
     * 本体が返した {@link TableData} が、このテストの想定どおりの内容であることを確かめる。
     *
     * <p>本体を正解として扱う以上、その正解自体が意図した内容であることをここで固定する。</p>
     */
    private static void assertTableValues(String message, TableData table,
                                          String[] expectedColumns, String[][] expectedRows) {
        assertThat(message + ": カラム名", Arrays.asList(table.getColumnNames()),
                is(Arrays.asList(expectedColumns)));
        assertThat(message + ": 行数", table.size(), is(expectedRows.length));
        for (int i = 0; i < expectedRows.length; i++) {
            for (int j = 0; j < expectedColumns.length; j++) {
                assertThat(message + ": " + i + " 行目の " + expectedColumns[j],
                        table.getValue(i, expectedColumns[j]), is((Object) expectedRows[i][j]));
            }
        }
    }

    /**
     * 本体が返した {@code LIST_MAP} の結果が、このテストの想定どおりの内容であることを確かめる。
     *
     * <p>本体を正解として扱う以上、その正解自体が意図した内容であることをここで固定する。</p>
     */
    private static void assertListMapValues(String message, List<Map<String, String>> rows,
                                            String[] expectedKeys, String[][] expectedRows) {
        assertThat(message + ": 件数", rows.size(), is(expectedRows.length));
        for (int i = 0; i < expectedRows.length; i++) {
            assertThat(message + ": " + i + " 件目のキー集合",
                    new ArrayList<String>(rows.get(i).keySet()), is(Arrays.asList(expectedKeys)));
            for (int j = 0; j < expectedKeys.length; j++) {
                assertThat(message + ": " + i + " 件目の " + expectedKeys[j],
                        rows.get(i).get(expectedKeys[j]), is(expectedRows[i][j]));
            }
        }
    }

    /** 1 行分の値をカラム名（{@link #COLUMNS}）の順に取り出す。 */
    private static List<Object> rowValuesOf(TableData table, int rowIndex) {
        List<Object> values = new ArrayList<Object>(COLUMNS.length);
        for (String column : COLUMNS) {
            values.add(table.getValue(rowIndex, column));
        }
        return values;
    }

    /** テーブルデータが 1 件だけ取得できることを確かめ、その 1 件を返す。 */
    private static TableData single(List<TableData> tables, String caseName) {
        assertThat(caseName + ": テーブルデータが 1 件取得できること", tables.size(), is(1));
        return tables.get(0);
    }
}
