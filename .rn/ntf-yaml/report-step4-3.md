# Step 4 報告 — スキーマ `description` と解説書（SSoT）の全件突合（#47）

指示書: `nablarch-document@origin/ntf-yaml-support` の
`.rn/20260724-ntf-yaml-support/ntf-step4-13-yaml-schema-consistency.md`

## 0. 出典の書き方

- **解説書** — ピン `nablarch-document@ed3de95f`。`notation.rst:NNN` は
  `ja/development_tools/testing_framework/implementation/testdata_notation.rst` の行番号
  （`git show ed3de95f:<path>` で取得。作業ツリーは読んでいない）。
  他ファイルはパスを明記する（`setup/common.rst` 等。同じくピン配下）。
- **本モジュール** — `src/main/java/...` のファイル:行（作業ツリー `8b56776` 時点。是正コミット前）。
- **本体（nablarch-testing）** — ピン `3c4bd2a`。`git show 3c4bd2a:<path>` で取得。
- **スキーマ** — `src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json` の行番号
  （是正前 = `8b56776` 時点）。

判定は指示書 §1 の3値。

| 判定 | 意味 |
|---|---|
| 一致 | 解説書に同旨の記述がある |
| 記述なし | スキーマだけが述べている（実装・スキーマ制約の説明等） |
| 矛盾 | 解説書と食い違う |

## 1. 母集合（機械抽出）

```
$ grep -c '"description"' src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json
64
```

JSON をパースして `description` キーを持つノードを列挙した結果も 64 件で一致した
（トップレベル 1 + トップレベルプロパティ 11 + `$defs` 10 + `$defs` 配下のプロパティ 42）。

```
$ python3 -c '
import json,re,sys
p="src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json"
lines=open(p,encoding="utf-8").read().split("\n")
print("grep相当:", len([1 for l in lines if re.match(r"\s*\"description\"\s*:", l)]))
d=json.load(open(p,encoding="utf-8"))
out=[]
def walk(n,path):
    if isinstance(n,dict):
        if isinstance(n.get("description"),str): out.append(path or "(root)")
        for k,v in n.items():
            if k!="description": walk(v,path+"/"+k)
    elif isinstance(n,list):
        for i,v in enumerate(n): walk(v,path+"/"+str(i))
walk(d,"")
print("JSON走査:", len(out))'
grep相当: 64
JSON走査: 64
```

**主張の件数: 444 件**（description 由来 356 件 + 構造制約 88 件）。§3 の全表の行数の合計であり、
サンプリングはしていない。番号は重複なし（444 行・ユニーク 444）。

```
$ python3 -c '
import re,collections
s=open(".rn/ntf-yaml/report-step4-3.md",encoding="utf-8").read()
labels=re.findall(r"^\| (\d+-(?:構造)?\d+) \|", s, re.M)
print("行数:",len(labels),"ユニーク:",len(set(labels)))
print("description由来:",len([l for l in labels if "構造" not in l]),
      "構造制約:",len([l for l in labels if "構造" in l]))'
行数: 444 ユニーク: 444
description由来: 356 構造制約: 88
```

判定の内訳:

| 判定 | 件数 |
|---|---|
| 一致 | 349 |
| 一致（挙動）＋記述なし（クラス名等）の併記 | 11 |
| 記述なし | 75 |
| 矛盾 | 9（description 8 = C1〜C8、構造制約 1 = S1） |
| 合計 | 444 |

## 2. 結論

**矛盾は 7 件**（すべて description。スキーマ側を解説書の逐語に合わせて是正した）。
**構造制約の矛盾（変更せず報告のみ）は 1 件**。**解説書側が誤っている疑いは 0 件**。

| # | 箇所 | 矛盾の内容 | 解説書の出典 |
|---|---|---|---|
| C1 | `properties.messages`（スキーマ `:53`） | `messages` を「MockMessaging 経路の要求/応答電文データ」とした。`setUpMessages`／`expectedMessages` を読むのは本体 `MQSupport`（メッセージング受信テスト）であり、MockMessagingContext／MockMessagingClient は `RESPONSE_*_MESSAGES` しか読まない | `notation.rst:1130` |
| C2 | `$defs.message_data`（`:196`） | 同上（「MockMessaging 経路の要求/応答電文1メッセージ」） | `notation.rst:1130` |
| C3 | `$defs.message_data.properties.fw_header`（`:216`） | 「messages（MESSAGE: MockMessaging 経路）でのみ使用する」の経路名部分 | `notation.rst:1130`・`:1260` |
| C4 | `properties.response_header_messages`（`:74`） | errorMode が「経路B のみ」で効くとした | `notation.rst:1238` |
| C5 | `properties.response_body_messages`（`:81`） | 同上（errorMode「経路B のみ」） | `notation.rst:1238` |
| C6 | `$defs.group_message_data.properties.records`（`:275`） | 「RequestTestingSendSyncSupport 経路では errorMode は無視される」 | `notation.rst:1238` |
| C7 | `properties.response_body_messages`（`:81`） | 長さの一致条件を「電文レコード全体のバイト長」とした（解説書は「各データエントリの文字列長」） | `notation.rst:1186`・`:1209` |
| C8 | `$defs.record_fragment.properties.rows`（`:380`） | 「rows が0件でも有効」 | `notation.rst:838`・`:861` |

構造制約の矛盾（**変更していない**。指示書 §2 により報告して止まる）:

| # | 箇所 | 内容 | 解説書の出典 |
|---|---|---|---|
| S1 | `$defs.record_fragment.properties.rows`（`:378`-`:390`） | `minItems` が無く 0 件を通す。解説書はデータを「1件以上」と定める。C8 の description は是正したが、検証挙動が変わるためスキーマ制約は変更していない | `notation.rst:838` |

構造制約が解説書より緩い箇所（矛盾ではないが未表現。参考）:

| # | 箇所 | 内容 | 解説書の出典 |
|---|---|---|---|
| L1 | `$defs.directives`（`:282`-`:356`） | 固定長11キー・可変長9キーの和集合17キーを、`type` によらず一律に許す。型別の限定はスキーマで表現していない | `notation.rst:884`・`:911` |

## 3. 対応表（全件）

各 description を文単位の主張に分解し、1主張ずつ判定した。「構造」行はその description が付くノードの
構造制約（`type`・`required`・`pattern`・`maxItems` 等）である。

### D1 `(root)` — スキーマ `:5`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 1-1 | Nablarch Testing Framework のテストデータの YAML 表現スキーマである | 一致 | `notation.rst:80`「YAML テストデータには JSON Schema が定義されており、nablarch-testing-yaml の jar に nablarch/test/ntf-testdata-yaml-schema.json として同梱されている」 |
| 1-構造1 | ルートはマッピング（`type: object`） | 一致 | `notation.rst:219`-`:223`（`setup_tables:` を先頭に置くトップレベルマッピングの例）。実装 `YamlLoader.java:146`-`:150` も root が Map でなければ例外 |
| 1-構造2 | トップレベルキーは11種（`setup_tables`〜`response_body_messages`） | 一致 | `notation.rst:188`-`:215`（データタイプ→YAML トップレベルキーの対応表。11行） |
| 1-構造3 | `additionalProperties: false`（列挙外のキーは検証エラー） | 記述なし | スキーマ `:7`／`YamlLoader.java:154`。解説書 `:186` は「データタイプごとに専用のトップレベルキーを使う（完全一致のため前方一致は発生しない）」と述べるが、未知キーがエラーになるとは書いていない。所見: 誤記の早期検出につながるため解説書にあってよい |

### D2 `/properties/setup_tables` — スキーマ `:11`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 2-1 | テスト実行前に NTF が対象テーブルへ INSERT するデータである | 一致 | `notation.rst:132`-`:133`（`SETUP_TABLE` = テスト実行前にデータベースへ登録するテーブルデータ） |
| 2-2 | rows が 0 行の場合（`rows: []` の指定、および `{}` の行の除去で 0 行になった場合）、そのテーブルの全件 DELETE のみ実行される（INSERT なし） | 一致 | `notation.rst:713`「準備データを0件にすると、SETUP_TABLE による登録で行われる全件 DELETE だけが行われ、対象テーブルは空になる」＋`:816`（0件は `rows: []`）＋`:1486`（`{}` の行は読み飛ばされる） |
| 2-3 | 同一 group_id を持つ複数エントリはすべて収集されそれぞれ INSERT される | 一致 | `notation.rst:134`「同じグループのものを全て収集」 |
| 2-4 | rows の有無にかかわらず、NTF はまず対象テーブルを全件 DELETE してから INSERT を行う | 一致 | `notation.rst:648`「`SETUP_TABLE` による登録は、対象テーブルを一旦全件 DELETE したうえで INSERT し直すという動きになる」 |
| 2-5 | rows が空の場合も DELETE は実行される | 一致 | `notation.rst:713` |
| 2-6 | FK の親テーブルを clear する場合は、参照元の子テーブルも setup_tables に列挙すること | 一致 | `notation.rst:648`「FK 関係を持つ親テーブルを準備する際は、子テーブル側も `SETUP_TABLE` の対象に含めておく」 |
| 2-7 | NTF は子テーブル → 親テーブルの順で DELETE する | 一致 | `notation.rst:648`「（削除順序は子から親になる）」 |
| 2-8 | 子テーブルを列挙しないと FK 制約違反で DELETE が失敗する | 一致 | `notation.rst:648`「別のテストが残した子テーブルの行が原因で、親テーブルの DELETE が FK 違反により失敗するケースがある」 |
| 2-構造1 | `type: array`、要素は `$defs/table_data` | 一致 | `notation.rst:224`「同種のデータは、同じキーの下にリストとして並べる」＋`:219`-`:222` |

### D3 `/properties/expected_tables` — スキーマ `:18`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 3-1 | テスト実行後に NTF が DB の実際の状態と照合するデータである | 一致 | `notation.rst:135`-`:136`（`EXPECTED_TABLE` = テスト実行後に比較するテーブルデータ） |
| 3-2 | 比較対象になるカラムは、`{}` の行の除去後の先頭行（カラム名決定行）に現れたカラムだけである | 一致 | `notation.rst:660`「カラムを省略すると、その列は比較の対象から外れる」＋`:799`「カラム名は、最初の行（`rows:` の先頭要素）のキーで決まる」＋`:1486`（`{}` の行の除去） |
| 3-3 | マーカーカラムは DB 操作の対象外なので除く | 一致 | `notation.rst:1468`「カラム名を半角角括弧 `[ ]` で囲むと、そのカラムは「マーカーカラム」として読み込み対象から除外される」 |
| 3-4 | カラム名決定行に無いカラムは比較されない | 一致 | `notation.rst:660`・`:680` |
| 3-5 | カラム名決定行にあり個々の行で省略したカラムは null として比較される | 一致 | `notation.rst:638`「YAML 形式では、`rows:` の先頭行のキーの一部を後続の行が持たない場合、そのカラムは `null` を明示的に指定したのと同じ扱いになる」 |
| 3-6 | 同一 group_id を持つ複数エントリはすべて収集されそれぞれ照合される | 一致 | `notation.rst:137`「同じグループのものを全て収集」 |
| 3-7 | 期待行と DB 行の対応付けは DB の主キーで行われる | 一致 | `notation.rst:661`「期待側の行とデータベース側の行は、主キーの値で突き合わされる」 |
| 3-8 | 主キーカラムは省略しないこと | 一致 | `notation.rst:661`「このため主キーカラムだけは期待側でも省略できない」 |
| 3-9 | 対象テーブルに存在する DB 行は全件を rows に列挙すること（列挙されていない行が DB にあると照合エラー。部分検証は不可） | 一致 | `notation.rst:662`「データベース側に存在する行は、期待側に漏れなく記述する必要がある。記述されていないデータベース行が見つかると、余分な行がある旨のエラーになる（一部の行だけを検証対象にすることはできない）」 |
| 3-10 | 主キーが自動採番のテーブルでは期待側に主キー値を書けないため複数行の検証が成立しない | 一致 | `notation.rst:666`「主キーが自動採番（IDENTITY・シーケンスなど）の場合、テスト実行時に払い出される値が不定になるため期待側に主キー値を書けず、複数行の検証が成立しない」 |
| 3-構造1 | `type: array`、要素は `$defs/table_data` | 一致 | `notation.rst:196`-`:197`・`:224` |

### D4 `/properties/expected_complete_tables` — スキーマ `:25`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 4-1 | テスト実行後に NTF が DB と照合するデータである | 一致 | `notation.rst:138`-`:139` |
| 4-2 | 省略カラムにはカラム型ごとのデフォルト値を補完してから全カラム比較する | 一致 | `notation.rst:680`「`EXPECTED_COMPLETE_TABLE` では、省略したカラムに次の表のデフォルト値が格納されているものとして比較が行われる」 |
| 4-3 | デフォルト値: 数値型="0" | 一致 | `notation.rst:690`-`:691` |
| 4-4 | デフォルト値: 固定長文字=半角スペース×カラム長 | 一致 | `notation.rst:692`-`:693` |
| 4-5 | デフォルト値: 可変長文字=" " | 一致 | `notation.rst:694`-`:695` |
| 4-6 | デフォルト値: 日付型=epoch 起点の JVM タイムゾーン依存値 | 一致 | `notation.rst:696`-`:697`・`:705` |
| 4-7 | デフォルト値: バイナリ型=10バイトゼロ列の HexString | 一致 | `notation.rst:698`-`:699` |
| 4-8 | デフォルト値: Boolean="false" | 一致 | `notation.rst:700`-`:701` |
| 4-9 | 補完されるのはカラム名決定行に無いカラムだけである | 一致 | `notation.rst:680`＋`:799`（カラム名は先頭行のキーで決まる） |
| 4-10 | カラム名決定行にあり個々の行で省略したカラムは null のまま比較される | 一致 | `notation.rst:638` |
| 4-11 | 期待行と DB 行の対応付けは DB の主キーで行われる（主キーカラムは省略しないこと） | 一致 | `notation.rst:661` |
| 4-構造1 | `type: array`、要素は `$defs/table_data` | 一致 | `notation.rst:198`-`:199`・`:224` |

### D5 `/properties/list_maps` — スキーマ `:32`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 5-1 | 汎用キーバリュー形式データである | 一致 | `notation.rst:566`「キーバリュー形式の汎用データをまとめて記述したい場合は、`LIST_MAP` というデータタイプを使う」 |
| 5-2 | テストケース定義（testShots）・リクエストパラメータ・期待ログ等に使用される | 一致 | `notation.rst:566`「テストショット一覧・リクエストパラメータ・期待値オブジェクト・期待ログなど、さまざまな用途で使われる」 |
| 5-3 | id で完全一致検索される | 一致 | `notation.rst:602`「`LIST_MAP` の ID は完全一致で検索される」 |
| 5-4 | 重複時は先着1件のみ有効（2件目以降は無視） | 一致 | `notation.rst:602`・`:143` |
| 5-5 | 指定 id のエントリが存在しない場合は空データ扱い（エラーにならない） | 記述なし | `YamlTableDataBuilder.java:191`（`return Collections.emptyList();`）。所見: 解説書は `:427`「`responseResult` を用意しない場合、この検証はスキップされる」のように個別カラムでしか触れておらず、`LIST_MAP` 一般の規則としては書かれていない。あるべき内容と考える |
| 5-構造1 | `type: array`、要素は `$defs/list_map_data` | 一致 | `notation.rst:200`-`:201`・`:616`-`:624` |

