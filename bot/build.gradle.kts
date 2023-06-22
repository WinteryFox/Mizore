import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val logback_version: String by project
val jansi_version: String by project
val kordex_version: String by project

plugins {
    kotlin("jvm")
    id("application")
    id("com.google.cloud.tools.jib")
    id("com.github.johnrengelman.shadow")
    id("io.ktor.plugin")
    sources
}

description = "The gateway for Mizore"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("com.kotlindiscord.kord.extensions:kord-extensions:$kordex_version")
    implementation("com.kotlindiscord.kord.extensions:unsafe:$kordex_version")
    implementation("com.rabbitmq:amqp-client:5.16.0")
    implementation(project(":gateway"))

    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-logging-jvm:$ktor_version")
    implementation(project(mapOf("path" to ":api")))

    runtimeOnly("ch.qos.logback:logback-classic:$logback_version")
    runtimeOnly("org.fusesource.jansi:jansi:$jansi_version")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "18"
            kotlinOptions.freeCompilerArgs = listOf("-Xcontext-receivers")
        }
    }
}

jib {
    to {
        image = "winteryfox/mizore-bot"
        tags = setOf("latest", version.toString())
    }
    from.image = "amazoncorretto:19-alpine3.16"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "$group.$name.MainKt"
        )
    }
}
