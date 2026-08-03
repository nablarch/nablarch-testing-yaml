package nablarch.test.core.reader.yaml;

import nablarch.core.dataformat.LayoutDefinition;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.DataFileFragment;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link YamlFileBuilder} のファイル系メソッド（{@code buildDataFileList}）のテストクラス。
 *
 * <p>
 * {@link YamlLoader#load} が返す YAML Map を {@link YamlFileBuilder} が走査し、値加工
 * （{@code ${...}} の解釈・グループ絞り込み・必須チェック）して {@link DataFile} を組み立てる
 * 一連のロジックを検証する。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlFileBuilderTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    private YamlFileBuilder builder;

    @Before
    public void before() {
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("interpreters");
        builder = new YamlFileBuilder(InterpreterResolver.withBinaryFile(interpreters));
    }

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    // ビルダ（YAML Map → 本体器）を通すヘルパー。
    private List<DataFile> buildFileList(Map<String, Object> yaml, String sectionKey,
                                        String groupId, String basePath) {
        return builder.buildDataFileList(yaml, sectionKey, groupId, basePath);
    }

    // ========================================================================
    // buildFileList: 固定長・可変長ファイルが取得できること
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: グループ ID なしで固定長・可変長ファイルが取得できること。
     *
     * <p>
     * Given: setup_files に fixed と variable の 2 エントリ<br>
     * When:  buildFileList(yaml, "setup_files", "", path) を呼ぶ<br>
     * Then:  FixedLengthFile と VariableLengthFile の 2 件が返ること
     * </p>
     */
    @Test
    public void buildFileList_fixedAndVariable() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "", DIR);

        // Then
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    /**
     * [YamlFileBuilder] buildFileList: 取得した DataFile の path が正しく設定されていること。
     *
     * <p>
     * Given: setup_files に path=dummy/setup_fixed.dat のエントリ<br>
     * When:  buildFileList を呼ぶ<br>
     * Then:  getPath() が正しいパスを返すこと
     * </p>
     */
    @Test
    public void buildFileList_pathIsSet() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "", DIR);

        // Then
        assertThat(result.get(0).getPath(), is("dummy/setup_fixed.dat"));
        assertThat(result.get(1).getPath(), is("dummy/setup_variable.csv"));
    }

    /**
     * [YamlFileBuilder] buildFileList: グループ ID 指定で対象グループのみ取得されること。
     *
     * <p>
     * Given: setup_files に grp1 グループのエントリ<br>
     * When:  buildFileList(yaml, "setup_files", "[grp1]", path) を呼ぶ<br>
     * Then:  grp1 の 1 件のみ返ること
     * </p>
     */
    @Test
    public void buildFileList_withGroupId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "[grp1]", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
    }

    /**
     * [YamlFileBuilder] buildFileList: expected_files の末尾セクションデータが欠落しないこと（RS-07）。
     *
     * <p>
     * Given: setup_files の後に expected_files が YAML 末尾に記述されている<br>
     * When:  buildFileList(yaml, "expected_files", "", path) を呼ぶ<br>
     * Then:  末尾セクションのデータが欠落せず 2 件返ること（RS-07）
     * </p>
     */
    @Test
    public void buildFileList_lastSectionNotLost() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "expected_files", "", DIR);

        // Then: 末尾セクションが欠落していないこと
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat(result.get(1), instanceOf(VariableLengthFile.class));
    }

    /**
     * [YamlFileBuilder] buildFileList: セクションが存在しない場合は空リストが返ること。
     *
     * <p>
     * Given: setup_files キーが存在しない YAML<br>
     * When:  buildFileList を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void buildFileList_sectionNotExists() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/emptyYaml");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "", DIR);

        // Then
        assertThat(result.size(), is(0));
    }

    // ========================================================================
    // ディレクティブが正しく設定されること
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: 複数のグループ（グループIDなし・grp1）が存在する場合、
     * グループIDなしの件数が正しく取得されること。
     *
     * <p>
     * Given: setup_files にグループIDなし 2 件 + grp1 の 1 件<br>
     * When:  buildFileList(yaml, "setup_files", "", path) を呼ぶ<br>
     * Then:  グループIDなしの 2 件のみ返ること
     * </p>
     */
    @Test
    public void buildFileList_onlyNoGroupIdEntries() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "", DIR);

        // Then: グループIDなしの 2 件のみ
        assertThat(result.size(), is(2));
    }

    // ========================================================================
    // ディレクティブが正しく設定されること（QA-2）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: directives が DataFile に正しく設定されること（QA-2）。
     *
     * <p>
     * Given: setup_files の fixed エントリに text-encoding: Windows-31J が指定されている<br>
     * When:  buildFileList を呼ぶ<br>
     * Then:  getDirective("text-encoding") が "Windows-31J" を返すこと
     * </p>
     */
    @Test
    public void buildFileList_directivesAreSet() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "", DIR);

        // Then
        assertThat(result.get(0).createLayout().getDirective().get("text-encoding"), is("Windows-31J"));
    }

    // ========================================================================
    // record_type が YAML に存在しない場合 "default" にフォールバックすること（QA観点2-軽微）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: records に record_type キーが存在しない場合 "default" にフォールバックすること（QA観点2-軽微）。
     *
     * <p>
     * Given: setup_files の noRecordType グループのエントリで records に record_type キーなし<br>
     * When:  buildFileList(yaml, "setup_files", "[noRecordType]", path) を呼ぶ<br>
     * Then:  FixedLengthFile のフラグメントの record_type が "default" であること
     * </p>
     */
    @Test
    public void buildFileList_recordTypeNullFallbackToDefault() {
        // Given: record_type キーのないレコードエントリを直接構築（スキーマ検証の対象外で Builder の防衛コードをテスト）
        Map<String, Object> fieldDef = new LinkedHashMap<>();
        fieldDef.put("name", "FIELD1");
        fieldDef.put("type", "半角");
        fieldDef.put("length", 5);
        Map<String, Object> record = new LinkedHashMap<>();
        // record_type キーを意図的に省略
        record.put("fields", Arrays.<Object>asList(fieldDef));
        record.put("rows", Collections.emptyList());
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("group_id", "noRecordType");
        entry.put("path", "dummy/no_record_type.dat");
        entry.put("type", "fixed");
        entry.put("records", Arrays.<Object>asList(record));
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("setup_files", Arrays.<Object>asList(entry));

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "[noRecordType]", DIR);

        // Then: record_type がない場合 "default" にフォールバックすること
        assertThat(result.size(), is(1));
        LayoutDefinition layout = result.get(0).createLayout();
        assertThat("record_type なしの場合は 'default' にフォールバックすること",
                layout.getRecords().get(0).getTypeName(), is("default"));
    }

    // ========================================================================
    // path キーが存在しないエントリで IllegalStateException がスローされること（E-2）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: path キーが存在しないエントリで IllegalStateException がスローされること（E-2）。
     *
     * <p>
     * Given: setup_files に path キーがない missingPath グループのエントリ<br>
     * When:  buildFileList(yaml, "setup_files", "[missingPath]", basePath) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージにセクション名とグループIDが含まれること
     * </p>
     */
    @Test
    public void buildFileList_missingPathThrowsException() {
        // Given: path キーのないエントリを直接構築（スキーマ検証の対象外で Builder の検証をテスト）
        Map<String, Object> fieldDef = new LinkedHashMap<>();
        fieldDef.put("name", "FIELD1");
        fieldDef.put("type", "半角");
        fieldDef.put("length", 5);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("record_type", "DATA");
        record.put("fields", Arrays.<Object>asList(fieldDef));
        record.put("rows", Collections.emptyList());
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("group_id", "missingPath");
        // path キーを意図的に省略
        entry.put("type", "fixed");
        entry.put("records", Arrays.<Object>asList(record));
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("setup_files", Arrays.<Object>asList(entry));

        // When
        try {
            buildFileList(yaml, "setup_files", "[missingPath]", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("フィールド名がメッセージに含まれること", e.getMessage(), containsString("path"));
            assertThat("セクション名がメッセージに含まれること", e.getMessage(), containsString("setup_files"));
            assertThat("グループIDがメッセージに含まれること", e.getMessage(), containsString("[missingPath]"));
        }
    }

    // ========================================================================
    // 可変長ファイルで length なしのフィールドが正しく扱われること（QA観点2-軽微）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: records に複数のレコードレイアウトを記述した場合、全レコードが構築されること。
     *
     * <p>
     * 1ファイルセクション内に複数のレコードレイアウトを連続して記述できます<br>
     * Given: setup_files の multiRecord グループに HEADER + DATA の 2 レコードを持つエントリ<br>
     * When:  buildFileList(yaml, "setup_files", "[multiRecord]", path) を呼ぶ<br>
     * Then:  DataFile の toDataRecords() が HEADER 行 + DATA 行の 2 件を返すこと
     * </p>
     */
    @Test
    public void buildFileList_multipleRecordLayouts() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "[multiRecord]", DIR);

        // Then: DataFile にフラグメント数を返す公開 API がないため、private フィールド "all" をリフレクションで確認する。
        assertThat(result.size(), is(1));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        Field allField = DataFile.class.getDeclaredField("all");
        allField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<DataFileFragment> fragments = (List<DataFileFragment>) allField.get(result.get(0));
        assertThat("HEADER + DATA の 2 フラグメントが生成されること", fragments.size(), is(2));

        Field recordTypeField = DataFileFragment.class.getDeclaredField("recordType");
        recordTypeField.setAccessible(true);
        assertThat("1つ目のレコード種別が HEADER であること",
                recordTypeField.get(fragments.get(0)).toString(), is("HEADER"));
        assertThat("2つ目のレコード種別が DATA であること",
                recordTypeField.get(fragments.get(1)).toString(), is("DATA"));
    }

    /**
     * [YamlFileBuilder] buildFileList: records が空配列のエントリは空ファイルとして扱われること。
     *
     * <p>
     * 0バイトの空ファイルを表現するには、ディレクティブのみを記述してレコード定義を省略します（records: []）<br>
     * Given: setup_files の emptyFile グループに records: [] のエントリ<br>
     * When:  buildFileList(yaml, "setup_files", "[emptyFile]", path) を呼ぶ<br>
     * Then:  FixedLengthFile が 1 件返り、レコード定義が 0 件でディレクティブが設定されていること
     * </p>
     */
    @Test
    public void buildFileList_emptyRecords() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "[emptyFile]", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        assertThat("path が正しく設定されていること", result.get(0).getPath(), is("input/empty.dat"));
        LayoutDefinition layout = result.get(0).createLayout();
        assertThat("レコード定義が 0 件であること", layout.getRecords().size(), is(0));
        assertThat("ディレクティブが設定されていること", layout.getDirective().get("text-encoding"), is("MS932"));
    }

    /**
     * [YamlFileBuilder] buildFileList: 可変長ファイルで length が指定されていない場合、setLengths が呼ばれないこと（QA観点2-軽微）。
     *
     * <p>
     * Given: setup_files の variable エントリで fields に length なし<br>
     * When:  buildFileList(yaml, "setup_files", "", path) を呼ぶ<br>
     * Then:  VariableLengthFile が返り、レコード定義に lengths が含まれないこと（record-length ディレクティブなし）
     * </p>
     */
    @Test
    public void buildFileList_variableFileWithNoLength() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "", DIR);
        VariableLengthFile variableFile = (VariableLengthFile) result.get(1);

        // Then: length なしフィールドの場合 record-length ディレクティブが null であること（setLengths は呼ばれない）
        LayoutDefinition layout = variableFile.createLayout();
        assertThat("可変長ファイルでは record-length ディレクティブが設定されないこと",
                layout.getDirective().get("record-length"), nullValue());
    }

    /**
     * [YamlFileBuilder] buildFileList: field-separator に 2 文字以上を指定すると IllegalArgumentException がスローされること（9.3 QA-6）。
     *
     * <p>
     * field-separator は 1 文字のみ有効。2 文字以上の場合は IllegalArgumentException がスローされる<br>
     * Given: expected_files の twoCharSeparator グループに field-separator: ",,"（2文字）<br>
     * When:  buildFileList 後に createLayout() を呼ぶ<br>
     * Then:  IllegalArgumentException がスローされること
     * </p>
     */
    @Test
    public void buildFileList_twoCharFieldSeparatorThrowsException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        try {
            buildFileList(yaml, "expected_files", "[twoCharSeparator]", DIR);
            fail("IllegalArgumentException が期待される");
        } catch (IllegalArgumentException e) {
            // Then
            // OK: IllegalArgumentException がスローされること
        }
    }

    // ========================================================================
    // 重複フィールド名で IllegalArgumentException がスローされること
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: 固定長ファイルのレコード内に重複フィールド名がある場合、
     * IllegalArgumentException がスローされること。
     *
     * <p>
     * Given: expected_files の duplicateFixedName グループに "dup" が 2 つある固定長エントリ<br>
     * When:  buildFileList を呼ぶ<br>
     * Then:  IllegalArgumentException がスローされ、メッセージに "Duplicate field names are not permitted in a record" が含まれること
     * </p>
     */
    @Test
    public void buildFileList_fixedDuplicateFieldNameThrowsException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        try {
            buildFileList(yaml, "expected_files", "[duplicateFixedName]", DIR);
            fail("IllegalArgumentException が期待される");
        } catch (IllegalArgumentException e) {
            // Then
            assertThat(e.getMessage(), containsString("Duplicate field names are not permitted in a record"));
        }
    }

    /**
     * [YamlFileBuilder] buildFileList: 可変長ファイルのレコード内に重複フィールド名がある場合、
     * IllegalArgumentException がスローされること。
     *
     * <p>
     * Given: expected_files の duplicateVariableName グループに "dup" が 2 つある可変長エントリ<br>
     * When:  buildFileList を呼ぶ<br>
     * Then:  IllegalArgumentException がスローされ、メッセージに "Duplicate field names are not permitted in a record" が含まれること
     * </p>
     */
    @Test
    public void buildFileList_variableDuplicateFieldNameThrowsException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        try {
            buildFileList(yaml, "expected_files", "[duplicateVariableName]", DIR);
            fail("IllegalArgumentException が期待される");
        } catch (IllegalArgumentException e) {
            // Then
            assertThat(e.getMessage(), containsString("Duplicate field names are not permitted in a record"));
        }
    }

    /**
     * [YamlFileBuilder] buildFileList: 可変長ファイルの field-separator に "\\t" を指定するとタブ文字になること（9.3 G-3）。
     *
     * <p>
     * field-separator の "\\t" 指定はタブ文字（0x09）として設定される<br>
     * Given: setup_files の variable エントリで directives.field-separator = "\\t"<br>
     * When:  buildFileList(yaml, "expected_files", "[tabSeparator]", path) を呼ぶ<br>
     * Then:  createLayout().getDirective().get("field-separator") がタブ文字（"\t"）であること
     * </p>
     */
    @Test
    public void buildFileList_tabFieldSeparatorBecomesTabChar() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "expected_files", "[tabSeparator]", DIR);

        // Then
        assertThat("1件取得できること", result.size(), is(1));
        assertThat(result.get(0), instanceOf(VariableLengthFile.class));
        LayoutDefinition layout = result.get(0).createLayout();
        assertThat("field-separator \"\\\\t\" はタブ文字になること",
                layout.getDirective().get("field-separator"), is("\t"));
    }
}
