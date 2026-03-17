# ブランチカバレッジ 100% 自動化

対象Javaソースファイルのブランチカバレッジを100%にします。

**対象ファイル**: $ARGUMENTS

---

## 実行手順

### Step 1: 対象ファイルの確認

`$ARGUMENTS` を Read ツールで読み込み、以下を取得する:
- パッケージ名 (`package com.example.xxx;`)
- クラス名

対応するテストファイルのパスを算出する:
- `src/main/java/com/example/MyService.java`
  → `src/test/java/com/example/MyServiceTest.java`

### Step 2: 最新のJacocoレポートを生成

```bash
mvn test jacoco:report -q 2>&1
```

失敗した場合はエラー内容を確認し、コンパイルエラーや依存関係の問題を先に修正する。

### Step 3: カバレッジ不足ブランチの特定

`target/site/jacoco/jacoco.xml` を Read ツールで読み込む。

対象クラスの `<sourcefile name="クラス名.java">` セクションを探し、以下を確認する:
- `<counter type="BRANCH" missed="N" covered="M"/>` で `missed > 0` なら未カバーブランチあり
- `<line nr="行番号" ... mb="N" cb="M"/>` で `mb > 0` の行 = 未カバーブランチのある行

特定した行番号をソースコードと照合し、**どの条件分岐が未テストか**を分析する。

典型的なパターン:
- `if (x == null)` → null ケースが未テスト
- `if (flag)` → true/false どちらかが未テスト
- `switch` → 未テストの case がある
- 三項演算子 `a ? b : c` → 片方が未テスト
- `&&` / `||` の短絡評価 → 片方の分岐が未テスト

### Step 4: テストファイルの確認・作成

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

    // モックは必要に応じて追加
}
```

### Step 5: テストコードの生成・追加

Step 3 で特定した未カバーブランチをすべてカバーするテストメソッドを生成し、テストファイルに追加する。

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

### Step 7: カバレッジ検証・ループ

`target/site/jacoco/jacoco.xml` を再度読み込み、対象クラスの `<counter type="BRANCH" missed="N"/>` を確認する。

- `missed="0"` → **完了！** ブランチカバレッジ100%達成を報告する
- `missed > 0` → まだ未カバーブランチが残っている → Step 3 に戻る

---

## 注意事項

- `enum` の暗黙的なブランチや到達不能コードは、`// coverage:ignore` コメントや JaCoCo の除外設定を提案する
- 外部ライブラリのコードは対象外
- テストの品質より**カバレッジ達成を優先**する（クソ縛りなので）
- ループは最大5回まで。5回で達成できない場合は残りの未カバーブランチと理由を報告する
