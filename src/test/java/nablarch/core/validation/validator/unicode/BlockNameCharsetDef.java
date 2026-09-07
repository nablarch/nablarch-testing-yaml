package nablarch.core.validation.validator.unicode;

import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Character.UnicodeBlock}の名称による許容文字集合定義クラス。<br/>
 * 許容したいコードポイントの範囲が{@link Character.UnicodeBlock}に定数定義されている場合、
 * 本クラスにブロック名称を列挙することで簡便に許容文字集合を定義できる。
 * <p/>
 * 例えば、ひらがな(U+3040～U+309F)とカタカナ(U+30A0～U+30FF)を定義したい場合、
 * 以下のようにプロパティを設定する。
 * <pre>
 * {@code
 * BlockNameCharsetDef kana = new BlockNameCharsetDef();
 * kana.setBlockNames(Arrays.asList("HIRAGANA", "KATAKANA"));
 * }
 * </pre>
 * <p/>
 * コンポーネント設定ファイルに定義する場合、以下の記述が等価となる。
 * <pre>
 * {@literal
 * <component name="kana" class="nablarch.core.validation.validator.unicode.BlockNameCharsetDef">
 *     <property name="blockNames">
 *         <list>
 *             <value>HIRAGANA</value>
 *             <value>KATAKANA</value>
 *         </list>
 *     </property>
 * </component>
 * }
 * </pre>
 * <p>
 * 実行例を以下に示す。
 * <pre>
 * {@code
 * kana.contains("あア"); // -> true
 * kana.contains("ab");     // -> false
 * }
 * </p>
 * <p>
 * {@link #contains(int)}にて、与えられたコードポイントが許容ブロックに含まれるか否かの判定処理は、
 * コードブロック名の設定順に行われる。上記の例で言うと、ひらがな、カタカナの順で判定が行われる。
 * よって、そのシステムで出現頻度が高いブロックを先頭に配置することで処理性能が向上する。
 * </p>
 *
 * @author T.Kawasaki
 * @see Character.UnicodeBlock
 */
public class BlockNameCharsetDef extends CharsetDefSupport {

    /** 許容するUnicodeブロック */
    private List<UnicodeBlock> unicodeBlocks;

    /** {@inheritDoc} */
    public boolean contains(int codePoint) {
        for (UnicodeBlock e : unicodeBlocks) {
            if (contains(codePoint, e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * コードポイントが指定したブロックに含まれるか判定する。
     *
     * @param codePoint コードポイント
     * @param expected  ブロック
     * @return コードポイントが指定したブロックに含まれる場合、真
     */
    private boolean contains(int codePoint, UnicodeBlock expected) {
        UnicodeBlock actual = UnicodeBlock.of(codePoint);
        return actual.equals(expected);
    }

    /**
     * ブロック名称一覧を設定する。
     *
     * @param blockNames ブロック名称一覧
     * @throws IllegalArgumentException 不正なブロック名称が含まれている場合
     */
    public void setBlockNames(List<String> blockNames) throws IllegalArgumentException {
        this.unicodeBlocks = toUnicodeBlocks(blockNames);
    }

    /**
     * ブロック名称一覧から{@link UnicodeBlock}の一覧に変換する。
     *
     * @param blockNames ブロック名称一覧
     * @return {@link UnicodeBlock}の一覧
     * @throws IllegalArgumentException 不正なブロック名称が含まれている場合
     */
    private List<UnicodeBlock> toUnicodeBlocks(List<String> blockNames) throws IllegalArgumentException {
        List<UnicodeBlock> blocks = new ArrayList<UnicodeBlock>(blockNames.size());
        for (String name : blockNames) {
            blocks.add(forName(name));
        }
        return blocks;
    }

    /**
     * ブロック名称から{@link UnicodeBlock}へ変換する。
     *
     * @param blockName ブロック名称
     * @return 名称に対応する {@link UnicodeBlock}
     * @throws IllegalArgumentException ブロック名称が不正な場合
     * @see UnicodeBlock#forName(String)
     */
    private UnicodeBlock forName(String blockName) throws IllegalArgumentException {
        try {
            return UnicodeBlock.forName(blockName);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "specified unicode block name was invalid. name=[" + blockName + "]");
        }
    }
}
