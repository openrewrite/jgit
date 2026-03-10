plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    implementation("com.googlecode.javaewah:JavaEWAH:1.1.13")
    compileOnly("org.slf4j:slf4j-api:1.7.36")
    testImplementation("org.assertj:assertj-core:3.26.3") {
        version { strictly("3.26.3") }
    }
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.36")
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