### D6 `/properties/setup_files` — スキーマ `:39`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 6-1 | テスト実行前に NTF が入力ファイルとして配置する固定長・可変長ファイルデータである | 一致 | `notation.rst:144`-`:152`（`SETUP_FIXED` = 準備用の固定長ファイル、`SETUP_VARIABLE` = 準備用の可変長ファイル）＋`:464`-`:465`（`setUpFile` = 入力用ファイル作成時に参照するデータ） |
| 6-2 | `SETUP_FIXED` / `SETUP_VARIABLE` を1つのキーに統合している | 一致 | `notation.rst:202`-`:203`（`SETUP_FIXED`・`SETUP_VARIABLE` → `setup_files`） |
| 6-3 | type フィールドで固定長か可変長かを区別する | 一致 | `notation.rst:878`「固定長か可変長かは、データブロック内の記述で区別される」＋`:1105`（`type: fixed`）＋`:1122` |
| 6-4 | 同一 group_id を持つ複数エントリはすべて収集される | 一致 | `notation.rst:146`・`:152`「同じグループのものを全て収集」＋`:878`「準備用ファイルデータ（`SETUP_FIXED`・`SETUP_VARIABLE`）は、固定長・可変長の区別なくまとめて収集される」 |
| 6-構造1 | `type: array`、要素は `$defs/file_data` | 一致 | `notation.rst:1103`-`:1114`（`setup_files:` 配下のリスト例） |

### D7 `/properties/expected_files` — スキーマ `:46`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 7-1 | テスト実行後に NTF が出力ファイルの内容と照合する期待値ファイルデータである | 一致 | `notation.rst:147`-`:155`（`EXPECTED_FIXED`／`EXPECTED_VARIABLE` = 期待値を示す固定長／可変長ファイル）＋`:880` |
| 7-2 | `EXPECTED_FIXED` / `EXPECTED_VARIABLE` を1つのキーに統合している | 一致 | `notation.rst:204`-`:205` |
| 7-3 | type フィールドで固定長か可変長かを区別する | 一致 | `notation.rst:878`・`:1122` |
| 7-4 | 同一 group_id を持つ複数エントリはすべて収集される | 一致 | `notation.rst:148`・`:154`＋`:878`「期待値ファイル（`EXPECTED_FIXED`・`EXPECTED_VARIABLE`）も同様である」 |
| 7-構造1 | `type: array`、要素は `$defs/file_data` | 一致 | `notation.rst:204`-`:205`・`:1122` |

### D8 `/properties/messages` — スキーマ `:53`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 8-1 | **（是正前）MockMessaging 経路の要求/応答電文データである** | **矛盾（C1）** | `notation.rst:1130`「`setUpMessages`（メッセージング受信テストの要求電文 ID、固定値）・`expectedMessages`（同期応答メッセージ受信の応答電文期待値 ID、固定値。応答電文を持たない応答不要メッセージ受信では読み込まれない）」。本体でも `setUpMessages`／`expectedMessages` を読むのは `MQSupport.java:64`・`:74`（`3c4bd2a`。`MessagingRequestTestSupport`／`MessagingReceiveTestSupport` が使う）であり、`MockMessagingContext.java:97`・`:99` と `MockMessagingClient.java:57`・`:70` は `RESPONSE_HEADER_MESSAGES`／`RESPONSE_BODY_MESSAGES` しか読まない |
| 8-2 | id で完全一致検索され先着1件のみ有効（2件目以降は無視） | 一致 | `notation.rst:156`-`:158`（`MESSAGE` = 最初の1件のみ有効）＋`:246` |
| 8-3 | fw_header で FW 制御ヘッダを指定する | 一致 | `notation.rst:1260`「フレームワーク制御ヘッダを `fw_header:` マップ（キー: 値）で記述する」 |
| 8-4 | records で電文本文フィールドを定義する | 一致 | `notation.rst:1260`「メッセージボディ側のフィールドは、従来どおり `records:` の `fields:`・`rows:` に記載する」 |
| 8-5 | id はデータタイプ MESSAGE の識別子である | 一致 | `notation.rst:1132`「データタイプ `MESSAGE` の識別子として `setUpMessages`（要求電文）・`expectedMessages`（応答電文）を指定し」 |
| 8-6 | `setUpMessages`（要求電文）・`expectedMessages`（応答電文）という固定値を指定する | 一致 | `notation.rst:1132`「これらの識別子は固定である」 |
| 8-7 | `sendSyncTestData` は、取引単体テストのモックアップクラスが読む同期応答メッセージ送信のテストデータのベースディレクトリに付けるコンポーネント設定のキーである | 一致 | `notation.rst:1132`「取引単体テストのモックアップクラスが読む同期応答メッセージ送信のテストデータは、コンポーネント設定ファイルで `sendSyncTestData` というキーに設定したベースディレクトリの配下に置く」＋`setup/common.rst:168` |
| 8-8 | `sendSyncTestData/{requestId}/message` は読み込み単位を指すパスであって、この id に書く値ではない | 一致 | `notation.rst:1132`「YAML 形式ではリクエスト ID と同じ名前のディレクトリ配下の `message.yaml` である（`message` は固定の名前）…`sendSyncTestData` はデータブロックの識別子ではない」 |
| 8-構造1 | `type: array`、要素は `$defs/message_data` | 一致 | `notation.rst:206`-`:207`・`:1264`-`:1277` |

### D9 `/properties/expected_request_header_messages` — スキーマ `:60`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 9-1 | 要求電文ヘッダの期待値である | 一致 | `notation.rst:1161`-`:1163`（`EXPECTED_REQUEST_HEADER_MESSAGES` = 要求電文ヘッダの期待値） |
| 9-2 | testShots の expectedMessage カラムで group_id を指定すると同一 group_id を持つエントリを全件収集する | 一致 | `notation.rst:161`「グループID指定時は全件収集、ID直接指定時は最初の1件」＋`:420`-`:421`（`expectedMessage` = 期待する要求電文のグループID） |
| 9-3 | group_id を省略すると id 直接指定で先着1件収集する | 一致 | `notation.rst:161` |
| 9-4 | expected_request_body_messages とエントリ数（rows 合計）を一致させること | 一致 | `notation.rst:1186`「`EXPECTED_REQUEST_HEADER_MESSAGES` と `EXPECTED_REQUEST_BODY_MESSAGES` のデータ行の合計数は一致していなければならない」 |
| 9-5 | 不一致時はエラーになる | 記述なし | 解説書 `:1186` は「一致していなければならない」とだけ述べ、エラーになるとは書いていない。本体で件数を突き合わせる検査は `RequestTestingSendSyncSupport.java:43`-`:49`（`3c4bd2a`）には無く、**根拠は未確認**。所見: 「一致していなければならない」で足り、「エラー」の断定は根拠を確認できるまで書かないのが安全 |
| 9-構造1 | `type: array`、要素は `$defs/expected_request_message_data` | 一致 | `notation.rst:208`-`:209` |

### D10 `/properties/expected_request_body_messages` — スキーマ `:67`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 10-1 | 要求電文ボディの期待値である | 一致 | `notation.rst:1164`-`:1166` |
| 10-2 | testShots の expectedMessage カラムで group_id を指定すると同一 group_id を持つエントリを全件収集する | 一致 | `notation.rst:163`・`:420`-`:421` |
| 10-3 | group_id を省略すると id 直接指定で先着1件収集する | 一致 | `notation.rst:163` |
| 10-4 | expected_request_header_messages とエントリ数（rows 合計）を一致させること | 一致 | `notation.rst:1186` |
| 10-5 | 不一致時はエラーになる | 記述なし | 9-5 と同じ（未確認） |
| 10-構造1 | `type: array`、要素は `$defs/expected_request_message_data` | 一致 | `notation.rst:210`-`:211` |

### D11 `/properties/response_header_messages` — スキーマ `:74`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 11-1 | 応答電文ヘッダデータである | 一致 | `notation.rst:1167`-`:1169`（`RESPONSE_HEADER_MESSAGES` = 応答電文ヘッダデータ） |
| 11-2 | (A) RequestTestingSendSyncSupport 経路では group_id でフィルタリングして全件収集する | 一致（挙動）／記述なし（クラス名） | 挙動: `notation.rst:167`「グループID指定時は全件収集、ID直接指定時は最初の1件」。クラス名 `RequestTestingSendSyncSupport` は解説書に無い。実装 `YamlMessageBuilder.java:146`-`:167`（`buildSendSyncList` が group 照合で全件収集）。所見: 解説書は経路をクラス名でなく「リクエスト単体テストで応答電文を返す経路」（`:1238`）と呼んでおり、その語に揃えるのが望ましい |
| 11-3 | (B) MockMessagingContext / MockMessagingClient 経路では id で照合して先着1件収集する | 一致（挙動）／記述なし（クラス名） | 挙動: `notation.rst:1207`「識別子の書式がグループIDを持たない点が同期応答メッセージ送信のテストと異なり」＋`:1254`。`MockMessagingContext` は解説書に無い（`MockMessagingClient` は `setup/deal_unit_test/http_messaging.rst:20`、MOM 側は `MockMessagingProvider` として `setup/deal_unit_test/mom.rst:20`）。実装 `YamlMessageBuilder.java:118`-`:135` |
| 11-4 | errorMode 行の先頭値は `errorMode:timeout` または `errorMode:msgException` である | 一致 | `notation.rst:1220`「応答電文の先頭フィールドに `errorMode:` から始まる特定の値を設定すると、障害系のテストを行える」＋`:1229`・`:1232` |
| 11-5 | errorMode 行を含めると送受信エラーをシミュレートできる | 一致 | `notation.rst:1229`-`:1234` |
| 11-6 | **（是正前）errorMode が効くのは経路B のみである** | **矛盾（C4）** | `notation.rst:1238`「`errorMode:` を記述できるのは応答電文（RESPONSE 系）のデータブロックであり、リクエスト単体テストで応答電文を返す経路と、取引単体テストのモックアップクラス経由の送受信の両方で反映される」。本体 `RequestTestingMessagePool.java:78`-`:84`（`3c4bd2a`）が errorMode を判定し、これを呼ぶのは経路A の `RequestTestingMessagingProvider.java:203`・`:205` と `RequestTestingMessagingClient.java:228`・`:231` である |
| 11-構造1 | `type: array`、要素は `$defs/group_message_data` | 一致 | `notation.rst:212`-`:213` |

### D12 `/properties/response_body_messages` — スキーマ `:81`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 12-1 | 応答電文ボディデータである | 一致 | `notation.rst:1170`-`:1172` |
| 12-2 | (A) RequestTestingSendSyncSupport 経路では group_id でフィルタリングして全件収集する | 一致（挙動）／記述なし（クラス名） | 11-2 と同じ |
| 12-3 | (B) MockMessagingContext / MockMessagingClient 経路では id で照合して先着1件収集する | 一致（挙動）／記述なし（クラス名） | 11-3 と同じ |
| 12-4 | **（是正前）各エントリの電文レコード全体のバイト長が同一でなければならない（不一致時はエラー）** | **矛盾（C7）** | `notation.rst:1186`「HTTPメッセージ送信の応答電文本文（`RESPONSE_BODY_MESSAGES`）は、各データエントリの文字列長が同一である必要がある」＋`:1209`「複数回電文を送信する場合は、同一データタイプ・同一リクエストIDのデータをそれぞれまとめて記述し、電文の長さを揃える必要がある」。単位（バイト長／文字列長）と条件の書き方が食い違う |
| 12-5 | errorMode 行を含めると送受信エラーをシミュレートできる | 一致 | `notation.rst:1220`・`:1229`-`:1234` |
| 12-6 | **（是正前）errorMode が効くのは経路B のみである** | **矛盾（C5）** | 11-6 と同じ（`notation.rst:1238`） |
| 12-構造1 | `type: array`、要素は `$defs/group_message_data` | 一致 | `notation.rst:214`-`:215` |

### D13 `/$defs/table_data` — スキーマ `:95`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 13-1 | テーブルデータ1ブロックである | 一致 | `notation.rst:632`「データベースのテーブルに対応するテストデータ（テーブルデータ）は、`SETUP_TABLE`・`EXPECTED_TABLE`・`EXPECTED_COMPLETE_TABLE` のいずれかのデータタイプで記述する。いずれも、データタイプと識別子の値・カラム名・データ行という共通の構成を持つ」 |
| 13-2 | setup_tables / expected_tables / expected_complete_tables の各エントリ1件に対応する | 一致 | `notation.rst:194`-`:199` |
| 13-構造1 | `table` は必須 | 一致 | `notation.rst:799`「`setup_tables`・`expected_tables`・`expected_complete_tables` の各エントリには `table` キーが必須である」 |
| 13-構造2 | `rows` は必須 | 一致 | `notation.rst:816`「0件のデータは、`rows:` に空配列 `[]` を記載する」（0件でも `rows:` を書くことを求めている） |
| 13-構造3 | `additionalProperties: false`（`group_id`・`table`・`rows` 以外のキーは検証エラー） | 記述なし | スキーマ `:94`／`YamlLoader.java:154`。所見: 解説書はエントリのキーを `group_id`（`:310`）・`table`・`rows`（`:788`-`:799`）としか書いておらず、閉じていることは述べていない |

### D14 `/$defs/table_data/properties/group_id` — スキーマ `:100`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 14-1 | データブロックの収集単位である | 一致 | `notation.rst:232`「同じデータタイプのデータブロックを複数記述したい場合は、グループIDでそれらを区別する」＋`:248`-`:250` |
| 14-2 | testShots の setUpTable / expectedTable 等のカラムで指定した値と一致するブロックが全件収集される | 一致 | `notation.rst:402`-`:403`（`setUpTable`）・`:411`-`:412`（`expectedTable`）・`:249` |
| 14-3 | 省略時はグループIDなし（デフォルトグループ）扱いになる | 一致 | `notation.rst:234`「グループIDを省略した場合は、グループIDを持たないデータブロック（デフォルトグループ）が対象になる」 |
| 14-4 | testShots のカラムを省略したテストケースから参照される | 一致 | `notation.rst:234` |
| 14-5 | バッチ処理テストでは `"default"` を指定するとグループIDなし扱いと同等に動作する | 一致 | `notation.rst:234`「Nablarchバッチアプリケーションでは、グループIDに文字列として `"default"` を指定した場合も、グループIDなしと同等に扱われる」 |
| 14-6 | ウェブ・メッセージングテストでは `"default"` は通常の group_id として扱われる | 一致 | `notation.rst:234`「（ウェブアプリケーション・メッセージングのテストには、この挙動は適用されない）」 |
| 14-7 | 空文字 `""` は誤マッチを引き起こすため `minLength: 1` で禁止される | 記述なし | スキーマ `:99`／`YamlSection.java:448`-`:451`（`groupMatches` は `"[" + rawGroupId + "]"` を作るため `""` は `"[]"` になり、グループなし `""` とは別物として誤マッチを招く）。所見: 「省略」と「空文字」の区別は YAML 形式固有であり、解説書の YAML 形式の節（`:310`）にあってよい |
| 14-構造1 | `type: string`／`minLength: 1` | 記述なし | 14-7 と同じ |

