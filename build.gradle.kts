import com.github.jk1.license.render.*
import java.nio.file.Paths
import java.util.*

plugins {
    kotlin("jvm") version "1.5.20"
    id("com.github.rodm.teamcity-server") version "1.5.2"
    id("com.github.rodm.teamcity-environments") version "1.5.2"
    id ("com.github.jk1.dependency-license-report") version "2.5"
}

group = "org.jetbrains.teamcity"
val pluginVersion = project.findProperty("PluginVersion") ?: "999999-snapshot-${Date().time}"
version = pluginVersion

val teamcityVersion by extra { findProperty("teamcityVersion") ?: "2023.11-SNAPSHOT" }

val spacePackagesToken = rootProject.findProperty("spacePackagesToken") as String?
val spacePackagesUsername = rootProject.findProperty("spacePackagesUsername") as String?
val spacePackagesPassword = rootProject.findProperty("spacePackagesPassword") as String?

val canDownloadSpacePackages = spacePackagesToken != null ||
        (spacePackagesUsername != null && spacePackagesPassword != null)

if (!canDownloadSpacePackages) {
    println("Not running integration tests, can't authorize to Space")
}


extra["teamcityVersion"] = teamcityVersion
extra["downloadsDir"] = project.findProperty("downloads.dir") ?: "${rootDir}/downloads"
extra["canDownloadSpacePackages"] = canDownloadSpacePackages

allprojects {
    repositories {
        mavenLocal()
        findProperty("TC_LOCAL_REPO")?.toString()?.let {
            maven {
                url = Paths.get(it).toUri()
            }
        }
        maven(url = "https://cache-redirector.jetbrains.com/maven-central")
        maven(url = "https://download.jetbrains.com/teamcity-repository")
        maven(url = "https://repo.labs.intellij.net/teamcity")
        mavenCentral()

        if (canDownloadSpacePackages) {
            maven(url = "https://packages.jetbrains.team/maven/p/tc/maven") {
                if (spacePackagesToken != null) {
                    credentials(HttpHeaderCredentials::class) {
                        name = "Authorization"
                        value = "Bearer $spacePackagesToken"
                    }
                    authentication {
                        create<HttpHeaderAuthentication>("header")
                    }
                } else {
                    credentials {
                        username = spacePackagesUsername
                        password = spacePackagesPassword
                    }
                }
            }
        }
    }
}


dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.3") {
        exclude("com.fasterxml.jackson.core", "jackson-annotations")
        exclude("com.fasterxml.jackson.core", "jackson-databind")
    }
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.2")

    provided("org.jetbrains.teamcity:server-api:${teamcityVersion}")
    provided("org.jetbrains.teamcity:oauth:${teamcityVersion}")
    provided("org.jetbrains.teamcity:web-openapi:${teamcityVersion}")
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
    version = "2022.10"

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

licenseReport {
    renderers = arrayOf(JsonReportRenderer("third-party-libraries.json"))
}

tasks.serverPlugin {
    finalizedBy(project.tasks.getByName("generateLicenseReport"))
}



