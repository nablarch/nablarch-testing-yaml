package nablarch.core.validation.validator.japanese;

import java.lang.annotation.Annotation;
import java.util.Map;

import nablarch.core.validation.validator.japanese.HankakuKanaChar;


/**
 * 半角カナ文字のみからなる文字列であるかをチェックするクラス。
 * 
 * @author Koichi Asano
 *
 */
public class HankakuKanaCharValidator extends JapaneseCharacterValidatorSupport<HankakuKanaChar> {

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> getAnnotationClass() {
        return HankakuKanaChar.class;
    }

    @Override
    protected boolean isValid(HankakuKanaChar annotation, String value) {
        return getJapaneseCharacterChecker().checkHankakuKanaOnly(value);
    }

    @Override
    protected String getMessageIdFromAnnotation(HankakuKanaChar annotation) {
        return annotation.messageId();
    }
    
    @Override
    public HankakuKanaChar createAnnotation(final Map<String, Object> params) {
        return new HankakuKanaChar() {
            public Class<? extends Annotation> annotationType() {
                return HankakuKanaChar.class;
            }

            public String messageId() {
                String messageId = (String) params.get("messageId");
                return (messageId == null) ? ""
                                           : messageId;
            }};
    }
}
