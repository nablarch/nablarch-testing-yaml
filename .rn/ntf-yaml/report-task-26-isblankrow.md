# task-26 報告: `YamlSection#isBlankRow` の直し方の案

対象: `#26 §4-1 isBlankRow の直し方の案を報告する（§5 事前ゲート1）`
状態: **報告のみ。`src/main` は変更していない。** 実装は #27。

---

## 結論（先に）

1. **`TestDataParsingTemplate#isBlankLine` は Excel 実入力で「発火する」。ただし発火の原因は空文字であって Java null ではない。**
   実測（nablarch-testing の `src` 配下の Excel 61 ブック / 246 シート / 全 147,632 セル）で
   **`PoiXlsReader` が返す `List<String>` に Java null の要素は 1 個も無かった（0 件）**。
   一方 `isBlankLine` 自体は 5,852 行中 **1 行で `true` を返した**（`cutComment` が行末コメントを落とした結果、
   残りが `["", ""]` になった行）。
   よって `StringUtil.isNullOrEmpty(Collection)` の **「Java null を空扱いする」枝は実入力で発火しない**。
   → 本指示書 §4-1 の記述は結論としては妥当だが、**「この経路は発火しない」という言い方は不正確**（§7 に反例として記載）。

2. **案（判別条件）**: `isBlankRow` を「値が 1 つも無い ⇒ 空」「値がすべて **Java null でない空文字** ⇒ 空」に限定する。
   Java null の値が 1 つでもあれば行として残す。

3. **`resolveColumns` への波及**: 先頭行が全 Java null の場合、現行は列名が 2 行目から決まるが、案では
   先頭行から決まる（実測で確認）。解説書 `testdata_notation.rst:818` とは矛盾しない（むしろ整合が改善する）。

---

## 1. 参照点

| リポジトリ | コミット | 用途 |
|---|---|---|
| `nablarch-testing-yaml` | `0db22217464eb29b7d2d1eaf4cf6c83686682a15` | 本モジュールのソース |
| `nablarch-testing` | `3c4bd2a103d399b3e715c04ee12e126cabf73cb1` | 依存先 Excel 経路のソース（参照のみ） |
| `nablarch-document` | `76e6e6189c521de00607137d492619496c37a3cf` | 解説書（SSoT、参照のみ） |

- すべて `git show <SHA>:<path>` で読んだ。作業ツリーの HEAD は読んでいない。
- 補助的に確認した同一性:
  - `git diff --stat 0db2221 6e98995 -- src/main/java/nablarch/test/core/reader/yaml/YamlSection.java` → 差分なし。
  - `git diff --stat 3c4bd2a 608091e -- src/main/java/nablarch/test/core/reader/PoiXlsReader.java src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java` → 差分なし。
    さらに `3c4bd2a` と nablarch-testing 作業ツリーの `src/main/java/nablarch/test/core/reader/` にも差分なし。
  - 実行に使った `~/.m2/.../nablarch-testing-6-NEXT-SNAPSHOT.jar` 内の
    `PoiXlsReader.class` / `TestDataParsingTemplate.class` の md5 は
    `nablarch-testing/target/classes` の同名クラスと一致（`d95cf38de37907ba37a293b426a512ac` /
    `b5ff4554768ce1c18b1505727b769f2a`）。**実行したバイトコードは上記ソースと同一系である。**
  - `5301d6e` は `76e6e61` の祖先（`git merge-base --is-ancestor 5301d6e 76e6e61` → 真）。
    したがって `76e6e61` の `testdata_notation.rst:1500` は是正後の文面である。

---

## 2. 現行の判定経路（YAML 側・`0db2221`）

### 2.1 逐語

`src/main/java/nablarch/test/core/reader/yaml/YamlSection.java:201`-`:209`

```java
    private static boolean isBlankRow(Object row) {
        for (Object value : castMap(row).values()) {
            String str = objectToString(value);
            if (str != null && !str.isEmpty()) {
                return false;
            }
        }
        return true;
    }
```

`YamlSection.java:147`-`:149`

```java
    public static String objectToString(Object value) {
        return toStr(value);
    }
```

`YamlSection.java:127`-`:129`

