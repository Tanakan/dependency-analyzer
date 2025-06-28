# Dependencies Analyzer - Dev Container

このプロジェクトはDev Containerを使用して一貫した開発環境を提供します。

## 必要な環境

- Visual Studio Code
- Docker Desktop
- Dev Containers 拡張機能

## セットアップ

1. このリポジトリをクローン
2. Visual Studio Codeでプロジェクトフォルダーを開く
3. Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) を開く
4. "Dev Containers: Reopen in Container" を選択

## 含まれる環境

### 言語・ランタイム
- Java 17 (OpenJDK)
- Node.js 18
- Maven
- npm

### VS Code 拡張機能
- Java Extension Pack
- Spring Boot Extension Pack
- ESLint
- Prettier
- Maven for Java

### 開発ツール
- Git
- Docker-in-Docker
- curl, wget, vim, tree, jq

## 利用可能なコマンド

### バックエンド
```bash
# 依存関係の解析実行
mvn clean compile
java -cp target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) \
  com.example.dependencies.analyzer.cli.DependencyAnalyzerCLI /path/to/repositories

# Spring Boot起動
mvn spring-boot:run
```

### フロントエンド
```bash
cd frontend
npm start
```

## ポート転送

- `:3000` - React開発サーバー
- `:3030` - Spring Boot アプリケーション

## トラブルシューティング

### Mavenキャッシュの問題
```bash
mvn dependency:purge-local-repository
mvn clean install
```

### Nodeモジュールの問題
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```