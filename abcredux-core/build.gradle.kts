import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
}

apply {
    plugin("kotlin")
}

group = "hu.nemi.abcredux"
version = "0.1-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    implementation(Dependencies.kotlinStdLib)

    testImplementation(Dependencies.kotlinStdLib)
    testImplementation(TestDependencies.junit)
    testImplementation(TestDependencies.assertj)
    testImplementation(TestDependencies.mockitoCore)
    testImplementation(TestDependencies.mockitoKotlin)
}
