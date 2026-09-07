package nablarch.core.validation.validator.japanese;

/**
 * 日本語の文字チェックを行うインタフェース。
 * 
 * @author Koichi Asano
 */
public interface JapaneseCharacterChecker {

    /**
     * 文字列が半角文字のみからなるかチェックする。<br/>
     * このメソッドでは下記文字を有効な文字とみなす。
     * <ul>
     *   <li>全てのascii文字</li>
     *   <li>全ての半角カナ文字</li>
     * </ul>
     * 
     * @param value チェック対象の文字列
     * @return チェック対象の文字列が全て半角文字からなる場合true
     */
    boolean checkHankakuCharOnly(String value);

    /**
     * 文字列が半角カナ文字のみからなるかチェックする。<br/>
     * 
     * @param value チェック対象の文字列
     * @return チェック対象の文字列が全て半角カナ文字からなる場合true
     */
    boolean checkHankakuKanaOnly(String value);

    /**
     * 文字列が全角文字のみからなるかチェックする。<br/>
     * 
     * @param value チェック対象の文字列
     * @return チェック対象の文字列が全て全角文字からなる場合true
     */
    boolean checkZenkakuCharOnly(String value);

    /**
     * 文字列が全角ひらがなのみからなるかチェックする。<br/>
     * @param value チェック対象の文字列
     * @return チェック対象の文字列が全て全角ひらがなからなる場合true
     */
    boolean checkZenkakuHiraganaCharOnly(String value);

    /**
     * 文字列が全角カタカナのみからなるかチェックする。<br/>
     * @param value チェック対象の文字列
     * @return チェック対象の文字列が全て全角カタカナからなる場合true
     */
    boolean checkZenkakuKatakanaCharOnly(String value);

    /**
     * 文字列が全角文字および外字のみからなるかチェックする。<br/>
     * 
     * @param value チェック対象の文字列
     * @return チェック対象の文字列が全て半角文字からなる場合true
     */
    boolean checkZenkakuCharAndGaijiCharOnly(String value);

}
