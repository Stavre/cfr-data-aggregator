plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.stavre"
version = "0.0.1-SNAPSHOT"
description = "CFR-data-aggregator"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.cloud.openfeign)
    implementation(libs.picocli.spring.boot)
    compileOnly(libs.lombok)
    developmentOnly(libs.spring.boot.devtools)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.picocli.codegen)
    testImplementation(libs.spring.boot.test)
    testCompileOnly(libs.lombok)
    testRuntimeOnly(libs.junit.launcher)
    testAnnotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform()
}