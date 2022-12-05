plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin", version = "1.7.21"))
    implementation(kotlin("serialization", version = "1.7.21"))
    implementation("com.github.johnrengelman.shadow", "com.github.johnrengelman.shadow.gradle.plugin", "7.1.2")
    implementation("io.ktor.plugin", "plugin", "2.1.3")
    implementation(gradleApi())
    implementation(localGroovy())
}