### D15 `/$defs/table_data/properties/table` — スキーマ `:104`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 15-1 | 対象テーブル名である | 一致 | `notation.rst:793`（`- table: テーブル名`）＋`:799` |
| 15-2 | NTF により trim・大文字変換される | 記述なし | 本体 `TableData.java:97`（`3c4bd2a`。`tableName = name.trim().toUpperCase();`）。所見: 記述の揺れ（小文字・前後空白）が吸収されることは利用者に有用で、解説書にあってよい |
| 15-構造1 | `type: string` | 一致 | `notation.rst:793` |

### D16 `/$defs/table_data/properties/rows` — スキーマ `:108`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 16-1 | データ行であり、各要素がレコード1件（キー=カラム名、値=セル値）である | 一致 | `notation.rst:788`「`rows:` 配列に、各行をオブジェクトとして記述する」＋`:794`-`:797` |
| 16-2 | テーブル系の rows はオブジェクト配列である | 一致 | `notation.rst:788` |
| 16-3 | record_fragment の rows は配列の配列である | 一致 | `notation.rst:1119`「`rows:` の各行は配列形式で、`fields:` と同じ順序・同じ件数で値を並べる」 |
| 16-4 | 数値・真偽値も必ず文字列（クォート付き）で記述すること（例: AGE: "30"、FLAG: "true"） | 一致 | `notation.rst:1385`「`rows:` 内の全てのデータ値を必ずダブルクォートで囲む必要がある（クォートがないと SnakeYAML が数値・真偽値に型変換してしまう）」＋`:1406`-`:1411` |
| 16-5 | 値を Java null にする書き方はクォートなしの小文字 `null` である | 一致 | `notation.rst:809`-`:810`「null（Java の null）→ アンクォートの `null`」 |
| 16-6 | 値を Java null にするもう1つの書き方はキーだけ書いて値を省略した `COL:` である | 一致 | `notation.rst:638`・`:799`「後続の行がこのキーの一部を持たない場合、そのカラムは `null` を明示的に指定したのと同じ扱いになる」＋`:1486`「YAML 形式でキーを省略した場合は前述のとおり null」 |
| 16-7 | いずれもロード時点で null になる | 一致 | `notation.rst:809`-`:810`・`:1486` |
| 16-8 | クォート付きの `"null"` や大文字を含む `NULL` / `Null` は文字列としてロードされ、文字列のまま扱われる | 記述なし | 解説書 `:1400`-`:1401` は「文字列の null → `"null"`（クォートあり）」までで、大文字表記には触れていない。実測（本モジュールの依存 `snakeyaml-engine 3.0.1`）で `null` と値省略のみ Java null、`NULL`・`Null`・`"null"`・`"NULL"`・`~` はいずれも String になることを確認。所見: Excel 形式（`:1346` 大文字小文字不問）との差なので解説書にあってよい |
| 16-9 | 日付型カラムに `""` を指定すると null 扱いになる | 一致 | `notation.rst:813`-`:814`「日付型カラムの空文字 → `""`（null 扱い）」 |
| 16-10 | 文字型の `""` は空文字のまま INSERT される | 一致 | `notation.rst:811`-`:812`「空文字 → `""`」 |
| 16-11 | 値を1つも持たない空マッピング `{}` の行だけが、行が無いものとして取り除かれる | 一致 | `notation.rst:1486`「記法として空のエントリは読み飛ばされる。…YAML 形式では `rows:` 内の要素が空マッピング（`{}`）の場合である」 |
| 16-12 | `""` と書いた空文字は値であり、すべての値が `""` の行は取り除かれず、全カラムが空文字の行として読み込まれる | 一致 | `notation.rst:1486`「`""` と書いた空文字は値であり、すべての値が `""` のエントリは読み飛ばされず、全カラムが空文字のエントリとして読み込まれる」 |
| 16-13 | Java null（クォートなしの `null`・値を省略した `COL:`）と、文字列としてロードされる null 表記も値であり、これだけの行も残る | 記述なし | `YamlSection.java:226`-`:230`・`:232`-`:234`（`isBlankRow` は `castMap(row).isEmpty()` のみ）。所見: 解説書 `:1486` は `""` だけを挙げており、null だけの行が残ることは書かれていない。あるべき内容と考える |
| 16-14 | YAML 経路では NullInterpreter を指定しないため、前者は値が null に、後者は文字列のまま値になる | 記述なし | `YamlSection.java:344`-`:347`（`yamlInterpreters` に `NullInterpreter`・`QuotationTrimmer`・`LineSeparatorInterpreter` を指定しない）。所見: インタープリタ名は設定側の話で、解説書 `setup/common.rst` 側の題材 |
| 16-15 | この判定はマーカーカラム（`[COL]` のように `[` と `]` で囲んだ、DB 操作の対象外となるカラム）を除外する前に行われる | 一致 | `notation.rst:1486`「この判定はマーカーカラムを除外する前に行われる」＋`:1468`（マーカーカラムの定義） |
| 16-16 | そのためマーカーカラムだけに値がある行も取り除かれない | 一致 | `notation.rst:1486`「そのため、マーカーカラムだけに値があるエントリは読み飛ばされない」 |
| 16-17 | この除去はカラム名の決定より前に行われる | 記述なし | `YamlTableDataBuilder.java:93`-`:94`（`dropBlankRows` → `resolveColumns` の順）。所見: 解説書は `:799`（カラム名は先頭行）と `:1486`（`{}` の除去）を別々に述べるだけで、順序には触れていない。並び次第で結果が変わるため解説書にあってよい |
| 16-18 | カラム名は残った先頭の行のキーで決まる | 一致 | `notation.rst:799`「カラム名は、最初の行（`rows:` の先頭要素）のキーで決まる」 |
| 16-19 | 後続の行にしか無いキーは無視される | 一致 | `notation.rst:799`「後続の行に最初の行のキーにないものを追加しても、そのキーは読み込まれない」 |
| 16-20 | 除去の対象はテーブル系（table_data / list_map_data）の rows だけである | 一致 | `notation.rst:1486`「テーブルデータや `LIST_MAP` のエントリ自体を無いものとして扱う点で異なる」＋`:1468`（マーカーカラムが使えるのは `setup_tables`・`expected_tables`・`expected_complete_tables`・`list_maps`） |
| 16-21 | record_fragment の rows の要素は配列である | 一致 | `notation.rst:1119` |
| 16-22 | record_fragment の rows に空配列 `[]` を書いても取り除かれず、全フィールドが `""` のレコード1件として読み込まれる | 記述なし | `YamlFileBuilder.java:243`-`:267`（`List` であれば通り、`NablarchTestUtils.trimTailCopy` の後 `addValue` がフィールド数まで `""` で埋める）。所見: 解説書 `:863` は YAML では「`rows:` の値をすべて `""` とした行」を挙げており、空配列 `[]` には触れていない。等価であることは解説書にあってよい |
| 16-23 | rows が 0 行のとき（空配列 `[]`／除去で全行が消えた場合）、setup_tables では対象テーブルの全件 DELETE のみ実行される（INSERT なし） | 一致 | `notation.rst:713`＋`:816` |
| 16-24 | 同じく expected_tables / expected_complete_tables では対象テーブルに行が存在しないことの検証になる | 一致 | `notation.rst:715`「期待値を0件にすると、そのテーブルにレコードが1件もないことの検証になる」 |
| 16-25 | (1) カラム名決定行に無いカラムはカラム名の集合に入らない | 一致 | `notation.rst:799` |
| 16-26 | 全ての行で省略した場合のほか、カラム名決定行より後ろの行にだけ書いた場合も (1) に当たる | 一致 | `notation.rst:799` |
| 16-27 | 後者ではその行に書いた値は捨てられる | 一致 | `notation.rst:799`「そのキーは読み込まれない」 |
| 16-28 | (2) カラム名決定行にはあるが個々の行で省略したカラムは、その行でそのカラムに `null` を書いたのと同じ扱いになる（キーが無い状態ではなく値が null の状態で保持される） | 一致 | `notation.rst:638`・`:799` |
| 16-29 | カラム名は `{}` の行の除去のあとに残った先頭の行のキーだけで決まるため、同じカラムが (1) と (2) のどちらに当たるかは行の並びによって変わる | 記述なし | `YamlTableDataBuilder.java:93`-`:94`。所見: `:799` と `:1486` の合成であり、解説書に明示は無い。誤読を招きやすいので解説書にあってよい |
| 16-30 | (1) の setup_tables での挙動は、INSERT 時にカラム型ごとのデフォルト値が補完される | 一致 | `notation.rst:646`・`:674`・`:680` |
| 16-31 | 主キーは省略しないこと | 一致 | `notation.rst:646`「主キーカラムは記述を省略できない」 |
| 16-32 | (1) の expected_tables での挙動は、比較対象に入らない | 一致 | `notation.rst:660`・`:680` |
| 16-33 | (1) の expected_complete_tables での挙動は、型ごとのデフォルト値を補完したうえで DB の全カラムが比較される | 一致 | `notation.rst:680` |
| 16-34 | (2) の挙動は3セクション共通で、いずれも null として扱われる | 一致 | `notation.rst:638`（テーブルのデータを記述する節の important。3データタイプ共通の位置にある） |
| 16-35 | setup_tables ではデフォルト値の補完は行われず NULL が INSERT される | 一致 | `notation.rst:647`「NULL にしたいカラムは、省略ではなく `null` を明示的に指定する（カラムの省略は NULL の指定と等価ではない）」＋`:638` |
| 16-36 | NOT NULL 制約のあるカラムでは INSERT が失敗する | 記述なし | DB の一般則であり、解説書にも実装にも明示の根拠は無い（**未確認**）。所見: 一般則なので解説書に無くてよい |
| 16-37 | Boolean 型カラムは例外で、値が null になれば NullPointerException になる（原因が (2) の行ごとの省略でも、クォートなし小文字 `null`・`COL:` の値省略でも同じ） | 記述なし | 本体 `TableData.java:162`-`:164`（`3c4bd2a`。`insert.setBoolean(bindIndex++, row.containsKey(columnName) ? row.getBoolean(columnName) : (Boolean) getDefaultValue(columnName))`。キーがあれば `getBoolean` の結果がそのまま `boolean` へアンボクシングされる）。所見: 落とし穴なので解説書にあってよい |
| 16-38 | そのため true/false のいずれかの値を明示すること（null を明示しても NullPointerException を防げない） | 記述なし | 16-37 と同じ |
| 16-39 | expected_tables / expected_complete_tables では (2) は期待値 null として比較される（比較対象から外れることはない） | 一致 | `notation.rst:638` |
| 16-40 | setup_tables の rows であるカラムをカラム名決定行で省略すると、数値型カラムには NTF がデフォルト値 `"0"` を補完して INSERT する | 一致 | `notation.rst:647`「省略すると値 `0` が INSERT され」＋`:690`-`:691` |
| 16-41 | FK 制約のある数値カラムがカラム名決定行に無いと `"0"` が INSERT され、参照先テーブルに ID=0 の行が存在しない場合は FK 制約違反になる | 一致 | `notation.rst:647`「参照先テーブルに ID=0 の行がない場合は FK 違反でエラーになる」 |
| 16-42 | FK カラムは必ず明示的に値を記述すること | 一致 | `notation.rst:647`「FK が設定された数値カラムも同様に省略できない」 |
| 16-43 | NULL 許容カラムを NULL にしたい場合は省略せず `null`（クォートなし）を明示すること | 一致 | `notation.rst:647`＋`:810` |
| 16-44 | (1) の省略は NULL ではなくデフォルト値の補完を意味する | 一致 | `notation.rst:647`「カラムの省略は NULL の指定と等価ではない」 |
| 16-45 | (2) に当たるかどうかは行の並びに依存する | 記述なし | 16-29 と同じ |
| 16-46 | Boolean 型カラムは `null` を明示しても NULL にはならず NullPointerException になるため、NULL 許容カラムであっても true/false のいずれかを明示すること | 記述なし | 16-37 と同じ |
| 16-47 | バックスラッシュと `r` の2文字（`"\\r"`）を含む値はエラーになる | 一致 | `notation.rst:1429`「バックスラッシュと `r` の2文字（`"\\r"`）を含む値は書けない。…YAML 形式ではエラーになる」 |
| 16-48 | Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値はテスティングフレームワークの仕様上存在しない | 一致 | `notation.rst:1429`「Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値はテスティングフレームワークの仕様上存在せず」 |
| 16-構造1 | `type: array`（`minItems`・`maxItems` の制約なし） | 一致 | `notation.rst:711`「テーブルデータは、データ行を1行も記述しないことで0件にできる」＋`:816`（上限の定めは解説書に無い） |
| 16-構造2 | `items.type: object` | 一致 | `notation.rst:788` |
| 16-構造3 | `items.additionalProperties.type: ["string","null"]`（値は文字列または null） | 一致 | `notation.rst:1385`（全データ値はダブルクォート）＋`:809`-`:810`（アンクォートの `null`） |
| 16-構造4 | カラム名（キー）に制約を課さない（任意のキーを許す） | 一致 | `notation.rst:799`（カラム名は先頭行のキーで決まる。名前の制約は解説書に無い）＋`:1468`（マーカーカラム `[COL]` も書ける） |

### D17 `/$defs/list_map_data` — スキーマ `:128`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 17-1 | LIST_MAP データ1ブロックである | 一致 | `notation.rst:566`・`:632` |
| 17-2 | list_maps の各エントリ1件に対応する | 一致 | `notation.rst:200`-`:201`・`:614`「`list_maps:` 下の、指定した ID を持つエントリに記載する」 |
| 17-3 | id が重複した場合は最初の1件のみ有効（2件目以降は無視） | 一致 | `notation.rst:602`「同一の読み込み単位内に同じ ID のデータブロックが複数ある場合は先着一致となり、2件目以降は無視される」＋`:143` |
| 17-構造1 | `id` は必須 | 一致 | `notation.rst:614`・`:618`-`:619` |
| 17-構造2 | `rows` は必須 | 記述なし | スキーマ `:123`-`:126`／`YamlLoader.java:154`。所見: 解説書はテーブルデータについては `:816` で 0 件でも `rows: []` を書くことを求めているが、`LIST_MAP` については書いていない。同旨を YAML 形式の節（`:612`-`:624`）に書いてよい |
| 17-構造3 | `additionalProperties: false`（`id`・`rows` 以外のキーは検証エラー） | 記述なし | スキーマ `:127`。所見: `group_id` を持たない点（テーブル・ファイル系との差）が読み取れるので解説書にあってよい |

