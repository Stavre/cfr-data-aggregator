plugins {
    java
    pmd
    checkstyle
    jacoco
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
    implementation(libs.jackson.dataformat.csv)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.progressbar)
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

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

pmd {
    toolVersion = libs.versions.pmd.get()
    ruleSets = emptyList()
    ruleSetFiles = files("config/pmd/ruleset.xml")
    isConsoleOutput = true
    isIgnoreFailures = false
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.withType<Test>())
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.bootJar {
    destinationDirectory = layout.projectDirectory
}