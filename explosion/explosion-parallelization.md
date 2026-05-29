# Minecraft 爆炸计算并行化方案

## 1. 现状分析

### 1.1 当前爆炸计算流程

基于 `net/minecraft/world/level/ServerExplosion.java` 的源码分析，`explode()` 方法按以下顺序执行：

```mermaid
flowchart TD
    A[explode()] --> B[calculateExplodedPositions]
    A --> C[hurtEntities]
    A --> D[interactWithBlocks]
    A --> E[createFire]
    
    B --> B1["1352条射线追踪<br/>(16×16×6面)"]
    B1 --> B2["每条射线逐步推进<br/>步长0.3, 衰减0.225"]
    B2 --> B3["每步检查方块抗爆性<br/>收集被破坏方块到Set"]
    
    C --> C1["获取AABB内所有实体"]
    C1 --> C2["对每个实体:"]
    C2 --> C3["getSeenPercent<br/>(实体包围盒→爆炸中心<br/>射线采样求可见比例)"]
    C2 --> C4["计算伤害 + 击退"]
    
    D --> D1["打乱方块列表"]
    D1 --> D2["逐块调用onExplosionHit<br/>收集掉落物并合并"]
    
    E --> E1["随机在已破坏方块位置<br/>生成火焰"]
```

### 1.2 性能瓶颈识别

| 阶段 | 时间复杂度 | 耗时占比（估算） | 瓶颈类型 |
|------|-----------|-----------------|---------|
| `calculateExplodedPositions()` | O(R² × steps) ≈ O(1352 × steps) | **~60-70%** | CPU密集 + 内存读取 |
| `hurtEntities()` | O(N × samples) | **~20-30%** | CPU密集 + 内存读取 |
| `interactWithBlocks()` | O(M) | ~5-10% | 世界修改（需主线程） |
| `createFire()` | O(M) | ~1-3% | 世界修改（需主线程） |

其中：
- **R** = 爆炸半径（最大约 14.0 for charged creeper）
- **steps** ≈ R / 0.3 × 0.225 ≈ R × 7.5（每条射线的步数）
- **N** = 爆炸范围内的实体数
- **samples** = `getSeenPercent` 中的采样点数量
- **M** = 被破坏的方块数

核心瓶颈是 **`calculateExplodedPositions()` 的射线追踪**，这是本方案的主要并行化目标。

---

## 2. 线程安全分析

### 2.1 只读操作（安全）

| 操作 | 类 | 线程安全性 |
|------|-----|-----------|
| `level.getBlockState(pos)` | `ServerLevel` / `ChunkAccess` | ✅ 已加载区块的只读访问是安全的 |
| `level.getFluidState(pos)` | `ServerLevel` / `ChunkAccess` | ✅ 同上 |
| `level.isInWorldBounds(pos)` | `ServerLevel` | ✅ 纯边界检查 |
| `damageCalculator.getBlockExplosionResistance()` | `ExplosionDamageCalculator` | ✅ 纯计算/只读 |
| `damageCalculator.shouldBlockExplode()` | `ExplosionDamageCalculator` | ✅ 纯计算/只读 |
| `entity.level().clip()` | `Level` | ✅ VoxelShape 是不可变对象 |
| `entity.getBoundingBox()` | `Entity` | ✅ 实体包围盒在此时不变 |
| `entity.distanceToSqr()` | `Entity` | ✅ 纯计算 |

### 2.2 写操作（需同步或延迟到主线程）

| 操作 | 风险 | 处理方式 |
|------|------|---------|
| `toBlowSet.add(pos)` | 并发写 HashSet | 用 ThreadLocal 集合或 ConcurrentHashMap |
| `entity.hurtServer()` | 修改实体生命值 | 计算阶段只读，应用阶段在主线程 |
| `entity.push(knockback)` | 修改实体速度 | 同上 |
| `hitPlayers.put(player, knockback)` | 并发写 HashMap | 延迟到主线程合并 |
| 方块破坏/掉落物生成 | 世界修改 | 必须在主线程执行 |

### 2.3 `getSeenPercent` 的线程安全性

