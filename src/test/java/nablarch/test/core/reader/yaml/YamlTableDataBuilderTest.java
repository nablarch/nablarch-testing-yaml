package nablarch.test.core.reader.yaml;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.util.interpreter.DateTimeInterpreter;
import nablarch.test.core.util.interpreter.NullInterpreter;
import nablarch.test.core.util.interpreter.QuotationTrimmer;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link YamlTableDataBuilder} のテーブル系メソッド（{@code buildTableDataList}／{@code buildListMapRows}）のテストクラス。
 *
 * <p>
 * {@link YamlLoader#load} が返す YAML Map を {@link YamlTableDataBuilder} が走査し、値加工
 * （{@code ${...}} の解釈・マーカー列除外・グループ絞り込み・{@code fillDefaultValues}・
 * list_maps の TreeMap ソート）して {@link TableData}・ListMap を組み立てる一連のロジックを検証する。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlTableDataBuilderTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    private DbInfo dbInfo;
    private YamlTableDataBuilder builder;

    @BeforeClass
    public static void beforeClass() {
        VariousDbTestHelper.createTable(TestTable.class);
    }

    @Before
    public void before() {
        dbInfo = repositoryResource.getComponent("dbInfo");
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("yamlInterpreters");
        builder = new YamlTableDataBuilder(dbInfo, new BasicDefaultValues(), InterpreterResolver.withBinaryFile(interpreters));
    }

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    // ------------------------------------------------------------------------
    // ビルダ（YAML Map → 本体器）を通すヘルパー。
    // ------------------------------------------------------------------------

    private List<TableData> buildTableDataList(Map<String, Object> yaml, String sectionKey,
                                               String groupId, boolean fillDefaults, String path) {
        return builder.buildTableDataList(yaml, sectionKey, groupId, fillDefaults, path);
    }

    private List<Map<String, String>> buildListMapRows(Map<String, Object> yaml, String id, String path) {
        return builder.buildListMapRows(yaml, id, path);
    }

    // ========================================================================
    // buildTableDataList: グループ ID なしでデータを取得できること
    // ========================================================================

    /**
     * [YamlTableDataBuilder] buildTableDataList: グループ ID なしで setup_tables の TableData が取得できること。
     *
     * <p>
     * Given: setup_tables にグループ ID なしの 1 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "", false, path) を呼ぶ<br>
     * Then:  1 件の TableData が返り、テーブル名・カラム値が正しいこと
     * </p>
     */
    @Test
    public void buildTableDataList_noGroupId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getTableName(), is("TEST_TABLE"));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_tables に groupA / groupB のエントリがある<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[groupA]", false, path) を呼ぶ<br>
     * Then:  groupA の 1 件のみ返ること
     * </p>
     */
    @Test
    public void buildTableDataList_withGroupId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[groupA]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000002"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: rows が空（rows: []）のエントリは 0 行の TableData として返ること。
     *
     * <p>
     * Given: setup_tables に rows: [] のエントリ（emptyRows グループ）<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[emptyRows]", false, path) を呼ぶ<br>
     * Then:  サイズ 1 のリストが返り、テーブル名が "TEST_TABLE"、カラム名が 0 件、行数が 0 であること
     * </p>
     * <p>
     * カラム名が 0 件で正しいのは、rows: [] ではどの行もキーを持たず、YAML に列名を書く場所が
     * 無いためである。setup_tables では列名は 0 件のまま解決されないが、それで支障がない理由は
     * {@code YamlTableDataBuilder#buildTableData} の javadoc に記す。
     * </p>
     */
    @Test
    public void buildTableDataList_emptyRowsExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[emptyRows]", false, DIR);

        // Then: rows:[] エントリは 0 行の TableData として返る（Excel 経路の振る舞いに合わせる）
        assertThat("サイズ 1 のリストが返ること", result.size(), is(1));
        assertThat("テーブル名が TEST_TABLE であること", result.get(0).getTableName(), is("TEST_TABLE"));
        assertThat("カラム名が 0 件であること", result.get(0).getColumnNames().length, is(0));
        assertThat("行数が 0 であること", result.get(0).size(), is(0));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: fillDefaults=true の場合、fillDefaultValues が適用されること。
     *
     * <p>
     * Given: expected_complete_tables に PK_COL1/PK_COL2 のみのエントリ<br>
     * When:  buildTableDataList(yaml, "expected_complete_tables", "", true, path) を呼ぶ<br>
     * Then:  省略カラムにデフォルト値が補完されていること
     * </p>
     */
    @Test
    public void buildTableDataList_fillDefaultValues() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/completedTable");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_complete_tables", "", true, DIR);

        // Then
        assertThat(result.size(), is(1));
        TableData td = result.get(0);
        assertTrue("fillDefaultValues により全カラムが補完されていること", td.getColumnNames().length > 2);
        assertThat("NUMBER_COL のデフォルト値が補完されていること",
                td.getValue(0, "NUMBER_COL").toString(), is("0"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: セクションが存在しない場合は空リストが返ること。
     *
     * <p>
     * Given: setup_tables キーが存在しない YAML<br>
     * When:  buildTableDataList(yaml, "setup_tables", "", false, path) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void buildTableDataList_sectionNotExists() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/emptyYaml");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "", false, DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    // ========================================================================
    // buildListMapRows
    // ========================================================================

    /**
     * [YamlTableDataBuilder] buildListMapRows: 指定 ID のデータが取得できること。
     *
     * <p>
     * Given: list_maps に id=testListMap が 2 行<br>
     * When:  buildListMapRows(yaml, "testListMap", path) を呼ぶ<br>
     * Then:  2 行のデータが返ること
     * </p>
     */
    @Test
    public void buildListMapRows_normalCase() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "testListMap", DIR);

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0).get("KEY1"), is("val1"));
        assertThat(result.get(0).get("KEY2"), is("val2"));
        assertThat(result.get(1).get("KEY1"), is("val3"));
        assertThat(result.get(1).get("KEY2"), is("val4"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: マーカーカラム（[COL] 形式）は除外されること。
     *
     * <p>
     * Given: list_maps に "[NO]" キーを含む行<br>
     * When:  buildListMapRows(yaml, "markerColTest", path) を呼ぶ<br>
     * Then:  "[NO]" キーが結果に含まれないこと
     * </p>
     */
    @Test
    public void buildListMapRows_markerColumnsExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "markerColTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertFalse(result.get(0).containsKey("[NO]"));
        assertThat(result.get(0).get("KEY1"), is("val1"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 存在しない ID を指定した場合は空リストが返ること。
     *
     * <p>
     * Given: list_maps に存在しない id<br>
     * When:  buildListMapRows(yaml, "noSuchId", path) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void buildListMapRows_idNotFound() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "noSuchId", DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: YAML ネイティブ null は Java null として取得されること。
     *
     * <p>
     * Given: list_maps に NULL_COL: null（YAML ネイティブ null）<br>
     * When:  buildListMapRows(yaml, "nativeNullTest", path) を呼ぶ<br>
     * Then:  NULL_COL の値が null であること
     * </p>
     */
    @Test
    public void buildListMapRows_nativeNullIsJavaNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "nativeTypeTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertNull(result.get(0).get("NULL_COL"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: 同一グループID に同一テーブル名のエントリが複数ある場合、
     * 全件取得できること（QA観点2-軽微）。
     *
     * <p>
     * Given: setup_tables に group_id=dupTable で TEST_TABLE が 2 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[dupTable]", false, path) を呼ぶ<br>
     * Then:  2 件の TableData が返り、それぞれのデータが正しいこと
     * </p>
     */
    @Test
    public void buildTableDataList_duplicateTableNamesInSameGroup() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[dupTable]", false, DIR);

        // Then
        assertThat("同一グループの同一テーブル名エントリが 2 件返ること", result.size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000010"));
        assertThat(result.get(1).getValue(0, "PK_COL1").toString(), is("0000000011"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: table キーが存在しないエントリで IllegalStateException がスローされること（E-1）。
     *
     * <p>
     * Given: setup_tables に table キーがない missingTable グループのエントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[missingTable]", false, path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージにセクション名とファイルパスが含まれること
     * </p>
     */
    @Test
    public void buildTableDataList_missingTableThrowsException() {
        // Given: 'table' キーを持たないエントリを直接構築（スキーマ検証の対象外で Builder の検証をテスト）
        Map<String, Object> entryWithoutTable = new java.util.LinkedHashMap<>();
        entryWithoutTable.put("group_id", "missingTable");
        entryWithoutTable.put("rows", Collections.<Object>emptyList());
        Map<String, Object> yaml = new java.util.LinkedHashMap<>();
        yaml.put("setup_tables", Arrays.<Object>asList(entryWithoutTable));

        // When
        try {
            buildTableDataList(yaml, "setup_tables", "[missingTable]", false, DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("フィールド名がメッセージに含まれること", e.getMessage(), containsString("table"));
            assertThat("セクション名がメッセージに含まれること", e.getMessage(), containsString("setup_tables"));
            assertThat("ファイルパスがメッセージに含まれること", e.getMessage(), containsString(DIR));
        }
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 同一ファイル内で同一 ID のエントリが 2 件ある場合、先着一致で最初の 1 件のみ返ること。
     *
     * <p>
     * 同一ファイル内で同一 ID の重複エントリは先着一致で、2件目以降は無視されます<br>
     * Given: list_maps に id=dupIdFirst が 2 エントリ（1件目 KEY1="first", 2件目 KEY1="second"）<br>
     * When:  buildListMapRows(yaml, "dupIdFirst", path) を呼ぶ<br>
     * Then:  1件目の KEY1="first" が返ること
     * </p>
     */
    @Test
    public void buildListMapRows_duplicateIdReturnsFirst() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "dupIdFirst", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("先着一致で最初の 1 件のみ返ること", result.get(0).get("KEY1"), is("first"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: rows 内の空エントリ（{}）は読み飛ばされること。
     *
     * <p>
     * rows 内の要素が空マッピング（{}）の場合にスキップされます<br>
     * Given: setup_tables の emptyRowMixed グループに 通常行・{} 行・通常行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[emptyRowMixed]", false, path) を呼ぶ<br>
     * Then:  {} 行がスキップされ、2 行のみ返ること
     * </p>
     */
    @Test
    public void buildTableDataList_emptyRowEntrySkipped() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[emptyRowMixed]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        // result.get(0).size() は TableData の行数（addRow された件数）を返す
        assertThat("空エントリ {} をスキップして 2 行のみ返ること", result.get(0).size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000020"));
        assertThat(result.get(0).getValue(1, "PK_COL1").toString(), is("0000000021"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: rows が空マッピング（{}）のみのとき TableData が 1 件返ること。
     *
     * <p>
     * 全行が空マッピングのため、列名解決（{@code YamlSection#resolveColumns}）へ到達する前に
     * {@code YamlSection#dropBlankRows} が全行を取り除く。列名解決に渡るのは空リストなので
     * カラム名が 0 件になり、データ行も 0 件になる
     * （取り除くこと自体は {@code buildTableDataList_emptyRowEntrySkipped} が検証する）。<br>
     * Given: setup_tables の allEmptyRows グループに {} × 2 のみ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[allEmptyRows]", false, path) を呼ぶ<br>
     * Then:  TableData が 1 件返り、カラム名が 0 件、行 0 件であること
     * </p>
     * <p>
     * その 0 件を列名解決で埋めないのが正しいのは、このケースは全行が {} でどの行もキーを持たず、
     * YAML に列名を書く場所が無いためである。setup_tables では列名は 0 件のまま解決されないが、
     * それで支障がない理由は {@code YamlTableDataBuilder#buildTableData} の javadoc に記す。
     * </p>
     */
    @Test
    public void buildTableDataList_allEmptyRowsReturnsTableDataWithNoColumns() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[allEmptyRows]", false, DIR);

        // Then
        assertThat("全行が {} の場合も TableData は 1 件生成されること", result.size(), is(1));
        assertThat("カラム名が 0 件であること", result.get(0).getColumnNames().length, is(0));
        assertThat("行数が 0 件であること", result.get(0).size(), is(0));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: setup_tables のマーカーカラム（[COL] 形式）は除外されること。
     *
     * <p>
     * YAML では setup_tables / expected_tables / list_maps すべてでマーカーカラムが除外されます<br>
     * Given: setup_tables の markerColInTable グループに "[NO]" カラムを含む行<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[markerColInTable]", false, path) を呼ぶ<br>
     * Then:  "[NO]" カラムが TableData のカラム名に含まれないこと
     * </p>
     */
    @Test
    public void buildTableDataList_markerColumnsExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[markerColInTable]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        String[] columnNames = result.get(0).getColumnNames();
        for (String col : columnNames) {
            assertFalse("マーカーカラム [NO] が含まれないこと", col.equals("[NO]"));
        }
        assertThat("PK_COL1 は含まれること", result.get(0).getValue(0, "PK_COL1").toString(), is("0000000001"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_tables のマーカーカラム（[COL] 形式）は除外されること。
     *
     * <p>
     * YAML では setup_tables / expected_tables / list_maps すべてでマーカーカラムが除外されます<br>
     * Given: expected_tables の markerColInTable グループに "[NO]" カラムを含む行<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[markerColInTable]", false, path) を呼ぶ<br>
     * Then:  "[NO]" カラムが TableData のカラム名に含まれないこと
     * </p>
     */
    @Test
    public void buildTableDataList_markerColumnsExcludedInExpectedTables() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[markerColInTable]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        String[] columnNames = result.get(0).getColumnNames();
        for (String col : columnNames) {
            assertFalse("マーカーカラム [NO] が含まれないこと", col.equals("[NO]"));
        }
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: クォートあり "null" は Java null として取得されること。
     *
     * <p>
     * YAML の "null"（クォートあり）も Java null になります（NullInterpreter が変換）<br>
     * Given: list_maps に QUOTED_NULL: "null"（クォートあり）<br>
     * When:  buildListMapRows(yaml, "interpreterTest", path) を呼ぶ<br>
     * Then:  QUOTED_NULL の値が null であること
     * </p>
     */
    @Test
    public void buildListMapRows_quotedNullIsJavaNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertNull("\"null\"（クォートあり）は Java null になること", result.get(0).get("QUOTED_NULL"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: " " はクォート除去後にスペース1文字になること。
     *
     * <p>
     * " "（スペースをダブルクォートで囲む）→ QuotationTrimmer が外側クォートを除去してスペース1文字<br>
     * Given: list_maps に SPACE_COL: " "<br>
     * When:  buildListMapRows(yaml, "interpreterTest", path) を呼ぶ<br>
     * Then:  SPACE_COL の値がスペース1文字であること
     * </p>
     */
    @Test
    public void buildListMapRows_spaceBetweenQuotesIsSpace() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\" \" はスペース1文字になること", result.get(0).get("SPACE_COL"), is(" "));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: "\\r" は CR（キャリッジリターン）文字に変換されること。
     *
     * <p>
     * "\\r" → LineSeparatorInterpreter が CR（0x0D）に変換（デフォルト設定）<br>
     * Given: list_maps に CR_COL: "\\r"<br>
     * When:  buildListMapRows(yaml, "interpreterTest", path) を呼ぶ<br>
     * Then:  CR_COL の値が CR 文字（"\r"）であること
     * </p>
     */
    @Test
    public void buildListMapRows_escapedCrIsCarriageReturn() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\"\\\\r\" は CR 文字に変換されること", result.get(0).get("CR_COL"), is("\r"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: "${systemTime}" 完全一致の場合はシステム時刻に変換されること。
     *
     * <p>
     * DateTimeInterpreter は完全一致のみ変換する。部分文字列は変換されない<br>
     * Given: list_maps に EXACT_COL="${systemTime}", PARTIAL_COL="prefix_${systemTime}"<br>
     * When:  buildListMapRows(yaml, "dateTimeTest", path) を呼ぶ<br>
     * Then:  EXACT_COL はシステム時刻文字列になり、PARTIAL_COL は変換されないこと
     * </p>
     */
    @Test
    public void buildListMapRows_dateTimeInterpreterExactMatchOnly() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "dateTimeTest", DIR);

        // Then
        // 期待値 "2010-09-14 12:34:56.0" は unit-test-yaml.xml の dateProvider（BasicDateTimeProvider）に
        // 固定値 "2010-09-14 12:34:56" が設定されているため。
        assertThat(result.size(), is(1));
        assertThat("${systemTime} 完全一致はシステム時刻に変換されること",
                result.get(0).get("EXACT_COL"), is("2010-09-14 12:34:56.0"));
        assertThat("部分文字列 prefix_${systemTime} は変換されないこと",
                result.get(0).get("PARTIAL_COL"), is("prefix_${systemTime}"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: "${binaryFile:path}" はファイル内容の HexString に変換されること。
     *
     * <p>
     * BinaryFileInterpreter のパスは YAML ファイルのディレクトリからの相対パス<br>
     * Given: list_maps に BIN_COL="${binaryFile:YamlTableDataBuilderTest/test.bin}"<br>
     * When:  buildListMapRows(yaml, "binaryFileTest", path) を呼ぶ<br>
     * Then:  BIN_COL が test.bin のバイト列 HexString（"414243"）になること
     * </p>
     */
    @Test
    public void buildListMapRows_binaryFileInterpreterResolvesRelativePath() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "binaryFileTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("${binaryFile:path} はファイル内容の HexString に変換されること",
                result.get(0).get("BIN_COL"), is("414243"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: "${半角英字,N}" 形式で指定長の文字列が生成されること。
     *
     * <p>
     * BasicJapaneseCharacterInterpreter が ${文字種,文字数} を生成する<br>
     * Given: list_maps に ALPHA_COL="${半角英字,10}", NUM_COL="${半角数字,5}"<br>
     * When:  buildListMapRows(yaml, "charGenTest", path) を呼ぶ<br>
     * Then:  ALPHA_COL は 10 文字の半角英字、NUM_COL は 5 文字の半角数字になること
     * </p>
     */
    @Test
    public void buildListMapRows_charTypeGeneratorProducesSpecifiedLength() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "charGenTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        String alphaVal = result.get(0).get("ALPHA_COL");
        assertThat("${半角英字,10} は 10 文字になること", alphaVal.length(), is(10));
        assertTrue("${半角英字,10} は半角英字のみであること", alphaVal.matches("[a-zA-Z]{10}"));
        String numVal = result.get(0).get("NUM_COL");
        assertThat("${半角数字,5} は 5 文字になること", numVal.length(), is(5));
        assertTrue("${半角数字,5} は半角数字のみであること", numVal.matches("[0-9]{5}"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: "\""（YAML エスケープ）はダブルクォート1文字になること。
     *
     * <p>
     * `"\""` → YAML パース後は `"` 1文字。
     * YAML 経路では QuotationTrimmer は使わないため、`"` がそのまま返ること<br>
     * Given: list_maps に DQ_COL: "\""<br>
     * When:  buildListMapRows(yaml, "quotationTest", path) を呼ぶ<br>
     * Then:  DQ_COL の値がダブルクォート1文字（`"`）であること
     * </p>
     */
    @Test
    public void buildListMapRows_escapedDoubleQuoteIsDoubleQuoteChar() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "quotationTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\"\\\"\" はダブルクォート1文字になること",
                result.get(0).get("DQ_COL"), is("\""));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: '"'（YAML シングルクォート記法）でのダブルクォート1文字になること。
     *
     * <p>
     * シングルクォートで囲んだ '"' も YAML パース後は " 1文字。
     * YAML 経路では QuotationTrimmer は使わないため、`"` がそのまま返ること<br>
     * Given: list_maps に DQ_COL: '"'（YAML シングルクォート記法）<br>
     * When:  buildListMapRows(yaml, "singleQuoteNotationTest", path) を呼ぶ<br>
     * Then:  DQ_COL の値がダブルクォート1文字（"）であること
     * </p>
     */
    @Test
    public void buildListMapRows_singleQuoteNotationForDoubleQuote() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "singleQuoteNotationTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("'\"'（シングルクォート記法）はダブルクォート1文字になること",
                result.get(0).get("DQ_COL"), is("\""));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: "${updateTime}" / "${setUpTime}" はシステム時刻に変換されること。
     *
     * <p>
     * DateTimeInterpreter は "${updateTime}" と "${setUpTime}" も完全一致で変換する<br>
     * Given: list_maps に UPDATE_COL="${updateTime}", SET_UP_TIME_COL="${setUpTime}"、
     *        DateTimeInterpreter に setSetUpDateTime("2010-09-14 12:34:56.0") 設定済み<br>
     * When:  buildListMapRows(yaml, "quotationTest", path) を呼ぶ<br>
     * Then:  両カラムがシステム時刻文字列（"2010-09-14 12:34:56.0"）になること
     * </p>
     */
    @Test
    public void buildListMapRows_updateTimeAndSetUpTimeConverted() {
        // Given
        // @Before の sut は setSetUpDateTime 未設定のため、ここで専用インスタンスを生成する
        DateTimeInterpreter dateTimeInterpreter = new DateTimeInterpreter();
        dateTimeInterpreter.setSystemTimeProvider(repositoryResource.getComponent("dateProvider"));
        dateTimeInterpreter.setSetUpDateTime("2010-09-14 12:34:56.0");
        List<TestDataInterpreter> interpreters = Arrays.asList(
                new NullInterpreter(),
                dateTimeInterpreter
        );
        YamlTableDataBuilder builderWithSetUp = new YamlTableDataBuilder(dbInfo, new BasicDefaultValues(), InterpreterResolver.withBinaryFile(interpreters));
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = builderWithSetUp.buildListMapRows(yaml, "quotationTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("${updateTime} はシステム時刻に変換されること",
                result.get(0).get("UPDATE_COL"), is("2010-09-14 12:34:56.0"));
        assertThat("${setUpTime} はシステム時刻に変換されること",
                result.get(0).get("SET_UP_TIME_COL"), is("2010-09-14 12:34:56.0"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: "[" で始まるが "]" で終わらないキーは除外されないこと。
     *
     * <p>
     * マーカーカラムは "[COL]" 形式（両端が角括弧）のみ除外される。
     * "[OPEN" のように "[" で始まっても "]" で終わらないキーは通常カラムとして扱われること<br>
     * Given: list_maps の partialBracketColTest に "[OPEN" キーと "KEY1" キーを含む行<br>
     * When:  buildListMapRows(yaml, "partialBracketColTest", path) を呼ぶ<br>
     * Then:  "[OPEN" キーが結果 Map に含まれること（マーカーと見なされないこと）
     * </p>
     */
    @Test
    public void buildListMapRows_partialBracketKeyIsNotExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "partialBracketColTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertTrue("\"[OPEN\" はマーカーではないため結果 Map に含まれること",
                result.get(0).containsKey("[OPEN"));
        assertThat("KEY1 の値が正しいこと", result.get(0).get("KEY1"), is("real_val"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: rows 内の空エントリ（{}）は読み飛ばされること。
     *
     * <p>
     * 空マッピング行は行として存在しないものとして扱われ、結果に含まれない。<br>
     * Given: list_maps の emptyRowListMap に 通常行・{} 行・通常行 の 3 エントリ<br>
     * When:  buildListMapRows(yaml, "emptyRowListMap", path) を呼ぶ<br>
     * Then:  {} 行がスキップされ、通常行 2 件のみ返ること
     * </p>
     */
    @Test
    public void buildListMapRows_emptyRowEntrySkipped() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "emptyRowListMap", DIR);

        // Then
        assertThat("空エントリ {} をスキップして 2 件のみ返ること", result.size(), is(2));
        assertThat("1 件目の KEY1 が正しいこと", result.get(0).get("KEY1"), is("before"));
        assertFalse("1 件目は空 Map でないこと", result.get(0).isEmpty());
        assertThat("2 件目の KEY1 が正しいこと", result.get(1).get("KEY1"), is("after"));
        assertFalse("2 件目は空 Map でないこと", result.get(1).isEmpty());
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: rows に Map でない要素（スカラー）が含まれる場合はスキップされること。
     *
     * <p>
     * Given: list_maps の nonMapRowTest に 通常行・スカラー文字列・通常行 の 3 エントリ<br>
     * When:  buildListMapRows(yaml, "nonMapRowTest", path) を呼ぶ<br>
     * Then:  Map でない行はスキップされ、Map の行 2 件のみ返ること
     * </p>
     */
    @Test
    public void buildListMapRows_nonMapRowSkipped() {
        // Given: スカラー行（Map でない要素）を直接構築（スキーマ検証で弾かれるためフィクスチャでは書けない）。
        //        除外を行うのは YamlSection#dropBlankRows だが、Builder 経路を通しても落ちることを固定する。
        Map<String, Object> row1 = new java.util.LinkedHashMap<>();
        row1.put("KEY1", "valid");
        Map<String, Object> row2 = new java.util.LinkedHashMap<>();
        row2.put("KEY1", "also_valid");
        Map<String, Object> entry = new java.util.LinkedHashMap<>();
        entry.put("id", "nonMapRowTest");
        entry.put("rows", Arrays.<Object>asList(row1, "scalar_entry", row2));
        Map<String, Object> yaml = new java.util.LinkedHashMap<>();
        yaml.put("list_maps", Arrays.<Object>asList(entry));

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "nonMapRowTest", DIR);

        // Then
        assertThat("Map でない行はスキップされ 2 件のみ返ること", result.size(), is(2));
        assertThat("1 件目が正しいこと", result.get(0).get("KEY1"), is("valid"));
        assertThat("2 件目が正しいこと", result.get(1).get("KEY1"), is("also_valid"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: setSetUpDateTime 未設定時に "${setUpTime}" が変換されないこと。
     *
     * <p>
     * setSetUpDateTime を呼ばずに "${setUpTime}" を使った場合、変換されずにそのまま残ること<br>
     * Given: @Before の sut（setSetUpDateTime 未設定）で list_maps に SET_UP_TIME_COL="${setUpTime}"<br>
     * When:  buildListMapRows(yaml, "quotationTest", path) を呼ぶ<br>
     * Then:  SET_UP_TIME_COL の値が "${setUpTime}" のまま変換されないこと
     * </p>
     */
    @Test
    public void buildListMapRows_setUpTimeNotConvertedWithoutSetSetUpDateTime() {
        // Given: @Before の sut は setSetUpDateTime 未設定
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "quotationTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("setSetUpDateTime 未設定時は ${setUpTime} が変換されないこと",
                result.get(0).get("SET_UP_TIME_COL"), is("${setUpTime}"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: testShots 予約 ID で list_maps が正しく取得できること。
     *
     * <p>
     * testShots は予約 ID であり、通常の list_maps エントリと同様に取得できること<br>
     * Given: list_maps に id=testShots で no/description/expectedStatusCode/setUpTable/expectedTable カラムを持つ2件のエントリ<br>
     * When:  buildListMapRows(yaml, "testShots", path) を呼ぶ<br>
     * Then:  2件取得でき、各カラム値が保持されていること
     * </p>
     */
    @Test
    public void buildListMapRows_testShotsReservedId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "testShots", DIR);

        // Then
        assertThat("2件取得できること", result.size(), is(2));
        Map<String, String> row1 = result.get(0);
        assertThat("no カラムが保持されること", row1.get("no"), is("1"));
        assertThat("description カラムが保持されること", row1.get("description"), is("ケース1"));
        assertThat("expectedStatusCode カラムが保持されること", row1.get("expectedStatusCode"), is("200"));
        assertThat("setUpTable カラムが保持されること", row1.get("setUpTable"), is(""));
        Map<String, String> row2 = result.get(1);
        assertThat("2件目の no カラムが保持されること", row2.get("no"), is("2"));
        assertThat("2件目の setUpTable カラムが保持されること", row2.get("setUpTable"), is("case2"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: rows: [] の expected_tables エントリがカラム名 0 件・行数 0 の TableData を返すこと。
     *
     * <p>
     * Given: expected_tables に rows: [] のエントリ（newGroup_emptyExpected グループ）<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[newGroup_emptyExpected]", false, path) を呼ぶ<br>
     * Then:  サイズ 1 のリストが返り、テーブル名が "TEST_TABLE"、カラム名が 0 件、行数が 0 であること
     * </p>
     * <p>
     * カラム名が 0 件で正しいのは、rows: [] ではどの行もキーを持たず、YAML に列名を書く場所が
     * 無いためである。expected_tables では列名が 0 件でも行の有無は検証される。その理由は
     * {@code YamlTableDataBuilder#buildTableData} の javadoc に記す。
     * 同じ rows: [] でも {@code buildTableDataList_emptyExpectedCompleteTableReturnsTableDataWithAllDbColumns}
     * が 11 カラムを期待するのは、そちらが fillDefaults=true で呼ばれる経路だからである。
     * </p>
     */
    @Test
    public void buildTableDataList_emptyExpectedTableReturnsTableDataWithNoColumns() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[newGroup_emptyExpected]", false, DIR);

        // Then
        assertThat("サイズ 1 のリストが返ること", result.size(), is(1));
        assertThat("テーブル名が TEST_TABLE であること", result.get(0).getTableName(), is("TEST_TABLE"));
        assertThat("カラム名が 0 件であること（解決は本体側の責務）", result.get(0).getColumnNames().length, is(0));
        assertThat("行数が 0 であること", result.get(0).size(), is(0));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: rows: [] の expected_complete_tables エントリが dbInfo の全カラムを返し行数 0 であること。
     *
     * <p>
     * Given: expected_complete_tables に rows: [] のエントリ（newGroup_emptyComplete グループ）<br>
     * When:  buildTableDataList(yaml, "expected_complete_tables", "[newGroup_emptyComplete]", true, path) を呼ぶ<br>
     * Then:  サイズ 1 のリストが返り、テーブル名が "TEST_TABLE"、dbInfo の全カラム数（11）が返り、行数が 0 であること
     *        （fillDefaults=true でも rows:[] の場合は NPE なく動作すること）
     * </p>
     */
    @Test
    public void buildTableDataList_emptyExpectedCompleteTableReturnsTableDataWithAllDbColumns() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/completedTable");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_complete_tables", "[newGroup_emptyComplete]", true, DIR);

        // Then
        assertThat("サイズ 1 のリストが返ること", result.size(), is(1));
        assertThat("テーブル名が TEST_TABLE であること", result.get(0).getTableName(), is("TEST_TABLE"));
        assertThat("dbInfo の全カラム数（11）が返ること", result.get(0).getColumnNames().length, is(11));
        assertThat("行数が 0 であること", result.get(0).size(), is(0));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: YAML ネイティブ boolean / integer / float は文字列化されること。
     *
     * <p>
     * Given: BOOL_TRUE=true, INT_COL=42, FLOAT_COL=3.14（クォートなし）<br>
     * When:  buildListMapRows(yaml, "nativeTypeTest", path) を呼ぶ<br>
     * Then:  それぞれ "true", "42", "3.14" として取得されること
     * </p>
     */
    @Test
    public void buildListMapRows_nativeTypesStringified() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "nativeTypeTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertThat(row.get("BOOL_TRUE"), is("true"));
        assertThat(row.get("BOOL_FALSE"), is("false"));
        assertThat(row.get("INT_COL"), is("42"));
        assertThat(row.get("FLOAT_COL"), is("3.14"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: setup_tables の列名が先頭の非空マッピング行のキーで決まり、後続のデータ行が保持されること。
     *
     * <p>
     * 先頭の {} を読み飛ばした後、最初にキーを持つ行（2 行目）が列名を決めることを固定する。
     * 3 行目はキーの一部を持たないので、列名が 3 行目由来になっていれば列名リストが食い違って落ちる。<br>
     * Given: setup_tables の leadingEmptyRow グループに {} 行・5 キーの行・3 キーの行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[leadingEmptyRow]", false, path) を呼ぶ<br>
     * Then:  列名が 2 行目の 5 キーで YAML 記述順に決まり、後続 2 行が全列の値つきで返ること
     *        （3 行目に無いキーは null になること）
     * </p>
     */
    @Test
    public void buildTableDataList_leadingEmptyRowKeepsFollowingRows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[leadingEmptyRow]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("列名が先頭の非空マッピング行（2 行目）のキーで YAML 記述順に決まること",
                Arrays.asList(result.get(0).getColumnNames()),
                is(Arrays.asList("PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL", "NUMBER_COL2")));
        assertThat("先頭 {} を除く 2 行が保持されること", result.get(0).size(), is(2));
        assertThat("1 行目の PK_COL1", result.get(0).getValue(0, "PK_COL1").toString(), is("0000000030"));
        assertThat("1 行目の PK_COL2", result.get(0).getValue(0, "PK_COL2").toString(), is("CC"));
        assertThat("1 行目の VARCHAR2_COL", result.get(0).getValue(0, "VARCHAR2_COL").toString(), is("second"));
        assertThat("1 行目の NUMBER_COL", result.get(0).getValue(0, "NUMBER_COL").toString(), is("30"));
        assertThat("1 行目の NUMBER_COL2", result.get(0).getValue(0, "NUMBER_COL2").toString(), is("30.0"));
        assertThat("2 行目の PK_COL1", result.get(0).getValue(1, "PK_COL1").toString(), is("0000000031"));
        assertThat("2 行目の PK_COL2", result.get(0).getValue(1, "PK_COL2").toString(), is("DD"));
        assertThat("2 行目の VARCHAR2_COL", result.get(0).getValue(1, "VARCHAR2_COL").toString(), is("third"));
        assertNull("2 行目が持たない NUMBER_COL は null になること", result.get(0).getValue(1, "NUMBER_COL"));
        assertNull("2 行目が持たない NUMBER_COL2 は null になること", result.get(0).getValue(1, "NUMBER_COL2"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_tables の列名が先頭の非空マッピング行のキーで決まり、後続のデータ行が保持されること。
     *
     * <p>
     * 先頭の {} を読み飛ばした後、最初にキーを持つ行（2 行目）が列名を決めることを固定する。
     * 3 行目はキーの一部を持たないので、列名が 3 行目由来になっていれば列名リストが食い違って落ちる。<br>
     * Given: expected_tables の leadingEmptyRowExpected グループに {} 行・5 キーの行・3 キーの行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[leadingEmptyRowExpected]", false, path) を呼ぶ<br>
     * Then:  列名が 2 行目の 5 キーで YAML 記述順に決まり、後続 2 行が全列の値つきで返ること
     *        （3 行目に無いキーは null になること）
     * </p>
     */
    @Test
    public void buildTableDataList_leadingEmptyRowInExpectedTableKeepsFollowingRows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[leadingEmptyRowExpected]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("列名が先頭の非空マッピング行（2 行目）のキーで YAML 記述順に決まること",
                Arrays.asList(result.get(0).getColumnNames()),
                is(Arrays.asList("PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL", "NUMBER_COL2")));
        assertThat("先頭 {} を除く 2 行が保持されること", result.get(0).size(), is(2));
        assertThat("1 行目の PK_COL1", result.get(0).getValue(0, "PK_COL1").toString(), is("0000000040"));
        assertThat("1 行目の PK_COL2", result.get(0).getValue(0, "PK_COL2").toString(), is("EE"));
        assertThat("1 行目の VARCHAR2_COL", result.get(0).getValue(0, "VARCHAR2_COL").toString(), is("expected"));
        assertThat("1 行目の NUMBER_COL", result.get(0).getValue(0, "NUMBER_COL").toString(), is("40"));
        assertThat("1 行目の NUMBER_COL2", result.get(0).getValue(0, "NUMBER_COL2").toString(), is("40.0"));
        assertThat("2 行目の PK_COL1", result.get(0).getValue(1, "PK_COL1").toString(), is("0000000041"));
        assertThat("2 行目の PK_COL2", result.get(0).getValue(1, "PK_COL2").toString(), is("FF"));
        assertThat("2 行目の VARCHAR2_COL", result.get(0).getValue(1, "VARCHAR2_COL").toString(), is("expected2"));
        assertNull("2 行目が持たない NUMBER_COL は null になること", result.get(0).getValue(1, "NUMBER_COL"));
        assertNull("2 行目が持たない NUMBER_COL2 は null になること", result.get(0).getValue(1, "NUMBER_COL2"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_complete_tables の先頭行が空マッピング（{}）でも後続のデータ行が保持され、省略カラムが補完されること。
     *
     * <p>
     * fillDefaults=true の経路は {@code fillDefaultValues()} が列名を dbInfo の全カラムへ差し替えるため、
     * 先頭 {} で列名解決に失敗すると行そのものが落ちる。行が残ること・YAML に書いた値が
     * デフォルト値で上書きされないことを固定する。<br>
     * Given: expected_complete_tables の leadingEmptyRowComplete グループに {} 行・3 キーの行・2 キーの行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_complete_tables", "[leadingEmptyRowComplete]", true, path) を呼ぶ<br>
     * Then:  2 行が保持され、dbInfo の全カラム数（11）になり、YAML に書いた値はそのまま・
     *        省略カラムはデフォルト値で補完されること
     * </p>
     */
    @Test
    public void buildTableDataList_leadingEmptyRowInExpectedCompleteTableKeepsFollowingRows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/completedTable");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_complete_tables", "[leadingEmptyRowComplete]", true, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("dbInfo の全カラム数（11）に差し替わること", result.get(0).getColumnNames().length, is(11));
        assertThat("先頭 {} を除く 2 行が保持されること", result.get(0).size(), is(2));
        assertThat("1 行目の PK_COL1", result.get(0).getValue(0, "PK_COL1").toString(), is("0000000098"));
        assertThat("1 行目の PK_COL2", result.get(0).getValue(0, "PK_COL2").toString(), is("YY"));
        assertThat("1 行目の VARCHAR2_COL が YAML の値のままであること",
                result.get(0).getValue(0, "VARCHAR2_COL").toString(), is("complete"));
        assertThat("1 行目の省略カラム NUMBER_COL がデフォルト値で補完されること",
                result.get(0).getValue(0, "NUMBER_COL").toString(), is("0"));
        assertThat("2 行目の PK_COL1", result.get(0).getValue(1, "PK_COL1").toString(), is("0000000097"));
        assertThat("2 行目の PK_COL2", result.get(0).getValue(1, "PK_COL2").toString(), is("XY"));
        assertNull("2 行目が持たない VARCHAR2_COL は列名に含まれるため補完対象外で null になること",
                result.get(0).getValue(1, "VARCHAR2_COL"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: list_maps の列名が先頭の非空マッピング行のキーで決まり、後続のデータ行が保持されること。
     *
     * <p>
     * 先頭の {} を読み飛ばした後、最初にキーを持つ行（2 行目）が列名を決めることを固定する。
     * 3 行目は KEY2 を持たないので、列名が 3 行目由来になっていれば 2 件目の KEY2 が落ちて検出できる。<br>
     * Given: list_maps の leadingEmptyRowListMap に {} 行・KEY1/KEY2 の行・KEY1 のみの行 の 3 エントリ<br>
     * When:  buildListMapRows(yaml, "leadingEmptyRowListMap", path) を呼ぶ<br>
     * Then:  先頭 {} を除く 2 件が返り、いずれも 2 行目のキーから解決した列を持つこと
     *        （2 件目に無い KEY2 は null で保持されること）
     * </p>
     */
    @Test
    public void buildListMapRows_leadingEmptyRowKeepsFollowingRows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "leadingEmptyRowListMap", DIR);

        // Then
        assertThat("先頭 {} を除く 2 件が返ること", result.size(), is(2));
        assertThat("列名が先頭の非空マッピング行（2 行目）のキーで決まること",
                new ArrayList<String>(result.get(0).keySet()), is(Arrays.asList("KEY1", "KEY2")));
        assertThat("1 件目の KEY1 が保持されること", result.get(0).get("KEY1"), is("second"));
        assertThat("1 件目の KEY2 が保持されること", result.get(0).get("KEY2"), is("val2"));
        assertThat("2 件目も 2 行目のキーで列が決まること",
                new ArrayList<String>(result.get(1).keySet()), is(Arrays.asList("KEY1", "KEY2")));
        assertThat("2 件目の KEY1 が保持されること", result.get(1).get("KEY1"), is("third"));
        assertNull("2 件目が持たない KEY2 は null になること", result.get(1).get("KEY2"));
    }

    // ========================================================================
    // 空マッピング（{}）の行、および全ての値が null または空文字の行が、
    // 行として存在しないものとして扱われること
    // ========================================================================

    /**
     * [YamlTableDataBuilder] buildTableDataList: setup_tables の先頭行の値が全て null／空文字の場合、その行が列名解決からもデータ行からも除外されること。
     *
     * <p>
     * 全ての値が null または空文字の行は、Excel の全セル空行と同様に行として存在しないものとして扱う。
     * 先頭行のキー集合を後続行と変えてあるので、列名がその行から決まっていれば列名リストが食い違って落ちる。<br>
     * Given: setup_tables の blankValueRowLeading グループに 値が全て空の 2 キーの行・5 キーの通常行 の 2 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[blankValueRowLeading]", false, path) を呼ぶ<br>
     * Then:  列名が 2 行目の 5 キーで決まり、データ行は 1 件のみになること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowLeadingExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[blankValueRowLeading]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("列名が値を持つ行（2 行目）のキーで YAML 記述順に決まること",
                Arrays.asList(result.get(0).getColumnNames()),
                is(Arrays.asList("PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL", "NUMBER_COL2")));
        assertThat("先頭の全値空行を除く 1 行のみ返ること", result.get(0).size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000050"));
        assertThat(result.get(0).getValue(0, "VARCHAR2_COL").toString(), is("leading"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: setup_tables の中間行の値が全て null／空文字の場合、その行が除外されること。
     *
     * <p>
     * Given: setup_tables の blankValueRowMiddle グループに 通常行・値が全て空の行・通常行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[blankValueRowMiddle]", false, path) を呼ぶ<br>
     * Then:  全値空の行がスキップされ、2 行のみ返ること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowMiddleExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[blankValueRowMiddle]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("中間の全値空行をスキップして 2 行のみ返ること", result.get(0).size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000051"));
        assertThat(result.get(0).getValue(1, "PK_COL1").toString(), is("0000000052"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: 値が 1 つでも非空の行は保持されること。
     *
     * <p>
     * 除去対象は「全ての値が null または空文字」の行だけであり、1 つでも非空の値を持つ行は
     * 従来どおりデータ行として扱われる。<br>
     * Given: setup_tables の partiallyBlankValueRow グループに 一部のみ値を持つ行・値が全て空の行 の 2 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[partiallyBlankValueRow]", false, path) を呼ぶ<br>
     * Then:  一部のみ値を持つ行だけが 1 行返り、空文字カラムは空文字・null カラムは null で保持されること
     * </p>
     */
    @Test
    public void buildTableDataList_partiallyBlankValueRowKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[partiallyBlankValueRow]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("値を 1 つでも持つ行のみ 1 行返ること", result.get(0).size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000053"));
        assertThat(result.get(0).getValue(0, "PK_COL2").toString(), is("JJ"));
        assertThat("空文字のカラムは空文字のまま保持されること",
                result.get(0).getValue(0, "VARCHAR2_COL").toString(), is(""));
        assertNull("null のカラムは null のまま保持されること", result.get(0).getValue(0, "NUMBER_COL"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_tables の先頭行の値が全て null／空文字の場合、その行が列名解決からもデータ行からも除外されること。
     *
     * <p>
     * Given: expected_tables の blankValueRowLeadingExpected グループに 値が全て空の 2 キーの行・5 キーの通常行 の 2 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[blankValueRowLeadingExpected]", false, path) を呼ぶ<br>
     * Then:  列名が 2 行目の 5 キーで決まり、データ行は 1 件のみになること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowLeadingInExpectedTableExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[blankValueRowLeadingExpected]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("列名が値を持つ行（2 行目）のキーで YAML 記述順に決まること",
                Arrays.asList(result.get(0).getColumnNames()),
                is(Arrays.asList("PK_COL1", "PK_COL2", "VARCHAR2_COL", "NUMBER_COL", "NUMBER_COL2")));
        assertThat("先頭の全値空行を除く 1 行のみ返ること", result.get(0).size(), is(1));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000062"));
        assertThat(result.get(0).getValue(0, "VARCHAR2_COL").toString(), is("leading"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_tables の中間行の値が全て null／空文字の場合、その行が除外されること。
     *
     * <p>
     * Given: expected_tables の blankValueRowMiddleExpected グループに 通常行・値が全て空の行・通常行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[blankValueRowMiddleExpected]", false, path) を呼ぶ<br>
     * Then:  全値空の行がスキップされ、2 行のみ返ること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowMiddleInExpectedTableExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[blankValueRowMiddleExpected]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("中間の全値空行をスキップして 2 行のみ返ること", result.get(0).size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000063"));
        assertThat(result.get(0).getValue(1, "PK_COL1").toString(), is("0000000064"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_tables の rows 内の空エントリ（{}）は読み飛ばされること。
     *
     * <p>
     * Given: expected_tables の emptyRowMixedExpected グループに 通常行・{} 行・通常行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[emptyRowMixedExpected]", false, path) を呼ぶ<br>
     * Then:  {} 行がスキップされ、2 行のみ返ること
     * </p>
     */
    @Test
    public void buildTableDataList_emptyRowEntryInExpectedTableSkipped() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[emptyRowMixedExpected]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("空エントリ {} をスキップして 2 行のみ返ること", result.get(0).size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000060"));
        assertThat(result.get(0).getValue(1, "PK_COL1").toString(), is("0000000061"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_complete_tables でも値が全て null／空文字の行が除外されること。
     *
     * <p>
     * fillDefaults=true の経路は {@code fillDefaultValues()} が列名を dbInfo の全カラムへ差し替えるため、
     * 「全カラムを空で書いた行が無言で消える」という挙動変化がこの経路にも及ぶ。先頭・中間・末尾の
     * どこに置いても除外されること、および残った行が列名解決の基準になることを固定する。
     * 末尾の 1 件は「最終行は常に残す」という変異を落とすための門番である。<br>
     * Given: expected_complete_tables の blankValueRowComplete グループに
     *        値が全て空の 2 キーの行（先頭）・3 キーの通常行・値が全て空の 3 キーの行（中間）・
     *        3 キーの通常行・値が全て空の 2 キーの行（末尾） の 5 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_complete_tables", "[blankValueRowComplete]", true, path) を呼ぶ<br>
     * Then:  全値空の 3 行が除外されて通常行 2 行のみ残り、dbInfo の全カラム数（11）に差し替わること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowInExpectedCompleteTableExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/completedTable");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_complete_tables", "[blankValueRowComplete]", true, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("先頭・中間・末尾の全値空行が除外されて通常行 2 行のみ残ること", result.get(0).size(), is(2));
        assertThat("dbInfo の全カラム数（11）に差し替わること", result.get(0).getColumnNames().length, is(11));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000096"));
        assertThat(result.get(0).getValue(0, "PK_COL2").toString(), is("WW"));
        assertThat("YAML に書いた値がデフォルト値で上書きされないこと",
                result.get(0).getValue(0, "VARCHAR2_COL").toString(), is("kept"));
        assertThat("中間の全値空行が除外され、2 行目が YAML の 4 行目になること",
                result.get(0).getValue(1, "PK_COL1").toString(), is("0000000095"));
        assertThat(result.get(0).getValue(1, "PK_COL2").toString(), is("VV"));
        assertThat("末尾の全値空行が除外され、最後の行が YAML の 4 行目のままであること",
                result.get(0).getValue(1, "VARCHAR2_COL").toString(), is("kept2"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: list_maps の先頭行の値が全て null／空文字の場合、その行が列名解決からも結果からも除外されること。
     *
     * <p>
     * 先頭行のキー集合を後続行と変えてあるので、列名がその行から決まっていれば結果のキーが食い違って落ちる。<br>
     * Given: list_maps の blankValueRowLeadingListMap に 値が全て空の KEY9/KEY8 の行・KEY1/KEY2 の行 の 2 エントリ<br>
     * When:  buildListMapRows(yaml, "blankValueRowLeadingListMap", path) を呼ぶ<br>
     * Then:  1 件のみ返り、そのキーが 2 行目の KEY1／KEY2 であること
     * </p>
     */
    @Test
    public void buildListMapRows_blankValueRowLeadingExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "blankValueRowLeadingListMap", DIR);

        // Then
        assertThat("先頭の全値空行を除く 1 件のみ返ること", result.size(), is(1));
        assertThat("列名が値を持つ行（2 行目）のキーで決まること",
                new ArrayList<String>(result.get(0).keySet()), is(Arrays.asList("KEY1", "KEY2")));
        assertThat(result.get(0).get("KEY1"), is("second"));
        assertThat(result.get(0).get("KEY2"), is("val2"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: list_maps の中間行の値が全て null／空文字の場合、その行が除外されること。
     *
     * <p>
     * Given: list_maps の blankValueRowMiddleListMap に 通常行・値が全て空の行・通常行 の 3 エントリ<br>
     * When:  buildListMapRows(yaml, "blankValueRowMiddleListMap", path) を呼ぶ<br>
     * Then:  全値空の行がスキップされ、2 件のみ返ること
     * </p>
     */
    @Test
    public void buildListMapRows_blankValueRowMiddleExcluded() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "blankValueRowMiddleListMap", DIR);

        // Then
        assertThat("中間の全値空行をスキップして 2 件のみ返ること", result.size(), is(2));
        assertThat(result.get(0).get("KEY1"), is("before"));
        assertThat(result.get(1).get("KEY1"), is("after"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 値が 1 つでも非空の行は保持されること。
     *
     * <p>
     * Given: list_maps の partiallyBlankValueRowListMap に 一部のみ値を持つ行・値が全て空の行 の 2 エントリ<br>
     * When:  buildListMapRows(yaml, "partiallyBlankValueRowListMap", path) を呼ぶ<br>
     * Then:  一部のみ値を持つ行だけが 1 件返り、空文字キーは空文字・null キーは null で保持されること
     * </p>
     */
    @Test
    public void buildListMapRows_partiallyBlankValueRowKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "partiallyBlankValueRowListMap", DIR);

        // Then
        assertThat("値を 1 つでも持つ行のみ 1 件返ること", result.size(), is(1));
        assertThat(result.get(0).get("KEY1"), is("kept"));
        assertThat("空文字のキーは空文字のまま保持されること", result.get(0).get("KEY2"), is(""));
        assertNull("null のキーは null のまま保持されること", result.get(0).get("KEY3"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: 値加工を通すと全ての値が Java null になる行も、行として保持されること。
     *
     * <p>
     * 空行判定（{@code YamlSection#dropBlankRows}）は値加工（{@code YamlSection#interpret}）より前に
     * 行う。この順序は依存先 nablarch-testing の {@code TestDataParsingTemplate#readTestData}
     * （空行判定 → 値加工）に揃えたものであり、この変更の要となる設計判断である。
     * クォートあり {@code "null"} は {@code NullInterpreter} が Java null へ変換するため、
     * {@code "null"} だけを持つ行は「判定が値加工より前なら保持され、後なら消える」区別がつく。
     * 判定を値加工の後ろへずらすとこのテストが落ちる。<br>
     * Given: setup_tables の interpretedToNullRow グループに 通常行・全ての値が "null" の行 の 2 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[interpretedToNullRow]", false, path) を呼ぶ<br>
     * Then:  2 行とも保持され、2 行目の全カラムが Java null になること
     * </p>
     */
    @Test
    public void buildTableDataList_rowInterpretedToAllNullIsKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[interpretedToNullRow]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("値加工後に全て null になる行も行として保持され、2 行返ること", result.get(0).size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000054"));
        assertThat(result.get(0).getValue(0, "VARCHAR2_COL").toString(), is("kept"));
        assertNull("2 行目の PK_COL1 は NullInterpreter により Java null になること",
                result.get(0).getValue(1, "PK_COL1"));
        assertNull("2 行目の PK_COL2 は NullInterpreter により Java null になること",
                result.get(0).getValue(1, "PK_COL2"));
        assertNull("2 行目の VARCHAR2_COL は NullInterpreter により Java null になること",
                result.get(0).getValue(1, "VARCHAR2_COL"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 値加工を通すと全ての値が Java null になる行も、行として保持されること。
     *
     * <p>
     * list_maps 経路でも空行判定を値加工より前に行うことを固定する（趣旨は
     * {@code buildTableDataList_rowInterpretedToAllNullIsKept} と同じ）。<br>
     * Given: list_maps の interpretedToNullRowListMap に 通常行・全ての値が "null" の行 の 2 エントリ<br>
     * When:  buildListMapRows(yaml, "interpretedToNullRowListMap", path) を呼ぶ<br>
     * Then:  2 件とも保持され、2 件目の全キーが Java null になること
     * </p>
     */
    @Test
    public void buildListMapRows_rowInterpretedToAllNullIsKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "interpretedToNullRowListMap", DIR);

        // Then
        assertThat("値加工後に全て null になる行も行として保持され、2 件返ること", result.size(), is(2));
        assertThat(result.get(0).get("KEY1"), is("kept"));
        assertThat(result.get(0).get("KEY2"), is("val"));
        assertTrue("2 件目は KEY1 をキーとして持つこと", result.get(1).containsKey("KEY1"));
        assertNull("2 件目の KEY1 は NullInterpreter により Java null になること", result.get(1).get("KEY1"));
        assertNull("2 件目の KEY2 は NullInterpreter により Java null になること", result.get(1).get("KEY2"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 全行が行として存在しないものの場合は空リストが返ること。
     *
     * <p>
     * テーブル系の {@code buildTableDataList_allEmptyRowsReturnsTableDataWithNoColumns} と対になる
     * list_maps 経路の固定。残る行が 0 件のとき列名も決まらないため、行 0 件の空リストが返る。<br>
     * Given: list_maps の allBlankRowsListMap に {} 行・値が全て null／空文字の行・{} 行 の 3 エントリ<br>
     * When:  buildListMapRows(yaml, "allBlankRowsListMap", path) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void buildListMapRows_allBlankRowsReturnsEmptyList() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "allBlankRowsListMap", DIR);

        // Then
        assertTrue("全行が行として存在しないものの場合は空リストが返ること", result.isEmpty());
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: マーカーカラムだけが値を持つ行は行として残り、結果が空 Map になること。
     *
     * <p>
     * 空行判定はマーカーカラム（{@code [COL]}）の値も対象に含めるため、マーカーカラムだけが値を持つ
     * 行は「全ての値が null／空文字」ではなく、行としては残る。一方 {@code list_maps} の結果組み立ては
     * マーカーカラムを DB 操作対象外として除外するため、有効な列が 1 つも無いこの行は空 Map になる。
     * この「行は残るが中身は空 Map」という組み合わせは、依存先 nablarch-testing の
     * {@code ListMapParser#onReadLine} が {@code HeaderLine#getMapExcludingMarkerColumns} の戻り値を
     * 無条件に結果へ積む（マーカーを除いた有効カラムが 0 件なら空 Map になる）挙動と一致する。<br>
     * Given: list_maps の markerOnlyRowListMap に {@code "[NO]": "1"} だけを持つ行 1 エントリ<br>
     * When:  buildListMapRows(yaml, "markerOnlyRowListMap", path) を呼ぶ<br>
     * Then:  1 件返り、その要素が空 Map であること
     * </p>
     */
    @Test
    public void buildListMapRows_markerOnlyRowKeptAsEmptyMap() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "markerOnlyRowListMap", DIR);

        // Then
        assertThat("マーカーカラムだけが値を持つ行も行としては残ること", result.size(), is(1));
        assertTrue("マーカーカラムは除外されるため、結果の要素は空 Map になること", result.get(0).isEmpty());
    }

}
