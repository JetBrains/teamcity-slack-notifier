plugins {
    kotlin("jvm") version "1.3.61"
    id("com.github.rodm.teamcity-server") version "1.2"
    id("com.github.rodm.teamcity-environments") version "1.2"
}

group = "org.jetbrains.teamcity"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://download.jetbrains.com/teamcity-repository")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.3")
    implementation("com.squareup.retrofit2:retrofit:2.7.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.2")

    provided("org.jetbrains.teamcity.internal:plugins:2020.1-SNAPSHOT")
    provided("org.jetbrains.teamcity.internal:server:2020.1-SNAPSHOT")
    provided("org.jetbrains.teamcity.internal:web:2020.1-SNAPSHOT")
    provided("org.jetbrains.teamcity:oauth:2020.1-SNAPSHOT")
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
    version = "2019.2"

    server {
        descriptor {
            // required properties
            name = "slackNotifier"
            displayName = "Slack Notifier"
            version = project.version as String?
            vendorName = "JetBrains"

            // optional properties
            description = "Official plugin to send notifications to Slack"
            downloadUrl = "download url"
            email = ""
            vendorUrl = "vendor url"
            vendorLogo = "vendor logo"

            useSeparateClassloader = true
            allowRuntimeReload = true
        }
    }
}

fun Project.teamcity(configuration: com.github.rodm.teamcity.TeamCityPluginExtension.() -> Unit) {
    configure(configuration)
}