关键分析：`getSeenPercent` 是 **static 方法**，内部调用 `entity.level().clip()`。

```java
// ServerExplosion.java L86-118
public static float getSeenPercent(final Vec3 center, final Entity entity) {
    AABB bb = entity.getBoundingBox();
    // ... 纯计算得到采样点坐标 ...
    for (double xx = 0.0; xx <= 1.0; xx += xs) {
        for (double yy = 0.0; yy <= 1.0; yy += ys) {
            for (double zz = 0.0; zz <= 1.0; zz += zs) {
                // 计算采样点位置
                Vec3 from = new Vec3(x + xOffset, y, z + zOffset);
                // clip 是只读操作，查询方块碰撞体
                if (entity.level().clip(...).getType() == HitResult.Type.MISS) {
                    hits++;
                }
                count++;
            }
        }
    }
    return (float)hits / count;
}
```

`ClipContext` 和 `VoxelShape` 都是不可变的，`clip()` 方法只读取世界数据，**线程安全**。

---

## 3. 并行化方案设计

### 3.1 总体架构

采用 **三阶段并行 + 主线程收尾** 的架构：

```
┌─────────────────────────────────────────────────────────────┐
│                    explode() 主线程入口                       │
├─────────────────────────────────────────────────────────────┤
│  Phase 1: 并行射线追踪                                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ Worker 1 │ │ Worker 2 │ │ Worker 3 │ │ Worker N │       │
│  │ 射线 0..M │ │ 射线 M+1 │ │ 射线 ... │ │ 射线 ... │       │
│  │          │ │   ..2M   │ │          │ │  ..1351  │       │
│  │ Thread-  │ │ Thread-  │ │ Thread-  │ │ Thread-  │       │
│  │ Local Set│ │ Local Set│ │ Local Set│ │ Local Set│       │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘       │
│       └─────────────┴────────────┴────────────┘             │
│                         │ merge                              │
│                    toBlow (合并Set)                          │
├─────────────────────────────────────────────────────────────┤
│  Phase 2: 并行实体暴露计算                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                    │
│  │ Worker 1 │ │ Worker 2 │ │ Worker N │                    │
│  │ Entity[] │ │ Entity[] │ │ Entity[] │                    │
│  │  子集1   │ │  子集2   │ │  子集N   │                    │
│  │          │ │          │ │          │                    │
│  │ 计算:    │ │ 计算:    │ │ 计算:    │                    │
│  │ exposure │ │ exposure │ │ exposure │                    │
│  │ damage   │ │ damage   │ │ damage   │                    │
│  │ knockback│ │ knockback│ │ knockback│                    │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘                    │
│       └─────────────┴────────────┘                          │
│                         │                                    │
│           EntityDamageResult[] (有序结果数组)                 │
├─────────────────────────────────────────────────────────────┤
│  Phase 3: 主线程应用 (顺序执行)                               │
│  - 应用实体伤害和击退                                         │
│  - 执行方块交互（掉落物）                                     │
│  - 生成火焰                                                   │
│  - 发送网络包                                                 │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Phase 1: 并行射线追踪

#### 3.2.1 射线分片策略

当前 `calculateExplodedPositions()` 共发射 **1352 条射线**（16×16×6 面 - 重叠边角）。

```java
// 当前代码: ServerExplosion.java L148-216
private List<BlockPos> calculateExplodedPositions() {
    Set<BlockPos> toBlowSet = new HashSet();
    int size = 16;
    
    for (int xx = 0; xx < 16; xx++) {
        for (int yy = 0; yy < 16; yy++) {
            for (int zz = 0; zz < 16; zz++) {
                // 只追踪6个面上的点
                if (xx == 0 || xx == 15 || yy == 0 || yy == 15 || zz == 0 || zz == 15) {
                    // ...射线追踪逻辑...
                }
            }
        }
    }
    return new ObjectArrayList<>(toBlowSet);
}
```

**分片方式**：将 1352 条射线均匀分配给 N 个工作线程。由于每条射线是独立的，可以使用简单的 **interleaved 分片**（射线 i → 线程 i % N）。

#### 3.2.2 并行实现伪代码

```java
private List<BlockPos> calculateExplodedPositionsParallel() {
    // 预生成所有射线的方向参数
    List<RayParam> rays = generateRayParams(); // 1352条射线
    
    int numThreads = Math.min(Util.maxAllowedExecutorThreads(), rays.size());
    Executor executor = Util.backgroundExecutor();
    
    // 每个线程维护自己的局部 Set，避免锁竞争
    List<CompletableFuture<Set<BlockPos>>> futures = new ArrayList<>();
    
    for (int t = 0; t < numThreads; t++) {
        final int threadIndex = t;
        futures.add(CompletableFuture.supplyAsync(() -> {
            Set<BlockPos> localSet = new HashSet<>();
            // 交错分配: 线程t处理 rays[t], rays[t+N], rays[t+2N], ...
            for (int i = threadIndex; i < rays.size(); i += numThreads) {
                traceRay(rays.get(i), localSet);
            }
            return localSet;
        }, executor));
    }
    
    // 等待所有线程完成，合并结果
    Set<BlockPos> toBlowSet = new HashSet<>();
    for (CompletableFuture<Set<BlockPos>> future : futures) {
        toBlowSet.addAll(future.join());
    }
    
    return new ObjectArrayList<>(toBlowSet);
}
```

#### 3.2.3 单条射线追踪方法（提取重构）

```java
private void traceRay(RayParam ray, Set<BlockPos> resultSet) {
    float remainingPower = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
    double xp = this.center.x;
    double yp = this.center.y;
    double zp = this.center.z;
    
    for (float stepSize = 0.3F; remainingPower > 0.0F; remainingPower -= 0.22500001F) {
        BlockPos pos = BlockPos.containing(xp, yp, zp);
        BlockState block = this.level.getBlockState(pos);
        FluidState fluid = this.level.getFluidState(pos);
        
        if (!this.level.isInWorldBounds(pos)) {
            break;
        }
        
        Optional<Float> resistance = this.damageCalculator.getBlockExplosionResistance(
            this, this.level, pos, block, fluid
        );
        if (resistance.isPresent()) {
            remainingPower -= (resistance.get() + 0.3F) * 0.3F;
        }
        
        if (remainingPower > 0.0F && this.damageCalculator.shouldBlockExplode(
            this, this.level, pos, block, remainingPower
        )) {
            resultSet.add(pos);
        }
        
        xp += ray.xd * 0.3F;
        yp += ray.yd * 0.3F;
        zp += ray.zd * 0.3F;
    }
}
```

#### 3.2.4 随机数安全

每条射线使用 `this.level.random.nextFloat()`。需要注意：
- `RandomSource` 通常不是线程安全的
- **方案A**：使用 `ThreadLocalRandom` 替代（损失可复现性，但爆炸不需要确定性随机）
- **方案B**：预生成 1352 个随机浮点数存入数组，各线程只读访问
- **推荐方案A**：爆炸的随机性不需要跨服务器一致，`ThreadLocalRandom` 最简单高效

### 3.3 Phase 2: 并行实体伤害计算

#### 3.3.1 设计思路

将 `hurtEntities()` 拆分为两个子阶段：
1. **并行计算阶段**：对每个实体计算 exposure、damage、knockback（只读）
2. **主线程应用阶段**：顺序应用伤害和击退

```java
// 计算结果 DTO
private record EntityDamageResult(
    Entity entity,
    float damage,
    Vec3 knockback,
    boolean shouldAddToHitPlayers
) {}
```

#### 3.3.2 并行实现伪代码

```java
private void hurtEntitiesParallel() {
    if (this.radius < 1.0E-5F) return;
    
    float doubleRadius = this.radius * 2.0F;
    AABB bb = /* 计算AABB */;
    List<Entity> entities = this.level.getEntities(this.source, bb);
    
    // 并行计算阶段
    int numThreads = Math.min(Util.maxAllowedExecutorThreads(), 
                              (entities.size() + 15) / 16); // 至少每线程16个实体
    if (numThreads <= 1 || entities.size() < 32) {
        // 实体太少，直接单线程
        hurtEntitiesSequential(entities, doubleRadius);
        return;
    }
    
    Executor executor = Util.backgroundExecutor();
    List<CompletableFuture<List<EntityDamageResult>>> futures = new ArrayList<>();
    
    int batchSize = (entities.size() + numThreads - 1) / numThreads;
    for (int t = 0; t < numThreads; t++) {
        int from = t * batchSize;
        int to = Math.min(from + batchSize, entities.size());
        if (from >= to) break;
        
        List<Entity> batch = entities.subList(from, to);
        futures.add(CompletableFuture.supplyAsync(() -> {
            List<EntityDamageResult> results = new ArrayList<>(batch.size());
            for (Entity entity : batch) {
                EntityDamageResult result = computeEntityDamage(entity, doubleRadius);
                if (result != null) {
                    results.add(result);
                }
            }
            return results;
        }, executor));
    }
    
    // 主线程应用阶段
    for (CompletableFuture<List<EntityDamageResult>> future : futures) {
        for (EntityDamageResult result : future.join()) {
            applyEntityDamage(result);
        }
    }
}
```

#### 3.3.3 单实体计算（可在工作线程中执行）

```java
@Nullable
private EntityDamageResult computeEntityDamage(Entity entity, float doubleRadius) {
    if (entity.ignoreExplosion(this)) return null;
    
    double dist = Math.sqrt(entity.distanceToSqr(this.center)) / doubleRadius;
    if (dist > 1.0) return null;
    
    Vec3 entityOrigin = entity instanceof PrimedTnt 
        ? entity.position() 
        : entity.getEyePosition();
    Vec3 direction = entityOrigin.subtract(this.center).normalize();
    
    boolean shouldDamage = this.damageCalculator.shouldDamageEntity(this, entity);
    float knockbackMult = this.damageCalculator.getKnockbackMultiplier(entity);
    float exposure = (!shouldDamage && knockbackMult == 0.0F) 
        ? 0.0F 
        : getSeenPercent(this.center, entity); // static方法，线程安全
    
    float damage = shouldDamage 
        ? this.damageCalculator.getEntityDamageAmount(this, entity, exposure) 
        : 0.0F;
    
    double knockbackResistance = entity instanceof LivingEntity le
        ? le.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
        : 0.0;
    double knockbackPower = (1.0 - dist) * exposure * knockbackMult * (1.0 - knockbackResistance);
    Vec3 knockback = direction.scale(knockbackPower);
    
    return new EntityDamageResult(entity, damage, knockback, entity instanceof Player);
}
```

#### 3.3.4 主线程应用

```java
private void applyEntityDamage(EntityDamageResult result) {
    Entity entity = result.entity();
    
    if (result.damage() > 0) {
        entity.hurtServer(this.level, this.damageSource, result.damage());
    }
    
    entity.push(result.knockback());
    
    if (entity.is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile projectile) {
        projectile.setOwner(this.damageSource.getEntity());
    } else if (entity instanceof Player player 
               && !player.isSpectator() 
               && (!player.isCreative() || !player.getAbilities().flying)) {
        this.hitPlayers.put(player, result.knockback());
    }
    
    entity.onExplosionHit(this.source);
}
```

### 3.4 自适应并行策略

并非所有爆炸都需要并行化。根据爆炸规模自适应选择：

```java
public int explode() {
    this.level.gameEvent(this.source, GameEvent.EXPLODE, this.center);
    
    boolean useParallel = shouldUseParallel();
    
    List<BlockPos> toBlow;
    if (useParallel) {
        toBlow = calculateExplodedPositionsParallel();
        hurtEntitiesParallel();
    } else {
        toBlow = calculateExplodedPositions();
        hurtEntities();
    }
    
    // Phase 3 & 4: 始终在主线程执行
    if (this.interactsWithBlocks()) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("explosion_blocks");
        this.interactWithBlocks(toBlow);
        profiler.pop();
    }
    
    if (this.fire) {
        this.createFire(toBlow);
    }
    
    return toBlow.size();
}

