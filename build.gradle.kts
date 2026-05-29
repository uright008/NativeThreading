import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.fabricmc.fabric-loom") apply false
    base
    `maven-publish`
}

val modVersion = providers.gradleProperty("mod_version").get()
val modName = providers.gradleProperty("archives_base_name").get()

tasks.register<Jar>("releaseJar") {
    dependsOn(":fabric:jar")
    archiveBaseName = modName
    archiveVersion = modVersion
    destinationDirectory = layout.buildDirectory.dir("libs")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(zipTree(project(":fabric").layout.buildDirectory.file("libs/${modName}-${modVersion}.jar")))

    manifest {
        attributes(
            "Agent-Class" to "com.github.uright008.vec.core.VectorialAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
}

tasks.named("assemble") {
    dependsOn("releaseJar")
}

publishing {
    repositories {
        mavenLocal()
    }
}