### D18 `/$defs/list_map_data/properties/id` — スキーマ `:132`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 18-1 | 識別IDであり、完全一致で検索される | 一致 | `notation.rst:602`「`LIST_MAP` の ID は完全一致で検索される」 |
| 18-2 | `testShots` は予約IDである | 一致 | `notation.rst:340`「`testShots` は、テストショット一覧を表す予約 ID である」 |
| 18-3 | テストケース定義として NTF が自動読み込みし各行を1テストケースとして実行する | 一致 | `notation.rst:340`「フレームワークがこの ID を持つデータブロックを自動的に読み込み、各エントリを1つのテストショットとして実行する」 |
| 18-4 | 1件以上の rows が必須で0件はエラーになる | 一致 | `notation.rst:340`「テスト実行には `testShots` に1件以上のエントリが必要である」 |
| 18-5 | ファイル内で重複した場合は先着1件のみ有効 | 一致 | `notation.rst:602` |
| 18-構造1 | `type: string`（`minLength` なし） | 記述なし | スキーマ `:131`。所見: 解説書は ID の空文字について述べていない。`group_id` の `minLength: 1` と非対称なので、意図した非対称なら解説書に書くとよい |

### D19 `/$defs/list_map_data/properties/rows` — スキーマ `:136`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 19-1 | データ行であり、各要素が `Map<String,String>` の1件である | 一致 | `notation.rst:566`「読み込むと、Java の `List<Map<String, String>>` 形式のオブジェクトとして取得できる」 |
| 19-2 | NTF はこのマップをテストコードへそのまま渡す | 一致 | `notation.rst:570`-`:600`（`getListMap` で取得できる `List` と等価であることを例示） |
| 19-3 | 数値・真偽値も必ず文字列（クォート付き）で記述すること | 一致 | `notation.rst:1385`・`:1406`-`:1411` |
| 19-4 | 値を1つも持たない空マッピング `{}` の行だけが、行が無いものとして取り除かれる | 一致 | `notation.rst:1486` |
| 19-5 | 取り除かれた行はテストコードへ渡すマップの件数に数えられない | 一致 | `notation.rst:1486`「テーブルデータや `LIST_MAP` のエントリ自体を無いものとして扱う」 |
| 19-6 | `""` と書いた空文字は値であり、すべての値が `""` の行は取り除かれず、全キーの値が空文字のマップ1件として渡される | 一致 | `notation.rst:1486` |
| 19-7 | Java null（クォートなしの `null`・値を省略した `COL:`）と、文字列としてロードされる `"null"` / `NULL` も値であり、これだけの行も残る | 記述なし | `YamlSection.java:226`-`:230`・`:232`-`:234`。16-13 と同じ（所見も同じ） |
| 19-8 | YAML 経路では NullInterpreter を指定しないため、前者はマップの値が Java null に、後者は文字列のままマップの値になる | 記述なし | `YamlSection.java:344`-`:347`。16-14 と同じ |
| 19-9 | この判定はマーカーカラム（`[COL]` のようにマップから除外されるキー）を除外する前に行われる | 一致 | `notation.rst:1486`＋`:1468`（マーカーカラムは `list_maps` でも使える） |
| 19-10 | そのためマーカーカラムだけに値がある行も取り除かれない | 一致 | `notation.rst:1486` |
| 19-11 | この除去はキーの決定より前に行われる | 記述なし | `YamlTableDataBuilder.java:185`-`:186`（`dropBlankRows` → `resolveColumns`）。16-17 と同じ |
| 19-12 | キーは残った先頭の行のキーで決まり、後続の行にしか無いキーは無視される | 一致 | `notation.rst:632`（`LIST_MAP` もテーブルデータと「カラム名・データ行」の構成を共有する）＋`:799`（その構成における YAML のカラム名決定規則） |
| 19-13 | この規則は table_data の rows と共通で、record_fragment の rows には適用されない | 一致 | `notation.rst:1486`（テーブルデータ・`LIST_MAP` が対象）＋`:1468`（マーカーカラムが使える4データタイプ） |
| 19-14 | 全行が取り除かれると空リストになる | 一致 | `notation.rst:1486`（エントリ自体を無いものとして扱う） |
| 19-15 | 予約ID `testShots` では、rows を0件書いた場合と同じくエラーになる | 一致 | `notation.rst:340`「テスト実行には `testShots` に1件以上のエントリが必要である」 |
| 19-16 | バックスラッシュと `r` の2文字（`"\\r"`）を含む値はエラーになる | 一致 | `notation.rst:1429` |
| 19-17 | Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値は仕様上存在しない | 一致 | `notation.rst:1429` |
| 19-構造1 | `type: array`（件数制約なし） | 一致 | `notation.rst:340`（`testShots` は1件以上。一般の `LIST_MAP` に件数制約は解説書に無い） |
| 19-構造2 | `items.type: object` | 一致 | `notation.rst:618`-`:624`（`rows:` の各要素はキー: 値のマッピング） |
| 19-構造3 | `items.additionalProperties.type: ["string","null"]` | 一致 | `notation.rst:1385`・`:809`-`:810` |

### D20 `/$defs/file_data` — スキーマ `:157`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 20-1 | ファイルデータ1ブロックである | 一致 | `notation.rst:830`「固定長ファイル・可変長ファイルに対応するテストデータ（ファイルデータ）は、`SETUP_FIXED`・`EXPECTED_FIXED`（固定長）、`SETUP_VARIABLE`・`EXPECTED_VARIABLE`（可変長）のいずれかのデータタイプで記述する」 |
| 20-2 | setup_files / expected_files の各エントリ1件に対応する | 一致 | `notation.rst:202`-`:205` |
| 20-3 | 1ファイル分のディレクティブ・レコード定義・データ行を含む | 一致 | `notation.rst:832`-`:838`（ディレクティブ → レコード種別＋フィールド名称 → データ型 → フィールド長 → データ） |
| 20-構造1 | `path`・`type`・`records` は必須 | 一致 | `notation.rst:1122`「`setup_files`・`expected_files` の各エントリには `path`・`type`・`records` の3キーが必須である」 |
| 20-構造2 | `additionalProperties: false`（`group_id`・`path`・`type`・`directives`・`records` 以外は検証エラー） | 記述なし | スキーマ `:156`。所見: 解説書はキーの列挙（`:1103`-`:1122`）だけで、閉じていることは述べていない |

### D21 `/$defs/file_data/properties/group_id` — スキーマ `:162`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 21-1 | データブロックの収集単位である | 一致 | `notation.rst:232`・`:248`-`:250` |
| 21-2 | testShots の setUpFile / expectedFile 等のカラムで指定した値と一致するブロックが全件収集される | 一致 | `notation.rst:464`-`:471`（`setUpFile`・`expectedFile`）＋`:1027`-`:1028` |
| 21-3 | 省略時はグループIDなし（デフォルトグループ）扱いになる | 一致 | `notation.rst:234` |
| 21-4 | 空文字 `""` は誤マッチを引き起こすため `minLength: 1` で禁止される | 記述なし | スキーマ `:161`／`YamlSection.java:448`-`:451`。14-7 と同じ |
| 21-構造1 | `type: string`／`minLength: 1` | 記述なし | 21-4 と同じ |

### D22 `/$defs/file_data/properties/path` — スキーマ `:166`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 22-1 | ファイルパスである | 一致 | `notation.rst:1029`-`:1030`「ファイルパス: カレントディレクトリからのファイルパス（ファイル名を含む）を記載する」＋`:1104` |
| 22-2 | setup_files では NTF がこのパスに入力ファイルを配置する | 一致 | `notation.rst:99`（テスト実行前にデータベース・ファイルへ投入するデータ）＋`:464`-`:465` |
| 22-3 | expected_files では NTF がこのパスに出力されたファイルの内容と期待値を照合する | 一致 | `notation.rst:470`-`:471`（`expectedFile` = 期待する出力ファイルのグループID）＋`:880` |
| 22-構造1 | `type: string` | 一致 | `notation.rst:1104` |

### D23 `/$defs/file_data/properties/type` — スキーマ `:174`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 23-1 | ファイル種別である | 一致 | `notation.rst:1105`（`type: fixed`）＋`:1122` |
| 23-2 | fixed = 固定長（SETUP_FIXED / EXPECTED_FIXED） | 一致 | `notation.rst:202`・`:204`＋`:144`-`:149` |
| 23-3 | variable = 可変長（SETUP_VARIABLE / EXPECTED_VARIABLE） | 一致 | `notation.rst:202`・`:204`＋`:150`-`:155` |
| 23-4 | NTF はこの値に応じてパーサ・フォーマッタを切り替える | 一致 | `notation.rst:878`「固定長か可変長かは、データブロック内の記述で区別される」。実装 `YamlFileBuilder.java:85`-`:87`（`FixedLengthFile`／`VariableLengthFile` の切り替え） |
| 23-構造1 | `enum: ["fixed","variable"]` | 一致 | `notation.rst:1105`・`:202`-`:205`（2種のみ） |

### D24 `/$defs/file_data/properties/records` — スキーマ `:182`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 24-1 | レコード種別ごとのブロックである | 一致 | `notation.rst:1033`-`:1034`「レコード種別: レコード種別を記載する。複数レコードレイアウトの場合は、この記述を連続して記載する」＋`:1108`-`:1114` |
| 24-2 | 空配列 `[]` を指定すると 0バイトの空ファイルを表現する | 一致 | `notation.rst:1122`「0バイトの空ファイルを表現するには、`records:` に空配列 `[]` を記載する」＋`:861` |
| 24-3 | setup_files では空ファイルを配置する | 一致 | `notation.rst:861`＋`:99`・`:464`-`:465` |
| 24-4 | expected_files では出力ファイルが空であることを検証する | 一致 | `notation.rst:861`＋`:880` |
| 24-構造1 | `type: array`／`minItems: 0` | 一致 | `notation.rst:1122` |

### D25 `/$defs/message_data` — スキーマ `:196`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 25-1 | messages エントリ1件である | 一致 | `notation.rst:206`-`:207`・`:1264`-`:1277` |
| 25-2 | **（是正前）MockMessaging 経路の要求/応答電文1メッセージを表す** | **矛盾（C2）** | `notation.rst:1130`。根拠は 8-1 と同じ（本体 `MQSupport.java:64`・`:74`／`MockMessagingContext.java:97`・`:99`） |
| 25-3 | id で完全一致検索され先着1件のみ有効 | 一致 | `notation.rst:246`・`:158` |
| 25-4 | fw_header で FW 制御ヘッダを指定する | 一致 | `notation.rst:1260` |
| 25-5 | records で電文本文フィールド（型・長さつき）を定義する | 一致 | `notation.rst:1260`・`:1271`-`:1277` |
| 25-構造1 | `id`・`records` は必須 | 一致 | `notation.rst:1132`（識別子は固定値）＋`:1133`（メッセージボディはファイルデータと同じ構成） |
| 25-構造2 | `records` は `minItems: 1`／`maxItems: 1` | 一致 | `notation.rst:1283`「`records:` に記述するレコードレイアウトは1つである。2つ以上記述するとエラーになる」＋`:1134` |
| 25-構造3 | `additionalProperties: false`（`id`・`directives`・`records`・`fw_header` 以外は検証エラー） | 記述なし | スキーマ `:195`。所見: `group_id` を持たない点（`:1246` の `MESSAGE=setUpMessages` にグループID書式が無いこと）と対応する。解説書にあってよい |

### D26 `/$defs/message_data/properties/id` — スキーマ `:200`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 26-1 | メッセージIDであり、データタイプ MESSAGE の識別子である | 一致 | `notation.rst:156`-`:157`・`:1132` |
| 26-2 | `setUpMessages`（要求電文）・`expectedMessages`（応答電文）という固定値を指定する | 一致 | `notation.rst:1132`「これらの識別子は固定である」 |
| 26-3 | `sendSyncTestData` は、取引単体テストのモックアップクラスが読む同期応答メッセージ送信のテストデータのベースディレクトリに付けるコンポーネント設定のキーである | 一致 | `notation.rst:1132`＋`setup/common.rst:168`・`:189` |
| 26-4 | `sendSyncTestData/{requestId}/message` は読み込み単位（リクエストID と同じ名前のディレクトリ配下の `message.yaml`）を指すパスである | 一致 | `notation.rst:1132`「YAML 形式ではリクエスト ID と同じ名前のディレクトリ配下の `message.yaml` である（`message` は固定の名前）」 |
| 26-5 | それはデータブロックの識別子ではない | 一致 | `notation.rst:1132`「`sendSyncTestData` はデータブロックの識別子ではない」 |
| 26-6 | ファイル内で重複した場合は先着1件のみ有効 | 一致 | `notation.rst:158`・`:246` |
| 26-構造1 | `type: string` | 一致 | `notation.rst:1265`（`- id: setUpMessages`） |

### D27 `/$defs/message_data/properties/records` — スキーマ `:209`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 27-1 | 電文本文のレコード定義である | 一致 | `notation.rst:1133`「フレームワーク制御ヘッダ以降のメッセージボディは、フィールド名称・データ型・フィールド長・データという、前述のファイルデータと同じ構成を持つ」＋`:1260` |
| 27-2 | records に記述するレコードレイアウトは1つであり、2つ以上記述するとエラーになる | 一致 | `notation.rst:1283` |
| 27-3 | 電文はファイルデータのように複数のレコードレイアウトを持たない | 一致 | `notation.rst:1134`「ただし、電文のレコードレイアウトは1つであり、ファイルデータのように複数のレコードレイアウトを持たない」 |
| 27-4 | FW 制御ヘッダは fw_header に記述するため records には含めない | 一致 | `notation.rst:1260` |
| 27-5 | 旧形式の `record_type: FW_HEADER` は廃止された | 一致 | `notation.rst:1287`「旧版ではフレームワーク制御ヘッダを `record_type: FW_HEADER` のレコードとして表していたが、現在の仕様では…`record_type` に特別な予約値はない」 |
| 27-6 | この records の record_type は内部で常に `"default"` に置換される | 一致 | `notation.rst:1144`「`MESSAGE`（`setUpMessages`・`expectedMessages`）では、記載した値は使われず、デフォルトのレコード種別（`"default"`）になる」 |
| 27-7 | その置換を行うのは `MessageParser` である | 記述なし | 解説書にクラス名は無い。**所見: YAML 経路で置換するのは `YamlFileBuilder.java:206`-`:208`（`keepRecordType=false`）であり、`MessageParser` は Excel 経路（本体）の実装である。YAML のスキーマ description にこのクラス名を書くのは誤解を招く。指示書 §1 の「矛盾」（解説書との食い違い）には当たらないため今回は変更していない。ユーザー判断を仰ぐ** |
| 27-構造1 | `type: array`／`minItems: 1`／`maxItems: 1` | 一致 | `notation.rst:1283` |

