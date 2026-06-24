package nablarch.test.core.reader.yaml;

import com.networknt.schema.Error;

import java.util.List;
import java.util.stream.Collectors;

/**
 * YAML ファイルがスキーマ検証に違反した場合にスローされる例外。
 */
public class YamlSchemaValidationException extends IllegalStateException {

    private final String filePath;
    private final List<Error> errors;

    /**
     * コンストラクタ。
     *
     * @param filePath 検証対象の YAML ファイルパス
     * @param errors   スキーマ違反のエラーリスト
     */
    public YamlSchemaValidationException(String filePath, List<Error> errors) {
        this.filePath = filePath;
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        return "YAML がスキーマに違反しています: " + filePath + "\n"
                + errors.stream().map(Error::toString).collect(Collectors.joining("\n"));
    }

    /**
     * スキーマ違反エラーのリストを返す。
     *
     * @return バリデーションエラーリスト
     */
    public List<Error> getErrors() {
        return errors;
    }
}
