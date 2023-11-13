plugins {
    kotlin("jvm")
}

val teamcityVersion = rootProject.extra["teamcityVersion"] as String
val canDownloadSpacePackages = rootProject.extra["canDownloadSpacePackages"] as Boolean

dependencies {
    implementation(project(rootProject.path))
    implementation(kotlin("stdlib"))

    testImplementation("org.jetbrains.teamcity.internal:integration-test:$teamcityVersion")
    testImplementation("org.jetbrains.teamcity:tests-support:$teamcityVersion")

    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.2")
    testImplementation("com.github.salomonbrys.kotson:kotson:2.5.0")

    testImplementation("org.jetbrains.teamcity:server-api:$teamcityVersion")
    testImplementation("org.jetbrains.teamcity:oauth:$teamcityVersion")

    testImplementation("org.testng:testng:6.8")
    testImplementation("junit:junit:3.8.1")
    testImplementation("io.mockk:mockk:1.10.0")
}

tasks.named<Test>("test") {
    useTestNG {
        suites("src/test/testng-slack-notifier.xml")
    }
}

if (!canDownloadSpacePackages) {
    tasks.findByName("compileTestKotlin")?.enabled = false
}