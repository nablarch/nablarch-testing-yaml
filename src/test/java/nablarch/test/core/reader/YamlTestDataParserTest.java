package nablarch.test.core.reader;

import nablarch.core.dataformat.DataRecord;
import nablarch.test.Assertion;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.DataFileFragment;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.reader.yaml.YamlSchemaValidationException;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.log.app.OnMemoryLogWriter;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * {@link YamlTestDataParser} のテストクラス。
 *
 * <p>
 * {@link TestDataReader#readLine()} が終端で null を返す仕様は {@link TestDataReader} 実装側のものであり、
 * {@code YamlTestDataParser} は {@link TestDataReader} を使用しないため対象外。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlTestDataParserTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";

    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/";

    /** モックアップクラスが読む同期応答メッセージ送信のテストデータのベースディレクトリ。 */
    private static final String SEND_SYNC_DIR = DIR + "YamlTestDataParserTest/sendSyncTestData";

    /** TestDataParser を直接使って読む、テストコードと別のディレクトリ。 */
    private static final String OTHER_DIR = DIR + "YamlTestDataParserTest/otherDir";

    private YamlTestDataParser sut;

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @Before
    public void before() {
        DbInfo dbInfo = repositoryResource.getComponent("dbInfo");
        DefaultValues defaultValues = new BasicDefaultValues();
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> interpreters =
                repositoryResource.getComponent("yamlInterpreters");

        sut = new YamlTestDataParser();
        sut.setDbInfo(dbInfo);
        sut.setDefaultValues(defaultValues);
        sut.setInterpreters(interpreters);
    }

    @After
    public void after() {
        // static YAML_CACHE をリセットしてテスト間の汚染を防ぐ（B-5）
        YamlTestDataParser.clearCacheForTest();
    }

    // ========================================================================
    // {dataName}.yaml ファイルを検索する
    // ========================================================================

    /**
     * getSetupTableData: .yaml ファイルを path/resourceName.yaml として開けること。
     *
     * <p>
     * Given: YAML ファイルが path/resourceName.yaml として配置されている<br>
     * When:  getSetupTableData(dir, "YamlTestDataParserTest/tableData") を呼ぶ<br>
     * Then:  setup_tables のデータが取得できること
     * </p>
     */
    @Test
    public void getSetupTableDataLoadsYamlFile() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/tableData");

        // Then: グループID なしの 1 件が取得される
        assertThat(result.size(), is(1));
        TableData td = result.get(0);
        assertThat(td.getTableName(), is("TEST_TABLE"));
        assertThat(td.getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    // ========================================================================
    // isResourceExisting（入れ物単位）
    // ========================================================================

    /**
     * isResourceExisting: 入れ物（basePath/クラス名 ディレクトリ）が存在する場合は true を返すこと。
     *
     * <p>
     * Given: 入れ物ディレクトリ YamlTestDataParserTest と、その配下の existingForTest.yaml<br>
     * When:  isResourceExisting(dir, "YamlTestDataParserTest/existingForTest") を呼ぶ<br>
     * Then:  true が返ること
     * </p>
     */
    @Test
    public void isResourceExistingReturnsTrueWhenContainerExists() {
        // Given / When / Then
        assertTrue(sut.isResourceExisting(DIR, "YamlTestDataParserTest/existingForTest"));
    }

    /**
     * isResourceExisting: 判定単位が入れ物であること（読み込み単位の YAML が無くても
     * 入れ物ディレクトリがあれば true）を担保する。
     *
     * <p>
     * これにより、{@code setUpDb.yaml} を置いていないテストクラスでも
     * {@code TestSupport#getPathOf} がテストデータのパスを解決できる。
     * </p>
     *
     * <p>
     * Given: 入れ物ディレクトリ YamlTestDataParserTest は存在するが setUpDb.yaml は存在しない<br>
     * When:  isResourceExisting(dir, "YamlTestDataParserTest/setUpDb") を呼ぶ<br>
     * Then:  true が返ること
     * </p>
     */
    @Test
    public void isResourceExistingReturnsTrueWhenReadUnitMissingButContainerExists() {
        // Given / When / Then
        assertTrue(sut.isResourceExisting(DIR, "YamlTestDataParserTest/setUpDb"));
    }

    /**
     * isResourceExisting: 入れ物ディレクトリが存在しない場合は false を返すこと。
     *
     * <p>
     * Given: 存在しない入れ物名<br>
     * When:  isResourceExisting を呼ぶ<br>
     * Then:  false が返ること
     * </p>
     */
    @Test
    public void isResourceExistingReturnsFalseWhenContainerNotExists() {
        // Given / When / Then
        assertFalse(sut.isResourceExisting(DIR, "NoSuchTestClass/existingForTest"));
    }

    // ========================================================================
    // null 返却後の最終セクションデータ欠落防止
    // ========================================================================

    /**
     * getExpectedFile: YAML 末尾セクション（expected_files）のデータが欠落しないこと。
     *
     * <p>
     * Given: setup_files に続いて expected_files が YAML ファイル末尾に記述されている<br>
     * When:  getExpectedFile を呼ぶ<br>
     * Then:  末尾セクション（expected_files）のデータが欠落せずに取得できること
     * </p>
     */
    @Test
    public void lastSectionDataNotLostAtEndOfFile() {
        // Given / When
        List<DataFile> result = sut.getExpectedFile(DIR, "YamlTestDataParserTest/fileData");

        // Then: 末尾セクションのデータが欠落していないこと
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    // ========================================================================
    // YAML ネイティブ null は Java null
    // YAML ネイティブ boolean は文字列化
    // YAML ネイティブ integer/float は文字列化
    // ========================================================================

    /**
     * getListMap: YAML ネイティブ null は Java null として取得されること。
     *
     * <p>
     * Given: NULL_COL の値が YAML ネイティブ null（アンクォート）<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  NULL_COL の値が Java null であること
     * </p>
     */
    @Test
    public void yamlNativeNullIsJavaNull() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/nativeTypes", "nativeTypeTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertNull(row.get("NULL_COL"));
    }

    /**
     * getListMap: YAML ネイティブ boolean は文字列 "true"/"false" として取得されること。
     *
     * <p>
     * Given: BOOL_TRUE が YAML ネイティブ boolean true、BOOL_FALSE が false（クォートなし）<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  それぞれ文字列 "true", "false" として取得されること
     * </p>
     */
    @Test
    public void yamlNativeBooleanIsStringified() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/nativeTypes", "nativeTypeTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat(row.get("BOOL_TRUE"), is("true"));
        assertThat(row.get("BOOL_FALSE"), is("false"));
    }

    /**
     * getListMap: YAML ネイティブ integer/float は文字列として取得されること。
     *
     * <p>
     * Given: INT_COL が YAML ネイティブ整数 42、FLOAT_COL が 3.14（クォートなし）<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  それぞれ文字列 "42", "3.14" として取得されること
     * </p>
     */
    @Test
    public void yamlNativeNumberIsStringified() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/nativeTypes", "nativeTypeTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat(row.get("INT_COL"), is("42"));
        assertThat(row.get("FLOAT_COL"), is("3.14"));
    }

    /**
     * getListMap: YAML 科学的記数法（1e10）は文字列として取得されること。
     *
     * <p>
     * Given: FLOAT_SCIENTIFIC が YAML ネイティブ 1e10（SnakeYAML が Double 1.0E10 として解釈）<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  Java の {@code Double.toString(1.0E10)} の出力（"1.0E10"）として取得されること
     * </p>
     */
    @Test
    public void yamlScientificNotationIsStringified() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/nativeTypes", "nativeTypeTest");

        // Then: Java の Double.toString(1e10) = "1.0E10"
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat(row.get("FLOAT_SCIENTIFIC"), is(Double.toString(1e10)));
    }

    // ========================================================================
    // YAML 1.2 Core Schema: yes / no / on / off は文字列
    // ${attach:ファイルパス}
    // ========================================================================

    /**
     * getListMap: クォートなしの {@code yes}/{@code no}/{@code on}/{@code off} が、キーでも値でも文字列のままになること。
     *
     * <p>
     * 何を担保するか: YAML ファイルが YAML 1.2 に準拠し、YAML 1.1 のように
     * {@code yes}/{@code no}/{@code on}/{@code off} を真偽値へ型変換しないこと。<br>
     * 根拠: implementation/testdata_notation.rst:92<br>
     * Given: list_maps の 1 行に、キーも値もクォートなしの no / yes / on / off を書いた YAML<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  キーが no / yes / on / off の 4 つのまま取得でき、値もそれぞれ同じ文字列になること
     *        （YAML 1.1 ならキーが true / false の 2 つに縮退し、値も "true" / "false" になる）
     * </p>
     */
    @Test
    public void yaml12BooleanWordsAreStringsAsKeysAndValues() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(
                DIR, "YamlTestDataParserTest/nativeTypes", "yaml12BooleanWordTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat("キーが真偽値へ型変換されず 4 つのまま残ること: " + row.keySet(), row.size(), is(4));
        assertThat("キー no が文字列のまま残ること: " + row.keySet(), row.containsKey("no"), is(true));
        assertThat("キー yes が文字列のまま残ること: " + row.keySet(), row.containsKey("yes"), is(true));
        assertThat("キー on が文字列のまま残ること: " + row.keySet(), row.containsKey("on"), is(true));
        assertThat("キー off が文字列のまま残ること: " + row.keySet(), row.containsKey("off"), is(true));
        assertThat("値 no が文字列 \"no\" になること", row.get("no"), is("no"));
        assertThat("値 yes が文字列 \"yes\" になること", row.get("yes"), is("yes"));
        assertThat("値 on が文字列 \"on\" になること", row.get("on"), is("on"));
        assertThat("値 off が文字列 \"off\" になること", row.get("off"), is("off"));
    }

    /**
     * getListMap: 解説書の実例どおり、testShots のカラム {@code no} をクォートなしのキーで書いても文字列キーになること。
     *
     * <p>
     * 何を担保するか: 解説書のバッチテスト例がクォートなしの {@code - no: "1"} を使っており、
     * YAML 1.1 ならキーが真偽値 false になってしまうところ、YAML 1.2 では文字列キー {@code no} のまま
     * 読めること。<br>
     * 根拠: implementation/deal_unit_test/batch.rst:352（実例）、implementation/testdata_notation.rst:92<br>
     * Given: list_maps の 1 行に、クォートなしのキー no と値 "1" を書いた YAML<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  キー "no" で値 "1" が取得でき、キー "false" にはならないこと
     * </p>
     */
    @Test
    public void unquotedNoKeyStaysStringKey() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(
                DIR, "YamlTestDataParserTest/nativeTypes", "unquotedNoKeyTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat("クォートなしのキー no が文字列キーのまま残ること: " + row.keySet(),
                row.containsKey("no"), is(true));
        assertThat("YAML 1.1 のようにキーが false へ型変換されないこと: " + row.keySet(),
                row.containsKey("false"), is(false));
        assertThat(row.get("no"), is("1"));
        assertThat(row.get("description"), is("ファイル入力"));
        assertThat(row.get("expectedStatusCode"), is("0"));
    }

    /**
     * getListMap: {@code "${attach:ファイルパス}"} がアップロードファイルの指定として読めること。
     *
     * <p>
     * 何を担保するか: HTTP リクエストパラメータの値に書いた {@code ${attach:ファイルパス}} が、
     * YAML の読み込み経路でそのままの表記として取り出せること。アップロードファイルの指定は
     * {@code HttpRequestTestSupport}（nablarch-testing）が値の表記 {@code ${attach:...}} を見て
     * 判定するため、YAML 経路が表記を保ったまま渡すことがその前提になる。<br>
     * 根拠: implementation/testdata_notation.rst:1339<br>
     * Given: list_maps の 1 行に uploadFile: "${attach:&lt;プロジェクトルートからの相対パス&gt;}" を書いた YAML<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  値が {@code ${attach:...}} の表記のまま取得でき、示すファイルが
     *        テスト実行時のカレントディレクトリ（プロジェクトルート）から見て存在すること
     * </p>
     */
    @Test
    public void attachNotationIsReadableAsUploadFileSpecification() {
        // Given
        String path = "src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/test.bin";

        // When
        List<Map<String, String>> result = sut.getListMap(
                DIR, "YamlTestDataParserTest/nativeTypes", "attachNotationTest");

        // Then
        assertThat(result.size(), is(1));
        String value = result.get(0).get("uploadFile");
        assertThat("${attach:ファイルパス} の表記のまま取得できること", value, is("${attach:" + path + "}"));
        assertTrue("表記が ${attach: で始まること", value.startsWith("${attach:"));
        assertTrue("表記が } で終わること", value.endsWith("}"));
        assertTrue("示すファイルがカレントディレクトリからの相対パスで存在すること: " + path,
                new java.io.File(path).exists());
    }

    // ========================================================================
    // YAML ネイティブ null は Java null（末尾キー省略含む）
    // ========================================================================

    /**
     * getListMap: YAML ネイティブ null（明示記述）は Java null として取得されること。
     *
     * <p>
     * Given: rows の各行に COL2/COL3: null が明示的に含まれる YAML データ<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  null 値のカラムが Java null として返ること
     * </p>
     */
    @Test
    public void trailingNativeNullIsJavaNull() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/trailingNulls", "trailingNullTest");

        // Then
        assertThat(result.size(), is(2));

        // 1 行目の確認
        Map<String, String> row0 = result.get(0);
        assertThat(row0.get("COL1"), is("val1"));
        assertThat(row0.get("COL2"), is("val2"));
        // COL3: null → SnakeYAML が Java null に変換し、objectToString() がそのまま null を返す
        assertNull(row0.get("COL3"));

        // 2 行目の確認
        Map<String, String> row1 = result.get(1);
        assertThat(row1.get("COL1"), is("val4"));
        assertNull(row1.get("COL2"));
        assertNull(row1.get("COL3"));
    }

    /**
     * getListMap: YAML 後続行で末尾キーを省略した場合、省略キーの値は null として取得されること。
     *
     * <p>
     * Given: 2 行目に COL3 キーが省略されている list_maps エントリ<br>
     * When:  getListMap を呼ぶ<br>
     * Then:  2 行目の COL3 が null として取得されること
     * </p>
     */
    @Test
    public void trailingKeyOmittedIsNull() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/trailingNulls", "trailingKeyOmitTest");

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0).get("COL3"), is("row1_c"));
        // 2 行目は COL3 キーが YAML に記述されていない → Map に存在しないため null
        assertNull(result.get(1).get("COL3"));
    }

    // ========================================================================
    // getSetupTableData / getExpectedTableData（グループID 付き）
    // ========================================================================

    /**
     * getSetupTableData: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_tables に groupA / groupB のエントリがある<br>
     * When:  getSetupTableData(dir, resource, "groupA") を呼ぶ<br>
     * Then:  groupA の 1 件のみ返ること
     * </p>
     */
    @Test
    public void getSetupTableDataWithGroupId() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/tableData", "groupA");

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * getSetupTableData: 存在しないグループ ID を指定した場合に空リストが返ること。
     *
     * <p>
     * Given: 存在しないグループ ID<br>
     * When:  getSetupTableData を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void getSetupTableDataNotExist() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/tableData", "noSuchGroup");

        // Then
        assertThat(result.size(), is(0));
    }

    /**
     * getExpectedTableData: グループ ID 付きで取得できること。
     *
     * <p>
     * Given: expected_tables に groupA のエントリがある<br>
     * When:  getExpectedTableData(dir, resource, "groupA") を呼ぶ<br>
     * Then:  groupA の 1 件が返ること
     * </p>
     */
    @Test
    public void getExpectedTableDataWithGroupId() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/tableData", "groupA");

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * getExpectedTableData: グループ ID なしで全件取得できること。
     *
     * <p>
     * Given: expected_tables にグループ ID なしのエントリ<br>
     * When:  getExpectedTableData(dir, resource) を呼ぶ<br>
     * Then:  グループ ID なしの 1 件が返ること
     * </p>
     */
    @Test
    public void getExpectedTableDataWithoutGroupId() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/tableData");

        // Then: expected_tables（グループIDなし 1 件）のみ
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    /**
     * [BUG-F] getExpectedTableData: rows が空のとき DB に行があれば assertTableEquals が FAIL すること（偽陰性の防止）。
     *
     * <p>
     * Given: DB の TEST_TABLE に 1 件のレコードがある<br>
     * When:  expected_tables に rows: [] のエントリがある YAML で getExpectedTableData を呼び、
     *        Assertion.assertTableEquals を実行する<br>
     * Then:  AssertionError がスローされること（DB に行があるのに通り抜けない）
     * </p>
     */
    @Test
    public void emptyExpectedTable_failsWhenDbHasRows() {
        // Given: DB に 1 件挿入
        VariousDbTestHelper.insert(new TestTable("00001", 1L, "v", 1L,
                new java.math.BigDecimal("1.0"),
                java.sql.Date.valueOf("2024-01-01"),
                java.sql.Timestamp.valueOf("2024-01-01 00:00:00"), null, null, null, null));

        // When
        List<TableData> expected = sut.getExpectedTableData(
                DIR, "YamlTestDataParserTest/tableData", "emptyRows");

        // Then: 1 件返り、assertTableEquals が AssertionError をスローすること
        assertThat("1 件の TableData が返ること", expected.size(), is(1));
        boolean assertionFired = false;
        try {
            Assertion.assertTableEquals("空テーブル検証", expected.get(0));
        } catch (AssertionError e) {
            // OK: assertTableEquals が正しく FAIL した
            assertionFired = true;
        }
        assertTrue("DB に行があるのに assertTableEquals が通り抜けた（偽陰性）", assertionFired);
    }

    /**
     * [BUG-F] getExpectedTableData: expected_complete_tables に rows: [] のとき NPE にならないこと。
     *
     * <p>
     * Given: expected_complete_tables に rows: [] のエントリがある YAML<br>
     * When:  getExpectedTableData を呼ぶ<br>
     * Then:  NPE が発生せず TableData が返り、getColumnNames() が DB の全カラムを返すこと
     * </p>
     */
    @Test
    public void emptyExpectedCompleteTable_noNpe() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(
                DIR, "YamlTestDataParserTest/emptyCompleteTable");

        // Then
        assertThat("1 件の TableData が返ること", result.size(), is(1));
        String[] cols = result.get(0).getColumnNames();
        assertThat("getColumnNames() が DB の全カラム（11 列）を返すこと", cols.length, is(11));
    }

    /**
     * getExpectedTableData: ファイルが存在しない場合は IllegalStateException がスローされること。
     *
     * <p>
     * Given: 存在しない YAML ファイルのリソース名<br>
     * When:  getExpectedTableData を呼ぶ<br>
     * Then:  IllegalStateException がスローされること
     * </p>
     */
    @Test(expected = IllegalStateException.class)
    public void getExpectedTableDataThrowsWhenFileNotExists() {
        // Given / When / Then
        sut.getExpectedTableData(DIR, "YamlTestDataParserTest/noSuchFile");
    }

    // ========================================================================
    // getListMap
    // ========================================================================

    /**
     * getListMap: 指定 ID のデータが取得できること。
     *
     * <p>
     * Given: list_maps に id=testListMap が 2 行<br>
     * When:  getListMap(dir, resource, "testListMap") を呼ぶ<br>
     * Then:  2 行のデータが返ること
     * </p>
     */
    @Test
    public void getListMap() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/tableData", "testListMap");

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0).get("KEY1"), is("val1"));
        assertThat(result.get(0).get("KEY2"), is("val2"));
        assertThat(result.get(1).get("KEY1"), is("val3"));
        assertThat(result.get(1).get("KEY2"), is("val4"));
    }

    // ========================================================================
    // getSetupFile / getExpectedFile
    // ========================================================================

    /**
     * getSetupFile: 固定長ファイルと可変長ファイルが取得できること。
     *
     * <p>
     * Given: setup_files に fixed と variable の 2 エントリ<br>
     * When:  getSetupFile を呼ぶ<br>
     * Then:  FixedLengthFile と VariableLengthFile の 2 件が返ること
     * </p>
     */
    @Test
    public void getSetupFile() {
        // Given / When
        List<DataFile> result = sut.getSetupFile(DIR, "YamlTestDataParserTest/fileData");

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    /**
     * getSetupFile: 取得した DataFile の path が正しく設定されていること。
     *
     * <p>
     * Given: setup_files に path=dummy/setup_fixed.dat のエントリ<br>
     * When:  getSetupFile を呼ぶ<br>
     * Then:  getPath() が "dummy/setup_fixed.dat" を返すこと
     * </p>
     */
    @Test
    public void getSetupFileHasCorrectPath() {
        // Given / When
        List<DataFile> result = sut.getSetupFile(DIR, "YamlTestDataParserTest/fileData");

        // Then
        assertThat(result.get(0).getPath(), is("dummy/setup_fixed.dat"));
        assertThat(result.get(1).getPath(), is("dummy/setup_variable.csv"));
    }

    /**
     * getSetupFile: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_files に grp1 のエントリがある<br>
     * When:  getSetupFile(dir, resource, "grp1") を呼ぶ<br>
     * Then:  grp1 の 1 件のみ返ること
     * </p>
     */
    @Test
    public void getSetupFileWithGroupId() {
        // Given / When
        List<DataFile> result = sut.getSetupFile(DIR, "YamlTestDataParserTest/fileData", "grp1");

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
    }

    /**
     * getExpectedFile: 固定長ファイルと可変長ファイルが取得できること。
     *
     * <p>
     * Given: expected_files に fixed と variable の 2 エントリ<br>
     * When:  getExpectedFile を呼ぶ<br>
     * Then:  FixedLengthFile と VariableLengthFile の 2 件が返ること
     * </p>
     */
    @Test
    public void getExpectedFile() {
        // Given / When
        List<DataFile> result = sut.getExpectedFile(DIR, "YamlTestDataParserTest/fileData");

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    /**
     * getExpectedFile: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_files と同構造で expected_files にも grp1 のエントリを追加したテストデータ<br>
     * When:  getExpectedFile(dir, resource, "grp1") を呼ぶ<br>
     * Then:  grp1 の 1 件のみ返ること
     * </p>
     */
    @Test
    public void getExpectedFileWithGroupId() {
        // Given / When
        List<DataFile> result = sut.getExpectedFile(DIR, "YamlTestDataParserTest/fileDataWithGroup", "grp1");

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
    }

    /**
     * getExpectedFile: 取得した DataFile の path が正しく設定されていること。
     *
     * <p>
     * Given: expected_files に path=dummy/expected_fixed.dat のエントリ<br>
     * When:  getExpectedFile を呼ぶ<br>
     * Then:  getPath() が "dummy/expected_fixed.dat" を返すこと
     * </p>
     */
    @Test
    public void getExpectedFileHasCorrectPath() {
        // Given / When
        List<DataFile> result = sut.getExpectedFile(DIR, "YamlTestDataParserTest/fileData");

        // Then
        assertThat(result.get(0).getPath(), is("dummy/expected_fixed.dat"));
        assertThat(result.get(1).getPath(), is("dummy/expected_variable.csv"));
    }

    // ========================================================================
    // getMessage
    // ========================================================================

    /**
     * getMessage: メッセージが取得でき、FW ヘッダ値（requestId・userId）が設定されていること。
     *
     * <p>
     * Given: messages の fw_header: マップに requestId="0000000001", userId="testUser01" が含まれる<br>
     * When:  getMessage を呼ぶ<br>
     * Then:  MessagePool が返り、requestId と userId が extractFwHeader で正しく抽出されていること
     * </p>
     */
    @Test
    public void getMessage() throws Exception {
        // Given / When
        MessagePool result = sut.getMessage(DIR, "YamlTestDataParserTest/messageData", "req001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));

        // FW ヘッダ実値の検証: MessagePool.getFwHeader() はパッケージプライベートのため
        // リフレクションで fwHeader フィールドを直接取得して検証する
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);

        assertThat("requestId が設定されていること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("userId が設定されていること", fwHeader.get("userId"), is("testUser01"));
        assertThat("resendFlag が設定されていること", fwHeader.get("resendFlag"), is("0"));
        assertThat("resultCode が設定されていること", fwHeader.get("resultCode"), is("0000"));
    }

    // ========================================================================
    // getMessageWithoutCache（SendSyncMessageParser 相当）
    // ========================================================================

    /**
     * getMessageWithoutCache(EXPECTED_REQUEST_BODY_MESSAGES): メッセージが取得できること。
     *
     * <p>
     * Given: expected_request_body_messages に id=req001 と SEARCH_KEY フィールドがある<br>
     * When:  getMessageWithoutCache(dir, resource, EXPECTED_REQUEST_BODY_MESSAGES, "req001") を呼ぶ<br>
     * Then:  MessagePool が返ること
     * </p>
     */
    @Test
    public void getMessageWithoutCache_expectedRequestBodyMessages() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.EXPECTED_REQUEST_BODY_MESSAGES, "req001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    /**
     * getMessageWithoutCache(EXPECTED_REQUEST_HEADER_MESSAGES): 電文本文が記述どおりに取得できること。
     *
     * <p>
     * {@code record_type} に特別な予約値はなく、値が "FW_HEADER" のレコードも読み飛ばされない。
     * このセクションは {@code fw_header:} を使わず、ヘッダ項目も {@code records} の fields/rows に記述するため、
     * 読み飛ばされると期待電文が 0 件の空メッセージになってしまう。
     * また同期応答メッセージ送信の 4 データタイプでは記載した値がそのままレコード種別になるため、
     * "FW_HEADER" は単に "FW_HEADER" というレコード種別になる<br>
     * Given: expected_request_header_messages の id=req001 に record_type: FW_HEADER のレコードがあり、
     *        requestId/userId/resendFlag/resultCode の値行が 1 行記述されている<br>
     * When:  getMessageWithoutCache(dir, resource, EXPECTED_REQUEST_HEADER_MESSAGES, "req001") を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返り、期待電文 1 件のレコード種別が "FW_HEADER"、
     *        各フィールドに記述どおりの値が入っていること
     * </p>
     */
    @Test
    public void getMessageWithoutCache_expectedRequestHeaderMessages() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.EXPECTED_REQUEST_HEADER_MESSAGES, "req001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));

        // Then: 本文行の中身まで検証する（record_type: FW_HEADER が読み飛ばされていないこと）
        List<DataRecord> messages = ((RequestTestingMessagePool) result).getExpectedMessageList();
        assertThat("FW_HEADER レコードも読み飛ばされず期待電文が 1 件取得できること", messages.size(), is(1));
        DataRecord message = messages.get(0);
        assertThat("送信同期4データタイプでは record_type の記載値がそのままレコード種別になること",
                message.getRecordType(), is("FW_HEADER"));
        assertThat(message.getString("requestId"), is("0000000001"));
        assertThat(message.getString("userId"), is("testUser01"));
        assertThat(message.getString("resendFlag"), is("0"));
        assertThat(message.getString("resultCode"), is("0000"));
    }

    /**
     * getMessageWithoutCache(RESPONSE_BODY_MESSAGES): メッセージが取得できること。
     *
     * <p>
     * Given: response_body_messages に group_id=grp1, id=resp001, RESULT_CODE="0000" のエントリ<br>
     * When:  getMessageWithoutCache(dir, resource, RESPONSE_BODY_MESSAGES, "resp001") を呼ぶ<br>
     * Then:  MessagePool が返ること
     * </p>
     */
    @Test
    public void getMessageWithoutCache_responseBodyMessages() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.RESPONSE_BODY_MESSAGES, "resp001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    /**
     * getMessageWithoutCache(RESPONSE_HEADER_MESSAGES): メッセージが取得できること。
     *
     * <p>
     * Given: response_header_messages に group_id=grp1, id=resp001, requestId="0000000001" のエントリ<br>
     * When:  getMessageWithoutCache(dir, resource, RESPONSE_HEADER_MESSAGES, "resp001") を呼ぶ<br>
     * Then:  MessagePool が返ること
     * </p>
     */
    @Test
    public void getMessageWithoutCache_responseHeaderMessages() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.RESPONSE_HEADER_MESSAGES, "resp001");

        // Then: non-null かつ RequestTestingMessagePool であること
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    // ========================================================================
    // getSendSyncMessage（GroupMessageParser 相当）
    // ========================================================================

    /**
     * getSendSyncMessage: グループ ID 付きのメッセージリストが取得できること。
     *
     * <p>
     * Given: response_body_messages に group_id=grp1 のエントリ<br>
     * When:  getSendSyncMessage(dir, resource, "[grp1]", RESPONSE_BODY_MESSAGES) を呼ぶ<br>
     * Then:  RequestTestingMessagePool のリストが返ること
     * </p>
     */
    @Test
    public void getSendSyncMessage() {
        // Given / When
        List<RequestTestingMessagePool> result = sut.getSendSyncMessage(
                DIR, "YamlTestDataParserTest/messageData",
                "[grp1]", DataType.RESPONSE_BODY_MESSAGES);

        // Then
        assertNotNull(result);
        assertThat(result.size(), is(1));
    }

    /**
     * getSendSyncMessage: 存在しないグループ ID を指定した場合は null が返ること。
     *
     * <p>
     * Given: 存在しないグループ ID "noSuchGroup"<br>
     * When:  getSendSyncMessage を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void getSendSyncMessageReturnsNullForUnknownGroupId() {
        // Given / When
        List<RequestTestingMessagePool> result = sut.getSendSyncMessage(
                DIR, "YamlTestDataParserTest/messageData",
                "noSuchGroup", DataType.RESPONSE_BODY_MESSAGES);

        // Then
        assertNull(result);
    }

    // ========================================================================
    // record_type の値 "FW_HEADER" が特別扱いされないこと（公開 API 経路）
    // ========================================================================

    /**
     * getMessage: messages の records に record_type: FW_HEADER のレコードがあっても読み飛ばされず
     * フラグメントになり、FW 制御ヘッダは fw_header: マップから別途取得されること。
     *
     * <p>
     * {@code record_type} に特別な予約値はなく、フレームワーク制御ヘッダは {@code fw_header:} マップで記述する。
     * {@code messages}（MESSAGE）では記載した値は使われず、デフォルトのレコード種別（"default"）になる
     * （記載値がそのままレコード種別になるのは同期応答メッセージ送信の 4 データタイプのみ）<br>
     * 電文のレコードレイアウトは1つなので、records には record_type: FW_HEADER のレコードを1件だけ書く<br>
     * 根拠: implementation/testdata_notation.rst:1153, :1299<br>
     * Given: messages の id=fwHeaderRecordType001 が fw_header: マップ（requestId/userId）と
     *        record_type が "FW_HEADER" のレコード1件（10 バイト・値行 2 行）を持つ<br>
     * When:  getMessage(dir, resource, "fwHeaderRecordType001") を呼ぶ<br>
     * Then:  値行が読み飛ばされず電文本文になり（record_type は "default" になる）、
     *        fw_header: の値は本文に混ざらず FW 制御ヘッダとして取得できること
     * </p>
     */
    @Test
    public void getMessage_fwHeaderRecordTypeIsNotSkipped() throws Exception {
        // Given / When
        MessagePool result = sut.getMessage(DIR, "YamlTestDataParserTest/messageData", "fwHeaderRecordType001");

        // Then: record_type: FW_HEADER のレコードも読み飛ばされず電文本文になること
        assertNotNull(result);
        List<DataRecord> messages = ((RequestTestingMessagePool) result).getExpectedMessageList();
        assertThat("FW_HEADER レコードも読み飛ばされず 2 件の電文本文になること", messages.size(), is(2));
        assertThat("messages では記載値が使われず \"default\" になること",
                messages.get(0).getRecordType(), is("default"));
        assertThat("messages では記載値が使われず \"default\" になること",
                messages.get(1).getRecordType(), is("default"));
        assertThat(messages.get(0).getString("HEAD_KEY"), is("HEADKEY001"));
        assertThat(messages.get(1).getString("HEAD_KEY"), is("HEADKEY002"));

        // Then: fw_header: に書いた項目は本文フラグメントにならないこと
        assertThat("fw_header: の requestId は本文に混ざらないこと",
                messages.get(0).containsKey("requestId"), is(false));
        assertThat("fw_header: の requestId は本文に混ざらないこと",
                messages.get(1).containsKey("requestId"), is(false));

        // Then: FW 制御ヘッダは fw_header: マップから取得できること
        // （MessagePool#getFwHeader はパッケージプライベートのためリフレクションで取得する）
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("fw_header: の requestId が FW 制御ヘッダとして取得できること",
                fwHeader.get("requestId"), is("0000000002"));
        assertThat("fw_header: の userId が FW 制御ヘッダとして取得できること",
                fwHeader.get("userId"), is("testUser02"));
    }

    /**
     * getSendSyncMessage: 送信同期経路でも record_type: FW_HEADER のレコードが読み飛ばされず
     * フラグメントになり、値行に連番が付与されること。
     *
     * <p>
     * "FW_HEADER" は予約値ではないため、送信同期経路では単に "FW_HEADER" というレコード種別になる。
     * 電文のレコードレイアウトは1つなので、records には record_type: FW_HEADER のレコードを1件だけ書く<br>
     * 根拠: implementation/testdata_notation.rst:1153, :1299<br>
     * Given: response_body_messages の group_id=fwHeaderSync に record_type が "FW_HEADER" の
     *        レコード1件（10 バイト・値行 2 行）がある<br>
     * When:  getSendSyncMessage(dir, resource, "[fwHeaderSync]", RESPONSE_BODY_MESSAGES) を呼ぶ<br>
     * Then:  FW_HEADER レコードの値行が読み飛ばされず 2 件の電文本文になり、記載どおりのレコード種別
     *        （"FW_HEADER"）が保持され、
     *        連番（FIRST_FIELD_NO）が 1 始まりで付与されていること
     * </p>
     */
    @Test
    public void getSendSyncMessage_fwHeaderRecordTypeIsNotSkipped() {
        // Given / When
        List<RequestTestingMessagePool> result = sut.getSendSyncMessage(
                DIR, "YamlTestDataParserTest/messageData",
                "[fwHeaderSync]", DataType.RESPONSE_BODY_MESSAGES);

        // Then
        assertNotNull(result);
        assertThat("エントリが 1 件返ること", result.size(), is(1));
        List<DataRecord> messages = result.get(0).getExpectedMessageList();
        assertThat("FW_HEADER レコードの値行も読み飛ばされず計 2 件になること", messages.size(), is(2));
        assertThat(messages.get(0).getString("HEAD_KEY"), is("HEADKEY001"));
        assertThat(messages.get(1).getString("HEAD_KEY"), is("HEADKEY002"));

        // Then: "FW_HEADER" は予約値ではなく、記載どおりのレコード種別になること
        assertThat("record_type: FW_HEADER が単に \"FW_HEADER\" というレコード種別になること",
                messages.get(0).getRecordType(), is("FW_HEADER"));
        assertThat("record_type: FW_HEADER が単に \"FW_HEADER\" というレコード種別になること",
                messages.get(1).getRecordType(), is("FW_HEADER"));

        // Then: 連番はフラグメント単位で 1 始まり（現行挙動）
        assertThat("FW_HEADER レコードの値行にも連番が付与されること",
                messages.get(0).getString(DataFileFragment.FIRST_FIELD_NO), is("1"));
        assertThat("2 行目の連番が \"2\" にインクリメントされること",
                messages.get(1).getString(DataFileFragment.FIRST_FIELD_NO), is("2"));
    }

    /**
     * getMessageWithoutCache: 同期応答メッセージ送信の 4 データタイプでは
     * {@code record_type} の記載値がそのままレコード種別になること。
     *
     * <p>
     * 電文のレコード種別の扱いはデータタイプによって異なる。{@code EXPECTED_REQUEST_HEADER_MESSAGES}／
     * {@code EXPECTED_REQUEST_BODY_MESSAGES}／{@code RESPONSE_HEADER_MESSAGES}／
     * {@code RESPONSE_BODY_MESSAGES} の 4 データタイプでは記載した値がそのままレコード種別になる。
     * "FW_HEADER" も予約値ではないため、単に "FW_HEADER" というレコード種別になる<br>
     * Given: messageData.yaml の送信同期 4 セクションに record_type がそれぞれ
     *        "FW_HEADER"／"BODY"／"HEADER"／"BODY" のエントリがある<br>
     * When:  各データタイプで getMessageWithoutCache を呼ぶ<br>
     * Then:  取得した電文のレコード種別が記載値と一致すること
     * </p>
     */
    @Test
    public void getMessageWithoutCache_recordTypeIsKeptForSendSyncDataTypes() {
        // Given
        final String resource = "YamlTestDataParserTest/messageData";

        // When / Then
        assertThat("expected_request_header_messages: 記載値 \"FW_HEADER\" が保持されること",
                firstRecordTypeOf(sut.getMessageWithoutCache(
                        DIR, resource, DataType.EXPECTED_REQUEST_HEADER_MESSAGES, "req001")),
                is("FW_HEADER"));
        assertThat("expected_request_body_messages: 記載値 \"BODY\" が保持されること",
                firstRecordTypeOf(sut.getMessageWithoutCache(
                        DIR, resource, DataType.EXPECTED_REQUEST_BODY_MESSAGES, "req001")),
                is("BODY"));
        assertThat("response_header_messages: 記載値 \"HEADER\" が保持されること",
                firstRecordTypeOf(sut.getMessageWithoutCache(
                        DIR, resource, DataType.RESPONSE_HEADER_MESSAGES, "resp001")),
                is("HEADER"));
        assertThat("response_body_messages: 記載値 \"BODY\" が保持されること",
                firstRecordTypeOf(sut.getMessageWithoutCache(
                        DIR, resource, DataType.RESPONSE_BODY_MESSAGES, "resp001")),
                is("BODY"));
    }

    /**
     * getMessageWithoutCache: {@code MESSAGE}（{@code messages}）では
     * {@code record_type} の記載値が使われず "default" になること。
     *
     * <p>
     * 送信同期 4 データタイプとの対比を固定する。{@code messages} は記載値によらず
     * デフォルトのレコード種別（"default"）になる<br>
     * Given: messages の id=req001 に record_type: BODY のレコードがある<br>
     * When:  getMessageWithoutCache(dir, resource, MESSAGE, "req001") を呼ぶ<br>
     * Then:  レコード種別が記載値 "BODY" ではなく "default" になること
     * </p>
     */
    @Test
    public void getMessageWithoutCache_recordTypeIsDefaultForMessages() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData", DataType.MESSAGE, "req001");

        // Then
        assertThat("messages では記載値 \"BODY\" が使われず \"default\" になること",
                firstRecordTypeOf(result), is("default"));
    }

    /**
     * メッセージプールの先頭電文のレコード種別を取り出す。
     *
     * @param pool メッセージプール
     * @return 先頭電文のレコード種別
     */
    private static String firstRecordTypeOf(MessagePool pool) {
        assertNotNull(pool);
        List<DataRecord> messages = ((RequestTestingMessagePool) pool).getExpectedMessageList();
        assertThat("電文が 1 件以上取得できること", messages.isEmpty(), is(false));
        return messages.get(0).getRecordType();
    }

    /**
     * getMessage: 旧形式（FW 制御ヘッダを record_type: FW_HEADER のレコードで表す書き方）が
     * messages に残っている場合、スキーマ検証で弾かれること。
     *
     * <p>
     * 何を担保するか: 旧形式の書き方は通らないこと。現仕様では FW 制御ヘッダは {@code fw_header:} マップで
     * 記述し、{@code record_type} に予約値はない。旧形式は FW_HEADER レコードと本文レコードの
     * 2 レコードレイアウトになるが、電文のレコードレイアウトは1つであり 2 つ以上記述するとエラーになるため、
     * 電文を組み立てる手前のスキーマ検証（{@code records} の {@code maxItems: 1}）で弾かれる<br>
     * 根拠: implementation/testdata_notation.rst:1153, :1299<br>
     * Given: messages の id=legacyFwHeaderRecord001 に旧形式の FW_HEADER レコード（25 バイト）と
     *        BODY レコード（10 バイト）の 2 レコードがある読み込み単位<br>
     * When:  getMessage(dir, resource, "legacyFwHeaderRecord001") を呼ぶ<br>
     * Then:  YamlSchemaValidationException がスローされ、メッセージに出所
     *        （ファイルパスと messages セクションの records のパス）が含まれること
     * </p>
     */
    @Test
    public void getMessage_legacyFwHeaderRecordIsRejectedBySchemaValidation() {
        // Given / When
        try {
            sut.getMessage(DIR, "YamlTestDataParserTest/legacyFwHeaderRecord", "legacyFwHeaderRecord001");
            fail("YamlSchemaValidationException が期待される");
        } catch (YamlSchemaValidationException e) {
            // Then
            assertThat("エラーメッセージにファイルパスが含まれること",
                    e.getMessage(), containsString("YamlTestDataParserTest/legacyFwHeaderRecord"));
            assertThat("エラーメッセージに出所（messages セクションの records）が含まれること",
                    e.getMessage(), containsString("messages[0].records"));
            assertThat("レコードレイアウトの上限超過として弾かれること（maxItems 違反）",
                    e.getErrors().get(0).getType(), is("maxItems"));
        }
    }

    // ========================================================================
    // getSetupTableData: 内部ガードは読み込み単位で判定する
    // ========================================================================

    /**
     * getSetupTableData: 入れ物ディレクトリが存在しても、読み込み単位の YAML ファイルが存在しない場合は
     * 空リストを返すこと（内部ガードが読み込み単位で判定されることを担保する）。
     *
     * <p>
     * Given: 入れ物ディレクトリ YamlTestDataParserTest は存在するが noSuchFile.yaml は存在しない<br>
     * When:  getSetupTableData を呼ぶ<br>
     * Then:  例外を送出せず空リストが返ること
     * </p>
     */
    @Test
    public void getSetupTableDataReturnsEmptyWhenReadUnitNotExists() {
        // Given: 入れ物は存在する
        assertTrue(sut.isResourceExisting(DIR, "YamlTestDataParserTest/noSuchFile"));

        // When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/noSuchFile");

        // Then
        assertThat(result.size(), is(0));
    }

    /**
     * getSetupTableData: 入れ物ディレクトリごと存在しない場合も空リストを返すこと。
     *
     * <p>
     * Given: 存在しない入れ物名<br>
     * When:  getSetupTableData を呼ぶ<br>
     * Then:  例外を送出せず空リストが返ること
     * </p>
     */
    @Test
    public void getSetupTableDataReturnsEmptyWhenContainerNotExists() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "NoSuchTestClass/noSuchFile");

        // Then
        assertThat(result.size(), is(0));
    }

    /**
     * getSetupTableData: リソース名に "/" を含まない場合、basePath 直下の YAML を読み込み単位として
     * 読み込めること（マスタデータ投入ツールの YAML 経路を担保する）。
     *
     * <p>
     * {@code MasterDataSetUpper} は Excel 形式以外のマスタデータファイルを
     * {@code getSetupTableData(<マスタデータディレクトリ>, <拡張子を除いたファイル名>)} で問い合わせる。
     * </p>
     *
     * <p>
     * Given: マスタデータディレクトリ直下の master-data.yaml<br>
     * When:  getSetupTableData(masterDataDir, "master-data") を呼ぶ<br>
     * Then:  setup_tables のデータが取得できること
     * </p>
     */
    @Test
    public void getSetupTableDataLoadsMasterDataFileWithoutSlash() {
        // Given
        String masterDataDir = DIR + "YamlTestDataParserTest/masterdata";

        // When
        List<TableData> result = sut.getSetupTableData(masterDataDir, "master-data");

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getTableName(), is("TEST_TABLE"));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000009001"));
    }

    /**
     * getSetupTableData: Excel 形式のマスタデータファイルを YAML 用パーサに問い合わせた場合、
     * 投入対象が 0 件になり、例外も警告も出ないこと。
     *
     * <p>
     * 解説書 {@code tools/master_data_tool.rst:28}（important）が述べる挙動を担保する。
     * {@code MasterDataSetUpper} は Excel 形式のマスタデータファイルに対して
     * {@code <ファイル名>/<シート名>} をリソース名として問い合わせるため、
     * 同名の YAML が無ければ空リストが返る。
     * </p>
     *
     * <p>
     * Given: マスタデータディレクトリに master-data-excel.xls 相当の YAML が存在しない<br>
     * When:  getSetupTableData(masterDataDir, "master-data-excel/Sheet1") を呼ぶ<br>
     * Then:  空リストが返り、WARN 以上のログが出力されないこと
     * </p>
     */
    @Test
    public void getSetupTableDataOnExcelMasterDataIsSilentlyEmpty() {
        // Given
        String masterDataDir = DIR + "YamlTestDataParserTest/masterdata";
        OnMemoryLogWriter.clear();

        // When
        List<TableData> result = sut.getSetupTableData(masterDataDir, "master-data-excel/Sheet1");

        // Then: 投入の対象が 0 件
        assertThat(result.size(), is(0));

        // Then: 例外も警告も出ない（WARN 以上のログが 1 件も無い）
        for (String message : OnMemoryLogWriter.getMessages("writer.memlog")) {
            assertThat(message, not(containsString("-WARN-")));
            assertThat(message, not(containsString("-ERROR-")));
            assertThat(message, not(containsString("-FATAL-")));
        }
    }

    // ========================================================================
    // setup_tables: rows が空のエントリは除外される
    // ========================================================================

    /**
     * getSetupTableData: rows が空（rows: []）のエントリは 0 行の TableData として返ること。
     *
     * <p>
     * Given: setup_tables に rows: [] のエントリ（emptyRows グループ）<br>
     * When:  getSetupTableData(dir, resource, "emptyRows") を呼ぶ<br>
     * Then:  サイズ 1 のリストが返り、その TableData の行数が 0 であること
     * </p>
     */
    @Test
    public void getSetupTableDataExcludesEmptyRows() {
        // Given / When
        List<TableData> result = sut.getSetupTableData(DIR, "YamlTestDataParserTest/tableData", "emptyRows");

        // Then: rows:[] エントリは 0 行の TableData として返る（Excel 経路の振る舞いに合わせる）
        assertThat(result.size(), is(1));
        assertThat(result.get(0).size(), is(0));
    }

    // ========================================================================
    // getListMap: 存在しない ID は空リストを返す
    // ========================================================================

    /**
     * getListMap: 存在しない ID を指定した場合は空リストが返ること。
     *
     * <p>
     * Given: list_maps に存在しない id<br>
     * When:  getListMap(dir, resource, "noSuchId") を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void getListMapReturnsEmptyWhenIdNotFound() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/tableData", "noSuchId");

        // Then
        assertThat(result.size(), is(0));
    }

    // ========================================================================
    // getListMap: マーカーカラム（[COL] 形式）は除外される
    // ========================================================================

    /**
     * getListMap: マーカーカラム（[COL] 形式）は結果の Map から除外されること。
     *
     * <p>
     * Given: list_maps に "[NO]" キーを含む行<br>
     * When:  getListMap(dir, resource, "markerColTest") を呼ぶ<br>
     * Then:  "[NO]" キーが結果に含まれず、通常カラムのみ返ること
     * </p>
     */
    @Test
    public void getListMapExcludesMarkerColumns() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(DIR, "YamlTestDataParserTest/tableData", "markerColTest");

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertFalse(row.containsKey("[NO]"));
        assertThat(row.get("KEY1"), is("val1"));
        assertThat(row.get("KEY2"), is("val2"));
    }

    // ========================================================================
    // getMessage / getMessageWithoutCache: 存在しない ID は null を返す
    // ========================================================================

    /**
     * getMessage: 存在しない ID を指定した場合は null が返ること。
     *
     * <p>
     * Given: messages に存在しない id<br>
     * When:  getMessage(dir, resource, "noSuchId") を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void getMessageReturnsNullWhenIdNotFound() {
        // Given / When
        MessagePool result = sut.getMessage(DIR, "YamlTestDataParserTest/messageData", "noSuchId");

        // Then
        assertNull(result);
    }

    /**
     * getMessageWithoutCache: 存在しない ID を指定した場合は null が返ること。
     *
     * <p>
     * Given: expected_request_body_messages に存在しない id<br>
     * When:  getMessageWithoutCache を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void getMessageWithoutCacheReturnsNullWhenIdNotFound() {
        // Given / When
        MessagePool result = sut.getMessageWithoutCache(
                DIR, "YamlTestDataParserTest/messageData",
                DataType.EXPECTED_REQUEST_BODY_MESSAGES, "noSuchId");

        // Then
        assertNull(result);
    }

    // ========================================================================
    // setTestDataReader: 何もしない（INFO ログを出力して無視）
    // ========================================================================

    /**
     * setTestDataReader: 何もしない（INFO ログを出力して無視）。
     *
     * <p>
     * Given: YamlTestDataParser インスタンス<br>
     * When:  setTestDataReader(reader) を呼ぶ<br>
     * Then:  例外なく終了し、INFO ログに "does not use TestDataReader" が含まれること
     * </p>
     */
    @Test
    public void setTestDataReaderLogsInfoAndIgnores() {
        OnMemoryLogWriter.clear();
        // Given / When
        sut.setTestDataReader(new MockTestDataReader());
        // Then
        OnMemoryLogWriter.assertLogContains("writer.memlog", "does not use TestDataReader");
    }

    // ========================================================================
    // S-6: JSON Schema 全項目網羅テスト
    // ========================================================================

    /**
     * [S-6] schemaFullCoverage: スキーマの全トップレベルキー・全 directives・length="-" を含む YAML を
     * 実装が正しく解釈できること。
     *
     * <p>
     * Given: スキーマ（ntf-testdata-yaml-schema.json）の全項目を含む schemaFullCoverage.yaml<br>
     * When:  各 get* メソッドで読み込む<br>
     * Then:  エラーなしに読み込まれ、各トップレベルキーの件数が正しいこと
     * </p>
     *
     * <p>
     * 【設計方針】このメソッドは「スキーマ全11トップレベルキーを一括で通過させる統合煙突テスト」である。
     * 各 DataType・DataFile 型・メッセージ経路の個別検証は既存の testRs0x_ / testGetSetupFile 等のメソッドで
     * 実施済みであり、本メソッドはスキーマに定義されたすべての項目が実装によって解釈可能であることを
     * 一括で確認することを目的とするため、意図的に1メソッドに集約している。
     * </p>
     *
     * <p>
     * 【ディレクティブ値の検証方針】DataFile#setDirective() は無効なキーを IllegalArgumentException で
     * スローする（DR-11・DataFileTest#testConvertValueWithInvalidDirective で確認済み）。そのため、
     * スキーマに記載した全 directives キー（text-encoding, record-separator, file-type, record-length,
     * positive/negative-zone/pack-sign-nibble, required-decimal-point, fixed-sign-position, required-plus-sign,
     * field-separator, quoting-delimiter, ignore-blank-lines, requires-title, max-record-length, title-record-type-name）を
     * 含む YAML が例外なく読み込まれた時点で、全キーが実装で有効であることが証明される。
     * </p>
     */
    @Test
    public void schemaFullCoverage() throws Exception {
        // Given
        final String resource = "YamlTestDataParserTest/schemaFullCoverage";

        // When / Then: 各セクションを読み込み、件数・型・値を検証する
        // setup_tables: group_id なし・grp1・emptySetup の 3 エントリ。
        // グループID なし呼び出しは group_id フィールドのないエントリのみ返す（rows 空除外後の 1 件）。
        List<TableData> setupTables = sut.getSetupTableData(DIR, resource);
        assertThat("setup_tables: group_id なしエントリが取得できること", setupTables.size(), is(1));
        assertThat(setupTables.get(0).getTableName(), is("TEST_TABLE"));

        List<TableData> setupTablesGrp1 = sut.getSetupTableData(DIR, resource, "grp1");
        assertThat("setup_tables: grp1 エントリが取得できること", setupTablesGrp1.size(), is(1));

        // expected_tables: group_id なし・grp1・emptyExpected の 3 エントリ。
        // getExpectedTableData はグループID なしでは expected_tables(1件) + expected_complete_tables(1件) = 2 件。
        List<TableData> expectedTables = sut.getExpectedTableData(DIR, resource);
        assertThat("expected_tables + expected_complete_tables: group_id なしエントリが取得できること", expectedTables.size(), is(2));

        List<TableData> expectedTablesGrp1 = sut.getExpectedTableData(DIR, resource, "grp1");
        assertThat("expected_tables: grp1 エントリが取得できること", expectedTablesGrp1.size(), is(1));

        // list_maps: id=listMapId1 が取得できること
        List<Map<String, String>> listMap = sut.getListMap(DIR, resource, "listMapId1");
        assertThat("list_maps: 2 行取得できること", listMap.size(), is(2));
        assertThat("list_maps: KEY1 が val1 であること", listMap.get(0).get("KEY1"), is("val1"));
        assertThat("list_maps: KEY2 の null 値が null として取得されること", listMap.get(1).get("KEY2"), nullValue());

        // setup_files: group_id なしエントリが3件（all_directives[fixed], variable, empty_file[fixed]）。
        // group_id=grpFixed のエントリはグループIDなし呼び出しではフィルタされるため除外される。
        List<DataFile> setupFiles = sut.getSetupFile(DIR, resource);
        assertThat("setup_files: グループなしの 3 件が取得できること", setupFiles.size(), is(3));
        assertThat("setup_files[0]: FixedLengthFile であること", setupFiles.get(0), instanceOf(FixedLengthFile.class));
        assertThat("setup_files[0]: path が正しいこと",
                setupFiles.get(0).getPath(), is("dummy/setup_fixed_all_directives.dat"));
        assertThat("setup_files[1]: VariableLengthFile であること", setupFiles.get(1), instanceOf(VariableLengthFile.class));
        assertThat("setup_files[2]: records 空の FixedLengthFile であること", setupFiles.get(2), instanceOf(FixedLengthFile.class));

        List<DataFile> setupFilesGrp = sut.getSetupFile(DIR, resource, "grpFixed");
        assertThat("setup_files: grpFixed エントリが取得できること", setupFilesGrp.size(), is(1));

        // expected_files: fixed 1 件 + variable 1 件
        List<DataFile> expectedFiles = sut.getExpectedFile(DIR, resource);
        assertThat("expected_files: 2 件取得できること", expectedFiles.size(), is(2));
        assertThat("expected_files[0]: FixedLengthFile であること", expectedFiles.get(0), instanceOf(FixedLengthFile.class));
        assertThat("expected_files[1]: VariableLengthFile であること", expectedFiles.get(1), instanceOf(VariableLengthFile.class));

        // messages: id=msgId1 が取得できること
        MessagePool msg = sut.getMessage(DIR, resource, "msgId1");
        assertThat("messages: non-null であること", msg, notNullValue());
        assertThat("messages: RequestTestingMessagePool であること", msg, instanceOf(RequestTestingMessagePool.class));

        // expected_request_header_messages: id=msgId1 が取得できること
        MessagePool reqHeader = sut.getMessageWithoutCache(
                DIR, resource, DataType.EXPECTED_REQUEST_HEADER_MESSAGES, "msgId1");
        assertThat("expected_request_header_messages: non-null であること", reqHeader, notNullValue());

        // expected_request_body_messages: id=msgId1 が取得できること
        MessagePool reqBody = sut.getMessageWithoutCache(
                DIR, resource, DataType.EXPECTED_REQUEST_BODY_MESSAGES, "msgId1");
        assertThat("expected_request_body_messages: non-null であること", reqBody, notNullValue());

        // response_body_messages: getSendSyncMessage で grp1 エントリが取得できること
        List<RequestTestingMessagePool> respBody = sut.getSendSyncMessage(
                DIR, resource, "[grp1]", DataType.RESPONSE_BODY_MESSAGES);
        assertThat("response_body_messages: grp1 の 1 件が取得できること", respBody.size(), is(1));

        // response_header_messages: getSendSyncMessage で grp1 エントリが取得できること（GroupData 経路）
        List<RequestTestingMessagePool> respHeader = sut.getSendSyncMessage(
                DIR, resource, "[grp1]", DataType.RESPONSE_HEADER_MESSAGES);
        assertThat("response_header_messages: grp1 の 1 件が取得できること", respHeader.size(), is(1));

        // response_header/body_messages: SingleData 経路（group_id なし）のエントリが取得できること
        MessagePool respHeaderSingle = sut.getMessageWithoutCache(
                DIR, resource, DataType.RESPONSE_HEADER_MESSAGES, "respHeaderSingle");
        assertThat("response_header_messages: SingleData 経路のエントリが取得できること", respHeaderSingle, notNullValue());

        MessagePool respBodySingle = sut.getMessageWithoutCache(
                DIR, resource, DataType.RESPONSE_BODY_MESSAGES, "respBodySingle");
        assertThat("response_body_messages: SingleData 経路のエントリが取得できること", respBodySingle, notNullValue());
    }

    // ========================================================================
    // expected_complete_tables: fillDefaultValues が呼ばれること
    // ========================================================================

    /**
     * getExpectedTableData: expected_complete_tables では fillDefaultValues が呼ばれること。
     *
     * <p>
     * Given: expected_complete_tables に PK_COL1/PK_COL2 のみのエントリ（他カラム省略）<br>
     * When:  getExpectedTableData を呼ぶ<br>
     * Then:  省略カラムにデフォルト値が補完されていること（カラム数が増え、具体的なデフォルト値が設定されること）
     * </p>
     */
    @Test
    public void getExpectedTableDataCompleted() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/completedTable");

        // Then: expected_complete_tables の 1 件が返り、省略カラムが補完されていること
        assertThat(result.size(), is(1));
        TableData td = result.get(0);
        assertThat(td.getTableName(), is("TEST_TABLE"));
        // fillDefaultValues() により DB の全カラムが追加される（YAML 記述の 2 カラムより多い）
        assertTrue("fillDefaultValues により全カラムが補完されていること", td.getColumnNames().length > 2);
        // 数値型（NUMBER_COL）のデフォルト値は "0"（BasicDefaultValues の仕様）
        assertThat("NUMBER_COL のデフォルト値が補完されていること",
                td.getValue(0, "NUMBER_COL").toString(), is("0"));
        // 文字列型（VARCHAR2_COL）のデフォルト値は " "（半角スペース）
        assertThat("VARCHAR2_COL のデフォルト値が補完されていること",
                td.getValue(0, "VARCHAR2_COL").toString(), is(" "));
    }

    // ========================================================================
    // T1: フィールド型記法を日本語名称に統一
    // ========================================================================

    /**
     * [IV-12] フィールド型を日本語名称で記述した固定長/可変長 YAML が
     * dataTypeMapping identity mapping なしで例外なく読み込めること。
     * BasicDataTypeMapping.DEFAULT_TABLE の全 22 エントリ（X/N/XN/Z/SZ/P/SP/X9/SX9/B 系）を網羅する。
     *
     * <p>
     * Given: 半角英字/半角数字/半角記号/半角カナ/半角英数字/半角英数字記号/半角/
     *        全角英字/全角数字/全角ひらがな/全角カタカナ/全角漢字/全角/全半角/
     *        数値/符号無ゾーン10進数/符号付ゾーン10進数/符号無パック10進数/符号付パック10進数/
     *        符号無数値/符号付数値/バイナリ を含む YAML<br>
     * When:  getSetupFile / getExpectedFile を呼ぶ<br>
     * Then:  FixedLengthFile / VariableLengthFile が取得でき、例外が発生しないこと
     * </p>
     */
    @Test
    public void japaneseFieldTypeIsReadableWithoutIdentityMapping() {
        // Given / When
        List<DataFile> setupFiles = sut.getSetupFile(DIR, "YamlTestDataParserTest/japaneseFieldType");
        List<DataFile> expectedFiles = sut.getExpectedFile(DIR, "YamlTestDataParserTest/japaneseFieldType");

        // Then: 3固定長（X系/N系/misc系）+ 1可変長
        assertThat(setupFiles.size(), is(4));
        assertThat(setupFiles.get(0), instanceOf(FixedLengthFile.class));
        assertThat(setupFiles.get(1), instanceOf(FixedLengthFile.class));
        assertThat(setupFiles.get(2), instanceOf(FixedLengthFile.class));
        assertThat(setupFiles.get(3), instanceOf(VariableLengthFile.class));
        assertThat(expectedFiles.size(), is(1));
        assertThat(expectedFiles.get(0), instanceOf(FixedLengthFile.class));
    }

    /**
     * [IV-12] identity mapping 廃止後、旧記法（型記号 X）を YAML に書くと例外になること。
     *
     * <p>
     * Given: type: X（旧型記号記法）を含む YAML<br>
     * When:  getSetupFile を呼ぶ<br>
     * Then:  BasicDataTypeMapping が "X" を変換できず IllegalArgumentException が送出されること
     * </p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void oldTypeSymbolThrowsWhenIdentityMappingAbsent() {
        // Given / When / Then: IllegalArgumentException を期待
        sut.getSetupFile(DIR, "YamlTestDataParserTest/oldTypeSymbol");
    }

    // ========================================================================
    // T6: expected_tables / expected_complete_tables 混在順序非依存
    // ========================================================================

    /**
     * getExpectedTableData: expected_tables → expected_complete_tables の順で記述しても
     * 両セクションが全件取得されること（通常順）。
     *
     * <p>
     * Given: expected_tables(1件) → expected_complete_tables(1件) の順で記述した YAML<br>
     * When:  getExpectedTableData(dir, resource) を呼ぶ<br>
     * Then:  expected_tables 1 件 + expected_complete_tables 1 件 = 合計 2 件が取得されること
     *        （取得順: expected_tables → expected_complete_tables のセクション結合順）
     * </p>
     */
    @Test
    public void mixedTablesNormalOrderReturnsBothSections() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/mixedTablesNormalOrder");

        // Then: expected_tables(1) + expected_complete_tables(1) = 2 件
        assertThat("通常順: 2 件取得できること", result.size(), is(2));
        assertThat("通常順: 1 件目は PK_COL1=0000000001 であること",
                result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
        assertThat("通常順: 2 件目は PK_COL1=0000000002 であること",
                result.get(1).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * getExpectedTableData: expected_complete_tables → expected_tables の順で記述しても
     * 両セクションが全件取得されること（逆順）。
     *
     * <p>
     * Given: expected_complete_tables(1件) → expected_tables(1件) の順で記述した YAML<br>
     * When:  getExpectedTableData(dir, resource) を呼ぶ<br>
     * Then:  expected_tables 1 件 + expected_complete_tables 1 件 = 合計 2 件が取得されること
     *        （取得順は YAML 記述順ではなくセクション結合順: expected_tables → expected_complete_tables）
     * </p>
     */
    @Test
    public void mixedTablesReverseOrderReturnsBothSections() {
        // Given / When
        List<TableData> result = sut.getExpectedTableData(DIR, "YamlTestDataParserTest/mixedTablesReverseOrder");

        // Then: expected_tables(1) + expected_complete_tables(1) = 2 件（YAML 記述順に関係なく両方取得）
        // 取得順は YAML 記述順ではなくセクション結合順（expected_tables → expected_complete_tables）に従う
        assertThat("逆順: 2 件取得できること", result.size(), is(2));
        assertThat("逆順: 1 件目は expected_tables の PK_COL1=0000000001 であること",
                result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
        assertThat("逆順: 2 件目は expected_complete_tables の PK_COL1=0000000002 であること",
                result.get(1).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * getExpectedTableData: 各セクションに複数エントリがあっても
     * グループID指定で正しくフィルタリングされること（複数エントリ混在）。
     *
     * <p>
     * Given: expected_tables(2件: groupIDなし + grpA) と expected_complete_tables(2件: groupIDなし + grpA) を含む YAML<br>
     * When(a): getExpectedTableData(dir, resource) を呼ぶ<br>
     * Then(a): groupID なしの expected_tables 1件 + expected_complete_tables 1件 = 合計 2 件<br>
     * When(b): getExpectedTableData(dir, resource, "grpA") を呼ぶ<br>
     * Then(b): grpA の expected_tables 1件 + expected_complete_tables 1件 = 合計 2 件
     * </p>
     */
    @Test
    public void mixedTablesMultipleEntriesReturnsCorrectEntriesPerGroupId() {
        // Given
        final String resource = "YamlTestDataParserTest/mixedTablesMultipleEntries";

        // When(a): groupID なし
        List<TableData> withoutGroupId = sut.getExpectedTableData(DIR, resource);

        // Then(a): groupID なしの 2 件（expected_tables の groupIDなし + expected_complete_tables の groupIDなし）
        assertThat("複数エントリ混在: groupIDなし → 2 件取得できること", withoutGroupId.size(), is(2));
        assertThat("複数エントリ混在: 1 件目は expected_tables の PK_COL1=0000000001 であること",
                withoutGroupId.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
        assertThat("複数エントリ混在: 2 件目は expected_complete_tables の PK_COL1=0000000002 であること",
                withoutGroupId.get(1).getValue(0, "PK_COL1").toString(), is("0000000002"));

        // When(b): grpA 指定
        List<TableData> withGrpA = sut.getExpectedTableData(DIR, resource, "grpA");

        // Then(b): grpA の 2 件（expected_tables の grpA + expected_complete_tables の grpA）
        assertThat("複数エントリ混在: grpA → 2 件取得できること", withGrpA.size(), is(2));
        assertThat("複数エントリ混在: grpA 1 件目は expected_tables の PK_COL1=0000000003 であること",
                withGrpA.get(0).getValue(0, "PK_COL1").toString(), is("0000000003"));
        assertThat("複数エントリ混在: grpA 2 件目は expected_complete_tables の PK_COL1=0000000004 であること",
                withGrpA.get(1).getValue(0, "PK_COL1").toString(), is("0000000004"));
    }

    // ========================================================================
    // #16: yamlInterpreters（解説書が定める 2 つだけ）
    // ========================================================================

    /**
     * [#16] yamlInterpreters: 解説書が定める 2 つ（DateTimeInterpreter・CompositeInterpreter）だけが
     * 指定されていること。
     *
     * <p>
     * null・空文字・ダブルクォート・改行文字は YAML のパーサが構文として解釈するため、Excel 形式で必要な
     * NullInterpreter・QuotationTrimmer・LineSeparatorInterpreter は指定しない。とりわけ
     * NullInterpreter を指定すると、文字列として記述した "null" も Java の null になり、両者を
     * 区別できなくなる。<br>
     * Given: unit-test-yaml.xml（経由 unit-test.xml）が定義する yamlInterpreters<br>
     * When:  リポジトリから yamlInterpreters を取得する<br>
     * Then:  DateTimeInterpreter・CompositeInterpreter の 2 件だけであること
     * </p>
     */
    @Test
    public void yamlInterpretersAreOnlyDocumentedTwo() {
        // Given / When
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> yamlInterpreters =
                repositoryResource.getComponent("yamlInterpreters");

        // Then
        assertNotNull("yamlInterpreters コンポーネントが定義されていること", yamlInterpreters);
        assertThat("yamlInterpreters は 2 件だけであること", yamlInterpreters.size(), is(2));
        assertThat("1 件目は DateTimeInterpreter であること", yamlInterpreters.get(0),
                is(instanceOf(nablarch.test.core.util.interpreter.DateTimeInterpreter.class)));
        assertThat("2 件目は CompositeInterpreter であること", yamlInterpreters.get(1),
                is(instanceOf(nablarch.test.core.util.interpreter.CompositeInterpreter.class)));
    }

    // ========================================================================
    // #35: fileExtensions（sendSyncTestData を設定しない）
    // ========================================================================

    /**
     * [#35] filePathSetting: fileExtensions に sendSyncTestData キーが無いこと。
     *
     * <p>
     * 何を担保するか: YAML 形式では、モックアップクラスが読む同期応答メッセージ送信のテストデータは
     * リクエスト ID と同じ名前の<b>ディレクトリ</b>配下の {@code message.yaml} が読み込み単位になる。
     * このため {@code fileExtensions} に {@code sendSyncTestData} の拡張子を設定してはならない。
     * 設定するとテストデータが見つからず、テストの実行時に例外が発生する。<br>
     * 根拠: setup/common.rst:264（{@code .. important::} の本文）<br>
     * Given: unit-test-yaml.xml（経由 unit-test.xml）が定義する filePathSetting<br>
     * When:  リポジトリから filePathSetting を取得し fileExtensions を見る<br>
     * Then:  sendSyncTestData キーが存在しないこと
     * </p>
     */
    @Test
    public void fileExtensionsHasNoSendSyncTestData() {
        // Given / When
        nablarch.core.util.FilePathSetting filePathSetting =
                repositoryResource.getComponent("filePathSetting");

        // Then
        assertNotNull("filePathSetting コンポーネントが定義されていること", filePathSetting);
        Map<String, String> fileExtensions = filePathSetting.getFileExtensions();
        assertNotNull("fileExtensions が取得できること", fileExtensions);
        assertFalse("fileExtensions に sendSyncTestData キーが無いこと（setup/common.rst:264）",
                fileExtensions.containsKey("sendSyncTestData"));
    }

    /**
     * [#16] yamlInterpreters: null の記法が YAML のパーサの解釈どおりに扱われること。
     *
     * <p>
     * NullInterpreter を指定しないため、クォート付きの "null" は文字列のまま残り、クォートなしの
     * null（および値を省略した COL:）だけが Java null になる。両者が区別できることを固定する。<br>
     * Given: list_maps に BARE_NULL: null / OMITTED_NULL:（値省略）/ QUOTED_NULL: "null" /
     *        UPPER_QUOTED_NULL: "NULL" を持つ行<br>
     * When:  yamlInterpreters で初期化した YamlTestDataParser で getListMap を呼ぶ<br>
     * Then:  クォートなしの 2 つは Java null、クォート付きの 2 つは文字列のまま取得されること
     * </p>
     */
    @Test
    public void quotedNullIsKeptAsStringAndDistinguishableFromBareNull() {
        // Given / When
        List<Map<String, String>> result = sut.getListMap(
                DIR, "YamlTestDataParserTest/quotedValues", "nullNotationTest");

        // Then
        assertThat("行数が 1 件であること", result.size(), is(1));
        assertNull("クォートなしの null は Java null になること", result.get(0).get("BARE_NULL"));
        assertNull("値を省略した COL: は Java null になること", result.get(0).get("OMITTED_NULL"));
        assertThat("クォート付きの \"null\" は文字列のまま残ること",
                result.get(0).get("QUOTED_NULL"), is("null"));
        assertThat("クォート付きの \"NULL\" は文字列のまま残ること",
                result.get(0).get("UPPER_QUOTED_NULL"), is("NULL"));
    }

    /**
     * [#16] yamlInterpreters: QuotationTrimmer を含まないインタープリタリストで
     * YAML のダブルクォート値が正しく処理されること。
     *
     * <p>
     * YAML パーサはダブルクォートを構文として処理済みで文字列値を返すため、
     * QuotationTrimmer は不要かつ二重処理となる。yamlInterpreters はこれを含まず、
     * YAML パーサが返す値をそのまま使用する。
     * </p>
     *
     * <p>
     * Given: YAML でダブルクォート記法 "hello"/"world"/"" を含む list_maps<br>
     * When:  yamlInterpreters（DateTimeInterpreter/CompositeInterpreter→
     *         BasicJapaneseCharacterInterpreter）で
     *         初期化した YamlTestDataParser で getListMap を呼ぶ<br>
     * Then:  クォートなしの値 hello/world/"" が取得されること（二重処理なし）
     * </p>
     */
    @Test
    public void yamlInterpretersDoNotDoubleProcessQuotes() {
        // Given: yamlInterpreters（QuotationTrimmer なし）で SUT を再構築
        List<nablarch.test.core.util.interpreter.TestDataInterpreter> yamlInterpreters =
                repositoryResource.getComponent("yamlInterpreters");

        // yamlInterpreters コンポーネントが unit-test-yaml.xml（経由 unit-test.xml）で定義されていること
        assertNotNull("yamlInterpreters コンポーネントが定義されていること", yamlInterpreters);
        assertFalse("yamlInterpreters が空リストでないこと（ガード）", yamlInterpreters.isEmpty());
        // QuotationTrimmer が含まれていないこと
        for (nablarch.test.core.util.interpreter.TestDataInterpreter interp : yamlInterpreters) {
            assertFalse("yamlInterpreters に QuotationTrimmer が含まれていないこと",
                    interp instanceof nablarch.test.core.util.interpreter.QuotationTrimmer);
        }

        YamlTestDataParser sutWithYamlInterpreters = new YamlTestDataParser();
        sutWithYamlInterpreters.setDbInfo(repositoryResource.getComponent("dbInfo"));
        sutWithYamlInterpreters.setDefaultValues(new nablarch.test.core.db.BasicDefaultValues());
        sutWithYamlInterpreters.setInterpreters(yamlInterpreters);

        // When
        List<Map<String, String>> result = sutWithYamlInterpreters.getListMap(
                DIR, "YamlTestDataParserTest/quotedValues", "quotedValueTest");

        // Then: YAML がクォートを解除済みなので値はクォートなし
        assertThat("行数が 2 件であること", result.size(), is(2));
        assertThat("COL1 が hello であること", result.get(0).get("COL1"), is("hello"));
        assertThat("COL2 が world であること", result.get(0).get("COL2"), is("world"));
        assertThat("COL3 が空文字であること", result.get(0).get("COL3"), is(""));
        assertThat("COL1 が with space であること", result.get(1).get("COL1"), is("with space"));
        assertThat("COL2 が 123 であること", result.get(1).get("COL2"), is("123"));
        assertThat("COL3 が true であること", result.get(1).get("COL3"), is("true"));
    }

    // ========================================================================
    // 読み込み単位の解決（予約 ID・モックアップクラスの電文配置・TestDataParser の直接使用）
    // ========================================================================

    /**
     * getMessage: MESSAGE の予約 ID setUpMessages・expectedMessages で電文を取得できること。
     *
     * <p>
     * 何を担保するか: データタイプ MESSAGE の識別子として予約値 {@code setUpMessages}（要求電文）・
     * {@code expectedMessages}（応答電文）を書いて、テストの入力データ・期待値となる電文を
     * 取得できること。これらの識別子は固定である。<br>
     * 根拠: implementation/testdata_notation.rst:1149<br>
     * Given: messages に id=setUpMessages と id=expectedMessages のエントリ<br>
     * When:  getMessage(dir, resource, "setUpMessages")／getMessage(dir, resource, "expectedMessages") を呼ぶ<br>
     * Then:  それぞれの電文本文と FW 制御ヘッダが取得できること
     * </p>
     */
    @Test
    public void getMessage_reservedIdsSetUpMessagesAndExpectedMessages() throws Exception {
        // Given / When
        MessagePool setUp = sut.getMessage(DIR, "YamlTestDataParserTest/messageData", "setUpMessages");

        // Then
        assertNotNull("予約 ID setUpMessages の電文が取得できること", setUp);
        List<DataRecord> setUpMessages = ((RequestTestingMessagePool) setUp).getExpectedMessageList();
        assertThat("setUpMessages の電文本文が 1 件取得できること", setUpMessages.size(), is(1));
        assertThat("setUpMessages の電文本文が記述どおりであること",
                setUpMessages.get(0).getString("REQUEST_KEY"), is("SETUPKEY01"));
        assertThat("setUpMessages の FW 制御ヘッダが取得できること",
                fwHeaderOf(setUp).get("userId"), is("setUpUser0"));

        // Given / When
        MessagePool expected = sut.getMessage(DIR, "YamlTestDataParserTest/messageData", "expectedMessages");

        // Then
        assertNotNull("予約 ID expectedMessages の電文が取得できること", expected);
        List<DataRecord> expectedMessages = ((RequestTestingMessagePool) expected).getExpectedMessageList();
        assertThat("expectedMessages の電文本文が 1 件取得できること", expectedMessages.size(), is(1));
        assertThat("expectedMessages の電文本文が記述どおりであること",
                expectedMessages.get(0).getString("RESPONSE_KEY"), is("EXPECTKEY1"));
        assertThat("expectedMessages の FW 制御ヘッダが取得できること",
                fwHeaderOf(expected).get("userId"), is("expectUser"));
    }

    /**
     * getMessageWithoutCache: モックアップクラスの電文は リクエストID と同じ名前のディレクトリ配下の
     * message.yaml が読み込み単位になること。
     *
     * <p>
     * 何を担保するか: 取引単体テストのモックアップクラスが読む同期応答メッセージ送信のテストデータが、
     * リクエスト ID と同じ名前のディレクトリ配下の固定名 {@code message.yaml} から読まれること。
     * {@code <リクエストID>.yaml}（Excel 形式の {@code <リクエストID>.xlsx} に相当する置き方）は
     * 読み込み単位にならない。<br>
     * 根拠: implementation/deal_unit_test/mom.rst:72、implementation/testdata_notation.rst:1151<br>
     * 呼び出し方は、モックアップクラス経路の入口である nablarch-testing の
     * {@code SendSyncSupport#createTestDataInfo}（{@code SendSyncSupport.java:347} が
     * リソース名を {@code リクエストID + "/" + "message"} として組み立て、
     * {@code :393} が {@code getMessageWithoutCache} を呼ぶ）に合わせている。<br>
     * Given: {@code <base>/RM21AA0101/message.yaml}（RESULT_KEY="FROM_DIR01"）と、
     *        囮の {@code <base>/RM21AA0101.yaml}（RESULT_KEY="FROM_FILE1"）の 2 ファイル<br>
     * When:  getMessageWithoutCache(base, "RM21AA0101/message", RESPONSE_BODY_MESSAGES, "RM21AA0101") を呼ぶ<br>
     * Then:  ディレクトリ配下の message.yaml が読まれ、RESULT_KEY が "FROM_DIR01" になること
     * </p>
     */
    @Test
    public void getMessageWithoutCache_readsMessageYamlUnderRequestIdDirectory() {
        // Given: 囮の <リクエストID>.yaml が存在すること（この前提が崩れると本テストは空振りになる）
        assertTrue("囮ファイル <リクエストID>.yaml が存在すること",
                new File(SEND_SYNC_DIR + "/RM21AA0101.yaml").isFile());
        assertTrue("読み込み単位 <リクエストID>/message.yaml が存在すること",
                new File(SEND_SYNC_DIR + "/RM21AA0101/message.yaml").isFile());

        // When
        MessagePool result = sut.getMessageWithoutCache(
                SEND_SYNC_DIR, "RM21AA0101/message",
                DataType.RESPONSE_BODY_MESSAGES, "RM21AA0101");

        // Then
        assertNotNull("リクエストID と同じ名前のディレクトリ配下の message.yaml が読めること", result);
        List<DataRecord> messages = ((RequestTestingMessagePool) result).getExpectedMessageList();
        assertThat("電文が 1 件取得できること", messages.size(), is(1));
        assertThat("<リクエストID>/message.yaml が読み込み単位になること"
                        + "（囮の <リクエストID>.yaml が読まれると FROM_FILE1 になる）",
                messages.get(0).getString("RESULT_KEY"), is("FROM_DIR01"));
    }

    /**
     * getListMap: 第2引数 &lt;ファイル名&gt;/&lt;読み込み単位の名前&gt; が
     * &lt;ディレクトリ&gt;/&lt;ファイル名&gt;/&lt;読み込み単位の名前&gt;.yaml に解決されること。
     *
     * <p>
     * 何を担保するか: テストコードと別のディレクトリにあるテストデータを読む目的で
     * {@code TestDataParser} を直接使うとき、第1引数のディレクトリ・第2引数の
     * {@code <ファイル名>/<読み込み単位の名前>} が {@code <ディレクトリ>/<ファイル名>/<読み込み単位の名前>.yaml}
     * に解決されること。{@code <ディレクトリ>/<ファイル名>.yaml} は読まれない。<br>
     * 根拠: implementation/class_unit_test/component.rst:313<br>
     * Given: {@code <dir>/CommonTestData/employees.yaml}（list_maps の id=params）と、
     *        囮の {@code <dir>/CommonTestData.yaml}（同じ id=params で別の値）<br>
     * When:  getListMap(dir, "CommonTestData/employees", "params") を呼ぶ<br>
     * Then:  {@code <dir>/CommonTestData/employees.yaml} の内容が返ること
     * </p>
     */
    @Test
    public void getListMap_resolvesFileNameAndUnitNameToNestedYaml() {
        // Given: 囮の <ディレクトリ>/<ファイル名>.yaml が存在すること（この前提が崩れると本テストは空振りになる）
        assertTrue("囮ファイル <ディレクトリ>/<ファイル名>.yaml が存在すること",
                new File(OTHER_DIR + "/CommonTestData.yaml").isFile());
        assertTrue("読み込み単位 <ディレクトリ>/<ファイル名>/<読み込み単位の名前>.yaml が存在すること",
                new File(OTHER_DIR + "/CommonTestData/employees.yaml").isFile());

        // When
        List<Map<String, String>> result = sut.getListMap(OTHER_DIR, "CommonTestData/employees", "params");

        // Then
        assertThat("<ディレクトリ>/<ファイル名>/<読み込み単位の名前>.yaml が読まれること"
                        + "（囮の <ディレクトリ>/<ファイル名>.yaml が読まれると 1 件になる）",
                result.size(), is(2));
        assertThat(result.get(0).get("EMP_ID"), is("00001"));
        assertThat(result.get(0).get("EMP_NAME"), is("山田太郎"));
        assertThat(result.get(1).get("EMP_ID"), is("00002"));
        assertThat(result.get(1).get("EMP_NAME"), is("鈴木花子"));
    }

    /**
     * {@link MessagePool} の FW 制御ヘッダを取得する。
     *
     * <p>
     * {@code MessagePool#getFwHeader} はパッケージプライベートのため、リフレクションで
     * {@code fwHeader} フィールドを直接取得する（既存テストと同じ手法）。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> fwHeaderOf(MessagePool pool) throws Exception {
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        return (Map<String, String>) fwHeaderField.get(pool);
    }
}