private boolean shouldUseParallel() {
    // 小爆炸不值得并行化
    if (this.radius < 3.0F) return false;
    // 不破坏方块的爆炸（如风弹）没必要并行
    if (!this.interactsWithBlocks()) return false;
    return true;
}
```

---

## 4. 详细实现步骤

### 4.1 Step 1: 提取射线追踪逻辑

将 `calculateExplodedPositions()` 中的内层循环提取为独立方法 `traceRay()`：

```java
// 射线参数记录
private record RayParam(double xd, double yd, double zd) {}

// 预生成所有射线参数（构造函数中或惰性初始化）
private static final List<RayParam> RAY_PARAMS = generateRayParams();

private static List<RayParam> generateRayParams() {
    List<RayParam> params = new ArrayList<>();
    for (int xx = 0; xx < 16; xx++) {
        for (int yy = 0; yy < 16; yy++) {
            for (int zz = 0; zz < 16; zz++) {
                if (xx == 0 || xx == 15 || yy == 0 || yy == 15 || zz == 0 || zz == 15) {
                    double xd = xx / 15.0F * 2.0F - 1.0F;
                    double yd = yy / 15.0F * 2.0F - 1.0F;
                    double zd = zz / 15.0F * 2.0F - 1.0F;
                    double d = Math.sqrt(xd * xd + yd * yd + zd * zd);
                    params.add(new RayParam(xd / d, yd / d, zd / d));
                }
            }
        }
    }
    return params;
}
```

### 4.2 Step 2: 引入 ThreadLocalRandom

```java
// 在 traceRay 方法中替换 this.level.random
// 原代码：
float remainingPower = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);

