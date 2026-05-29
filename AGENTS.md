# AGENTS.md

## Modules (required before build)
```bash
# Modules are now regular directories (not git submodules)
# No submodule init needed
```
Module source is aggregated into the root mod jar at build time. Modules are independent Gradle projects and are included in `fabric/build.gradle.kts` as project dependencies.

## Build
```bash
./gradlew build
```
Artifacts land in `build/libs/mc-parallel-{version}.jar`. Requires **JDK 26**.

CI overrides the mod version with the git short SHA:
```bash
./gradlew build -Pmod_version=$(git rev-parse --short HEAD)
```

## Architecture
- **Minecraft Fabric server mod** targeting `26.1.2` (1.21.5), Fabric Loader ≥0.19.2
- Root project: `mc-parallel` — aggregates 5 submodules into one jar
- Entry point: `mc.github.uright008.mp.McParallel` — calls `onInitialize()` on each submodule in this exact order:
  1. `ParallelCore`
  2. `Explosion`
  3. `Hopper`
  4. `Redstone`
  5. `Vectorial`
- Submodules: `core`, `explosion`, `hopper`, `redstone`, `vectorial`
- Mixin configs from submodules are merged during `processResources` (their `fabric.mod.json` files are excluded)
- `fabric/src/main/resources/fabric.mod.json` `provides` lists all 5 submodule mod IDs so they satisfy optional dependencies
- Requires **JDK 26** (not 25)

## Gradle quirks
- `org.gradle.configuration-cache=false` (disabled)
- `org.gradle.parallel=true` (enabled)
- No tests exist in any source set

## Key files
- `build.gradle.kts` — source set aggregation, mixin merge, dependency declarations
- `settings.gradle.kts` — Fabric Loom plugin declaration
- `gradle.properties` — Minecraft/Fabric/Loom version pins
- `fabric/src/main/resources/fabric.mod.json` — mod metadata, entrypoint, mixin references
- `.github/workflows/build.yml` — CI: checkout with `submodules: recursive`, JDK 26, `./gradlew build`
- `.opencode/package.json` — OpenCode plugin dependency config

## Commit Convention
This repository follows [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).
```
<type>[optional scope]: <description>
```
Common types:
- `feat` — new feature
- `fix` — bug fix
- `perf` — performance improvement
- `refactor` — code restructuring without behaviour change
- `docs` — documentation only
- `chore` — build, CI, tooling

Each submodule has its own independent git history.  Commits go into the submodule that
contains the changed source, not the root repository.