```java
    public static String toStr(Object value) {
        return value != null ? value.toString() : null;
    }
```

`YamlSection.java:182`-`:190`

```java
    public static List<Object> dropBlankRows(List<Object> rows) {
        List<Object> result = new ArrayList<Object>(rows.size());
        for (Object row : rows) {
            if (!isBlankRow(row)) {
                result.add(row);
            }
        }
        return result;
    }
```

`YamlSection.java:227`-`:235`

```java
    public static List<String> resolveColumns(List<Object> rows) {
        for (Object row : rows) {
            Map<String, Object> rowMap = castMap(row);
            if (!rowMap.isEmpty()) {
                return new ArrayList<String>(rowMap.keySet());
            }
        }
        return new ArrayList<String>();
    }
```

### 2.2 Java null が空扱いになる理由（条件式のレベル）

- YAML の値が Java null のとき `toStr`（`:128`）の三項演算子の偽側が選ばれ **`null` が返る**。
- `objectToString`（`:148`）はそれをそのまま返す。
- `isBlankRow`（`:204`）の継続条件は `str != null && !str.isEmpty()`。`str == null` なので **左辺が偽**。
  非空値と判定されず、ループは `return false` に到達しない。
- 全値が Java null の行はループを抜けて `:208` の `return true` に落ちる ⇒ **空行と判定される**。
- `dropBlankRows`（`:185`）がその行を捨てる。

### 2.3 実測（現行挙動）

`YamlLoader`（`YamlLoader.java:104`-`:107`）と同じ `LoadSettings`（`setAllowDuplicateKeys(false)`）で
SnakeYAML Engine 3.0.1 にロードし、`0db2221` の `YamlSection` をそのまま呼んだ生の出力:

```
=== SnakeYAML パース結果（行ごと） ===
row[0] class=java.util.LinkedHashMap value={}
row[1] class=java.util.LinkedHashMap value={USER_ID=, USER_NAME=}
        key=USER_ID valueClass=java.lang.String objectToString=""
        key=USER_NAME valueClass=java.lang.String objectToString=""
row[2] class=java.util.LinkedHashMap value={USER_ID=null, USER_NAME=null}
        key=USER_ID valueClass=Java null objectToString=Java null
        key=USER_NAME valueClass=Java null objectToString=Java null
row[3] class=java.util.LinkedHashMap value={USER_ID=null, USER_NAME=null}
        key=USER_ID valueClass=Java null objectToString=Java null
        key=USER_NAME valueClass=Java null objectToString=Java null
row[4] class=java.util.LinkedHashMap value={USER_ID=~}
        key=USER_ID valueClass=java.lang.String objectToString="~"
row[5] class=java.util.LinkedHashMap value={USER_ID=null, USER_NAME=x}
        key=USER_ID valueClass=java.lang.String objectToString="null"
        key=USER_NAME valueClass=java.lang.String objectToString="x"
row[6] class=java.lang.String value=plain scalar
row[7] class=java.util.LinkedHashMap value={USER_ID=1, USER_NAME=a}
        key=USER_ID valueClass=java.lang.String objectToString="1"
        key=USER_NAME valueClass=java.lang.String objectToString="a"

=== 現行 dropBlankRows の結果 ===
入力 8 行 -> 残り 3 行
  kept: {USER_ID=~}
  kept: {USER_ID=null, USER_NAME=x}
  kept: {USER_ID=1, USER_NAME=a}
resolveColumns(dropBlankRows後) = [USER_ID]
resolveColumns(drop前)          = [USER_ID, USER_NAME]
```

入力 YAML（`row[2]` = アンクォート `null`、`row[3]` = キーのみ値省略）:

```yaml
rows:
  - {}
  - {USER_ID: "", USER_NAME: ""}
  - {USER_ID: null, USER_NAME: null}
  - USER_ID:
    USER_NAME:
  - {USER_ID: ~}
  - {USER_ID: "null", USER_NAME: x}
  - plain scalar
  - {USER_ID: "1", USER_NAME: "a"}
```

読み取れる事実:

