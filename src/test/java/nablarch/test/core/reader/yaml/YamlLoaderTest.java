package nablarch.test.core.reader.yaml;

import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import com.networknt.schema.ValidationMessage;

/**
 * {@link YamlLoader} のテストクラス。
 *
 * <p>
 * YAML ファイルのロード・キャッシュ・エラー処理を検証する。
 * </p>
 */
public class YamlLoaderTest {

    private static final String RESOURCE_ROOT = "src/test/java/";
    private static final String DIR = RESOURCE_ROOT + "nablarch/test/core/reader/yaml/";

    @After
    public void after() {
        YamlLoader.clearCacheForTest();
    }

    // ========================================================================
    // load: YAML ファイルを正常にロードできること
    // ========================================================================

    /**
     * [YamlLoader] load: YAML ファイルをロードしてトップレベル Map を返すこと。
     *
     * <p>
     * Given: setup_tables セクションを含む YAML ファイル<br>
     * When:  load(dir, "YamlLoaderTest/simple") を呼ぶ<br>
     * Then:  Map が返り、setup_tables キーが存在すること
     * </p>
     */
    @Test
    public void load_returnsTopLevelMap() {
        // Given / When
        Map<String, Object> result = YamlLoader.load(DIR, "YamlLoaderTest/simple");

        // Then
        assertThat(result, notNullValue());
        assertTrue(result.containsKey("setup_tables"));
    }

    /**
     * [YamlLoader] load: setup_tables の値が List であること。
     *
     * <p>
     * Given: setup_tables セクションを含む YAML ファイル<br>
     * When:  load し、setup_tables の値を取得する<br>
     * Then:  List であること
     * </p>
     */
    @Test
    public void load_setupTablesIsList() {
        // Given / When
        Map<String, Object> result = YamlLoader.load(DIR, "YamlLoaderTest/simple");

        // Then
        Object setupTables = result.get("setup_tables");
        assertTrue(setupTables instanceof List);
    }

    // ========================================================================
    // load: キャッシュ（同一パスは同一インスタンスを返す）
    // ========================================================================

    /**
     * [YamlLoader] load: 同一パスを2回ロードした場合、同一インスタンスが返ること（キャッシュ）。
     *
     * <p>
     * Given: 同じ YAML ファイルパス<br>
     * When:  load を2回呼ぶ<br>
     * Then:  同一 Map インスタンスが返ること
     * </p>
     */
    @Test
    public void load_returnsCachedInstance() {
        // Given / When
        Map<String, Object> first = YamlLoader.load(DIR, "YamlLoaderTest/simple");
        Map<String, Object> second = YamlLoader.load(DIR, "YamlLoaderTest/simple");

        // Then: 同一インスタンス
        assertThat(first == second, is(true));
    }

    // ========================================================================
    // load: 重複キーは例外をスローすること
    // ========================================================================

    /**
     * [YamlLoader] load: YAML ファイルに重複キーがある場合は IllegalStateException がスローされること。
     *
     * <p>
     * Given: setup_tables キーが2回定義された YAML ファイル<br>
     * When:  load を呼ぶ<br>
     * Then:  IllegalStateException がスローされること
     * </p>
     */
    @Test
    public void load_throwsOnDuplicateKey() {
        // When
        try {
            YamlLoader.load(DIR, "YamlLoaderTest/duplicateKey");
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("エラーメッセージにファイルパスが含まれること",
                    e.getMessage(), containsString("YamlLoaderTest/duplicateKey"));
        }
    }

    // ========================================================================
    // load: ファイルが存在しない場合は IllegalStateException をスローすること
    // ========================================================================

    /**
     * [YamlLoader] load: 存在しないファイルを指定した場合は IllegalStateException がスローされること。
     *
     * <p>
     * Given: 存在しないファイルパス<br>
     * When:  load を呼ぶ<br>
     * Then:  IllegalStateException がスローされること
     * </p>
     */
    @Test
    public void load_throwsWhenFileNotExists() {
        // When
        try {
            YamlLoader.load(DIR, "YamlLoaderTest/noSuchFile");
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("エラーメッセージにファイルパスが含まれること",
                    e.getMessage(), containsString("YamlLoaderTest/noSuchFile"));
        }
    }

