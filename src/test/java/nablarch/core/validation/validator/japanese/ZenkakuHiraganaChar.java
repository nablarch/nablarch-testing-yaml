package nablarch.core.validation.validator.japanese;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.validation.Validation;


/**
 * 全角ひらがなのみからなる文字列であることを表すアノテーション。
 * 
 * @author Koichi Asano
 *
 */
@Validation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZenkakuHiraganaChar {
    /**
     * 全角ひらがな以外が含まれた場合に出力するエラーメッセージ。
     */
    String messageId() default "";
}
