import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    id("com.google.cloud.tools.jib") version "3.3.1"
    idea
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

group = "bot.horo"
version = "1.0.0"
description = "A multi-functional Discord bot with a focus on tamagotchi"
java.sourceCompatibility = JavaVersion.VERSION_18
java.targetCompatibility = JavaVersion.VERSION_18

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.kotlindiscord.com/repository/maven-public")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    /*implementation("dev.kord:kord-core:0.8.0-M17")
    implementation("dev.kord:kord-gateway:0.8.0-M17")*/
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.5.5-SNAPSHOT")

    runtimeOnly("ch.qos.logback:logback-classic:1.4.5")
    runtimeOnly("org.fusesource.jansi:jansi:2.4.0")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "18"
        }
    }
}

jib {
    to.image = "winteryfox/horobot:$version"
    container.mainClass = "bot.horo.MainKt"
}