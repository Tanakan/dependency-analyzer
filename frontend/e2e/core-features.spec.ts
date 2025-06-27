import { test, expect } from '@playwright/test';
import path from 'path';

/**
 * Core Features E2E Test
 * 
 * 重要なテスト観点：
 * 1. アプリケーションの起動
 * 2. データの読み込みとグラフ表示
 * 3. Issues Panelの表示と動作
 * 4. インタラクティブ機能（ノード選択）
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

test.describe('Dependencies Analyzer Core Features', () => {
  test.beforeEach(async ({ page }) => {
    // ページを開く前の準備
    await page.goto('/');
    await hideWebpackOverlay(page);
    await page.waitForLoadState('domcontentloaded');
  });

  test('アプリケーションが正常に起動し、初期画面が表示される', async ({ page }) => {
    // タイトル確認
    await expect(page).toHaveTitle('Dependencies Analyzer');
    
    // ヘッダーが表示されるまで待つ
    await page.waitForSelector('h1', { state: 'visible', timeout: 15000 });
    
    // ヘッダー確認
    await expect(page.locator('h1')).toHaveText('Dependencies Analyzer');
    
    // データが自動的に読み込まれるまで待つ
    await page.waitForSelector('.react-flow', { state: 'visible', timeout: 15000 });
  });

  test('デフォルトデータが自動的に読み込まれてグラフが表示される', async ({ page }) => {
    // データが自動的に読み込まれるまで待つ
    await page.waitForSelector('.react-flow', { state: 'visible', timeout: 15000 });
    
    // React Flowのコンテナが表示される
    await expect(page.locator('.react-flow')).toBeVisible();
    
    // ノードが表示されるまで待つ
    await page.waitForSelector('.custom-node', { state: 'visible', timeout: 10000 });
    
    // ノードが存在する
    const nodes = page.locator('.custom-node');
    const nodeCount = await nodes.count();
    expect(nodeCount).toBeGreaterThan(0);
    
    // サイドバーが表示される
    await expect(page.locator('.sidebar')).toBeVisible();
  });

  test('Issues ページに遷移でき、セクションを展開できる', async ({ page }) => {
    // View Issues Analysis ボタンをクリック
    const viewIssuesButton = page.locator('.nav-link').filter({ hasText: 'View Issues Analysis' });
    await expect(viewIssuesButton).toBeVisible();
    await viewIssuesButton.click();
    
    // Issues ページに遷移
    await expect(page).toHaveURL('/issues');
    
    // Issues Panelが表示されるまで待つ
    await page.waitForSelector('.issues-panel', { state: 'visible', timeout: 15000 });
    
    // Issues Panelのヘッダーを確認
    await expect(page.locator('.issues-panel h2')).toContainText('Project Issues Analysis');
    
    // サマリーが表示される
    await expect(page.locator('.issues-summary')).toBeVisible();
    
    // Unreferenced Projectsセクションを展開
    const unreferencedSection = page.locator('.issue-section').filter({ hasText: 'Unreferenced Projects' });
    const header = unreferencedSection.locator('.section-header');
    await expect(header).toBeVisible();
    
    // クリックして展開
    await header.click();
    
    // コンテンツが表示される
    const content = unreferencedSection.locator('.section-content');
    await expect(content).toBeVisible();
  });

  test('Issuesページとグラフページを行き来できる', async ({ page }) => {
    // View Issues Analysis ボタンをクリック
    const viewIssuesButton = page.locator('.nav-link').filter({ hasText: 'View Issues Analysis' });
    await expect(viewIssuesButton).toBeVisible();
    await viewIssuesButton.click();
    
    // Issues ページに遷移
    await expect(page).toHaveURL('/issues');
    
    // Back to Graph ボタンが表示される
    const backButton = page.locator('.nav-link').filter({ hasText: 'Back to Graph' });
    await expect(backButton).toBeVisible();
    
    // Back to Graph ボタンをクリック
    await backButton.click();
    
    // グラフページに戻る
    await expect(page).toHaveURL('/');
    await expect(page.locator('.react-flow')).toBeVisible();
  });

  test('ノードをクリックするとサイドバーでハイライトされる', async ({ page }) => {
    
    // ノードが表示されるまで待つ
    await page.waitForSelector('.custom-node', { state: 'visible', timeout: 10000 });
    
    // 最初のリポジトリの展開ボタン（chevron）をクリック
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

  // ファイルアップロード機能は削除されたため、このテストはスキップ

  test('ズームコントロールが機能する', async ({ page }) => {
    await page.waitForSelector('.react-flow', { state: 'visible', timeout: 15000 });
    
    // ズームコントロールが表示される
    const controls = page.locator('.react-flow__controls');
    await expect(controls).toBeVisible();
    
    // ズームインボタンをクリック
    const zoomIn = controls.locator('button[aria-label="zoom in"]');
    await expect(zoomIn).toBeVisible();
    await zoomIn.click();
    
    // ズームアウトボタンをクリック
    const zoomOut = controls.locator('button[aria-label="zoom out"]');
    await expect(zoomOut).toBeVisible();
    await zoomOut.click();
    
    // フィットビューボタンをクリック
    const fitView = controls.locator('button[aria-label="fit view"]');
    await expect(fitView).toBeVisible();
    await fitView.click();
  });
});