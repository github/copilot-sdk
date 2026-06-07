plugins {
    kotlin("jvm") version "2.3.10"
    application
}

dependencies {
    implementation("ai.koog:koog-agents:1.0.0")
    implementation("com.github:copilot-sdk-java:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.21")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.github.copilot.samples.koog.MainKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