### D28 `/$defs/message_data/properties/fw_header` — スキーマ `:216`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 28-1 | FW 制御ヘッダ（キー: 値）である | 一致 | `notation.rst:1260`・`:1279` |
| 28-2 | **（是正前）messages（MESSAGE: MockMessaging 経路）でのみ使用する** | 一致（「messages でのみ使用する」）／**矛盾（C3。経路名部分）** | 「messages でのみ」は `notation.rst:1260`「`fw_header:` マップは `messages`（`MESSAGE`）でのみ使用し」で一致。「MockMessaging 経路」は 8-1 と同じ理由で矛盾 |
| 28-3 | 記載できるキーは `reader.fwHeaderfields` に指定した名前だけである | 一致 | `notation.rst:1279`「`fw_header:` に記載できるキーは、`reader.fwHeaderfields` の名前（省略時は `requestId`・`userId`・`resendFlag`・`resultCode`）だけである」 |
| 28-4 | 省略時は requestId, userId, resendFlag, resultCode である | 一致 | `notation.rst:1279`・`:1150` |
| 28-5 | それ以外のキーがあるとエラーになる | 一致 | `notation.rst:1279`「それ以外のキーがあるとエラーになる」 |
| 28-6 | キー名の検査は、そのエントリの電文を読み出したときに行われる | 記述なし | `YamlMessageBuilder.java:118`-`:135`（`buildMessageContent` が id 一致時にだけ `convertFwHeader` を呼ぶ）・`:298`-`:328`。所見: 検査の契機は利用者の体験（どのエントリで落ちるか）に直結するので解説書にあってよい |
| 28-7 | 誤記のあるエントリを読み出したときだけエラーになり、同一ファイル内の他のエントリの読み出しは巻き添えにならない | 記述なし | `YamlMessageBuilder.java:36`-`:44`・`:118`-`:135`。28-6 と同じ |
| 28-8 | 値は数値・真偽値も必ず文字列（クォート付き）で記述すること（例: requestId: `"0000000001"`） | 記述なし | スキーマ `:430`-`:432`（`$defs.fw_header` の `additionalProperties: {"type":"string"}`）。所見: 解説書 `:1385` のクォート必須は `rows:` 内の値に限った記述であり、`fw_header:` の値には触れていない（`:1268`-`:1270` の例はクォートなしの文字列）。あるべき内容と考える |
| 28-9 | 値の型はキー名と違ってこのスキーマ自身が課す制約である | 記述なし | スキーマ `:430`-`:432`＋`:434`（`$comment`）。28-8 と同じ |
| 28-10 | クォートなしの数値・真偽値を1つでも書くとロード時にファイル全体がエラーになり、他のエントリも読み出せなくなる | 記述なし | `YamlLoader.java:154`-`:157`（ロード時に全体をスキーマ検証）。28-8 と同じ |
| 28-11 | バックスラッシュと `r` の2文字（`"\\r"`）を含む値はエラーになる | 一致 | `notation.rst:1429` |
| 28-12 | バックスラッシュと `r` の2文字を含む**キー名**もエラーになる | 記述なし | `YamlMessageBuilder.java:311`-`:317`（許可キー判定より前に `rejectLiteralCr(key, source)`）。所見: 解説書 `:1429` は「値」だけを対象にしている。Excel 形式ではキーも値も同じセルで `interpret` を通る（`YamlSection.java:388`-`:392`）ため、同旨を解説書に書いてよい |
| 28-13 | Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値は仕様上存在しない | 一致 | `notation.rst:1429` |
| 28-構造1 | `$ref: "#/$defs/fw_header"`（マッピング） | 一致 | `notation.rst:1260`・`:1268`-`:1270` |

### D29 `/$defs/expected_request_message_data` — スキーマ `:224`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 29-1 | expected_request_header_messages / expected_request_body_messages のエントリ1件である | 一致 | `notation.rst:208`-`:211` |
| 29-2 | 要求電文の期待値を表す | 一致 | `notation.rst:1161`-`:1166` |
| 29-3 | fw_header は使用しない | 一致 | `notation.rst:1260`「`EXPECTED_REQUEST_HEADER_MESSAGES`・`EXPECTED_REQUEST_BODY_MESSAGES`・`RESPONSE_HEADER_MESSAGES`・`RESPONSE_BODY_MESSAGES` の4種では使わず」 |
| 29-4 | requestId 等のヘッダフィールドも含め records の fields/rows にフィールド単位で定義する | 一致 | `notation.rst:1260`「`requestId` などのヘッダフィールドも含めて `records` の `fields:`・`rows:` にフィールド単位で記載する」 |
| 29-構造1 | `id`・`records` は必須 | 一致 | `notation.rst:1161`-`:1166`（設定値=リクエストID）＋`:1133` |
| 29-構造2 | `records` は `minItems: 1`／`maxItems: 1` | 一致 | `notation.rst:1283`・`:1134` |
| 29-構造3 | `additionalProperties: false` | 記述なし | スキーマ `:223`。25-構造3 と同じ所見 |

### D30 `/$defs/expected_request_message_data/properties/group_id` — スキーマ `:230`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 30-1 | データブロックの収集単位である | 一致 | `notation.rst:232`・`:248`-`:250` |
| 30-2 | testShots の expectedMessage カラムで指定した値と一致するブロックが全件収集される | 一致 | `notation.rst:420`-`:421`（`expectedMessage` = 期待する要求電文のグループID）＋`:525`-`:526`＋`:161`・`:163` |
| 30-3 | 省略時は id 直接指定（先着1件）で動作する | 一致 | `notation.rst:161`・`:163`「グループID指定時は全件収集、ID直接指定時は最初の1件」 |
| 30-4 | 空文字 `""` は誤マッチを引き起こすため `minLength: 1` で禁止される | 記述なし | スキーマ `:229`／`YamlSection.java:448`-`:451`。14-7 と同じ |
| 30-構造1 | `type: string`／`minLength: 1` | 記述なし | 30-4 と同じ |

### D31 `/$defs/expected_request_message_data/properties/id` — スキーマ `:234`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 31-1 | リクエストIDである | 一致 | `notation.rst:1161`-`:1166`（設定値の列がすべて「リクエストID」） |
| 31-2 | NTF がフォーマット定義ファイル（`{requestId}_SEND`）を解決するために使用する | 一致 | `notation.rst:1188`「フォーマット定義ファイルの命名は、応答電文が `{requestId}_RECEIVE`、要求電文が `{requestId}_SEND` という規則に従う」 |
| 31-3 | group_id を指定した全件収集モードでも id は必須である | 一致 | `notation.rst:1188`「同一グループIDを持つ複数のメッセージプールを収集する場合、識別子の値をリクエストIDとして使用する」＋`:1246`（`EXPECTED_REQUEST_BODY_MESSAGES[グループID]=リクエストID`） |
| 31-構造1 | `type: string` | 一致 | `notation.rst:1161`-`:1166`（設定値はリクエストID）＋`testdata_examples.rst:1969`「`id:` にはリクエスト ID を記述する」 |

### D32 `/$defs/expected_request_message_data/properties/records` — スキーマ `:243`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 32-1 | 電文フィールドのレコード定義である | 一致 | `notation.rst:1260`・`:1133` |
| 32-2 | records に記述するレコードレイアウトは1つであり、2つ以上記述するとエラーになる | 一致 | `notation.rst:1283` |
| 32-3 | 電文はファイルデータのように複数のレコードレイアウトを持たない | 一致 | `notation.rst:1134` |
| 32-4 | requestId 等の FW 制御ヘッダフィールドも含め、すべてのフィールドをここに定義する（fw_header は使用しない） | 一致 | `notation.rst:1260` |
| 32-構造1 | `type: array`／`minItems: 1`／`maxItems: 1` | 一致 | `notation.rst:1283` |

### D33 `/$defs/group_message_data` — スキーマ `:257`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 33-1 | response_header_messages / response_body_messages のエントリ1件である | 一致 | `notation.rst:212`-`:215` |
| 33-2 | (A) RequestTestingSendSyncSupport 経路では group_id でフィルタリングして全件収集する | 一致（挙動）／記述なし（クラス名） | 挙動: `notation.rst:167`・`:169`「グループID指定時は全件収集」。クラス名は解説書に無い。実装 `YamlMessageBuilder.java:146`-`:167` |
| 33-3 | この経路で使う場合は group_id が必須である | 一致 | `notation.rst:1246`「グループID付きの書式例は `EXPECTED_REQUEST_BODY_MESSAGES[グループID]=リクエストID` である」＋`:420`-`:421`（`responseMessage` カラムにグループIDを指定）＋`:1254`（取引単体テスト側は「識別子はグループIDを持たず」） |
| 33-4 | (B) MockMessagingContext / MockMessagingClient 経路では id で照合して先着1件収集する（group_id 不要） | 一致（挙動）／記述なし（クラス名） | 挙動: `notation.rst:1207`「識別子の書式がグループIDを持たない点が同期応答メッセージ送信のテストと異なり」＋`:1254`「識別子はグループIDを持たず、`EXPECTED_REQUEST_HEADER_MESSAGES=リクエストID` のように記載する」 |
| 33-5 | group_id を省略した場合は経路 B として動作する | 一致 | `notation.rst:1207`・`:1254` |
| 33-構造1 | `id`・`records` は必須 | 一致 | `notation.rst:1167`-`:1172`（設定値=リクエストID）＋`:1133` |
| 33-構造2 | `records` は `minItems: 1`／`maxItems: 1` | 一致 | `notation.rst:1283`・`:1134` |
| 33-構造3 | `additionalProperties: false` | 記述なし | スキーマ `:256`。25-構造3 と同じ所見 |

### D34 `/$defs/group_message_data/properties/group_id` — スキーマ `:262`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 34-1 | データブロックの収集単位である | 一致 | `notation.rst:232`・`:248`-`:250` |
| 34-2 | RequestTestingSendSyncSupport 経路でフィルタリングに使用される | 一致（挙動）／記述なし（クラス名） | 挙動: `notation.rst:167`・`:169`＋`:420`-`:421` |
| 34-3 | MockMessagingContext / MockMessagingClient 経路では参照されないため省略可 | 一致（挙動）／記述なし（クラス名） | 挙動: `notation.rst:1207`・`:1254` |
| 34-4 | 空文字 `""` は誤マッチを引き起こすため `minLength: 1` で禁止される | 記述なし | スキーマ `:261`／`YamlSection.java:448`-`:451`。14-7 と同じ |
| 34-構造1 | `type: string`／`minLength: 1` | 記述なし | 34-4 と同じ |

### D35 `/$defs/group_message_data/properties/id` — スキーマ `:266`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 35-1 | メッセージ識別子である | 一致 | `notation.rst:1167`-`:1172`（設定値=リクエストID） |
| 35-2 | MockMessagingContext / MockMessagingClient 経路ではこの値で照合される | 一致（挙動）／記述なし（クラス名） | 挙動: `notation.rst:1254`「`EXPECTED_REQUEST_HEADER_MESSAGES=リクエストID` のように記載する」＋`testdata_examples.rst:1969`「`id:` にはリクエスト ID を記述する」 |
| 35-3 | RequestTestingSendSyncSupport 経路では group_id でのフィルタリング後に識別子として扱われる | 一致（挙動）／記述なし（クラス名） | 挙動: `notation.rst:1188`「同一グループIDを持つ複数のメッセージプールを収集する場合、識別子の値をリクエストIDとして使用する」 |
| 35-構造1 | `type: string` | 一致 | `testdata_examples.rst:1969` |

### D36 `/$defs/group_message_data/properties/records` — スキーマ `:275`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 36-1 | 電文フィールドのレコード定義である | 一致 | `notation.rst:1260`・`:1133` |
| 36-2 | records に記述するレコードレイアウトは1つであり、2つ以上記述するとエラーになる | 一致 | `notation.rst:1283` |
| 36-3 | 電文はファイルデータのように複数のレコードレイアウトを持たない | 一致 | `notation.rst:1134` |
| 36-4 | 先頭値が `errorMode:timeout` または `errorMode:msgException` の行を含めると送受信エラーをシミュレートできる | 一致 | `notation.rst:1220`・`:1229`-`:1234` |
| 36-5 | **（是正前）RequestTestingSendSyncSupport 経路では errorMode は無視される** | **矛盾（C6）** | `notation.rst:1238`。根拠は 11-6 と同じ（本体 `RequestTestingMessagePool.java:78`-`:84`／`RequestTestingMessagingProvider.java:203`・`:205`／`RequestTestingMessagingClient.java:228`・`:231`） |
| 36-構造1 | `type: array`／`minItems: 1`／`maxItems: 1` | 一致 | `notation.rst:1283` |

### D37 `/$defs/directives` — スキーマ `:285`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 37-1 | ファイルディレクティブである | 一致 | `notation.rst:882`「ディレクティブは、ファイル・電文のフォーマットに関する属性を、キー名と値の2要素で記述するものである」 |
| 37-2 | NTF のフォーマット定義に渡すファイル属性を指定する | 一致 | `notation.rst:882` |
| 37-3 | 固定長（type=fixed）と可変長（type=variable）で有効なキーが異なる | 一致 | `notation.rst:884`「固定長ファイルで有効なディレクティブキーは、以下の11個に限定される」＋`:911`「可変長ファイルで有効なディレクティブキーは、以下の9個に限定される」 |
| 37-構造1 | 列挙する17キーは、固定長11キーと可変長9キーの和集合である（共通3キー: `file-type`・`text-encoding`・`record-separator`） | 一致 | `notation.rst:886`-`:936`（11 + 9 − 3 = 17。スキーマ `:287`-`:354` の17キーと一致） |
| 37-構造2 | `additionalProperties: false`（17キー以外は検証エラー） | 一致 | `notation.rst:884`・`:911`「限定される」 |
| 37-構造3 | 型別（fixed/variable）の限定はスキーマで表現していない | 一致（description の主張）／未表現（L1） | `notation.rst:884`・`:911` は型別に限定しているが、スキーマは17キーを一律に許す。**description は「有効なキーが異なる」と正しく述べており矛盾しない。構造制約が緩い点は L1 として報告する（変更しない）** |

### D38 `/$defs/directives/properties/text-encoding` — スキーマ `:289`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 38-1 | 固定長・可変長の共通ディレクティブである | 一致 | `notation.rst:894`-`:895`（固定長）・`:921`-`:922`（可変長） |
| 38-2 | 文字エンコーディングである | 一致 | `notation.rst:895`「ファイルの文字エンコーディング」 |
| 38-3 | 例として UTF-8・MS932 を挙げる | 一致（MS932）／記述なし（UTF-8） | `notation.rst:1107`（`text-encoding: MS932`）。UTF-8 は解説書の例に無い（`:1091`・`:1267` は `Windows-31J`）。所見: 例示であり挙動の主張ではない |
| 38-構造1 | `type: string` | 一致 | `notation.rst:1107` |

### D39 `/$defs/directives/properties/record-separator` — スキーマ `:293`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 39-1 | 固定長・可変長の共通ディレクティブである | 一致 | `notation.rst:898`-`:899`・`:923`-`:924` |
| 39-2 | レコード区切り文字である | 一致 | `notation.rst:899`「レコード区切り」 |
| 39-3 | 改行コードは `NONE` / `CR` / `LF` / `CRLF` のシンボルで指定する | 一致 | `notation.rst:899`「改行コードは `NONE`/`CR`/`LF`/`CRLF` のシンボルで指定する」 |
| 39-4 | シンボル以外の文字列を書いた場合は、その文字列自身が区切り文字になる | 一致 | `notation.rst:899`「シンボル以外を記述した場合は、その文字列自身が区切り文字になる」 |
| 39-5 | バックスラッシュと `r` の2文字（`"\\r"`）を含む値はエラーになる | 一致 | `notation.rst:1429` |
| 39-6 | Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値は仕様上存在しない | 一致 | `notation.rst:1429` |
| 39-7 | YAML のダブルクォート文字列に `"\r\n"` と書くと実際の制御文字に展開される | 一致 | `notation.rst:1429`「YAML のパーサが制御文字に変換する」＋`:1427`-`:1428`（`"\r"`（CR）・`"\n"`（LF）） |
| 39-8 | NTF が値を trim する際に除去されて区切りが空文字になる（エラーにならないため注意） | 記述なし | 本体 `DataFile.java:304`（`3c4bd2a`。`convertDirectiveValue(directive, stringValue.trim())`）。所見: 気づきにくい落とし穴で、解説書の YAML 形式の節にあってよい |
| 39-構造1 | `type: string` | 一致 | `notation.rst:899` |

