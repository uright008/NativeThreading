pluginManagement {
    repositories {
        maven { name = "Fabric"; url = uri("https://maven.fabricmc.net/") }
        maven { name = "NeoForge"; url = uri("https://maven.neoforged.net/releases") }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version")
    }
}
rootProject.name = "entity"
include(":common", ":fabric", ":neoforge")
