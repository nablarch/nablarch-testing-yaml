package nablarch.test.core.reader.yaml;

import com.networknt.schema.Error;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * YAML file failed JSON Schema validation.
 */
public class YamlSchemaValidationException extends IllegalStateException {

    private final String filePath;
    private final List<Error> errors;

    /**
     * @param filePath path to the YAML file that failed validation
     * @param errors   list of schema violations
     */
    public YamlSchemaValidationException(String filePath, List<Error> errors) {
        super("YAML file failed schema validation: " + filePath);
        this.filePath = filePath;
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        return "YAML file failed schema validation: " + filePath + "\n"
                + errors.stream().map(Error::toString).collect(Collectors.joining("\n"));
    }

    /**
     * @return unmodifiable list of schema violations
     */
    public List<Error> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
