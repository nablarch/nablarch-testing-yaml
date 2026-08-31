# 期待値0件のテーブル検証が必ず PASS する事象（NTF 本体）

作成日: 2026-08-13

---

## 1. 何が起きるか

NTF のテストで「このテーブルには1行も無いはずだ」を検証したいことがある。
たとえば「権限を持たないユーザなので、権限テーブルには何も登録されない」を確かめる場合である。

このとき、**カラム名の行が無いと、テーブルに行が残っていても検証が必ず成功してしまう。**
検証したつもりで、実際には何も検証していない状態になる（偽陰性）。

テストが「嘘の合格」を返すため、バグを見逃す。

---

## 2. なぜそうなるか

NTF がテーブルを検証する手順は次のとおり。

1. テストデータに書いた期待値（0行）を用意する
2. DB から実際のデータを読む
3. 1 と 2 を突き合わせる

問題は 2 である。DB からデータを読むには `SELECT カラム名 FROM テーブル名` を組み立てる必要があり、
**カラム名が1つも無いと SQL が作れない。** そこで NTF は SQL を投げずに「実際のデータは0行」として扱う。

```
期待値: 0行（テストデータにそう書いた）
実際値: 0行（DB を読んでいないので、中身に関係なく0行）
        → 一致 → PASS
```

DB に100件残っていても PASS する。

### 該当箇所（GitHub / nablarch-testing main ブランチ）

