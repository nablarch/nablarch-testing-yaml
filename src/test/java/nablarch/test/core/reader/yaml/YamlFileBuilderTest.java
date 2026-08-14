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
 * も本クラスで検証する。これらは {@link YamlFileBuilder} が公開する API であり、SUT が本クラスの
 * 対象クラスと一致するためである（{@link YamlMessageBuilder} 経由の結合検証は
 * {@code YamlMessageBuilderTest} が担う）。
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
    public void buildFileList_multipleRecordLayouts() throws Exception {
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
    // record_type の値 "FW_HEADER" が特別扱いされないこと（メッセージ系経路）
    // ========================================================================

    /**
     * [YamlFileBuilder] buildFragmentsForMessage: record_type の値が "FW_HEADER" のレコードも
     * 読み飛ばされずフラグメントとして構築されること。
     *
     * <p>
     * {@code record_type} に特別な予約値はなく、フレームワーク制御ヘッダは {@code fw_header:} マップで記述する。
     * よって "FW_HEADER" という値も他の値と同じく装飾的な名前として扱われる<br>
     * Given: records に record_type が "FW_HEADER" と "BODY" の 2 レコード<br>
     * When:  buildFragmentsForMessage を呼ぶ<br>
     * Then:  2 フラグメントが構築され、いずれも record_type が "default" に固定され、
     *        両レコードとも電文本文としてレンダリングされること
     * </p>
     */
    @Test
    public void buildFragmentsForMessage_fwHeaderRecordTypeIsNotSkipped() {
        // Given
        List<Object> records = Arrays.<Object>asList(
                messageRecord("FW_HEADER", "requestId", "0000000001"),
                messageRecord("BODY", "SEARCH_KEY", "SEARCHKEY1"));
        FixedLengthFile file = new FixedLengthFile("dummy/message.dat");
        file.setDirective("text-encoding", "MS932");

        // When
        YamlFileBuilder.buildFragmentsForMessage(file, records, Collections.<TestDataInterpreter>emptyList());

        // Then: レコード定義はフラグメント 1 件につき 1 件生成される
        List<RecordDefinition> layoutRecords = file.createLayout().getRecords();
        assertThat("FW_HEADER レコードも読み飛ばされず 2 フラグメント構築されること", layoutRecords.size(), is(2));
        assertThat("1つ目のレコード種別が 'default' に固定されること", layoutRecords.get(0).getTypeName(), is("default"));
        assertThat("2つ目のレコード種別が 'default' に固定されること", layoutRecords.get(1).getTypeName(), is("default"));

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
        YamlFileBuilder.buildFragmentsForMessage(file, Arrays.<Object>asList(record),
                Collections.<TestDataInterpreter>emptyList());

        // Then
        List<RecordDefinition> layoutRecords = file.createLayout().getRecords();
        assertThat("length 未指定の FW_HEADER レコードも 1 フラグメント構築されること", layoutRecords.size(), is(1));
        assertThat("レコード種別が 'default' に固定されること", layoutRecords.get(0).getTypeName(), is("default"));

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
     * 読み飛ばされずフラグメントとして構築され、値行に連番が付与されること。
     *
     * <p>
     * Given: records に record_type が "FW_HEADER" と "BODY" の 2 レコード<br>
     * When:  buildFragmentsForSendSync を呼ぶ<br>
     * Then:  2 フラグメントが構築され、FW_HEADER レコードの値行にも連番（FIRST_FIELD_NO="1"）が付与されること
     * </p>
     */
    @Test
    public void buildFragmentsForSendSync_fwHeaderRecordTypeIsNotSkipped() {
        // Given
        List<Object> records = Arrays.<Object>asList(
                messageRecord("FW_HEADER", "requestId", "0000000001"),
                messageRecord("BODY", "SEARCH_KEY", "SEARCHKEY1"));
        MockMessages file = new MockMessages("dummy/sendSync.dat");
        file.setDirective("text-encoding", "MS932");

        // When
        YamlFileBuilder.buildFragmentsForSendSync(file, records, Collections.<TestDataInterpreter>emptyList());

        // Then
        List<RecordDefinition> layoutRecords = file.createLayout().getRecords();
        assertThat("FW_HEADER レコードも読み飛ばされず 2 フラグメント構築されること", layoutRecords.size(), is(2));
        assertThat("1つ目のレコード種別が 'default' に固定されること", layoutRecords.get(0).getTypeName(), is("default"));
        assertThat("2つ目のレコード種別が 'default' に固定されること", layoutRecords.get(1).getTypeName(), is("default"));

        List<DataRecord> dataRecords = file.toDataRecords();
        assertThat("FW_HEADER レコードの値行も電文本文になること", dataRecords.size(), is(2));
        assertThat("FW_HEADER レコードの値行が保持されること",
                dataRecords.get(0).getString("requestId"), is("0000000001"));
        assertThat("FW_HEADER レコードの値行にも連番（レコード内 1 始まり）が付与されること",
                dataRecords.get(0).getString(DataFileFragment.FIRST_FIELD_NO), is("1"));
        assertThat("BODY レコードの値行にも連番（レコード内 1 始まり）が付与されること",
                dataRecords.get(1).getString(DataFileFragment.FIRST_FIELD_NO), is("1"));
    }

    /** メッセージ系のレコードレイアウト（1 フィールド・1 値行）を組み立てる。 */
    private static Map<String, Object> messageRecord(String recordType, String fieldName, String value) {
        Map<String, Object> fieldDef = new LinkedHashMap<>();
        fieldDef.put("name", fieldName);
        fieldDef.put("type", "半角");
        fieldDef.put("length", value.length());
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("record_type", recordType);
        record.put("fields", Arrays.<Object>asList(fieldDef));
        record.put("rows", Arrays.<Object>asList(Arrays.asList(value)));
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
