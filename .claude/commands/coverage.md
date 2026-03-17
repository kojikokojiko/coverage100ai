# ブランチカバレッジ 100% 自動化

対象Javaソースファイルのブランチカバレッジを100%にします。

**対象ファイル**: $ARGUMENTS

---

## 実行手順

### Step 1: 最新のJacocoレポートを生成

```bash
mvn test jacoco:report -q 2>&1
```

失敗した場合はエラー内容を確認し、コンパイルエラーや依存関係の問題を先に修正する。

### Step 2: 対象クラスの未カバーブランチだけを抽出

以下のスクリプトを実行して、対象クラスの未カバー情報のみを取得する。
**jacoco.xml 全体を Read ツールで読み込んではいけない**（プロジェクトが大きいと巨大になるため）。

```bash
python3 - << 'PYEOF'
import xml.etree.ElementTree as ET, sys, os, json

target_path = "$ARGUMENTS"
class_name = os.path.basename(target_path)  # e.g. DiscountService.java

xml_path = "target/site/jacoco/jacoco.xml"
if not os.path.exists(xml_path):
    print(json.dumps({"error": "jacoco.xml not found. Run mvn test jacoco:report first."}))
    sys.exit(1)

tree = ET.parse(xml_path)
root = tree.getroot()

result = {"class": class_name, "uncovered_lines": [], "branch_summary": {}}

for sf in root.findall(f'.//sourcefile[@name="{class_name}"]'):
    for line in sf.findall("line"):
        mb = int(line.get("mb", 0))
        cb = int(line.get("cb", 0))
        if mb > 0:
            result["uncovered_lines"].append({
                "line": int(line.get("nr")),
                "missed_branches": mb,
                "covered_branches": cb
            })
    for counter in sf.findall("counter[@type='BRANCH']"):
        missed = int(counter.get("missed", 0))
        covered = int(counter.get("covered", 0))
        total = missed + covered
        result["branch_summary"] = {
            "missed": missed,
            "covered": covered,
            "total": total,
            "pct": round(covered / total * 100, 1) if total > 0 else 100.0
        }

print(json.dumps(result, indent=2))
PYEOF
```

このスクリプトの出力JSON だけを見て未カバーブランチを把握する。

`branch_summary.missed == 0` なら **100%達成済み**。完了を報告して終了する。

### Step 3: ソースファイルを読み込む

`$ARGUMENTS` を Read ツールで読み込む。
Step 2 の `uncovered_lines` の行番号に注目し、**どの条件分岐が未テストか**を分析する。

パッケージ名とクラス名も取得しておく。

典型的なパターン:
- `if (x == null)` → null ケースが未テスト
- `if (flag)` → true/false どちらかが未テスト
- `switch` → 未テストの case がある
- 三項演算子 `a ? b : c` → 片方が未テスト
- `&&` / `||` の短絡評価 → 片方の分岐が未テスト

### Step 4: テストファイルの確認・作成

対応するテストファイルのパスを算出する:
- `src/main/java/com/example/MyService.java`
  → `src/test/java/com/example/MyServiceTest.java`

テストファイルが存在する場合: Read ツールで読み込み、既存テストの構造・使用しているモックを把握する。

テストファイルが存在しない場合: 以下のテンプレートで新規作成する:

```java
package [パッケージ名];

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class [クラス名]Test {

    @InjectMocks
    private [クラス名] target;
}
```

### Step 5: テストコードの生成・追加

Step 2 で特定した未カバーブランチをすべてカバーするテストメソッドを生成し、テストファイルに追加する。

テスト生成の方針:
- 1つのブランチ = 1つの `@Test` メソッド（名前は `test_[メソッド名]_[シナリオ]` 形式）
- 既存テストと重複しないこと
- Mockito で依存オブジェクトをモックし、特定の分岐に誘導する
- `assertThrows` で例外分岐もカバーする
- private メソッドの分岐は、それを呼び出す public メソッド経由でカバーする

### Step 6: テスト実行・カバレッジ再確認

```bash
mvn test jacoco:report -q 2>&1
```

テストが失敗した場合:
1. エラーメッセージを確認して原因を特定
2. テストコードを修正
3. 再度実行

### Step 7: 結果確認・ループ

Step 2 のスクリプトを再実行して結果を確認する（jacoco.xml 全体は読まない）。

- `missed == 0` → **完了！** ブランチカバレッジ100%達成を報告する
- `missed > 0` → まだ未カバーブランチが残っている → Step 3 に戻る（最大5回）

---

## 注意事項

- **jacoco.xml 全体を Read ツールで読み込んではいけない**。必ず Step 2 のスクリプト経由で取得する
- `enum` の暗黙的なブランチや到達不能コードは JaCoCo の除外設定を提案する
- テストの品質より**カバレッジ達成を優先**する（クソ縛りなので）
- ループは最大5回まで。5回で達成できない場合は残りの未カバーブランチと理由を報告する
