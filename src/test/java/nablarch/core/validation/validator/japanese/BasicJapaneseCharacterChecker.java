package nablarch.core.validation.validator.japanese;

import java.util.BitSet;

import nablarch.core.repository.initialization.Initializable;
import nablarch.core.util.JapaneseCharsetUtil;

/**
 * JapaneseCharacterCheckerインタフェースの基本実装クラス。
 * 
 * @author Koichi Asano
 */
public class BasicJapaneseCharacterChecker implements JapaneseCharacterChecker, Initializable {
    /**
     * 半角カナの集合。
     */
    private BitSet hankakuKanaCharset;

    /**
     * 半角文字の集合。
     */
    private BitSet hankakuCharset;

    /**
     * 全角文字の集合。<br/>
     * 全角文字には下記が含まれる。
     * <ul>
     *  <li>JIS第1水準に含まれる全角英字</li>
     *  <li>JIS第1水準に含まれる全角数字</li>
     *  <li>JIS第1水準に含まれる全角ギリシャ文字</li>
     *  <li>JIS第1水準に含まれる全角ロシア文字</li>
     *  <li>JIS第1水準に含まれる全角ひらがな</li>
     *  <li>JIS第1水準に含まれる全角カタカナ</li>
     *  <li>JIS第1水準に含まれる全角記号</li>
     *  <li>JIS第1水準に含まれる全角罫線</li>
     *  <li>JIS第1水準に含まれる漢字</li>
     *  <li>JIS第2水準に含まれる漢字</li>
     * </ul>
     */
    private BitSet zenkakuCharset;

    /**
     * 全角ひらがなの集合。
     */
    private BitSet zenkakuHiraganaCharset;
    /**
     * 全角カタカナの集合。
     */
    private BitSet zenkakuKatakanaCharset;

    /**
     * 外字を含む全角文字の集合。<br/>
     * この集合にはZENKAKU_CHAR_SETと同じ全角文字に加えてシステムリポジトリに
     * 登録されたGaijiCharDefinitionの定義による外字が含まれる。
     */
    private BitSet zenkakuCharAndGaijiCharSet;

    /**
     * 外字の定義。
     */
    private CharacterDefinition gaijiCharacterDefinition = new BasicGaijiCharacterDefinition();

    /**
     * 外字の定義を設定する。
     * 
     * @param gaijiCharacterDefinition 外字の定義
     */
    public void setGaijiCharacterDefinition(CharacterDefinition gaijiCharacterDefinition) {
        this.gaijiCharacterDefinition = gaijiCharacterDefinition;
    }

    /**
     * {@inheritDoc}
     */
    public boolean checkHankakuCharOnly(String value) {
        return nablarch.core.util.CharacterCheckerUtil.checkValidCharOnly(hankakuCharset, value);
    }

    /**
     * {@inheritDoc} <br/>
     * 
     * 半角文字として判定する文字は下記の通り。
     * 
     * <table border>
     * <tr><td>
     * ｡｢｣､･ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ
     * </td></tr>
     * </table>
     */
    public boolean checkHankakuKanaOnly(String value) {
        return nablarch.core.util.CharacterCheckerUtil.checkValidCharOnly(hankakuKanaCharset, value);
    }

    /**
     * {@inheritDoc}<br/>
     * 
     * 全角文字には下記が含まれる。
     * <ul>
     *  <li>JIS第1水準に含まれる全角英字</li>
     *  <li>JIS第1水準に含まれる全角数字</li>
     *  <li>JIS第1水準に含まれる全角ギリシャ文字</li>
     *  <li>JIS第1水準に含まれる全角ロシア文字</li>
     *  <li>JIS第1水準に含まれる全角ひらがな</li>
     *  <li>JIS第1水準に含まれる全角カタカナ</li>
     *  <li>JIS第1水準に含まれる全角記号</li>
     *  <li>JIS第1水準に含まれる全角罫線</li>
     *  <li>JIS第1水準に含まれる漢字</li>
     *  <li>JIS第2水準に含まれる漢字</li>
     * </ul>
     * 
     * 外字は、SystemReqpositoryにコンポーネント名"gaiji.chardefine"で登録された
     * GaijiCharDefinitionインタフェースを実装したクラスの定義に従う。
     */
    public boolean checkZenkakuCharAndGaijiCharOnly(String value) {
        return nablarch.core.util.CharacterCheckerUtil.checkValidCharOnly(zenkakuCharAndGaijiCharSet, value);
    }

