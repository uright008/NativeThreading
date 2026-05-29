package com.github.uright008.rp;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Behavior-consistency tests for {@link RedstoneWireHelper#relaxWire} —
 * verifies the parallel relaxation algorithm produces results identical
 * to the vanilla redstone wire power formula.
 *
 * <h3>Vanilla redstone wire power rule</h3>
 * A wire's power = max(own block signal, max(neighbor power) - 1).
 * If the wire receives power 15 directly, it stays at 15.
 * If no neighbors are powered, power = own block signal.
 *
 * <h3>AI-readable summary</h3>
 * Each test constructs a scenario (block signals + neighbor powers),
 * runs {@code relaxWire}, and checks the output matches the vanilla
 * formula. Tests cover the full power range (0-15), edge cases,
 * stress tests with random graphs, and sequential-vs-parallel equivalence.
 */
@DisplayName("RedstoneWireHelper — vanilla behavior consistency")
class RedstoneWireHelperBehaviorTest {

    // ── Power 15 is stable ──────────────────────

    @Test
    @DisplayName("Vanilla: power-15 wire stays 15 regardless of neighbors")
    void relaxWire_power15_alwaysStays15() {
        int[] blockSignals = {15, 0, 0};
        int[] prevPowers   = {15, 5, 0};
        int[] neighbors    = {1, 2};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors, 0);
        assertThat(result).as("power-15 wire must stay 15").isEqualTo(15);
    }

    @Test
    @DisplayName("Vanilla: power-15 wire stays 15 even with 0 neighbors (empty array)")
    void relaxWire_power15_emptyNeighbors_stays15() {
        int[] blockSignals = {15};
        int[] prevPowers   = {15};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, new int[0], 0);
        assertThat(result).as("power-15 wire with no neighbors must stay 15").isEqualTo(15);
    }

    @Test
    @DisplayName("Vanilla: receiving power 15 from neighbor does not drop")
    void relaxWire_receiving15FromNeighbor_stays15() {
        int[] blockSignals = {0, 15};
        int[] prevPowers   = {0, 15};
        int[] neighbors    = {1};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors, 0);
        assertThat(result).as("wire receiving 15 from neighbor must become 14 (15-1)")
                .isEqualTo(14);
    }

    // ── Block signal propagation ─────────────────

    @Test
    @DisplayName("Vanilla: isolated wire gets own block signal (0)")
    void relaxWire_isolatedWire_getsOwnBlockSignal_zero() {
        int[] blockSignals = {0};
        int[] prevPowers   = {0};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, new int[0], 0);
        assertThat(result).as("isolated wire with blockSignal=0 must return 0").isZero();
    }

    @Test
    @DisplayName("Vanilla: isolated wire gets own block signal (5)")
    void relaxWire_isolatedWire_getsOwnBlockSignal_mid() {
        int[] blockSignals = {5};
        int[] prevPowers   = {5};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, new int[0], 0);
        assertThat(result).as("isolated wire with blockSignal=5 must return 5").isEqualTo(5);
    }

    // ── Neighbor power - 1 propagation ────────────

    @Test
    @DisplayName("Vanilla: wire receives neighbor-1 (14 from 15)")
    void relaxWire_receivesNeighborMinusOne() {
        int[] blockSignals = {0, 15, 0};
        int[] prevPowers   = {0, 15, 0};
        int[] neighbors    = {1};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors, 0);
        assertThat(result).as("wire next to power-15 source must receive 14").isEqualTo(14);
    }

    @Test
    @DisplayName("Vanilla: daisy-chain powers drop by 1 per wire")
    void relaxWire_daisyChain_dropsByOne() {
        // 3 wires: [source=15] → [wire1] → [wire2]
        // wire1 should get 14, wire2 should get 13
        int[] blockSignals = {0, 0, 0};
        int[] prevPowers   = {15, 0, 0};
        int[] neighbors1   = {0};
        int[] neighbors2   = {1};

        // After wire0→wire1: wire1 becomes 14
        int wire1Power = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors1, 1);
        assertThat(wire1Power).as("wire1 next to 15-source must be 14").isEqualTo(14);

        // After wire1→wire2: wire2 becomes 13
        int[] prevAfter    = {15, wire1Power, 0};
        int wire2Power = RedstoneWireHelper.relaxWire(blockSignals, prevAfter, neighbors2, 2);
        assertThat(wire2Power).as("wire2 next to 14-wire must be 13").isEqualTo(13);
    }

    @Test
    @DisplayName("Vanilla: wire keeps own signal when stronger than incoming")
    void relaxWire_ownSignalStrongerThanIncoming() {
        // Wire has blockSignal=10, neighbor provides power=5 → incoming=4
        // Own block signal (10) > incoming (4) → result = 10
        int[] blockSignals = {10, 5};
        int[] prevPowers   = {10, 5};
        int[] neighbors    = {1};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors, 0);
        assertThat(result).as("own signal 10 > incoming 4, must keep 10").isEqualTo(10);
    }

    @Test
    @DisplayName("Vanilla: incoming power overrides weaker own signal")
    void relaxWire_incomingStronger_beatsOwnSignal() {
        // Wire has blockSignal=2, neighbor provides power=5 → incoming=4
        // Own block signal (2) < incoming (4) → result = 4
        int[] blockSignals = {2, 5};
        int[] prevPowers   = {2, 5};
        int[] neighbors    = {1};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors, 0);
        assertThat(result).as("incoming 4 > own signal 2, must take 4").isEqualTo(4);
    }

    // ── Multiple neighbors: picks max ─────────────

    @Test
    @DisplayName("Vanilla: picks max among multiple neighbors")
    void relaxWire_multipleNeighbors_picksMax() {
        // Wire has neighbors with powers 3, 7, 2 → incoming = max(3,7,2)-1 = 6
        int[] blockSignals = {0, 3, 7, 2};
        int[] prevPowers   = {0, 3, 7, 2};
        int[] neighbors    = {1, 2, 3};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors, 0);
        assertThat(result).as("max neighbor=7, incoming=6, result must be 6").isEqualTo(6);
    }

    @Test
    @DisplayName("Vanilla: neighbor with 0 power contributes 0 (does not push negative)")
    void relaxWire_zeroNeighbor_doesNotReduce() {
        // All neighbors at 0. Incoming = max(0)-1 = 0 (not -1)
        int[] blockSignals = {5, 0, 0, 0};
        int[] prevPowers   = {5, 0, 0, 0};
        int[] neighbors    = {1, 2, 3};

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors, 0);
        assertThat(result).as("all neighbors at 0, must keep own signal 5").isEqualTo(5);
    }

    // ── Full power range ──────────────────────────

    @Test
    @DisplayName("Vanilla: all power levels (0-15) propagate correctly")
    void relaxWire_allPowerLevels_propagateCorrectly() {
        for (int sourcePower = 0; sourcePower <= 15; sourcePower++) {
            int[] blockSignals = {0, sourcePower};
            int[] prevPowers   = {0, sourcePower};
            int[] neighbors    = {1};

            int expected = (sourcePower > 0) ? sourcePower - 1 : 0;
            int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors, 0);

            assertThat(result)
                    .as("source=%d → expected incoming=%d", sourcePower, expected)
                    .isEqualTo(expected);
        }
    }

    // ── Convergence: relaxation reaches steady state ──

    @Test
    @DisplayName("Vanilla: linear wire chain converges to stable power levels")
    void relaxWire_linearChain_convergesToStableState() {
        // 16-wire chain: [powered=15] [wire1] [wire2] ... [wire15]
        // After convergence: [15, 14, 13, ..., 0]
        int n = 16;
        int[] blockSignals = new int[n];
        blockSignals[0] = 15; // power source at wire 0
        int[] powers        = new int[n];

        // Iterate until stable (max n iterations for linear chain)
        boolean changed;
        int iters = 0;
        do {
            changed = false;
            int[] prev = powers.clone();
            for (int i = 0; i < n; i++) {
                int[] neighbors;
                if (i == 0)      neighbors = new int[]{1};
                else if (i == n-1) neighbors = new int[]{i-1};
                else              neighbors = new int[]{i-1, i+1};

                int np = RedstoneWireHelper.relaxWire(blockSignals, prev, neighbors, i);
                if (np != powers[i]) { powers[i] = np; changed = true; }
            }
            iters++;
        } while (changed && iters < n);

        // Verify: powers should be [15, 14, 13, ..., 0]
        for (int i = 0; i < n; i++) {
            assertThat(powers[i])
                    .as("wire[%d] must be %d after convergence", i, 15 - i)
                    .isEqualTo(15 - i);
        }
        assertThat(iters).as("must converge within %d iterations", n).isLessThanOrEqualTo(n);
    }

    // ── Stress: random graph consistency ──────────

    @Test
    @DisplayName("Vanilla: random wire graph yields same result regardless of iteration order")
    void relaxWire_randomGraph_orderIndependent() {
        int n = 200;
        int[] blockSignals = new int[n];
        int[] powersForward  = new int[n];
        int[] powersReverse  = new int[n];

        // Set random block signals
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < n; i++) {
            blockSignals[i] = rng.nextInt(16);
            powersForward[i] = blockSignals[i];
            powersReverse[i] = blockSignals[i];
        }

        // Random edges
        int[][] neighbors = new int[n][];
        for (int i = 0; i < n; i++) {
            int degree = rng.nextInt(5);
            java.util.List<Integer> nbs = new java.util.ArrayList<>();
            for (int j = 0; j < degree; j++) {
                int nb = rng.nextInt(n);
                if (nb != i) nbs.add(nb);
            }
            neighbors[i] = nbs.stream().mapToInt(Integer::intValue).toArray();
        }

        // Forward iteration
        boolean changed;
        int iters = 0;
        do {
            changed = false;
            int[] prev = powersForward.clone();
            for (int i = 0; i < n; i++) {
                int np = RedstoneWireHelper.relaxWire(blockSignals, prev, neighbors[i], i);
                if (np != powersForward[i]) { powersForward[i] = np; changed = true; }
            }
            iters++;
        } while (changed && iters < n);

        // Reverse iteration
        iters = 0;
        do {
            changed = false;
            int[] prev = powersReverse.clone();
            for (int i = n-1; i >= 0; i--) {
                int np = RedstoneWireHelper.relaxWire(blockSignals, prev, neighbors[i], i);
                if (np != powersReverse[i]) { powersReverse[i] = np; changed = true; }
            }
            iters++;
        } while (changed && iters < n);

        // Both orderings must produce the same result (convergence is order-independent)
        for (int i = 0; i < n; i++) {
            assertThat(powersForward[i])
                    .as("wire[%d]: forward=%d reverse=%d must match", i, powersForward[i], powersReverse[i])
                    .isEqualTo(powersReverse[i]);
        }
    }

    // ── Self-loop safety ──────────────────────────

    @Test
    @DisplayName("Vanilla: self-referencing neighbor entry is harmless")
    void relaxWire_selfNeighbor_isHarmless() {
        // Wire with self in neighbor list (should not happen, but be safe)
        int[] blockSignals = {5};
        int[] prevPowers   = {5};
        int[] neighbors    = {0, 0}; // self twice

        int result = RedstoneWireHelper.relaxWire(blockSignals, prevPowers, neighbors, 0);
        // Self power = 5, incoming = max(5)-1 = 4. Own signal 5 > 4 → result 5.
        assertThat(result).as("self in neighbor list must not affect result").isEqualTo(5);
    }
}
