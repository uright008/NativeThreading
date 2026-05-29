plugins {
    id("net.fabricmc.fabric-loom")
}

dependencies {
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    compileOnly("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    compileOnly("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    implementation(project(":core"))
}

sourceSets {
    main {
        java {
            srcDir("common/src/main/java")
        }
    }
}

tasks.withType<JavaCompile>().configureEach { options.release = 25 }
java { withSourcesJar(); sourceCompatibility = JavaVersion.VERSION_25; targetCompatibility = JavaVersion.VERSION_25 }
