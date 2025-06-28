# Dependencies Analyzer

## 概要

このプロジェクトは、ローカルに存在するGitリポジトリ内のMavenおよびGradleプロジェクトの依存関係を可視化するツールです。

## プロジェクト構成

このツール自体はMavenプロジェクト（pomプロジェクト）として実装されています。

### アーキテクチャ

- **CLIツール (DependencyAnalyzerCLI)**: 
  - Gitリポジトリをスキャンして依存関係を解析
  - グラフ構造（ノードとリンク）のJSONファイルを出力
  
- **Spring Boot Webアプリケーション**: 
  - CLIで生成されたJSONファイルを読み込み
  - D3.jsを使用してインタラクティブなグラフとして可視化

## 機能

- 指定したディレクトリ配下の複数のGitリポジトリをスキャン
- 各リポジトリ内のMavenプロジェクト（pom.xml）とGradleプロジェクト（build.gradle、build.gradle.kts）を検出
- **社内プロジェクト間の依存関係のみを解析**（外部ライブラリへの依存は対象外）
- 依存関係を視覚的に表示

## 対象

- **Mavenプロジェクト**: pom.xmlファイルで管理される依存関係
- **Gradleプロジェクト**: build.gradleまたはbuild.gradle.ktsファイルで管理される依存関係

## 使用ケース

大規模な開発環境で、複数のリポジトリに分散した社内プロジェクト間の依存関係を把握し、以下のような分析を可能にします：

- 社内プロジェクト間の依存関係の把握
- 循環依存の検出
- 依存関係の影響範囲の確認
- プロジェクト構造の最適化

## 解析対象の判定

以下の条件で社内プロジェクトを判定します：
- スキャン対象ディレクトリ内に存在するプロジェクト
- 依存関係のgroupIdやartifactIdが社内プロジェクトと一致するもの

## 使用方法

### 1. 依存関係の解析（コマンドライン）

```bash
# プロジェクトをビルド
mvn clean compile

# 解析を実行（クラスパスを指定）
java -cp target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) \
  com.example.dependencies.analyzer.cli.DependencyAnalyzerCLI /path/to/repositories

# 結果はdependencies-analysis.jsonファイルに出力される
```

### 2. 解析結果の可視化（Spring Boot）

```bash
# Spring Bootアプリケーションを起動
mvn spring-boot:run

# ブラウザでhttp://localhost:3030にアクセス
# dependencies-analysis.jsonファイルが自動的に読み込まれ、グラフが表示される
```

## 出力形式

CLIツールは `dependencies-analysis.json` というファイルを生成します。このファイルには以下の情報が含まれます：

```json
{
  "analysisDate": "解析実行日時",
  "version": "1.0",
  "nodes": [
    {
      "id": "com.example:project-name",
      "name": "project-name",
      "version": "1.0.0",
      "group": "com.example",
      "type": "Maven/Gradle",
      "nodeGroup": "repository-name"
    }
  ],
  "links": [
    {
      "source": 0,  // ノードのインデックス
      "target": 1,  // ノードのインデックス
      "value": 1
    }
  ],
  "stats": {
    "totalProjects": 7,
    "totalDependencies": 7
  }
}
```

## 動作フロー

1. **解析フェーズ**:
   - CLIツールがディレクトリをスキャン
   - Maven/Gradleプロジェクトを検出
   - 社内プロジェクト間の依存関係を抽出
   - グラフ構造のJSONファイルを生成

2. **可視化フェーズ**:
   - Spring Bootアプリケーションが起動
   - 自動的にJSONファイルを読み込み
   - D3.jsでインタラクティブなグラフを表示

## フロントエンドテスト

### E2Eテスト実行方法

```bash
# フロントエンドディレクトリに移動
cd frontend

# すべてのE2Eテストをheadlessモードで実行
npm run test:e2e

# コア機能のみテスト（推奨）
npm run test:e2e:core

# デバッグモード（ブラウザ表示あり）
npm run test:e2e:debug

# UIモード（インタラクティブ）
npm run test:e2e:ui
```

### テスト内容

- **基本機能**: アプリケーション起動、初期画面表示
- **データ読み込み**: デフォルトデータ、JSONファイルアップロード、エラーハンドリング
- **グラフ表示**: React Flowによるノード・エッジ表示、ズーム・パン機能
- **インタラクション**: ノード選択、サイドバー連動、リポジトリフィルタリング
- **Issues Panel**: 課題分析表示、セクション展開・折りたたみ
- **レスポンシブ**: モバイル・タブレット対応

### 注意事項

- E2Eテストは自動的にheadlessモードで実行されます
- 全78テストが約20秒で完了します
- テスト前にReactアプリケーションが自動起動されます