### D40 `/$defs/directives/properties/file-type` — スキーマ `:297`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 40-1 | 固定長・可変長の共通ディレクティブである | 一致 | `notation.rst:892`-`:893`・`:919`-`:920` |
| 40-2 | ファイル種別（固定長=Fixed、可変長=Variable）である | 一致 | `notation.rst:893`「自動設定される（`"Fixed"`）」・`:920`「自動設定される（`"Variable"`）」 |
| 40-3 | type フィールドから自動設定されるため通常は記述不要である | 一致 | `notation.rst:893`・`:920`「通常は記述不要」＋`:1174` |
| 40-4 | 明示した場合は自動設定値を上書きする | 記述なし | 本体 `DataFile.java:89`-`:92`（コンストラクタが `setDirective("file-type", getFileType())` を先に実行）＋`YamlFileBuilder.java:282`-`:290`（`applyDirectives` は生成後に呼ぶため上書きになる）。解説書 `:1190`・`:1205` は電文について `file-type` を明示設定する運用を述べているが、ファイルの自動設定値を上書きする旨は書いていない。所見: 解説書にあってよい |
| 40-構造1 | `type: string` | 一致 | `notation.rst:893`・`:920` |

### D41 `/$defs/directives/properties/record-length` — スキーマ `:301`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 41-1 | 固定長専用のディレクティブである | 一致 | `notation.rst:896`-`:897`（固定長の表にのみ存在。可変長の表 `:917`-`:936` には無い） |
| 41-2 | レコード長（バイト数）である | 一致 | `notation.rst:896`「レコード長」＋`:854`-`:855`（フィールド長はバイト長） |
| 41-3 | 全フィールド長の合計から自動計算されるため通常は記述不要である | 一致 | `notation.rst:897`「フィールド長合計から自動計算される。通常は記述不要」＋`:1174` |
| 41-4 | 明示した場合は自動計算値を上書きする | 記述なし | 本体 `DataFile.java:304`（`setDirective` は後勝ち）。40-4 と同じ所見 |
| 41-構造1 | `type: integer` | 記述なし | スキーマ `:300`。所見: `field_def.length` は integer と string の両方を許す（`:414`-`:423`）のに対し非対称。解説書 `:1118` は `length` についてのみ両表記可と述べており、`record-length` の表記は述べていない |

### D42〜D45 ゾーン／パック符号ニブル — スキーマ `:305`・`:309`・`:313`・`:317`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 42-1 | `positive-zone-sign-nibble` は固定長専用である | 一致 | `notation.rst:900`-`:901`（固定長の表にのみ存在） |
| 42-2 | ゾーン数値の正符号ニブルである | 一致 | `notation.rst:901`「ゾーン10進数の正符号・負符号ニブル」 |
| 42-構造1 | `type: string` | 記述なし | スキーマ `:304`。解説書は値の型を書いていない |
| 43-1 | `negative-zone-sign-nibble` は固定長専用である | 一致 | `notation.rst:900`-`:901` |
| 43-2 | ゾーン数値の負符号ニブルである | 一致 | `notation.rst:901` |
| 43-構造1 | `type: string` | 記述なし | スキーマ `:308` |
| 44-1 | `positive-pack-sign-nibble` は固定長専用である | 一致 | `notation.rst:902`-`:903` |
| 44-2 | パック数値の正符号ニブルである | 一致 | `notation.rst:903`「パック10進数の正符号・負符号ニブル」 |
| 44-構造1 | `type: string` | 記述なし | スキーマ `:312` |
| 45-1 | `negative-pack-sign-nibble` は固定長専用である | 一致 | `notation.rst:902`-`:903` |
| 45-2 | パック数値の負符号ニブルである | 一致 | `notation.rst:903` |
| 45-構造1 | `type: string` | 記述なし | スキーマ `:316` |

### D46〜D48 固定長の真偽値ディレクティブ — スキーマ `:321`・`:325`・`:329`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 46-1 | `required-decimal-point` は固定長専用である | 一致 | `notation.rst:904`-`:905` |
| 46-2 | 小数点の要否である | 一致 | `notation.rst:905`「小数点を必須とするか（`true`/`false`）」 |
| 46-構造1 | `type: boolean` | 一致 | `notation.rst:905`「（`true`/`false`）」 |
| 47-1 | `fixed-sign-position` は固定長専用である | 一致 | `notation.rst:906`-`:907` |
| 47-2 | 符号位置固定の要否である | 一致 | `notation.rst:907`「符号を固定位置に置くか（`true`/`false`）」 |
| 47-構造1 | `type: boolean` | 一致 | `notation.rst:907` |
| 48-1 | `required-plus-sign` は固定長専用である | 一致 | `notation.rst:908`-`:909` |
| 48-2 | 正符号出力の要否である | 一致 | `notation.rst:909`「正符号を出力するか（`true`/`false`）」 |
| 48-構造1 | `type: boolean` | 一致 | `notation.rst:909` |

### D49 `/$defs/directives/properties/field-separator` — スキーマ `:333`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 49-1 | 可変長専用のディレクティブである | 一致 | `notation.rst:925`-`:926`（可変長の表にのみ存在） |
| 49-2 | フィールド区切り文字である | 一致 | `notation.rst:926`「フィールド区切り文字」 |
| 49-3 | 省略時はカンマ（`","`）である | 一致 | `notation.rst:926`「デフォルトは `","`」。本体 `VariableLengthFile.java:29`（`3c4bd2a`）も同じ |
| 49-4 | 2文字表記の `\t` を除き、1文字でない値はエラーになる | 一致 | `notation.rst:926`「タブを表す2文字表記の `\t` を除き、1文字でない値はエラーになる」 |
| 49-5 | タブは `field-separator: "\\t"` と記述する | 記述なし | 解説書 `:1056` は Excel 形式について `field-separator=\t` と述べるのみで、YAML 形式のエスケープには触れていない。実装根拠は 49-6 と同じ。所見: YAML 形式固有の書式なので解説書の YAML 形式の節にあってよい |
| 49-6 | YAML の `"\t"` は実際のタブ文字に展開され、NTF が値を trim する際に除去されて0文字になりエラーとなる | 記述なし | 本体 `DataFile.java:304`（`stringValue.trim()`）。39-8 と同じ所見 |
| 49-構造1 | `type: string` | 一致 | `notation.rst:926` |

### D50〜D54 可変長の残りのディレクティブ — スキーマ `:337`・`:341`・`:345`・`:349`・`:353`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 50-1 | `quoting-delimiter` は可変長専用である | 一致 | `notation.rst:927`-`:928` |
| 50-2 | クォート区切り文字である | 一致 | `notation.rst:928`「クォート文字」 |
| 50-構造1 | `type: string` | 一致 | `notation.rst:928` |
| 51-1 | `ignore-blank-lines` は可変長専用である | 一致 | `notation.rst:929`-`:930` |
| 51-2 | 空行を無視するか否かである | 一致 | `notation.rst:930`「空行を無視するか」 |
| 51-構造1 | `type: boolean` | 記述なし | スキーマ `:340`。解説書は値の型を書いていない（`required-*` と違い `（true/false）` の注記が無い） |
| 52-1 | `requires-title` は可変長専用である | 一致 | `notation.rst:931`-`:932` |
| 52-2 | タイトル行の要否である | 一致 | `notation.rst:932`「タイトル行の有無」 |
| 52-構造1 | `type: boolean` | 記述なし | スキーマ `:344`。51-構造1 と同じ |
| 53-1 | `max-record-length` は可変長専用である | 一致 | `notation.rst:933`-`:934` |
| 53-2 | 最大レコード長である | 一致 | `notation.rst:934`「レコードの最大長」 |
| 53-3 | 単位はバイト数である | 記述なし | 解説書 `:934` は単位を書いていない。実装上の根拠も本モジュールには無い（本体 `DataFile#setDirective` へ素通し）。**未確認** |
| 53-構造1 | `type: integer` | 記述なし | スキーマ `:348` |
| 54-1 | `title-record-type-name` は可変長専用である | 一致 | `notation.rst:935`-`:936` |
| 54-2 | タイトルレコード種別名である | 一致 | `notation.rst:936`「タイトルレコードの種別名」 |
| 54-構造1 | `type: string` | 一致 | `notation.rst:936` |

### D55 `/$defs/record_fragment` — スキーマ `:364`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 55-1 | レコード種別1ブロックである | 一致 | `notation.rst:848`-`:849`「レコード種別: ファイルデータのレコード種別を示す」＋`:1033`-`:1034` |
| 55-2 | 1つのレコードレイアウト（フィールド定義 + データ行）を表す | 一致 | `notation.rst:834`-`:838`（レコード種別＋フィールド名称 → データ型 → フィールド長 → データ）＋`:1108`-`:1114` |
| 55-3 | record_fragment の rows は配列の配列である | 一致 | `notation.rst:1119`「`rows:` の各行は配列形式で、`fields:` と同じ順序・同じ件数で値を並べる」 |
| 55-4 | テーブル系（table_data / list_map_data）の rows はオブジェクト配列である | 一致 | `notation.rst:788`「`rows:` 配列に、各行をオブジェクトとして記述する」 |
| 55-構造1 | `fields`・`rows` は必須 | 記述なし | スキーマ `:359`-`:362`。解説書 `:1122` は `path`・`type`・`records` の必須だけを明示し、レコード種別ブロック内の必須キーは述べていない。所見: `:834`-`:838` の構成から導けるが、明示があってよい |
| 55-構造2 | `additionalProperties: false`（`record_type`・`fields`・`rows` 以外は検証エラー） | 記述なし | スキーマ `:363`。20-構造2 と同じ所見 |

### D56 `/$defs/record_fragment/properties/record_type` — スキーマ `:368`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 56-1 | レコード種別名である | 一致 | `notation.rst:848`-`:849`・`:1033`-`:1034` |
| 56-2 | ファイルデータ（setup_files / expected_files）では複数レコードレイアウトを持つファイルの識別に使われる | 一致 | `notation.rst:1034`「複数レコードレイアウトの場合は、この記述を連続して記載する」＋`:1058`「1つのファイルデータブロック内に複数のレコードレイアウトを連続して記述すると、データの後ろに新たなレコード種別とフィールド名称を書いた時点で、新しいレコードレイアウトとして扱われる」 |
| 56-3 | messages では記載した値は使われず、NTF 内部で常に `"default"` になる | 一致 | `notation.rst:1144`「`MESSAGE`（`setUpMessages`・`expectedMessages`）では、記載した値は使われず、デフォルトのレコード種別（`"default"`）になる」 |
| 56-4 | そのため実行時の挙動に影響しない | 一致 | `notation.rst:1144` |
| 56-5 | 可読性のために任意の名前を記述してよい | 記述なし | `YamlFileBuilder.java:206`-`:208`（`keepRecordType=false` のとき記載値を捨てて `"default"` にする）。所見: `:1144` から導ける運用上の助言。解説書に書いてもよい |
| 56-6 | 同期応答メッセージ送信で使う4セクションでは、記載した値がそのままレコード種別になる | 一致 | `notation.rst:1144`「同期応答メッセージ送信で使う4つのデータタイプ…と取引単体テストのモックアップクラスの電文では、記載した値がそのままレコード種別になる」 |
| 56-7 | いずれのセクションでも FW_HEADER のような予約値はない | 一致 | `notation.rst:1287`「`record_type` に特別な予約値はない」 |
| 56-構造1 | `type: string`（`required` に含めない＝省略可） | 記述なし | スキーマ `:359`-`:362`・`:366`-`:368`。解説書 `:1272` の例は `record_type: default` を書いており、省略可否には触れていない。実装 `YamlFileBuilder.java:206`-`:208` は未指定なら `"default"` |

### D57 `/$defs/record_fragment/properties/fields` — スキーマ `:373`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 57-1 | フィールド定義リストである | 一致 | `notation.rst:1116`「`fields:` の各要素は `{name: フィールド名, type: データ型, length: バイト長}` の形式」 |
| 57-2 | フィールド名・データ型・フィールド長を1要素にまとめた定義である | 一致 | `notation.rst:1116` |
| 57-3 | 同一レコード種別内のフィールド名は重複不可（重複時はエラー） | 一致 | `notation.rst:1046`「1つのレコード種別において、フィールド名称に重複した名称は許容されない」＋`:1140`。本体 `DataFileFragment.java:190`-`:193`・`:354`-`:358`（`3c4bd2a`）も同じ |
| 57-構造1 | `type: array`／`minItems: 1` | 一致 | `notation.rst:834`-`:838`（レコード種別にはフィールド名称が続く）＋`:1036`「フィールドの数だけ記載する」。本体 `DataFileFragment.java:191`（`assertNotNullOrEmpty(names, "names")`）も 0 件を拒否する |

### D58 `/$defs/record_fragment/properties/rows` — スキーマ `:380`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 58-1 | データ行リストである | 一致 | `notation.rst:838`（データ（1件以上））＋`:1119` |
| 58-2 | 各要素は fields と同じ順序・同じ件数で値を並べた配列である | 一致 | `notation.rst:1119` |
| 58-3 | NTF は fields の順序で先頭から対応付ける | 一致 | `notation.rst:1119` |
| 58-4 | 各配列の要素数が fields の件数に満たない場合、不足した末尾のフィールドは `""` として扱われる | 一致 | `notation.rst:863`「フィールドの数だけ値を並べる必要はない。末尾のフィールドの値を書かなければ、そのフィールドは `""` として扱われる」 |
| 58-5 | 末尾のフィールドに `null` と書いた場合も `""` になる | 一致 | `notation.rst:870`「末尾のフィールドに `null` と記述した場合は、形式によらず `""` になる」 |
| 58-6 | 後ろに値のあるフィールドがあれば `null` のまま保持される | 一致 | `notation.rst:870`「後ろに空文字でも null でもないフィールドがあれば null のまま保持される」 |
| 58-7 | ここでいう「値のあるフィールド」に `""` は含まれない | 一致 | `notation.rst:870`「空文字でも null でもないフィールド」 |
| 58-8 | 末尾側に `""` と `null` がどの順で並んでもまとめて `""` になり、`null` は保持されない（例: fields 3 件に対し `["x", null, ""]` は `"x"`・`""`・`""`） | 一致 | `notation.rst:870`「（末尾側に並んだ `""` と `null` は、まとめて `""` になる）」 |
| 58-9 | 空配列 `[]` を1要素書くと全フィールドが `""` のレコード1件になる | 記述なし | `YamlFileBuilder.java:243`-`:267`（`NablarchTestUtils.trimTailCopy` → `addValue`）。解説書 `:863` は YAML について「`rows:` の値をすべて `""` とした行」を挙げており、空配列 `[]` には触れていない。所見: 等価であることは解説書にあってよい |
| 58-10 | **（是正前）rows が0件でも有効である** | **矛盾（C8）** | `notation.rst:838`「データ（1件以上）」＋`:861`「0バイトの空ファイルは、レコード定義を持たないファイルデータブロックとして表現する」（＝レコード定義を持つならデータは1件以上） |
| 58-11 | バックスラッシュと `r` の2文字（`"\\r"`）を含む値はエラーになる | 一致 | `notation.rst:1429` |
| 58-12 | Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値は仕様上存在しない | 一致 | `notation.rst:1429` |
| 58-構造1 | `type: array`（`minItems` なし） | **矛盾（S1。変更せず報告）** | `notation.rst:838`「データ（1件以上）」。description は C8 として是正したが、`minItems: 1` の追加は検証挙動が変わるため指示書 §2 により行っていない |
| 58-構造2 | `items.type: array` | 一致 | `notation.rst:1119` |
| 58-構造3 | `items.items.type: ["string","null"]` | 一致 | `notation.rst:1120`「`rows:` 内の値はダブルクォートで囲む」＋`:870`（`null` と記述できる） |

