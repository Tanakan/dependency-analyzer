plugins {
    java
    war
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "com.example.reporting"
version = "1.2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    // Internal dependencies
    implementation("com.example.analytics:analytics-processor:1.0.0")
    implementation("com.example:inventory-service:1.1.0")
    implementation("com.example.order:order-processing:1.0.0")
    implementation("com.example:payment-service:1.5.0")
    
    // Spring Boot dependencies
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
    
    // Charting library
    implementation("org.webjars:chartjs:3.7.1")
    implementation("org.webjars:bootstrap:5.1.3")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<War> {
    enabled = true
}

tasks.bootWar {
    mainClass.set("com.example.reporting.ReportingDashboardApplication")
}