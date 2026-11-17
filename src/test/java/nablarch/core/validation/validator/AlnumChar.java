package nablarch.core.validation.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.validation.Validation;


/**
 * 英数字のみからなる文字列であることを表わすアノテーション。
 * 
 * @author Koichi Asano
 *
 */
@Validation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AlnumChar {
    /**
     * 英数字以外が含まれた場合に出力するエラーメッセージ。
     */
    String messageId() default "";
}
