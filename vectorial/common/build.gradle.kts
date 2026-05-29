plugins {
    id("net.fabricmc.fabric-loom")
}
version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()
repositories {
    mavenLocal()
    maven { name = "JitPack"; url = uri("https://jitpack.io") }
    mavenCentral()
}
dependencies {
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    compileOnly("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    compileOnly("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    api("org.javassist:javassist:3.30.2-GA")
    api("net.bytebuddy:byte-buddy-agent:1.15.11")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.0")
}

val generateFields by tasks.registering {
    val outputFile = file("src/generated/java/com/github/uright008/vec/core/GeneratedFields.java")
    outputs.file(outputFile)
    doLast {
        val mcJar = configurations.compileClasspath.get().files
            .find { it.name.contains("minecraft-merged") }
            ?: error("Minecraft merged jar not found")
        val proc = ProcessBuilder("javap", "-p", "-cp", mcJar.absolutePath, "net.minecraft.world.entity.Entity")
            .redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        val fields = mutableListOf<Triple<String, String, String>>()
        val r = Regex("""^\s+(public|private|protected)\s+(double|float|int|boolean|net\.minecraft\.world\.phys\.Vec3|net\.minecraft\.world\.phys\.AABB|net\.minecraft\.core\.BlockPos)\s+(\w+)\s*;""", RegexOption.MULTILINE)
        for (m in r.findAll(out)) {
            val t = when {
                m.groupValues[2] == "double" -> "double"
                m.groupValues[2] == "float" -> "float"
                m.groupValues[2] == "int" -> "int"
                m.groupValues[2] == "boolean" -> "boolean"
                m.groupValues[2].contains("Vec3") -> "Vec3"
                m.groupValues[2].contains("AABB") -> "AABB"
                m.groupValues[2].contains("BlockPos") -> "BlockPos"
                else -> null
            }
            if (t != null) fields.add(Triple(m.groupValues[3], t, m.groupValues[1]))
        }
        val sb = StringBuilder()
        sb.append("package com.github.uright008.vec.core;\n\n")
        sb.append("// AUTO-GENERATED from Entity.class via javap — do not edit\n")
        sb.append("public final class GeneratedFields {\n")
        sb.append("    public record Spec(String name, String type, String access) {\n")
        sb.append("        public boolean isDouble() { return type.equals(\"double\") || type.equals(\"Vec3\"); }\n")
        sb.append("        public boolean isFloat() { return type.equals(\"float\"); }\n")
        sb.append("        public boolean isInt() { return type.equals(\"int\"); }\n")
        sb.append("        public boolean isBoolean() { return type.equals(\"boolean\"); }\n")
        sb.append("        public boolean isVec3() { return type.equals(\"Vec3\"); }\n")
        sb.append("        public boolean isAABB() { return type.equals(\"AABB\"); }\n")
        sb.append("    }\n")
        sb.append("    public static final Spec[] ALL = {\n")
        for ((i, f) in fields.withIndex()) {
            val comma = if (i < fields.size - 1) "," else " "
            sb.append("        new Spec(\"${f.first}\", \"${f.second}\", \"${f.third}\")$comma\n")
        }
        sb.append("    };\n\n")

        // ── Field ordinals (array index) ──
        // Convert camelCase to UPPER_SNAKE_CASE
        fun String.toSnake() = replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

        var idx = 0
        sb.append("    // ── Field ordinals (array index) ──\n")
        for (f in fields) {
            val n = f.first.toSnake()
            when (f.second) {
                "double", "float", "int", "boolean" -> {
                    sb.append("    public static final int ${n} = ${idx};\n")
                    idx++
                }
                "Vec3" -> {
                    sb.append("    public static final int ${n}_X = ${idx};\n")
                    sb.append("    public static final int ${n}_Y = ${idx+1};\n")
                    sb.append("    public static final int ${n}_Z = ${idx+2};\n")
                    idx += 3
                }
                "AABB" -> {
                    sb.append("    public static final int ${n}_MIN_X = ${idx};\n")
                    sb.append("    public static final int ${n}_MIN_Y = ${idx+1};\n")
                    sb.append("    public static final int ${n}_MIN_Z = ${idx+2};\n")
                    sb.append("    public static final int ${n}_MAX_X = ${idx+3};\n")
                    sb.append("    public static final int ${n}_MAX_Y = ${idx+4};\n")
                    sb.append("    public static final int ${n}_MAX_Z = ${idx+5};\n")
                    idx += 6
                }
            }
        }
        sb.append("    public static final int COUNT = ${idx};\n")
        sb.append("}\n")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(sb.toString())
        logger.lifecycle("GeneratedFields: ${fields.size} fields from Entity.class via javap")
    }
}

sourceSets { main { java { srcDirs("src/main/java", "src/generated/java") } } }
tasks.named("compileJava") { dependsOn(generateFields) }
afterEvaluate { tasks.matching { it.name == "sourcesJar" }.configureEach { dependsOn(generateFields) } }
tasks.withType<JavaCompile>().configureEach { options.release = 25 }
java { withSourcesJar(); sourceCompatibility = JavaVersion.VERSION_25; targetCompatibility = JavaVersion.VERSION_25 }
sourceSets { test { java { srcDirs("src/test/java") } } }
tasks.test { useJUnitPlatform() }
tasks.named<Jar>("sourcesJar") { duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE }
