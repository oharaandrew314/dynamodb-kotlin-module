plugins {
    kotlin("jvm") version "1.5.30"
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

    implementation(platform("software.amazon.awssdk:bom:2.17.34+"))
    api("software.amazon.awssdk:dynamodb-enhanced")

    testImplementation("com.github.oharaandrew314:mock-aws-java-sdk:1.0.0-alpha.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.6.3")
}

tasks.test {
    useJUnitPlatform()
}


tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/reports.xml"))
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