- **アンクォートの `null`（row[2]）と、キーだけ書いて値を省略した行（row[3]）は、SnakeYAML が
  どちらも同じ `{USER_ID=null, USER_NAME=null}`（値が Java null）にパースする。** 区別できない／する必要もない。
- 現行はこの 2 行をどちらも捨てている（残り 3 行に含まれない）。
- 副次的事実: **`~` は Java null にならず、文字列 `"~"` になる**（row[4]。snakeyaml-engine の
  既定 JSON スキーマのため）。解説書は `~` を null 表記として挙げていないので仕様どおりだが、
  Excel 側の `NullInterpreter`（"null" のみを見る）とも一致しており、誤解の余地はない。

---

## 3. Excel 側の実測（本丸）

### 3.1 逐語

`nablarch-testing` `3c4bd2a` `src/main/java/nablarch/test/core/reader/PoiXlsReader.java:140`-`:147`

```java
    private boolean isBlankLine(List<String> line) {
        for (String e : line) {
            if (!e.isEmpty()) {
                return false;
            }
        }
        return true;
    }
```

同 `PoiXlsReader.java:119`-`:130`（この `readOneLine` が上の `isBlankLine` の入力を作る）

```java
        List<String> line = new ArrayList<String>(64);
        int lastCellNum = row.getLastCellNum();
        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            String cellValue = cell == null ? "" : cell.toString();
            line.add(cellValue);
            ...
```

同 `src/main/java/nablarch/test/core/reader/TestDataParsingTemplate.java:407`-`:409`

```java
    private boolean isBlankLine(List<String> line) {
        return StringUtil.isNullOrEmpty(line);
    }
```

`nablarch-core` `StringUtil.java:155`-`:165`（`nablarch-core-6-NEXT-SNAPSHOT-sources.jar` から抽出）

```java
    public static boolean isNullOrEmpty(Collection<String> strings) {
        if (strings == null) {
            return true;
        }
        for (String e : strings) {
            if (hasValue(e)) {
                return false;
            }
        }
        return true;
    }
```

呼び出し順（`TestDataParsingTemplate.java:169`-`:190` の `readTestData`）:

```java
            List<String> line = reader.readLine();      // :172  PoiXlsReader 側で空行は既に読み飛ばし済み
            if (line == null) { break; }
            if (isCommentRow(line)) { continue; }        // :176
            line = cutComment(line);                     // :179
            if (isBlankLine(line)) { continue; }         // :180  ← ここが問題の経路
            List<String> interpret = interpret(line);    // :183  ← 値加工はこの後
```

### 3.2 実行と生の出力（1）: `PoiXlsReader` の要素は空文字か Java null か

`PoiXlsReader#readOneLine`（private）をリフレクションで直接呼び、`readLine` の空行スキップより手前の
**生の 1 行** を全件走査した。対象は `nablarch-testing/src` 配下の `.xls`/`.xlsx` 全件。

実行コマンド:

```
JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64 mvn -o dependency:build-classpath \
  -Dmdep.outputFile=<scratchpad>/cp.txt -DincludeScope=test        # nablarch-testing-yaml 上で実行
javac -encoding UTF-8 -cp "$(cat cp.txt)" -d out ExcelProbe.java
java  -cp "out:$(cat cp.txt)" ExcelProbe /home/tie303177/work/nablarch/nablarch-testing/src
```

生の出力:

```
book count = 61
=== PoiXlsReader#readOneLine 全走査結果 ===
sheets            = 246
raw lines         = 11871
cells             = 147632
Java null cells   = 0
empty("") cells   = 131515
all-empty rawlines= 6019  (readLine がスキップする行)
null samples      = []
```

**判定: `PoiXlsReader` が返す `List<String>` の要素は、空セルでも Java null にならない。実測 147,632 セルで 0 件。**
根拠は `PoiXlsReader.java:123` の `cell == null ? "" : cell.toString()`（読んだ内容とも一致）。

### 3.3 実行と生の出力（2）: `TestDataParsingTemplate#isBlankLine` は発火するか

`ListMapParser`（`TestDataParsingTemplate` の具象サブクラス）をリフレクションで生成し、
`isCommentRow` / `cutComment` / `isBlankLine`（いずれも private / package-private）を
`readTestData`（`:169`-`:190`）と同じ順序で呼び、実 Excel 全件を流した。