    // ========================================================================
    // load: 空の YAML は空 Map を返すこと
    // ========================================================================

    /**
     * [YamlLoader] load: 空の YAML ファイルをロードした場合は空 Map が返ること。
     *
     * <p>
     * Given: 内容が空の YAML ファイル<br>
     * When:  load を呼ぶ<br>
     * Then:  空 Map が返ること
     * </p>
     */
    @Test
    public void load_emptyYamlReturnsEmptyMap() {
        // Given / When
        Map<String, Object> result = YamlLoader.load(DIR, "YamlLoaderTest/empty");

        // Then
        assertThat(result.isEmpty(), is(true));
    }

    // ========================================================================
    // load: YAML ルートがマッピングでない場合は IllegalStateException をスローすること
    // ========================================================================

    /**
     * [YamlLoader] load: YAML ルートがリストの場合は IllegalStateException がスローされること。
     *
     * <p>
     * Given: ルートがリスト（- item1, - item2）の YAML ファイル<br>
     * When:  load を呼ぶ<br>
     * Then:  IllegalStateException がスローされ、メッセージにファイルパスが含まれること
     * </p>
     */
    @Test
    public void load_throwsWhenRootIsNotMap() {
        // When
        try {
            YamlLoader.load(DIR, "YamlLoaderTest/rootIsList");
            fail("IllegalStateException が期待される");
        } catch (IllegalStateException e) {
            // Then
            assertThat("エラーメッセージにファイルパスが含まれること",
                    e.getMessage(), containsString("YamlLoaderTest/rootIsList"));
        }
    }

    // ========================================================================
    // isResourceExisting（入れ物単位）／isDataExisting（読み込み単位）
    // ========================================================================

    /**
     * [YamlLoader] isResourceExisting: 入れ物ディレクトリが存在する場合は true を返すこと。
     *
     * <p>
     * Given: 入れ物ディレクトリ YamlLoaderTest と、その配下の simple.yaml<br>
     * When:  isResourceExisting(dir, "YamlLoaderTest/simple") を呼ぶ<br>
     * Then:  true が返ること
     * </p>
     */
    @Test
    public void isResourceExisting_trueWhenContainerDirectoryExists() {
        // When / Then
        assertThat(YamlLoader.isResourceExisting(DIR, "YamlLoaderTest/simple"), is(true));
    }

    /**
     * [YamlLoader] isResourceExisting: 判定単位が入れ物であること（読み込み単位の YAML が無くても
     * 入れ物ディレクトリがあれば true）を担保する。
     *
     * <p>
     * Given: 入れ物ディレクトリ YamlLoaderTest は存在するが noSuchFile.yaml は存在しない<br>
     * When:  isResourceExisting(dir, "YamlLoaderTest/noSuchFile") を呼ぶ<br>
     * Then:  true が返ること（読み込み単位の判定である isDataExisting は false）
     * </p>
     */
    @Test
    public void isResourceExisting_trueWhenReadUnitMissingButContainerExists() {
        // Given
        assertThat("読み込み単位は存在しない", YamlLoader.isDataExisting(DIR, "YamlLoaderTest/noSuchFile"), is(false));

        // When / Then
        assertThat(YamlLoader.isResourceExisting(DIR, "YamlLoaderTest/noSuchFile"), is(true));
    }

    /**
     * [YamlLoader] isResourceExisting: 入れ物ディレクトリが存在しない場合は false を返すこと。
     *
     * <p>
     * Given: 存在しない入れ物名<br>
     * When:  isResourceExisting を呼ぶ<br>
     * Then:  false が返ること
     * </p>
     */
    @Test
    public void isResourceExisting_falseWhenContainerDirectoryNotExists() {
        // When / Then
        assertThat(YamlLoader.isResourceExisting(DIR, "NoSuchContainer/simple"), is(false));
    }

