# AGENTS.md — Redstone Parallelization

Minecraft 26.1.2 server-side Fabric mod. Java 25, Gradle + Fabric Loom 1.16-SNAPSHOT.

## Build

```bash
./gradlew build
```

The version (`0.1.0`) lives in `gradle.properties` as `mod_version` and is expanded into `fabric.mod.json` at build time. The JAR lands in `build/libs/`.

## No tests, no lint

There are no test suites, linters, or formatters configured. Do not suggest running `test`, `lint`, `typecheck`, or `format` commands — they don't exist.

## Architecture

**Package**: `com.github.uright008.rp`
**Entrypoint**: `RedstoneParallelization` (implements `ModInitializer`, registered in `fabric.mod.json`)
**Environment**: `server` only — no client code, no client mixins

### Configuration

`RedstoneParallelConfig` extends `parallel-core`'s `ParallelConfig` abstract base. Writes to `config/mc-parallel.json` under the `"redstone"` section key. Uses double-checked locking for lazy init.

### Extending the `/parallel` command

Registers via `ParallelCommand.registerSubCommand()` with a `RedstoneParallelCommand` implementing `ParallelSubCommand`. Requires `LEVEL_GAMEMASTERS` (op level 2).

### Mixins

All mixins must be declared in `src/main/resources/redstone.mixins.json`:
- `requireAnnotations: true` — only `@Override`-annotated methods are injected
- `compatibilityLevel: JAVA_25`

## Version lifecycle

Minecraft, loader, and Fabric API versions are pinned in `gradle.properties`. When upgrading Minecraft, update all three: `minecraft_version`, `loader_version`, and `fabric_api_version`.
