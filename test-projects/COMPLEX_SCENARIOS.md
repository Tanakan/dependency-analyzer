# Complex Test Projects Structure

This directory contains extremely complex test projects designed to stress-test dependency analyzers with real-world scenarios.

## Complexity Features

### 1. Legacy System Integration
- **legacy-monolith/**: Uses Java 1.6, Spring 2.5.6, Hibernate 3.2.7 with security vulnerabilities
- **legacy-adapter/**: Bridges legacy system with modern Spring Boot 2.7.5, complex exclusions

### 2. Multiple Version Conflicts
- **api-gateway-v1/**: Spring Boot 2.5.14, Spring Cloud Gateway 3.0.4, Jedis
- **api-gateway-v2/**: Spring Boot 3.0.2, Spring Cloud Gateway 4.0.0, Lettuce
- Services depend on different gateway versions creating conflicts

### 3. External System Mocks
- **external-apis-mock/**: Multi-module project with WireMock
- **mock-payment/**: Depends on payment-service API classifier
- **mock-shipping/**: Depends on mock-payment and api-gateway-v1
- **mock-inventory/**: Uses api-gateway-v2 and legacy-adapter

### 4. Complex Build Configurations
- **composite-builds/**: Gradle composite with includedBuilds
- **maven-profiles-example/**: Multiple profiles (dev, prod, integration-test, linux-native)
- Different dependencies per profile

### 5. Nested Multi-Module Projects
- **platform-core/**:
  - Uses Java Platform plugin
  - Nested structure: core-security/security-api, core-security/security-impl
  - core-data with MongoDB, PostgreSQL, Redis implementations
  - Cross-module test dependencies

### 6. Test Dependencies
- **testing-framework/**: Custom framework with JUnit 5, Testcontainers, WireMock
- Used as test-scoped dependency across projects

### 7. Build Tool Mixing
- **hybrid-project/**: Both Maven and Gradle modules
- maven-module depends on gradle-module
- Dynamic versions and version ranges

### 8. Dependency Scenarios

#### Diamond Dependencies
- Multiple paths to jackson-databind: 2.13.4, 2.14.0, 2.14.2, 2.15.0
- Spring versions: 2.5.6 (legacy), 5.3.23, Spring Boot 2.7.x and 3.0.x

#### Version Conflicts
- JWT libraries: jjwt 0.9.1 vs jjwt-api 0.11.5
- Redis clients: Jedis vs Lettuce
- Logging: log4j 1.2.14, slf4j with bridges

#### Scope Variations
- compile, runtime, provided, test, optional
- import scope for BOMs
- System scope (in some configurations)

#### Classifiers
- sources, javadoc, linux-x86_64, osx-x86_64
- Custom API jars with classifier

#### Dynamic Versions
- Gradle: 31.+, latest.release
- Maven: [3.12,4.0), [1.0,2.0)

### 9. Transitive Dependency Chain
- **transitive-chain/**: A→B→C→D→E→F
- Each module adds different scopes and conflicts
- Diamond dependency created at the end

### 10. Platform Dependencies
- **spring-cloud-services/**: Multiple BOM imports
- Spring Cloud, AWS SDK, internal platform BOMs
- enforcedPlatform() usage in Gradle

### 11. Repository Configuration
- Internal Nexus: https://nexus.internal.company.com/repository/maven-public/
- Legacy repository for old artifacts
- Artifactory references

### 12. Complex Service Example
- **complex-service-example/**: Demonstrates all scenarios
- Conflicting API gateway versions
- Legacy integration
- Transitive chains
- Multiple classifiers
- Dynamic versions

## Testing Dependency Analyzers

This structure tests:
1. Version conflict resolution
2. Transitive dependency handling
3. Scope management
4. Classifier support
5. Dynamic version resolution
6. Platform/BOM handling
7. Multi-module navigation
8. Cross-build-tool dependencies
9. Legacy system integration
10. Profile-based dependencies
11. Composite build resolution
12. Optional dependency handling