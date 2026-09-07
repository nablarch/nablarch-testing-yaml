package nablarch.core.validation.validator;

import java.lang.annotation.Annotation;
import java.util.Map;

import nablarch.core.validation.validator.AlphaChar;

/**
 * 英字のみからなる文字列であるかをチェックするクラス。
 * 
 * @author Koichi Asano
 *
 */
public class AlphaCharValidator extends CharacterLimitationValidator<AlphaChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return AlphaChar.class;
    }

    @Override
    protected boolean isValid(AlphaChar annotation, String value) {
        return AsciiCharacterChecker.checkAlphaCharOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(AlphaChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public AlphaChar createAnnotation(final Map<String, Object> params) {
        return new AlphaChar() {
            public Class<? extends Annotation> annotationType() {
                return AlphaChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }};
    }
}
