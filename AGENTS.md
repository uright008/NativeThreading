# NativeThreading

Multi-module Minecraft 26.2 Fabric mod for parallelizing 5 subsystems via mixins.

## Build

```bash
./gradlew build -x test              # Full build (all 6 modules + fabric + neoforge)
./gradlew :core:compileJava          # Core only
./gradlew :explosion:compileJava     # Explosion module
```

Output: `build/libs/native-threading-*.jar`

## Project Structure

```
NativeThreading/
├── core/common/src/main/java/com/github/uright008/pc/
│   ├── ParallelWorker.java          # map, forEach, mapEach, mapBatched, forEachBatched, Batch<T,R>
│   ├── ParallelThreadPool.java      # Named pool factory (THREAD_POOL/FORK_JOIN/VIRTUAL)
│   ├── SafeLevelAccess.java         # ThreadLocal<int[]> safe-zone for worker world access
│   ├── SafeOps.java                 # Deferred writes + synchronized container ops
│   ├── ConcurrentWriteQueue.java    # Thread-local ArrayList drain queue
│   ├── ChunkGrid.java               # 2D ChunkAccess[][] cache + section cache + getBlockState()
│   ├── ThreadSafeRandomSource.java  # ThreadLocal LCG random
│   └── simd/SimdBatchOps.java       # SIMD AABB, distance, position ops
│   └── mixin/                       # Core mixins (7 files)
├── explosion/common/src/main/java/com/github/uright008/ep/
│   ├── ExplosionHelper.java         # Precomputed ray params, FULL_CUBE cache
│   └── mixin/ServerExplosionMixin.java # Parallel explosion ray tracing + entity damage
├── hopper/common/src/main/java/com/github/uright008/hp/
│   ├── HopperParallelHelper.java    # Two-phase hopper: plan (parallel) + execute (sequential)
│   └── mixin/LevelMixin.java        # Cancels vanilla tickBlockEntities
├── redstone/common/src/main/java/com/github/uright008/rp/
│   ├── RedstoneWireHelper.java      # Graph-based parallel wire relaxation
│   ├── DiodeTickBatcher.java        # Batched diode ticks via ConcurrentLinkedQueue
│   └── mixin/                       # Wire + diode mixins
├── entity/common/src/main/java/com/github/uright008/em/
│   └── mixin/EntityTickMixin.java   # Parallel entity tick by chunk-section bucketing
└── vectorial/common/src/main/java/com/github/uright008/vec/
    └── core/SoAStore.java           # Lock-free SoA (64x double[]), CAS slot allocator
```

## ParallelWorker API (core)

Implementers should use the **Batch API** — simplest zero-config pattern:

```java
// With results (mapper):
ParallelWorker.Batch<Input, Result> batch = new ParallelWorker.Batch<>(pool);
for (Input item : items) batch.add(item);
List<Result> results = batch.flush(item -> compute(item), timeoutSec);

// Void (consumer):
ParallelWorker.Batch<Input, Void> batch = new ParallelWorker.Batch<>(pool);
for (Input item : items) batch.add(item);
batch.flushVoid(item -> process(item), timeoutSec);
```

Legacy APIs still available: `map()`, `forEach()`, `mapEach()`, `mapBatched()`, `forEachBatched()`.

## Commit Convention

[Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

perf(core): add Batch<T,R> API with auto-batching
perf(explosion): use ChunkGrid flat BlockState[] for traceRay
fix(redstone): wireThreshold config override
chore(build): update Gradle wrapper
```

Scopes: `core`, `explosion`, `hopper`, `redstone`, `entity`, `vectorial`, `build`, `docs`

## No Cross-Dependency

NativeThreading must NOT depend on NotEnoughPalette. Both mods are tested independently.
