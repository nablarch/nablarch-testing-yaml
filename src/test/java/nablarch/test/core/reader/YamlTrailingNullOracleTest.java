package nablarch.test.core.reader;

import nablarch.core.dataformat.DataRecord;
import nablarch.test.core.db.BasicDefaultValues;
import nablarch.test.core.file.DataFile;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.util.interpreter.TestDataInterpreter;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * データ行の末尾フィールドに {@code null} と記述した場合の扱いを、本体（{@code nablarch-testing}）を
 * 正解（oracle）として突き合わせるテストクラス。
 *
 * <p>
 * 解説書は、末尾のフィールドに {@code null} と記述した場合は形式によらず {@code ""} になり、
 * 後ろに値のあるフィールドがあれば {@code null} のまま保持される、と定めている
 * （出典: {@code implementation/testdata_notation.rst:889}（ファイルデータ）、
 * 同 {@code :1155}（電文））。本体はこの規則を
 * {@code DataFileParser#onReadLine} が {@code NablarchTestUtils.trimTailCopy} を掛けてから
 * {@code DataFileFragment#addValue}（フィールド名称の数まで {@code ""} で埋める）へ渡すことで実現している。
 * </p>
 * <p>
 * 本テストは規則を手写しした期待値ではなく、同じ意味の入力を本体に読ませた実行結果と突き合わせる。
 * 本体側の入力は {@link BodyExcelOracle} が POI で {@code .xlsx} を組み立て、本体の
 * {@link BasicTestDataParser}（{@link PoiXlsReader} と、{@code NullInterpreter}・
 * {@code QuotationTrimmer}・{@code LineSeparatorInterpreter} を含む {@code interpreters}）で読む。
 * YAML 側の入力は {@code YamlTrailingNullOracleTest/trailingNull.yaml} に手書きし、
 * {@link YamlTestDataParser}（{@code yamlInterpreters}）で読む。
 * インタープリタが異なるのは、YAML ではクォート無しの {@code null} と {@code ""} を
 * YAML パーサ自身が Java の {@code null}・空文字にするためで、両者は同じ意味集合を表す
 * （出典: {@code setup/common.rst:77}・{@code :81}）。
 * </p>
 * <p>
 * ファイルデータのケース（F1〜F6）に可変長（{@code VariableLengthFile}）を使うのは、
 * 可変長の {@code DataRecord} 変換が恒等写像であり、値行に詰まった {@code ""} と {@code null} を
 * そのまま観測できるためである。電文（M1・S2）は本体が固定長でしか扱わないため固定長を使う。
 * </p>
 * <p>
 * 是正の前後で結果が変わるのは F1・F4・F6・S2 である。F2・F3・F5・M1 は結果が変わらない対照ケースで、
 * 是正が「末尾の null 以外の扱い」を壊していないことを固定する
 * （M1 が対照になる理由は {@link #getMessage_trailingNullsBecomeEmptyStrings()} の説明を参照）。
 * </p>
 *
 * @author kiyotis
 */
@RunWith(DatabaseTestRunner.class)
public class YamlTrailingNullOracleTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource("unit-test-yaml.xml");

    /** YAML 側テストデータのベースディレクトリ。 */
    private static final String YAML_DIR = "src/test/java/nablarch/test/core/reader/";

    /** YAML 側テストデータのリソース名。 */
    private static final String YAML_RESOURCE = "YamlTrailingNullOracleTest/trailingNull";

    /**
     * oracle 用ブック名。本体側の静的キャッシュ（テストデータ・ブック・解析結果）は
     * ディレクトリ名とリソース名をキーに持つため、他のテストと衝突しない名前にする。
     */
    private static final String ORACLE_BOOK = "YamlTrailingNullOracleTest";

    /** 本体（Excel 経路）の oracle。1 度だけ組み立てて全テストで共有する。 */
    private static BodyExcelOracle oracle;

    private YamlTestDataParser sut;

    @Before
    public void before() {
        if (oracle == null) {
            oracle = buildOracleBook(
                    repositoryResource.<List<TestDataInterpreter>>getComponent("interpreters"));
        }
        sut = new YamlTestDataParser();
        sut.setDbInfo(repositoryResource.getComponent("dbInfo"));
        sut.setDefaultValues(new BasicDefaultValues());
        sut.setInterpreters(
                repositoryResource.<List<TestDataInterpreter>>getComponent("yamlInterpreters"));
    }

    @After
    public void after() {
        YamlTestDataParser.clearCacheForTest();
    }

    /**
     * 本体に読ませる {@code .xlsx} を組み立てて書き出す。
     *
     * <p>
     * シート名はケース名（F1〜F6・M1・S2）と一致させ、{@code trailingNull.yaml} の
     * グループ ID／ID と 1 対 1 に対応させる。
     * </p>
     *
     * @param interpreters 本体の Excel 経路で使うインタープリタリスト
     * @return 書き出し済みの oracle
     */
    private static BodyExcelOracle buildOracleBook(List<TestDataInterpreter> interpreters) {
        BodyExcelOracle book = new BodyExcelOracle(ORACLE_BOOK, interpreters);
        // ファイルデータ（可変長）。データ行の列 0 は制御列であり空にする。
        variableFileSheet(book, "F1", "x", "null", "null");
        variableFileSheet(book, "F2", "x", "null", "y");
        variableFileSheet(book, "F3", "\"\"", "", "");
        variableFileSheet(book, "F4", "null", "null", "null");
        variableFileSheet(book, "F5", "x", "", "");
        variableFileSheet(book, "F6", "x", "\"\"", "null");
        // 電文（固定長）。
        messageSheet(book, "M1", "x", "null", "null");
        // 送信同期電文（固定長）。値行の先頭セルは No 列（連番）である。
        sendSyncSheet(book, "S2", "x", "null", "null");
        return book.write();
    }

    /**
     * 可変長ファイルデータのシートを組み立てる。
     *
     * <p>可変長はフィールド長の行を持たない（出典: {@code implementation/testdata_notation.rst:857}）。</p>
     *
     * @param book   組み立て先
     * @param name   シート名（＝グループ ID）
     * @param values データ行の値（列 1 以降）
     */
    private static void variableFileSheet(BodyExcelOracle book, String name, String... values) {
        book.row(name, "SETUP_VARIABLE[" + name + "]=dummy/" + name + ".csv");
        book.row(name, "text-encoding", "UTF-8");
        book.row(name, "field-separator", ",");
        book.row(name, "DATA", "FIELD1", "FIELD2", "FIELD3");
        book.row(name, "", "半角", "半角", "半角");
        book.row(name, prependControlColumn(values));
    }

    /**
     * 電文（MESSAGE）のシートを組み立てる。
     *
     * @param book   組み立て先
     * @param name   シート名（＝ID）
     * @param values データ行の値（列 1 以降）
     */
    private static void messageSheet(BodyExcelOracle book, String name, String... values) {
        book.row(name, "MESSAGE=" + name);
        book.row(name, "text-encoding", "Windows-31J");
        book.row(name, "BODY", "FIELD1", "FIELD2", "FIELD3");
        book.row(name, "", "半角", "半角", "半角");
        book.row(name, "", "5", "5", "5");
        book.row(name, prependControlColumn(values));
    }

    /**
     * 送信同期電文（RESPONSE_BODY_MESSAGES）のシートを組み立てる。
     *
     * <p>
     * 本体（{@code SendSyncMessageParser}）は値行の先頭セルを No 列（連番）として値から切り離すため、
     * データ行の列 0 には連番を置く。YAML には No 列が存在せず、{@link YamlFileBuilder} が
     * 1 始まりの行インデックスで補う。
     * </p>
     *
     * @param book   組み立て先
     * @param name   シート名（＝グループ ID）
     * @param values データ行の値（列 1 以降）
     */
    private static void sendSyncSheet(BodyExcelOracle book, String name, String... values) {
        book.row(name, "RESPONSE_BODY_MESSAGES[" + name + "]=" + name);
        book.row(name, "text-encoding", "Windows-31J");
        book.row(name, "BODY", "FIELD1", "FIELD2", "FIELD3");
        book.row(name, "", "半角", "半角", "半角");
        book.row(name, "", "5", "5", "5");
        List<String> row = new ArrayList<String>();
        row.add("1");
        for (String value : values) {
            row.add(value);
        }
        book.row(name, row.toArray(new String[row.size()]));
    }

    /** データ行の先頭に制御列（空セル）を足す。 */
    private static String[] prependControlColumn(String... values) {
        String[] row = new String[values.length + 1];
        row[0] = "";
        System.arraycopy(values, 0, row, 1, values.length);
        return row;
    }

    // ========================================================================
    // ファイルデータ（setup_files）
    // ========================================================================

    /**
     * [YamlTestDataParser] getSetupFile: 末尾の 2 フィールドに null と書くと、いずれも "" になること（F1）。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:889}
     * 「末尾のフィールドに {@code null} と記述した場合は、形式によらず {@code ""} になる」。<br>
     * Given: fields 3 件に対し値行 {@code ["x", null, null]}（Excel は {@code x}／{@code null}／{@code null}）<br>
     * When:  YAML と本体（Excel）の双方を読み、{@code toDataRecords()} を比べる<br>
     * Then:  いずれも FIELD1="x"、FIELD2=""、FIELD3="" になること
     * </p>
     */
    @Test
    public void getSetupFile_trailingNullsBecomeEmptyStrings() {
        assertFileCase("F1", "x", "", "");
    }

    /**
     * [YamlTestDataParser] getSetupFile: null の後ろに値のあるフィールドがあれば null のまま保持されること（F2）。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:889}
     * 「後ろに値のあるフィールドがあれば null のまま保持される」。<br>
     * Given: fields 3 件に対し値行 {@code ["x", null, "y"]}<br>
     * When:  YAML と本体（Excel）の双方を読み、{@code toDataRecords()} を比べる<br>
     * Then:  いずれも FIELD1="x"、FIELD2=null、FIELD3="y" になること
     * </p>
     */
    @Test
    public void getSetupFile_nullFollowedByValueIsKept() {
        assertFileCase("F2", "x", null, "y");
    }

    /**
     * [YamlTestDataParser] getSetupFile: 先頭が "" で後続フィールドを書かない場合、全フィールドが "" になること（F3）。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:887}
     * 「いずれか1つのフィールドに {@code ""} と記述した行」で全フィールドが {@code ""} のレコードになる。
     * 本ケースは、本体では「空セル」でも「{@code ""}」でも同じ結果になることを示す対照ケースであり、
     * 是正の前後で結果が変わらない。<br>
     * Given: Excel は {@code ""}／空セル／空セル、YAML は {@code [""]}<br>
     * When:  YAML と本体（Excel）の双方を読み、{@code toDataRecords()} を比べる<br>
     * Then:  いずれも 3 フィールドとも "" になること
     * </p>
     */
    @Test
    public void getSetupFile_emptyStringWithOmittedTrailingFields() {
        assertFileCase("F3", "", "", "");
    }

    /**
     * [YamlTestDataParser] getSetupFile: 全フィールドに null と書くと、全フィールドが "" になること（F4）。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:889}。
     * 全フィールドが null の場合は値行全体が畳まれ、フィールド名称の数だけ "" で埋められる。<br>
     * Given: fields 3 件に対し値行 {@code [null, null, null]}<br>
     * When:  YAML と本体（Excel）の双方を読み、{@code toDataRecords()} を比べる<br>
     * Then:  いずれも 3 フィールドとも "" になること（レコード自体は 1 件残ること）
     * </p>
     */
    @Test
    public void getSetupFile_allNullsBecomeEmptyStrings() {
        assertFileCase("F4", "", "", "");
    }

    /**
     * [YamlTestDataParser] getSetupFile: 後続フィールドの値を書かない場合、それらが "" になること（F5）。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:886}
     * 「末尾のフィールドの値を書かなければ、そのフィールドは {@code ""} として扱われる」。
     * 本ケースは是正の前後で結果が変わらない対照ケースである。<br>
     * Given: fields 3 件に対し値行 {@code ["x"]}<br>
     * When:  YAML と本体（Excel）の双方を読み、{@code toDataRecords()} を比べる<br>
     * Then:  いずれも FIELD1="x"、FIELD2=""、FIELD3="" になること
     * </p>
     */
    @Test
    public void getSetupFile_omittedTrailingFieldsBecomeEmptyStrings() {
        assertFileCase("F5", "x", "", "");
    }

    /**
     * [YamlTestDataParser] getSetupFile: 末尾が "" と null の並びでも、いずれも "" になること（F6）。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:889}。
     * 本体の {@code NablarchTestUtils.trimTail} は末尾から「null または空文字」を連続して取り除くため、
     * 末尾の null を落とした結果その手前の "" も末尾になり、まとめて畳まれる。<br>
     * Given: fields 3 件に対し値行 {@code ["x", "", null]}<br>
     * When:  YAML と本体（Excel）の双方を読み、{@code toDataRecords()} を比べる<br>
     * Then:  いずれも FIELD1="x"、FIELD2=""、FIELD3="" になること
     * </p>
     */
    @Test
    public void getSetupFile_trailingEmptyStringAndNullBecomeEmptyStrings() {
        assertFileCase("F6", "x", "", "");
    }

    // ========================================================================
    // 電文（messages / response_body_messages）
    // ========================================================================

    /**
     * [YamlTestDataParser] getMessage: 電文でも末尾の null が "" になること（M1）。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:1155}
     * 「末尾に {@code null} と記述した場合の扱いも、ファイルデータと同じである」。
     * 本体の電文パーサ（{@code MessageParser}）は {@code FixedLengthFileParser} へ委譲するため、
     * ファイルデータと同じ {@code onReadLine} の {@code trimTailCopy} を通る。<br>
     * なお {@code messages} は固定長のため、{@code DataRecord} へ変換する際にデータ型のパディング除去
     * （{@code DataFileFragment#removePadding}）を通り、値が null でも "" が返る。そのため本ケースは
     * {@code DataRecord} の水準では是正の前後で結果が変わらない（実測済み）。null と "" の差が
     * {@code DataRecord} に現れるのは {@code MockMessages$MockMessage#removePadding}
     * （{@code nablarch-testing} の {@code MockMessages.java:64}）を通る送信同期電文の経路であり、
     * そちらは {@link #getSendSyncMessage_trailingNullsBecomeEmptyStrings()} が担保する。
     * 本ケースは電文（{@code messages}）でも本体と同じ結果になることを固定する。<br>
     * Given: fields 3 件（半角 5 バイト）に対し値行 {@code ["x", null, null]}<br>
     * When:  YAML と本体（Excel）の双方を読み、電文本文のレコードを比べる<br>
     * Then:  いずれも FIELD1="x"、FIELD2=""、FIELD3="" になること
     * </p>
     */
    @Test
    public void getMessage_trailingNullsBecomeEmptyStrings() {
        // Given / When
        List<DataRecord> expected = expectedMessagesOf(
                oracle.parser().getMessage(oracle.dir(), oracle.resource("M1"), "M1"));
        List<DataRecord> actual = expectedMessagesOf(
                sut.getMessage(YAML_DIR, YAML_RESOURCE, "M1"));

        // Then
        assertRecordValues("M1 本体（Excel）", expected, "x", "", "");
        assertSameAsOracle("M1", expected, actual);
    }

    /**
     * [YamlTestDataParser] getSendSyncMessage: 送信同期電文でも末尾の null が "" になること（S2）。
     *
     * <p>
     * 根拠: {@code implementation/testdata_notation.rst:1155}。
     * 本体の {@code SendSyncMessageParser} は値行の先頭セル（No 列）を値から切り離してから
     * {@code addValueWithId} へ渡すが、{@code trimTailCopy} は {@code onReadLine} で
     * 行全体に掛かっており、先頭セルが非空であるため値だけに掛けるのと等価である。<br>
     * Given: fields 3 件（半角 5 バイト）に対し値行 {@code ["x", null, null]}
     *        （Excel は No 列 1 に続けて {@code x}／{@code null}／{@code null}）<br>
     * When:  YAML と本体（Excel）の双方を読み、電文本文のレコードを比べる<br>
     * Then:  いずれも FIELD1="x"、FIELD2=""、FIELD3="" になること
     * </p>
     */
    @Test
    public void getSendSyncMessage_trailingNullsBecomeEmptyStrings() {
        // Given / When
        List<DataRecord> expected = expectedMessagesOf(oracle.parser().getSendSyncMessage(
                oracle.dir(), oracle.resource("S2"), "[S2]", DataType.RESPONSE_BODY_MESSAGES).get(0));
        List<DataRecord> actual = expectedMessagesOf(sut.getSendSyncMessage(
                YAML_DIR, YAML_RESOURCE, "[S2]", DataType.RESPONSE_BODY_MESSAGES).get(0));

        // Then
        assertRecordValues("S2 本体（Excel）", expected, "x", "", "");
        assertSameAsOracle("S2", expected, actual);
    }

    // ========================================================================
    // ヘルパー
    // ========================================================================

    /**
     * ファイルデータ 1 ケースについて、本体（Excel）の結果を正解として YAML の結果と突き合わせる。
     *
     * @param caseName       ケース名（＝ Excel のシート名 ＝ YAML のグループ ID）
     * @param expectedValues 本体が返す FIELD1〜FIELD3 の値（解説書が定める挙動）
     */
    private void assertFileCase(String caseName, String... expectedValues) {
        List<DataRecord> expected = toDataRecords(
                oracle.parser().getSetupFile(oracle.dir(), oracle.resource(caseName), caseName));
        List<DataRecord> actual = toDataRecords(
                sut.getSetupFile(YAML_DIR, YAML_RESOURCE, caseName));

        assertRecordValues(caseName + " 本体（Excel）", expected, expectedValues);
        assertSameAsOracle(caseName, expected, actual);
    }

    /** DataFile リストからデータレコードを取り出す。 */
    private static List<DataRecord> toDataRecords(List<DataFile> files) {
        assertThat("ファイルが 1 件取得できること", files.size(), is(1));
        return files.get(0).toDataRecords();
    }

    /** 電文プールから電文本文のレコードを取り出す。 */
    private static List<DataRecord> expectedMessagesOf(MessagePool pool) {
        return ((RequestTestingMessagePool) pool).getExpectedMessageList();
    }

    /**
     * 本体が返した値が解説書の定める挙動どおりであることを確かめる。
     *
     * <p>本体を正解として扱う以上、その正解自体が解説書と一致していることをここで固定する。</p>
     */
    private static void assertRecordValues(String message, List<DataRecord> records, String... values) {
        assertThat(message + ": レコードが 1 件であること", records.size(), is(1));
        DataRecord record = records.get(0);
        for (int i = 0; i < values.length; i++) {
            String name = "FIELD" + (i + 1);
            assertThat(message + ": " + name, record.get(name), is((Object) values[i]));
        }
    }

    /**
     * YAML の結果が本体（Excel）の結果と一致することを確かめる。
     *
     * @param caseName ケース名
     * @param expected 本体（Excel）の結果
     * @param actual   YAML の結果
     */
    private static void assertSameAsOracle(String caseName, List<DataRecord> expected, List<DataRecord> actual) {
        assertThat(caseName + ": レコード件数が本体と一致すること", actual.size(), is(expected.size()));
        for (int i = 0; i < expected.size(); i++) {
            DataRecord expectedRecord = expected.get(i);
            DataRecord actualRecord = actual.get(i);
            assertThat(caseName + ": フィールド名の集合が本体と一致すること",
                    actualRecord.keySet(), is(expectedRecord.keySet()));
            for (Map.Entry<String, Object> field : expectedRecord.entrySet()) {
                assertThat(caseName + ": " + field.getKey() + " が本体と一致すること",
                        actualRecord.get(field.getKey()), is(field.getValue()));
            }
        }
    }
}
