plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.30"
    application
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

val exposedVersion = "0.31.1"
val coroutinesVersion = "1.5.1"
val ktorVersion = "1.6.3"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")

    // Server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-jetty:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    // Database
    implementation("org.postgresql:postgresql:42.2.23")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    // Crypto
    implementation("org.bouncycastle:bcprov-jdk15on:1.69")
    implementation("commons-codec:commons-codec:1.15")

    // Mail
    implementation("org.simplejavamail:simple-java-mail:6.6.1")
    implementation("org.freemarker:freemarker:2.3.31")

    // Utilities
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.0.5")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClass.set("io.ktor.server.jetty.EngineMain")
}
