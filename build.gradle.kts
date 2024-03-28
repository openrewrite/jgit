plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.tools"
description = "Fork of jgit to maintain Java 8 compatibility"

dependencies {
    implementation("com.googlecode.javaewah:JavaEWAH:1.1.13")
    compileOnly("org.slf4j:slf4j-api:1.7.30")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.release.set(null as? Int?) // remove `--release 8` set in `org.openrewrite.java-base`
}

tasks.named("javadoc") {
    enabled = false
}
tasks.named("licenseMain") {
    enabled = false
}
