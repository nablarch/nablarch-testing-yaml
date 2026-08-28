package nablarch.test.core.db;

import nablarch.core.db.connection.TransactionManagerConnection;
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
import java.sql.Timestamp;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * 日付の記述形式（{@code yyyyMMddHHmmssSSS} の後置0埋め省略）を YAML 経由の実経路で固定するテストクラス。
 *
 * <p>
 * 解説書 {@code implementation/testdata_notation.rst:1328}-{@code :1333} は、日付を
 * {@code yyyyMMddHHmmssSSS} 形式または JDBC タイムスタンプエスケープ形式で記述でき、
 * 時刻のミリ秒または全部を後置0埋めの形で省略できると述べている。ミリ秒を省略した場合はミリ秒0、
 * 時刻全部を省略した場合は0時0分0秒000として扱われ、例として {@code 20210123123456} は
 * 「2021年1月23日 12時34分56秒000」と評価されるとしている。
 * </p>
 *
 * <p>
 * 日付型カラムへの値の評価は INSERT 時に行われるため、YAML → {@link YamlTestDataParser} →
 * {@link TableData} → DB の実経路を通し、DB に入った値を読み戻して確認する。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlDateNotationTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource =
            new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String DIR = "src/test/java/nablarch/test/core/db/";
    private static final String RES = "YamlDateNotationTest/date";

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

    /** 指定グループの setup_tables を DB へ反映する。 */
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

    /** 生の JDBC で TIMESTAMP_COL を読む。 */
    private static Timestamp rawTimestamp(String pk1) throws Exception {
        try (Connection c = VariousDbTestHelper.getNativeConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT TIMESTAMP_COL FROM TEST_TABLE WHERE PK_COL1 = ?")) {
            ps.setString(1, pk1);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("行が存在すること pk=" + pk1, rs.next());
                return rs.getTimestamp(1);
            }
        }
    }

    // ------------------------------------------------------------------ test

    /**
     * 時刻のミリ秒を省略した {@code "20210123123456"} が「2021年1月23日 12時34分56秒000」と評価されること。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:1328}-{@code :1333}<br>
     * Given: setup_tables の TIMESTAMP_COL に {@code "20210123123456"}（ミリ秒を省略した yyyyMMddHHmmss）<br>
     * When:  YAML を読み込んで DB へ INSERT する<br>
     * Then:  DB の値が {@code 2021-01-23 12:34:56.000} になること
     * </p>
     */
    @Test
    public void omittedMillisIsFilledWithZero() throws Exception {
        // Given / When
        setUp("omitMillis");

        // Then
        assertThat("ミリ秒を省略した場合はミリ秒0として扱われること",
                rawTimestamp("00001"), is(Timestamp.valueOf("2021-01-23 12:34:56.000")));
    }

    /**
     * 時刻を全部省略した {@code "20210123"} が「2021年1月23日 0時0分0秒000」と評価されること。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:1328}-{@code :1333}<br>
     * Given: setup_tables の TIMESTAMP_COL に {@code "20210123"}（時刻を全部省略した yyyyMMdd）<br>
     * When:  YAML を読み込んで DB へ INSERT する<br>
     * Then:  DB の値が {@code 2021-01-23 00:00:00.000} になること
     * </p>
     */
    @Test
    public void omittedTimeIsFilledWithZero() throws Exception {
        // Given / When
        setUp("omitTime");

        // Then
        assertThat("時刻全部を省略した場合は0時0分0秒000として扱われること",
                rawTimestamp("00002"), is(Timestamp.valueOf("2021-01-23 00:00:00.000")));
    }
}