実行コマンド:

```
javac -encoding UTF-8 -cp "$(cat cp.txt)" -d out BlankLineProbe.java
java  -cp "out:$(cat cp.txt)" BlankLineProbe /home/tie303177/work/nablarch/nablarch-testing/src
```

生の出力:

```
=== (1) isBlankLine の単体挙動（人工入力） ===
isBlankLine([])            = true
isBlankLine(["",""])       = true
isBlankLine([null,null])   = true
isBlankLine([null,"a"])    = false
StringUtil.isNullOrEmpty([null,null]) = true

=== (2) readTestData 相当のパイプラインを実 Excel 全件で流す ===
readLine 通過行数                       = 5852
うちコメント行(isCommentRow=true)       = 1210
readLine 通過セル数                     = 81873
うち Java null のセル数                 = 0
isBlankLine が true を返した回数         = 1
うち Java null を含んでいた回数          = 0
isBlankLine=true サンプル(最大10件):
  RequestTestingSendSyncBatchTest.xls#testPaddingRemoved raw=[, , // 半角スペース20個, , ] cut=[, ]
```

**判定（実測。未確認ではない）:**

- `TestDataParsingTemplate#isBlankLine` という **メソッド自体は実入力で発火する**。5,852 行中 1 行。
  発火の仕組みは「`PoiXlsReader` が非空セル（コメントセル）を含む行として返す → `cutComment` が
  コメント以降を落とす → 残りが `["", ""]` になる」。
- しかし **その 1 件の入力に Java null は含まれない**。パイプラインを通った 81,873 セルすべてで Java null は 0 件。
- したがって **`StringUtil.isNullOrEmpty(Collection)` の「Java null を空扱いする」枝は実入力で一度も踏まれない。**
  Excel 側で Java null が現れるのは `interpret`（`:183`。`NullInterpreter`）を通した **後** だけであり、
  空行判定はその **前**（`:180`）にあるため、Java null が空行判定に関与する余地は無い。

### 3.4 実行と生の出力（3）: Excel は「値が全部 Java null になる行」を残す

Excel 側の期待挙動を末端まで確かめる。`PoiXlsReader` が返すのと同じ形（空セル = 空文字、Java null なし）の
スタブリーダを与え、`NullInterpreter` を積んで `readTestData`（private）を実行した。

実行コマンド:

```
javac -encoding UTF-8 -cp "$(cat cp.txt)" -d out ExcelNullRowProbe.java
java  -cp "out:$(cat cp.txt)" ExcelNullRowProbe
```

入力行（`readLine` が返す想定の 4 行）と生の出力:

```
入力行数 = 4 -> readTestData 出力行数 = 3
  ["USER_ID", "USER_NAME"]
  [<Java null>, <Java null>]
  ["1", "a"]
```

（入力は `["USER_ID","USER_NAME"]` / `["null","null"]` / `["",""]` / `["1","a"]`）

**Excel の確定した挙動:**

| Excel 上の行 | 結果 |
|---|---|
| 全セルが空 | **落ちる**（`PoiXlsReader#isBlankLine`。空文字判定） |
| 全セルが文字列 `null` | **残る**。値加工後は全カラムが Java null になる |

`NullInterpreter`（`nablarch-testing` `3c4bd2a`
`src/main/java/nablarch/test/core/util/interpreter/NullInterpreter.java:14`-`:19`）が
`"null"`（大小文字非区別）を Java null に変換する。テスト用設定（例
`src/test/resources/unit-test.xml:30`）で実際に登録されている。

### 3.5 「Excel に合わせる」の意味（user 判断・案A の裏付け）

上表と YAML の対応:

| 意味 | Excel の書き方 | YAML の書き方 | あるべき扱い |
|---|---|---|---|
| 行が無い | 全セル空 | `{}` / 全値が `""` | 落とす |
| 行はある。全カラムが Java null | 全セルが `null`（文字列） | 全値がアンクォート `null` ／ 値省略 | **残す** |

**現行の YAML 実装は 2 行目のケースを落としてしまう。ここが直す対象である。**

---

## 4. 3 ケースを区別する実装案

