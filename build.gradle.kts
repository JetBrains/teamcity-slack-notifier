import com.github.jk1.license.filter.*
import com.github.jk1.license.render.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.rodm.teamcity-server") version "1.5.6"
    id("com.github.rodm.teamcity-environments") version "1.5.6"
    id ("com.github.jk1.dependency-license-report") version "2.9"
}

initializeWorkspace()

group = "org.jetbrains.teamcity"
val pluginVersion = anyParam("PluginVersion") ?: "999999-snapshot-${Date().time}"
version = pluginVersion

val teamcityVersion = anyParam("teamcityVersion") ?: "2023.11-SNAPSHOT"

val spacePackagesToken = anyParam("spacePackagesToken")
val spacePackagesUsername = anyParam("spacePackagesUsername")
val spacePackagesPassword = anyParam("spacePackagesPassword")

val canDownloadSpacePackages = spacePackagesToken != null ||
        (spacePackagesUsername != null && spacePackagesPassword != null)
val localRepo = anyParamPath("TC_LOCAL_REPO")

if (!canDownloadSpacePackages) {
    println("Not running integration tests, can't authorize to Space")
}


extra["teamcityVersion"] = teamcityVersion
extra["downloadsDir"] = anyParam("downloads.dir") ?: "${rootDir}/downloads"
extra["canDownloadSpacePackages"] = canDownloadSpacePackages

allprojects {
    repositories {
        if (localRepo != null) {
            maven(url = "file:///${localRepo}")
        }
        mavenLocal()
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

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
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
    provided("com.ibm.icu:icu4j:4.8.1.1")

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
    excludes = arrayOf("org.jetbrains.*", "com.jetbrains.*", ".*jackson-bom*")
    filters = arrayOf<DependencyFilter>(
        LicenseBundleNormalizer("${project.rootDir}/license-third-party-normalizer.json", false)
    )
}

tasks.serverPlugin {
    finalizedBy(project.tasks.getByName("generateLicenseReport"))
}


fun anyParamPath(vararg names: String): Path? {
    val param = anyParam(*names)
    if (param == null || param.isEmpty())
        return null
    return if (Paths.get(param).isAbsolute()) {
        Paths.get(param)
    } else {
        getRootDir().toPath().resolve(param)
    }
}

fun anyParam(vararg names: String): String? {
    var param: String? = ""
    try {
        for(name in names) {
            param = if (project.hasProperty(name)) {
                project.property(name).toString()
            } else {
                System.getProperty(name) ?: System.getenv(name) ?: null
            }
            if (param != null)
                break;
        }
        if (param == null || param.isEmpty())
            param = null
    } finally {
        println("AnyParam: ${names.joinToString(separator = ",")} -> $param")
    }
    return param
}


fun initializeWorkspace() {
    if (System.getProperty("idea.active") != null) {
        println("Attempt to configure workspace in IDEA")
        val coreVersionProperties = project.projectDir.toPath().parent.parent.resolve(".version.properties")
        if (coreVersionProperties.toFile().exists()) {
            val p = Properties().also {
                it.load(FileInputStream(coreVersionProperties.toFile()))
            }
            p.forEach {(k,v) ->
                System.setProperty(k.toString(), v.toString());
            }
        }
    }
}