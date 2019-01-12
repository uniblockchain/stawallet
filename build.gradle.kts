import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kethereumVersion=0.67

plugins {
    kotlin("jvm") version "1.3.11"
}

group = "stacrypt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.0")
    implementation("org.jetbrains.xodus:xodus-environment:1.2.3")
    implementation("org.jetbrains.xodus:xodus-entity-store:1.2.3")

    compile(kotlin("stdlib-jdk8"))
    compile("com.typesafe:config:1.3.2")
    compile("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.3")
    compile("redis.clients:jedis:3.0.1")

    compile("com.github.walleth.kethereum:bip32:$kethereumVersion")
    compile("com.github.walleth.kethereum:functions:$kethereumVersion")
    compile("com.github.walleth.kethereum:bip44:$kethereumVersion")

//    compile("org.slf4j:slf4j:1.7.5")
//    compile("org.slf4j:slf4j-core:1.7.5")
//    compile("org.slf4j:slf4j-simple:1.7.5")
//    compile("org.slf4j:slf4j-api:1.7.5")
//    compile("org.apache.logging.log4j:log4j:2.11.1")
//    compile("org.apache.logging.log4j:log4j-core:2.11.1")
//    compile("org.apache.logging.log4j:log4j-api:2.11.1")
//    compile("org.apache.logging.log4j:log4j-simple:2.11.1")
//    compile("org.apache.logging.log4j:log4j-1.2-api:2.11.1")
//    compile("org.apache.logging.log4j:log4j-jcl:2.11.1")

//    compile("org.jetbrains.xodus:xodus-openAPI:1.2.3")

//    compile("org.ethereum:ethereumj-core:1.9.1-RELEASE")
    compile("org.web3j:core:4.0.0")

    compile("org.bitcoinj:bitcoinj-core:0.14.7")
    compile("io.github.novacrypto:BIP32:2018.10.06")
    compile("io.github.novacrypto:BIP32derivation:2018.10.06")

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}