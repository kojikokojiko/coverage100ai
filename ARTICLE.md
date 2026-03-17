# Claude Code カスタムスキルでJaCoCoブランチカバレッジ100%を自動達成する

## はじめに

「ブランチカバレッジ100%」という縛りが課されているプロジェクトに携わることになった。

分岐の多いビジネスロジックに対してテストを手書きするのは地味に辛い。特に `switch` 文の全ケース、`null` チェックの両パス、`&&`/`||` の短絡評価パスなど、「網羅しなければいけないけど書くのが面倒」な分岐が山ほどある。

そこで **Claude Code のカスタムスキル（スラッシュコマンド）** を作り、`/coverage <ファイルパス>` の一発で100%達成できる仕組みを構築した。

---

## 作ったもの

```
/coverage src/main/java/com/example/service/OrderService.java
```

このコマンドを実行するだけで：

1. `mvn test jacoco:report` を実行してレポートを生成
2. JaCoCo XML から **対象クラスの未カバーブランチだけ** を抽出
3. ソースコードと照合して「どの条件分岐が未テストか」を分析
4. JUnit 5 + Mockito のテストを自動生成・追加
5. テスト実行 → カバレッジ検証 → missed=0 になるまでループ

が自動で行われる。

---

## 構成

```
.claude/
├── commands/
│   └── coverage.md          # スラッシュコマンド定義
└── scripts/
    └── extract_coverage.py  # JaCoCo XML パーサー
```

Claude Code では `.claude/commands/` に Markdown ファイルを置くだけでカスタムスラッシュコマンドを定義できる。チームメンバーがリポジトリをクローンすれば、全員がすぐ `/coverage` を使える。

---

## スキルの実装

### coverage.md（コマンド定義）

スキルファイルは Claude への自然言語の指示書だ。`$ARGUMENTS` にコマンドの引数が入る。

```markdown
# ブランチカバレッジ 100% 自動化

対象ファイル: $ARGUMENTS

## 手順

### Step 1: レポート生成
```bash
mvn test jacoco:report -q 2>&1
```

### Step 2: 未カバーブランチを抽出
```bash
python3 .claude/scripts/extract_coverage.py $ARGUMENTS
```
このJSONだけを見て分析する。jacoco.xml全体は読まない。

### Step 3: ソース読み込み・分析
$ARGUMENTS を読み込み、uncovered_lines の行番号の分岐を特定する。

### Step 4: テスト生成・追加
未カバーブランチをカバーするJUnit 5テストを生成してテストファイルに追加する。

### Step 5: 検証ループ
```bash
mvn test jacoco:report -q 2>&1
python3 .claude/scripts/extract_coverage.py $ARGUMENTS
```
missed=0 になるまで繰り返す（最大5回）。
```

### extract_coverage.py（XMLパーサー）

JaCoCo が生成する `jacoco.xml` は全クラスの情報を含むため、プロジェクトが大きくなると数MB超になる。これを Claude に丸ごと読ませると**コンテキストを大量消費**する。

そこで対象クラスの未カバーブランチだけを抽出するスクリプトを用意した：

```python
import xml.etree.ElementTree as ET
import sys, os, json

target_path = sys.argv[1]
class_name = os.path.basename(target_path)

tree = ET.parse("target/site/jacoco/jacoco.xml")
root = tree.getroot()

result = {"class": class_name, "uncovered_lines": [], "branch_summary": {}}

for sf in root.findall(f'.//sourcefile[@name="{class_name}"]'):
    for line in sf.findall("line"):
        mb = int(line.get("mb", 0))
        if mb > 0:
            result["uncovered_lines"].append({
                "line": int(line.get("nr")),
                "missed_branches": mb,
                "covered_branches": int(line.get("cb", 0))
            })
    for counter in sf.findall("counter[@type='BRANCH']"):
        missed = int(counter.get("missed", 0))
        covered = int(counter.get("covered", 0))
        total = missed + covered
        result["branch_summary"] = {
            "missed": missed, "covered": covered, "total": total,
            "pct": round(covered / total * 100, 1) if total > 0 else 100.0
        }

print(json.dumps(result, indent=2))
```

スクリプト本体はファイルに切り出してあるため、スキル実行時に Claude のコンテキストには入らない。出力されるコンパクトな JSON だけが渡される。

---

## 実際の動作

### 対象プロジェクト

Maven + Spring Boot + JUnit 5 + Mockito のサンプルプロジェクトを用意した。

