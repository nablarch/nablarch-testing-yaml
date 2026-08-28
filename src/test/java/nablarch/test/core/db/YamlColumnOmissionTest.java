package nablarch.test.core.db;

import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.test.Assertion;
import nablarch.test.core.reader.YamlTestDataParser;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@code ntf-testdata-yaml-schema.json} の {@code properties.expected_tables}（:18）・
 * {@code properties.expected_complete_tables}（:25）・
 * {@code $defs.table_data.properties.rows}（:108）の description が述べる
 * 「カラムの省略」の挙動を、YAML ファイルを経由した実経路（{@link YamlTestDataParser} →
 * {@link TableData} → {@link Assertion}）で固定する。
 *
 * <p>
 * カラムの省略には2種類あり、扱いが異なる（{@code #24} で is 108 に明文化）。
 * (1) カラム名決定行に無いカラム（全ての行で省略した場合のほか、カラム名決定行より
 * 後ろの行にだけ書いた場合もこれに当たる）。
 * (2) カラム名決定行にはあるが個々の行で省略したカラム（値 null として保持される）。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlColumnOmissionTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource =
            new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String DIR = "src/test/java/nablarch/test/core/db/";
    private static final String RES = "YamlColumnOmissionTest/omission";

    private YamlTestDataParser sut;

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @Before
    public void before() {
        DbInfo dbInfo = repositoryResource.getComponent("dbInfo");
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("yamlInterpreters");
        sut = new YamlTestDataParser();
        sut.setDbInfo(dbInfo);
        sut.setDefaultValues(new BasicDefaultValues());
        sut.setInterpreters(interpreters);
        VariousDbTestHelper.delete(TestTable.class);
    }

    @After
    public void after() {
        YamlTestDataParser.clearCacheForTest();
    }

    // ------------------------------------------------------------------ util

    private void setUp(String groupId) {
        final List<TableData> tables = sut.getSetupTableData(DIR, RES, groupId);
        assertThat("setup グループが1件取れること: " + groupId, tables.size(), is(1));
        new TransactionTemplateInternal(DbAccessTestSupport.DB_TRANSACTION_FOR_TEST) {
            @Override
            protected void doInTransaction(TransactionManagerConnection conn) {
                for (TableData t : tables) {
                    t.deleteData(conn);
                }
                for (TableData t : tables) {
                    t.insertData(conn);
                }
            }
        }.execute();
    }

    private TableData expected(String groupId) {
        List<TableData> l = sut.getExpectedTableData(DIR, RES, groupId);
        assertThat("expected グループが1件取れること: " + groupId, l.size(), is(1));
        return l.get(0);
    }

    /** YAML 経路を通さずに DB へ 1 行入れる（期待値側だけを変異させたときに検知できるようにするため）。 */
    private static void seed(String pk1, long pk2, String varchar2, long number) {
        VariousDbTestHelper.insert(new TestTable(pk1, pk2, varchar2, number,
                new java.math.BigDecimal("0"), new java.sql.Date(0L), new java.sql.Timestamp(0L),
                " ", " ".toCharArray(), new byte[10], Boolean.FALSE));
    }

    /** PK と VARCHAR2_COL 以外を BasicDefaultValues と同じ値にした 1 行を DB へ入れる。 */
    private static void seedAllDefaults(String pk1, long pk2, String varchar2) {
        seed(pk1, pk2, varchar2, 0L);
    }

    /** 生の JDBC で 1 カラムを読む。NULL は Java null で返る。 */
    private static Object raw(String pk1, String col) throws Exception {
        try (Connection c = VariousDbTestHelper.getNativeConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT " + col + " FROM TEST_TABLE WHERE PK_COL1 = ?")) {
            ps.setString(1, pk1);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("行が存在すること pk=" + pk1, rs.next());
                return rs.getObject(1);
            }
        }
    }

    private static void exec(String sql) throws Exception {
        try (Connection c = VariousDbTestHelper.getNativeConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private static boolean assertionFires(TableData expected) {
        try {
            Assertion.assertTableEquals("YamlColumnOmissionTest", expected);
            return false;
        } catch (AssertionError e) {
            return true;
        }
    }

    // ------------------------------------------------------------- (1) の挙動

    /** (1): カラム名決定行に無い（全ての行で省略した）カラムはカラム名の集合に入らない。 */
    @Test
    public void allRowOmittedColumnIsNotInColumnNames() {
        List<TableData> tables = sut.getSetupTableData(DIR, RES, "s1");
        String[] cols = tables.get(0).getColumnNames();
        assertThat("YAML に書いた 6 カラムだけになること: " + Arrays.toString(cols), cols.length, is(6));
        assertThat("全行で省略した VARCHAR2_COL が入らないこと",
                Arrays.asList(cols).contains("VARCHAR2_COL"), is(false));
        assertThat(Arrays.asList(cols).contains("NULL_COL"), is(false));
        assertThat(Arrays.asList(cols).contains("BOOL_COL"), is(false));
    }

    /**
     * (1) の別ケース: カラム名決定行より後ろの行にしか無いキーもカラム名の集合に入らず、
     * その行に書いた値は捨てられてデフォルト値が INSERT される。
     */
    @Test
    public void keyOnlyInLaterRowIsIgnoredAndItsValueIsDiscarded() throws Exception {
        List<String> cols = Arrays.asList(
                sut.getSetupTableData(DIR, RES, "s7").get(0).getColumnNames());
        assertThat("2行目にしか無い VARCHAR2_COL はカラム名に入らないこと: " + cols,
                cols.contains("VARCHAR2_COL"), is(false));
        setUp("s7");
        assertThat("2行目に書いた \"LATER\" は捨てられデフォルト値になること",
                raw("00002", "VARCHAR2_COL"), is((Object) " "));
    }

    /** カラム名は空エントリ（{}）除去のあとに残った先頭行のキーだけで決まる。行の並びだけが違うと (1)/(2) の帰属が変わる。 */
    @Test
    public void columnNamesDependOnRowOrderAfterBlankRowRemoval() {
        List<String> a = Arrays.asList(sut.getSetupTableData(DIR, RES, "s4a").get(0).getColumnNames());
        List<String> b = Arrays.asList(sut.getSetupTableData(DIR, RES, "s4b").get(0).getColumnNames());
        assertThat("先頭の空エントリ（{}）は除去されカラム名決定に使われないこと",
                a.contains("NULL_COL"), is(false));
        assertThat(b.contains("NULL_COL"), is(false));
        assertThat("s4a は VARCHAR2_COL を含むこと", a.contains("VARCHAR2_COL"), is(true));
        assertThat("s4b は VARCHAR2_COL を含まないこと", b.contains("VARCHAR2_COL"), is(false));
        assertThat("並びが違えばカラム名集合が変わること", a, is(not(b)));
    }

    /** 上記の帰結: 同じ値を書いても行の並びで INSERT される値が変わる。 */
    @Test
    public void insertedValueDependsOnRowOrder() throws Exception {
        setUp("s4a");
        assertThat("s4a: 1行目は AAA", raw("00001", "VARCHAR2_COL"), is((Object) "AAA"));
        assertThat("s4a: 2行目は (2) なので NULL", raw("00002", "VARCHAR2_COL"), is(nullValue()));

        setUp("s4b");
        assertThat("s4b: VARCHAR2_COL はカラム名に無いので AAA と書いても捨てられデフォルト値になる",
                raw("00001", "VARCHAR2_COL"), is((Object) " "));
        assertThat("s4b: 00002 もデフォルト値", raw("00002", "VARCHAR2_COL"), is((Object) " "));
    }

    /** (1): setup_tables でデフォルト値が補完されて INSERT される（FK ブロックの数値型 "0" を含む）。 */
    @Test
    public void setupFillsDefaultForAllRowOmittedColumn() throws Exception {
        setUp("s1");
        assertThat("可変長文字はデフォルト \" \"", raw("00001", "VARCHAR2_COL"), is((Object) " "));
        assertThat("NULL_COL もデフォルト \" \"", raw("00001", "NULL_COL"), is((Object) " "));
        assertThat("Boolean はデフォルト false", raw("00001", "BOOL_COL"), is((Object) Boolean.FALSE));

        setUp("s0");
        assertThat("数値型はデフォルト 0", raw("00001", "NUMBER_COL").toString(), is("0"));
    }

    /** (1): expected_tables で比較対象に入らない。 */
    @Test
    public void expectedTablesIgnoresAllRowOmittedColumn() {
        seed("00001", 1L, "ZZZ", 7L);   // DB の VARCHAR2_COL は "ZZZ"
        assertThat("VARCHAR2_COL を書かない e1 は通ること", assertionFires(expected("e1")), is(false));
        assertThat("VARCHAR2_COL を書いた e1b は落ちること（比較が効いている証拠）",
                assertionFires(expected("e1b")), is(true));
    }

    /** (1): expected_complete_tables でデフォルト値補完のうえ全カラム比較される。 */
    @Test
    public void expectedCompleteTablesFillsDefaultAndComparesAllColumns() throws Exception {
        seedAllDefaults("00001", 1L, " ");   // PK 以外は BasicDefaultValues と同じ値
        TableData c1 = expected("c1");
        assertThat("fillDefaultValues で DB 全カラムになること", c1.getColumnNames().length, is(11));
        assertThat("補完された NUMBER_COL は \"0\"", c1.getValue(0, "NUMBER_COL").toString(), is("0"));
        assertThat("デフォルト同士なので通ること", assertionFires(c1), is(false));

        // 全カラム比較であることの反証: 書いていないカラムを1つだけ変えると落ちること
        exec("UPDATE TEST_TABLE SET NUMBER_COL = 99 WHERE PK_COL1 = '00001'");
        assertThat("省略カラムを変えると落ちること", assertionFires(expected("c1")), is(true));
    }

    // ------------------------------------------------------------- (2) の挙動

    /** (2): カラム名決定行にあり個々の行で省略したカラムは、値 null として保持される（キー自体は無くならない）。 */
    @Test
    public void rowLevelOmittedColumnStaysInColumnNames() {
        List<TableData> tables = sut.getSetupTableData(DIR, RES, "s2");
        TableData td = tables.get(0);
        assertThat(Arrays.asList(td.getColumnNames()).contains("VARCHAR2_COL"), is(true));
        assertThat("1行目は値を持つこと", td.getValue(0, "VARCHAR2_COL"), is((Object) "AAA"));
        assertThat("2行目は null になること", td.getValue(1, "VARCHAR2_COL"), is(nullValue()));
    }

    /** (2): NULL 許容カラムでは、setup_tables はデフォルト値補完を行わず NULL を INSERT する。 */
    @Test
    public void setupInsertsNullForRowLevelOmittedNullableColumn() throws Exception {
        setUp("s2");
        assertThat("1行目は値どおり", raw("00001", "VARCHAR2_COL"), is((Object) "AAA"));
        assertThat("2行目は NULL（デフォルト \" \" ではない）",
                raw("00002", "VARCHAR2_COL"), is(nullValue()));
    }

    /** (2): NOT NULL カラムでは、補完されず NULL のまま INSERT が試みられ失敗する。 */
    @Test
    public void setupFailsWhenRowLevelOmittedColumnIsNotNull() {
        try {
            setUp("s3");
            fail("NOT NULL カラムに NULL を INSERT したのに例外が出なかった");
        } catch (RuntimeException e) {
            Throwable t = e;
            StringBuilder sb = new StringBuilder();
            while (t != null) {
                sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append(" | ");
                t = t.getCause();
            }
            assertTrue("DB の NOT NULL 制約違反（SQLState 23502）であること: " + sb,
                    sb.toString().contains("23502"));
        }
    }

    /**
     * (2) の例外: Boolean 型カラムは NULL を扱えず {@link NullPointerException} になる
     * （{@code TableData#insertData} が {@code row.getBoolean(columnName)} の結果を
     * unboxing してバインドするため）。NOT NULL 制約の有無に関わらず起きる
     * （{@code BOOL_COL} は NULL 許容カラムである）。
     */
    @Test
    public void setupThrowsNpeWhenRowLevelOmittedColumnIsBoolean() {
        try {
            setUp("s8");
            fail("Boolean 型カラムの行内省略で NullPointerException が出なかった");
        } catch (NullPointerException e) {
            // 期待どおり
        }
    }

    /**
     * (2) の例外は「行ごとの省略」に限らない: Boolean 型カラムにクォートなし小文字 {@code null} を
     * 明示した場合（省略ではなく明示的な記述）も同じ NullPointerException になる。
     * これは :108 の「行ごとに省略せず値を明示すること」という助言が、読み手が null を明示することで
     * 満たしてしまえる書き方になっており、その場合も NPE を防げないことの実証である。
     */
    @Test
    public void setupThrowsNpeWhenBooleanColumnIsExplicitNull() {
        try {
            setUp("s10");
            fail("Boolean 型カラムに明示的な null を書いても NullPointerException が出なかった");
        } catch (NullPointerException e) {
            // 期待どおり
        }
    }

    /**
     * クォート付き {@code "null"} は Java null にならないため (2) の例外に当たらない: yamlInterpreters は
     * NullInterpreter を含まないため、クォート付き {@code "null"} は文字列のまま Boolean 型カラムへ
     * 渡され、NullPointerException にならずに INSERT できる。クォートなし小文字 {@code null} を明示した
     * 場合（{@code setupThrowsNpeWhenBooleanColumnIsExplicitNull}）との違いを固定する。
     */
    @Test
    public void setupSucceedsWhenBooleanColumnIsQuotedNullString() throws Exception {
        // When: NullPointerException にならずに INSERT できること
        setUp("s11");

        // Then: Java null ではない値が INSERT されること
        assertThat("クォート付き \"null\" は Java null にならないこと",
                raw("00001", "BOOL_COL"), is((Object) Boolean.FALSE));
    }

    /**
     * (2) と「クォートなしの null を明示した場合」は同じ扱いになる
     * （{@code YamlSection} の値解決を通ると、キー省略も {@code COL: null} も同じ Java null になる）。
     */
    @Test
    public void rowLevelOmittedColumnIsEquivalentToExplicitNull() throws Exception {
        setUp("s2");   // 2行目は VARCHAR2_COL を省略
        Object omitted = raw("00002", "VARCHAR2_COL");

        VariousDbTestHelper.delete(TestTable.class);
        setUp("s9");   // 2行目は VARCHAR2_COL: null を明示
        Object explicitNull = raw("00002", "VARCHAR2_COL");

        assertThat("行内省略も明示的な null も同じ結果（NULL）になること",
                omitted, is(explicitNull));
        assertThat(explicitNull, is(nullValue()));
    }

    /** (2): expected_tables では期待値 null として比較される（比較対象から外れない）。 */
    @Test
    public void expectedTablesComparesRowLevelOmittedColumnAsNull() throws Exception {
        // DB は YAML 経路を通さずに直接作る（期待値側の変異を検知できるようにするため）
        seed("00001", 1L, "AAA", 7L);
        seed("00002", 2L, null, 8L);
        assertThat("DB が NULL なら通ること", assertionFires(expected("e2")), is(false));

        exec("UPDATE TEST_TABLE SET VARCHAR2_COL = 'XXX' WHERE PK_COL1 = '00002'");
        assertThat("DB を非 NULL にすると落ちること（比較対象から外れていない証拠）",
                assertionFires(expected("e2")), is(true));
    }

    /** (2): expected_complete_tables でも補完されず null のまま比較される。 */
    @Test
    public void expectedCompleteTablesKeepsRowLevelOmittedColumnNull() throws Exception {
        // DB は YAML 経路を通さずに直接作る（PK と VARCHAR2_COL 以外は BasicDefaultValues と同じ値）
        seedAllDefaults("00001", 1L, "AAA");
        seedAllDefaults("00002", 2L, null);
        assertThat("2行目 VARCHAR2_COL は NULL であること", raw("00002", "VARCHAR2_COL"), is(nullValue()));

        TableData c2 = expected("c2");
        assertThat("fillDefaultValues 後も 11 カラム", c2.getColumnNames().length, is(11));
        assertThat("(2) のカラムは補完されず null のまま", c2.getValue(1, "VARCHAR2_COL"), is(nullValue()));
        assertThat("(1) のカラムは補完される", c2.getValue(1, "NUMBER_COL").toString(), is("0"));
        assertThat("null 同士なので通ること", assertionFires(c2), is(false));

        exec("UPDATE TEST_TABLE SET VARCHAR2_COL = ' ' WHERE PK_COL1 = '00002'");
        assertThat("DB を \" \"（デフォルト値）にすると落ちること = 補完されていない証拠",
                assertionFires(expected("c2")), is(true));
    }
}
