package nablarch.test.core.reader.yaml;

import com.networknt.schema.ValidationMessage;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * YAML file failed JSON Schema validation.
 */
public class YamlSchemaValidationException extends IllegalStateException {

    private final String filePath;
    private final List<ValidationMessage> errors;

    /**
     * @param filePath path to the YAML file that failed validation
     * @param errors   list of schema violations
     */
    public YamlSchemaValidationException(String filePath, List<ValidationMessage> errors) {
        super("YAML file failed schema validation: " + filePath);
        this.filePath = filePath;
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        return "YAML file failed schema validation: " + filePath + "\n"
                + errors.stream().map(ValidationMessage::toString).collect(Collectors.joining("\n"));
    }

    /**
     * @return unmodifiable list of schema violations
     */
    public List<ValidationMessage> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
