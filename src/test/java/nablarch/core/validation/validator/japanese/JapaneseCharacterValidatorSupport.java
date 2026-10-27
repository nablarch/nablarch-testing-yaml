package nablarch.core.validation.validator.japanese;

import java.lang.annotation.Annotation;

import nablarch.core.validation.validator.CharacterLimitationValidator;

/**
 * 日本語文字をチェックするバリデータの作成を助けるサポートクラス。
 * 
 * @param <A> 対応するアノテーションの型
 * 
 * @author Koichi Asano
 */
public abstract class JapaneseCharacterValidatorSupport<A extends Annotation> extends CharacterLimitationValidator<A> {

    /**
     * 日本語文字のチェッカ。
     */
    private JapaneseCharacterChecker japaneseCharacterChecker;
    
    /**
     * 日本語文字のチェッカを設定する。
     * 
     * @param japaneseCharacterChecker 日本語文字のチェッカ
     */
    public void setJapaneseCharacterChecker(JapaneseCharacterChecker japaneseCharacterChecker) {
        this.japaneseCharacterChecker = japaneseCharacterChecker;
    }

    /**
     * 日本語文字のチェッカを取得する。
     * 
     * @return 日本語文字のチェッカ
     */
    public JapaneseCharacterChecker getJapaneseCharacterChecker() {
        return japaneseCharacterChecker;
    }
}
