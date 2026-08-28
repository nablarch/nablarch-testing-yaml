package nablarch.test.core.reader.yaml;

import nablarch.core.repository.SystemRepository;
import nablarch.core.util.StringUtil;
import nablarch.test.NablarchTestUtils;
import nablarch.test.core.file.FixedLengthFile;
import nablarch.test.core.file.MockMessages;
import nablarch.test.core.messaging.MessagePool;
import nablarch.test.core.messaging.RequestTestingMessagePool;
import nablarch.test.core.util.interpreter.TestDataInterpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static nablarch.test.core.reader.yaml.YamlSection.FIELD_FW_HEADER;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_GROUP_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_ID;
import static nablarch.test.core.reader.yaml.YamlSection.FIELD_RECORDS;
import static nablarch.test.core.reader.yaml.YamlSection.castMap;
import static nablarch.test.core.reader.yaml.YamlSection.getList;
import static nablarch.test.core.reader.yaml.YamlSection.objectToString;
import static nablarch.test.core.reader.yaml.YamlSection.groupMatches;
import static nablarch.test.core.reader.yaml.YamlSection.toStr;

/**
 * YAML のメッセージ系セクション（{@code messages}／{@code expected_request_*_messages}／
 * {@code response_*_messages}）から、本体の器（{@link MessagePool}）を直接組み立てるビルダ。
 *
 * <p>
 * 本文レコード・データ行・ディレクティブの組み立ては {@link YamlFileBuilder} の処理を再利用する
 * （重複ゼロ）。FW 制御ヘッダ（{@code fw_header:}）は「マップであること」「キーが
 * {@code reader.fwHeaderfields} の名前であること」の検証・文字列化を、
 * <b>実際に読み出すメッセージに対してのみ遅延実行</b>する（同一ファイル内の誤記エントリが他エントリの
 * 読み出しを巻き添えにしない挙動）。値の解釈（interpret）は行わず文字列化のみを行う。
 * ただしこの「巻き添えにしない」範囲は本クラスが行うキーの検査に限られる。値の型は
 * スキーマ（{@code $defs.fw_header} の {@code additionalProperties: {"type":"string"}}）が検査するため、
 * どれか 1 エントリの {@code fw_header:} の値がクォートなしの数値・真偽値であれば
 * {@code YamlLoader.load} がファイル全体をロード時に落とす。
 * </p>
 *
 * <p>
 * 集合外のキーを例外にする点は本体と意図的に異なる。本体 {@code MessageParser} は
 * {@code processDirectives} が集合外の名前に対して {@code false} を返すため、エラーにせず黙って本文側
 * （フィールド名称行）として扱う（解説書 {@code implementation/testdata_notation.rst:1264}）。
 * これに対し YAML 形式では {@code fw_header:} が本文（{@code records:}）と分離した専用ブロックであり
 * 未知キーが他の意味を持ち得ないため、解説書 {@code implementation/testdata_notation.rst:1295}
 * 「それ以外のキーがあるとエラーになる」のとおり例外とする。
 * </p>
 *
 * @author kiyotis
 */
public final class YamlMessageBuilder {

    /** FW 制御ヘッダの項目名を設定する {@link SystemRepository} のキー（本体 {@code MessageParser} と同じ） */
    private static final String FW_HEADER_KEY = "reader.fwHeaderfields";

    /**
     * {@code reader.fwHeaderfields} 省略時の FW 制御ヘッダの項目名（本体 {@code MessageParser} と同じ）。
     *
     * <p>書き換えられないよう不変の {@link Set} で保持する。</p>
     */
    private static final Set<String> DEFAULT_FW_HEADER_FIELDS = Collections.unmodifiableSet(
            NablarchTestUtils.asSet("requestId", "userId", "resendFlag", "resultCode"));

    private final InterpreterResolver interpreterResolver;

    /**
     * コンストラクタ。
     *
     * @param interpreterResolver basePath ごとに値加工インタープリタチェーンを解決する戦略
     */
    public YamlMessageBuilder(InterpreterResolver interpreterResolver) {
        this.interpreterResolver = interpreterResolver;
    }

