import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar

plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.141"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()
base { archivesName = "vectorial-neoforge" }

neoForge {
    version = providers.gradleProperty("neoforge_version").get()
}

java.toolchain.languageVersion = JavaLanguageVersion.of(26)

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.javassist:javassist:3.30.2-GA")
    implementation("net.bytebuddy:byte-buddy-agent:1.15.11")
}

sourceSets {
    main {
        java {
            srcDir("../common/src/main/java")
            srcDir("../common/src/generated/java")
        }
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val version = version; inputs.property("version", version)
    filesMatching("META-INF/neoforge.mods.toml") { expand("version" to version) }
    from("../fabric/src/main/resources") { exclude("fabric.mod.json"); into("") }
}

tasks.withType<JavaCompile>().configureEach { options.release = 26 }
java { withSourcesJar(); sourceCompatibility = JavaVersion.VERSION_26; targetCompatibility = JavaVersion.VERSION_26 }
tasks.named("compileJava") { dependsOn(":common:generateFields") }
tasks.named<Jar>("sourcesJar") {
    dependsOn(":common:generateFields")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.jar {
    val projectName = project.name; inputs.property("projectName", projectName)
    from("LICENSE") { rename { "${it}_$projectName" } }

    from(configurations.runtimeClasspath.get().filter {
        it.name.contains("javassist") || it.name.contains("byte-buddy-agent")
    }.map { zipTree(it) })

    manifest {
        attributes(
            "Agent-Class" to "com.github.uright008.vec.core.VectorialAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
}
