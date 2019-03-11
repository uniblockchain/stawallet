import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kethereumVersion = 0.67


plugins {
    kotlin("jvm") version "1.3.11"
//    id ("com.palantir.docker") version "0.21.0"
//    id ("com.palantir.docker-compose") version "0.21.0"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

group = "stacrypt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://kotlin.bintray.com/ktor")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.0")
    implementation("org.jetbrains.xodus:xodus-environment:1.2.3")
    implementation("org.jetbrains.xodus:xodus-entity-store:1.2.3")

    compile("io.ktor:ktor-server-netty:1.0.0")
    compile("io.ktor:ktor-server-core:1.0.0")
    compile("io.ktor:ktor-auth:1.0.0")
    compile("io.ktor:ktor-auth-jwt:1.0.0")
    compile("io.ktor:ktor-jackson:1.0.0")
    compile("io.ktor:ktor-locations:1.0.0")
    compile("ch.qos.logback:logback-classic:1.2.3")


    compile(kotlin("stdlib-jdk8"))
//    compile("com.typesafe:config:1.3.2")
    compile("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.3")
    compile("redis.clients:jedis:3.0.1")

    compile("org.postgresql:postgresql:42.2.5")
    compile("org.jetbrains.exposed:exposed:0.12.1")

    compile("com.github.walleth.kethereum:bip32:$kethereumVersion")
    compile("com.github.walleth.kethereum:functions:$kethereumVersion")
    compile("com.github.walleth.kethereum:bip44:$kethereumVersion")
    compile("com.github.walleth.kethereum:crypto:$kethereumVersion")

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

//    compile("org.bitcoinj:bitcoinj-core:0.14.7")
    compile("io.github.novacrypto:BIP32:2018.10.06")
    compile("io.github.novacrypto:BIP32derivation:2018.10.06")

    compile("com.github.mahdi13:markdownk:1.4")

    testCompile("io.ktor:ktor-server-test-host:1.0.0")
    testCompile("com.opentable.components:otj-pg-embedded:0.13.0")

    testImplementation("io.mockk:mockk:1.9")

}

val mainClassName = "io.ktor.server.netty.EngineMain" // Starting with 1.0.0-beta-3


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


tasks.withType<ShadowJar> {
    baseName = "stawallet"
    classifier = ""
    version = ""
    manifest {
        attributes("Main-Class" to mainClassName)
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "4.10"
}