    /**
     * メッセージ系セクションから指定 ID の {@link MessagePool} を組み立てる。
     *
     * @param yaml        YAML トップレベル Map
     * @param sectionKey  セクションキー（例: {@code "messages"}）
     * @param id          メッセージ ID
     * @param useFwHeader {@code fw_header:} を使用するか（{@code messages} 経路のみ true。その他は空 Map）
     * @param basePath    インタープリタ用ベースパス
     * @return {@link MessagePool}（実体は {@link RequestTestingMessagePool}）、または存在しない場合 null
     */
    public MessagePool buildMessagePool(Map<String, Object> yaml, String sectionKey, String id,
                                        boolean useFwHeader, String basePath) {
        MessageContent content = buildMessageContent(yaml, sectionKey, id, useFwHeader, basePath);
        return content == null ? null : new RequestTestingMessagePool(content.getBody(), content.getFwHeader());
    }

    /**
     * メッセージ系セクションから指定 ID の本文（固定長ファイルの器）と FW 制御ヘッダを組み立てる。
     *
     * <p>
     * {@link #buildMessagePool} が {@link RequestTestingMessagePool} へ包む前の生の構成要素を返す。
     * 変換ツールは本体 {@link MessagePool} の
     * {@code getSource()} が別パッケージから不可視なため、本メソッドで本文の {@link FixedLengthFile} を直接受け取る。
     * </p>
     *
     * @param yaml        YAML トップレベル Map
     * @param sectionKey  セクションキー（例: {@code "messages"}）
     * @param id          メッセージ ID
     * @param useFwHeader {@code fw_header:} を使用するか（{@code messages} 経路のみ true。その他は空 Map）
     * @param basePath    インタープリタ用ベースパス
     * @return 本文と FW 制御ヘッダ、または存在しない場合 null
     */
    public MessageContent buildMessageContent(Map<String, Object> yaml, String sectionKey, String id,
                                              boolean useFwHeader, String basePath) {
        List<TestDataInterpreter> interps = interpreterResolver.resolve(basePath);
        for (Object entry : getList(yaml, sectionKey)) {
            Map<String, Object> map = castMap(entry);
            if (id.equals(toStr(map.get(FIELD_ID)))) {
                FixedLengthFile file = buildMessageBodyFile(
                        new FixedLengthFile(id), map, YamlSection.isSendSyncMessageSectionKey(sectionKey), interps);
                Map<String, String> fwHeader = useFwHeader
                        ? convertFwHeader(map.get(FIELD_FW_HEADER), id)
                        : Collections.<String, String>emptyMap();
                return new MessageContent(fwHeader, file);
            }
        }
        return null;
    }

