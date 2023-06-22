import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("application")
    id("com.google.cloud.tools.jib")
    id("com.github.johnrengelman.shadow")
}

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
    implementation("dev.kord:kord-core:0.8.x-SNAPSHOT")
    implementation("dev.kord:kord-gateway:0.8.x-SNAPSHOT")
    implementation("com.rabbitmq:amqp-client:5.16.0")

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
    to {
        image = "winteryfox/mizore-gateway:$version"
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