    /**
     * [YamlLoader] isResourceExisting: 入れ物名と同名のものがディレクトリでない場合は false を返すこと。
     *
     * <p>
     * Given: basePath 直下に同名のファイル（YamlLoaderTest.java）が存在する<br>
     * When:  isResourceExisting(dir, "YamlLoaderTest.java/simple") を呼ぶ<br>
     * Then:  false が返ること（入れ物はディレクトリでなければならない）
     * </p>
     */
    @Test
    public void isResourceExisting_falseWhenContainerIsNotDirectory() {
        // When / Then
        assertThat(YamlLoader.isResourceExisting(DIR, "YamlLoaderTest.java/simple"), is(false));
    }

    /**
     * [YamlLoader] isResourceExisting: resourceName に "/" が含まれない場合は resourceName 全体を
     * 入れ物名として扱うこと。
     *
     * <p>
     * Given: "/" を含まないリソース名<br>
     * When:  isResourceExisting を呼ぶ<br>
     * Then:  basePath/resourceName ディレクトリの有無がそのまま返ること
     * </p>
     */
    @Test
    public void isResourceExisting_wholeNameIsContainerWhenNoSlash() {
        // When / Then
        assertThat(YamlLoader.isResourceExisting(DIR, "YamlLoaderTest"), is(true));
        assertThat(YamlLoader.isResourceExisting(DIR, "NoSuchContainer"), is(false));
    }

    /**
     * [YamlLoader] isDataExisting: 読み込み単位の YAML ファイルが存在する場合は true を返すこと。
     *
     * <p>
     * Given: YamlLoaderTest/simple.yaml が配置されている<br>
     * When:  isDataExisting を呼ぶ<br>
     * Then:  true が返ること
     * </p>
     */
    @Test
    public void isDataExisting_trueWhenYamlFileExists() {
        // When / Then
        assertThat(YamlLoader.isDataExisting(DIR, "YamlLoaderTest/simple"), is(true));
    }

    /**
     * [YamlLoader] isDataExisting: 読み込み単位の YAML ファイルが存在しない場合は false を返すこと。
     *
     * <p>
     * Given: 入れ物ディレクトリは存在するが、その配下に noSuchFile.yaml は存在しない<br>
     * When:  isDataExisting を呼ぶ<br>
     * Then:  false が返ること
     * </p>
     */
    @Test
    public void isDataExisting_falseWhenYamlFileNotExists() {
        // When / Then
        assertThat(YamlLoader.isDataExisting(DIR, "YamlLoaderTest/noSuchFile"), is(false));
    }

    /**
     * [YamlLoader] isDataExisting: "/" を含まないリソース名は basePath 直下の YAML を指すこと。
     *
     * <p>
     * Given: basePath 直下に YamlLoaderTest.yaml は存在しない<br>
     * When:  isDataExisting(dir, "YamlLoaderTest") を呼ぶ<br>
     * Then:  false が返ること（同名ディレクトリの存在に引きずられないこと）
     * </p>
     */
    @Test
    public void isDataExisting_falseWhenOnlySameNamedDirectoryExists() {
        // When / Then
        assertThat(YamlLoader.isDataExisting(DIR, "YamlLoaderTest"), is(false));
    }

    // ========================================================================
    // load: LRU キャッシュ上限超過で最古エントリが追い出されること
    // ========================================================================

