package nablarch.test.core.reader.yaml;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import nablarch.test.NablarchTestUtils;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class YamlLoader {

    private static final String YAML_EXTENSION = ".yaml";
    private static final int YAML_CACHE_MAX_SIZE = 8;
    private static final Map<String, Map<String, Object>> YAML_CACHE =
            NablarchTestUtils.createLRUMap(YAML_CACHE_MAX_SIZE);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Schema JSON_SCHEMA;

    static {
        try (InputStream schemaStream = YamlLoader.class.getClassLoader()
                .getResourceAsStream("nablarch/test/ntf-testdata-yaml-schema.json")) {
            if (schemaStream == null) {
                throw new IllegalStateException("Schema file not found on classpath");
            }
            JSON_SCHEMA = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                    .getSchema(schemaStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load JSON schema", e);
        }
    }

    private YamlLoader() {
    }

    private static String buildFilePath(String basePath, String resourceName) {
        if (basePath.endsWith("/")) {
            return basePath + resourceName + YAML_EXTENSION;
        }
        return basePath + "/" + resourceName + YAML_EXTENSION;
    }

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
            List<Error> errors = JSON_SCHEMA.validate(jsonNode);
            if (!errors.isEmpty()) {
                throw new YamlSchemaValidationException(filePath, errors);
            }
            YAML_CACHE.put(filePath, result);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load YAML file: " + filePath, e);
        } catch (YamlEngineException e) {
            throw new IllegalStateException("Failed to parse YAML file: " + filePath, e);
        }
    }

    public static boolean isResourceExisting(String basePath, String resourceName) {
        return new File(buildFilePath(basePath, resourceName)).exists();
    }

    public static void clearCacheForTest() {
        YAML_CACHE.clear();
    }
}
