buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:2.0.4")
    }
}

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "2.0.4"
}

group = "bot.horo"
version = "0.0.1"
description = "A Discord bot written with love in Kotlin using Discord4J"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.spring.io/snapshot")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.discord4j.discord4j:discord4j-core:2b6287f")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.3.7")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.0-SNAPSHOT")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    shadowJar {
        archiveBaseName.set("Horo")
        archiveVersion.set("")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "bot.horo.MainKt"))
        }
    }
}