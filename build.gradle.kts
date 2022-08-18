import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "de.neo"
version = "1.0-SNAPSHOT"

application.mainClass.set("de.neo.profi5e.Profi5EDecompilerKt")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20220320")
    implementation("commons-cli:commons-cli:1.5.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}