package nablarch.core.validation.validator;

import java.lang.annotation.Annotation;
import java.util.Map;

import nablarch.core.validation.validator.NumberChar;

/**
 * 数字のみからなる文字列であるかをチェックするクラス。
 * 
 * @author Koichi Asano
 *
 */
public class NumberCharValidator extends CharacterLimitationValidator<NumberChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return NumberChar.class;
    }

    @Override
    protected boolean isValid(NumberChar annotation, String value) {
        return AsciiCharacterChecker.checkNumberCharOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(NumberChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public NumberChar createAnnotation(final Map<String, Object> params) {
        return new NumberChar() {
            public Class<? extends Annotation> annotationType() {
                return NumberChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }};
    }
}
