package nablarch.test.core.reader.yaml;

import nablarch.core.dataformat.LayoutDefinition;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.reader.DataType;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link YamlMessageBuilder} のメッセージ系メソッド（{@code buildMessagePool}／{@code buildSendSyncList}）のテストクラス。
 *
 * <p>
 * {@link YamlLoader#load} が返す YAML Map を {@link YamlMessageBuilder} が走査し、値加工
 * （FW_HEADER スキップ・メッセージ長 {@code -} 注入・{@code fw_header} のマップ検証）して
 * {@link MessagePool}・{@link nablarch.test.core.file.MockMessages} を組み立てる一連のロジックを検証する。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlMessageBuilderTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    private YamlMessageBuilder builder;

    @Before
    public void before() {
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("interpreters");
        builder = new YamlMessageBuilder(InterpreterResolver.withBinaryFile(interpreters));
    }

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    // ------------------------------------------------------------------------
    // ビルダ（YAML Map → 本体器）を通すヘルパー。
    // fw_header を使うのは messages 経路のみ（expected_*/response_* は空 Map）。
    // ------------------------------------------------------------------------

    private MessagePool buildMessagePool(Map<String, Object> yaml, String sectionKey, String id, String path) {
        boolean useFwHeader = YamlSection.KEY_MESSAGES.equals(sectionKey);
        return builder.buildMessagePool(yaml, sectionKey, id, useFwHeader, path);
    }

    private List<RequestTestingMessagePool> buildSendSyncMessageList(Map<String, Object> yaml, String sectionKey,
                                                                     String groupId, String path) {
        return builder.buildSendSyncList(yaml, sectionKey, groupId, path);
    }

    /** MessagePool の内部 FixedLengthFile（source）を取り出すヘルパー。 */
    private static FixedLengthFile sourceOf(MessagePool pool) throws Exception {
        Field sourceField = MessagePool.class.getDeclaredField("source");
        sourceField.setAccessible(true);
        return (FixedLengthFile) sourceField.get(pool);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getFwHeader(MessagePool pool) throws Exception {
        Field f = MessagePool.class.getDeclaredField("fwHeader");
        f.setAccessible(true);
        return (Map<String, String>) f.get(pool);
    }

    // ========================================================================
    // buildMessagePool: getMessage 相当
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: messages の id 指定でメッセージが取得でき、
     * FW ヘッダ（requestId・userId 等）が設定されていること。
     *
     * <p>
     * Given: messages に id=req001 が FW_HEADER/BODY レコードで定義されている<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返り、requestId="0000000001", userId="testUser01" が設定されていること
     * </p>
     */
    @Test
    public void buildMessagePool_withFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "req001", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));

        // FW ヘッダ実値の検証
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("requestId が設定されていること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("userId が設定されていること", fwHeader.get("userId"), is("testUser01"));
        assertThat("resendFlag が設定されていること", fwHeader.get("resendFlag"), is("0"));
        assertThat("resultCode が設定されていること", fwHeader.get("resultCode"), is("0000"));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: 存在しない ID を指定した場合は null が返ること。
     *
     * <p>
     * Given: messages に存在しない id<br>
     * When:  buildMessagePool(yaml, "messages", "noSuchId", path) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void buildMessagePool_idNotFound() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "noSuchId", DIR);

        // Then
        assertNull(result);
    }

    // ========================================================================
    // buildMessagePool: セクションキーに応じたメッセージが取得できること
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: expected_request_body_messages から取得できること。
     *
     * <p>
     * Given: expected_request_body_messages に id=req001<br>
     * When:  buildMessagePool(yaml, "expected_request_body_messages", "req001", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返ること
     * </p>
     */
    @Test
    public void buildMessagePool_expectedRequestBodyMessages() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = buildMessagePool(yaml, "expected_request_body_messages", "req001", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: expected_request_header_messages から取得できること（7.2 G-5）。
     *
     * <p>
     * 解説書 7.2: expected_request_header_messages セクションから buildMessagePool で取得できること<br>
     * Given: expected_request_header_messages に id=req001（FW_HEADER レコード）<br>
     * When:  buildMessagePool(yaml, "expected_request_header_messages", "req001", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返ること
     * </p>
     */
    @Test
    public void buildMessagePool_expectedRequestHeaderMessages() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = buildMessagePool(yaml, "expected_request_header_messages", "req001", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: messages の id にパスセグメントを含む形式が正しく取得できること（7.3 G-4）。
     *
     * <p>
     * 解説書 7.1/7.3: sendSyncTestData/{requestId}/message という id 形式が正しく取得できること<br>
     * Given: messages に id="sendSyncTestData/REQ001/message"<br>
     * When:  buildMessagePool(yaml, "messages", "sendSyncTestData/REQ001/message", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返り、FW ヘッダの requestId="REQ0000001" であること
     * </p>
     */
    @Test
    public void buildMessagePool_idWithPathSegments() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "sendSyncTestData/REQ001/message", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("requestId が正しく設定されていること", fwHeader.get("requestId"), is("REQ0000001"));
        assertThat("userId が正しく設定されていること", fwHeader.get("userId"), is("pathUser01"));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: response_body_messages の id 指定で取得できること。
     *
     * <p>
     * Given: response_body_messages に id=resp001<br>
     * When:  buildMessagePool(yaml, "response_body_messages", "resp001", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が返ること
     * </p>
     */
    @Test
    public void buildMessagePool_responseBodyMessages() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = buildMessagePool(yaml, "response_body_messages", "resp001", DIR);

        // Then
        assertNotNull(result);
        assertThat(result, instanceOf(RequestTestingMessagePool.class));
    }

    // ========================================================================
    // buildSendSyncMessageList: getSendSyncMessage 相当
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: group_id 指定でメッセージリストが取得できること。
     *
     * <p>
     * Given: response_body_messages に group_id=grp1 のエントリ<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "grp1", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool のリストが返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_normalCase() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "grp1", DIR);

        // Then
        assertNotNull(result);
        assertThat(result.size(), is(1));
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: 存在しない group_id を指定した場合は null が返ること。
     *
     * <p>
     * Given: 存在しない group_id "noSuchGroup"<br>
     * When:  buildSendSyncMessageList を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_groupIdNotFound() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "noSuchGroup", DIR);

        // Then
        assertNull(result);
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: requestId が MessagePool に設定されること（QA-3）。
     *
     * <p>
     * Given: response_body_messages に id=sync001, group_id=grp1 のエントリ<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "grp1", path) を呼ぶ<br>
     * Then:  result.get(0).getRequestId() が "sync001" を返すこと（QA-3）
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_requestIdIsSet() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "grp1", DIR);

        // Then
        assertNotNull(result);
        assertThat(result.get(0).getRequestId(), is("sync001"));
    }

    // ========================================================================
    // buildMessageFile: skipFwHeader=true で FW_HEADER フラグメント除外（QA観点1-軽微）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: BODY のみの messages を読んだとき
     * FixedLengthFile に 1 フラグメントだけ含まれること。
     *
     * <p>
     * Given: messages に id=req001 が fw_header: マップ + BODY レコードで定義されている<br>
     * When:  buildMessagePool を呼ぶ（records の BODY のみがフラグメントになる）<br>
     * Then:  FixedLengthFile の layout に BODY レコード 1 件のみ含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderFragmentExcluded() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: MessagePool を構築し、内部の FixedLengthFile（source）の構造を検証する
        MessagePool pool = buildMessagePool(yaml, "messages", "req001", DIR);
        assertNotNull(pool);
        FixedLengthFile file = sourceOf(pool);

        // Then: records に BODY のみ 1 フラグメントであること
        assertNotNull(file);
        LayoutDefinition layout = file.createLayout();
        assertThat("BODY レコードのみが含まれること", layout.getRecords().size(), is(1));
        assertThat("レコードタイプが 'default' に固定されること", layout.getRecords().get(0).getTypeName(), is("default"));
    }

    // ========================================================================
    // buildSendSyncMessageList: directives が MockMessages に設定されること（QA観点1-軽微）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: directives が MockMessages に設定されること。
     *
     * <p>
     * Given: response_body_messages の grp1 エントリに text-encoding: UTF-8 が指定されている<br>
     * When:  buildSendSyncMessageList を呼ぶ<br>
     * Then:  result.get(0).createLayout().getDirective("text-encoding") が "UTF-8" を返すこと
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_directivesAreSet() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "grp1", DIR);

        // Then: directives が MockMessages に設定されていること（source フィールド経由で確認）
        assertNotNull(result);
        Field sourceField = MessagePool.class.getDeclaredField("source");
        sourceField.setAccessible(true);
        FixedLengthFile source = (FixedLengthFile) sourceField.get(result.get(0));
        assertThat(source.createLayout().getDirective().get("text-encoding"), is("UTF-8"));
    }

    // ========================================================================
    // buildMessageFile: 存在しない ID で null が返ること（QA観点2-軽微）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: 存在しない ID を指定した場合は null が返ること（QA観点2-軽微）。
     *
     * <p>
     * Given: messages に存在しない id<br>
     * When:  buildMessagePool(yaml, "messages", "noSuchId", true, path) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void buildMessageFile_idNotFound() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: 存在しない id では MessagePool が null になること
        MessagePool result = buildMessagePool(yaml, "messages", "noSuchId", DIR);

        // Then
        assertNull(result);
    }

    // ========================================================================
    // FW_HEADER rows が空のとき例外なく空 Map が返ること（E-3 分岐D）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: が空マップの場合、
     * 例外をスローせず空の fwHeader で MessagePool が返ること（E-3 分岐D）。
     *
     * <p>
     * Given: messages に id=emptyRows001 の fw_header が空マップ<br>
     * When:  buildMessagePool(yaml, "messages", "emptyRows001", path) を呼ぶ<br>
     * Then:  MessagePool が返り、fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void buildMessagePool_emptyFwHeaderRows() throws Exception {
        // Given: fw_header が空マップのエントリを直接構築（スキーマ上 minProperties:1 を回避）
        Map<String, Object> fieldDef = new LinkedHashMap<>();
        fieldDef.put("name", "SEARCH_KEY");
        fieldDef.put("type", "半角");
        fieldDef.put("length", 10);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("record_type", "BODY");
        record.put("fields", Arrays.<Object>asList(fieldDef));
        record.put("rows", Arrays.<Object>asList(Arrays.asList("SEARCHKEY1")));
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", "emptyRows001");
        entry.put("fw_header", new LinkedHashMap<String, Object>());  // 空マップ
        entry.put("records", Arrays.<Object>asList(record));
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("messages", Arrays.<Object>asList(entry));

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "emptyRows001", DIR);

        // Then: 例外なく返り、fwHeader は空 Map
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("fw_header が空マップのとき fwHeader は空 Map であること", fwHeader.size(), is(0));
    }

    // ========================================================================
    // RS-20: FW_HEADER フラグメントが存在しない場合は空 Map を FW ヘッダとして使用すること
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: マップがない場合、
     * 空 Map を FW ヘッダとして MessagePool が返ること（RS-20）。
     *
     * <p>
     * 解説書: RS-20（fw_header マップ不在の代替フロー）<br>
     * Given: messages に id=bodyOnly001 の BODY レコードのみ（fw_header マップなし）<br>
     * When:  buildMessagePool(yaml, "messages", "bodyOnly001", path) を呼ぶ<br>
     * Then:  MessagePool が返り、fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void buildMessagePool_noFwHeaderFragmentReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "bodyOnly001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("fw_header マップがない場合は空 Map が使用されること", fwHeader.size(), is(0));
    }

    // ========================================================================
    // FW_HEADER rows が Map 形式（誤記）のとき IllegalStateException + context（E-3）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: の値がマップでなくリスト形式の場合、
     * IllegalStateException がスローされ id がメッセージに含まれること（E-3）。
     *
     * <p>
     * Given: messages に id=malformed001 の fw_header がリスト形式（誤記）<br>
     * When:  buildMessagePool(yaml, "messages", "malformed001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、id がメッセージに含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_malformedFwHeaderRowsThrowsException() {
        // Given: fw_header がリスト形式（誤記）のエントリを直接構築（スキーマ検証の対象外で Builder の検証をテスト）
        Map<String, Object> requestIdMap = new LinkedHashMap<>();
        requestIdMap.put("requestId", "0000000001");
        Map<String, Object> fieldDef = new LinkedHashMap<>();
        fieldDef.put("name", "DATA");
        fieldDef.put("type", "半角");
        fieldDef.put("length", 10);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("record_type", "BODY");
        record.put("fields", Arrays.<Object>asList(fieldDef));
        record.put("rows", Arrays.<Object>asList(Arrays.asList("TESTDATA1")));
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", "malformed001");
        entry.put("fw_header", Arrays.<Object>asList(requestIdMap));  // リスト形式（誤記）
        entry.put("records", Arrays.<Object>asList(record));
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("messages", Arrays.<Object>asList(entry));

        // When
        try {
            buildMessagePool(yaml, "messages", "malformed001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("id がメッセージに含まれること", e.getMessage(), containsString("malformed001"));
        }
    }

    // ========================================================================
    // dataTypeToSectionKey: 不正DataTypeで IllegalArgumentException（QA観点2-中）
    // ========================================================================

    /**
     * [YamlSection] dataTypeToSectionKey: messaging 以外の DataType を渡した場合 IllegalArgumentException がスローされること（QA観点2-中）。
     *
     * <p>
     * Given: DataType.SETUP_TABLE_DATA（messaging 系以外）<br>
     * When:  YamlSection.dataTypeToSectionKey(DataType.SETUP_TABLE_DATA) を呼ぶ<br>
     * Then:  IllegalArgumentException がスローされること
     * </p>
     */
    @Test
    public void dataTypeToSectionKey_unsupportedDataTypeThrowsException() {
        // When
        try {
            YamlSection.dataTypeToSectionKey(DataType.SETUP_TABLE_DATA);
            fail("IllegalArgumentException が期待される");
        } catch (IllegalArgumentException e) {
            // Then
            // OK: 不正な DataType に対して例外がスローされること
        }
    }

    // ========================================================================
    // buildSendSyncMessageList: id なしエントリで requestId が設定されないこと
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: group_id があるが id がないエントリの場合、
     * MessagePool の requestId が null のまま返ること。
     *
     * <p>
     * Given: response_body_messages に group_id=grp2 のエントリが id フィールドなしで定義されている<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "grp2", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が 1 件返り、getRequestId() が null であること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_noIdEntryReturnsPoolWithNullRequestId() throws Exception {
        // Given: id キーのないエントリを直接構築（スキーマ検証の対象外で Builder の防衛コードをテスト）
        Map<String, Object> fieldDef = new LinkedHashMap<>();
        fieldDef.put("name", "DATA");
        fieldDef.put("type", "半角");
        fieldDef.put("length", 10);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("record_type", "BODY");
        record.put("fields", Arrays.<Object>asList(fieldDef));
        record.put("rows", Arrays.<Object>asList(Arrays.asList("NO_ID_DATA")));
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("group_id", "grp2");
        // id キーを意図的に省略
        entry.put("records", Arrays.<Object>asList(record));
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("response_body_messages", Arrays.<Object>asList(entry));

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "grp2", DIR);

        // Then
        assertNotNull(result);
        assertThat("id なしエントリは 1 件返ること", result.size(), is(1));
        assertNull("id なしエントリの requestId は null であること", result.get(0).getRequestId());
    }

    // ========================================================================
    // extractFwHeader: FW_HEADER の行がフィールド数より少ない場合はスキップされること
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: マップに一部のキーのみ含まれる場合、
     * 記載されたキーのみ fwHeader に設定されること。
     *
     * <p>
     * Given: messages_partial_fw_header に id=partialHeader001 の fw_header が requestId のみ<br>
     * When:  buildMessagePool(yaml, "messages_partial_fw_header", "partialHeader001", path) を呼ぶ<br>
     * Then:  fwHeader に requestId のみ設定され、userId は含まれないこと
     * </p>
     */
    @Test
    public void buildMessagePool_shortFwHeaderRowOnlyCoversAvailableFields() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "partialHeader001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("記載された requestId は設定されること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("記載されていない userId は含まれないこと", fwHeader.containsKey("userId"), is(false));
    }


    // ========================================================================
    // fwHeaderFields カスタム設定（QA-4）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: マップにプロジェクト独自キーと既定キーが混在する場合、
     * fw_header に記述した全キーが保持されること（fwHeaderFields フィルタ廃止後の確認）。
     *
     * <p>
     * Given: messages に id=req001 の fw_header マップに customField/requestId を記述<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  customField と requestId の両方が FW ヘッダに含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_customFwHeaderFields() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/customFwHeaderData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "req001", DIR);

        // Then: fw_header に記述した全キーが保持されること（フィルタなし）
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("独自キー customField が保持されること", fwHeader.get("customField"), is("CUSTOM_VALUE"));
        assertThat("既定キー requestId も保持されること", fwHeader.get("requestId"), is("0000000001"));
    }

    // ========================================================================
    // T2: fw_header マップ対応（ランタイム、messages 限定）
    // ========================================================================

    /**
     * [MS-04] messages の fw_header: マップの全キー（既定＋独自）が getFwHeader() に保持されること。
     *
     * <p>
     * Given: messages に id=req001 のエントリが fw_header: マップ
     *        （requestId/userId/resendFlag/resultCode + customProjectKey）を持つ YAML<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader に全キー（既定＋独自）が保持されること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderMapAllKeysRetainedIncludingCustom() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "req001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("requestId が設定されていること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("userId が設定されていること", fwHeader.get("userId"), is("testUser01"));
        assertThat("resendFlag が設定されていること", fwHeader.get("resendFlag"), is("0"));
        assertThat("resultCode が設定されていること", fwHeader.get("resultCode"), is("0000"));
        assertThat("独自キー customProjectKey が黙って消えないこと", fwHeader.get("customProjectKey"), is("PROJECT_VALUE"));
    }

    /**
     * [MS-04] records 側に FW_HEADER レコードがなくても fw_header: マップから FW ヘッダが取得できること。
     *
     * <p>
     * Given: messages エントリに fw_header: マップがあり records には FW_HEADER レコードがない YAML<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader に requestId が設定されていること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderMapReadableWithoutFwHeaderRecord() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "req001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("records に FW_HEADER レコードがなくても fw_header マップから取得できること",
                fwHeader.get("requestId"), is("0000000001"));
    }

    /**
     * [MS-04] getMessageWithoutCache（expected/response）経路は extractFwHeader を呼ばず空 Map を渡すこと。
     *
     * <p>
     * Given: expected_request_body_messages に id=req001 のエントリ（fw_header: なし）<br>
     * When:  buildMessagePool(yaml, "expected_request_body_messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること（extractFwHeader を呼ばない）
     * </p>
     */
    @Test
    public void buildMessagePool_expectedRequestBodyMessagesReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "expected_request_body_messages", "req001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("expected_request_* 経路は fwHeader が空 Map であること", fwHeader.isEmpty(), is(true));
    }

    /**
     * [MS-04] getMessageWithoutCache（response_*）経路は extractFwHeader を呼ばず空 Map を渡すこと。
     *
     * <p>
     * Given: response_body_messages に id=resp001 のエントリ<br>
     * When:  buildMessagePool(yaml, "response_body_messages", "resp001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void buildMessagePool_responseBodyMessagesReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "response_body_messages", "resp001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("response_* 経路は fwHeader が空 Map であること", fwHeader.isEmpty(), is(true));
    }

    /**
     * [MS-04] getMessageWithoutCache（expected_request_header_messages）経路は extractFwHeader を呼ばず空 Map を渡すこと。
     *
     * <p>
     * Given: expected_request_header_messages に id=req001 のエントリ<br>
     * When:  buildMessagePool(yaml, "expected_request_header_messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void buildMessagePool_expectedRequestHeaderMessagesReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "expected_request_header_messages", "req001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("expected_request_header_messages 経路は fwHeader が空 Map であること", fwHeader.isEmpty(), is(true));
    }

    /**
     * [MS-04] getMessageWithoutCache（response_header_messages）経路は extractFwHeader を呼ばず空 Map を渡すこと。
     *
     * <p>
     * Given: response_header_messages に id=resp001 のエントリ<br>
     * When:  buildMessagePool(yaml, "response_header_messages", "resp001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void buildMessagePool_responseHeaderMessagesReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "response_header_messages", "resp001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("response_header_messages 経路は fwHeader が空 Map であること", fwHeader.isEmpty(), is(true));
    }

    /**
     * [MS-04] fw_header: の値がクォートなしの数値・真偽値の場合、文字列に変換されること。
     *
     * <p>
     * Given: messages に id=numericValues001 の fw_header にクォートなし数値 (0, 1234) と真偽値 (true) を記述<br>
     * When:  buildMessagePool(yaml, "messages", "numericValues001", path) を呼ぶ<br>
     * Then:  fwHeader の各値が文字列に変換されていること（"0", "1234", "true"）
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderMapWithUnquotedNumericAndBooleanValues() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "numericValues001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("整数値が文字列に変換されること", fwHeader.get("resendFlag"), is("0"));
        assertThat("4桁整数が文字列に変換されること", fwHeader.get("resultCode"), is("1234"));
        assertThat("真偽値が文字列に変換されること", fwHeader.get("boolFlag"), is("true"));
    }

    // ========================================================================
    // buildSendSyncMessageList: length なしフィールドを持つエントリでも NPE が起きないこと
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: length が指定されていないフィールドを持つエントリを
     * 読み込んでも NullPointerException が発生しないこと。
     *
     * <p>
     * Given: response_body_messages に group_id=noLengthGrp のエントリが length なしフィールドで定義されている<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "noLengthGrp", path) を呼ぶ<br>
     * Then:  NullPointerException が発生せず、RequestTestingMessagePool が 1 件返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_fieldWithoutLengthDoesNotThrowNpe() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: length なしフィールドの MockMessages を buildSendSyncMessageList で構築する
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "noLengthGrp", DIR);

        // Then
        assertNotNull("length なしフィールドでも NPE が発生せず結果が返ること", result);
        assertThat("エントリが 1 件返ること", result.size(), is(1));
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: length が指定されているフィールドと
     * 指定されていないフィールドが混在する場合でも NullPointerException が発生しないこと。
     *
     * <p>
     * Given: response_body_messages に group_id=mixedLengthGrp のエントリが
     *        length ありフィールド（FIXED_FIELD）と length なしフィールド（NO_LENGTH_FIELD）を持つ<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "mixedLengthGrp", path) を呼ぶ<br>
     * Then:  NullPointerException も NumberFormatException も発生せず、RequestTestingMessagePool が 1 件返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_partialLengthFieldDoesNotThrowException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: 一部 length あり・一部なしの MockMessages を buildSendSyncMessageList で構築する
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "mixedLengthGrp", DIR);

        // Then
        assertNotNull("一部 length なしフィールドでも例外が発生せず結果が返ること", result);
        assertThat("エントリが 1 件返ること", result.size(), is(1));
    }

    /**
     * [MS-04] messages の fw_header: がない場合は空 Map を返すこと。
     *
     * <p>
     * Given: messages に id=bodyOnly001 のエントリ（fw_header: なし）<br>
     * When:  buildMessagePool(yaml, "messages", "bodyOnly001", path) を呼ぶ<br>
     * Then:  fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void buildMessagePool_messagesWithoutFwHeaderMapReturnsEmptyFwHeader() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "bodyOnly001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("fw_header: マップなしの messages エントリは空 Map であること", fwHeader.isEmpty(), is(true));
    }

    // ========================================================================
    // buildSendSyncBodies: group_id 一致・不一致・null のテスト
    // ========================================================================

    // ========================================================================
    // buildSendSyncList: stripBrackets の null / 角括弧付き分岐カバレッジ
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: groupId に null を渡したとき null が返ること（stripBrackets null 分岐）。
     *
     * <p>
     * stripBrackets(null) → null を返し、rawGroupId.equals(null) が呼ばれないため全エントリがスキップされ null が返る。<br>
     * Given: response_body_messages にエントリが存在する<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", null, path) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_nullGroupIdReturnsNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: groupId=null → stripBrackets は null を返し、比較がスキップされ結果は null
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", null, DIR);

        // Then
        assertNull("groupId が null のとき null が返ること", result);
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: groupId に "[grp1]" を渡したとき stripBrackets が角括弧を除去してマッチすること。
     *
     * <p>
     * stripBrackets("[grp1]") → "grp1" となり、YAML の group_id=grp1 にマッチする。<br>
     * Given: response_body_messages に group_id=grp1 のエントリが定義されている<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[grp1]", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が 1 件返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_bracketGroupIdStripped() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: "[grp1]" → stripBrackets → "grp1" → YAML の group_id=grp1 にマッチ
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "[grp1]", DIR);

        // Then
        assertNotNull("角括弧付き groupId でも正しくマッチして結果が返ること", result);
        assertThat("1 件返ること", result.size(), is(1));
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: groupId が "[" で始まるが "]" で終わらない場合は角括弧が除去されずそのまま使われ null が返ること（stripBrackets partial-bracket 分岐）。
     *
     * <p>
     * stripBrackets の条件 startsWith("[") &amp;&amp; endsWith("]") が false になる分岐（startsWith true / endsWith false）を踏む。<br>
     * Given: response_body_messages に group_id=grp1 のエントリが定義されている<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[grp1", path) を呼ぶ<br>
     * Then:  "[grp1" はそのまま使われ、grp1 とはマッチしないため null が返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_partialBracketGroupIdReturnsNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: "[grp1" は startsWith("[")=true かつ endsWith("]")=false → stripBrackets はそのまま返す → 不一致
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "[grp1", DIR);

        // Then
        assertNull("角括弧が不完全な groupId はマッチせず null が返ること", result);
    }

    /**
     * [YamlMessageBuilder] buildSendSyncBodies: group_id が一致するとき FixedLengthFile リストが返ること。
     *
     * <p>
     * Given: response_body_messages に group_id=grp1 のエントリが定義されている<br>
     * When:  buildSendSyncBodies(yaml, "response_body_messages", "grp1", path) を呼ぶ<br>
     * Then:  FixedLengthFile が 1 件返ること
     * </p>
     */
    @Test
    public void buildSendSyncBodies_groupIdMatchReturnsFixedLengthFileList() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<FixedLengthFile> result = builder.buildSendSyncBodies(
                yaml, "response_body_messages", "grp1", DIR);

        // Then
        assertNotNull(result);
        assertThat("group_id が一致するとき 1 件返ること", result.size(), is(1));
    }

    /**
     * [YamlMessageBuilder] buildSendSyncBodies: group_id が不一致のとき空リストが返ること。
     *
     * <p>
     * Given: response_body_messages に "grp1" のエントリが定義されている<br>
     * When:  buildSendSyncBodies(yaml, "response_body_messages", "noSuchGroup", path) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void buildSendSyncBodies_groupIdMismatchReturnsEmptyList() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<FixedLengthFile> result = builder.buildSendSyncBodies(
                yaml, "response_body_messages", "noSuchGroup", DIR);

        // Then
        assertNotNull(result);
        assertThat("group_id が不一致のとき空リストが返ること", result.isEmpty(), is(true));
    }

    /**
     * [YamlMessageBuilder] buildSendSyncBodies: groupId に null を渡したとき空リストが返ること。
     *
     * <p>
     * buildSendSyncBodies は rawGroupId の null チェック（{@code rawGroupId != null &&}）で比較前に
     * 短絡評価するため、groupId=null でも NPE は発生せず全エントリがスキップされる。<br>
     * Given: response_body_messages に group_id=grp1 のエントリが定義されている<br>
     * When:  buildSendSyncBodies(yaml, "response_body_messages", null, path) を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void buildSendSyncBodies_nullGroupIdReturnsEmptyList() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: rawGroupId != null の短絡評価により全エントリがスキップされ空リストが返る
        List<FixedLengthFile> result = builder.buildSendSyncBodies(
                yaml, "response_body_messages", null, DIR);

        // Then
        assertNotNull(result);
        assertThat("null groupId のとき空リストが返ること", result.isEmpty(), is(true));
    }

    // ========================================================================
    // InterpreterResolver.raw(): resolve("any") が空リストを返すこと
    // ========================================================================

    /**
     * [InterpreterResolver] raw(): resolve("any") が常に空リストを返すこと。
     *
     * <p>
     * Given: InterpreterResolver.raw() で生成したリゾルバ<br>
     * When:  resolver.resolve("anyPath") を呼ぶ<br>
     * Then:  空リストが返ること
     * </p>
     */
    @Test
    public void interpreterResolverRaw_resolveReturnsEmptyList() {
        // Given
        InterpreterResolver resolver = InterpreterResolver.raw();

        // When
        List<TestDataInterpreter> result = resolver.resolve("anyPath");

        // Then
        assertNotNull(result);
        assertThat("raw() の resolve は常に空リストを返すこと", result.isEmpty(), is(true));
    }
}
