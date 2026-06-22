package nablarch.core.validation.validator;

import java.lang.annotation.Annotation;
import java.util.Map;

import nablarch.core.validation.validator.AlnumChar;

/**
 * 英数字のみからなる文字列であるかをチェックするクラス。
 * 
 * @author Koichi Asano
 *
 */
public class AlnumCharValidator extends CharacterLimitationValidator<AlnumChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return AlnumChar.class;
    }

    @Override
    protected boolean isValid(AlnumChar annotation, String value) {
        return AsciiCharacterChecker.checkAlnumCharOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(AlnumChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public AlnumChar createAnnotation(final Map<String, Object> params) {
        return new AlnumChar() {
            public Class<? extends Annotation> annotationType() {
                return AlnumChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }};
    }
}
