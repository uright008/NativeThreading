# AGENTS.md — Vectorial

Minecraft 26.1.2 server-side Fabric mod. Java 25, Gradle + Fabric Loom 1.16-SNAPSHOT.

## Build

```bash
./gradlew build
```

## Architecture

**Package**: `com.github.uright008.vec`
**Entrypoint**: `Vectorial`
**Environment**: `server` only

### Concept

Transforms entity field access from AoS (Array of Structures) to SoA
(Structure of Arrays) via bytecode injection:
- Double fields (posX, bb min/max) → contiguous double[]
- Enables SIMD batch entity queries
- Optional dependency for other mods

### Mixins

All mixins must be declared in `vectorial.mixins.json`:
- `requireAnnotations: true`
- `compatibilityLevel: JAVA_25`