### 4.1 判別条件

`isBlankRow` を次の 2 条件だけに限定する。

1. **値が 1 つも無い** — `castMap(row).isEmpty()`。空マッピング `{}` と、マッピングでない行（スカラ・数値）が
   ここに入る（`castMap` が Map でない値に空 Map を返すため。`YamlSection.java:113`-`:118`）。
2. **すべての値が「Java null でない空文字」** — 全値について `objectToString(value)` が
   `""`（`str != null && str.isEmpty()`）。

上記どちらにも当てはまらなければ行として残す。とくに **`objectToString(value) == null`（＝ YAML の Java null）が
1 つでもあれば、その時点で行として存在する**とみなす。

現行との差分は `isBlankRow` の内側 1 行だけである（`str != null && !str.isEmpty()` → `str == null || !str.isEmpty()`）。
ただしそれだけでは値が 0 個の行（`{}`）で `return true` に落ちる保証が分かりにくくなるので、
条件 1 を明示的に前に置く。既存の書き方（`castMap` 経由・拡張 for・早期 return）は踏襲する。

```java
    private static boolean isBlankRow(Object row) {
        Map<String, Object> rowMap = castMap(row);
        if (rowMap.isEmpty()) {
            return true;
        }
        for (Object value : rowMap.values()) {
            String str = objectToString(value);
            if (str == null || !str.isEmpty()) {
                return false;
            }
        }
        return true;
    }
```

### 4.2 3 ケースの当てはめ（実測）

`ColumnsProbe` / `YamlProbe` で上記条件を同じ入力に当てた生の出力:

```
row[0] proposedIsBlankRow=true  <- {}
row[1] proposedIsBlankRow=true  <- {USER_ID=, USER_NAME=}
row[2] proposedIsBlankRow=false  <- {USER_ID=null, USER_NAME=null}
row[3] proposedIsBlankRow=false  <- {USER_ID=null, USER_NAME=null}
row[4] proposedIsBlankRow=false  <- {USER_ID=~}
row[5] proposedIsBlankRow=false  <- {USER_ID=null, USER_NAME=x}
row[6] proposedIsBlankRow=true  <- plain scalar
row[7] proposedIsBlankRow=false  <- {USER_ID=1, USER_NAME=a}
```

| ケース | YAML 記法 | パース結果 | 案の判定 | 解説書 |
|---|---|---|---|---|
| (a) 空マッピング | `- {}` | `{}`（空 Map） | **落ちる** | `notation:1500` の「空マッピング（`{}`）」 |
| (b) 全値が空文字 | `- {A: "", B: ""}` | 値が `""` | **落ちる** | `notation:1500` の「すべての値が空文字」 |
| (c1) アンクォート `null` | `- {A: null, B: null}` | 値が Java null | **残る** | `notation:1500` は挙げていない ⇒ 落としてはならない |
| (c2) キーのみ・値省略 | `- A:` / `  B:` | 値が Java null（c1 と同一） | **残る** | 同上 |
| — マッピングでない行 | `- plain scalar` | `String` | 落ちる（`castMap` が空 Map） | 現行どおり（挙動不変） |

**(c1) と (c2) は SnakeYAML が同じ `{A=null, B=null}` にパースするので（§2.3 の row[2] / row[3]）、
判別条件を 1 つ書けば両方が (c) に入る。区別するコードは不要。**

### 4.3 案の副作用（意図した挙動変更）

- 「一部が Java null、残りが空文字」の行（例 `{A: "", B: null}`）も **残る**ようになる（現行は落ちる）。
  これは Excel の `["", "null"]`（`isBlankLine` が false → 残る）と一致するので、意図どおり。
- マーカーカラムのみが Java null の行（例 `{"[NO]": null}`）も残る。Excel の `["null"]` と一致。
  `notation:1500` 末尾の「この判定はマーカーカラムを除外する前に行われる」とも整合する。
- 空白文字だけの値（`" "`）は現行どおり非空扱い（trim しない）。変更なし。

### 4.4 検討したが採らない案

