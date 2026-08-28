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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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

    /**
     * 本体 {@code MessageParser} をリフレクションで覗く前提が崩れたときの {@code fail} メッセージ。
     *
     * <p>素の {@code NoSuchFieldException}／{@code NullPointerException} では原因が読み取れないため、
     * 「上流が変わった」ことと次にすべきことを明示する。</p>
     */
    private static final String UPSTREAM_CHANGED =
            "本体 ../nablarch-testing/src/main/java/nablarch/test/core/reader/MessageParser.java:107-:110 の"
                    + "集合生成が変わった可能性がある。YamlMessageBuilder.fwHeaderFields() を本体に合わせ直すこと。";

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
     * <p>
     * この 2 つの前提（private フィールド名 {@code fwHeaderFields} が在ること・コンストラクタが null を
     * 受け付けること）が崩れると素の {@link NoSuchFieldException}／{@link NullPointerException} で落ち、
     * 「上流の本体実装が変わった」ことが読み取れない。どちらも捕まえて {@link #UPSTREAM_CHANGED} を
     * message に持つ {@link AssertionError} を投げ、次に何をすべきかを出す。捕まえた例外は
     * cause として渡す（{@code fail} は cause を取らないため、とくに NPE では
     * 「本体のどこで落ちたか」というスタックトレースが失われる）。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private static Set<String> mainFwHeaderFields() throws Exception {
        MessageParser parser;
        try {
            parser = new MessageParser(null, null, DataType.MESSAGE);
        } catch (NullPointerException e) {
            throw new AssertionError(
                    UPSTREAM_CHANGED + " （reader・interpreters に null を渡せなくなった）", e);
        }
        Field f;
        try {
            f = MessageParser.class.getDeclaredField("fwHeaderFields");
        } catch (NoSuchFieldException e) {
            throw new AssertionError(
                    UPSTREAM_CHANGED + " （private フィールド fwHeaderFields が見つからない）", e);
        }
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

    /** 集合が不変（{@code add}・{@code remove} が {@link UnsupportedOperationException}）であることを検証するヘルパー。 */
    private static void assertUnmodifiable(String label, Set<String> actual) {
        try {
            actual.add("zzzInjectedByTest");
            fail(label + ": 戻り値が不変でない（add できてしまった）");
        } catch (UnsupportedOperationException expected) {
            // 期待どおり
        }
        try {
            actual.remove("requestId");
            fail(label + ": 戻り値が不変でない（remove できてしまった）");
        } catch (UnsupportedOperationException expected) {
            // 期待どおり
        }
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
     * [YamlMessageBuilder] buildMessagePool: fw_header: の値がマップでない場合（リスト・文字列・数値）、
     * IllegalStateException がスローされ、id と<b>実際の型名</b>がメッセージに含まれること（E-3）。
     *
     * <p>
     * リストだけでなくスカラ（文字列・数値）も回すのは、実装のガードが {@code !(x instanceof Map)} であって
     * {@code x instanceof List} ではないことを押さえるためである。型名まで assert するのは、
     * メッセージ後半 {@code but was: <型名>} が誤記の原因究明に効く部分だからである。
     * </p>
     * <p>
     * どのケースも<b>実 YAML からは到達しない</b>。スキーマ {@code $defs.fw_header} が
     * {@code "type": "object"} を課すため、マップ以外の {@code fw_header:} は
     * {@code YamlLoader.load} がロード時に {@code YamlSchemaValidationException} で弾く
     * （隔離コピーで {@code fw_header: "NOT_A_MAP"} を実測して確認した）。よってスキーマ検証を通さない
     * 合成 Map（{@link #yamlWithFwHeader}）で {@code convertFwHeader} の防御分岐だけを押さえる。
     * </p>
     *
     * <p>
     * Given: id=malformed001 の fw_header がリスト／文字列／数値（いずれも誤記）<br>
     * When:  buildMessagePool(yaml, "messages", "malformed001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、{@code id='malformed001'} と
     *        {@code must be a map, but was: ArrayList／String／Integer} が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_nonMapFwHeaderThrowsExceptionWithTypeName() {
        // Given: マップ以外の fw_header と、期待される型名（Class#getSimpleName）
        Map<String, Object> requestIdMap = new LinkedHashMap<>();
        requestIdMap.put("requestId", "0000000001");
        Object[][] cases = {
                {new ArrayList<Object>(Arrays.<Object>asList(requestIdMap)), "ArrayList"},
                {"requestId=0000000001", "String"},
                {Integer.valueOf(1234), "Integer"},
        };

        for (Object[] c : cases) {
            Map<String, Object> yaml = yamlWithFwHeader("malformed001", c[0]);

            // When
            try {
                buildMessagePool(yaml, "messages", "malformed001", DIR);
                fail("IllegalStateException が期待される（fw_header: " + c[0] + "）");
            } catch (IllegalStateException e) {
                // Then
                assertThat("id がメッセージに含まれること", e.getMessage(),
                        containsString("id='malformed001'"));
                assertThat("実際の型名がメッセージに含まれること", e.getMessage(),
                        containsString("must be a map, but was: " + c[1]));
            }
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
    // 出典: nablarch-document の implementation/testdata_notation.rst
    //       「メッセージングのデータを記述する」節の「YAML形式の場合」項
    //       「fw_header: に記載できるキーは、reader.fwHeaderfields の名前
    //         （省略時は requestId・userId・resendFlag・resultCode）だけである。それ以外のキーがあるとエラーになる。」
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code reader.fwHeaderfields} を設定していない場合、
     * 既定 4 キー（requestId・userId・resendFlag・resultCode）以外のキーを {@code fw_header:} に
     * 書くと {@link IllegalStateException} がスローされること。
     *
     * <p>
     * 例外メッセージ後半（何が許されるのか）も同時に丸ごと assert する。既定 4 キーを {@code HashSet} の
     * まま出すと反復順は {@code [requestId, resultCode, userId, resendFlag]} であり期待値と一致しないため、
     * この assert は {@code TreeSet} による辞書順（＝メッセージの決定性）と、各名前のクォート
     * （{@code YamlMessageBuilder.formatAllowedFields}）も同時に守る。
     * </p>
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} 未設定、messages の id=req001 の fw_header に独自キー customField<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、電文の id（req001）・不正なキー名（customField）・
     *        {@code allowed keys (reader.fwHeaderfields): ['requestId', 'resendFlag', 'resultCode', 'userId']}
     *        がメッセージに含まれること
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
            assertThat("許可されるキーが設定キー名つきで辞書順・クォート付きに列挙されること", e.getMessage(),
                    containsString("allowed keys (reader.fwHeaderfields): "
                            + "['requestId', 'resendFlag', 'resultCode', 'userId']"));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code reader.fwHeaderfields} を設定した場合、
     * 設定した名前だけが許可され、既定 4 キーであっても設定に無ければ
     * {@link IllegalStateException} がスローされること。
     *
     * <p>
     * 例外メッセージ後半の {@code allowed keys} も同時に assert する。設定値を記述順が辞書順と逆になる
     * 2 要素（{@code "userId,customField"}）にしてあるため、この assert は「設定した名前が出ること」に加えて
     * {@code TreeSet} による辞書順（＝メッセージの決定性）と各名前のクォートも守る（1 要素では順序が現れない）。
     * </p>
     * <p>
     * 2 要素目の {@code userId} は既定 4 キーの 1 つだが、<b>このテストは {@code userId} が通ることを
     * 押さえていない</b>。フィクスチャ {@code customFwHeaderData.yaml} の {@code fw_header:} は
     * {@code customField} と {@code requestId} の 2 キーだけで {@code userId} を持たず、
     * {@code convertFwHeader} は最初に見つけた不許可キー（{@code requestId}）でそのまま throw するため、
     * {@code userId} は一度も評価されない。設定に載っている既定キーが通ることを押さえているのは
     * {@link #buildMessagePool_customFwHeaderFields}（設定 {@code "customField,requestId"} で既定キー
     * {@code requestId} が FW ヘッダに保持されることを assert する）である。
     * </p>
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} に {@code "userId,customField"} を設定、messages の id=req001 の
     *        fw_header に customField と requestId<br>
     * When:  buildMessagePool(yaml, "messages", "req001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、電文の id（req001）・設定に無いキー名（requestId）・
     *        {@code allowed keys (reader.fwHeaderfields): ['customField', 'userId']} が
     *        メッセージに含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderKeyNotInConfiguredFieldsThrows() {
        // Given: 既定 4 キーは設定値で置き換えられる（記述順は辞書順と逆にしてある）
        setFwHeaderFields("userId,customField");
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
            assertThat("設定した名前が辞書順・クォート付きに列挙されること", e.getMessage(),
                    containsString("allowed keys (reader.fwHeaderfields): ['customField', 'userId']"));
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
     *        IllegalStateException がスローされ、{@code allowed keys} が
     *        {@code [' requestId', 'customField']} と各名前をクォートして列挙されること
     *        （{@code YamlMessageBuilder.formatAllowedFields} の存在理由＝空白入りの項目名を
     *        見分けられるようにすること、をここで押さえる）
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
            // formatAllowedFields の存在理由（空白入りの項目名を見分けられるようにする）をここで押さえる。
            // クォートが無ければ Set#toString() の [ requestId, customField] となり、名前の先頭の空白が
            // 括弧・区切りの空白と地続きで見分けにくい。
            assertThat("空白を含む項目名がクォートで見分けられる形で列挙されること", e.getMessage(),
                    containsString("allowed keys (reader.fwHeaderfields): [' requestId', 'customField']"));
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
     * Given: {@code reader.fwHeaderfields} に 8 通り（未設定／{@code ""}／空白のみ／カンマのみ／
     *        {@code "a,b"}／カンマの後に空白／末尾カンマ／<b>先頭カンマ</b>）を順に設定する<br>
     * When:  本体 {@code MessageParser} と {@link YamlMessageBuilder} の集合をそれぞれ取り出す<br>
     * Then:  8 通りすべてで両者が等しいこと
     * </p>
     * <p>
     * 先頭カンマ（{@code ",requestId"}）は {@code Pattern.compile(",").split()} が先頭の空要素を残すため
     * 集合が {@code ["", "requestId"]} になり、{@code fw_header: {"": "V"}} が例外にならず素通りする。
     * これは本体と同じ挙動である（本体 {@code MessageParser} の private {@code isFrameworkHeader("")} を
     * 同じ設定でリフレクション実行して {@code true} を確認済み）。仕様として固定するのは
     * 「集合が本体と一致すること」であり、素通りはその帰結なので個別のテストは置かない。
     * </p>
     * <p>
     * テスト名で網羅（Every）を謳わないのは、ここで回すのが列挙した 8 通りだけだからである。
     * </p>
     */
    @Test
    public void fwHeaderFields_isIdenticalToMessageParserForListedConfigurations() throws Exception {
        // Given: null は「未設定」（SystemRepository.getString が null を返す状態）を表す
        String[] configurations = {null, "", " ", ",", "a,b", "a, b", "requestId,", ",requestId"};

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
     * [YamlMessageBuilder] {@code fwHeaderFields()} の戻り値が不変であること
     * （{@code reader.fwHeaderfields} 未設定・設定ありの<b>両分岐</b>）。
     *
     * <p>
     * 未設定分岐は {@code static final} の {@code DEFAULT_FW_HEADER_FIELDS} をそのまま返すため、
     * 呼び出し側が書き換えると以後すべての電文の許可集合が壊れる。設定あり分岐も契約を揃えて不変にしてある。
     * <b>このテストが無ければ</b>、{@code Collections.unmodifiableSet(...)} を両分岐から外しても
     * 他のどのテストも落ちない（隔離コピーでこのテストを {@code @Ignore} にして外し、
     * {@code mvn -o clean test} が {@code Tests run: 291, Failures: 0, Errors: 0, Skipped: 2} /
     * BUILD SUCCESS になることを実測した）。逆にこのテストが有効なままだと、
     * ここでの {@code add} が共有の既定集合を汚染する影響まで含めて {@code Failures: 3} になる。
     * よってここで直接 assert する。
     * </p>
     *
     * <p>
     * Given: {@code reader.fwHeaderfields} が空文字（未設定と同じ）／{@code "customField,requestId"}<br>
     * When:  {@code fwHeaderFields()} の戻り値に {@code add}／{@code remove} を試みる<br>
     * Then:  どちらの分岐でも {@link UnsupportedOperationException} になること
     * </p>
     */
    @Test
    public void fwHeaderFields_returnsUnmodifiableSetInBothBranches() throws Exception {
        // Given: 未設定（空文字）分岐
        setFwHeaderFields("");

        // When / Then
        assertUnmodifiable("reader.fwHeaderfields 未設定（既定 4 キー）", yamlFwHeaderFields());

        // Given: 設定あり分岐
        setFwHeaderFields("customField,requestId");

        // When / Then
        assertUnmodifiable("reader.fwHeaderfields 設定あり", yamlFwHeaderFields());
    }

    /**
     * [YamlMessageBuilder][MS-04] buildMessagePool: {@code reader.fwHeaderfields} が空文字の場合、
     * 未設定と同じく既定 4 キーが通ること（本体 {@code MessageParser.java:108} の {@code isNullOrEmpty} ガード）。
     *
     * <p>
     * 「messages の {@code fw_header:} マップの既定 4 キーが getFwHeader() に保持されること」（MS-04）も
     * このテストが押さえる。{@code reader.fwHeaderfields} は {@code src/test/resources} のどの設定にも
     * 定義が無いため（{@code grep -rn fwHeaderfields src/test/resources} が 0 件）、真に未設定
     * （{@code SystemRepository.getString} が {@code null}）の状態で走るのは<b>このクラスで最初に走る 1 件だけ</b>で、
     * それ以降は {@code @After}（{@link #after()}）が毎回 {@code ""} に戻した状態で走る
     * （各テストの直前に値を出力して実測した）。同じフィクスチャ・同じ id・同じ assert のテストを
     * 別に置いても押さえるものが増えないため 1 件にまとめてある。
     * </p>
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
     * 不正エントリを実際に読み出したときは例外になることも同じテストで押さえる。読み出したときだけ落ちる
     * ／読み出さなければ落ちない、という 1 つの性質の表裏であり、別テストに分けても押さえるものが増えない。
     * </p>
     *
     * <p>
     * Given: mixedFwHeaderKeysData に id=badKey001（不正キー customField）・id=goodKey001（既定キーのみ）を
     *        含む 7 エントリ（他は境界値用の不正キーのエントリ）<br>
     * When:  buildMessagePool(yaml, "messages", "goodKey001", path) と
     *        buildMessagePool(yaml, "messages", "badKey001", path) をそれぞれ呼ぶ<br>
     * Then:  goodKey001 は例外なく FW ヘッダが取得でき、badKey001 は IllegalStateException がスローされ
     *        不正エントリの id と不正なキー名が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_unknownKeyIsCheckedOnlyForTheEntryBeingRead() throws Exception {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/mixedFwHeaderKeysData");

        // When: 正常エントリを読み出す
        MessagePool result = buildMessagePool(yaml, "messages", "goodKey001", DIR);

        // Then: 同一ファイルの不正エントリに巻き添えにされないこと
        assertNotNull(result);
        Map<String, String> fwHeader = getFwHeader(result);
        assertThat("正常エントリの requestId が取得できること", fwHeader.get("requestId"), is("0000000001"));
        assertThat("正常エントリの userId が取得できること", fwHeader.get("userId"), is("testUser01"));
        assertThat("正常エントリのキーだけが設定されていること", fwHeader.size(), is(2));

        // When: 不正エントリを読み出す
        try {
            buildMessagePool(yaml, "messages", "badKey001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then: 読み出したときは落ちること
            assertThat("不正エントリの id がメッセージに含まれること",
                    e.getMessage(), containsString("id='badKey001'"));
            assertThat("不正なキー名がメッセージに含まれること",
                    e.getMessage(), containsString("has unknown key 'customField'"));
        }
    }

    // ========================================================================
    // fw_header: のキーの境界値（大文字小文字違い・~・非文字列・null）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code fw_header:} のキーは大文字小文字を区別し、
     * 既定キーと綴りが同じでも大文字小文字が違えば {@link IllegalStateException} になること。
     *
     * <p>
     * 合成 Map ではなく実フィクスチャ（{@code mixedFwHeaderKeysData.yaml} の id=lowerCaseKey001・
     * id=upperCaseKey001）を通す。スキーマ {@code $defs.fw_header} はキー名を制限しないため
     * 大文字小文字違いのキーはロードを通り、{@link String} のまま {@code convertFwHeader} に届く
     * （{@link YamlLoader#load} 後のキーの型が {@link String} であることを隔離コピーで実測して確認した）。
     * </p>
     *
     * <p>
     * Given: mixedFwHeaderKeysData の id=lowerCaseKey001 が fw_header のキーに {@code requestid}、
     *        id=upperCaseKey001 が {@code REQUESTID}（既定キー {@code requestId} と大文字小文字だけが違う）<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  どちらも IllegalStateException がスローされ、書いたとおりのキー名が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderKeyIsCaseSensitive() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/mixedFwHeaderKeysData");
        String[][] cases = {{"lowerCaseKey001", "requestid"}, {"upperCaseKey001", "REQUESTID"}};

        for (String[] c : cases) {
            // When
            try {
                buildMessagePool(yaml, "messages", c[0], DIR);
                fail("IllegalStateException が期待される（キー: " + c[1] + "）");
            } catch (IllegalStateException e) {
                // Then
                assertThat("大文字小文字が違うキーは不正キーになること",
                        e.getMessage(), containsString("has unknown key '" + c[1] + "'"));
                assertThat("電文の id がメッセージに含まれること",
                        e.getMessage(), containsString("id='" + c[0] + "'"));
            }
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: 実 YAML に {@code ~} をキーとして書いた場合、
     * それは <b>null キーではなく文字列 {@code "~"}</b> として読み込まれ、不正キーとして
     * {@link IllegalStateException} になること。
     *
     * <p>
     * 合成 Map ではなく実フィクスチャ（{@code mixedFwHeaderKeysData.yaml} の id=tildeKey001）を通す。
     * {@code ~} は YAML では null スカラだが、マップのキーに書いても snakeyaml-engine が
     * 文字列 {@code "~"} として渡す（{@link YamlLoader#load} 後のキーの型が {@link String} であることを
     * 隔離コピーで実測して確認した）。真の Java {@code null} キーは実 YAML からは到達しない
     * （後述 {@code buildMessagePool_fwHeaderNullKeyIsRejectedInDefensiveBranch} の javadoc 参照）。
     * </p>
     *
     * <p>
     * Given: mixedFwHeaderKeysData の id=tildeKey001 が fw_header のキーに {@code ~} を持つ<br>
     * When:  buildMessagePool(yaml, "messages", "tildeKey001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、{@code has unknown key '~'} が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderTildeKeyIsReadAsStringAndRejected() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/mixedFwHeaderKeysData");

        // When
        try {
            buildMessagePool(yaml, "messages", "tildeKey001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("~ は文字列 \"~\" として不正キーになること",
                    e.getMessage(), containsString("has unknown key '~'"));
            assertThat("電文の id がメッセージに含まれること",
                    e.getMessage(), containsString("id='tildeKey001'"));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code fw_header:} のキーが文字列でない場合
     * （数値・真偽値）、文字列化したキー名を含む {@link IllegalStateException} になること。
     *
     * <p>
     * 合成 Map ではなく実フィクスチャ（{@code mixedFwHeaderKeysData.yaml} の id=numericKey001・
     * id=booleanKey001）を通す。クォートなしの数値・真偽値をキーに書いても、
     * {@code YamlLoader.java} の {@code OBJECT_MAPPER.valueToTree} が JSON 化の際にキーを文字列にするため
     * スキーマ {@code $defs.fw_header} を通過し、{@code convertFwHeader} には
     * {@link Integer}／{@link Boolean} のまま届く（{@link YamlLoader#load} 後のキーの型が
     * {@code java.lang.Integer}／{@code java.lang.Boolean} であることを隔離コピーで実測して確認した）。
     * </p>
     *
     * <p>
     * Given: mixedFwHeaderKeysData の id=numericKey001 が fw_header のキーに数値 {@code 1234}、
     *        id=booleanKey001 が真偽値 {@code true}<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  どちらも IllegalStateException がスローされ、文字列化したキー名
     *        （{@code '1234'}／{@code 'true'}）が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderNonStringKeyThrowsWithStringifiedKeyName() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/mixedFwHeaderKeysData");
        String[][] cases = {{"numericKey001", "1234"}, {"booleanKey001", "true"}};

        for (String[] c : cases) {
            // When
            try {
                buildMessagePool(yaml, "messages", c[0], DIR);
                fail("IllegalStateException が期待される（id: " + c[0] + "）");
            } catch (IllegalStateException e) {
                // Then
                assertThat("文字列化したキー名がメッセージに含まれること",
                        e.getMessage(), containsString("has unknown key '" + c[1] + "'"));
                assertThat("電文の id がメッセージに含まれること",
                        e.getMessage(), containsString("id='" + c[0] + "'"));
            }
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: {@code fw_header:} のキーが Java の {@code null} の場合、
     * 許可集合に無いキーとして {@code unknown key 'null'} を含む {@link IllegalStateException} に
     * なること（防御分岐）。
     *
     * <p>
     * {@code YamlSection.objectToString} は {@code YamlSection.toStr} へ委譲し、{@code toStr} は
     * {@code value != null ? value.toString() : null} なので <b>Java の {@code null} を返す</b>
     * （文字列 {@code "null"} にはしない）。{@code allowedFields.contains(null)} が {@code false} になって
     * 例外に入り、メッセージに現れる {@code null} という文字列は
     * {@code "... has unknown key '" + key + "'..."} という文字列連結が作っている。
     * </p>
     *
     * <p>
     * <b>この分岐は実 YAML からは到達しない。</b>隔離コピーでの実測は次のとおり。
     * </p>
     * <ul>
     * <li>{@code ~: "V"} と書いたキーは Java の {@code null} ではなく文字列 {@code "~"} になる
     *     （{@code buildMessagePool_fwHeaderTildeKeyIsReadAsStringAndRejected} が実フィクスチャで押さえている）</li>
     * <li>{@code null: "V"} と書くとキーは真の Java {@code null} になるが、{@code convertFwHeader} には
     *     届かない。{@code YamlLoader.java:151} の {@code OBJECT_MAPPER.valueToTree} が
     *     {@code IllegalArgumentException: Null key for a Map not allowed in JSON} を投げ、
     *     スキーマ検証の手前でファイル全体のロードが失敗する</li>
     * </ul>
     * <p>
     * したがってこのテストは、スキーマ検証を通さない合成 Map（{@link #yamlWithFwHeader}）で
     * {@code convertFwHeader} の防御分岐だけを押さえる。実 YAML から到達しない以上「NPE でないこと」を
     * 名前で謳う意味はないため、テスト名は起きること（{@code null} キーが不正キーとして弾かれること）で
     * 名づけている。
     * </p>
     *
     * <p>
     * Given: fw_header のキーが Java の null（合成 Map。スキーマ検証は通していない）<br>
     * When:  buildMessagePool を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、{@code has unknown key 'null'} が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_fwHeaderNullKeyIsRejectedInDefensiveBranch() {
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
            assertThat("null キーは許可集合に無いため不正キーになり、文字列連結で unknown key 'null' と出ること",
                    e.getMessage(), containsString("has unknown key 'null'"));
            assertThat("電文の id がメッセージに含まれること",
                    e.getMessage(), containsString("id='nullKey001'"));
        }
    }

    // ========================================================================
    // T2: fw_header マップ対応（ランタイム、messages 限定）
    // ========================================================================

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
     * [YamlMessageBuilder] {@code fw_header:} のキー検査が {@code messages} 経路（{@code useFwHeader=true}）
     * 限定であること。非 {@code messages} 経路では不正キーがあっても検査されず空 Map になること。
     *
     * <p>
     * 実装は {@code YamlMessageBuilder.buildMessageContent} の {@code useFwHeader ? convertFwHeader(...) :
     * Collections.emptyMap()} と、{@code buildSendSyncList} が常に {@code Collections.emptyMap()} を渡すこと。
     * このガードを外して {@code useFwHeader=false} でも {@code convertFwHeader} を通すようにしても
     * 他のどのテストも落ちない（隔離コピーで実測）ため、ここで直接押さえる。
     * </p>
     * <p>
     * 実 YAML では書けない形である。スキーマの {@code $defs.expected_request_message_data}・
     * {@code $defs.group_message_data} は {@code additionalProperties: false} で {@code fw_header} を
     * プロパティに持たないため、{@code expected_request_*_messages}／{@code response_*_messages} に
     * {@code fw_header:} を書くと {@code YamlLoader.load} がロード時に
     * {@code プロパティ 'fw_header' がスキーマで定義されておらず…} で弾く（隔離コピーで実測して確認した）。
     * そのため合成 Map（{@link #yamlWithFwHeader}）を使い、{@code useFwHeader} 引数と
     * {@code buildSendSyncList} の経路そのものを直接呼ぶ。
     * </p>
     *
     * <p>
     * Given: 既定 4 キーにも無い customField を持つ fw_header の合成 Map<br>
     * When:  {@code buildMessagePool(..., useFwHeader=false, ...)} と {@code buildSendSyncList(...)} を呼ぶ<br>
     * Then:  どちらも例外にならず、FW 制御ヘッダが空 Map であること
     * </p>
     */
    @Test
    public void fwHeaderIsNotCheckedOutsideMessagesPathAndBecomesEmptyMap() throws Exception {
        // Given: messages 経路なら例外になる不正キー
        Map<String, Object> fwHeader = new LinkedHashMap<>();
        fwHeader.put("customField", "CUSTOM_VALUE");
        Map<String, Object> yaml = yamlWithFwHeader("nonMessages001", fwHeader);

        // When: useFwHeader=false（expected_*／response_* 経路が渡す値）
        MessagePool pool = builder.buildMessagePool(yaml, "messages", "nonMessages001", false, DIR);

        // Then
        assertNotNull(pool);
        assertThat("useFwHeader=false では検査されず空 Map になること", getFwHeader(pool).size(), is(0));

        // When: buildSendSyncList 経路（常に空 Map を渡す）
        List<RequestTestingMessagePool> list = buildSendSyncMessageList(yaml, "messages", "", DIR);

        // Then
        assertNotNull(list);
        assertThat("グループ ID 無指定のエントリが 1 件収集されること", list.size(), is(1));
        assertThat("buildSendSyncList では検査されず空 Map になること",
                getFwHeader(list.get(0)).size(), is(0));
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
     * フィクスチャ {@code fwHeaderMapData.yaml:41}-{@code :43} の値は
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
    // バックスラッシュと r の 2 文字を含む値がエラーになること（2-5）
    // ========================================================================

    /**
     * [YamlMessageBuilder] buildMessagePool: FW 制御ヘッダの値にバックスラッシュと r の 2 文字を書くと
     * エラーになること（2-5）。
     *
     * <p>
     * 解説書 {@code implementation/testdata_notation.rst} の
     * 「null・空文字・改行など特殊な値を記述する」節の「YAML形式の場合」項:
     * 「バックスラッシュと {@code r} の2文字（{@code "\\r"}）を含む値は書けない。Excel 形式ではこの2文字が必ず
     * CR に変換されるため、この2文字を含む値はテスティングフレームワークの仕様上存在せず、
     * YAML 形式ではエラーになる。」
     * {@code fw_header:} の値は解釈（interpret）を通らないため、{@code convertFwHeader} が検査を直接呼ぶ<br>
     * Given: messages の literalCrInFwHeaderValue001 の fw_header に requestId: "\\r"（2 文字）<br>
     * When:  buildMessagePool(yaml, "messages", "literalCrInFwHeaderValue001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージに値と出所（セクション・id）が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_literalBackslashRInFwHeaderValueThrows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        try {
            buildMessagePool(yaml, "messages", "literalCrInFwHeaderValue001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("値がメッセージに含まれること", e.getMessage(), containsString("value=[\\r]"));
            assertThat("出所（セクションと id）がメッセージに含まれること", e.getMessage(),
                    containsString("source=messages entry id='literalCrInFwHeaderValue001'"));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: FW 制御ヘッダのキーにバックスラッシュと r の 2 文字を書くと
     * エラーになること（2-5）。
     *
     * <p>
     * キーの検査は許可キー判定より前に置いてある。この 2 文字を含むキーは許可キーに一致しえないため、
     * 後ろに置くと到達せず「{@code has unknown key}」になってしまう<br>
     * Given: messages の literalCrInFwHeaderKey001 の fw_header に "req\\rId" というキー（2 文字を含む）<br>
     * When:  buildMessagePool(yaml, "messages", "literalCrInFwHeaderKey001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージにキーの値と出所（セクション・id）が含まれ、
     *        {@code has unknown key} ではないこと
     * </p>
     */
    @Test
    public void buildMessagePool_literalBackslashRInFwHeaderKeyThrows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/fwHeaderMapData");

        // When
        try {
            buildMessagePool(yaml, "messages", "literalCrInFwHeaderKey001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("キーの値がメッセージに含まれること", e.getMessage(),
                    containsString("value=[req\\rId]"));
            assertThat("出所（セクションと id）がメッセージに含まれること", e.getMessage(),
                    containsString("source=messages entry id='literalCrInFwHeaderKey001'"));
            assertThat("許可キー判定より前に検査されること", e.getMessage(),
                    not(containsString("has unknown key")));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: 電文の本文データ行に 2 文字を書くとエラーになること（2-5）。
     *
     * <p>
     * 何を担保するか: {@code messages} エントリの {@code fw_header} 側だけでなく、本文
     * （{@code records.rows}）側でも検査が働き、出所に {@code messages} セクションと id が入ること。
     * 本文は {@code YamlFileBuilder#buildFragmentsForMessage} 経由で {@code YamlSection#interpret} を通る<br>
     * Given: messages の literalCrInMessageBody001 の rows に {@code "\\r"}<br>
     * When:  buildMessagePool(yaml, "messages", "literalCrInMessageBody001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージに値と出所（セクション・id）が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_literalBackslashRInMessageBodyRowThrows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        try {
            buildMessagePool(yaml, "messages", "literalCrInMessageBody001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("値がメッセージに含まれること", e.getMessage(), containsString("value=[\\r]"));
            assertThat("出所（セクションと id）がメッセージに含まれること", e.getMessage(),
                    containsString("source=messages entry id='literalCrInMessageBody001'"));
        }
    }

    /**
     * [YamlMessageBuilder] buildMessagePool: 電文のディレクティブの値に 2 文字を書くとエラーになること（2-5）。
     *
     * <p>
     * 何を担保するか: 電文のディレクティブもデータ行と同じ {@code YamlSection#interpret} を通ること
     * （{@code YamlFileBuilder#applyDirectives}）。ファイル系のディレクティブは別テストで固定済みだが、
     * 電文系は入口（{@code YamlMessageBuilder#buildMessageBodyFile}）が別である<br>
     * Given: messages の literalCrInMessageDirective001 の record-separator に {@code "\\r"}<br>
     * When:  buildMessagePool(yaml, "messages", "literalCrInMessageDirective001", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージに値と出所（セクション・id）が含まれること
     * </p>
     */
    @Test
    public void buildMessagePool_literalBackslashRInMessageDirectiveThrows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        try {
            buildMessagePool(yaml, "messages", "literalCrInMessageDirective001", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("値がメッセージに含まれること", e.getMessage(), containsString("value=[\\r]"));
            assertThat("出所（セクションと id）がメッセージに含まれること", e.getMessage(),
                    containsString("source=messages entry id='literalCrInMessageDirective001'"));
        }
    }

    /**
     * [YamlMessageBuilder] buildSendSyncList: 送信同期セクションのデータ行に 2 文字を書くと
     * エラーになること（2-5）。
     *
     * <p>
     * 何を担保するか: 送信同期 4 セクションの経路でも検査が働き、出所がセクションキーと
     * エントリの id で組み立てられること。{@code buildSendSyncList} は
     * {@code buildMessageContent} とは別に出所文字列を組み立てるため、独立に固定する<br>
     * Given: response_body_messages の literalCrSendSync グループ（id: s1）の rows に {@code "\\r"}<br>
     * When:  buildSendSyncList(yaml, "response_body_messages", "[literalCrSendSync]", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージに値と出所（セクション・id）が含まれること
     * </p>
     */
    @Test
    public void buildSendSyncList_literalBackslashRInRowThrows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        try {
            buildSendSyncMessageList(yaml, "response_body_messages", "[literalCrSendSync]", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("値がメッセージに含まれること", e.getMessage(), containsString("value=[\\r]"));
            assertThat("出所（セクションと id）がメッセージに含まれること", e.getMessage(),
                    containsString("source=response_body_messages entry id='s1'"));
        }
    }

    /**
     * [YamlMessageBuilder] buildSendSyncBodies: 送信同期セクションのデータ行に 2 文字を書くと
     * エラーになること（2-5）。
     *
     * <p>
     * 何を担保するか: 変換ツール用の {@code buildSendSyncBodies} でも検査が働き、出所が
     * セクションキーとエントリの id で組み立てられること。{@code buildSendSyncList} とは
     * 別の箇所で出所文字列を組み立てているため、独立に固定する<br>
     * Given: response_body_messages の literalCrSendSyncBodies グループ（id: s2）の rows に {@code "\\r"}<br>
     * When:  buildSendSyncBodies(yaml, "response_body_messages", "literalCrSendSyncBodies", path) を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージに値と出所（セクション・id）が含まれること
     * </p>
     */
    @Test
    public void buildSendSyncBodies_literalBackslashRInRowThrows() {
        // Given
        Map<String, Object> yaml = YamlLoader.load(DIR, "YamlMessageBuilderTest/messageData");

        // When
        try {
            builder.buildSendSyncBodies(yaml, "response_body_messages", "literalCrSendSyncBodies", DIR);
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("値がメッセージに含まれること", e.getMessage(), containsString("value=[\\r]"));
            assertThat("出所（セクションと id）がメッセージに含まれること", e.getMessage(),
                    containsString("source=response_body_messages entry id='s2'"));
        }
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
