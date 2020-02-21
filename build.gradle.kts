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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.3")
    implementation("com.squareup.retrofit2:retrofit:2.7.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.2")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}