- **`objectToString` / `toStr` を変えて null を別値に写す** — 採らない。
  `YamlTableDataBuilder#extractRows`（`YamlTableDataBuilder.java:220`）が
  `objectToString(rowMap.get(col))` の戻り値 null をそのまま「Java null のセル値」として使っており、
  `interpret`（`YamlSection.java:247`-`:250`）も null を null で返す設計になっている。
  ここを変えると Java null をデータとして扱う経路が壊れる。**責務は `isBlankRow` に閉じる。**
- **判定を `interpret` の後ろへ動かす** — 採らない。Excel 側は `interpret`（`:183`）の **前** に
  `isBlankLine`（`:180`）を置いており、`dropBlankRows` の javadoc（`YamlSection.java:168`-`:175`）も
  この順序に揃える意図を書いている。既存テスト
  `YamlTableDataBuilderTest#buildTableDataList_rowInterpretedToAllNullIsKept`（`:1383`）がこの順序を固定している。

---

## 5. `resolveColumns` への波及の見立て

### 5.1 何が変わるか（実測）

`dropBlankRows` の後に `resolveColumns`（`YamlSection.java:227`）が走る
（`YamlTableDataBuilder.java:91`-`:92`、`:181`-`:182`）。残る行が増えると **列名を決める行が前にずれる**。

`ColumnsProbe` の生の出力:

```
--- 先頭行が全 Java null（値省略） ---
  parsed rows          = [{USER_ID=null, USER_NAME=null, STATUS=null}, {USER_ID=1}]
  現行 kept            = [{USER_ID=1}] / columns=[USER_ID]
  案   kept            = [{USER_ID=null, USER_NAME=null, STATUS=null}, {USER_ID=1}] / columns=[USER_ID, USER_NAME, STATUS]

--- 先頭行が全 Java null（アンクォート null） ---
  parsed rows          = [{USER_ID=null, USER_NAME=null, STATUS=null}, {USER_ID=1}]
  現行 kept            = [{USER_ID=1}] / columns=[USER_ID]
  案   kept            = [{USER_ID=null, USER_NAME=null, STATUS=null}, {USER_ID=1}] / columns=[USER_ID, USER_NAME, STATUS]

--- 先頭行が全 空文字（落ちる。挙動不変） ---
  parsed rows          = [{USER_ID=, USER_NAME=, STATUS=}, {USER_ID=1}]
  現行 kept            = [{USER_ID=1}] / columns=[USER_ID]
  案   kept            = [{USER_ID=1}] / columns=[USER_ID]

--- 先頭行が空マッピング（落ちる。挙動不変） ---
  parsed rows          = [{}, {USER_ID=1, USER_NAME=n}]
  現行 kept            = [{USER_ID=1, USER_NAME=n}] / columns=[USER_ID, USER_NAME]
  案   kept            = [{USER_ID=1, USER_NAME=n}] / columns=[USER_ID, USER_NAME]
```

- 変わるのは **「先頭側の行が全 Java null」のときだけ**。(a)(b) では列名は変わらない。
- `resolveColumns` 自身のコードは変えない。`:230` の `!rowMap.isEmpty()` ガードは、案でも
  `dropBlankRows` が空マッピングを落とすため到達しないままで、javadoc（`:216`-`:219`）の説明も
  そのまま成り立つ。

### 5.2 解説書 `implementation/testdata_notation.rst:818` との整合

`76e6e61` の `:818` 逐語（該当部分）:

> カラム名は、最初の行（\ ``rows:``\ の先頭要素）のキーで決まる。後続の行がこのキーの一部を持たない場合、そのカラムは\ ``null``\ を明示的に指定したのと同じ扱いになる。後続の行に最初の行のキーにないものを追加しても、そのキーは読み込まれない。

**矛盾しない。むしろ整合が改善する。**

- `:818` の「最初の行」は、`:1500`（読み飛ばし規則）を適用した後の最初の行と読むほかない
  （空マッピングを列名の出所にはできないため）。
- 現行は `:1500` が挙げていない「全値が Java null の行」まで読み飛ばしているので、
  **`{A: null, B: null}` を先頭に書いたユーザから見ると「最初の行のキーで決まる」が成り立っていない**。
  案はこれを `:1500` の文面どおりに戻すので、`:818` の記述と一致する。
