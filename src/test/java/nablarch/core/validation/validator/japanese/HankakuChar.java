package nablarch.core.validation.validator.japanese;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nablarch.core.validation.Validation;


/**
 * 半角文字のみからなる文字列であることを表すアノテーション。<br/>
 * 
 * 半角文字には以下に示す文字が含まれる。
 * <ul>
 *   <li>全てのascii文字</li>
 *   <li>全ての半角カナ文字</li>
 * </ul>
 * 
 * @author Koichi Asano
 *
 */
@Validation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HankakuChar {
    /**
     * 半角文字以外が含まれた場合に出力するエラーメッセージ。
     */
    String messageId() default "";
}
