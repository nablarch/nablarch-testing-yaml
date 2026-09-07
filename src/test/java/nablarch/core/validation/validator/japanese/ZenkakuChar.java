package nablarch.core.validation.validator.japanese;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.validation.Validation;


/**
 * 全角文字のみからなる文字列であることを表すアノテーション。<br/>
 * 文字チェックには{@link JapaneseCharacterCheckerUtil#checkZenkakuCharOnly(String)}を使用する。
 * 
 * @author Koichi Asano
 *
 */
@Validation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZenkakuChar {
    /**
     * 全角文字以外が含まれた場合に出力するエラーメッセージ。
     */
    String messageId() default "";
}