    /**
     * {@inheritDoc}<br/>
     * 全角文字には下記が含まれる。
     * <ul>
     *  <li>JIS第1水準に含まれる全角英字</li>
     *  <li>JIS第1水準に含まれる全角数字</li>
     *  <li>JIS第1水準に含まれる全角ギリシャ文字</li>
     *  <li>JIS第1水準に含まれる全角ロシア文字</li>
     *  <li>JIS第1水準に含まれる全角ひらがな</li>
     *  <li>JIS第1水準に含まれる全角カタカナ</li>
     *  <li>JIS第1水準に含まれる全角記号</li>
     *  <li>JIS第1水準に含まれる全角罫線</li>
     *  <li>JIS第1水準に含まれる漢字</li>
     *  <li>JIS第2水準に含まれる漢字</li>
     * </ul>
     */
    public boolean checkZenkakuCharOnly(String value) {
        return nablarch.core.util.CharacterCheckerUtil.checkValidCharOnly(zenkakuCharset, value);
    }

    /**
     * {@inheritDoc}
     * この実装では、「全角ひらがな」の範囲をJISの「全角ひらがな」の範囲に加えて、全角長音「ー」も許可する。
     */
    public boolean checkZenkakuHiraganaCharOnly(String value) {
        return nablarch.core.util.CharacterCheckerUtil.checkValidCharOnly(zenkakuHiraganaCharset, value);
    }

    /**
     * {@inheritDoc}
     * この実装では、「全角カタカナ」の範囲をJISの「全角カタカナ」の範囲に加えて、全角長音「ー」も許可する。
     */
    public boolean checkZenkakuKatakanaCharOnly(String value) {
        return nablarch.core.util.CharacterCheckerUtil.checkValidCharOnly(zenkakuKatakanaCharset, value);
    }

    /**
     * {@inheritDoc}<br/>
     * ビットセットを全て初期化する。
     */
    public void initialize() {

        String gaiji;
        if (gaijiCharacterDefinition != null) {
            gaiji = gaijiCharacterDefinition.validCharacters();
        } else {
            gaiji = "";
        }
        hankakuCharset = nablarch.core.util.CharacterCheckerUtil.createCharSet(JapaneseCharsetUtil.getAsciiChars(), JapaneseCharsetUtil.getHankakuKanaChars());
        zenkakuCharset 
            = nablarch.core.util.CharacterCheckerUtil.createCharSet(JapaneseCharsetUtil.getZenkakuAlphaChars()
                    , JapaneseCharsetUtil.getZenkakuNumChars()
                    , JapaneseCharsetUtil.getZenkakuGreekChars()
                    , JapaneseCharsetUtil.getZenkakuRussianChars()
                    , JapaneseCharsetUtil.getZenkakuHiraganaChars()
                    , JapaneseCharsetUtil.getZenkakuKatakanaChars()
                    , JapaneseCharsetUtil.getZenkakuKeisenChars()
                    , JapaneseCharsetUtil.getLevel1Kanji()
                    , JapaneseCharsetUtil.getLevel2Kanji()
                    , JapaneseCharsetUtil.getJisSymbolChars()
                    );
        hankakuKanaCharset = nablarch.core.util.CharacterCheckerUtil.createCharSet(JapaneseCharsetUtil.getHankakuKanaChars());
        zenkakuHiraganaCharset = nablarch.core.util.CharacterCheckerUtil.createCharSet(
                JapaneseCharsetUtil.getZenkakuHiraganaChars(),
                // 全角長音は許可する。
                "ー"
                );
        zenkakuKatakanaCharset = nablarch.core.util.CharacterCheckerUtil.createCharSet(
                JapaneseCharsetUtil.getZenkakuKatakanaChars(),
                // 全角長音は許可する。
                "ー"
                );
        zenkakuCharAndGaijiCharSet = nablarch.core.util.CharacterCheckerUtil.createCharSet(JapaneseCharsetUtil.getZenkakuAlphaChars()
                , JapaneseCharsetUtil.getZenkakuNumChars()
                , JapaneseCharsetUtil.getZenkakuGreekChars()
                , JapaneseCharsetUtil.getZenkakuRussianChars()
                , JapaneseCharsetUtil.getZenkakuHiraganaChars()
                , JapaneseCharsetUtil.getZenkakuKatakanaChars()
                , JapaneseCharsetUtil.getZenkakuKeisenChars()
                , JapaneseCharsetUtil.getLevel1Kanji()
                , JapaneseCharsetUtil.getLevel2Kanji()
                , JapaneseCharsetUtil.getJisSymbolChars()
                , gaiji
                );
    }
}