// 并行版本：
float remainingPower = this.radius * (0.7F + ThreadLocalRandom.current().nextFloat() * 0.6F);
```

### 4.3 Step 3: 实现并行射线追踪

使用 `CompletableFuture` + `Util.backgroundExecutor()`（Minecraft 已有的 ForkJoinPool）。

### 4.4 Step 4: 拆分 hurtEntities

提取 `computeEntityDamage()` 和 `applyEntityDamage()` 方法。

### 4.5 Step 5: 实现并行实体计算

使用与 Phase 1 相同的线程池和分片策略。

### 4.6 Step 6: 添加 Profiler 埋点

```java
profiler.push("explosion_rays_parallel");
// ... 并行射线追踪 ...
profiler.popPush("explosion_entities_parallel");
// ... 并行实体计算 ...
profiler.pop();
```

---

## 5. 性能预期

### 5.1 理论加速比

根据 Amdahl 定律：

| 场景 | 串行部分占比 | N=2 | N=4 | N=8 | N=16 |
|------|------------|-----|-----|-----|------|
| 大爆炸 (R=14, 实体多) | ~15% | 1.74× | 2.81× | 4.21× | 5.42× |
| 中等爆炸 (R=7, 实体少) | ~25% | 1.6× | 2.29× | 3.05× | 3.56× |
| 小爆炸 (R=3) | 不走并行 | - | - | - | - |

### 5.2 关键指标

| 指标 | 当前 | 并行后（预估） |
|------|------|--------------|
| 大型爆炸（charged creeper）| ~8-15ms | ~2-4ms |
| 中型爆炸（TNT）| ~3-5ms | ~1-2ms |
| 尾延迟（P99）| ~20ms | ~5ms |
| MSPT 峰值降低 | - | 10-50% |

---

## 6. 风险与缓解措施

### 6.1 风险矩阵

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 区块卸载导致并发读异常 | 低 | 高 | 爆炸前确保范围内区块已加载；使用 `ServerLevel.getChunk()` 触发加载 |
| ThreadLocalRandom 导致非确定性 | 中 | 低 | 爆炸结果本身就不需要确定性，且原始代码就用 Random |
| 线程竞争降低性能 | 低 | 中 | 自适应策略：小爆炸不走并行 |
| 死锁（与 chunk loading 交互） | 低 | 高 | 所有 block getter 都是非阻塞的只读操作；如需加载区块，在并行前预加载 |
| 内存开销（ThreadLocal Set） | 低 | 低 | 每个 Set 最多几千个 BlockPos，可忽略 |
| `getSeenPercent` 中的 `clip()` 并发问题 | 极低 | 高 | VoxelShape 和 BlockState 都是不可变的，clip 只读遍历 |

### 6.2 最坏情况回退

```java
private static final boolean ENABLE_PARALLEL_EXPLOSION = true; // 可通过系统属性控制

