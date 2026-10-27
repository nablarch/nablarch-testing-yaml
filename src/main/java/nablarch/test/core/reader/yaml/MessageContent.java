package nablarch.test.core.reader.yaml;

import nablarch.test.core.file.FixedLengthFile;

import java.util.Map;

/**
 * メッセージ本文（固定長ファイルの器）と FW 制御ヘッダの組。
 *
 * @author kiyotis
 * @see YamlMessageBuilder#buildMessageContent(Map, String, String, boolean, String)
 */
public final class MessageContent {

    /** FW 制御ヘッダ（{@code messages} 経路のみ非空。その他は空 Map） */
    private final Map<String, String> fwHeader;

    /** 本文（固定長ファイルの器） */
    private final FixedLengthFile body;

    /**
     * コンストラクタ。インスタンス生成は {@link YamlMessageBuilder} のみが行う（package-private）。
     *
     * @param fwHeader FW 制御ヘッダ
     * @param body     本文
     */
    MessageContent(Map<String, String> fwHeader, FixedLengthFile body) {
        this.fwHeader = fwHeader;
        this.body = body;
    }

    /** @return FW 制御ヘッダ（{@code messages} 経路のみ非空。その他は空 Map） */
    public Map<String, String> getFwHeader() {
        return fwHeader;
    }

    /** @return 本文（固定長ファイルの器） */
    public FixedLengthFile getBody() {
        return body;
    }
}
