import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.11"
}

group = "stacrypt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.0")
    compile(kotlin("stdlib-jdk8"))
    compile("com.typesafe:config:1.3.2")
    compile("wf.bitcoin:JavaBitcoindRpcClient:0.9.13")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}