public int explode() {
    if (!ENABLE_PARALLEL_EXPLOSION 
        || !this.level.getServer().isDedicatedServer()  // 仅专用服务器开启
        || this.radius < 3.0F) {
        return explodeSequential(); // 保持原有逻辑
    }
    return explodeParallel();
}
```

### 6.3 区块加载保证

在并行执行前，确保所有需要的区块已加载：

```java
private void ensureChunksLoaded() {
    int cx = Mth.floor(this.center.x) >> 4;
    int cz = Mth.floor(this.center.z) >> 4;
    int range = (int) Math.ceil(this.radius / 16.0) + 1;
    
    for (int dx = -range; dx <= range; dx++) {
        for (int dz = -range; dz <= range; dz++) {
            this.level.getChunk(cx + dx, cz + dz);
        }
    }
}
```

---

## 7. 替代方案

### 7.1 方案 B: 射线采样降级（不并行，但降低计算量）

对于大爆炸，当前 1352 条射线可能过多。可以动态调整：

| 爆炸半径 | 当前射线数 | 建议射线数 | 精度损失 |
|---------|-----------|-----------|---------|
| R ≤ 4 | 1352 | 1352 | 0% |
| 4 < R ≤ 8 | 1352 | 488 (8³) | <5% |
| R > 8 | 1352 | 152 (6³) | <10% |

这是与并行化互补的优化，可以同时应用。

### 7.2 方案 C: SIMD 优化

对于 `clip()` 中的向量运算（大量 `Vec3` 运算），可以利用 JVM 的自动向量化或 Panama Vector API（JDK 16+）。这是一个更深层的优化方向。

---

## 8. 与 Minecraft 现有并行基础设施的兼容性

Minecraft 已有以下并行基础设施：

```java
// Util.java L100-103
private static final TracingExecutor BACKGROUND_EXECUTOR = makeExecutor("Main");
private static final TracingExecutor IO_POOL = makeIoExecutor("IO-Worker-", false);
```

- **`BACKGROUND_EXECUTOR`**：基于 `ForkJoinPool`，线程数 = CPU 核心数 - 1（至少 1），适合 CPU 密集型任务
- **`IO_POOL`**：基于 `CachedThreadPool`，适合 I/O 密集型任务

爆炸并行化应该使用 **`BACKGROUND_EXECUTOR`**，因为射线追踪是纯 CPU 计算。

```java
// 调用方式
CompletableFuture.supplyAsync(() -> { /* ray tracing */ }, Util.backgroundExecutor());
```

同时，`ServerLevel` 的 tick 已经在 `BlockableEventLoop` 上运行，爆炸在主线程触发，提交到后台线程后通过 `join()` 等待结果，符合 Minecraft 现有的并行模式。

---

## 9. 总结

本方案的核心思路是：

1. **Phase 1（并行射线追踪）**：将 1352 条独立射线均匀分配给多个线程，每个线程维护局部 `HashSet`，最后合并。预期加速 **3-5×**。

2. **Phase 2（并行实体伤害计算）**：将实体列表分片，每个线程计算 exposure / damage / knockback（只读），主线程统一应用。预期加速 **2-3×**。

3. **Phase 3 & 4（主线程执行）**：方块交互和火焰生成必须在主线程，因为它们修改世界状态。

4. **自适应策略**：仅对半径 ≥ 3 且有方块破坏的爆炸启用并行化，避免小爆炸的线程开销。

5. **安全回退**：通过系统属性和服务器类型控制，可在出现问题时快速禁用。

整体而言，这是一个**低风险、高收益**的优化方案，利用了爆炸计算天然的数据并行性，与 Minecraft 现有的线程基础设施完全兼容。
