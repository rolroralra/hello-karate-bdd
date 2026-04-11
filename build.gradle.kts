plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Docker Compose Support (dev only — auto-starts compose.yaml services)
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")

    // Karate BDD
    testImplementation("com.intuit.karate:karate-junit5:1.4.1")

}

tasks.withType<Test> {
    useJUnitPlatform()
    // Required for Karate's dynamic class loading on Java 17+
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED"
    )
    // Activate test profile so H2 is used instead of PostgreSQL
    systemProperty("spring.profiles.active", "test")
}
