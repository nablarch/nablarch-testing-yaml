package nablarch.test.core.reader.yaml;

import nablarch.test.core.db.DbInfo;
import nablarch.test.core.db.DefaultValues;
import nablarch.test.core.db.TableData;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ROWS;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_TABLE;
import static nablarch.test.core.reader.yaml.YamlSection.KEY_LIST_MAPS;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.groupMatches;
import static nablarch.test.core.reader.yaml.YamlSection.interpret;
import static nablarch.test.core.reader.yaml.YamlSection.isMarker;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.resolveColumns;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML のテーブル系セクション（{@code setup_tables}／{@code expected_tables}／
 * {@code expected_complete_tables}／{@code list_maps}）から、本体の器（{@link TableData} および
 * list_maps 行リスト）を直接組み立てるビルダ。
 *
 * <p>
 * YAML トップレベル Map（{@link YamlLoader#load} が返す順序保持 Map）を走査し、構造の写し取りと
 * 値加工（特殊記法 {@code ${...}} の解釈・{@code ${binaryFile:}} の basePath 解決・マーカーカラム除外・
 * デフォルト値補完・グループ ID 絞り込み）を一括で行う。カラム名は各エントリ先頭行のキー
 * （マーカー含む・YAML 記述順）から決定する。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlTableDataBuilder {

    private final DbInfo dbInfo;
    private final DefaultValues defaultValues;
    private final InterpreterResolver interpreterResolver;

    /**
     * コンストラクタ。
     *
     * @param dbInfo              DB 情報（テーブル構築に使用）
     * @param defaultValues       デフォルト値設定（{@code fillDefaultValues} に使用）
     * @param interpreterResolver basePath ごとに値加工インタープリタチェーンを解決する戦略
     */
    public YamlTableDataBuilder(DbInfo dbInfo, DefaultValues defaultValues,
                                InterpreterResolver interpreterResolver) {
        this.dbInfo = dbInfo;
        this.defaultValues = defaultValues;
        this.interpreterResolver = interpreterResolver;
    }

    /**
     * テーブル系セクションから指定グループの {@link TableData} 群を組み立てる。
     *
     * @param yaml         YAML トップレベル Map
     * @param sectionKey   セクションキー（例: {@code "setup_tables"}）
     * @param groupId      整形済みグループ ID（例: {@code "[case01]"} または {@code ""}）
     * @param fillDefaults true の場合 {@link TableData#fillDefaultValues()} を適用する
     * @param basePath     インタープリタ用ベースパス
     * @return TableData リスト
     */
    public List<TableData> buildTableDataList(Map<String, Object> yaml, String sectionKey, String groupId,
                                              boolean fillDefaults, String basePath) {
        List<TableData> result = new ArrayList<TableData>();
        List<TestDataInterpreter> interps = interpreterResolver.resolve(basePath);
        for (Object entry : getList(yaml, sectionKey)) {
            Map<String, Object> map = castMap(entry);
            if (!groupMatches(toStr(map.get(FIELD_GROUP_ID)), groupId)) {
                continue;
            }
            String tableName = toStr(map.get(FIELD_TABLE));
            if (tableName == null) {
                throw new IllegalStateException(
                        "Missing required field 'table' in " + sectionKey + " entry. groupId=" + groupId
                                + ", basePath=" + basePath);
            }
            List<Object> rows = getList(map, FIELD_ROWS);
            List<String> columnNames = resolveColumns(rows);
            List<List<String>> rawRows = extractRows(rows, columnNames);
            // rows が空（rows: []）のテーブルも 0 行の TableData として生成する。
            // setup_tables では「対象テーブルを空にクリアする」指示を意味し、本体（Excel 経路）は
            // これを 0 行の TableData として返して setUpDb の削除→挿入でテーブルを空にする。
            // ここで空テーブルを脱落させると、メッセージ受信テスト等のショット間でクリアが行われず
            // 前ショットのデータが残存して検証がずれる。空でも生成して本体（Excel）の挙動に合わせる。
            result.add(buildTableData(tableName, columnNames, rawRows, fillDefaults, interps));
        }
        return result;
    }

    /**
     * 1 エントリ分の {@link TableData} を組み立てる。
     *
     * <p>
     * 列名が 0 件のまま渡ってくることがある（条件は下の {@code new TableData(...)} 直前のコメント）。
     * 0 件のまま渡してよい根拠は {@code fillDefaults} で分かれる。
     * </p>
     * <ul>
     *   <li>fillDefaults=false のうち {@code expected_tables}: 依存先 nablarch-testing の
     *       {@code TableData#loadData()} が列名 0 件のとき {@code dbInfo.getColumns(tableName)} を
     *       取得対象カラムとして DB を読むため、行の有無は検証される。この前提が崩れれば
     *       {@code nablarch.test.core.reader.YamlTestDataParserTest#emptyExpectedTable_failsWhenDbHasRows}
     *       が落ちる。</li>
     *   <li>fillDefaults=false のうち {@code setup_tables}: 列名は 0 件のまま解決されない。
     *       {@code TableData#deleteData()} は {@code "DELETE FROM <table>"} で列名を使わず、
     *       {@code TableData#insertData()} が使うのは {@code dbInfo} から得た自動計算カラム除外の
     *       カラムであって TableData の列名ではないため、列名は必要にならない。</li>
     *   <li>fillDefaults=true（{@code expected_complete_tables}）: 下の {@code fillDefaultValues()} が
     *       呼ぶ {@code TableData#fillDefaultValues()} が {@code dbInfo.getColumns(tableName)} を
     *       列名として設定するため、この経路の TableData には DB 由来の全カラムが入る。</li>
     * </ul>
     *
     * <p>
     * 最後の 1 件は {@code dbInfo.getColumns} を使うが、これは列名解決をそれに依存させない方針と
     * 矛盾しない。{@code getColumns} を提供しない {@link DbInfo} 実装と組み合わせる読み込み専用の
     * 経路は fillDefaults=false で呼ぶ約束であり、{@code fillDefaultValues()} には到達しないためである。
     * </p>
     */
    private TableData buildTableData(String tableName, List<String> cols, List<List<String>> rawRows,
                                     boolean fillDefaults, List<TestDataInterpreter> interps) {
        List<String> dataColumns = new ArrayList<String>();
        List<Integer> dataColumnIndexes = new ArrayList<Integer>();
        for (int i = 0; i < cols.size(); i++) {
            if (!isMarker(cols.get(i))) {
                dataColumns.add(cols.get(i));
                dataColumnIndexes.add(i);
            }
        }
        // 列名は先頭行（rows.get(0)）のキーからのみ決まる（YamlSection#resolveColumns）。どの行もキーを
        // 持たない場合、すなわち rows が空（rows: []）か全行が空マッピング（{}）の場合は列名 0 件になるが、
        // YAML に列名を書く場所が無いためここで作り出すことはしない。本ビルダは getColumns を提供しない
        // DbInfo 実装と組み合わせて読み込み専用に使われうるので、dbInfo.getColumns にも依存させない。
        TableData td = new TableData(dbInfo, tableName, dataColumns.toArray(new String[0]), defaultValues);
        for (List<String> rawRow : rawRows) {
            if (rawRow.isEmpty()) {
                // 空マッピング（{}）行はデータ行として扱わない。
                continue;
            }
            List<String> values = new ArrayList<String>(dataColumnIndexes.size());
            for (int idx : dataColumnIndexes) {
                values.add(interpret(rawRow.get(idx), interps));
            }
            td.addRow(values);
        }
        if (fillDefaults) {
            td.fillDefaultValues();
        }
        return td;
    }

    /**
     * {@code list_maps} から指定 ID の行リストを組み立てる。
     *
     * <p>
     * 出力 Map のキー順は従来どおり {@link TreeMap} でソートする（本体読み込みの振る舞い不変）。
     * マーカーカラム（{@code [COL]}）は DB 操作対象外として除外する。
     * </p>
     *
     * @param yaml     YAML トップレベル Map
     * @param id       list_maps エントリの id
     * @param basePath インタープリタ用ベースパス
     * @return 行リスト（見つからない場合は空リスト）
     */
    public List<Map<String, String>> buildListMapRows(Map<String, Object> yaml, String id, String basePath) {
        List<TestDataInterpreter> interps = interpreterResolver.resolve(basePath);
        for (Object entry : getList(yaml, KEY_LIST_MAPS)) {
            Map<String, Object> map = castMap(entry);
            if (id.equals(toStr(map.get(FIELD_ID)))) {
                List<Object> rows = getList(map, FIELD_ROWS);
                List<String> columnNames = resolveColumns(rows);
                return buildListMapRows(columnNames, extractRows(rows, columnNames), interps);
            }
        }
        return Collections.emptyList();
    }

    private List<Map<String, String>> buildListMapRows(List<String> cols, List<List<String>> rawRows,
                                                       List<TestDataInterpreter> interps) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (List<String> rawRow : rawRows) {
            Map<String, String> row = new TreeMap<String, String>();
            // 空マッピング（{}）行は空の行として保持する。
            if (!rawRow.isEmpty()) {
                for (int i = 0; i < cols.size(); i++) {
                    String col = cols.get(i);
                    if (isMarker(col)) {
                        continue;
                    }
                    row.put(col, interpret(rawRow.get(i), interps));
                }
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 各行をカラム名に揃えた未加工値リストへ写す。
     *
     * <p>
     * マッピングでない行（スカラ等）は構造を持たないため除外する。空マッピング（{@code {}}）は
     * 空リストとして保持する（行の有無を後段が判別できるようにするため）。
     * </p>
     */
    private static List<List<String>> extractRows(List<Object> rows, List<String> columnNames) {
        List<List<String>> rawRows = new ArrayList<List<String>>();
        for (Object rowObj : rows) {
            if (!(rowObj instanceof Map)) {
                continue;
            }
            Map<String, Object> rowMap = castMap(rowObj);
            if (rowMap.isEmpty()) {
                rawRows.add(new ArrayList<String>());
                continue;
            }
            List<String> rowValues = new ArrayList<String>(columnNames.size());
            for (String col : columnNames) {
                rowValues.add(objectToString(rowMap.get(col)));
            }
            rawRows.add(rowValues);
        }
        return rawRows;
    }

}
