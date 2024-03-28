plugins {
    id("org.openrewrite.build.language-library")
}

val bcVersion = "1.70"
dependencies {
    implementation(project(":jgit"))
    compileOnly("org.slf4j:slf4j-api:1.7.36")
    implementation("org.bouncycastle:bcpg-jdk15on:$bcVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:$bcVersion")
    implementation("org.bouncycastle:bcutil-jdk15on:$bcVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bcVersion")
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
