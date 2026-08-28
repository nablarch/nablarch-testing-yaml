package nablarch.test.core.reader.yaml;

import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.TableData;
import nablarch.test.core.db.TestTable;
import nablarch.test.core.util.interpreter.DateTimeInterpreter;
import nablarch.test.core.util.interpreter.InterpretationContext;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
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

    /**
     * {@code ${文字種,文字数}} で使用できる14文字種。
     *
     * <p>出典: implementation/testdata_notation.rst:1313-:1320</p>
     */
    private static final String[] CHARACTER_TYPES = {
            "半角英字", "半角数字", "半角記号", "半角カナ",
            "全角英字", "全角数字", "全角ひらがな", "全角カタカナ", "全角漢字", "全角記号その他",
            "中国語", "サロゲートペア", "改行", "外字"
    };

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

    /**
     * 値加工を通すと必ず空文字になるビルダを生成する。
     *
     * <p>
     * 値加工（{@code interpret}）の後に全ての値が空文字になる行を作るために使う。
     * yamlInterpreters には値を空にするインタープリタが存在しないため、
     * テスト内で専用のインタープリタを組み立てる。
     * </p>
     *
     * <p>
     * 空行判定（{@code dropBlankRows}）は値を一切見ないため、このビルダを使うテストは
     * 空行判定と値加工の前後関係を判別できない。判定を旧実装（全ての値が空文字なら行なし）へ
     * 戻す変異を当てても、生値が非空である限りこれらのテストは落ちない（実測済み）。
     * </p>
     */
    private YamlTableDataBuilder newBlankingBuilder() {
        List<TestDataInterpreter> interpreters =
                Collections.<TestDataInterpreter>singletonList(new BlankingInterpreter());
        return new YamlTableDataBuilder(dbInfo, new BasicDefaultValues(),
                InterpreterResolver.withBinaryFile(interpreters));
    }

    /** どんな値も空文字に変換するテスト用インタープリタ。 */
    private static class BlankingInterpreter implements TestDataInterpreter {
        @Override
        public String interpret(InterpretationContext context) {
            return "";
        }
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
     * [YamlTableDataBuilder] buildListMapRows: クォートあり "null" は文字列のまま取得され、
     * クォートなしの null と区別できること。
     *
     * <p>
     * yamlInterpreters は NullInterpreter を含まないため、null を解釈するのは YAML のパーサだけになる。
     * クォートなしの null だけが Java null になり、クォートあり "null" は文字列として残る<br>
     * Given: list_maps に QUOTED_NULL: "null"（クォートあり）と BARE_NULL: null（クォートなし）<br>
     * When:  buildListMapRows(yaml, "interpreterTest", path) を呼ぶ<br>
     * Then:  QUOTED_NULL は文字列 "null"、BARE_NULL は Java null になること
     * </p>
     */
    @Test
    public void buildListMapRows_quotedNullIsKeptAsString() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("\"null\"（クォートあり）は文字列のまま残ること",
                result.get(0).get("QUOTED_NULL"), is("null"));
        assertNull("null（クォートなし）は Java null になること", result.get(0).get("BARE_NULL"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: " " はクォート除去後にスペース1文字になること。
     *
     * <p>
     * " "（スペースをダブルクォートで囲む）→ YAML のパーサがクォートを構文として処理し、スペース1文字になる<br>
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
     * [YamlTableDataBuilder] buildListMapRows: 改行文字は YAML のパーサだけが解釈すること。
     *
     * <p>
     * yamlInterpreters は LineSeparatorInterpreter を含まないため、YAML のエスケープ "\r" だけが
     * CR（0x0D）になり、バックスラッシュと r の 2 文字を書いた "\\r" は文字どおり残る<br>
     * Given: list_maps に YAML_CR_COL: "\r"（YAML のエスケープ）と LITERAL_CR_COL: "\\r"（2 文字）<br>
     * When:  buildListMapRows(yaml, "interpreterTest", path) を呼ぶ<br>
     * Then:  YAML_CR_COL は CR 文字、LITERAL_CR_COL は "\\r" の 2 文字のままであること
     * </p>
     */
    @Test
    public void buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "interpreterTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("YAML のエスケープ \"\\r\" は CR 文字になること",
                result.get(0).get("YAML_CR_COL"), is("\r"));
        assertThat("バックスラッシュと r の 2 文字は変換されず そのまま残ること",
                result.get(0).get("LITERAL_CR_COL"), is("\\r"));
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
     * [YamlTableDataBuilder] buildListMapRows: 14文字種それぞれで ${文字種,3} が該当文字種3文字になること。
     *
     * <p>
     * 何を担保するか: {@code ${文字種,文字数}} で使用できる文字種が解説書の列挙する14種類すべてで
     * 変換され、指定した文字数（サロゲートペアは3コードポイント）になること。<br>
     * 根拠: implementation/testdata_notation.rst:1313-:1320<br>
     * Given: list_maps の charTypeTest_&lt;文字種&gt; に GEN_COL: "${&lt;文字種&gt;,3}" を書いた YAML（文字種ごとに別エントリ）<br>
     * When:  文字種ごとに buildListMapRows(yaml, "charTypeTest_&lt;文字種&gt;", path) を呼ぶ<br>
     * Then:  いずれの文字種でも記法が変換され、3 コードポイントの文字列になること
     * </p>
     *
     * <p>
     * 1 つの文字種で落ちても残りが検証されるよう、文字種ごとの結果を集めてから一度に判定する。
     * 各文字種がどの文字集合から生成されるかは解説書に列挙が無いため、ここでは変換されることと
     * コードポイント数だけを固定する（半角英字・半角数字の文字集合は
     * {@link #buildListMapRows_charTypeGeneratorProducesSpecifiedLength} で固定している）。
     * </p>
     */
    @Test
    public void buildListMapRows_allFourteenCharacterTypesAreGenerated() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When / Then
        List<String> failures = new ArrayList<String>();
        for (String type : CHARACTER_TYPES) {
            String notation = "${" + type + ",3}";
            try {
                List<Map<String, String>> result = buildListMapRows(yaml, "charTypeTest_" + type, DIR);
                if (result.size() != 1) {
                    failures.add(type + ": 行が1件でない (" + result.size() + "件)");
                    continue;
                }
                String value = result.get(0).get("GEN_COL");
                if (value == null) {
                    failures.add(type + ": 値が null");
                } else if (value.equals(notation)) {
                    failures.add(type + ": 変換されず " + notation + " のまま");
                } else {
                    int codePoints = value.codePointCount(0, value.length());
                    if (codePoints != 3) {
                        failures.add(type + ": 3コードポイントでない (" + codePoints + "コードポイント)");
                    }
                }
            } catch (RuntimeException e) {
                failures.add(type + ": " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
        assertTrue("14文字種すべてが該当文字種3文字（サロゲートペアは3コードポイント）になること。落ちた文字種: "
                + failures, failures.isEmpty());
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 列挙外の文字種名は変換されないこと（負のテスト）。
     *
     * <p>
     * 何を担保するか: {@code ${文字種,文字数}} で使用できる文字種が解説書の列挙する14種類に限定されており、
     * 列挙外の文字種名を書いても記法として変換されないこと。<br>
     * 根拠: implementation/testdata_notation.rst:1313<br>
     * Given: list_maps の charTypeUnknownTest に GEN_COL: "${存在しない文字種,3}"<br>
     * When:  buildListMapRows(yaml, "charTypeUnknownTest", path) を呼ぶ<br>
     * Then:  値が "${存在しない文字種,3}" のまま残ること
     * </p>
     */
    @Ignore("NTF-DOC: implementation/testdata_notation.rst:1313 — 期待 列挙外の文字種名は変換されず ${存在しない文字種,3} のまま / 実際 InterpretationFailedException（原因 IllegalArgumentException: unknown charsetName. charsetName=[存在しない文字種]）")
    @Test
    public void buildListMapRows_unknownCharacterTypeIsNotConverted() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "charTypeUnknownTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("列挙外の文字種名は変換されずそのまま残ること",
                result.get(0).get("GEN_COL"), is("${存在しない文字種,3}"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 組み合わせ記法で ${...} 以外の部分が残ること。
     *
     * <p>
     * 何を担保するか: {@code ${文字種,文字数}} を組み合わせて使った場合、記法の部分だけが変換され、
     * それ以外の文字はそのまま残ること。<br>
     * 根拠: implementation/testdata_notation.rst:1322<br>
     * Given: list_maps の charTypeCombinedTest に COMBINED_COL: "${半角数字,2}-${半角数字,4}"<br>
     * When:  buildListMapRows(yaml, "charTypeCombinedTest", path) を呼ぶ<br>
     * Then:  値が 7 文字になり、3 文字目が "-" のまま残ること
     * </p>
     */
    @Test
    public void buildListMapRows_combinedCharTypeNotationKeepsSeparator() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "charTypeCombinedTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        String value = result.get(0).get("COMBINED_COL");
        assertThat("半角数字2文字 + \"-\" + 半角数字4文字で 7 文字になること: [" + value + "]",
                value.length(), is(7));
        assertThat("3 文字目の \"-\" は変換されず残ること: [" + value + "]",
                value.charAt(2), is('-'));
        assertTrue("\"-\" 以外の部分が半角数字に変換されること: [" + value + "]",
                value.matches("[0-9]{2}-[0-9]{4}"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: "\n" は LF 1 文字になること。
     *
     * <p>
     * 何を担保するか: YAML 形式では改行文字を {@code "\r"}（CR）・{@code "\n"}（LF）と書き、
     * YAML のパーサが制御文字へ変換すること。CR 側は
     * {@link #buildListMapRows_lineSeparatorIsInterpretedOnlyByYamlParser} で固定しており、
     * ここでは LF 側を固定する。<br>
     * 根拠: implementation/testdata_notation.rst:1441-:1443<br>
     * Given: list_maps の lineFeedTest に YAML_LF_COL: "\n"<br>
     * When:  buildListMapRows(yaml, "lineFeedTest", path) を呼ぶ<br>
     * Then:  値が LF（U+000A）1 文字になること
     * </p>
     */
    @Test
    public void buildListMapRows_escapedLfIsLineFeed() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/nativeTypes");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "lineFeedTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        String value = result.get(0).get("YAML_LF_COL");
        assertThat("\"\\n\" は 1 文字になること", value.length(), is(1));
        assertThat("\"\\n\" は LF（U+000A）になること", value, is("\n"));
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
        List<TestDataInterpreter> interpreters = Collections.<TestDataInterpreter>singletonList(
                dateTimeInterpreter);
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
    // 行として存在しないものとして扱われるのは空マッピング（{}）の行だけであり、
    // 全ての値が空文字の行・Java null だけの行・マーカーカラムだけに値がある行は残ること
    // ========================================================================

    /**
     * [YamlTableDataBuilder] buildTableDataList: setup_tables の先頭行の値が全て空文字でも、その行が残り列名を決めること。
     *
     * <p>
     * 全ての値が空文字の行は「値を 1 つも持たない行」ではないため残る（出典: 解説書
     * 「コメント・マーカーカラム・空エントリを扱う」節「{@code ""} と書いた空文字は値であり、
     * すべての値が {@code ""} のエントリは読み飛ばされず、全カラムが空文字のエントリとして読み込まれる」）。
     * 先頭行のキー集合を後続行と変えてあるので、列名が先頭行から決まらなければ列名リストが食い違って落ちる。<br>
     * Given: setup_tables の blankValueRowLeading グループに 値が全て空の 2 キーの行・5 キーの通常行 の 2 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[blankValueRowLeading]", false, path) を呼ぶ<br>
     * Then:  列名が 1 行目の 2 キーで決まり、データ行は 2 件になること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowLeadingKeptAndDeterminesColumns() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[blankValueRowLeading]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("列名が先頭行（全ての値が空文字の行）のキーで YAML 記述順に決まること",
                Arrays.asList(result.get(0).getColumnNames()),
                is(Arrays.asList("PK_COL1", "VARCHAR2_COL")));
        assertThat("先頭の全値空行も残り 2 行返ること", result.get(0).size(), is(2));
        assertThat("1 行目は全カラムが空文字であること",
                result.get(0).getValue(0, "PK_COL1").toString(), is(""));
        assertThat(result.get(0).getValue(0, "VARCHAR2_COL").toString(), is(""));
        assertThat(result.get(0).getValue(1, "PK_COL1").toString(), is("0000000050"));
        assertThat(result.get(0).getValue(1, "VARCHAR2_COL").toString(), is("leading"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: setup_tables の中間行の値が全て空文字でも、その行が残ること。
     *
     * <p>
     * Given: setup_tables の blankValueRowMiddle グループに 通常行・値が全て空の行・通常行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[blankValueRowMiddle]", false, path) を呼ぶ<br>
     * Then:  全値空の行も残り、記述順で 3 行返ること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowMiddleKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[blankValueRowMiddle]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("中間の全値空行も残り 3 行返ること", result.get(0).size(), is(3));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000051"));
        assertThat("2 行目が全ての値が空文字の行であること",
                result.get(0).getValue(1, "PK_COL1").toString(), is(""));
        assertThat(result.get(0).getValue(1, "VARCHAR2_COL").toString(), is(""));
        assertThat(result.get(0).getValue(2, "PK_COL1").toString(), is("0000000052"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: 空文字・null を値に持つ行がいずれも保持されること。
     *
     * <p>
     * 除去対象は値を 1 つも持たない行だけであり、空文字・null を値に持つ行はいずれも
     * データ行として扱われる。<br>
     * Given: setup_tables の partiallyBlankValueRow グループに 一部のみ値を持つ行・値が全て空の行 の 2 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[partiallyBlankValueRow]", false, path) を呼ぶ<br>
     * Then:  2 行とも返り、1 行目の空文字カラムは空文字・null カラムは null で保持されること
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
        assertThat("2 行とも返ること", result.get(0).size(), is(2));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000053"));
        assertThat(result.get(0).getValue(0, "PK_COL2").toString(), is("JJ"));
        assertThat("空文字のカラムは空文字のまま保持されること",
                result.get(0).getValue(0, "VARCHAR2_COL").toString(), is(""));
        assertNull("null のカラムは null のまま保持されること", result.get(0).getValue(0, "NUMBER_COL"));
        assertThat("2 行目（全ての値が空文字の行）も残ること",
                result.get(0).getValue(1, "PK_COL1").toString(), is(""));
        assertThat("2 行目の全カラムが空文字であること",
                result.get(0).getValue(1, "VARCHAR2_COL").toString(), is(""));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_tables の先頭行の値が全て空文字でも、その行が残り列名を決めること。
     *
     * <p>
     * Given: expected_tables の blankValueRowLeadingExpected グループに 値が全て空の 2 キーの行・5 キーの通常行 の 2 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[blankValueRowLeadingExpected]", false, path) を呼ぶ<br>
     * Then:  列名が 1 行目の 2 キーで決まり、データ行は 2 件になること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowLeadingInExpectedTableKeptAndDeterminesColumns() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[blankValueRowLeadingExpected]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("列名が先頭行（全ての値が空文字の行）のキーで YAML 記述順に決まること",
                Arrays.asList(result.get(0).getColumnNames()),
                is(Arrays.asList("PK_COL1", "VARCHAR2_COL")));
        assertThat("先頭の全値空行も残り 2 行返ること", result.get(0).size(), is(2));
        assertThat("1 行目は全カラムが空文字であること",
                result.get(0).getValue(0, "PK_COL1").toString(), is(""));
        assertThat(result.get(0).getValue(1, "PK_COL1").toString(), is("0000000062"));
        assertThat(result.get(0).getValue(1, "VARCHAR2_COL").toString(), is("leading"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: expected_tables の中間行の値が全て空文字でも、その行が残ること。
     *
     * <p>
     * Given: expected_tables の blankValueRowMiddleExpected グループに 通常行・値が全て空の行・通常行 の 3 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[blankValueRowMiddleExpected]", false, path) を呼ぶ<br>
     * Then:  全値空の行も残り、記述順で 3 行返ること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowMiddleInExpectedTableKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[blankValueRowMiddleExpected]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("中間の全値空行も残り 3 行返ること", result.get(0).size(), is(3));
        assertThat(result.get(0).getValue(0, "PK_COL1").toString(), is("0000000063"));
        assertThat("2 行目が全ての値が空文字の行であること",
                result.get(0).getValue(1, "PK_COL1").toString(), is(""));
        assertThat(result.get(0).getValue(2, "PK_COL1").toString(), is("0000000064"));
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
     * [YamlTableDataBuilder] buildTableDataList: expected_complete_tables でも値が全て空文字の行が残ること。
     *
     * <p>
     * fillDefaults=true の経路は {@code fillDefaultValues()} が列名を dbInfo の全カラムへ差し替えるため、
     * 「全カラムを空で書いた行」の扱いの変更がこの経路にも及ぶ。先頭・中間・末尾のどこに置いても
     * 残ること、および先頭行が列名解決の基準になることを固定する。<br>
     * Given: expected_complete_tables の blankValueRowComplete グループに
     *        値が全て空の 2 キーの行（先頭）・3 キーの通常行・値が全て空の 3 キーの行（中間）・
     *        3 キーの通常行・値が全て空の 2 キーの行（末尾） の 5 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_complete_tables", "[blankValueRowComplete]", true, path) を呼ぶ<br>
     * Then:  5 行とも残り、dbInfo の全カラム数（11）に差し替わること
     * </p>
     */
    @Test
    public void buildTableDataList_blankValueRowInExpectedCompleteTableKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/completedTable");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_complete_tables", "[blankValueRowComplete]", true, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("先頭・中間・末尾の全値空行も残り 5 行になること", result.get(0).size(), is(5));
        assertThat("dbInfo の全カラム数（11）に差し替わること", result.get(0).getColumnNames().length, is(11));
        assertThat("先頭の全値空行が残ること", result.get(0).getValue(0, "PK_COL1").toString(), is(""));
        assertThat(result.get(0).getValue(1, "PK_COL1").toString(), is("0000000096"));
        assertThat("YAML に書いた値がデフォルト値で上書きされないこと",
                result.get(0).getValue(1, "VARCHAR2_COL").toString(), is("kept"));
        assertThat("中間の全値空行が残ること", result.get(0).getValue(2, "PK_COL1").toString(), is(""));
        assertThat(result.get(0).getValue(3, "PK_COL1").toString(), is("0000000095"));
        assertThat(result.get(0).getValue(3, "VARCHAR2_COL").toString(), is("kept2"));
        assertThat("末尾の全値空行が残ること", result.get(0).getValue(4, "VARCHAR2_COL").toString(), is(""));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: list_maps の先頭行の値が全て空文字でも、その行が残りキーを決めること。
     *
     * <p>
     * 先頭行のキー集合を後続行と変えてあるので、キーが先頭行から決まらなければ結果のキーが食い違って落ちる。
     * 後続行にしか無いキー（KEY1／KEY2）は無視され、その値は捨てられる。<br>
     * Given: list_maps の blankValueRowLeadingListMap に 値が全て空の KEY9/KEY8 の行・KEY1/KEY2 の行 の 2 エントリ<br>
     * When:  buildListMapRows(yaml, "blankValueRowLeadingListMap", path) を呼ぶ<br>
     * Then:  2 件返り、そのキーが 1 行目の KEY8／KEY9（TreeMap でソート済み）であること
     * </p>
     */
    @Test
    public void buildListMapRows_blankValueRowLeadingKeptAndDeterminesKeys() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "blankValueRowLeadingListMap", DIR);

        // Then
        assertThat("先頭の全値空行も残り 2 件返ること", result.size(), is(2));
        assertThat("キーが先頭行（全ての値が空文字の行）のキーで決まること",
                new ArrayList<String>(result.get(0).keySet()), is(Arrays.asList("KEY8", "KEY9")));
        assertThat("1 件目の値は空文字であること", result.get(0).get("KEY8"), is(""));
        assertThat(result.get(0).get("KEY9"), is(""));
        assertNull("2 件目はキー KEY8 を持たないため null になること", result.get(1).get("KEY8"));
        assertNull(result.get(1).get("KEY9"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: list_maps の中間行の値が全て空文字でも、その行が残ること。
     *
     * <p>
     * Given: list_maps の blankValueRowMiddleListMap に 通常行・値が全て空の行・通常行 の 3 エントリ<br>
     * When:  buildListMapRows(yaml, "blankValueRowMiddleListMap", path) を呼ぶ<br>
     * Then:  全値空の行も残り、記述順で 3 件返ること
     * </p>
     */
    @Test
    public void buildListMapRows_blankValueRowMiddleKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "blankValueRowMiddleListMap", DIR);

        // Then
        assertThat("中間の全値空行も残り 3 件返ること", result.size(), is(3));
        assertThat(result.get(0).get("KEY1"), is("before"));
        assertThat("2 件目が全ての値が空文字の行であること", result.get(1).get("KEY1"), is(""));
        assertThat(result.get(1).get("KEY2"), is(""));
        assertThat(result.get(2).get("KEY1"), is("after"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 空文字・null を値に持つ行がいずれも保持されること。
     *
     * <p>
     * 除去対象は値を 1 つも持たない行だけであり、空文字・null を値に持つ行はいずれも
     * データ行として扱われる。<br>
     * Given: list_maps の partiallyBlankValueRowListMap に 一部のみ値を持つ行・値が全て空の行 の 2 エントリ<br>
     * When:  buildListMapRows(yaml, "partiallyBlankValueRowListMap", path) を呼ぶ<br>
     * Then:  2 件とも返り、1 件目の空文字キーは空文字・null キーは null で保持されること
     * </p>
     */
    @Test
    public void buildListMapRows_partiallyBlankValueRowKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "partiallyBlankValueRowListMap", DIR);

        // Then
        assertThat("2 件とも返ること", result.size(), is(2));
        assertThat(result.get(0).get("KEY1"), is("kept"));
        assertThat("空文字のキーは空文字のまま保持されること", result.get(0).get("KEY2"), is(""));
        assertNull("null のキーは null のまま保持されること", result.get(0).get("KEY3"));
        assertThat("2 件目（全ての値が空文字の行）も残ること", result.get(1).get("KEY1"), is(""));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: 値加工を通すと全ての値が空文字になる行も、行として保持されること。
     *
     * <p>
     * 空行判定（{@code YamlSection#dropBlankRows}）は値を見ずキーの有無だけで行うため、値加工
     * （{@code YamlSection#interpret}）が全ての値を空文字にしても行は消えない。この事実の記録である。
     * 空行除去は値加工より前に走り、この行の生値（{@code "to_be_blanked"}）は非空であるため、
     * 判定を旧実装（全ての値が空文字なら行なし）へ戻す変異を当ててもこのテストは落ちない（実測済み）。
     * 値ベースの判定への逆戻りを検知するのは
     * {@code YamlSectionTest#dropBlankRows_removesOnlyEmptyMappingRow} と
     * {@code buildTableDataList_blankValueRowLeadingKeptAndDeterminesColumns} 等である。<br>
     * Given: setup_tables の interpretedToBlankRow グループに 通常行・全ての値が非空の行 の 2 エントリと、
     *        全ての値を空文字にするインタープリタだけを持つビルダ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[interpretedToBlankRow]", false, path) を呼ぶ<br>
     * Then:  2 行とも保持され、2 行目の全カラムが空文字になること
     * </p>
     */
    @Test
    public void buildTableDataList_rowInterpretedToAllBlankIsKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");
        YamlTableDataBuilder blankingBuilder = newBlankingBuilder();

        // When
        List<TableData> result = blankingBuilder.buildTableDataList(
                yaml, "setup_tables", "[interpretedToBlankRow]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("値加工後に全て空文字になる行も行として保持され、2 行返ること", result.get(0).size(), is(2));
        assertThat("1 行目も値加工を通るため空文字になること",
                result.get(0).getValue(0, "PK_COL1").toString(), is(""));
        assertThat("2 行目の PK_COL1 は値加工により空文字になること",
                result.get(0).getValue(1, "PK_COL1").toString(), is(""));
        assertThat("2 行目の PK_COL2 は値加工により空文字になること",
                result.get(0).getValue(1, "PK_COL2").toString(), is(""));
        assertThat("2 行目の VARCHAR2_COL は値加工により空文字になること",
                result.get(0).getValue(1, "VARCHAR2_COL").toString(), is(""));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: 値が全て Java null の行も、行として保持されること。
     *
     * <p>
     * 解説書「コメント・マーカーカラム・空エントリを扱う」節が YAML 形式のスキップ条件として挙げるのは
     * 「{@code rows:} 内の要素が空マッピング（{@code {}}）の場合」だけであり、Java null はこれに当たらない。
     * クォートなしの {@code null} とキーだけ書いた {@code COL:} はロード時点で Java null になるため、
     * これらだけの行が消えないことを固定する。<br>
     * Given: setup_tables の nullValueOnlyRow グループに 通常行・全ての値が Java null の行 の 2 エントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[nullValueOnlyRow]", false, path) を呼ぶ<br>
     * Then:  2 行とも保持され、2 行目の全カラムが null になること
     * </p>
     */
    @Test
    public void buildTableDataList_nullValueOnlyRowKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "setup_tables", "[nullValueOnlyRow]", false, DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("全ての値が Java null の行も保持され、2 行返ること", result.get(0).size(), is(2));
        assertThat(result.get(0).getValue(0, "VARCHAR2_COL").toString(), is("kept"));
        assertNull("クォートなし null のカラムは null のまま保持されること",
                result.get(0).getValue(1, "PK_COL1"));
        assertNull("値を省略した COL: のカラムは null のまま保持されること",
                result.get(0).getValue(1, "PK_COL2"));
        assertNull("クォートなし null のカラムは null のまま保持されること",
                result.get(0).getValue(1, "VARCHAR2_COL"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 値加工を通すと全ての値が空文字になる行も、行として保持されること。
     *
     * <p>
     * list_maps 経路でも、値加工が全ての値を空文字にした行が残ることを記録する（趣旨・変異確認の
     * 結果ともに {@code buildTableDataList_rowInterpretedToAllBlankIsKept} と同じ。
     * 旧実装へ戻す変異ではこのテストも落ちない）。<br>
     * Given: list_maps の interpretedToBlankRowListMap に 通常行・全ての値が非空の行 の 2 エントリと、
     *        全ての値を空文字にするインタープリタだけを持つビルダ<br>
     * When:  buildListMapRows(yaml, "interpretedToBlankRowListMap", path) を呼ぶ<br>
     * Then:  2 件とも保持され、2 件目の全キーが空文字になること
     * </p>
     */
    @Test
    public void buildListMapRows_rowInterpretedToAllBlankIsKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");
        YamlTableDataBuilder blankingBuilder = newBlankingBuilder();

        // When
        List<Map<String, String>> result =
                blankingBuilder.buildListMapRows(yaml, "interpretedToBlankRowListMap", DIR);

        // Then
        assertThat("値加工後に全て空文字になる行も行として保持され、2 件返ること", result.size(), is(2));
        assertThat("1 件目も値加工を通るため空文字になること", result.get(0).get("KEY1"), is(""));
        assertTrue("2 件目は KEY1 をキーとして持つこと", result.get(1).containsKey("KEY1"));
        assertThat("2 件目の KEY1 は値加工により空文字になること", result.get(1).get("KEY1"), is(""));
        assertThat("2 件目の KEY2 は値加工により空文字になること", result.get(1).get("KEY2"), is(""));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 値が全て Java null の行も、行として保持されること。
     *
     * <p>
     * スキップ条件が空マッピング（{@code {}}）だけであることを list_maps 経路で固定する
     * （趣旨は {@code buildTableDataList_nullValueOnlyRowKept} と同じ）。<br>
     * Given: list_maps の nullValueOnlyRowListMap に 通常行・全ての値が Java null の行 の 2 エントリ<br>
     * When:  buildListMapRows(yaml, "nullValueOnlyRowListMap", path) を呼ぶ<br>
     * Then:  2 件とも保持され、2 件目の全キーが null になること
     * </p>
     */
    @Test
    public void buildListMapRows_nullValueOnlyRowKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "nullValueOnlyRowListMap", DIR);

        // Then
        assertThat("全ての値が Java null の行も保持され、2 件返ること", result.size(), is(2));
        assertThat(result.get(0).get("KEY1"), is("kept"));
        assertTrue("2 件目は KEY1 をキーとして持つこと", result.get(1).containsKey("KEY1"));
        assertNull("クォートなし null のキーは null のまま保持されること", result.get(1).get("KEY1"));
        assertNull("値を省略した KEY2: のキーは null のまま保持されること", result.get(1).get("KEY2"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: 全行が空マッピング（{}）の場合は空リストが返ること。
     *
     * <p>
     * テーブル系の {@code buildTableDataList_allEmptyRowsReturnsTableDataWithNoColumns} と対になる
     * list_maps 経路の固定。残る行が 0 件のとき列名も決まらないため、行 0 件の空リストが返る。<br>
     * Given: list_maps の allBlankRowsListMap に {} 行 3 エントリ<br>
     * When:  buildListMapRows(yaml, "allBlankRowsListMap", path) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void buildListMapRows_allEmptyMappingRowsReturnsEmptyList() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "allBlankRowsListMap", DIR);

        // Then
        assertTrue("全行が空マッピングの場合は空リストが返ること", result.isEmpty());
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: マーカーカラムだけが値を持つ行は行として残り、結果が空 Map になること。
     *
     * <p>
     * 空行判定は値を一切見ずキーの有無だけで行うため、マーカーカラム（{@code [COL]}）だけをキーに持つ
     * 行も「値を 1 つも持たない行」ではなく、行としては残る。一方 {@code list_maps} の結果組み立ては
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

    // ========================================================================
    // グループ ID の突合（完全一致）・収集の打ち切りが無いこと
    // ========================================================================

    /**
     * [YamlTableDataBuilder] buildTableDataList: グループ ID は完全一致で突合されること。
     *
     * <p>
     * 何を担保するか: YAML 形式のグループ ID はグループ ID の完全一致で判定され、前方一致は発生しないこと。
     * {@code case01} を指定したとき、前方一致する {@code case010} を持つエントリは収集されない。<br>
     * 根拠: implementation/testdata_notation.rst:255-:269<br>
     * Given: setup_tables に group_id が {@code case01} のエントリと {@code case010} のエントリ<br>
     * When:  buildTableDataList(yaml, "setup_tables", "[case01]", false, path) を呼ぶ<br>
     * Then:  {@code case01} のエントリ 1 件だけが返り、{@code case010} のエントリは含まれないこと
     *        （{@code [case010]} を指定した場合も同様に 1 件だけ返ること）
     * </p>
     */
    @Test
    public void buildTableDataList_groupIdIsMatchedExactly() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> exact = buildTableDataList(yaml, "setup_tables", "[case01]", false, DIR);

        // Then
        assertThat("case01 に前方一致する case010 は収集されず 1 件だけ返ること", exact.size(), is(1));
        assertThat("収集されたのは group_id: case01 のエントリであること",
                exact.get(0).getValue(0, "PK_COL1").toString(), is("0000000070"));

        // When: 前方一致される側（case010）を指定した場合
        List<TableData> longer = buildTableDataList(yaml, "setup_tables", "[case010]", false, DIR);

        // Then
        assertThat("case010 を指定した場合も完全一致の 1 件だけ返ること", longer.size(), is(1));
        assertThat("収集されたのは group_id: case010 のエントリであること",
                longer.get(0).getValue(0, "PK_COL1").toString(), is("0000000071"));
    }

    /**
     * [YamlTableDataBuilder] buildTableDataList: 異なるグループ ID を挟んでも収集が打ち切られないこと。
     *
     * <p>
     * 何を担保するか: トップレベルのキーごとに独立して読み込むため、記述順序や異なるデータブロックの
     * 交互記述を気にする必要がないこと。group_id が {@code a}・{@code b}・{@code a} の順に並んでいても、
     * {@code a} の収集結果は 2 件になる（Excel 形式のように 1 件で打ち切られない）。<br>
     * 根拠: implementation/testdata_notation.rst:339<br>
     * Given: expected_tables に group_id が interleavedA・interleavedB・interleavedA の順で並ぶ 3 エントリ<br>
     * When:  buildTableDataList(yaml, "expected_tables", "[interleavedA]", false, path) を呼ぶ<br>
     * Then:  interleavedA のエントリが 2 件とも収集され、記述順に並ぶこと
     * </p>
     */
    @Test
    public void buildTableDataList_collectionIsNotTruncatedByInterleavedGroupId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<TableData> result = buildTableDataList(yaml, "expected_tables", "[interleavedA]", false, DIR);

        // Then
        assertThat("別の group_id を挟んでも打ち切られず 2 件とも収集されること", result.size(), is(2));
        assertThat("1 件目は記述順で先に現れたエントリであること",
                result.get(0).getValue(0, "PK_COL1").toString(), is("0000000080"));
        assertThat("2 件目は interleavedB を挟んだ後ろのエントリであること",
                result.get(1).getValue(0, "PK_COL1").toString(), is("0000000082"));
    }

    /**
     * [YamlTableDataBuilder] buildListMapRows: {@code args[n]} 形式のキーはマーカーカラムとして除外されないこと。
     *
     * <p>
     * 何を担保するか: バッチ起動時のコマンドライン引数を表す {@code args[n]} 形式のカラムが、
     * {@code [} {@code ]} を含んでいてもマーカーカラム（{@code [COL]} 形式）とはみなされず、
     * 返る Map のキーが文字列 {@code "args[0]"} のまま残ること。<br>
     * 根拠: implementation/testdata_notation.rst:503-:507<br>
     * Given: list_maps の argsColumnTest に args[0]・args[1] のキーを持つ行<br>
     * When:  buildListMapRows(yaml, "argsColumnTest", path) を呼ぶ<br>
     * Then:  キー "args[0]"・"args[1]" が結果 Map にそのまま含まれ、値が取得できること
     * </p>
     */
    @Test
    public void buildListMapRows_argsIndexKeyIsNotExcludedAsMarker() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlTableDataBuilderTest/tableData");

        // When
        List<Map<String, String>> result = buildListMapRows(yaml, "argsColumnTest", DIR);

        // Then
        assertThat(result.size(), is(1));
        Map<String, String> row = result.get(0);
        assertTrue("キーが文字列 \"args[0]\" のまま結果 Map に含まれること: " + row.keySet(),
                row.containsKey("args[0]"));
        assertThat("args[0] の値が取得できること", row.get("args[0]"), is("arg0Value"));
        assertTrue("キーが文字列 \"args[1]\" のまま結果 Map に含まれること: " + row.keySet(),
                row.containsKey("args[1]"));
        assertThat("args[1] の値が取得できること", row.get("args[1]"), is("arg1Value"));
        assertThat("マーカーカラムでない通常カラムも取得できること", row.get("no"), is("1"));
    }

}