| 内容 | URL |
|---|---|
| 期待値を clone して `loadData()` で DB を読む | [Assertion.java#L79-L83](https://github.com/nablarch/nablarch-testing/blob/main/src/main/java/nablarch/test/Assertion.java#L79-L83) |
| **カラム名が0件なら SQL を投げずに0行を返す（本事象の核心）** | [TableData.java#L337-L347](https://github.com/nablarch/nablarch-testing/blob/main/src/main/java/nablarch/test/core/db/TableData.java#L337-L347) |
| カラム名が未設定なら DbInfo から取得する（0件の配列はこの分岐に入らない） | [TableData.java#L501-L508](https://github.com/nablarch/nablarch-testing/blob/main/src/main/java/nablarch/test/core/db/TableData.java#L501-L508) |

---

## 3. どういう書き方をすると踏むか

### Excel 形式 — カラム名の行を書かなかった場合のみ

Excel リーダは、識別子の行（`EXPECTED_TABLE=テーブル名`）の**次の行を無条件にカラム名の行として読む**。

| 該当箇所 | URL |
|---|---|
| 識別子の次の行をカラム名の行として読む | [TableDataParser.java#L89-L97](https://github.com/nablarch/nablarch-testing/blob/main/src/main/java/nablarch/test/core/reader/TableDataParser.java#L89-L97) |
| 空行は読み飛ばす | [PoiXlsReader.java#L83-L96](https://github.com/nablarch/nablarch-testing/blob/main/src/main/java/nablarch/test/core/reader/PoiXlsReader.java#L83-L96) |
| 読む行が無い（null）ならカラム名0件になる | [HeaderLine.java#L32-L41](https://github.com/nablarch/nablarch-testing/blob/main/src/main/java/nablarch/test/core/reader/HeaderLine.java#L32-L41) |

実際に xlsx を作って本体の Excel リーダに読ませた結果（実測）:

| シートの中身 | 得られたカラム名 | 判定 |
|---|---|---|
| `EXPECTED_TABLE=CLIENT` がシート最終行（カラム名の行なし） | `[]` | **本事象が発生** |
| `EXPECTED_TABLE=CLIENT` + カラム名の行 `CLIENT_ID`,`NAME`（対照） | `[CLIENT_ID, NAME]` | 正常 |
| カラム名の位置に空行を置く | `[]` | **本事象が発生**（空行は飛ばされ最終行扱い） |
| カラム名を書かず次のブロックが続く | `[EXPECTED_TABLE=PROJECT]` | 別事象。次のブロックの行がカラム名として食われ、**そのブロックごと消える** |
| カラム名の行がマーカーカラム `[TEST]` のみ | `[]` | **本事象が発生** |

空行が読み飛ばされるため、「カラム名の行だけ空ける」という書き方はできない。
つまり Excel で踏むのは、**識別子の行がシート末尾にある／カラム名がマーカーだけ**といった限られた場合である。

### YAML 形式 — `rows: []` と書くと必ず踏む

YAML ではカラム名を書く専用の場所が無く、`rows:` の先頭要素のキーがそのままカラム名になる。

```yaml
expected_tables:
  - table: "CLIENT"
    rows:
      - CLIENT_ID: "1"      # このキーがカラム名になる
        NAME: "山田"
```

したがって行が0件だとカラム名を書く場所が無い。

```yaml
expected_tables:
  - table: "CLIENT"
    rows: []               # カラム名が1つも無い → 必ず本事象が発生
```

---

## 4. 準備データ（SETUP_TABLE）は影響を受けない

同じ「0件」でも、準備データ側は正常に動く。実測で確認済み。

```
確認前の行数 = 1
カラム名の数 = 0
テーブル初期化 = 成功
確認後の行数 = 0        ← 0件に初期化されている
```

| 内容 | URL |
|---|---|
| 初期化は削除→挿入の順に行う | [TableData.java#L117-L120](https://github.com/nablarch/nablarch-testing/blob/main/src/main/java/nablarch/test/core/db/TableData.java#L117-L120) |
| 削除は `DELETE FROM テーブル名` だけでカラム名を使わない | [TableData.java#L127-L130](https://github.com/nablarch/nablarch-testing/blob/main/src/main/java/nablarch/test/core/db/TableData.java#L127-L130) |
| 挿入は保持している行を1件ずつ処理するため、行が0件なら何も実行されない | [TableData.java#L137-L140](https://github.com/nablarch/nablarch-testing/blob/main/src/main/java/nablarch/test/core/db/TableData.java#L137-L140) |

**壊れているのは期待値の検証だけである。**

---

## 5. 解説書の記載状況

**「カラム名は必須」という記載は無い。** 記法の説明として構成を示しているだけである。

### main ブランチ（現行の解説書）

| 記載内容 | URL |
|---|---|
| データタイプ一覧。`EXPECTED_TABLE` は「テスト実行後の期待するデータベースのデータ」 | [01_Abstract.rst#L275](https://github.com/nablarch/nablarch-document/blob/main/ja/development_tools/testing_framework/guide/development_guide/06_TestFWGuide/01_Abstract.rst#L275) |
| 共通の書式「データ1行目は『データタイプ=値』」「2行目以降の書式はデータタイプにより異なる」 | [01_Abstract.rst#L303-L306](https://github.com/nablarch/nablarch-document/blob/main/ja/development_tools/testing_framework/guide/development_guide/06_TestFWGuide/01_Abstract.rst#L303-L306) |
| `SETUP_TABLE=COMPOSER` の記載例（カラム名の行あり） | [01_Abstract.rst#L314-L322](https://github.com/nablarch/nablarch-document/blob/main/ja/development_tools/testing_framework/guide/development_guide/06_TestFWGuide/01_Abstract.rst#L314-L322) |
| `EXPECTED_TABLE=PLAYER` の記載例（カラム名の行あり） | [01_Abstract.rst#L333-L343](https://github.com/nablarch/nablarch-document/blob/main/ja/development_tools/testing_framework/guide/development_guide/06_TestFWGuide/01_Abstract.rst#L333-L343) |
| マーカーカラム（`[ ]` で囲んだカラム名は読み込み対象外） | [01_Abstract.rst#L360](https://github.com/nablarch/nablarch-document/blob/main/ja/development_tools/testing_framework/guide/development_guide/06_TestFWGuide/01_Abstract.rst#L360) |

「2行目以降の書式はデータタイプにより異なる」とあるだけで、テーブル系でカラム名の行が必須かどうかは書かれていない。

### 刷新中ブランチ（`ntf-yaml-support`）

| 記載内容 | URL |
|---|---|
| 「データタイプと識別子の値・**カラム名**・データ行という共通の構成を持つ」 | [testdata_notation.rst#L650](https://github.com/nablarch/nablarch-document/blob/ntf-yaml-support/ja/development_tools/testing_framework/implementation/testdata_notation.rst#L650) |
| Excel 形式「識別子行に続けて、カラム名の行とデータ行を記載する」 | [testdata_notation.rst#L731](https://github.com/nablarch/nablarch-document/blob/ntf-yaml-support/ja/development_tools/testing_framework/implementation/testdata_notation.rst#L731) |
| YAML 形式「カラム名は、最初の行（`rows:` の先頭要素）のキーで決まる」 | [testdata_notation.rst#L792](https://github.com/nablarch/nablarch-document/blob/ntf-yaml-support/ja/development_tools/testing_framework/implementation/testdata_notation.rst#L792) |

構成として示してはいるが、こちらにも「必須」「省略した場合はこうなる」の記述は無い。

`rows: []`（データ行0件）についての記述は、`ja/development_tools/` 配下の rst 全体で **0件**。
カラム名を省略した場合の挙動についての記述も無い。

### 記載例ではカラム名が書かれているか → **すべて書かれている**

調べた結果、**0件のケースの公式例が2つあり、いずれもカラム名の行を書いている。**

**① 期待値0件の例（本事象そのものの場面）**

[02_componentUnitTest.rst#L262](https://github.com/nablarch/nablarch-document/blob/main/ja/development_tools/testing_framework/guide/development_guide/05_UnitTestGuide/01_ClassUnitTest/02_componentUnitTest.rst#L262)
が参照する図 [componentUnitTest_expectedDataNormal.png](https://github.com/nablarch/nablarch-document/blob/main/ja/development_tools/testing_framework/guide/development_guide/05_UnitTestGuide/01_ClassUnitTest/_image/componentUnitTest_expectedDataNormal.png)

図中の case1（「エラーが発生しない、システムアカウント権限なし」）に、次のブロックがある。

```
EXPECTED_TABLE[case1]=SYSTEM_ACCOUNT_AUTHORITY
USER_ID | USE_CASE_ID | INSERT_USER_ID | INSERT_DATE | UPDATED_USER_ID | UPDATED_DATE
                                     ← データ行なし
```

「権限が登録されていないこと」を0行の期待値で検証している。
**カラム名の行が書かれているため、この例は現状の実装でも正しく動く。**
（同じ図の case2「権限あり(1件)」では同じテーブルに1行が書かれている）

**② 準備データ0件の例**

[02_componentUnitTest.rst#L108-L110](https://github.com/nablarch/nablarch-document/blob/main/ja/development_tools/testing_framework/guide/development_guide/05_UnitTestGuide/01_ClassUnitTest/02_componentUnitTest.rst#L108-L110)
が「USERS / UGROUP_SYSTEM_ACCOUNT / SYSTEM_ACCOUNT_AUTHORITY は初期データ0件」と説明し、
図 [componentUnitTest_Setup.png](https://github.com/nablarch/nablarch-document/blob/main/ja/development_tools/testing_framework/guide/development_guide/05_UnitTestGuide/01_ClassUnitTest/_image/componentUnitTest_Setup.png)
では該当する3ブロックがいずれも `SETUP_TABLE=テーブル名` ＋ カラム名の行のみ、データ行なしの形になっている。

**まとめ**: 本文に「必須」と明記されていないが、公式の記載例はすべてカラム名の行を書いている。
Excel 形式で本事象を踏むのは、記載例から外れた書き方をした場合に限られる。

---

## 6. 単純に直した場合の影響（後方互換）

`loadData()` が「カラム名0件でも DB を読む」ようになると、**これまで PASS していたテストが FAIL し始める。**

これは正しい挙動である。「テーブルが空であること」を検証したかったのに検証されていなかったものが、
本来の検証を行うようになるだけで、**落ちるのは意図したアサートの結果**である。

ただし次の懸念がある。

- 既存プロジェクトで大量に FAIL が発生すると、バージョンアップの障壁になる
- 落ちたテストが「本当のバグの検出」なのか「テストデータの書き方の問題」なのか、
  1件ずつの切り分けが必要になる

Excel 形式では公式の記載例どおりに書いていれば踏まないため、影響は限定的と見込まれる。
一方 YAML 形式では `rows: []` と書いたものがすべて対象になる。

---

## 7. 対応案

### A. 不具合として直す

`loadData()` を修正し、カラム名が0件のときも DB のカラムを使って SELECT する。

- 利点: 事象が根本から消える。設定も分岐も増えない
- 欠点: 後方互換影響を利用者が一斉に受ける

### B. 直したうえで、設定で現行動作に戻せるようにする

A と同じく `loadData()` を修正して**新しい動作（カラム名0件でも DB のカラムを使って SELECT する）を既定**とし、
設定を入れたときだけ現行動作（SQL を投げず0行扱い）に戻せるようにする。

- 利点: 新規利用者は何も設定せずにあるべき姿になる。
  バージョンアップで後方互換を維持したい既存プロジェクトだけが設定を入れればよく、移行時期を選べる
- 欠点: 設定と分岐が増える

### C. YAML 形式のときだけ直す

Excel 形式は現行動作のまま、YAML 形式から読んだデータのときだけ SELECT する。

- 利点: Excel 側の後方互換影響がゼロ。YAML は新形式なので互換の制約が無い
- 欠点: 形式によって検証の挙動が変わる。Excel 側の事象は残る

---

## 8. 前提（YAML 形式のスキーマについて）

YAML 形式はデータモデルをそのまま表す形式であり、**現在のスキーマがあるべき姿である。**
1行を1つのオブジェクトとして書き、カラム名はその行のキーとして表れる。

行が0件のときにカラム名だけを別に宣言する仕組み（`columns:` のような追加フィールド）は導入しない。
カラム名は DB のスキーマが持っている情報であり、テストデータに二重に持たせるものではないためである。

したがって本事象の対応は、**テストデータ形式側ではなく NTF 本体側（`loadData()`）で行う。**

---

## 9. 判断が必要な事項

1. A・B・C のどれを採るか
2. 解説書に「カラム名の行は省略できない」旨を明記するか（現状は未定義）
3. カラム名を書かず次のブロックが続く場合に**ブロックが黙って消える**件（3章の表4行目）を、
   本事象とは別の課題として扱うか

---

## 付録: 確認方法

本書の実測値は次の方法で取得した。

- **Excel の挙動**: `nablarch-testing` の `BasicTestDataParser` と `PoiXlsReader` に、
  Apache POI で生成した実 xlsx を読ませ、生成された `TableData` のカラム名を出力した
- **準備データの挙動**: DB に1件挿入した状態で `rows: []` の `setup_tables` を読み込み、
  `TableData#replaceData()` を実行して前後の行数を確認した
- **解説書**: `nablarch-document` の `main` および `ntf-yaml-support` ブランチのソース（rst と図）を参照した
