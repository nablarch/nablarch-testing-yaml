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
import static org.junit.Assert.assertThat;

/**
 * 「記法として空のエントリ」の判定を、本体（{@code nablarch-testing}）を正解（oracle）として
 * 突き合わせるテストクラス。
 *
 * <p>
 * 解説書（{@code nablarch-document} リポジトリの
 * {@code ja/development_tools/testing_framework/implementation/testdata_notation.rst}）の
 * 「コメント・マーカーカラム・空エントリを扱う」節は次のように定める。行番号は改版で腐るため
 * 節見出しと引用文で示す。
 * </p>
 * <blockquote>
 * 記法として空のエントリは読み飛ばされる。Excel 形式では行の全セルが空セルの場合、YAML 形式では
 * {@code rows:} 内の要素が空マッピング（{@code {}}）の場合である。{@code ""} と書いた空文字は値であり、
 * すべての値が {@code ""} のエントリは読み飛ばされず、全カラムが空文字のエントリとして読み込まれる。
 * （中略）この判定はマーカーカラムを除外する前に行われる。そのため、マーカーカラムだけに値がある
 * エントリは読み飛ばされず、他のカラムがすべて空文字のエントリとして読み込まれる。
 * </blockquote>
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
 * テーブルデータ（{@code setup_tables}、ケース T1〜T4）と {@code LIST_MAP}（{@code list_maps}、
 * ケース L1〜L4）の双方で確かめる。是正の前後で結果が変わるのは T2・L2 である
 * （是正前は「全ての値が空文字」の行も読み飛ばしていたため）。T1・T3・T4・L1・L3・L4 は結果が
 * 変わらない対照ケースで、是正が {@code {}}・Java null・マーカーカラムの扱いを壊していないことを
 * 固定する（マーカーカラムだけに値がある行は、是正前の判定でもマーカーの値を非空と見て残していた）。
 * </p>
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
        }
        // TableData はカラム型の解決に DbInfo を使うため、本体パーサにも設定する。
        oracle.parser().setDbInfo(dbInfo);
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
     * シート名はケース名（T1〜T4・L1〜L4）と一致させ、{@code blankEntry.yaml} の
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
        // LIST_MAP。
        dataSheet(book, "LIST_MAP=L1", "L1", KEYS,
                row("v1", "v2", "v3"), row("", "", ""));
        dataSheet(book, "LIST_MAP=L2", "L2", KEYS,
                row("v1", "v2", "v3"), row("\"\"", "\"\"", "\"\""));
        dataSheet(book, "LIST_MAP=L3", "L3", KEYS,
                row("v1", "v2", "v3"), row("null", "null", "null"));
        dataSheet(book, "LIST_MAP=L4", "L4", MARKED_KEYS,
                row("1", "v1", "v2", "v3"), row("2", "", "", ""));
        return book.write();
    }

    /**
     * カラム名の行を持つデータブロック（テーブルデータ・{@code LIST_MAP}）のシートを組み立てる。
     *
     * <p>
     * 識別子行の次の行がカラム名の行として読み込まれる（出典: 解説書「テーブルのデータを記述する」節の
     * 「0件のデータを記述する」項の「Excel形式の場合」
     * 「識別子行の次の行がカラム名の行として読み込まれるため、カラム名の行を書かないと、
     * その次に現れた行がカラム名の行になる」）。{@code LIST_MAP} も同じくカラム名の行を持つ
     * （同「データブロックとデータタイプ」節「マーカーカラムはカラム名の行を持つデータタイプ
     * （テーブルデータと {@code LIST_MAP}）にかかる」）。
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
     * 根拠: 「コメント・マーカーカラム・空エントリを扱う」節「記法として空のエントリは読み飛ばされる。
     * Excel 形式では行の全セルが空セルの場合、YAML 形式では {@code rows:} 内の要素が
     * 空マッピング（{@code {}}）の場合である」。是正の前後で結果が変わらない対照ケースである。<br>
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
     * 根拠: 同節「{@code ""} と書いた空文字は値であり、すべての値が {@code ""} のエントリは
     * 読み飛ばされず、全カラムが空文字のエントリとして読み込まれる」。<br>
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
     * 根拠: 同節が読み飛ばしの条件として挙げるのは「全セルが空セル」（Excel）と
     * 「空マッピング {@code {}}」（YAML）だけであり、null はそのどちらでもない。
     * 是正の前後で結果が変わらない対照ケースである。<br>
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
     * 根拠: 同節「この判定はマーカーカラムを除外する前に行われる。そのため、マーカーカラムだけに値がある
     * エントリは読み飛ばされず、他のカラムがすべて空文字のエントリとして読み込まれる」。<br>
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

    // ========================================================================
    // ヘルパー
    // ========================================================================

    /**
     * テーブルデータ 1 ケースについて、本体（Excel）の結果を正解として YAML の結果と突き合わせる。
     *
     * @param caseName        ケース名（＝ Excel のシート名 ＝ YAML のグループ ID）
     * @param expectedColumns 本体が返すカラム名（解説書が定める挙動）
     * @param expectedRows    本体が返す各行の値（解説書が定める挙動）
     */
    private void assertTableCase(String caseName, String[] expectedColumns, String[][] expectedRows) {
        TableData expected = single(
                oracle.parser().getSetupTableData(oracle.dir(), oracle.resource(caseName), caseName), caseName);
        TableData actual = single(
                sut.getSetupTableData(YAML_DIR, YAML_RESOURCE, caseName), caseName);

        // 本体を正解として扱う以上、その正解自体が解説書と一致していることをここで固定する。
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
     * @param expectedKeys 本体が返すキー名（解説書が定める挙動）
     * @param expectedRows 本体が返す各行の値（解説書が定める挙動）
     */
    private void assertListMapCase(String caseName, String[] expectedKeys, String[][] expectedRows) {
        List<Map<String, String>> expected =
                oracle.parser().getListMap(oracle.dir(), oracle.resource(caseName), caseName);
        List<Map<String, String>> actual = sut.getListMap(YAML_DIR, YAML_RESOURCE, caseName);

        // 本体を正解として扱う以上、その正解自体が解説書と一致していることをここで固定する。
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

    /** 本体が返した {@link TableData} が解説書の定める挙動どおりであることを確かめる。 */
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

    /** 本体が返した {@code LIST_MAP} の結果が解説書の定める挙動どおりであることを確かめる。 */
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

    /** テーブルデータが 1 件だけ取得できることを確かめ、その 1 件を返す。 */
    private static TableData single(List<TableData> tables, String caseName) {
        assertThat(caseName + ": テーブルデータが 1 件取得できること", tables.size(), is(1));
        return tables.get(0);
    }
}
