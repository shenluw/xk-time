plugins {
    java
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    annotationProcessor(project(":apt"))
}

group = "com.shenluw.tools"
version = "3.2.3.1"
description = "xk-time"
java.sourceCompatibility = JavaVersion.VERSION_1_8


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = groupId
            artifactId = project.name
            version = version
        }
    }
}