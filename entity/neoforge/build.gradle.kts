import org.gradle.api.file.DuplicatesStrategy

plugins {
    id("java")
    id("net.neoforged.gradle.userdev") version "7.1.27"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()
base { archivesName = "entity-neoforge" }

java.toolchain.languageVersion = JavaLanguageVersion.of(26)

repositories {
    mavenLocal()
    maven { name = "NeoForge"; url = uri("https://maven.neoforged.net/releases") }
    maven { name = "Fabric"; url = uri("https://maven.fabricmc.net/") }
    maven { name = "JitPack"; url = uri("https://jitpack.io") }
}

dependencies {
    implementation("net.neoforged:neoforge:${providers.gradleProperty("neoforge_version").get()}")
    implementation("com.github.uright008:parallel-core:${providers.gradleProperty("parallel_core_version").get()}")
}

sourceSets {
    main {
        java { srcDir("../common/src/main/java") }
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val version = version
    inputs.property("version", version)
    filesMatching("META-INF/neoforge.mods.toml") { expand("version" to version) }
    from("../fabric/src/main/resources") {
        exclude("fabric.mod.json")
        into("")
    }
}

tasks.withType<JavaCompile>().configureEach { options.release = 26 }
java { withSourcesJar(); sourceCompatibility = JavaVersion.VERSION_26; targetCompatibility = JavaVersion.VERSION_26 }
tasks.named<Jar>("sourcesJar") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)
    from("LICENSE") { rename { "${it}_$projectName" } }
}
