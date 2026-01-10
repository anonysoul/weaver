import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("org.openapi.generator") version "7.12.0"
    id("com.diffplug.spotless") version "6.25.0"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
}

group = "com.anonysoul.weaver"
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.xerial:sqlite-jdbc")
    runtimeOnly("org.hibernate.orm:hibernate-community-dialects")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

sourceSets {
    main {
        kotlin.srcDir(projectDir.resolve("build/generated/openapi/src/main/kotlin").absolutePath)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.3.1")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks {
    openApiValidate {
        inputSpec.set(projectDir.resolve("src/main/resources/openapi/openapi.yaml").absolutePath.toString())
    }

    openApiGenerate {
        generatorName.set("kotlin-spring")
        inputSpec.set(projectDir.resolve("src/main/resources/openapi/openapi.yaml").absolutePath.toString())
        outputDir.set(projectDir.resolve("build/generated/openapi").absolutePath.toString())
        apiPackage.set("com.anonysoul.weaver.provider.api")
        modelPackage.set("com.anonysoul.weaver.provider")
        configOptions.set(
            mapOf(
                "enumPropertyNaming" to "UPPERCASE",
                "interfaceOnly" to "true",
                "useSpringBoot3" to "true",
                "requestMappingMode" to "api_interface",
            ),
        )
        cleanupOutput.set(true)
    }

    compileKotlin {
        dependsOn(openApiGenerate)
    }
}