    /**
     * [YamlLoader] load: LRU キャッシュ上限（8件）を超えると最初にロードしたエントリが追い出されること。
     *
     * <p>
     * Given: lru1.yaml〜lru9.yaml（9ファイル）。キャッシュ上限は 8<br>
     * When:  9ファイルをロードした後、lru1.yaml を再ロードする<br>
     * Then:  lru1.yaml の再ロード結果が最初のロードと別インスタンスであること（キャッシュから追い出されたため）
     * </p>
     */
    @Test
    public void load_lruEvictionWhenCacheFull() {
        // Given: キャッシュ上限 8 を超える 9 ファイルをロードする
        Map<String, Object> first = YamlLoader.load(DIR, "YamlLoaderTest/lru1");
        YamlLoader.load(DIR, "YamlLoaderTest/lru2");
        YamlLoader.load(DIR, "YamlLoaderTest/lru3");
        YamlLoader.load(DIR, "YamlLoaderTest/lru4");
        YamlLoader.load(DIR, "YamlLoaderTest/lru5");
        YamlLoader.load(DIR, "YamlLoaderTest/lru6");
        YamlLoader.load(DIR, "YamlLoaderTest/lru7");
        YamlLoader.load(DIR, "YamlLoaderTest/lru8");

        // When: 9件目をロードして lru1 をキャッシュから追い出す
        YamlLoader.load(DIR, "YamlLoaderTest/lru9");

        // Then: lru1 は追い出されているため再ロードすると別インスタンス
        Map<String, Object> reloaded = YamlLoader.load(DIR, "YamlLoaderTest/lru1");
        assertThat("lru1 はキャッシュから追い出され、別インスタンスになること",
                first == reloaded, is(false));
    }

    // ========================================================================
    // load: 末尾 "/" 付き basePath でも正常にロードできること
    // ========================================================================

    /**
     * [YamlLoader] load: basePath に末尾 "/" がない場合でも正常にロードできること。
     *
     * <p>
     * buildFilePath の false 分岐（line 53）: basePath が "/" で終わっていない場合は "/" を補完して連結する。<br>
     * DIR は末尾 "/" 付きのため、それを除いた basePath を使ってロードできることを確認する。<br>
     * Given: 末尾 "/" なしの basePath（DIR の末尾 "/" を除いた文字列）<br>
     * When:  load(DIR without trailing slash, "YamlLoaderTest/simple") を呼ぶ<br>
     * Then:  Map が返り、setup_tables キーが存在すること
     * </p>
     */
    @Test
    public void load_noTrailingSlashBasePathLoadsCorrectly() {
        // Given: DIR の末尾 "/" を除いた basePath（false 分岐を踏む）
        String basePathNoSlash = DIR.substring(0, DIR.length() - 1);

        // When
        Map<String, Object> result = YamlLoader.load(basePathNoSlash, "YamlLoaderTest/simple");

        // Then
        assertThat(result, notNullValue());
        assertTrue(result.containsKey("setup_tables"));
    }

    /**
     * [YamlLoader] load: 最近アクセスしたエントリが LRU キャッシュから追い出されないこと（QA観点2-中）。
     *
     * <p>
     * Given: lru1.yaml〜lru8.yaml（8ファイル）をロード後、lru1 に再アクセスする<br>
     * When:  lru9.yaml（9件目）をロードしてエビクションを起こす<br>
     * Then:  最近アクセスした lru1 がキャッシュに残っており、同一インスタンスが返ること
     * </p>
     */
    @Test
    public void load_recentlyAccessedEntryIsNotEvicted() {
        // Given: 8 ファイルをロードしてキャッシュを満杯にする
        Map<String, Object> lru1 = YamlLoader.load(DIR, "YamlLoaderTest/lru1");
        YamlLoader.load(DIR, "YamlLoaderTest/lru2");
        YamlLoader.load(DIR, "YamlLoaderTest/lru3");
        YamlLoader.load(DIR, "YamlLoaderTest/lru4");
        YamlLoader.load(DIR, "YamlLoaderTest/lru5");
        YamlLoader.load(DIR, "YamlLoaderTest/lru6");
        YamlLoader.load(DIR, "YamlLoaderTest/lru7");
        YamlLoader.load(DIR, "YamlLoaderTest/lru8");

        // When: lru1 に再アクセスして「最近使用」にしてから 9 件目をロード
        YamlLoader.load(DIR, "YamlLoaderTest/lru1");  // lru1 を最近使用に更新
        YamlLoader.load(DIR, "YamlLoaderTest/lru9");  // lru2 が追い出されるはず

        // Then: lru1 はキャッシュに残っており同一インスタンス（最近アクセスしたので追い出されない）
        Map<String, Object> afterEviction = YamlLoader.load(DIR, "YamlLoaderTest/lru1");
        assertThat("最近アクセスした lru1 はキャッシュに残っているため同一インスタンスであること",
                lru1 == afterEviction, is(true));
    }

