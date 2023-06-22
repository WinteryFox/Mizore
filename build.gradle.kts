val project_version: String by project

plugins {
    id("org.sonarqube")
}

subprojects {
    version = project_version
    group = "bot.mizore"

    apply(plugin = "com.google.cloud.tools.jib")
}

sonarqube {
    properties {
        property("sonar.projectKey", "WinteryFox_Mizore")
        property("sonar.organization", "winteryfox")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
