plugins {
    java
    alias(libs.plugins.weaverUserdev)
    alias(libs.plugins.horizon)
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
    maven("https://maven.canvasmc.io/releases") { name = "canvasmc" }
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    mavenLocal()
    maven("https://jitpack.io") { name = "JitPack" }
}

dependencies {
    horizon.horizonApi(libs.versions.horizonApi.get())
    paperweight.paperDevBundle(libs.versions.paperApi.get())
    implementation("com.github.uright008:vectorial:${providers.gradleProperty("vectorial_version").get()}")
}

sourceSets {
    main {
        java {
            srcDir("../common/src/main/java")
        }
    }
}

tasks.processResources {
    from("../fabric/src/main/resources") {
        include("core.mixins.json")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.compilerArgs.add("--add-modules=jdk.incubator.vector")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
