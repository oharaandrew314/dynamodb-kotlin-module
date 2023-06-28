plugins {
    kotlin("jvm") version "1.8.22"
    id("jacoco")
    id("maven-publish")
}

repositories {
    mavenCentral()

    maven { url = uri("https://jitpack.io") }
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    api("software.amazon.awssdk:dynamodb-enhanced:2.20.86+")

    testImplementation("com.github.oharaandrew314:mock-aws-java-sdk:1.2.0")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
}

tasks.test {
    useJUnitPlatform()
}


tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