- `:818` の「後続の行がこのキーの一部を持たない場合、そのカラムは `null` を明示的に指定したのと同じ扱い」は
  `extractRows`（`YamlTableDataBuilder.java:214`-`:225`）が `rowMap.get(col)`（キー不在なら null）を
  `objectToString` に通す実装で担保されており、案では変更しない。
  なお **この文自体が「値が Java null であること」を正当なデータとして認めている**ので、
  全カラムが Java null の行を「行が無い」とみなす現行の扱いは `:818` の考え方とも噛み合っていない。

### 5.3 #27 で追随が要る既存テスト（見立て。実行して落としてはいない）

`0db2221` の下記テストは fixture の「空行」に **`""` と `null` を混在**させており、案では行が残るため落ちる見込み。

- `src/test/java/nablarch/test/core/reader/yaml/YamlSectionTest.java:434`
  `dropBlankRows_removesEmptyMappingAndAllBlankValueRows`（`rowOf("COL_A", null, "COL_B", null)` を除去対象としている）
- `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest.java` の
  `:1111` / `:1138` / `:1164` / `:1191` / `:1218` / `:1272` / `:1305` / `:1330` / `:1353`
  （fixture `src/test/java/nablarch/test/core/reader/yaml/YamlTableDataBuilderTest/tableData.yaml:97`-`:98`、
  `:114`-`:116`、`:134`-`:136`、`:217`-`:218`、`:227`-`:228`、`:318`-`:319`、`:335`-`:337` などが
  `PK_COL1: ""` と `VARCHAR2_COL: null` を同じ「空行」に混ぜている）

**#27 では「空行 fixture を全値 `""` に直す」＋「全値 Java null の行が残ることを固定する新規テストを足す」
の 2 本立てが要る。** `:1242`（`emptyRowEntryInExpectedTableSkipped`）と `:1383`
（`rowInterpretedToAllNullIsKept`）は fixture 次第で影響なしの見込み。

---

## 6. Javadoc の是正が必要な箇所（このタスクでは直さない。特定のみ）

すべて `0db2221`。

### 6.1 `YamlSection.java:152`-`:159`（`dropBlankRows` の javadoc 第 1 段落）