    // ========================================================================
    // load: スキーマ違反の YAML は YamlSchemaValidationException をスローすること
    // ========================================================================

    /**
     * [YamlLoader] load: スキーマ違反の YAML をロードした場合は YamlSchemaValidationException がスローされること。
     *
     * <p>
     * Given: rows が配列でない YAML ファイル（スキーマ違反）<br>
     * When:  load を呼ぶ<br>
     * Then:  YamlSchemaValidationException がスローされ、メッセージにファイルパスが含まれること
     * </p>
     */
    @Test
    public void load_schemaViolation_throwsYamlSchemaValidationException() {
        // When
        try {
            YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_wrongType_rows");
            fail("YamlSchemaValidationException が期待される");
        } catch (YamlSchemaValidationException e) {
            // Then
            assertThat("エラーメッセージにファイルパスが含まれること",
                    e.getMessage(), containsString("YamlLoaderTest/schemaViolation_wrongType_rows"));
            List<ValidationMessage> errors = e.getErrors();
            assertThat("エラーが1件以上あること", errors.isEmpty(), is(false));
        }
    }

    /**
     * [YamlLoader] load: required フィールドが欠落した YAML をロードした場合は YamlSchemaValidationException がスローされること。
     *
     * <p>
     * Given: required フィールド（table 等）が欠落した YAML ファイル<br>
     * When:  load を呼ぶ<br>
     * Then:  YamlSchemaValidationException がスローされること
     * </p>
     */
    @Test(expected = YamlSchemaValidationException.class)
    public void load_schemaViolation_missingRequired() {
        YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_missingRequired");
    }

    /**
     * [YamlLoader] load: rows の型が配列でない（スカラー）YAML をロードした場合は YamlSchemaValidationException がスローされること。
     *
     * <p>
     * Given: rows がスカラー値の YAML ファイル（スキーマ型違反）<br>
     * When:  load を呼ぶ<br>
     * Then:  YamlSchemaValidationException がスローされること
     * </p>
     */
    @Test(expected = YamlSchemaValidationException.class)
    public void load_schemaViolation_wrongTypeRows() {
        YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_wrongType_rows");
    }

    /**
     * [YamlLoader] load: type フィールドが enum 違反（fixed/variable 以外）の YAML をロードした場合は YamlSchemaValidationException がスローされること。
     *
     * <p>
     * Given: type フィールドに "invalid_type" が設定された YAML ファイル（enum 違反）<br>
     * When:  load を呼ぶ<br>
     * Then:  YamlSchemaValidationException がスローされること
     * </p>
     */
    @Test(expected = YamlSchemaValidationException.class)
    public void load_schemaViolation_enumViolation() {
        YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_enumViolation");
    }

    // ネストが深い場所の違反 → エラーの JSON Path が深いパスを含むこと
    @Test
    public void load_schemaViolation_deepNested_errorPathIsDeep() {
        try {
            YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_deepNested");
            fail("YamlSchemaValidationException が期待される");
        } catch (YamlSchemaValidationException e) {
            // エラーパスに records か fields が含まれること（深いネストの場所が特定できること）
            String msg = e.getMessage();
            assertThat("error message must contain a path separator indicating nesting", msg, containsString("/"));
            assertThat("error path must point to nested location", msg, anyOf(
                    containsString("records"), containsString("fields")));
        }
    }

    // 複数違反 → getErrors() が複数件返ること
    @Test
    public void load_schemaViolation_multipleErrors_allReported() {
        try {
            YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_multipleErrors");
            fail("YamlSchemaValidationException が期待される");
        } catch (YamlSchemaValidationException e) {
            assertThat("複数の違反が全て報告されること", e.getErrors().size(), is(greaterThan(1)));
        }
    }

