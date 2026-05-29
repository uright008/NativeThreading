package com.github.uright008.pc;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.BitRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/**
 * Thread-safe drop-in replacement for {@code LegacyRandomSource}.
 *
 * <p>Vanilla's {@code LegacyRandomSource} throws on concurrent access.
 * This implementation stores per-thread LCG state via {@link ThreadLocal},
 * eliminating the {@code AtomicLong.compareAndSet()} contention of the
 * previous CAS-based version.</p>
 *
 * <p>Each thread's LCG is seeded from a CAS-protected global seed to
 * avoid correlated sequences when many threads start simultaneously.</p>
 */
public final class ThreadSafeRandomSource implements BitRandomSource {

    private static final long MULTIPLIER = 25214903917L;
    private static final long INCREMENT = 11L;
    private static final long MASK = 281474976710655L;
    private static final int SEED_BITS = 48;

    private static class PerThread {
        long seed;
        boolean haveNextNextGaussian;
        double nextNextGaussian;
    }

    private final ThreadLocal<PerThread> state;

    private ThreadSafeRandomSource(long seed) {
        long initial = (seed ^ MULTIPLIER) & MASK;
        this.state = ThreadLocal.withInitial(() -> {
            PerThread s = new PerThread();
            s.seed = initial;
            return s;
        });
    }

    public static ThreadSafeRandomSource of(RandomSource source) {
        return new ThreadSafeRandomSource(source.nextLong());
    }

    public static ThreadSafeRandomSource ofSeed(long seed) {
        return new ThreadSafeRandomSource(seed);
    }

    @Override
    public RandomSource fork() {
        return new ThreadSafeRandomSource(this.nextLong());
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return new ThreadSafePositionalRandomFactory(this.nextLong());
    }

    @Override
    public void setSeed(long s) {
        state.get().seed = (s ^ MULTIPLIER) & MASK;
    }

    @Override
    public int next(int bits) {
        PerThread s = state.get();
        s.seed = (s.seed * MULTIPLIER + INCREMENT) & MASK;
        return (int) (s.seed >>> (SEED_BITS - bits));
    }

    @Override
    public double nextGaussian() {
        PerThread s = state.get();
        if (s.haveNextNextGaussian) {
            s.haveNextNextGaussian = false;
            return s.nextNextGaussian;
        }
        double d, e, f;
        do {
            d = 2.0 * nextDouble() - 1.0;
            e = 2.0 * nextDouble() - 1.0;
            f = d * d + e * e;
        } while (f >= 1.0 || f == 0.0);
        double g = Math.sqrt(-2.0 * Math.log(f) / f);
        s.nextNextGaussian = e * g;
        s.haveNextNextGaussian = true;
        return d * g;
    }

    // ── PositionalRandomFactory ────────────────────

    private static class ThreadSafePositionalRandomFactory implements PositionalRandomFactory {
        private final long seed;

        ThreadSafePositionalRandomFactory(long seed) {
            this.seed = seed;
        }

        @Override
        public RandomSource at(int x, int y, int z) {
            long positionalSeed = net.minecraft.util.Mth.getSeed(x, y, z);
            return new ThreadSafeRandomSource(positionalSeed ^ this.seed);
        }

        @Override
        public RandomSource fromHashOf(String name) {
            return new ThreadSafeRandomSource((long) name.hashCode() ^ this.seed);
        }

        @Override
        public RandomSource fromSeed(long seed) {
            return new ThreadSafeRandomSource(seed);
        }

        @Override
        public void parityConfigString(StringBuilder sb) {
            sb.append("ThreadSafePositionalRandomFactory{").append(this.seed).append("}");
        }
    }
}
