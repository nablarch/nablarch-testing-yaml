import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import nablarch.test.core.reader.PoiXlsReader;

/**
 * PoiXlsReader が返す List<String> の要素が、空セルで空文字か Java null かを実測する。
 */
public class ExcelProbe {

    static long totalSheets = 0;
    static long totalRawLines = 0;
    static long totalCells = 0;
    static long nullCells = 0;
    static long emptyCells = 0;
    static long allEmptyRawLines = 0;
    static List<String> nullSamples = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        List<File> books = new ArrayList<File>();
        collect(root, books);
        Collections.sort(books);
        System.out.println("book count = " + books.size());

        Method readOneLine = PoiXlsReader.class.getDeclaredMethod("readOneLine");
        readOneLine.setAccessible(true);

        for (File book : books) {
            String name = book.getName();
            int dot = name.lastIndexOf('.');
            String base = name.substring(0, dot);
            List<String> sheets = new ArrayList<String>();
            FileInputStream in = new FileInputStream(book);
            try {
                Workbook wb = WorkbookFactory.create(in);
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    sheets.add(wb.getSheetName(i));
                }
            } catch (Exception e) {
                System.out.println("SKIP(open failed) " + book + " : " + e);
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
                    System.out.println("SKIP(sheet open failed) " + book + "#" + sheet + " : " + e);
                    continue;
                }
                totalSheets++;
                while (true) {
                    @SuppressWarnings("unchecked")
                    List<String> line = (List<String>) readOneLine.invoke(reader);
                    if (line == null) {
                        break;
                    }
                    totalRawLines++;
                    boolean allEmpty = true;
                    for (int i = 0; i < line.size(); i++) {
                        String e = line.get(i);
                        totalCells++;
                        if (e == null) {
                            nullCells++;
                            if (nullSamples.size() < 20) {
                                nullSamples.add(book.getName() + "#" + sheet + " row(raw#" + totalRawLines + ") col" + i);
                            }
                        } else if (e.isEmpty()) {
                            emptyCells++;
                        } else {
                            allEmpty = false;
                        }
                    }
                    if (allEmpty) {
                        allEmptyRawLines++;
                    }
                }
                reader.close();
            }
        }
        System.out.println("=== PoiXlsReader#readOneLine 全走査結果 ===");
        System.out.println("sheets            = " + totalSheets);
        System.out.println("raw lines         = " + totalRawLines);
        System.out.println("cells             = " + totalCells);
        System.out.println("Java null cells   = " + nullCells);
        System.out.println("empty(\"\") cells   = " + emptyCells);
        System.out.println("all-empty rawlines= " + allEmptyRawLines + "  (readLine がスキップする行)");
        System.out.println("null samples      = " + nullSamples);
    }

    static void collect(File dir, List<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) {
            return;
        }
        for (File f : fs) {
            if (f.isDirectory()) {
                collect(f, out);
            } else {
                String n = f.getName().toLowerCase(Locale.ENGLISH);
                if (n.endsWith(".xls") || n.endsWith(".xlsx")) {
                    out.add(f);
                }
            }
        }
    }
}