    /**
     * メッセージ系セクションから指定グループの SendSync 用メッセージリストを組み立てる。
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー
     * @param groupId    グループ ID（整形済み形式 {@code "[xxx]"} または {@code ""}）
     * @param basePath   インタープリタ用ベースパス
     * @return {@link RequestTestingMessagePool} リスト、または存在しない場合 null
     */
    public List<RequestTestingMessagePool> buildSendSyncList(Map<String, Object> yaml, String sectionKey,
                                                             String groupId, String basePath) {
        List<TestDataInterpreter> interps = interpreterResolver.resolve(basePath);
        List<RequestTestingMessagePool> result = new ArrayList<RequestTestingMessagePool>();
        for (Object entry : getList(yaml, sectionKey)) {
            Map<String, Object> map = castMap(entry);
            if (!groupMatches(toStr(map.get(FIELD_GROUP_ID)), groupId)) {
                continue;
            }
            String id = toStr(map.get(FIELD_ID));
            MockMessages file = buildSendSyncFile(
                    id, map, YamlSection.isSendSyncMessageSectionKey(sectionKey), interps);
            RequestTestingMessagePool pool =
                    new RequestTestingMessagePool(file, Collections.<String, String>emptyMap());
            if (id != null) {
                pool.setRequestId(id);
            }
            result.add(pool);
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * メッセージ系セクションから指定グループの SendSync 用本文（固定長ファイルの器）リストを組み立てる。
     *
     * <p>
     * {@link #buildSendSyncList} が {@link RequestTestingMessagePool} へ包む前の本文をそのまま返す
     * （変換ツール用）。各 {@link MockMessages} の {@link nablarch.test.core.file.DataFile#getPath() path} は
     * エントリの {@code id}（無指定は空文字）に一致する。
     * </p>
     * <p>
     * 本メソッドは変換ツール専用であり、グループ ID は角括弧なしの生値で照合する
     * （{@link #buildSendSyncList} は角括弧付きの整形済み ID で照合する）。
     * </p>
     *
     * @param yaml       YAML トップレベル Map
     * @param sectionKey セクションキー
     * @param groupId    グループ ID（生値で一致比較する）
     * @param basePath   インタープリタ用ベースパス
     * @return 本文（固定長ファイルの器）リスト（記述順。対象が無ければ空）
     */
    public List<FixedLengthFile> buildSendSyncBodies(Map<String, Object> yaml, String sectionKey,
                                                     String groupId, String basePath) {
        List<TestDataInterpreter> interps = interpreterResolver.resolve(basePath);
        List<FixedLengthFile> result = new ArrayList<FixedLengthFile>();
        for (Object entry : getList(yaml, sectionKey)) {
            Map<String, Object> map = castMap(entry);
            String rawGroupId = toStr(map.get(FIELD_GROUP_ID));
            if (rawGroupId != null && rawGroupId.equals(groupId)) {
                result.add(buildSendSyncFile(toStr(map.get(FIELD_ID)), map,
                        YamlSection.isSendSyncMessageSectionKey(sectionKey), interps));
            }
        }
        return result;
    }

    /**
     * SendSync 用の本文（{@link MockMessages}）を 1 エントリ分組み立てる。
     *
     * @param id             エントリの id（{@code null} 可。{@code null} の場合 path は空文字）
     * @param map            エントリ Map
     * @param keepRecordType {@code record_type} の記載値をレコード種別として保持するか
     * @param interps        使用するインタープリタリスト
     * @return 本文の器
     */
    private static MockMessages buildSendSyncFile(String id, Map<String, Object> map, boolean keepRecordType,
                                                  List<TestDataInterpreter> interps) {
        MockMessages file = new MockMessages(id != null ? id : "");
        // 送信同期メッセージは本体パーサが値行先頭の No 列を FIRST_FIELD_NO に隔離するため、
        // YAML 経路でも同じ形の器になるよう withId=true で連番を補う
        // （連番は照合には使われず、失敗時メッセージの test no=[...] にのみ使われる）。
        return buildSendSyncBodyFile(file, map, keepRecordType, interps);
    }

    /**
     * 受信メッセージ用の本文ファイルにディレクティブとレコードレイアウト（{@code records} 全件）を組み立てて返す。
     *
     * @param file           本文ファイル（{@link FixedLengthFile}）
     * @param map            エントリ Map
     * @param keepRecordType {@code record_type} の記載値をレコード種別として保持するか
     * @param interps        使用するインタープリタリスト
     * @param <T>            本文ファイルの具体型
     * @return 組み立て済みの本文ファイル（引数と同一インスタンス）
     */
    private static <T extends FixedLengthFile> T buildMessageBodyFile(T file, Map<String, Object> map,
                                                                       boolean keepRecordType,
                                                                       List<TestDataInterpreter> interps) {
        YamlFileBuilder.applyDirectives(file, YamlFileBuilder.mapDirectives(map), interps);
        YamlFileBuilder.buildFragmentsForMessage(file, getList(map, FIELD_RECORDS), keepRecordType, interps);
        return file;
    }

    /**
     * 送信同期メッセージ用の本文ファイルにディレクティブとレコードレイアウト（{@code records} 全件）を組み立てて返す。
     *
     * <p>値行に連番（FIRST_FIELD_NO）を付与する。</p>
     *
     * @param file           本文ファイル（{@link MockMessages}）
     * @param map            エントリ Map
     * @param keepRecordType {@code record_type} の記載値をレコード種別として保持するか
     * @param interps        使用するインタープリタリスト
     * @param <T>            本文ファイルの具体型
     * @return 組み立て済みの本文ファイル（引数と同一インスタンス）
     */
    private static <T extends FixedLengthFile> T buildSendSyncBodyFile(T file, Map<String, Object> map,
                                                                        boolean keepRecordType,
                                                                        List<TestDataInterpreter> interps) {
        YamlFileBuilder.applyDirectives(file, YamlFileBuilder.mapDirectives(map), interps);
        YamlFileBuilder.buildFragmentsForSendSync(file, getList(map, FIELD_RECORDS), keepRecordType, interps);
        return file;
    }

    /**
     * 生の {@code fw_header} 値を検証・文字列化して {@code Map<String,String>} へ変換する（{@code messages} 経路のみ呼ばれる）。
     *
     * <p>
     * 値は文字列化のみで解釈（interpret）はしない。マップ以外が指定された場合、および
     * {@code fwHeaderFields()} に無いキーが記載された場合は ID 付きで
     * {@link IllegalStateException} を投げる。
     * </p>
     *
     * @param fwHeaderObj 生の fw_header 値（マップ／その他／null）
     * @param id          メッセージ ID（例外メッセージ用）
     * @return FW 制御ヘッダ Map（省略時・null 時は空 Map）
     */
    private Map<String, String> convertFwHeader(Object fwHeaderObj, String id) {
        if (fwHeaderObj == null) {
            return Collections.emptyMap();
        }
        if (!(fwHeaderObj instanceof Map)) {
            throw new IllegalStateException(
                    "fw_header in message entry id='" + id + "' must be a map, "
                            + "but was: " + fwHeaderObj.getClass().getSimpleName());
        }
        Set<String> allowedFields = fwHeaderFields();
        Map<String, String> fwHeader = new LinkedHashMap<String, String>();
        for (Map.Entry<?, ?> kv : ((Map<?, ?>) fwHeaderObj).entrySet()) {
            String key = objectToString(kv.getKey());
            if (!allowedFields.contains(key)) {
                throw new IllegalStateException(
                        "fw_header in message entry id='" + id + "' has unknown key '" + key + "'. "
                                + "allowed keys (" + FW_HEADER_KEY + "): " + new TreeSet<String>(allowedFields));
            }
            fwHeader.put(key, objectToString(kv.getValue()));
        }
        return fwHeader;
    }

    /**
     * {@code fw_header:} に記載できる FW 制御ヘッダの項目名を取得する。
     *
     * <p>
     * {@code reader.fwHeaderfields} が設定されていればその名前（カンマ区切り。前後の空白は取り除かない）、
     * 設定されていなければ既定の 4 つ（{@code requestId}・{@code userId}・{@code resendFlag}・{@code resultCode}）を
     * 返す。本体 {@code MessageParser} と同じキー・同じ既定値・同じ分割（{@link NablarchTestUtils#makeArray}）である。
     * </p>
     * <p>
     * 本体は読み込み単位ごとに生成される {@code MessageParser} のフィールド初期化で 1 回だけ集合を作るが、
     * 本クラスは {@code SystemRepository} に登録される {@code YamlTestDataParser} が保持し続けるため、
     * 設定の変更を取りこぼさないよう呼び出しのたびに引く。
     * </p>
     *
     * @return FW 制御ヘッダの項目名の集合
     */
    private static Set<String> fwHeaderFields() {
        String configured = SystemRepository.getString(FW_HEADER_KEY);
        return StringUtil.isNullOrEmpty(configured)
                ? DEFAULT_FW_HEADER_FIELDS
                : NablarchTestUtils.asSet(NablarchTestUtils.makeArray(configured));
    }
}
