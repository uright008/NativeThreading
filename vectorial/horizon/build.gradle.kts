plugins {
    java
    id("com.gradleup.shadow") version "9.4.2"
    alias(libs.plugins.weaverUserdev)
    alias(libs.plugins.horizon)
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
    maven("https://maven.canvasmc.io/releases") { name = "canvasmc" }
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    mavenCentral()
}

dependencies {
    horizon.horizonApi(libs.versions.horizonApi.get())
    paperweight.paperDevBundle(libs.versions.paperApi.get())
    implementation("org.javassist:javassist:3.30.2-GA")
    implementation("net.bytebuddy:byte-buddy-agent:1.15.11")
}

sourceSets {
    main {
        java {
            srcDir("../common/src/main/java")
            srcDir("../common/src/generated/java")
            srcDir("../../core/common/src/main/java")
            exclude("**/ParallelCommandFabric.java")
        }
    }
}

tasks.processResources {
    from("../fabric/src/main/resources") {
        include("vectorial.mixins.json")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}
tasks.named("compileJava").configure { dependsOn(":common:generateFields") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    manifest {
        attributes(
            "Premain-Class" to "com.github.uright008.vec.core.VectorialAgent",
            "Agent-Class" to "com.github.uright008.vec.core.VectorialAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
}
tasks.jar { enabled = false }
tasks.assemble { dependsOn(tasks.named("shadowJar")) }
