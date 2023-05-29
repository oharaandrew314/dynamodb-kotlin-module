plugins {
    kotlin("jvm") version "1.8.20"
    id("jacoco")
    id("maven-publish")
}

repositories {
    mavenCentral()

    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    api("software.amazon.awssdk:dynamodb-enhanced:2.17.34+")

    testImplementation("com.github.oharaandrew314:mock-aws-java-sdk:1.2.0")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.6.2")
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