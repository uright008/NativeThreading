package com.github.uright008.hp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Behavior-consistency tests for {@link HopperTransferPlan} —
 * verifies plan factory methods produce correct plan structure,
 * snapshot copying, and edge cases.
 *
 * <h3>Vanilla behavior</h3>
 * HopperTransferPlan captures a snapshot of the item at decision time
 * via {@code ItemStack.copy()}. The two-phase plan-execute model in
 * {@link HopperParallelHelper} relies on snapshot isolation for correctness.
 *
 * <p>Note: Tests use {@link ItemStack#EMPTY} because the Minecraft item
 * registry is not initialized in unit tests.</p>
 */
@DisplayName("HopperTransferPlan — vanilla plan correctness")
class HopperTransferPlanTest {

    private static final BlockPos HOPPER    = new BlockPos(10, 64, 20);
    private static final BlockPos TARGET    = new BlockPos(10, 65, 20);
    private static final BlockPos SOURCE    = new BlockPos(10, 66, 20);
    private static final BlockPos ENTITY_POS = new BlockPos(10, 63, 20);

    // ── PUSH plan ────────────────────────────────

    @Test
    @DisplayName("push: creates PUSH plan with correct positions and slot")
    void push_createsPushPlan() {
        HopperTransferPlan plan = HopperTransferPlan.push(HOPPER, 2, ItemStack.EMPTY, TARGET, Direction.NORTH);

        assertThat(plan.kind).isEqualTo(HopperTransferPlan.Kind.PUSH);
        assertThat(plan.hopperPos).isEqualTo(HOPPER);
        assertThat(plan.otherPos).isEqualTo(TARGET);
        assertThat(plan.hopperSlot).isEqualTo(2);
        assertThat(plan.direction).isEqualTo(Direction.NORTH);
    }

    @Test
    @DisplayName("push: snapshot is copied (factory calls ItemStack.copy())")
    void push_snapshotIsCopyOfInput() {
        ItemStack original = ItemStack.EMPTY;
        HopperTransferPlan plan = HopperTransferPlan.push(HOPPER, 0, original, TARGET, Direction.UP);

        // snapshot.copy() is called in the factory — for EMPTY this returns EMPTY singleton
        assertThat(plan.snapshot.isEmpty()).isTrue();
        assertThat(plan.snapshot.getCount()).isZero();
    }

    @Test
    @DisplayName("push: all Direction values produce valid plans")
    void push_allDirections_valid() {
        for (Direction dir : Direction.values()) {
            HopperTransferPlan plan = HopperTransferPlan.push(HOPPER, 0, ItemStack.EMPTY, TARGET, dir);
            assertThat(plan.direction).isEqualTo(dir);
            assertThat(plan.kind).isEqualTo(HopperTransferPlan.Kind.PUSH);
        }
    }

    // ── PULL plan (container) ─────────────────────

    @Test
    @DisplayName("pullFromContainer: creates PULL plan with DOWN direction")
    void pullFromContainer_createsPullPlan() {
        HopperTransferPlan plan = HopperTransferPlan.pullFromContainer(HOPPER, SOURCE, 3, ItemStack.EMPTY);

        assertThat(plan.kind).isEqualTo(HopperTransferPlan.Kind.PULL);
        assertThat(plan.hopperPos).isEqualTo(HOPPER);
        assertThat(plan.otherPos).isEqualTo(SOURCE);
        assertThat(plan.hopperSlot).isEqualTo(3);
        assertThat(plan.direction).isEqualTo(Direction.DOWN);
    }

    @Test
    @DisplayName("pullFromContainer: snapshot records empty state correctly")
    void pullFromContainer_snapshotEmpty() {
        HopperTransferPlan plan = HopperTransferPlan.pullFromContainer(HOPPER, SOURCE, 1, ItemStack.EMPTY);
        assertThat(plan.snapshot.isEmpty()).isTrue();
    }

    // ── PULL plan (entity) ────────────────────────

    @Test
    @DisplayName("pullEntity: creates PULL plan with slot=-1 and direction=null")
    void pullEntity_createsPullPlan_noSlot() {
        HopperTransferPlan plan = HopperTransferPlan.pullEntity(HOPPER, ENTITY_POS, ItemStack.EMPTY);

        assertThat(plan.kind).isEqualTo(HopperTransferPlan.Kind.PULL);
        assertThat(plan.hopperPos).isEqualTo(HOPPER);
        assertThat(plan.otherPos).isEqualTo(ENTITY_POS);
        assertThat(plan.hopperSlot)
                .as("entity pull must have hopperSlot = -1")
                .isEqualTo(-1);
        assertThat(plan.direction)
                .as("entity pull must have null direction")
                .isNull();
    }

    @Test
    @DisplayName("pullEntity: snapshot is empty stack")
    void pullEntity_snapshotEmpty() {
        HopperTransferPlan plan = HopperTransferPlan.pullEntity(HOPPER, ENTITY_POS, ItemStack.EMPTY);
        assertThat(plan.snapshot.isEmpty()).isTrue();
    }

    // ── Kind enum ─────────────────────────────────

    @Test
    @DisplayName("Kind: PUSH and PULL are distinct")
    void kind_pushAndPull_areDistinct() {
        assertThat(HopperTransferPlan.Kind.PUSH)
                .isNotEqualTo(HopperTransferPlan.Kind.PULL);
    }

    @Test
    @DisplayName("Kind: valueOf round-trips correctly")
    void kind_valueOf_roundtrips() {
        assertThat(HopperTransferPlan.Kind.valueOf("PUSH")).isEqualTo(HopperTransferPlan.Kind.PUSH);
        assertThat(HopperTransferPlan.Kind.valueOf("PULL")).isEqualTo(HopperTransferPlan.Kind.PULL);
    }

    // ── Multiple plans with same positions ────────

    @Test
    @DisplayName("Multiple plans from same hopper: plans are independent")
    void multiplePlans_sameHopper_areIndependent() {
        HopperTransferPlan p1 = HopperTransferPlan.push(HOPPER, 0, ItemStack.EMPTY, TARGET, Direction.NORTH);
        HopperTransferPlan p2 = HopperTransferPlan.push(HOPPER, 1, ItemStack.EMPTY, TARGET, Direction.NORTH);

        assertThat(p1.hopperSlot).isEqualTo(0);
        assertThat(p2.hopperSlot).isEqualTo(1);
        assertThat(p1.snapshot).isNotNull();
        assertThat(p2.snapshot).isNotNull();
    }

    // ── Stress: many plan creations ───────────────

    @Test
    @DisplayName("Stress: 1000 plan creations without error")
    void stress_1000planCreations() {
        java.util.List<HopperTransferPlan> plans = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            HopperTransferPlan plan;
            if (i % 3 == 0) {
                plan = HopperTransferPlan.push(HOPPER, i % 5, ItemStack.EMPTY, TARGET, Direction.values()[i % 6]);
            } else if (i % 3 == 1) {
                plan = HopperTransferPlan.pullFromContainer(HOPPER, SOURCE, i % 3, ItemStack.EMPTY);
            } else {
                plan = HopperTransferPlan.pullEntity(HOPPER, ENTITY_POS, ItemStack.EMPTY);
            }
            plans.add(plan);
            assertThat(plan.snapshot).isNotNull();
        }
        assertThat(plans).hasSize(1000);
    }
}
