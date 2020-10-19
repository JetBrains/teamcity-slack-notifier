plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.rodm.teamcity-server") version "1.3.1"
    id("com.github.rodm.teamcity-environments") version "1.3.1"
}

group = "org.jetbrains.teamcity"
val pluginVersion = "${if (project.hasProperty("PluginVersion")) project.property("PluginVersion") else "SNAPSHOT"}"
version = pluginVersion

val teamcityVersion = "2020.2-SNAPSHOT"

extra["teamcityVersion"] = teamcityVersion

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.2")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")

    provided("org.jetbrains.teamcity:server-api:${teamcityVersion}")
    provided("org.jetbrains.teamcity:oauth:${teamcityVersion}")
    provided("org.jetbrains.teamcity:server-web-api:${teamcityVersion}")
    provided("org.jetbrains.teamcity.internal:server:${teamcityVersion}")
    provided("org.jetbrains.teamcity.internal:web:${teamcityVersion}")

    testImplementation("org.assertj:assertj-core:1.7.1")
    testImplementation("org.testng:testng:6.8")
    testImplementation("io.mockk:mockk:1.10.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

teamcity {
    version = "2020.1"

    server {
        archiveName = "slack.zip"
        descriptor = file("teamcity-plugin.xml")
        tokens = mapOf("Version" to pluginVersion)

        files {
            into("kotlin-dsl") {
                from("src/kotlin-dsl")
            }
        }
    }
}