> 空マッピング（{@code {}}）の行、および全ての値が {@code null} または空文字の行は、Excel の
> 全セル空行と同じく行が無いものとして扱う。マッピングでない行（スカラ等）も構造を持たないため
> ここで取り除く（{@link #castMap(Object)} が Map でない値に対して空 Map を返すので、
> 空マッピングと同じ判定で扱える）。

- 誤りになる箇所: **「全ての値が `{@code null}` または空文字の行」の「`{@code null}` または」**。
  是正後は「全ての値が空文字の行」。
- 「Excel の全セル空行と同じく」は是正後に **正しくなる**（現状はここが実装と食い違っている）。残す。

### 6.2 `YamlSection.java:192`-`:199`（`isBlankRow` の javadoc）

> 行として存在しないもの（全ての値が {@code null} または空文字）か判定する。
> （中略）値が 1 つも無い行（空マッピング・マッピングでない行）も該当する。マーカーカラム
> （{@code [COL]}）の値も判定対象に含める（依存先 nablarch-testing の {@code isBlankLine} が
> 行の全セルを対象とするのに合わせる）。

- 誤りになる箇所: **1 行目の「全ての値が `{@code null}` または空文字」の「`{@code null}` または」**。
- 第 2 段落（値が 1 つも無い行・マーカーカラム）は是正後もそのまま正しい。
- 追記が望ましい: 「値が Java null の行は残す（Excel の文字列 `null` の行が残るのに合わせる）」。

### 6.3 `YamlSection.java:168`-`:175`（`dropBlankRows` の javadoc 第 3 段落・順序の根拠）

> 列名解決（{@link #resolveColumns(List)}）と値加工（{@link #interpret(String, List)}）より前に
> 適用すること。依存先 nablarch-testing では、{@code PoiXlsReader#readLine} が読み込み段階で
> 空行（{@code isBlankLine}）をそのまま読み飛ばす。値加工（{@code interpret}）を持つのは
> {@code TestDataParsingTemplate#readTestData} の方で、こちらも {@code isBlankLine} による
> 読み飛ばしを {@code interpret} より前に行う。本メソッドはこの順序に揃える。この順序により、
> 値加工を通すと空になる値（例えば {@code NullInterpreter} が Java null へ変換する
> {@code "null"}）だけを持つ行も、行としては保持される。

- **誤りになる箇所は無い。**§3.1 / §3.3 の実測どおり、記述はすべて事実と一致する。
- ただし是正後は「YAML でアンクォートの `null`（＝ Java null）だけを持つ行も同様に保持される」旨を
  1 文足すのが親切（Excel の `"null"` 行と YAML の `null` 行が同じ扱いになることが、案の眼目のため）。

### 6.4 上記 3 箇所の外にも同じ文言がある（見落とし防止）

- `YamlTableDataBuilder.java:37`-`:38` — 「全ての値が {@code null} または空文字の行は、列名解決より前に…」
- `YamlTableDataBuilder.java:89`-`:90`（コード内コメント） — 「（空マッピング、および全ての値が null／空文字の行）を…」
- `YamlTableDataBuilder.java:167`-`:168` — 「（空マッピング、および全ての値が null／空文字の行）は列名解決より前に取り除く。」
- `YamlTableDataBuilder.java:210`-`:211` — 「引数の rows は … を通した後の行であり、**値を持つマッピングだけが残っている**。」
  是正後は「全カラムが Java null の行」も残るため、この言い回しは誤解を招く（「キーを持つマッピングだけが残っている」等へ）。

---

## 7. 本指示書に見つけた反例・疑義

**1 件。**

### 7-1. 「`TestDataParsingTemplate.isBlankLine` の経路は実際には発火しない」は不正確

本指示書 §4-1 の記述:

> ディレクターが確認した範囲では、`PoiXlsReader` が返す `List<String>` の要素は空セルでも空文字であって Java null にはならないため、**この経路は実際には発火しないと考えられる**。

実測（§3.3）:

- `PoiXlsReader` が Java null を返さないこと自体は **正しい**（147,632 セルで 0 件）。
- しかし `TestDataParsingTemplate#isBlankLine` は **発火する**。`cutComment`（`:179`）が行末コメントを
  落とした結果、残りが全部空文字になる行があるため（`RequestTestingSendSyncBatchTest.xls#testPaddingRemoved` で 1 件）。
- 発火しないのは **`StringUtil.isNullOrEmpty(Collection)` の「Java null を空扱いする」枝** の方である。

**帰結**: 本タスクの結論（Java null を空扱いする挙動に Excel 側の裏付けは無い ⇒ YAML 側だけの逸脱）は変わらない。
ただし「`isBlankLine` は死んだコードだ」と読める書き方は誤りで、このメソッド自体は
「コメント行を除いた結果の空行」を落とすという別の役割で現に機能している。#27 でこの経路を触る必要は無い。

その他、本指示書の記述はすべて実測と一致した（`isBlankRow` の判定経路、`objectToString`/`toStr` の委譲、
`PoiXlsReader#isBlankLine` が `!e.isEmpty()` だけを見ること、`YamlSection.java:152`-`:159` の javadoc が
「Excel の全セル空行と同じく扱う」と宣言していること、解説書 `76e6e61` の `:818` / `:1500` / `:828`-`:833` の文面）。

---

## 8. #27 への申し送り（実装時にやること）

1. `YamlSection.java:201`-`:209` の `isBlankRow` を §4.1 の条件に差し替える。`objectToString` / `toStr` /
   `dropBlankRows` / `resolveColumns` / `interpret` は変更しない。
2. javadoc / コメントを §6 の 7 箇所（`YamlSection` 3 箇所、`YamlTableDataBuilder` 4 箇所）で是正する。
3. §5.3 の既存テストを追随させ、「全値が Java null の行が残る」「(c1)(c2) が同じ扱いになる」
   「先頭が全 Java null のとき列名がその行から決まる」を固定するテストを足す。
4. 解説書 `notation:1500` は `5301d6e` で是正済み。**解説書側の追加変更は不要**（`nablarch-document` は参照のみ）。
