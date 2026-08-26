package nablarch.test.core.reader.yaml;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.FieldDefinition;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.dataformat.RecordDefinition;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.file.DataFileFragment;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.MockMessages;
import nablarch.test.core.file.VariableLengthFile;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
 * {@link YamlFileBuilder} のテストクラス。
 *
 * <p>
 * ファイル系メソッド（{@code buildDataFileList}）については、{@link YamlLoader#load} が返す YAML Map を
 * {@link YamlFileBuilder} が走査し、値加工（{@code ${...}} の解釈・グループ絞り込み・必須チェック）して
 * {@link DataFile} を組み立てる一連のロジックを検証する。
 * </p>
 * <p>
 * あわせて、メッセージ系のレコードレイアウト組み立てメソッド
 * （{@link YamlFileBuilder#buildFragmentsForMessage}／{@link YamlFileBuilder#buildFragmentsForSendSync}）
 * も本クラスで検証する。これらは package-private であり、同一パッケージに属する本クラスからのみ
 * 直接呼び出せる。ここでは分岐単位の挙動（{@code record_type} の保持／"default" 化・長さ未指定フィールドの動的計算・
 * 値行への連番付与）を細かく固定する。利用者が実際に通る公開 API 経路
 * （{@code YamlTestDataParser#getMessage} 等）での担保は {@code YamlTestDataParserTest} が、
 * {@link YamlMessageBuilder} 経由の結合検証は {@code YamlMessageBuilderTest} が担う。
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
     * [YamlFileBuilder] buildFileList: expected_files の末尾セクションデータが欠落しないこと。
     *
     * <p>
     * Given: setup_files の後に expected_files が YAML 末尾に記述されている<br>
     * When:  buildFileList(yaml, "expected_files", "", path) を呼ぶ<br>
     * Then:  末尾セクションのデータが欠落せず 2 件返ること
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
    // ディレクティブが正しく設定されること
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: directives が DataFile に正しく設定されること。
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
    public void buildFileList_multipleRecordLayouts() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "[multiRecord]", DIR);

        // Then: レコード定義はフラグメント 1 件につき 1 件生成されるため、公開 API の createLayout() で確認する。
        assertThat(result.size(), is(1));
        assertThat(result.get(0), instanceOf(FixedLengthFile.class));
        List<RecordDefinition> records = result.get(0).createLayout().getRecords();
        assertThat("HEADER + DATA の 2 フラグメントが生成されること", records.size(), is(2));
        assertThat("1つ目のレコード種別が HEADER であること", records.get(0).getTypeName(), is("HEADER"));
        assertThat("2つ目のレコード種別が DATA であること", records.get(1).getTypeName(), is("DATA"));

        // Then: 各レコードの値行が記述どおりに変換されること
        List<DataRecord> dataRecords = result.get(0).toDataRecords();
        assertThat("HEADER 行 + DATA 行の 2 件が返ること", dataRecords.size(), is(2));
        assertThat(dataRecords.get(0).getRecordType(), is("HEADER"));
        assertThat(dataRecords.get(0).getString("SEQ"), is("H001"));
        assertThat(dataRecords.get(1).getRecordType(), is("DATA"));
        assertThat(dataRecords.get(1).getString("USER_ID"), is("001"));
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
     * [YamlFileBuilder] buildFileList: 全フィールドの値が "" のレコードも 1 件のレコードとして保持されること。
     *
     * <p>
     * テーブル系セクション・{@code list_maps} では「全ての値が空文字の行」を行として存在しない
     * ものとして扱う（{@link YamlSection#dropBlankRows}）が、ファイルデータにはその規則を適用しない。
     * 全フィールドが空のレコードはそれ自体が意味を持つデータであり、落としてはならない。
     * 値行に「全要素が空ならスキップ」を入れるとこのテストが落ちる。
     * ここで固定するのは空文字を要素数ぶん明示的に並べた {@code ["", "", ""]} の側であり、
     * 要素数 0 の配列 {@code []} の側は
     * {@link #buildFileList_emptyRowBecomesOneRecordOfEmptyStrings()} が担保する。<br>
     * Given: expected_files の allBlankFieldsRecord グループに rows が {@code ["", "", ""]} 1 件のエントリ<br>
     * When:  buildFileList(yaml, "expected_files", "[allBlankFieldsRecord]", path) を呼ぶ<br>
     * Then:  レコードが 1 件保持され、3 フィールドとも空文字（{@code null} ではない）で保持されること
     * </p>
     */
    @Test
    public void buildFileList_allBlankFieldRecordIsKept() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "expected_files", "[allBlankFieldsRecord]", DIR);

        // Then
        assertThat(result.size(), is(1));
        assertThat("全フィールドが \"\" のレコードも 1 件として保持されること",
                result.get(0).toDataRecords().size(), is(1));
        DataRecord record = result.get(0).toDataRecords().get(0);
        assertThat("レコードが 3 フィールドとも保持していること", record.size(), is(3));
        assertThat("FIELD1 が空文字のまま保持されること", record.get("FIELD1"), is((Object) ""));
        assertThat("FIELD2 が空文字のまま保持されること", record.get("FIELD2"), is((Object) ""));
        assertThat("FIELD3 が空文字のまま保持されること", record.get("FIELD3"), is((Object) ""));
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
     * [YamlFileBuilder] buildFileList: field-separator に 2 文字以上を指定すると IllegalArgumentException がスローされること。
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
     * [YamlFileBuilder] buildFileList: 可変長ファイルの field-separator に "\\t" を指定するとタブ文字になること。
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

    // ========================================================================
    // 区切り文字ディレクティブの記法（JSON スキーマの description が述べる挙動）
    //
    // ntf-testdata-yaml-schema.json の
    //   $defs.directives.properties.record-separator.description
    //   $defs.directives.properties.field-separator.description
    // が述べている挙動を、YAML ファイルを経由した実際の経路で実測して固定する。
    // 担保するのは以下の 2 点である。
    //   * description が示す記法（シンボル指定・バックスラッシュと t の 2 文字表記）が
    //     実行で通り、期待どおりの値になること
    //   * description が示していない記法（YAML のダブルクォート文字列内エスケープ
    //     シーケンス "\r\n" / "\t" による実制御文字の指定）が実行で通らないこと
    //
    // これらのテストは description の文字列自体を参照しない。したがって description の
    // 文言が変わってもテストは落ちない。文言と挙動の整合を保つのは文言を書く側の責務で、
    // ここで固定するのは文言が拠り所とする実挙動のみである。
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: record-separator のシンボル 4 種が、それぞれ対応する改行コードに変換されること。
     *
     * <p>
     * description が述べる挙動:
     * {@code $defs.directives.properties.record-separator.description} の
     * 「改行コードは {@code NONE} / {@code CR} / {@code LF} / {@code CRLF} のシンボルで指定する」。<br>
     * Given: expected_files の symbolRecordSeparator{None,Cr,Lf,Crlf} グループに各シンボルを指定<br>
     * When:  buildFileList を呼び createLayout() する<br>
     * Then:  record-separator ディレクティブが順に 空文字 / CR / LF / CR+LF であること
     * </p>
     */
    @Test
    public void buildFileList_recordSeparatorSymbolsAreConvertedToLineSeparators() {
        // Given
        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("symbolRecordSeparatorNone", "");
        expected.put("symbolRecordSeparatorCr", "\r");
        expected.put("symbolRecordSeparatorLf", "\n");
        expected.put("symbolRecordSeparatorCrlf", "\r\n");
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String groupId = entry.getKey();

            // When
            List<DataFile> result = buildFileList(yaml, "expected_files", "[" + groupId + "]", DIR);

            // Then
            assertThat(groupId + " が1件取得できること", result.size(), is(1));
            LayoutDefinition layout = result.get(0).createLayout();
            assertThat(groupId + " のシンボルが対応する改行コードに変換されること",
                    layout.getDirective().get("record-separator"), is(entry.getValue()));
        }
    }

    /**
     * [YamlFileBuilder] buildFileList: record-separator に制御文字でない任意のリテラル文字列を指定すると
     * その文字列自身がレコード区切りになること。
     *
     * <p>
     * description が述べる挙動:
     * {@code $defs.directives.properties.record-separator.description} の
     * 「シンボル以外の文字列を書いた場合は、その文字列自身が区切り文字になる」。<br>
     * Given: expected_files の literalRecordSeparator グループに record-separator: ":"<br>
     * When:  buildFileList を呼び createLayout() する<br>
     * Then:  record-separator ディレクティブが ":" であること
     * </p>
     */
    @Test
    public void buildFileList_recordSeparatorLiteralStringIsUsedAsIs() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "expected_files", "[literalRecordSeparator]", DIR);

        // Then
        assertThat("1件取得できること", result.size(), is(1));
        LayoutDefinition layout = result.get(0).createLayout();
        assertThat("シンボルに合致しないリテラル文字列はそれ自身がレコード区切りになること",
                layout.getDirective().get("record-separator"), is(":"));
    }

    /**
     * [YamlFileBuilder] buildFileList: record-separator に実制御文字 CR+LF を指定すると
     * レコード区切りが空文字になること。
     *
     * <p>
     * description が述べる挙動:
     * {@code $defs.directives.properties.record-separator.description} の
     * 「YAML のダブルクォート文字列に {@code "\r\n"} と書くと実際の制御文字に展開され、
     * NTF が値を trim する際に除去されて区切りが空文字になる（エラーにならない）」。<br>
     * YAML のダブルクォート文字列は {@code "\r\n"} を実際の CR+LF へ展開するが、
     * {@code DataFile#setDirective} が値を {@code trim()} してから変換するため、
     * 制御文字だけの値は空文字になる。例外は送出されず、レコード区切りが無言で壊れる。<br>
     * Given: expected_files の controlCharRecordSeparator グループに record-separator: "\r\n"（実 CR+LF）<br>
     * When:  buildFileList を呼び createLayout() する<br>
     * Then:  record-separator ディレクティブが空文字であること（CR+LF にはならない）
     * </p>
     */
    @Test
    public void buildFileList_recordSeparatorControlCharBecomesEmpty() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "expected_files", "[controlCharRecordSeparator]", DIR);

        // Then
        assertThat("1件取得できること", result.size(), is(1));
        LayoutDefinition layout = result.get(0).createLayout();
        assertThat("実制御文字 CR+LF は trim() で除去され空文字になること（CR+LF にはならない）",
                layout.getDirective().get("record-separator"), is(""));
    }

    /**
     * [YamlFileBuilder] buildFileList: field-separator に実制御文字のタブを指定すると
     * {@link IllegalArgumentException} がスローされること。
     *
     * <p>
     * description が述べる挙動:
     * {@code $defs.directives.properties.field-separator.description} の
     * 「YAML の {@code "\t"} は実際のタブ文字に展開され、NTF が値を trim する際に除去されて
     * 0 文字になりエラーとなる」。<br>
     * YAML のダブルクォート文字列は {@code "\t"} を実際のタブ文字へ展開するが、
     * {@code DataFile#setDirective} の {@code trim()} で 0 文字になるため、
     * 「2 文字表記の {@code \t} を除き、1 文字でない値はエラー」の検査に引っかかって例外になる。<br>
     * タブを指定できる記法はバックスラッシュと t の 2 文字表記だけであり、
     * それが通ることは {@link #buildFileList_tabFieldSeparatorBecomesTabChar()} が担保する。<br>
     * Given: expected_files の controlCharFieldSeparator グループに field-separator: "\t"（実タブ文字）<br>
     * When:  buildFileList を呼ぶ<br>
     * Then:  IllegalArgumentException がスローされ、メッセージに
     *        "field-separator must be one character" が含まれること
     * </p>
     */
    @Test
    public void buildFileList_controlCharFieldSeparatorThrowsException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        try {
            buildFileList(yaml, "expected_files", "[controlCharFieldSeparator]", DIR);
            fail("IllegalArgumentException が期待される");
        } catch (IllegalArgumentException e) {
            // Then
            assertThat(e.getMessage(), containsString("field-separator must be one character"));
        }
    }

    // ========================================================================
    // record_type の値 "FW_HEADER" が特別扱いされないこと（メッセージ系経路）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFragmentsForMessage: record_type の値が "FW_HEADER" のレコードも
     * 読み飛ばされずフラグメントとして構築されること。
     *
     * <p>
     * {@code record_type} に特別な予約値はなく、フレームワーク制御ヘッダは {@code fw_header:} マップで記述する。
     * よって "FW_HEADER" という値も他の値と同じく扱われ、読み飛ばされない。
     * {@code keepRecordType=false}（{@code messages} 経路）では記載値は使われず "default" になる<br>
     * Given: records に record_type が "FW_HEADER" と "BODY" の 2 レコード<br>
     * When:  buildFragmentsForMessage を keepRecordType=false で呼ぶ<br>
     * Then:  2 フラグメントが構築され、いずれも record_type が "default" になり、
     *        両レコードとも電文本文としてレンダリングされること
     * </p>
     */
    @Test
    public void buildFragmentsForMessage_fwHeaderRecordTypeIsNotSkipped() {
        // Given
        List<Object> records = Arrays.<Object>asList(
                messageRecord("FW_HEADER", "requestId", 10, "0000000001"),
                messageRecord("BODY", "SEARCH_KEY", 10, "SEARCHKEY1"));
        FixedLengthFile file = new FixedLengthFile("dummy/message.dat");
        file.setDirective("text-encoding", "MS932");

        // When
        // keepRecordType=false: messages（MESSAGE）経路は記載値を使わず "default" になる
        YamlFileBuilder.buildFragmentsForMessage(file, records, false,
                Collections.<TestDataInterpreter>emptyList());

        // Then: レコード定義はフラグメント 1 件につき 1 件生成される
        List<RecordDefinition> layoutRecords = file.createLayout().getRecords();
        assertThat("FW_HEADER レコードも読み飛ばされず 2 フラグメント構築されること", layoutRecords.size(), is(2));
        assertThat("messages では記載値が使われず 'default' になること",
                layoutRecords.get(0).getTypeName(), is("default"));
        assertThat("messages では記載値が使われず 'default' になること",
                layoutRecords.get(1).getTypeName(), is("default"));

        // Then: 値行が電文本文としてレンダリングされること
        List<DataRecord> dataRecords = file.toDataRecords();
        assertThat("FW_HEADER レコードの値行も電文本文になること", dataRecords.size(), is(2));
        assertThat("FW_HEADER レコードの値行が保持されること",
                dataRecords.get(0).getString("requestId"), is("0000000001"));
        assertThat("BODY レコードの値行が保持されること",
                dataRecords.get(1).getString("SEARCH_KEY"), is("SEARCHKEY1"));
    }

    /**
     * [YamlFileBuilder] buildFragmentsForMessage: record_type の値が "FW_HEADER" で length 未指定のレコードも
     * 読み飛ばされず、フィールド長が値から動的計算されること。
     *
     * <p>
     * メッセージ系は常に固定長のため、{@code length} 未指定フィールドは {@code "-"}（動的計算）として扱われる。
     * この経路を "FW_HEADER" レコードでも通ることを担保する<br>
     * Given: records に record_type が "FW_HEADER" で length 未指定の 2 フィールドを持つレコード<br>
     * When:  buildFragmentsForMessage を呼ぶ<br>
     * Then:  1 フラグメントが構築され、各フィールドのバイト位置が値のバイト長から算出されること
     * </p>
     */
    @Test
    public void buildFragmentsForMessage_fwHeaderRecordWithoutLength() {
        // Given: length を指定しない 2 フィールド（値はいずれも 10 バイト）
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("record_type", "FW_HEADER");
        record.put("fields", Arrays.<Object>asList(
                messageFieldWithoutLength("requestId"),
                messageFieldWithoutLength("userId")));
        record.put("rows", Arrays.<Object>asList(Arrays.asList("0000000001", "testUser01")));
        FixedLengthFile file = new FixedLengthFile("dummy/message.dat");
        file.setDirective("text-encoding", "MS932");

        // When
        YamlFileBuilder.buildFragmentsForMessage(file, Arrays.<Object>asList(record), false,
                Collections.<TestDataInterpreter>emptyList());

        // Then
        List<RecordDefinition> layoutRecords = file.createLayout().getRecords();
        assertThat("length 未指定の FW_HEADER レコードも 1 フラグメント構築されること", layoutRecords.size(), is(1));
        assertThat("messages では記載値が使われず 'default' になること",
                layoutRecords.get(0).getTypeName(), is("default"));

        List<FieldDefinition> fields = layoutRecords.get(0).getFields();
        assertThat("1つ目のフィールドは先頭バイトから始まること", fields.get(0).getPosition(), is(1));
        assertThat("length 未指定フィールドの長さが値のバイト長（10）から動的計算されること",
                fields.get(1).getPosition(), is(11));

        List<DataRecord> dataRecords = file.toDataRecords();
        assertThat("値行が電文本文になること", dataRecords.size(), is(1));
        assertThat(dataRecords.get(0).getString("requestId"), is("0000000001"));
        assertThat(dataRecords.get(0).getString("userId"), is("testUser01"));
    }

    /**
     * [YamlFileBuilder] buildFragmentsForSendSync: record_type の値が "FW_HEADER" のレコードも
     * 読み飛ばされず、記載どおりのレコード種別 "FW_HEADER" のフラグメントとして構築され、
     * 値行に連番が付与されること。
     *
     * <p>
     * "FW_HEADER" は予約値ではない。{@code keepRecordType=true}（同期応答メッセージ送信の 4 セクション）では
     * 記載した値がそのままレコード種別になるため、単に "FW_HEADER" というレコード種別になる<br>
     * Given: records に record_type が "FW_HEADER" と "BODY" の 2 レコード<br>
     * When:  buildFragmentsForSendSync を keepRecordType=true で呼ぶ<br>
     * Then:  2 フラグメントが構築され、レコード種別が記載どおり "FW_HEADER"／"BODY" になり、
     *        FW_HEADER レコードの値行にも連番（FIRST_FIELD_NO="1"）が付与されること
     * </p>
     */
    @Test
    public void buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped() {
        // Given
        List<Object> records = Arrays.<Object>asList(
                messageRecord("FW_HEADER", "requestId", 10, "0000000001"),
                messageRecord("BODY", "SEARCH_KEY", 10, "SEARCHKEY1"));
        MockMessages file = new MockMessages("dummy/sendSync.dat");
        file.setDirective("text-encoding", "MS932");

        // When
        // keepRecordType=true: 送信同期4セクションは record_type の記載値がそのままレコード種別になる
        YamlFileBuilder.buildFragmentsForSendSync(file, records, true,
                Collections.<TestDataInterpreter>emptyList());

        // Then
        List<RecordDefinition> layoutRecords = file.createLayout().getRecords();
        assertThat("FW_HEADER レコードも読み飛ばされず 2 フラグメント構築されること", layoutRecords.size(), is(2));
        assertThat("1つ目のレコード種別が記載どおり 'FW_HEADER' になること",
                layoutRecords.get(0).getTypeName(), is("FW_HEADER"));
        assertThat("2つ目のレコード種別が記載どおり 'BODY' になること",
                layoutRecords.get(1).getTypeName(), is("BODY"));

        List<DataRecord> dataRecords = file.toDataRecords();
        assertThat("FW_HEADER レコードの値行も電文本文になること", dataRecords.size(), is(2));
        assertThat("FW_HEADER レコードの値行が保持されること",
                dataRecords.get(0).getString("requestId"), is("0000000001"));
        assertThat("FW_HEADER レコードの値行にも連番（レコード内 1 始まり）が付与されること",
                dataRecords.get(0).getString(DataFileFragment.FIRST_FIELD_NO), is("1"));
        assertThat("BODY レコードの値行にも連番（レコード内 1 始まり）が付与されること",
                dataRecords.get(1).getString(DataFileFragment.FIRST_FIELD_NO), is("1"));
    }

    /**
     * [YamlFileBuilder] buildFragmentsForSendSync: {@code keepRecordType=false} の場合は
     * record_type の記載値が使われず "default" になること。
     *
     * <p>
     * 送信同期の組み立て経路であっても、セクションキーが {@code messages} の場合
     * （{@code getSendSyncMessage} を DataType.MESSAGE で呼ぶ経路）は記載値を使わない。
     * レコード種別の扱いが引数で切り替わることを固定する<br>
     * Given: records に record_type が "FW_HEADER" と "BODY" の 2 レコード<br>
     * When:  buildFragmentsForSendSync を keepRecordType=false で呼ぶ<br>
     * Then:  いずれのレコード種別も "default" になること
     * </p>
     */
    @Test
    public void buildFragmentsForSendSync_recordTypeIsDefaultWhenNotKept() {
        // Given
        List<Object> records = Arrays.<Object>asList(
                messageRecord("FW_HEADER", "requestId", 10, "0000000001"),
                messageRecord("BODY", "SEARCH_KEY", 10, "SEARCHKEY1"));
        MockMessages file = new MockMessages("dummy/sendSync.dat");
        file.setDirective("text-encoding", "MS932");

        // When
        YamlFileBuilder.buildFragmentsForSendSync(file, records, false,
                Collections.<TestDataInterpreter>emptyList());

        // Then
        List<RecordDefinition> layoutRecords = file.createLayout().getRecords();
        assertThat("2 フラグメント構築されること", layoutRecords.size(), is(2));
        assertThat("keepRecordType=false では記載値が使われず 'default' になること",
                layoutRecords.get(0).getTypeName(), is("default"));
        assertThat("keepRecordType=false では記載値が使われず 'default' になること",
                layoutRecords.get(1).getTypeName(), is("default"));
    }

    /**
     * [YamlFileBuilder] buildFragmentsForSendSync: 値行が複数ある場合、連番が 1 始まりで
     * 1 行ごとにインクリメントされること。
     *
     * <p>
     * 連番は本体パーサ（{@code SendSyncMessageParser}）が YAML にはない No 列から取り出す値に相当する。
     * YAML 経路では行インデックスで補うため、全行が同じ値にならないことを固定する<br>
     * Given: records に値行を 3 行持つレコードが 1 件<br>
     * When:  buildFragmentsForSendSync を呼ぶ<br>
     * Then:  各値行の FIRST_FIELD_NO が記述順に "1", "2", "3" となること
     * </p>
     */
    @Test
    public void buildFragmentsForSendSync_rowNoIsIncrementedPerRow() {
        // Given: 値行 3 行（値は連番の検証と独立させるため互いに異なる値にする）
        List<Object> records = Arrays.<Object>asList(
                messageRecord("BODY", "PAYLOAD", 10, Arrays.asList("PAYLOAD_01", "PAYLOAD_02", "PAYLOAD_03")));
        MockMessages file = new MockMessages("dummy/sendSync.dat");
        file.setDirective("text-encoding", "MS932");

        // When
        // keepRecordType=true: 送信同期4セクションは record_type の記載値がそのままレコード種別になる
        YamlFileBuilder.buildFragmentsForSendSync(file, records, true,
                Collections.<TestDataInterpreter>emptyList());

        // Then
        List<DataRecord> dataRecords = file.toDataRecords();
        assertThat("値行 3 行が電文本文になること", dataRecords.size(), is(3));
        assertThat("1 行目の連番が \"1\" であること",
                dataRecords.get(0).getString(DataFileFragment.FIRST_FIELD_NO), is("1"));
        assertThat("2 行目の連番が \"2\" にインクリメントされること",
                dataRecords.get(1).getString(DataFileFragment.FIRST_FIELD_NO), is("2"));
        assertThat("3 行目の連番が \"3\" にインクリメントされること",
                dataRecords.get(2).getString(DataFileFragment.FIRST_FIELD_NO), is("3"));

        // Then: 連番は値行の中身とは独立していること（行の対応がずれていないことの確認）
        assertThat(dataRecords.get(0).getString("PAYLOAD"), is("PAYLOAD_01"));
        assertThat(dataRecords.get(1).getString("PAYLOAD"), is("PAYLOAD_02"));
        assertThat(dataRecords.get(2).getString("PAYLOAD"), is("PAYLOAD_03"));
    }

    // ========================================================================
    // 通常ファイル経路では record_type の値がそのままレコード種別名になること
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: 通常ファイル経路では record_type の値 "FW_HEADER" が
     * そのままレコード種別名として採用されること。
     *
     * <p>
     * {@code messages} 経路（{@code keepRecordType=false}）は record_type の記載値を使わず
     * "default" にするが、通常ファイル経路（{@code setup_files}／{@code expected_files}）は
     * 記述された値をそのまま使う。"FW_HEADER" もこの経路では予約値ではなく普通の文字列として
     * 扱われる、という非対称を固定する<br>
     * Given: setup_files の fwHeaderRecordTypeInFile グループに record_type が "FW_HEADER" と
     *        "DATA" の 2 レコード（レコード長はいずれも 10 バイト）<br>
     * When:  buildFileList(yaml, "setup_files", "[fwHeaderRecordTypeInFile]", path) を呼ぶ<br>
     * Then:  レコード種別が "default" に潰されず "FW_HEADER" / "DATA" のまま採用され、
     *        両レコードとも読み飛ばされずデータ行になること
     * </p>
     */
    @Test
    public void buildFileList_fwHeaderRecordTypeIsUsedAsIsInFileRoute() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "setup_files", "[fwHeaderRecordTypeInFile]", DIR);

        // Then: record_type の値がそのままレコード種別名になること
        assertThat(result.size(), is(1));
        List<RecordDefinition> records = result.get(0).createLayout().getRecords();
        assertThat("2 フラグメントが生成されること", records.size(), is(2));
        assertThat("通常ファイル経路では \"FW_HEADER\" がそのままレコード種別名になること",
                records.get(0).getTypeName(), is("FW_HEADER"));
        assertThat("2つ目のレコード種別が DATA であること", records.get(1).getTypeName(), is("DATA"));

        // Then: FW_HEADER レコードも読み飛ばされずデータ行になること
        List<DataRecord> dataRecords = result.get(0).toDataRecords();
        assertThat("FW_HEADER 行 + DATA 行の 2 件が返ること", dataRecords.size(), is(2));
        assertThat(dataRecords.get(0).getRecordType(), is("FW_HEADER"));
        assertThat(dataRecords.get(0).getString("HEAD_KEY"), is("HEADKEY001"));
        assertThat(dataRecords.get(1).getRecordType(), is("DATA"));
        assertThat(dataRecords.get(1).getString("BODY_KEY"), is("BODYKEY001"));
    }

    // ========================================================================
    // 値行と fields の対応付け（JSON スキーマの description が述べる挙動）
    //
    // ntf-testdata-yaml-schema.json の
    //   $defs.record_fragment.properties.rows.description
    // が述べている挙動を、YAML ファイルを経由した実際の経路
    // （YamlFileBuilder#buildDataFileList → DataFile#toDataRecords()）で実測して固定する。
    // 担保するのは以下の 3 点である。
    //   * 値行の要素数が fields の件数に満たないとき、不足した末尾のフィールドが "" になること
    //   * 空配列 [] を 1 要素書くと、全フィールドが "" のレコード 1 件になること
    //   * rows が 0 件のとき、データ行が 0 件になること
    //
    // フィクスチャに可変長（VariableLengthFile）を使うのは、可変長の DataRecord 変換が
    // 恒等写像（VariableLengthFileFragment#convertValue が引数をそのまま返す）であり、
    // DataFileFragment が値行に詰めた文字列がそのまま DataRecord の値として観測できるためである。
    // 固定長は convertForDataRecord が removePadding を通すため、"" が "" のまま返る根拠を
    // 当リポジトリの依存範囲で示せない。
    //
    // これらのテストは description の文字列自体を参照しない。したがって description の
    // 文言が変わってもテストは落ちない。文言と挙動の整合を保つのは文言を書く側の責務で、
    // ここで固定するのは文言が拠り所とする実挙動のみである。
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFileList: 値行の要素数が fields の件数に満たない場合、不足した末尾のフィールドが "" になること。
     *
     * <p>
     * description が述べる挙動:
     * {@code $defs.record_fragment.properties.rows.description} の
     * 「NTF は fields の順序で先頭から対応付ける」および
     * 「各配列の要素数が fields の件数に満たない場合、不足した末尾のフィールドは {@code ""} として扱われる」。<br>
     * 値行は fields の順序で先頭から対応付けられ、行データが尽きた位置以降は "" で埋められる。
     * 値行の先頭に 1 要素挿入して対応付けをずらすと FIELD1 の判定が落ち、不足分を null で埋めると
     * FIELD2／FIELD3 の判定が落ちる（どちらも実際に確認済み。{@code checks/task-22.md} の変異確認欄）。<br>
     * Given: expected_files の shortRow グループに、fields 3 件に対し要素 1 件（{@code ["AAAAA"]}）の値行<br>
     * When:  buildFileList(yaml, "expected_files", "[shortRow]", path) を呼び toDataRecords() する<br>
     * Then:  レコード 1 件が返り、FIELD1 が "AAAAA"、FIELD2 と FIELD3 が "" になること
     * </p>
     */
    @Test
    public void buildFileList_shortRowFillsTrailingFieldsWithEmptyString() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "expected_files", "[shortRow]", DIR);

        // Then
        assertThat("1 件取得できること", result.size(), is(1));
        List<DataRecord> records = result.get(0).toDataRecords();
        assertThat("値行 1 件がレコード 1 件になること", records.size(), is(1));
        DataRecord record = records.get(0);
        assertThat("fields の件数分のフィールドが埋まること", record.size(), is(3));
        assertThat("先頭から対応付けられること", record.get("FIELD1"), is((Object) "AAAAA"));
        assertThat("不足した 2 番目のフィールドが \"\" になること", record.get("FIELD2"), is((Object) ""));
        assertThat("不足した 3 番目のフィールドが \"\" になること", record.get("FIELD3"), is((Object) ""));
    }

    /**
     * [YamlFileBuilder] buildFileList: 空配列 [] を 1 要素書くと、全フィールドが "" のレコード 1 件になること。
     *
     * <p>
     * description が述べる挙動:
     * {@code $defs.record_fragment.properties.rows.description} の
     * 「これを利用し、空配列 {@code []} を1要素書くと全フィールドが {@code ""} のレコード1件になる」。<br>
     * 要素数 0 の値行も 1 件の値行として扱われ、全フィールドが "" で埋められる。
     * ここで固定するのは要素数 0 の配列という表記であり、空文字を要素数ぶん明示的に並べた
     * {@code ["", "", ""]} の側は {@link #buildFileList_allBlankFieldRecordIsKept()} が担保する。
     * 空の値行をスキップすると件数の判定が落ち、不足分を null で埋めると各フィールドの判定が落ちる
     * （どちらも実際に確認済み。{@code checks/task-22.md} の変異確認欄）。<br>
     * Given: expected_files の emptyRow グループに、fields 2 件に対し空配列 {@code []} 1 件の rows<br>
     * When:  buildFileList(yaml, "expected_files", "[emptyRow]", path) を呼び toDataRecords() する<br>
     * Then:  レコード 1 件が返り、FIELD1 と FIELD2 がいずれも "" になること
     * </p>
     */
    @Test
    public void buildFileList_emptyRowBecomesOneRecordOfEmptyStrings() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "expected_files", "[emptyRow]", DIR);

        // Then
        assertThat("1 件取得できること", result.size(), is(1));
        List<DataRecord> records = result.get(0).toDataRecords();
        assertThat("空配列 1 件がレコード 1 件になること", records.size(), is(1));
        DataRecord record = records.get(0);
        assertThat("fields の件数分のフィールドが埋まること", record.size(), is(2));
        assertThat("FIELD1 が \"\" になること", record.get("FIELD1"), is((Object) ""));
        assertThat("FIELD2 が \"\" になること", record.get("FIELD2"), is((Object) ""));
    }

    /**
     * [YamlFileBuilder] buildFileList: rows が 0 件のとき、データ行が 0 件になること。
     *
     * <p>
     * description が述べる挙動:
     * {@code $defs.record_fragment.properties.rows.description} の「rows が0件でも有効」。<br>
     * rows が 0 件でもレコード定義は生成され、データ行だけが 0 件になる。
     * 0 件の rows をレコード定義ごと落とすとレコード定義の判定が落ち、空の値行を 1 件補うと
     * データ行の判定が落ちる（どちらも実際に確認済み。{@code checks/task-22.md} の変異確認欄）。<br>
     * Given: expected_files の noRows グループに、fields 2 件・{@code rows: []} のエントリ<br>
     * When:  buildFileList(yaml, "expected_files", "[noRows]", path) を呼ぶ<br>
     * Then:  DataFile が 1 件返り、レコード定義は 1 件生成され、toDataRecords() が 0 件であること
     * </p>
     */
    @Test
    public void buildFileList_noRowsBecomesZeroDataRecords() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlFileBuilderTest/fileData");

        // When
        List<DataFile> result = buildFileList(yaml, "expected_files", "[noRows]", DIR);

        // Then
        assertThat("1 件取得できること", result.size(), is(1));
        assertThat("レコード定義は生成されること",
                result.get(0).createLayout().getRecords().size(), is(1));
        assertThat("データ行が 0 件であること", result.get(0).toDataRecords().size(), is(0));
    }

    /**
     * メッセージ系のレコードレイアウト（1 フィールド・1 値行）を組み立てる。
     *
     * @param recordType record_type に設定する値
     * @param fieldName  フィールド名
     * @param length     フィールド長（バイト）。固定長ファイルは全レコードのレコード長が一致している
     *                   必要があるため、複数レコードを組み立てる場合は同じ値を渡すこと
     * @param value      値行の値
     */
    private static Map<String, Object> messageRecord(String recordType, String fieldName,
                                                     int length, String value) {
        return messageRecord(recordType, fieldName, length, Arrays.asList(value));
    }

    /**
     * メッセージ系のレコードレイアウト（1 フィールド・複数値行）を組み立てる。
     *
     * @param recordType record_type に設定する値
     * @param fieldName  フィールド名
     * @param length     フィールド長（バイト）
     * @param values     値行の値（1 要素につき値行 1 行）
     */
    private static Map<String, Object> messageRecord(String recordType, String fieldName,
                                                     int length, List<String> values) {
        Map<String, Object> fieldDef = new LinkedHashMap<>();
        fieldDef.put("name", fieldName);
        fieldDef.put("type", "半角");
        fieldDef.put("length", length);
        List<Object> rows = new java.util.ArrayList<>();
        for (String value : values) {
            rows.add(Arrays.asList(value));
        }
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("record_type", recordType);
        record.put("fields", Arrays.<Object>asList(fieldDef));
        record.put("rows", rows);
        return record;
    }

    /** メッセージ系の length 未指定フィールド定義を組み立てる。 */
    private static Map<String, Object> messageFieldWithoutLength(String fieldName) {
        Map<String, Object> fieldDef = new LinkedHashMap<>();
        fieldDef.put("name", fieldName);
        fieldDef.put("type", "半角");
        return fieldDef;
    }
}
