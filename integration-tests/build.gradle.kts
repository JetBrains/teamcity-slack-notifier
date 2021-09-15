plugins {
    kotlin("jvm")
}

val teamcityVersion = rootProject.extra["teamcityVersion"] as String
val spacePackagesToken = rootProject.findProperty("spacePackagesToken") as String?
val spacePackagesUsername = rootProject.findProperty("spacePackagesUsername") as String?
val spacePackagesPassword = rootProject.findProperty("spacePackagesPassword") as String?

val canDownloadSpacePackages = spacePackagesToken != null ||
        (spacePackagesUsername != null && spacePackagesPassword != null)

if (!canDownloadSpacePackages) {
    println("Not running integration tests, can't authorize to Space")
}

repositories {
    mavenCentral()
    maven(url = "https://download.jetbrains.com/teamcity-repository")

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

dependencies {
    implementation(project(rootProject.path))
    implementation(kotlin("stdlib"))

    if (canDownloadSpacePackages) {
        testImplementation("org.jetbrains.teamcity.internal:integration-tests:$teamcityVersion")
    }

    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.2")
    testImplementation("com.github.salomonbrys.kotson:kotson:2.5.0")

    testImplementation("org.jetbrains.teamcity:server-api:$teamcityVersion")
    testImplementation("org.jetbrains.teamcity:oauth:$teamcityVersion")

    testImplementation("org.testng:testng:6.8")
    testImplementation("junit:junit:3.8.1")
    testImplementation("io.mockk:mockk:1.10.0")
}

if (canDownloadSpacePackages) {
    tasks.named<Test>("test") {
        useTestNG {
            suites("src/test/testng-slack-notifier.xml")
        }
    }
}

if (!canDownloadSpacePackages) {
    tasks.findByName("compileTestKotlin")?.enabled = false
}