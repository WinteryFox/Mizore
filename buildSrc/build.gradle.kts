val kotlin_version: String = "1.8.21"
val ktor_version: String = "2.3.1"

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = kotlin_version))
    implementation(kotlin("serialization", version = kotlin_version))
    implementation("io.ktor.plugin", "plugin", ktor_version)
    implementation("com.google.cloud.tools", "jib-gradle-plugin", "3.3.2")
    implementation("com.github.johnrengelman.shadow", "com.github.johnrengelman.shadow.gradle.plugin", "8.1.1")
    implementation("org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin", "4.2.1.3168")
    implementation(gradleApi())
    implementation(localGroovy())
}
