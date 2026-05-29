# AGENTS.md — Explosion Parallelization

Minecraft 26.1.2 server-side Fabric + NeoForge mod. Java 26, Gradle + Fabric Loom 1.16-SNAPSHOT.

## Build

```bash
./gradlew build
```

The version (`1.0.0`) lives in `gradle.properties` as `mod_version`. The JAR lands in `build/libs/`.

## No tests, no lint

There are no test suites, linters, or formatters configured. Do not suggest running `test`, `lint`, `typecheck`, or `format` commands.

## Architecture

**Package**: `com.github.uright008.ep`
**Entrypoint**: `ExplosionParallelizationFabric` (implements `ModInitializer`, registered in `fabric.mod.json`)
**Environment**: `server` only

### Configuration

`ExplosionParallelConfig` extends `parallel-core`'s `ParallelConfig`. Writes to `config/mc-parallel.json` under `"explosion"` key.

### Mixins

All mixins declared in `explosion.mixins.json`:
- `compatibilityLevel: JAVA_25`
