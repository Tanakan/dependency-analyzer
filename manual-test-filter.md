# Manual Filter Test Instructions

## フィルタ機能のテスト手順

### 1. アプリケーションの起動
```bash
mvn spring-boot:run
```

### 2. ブラウザでアクセス
Chrome で http://localhost:8080 を開く

### 3. プロジェクトフィルタのテスト
1. 左サイドバーで任意のプロジェクト（例：`user-service`）をクリック
2. 確認事項：
   - クリックしたプロジェクトが青色でハイライトされる
   - 「フィルタをクリア」ボタンが表示される
   - グラフ内で関連のないノードが薄く表示される（opacity: 0.2）
   - 関連のないリンクがより薄く表示される（opacity: 0.1）

### 4. リポジトリフィルタのテスト
1. 左サイドバーでリポジトリ名（例：`ecommerce-platform`）をクリック
2. 確認事項：
   - クリックしたリポジトリ名が青色背景になる
   - そのリポジトリ内の全プロジェクトとその依存関係が表示される
   - 他のノードは薄く表示される

### 5. WARプロジェクトの確認
1. 以下のWARプロジェクトが正しく表示されることを確認：
   - `admin-portal` - 赤い四角で表示
   - `customer-portal` - 赤い四角で表示
   - `legacy-webapp` - 赤い四角で表示
   - `reporting-dashboard` - 赤い四角で表示
   - `mobile-backend` - 赤い四角で表示

### 6. フィルタのクリア
1. 「フィルタをクリア」ボタンをクリック
2. 全てのノードとリンクが通常の不透明度に戻ることを確認

### 7. Developer Console での確認
1. Chrome DevTools を開く（F12）
2. Console タブを選択
3. プロジェクトをクリックしてフィルタリング時のログを確認：
   ```
   Filtering by project: com.example:user-service
   Found dependency: com.example:common-libs
   Total links checked: 56
   Related nodes: ["com.example:user-service", "com.example:common-libs"]
   ```

## テスト結果の記録
- [ ] プロジェクトクリックでフィルタが動作する
- [ ] リポジトリクリックでフィルタが動作する
- [ ] WARプロジェクトが赤い四角で表示される
- [ ] JARプロジェクトが青い円で表示される
- [ ] フィルタ適用時に関連ないノードが薄くなる
- [ ] フィルタをクリアボタンが正しく動作する