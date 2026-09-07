package nablarch.core.validation.validator.japanese;

import nablarch.core.util.JapaneseCharsetUtil;


/**
 * 頻繁に使われる外字の定義を取得するクラス。<br/>
 * 下記の文字を外字として許容する。
 * <ul>
 * <li> JIS第13区のNEC特殊文字 </li>
 * <li> NEC選定IBM拡張文字 </li>
 * <li> IBM拡張文字 </li>
 * </ul>
 *
 * @author Koichi Asano
 */
public class BasicGaijiCharacterDefinition implements CharacterDefinition {

    /** {@inheritDoc} */
    public String validCharacters() {
        return JapaneseCharsetUtil.getNecExtendedChars() 
                + JapaneseCharsetUtil.getJisSymbolChars()
                + JapaneseCharsetUtil.getNecSymbolChars()
                + JapaneseCharsetUtil.getIbmExtendedChars();
    }

}
