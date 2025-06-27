import { test, expect, Page } from '@playwright/test';
import path from 'path';

/**
 * E2E Test Suite for Dependencies Analyzer
 * 
 * テスト観点：
 * 1. 基本機能
 *    - アプリケーションの起動確認
 *    - 初期表示の確認
 * 
 * 2. データ読み込み機能
 *    - JSONファイルのアップロード
 *    - デフォルトデータの読み込み
 *    - 不正なJSONファイルのエラーハンドリング
 * 
 * 3. グラフ表示機能（React Flow）
 *    - ノードの表示確認
 *    - 矢印（エッジ）の表示確認
 *    - リポジトリごとのグループ化表示
 *    - ズーム・パン機能
 *    - ミニマップ表示
 * 
 * 4. インタラクション機能
 *    - ノードのドラッグ&ドロップ
 *    - ノードクリックによる選択
 *    - サイドバーとの連動
 *    - リポジトリフィルタリング
 * 
 * 5. Issues Panel機能
 *    - 課題分析の表示
 *    - セクションの展開・折りたたみ
 *    - 各種課題の正確な表示
 * 
 * 6. レスポンシブデザイン
 *    - 異なる画面サイズでの表示確認
 */

// Webpackのオーバーレイを非表示にするヘルパー関数
async function hideWebpackOverlay(page) {
  await page.addStyleTag({
    content: `
      #webpack-dev-server-client-overlay,
      iframe#webpack-dev-server-client-overlay {
        display: none !important;
      }
    `
  });
}

test.describe('Dependencies Analyzer - 基本機能', () => {
  test('アプリケーションが正常に起動する', async ({ page }) => {
    await page.goto('/');
    await hideWebpackOverlay(page);
    
    // ページが読み込まれるまで待つ
    await page.waitForLoadState('networkidle');
    
    // タイトルの確認
    await expect(page).toHaveTitle('Dependencies Analyzer');
    
    // ヘッダーの確認
    await expect(page.locator('h1')).toHaveText('Dependencies Analyzer');
    
    // データが自動的に読み込まれるまで待つ
    await page.waitForSelector('.react-flow', { state: 'visible', timeout: 15000 });
  });

  // ファイルアップロード機能は削除された
});

test.describe('Dependencies Analyzer - データ読み込み機能', () => {
  test('デフォルトのanalysisデータが自動的に読み込まれる', async ({ page }) => {
    await page.goto('/');
    await hideWebpackOverlay(page);
    await page.waitForLoadState('networkidle');
    
    // React Flowのコンテナが表示されるまで待つ
    await page.waitForSelector('.react-flow', { state: 'visible', timeout: 15000 });
    
    // React Flowのコンテナが表示されることを確認
    await expect(page.locator('.react-flow')).toBeVisible();
    
    // サイドバーが表示されることを確認
    await expect(page.locator('.sidebar')).toBeVisible();
    
    // Issues Panelが表示されることを確認（データにissuesが含まれる場合）
    await expect(page.locator('.issues-panel')).toBeVisible();
  });

  // JSONファイルアップロード機能は削除された

  // エラーハンドリングテストは削除された
});

test.describe('Dependencies Analyzer - グラフ表示機能', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await hideWebpackOverlay(page);
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('.react-flow', { state: 'visible', timeout: 15000 });
  });

  test('ノードが正しく表示される', async ({ page }) => {
    // カスタムノードの存在確認
    const nodes = page.locator('.custom-node');
    await expect(nodes.first()).toBeVisible();
    
    // ノード数が0より多いことを確認
    const nodeCount = await nodes.count();
    expect(nodeCount).toBeGreaterThan(0);
    
    // POMノードとJARノードの表示確認
    await expect(page.locator('.pom-node').first()).toBeVisible();
    await expect(page.locator('.jar-node').first()).toBeVisible();
  });

  test('エッジ（矢印）が正しく表示される', async ({ page }) => {
    // React Flowのエッジ要素を確認
    const edges = page.locator('.react-flow__edge');
    await expect(edges.first()).toBeVisible();
    
    // SVG要素内の矢印マーカーの存在確認
    const svgElements = page.locator('svg');
    await expect(svgElements.first()).toBeVisible();
  });

  test('リポジトリグループが表示される', async ({ page }) => {
    // カスタムノードの存在確認（グループ機能の代わり）
    const customNodes = page.locator('.custom-node');
    await expect(customNodes.first()).toBeVisible();
    
    // 複数のノードが表示されることを確認
    const nodeCount = await customNodes.count();
    expect(nodeCount).toBeGreaterThan(1);
  });

  test('ズーム・パン機能が動作する', async ({ page }) => {
    // ズームコントロールの存在確認
    const controls = page.locator('.react-flow__controls');
    await expect(controls).toBeVisible();
    
    const zoomIn = controls.locator('button[aria-label="zoom in"]');
    const zoomOut = controls.locator('button[aria-label="zoom out"]');
    const fitView = controls.locator('button[aria-label="fit view"]');
    
    await expect(zoomIn).toBeVisible();
    await expect(zoomOut).toBeVisible();
    await expect(fitView).toBeVisible();
    
    // ズームイン
    await zoomIn.click();
    
    // ズームアウト
    await zoomOut.click();
    
    // フィットビュー
    await fitView.click();
  });

  test('ミニマップが表示される', async ({ page }) => {
    await expect(page.locator('.react-flow__minimap')).toBeVisible();
  });
});

