package nablarch.core.validation.validator.japanese;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 全角文字のみからなる文字列であるかをチェックするバリデータ。
 * 
 * @author Koichi Asano
 *
 */
public class ZenkakuCharValidator extends JapaneseCharacterValidatorSupport<ZenkakuChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return ZenkakuChar.class;
    }

    @Override
    protected boolean isValid(ZenkakuChar annotation, String value) {
        return getJapaneseCharacterChecker().checkZenkakuCharOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(ZenkakuChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public ZenkakuChar createAnnotation(final Map<String, Object> params) {
        return new ZenkakuChar() {
            public Class<? extends Annotation> annotationType() {
                return ZenkakuChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }
        };
    }
}
