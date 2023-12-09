plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.filamagenta.bot"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.telegram)

    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
