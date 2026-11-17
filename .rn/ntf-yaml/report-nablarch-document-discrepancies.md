# 解説書（nablarch-document）への申し送り — YAML 実装との記述差異4件

作成日: 2026-08-25

対象ブランチ: `nablarch-document` `ntf-yaml-support`（コミット [`7b93cde`](https://github.com/nablarch/nablarch-document/commit/7b93cde7fd4fc55bdddb7b8be671ed4f9456713c)。作業ツリー clean・origin と一致）
対象ファイル: `ja/development_tools/testing_framework/implementation/testdata_notation.rst`

このリポジトリ（`nablarch-testing-yaml`）は解説書を変更する権限を持たないため、`#18`〜`#22` の作業中に見つかった解説書側の記述と実装の食い違いをここにまとめる。**本ファイルはこのリポジトリの Acceptance criteria の対象外**（`../nablarch-document` への書き込みはしていない）。

---

## 1. カラム名決定行の記述が古い（`:819`）

[testdata_notation.rst#L819](https://github.com/nablarch/nablarch-document/blob/ntf-yaml-support/ja/development_tools/testing_framework/implementation/testdata_notation.rst#L819)

> カラム名は、最初の行（`rows:` の先頭要素）のキーで決まる。

**実装との相違**: `nablarch-testing-yaml` の `YamlSection#dropBlankRows`（`#21` で追加。[`YamlSection.java:182`](../../src/main/java/nablarch/test/core/reader/yaml/YamlSection.java)）は、`resolveColumns` でカラム名を決める**前**に、空マッピング（`{}`）または全ての値が null／空文字の行を取り除く。したがってカラム名は「最初の行」ではなく「**先頭の非空行**」のキーで決まる。

**影響**: 先頭行が空（`{}` や全カラム未記入）で始まる `rows:` を書いた場合、現在の記述どおりならカラム名が0件になり後続のデータ行が消えるかのように読めるが、実際には次の非空行から正しくカラム名が拾われる。

同じ一文の後半（「後続の行がこのキーの一部を持たない場合、そのカラムは `null` を明示的に指定したのと同じ扱いになる。後続の行に最初の行のキーにないものを追加しても、そのキーは読み込まれない。」）は実装と一致しており、修正不要。

---

## 2. `fields` と `rows` の要素数が一致しない場合の挙動が書かれていない（`:1152` / `:1311`）

[testdata_notation.rst#L1152](https://github.com/nablarch/nablarch-document/blob/ntf-yaml-support/ja/development_tools/testing_framework/implementation/testdata_notation.rst#L1152)（ファイルデータ）
[testdata_notation.rst#L1311](https://github.com/nablarch/nablarch-document/blob/ntf-yaml-support/ja/development_tools/testing_framework/implementation/testdata_notation.rst#L1311)（メッセージングデータ）

> `rows:` の各行は配列形式で、`fields:` と同じ順序・同じ件数で値を並べる。

**実装との相違**: 依存先 `nablarch-testing`（`DataFileFragment.java:107`）は
`String value = i < line.size() ? line.get(i) : "";`
で、`names.size()` 回だけループする。つまり:

- 件数が**少ない**場合はエラーにならず、不足した**末尾**のフィールドが `""` として補完される（`rows: [[]]` と書けば全フィールド `""` の1レコードになる — これを利用した省略記法として `#18` でスキーマ `description` に明文化済み）
- 件数が**多い**場合もエラーにならず、超過分がループの対象にならず**無言で切り捨てられる**（警告もログも出ない）

「同じ件数で値を並べる」という規範だけが書かれており、この2つの挙動（不足時の補完・超過時の無言切り捨て）が読み手に伝わらない。

出典（当リポジトリ側の実測記録）: `checks/task-18.md`（`DataFileFragment.java:102-115` に `addValue` の override が無いことを含め確認済み）

---

## 3. 固定長ファイルの穴埋め方式の記述が数値型では不正確（`:885`）

[testdata_notation.rst#L885](https://github.com/nablarch/nablarch-document/blob/ntf-yaml-support/ja/development_tools/testing_framework/implementation/testdata_notation.rst#L885)

> 全フィールドが `""` のレコードとして保持される（固定長ファイルの場合はスペースパディングされた定長レコードとして書き出される）。

**実測との相違**（2026-08-24、`checks/task-18.md` round 3「新事実」。一時テストクラスから `YamlFileBuilder#buildDataFileList` → `DataFile#write()` を実行し `od -c` で出力バイトを確認。実行後に一時ファイルは削除済み）:

固定長（`F1` 半角5 / `F2` 半角5 / `F3` 数値3）で `rows` に `["AAAAA"]` と `[]` を並べた結果、`F3`（数値型）の出力は空白ではなく **ゼロ埋め**（`000`）だった。「スペースパディング」になるのは文字列系の型のみで、数値型には当てはまらない。

---

## 4. YAML の空行スキップ規則が「null」を明記していない（`:1545`）

[testdata_notation.rst#L1545](https://github.com/nablarch/nablarch-document/blob/ntf-yaml-support/ja/development_tools/testing_framework/implementation/testdata_notation.rst#L1545)

> 全要素が null または空文字のエントリは読み飛ばされる。Excel では行の全セルが空の場合、YAML では `rows:` 内の要素が空マッピング（`{}`）またはすべての値が空文字の場合にスキップされる。

**ギャップ**: 第1文は「null **または**空文字」と両方を対象にしているのに、直後の YAML 向けの言い換えでは「空文字」しか挙げておらず、クォートなし `null`（または値省略 `COL:`）だけで構成された行もスキップ対象であることが読み取れない。

実装（`YamlSection#dropBlankRows`、`#21` で追加）は、パース後に Java `null` になった値と空文字を同列に「空」として扱うため、全カラムがクォートなし `null` の行もスキップされる。

**関連する誤読の余地**: 同ファイル `:1456` の値記述の表にある「`null`（クォートなし）も `"null"`（クォートあり）も同じ結果」は、**値としての解釈**（どちらも Java の null になる）を指しており正しい。しかし `:1545` の行スキップ判定はロード後の値で見るため、クォートなし `null` の行は丸ごとスキップされる一方、クォート付き `"null"` の行は非空の文字列として残り、値だけが null になる通常の行として扱われる。この非対称性は現状 `:1545` 付近に書かれておらず、`:1456` の「同じ結果」と読み合わせると誤解を招く。

---

## 対応不要と判断した既存記述（参考）

- `:658` / `:819` 後半（値を書かない行の null 扱い）— 実装と一致。修正不要
- `:1456`「`null` も `"null"` も同じ結果」— 値の解釈としては正しい。上記4.の脚注のとおり文脈をまたぐと誤解の余地があるだけで、文自体に誤りはない
