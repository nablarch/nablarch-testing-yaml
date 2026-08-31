package nablarch.test.core.reader.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import nablarch.test.NablarchTestUtils;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * YAML ファイルのロードとキャッシュ管理。
 *
 * <p>
 * {@code nablarch.test.core.reader.yaml} パッケージ内のビルダ（{@code Yaml*Builder}）・
 * {@link nablarch.test.core.reader.YamlTestDataParser} から使用する。
 * </p>
 *
 * <p>
 * SnakeYAML Engine 3.x の {@link Load} を使用する。
 * デフォルトの Core Schema（YAML 1.2）が適用されるため、{@code no}/{@code yes}/{@code on}/{@code off} は
 * 文字列として扱われる（YAML 1.1 の Boolean 変換は行われない）。
 * 重複キーは {@link IllegalStateException} をスローする。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlLoader {

    private static final String YAML_EXTENSION = ".yaml";

    private static final String SCHEMA_RESOURCE_PATH = "nablarch/test/ntf-testdata-yaml-schema.json";

    /** 既存の {@link nablarch.test.core.reader.TableDataParser} 等のキャッシュサイズに合わせた値。 */
    private static final int YAML_CACHE_MAX_SIZE = 8;

    /** YAML キャッシュ（filePath → 解析済み Map）。アクセス順 LRU で最大 {@value #YAML_CACHE_MAX_SIZE} エントリを保持する。 */
    private static final Map<String, Map<String, Object>> YAML_CACHE =
            NablarchTestUtils.createLRUMap(YAML_CACHE_MAX_SIZE);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonSchema JSON_SCHEMA;

    // スキーマは本モジュールの jar に同梱するリソースであり、通常の実行環境ではクラスパスから欠落しない。
    // schemaStream == null と IOException の分岐は、クラスローダを細工しない限り到達不能な防御である。
    static {
        try (InputStream schemaStream = YamlLoader.class.getClassLoader()
                .getResourceAsStream(SCHEMA_RESOURCE_PATH)) {
            if (schemaStream == null) {
                throw new IllegalStateException("Schema file not found on classpath: " + SCHEMA_RESOURCE_PATH);
            }
            JSON_SCHEMA = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(schemaStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load JSON schema from: " + SCHEMA_RESOURCE_PATH, e);
        }
    }

    private YamlLoader() {
    }

    /**
     * basePath と resourceName を "/" 1 つで連結してファイルパスを組み立てる。
     * basePath が末尾 "/" 付きの場合は余分な "/" を追加しない。
     *
     * @param basePath     ベースディレクトリパス
     * @param resourceName リソース名（拡張子なし）
     * @return ファイルパス文字列
     */
    private static String buildFilePath(String basePath, String resourceName) {
        return join(basePath, resourceName) + YAML_EXTENSION;
    }

    /**
     * 入れ物（テストデータをまとめるディレクトリ）のパスを組み立てる。
     *
     * <p>
     * 入れ物名は resourceName の最後の {@code "/"} より前の部分とする。
     * resourceName に {@code "/"} が含まれない場合は resourceName 全体を入れ物名として扱う。
     * </p>
     *
     * @param basePath     ベースディレクトリパス
     * @param resourceName リソース名（拡張子なし）
     * @return 入れ物ディレクトリのパス文字列
     */
    private static String buildContainerPath(String basePath, String resourceName) {
        int index = resourceName.lastIndexOf('/');
        String containerName = (index < 0) ? resourceName : resourceName.substring(0, index);
        return join(basePath, containerName);
    }

    /**
     * basePath と名前を "/" 1 つで連結する。
     * basePath が末尾 "/" 付きの場合は余分な "/" を追加しない。
     *
     * @param basePath ベースディレクトリパス
     * @param name     連結する名前
     * @return 連結したパス文字列
     */
    private static String join(String basePath, String name) {
        if (basePath.endsWith("/")) {
            return basePath + name;
        }
        return basePath + "/" + name;
    }

    /**
     * 指定した YAML ファイルをロードし、トップレベルの Map を返す。
     * 同一ファイルパスは LRU キャッシュから返す。
     *
     * @param basePath     ベースディレクトリパス
     * @param resourceName リソース名（拡張子なし）
     * @return YAML トップレベル Map（キー: セクション名、値: セクションデータ）
     * @throws IllegalStateException ファイルが存在しない・IO エラー・重複キー・ルートがマッピングでない場合
     * @throws YamlSchemaValidationException スキーマ違反（型不正・required フィールド漏れ・enum 違反など）が検出された場合
     */
    public static Map<String, Object> load(String basePath, String resourceName) {
        String filePath = buildFilePath(basePath, resourceName);
        Map<String, Object> cached = YAML_CACHE.get(filePath);
        if (cached != null) {
            return cached;
        }
        LoadSettings settings = LoadSettings.builder()
                .setAllowDuplicateKeys(false)
                .build();
        Load loader = new Load(settings);
        try (FileInputStream in = new FileInputStream(new File(filePath))) {
            Object loaded = loader.loadFromInputStream(in);
            if (loaded == null) {
                YAML_CACHE.put(filePath, Collections.emptyMap());
                return Collections.emptyMap();
            }
            if (!(loaded instanceof Map)) {
                throw new IllegalStateException(
                        "YAML root must be a mapping, but was "
                                + loaded.getClass().getSimpleName() + ". file=" + filePath);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) loaded;
            JsonNode jsonNode = OBJECT_MAPPER.valueToTree(result);
            Set<ValidationMessage> errors = JSON_SCHEMA.validate(jsonNode);
            if (!errors.isEmpty()) {
                throw new YamlSchemaValidationException(filePath, new ArrayList<>(errors));
            }
            YAML_CACHE.put(filePath, result);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load YAML file: " + filePath, e);
        } catch (YamlEngineException e) {
            throw new IllegalStateException("Failed to parse YAML file: " + filePath, e);
        }
    }

    /**
     * テストデータの入れ物が存在するかどうかを返す。
     *
     * <p>
     * YAML 形式の入れ物は {@code <basePath>/<入れ物名>} ディレクトリである
     * （Excel 形式の {@code <basePath>/<入れ物名>.xls} に相当する）。
     * 入れ物名は resourceName の最後の {@code "/"} より前の部分とし、
     * resourceName に {@code "/"} が含まれない場合は resourceName 全体を入れ物名として扱う。
     * </p>
     *
     * <p>
     * 読み込み単位（{@code <basePath>/<入れ物名>/<読み込み単位名>.yaml}）の存在は見ない。
     * 読み込み単位の存在を判定する場合は {@link #isDataExisting} を使用する。
     * </p>
     *
     * @param basePath     ベースディレクトリパス
     * @param resourceName リソース名（拡張子なし）
     * @return 入れ物ディレクトリが存在する場合は {@code true}、存在しない場合は {@code false}
     */
    public static boolean isResourceExisting(String basePath, String resourceName) {
        return new File(buildContainerPath(basePath, resourceName)).isDirectory();
    }

    /**
     * 読み込み単位の YAML ファイルが存在するかどうかを返す。
     *
     * <p>
     * {@code <basePath>/<resourceName>.yaml} が通常ファイルとして存在するかを判定する
     * （Excel 形式の {@code isDataExisting}（シート単位）に相当する）。
     * </p>
     *
     * @param basePath     ベースディレクトリパス
     * @param resourceName リソース名（拡張子なし）
     * @return YAML ファイルが存在する場合は {@code true}、存在しない場合は {@code false}
     */
    public static boolean isDataExisting(String basePath, String resourceName) {
        return new File(buildFilePath(basePath, resourceName)).isFile();
    }

    /**
     * テスト専用: YAML キャッシュを全件クリアする。
     *
     * <p>
     * 各テストメソッドの {@code @After} から呼び出すことで、テスト間のキャッシュ汚染を防ぐ。
     * プロダクションコードからは呼び出さないこと。
     * </p>
     */
    public static void clearCacheForTest() {
        YAML_CACHE.clear();
    }
}
