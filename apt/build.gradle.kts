plugins {
    java
}

repositories {
    mavenLocal()
    mavenCentral()
}


dependencies {
    implementation("com.squareup:javapoet:1.13.0")
}


group = "com.shenluw.tools"
version = "3.2.3"
description = "xk-time apt"
java.sourceCompatibility = JavaVersion.VERSION_1_8
