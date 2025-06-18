# Auto-centering Test Guide

## 概要
フィルタリング時の自動センタリング機能のテストガイド

## 実装した改善点

1. **シミュレーション再起動**: フィルタリング時にノードの位置が定まっていない場合、force simulationを再起動
2. **待機時間の調整**: 500msに延長してノードの位置が安定するのを待つ
3. **リトライ機能**: ノードの位置が取得できない場合は自動的にリトライ
4. **視覚的フィードバック**: 「フィルタした箇所に移動中...」というインジケーターを表示
5. **デバッグ情報の強化**: より詳細なログ出力

## テスト手順

### 1. アプリケーションの起動
```bash
mvn spring-boot:run
```

### 2. ブラウザでアクセス
- Chrome で http://localhost:8080 を開く
- DevTools (F12) を開いてConsoleタブを選択

### 3. 基本的なフィルタリングテスト

#### テスト 1: 単一プロジェクトのフィルタ
1. サイドバーで任意のプロジェクト（例：`user-service`）をクリック
2. 確認事項：
   - 「フィルタした箇所に移動中...」が一時的に表示される
   - グラフが自動的にフィルタされたノードに移動・ズームする
   - コンソールに以下のログが表示される：
     ```
     Centering on nodes: ["com.example:user-service", ...]
     Visible nodes with positions: [...]
     Bounding box: {...}
     Center: {...}
     SVG dimensions: {...}
     Calculated scale: ...
     Final transform: {...}
     Centering animation completed
     ```

#### テスト 2: リポジトリ全体のフィルタ
1. サイドバーでリポジトリ名（例：`ecommerce-platform`）をクリック
2. 確認事項：
   - 複数のプロジェクトとその依存関係が表示される
   - 全てのノードが画面内に収まるようにズームレベルが調整される
   - アニメーションがスムーズに実行される

#### テスト 3: WARプロジェクトのフィルタ
1. `admin-portal`などのWARプロジェクトをクリック
2. 確認事項：
   - 赤い四角のノードが中心に表示される
   - 依存関係が正しく表示される

### 4. エッジケースのテスト

#### テスト 4: 依存関係のないプロジェクト
1. 依存関係を持たない単独のプロジェクトをクリック
2. 確認事項：
   - 単一のノードが画面中央に表示される
   - ズームレベルが1.5に設定される

#### テスト 5: 多数の依存関係を持つプロジェクト
1. `common-libs`など多くのプロジェクトから参照されるものをクリック
2. 確認事項：
   - 全ての関連ノードが画面内に表示される
   - ズームレベルが適切に調整される（0.5〜2.5の範囲内）

### 5. コンソールでの手動テスト

以下のコマンドをブラウザコンソールで実行：

```javascript
// テスト関数の実行
testFilter();

// 個別のプロジェクトでテスト
filterByProject('com.example:payment-service');

// リポジトリでテスト
filterByRepository('admin-portal');

// 現在の変換状態を確認
const currentTransform = d3.zoomTransform(svg.node());
console.log('Current transform:', {
    x: currentTransform.x,
    y: currentTransform.y,
    scale: currentTransform.k
});
```

### 6. パフォーマンステスト

1. 大きなリポジトリをフィルタ
2. すぐに別のプロジェクトをクリック
3. 確認事項：
   - アニメーションが中断されて新しいフィルタに切り替わる
   - インジケーターが適切に表示・非表示される

## トラブルシューティング

### センタリングが動作しない場合

1. **コンソールエラーを確認**
   - `d.x` や `d.y` が undefined の場合は、force simulationがまだ実行中
   - エラーメッセージがある場合は、その内容を確認

2. **手動でセンタリングをテスト**
   ```javascript
   // 全ノードの位置を確認
   d3.selectAll('.node').each(function(d) {
       console.log(d.id, 'x:', d.x, 'y:', d.y);
   });
   
   // 手動でセンタリング関数を呼び出し
   const testNodes = new Set(['com.example:user-service']);
   centerOnFilteredNodes(testNodes);
   ```

3. **Force Simulationの状態を確認**
   ```javascript
   console.log('Simulation alpha:', simulation.alpha());
   console.log('Simulation running:', simulation.alpha() > 0);
   ```

## 期待される動作

1. フィルタクリック後、0.5秒以内にセンタリングアニメーションが開始
2. 「フィルタした箇所に移動中...」インジケーターが表示
3. 0.75秒のアニメーションでスムーズに移動
4. 全てのフィルタされたノードが画面内に収まる
5. アニメーション完了後、インジケーターが非表示になる

## 既知の制限事項

- 初回読み込み直後は、force simulationが安定するまで若干の遅延がある
- 非常に多くのノード（100以上）がある場合、パフォーマンスが低下する可能性がある
- ズームレベルは0.5〜2.5の範囲に制限されている