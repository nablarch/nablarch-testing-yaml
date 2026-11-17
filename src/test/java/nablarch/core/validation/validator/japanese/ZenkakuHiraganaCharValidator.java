package nablarch.core.validation.validator.japanese;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 全角ひらがなのみからなる文字列であるかをチェックするバリデータ。
 * 
 * @author Koichi Asano
 *
 */
public class ZenkakuHiraganaCharValidator extends JapaneseCharacterValidatorSupport<ZenkakuHiraganaChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return ZenkakuHiraganaChar.class;
    }

    @Override
    protected boolean isValid(ZenkakuHiraganaChar annotation, String value) {
        return getJapaneseCharacterChecker().checkZenkakuHiraganaCharOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(ZenkakuHiraganaChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public ZenkakuHiraganaChar createAnnotation(final Map<String, Object> params) {
        return new ZenkakuHiraganaChar() {
            public Class<? extends Annotation> annotationType() {
                return ZenkakuHiraganaChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }
        };
    }
}
