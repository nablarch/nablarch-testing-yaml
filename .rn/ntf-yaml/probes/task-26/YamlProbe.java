import java.util.*;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import nablarch.test.core.reader.yaml.YamlSection;

/** SnakeYAML が各記法をどうパースするか、および現行 dropBlankRows/resolveColumns の挙動を実測する。 */
public class YamlProbe {
    public static void main(String[] a) {
        String yaml =
            "rows:\n" +
            "  - {}\n" +                                     // (a) 空マッピング
            "  - {USER_ID: \"\", USER_NAME: \"\"}\n" +       // (b) 全て空文字
            "  - {USER_ID: null, USER_NAME: null}\n" +       // (c1) アンクォート null
            "  - USER_ID:\n" +
            "    USER_NAME:\n" +                             // (c2) キーのみ・値省略
            "  - {USER_ID: ~}\n" +                           // (c3) チルダ
            "  - {USER_ID: \"null\", USER_NAME: x}\n" +      // 文字列 "null"
            "  - plain scalar\n" +                           // マッピングでない行
            "  - {USER_ID: \"1\", USER_NAME: \"a\"}\n";      // 通常行

        LoadSettings settings = LoadSettings.builder().setAllowDuplicateKeys(false).build();
        Load loader = new Load(settings);
        Map<String, Object> root = (Map<String, Object>) loader.loadFromString(yaml);
        List<Object> rows = YamlSection.getList(root, "rows");

        System.out.println("=== SnakeYAML パース結果（行ごと） ===");
        for (int i = 0; i < rows.size(); i++) {
            Object row = rows.get(i);
            System.out.println("row[" + i + "] class=" + (row == null ? "null" : row.getClass().getName())
                    + " value=" + row);
            Map<String, Object> m = YamlSection.castMap(row);
            for (Map.Entry<String, Object> e : m.entrySet()) {
                Object v = e.getValue();
                System.out.println("        key=" + e.getKey()
                        + " valueClass=" + (v == null ? "Java null" : v.getClass().getName())
                        + " objectToString=" + describe(YamlSection.objectToString(v)));
            }
        }

        System.out.println();
        System.out.println("=== 現行 dropBlankRows の結果 ===");
        List<Object> kept = YamlSection.dropBlankRows(rows);
        System.out.println("入力 " + rows.size() + " 行 -> 残り " + kept.size() + " 行");
        for (Object r : kept) {
            System.out.println("  kept: " + r);
        }
        System.out.println("resolveColumns(dropBlankRows後) = " + YamlSection.resolveColumns(kept));
        System.out.println("resolveColumns(drop前)          = " + YamlSection.resolveColumns(rows));

        System.out.println();
        System.out.println("=== 提案する判別条件（案）を同じ入力に当てた場合 ===");
        for (int i = 0; i < rows.size(); i++) {
            System.out.println("row[" + i + "] proposedIsBlankRow=" + proposedIsBlankRow(rows.get(i))
                    + "  <- " + rows.get(i));
        }
    }

    /** 案: 値が1つも無い行、または全ての値が「Java null でない空文字」の行だけを落とす。 */
    static boolean proposedIsBlankRow(Object row) {
        Map<String, Object> map = YamlSection.castMap(row);
        if (map.isEmpty()) {
            return true;
        }
        for (Object value : map.values()) {
            String str = YamlSection.objectToString(value);
            if (str == null || !str.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static String describe(String s) {
        return s == null ? "Java null" : "\"" + s + "\"";
    }
}