    /**
     * [YamlLoader] load: messages セクションに group_id を含む YAML をロードした場合は YamlSchemaValidationException がスローされること。
     *
     * <p>
     * Given: messages セクションに group_id: case1 を含む YAML ファイル（message_data の additionalProperties: false 違反）<br>
     * When:  load を呼ぶ<br>
     * Then:  YamlSchemaValidationException がスローされること
     * </p>
     */
    @Test(expected = YamlSchemaValidationException.class)
    public void load_schemaViolation_messagesWithGroupId() {
        YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_messages_groupId");
    }

    /**
     * [YamlLoader] load: expected_request_header_messages セクションに fw_header を含む YAML をロードした場合は YamlSchemaValidationException がスローされること。
     *
     * <p>
     * Given: expected_request_header_messages セクションに fw_header: {requestId: "001"} を含む YAML ファイル（expected_request_message_data の additionalProperties: false 違反）<br>
     * When:  load を呼ぶ<br>
     * Then:  YamlSchemaValidationException がスローされること
     * </p>
     */
    @Test(expected = YamlSchemaValidationException.class)
    public void load_schemaViolation_expectedRequestWithFwHeader() {
        YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_expectedRequest_fwHeader");
    }

    /**
     * [YamlLoader] load: expected_request_body_messages セクションに group_id: "" を含む YAML をロードした場合は YamlSchemaValidationException がスローされること。
     *
     * <p>
     * Given: expected_request_body_messages セクションに group_id: "" を含む YAML ファイル（minLength: 1 違反）<br>
     * When:  load を呼ぶ<br>
     * Then:  YamlSchemaValidationException がスローされること
     * </p>
     */
    @Test(expected = YamlSchemaValidationException.class)
    public void load_schemaViolation_expectedRequestEmptyGroupId() {
        YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_expectedRequest_emptyGroupId");
    }

    /**
     * [YamlLoader] load: setup_tables に前方一致するトップレベルキーはスキーマ違反になること。
     *
     * <p>
     * 何を担保するか: YAML 形式のトップレベルキーはデータタイプごとの専用キーであり完全一致で
     * 解決されること（前方一致は発生しない）。{@code setup_tables_extra} のように
     * {@code setup_tables} に前方一致するキーは {@code setup_tables} として読まれず、
     * スキーマ（{@code additionalProperties: false}）違反になる。<br>
     * 根拠: implementation/testdata_notation.rst:205<br>
     * Given: トップレベルキーが setup_tables_extra だけの YAML ファイル<br>
     * When:  load を呼ぶ<br>
     * Then:  YamlSchemaValidationException がスローされ、違反の種別が additionalProperties で、
     *        メッセージに違反したキー名とファイルパスが含まれること
     * </p>
     */
    @Test
    public void load_prefixMatchedTopLevelKeyIsSchemaViolation() {
        // When
        try {
            YamlLoader.load(DIR, "YamlLoaderTest/schemaViolation_prefixMatchedTopLevelKey");
            fail("YamlSchemaValidationException が期待される");
        } catch (YamlSchemaValidationException e) {
            // Then
            assertThat("エラーメッセージにファイルパスが含まれること",
                    e.getMessage(), containsString("YamlLoaderTest/schemaViolation_prefixMatchedTopLevelKey"));
            assertThat("前方一致するキーは setup_tables として読まれず、違反したキー名が報告されること",
                    e.getMessage(), containsString("setup_tables_extra"));
            List<ValidationMessage> errors = e.getErrors();
            assertThat("違反が 1 件報告されること", errors.size(), is(1));
            assertThat("スキーマに定義されていないトップレベルキーとして弾かれること"
                            + "（additionalProperties 違反）: " + errors.get(0),
                    errors.get(0).getType(), is("additionalProperties"));
        }
    }
}
