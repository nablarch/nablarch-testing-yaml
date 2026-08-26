import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import nablarch.test.core.reader.PoiXlsReader;
import nablarch.core.util.StringUtil;

/**
 * TestDataParsingTemplate#isBlankLine が Excel 実入力で発火するか、
 * 発火するとき Java null が関与するかを実測する。
 */
public class BlankLineProbe {

    public static void main(String[] args) throws Exception {
        Class<?> tmpl = Class.forName("nablarch.test.core.reader.TestDataParsingTemplate");
        Class<?> lmp = Class.forName("nablarch.test.core.reader.ListMapParser");
        Constructor<?> ctor = lmp.getDeclaredConstructor(
                Class.forName("nablarch.test.core.reader.TestDataReader"), List.class);
        ctor.setAccessible(true);
        Object parser = ctor.newInstance(new PoiXlsReader(), new ArrayList<Object>());

        Method isBlankLine = tmpl.getDeclaredMethod("isBlankLine", List.class);
        isBlankLine.setAccessible(true);
        Method cutComment = tmpl.getDeclaredMethod("cutComment", List.class);
        cutComment.setAccessible(true);
        Method isCommentRow = tmpl.getDeclaredMethod("isCommentRow", List.class);
        isCommentRow.setAccessible(true);

        System.out.println("=== (1) isBlankLine の単体挙動（人工入力） ===");
        System.out.println("isBlankLine([])            = " + isBlankLine.invoke(parser, new ArrayList<String>()));
        System.out.println("isBlankLine([\"\",\"\"])       = " + isBlankLine.invoke(parser, Arrays.asList("", "")));
        System.out.println("isBlankLine([null,null])   = " + isBlankLine.invoke(parser, Arrays.asList((String) null, (String) null)));
        System.out.println("isBlankLine([null,\"a\"])    = " + isBlankLine.invoke(parser, Arrays.asList(null, "a")));
        System.out.println("StringUtil.isNullOrEmpty([null,null]) = " + StringUtil.isNullOrEmpty(Arrays.asList((String) null, (String) null)));

        System.out.println();
        System.out.println("=== (2) readTestData 相当のパイプラインを実 Excel 全件で流す ===");
        File root = new File(args[0]);
        List<File> books = new ArrayList<File>();
        collect(root, books);
        Collections.sort(books);

        long lines = 0, commentRows = 0, blankFired = 0, blankFiredWithNull = 0, cellsSeen = 0, nullSeen = 0;
        List<String> firedSamples = new ArrayList<String>();

        for (File book : books) {
            String name = book.getName();
            String base = name.substring(0, name.lastIndexOf('.'));
            List<String> sheets = new ArrayList<String>();
            FileInputStream in = new FileInputStream(book);
            try {
                Workbook wb = WorkbookFactory.create(in);
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    sheets.add(wb.getSheetName(i));
                }
            } catch (Exception e) {
                continue;
            } finally {
                in.close();
            }
            for (String sheet : sheets) {
                PoiXlsReader reader = new PoiXlsReader();
                reader.setUseCache(false);
                try {
                    reader.open(book.getParent(), base + "/" + sheet);
                } catch (Exception e) {
                    continue;
                }
                List<String> line;
                while ((line = reader.readLine()) != null) {
                    lines++;
                    for (String c : line) {
                        cellsSeen++;
                        if (c == null) {
                            nullSeen++;
                        }
                    }
                    if ((Boolean) isCommentRow.invoke(parser, line)) {
                        commentRows++;
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    List<String> cut = (List<String>) cutComment.invoke(parser, line);
                    if ((Boolean) isBlankLine.invoke(parser, cut)) {
                        blankFired++;
                        boolean hasNull = false;
                        for (String c : cut) {
                            if (c == null) {
                                hasNull = true;
                            }
                        }
                        if (hasNull) {
                            blankFiredWithNull++;
                        }
                        if (firedSamples.size() < 10) {
                            firedSamples.add(book.getName() + "#" + sheet + " raw=" + line + " cut=" + cut);
                        }
                    }
                }
                reader.close();
            }
        }
        System.out.println("readLine 通過行数                       = " + lines);
        System.out.println("うちコメント行(isCommentRow=true)       = " + commentRows);
        System.out.println("readLine 通過セル数                     = " + cellsSeen);
        System.out.println("うち Java null のセル数                 = " + nullSeen);
        System.out.println("isBlankLine が true を返した回数         = " + blankFired);
        System.out.println("うち Java null を含んでいた回数          = " + blankFiredWithNull);
        System.out.println("isBlankLine=true サンプル(最大10件):");
        for (String s : firedSamples) {
            System.out.println("  " + s);
        }
    }

    static void collect(File dir, List<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) collect(f, out);
            else {
                String n = f.getName().toLowerCase(Locale.ENGLISH);
                if (n.endsWith(".xls") || n.endsWith(".xlsx")) out.add(f);
            }
        }
    }
}
