package nablarch.core.validation.validator.japanese;

import java.lang.annotation.Annotation;
import java.util.Map;

import nablarch.core.validation.validator.japanese.ZenkakuAndGaijiChar;


/**
 * 全角文字および外字のみからなる文字列であるかをチェックするバリデータ。
 * 
 * @author Koichi Asano
 *
 */
public class ZenkakuAndGaijiCharValidator extends JapaneseCharacterValidatorSupport<ZenkakuAndGaijiChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return ZenkakuAndGaijiChar.class;
    }

    @Override
    protected boolean isValid(ZenkakuAndGaijiChar annotation, String value) {
        return getJapaneseCharacterChecker().checkZenkakuCharAndGaijiCharOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(ZenkakuAndGaijiChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public ZenkakuAndGaijiChar createAnnotation(final Map<String, Object> params) {
        return new ZenkakuAndGaijiChar() {
            public Class<? extends Annotation> annotationType() {
                return ZenkakuAndGaijiChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }};
    }
}
