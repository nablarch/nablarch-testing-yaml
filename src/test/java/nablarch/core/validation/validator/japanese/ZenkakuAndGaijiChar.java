package nablarch.core.validation.validator.japanese;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.validation.Validation;


/**
 * 全角文字および外字のみからなる文字列であることを表すアノテーション。<br/>
 * 文字チェックには{@link JapaneseCharacterCheckerUtil#checkZenkakuCharAndGaijiCharOnly(String)}を使用する。
 * 
 * @author Koichi Asano
 *
 */
@Validation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZenkakuAndGaijiChar {
    /**
     * 英数字以外が含まれた場合に出力するエラーメッセージ。
     */
    String messageId() default "";
}
