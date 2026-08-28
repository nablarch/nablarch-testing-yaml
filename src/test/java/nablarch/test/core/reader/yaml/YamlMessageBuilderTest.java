package nablarch.test.core.reader.yaml;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.reader.DataType;
import nablarch.test.core.reader.MessageParser;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * （メッセージ長 {@code -} 注入・{@code fw_header} のマップ検証）して
 * {@link MessagePool}・{@link nablarch.test.core.file.MockMessages} を組み立てる一連のロジックを検証する。
 * </p>
 */
@RunWith(DatabaseTestRunner.class)
public class YamlMessageBuilderTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    /** FW 制御ヘッダの項目名を設定する SystemRepository のキー */
    private static final String FW_HEADER_FIELDS_KEY = "reader.fwHeaderfields";

    private YamlMessageBuilder builder;

    @Before
    public void before() {
        // 電文用のインタープリタリスト（解説書が定める CompositeInterpreter の 1 つだけ）を使う。
        List<TestDataInterpreter> interpreters = repositoryResource.getComponent("yamlMessagingInterpreters");
        builder = new YamlMessageBuilder(InterpreterResolver.withBinaryFile(interpreters));
    }

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
        // reader.fwHeaderfields を設定したテストの影響を他テストへ持ち越さない（空文字は未設定と同じ扱い）
        repositoryResource.addComponent(FW_HEADER_FIELDS_KEY, "");
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

    /** FW 制御ヘッダの項目名（{@code reader.fwHeaderfields}）を設定するヘルパー。{@link #after()} で解除される。 */
    private void setFwHeaderFields(String fwHeaderFields) {
        repositoryResource.addComponent(FW_HEADER_FIELDS_KEY, fwHeaderFields);
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

    /**
     * 本体 {@code MessageParser} が持つ FW 制御ヘッダ項目名の集合（private フィールド {@code fwHeaderFields}）を
     * 取り出すヘルパー。
     *
     * <p>
     * 本体 {@code ../nablarch-testing/src/main/java/nablarch/test/core/reader/MessageParser.java:107}-{@code :110}
     * のフィールド初期化は {@code MessageParser} の生成時に走るため、生成のたびに現在の
     * {@code reader.fwHeaderfields} が反映される。解析は行わないので reader・interpreters は null でよい。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private static Set<String> mainFwHeaderFields() throws Exception {
        MessageParser parser = new MessageParser(null, null, DataType.MESSAGE);
        Field f = MessageParser.class.getDeclaredField("fwHeaderFields");
        f.setAccessible(true);
        return (Set<String>) f.get(parser);
    }

    /** {@link YamlMessageBuilder} が許可する FW 制御ヘッダ項目名の集合（private static {@code fwHeaderFields()}）を取り出すヘルパー。 */
    @SuppressWarnings("unchecked")
    private static Set<String> yamlFwHeaderFields() throws Exception {
        Method m = YamlMessageBuilder.class.getDeclaredMethod("fwHeaderFields");
        m.setAccessible(true);
        return (Set<String>) m.invoke(null);
    }

    /**
     * {@code fw_header} の値だけを差し替えた messages 1 エントリの YAML Map を組み立てるヘルパー。
     *
     * <p>スキーマ検証を通さずに {@link YamlMessageBuilder} の検証だけを試すために使う。</p>
     *
     * @param id            エントリの id
     * @param fwHeaderValue {@code fw_header} に置く生値
     * @return YAML トップレベル Map
     */
    private static Map<String, Object> yamlWithFwHeader(String id, Object fwHeaderValue) {
        Map<String, Object> fieldDef = new LinkedHashMap<>();
        fieldDef.put("name", "DATA");
        fieldDef.put("type", "半角");
        fieldDef.put("length", 5);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("record_type", "BODY");
        record.put("fields", Arrays.<Object>asList(fieldDef));
        record.put("rows", Arrays.<Object>asList(Arrays.asList("HELLO")));
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", id);
        entry.put("fw_header", fwHeaderValue);
        entry.put("records", Arrays.<Object>asList(record));
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("messages", Arrays.<Object>asList(entry));
        return yaml;
    }

    // ========================================================================
    // buildMessagePool: getMessage 相当
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: messages の id 指定でメッセージが取得でき、
     * FW ヘッダ（requestId・userId 等）が設定されていること。
     *
     * <p>
     * Given: messages に id=req001 が fw_header: マップと BODY レコードで定義されている<br>
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
     * [YamlMessageBuilder] buildMessagePool: expected_request_header_messages から取得できること。
     *
     * <p>
     * expected_request_header_messages セクションから buildMessagePool で取得できること<br>
     * Given: expected_request_header_messages に id=req001（ヘッダ項目を fields/rows に記述したレコード）<br>
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
     * [YamlMessageBuilder] buildMessagePool: messages の id にパスセグメントを含む形式が正しく取得できること。
     *
     * <p>
     * sendSyncTestData/{requestId}/message という id 形式が正しく取得できること<br>
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
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[grp1]", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool のリストが返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_normalCase() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "[grp1]", DIR);

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
     * [YamlMessageBuilder] buildSendSyncMessageList: group_id を持たないエントリをグループID "" で取得して1件返ること。
     *
     * <p>
     * Given: response_body_messages に group_id を持たない resp001 エントリが存在する<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が 1 件返ること（group_id 省略 = グループなし）
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_noGroupId() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "", DIR);

        // Then
        assertNotNull("group_id 省略エントリがグループなし \"\" で取得できること", result);
        assertThat("1 件返ること", result.size(), is(1));
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: requestId が MessagePool に設定されること。
     *
     * <p>
     * Given: response_body_messages に id=sync001, group_id=grp1 のエントリ<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[grp1]", path) を呼ぶ<br>
     * Then:  result.get(0).getRequestId() が "sync001" を返すこと
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_requestIdIsSet() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "[grp1]", DIR);

        // Then
        assertNotNull(result);
        assertThat(result.get(0).getRequestId(), is("sync001"));
    }

    // ========================================================================
    // buildMessagePool: fw_header: に書いた項目は本文フラグメントにならないこと
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: messages の fw_header: マップに書いた項目は
     * 電文本文のフラグメントにならず、records に書いた本文レコードだけが本文になること。
     *
     * <p>
     * FW 制御ヘッダは {@code fw_header:} マップで受け、本文は {@code records} で受ける。
     * 両者が混ざらないことを固定する<br>
     * Given: messages の id=req001 が fw_header:（requestId/userId/resendFlag/resultCode）と
     *        BODY レコード（SEARCH_KEY のみ）を持つ<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  本文は BODY レコード 1 件のみで、fw_header: の項目は本文フィールドに現れず、
     *        FW 制御ヘッダ側にだけ現れること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderMapItemsAreNotBodyFragments() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool pool = buildMessagePool(yaml, "messages", "req001", DIR);
        assertNotNull(pool);

        // Then: 本文フラグメントは records に書いた BODY の 1 件だけであること
        FixedLengthFile file = sourceOf(pool);
        assertNotNull(file);
        assertThat("fw_header: はフラグメントにならず、本文は records の 1 レコードのみであること",
                file.createLayout().getRecords().size(), is(1));

        // Then: 本文フィールドは SEARCH_KEY のみで、fw_header: の項目を含まないこと
        List<DataRecord> messages = ((RequestTestingMessagePool) pool).getExpectedMessageList();
        assertThat("本文は 1 件であること", messages.size(), is(1));
        DataRecord body = messages.get(0);
        assertThat("records に書いた本文フィールドは取得できること", body.getString("SEARCH_KEY"), is("SEARCHKEY1"));
        assertThat("fw_header: の requestId が本文フィールドに混ざらないこと",
                body.containsKey("requestId"), is(false));
        assertThat("fw_header: の userId が本文フィールドに混ざらないこと",
                body.containsKey("userId"), is(false));
        assertThat("fw_header: の resendFlag が本文フィールドに混ざらないこと",
                body.containsKey("resendFlag"), is(false));
        assertThat("fw_header: の resultCode が本文フィールドに混ざらないこと",
                body.containsKey("resultCode"), is(false));

        // Then: fw_header: の項目は FW 制御ヘッダ側にだけ現れること
        Map<String, String> fwHeader = getFwHeader(pool);
        assertThat("requestId は FW 制御ヘッダとして取得できること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("userId は FW 制御ヘッダとして取得できること", fwHeader.get("userId"), is("testUser01"));
        assertThat("本文フィールド SEARCH_KEY は FW 制御ヘッダに混ざらないこと",
                fwHeader.containsKey("SEARCH_KEY"), is(false));
    }

    // ========================================================================
    // buildMessagePool: record_type の値が特別扱いされないこと
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: record_type の値が "FW_HEADER" のレコードも読み飛ばされず、
     * レコード種別 "FW_HEADER" のフラグメントとして電文本文になること。
     *
     * <p>
     * {@code record_type} に特別な予約値はない。{@code expected_request_header_messages} は
     * 同期応答メッセージ送信の 4 セクションの1つであり、記載した値がそのままレコード種別になる。
     * またこのセクションは {@code fw_header:} を使わずヘッダ項目も
     * {@code records} の fields/rows に記述するため、読み飛ばすと電文本文が 0 件になってしまう<br>
     * Given: expected_request_header_messages の id=req001 に record_type: FW_HEADER のレコードが
     *        requestId 等 4 フィールド・値行 1 行で定義されている<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  FixedLengthFile に 1 フラグメント（レコード種別 "FW_HEADER"）が構築され、
     *        値行が記述どおりの電文本文としてレンダリングされること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderRecordTypeIsNotSkipped() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: MessagePool を構築し、内部の FixedLengthFile（source）の構造を検証する
        MessagePool pool = buildMessagePool(yaml, "expected_request_header_messages", "req001", DIR);
        assertNotNull(pool);
        FixedLengthFile file = sourceOf(pool);

        // Then: FW_HEADER レコードも読み飛ばされず 1 フラグメントになること
        assertNotNull(file);
        LayoutDefinition layout = file.createLayout();
        assertThat("record_type: FW_HEADER のレコードもフラグメントになること", layout.getRecords().size(), is(1));
        assertThat("送信同期4セクションでは record_type の記載値がそのままレコード種別になること",
                layout.getRecords().get(0).getTypeName(), is("FW_HEADER"));

        // Then: 値行が電文本文としてレンダリングされること
        List<DataRecord> messages = ((RequestTestingMessagePool) pool).getExpectedMessageList();
        assertThat("期待電文が 1 件取得できること", messages.size(), is(1));
        assertThat(messages.get(0).getString("requestId"), is("0000000001"));
        assertThat(messages.get(0).getString("userId"), is("testUser01"));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code messages} では record_type の記載値が使われず
     * "default" になること。
     *
     * <p>
     * 送信同期 4 セクションとの対比を固定する。セクションキーが {@code messages} の場合は
     * 記載値によらずデフォルトのレコード種別（"default"）になる<br>
     * Given: messages の id=req001 に record_type: BODY のレコードがある<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  レコード種別が記載値 "BODY" ではなく "default" になること
     * </p>
     */
    @Test
    public void buildMessagePool_recordTypeIsDefaultForMessages() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        MessagePool pool = buildMessagePool(yaml, "messages", "req001", DIR);
        assertNotNull(pool);

        // Then
        LayoutDefinition layout = sourceOf(pool).createLayout();
        assertThat("messages では記載値 \"BODY\" が使われず \"default\" になること",
                layout.getRecords().get(0).getTypeName(), is("default"));
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: 送信同期経路では record_type の記載値が
     * そのままレコード種別になること。
     *
     * <p>
     * {@code response_body_messages} は同期応答メッセージ送信の 4 セクションの1つであり、
     * 記載した値がそのままレコード種別になる<br>
     * Given: response_body_messages の group_id=grp1 に record_type: BODY のレコードがある<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[grp1]", path) を呼ぶ<br>
     * Then:  電文のレコード種別が記載値 "BODY" になること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_recordTypeIsKeptAsIs() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "[grp1]", DIR);

        // Then
        assertNotNull(result);
        assertThat(result.size(), is(1));
        List<DataRecord> messages = result.get(0).getExpectedMessageList();
        assertThat("電文が 1 件取得できること", messages.size(), is(1));
        assertThat("送信同期4セクションでは record_type の記載値がそのままレコード種別になること",
                messages.get(0).getRecordType(), is("BODY"));
    }

    // ========================================================================
    // buildSendSyncMessageList: directives が MockMessages に設定されること（QA観点1-軽微）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: directives が MockMessages に設定されること。
     *
     * <p>
     * Given: response_body_messages の grp1 エントリに text-encoding: UTF-8 が指定されている<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[grp1]", path) を呼ぶ<br>
     * Then:  result.get(0).createLayout().getDirective("text-encoding") が "UTF-8" を返すこと
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_directivesAreSet() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "[grp1]", DIR);

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
    // fw_header: が空マップのとき例外なく空 Map が返ること（E-3 分岐D）
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
        // Given: fw_header が空マップのエントリを直接構築（`fw_header` が空マップのケース（スキーマは `minProperties:0` で空マップを許容））
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
    // fw_header: マップが存在しない場合は空 Map を FW ヘッダとして使用すること
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: fw_header: マップがない場合、
     * 空 Map を FW ヘッダとして MessagePool が返ること。
     *
     * <p>
     * Given: messages に id=bodyOnly001 の BODY レコードのみ（fw_header マップなし）<br>
     * When:  buildMessagePool(yaml, "messages", "bodyOnly001", path) を呼ぶ<br>
     * Then:  MessagePool が返り、fwHeader が空 Map であること
     * </p>
     */
    @Test
    public void buildMessagePool_noFwHeaderMapReturnsEmptyFwHeader() throws Exception {
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
    // fw_header: がマップ以外（誤記）のとき IllegalStateException + context（E-3）
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
            assertThat("id がメッセージに含まれること", e.getMessage(), containsString("id='malformed001'"));
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
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[grp2]", path) を呼ぶ<br>
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
                yaml, "response_body_messages", "[grp2]", DIR);

        // Then
        assertNotNull(result);
        assertThat("id なしエントリは 1 件返ること", result.size(), is(1));
        assertNull("id なしエントリの requestId は null であること", result.get(0).getRequestId());
    }

    // ========================================================================
    // fw_header: に一部のキーのみ記載した場合は記載分だけが設定されること
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
    // fwHeaderFields カスタム設定
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code reader.fwHeaderfields} にプロジェクト独自キーを
     * 設定した場合、そのキーを {@code fw_header:} に記載でき FW ヘッダに保持されること。
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} に customField,requestId を設定、
     *        messages に id=req001 の fw_header マップに customField/requestId を記述<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  customField と requestId の両方が FW ヘッダに含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_customFwHeaderFields() throws Exception {
        // Given: 独自キーを含む項目名を設定する
        setFwHeaderFields("customField,requestId");
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/customFwHeaderData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "req001", DIR);

        // Then: 設定した項目名のキーが保持されること
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("独自キー customField が保持されること", fwHeader.get("customField"), is("CUSTOM_VALUE"));
        assertThat("既定キー requestId も保持されること", fwHeader.get("requestId"), is("0000000001"));
    }

    // ========================================================================
    // fw_header: のキーは reader.fwHeaderfields の名前だけを許すこと
    // 出典: implementation/testdata_notation.rst:1295
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code reader.fwHeaderfields} を設定していない場合、
     * 既定 4 キー（requestId・userId・resendFlag・resultCode）以外のキーを {@code fw_header:} に
     * 書くと {@link IllegalStateException} がスローされること。
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} 未設定、messages の id=req001 の fw_header に独自キー customField<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、電文の id（req001）と不正なキー名（customField）が
     *        メッセージに含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderKeyNotInDefaultFieldsThrows() {
        // Given: reader.fwHeaderfields は設定しない（既定 4 キーが許可される）
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/customFwHeaderData");

        // When
        try {
            buildMessagePool(yaml, "messages", "req001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("電文の id がメッセージに含まれること", e.getMessage(), containsString("id='req001'"));
            assertThat("不正なキー名がメッセージに含まれること", e.getMessage(),
                    containsString("has unknown key 'customField'"));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code reader.fwHeaderfields} を設定した場合、
     * 設定した名前だけが許可され、既定 4 キーであっても設定に無ければ
     * {@link IllegalStateException} がスローされること。
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} に customField のみを設定、messages の id=req001 の
     *        fw_header に customField と requestId<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、電文の id（req001）と設定に無いキー名（requestId）が
     *        メッセージに含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderKeyNotInConfiguredFieldsThrows() {
        // Given: 既定 4 キーは設定値で置き換えられる
        setFwHeaderFields("customField");
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/customFwHeaderData");

        // When
        try {
            buildMessagePool(yaml, "messages", "req001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("電文の id がメッセージに含まれること", e.getMessage(), containsString("id='req001'"));
            assertThat("設定に無いキー名がメッセージに含まれること", e.getMessage(),
                    containsString("has unknown key 'requestId'"));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code reader.fwHeaderfields} はカンマで分割されるだけで
     * 前後の空白は取り除かれないこと（本体 {@code MessageParser} が使う
     * {@code NablarchTestUtils.makeArray} と同じ分割）。
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} に {@code "customField, requestId"}（カンマの後に空白）を設定<br>
     * When:  fw_header に customField と requestId を持つ messages の id=req001 を組み立てる<br>
     * Then:  空白付きの {@code " requestId"} が項目名になるため requestId が不正キーとなり
     *        IllegalStateException がスローされること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderFieldsAreSplitByCommaWithoutTrimming() {
        // Given: カンマの後に空白を置いた設定
        setFwHeaderFields("customField, requestId");
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/customFwHeaderData");

        // When
        try {
            buildMessagePool(yaml, "messages", "req001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("空白が取り除かれないため requestId が不正キーになること",
                    e.getMessage(), containsString("has unknown key 'requestId'"));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code reader.fwHeaderfields} を設定していない場合、
     * 例外メッセージが「何が許されるのか」＝既定 4 キーを {@code allowed keys} として辞書順で列挙すること。
     *
     * <p>
     * 例外メッセージの後半（許可されるキー名の一覧）を丸ごと固定する。既定 4 キーを
     * {@code HashSet} のまま出すと反復順は {@code [requestId, resultCode, userId, resendFlag]} であり
     * 期待値と一致しないため、{@code TreeSet} による辞書順（＝メッセージの決定性）もこの assert が守る。
     * </p>
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} 未設定、messages の id=req001 の fw_header に独自キー customField<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  {@code allowed keys (reader.fwHeaderfields): [requestId, resendFlag, resultCode, userId]}
     *        が例外メッセージに含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderErrorMessageListsDefaultAllowedKeysInSortedOrder() {
        // Given: reader.fwHeaderfields は設定しない（既定 4 キーが許可される）
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/customFwHeaderData");

        // When
        try {
            buildMessagePool(yaml, "messages", "req001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("許可されるキーが設定キー名つきで辞書順に列挙されること", e.getMessage(),
                    containsString(
                            "allowed keys (reader.fwHeaderfields): [requestId, resendFlag, resultCode, userId]"));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code reader.fwHeaderfields} を設定した場合、
     * 例外メッセージの {@code allowed keys} が設定した名前を辞書順で列挙すること。
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} に {@code "userId,customField"}（記述順は辞書順と逆）を設定、
     *        messages の id=req001 の fw_header に customField と requestId<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  設定に無い requestId が不正キーとなり、
     *        {@code allowed keys (reader.fwHeaderfields): [customField, userId]} が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderErrorMessageListsConfiguredAllowedKeysInSortedOrder() {
        // Given: 記述順を辞書順と逆にした設定
        setFwHeaderFields("userId,customField");
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/customFwHeaderData");

        // When
        try {
            buildMessagePool(yaml, "messages", "req001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("設定に無い requestId が不正キーになること",
                    e.getMessage(), containsString("has unknown key 'requestId'"));
            assertThat("設定した名前が辞書順に列挙されること",
                    e.getMessage(), containsString("allowed keys (reader.fwHeaderfields): [customField, userId]"));
        }
    }

    /**
     * [YamlMessageBuilder] {@code fw_header:} に記載できる項目名の集合が、
     * {@code reader.fwHeaderfields} をどう設定しても本体 {@code MessageParser} と一致すること。
     *
     * <p>
     * 完了条件「集合の作り方が本体と同じ（同じキー・同じ既定 4 つ・同じ {@code makeArray}）」を
     * javadoc やレビューではなくテストで守る。本体の集合は
     * {@code ../nablarch-testing/src/main/java/nablarch/test/core/reader/MessageParser.java:107}-{@code :110}
     * の private フィールド {@code fwHeaderFields} をリフレクションで読む。
     * </p>
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} に 7 通り（未設定／{@code ""}／空白のみ／カンマのみ／
     *        {@code "a,b"}／カンマの後に空白／末尾カンマ）を順に設定する<br>
     * When:  本体 {@code MessageParser} と {@link YamlMessageBuilder} の集合をそれぞれ取り出す<br>
     * Then:  7 通りすべてで両者が等しいこと
     * </p>
     */
    @Test
    public void fwHeaderFields_isIdenticalToMessageParserForEveryConfiguration() throws Exception {
        // Given: null は「未設定」（SystemRepository.getString が null を返す状態）を表す
        String[] configurations = {null, "", " ", ",", "a,b", "a, b", "requestId,"};

        for (String configuration : configurations) {
            setFwHeaderFields(configuration);

            // When
            Set<String> expected = mainFwHeaderFields();
            Set<String> actual = yamlFwHeaderFields();

            // Then
            assertThat("reader.fwHeaderfields=[" + configuration + "] のとき本体と同じ集合であること",
                    actual, is(expected));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code reader.fwHeaderfields} が空文字の場合、
     * 未設定と同じく既定 4 キーが通ること（本体 {@code MessageParser.java:107} の {@code isNullOrEmpty} ガード）。
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} に {@code ""} を設定、
     *        messages の id=req001 の fw_header に既定 4 キー<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  例外なく既定 4 キーが FW ヘッダに保持されること
     * </p>
     */
    @Test
    public void buildMessagePool_emptyFwHeaderFieldsBehavesAsUnset() throws Exception {
        // Given
        setFwHeaderFields("");
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "req001", DIR);

        // Then
        assertNotNull(result);
        Map<String, String> fwHeader = getFwHeader(result);
        assertThat("requestId が通ること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("userId が通ること", fwHeader.get("userId"), is("testUser01"));
        assertThat("resendFlag が通ること", fwHeader.get("resendFlag"), is("0"));
        assertThat("resultCode が通ること", fwHeader.get("resultCode"), is("0000"));
        assertThat("既定 4 キーだけが設定されていること", fwHeader.size(), is(4));
    }

    // ========================================================================
    // 同一ファイル内の誤記エントリが他エントリの読み出しを巻き添えにしないこと（キーの検査は遅延実行）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: 同一ファイル内に不正キーのエントリがあっても、
     * 正常エントリは読み出せること（キーの検査は読み出すエントリに対してのみ遅延実行される）。
     *
     * <p>
     * Given: mixedFwHeaderKeysData に id=badKey001（不正キー customField）と
     *        id=goodKey001（既定キーのみ）の 2 エントリ<br>
     * When:  buildMessagePool(yaml, "messages", "goodKey001", path) を呼ぶ<br>
     * Then:  例外なく MessagePool が返り、FW ヘッダが取得できること
     * </p>
     */
    @Test
    public void buildMessagePool_validEntryIsReadableThoughSameFileHasEntryWithUnknownKey() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/mixedFwHeaderKeysData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "goodKey001", DIR);

        // Then
        assertNotNull(result);
        Map<String, String> fwHeader = getFwHeader(result);
        assertThat("正常エントリの requestId が取得できること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("正常エントリの userId が取得できること", fwHeader.get("userId"), is("testUser01"));
        assertThat("正常エントリのキーだけが設定されていること", fwHeader.size(), is(2));
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: 同一ファイル内の不正キーのエントリを実際に読み出したときは
     * {@link IllegalStateException} がスローされること。
     *
     * <p>
     * Given: mixedFwHeaderKeysData に id=badKey001（不正キー customField）と
     *        id=goodKey001（既定キーのみ）の 2 エントリ<br>
     * When:  buildMessagePool(yaml, "messages", "badKey001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、不正エントリの id と不正なキー名が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_entryWithUnknownKeyThrowsWhenItIsRead() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/mixedFwHeaderKeysData");

        // When
        try {
            buildMessagePool(yaml, "messages", "badKey001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("不正エントリの id がメッセージに含まれること",
                    e.getMessage(), containsString("id='badKey001'"));
            assertThat("不正なキー名がメッセージに含まれること",
                    e.getMessage(), containsString("has unknown key 'customField'"));
        }
    }

    // ========================================================================
    // fw_header: のキーの境界値（大文字小文字違い・非文字列・null）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code fw_header:} のキーは大文字小文字を区別し、
     * 既定キーと綴りが同じでも大文字小文字が違えば {@link IllegalStateException} になること。
     *
     * <p>
     * Given: fw_header のキーが {@code requestid}／{@code REQUESTID}（既定キー {@code requestId} と
     *        大文字小文字だけが違う）<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  どちらも IllegalStateException がスローされ、書いたとおりのキー名が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderKeyIsCaseSensitive() {
        for (String key : new String[]{"requestid", "REQUESTID"}) {
            // Given
            Map<String, Object> fwHeader = new LinkedHashMap<>();
            fwHeader.put(key, "0000000001");
            Map<String, Object> yaml = yamlWithFwHeader("caseKey001", fwHeader);

            // When
            try {
                buildMessagePool(yaml, "messages", "caseKey001", DIR);
                fail("IllegalStateException が期待される（キー: " + key + "）");
            } catch (IllegalStateException e) {
                // Then
                assertThat("大文字小文字が違うキーは不正キーになること",
                        e.getMessage(), containsString("has unknown key '" + key + "'"));
                assertThat("電文の id がメッセージに含まれること",
                        e.getMessage(), containsString("id='caseKey001'"));
            }
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code fw_header:} のキーが文字列でない場合
     * （数値・真偽値）、文字列化したキー名を含む {@link IllegalStateException} になること。
     *
     * <p>
     * Given: fw_header のキーが数値 {@code 1234}／真偽値 {@code true}<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  どちらも IllegalStateException がスローされ、文字列化したキー名
     *        （{@code '1234'}／{@code 'true'}）が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderNonStringKeyThrowsWithStringifiedKeyName() {
        Object[][] cases = {{Integer.valueOf(1234), "1234"}, {Boolean.TRUE, "true"}};
        for (Object[] c : cases) {
            // Given
            Map<Object, Object> fwHeader = new LinkedHashMap<>();
            fwHeader.put(c[0], "VALUE");
            Map<String, Object> yaml = yamlWithFwHeader("nonStringKey001", fwHeader);

            // When
            try {
                buildMessagePool(yaml, "messages", "nonStringKey001", DIR);
                fail("IllegalStateException が期待される（キー: " + c[0] + "）");
            } catch (IllegalStateException e) {
                // Then
                assertThat("文字列化したキー名がメッセージに含まれること",
                        e.getMessage(), containsString("has unknown key '" + c[1] + "'"));
                assertThat("電文の id がメッセージに含まれること",
                        e.getMessage(), containsString("id='nonStringKey001'"));
            }
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code fw_header:} のキーが null（YAML の {@code ~}）の場合、
     * {@link NullPointerException} ではなく {@code unknown key 'null'} を含む
     * {@link IllegalStateException} になること。
     *
     * <p>
     * Given: fw_header のキーが null<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、{@code has unknown key 'null'} が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderNullKeyThrowsIllegalStateExceptionNotNpe() {
        // Given
        Map<Object, Object> fwHeader = new LinkedHashMap<>();
        fwHeader.put(null, "VALUE");
        Map<String, Object> yaml = yamlWithFwHeader("nullKey001", fwHeader);

        // When
        try {
            buildMessagePool(yaml, "messages", "nullKey001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("NPE ではなく unknown key 'null' を含む例外になること",
                    e.getMessage(), containsString("has unknown key 'null'"));
            assertThat("電文の id がメッセージに含まれること",
                    e.getMessage(), containsString("id='nullKey001'"));
        }
    }

    // ========================================================================
    // T2: fw_header マップ対応（ランタイム、messages 限定）
    // ========================================================================

    /**
     * [MS-04] {@code reader.fwHeaderfields} を設定しない場合、messages の fw_header: マップの
     * 既定 4 キーが getFwHeader() に保持されること。
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} 未設定、messages に id=req001 のエントリが fw_header: マップ
     *        （requestId/userId/resendFlag/resultCode）を持つ YAML<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader に既定 4 キーが保持されること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderMapAllDefaultKeysRetained() throws Exception {
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
        assertThat("既定 4 キーだけが設定されていること", fwHeader.size(), is(4));
    }

    /**
     * [MS-04] records 側にヘッダ相当のレコードがなくても fw_header: マップから FW ヘッダが取得できること。
     *
     * <p>
     * Given: messages エントリに fw_header: マップがあり records には本文レコードのみの YAML<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  fwHeader に requestId が設定されていること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderMapReadableWithoutHeaderRecord() throws Exception {
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
        assertThat("records にヘッダ相当のレコードがなくても fw_header マップから取得できること",
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
     * [MS-04] {@code reader.fwHeaderfields} に既定キーと独自キーを混ぜて設定した場合、
     * 数値・真偽値に見える値がその文字列のまま FW ヘッダに保持されること。
     *
     * <p>
     * フィクスチャ {@code fwHeaderMapData.yaml:38}-{@code :40} の値は
     * {@code "0"}・{@code "1234"}・{@code "true"} と<b>クォート済み</b>である
     * （スキーマ {@code $defs.fw_header} の {@code additionalProperties.type: "string"} が
     * クォートなしの数値・真偽値をロード時に弾くため、クォートなしでは書けない）。
     * したがって本テストが押さえるのは、YAML が数値・真偽値として解釈しかねない見た目の値でも
     * 文字列のまま素通しされること、および設定した項目名（既定キー 2 つ＋独自キー 1 つ）が通ることである。
     * </p>
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} に resendFlag,resultCode,boolFlag を設定、
     *        messages に id=numericValues001 の fw_header にクォート済みの "0"・"1234"・"true" を記述<br>
     * When:  buildMessagePool(yaml, "messages", "numericValues001", path) を呼ぶ<br>
     * Then:  fwHeader の各値が記述どおりの文字列であること（"0", "1234", "true"）
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderMapKeepsQuotedNumericAndBooleanLikeValuesAsStrings() throws Exception {
        // Given: 独自キー boolFlag を項目名に含める
        setFwHeaderFields("resendFlag,resultCode,boolFlag");
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        MessagePool result = buildMessagePool(yaml, "messages", "numericValues001", DIR);

        // Then
        assertNotNull(result);
        Field fwHeaderField = MessagePool.class.getDeclaredField("fwHeader");
        fwHeaderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> fwHeader = (Map<String, String>) fwHeaderField.get(result);
        assertThat("整数に見える値が文字列のまま保持されること", fwHeader.get("resendFlag"), is("0"));
        assertThat("4桁整数に見える値が文字列のまま保持されること", fwHeader.get("resultCode"), is("1234"));
        assertThat("真偽値に見える値が文字列のまま保持されること", fwHeader.get("boolFlag"), is("true"));
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
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[noLengthGrp]", path) を呼ぶ<br>
     * Then:  NullPointerException が発生せず、RequestTestingMessagePool が 1 件返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_fieldWithoutLengthDoesNotThrowNpe() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: length なしフィールドの MockMessages を buildSendSyncMessageList で構築する
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "[noLengthGrp]", DIR);

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
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[mixedLengthGrp]", path) を呼ぶ<br>
     * Then:  NullPointerException も NumberFormatException も発生せず、RequestTestingMessagePool が 1 件返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_partialLengthFieldDoesNotThrowException() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: 一部 length あり・一部なしの MockMessages を buildSendSyncMessageList で構築する
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "[mixedLengthGrp]", DIR);

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
    // buildSendSyncList: null / 角括弧付き / 不完全な括弧の分岐カバレッジ
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: groupId に null を渡したとき null が返ること。
     *
     * <p>
     * null を渡すと全エントリが groupMatches で不一致となり null が返る。<br>
     * Given: response_body_messages にエントリが存在する<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", null, path) を呼ぶ<br>
     * Then:  null が返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_nullGroupIdReturnsNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: groupId=null → groupMatches で全エントリが不一致 → null
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", null, DIR);

        // Then
        assertNull("groupId が null のとき null が返ること", result);
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: groupId に "[grp1]" を渡したとき groupMatches が YAML の group_id=grp1 にマッチすること。
     *
     * <p>
     * groupMatches("grp1", "[grp1]") → "[grp1]".equals("[grp1]") = true でマッチする。<br>
     * Given: response_body_messages に group_id=grp1 のエントリが定義されている<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[grp1]", path) を呼ぶ<br>
     * Then:  RequestTestingMessagePool が 1 件返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_bracketGroupIdStripped() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: "[grp1]" → groupMatches("grp1", "[grp1]") = true → YAML の group_id=grp1 にマッチ
        List<RequestTestingMessagePool> result = buildSendSyncMessageList(
                yaml, "response_body_messages", "[grp1]", DIR);

        // Then
        assertNotNull("角括弧付き groupId でも正しくマッチして結果が返ること", result);
        assertThat("1 件返ること", result.size(), is(1));
    }

    /**
     * [YamlMessageBuilder] buildSendSyncMessageList: groupId が "[" で始まるが "]" で終わらない場合は groupMatches で不一致となり null が返ること。
     *
     * <p>
     * groupMatches("grp1", "[grp1") → "[grp1]".equals("[grp1") = false で不一致になる。<br>
     * Given: response_body_messages に group_id=grp1 のエントリが定義されている<br>
     * When:  buildSendSyncMessageList(yaml, "response_body_messages", "[grp1", path) を呼ぶ<br>
     * Then:  "[grp1" はどのエントリともマッチしないため null が返ること
     * </p>
     */
    @Test
    public void buildSendSyncMessageList_partialBracketGroupIdReturnsNull() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When: "[grp1" → groupMatches("grp1", "[grp1") = "[grp1]".equals("[grp1") = false → 不一致
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

    /**
     * [YamlMessageBuilder] 電文用のインタープリタリストが解説書の定める 1 つだけであること。
     *
     * <p>
     * null・空文字・ダブルクォート・改行文字は YAML のパーサが構文として解釈するため、Excel 形式で
     * 必要な NullInterpreter・QuotationTrimmer は指定しない。<br>
     * Given: unit-test-yaml.xml（経由 unit-test.xml）が定義する yamlMessagingInterpreters<br>
     * When:  リポジトリから yamlMessagingInterpreters を取得する<br>
     * Then:  CompositeInterpreter の 1 件だけであること
     * </p>
     */
    @Test
    public void yamlMessagingInterpretersIsOnlyDocumentedOne() {
        // Given / When
        List<TestDataInterpreter> messagingInterpreters =
                repositoryResource.getComponent("yamlMessagingInterpreters");

        // Then
        assertNotNull("yamlMessagingInterpreters コンポーネントが定義されていること", messagingInterpreters);
        assertThat("yamlMessagingInterpreters は 1 件だけであること", messagingInterpreters.size(), is(1));
        assertThat("1 件目は CompositeInterpreter であること", messagingInterpreters.get(0),
                is(instanceOf(nablarch.test.core.util.interpreter.CompositeInterpreter.class)));
    }
}
