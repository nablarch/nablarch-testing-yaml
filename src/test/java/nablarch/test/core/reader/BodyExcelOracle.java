package nablarch.test.core.reader;

import nablarch.test.core.util.interpreter.TestDataInterpreter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 本体（{@code nablarch-testing}）の Excel 経路を「正解（oracle）」として使うためのテスト補助クラス。
 *
 * <p>
 * YAML 経路の挙動が本体と一致することを確かめるには、同じ意味のテストデータを本体に読ませて
 * 突き合わせるのが最も確実である。本クラスは、その本体側の入力となる {@code .xlsx} を
 * POI で組み立てて書き出し、本体の {@link BasicTestDataParser}（{@link PoiXlsReader} 経由）で
 * 読み込めるようにする。規則を手写ししないため、期待値は本体の実行結果そのものになる。
 * </p>
 * <p>
 * 使い方は「{@link #row(String, String...)} でシートに行を積む → {@link #write()} で書き出す →
 * {@link #parser()} と {@link #dir()}／{@link #resource(String)} で本体パーサに読ませる」である。
 * シート 1 枚が本体の 1 テストデータ（データタイプ行 + ディレクティブ行 + レコード定義 + データ行）に対応する。
 * </p>
 * <p>
 * 本体の {@link PoiXlsReader} は「すべて文字列書式」を前提とするため、本クラスは全セルを
 * 文字列として書き出す。
 * </p>
 * <p>
 * 本体側には {@code TestDataParsingTemplate} のテストデータキャッシュ・{@link PoiXlsReader} の
 * ブックキャッシュ・{@code DataFileParser} の解析結果キャッシュがあり、いずれもキーに
 * ディレクトリ名・リソース名を含む静的キャッシュである。テスト間で結果が混ざらないよう、
 * ブック名とシート名はテストごとに一意にすること。
 * </p>
 * <p>
 * 出力先は {@code target/} 配下（{@link #OUTPUT_DIR}）である。{@code mvn clean} で消えるため、
 * バイナリがリポジトリに残らない。
 * </p>
 *
 * @author kiyotis
 */
public final class BodyExcelOracle {

    /** oracle 用 {@code .xlsx} の出力先ディレクトリ。 */
    private static final String OUTPUT_DIR = "target/test-oracle";

    /** ブック名（拡張子なし）。本体のリソース名の前半になる。 */
    private final String bookName;

    /** 組み立て中のブック。 */
    private final Workbook book = new XSSFWorkbook();

    /** 本体のテストデータパーサ。 */
    private final BasicTestDataParser parser;

    /**
     * コンストラクタ。
     *
     * @param bookName     ブック名（拡張子なし）。テストごとに一意にすること
     * @param interpreters 本体の Excel 経路で使うインタープリタリスト
     *                     （{@code NullInterpreter}・{@code QuotationTrimmer}・
     *                     {@code LineSeparatorInterpreter} を含む {@code interpreters}）
     */
    public BodyExcelOracle(String bookName, List<TestDataInterpreter> interpreters) {
        this.bookName = bookName;
        parser = new BasicTestDataParser();
        parser.setTestDataReader(new PoiXlsReader());
        parser.setInterpreters(interpreters);
    }

    /**
     * シートの末尾に 1 行追加する。
     *
     * <p>シートが存在しない場合は新たに作成する。セルはすべて文字列書式で書き出す。</p>
     *
     * @param sheetName シート名
     * @param cells     列 0 から順に並べたセルの値
     * @return このインスタンス自身
     */
    public BodyExcelOracle row(String sheetName, String... cells) {
        Sheet sheet = book.getSheet(sheetName);
        if (sheet == null) {
            sheet = book.createSheet(sheetName);
        }
        Row row = sheet.createRow(sheet.getPhysicalNumberOfRows());
        for (int i = 0; i < cells.length; i++) {
            row.createCell(i).setCellValue(cells[i]);
        }
        return this;
    }

    /**
     * 組み立てたブックを {@code target/test-oracle/<ブック名>.xlsx} へ書き出す。
     *
     * @return このインスタンス自身
     */
    public BodyExcelOracle write() {
        File dir = new File(OUTPUT_DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IllegalStateException("can't create directory. " + dir.getAbsolutePath());
        }
        File file = new File(dir, bookName + ".xlsx");
        try {
            OutputStream out = new FileOutputStream(file);
            try {
                book.write(out);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("can't write oracle book. " + file.getAbsolutePath(), e);
        }
        return this;
    }

    /**
     * 本体のテストデータパーサを返す。
     *
     * @return 本体のテストデータパーサ
     */
    public BasicTestDataParser parser() {
        return parser;
    }

    /**
     * 本体パーサに渡す取得元ディレクトリを返す。
     *
     * @return 取得元ディレクトリ
     */
    public String dir() {
        return OUTPUT_DIR;
    }

    /**
     * 本体パーサに渡すリソース名（{@code ブック名/シート名}）を返す。
     *
     * @param sheetName シート名
     * @return リソース名
     */
    public String resource(String sheetName) {
        return bookName + '/' + sheetName;
    }
}
