plugins {
    kotlin("jvm") version "1.4.0"
    id("com.github.rodm.teamcity-server") version "1.2"
    id("com.github.rodm.teamcity-environments") version "1.2"
}

group = "org.jetbrains.teamcity"
val pluginVersion = "${if (project.hasProperty("PluginVersion")) project.property("PluginVersion") else "SNAPSHOT"}"
version = pluginVersion

val teamcityVersion = "2020.2-SNAPSHOT"

extra["teamcityVersion"] = teamcityVersion

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://download.jetbrains.com/teamcity-repository")
    }
}

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

java {
    sourceSets.getByName("main").java.srcDir("src/main/kotlin")
    sourceSets.getByName("test").java.srcDir("src/test/kotlin")
}

teamcity {
    version = "2020.1"

    server {
        descriptor = file("teamcity-plugin.xml")
        tokens = mapOf("Version" to pluginVersion)

        files {
            into("kotlin-dsl") {
                from("src/kotlin-dsl")
            }
        }
    }
}

fun Project.teamcity(configuration: com.github.rodm.teamcity.TeamCityPluginExtension.() -> Unit) {
    configure(configuration)
}

tasks.register<Copy>("renameDist") {
    from("build/distributions/teamcity-slack-plugin-${pluginVersion}.zip")
    into("build/distributions")
    rename("teamcity-slack-plugin-${pluginVersion}.zip", "slack.zip")
}

