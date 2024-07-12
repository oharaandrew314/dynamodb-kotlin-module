import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    kotlin("jvm") version "2.0.0"
    id("jacoco")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    api("software.amazon.awssdk:dynamodb-enhanced:2.21.26+")

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

mavenPublishing {
    configure(KotlinJvm(JavadocJar.None(), true))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    coordinates("dev.andrewohara", "dynamokt", "1.0.0")

    pom {
        name.set("DynamoDb Kotlin Module")
        description.set("Kotlin Module for the dynamodb-enhanced SDk")
        inceptionYear.set("2021")
        url.set("https://github.com/oharaandrew314/dynamodb-kotlin-module")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("oharaandrew314")
                name.set("Andrew O'Hara")
                url.set("https://github.com/oharaandrew314")
            }
        }
        scm {
            url.set("https://github.com/oharaandrew314/dynamodb-kotlin-module")
        }
    }
}
