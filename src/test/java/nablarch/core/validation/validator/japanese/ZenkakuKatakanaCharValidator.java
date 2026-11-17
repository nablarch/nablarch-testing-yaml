package nablarch.core.validation.validator.japanese;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 全角カタカナのみからなる文字列であるかをチェックするバリデータ。
 * 
 * @author Koichi Asano
 *
 */
public class ZenkakuKatakanaCharValidator extends JapaneseCharacterValidatorSupport<ZenkakuKatakanaChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return ZenkakuKatakanaChar.class;
    }

    @Override
    protected boolean isValid(ZenkakuKatakanaChar annotation, String value) {
        return getJapaneseCharacterChecker().checkZenkakuKatakanaCharOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(ZenkakuKatakanaChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public ZenkakuKatakanaChar createAnnotation(final Map<String, Object> params) {
        return new ZenkakuKatakanaChar() {
            public Class<? extends Annotation> annotationType() {
                return ZenkakuKatakanaChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }
        };
    }
}
