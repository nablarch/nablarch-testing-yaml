package nablarch.core.validation.validator.japanese;

/**
 * 有効文字を定義するインタフェース。<br/>
 * このインタフェースに定義されたvalidCharactersメソッドで返す文字列に、
 * システムで有効とする外字を全て含む文字列を作成することでシステムで取り扱える外字が定義できる。
 * 
 * @author Koichi Asano
 *
 */
public interface CharacterDefinition {

    /**
     * 有効とする外字の文字を全て含む文字列を取得する。
     * @return 有効とする外字の文字を全て含む文字列
     */
    String validCharacters();
}