### D59 `/$defs/record_fragment/properties/rows/items` — スキーマ `:389`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 59-1 | フィールド値のリストである | 一致 | `notation.rst:1119` |
| 59-2 | 数値・真偽値も必ず文字列（クォート付き）で記述すること | 一致 | `notation.rst:1120`「`rows:` 内の値はダブルクォートで囲む」＋`:1385`・`:1406`-`:1411` |

### D60 `/$defs/field_def` — スキーマ `:401`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 60-1 | フィールド定義1件である | 一致 | `notation.rst:1116` |
| 60-2 | フィールド名・データ型・フィールド長の3要素で1フィールドを定義する | 一致 | `notation.rst:1116`＋`:850`-`:855` |
| 60-3 | 固定長ファイル（type=fixed）では length が実質必須である | 一致 | `notation.rst:837`「フィールド長（固定長のみ）」＋`:854`-`:855`「各フィールドのバイト長（固定長ファイルのみ存在）」＋`:1052`「固定長との違いは、可変長ファイルの場合はフィールド長行を記載しない点のみである」 |
| 60-4 | 省略すると NTF パーサが record-length を計算できない | 一致 | `notation.rst:897`「フィールド長合計から自動計算される」 |
| 60-構造1 | `name`・`type` は必須、`length` は任意 | 一致 | `notation.rst:1116`＋`:1052`（可変長はフィールド長を書かない） |
| 60-構造2 | `additionalProperties: false`（`name`・`type`・`length` 以外は検証エラー） | 記述なし | スキーマ `:400`。所見: `:1116` が3要素と定めているので閉じているのは自然だが、明示は無い |

### D61 `/$defs/field_def/properties/name` — スキーマ `:405`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 61-1 | フィールド名である | 一致 | `notation.rst:850`-`:851`「フィールド名称: 各フィールドの名称」＋`:1116` |
| 61-2 | NTF がフォーマット定義ファイルのフィールド名と照合するために使用する | 記述なし | 解説書にフォーマット定義ファイルとの「照合」の記述は無い（`:1188` はフォーマット定義ファイルの命名規則のみ）。本体 `DataFileFragment.java:58`・`:105`-`:106`（`3c4bd2a`）はフィールド名から `FieldDefinition` を組み立てて使うが、フォーマット定義ファイル側の名前と突き合わせる処理は確認できていない。**この一文の根拠は未確認**。所見: 根拠が取れないなら削るか「レコード定義のフィールド名になる」に改めるのが安全。ユーザー判断を仰ぐ |
| 61-3 | 同一レコード種別内で重複不可である | 一致 | `notation.rst:1046`・`:1140`。本体 `DataFileFragment.java:192`・`:354`-`:358` |
| 61-構造1 | `type: string` | 一致 | `notation.rst:1111`-`:1112`（`{name: USER_ID, ...}`） |

### D62 `/$defs/field_def/properties/type` — スキーマ `:410`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 62-1 | データ型である | 一致 | `notation.rst:852`-`:853`「データ型: 各フィールドのデータ型（日本語表記の例: `半角英字`）」 |
| 62-2 | 日本語名称で記述する | 一致 | `notation.rst:938`「フィールドのデータ型は、以下の日本語型名称で指定する」＋`:1117`「`type` は Excel と同じ日本語型名称で記述する（変換ツールも Excel の型名称をそのまま出力する）」 |
| 62-3 | 列挙した13語（半角英字／半角数字／半角／全角／全角漢字／数値／符号無ゾーン10進数／符号付ゾーン10進数／符号無パック10進数／符号付パック10進数／符号無数値／符号付数値／バイナリ）は有効な型名称である | 一致 | `notation.rst:947`-`:976`（13語すべてが型名称の表に存在） |
| 62-4 | NTF が内部の型記号へ変換する | 一致 | `notation.rst:944`-`:976`（型名称→型記号の対応表） |
| 62-5 | その変換は `BasicDataTypeMapping` が行う | 記述なし | 本体 `BasicDataTypeMapping.java:16`（`3c4bd2a`。`implements DataTypeMapping`）。解説書にクラス名は無い。所見: 27-7 と同じくクラス名を description に書く必要は薄い |
| 62-6 | プロジェクト独自の型名称（`dataTypeMapping` で登録したもの）も許容する | 記述なし | 本体 `DataFileFragment.java:40`（`private static final String DATATYPE_MAPPING = "dataTypeMapping";`）。解説書は `:978` で `TEST_{型記号}` の登録には触れるが、`dataTypeMapping` による独自型名称の登録には触れていない。所見: 解説書にあってよい |
| 62-7 | そのため過度なパターン制約は設けない | 記述なし | スキーマ `:407`-`:409`（`type: string`／`minLength: 1` のみ）。62-6 と同じ |
| 62-構造1 | `type: string`／`minLength: 1`（`enum` にしない） | 記述なし | 62-6・62-7 と同じ。所見: 解説書 `:938`-`:976` は型名称を列挙しているが、`:978` の `TEST_{型記号}` と `dataTypeMapping` による拡張があるため `enum` にしない選択は妥当 |

### D63 `/$defs/field_def/properties/length` — スキーマ `:413`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 63-1 | フィールド長（バイト数）である | 一致 | `notation.rst:854`-`:855`「フィールド長: 各フィールドのバイト長（固定長ファイルのみ存在）」＋`:1116` |
| 63-2 | 固定長ファイルでは実質必須である | 一致 | `notation.rst:837`・`:1052` |
| 63-3 | 省略すると NTF が record-length を計算できない | 一致 | `notation.rst:897` |
| 63-4 | 可変長ファイルでは不要（省略可）である | 一致 | `notation.rst:1052`「可変長ファイルの場合はフィールド長行を記載しない」 |
| 63-5 | `"0"` はダミーフィールド（意味のある長さを持たないプレースホルダー）に使用可である | 記述なし | スキーマ `:414`-`:423`（`minimum: 0` と `^([0-9]+|-)$` が `0` を許す）。解説書はフィールド長 `0` の用途を述べていない。**用途（ダミーフィールド）の根拠は未確認**。所見: 用途を書くなら根拠が要る |
| 63-6 | `"-"` はオンデマンド計算で、そのフィールドに追加された全レコード値の最大バイト長に自動拡張される | 一致 | `notation.rst:1040`「`"-"` を指定すると、追加した全レコードの最大バイト長に自動拡張される」＋`:1205` |
| 63-7 | `"-"` フィールドの値は NTF が格納時に、値に含まれる改行とその前後の空白を除去する | 一致 | `notation.rst:1040`「この場合、値に含まれる改行と、その前後の空白は取り除かれる」 |
| 63-8 | 改行を含まない値の前後の空白は除去されない | 記述なし | 解説書 `:1040` は「値に含まれる改行と、その前後の空白」とだけ述べる。所見: `:1040` の読み違えを防ぐ補足であり、解説書にあってよい |
| 63-9 | integer 記法（10）も文字列記法（"10"）もどちらも有効である | 一致 | `notation.rst:1118`「`length` は整数（`length: 10`）または文字列（`length: "10"`）のどちらでも有効であり、変換ツールが生成した YAML は文字列形式になる」 |
| 63-構造1 | `anyOf`: `{type: integer, minimum: 0}` または `{type: string, pattern: "^([0-9]+|-)$"}` | 一致 | `notation.rst:1118`（両表記可）＋`:1040`（`"-"` 指定） |

### D64 `/$defs/fw_header` — スキーマ `:433`

| # | 主張 | 判定 | 出典・根拠 |
|---|---|---|---|
| 64-1 | FW 制御ヘッダ（キー: 値）である | 一致 | `notation.rst:1260`「フレームワーク制御ヘッダを `fw_header:` マップ（キー: 値）で記述する」 |
| 64-2 | messages セクション専用である | 一致 | `notation.rst:1260`「`fw_header:` マップは `messages`（`MESSAGE`）でのみ使用し」 |
| 64-3 | expected_request_* / response_* では使用しない | 一致 | `notation.rst:1260` |
| 64-4 | それらは records の fields/rows でフィールド単位（型・長さつき）に定義する | 一致 | `notation.rst:1260` |
| 64-5 | 記載できるキーは `reader.fwHeaderfields` に指定した名前だけである | 一致 | `notation.rst:1279`・`:1150` |
| 64-6 | 省略時は requestId, userId, resendFlag, resultCode である | 一致 | `notation.rst:1150`「デフォルト値は `requestId`・`userId`・`resendFlag`・`resultCode` の4種だが固定ではなく、`SystemRepository` の `reader.fwHeaderfields` キーでプロジェクトが任意の名前に変更できる」＋`:1279` |
| 64-7 | それ以外のキーがあるとエラーになる | 一致 | `notation.rst:1279`「それ以外のキーがあるとエラーになる」 |
| 64-8 | このキー名の検査は、そのエントリの電文を読み出したときに行われる | 記述なし | `YamlMessageBuilder.java:118`-`:135`・`:298`-`:328`。28-6 と同じ |
| 64-9 | 値は数値・真偽値も必ず文字列（クォート付き）で記述すること（例: requestId: `"0000000001"`） | 記述なし | スキーマ `:430`-`:432`。28-8 と同じ |
| 64-10 | 値の型はキー名と違ってこのスキーマ自身が課す制約である | 記述なし | スキーマ `:430`-`:432`・`:434`。28-9 と同じ |
| 64-11 | クォートなしの数値・真偽値を1つでも書くとロード時にファイル全体がエラーになる | 記述なし | `YamlLoader.java:154`-`:157`。28-10 と同じ |
| 64-12 | バックスラッシュと `r` の2文字を含む値はエラーになる | 一致 | `notation.rst:1429` |
| 64-13 | バックスラッシュと `r` の2文字を含む**キー名**もエラーになる | 記述なし | `YamlMessageBuilder.java:311`-`:317`。28-12 と同じ |
| 64-14 | Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値は仕様上存在しない | 一致 | `notation.rst:1429` |
| 64-構造1 | `type: object`／`minProperties: 0`（空マップも許す） | 記述なし | スキーマ `:428`-`:429`。所見: 解説書はヘッダを持たない電文について述べていない |
| 64-構造2 | `additionalProperties: {"type":"string"}`（キー名は列挙せず値の型だけ検査する） | 一致 | `notation.rst:1150`「デフォルト値は…4種だが固定ではなく、`reader.fwHeaderfields` キーでプロジェクトが任意の名前に変更できる」（設定依存のためスキーマでキーを列挙できない）＋スキーマ `:434` の `$comment` |

## 4. 是正の差分

変更したのは `description` 7 箇所のみ。構造（`type`・`required`・`enum`・`minItems`・`maxItems`・
`pattern`・`additionalProperties` 等）に差分が無いことを機械確認した。

```
$ python3 -c '
import json,subprocess
old=json.loads(subprocess.check_output(["git","show","HEAD:src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json"]).decode())
new=json.load(open("src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json",encoding="utf-8"))
def strip(n):
    if isinstance(n,dict): return {k:strip(v) for k,v in n.items() if k!="description"}
    if isinstance(n,list): return [strip(v) for v in n]
    return n
print("description を除いた構造が同一:", strip(old)==strip(new))'
description を除いた構造が同一: True

$ git diff --stat
 .../resources/nablarch/test/ntf-testdata-yaml-schema.json  | 14 +++++++-------
 1 file changed, 7 insertions(+), 7 deletions(-)
```

