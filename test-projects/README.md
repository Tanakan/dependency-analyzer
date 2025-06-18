# Test Projects for Dependencies Analyzer

このディレクトリには、Dependencies Analyzerのテスト用のサンプルプロジェクトが含まれています。

## プロジェクト構成

### 1. ecommerce-platform (Maven マルチモジュール)
- **ecommerce-core**: JPA、H2データベース、Lombok
- **ecommerce-api**: Spring Web、Validation、Swagger (ecommerce-coreに依存)
- **ecommerce-web**: Thymeleaf、Spring Security (ecommerce-apiに依存)

### 2. user-service (Maven 単一プロジェクト) - 更新版
- common-libsを親POMとして使用
- shared-utilsに依存（推移的依存関係）
- Spring Boot、Spring Security、MySQL、JWT、MapStruct

### 3. notification-service (Gradle Kotlin)
- user-serviceに依存
- Spring Web、Mail、Kafka、Twilio、SendGrid

### 4. analytics-platform (Maven + Gradle 混在)
- **analytics-collector** (Maven): ecommerce-apiに依存、Kafka
- **analytics-processor** (Gradle): analytics-collector、notification-serviceに依存、Apache Spark

### 5. common-libs (Maven Parent POM)
- 依存関係管理のための親POM
- Spring Boot、Spring Cloud、その他の共通ライブラリのバージョン管理
- すべての子プロジェクトで使用される依存関係の定義

### 6. microservices-bom (Maven BOM)
- マイクロサービス間のバージョン管理用BOM
- 内部サービスと外部依存関係のバージョン統一
- Spring Boot/Cloud BOMs のインポート

### 7. shared-utils (Maven)
- 複数のプロジェクトで使用される共通ユーティリティ
- Apache Commons、Guava、Jackson、バリデーション
- common-libsを親POMとして使用

### 8. payment-service (Maven)
- user-serviceとshared-utilsに依存（推移的依存関係の例）
- microservices-bomをインポート
- Stripe、PayPal SDK、Redis、Kafka
- Jacksonの異なるバージョンを使用（バージョン競合の例）

### 9. inventory-service (Maven - コンパイルエラーあり)
- 意図的なPOMエラーを含む
- payment-serviceに依存（循環依存の例）
- 不正なXML構造、無効なバージョン形式、存在しないアーティファクト

### 10. order-processing (Gradle マルチモジュール)
- **order-api**: Spring Boot Web アプリケーション、他のすべてのモジュールに依存
- **order-core**: ドメインモデルとビジネスロジック
- **order-persistence**: JPA、複数のデータベースドライバ（バージョン競合）
- **order-messaging**: Kafka、RabbitMQ、AWS SQS、order-persistenceに依存（循環依存）
- **order-integration-tests**: 統合テスト用モジュール、すべてのモジュールに依存

### 11. circular-deps-example (Maven マルチモジュール)
- **service-a**: service-bに依存
- **service-b**: service-cに依存
- **service-c**: service-aに依存（循環依存を完成）

## 依存関係の概要

```
共通ライブラリ:
├── common-libs (Parent POM)
├── microservices-bom (BOM)
└── shared-utils → common-libs

メインサービス:
├── user-service → common-libs, shared-utils
├── payment-service → common-libs, user-service, shared-utils, microservices-bom
└── inventory-service → common-libs, payment-service (循環依存)

Eコマースプラットフォーム:
├── ecommerce-core
├── ecommerce-api → ecommerce-core
└── ecommerce-web → ecommerce-api

通知・分析:
├── notification-service → user-service
└── analytics-platform/
    ├── analytics-collector → ecommerce-api
    └── analytics-processor → analytics-collector, notification-service

注文処理 (Gradle):
└── order-processing/
    ├── order-core
    ├── order-persistence → order-core
    ├── order-messaging → order-core, order-persistence (循環依存)
    └── order-api → order-core, order-persistence, order-messaging

循環依存の例:
└── circular-deps-example/
    └── service-a → service-b → service-c → service-a
```

## 特徴的な依存関係パターン

1. **推移的依存関係**: payment-service → user-service → shared-utils
2. **親POMによる依存関係管理**: common-libs が複数のプロジェクトで使用
3. **BOMの使用**: microservices-bom でバージョン統一
4. **バージョン競合**: 
   - payment-service: Jackson 2.15.2 vs common-libs: Jackson 2.14.2
   - order-persistence: 複数のデータベースドライバ
5. **循環依存**:
   - inventory-service ↔ payment-service
   - order-messaging ↔ order-persistence
   - service-a → service-b → service-c → service-a
6. **コンパイルエラー**: inventory-service の不正なPOM
7. **マルチモジュール**: ecommerce-platform (Maven), order-processing (Gradle)

## テスト実行方法

```bash
cd /Users/masatoshi/Projects/dependencies-analyzer
mvn clean package
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar test-projects
```