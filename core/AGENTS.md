# AGENTS.md — Parallel Core

Minecraft 26.1.2 server-side Fabric mod. Java 25, Gradle 9.4.1 + Fabric Loom 1.16-SNAPSHOT.

## Build

```bash
./gradlew build
```

The version (`0.1.2`) lives in `gradle.properties` as `mod_version` and is expanded into `fabric.mod.json` at build time. The JAR lands in `build/libs/`.

## No tests, no lint

There are no test suites, linters, or formatters configured. Do not suggest running `test`, `lint`, `typecheck`, or `format` commands — they don't exist.

## Architecture

**Package**: `com.github.uright008.pc`  
**Entrypoint**: `ParallelCore` (implements `ModInitializer`, registered in `fabric.mod.json`)  
**Environment**: `server` only — no client code, no client mixins

### Safe zone pattern (critical)

Vanilla `Level.getBlockEntity()` and other methods reject off-main-thread access. This mod works around that via a reference-counted safe zone:

```java
SafeLevelAccess.enterSafeZone();
try {
    // parallel work here
} finally {
    SafeLevelAccess.leaveSafeZone();
}
```

Always bracket parallel work with enter/leave. The safe zone is `AtomicInteger`-based and re-entrant. Mixins check `SafeLevelAccess.isInSafeZone()` to decide whether to use synchronized/redirected access to vanilla data structures.

### Thread pools

`ParallelThreadPool.getPool("name")` returns a per-subsystem daemon-backed `ThreadPoolExecutor`. Pool size = `max(2, availableProcessors - 2)`. Each subsystem (physics, explosions, hoppers) should use its own pool.

### Mixins

7 mixins in `com.github.uright008.pc.mixin`. All must be declared in `src/main/resources/core.mixins.json`. Notable constraints:
- `requireAnnotations: true` — only `@Override`-annotated methods are injected; accidental injections are caught
- `compatibilityLevel: JAVA_25`
- Mixins use the `parallelCore$` prefix for injected methods (e.g. `parallelCore$getChunkSafe`)

### Extending the `/parallel` command

External mods add subcommands by calling `ParallelCommand.registerSubCommand()` with a `ParallelSubCommand` implementation. Requires `LEVEL_GAMEMASTERS` (op level 2). The command registers both `/parallel` and `/pc`.

### Configuration

`ParallelConfig` abstract base writes to `config/mc-parallel.json`. Subclasses define a section key and implement `read()`/`write()`/`applyDefaults()`. Uses double-checked locking for lazy init.

## Version lifecycle

Minecraft, loader, and Fabric API versions are pinned in `gradle.properties`. When upgrading Minecraft, update all three: `minecraft_version`, `loader_version` (check Fabric site), and `fabric_api_version`.

## Publishing

CI publishes to GitHub Packages (Maven) and Modrinth:
- GitHub Packages: needs `GITHUB_ACTOR` + `GITHUB_TOKEN` in env (CI provides these)
- Modrinth: via `cloudnode-pro/modrinth-publish@v2` action, needs `MODRINTH_TOKEN` secret
- Commits use conventional commit prefixes (`feat:`, `fix:`, etc.) for auto-generated changelogs
