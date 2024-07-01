import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
    id("java-library")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "me.nealsanche"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.bitwig.com")
    }
}

dependencies {
    implementation("com.bitwig:extension-api:18") // provided
    implementation("org.apache.commons:commons-lang3:3.12.0")

    implementation("org.slf4j:slf4j-api:1.7.36")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    // development build
    if (project.hasProperty("dev")) {
        implementation("ch.qos.logback:logback-core:1.2.3")
        implementation("ch.qos.logback:logback-classic:1.2.3")
    }
}
defaultTasks("shadowJar")

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveFileName.set("OxiOne.bwextension")
    dependencies {
        // exclude provided dependencies
        exclude(dependency("com.bitwig:extension-api:18"))
        exclude(dependency("org.apache.commons:commons-lang3:3.5"))
        if(!project.hasProperty("dev")) {
            exclude("logback.xml")
        }
    }
}

tasks.register<Copy>("installBwExtension") {
    dependsOn("clean", "shadowJar")
    from("build/libs") {
        include("*.bwextension")
    }
    // TODO platform specific
    into("${System.getProperty("user.home")}/Documents/Bitwig Studio/Extensions")
}

// for debugging on console:
//   gradle -Pdev start
tasks.register<Exec>("start") {
    dependsOn("installBwExtension")
    if (project.hasProperty("dev")) {
        environment("BITWIG_DEBUG_PORT", 8989)
    }
    // TODO platform specific
    commandLine("/Applications/Bitwig Studio.app/Contents/MacOS/BitwigStudio")
}

