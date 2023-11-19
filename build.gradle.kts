plugins {
    kotlin("jvm") version "1.9.20"
    id("jacoco")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    api("software.amazon.awssdk:dynamodb-enhanced:2.20.86+")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
    testImplementation("org.http4k:http4k-connect-amazon-dynamodb-fake:5.5.0.1")
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
