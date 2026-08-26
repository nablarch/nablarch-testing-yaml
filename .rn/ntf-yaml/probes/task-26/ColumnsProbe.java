import java.util.*;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import nablarch.test.core.reader.yaml.YamlSection;

/** dropBlankRows の変更が resolveColumns に及ぼす影響を実測する。 */
public class ColumnsProbe {
    public static void main(String[] a) {
        run("先頭行が全 Java null（値省略）",
            "rows:\n" +
            "  - USER_ID:\n" +
            "    USER_NAME:\n" +
            "    STATUS:\n" +
            "  - {USER_ID: \"1\"}\n");

        run("先頭行が全 Java null（アンクォート null）",
            "rows:\n" +
            "  - {USER_ID: null, USER_NAME: null, STATUS: null}\n" +
            "  - {USER_ID: \"1\"}\n");

        run("先頭行が全 空文字（落ちる。挙動不変）",
            "rows:\n" +
            "  - {USER_ID: \"\", USER_NAME: \"\", STATUS: \"\"}\n" +
            "  - {USER_ID: \"1\"}\n");

        run("先頭行が空マッピング（落ちる。挙動不変）",
            "rows:\n" +
            "  - {}\n" +
            "  - {USER_ID: \"1\", USER_NAME: \"n\"}\n");
    }

    static void run(String title, String yaml) {
        LoadSettings s = LoadSettings.builder().setAllowDuplicateKeys(false).build();
        Map<String, Object> root = (Map<String, Object>) new Load(s).loadFromString(yaml);
        List<Object> rows = YamlSection.getList(root, "rows");

        List<Object> cur = YamlSection.dropBlankRows(rows);
        List<Object> pro = new ArrayList<Object>();
        for (Object r : rows) {
            if (!proposedIsBlankRow(r)) pro.add(r);
        }
        System.out.println("--- " + title + " ---");
        System.out.println("  parsed rows          = " + rows);
        System.out.println("  現行 kept            = " + cur + " / columns=" + YamlSection.resolveColumns(cur));
        System.out.println("  案   kept            = " + pro + " / columns=" + YamlSection.resolveColumns(pro));
        System.out.println();
    }

    static boolean proposedIsBlankRow(Object row) {
        Map<String, Object> map = YamlSection.castMap(row);
        if (map.isEmpty()) return true;
        for (Object v : map.values()) {
            String str = YamlSection.objectToString(v);
            if (str == null || !str.isEmpty()) return false;
        }
        return true;
    }
}
