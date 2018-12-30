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
    compile("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.3")
    compile("org.web3j:core:4.0.0")
    compile("redis.clients:jedis:3.0.1")
    compile("org.slf4j:slf4j:1.7.5")
    compile("org.slf4j:slf4j-core:1.7.5")
    compile("org.slf4j:slf4j-simple:1.7.5")
    compile("org.slf4j:slf4j-api:1.7.5")
    compile("org.apache.logging.log4j:log4j:2.11.1")
    compile("org.apache.logging.log4j:log4j-core:2.11.1")
    compile("org.apache.logging.log4j:log4j-api:2.11.1")
    compile("org.apache.logging.log4j:log4j-simple:2.11.1")
    compile("org.apache.logging.log4j:log4j-1.2-api:2.11.1")
    compile("org.apache.logging.log4j:log4j-jcl:2.11.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}