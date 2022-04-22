plugins {
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("junit:junit:4.13.1")
    testImplementation(kotlin("test"))

    testImplementation("org.slf4j:slf4j-simple:1.8.0-beta4")
    implementation("org.slf4j:slf4j-simple:1.8.0-beta4")
    implementation("io.github.microutils:kotlin-logging:1.12.5")
}