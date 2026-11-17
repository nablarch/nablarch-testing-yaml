package nablarch.core.validation.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.validation.Validation;


/**
 * 英字のみからなる文字列であることを表わすアノテーション。
 * 
 * @author Koichi Asano
 *
 */
@Validation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AlphaChar {
    /**
     * 英字以外が含まれた場合に出力するエラーメッセージ。
     */
    String messageId() default "";
}
