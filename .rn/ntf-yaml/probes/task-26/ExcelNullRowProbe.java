import java.lang.reflect.*;
import java.util.*;
import nablarch.test.core.reader.TestDataReader;
import nablarch.test.core.util.interpreter.NullInterpreter;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

/**
 * Excel 側: 全セルが文字列 "null" の行が readTestData を通ると
 * 「行としては残り、値は Java null になる」ことを実測する。
 * リーダは PoiXlsReader が返すのと同じ形（空セル = 空文字、Java null なし）を模したスタブ。
 */
public class ExcelNullRowProbe {

    static class StubReader implements TestDataReader {
        private final Iterator<List<String>> it;
        StubReader(List<List<String>> lines) { this.it = lines.iterator(); }
        public void open(String p, String d) { }
        public void close() { }
        public List<String> readLine() { return it.hasNext() ? it.next() : null; }
        public boolean isResourceExisting(String b, String r) { return true; }
        public boolean isDataExisting(String b, String r) { return true; }
    }

    public static void main(String[] args) throws Exception {
        // PoiXlsReader#readLine が返す形。全セル空("")の行は PoiXlsReader 側で既に落ちるのでここには来ない。
        List<List<String>> lines = new ArrayList<List<String>>();
        lines.add(Arrays.asList("USER_ID", "USER_NAME"));
        lines.add(Arrays.asList("null", "null"));    // 全セルが文字列 null
        lines.add(Arrays.asList("", ""));            // 全セル空（PoiXlsReader なら来ないが念のため）
        lines.add(Arrays.asList("1", "a"));

        Class<?> lmp = Class.forName("nablarch.test.core.reader.ListMapParser");
        Constructor<?> ctor = lmp.getDeclaredConstructor(TestDataReader.class, List.class);
        ctor.setAccessible(true);
        Object parser = ctor.newInstance(new StubReader(lines),
                Arrays.<TestDataInterpreter>asList(new NullInterpreter()));

        Class<?> tmpl = Class.forName("nablarch.test.core.reader.TestDataParsingTemplate");
        Method readTestData = tmpl.getDeclaredMethod("readTestData");
        readTestData.setAccessible(true);
        Field readerField = tmpl.getDeclaredField("reader");
        readerField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<List<String>> out = (List<List<String>>) readTestData.invoke(parser);
        System.out.println("入力行数 = " + lines.size() + " -> readTestData 出力行数 = " + out.size());
        for (List<String> l : out) {
            StringBuilder sb = new StringBuilder("  [");
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(l.get(i) == null ? "<Java null>" : "\"" + l.get(i) + "\"");
            }
            System.out.println(sb.append("]").toString());
        }
    }
}
