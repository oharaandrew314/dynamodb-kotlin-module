plugins {
    kotlin("jvm") version "1.9.10"
    id("jacoco")
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    api("software.amazon.awssdk:dynamodb-enhanced:2.20.86+")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
    testImplementation("org.http4k:http4k-aws:5.8.0.0")
    testImplementation("org.http4k:http4k-connect-amazon-dynamodb-fake:5.2.0.0")
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