```java
// 分岐が多いサービスクラスの例
public double calculateDiscountRate(Order order) {
    if (order == null) {
        throw new IllegalArgumentException("Order must not be null");
    }
    boolean isBulk = order.getQuantity() >= 10;

    if (order.isPremiumCustomer() && isBulk) {
        return COMBINED_DISCOUNT;   // 20%
    } else if (order.isPremiumCustomer()) {
        return PREMIUM_DISCOUNT;    // 15%
    } else if (isBulk) {
        return BULK_DISCOUNT;       // 10%
    } else {
        return 0.0;
    }
}
```

初期状態は3テストのみ（カバレッジ52.6%）。`OrderService` に至ってはテストが0件（0%）。

### extract_coverage.py の出力（実行例）

```json
{
  "class": "DiscountService.java",
  "uncovered_lines": [
    { "line": 25, "missed_branches": 1, "covered_branches": 1 },
    { "line": 31, "missed_branches": 1, "covered_branches": 3 },
    { "line": 33, "missed_branches": 1, "covered_branches": 1 },
    { "line": 35, "missed_branches": 1, "covered_branches": 1 },
    { "line": 55, "missed_branches": 1, "covered_branches": 1 },
    { "line": 59, "missed_branches": 4, "covered_branches": 1 }
  ],
  "branch_summary": {
    "missed": 9,
    "covered": 10,
    "total": 19,
    "pct": 52.6
  }
}
```

Claude はこの JSON を受け取り、ソースの該当行と照合して「行25は null チェックのthrowパスが未カバー」「行59は switch の CONFIRMED/SHIPPED/DELIVERED/CANCELLED が未カバー」と特定し、テストを生成する。

### 生成されたテスト（抜粋）

```java
@Test
void test_calculateDiscountRate_nullOrder_throwsException() {
    assertThrows(IllegalArgumentException.class,
            () -> discountService.calculateDiscountRate(null));
}

@Test
void test_calculateDiscountRate_premiumOnly_returnsPremiumDiscount() {
    Order order = new Order("2", "customer2", 5, 100.0, true);
    assertEquals(0.15, discountService.calculateDiscountRate(order));
}

@Test
void test_getStatusMessage_shipped() {
    Order order = new Order("1", "c1", 1, 100.0, false);
    order.setStatus(Order.Status.SHIPPED);
    assertEquals("発送済みです", discountService.getStatusMessage(order));
}
```

### 結果

| クラス | Before | After |
|--------|--------|-------|
| `DiscountService` | 52.6% (missed: 9) | **100%** ✅ |
| `OrderService` | 0% (missed: 18) | **100%** ✅ |

---

## コンテキスト最適化の効果

最初のバージョンでは `jacoco.xml` 全体を Claude に読み込ませていた。スクリプトによる前処理に変えた結果：

| | XML全体を読む（旧） | スクリプト抽出（新） |
|---|---|---|
| Claude に渡す情報量 | **12,269文字** | **~400文字** |
| 削減率 | — | **約97%削減** |
| プロジェクト規模依存 | あり（比例して増大） | **なし（常に一定）** |

スクリプトをファイルに外出しすることで、スキル定義ファイル自体の肥大化も防いでいる。スクリプト本体は Claude のコンテキストに一切入らず、出力結果の JSON だけが渡される。

---

## pom.xml の設定

JaCoCo の設定と、Java 21+ で Mockito の inline mock が使えない問題の対処も必要だった。

```xml
<!-- JaCoCo プラグイン -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>

<!-- Java 21+ で Mockito を使うための設定 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>${argLine} -XX:+EnableDynamicAgentLoading -Xshare:off</argLine>
    </configuration>
</plugin>
```

また、Java 21+ では Mockito の inline mock maker が動かないため、subclass mock maker を使う設定ファイルも必要だ：

```
# src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker
mock-maker-subclass
```

---

## まとめ

Claude Code のカスタムスキルを使うことで、「JaCoCo レポートを解析して不足テストを生成する」という繰り返し作業を完全に自動化できた。

ポイントは3つ：

1. **スキルは自然言語の指示書** — Markdown で手順を書くだけで Claude がその通り動く
2. **前処理スクリプトでコンテキストを絞る** — XML全体を渡すのではなく、必要な情報だけを抽出して渡す
3. **スクリプトはファイルに外出し** — スキル定義自体が肥大化するのを防ぎ、スクリプト本体がコンテキストを占有しない

リポジトリ: https://github.com/kojikokojiko/coverage100ai