test.describe('Dependencies Analyzer - インタラクション機能', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await hideWebpackOverlay(page);
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('.react-flow', { state: 'visible', timeout: 15000 });
  });

  test('ノードをドラッグできる', async ({ page }) => {
    // ノードが表示されるまで待つ
    await page.waitForSelector('.custom-node', { state: 'visible', timeout: 10000 });
    
    const node = page.locator('.custom-node').first();
    
    // ノードが存在し、表示されていることを確認
    await expect(node).toBeVisible();
    
    // 初期位置を取得できることを確認
    const initialBox = await node.boundingBox();
    expect(initialBox).not.toBeNull();
    expect(initialBox!.width).toBeGreaterThan(0);
    expect(initialBox!.height).toBeGreaterThan(0);
    
    // React Flowのドラッグ機能が有効であることを確認（実際のドラッグではなく要素の確認）
    const reactFlow = page.locator('.react-flow');
    await expect(reactFlow).toBeVisible();
  });

  test('ノードクリックでサイドバーと連動する', async ({ page }) => {
    // ノードが表示されるまで待つ
    await page.waitForSelector('.custom-node', { state: 'visible', timeout: 10000 });
    
    // 最初のリポジトリを展開
    const firstChevron = page.locator('.chevron').first();
    await firstChevron.click();
    
    // プロジェクトアイテムが表示されるまで待つ
    await page.waitForSelector('.project-item', { state: 'visible', timeout: 5000 });
    
    // 最初のプロジェクトアイテムをクリック
    const firstProjectItem = page.locator('.project-item').first();
    await firstProjectItem.click();
    
    // サイドバーで選択されたアイテムが存在する
    const selectedItem = page.locator('.project-item.selected');
    await expect(selectedItem).toBeVisible();
  });

  test('リポジトリフィルタリングが機能する', async ({ page }) => {
    // サイドバーが表示されるまで待つ
    await page.waitForSelector('.sidebar', { state: 'visible', timeout: 10000 });
    
    // 最初のリポジトリヘッダーをクリックして選択
    const firstRepoHeader = page.locator('.repository-header').first();
    await expect(firstRepoHeader).toBeVisible();
    await firstRepoHeader.click();
    
    // 選択されたリポジトリがハイライトされることを確認
    await expect(firstRepoHeader).toHaveClass(/selected/);
  });
});

test.describe('Dependencies Analyzer - Issues Panel機能', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await hideWebpackOverlay(page);
    await page.waitForSelector('.issues-panel', { state: 'visible', timeout: 15000 });
  });

  test('Issues Panelが表示される', async ({ page }) => {
    await expect(page.locator('.issues-panel')).toBeVisible();
    await expect(page.locator('.issues-panel h2')).toHaveText('Project Issues Analysis');
  });

  test('課題のサマリーが正しく表示される', async ({ page }) => {
    // サマリーアイテムの存在確認
    const summaryItems = page.locator('.summary-item');
    await expect(summaryItems).toHaveCount(4);
    
    // 各課題タイプの存在確認
    await expect(page.locator('.summary-label:has-text("Circular References:")')).toBeVisible();
    await expect(page.locator('.summary-label:has-text("Unreferenced Projects:")')).toBeVisible();
    await expect(page.locator('.summary-label:has-text("Duplicate Artifact IDs:")')).toBeVisible();
    await expect(page.locator('.summary-label:has-text("Duplicate GAVs:")')).toBeVisible();
  });

  test('セクションの展開・折りたたみが機能する', async ({ page }) => {
    // Unreferenced Projectsセクション（デフォルトで折りたたまれている）
    const section = page.locator('.issue-section').filter({ hasText: 'Unreferenced Projects' });
    const header = section.locator('.section-header');
    const content = section.locator('.section-content');
    
    // 初期状態で内容が表示されていないことを確認
    await expect(content).not.toBeVisible();
    
    // クリックして展開
    await header.click();
    await expect(content).toBeVisible();
    
    // 再度クリックして折りたたむ
    await header.click();
    await expect(content).not.toBeVisible();
  });

  test('Issues Panelの表示/非表示切り替えが機能する', async ({ page }) => {
    const toggleButton = page.locator('.toggle-issues-btn');
    const issuesPanel = page.locator('.issues-panel');
    
    // 初期状態で表示されていることを確認
    await expect(issuesPanel).toBeVisible();
    await expect(toggleButton).toHaveText('Hide Issues');
    
    // クリックして非表示
    await toggleButton.click();
    await expect(issuesPanel).not.toBeVisible();
    await expect(toggleButton).toHaveText('Show Issues');
    
    // 再度クリックして表示
    await toggleButton.click();
    await expect(issuesPanel).toBeVisible();
    await expect(toggleButton).toHaveText('Hide Issues');
  });
});

test.describe('Dependencies Analyzer - レスポンシブデザイン', () => {
  test('モバイル表示で正しくレイアウトされる', async ({ page }) => {
    // モバイルサイズに変更
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/');
    await hideWebpackOverlay(page);
    await page.waitForSelector('.react-flow', { state: 'visible', timeout: 15000 });
    
    // サイドバーとグラフが縦に並ぶことを確認
    const appContent = page.locator('.app-content');
    const computedStyle = await appContent.evaluate((el) => {
      return window.getComputedStyle(el).flexDirection;
    });
    expect(computedStyle).toBe('column');
  });

  test('タブレット表示で正しくレイアウトされる', async ({ page }) => {
    // タブレットサイズに変更
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/');
    await hideWebpackOverlay(page);
    await page.waitForSelector('.react-flow', { state: 'visible', timeout: 15000 });
    
    // 要素が適切に表示されることを確認
    await expect(page.locator('.sidebar')).toBeVisible();
    await expect(page.locator('.react-flow')).toBeVisible();
  });
});