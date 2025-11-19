import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "com.awssdk.tutorial"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // AWS SDK for Kotlin
    implementation(platform("aws.sdk.kotlin:bom:1.0.57"))
    implementation("aws.sdk.kotlin:s3")
    implementation("aws.sdk.kotlin:dynamodb")
    implementation("aws.sdk.kotlin:sns")
    implementation("aws.sdk.kotlin:sqs")
    implementation("aws.sdk.kotlin:lambda")
    implementation("aws.sdk.kotlin:secretsmanager")
    implementation("aws.sdk.kotlin:sts")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

application {
    mainClass.set("com.awssdk.tutorial.MainKt")
}
