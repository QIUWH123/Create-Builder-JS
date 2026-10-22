package net.quiwh.cbjs.kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.kubejs.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Real KubeJS Java listeners and the injected kinetic tick, without changing user listeners. */
@GameTestHolder("cbjs")
@PrefixGameTestTemplate(false)
public final class CogwheelEventGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static final class Observation {
        KineticBlockEntity entity;
        int speedCount;
        int stressCount;
        int overloadCount;
        String mode = "pass";
        KineticSpeedChangedEventJS speed;
        KineticStressChangedEventJS stress;

        boolean matches(KineticEventJS event) {
            return entity != null && event.getLevel() == entity.getLevel()
                    && event.getPos().equals(entity.getBlockPos());
        }
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_events")
    public static void kineticCallbacksCancellationAndReentry(GameTestHelper helper) {
        var live = new AtomicReference<Observation>(new Observation());
        // Registered closures retain only this holder, which is cleared in finally (not the world/helper).
        CBJSEvents.KINETIC_SPEED_CHANGED.listenJava(ScriptType.SERVER, null, raw -> {
            var state = live.get();
            var event = (KineticSpeedChangedEventJS) raw;
            if (state != null && state.matches(event)) { state.speedCount++; state.speed = event; }
            return null;
        });
        CBJSEvents.KINETIC_STRESS_CHANGED.listenJava(ScriptType.SERVER, null, raw -> {
            var state = live.get();
            var event = (KineticStressChangedEventJS) raw;
            if (state != null && state.matches(event)) { state.stressCount++; state.stress = event; }
            return null;
        });
        CBJSEvents.KINETIC_OVERLOAD.listenJava(ScriptType.SERVER, null, raw -> {
            var state = live.get();
            var event = (KineticOverloadEventJS) raw;
            if (state == null || !state.matches(event)) return null;
            state.overloadCount++;
            switch (state.mode) {
                case "cancel" -> event.cancel();
                case "replace" -> event.getLevel().setBlock(event.getPos(), Blocks.STONE.defaultBlockState(), 3);
                case "slow" -> state.entity.setSpeed(64);
                case "recursive" -> { GearLimitManager.check(state.entity); event.cancel(); }
                default -> { }
            }
            return null;
        });
        try {
            Block gear = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("kubejs:steel_cogwheel"));
            helper.assertTrue(gear != null && gear != Blocks.AIR, "Missing supplied scripted steel gear");
            var state = live.get();
            place(helper, state, gear);
            int baseline = state.speedCount;
            state.entity.setSpeed(64);
            state.entity.tick();
            helper.assertTrue(state.speedCount == baseline + 1 && state.speed.getOldRpm() == 0
                            && state.speed.getRpm() == 64 && state.speed.getTheoreticalRpm() == 64,
                    "Injected tick did not dispatch signed 0 to 64 RPM snapshot");
            state.entity.tick();
            helper.assertTrue(state.speedCount == baseline + 1, "Unchanged tick duplicated speed event");
            state.entity.setSpeed(-64);
            state.entity.tick();
            helper.assertTrue(state.speed.getOldRpm() == 64 && state.speed.getRpm() == -64,
                    "Reversal lost signed RPM fields");
            int stressBaseline = state.stressCount;
            state.entity.updateFromNetwork(100, 10, 1);
            state.entity.tick();
            helper.assertTrue(state.stressCount == stressBaseline + 1 && state.stress.getCapacity() == 100
                            && state.stress.getStress() == 10, "Network update did not dispatch stress/capacity");
            state.entity.updateFromNetwork(120, 10, 1);
            state.entity.tick();
            helper.assertTrue(state.stressCount == stressBaseline + 2 && state.stress.getOldCapacity() == 100
                            && state.stress.getCapacity() == 120, "Capacity-only update did not dispatch");
            state.entity.tick();
            helper.assertTrue(state.stressCount == stressBaseline + 2, "Unchanged tick duplicated stress event");

            helper.assertTrue(GearLimitManager.reload(event -> event.addKineticDataWithDrops(
                    gear, 64, null, List.of(new ItemStack(Items.STICK)))), "Temporary rule rejected");
            for (String mode : new String[]{"cancel", "recursive", "slow", "replace", "pass"}) {
                place(helper, state, gear);
                helper.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
                state.mode = mode;
                state.overloadCount = 0;
                state.entity.setSpeed(65);
                state.entity.tick();
                helper.assertTrue(state.overloadCount == 1, mode + " did not dispatch exactly one overload callback");
                if (mode.equals("pass")) {
                    helper.assertTrue(helper.getBlockState(POS).isAir(), "Uncancelled overload survived");
                    helper.assertItemEntityCountIs(Items.STICK, POS, 2, 1);
                } else {
                    helper.assertBlockPresent(mode.equals("replace") ? Blocks.STONE : gear, POS);
                    helper.assertEntityNotPresent(net.minecraft.world.entity.EntityType.ITEM);
                    if (mode.equals("slow")) helper.assertTrue(state.entity.getSpeed() == 64,
                            "Handler correction was not retained");
                }
            }
            helper.succeed();
        } finally {
            live.set(null);
            GearLimitManager.reload();
        }
    }

    private static void place(GameTestHelper helper, Observation state, Block gear) {
        state.entity = null;
        helper.setBlock(POS, Blocks.AIR);
        helper.setBlock(POS, gear.defaultBlockState());
        state.entity = (KineticBlockEntity) helper.getBlockEntity(POS);
        state.entity.updateSpeed = false;
        // Run initial validation while stationary before manually exercising observed RPM.
        state.entity.tick();
    }

    private CogwheelEventGameTests() {
    }
}
