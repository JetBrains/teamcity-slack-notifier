plugins {
    kotlin("jvm")
}

val teamcityVersion = rootProject.extra["teamcityVersion"] as String
val spacePackagesToken = rootProject.findProperty("spacePackagesToken")

repositories {
    if (spacePackagesToken != null) {
        maven(url = "https://packages.jetbrains.team/maven/p/tc/maven") {
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer $spacePackagesToken"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

dependencies {
    implementation(project(rootProject.path))
    implementation(kotlin("stdlib"))

    if (spacePackagesToken != null) {
        testImplementation("org.jetbrains.teamcity.internal:integration-tests:2020.2-SNAPSHOT")
    }

    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.2")
    testImplementation("com.github.salomonbrys.kotson:kotson:2.5.0")

    testImplementation("org.jetbrains.teamcity:server-api:2020.2-SNAPSHOT")
    testImplementation("org.jetbrains.teamcity:oauth:2020.2-SNAPSHOT")

    testImplementation("org.testng:testng:6.8")
    testImplementation("junit:junit:3.8.1")
    testImplementation("io.mockk:mockk:1.10.0")
}

if (spacePackagesToken != null) {
    tasks.named<Test>("test") {
        useTestNG {
            suites("src/test/testng-slack-notifier.xml")
        }
    }
}

if (spacePackagesToken == null) {
    tasks.findByName("compileTestKotlin")?.enabled = false
}