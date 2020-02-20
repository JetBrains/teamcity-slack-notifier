plugins {
    kotlin("jvm") version "1.3.61"
    id("org.openapi.generator") version ("4.2.3")
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project("slack_api_java"))
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$rootDir/openapi_spec/slack_web_openapi_v2.json")
    outputDir.set("$rootDir/slack_api_java")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}