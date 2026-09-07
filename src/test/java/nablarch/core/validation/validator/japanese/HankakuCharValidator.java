package nablarch.core.validation.validator.japanese;

import java.lang.annotation.Annotation;
import java.util.Map;

import nablarch.core.validation.validator.japanese.HankakuChar;


/**
 * 半角文字のみからなる文字列であるかをチェックするクラス。
 * 
 * @author Koichi Asano
 *
 */
public class HankakuCharValidator extends JapaneseCharacterValidatorSupport<HankakuChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return HankakuChar.class;
    }

    @Override
    protected boolean isValid(HankakuChar annotation, String value) {
        return getJapaneseCharacterChecker().checkHankakuCharOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(HankakuChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public HankakuChar createAnnotation(final Map<String, Object> params) {
        return new HankakuChar() {
            public Class<? extends Annotation> annotationType() {
                return HankakuChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }};
    }    
}
