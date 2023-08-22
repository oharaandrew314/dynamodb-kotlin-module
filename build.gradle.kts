plugins {
    kotlin("jvm") version "1.9.0"
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

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
    testImplementation("org.http4k:http4k-aws:5.7.2.0")
    testImplementation("org.http4k:http4k-connect-amazon-dynamodb-fake:5.1.5.0")

    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.0"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
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
