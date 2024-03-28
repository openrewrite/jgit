pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "openrewrite-jgit"

include("jgit")
include("jgit-gpg-bc")