```diff
diff --git a/src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json b/src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json
index 8c30792..5c02099 100644
--- a/src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json
+++ b/src/main/resources/nablarch/test/ntf-testdata-yaml-schema.json
@@ -52,3 +52,3 @@
       "type": "array",
-      "description": "MockMessaging 経路の要求/応答電文データ。id で完全一致検索され先着1件のみ有効（2件目以降は無視）。fw_header で FW 制御ヘッダを指定し、records で電文本文フィールドを定義する。id はデータタイプ MESSAGE の識別子であり、`setUpMessages`（要求電文）・`expectedMessages`（応答電文）という固定値を指定する。`sendSyncTestData` は、取引単体テストのモックアップクラスが読む同期応答メッセージ送信のテストデータのベースディレクトリに付けるコンポーネント設定のキーであり、`sendSyncTestData/{requestId}/message` は読み込み単位を指すパスであって、この id に書く値ではない",
+      "description": "メッセージング受信テストの要求電文（setUpMessages）と、同期応答メッセージ受信の応答電文期待値（expectedMessages）のデータ。応答電文を持たない応答不要メッセージ受信では expectedMessages は読み込まれない。id で完全一致検索され先着1件のみ有効（2件目以降は無視）。fw_header で FW 制御ヘッダを指定し、records で電文本文フィールドを定義する。id はデータタイプ MESSAGE の識別子であり、`setUpMessages`（要求電文）・`expectedMessages`（応答電文）という固定値を指定する。`sendSyncTestData` は、取引単体テストのモックアップクラスが読む同期応答メッセージ送信のテストデータのベースディレクトリに付けるコンポーネント設定のキーであり、`sendSyncTestData/{requestId}/message` は読み込み単位を指すパスであって、この id に書く値ではない",
       "items": {
@@ -73,3 +73,3 @@
       "type": "array",
-      "description": "応答電文ヘッダデータ。2経路で参照される: (A) RequestTestingSendSyncSupport 経路では group_id でフィルタリングして全件収集、(B) MockMessagingContext / MockMessagingClient 経路では id で照合して先着1件収集。errorMode 行（先頭値が `errorMode:timeout` または `errorMode:msgException`）を含めると送受信エラーをシミュレートできる（経路B のみ）",
+      "description": "応答電文ヘッダデータ。2経路で参照される: (A) RequestTestingSendSyncSupport 経路では group_id でフィルタリングして全件収集、(B) MockMessagingContext / MockMessagingClient 経路では id で照合して先着1件収集。errorMode 行（先頭値が `errorMode:timeout` または `errorMode:msgException`）を含めると送受信エラーをシミュレートできる。errorMode は、リクエスト単体テストで応答電文を返す経路（A）と取引単体テストのモックアップクラス経由の送受信（B）の両方で反映される",
       "items": {
@@ -80,3 +80,3 @@
       "type": "array",
-      "description": "応答電文ボディデータ。2経路で参照される: (A) RequestTestingSendSyncSupport 経路では group_id でフィルタリングして全件収集、(B) MockMessagingContext / MockMessagingClient 経路では id で照合して先着1件収集。各エントリの電文レコード全体のバイト長が同一でなければならない（不一致時はエラー）。errorMode 行を含めると送受信エラーをシミュレートできる（経路B のみ）",
+      "description": "応答電文ボディデータ。2経路で参照される: (A) RequestTestingSendSyncSupport 経路では group_id でフィルタリングして全件収集、(B) MockMessagingContext / MockMessagingClient 経路では id で照合して先着1件収集。HTTP メッセージ送信の応答電文本文では、各データエントリの文字列長が同一である必要がある。複数回電文を送信する場合は、同一データタイプ・同一リクエストIDのデータをそれぞれまとめて記述し、電文の長さを揃える必要がある。errorMode 行を含めると送受信エラーをシミュレートできる。errorMode は、リクエスト単体テストで応答電文を返す経路（A）と取引単体テストのモックアップクラス経由の送受信（B）の両方で反映される",
       "items": {
@@ -195,3 +195,3 @@
       "additionalProperties": false,
-      "description": "messages エントリ1件。MockMessaging 経路の要求/応答電文1メッセージを表す。id で完全一致検索され先着1件のみ有効。fw_header で FW 制御ヘッダを指定し、records で電文本文フィールド（型・長さつき）を定義する",
+      "description": "messages エントリ1件。メッセージング受信テストの要求電文（setUpMessages）または同期応答メッセージ受信の応答電文期待値（expectedMessages）の1メッセージを表す。id で完全一致検索され先着1件のみ有効。fw_header で FW 制御ヘッダを指定し、records で電文本文フィールド（型・長さつき）を定義する",
       "properties": {
@@ -215,3 +215,3 @@
           "$ref": "#/$defs/fw_header",
-          "description": "FW 制御ヘッダ（キー: 値）。messages（MESSAGE: MockMessaging 経路）でのみ使用する。記載できるキーは、プロジェクトの reader.fwHeaderfields 設定に指定した名前（省略時は requestId, userId, resendFlag, resultCode）だけであり、それ以外のキーがあるとエラーになる。キー名の検査は、そのエントリの電文を読み出したときに行われる。誤記のあるエントリを読み出したときだけエラーになり、同一ファイル内の他のエントリの読み出しは巻き添えにならない。値は数値・真偽値も必ず文字列（クォート付き）で記述すること（例: requestId: `\"0000000001\"`）。値の型はキー名と違ってこのスキーマ自身が課す制約であり、クォートなしの数値・真偽値を1つでも書くとロード時にファイル全体がエラーになり、他のエントリも読み出せなくなる。バックスラッシュと `r` の2文字（`\"\\\\r\"`）を含むキー名・値はエラーになる（Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値はテスティングフレームワークの仕様上存在しない）"
+          "description": "FW 制御ヘッダ（キー: 値）。messages（MESSAGE）でのみ使用する。記載できるキーは、プロジェクトの reader.fwHeaderfields 設定に指定した名前（省略時は requestId, userId, resendFlag, resultCode）だけであり、それ以外のキーがあるとエラーになる。キー名の検査は、そのエントリの電文を読み出したときに行われる。誤記のあるエントリを読み出したときだけエラーになり、同一ファイル内の他のエントリの読み出しは巻き添えにならない。値は数値・真偽値も必ず文字列（クォート付き）で記述すること（例: requestId: `\"0000000001\"`）。値の型はキー名と違ってこのスキーマ自身が課す制約であり、クォートなしの数値・真偽値を1つでも書くとロード時にファイル全体がエラーになり、他のエントリも読み出せなくなる。バックスラッシュと `r` の2文字（`\"\\\\r\"`）を含むキー名・値はエラーになる（Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値はテスティングフレームワークの仕様上存在しない）"
         }
@@ -274,3 +274,3 @@
           "maxItems": 1,
-          "description": "電文フィールドのレコード定義。records に記述するレコードレイアウトは1つであり、2つ以上記述するとエラーになる（電文はファイルデータのように複数のレコードレイアウトを持たない）。先頭値が `errorMode:timeout` または `errorMode:msgException` の行を含めると MockMessagingContext / MockMessagingClient 経路で送受信エラーをシミュレートできる（RequestTestingSendSyncSupport 経路では errorMode は無視される）",
+          "description": "電文フィールドのレコード定義。records に記述するレコードレイアウトは1つであり、2つ以上記述するとエラーになる（電文はファイルデータのように複数のレコードレイアウトを持たない）。先頭値が `errorMode:timeout` または `errorMode:msgException` の行を含めると送受信エラーをシミュレートできる。errorMode は、リクエスト単体テストで応答電文を返す経路と取引単体テストのモックアップクラス経由の送受信の両方で反映される",
           "items": {
@@ -379,3 +379,3 @@
           "type": "array",
-          "description": "データ行リスト。各要素は fields と同じ順序・同じ件数で値を並べた配列（NTF は fields の順序で先頭から対応付ける）。各配列の要素数が fields の件数に満たない場合、不足した末尾のフィールドは `\"\"` として扱われる。末尾のフィールドに `null` と書いた場合も `\"\"` になる（後ろに値のあるフィールドがあれば `null` のまま保持される）。ここでいう「値のあるフィールド」に `\"\"` は含まれない。末尾側に `\"\"` と `null` がどの順で並んでもまとめて `\"\"` になり、`null` は保持されない（例: fields 3 件に対し `[\"x\", null, \"\"]` と書くと `\"x\"`・`\"\"`・`\"\"` になる）。これを利用し、空配列 `[]` を1要素書くと全フィールドが `\"\"` のレコード1件になる。rows が0件でも有効。バックスラッシュと `r` の2文字（`\"\\\\r\"`）を含む値はエラーになる（Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値はテスティングフレームワークの仕様上存在しない）",
+          "description": "データ行リスト。各要素は fields と同じ順序・同じ件数で値を並べた配列（NTF は fields の順序で先頭から対応付ける）。各配列の要素数が fields の件数に満たない場合、不足した末尾のフィールドは `\"\"` として扱われる。末尾のフィールドに `null` と書いた場合も `\"\"` になる（後ろに値のあるフィールドがあれば `null` のまま保持される）。ここでいう「値のあるフィールド」に `\"\"` は含まれない。末尾側に `\"\"` と `null` がどの順で並んでもまとめて `\"\"` になり、`null` は保持されない（例: fields 3 件に対し `[\"x\", null, \"\"]` と書くと `\"x\"`・`\"\"`・`\"\"` になる）。これを利用し、空配列 `[]` を1要素書くと全フィールドが `\"\"` のレコード1件になる。データは1件以上記述する（0バイトの空ファイルは、レコード定義を持たないブロックとして `records:` に空配列 `[]` を書いて表す）。バックスラッシュと `r` の2文字（`\"\\\\r\"`）を含む値はエラーになる（Excel 形式ではこの2文字が必ず CR に変換されるため、この2文字を含む値はテスティングフレームワークの仕様上存在しない）",
           "items": {
```

### 4.1 是正1件ずつの逐語根拠

| # | スキーマの旧文 | スキーマの新文 | 解説書の逐語 |
|---|---|---|---|
| C1 | 「MockMessaging 経路の要求/応答電文データ。」 | 「メッセージング受信テストの要求電文（setUpMessages）と、同期応答メッセージ受信の応答電文期待値（expectedMessages）のデータ。応答電文を持たない応答不要メッセージ受信では expectedMessages は読み込まれない。」 | `notation.rst:1130`「`setUpMessages`（メッセージング受信テストの要求電文 ID、固定値）・`expectedMessages`（同期応答メッセージ受信の応答電文期待値 ID、固定値。応答電文を持たない応答不要メッセージ受信では読み込まれない）」 |
| C2 | 「messages エントリ1件。MockMessaging 経路の要求/応答電文1メッセージを表す。」 | 「messages エントリ1件。メッセージング受信テストの要求電文（setUpMessages）または同期応答メッセージ受信の応答電文期待値（expectedMessages）の1メッセージを表す。」 | 同上 |
| C3 | 「messages（MESSAGE: MockMessaging 経路）でのみ使用する。」 | 「messages（MESSAGE）でのみ使用する。」 | `notation.rst:1260`「`fw_header:` マップは `messages`（`MESSAGE`）でのみ使用し」 |
| C4 | 「…送受信エラーをシミュレートできる（経路B のみ）」 | 「…送受信エラーをシミュレートできる。errorMode は、リクエスト単体テストで応答電文を返す経路（A）と取引単体テストのモックアップクラス経由の送受信（B）の両方で反映される」 | `notation.rst:1238`「リクエスト単体テストで応答電文を返す経路と、取引単体テストのモックアップクラス経由の送受信の両方で反映される」 |
| C5 | 同上（response_body_messages 側） | 同上 | 同上 |
| C6 | 「…MockMessagingContext / MockMessagingClient 経路で送受信エラーをシミュレートできる（RequestTestingSendSyncSupport 経路では errorMode は無視される）」 | 「…送受信エラーをシミュレートできる。errorMode は、リクエスト単体テストで応答電文を返す経路と取引単体テストのモックアップクラス経由の送受信の両方で反映される」 | 同上 |
| C7 | 「各エントリの電文レコード全体のバイト長が同一でなければならない（不一致時はエラー）。」 | 「HTTP メッセージ送信の応答電文本文では、各データエントリの文字列長が同一である必要がある。複数回電文を送信する場合は、同一データタイプ・同一リクエストIDのデータをそれぞれまとめて記述し、電文の長さを揃える必要がある。」 | `notation.rst:1186`「HTTPメッセージ送信の応答電文本文（`RESPONSE_BODY_MESSAGES`）は、各データエントリの文字列長が同一である必要がある」＋`:1209`「複数回電文を送信する場合は、同一データタイプ・同一リクエストIDのデータをそれぞれまとめて記述し、電文の長さを揃える必要がある」 |
| C8 | 「rows が0件でも有効。」 | 「データは1件以上記述する（0バイトの空ファイルは、レコード定義を持たないブロックとして `records:` に空配列 `[]` を書いて表す）。」 | `notation.rst:838`「データ（1件以上）」＋`:861`「0バイトの空ファイルは、レコード定義を持たないファイルデータブロックとして表現する」 |

### 4.2 C1・C2・C4〜C6 が「スキーマ側の誤り」である実測根拠

C1・C2（`messages` の読み手）:

- `MQSupport.java:64`・`:74`（`3c4bd2a`）が `getMessages(sheetName, "expectedMessages")`／`"setUpMessages"` を呼ぶ。
  この `MQSupport` を使うのは `MessagingRequestTestSupport.java:83`-`:84` と
  `MessagingReceiveTestSupport.java:43`（＝メッセージング受信テスト）である。
- `MockMessagingContext.java:97`・`:99` と `MockMessagingClient.java:57`・`:70` が読むのは
  `DataType.RESPONSE_HEADER_MESSAGES`／`DataType.RESPONSE_BODY_MESSAGES` だけで、`MESSAGE` は読まない。
- 全文検索: `grep -rn "setUpMessages\|expectedMessages" src/main/java` の結果に
  `MockMessaging*` は現れない。

C4〜C6（errorMode がどの経路で効くか）:

- `RequestTestingMessagePool.java:78`-`:84`（`3c4bd2a`）が errorMode を判定して
  `null` を返す／`MessagingException` を投げる。
- このメソッド（`createRequestTestingReceivedMessageBinary`）を呼ぶのは
  `RequestTestingMessagingProvider.java:203`・`:205` と `RequestTestingMessagingClient.java:228`・`:231`
  である。前者は `RequestTestingMessagingProvider.java:192`（`RequestTestingSendSyncSupport` を生成）と
  `:195`-`:200`（`getResponseMessage` で `RESPONSE_HEADER_MESSAGES`／`RESPONSE_BODY_MESSAGES` を取得）を
  経由しており、これが経路A である。つまり経路A でも errorMode は効く。
- 経路B 側の `SendSyncSupport.java:290`・`:293` にも同じ判定がある。

## 5. 解説書側の疑い

**0 件。** 「矛盾」と判定した 8 件（C1〜C8）は、いずれも本体（`3c4bd2a`）の実装で解説書が正しいことを確認できた。
解説書を直すべき箇所は見つからなかった。

## 6. 判断を仰ぎたい事項（変更していない）

| # | 箇所 | 内容 |
|---|---|---|
| Q1 | `$defs.record_fragment.properties.rows`（`:378`-`:390`） | S1。`minItems: 1` を足すか。足すと検証挙動が変わる（現在 0 件を通す）。指示書 §2 により変更していない |
| Q2 | `$defs.directives`（`:282`-`:356`） | L1。固定長11キー・可変長9キーの型別限定を `allOf`/`if-then` で表現するか。現状は和集合17キーを一律に許す |
| Q3 | `$defs.message_data.properties.records`（`:209`。27-7） | 「`MessageParser` はこの records の record_type を内部で常に "default" に置換する」の `MessageParser` は Excel 経路（本体）のクラス名で、YAML 経路の実装は `YamlFileBuilder.java:206`-`:208` である。クラス名を落として `notation.rst:1144` の逐語に寄せるか |
| Q4 | `$defs.field_def.properties.name`（`:405`。61-2） | 「NTF がフォーマット定義ファイルのフィールド名と照合するために使用する」の根拠が取れていない（解説書にも本体にも確認できず）。削るか書き換えるか |
| Q5 | `$defs.field_def.properties.length`（`:413`。63-5） | 「`"0"` はダミーフィールドに使用可」の用途の根拠が取れていない |
| Q6 | `$defs.directives.properties.max-record-length`（`:349`。53-3） | 「（バイト数）」の根拠が取れていない |

Q3〜Q6 はいずれも解説書との「矛盾」ではないため、指示書 §1 の是正対象に当たらず今回は変更していない。

## 7. 完了条件の自己判定

| 指示書 §3 の条件 | 結果 |
|---|---|
| 1. 母集合が機械抽出コマンドつきで報告にあり、全主張が対応表に載っている | OK（§1・§3。64 description / 444 主張。サンプリングなし） |
| 2. 「矛盾」の全件に処置が付いている | OK（C1〜C8 は是正コミット、S1 は §2・§6 Q1 として報告） |
| 3. `git diff` が description のみの変更で、是正は解説書の逐語根拠つき | OK（§4・§4.1。構造同一を機械確認） |
| 4. `mvn clean test` 全件緑・`git status --short` 空・push | OK（`Tests run: 320, Failures: 0, Errors: 0, Skipped: 0`） |
| 5. 報告して停止 | OK |

### 320件と318件について

指示書 §3-4 は「318件基準」とあるが、本モジュールの現状は 320 件である。`#45` の T6/L6 追加による差で、
`#46` 完了時のユーザー判断（steering の `#45`・`#46` 記録）どおり 320 件が正。今回の是正で件数は変わっていない。
