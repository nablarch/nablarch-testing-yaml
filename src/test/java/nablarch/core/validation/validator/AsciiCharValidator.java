package nablarch.core.validation.validator;

import java.lang.annotation.Annotation;
import java.util.Map;

import nablarch.core.validation.validator.AsciiChar;

/**
 * Ascii文字のみからなる文字列であるかをチェックするクラス。<br/>
 * 
 * 
 * @author Koichi Asano
 *
 */
public class AsciiCharValidator extends CharacterLimitationValidator<AsciiChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return AsciiChar.class;
    }

    @Override
    protected boolean isValid(AsciiChar annotation, String value) {
        return AsciiCharacterChecker.checkAsciiCharOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(AsciiChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public AsciiChar createAnnotation(final Map<String, Object> params) {
        return new AsciiChar() {
            public Class<? extends Annotation> annotationType() {
                return AsciiChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }};
    }
}
