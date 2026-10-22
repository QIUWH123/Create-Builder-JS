package net.quiwh.cbjs.kinetics;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.content.MaterialGear;

/** Sequential real propagation gates: no Create config mutation or overlapping temporary rules. */
@GameTestHolder("cbjs")
@PrefixGameTestTemplate(false)
public final class IndependentRpmGameTests {
    private static final BlockPos MOTOR = new BlockPos(0, 1, 1);
    private static final BlockPos LARGE = new BlockPos(1, 1, 1);
    private static final BlockPos SMALL = new BlockPos(1, 2, 2);
    private static final BlockPos SHAFT = new BlockPos(2, 2, 2);

    @GameTest(template = "empty3x3x3", batch = "cbjs_rpm_rules", timeoutTicks = 240)
    public static void ordinaryShaftWithKineticDataUsesOwnRpmLimit(GameTestHelper helper) {
        helper.assertTrue(AllConfigs.server().kinetics.maxRotationSpeed.get() == 256,
                "This gate requires Create's normal 256 RPM cap");
        Block large = scripted(helper, "steel_large_cogwheel");
        Block small = scripted(helper, "steel_cogwheel");
        Block shaft = AllBlocks.SHAFT.get();
        var sequence = helper.startSequence();
        for (Integer limit : new Integer[]{1024, null}) {
            sequence.thenExecute(() -> restoreRulesOnFailure(() -> {
                helper.assertTrue(GearLimitManager.reload(event -> {
                            var modifier = event.modifyKineticData("create:shaft");
                            if (limit == null) modifier.noMaxRPM();
                            else modifier.maxRpm(limit);
                        }),
                        "Could not install shaft kinetic data");
                place(helper, large, small);
                helper.setBlock(SHAFT, shaft.defaultBlockState()
                        .setValue(BlockStateProperties.AXIS, Direction.Axis.X));
                motor(helper).generatedSpeed.setValue(256);
            })).thenExecuteAfter(20, () -> restoreRulesOnFailure(() -> {
                assertDriven(helper, large, small, 256, -512);
                assertShaftSpeed(helper, -512);
                motor(helper).generatedSpeed.setValue(-256);
            })).thenExecuteAfter(20, () -> restoreRulesOnFailure(() -> {
                assertDriven(helper, large, small, -256, 512);
                assertShaftSpeed(helper, 512);
            }));
        }
        sequence.thenExecute(() -> restoreRulesOnFailure(() -> {
            helper.assertTrue(GearLimitManager.reload(event ->
                            event.modifyKineticData("create:shaft").maxRpm(511)),
                    "Could not install stricter shaft rule");
            GearLimitManager.check((KineticBlockEntity) helper.getBlockEntity(SHAFT));
            helper.assertTrue(helper.getBlockState(SHAFT).isAir(),
                    "Shaft survived above its own 511 RPM limit");
            GearLimitManager.reload(event -> event.modifyKineticData("create:shaft").maxStress(8192));
            helper.assertFalse(GearLimitManager.usesIndependentRpmLimit(shaft),
                    "A stress-only shaft rule bypassed Create's RPM cap");
            place(helper, large, small);
            helper.setBlock(SHAFT, shaft.defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.X));
            motor(helper).generatedSpeed.setValue(256);
        })).thenExecuteAfter(20, () -> restoreRulesOnFailure(() -> {
            helper.assertTrue(helper.getBlockState(SMALL).isAir(),
                    "Stress-only shaft kinetic data bypassed Create's global RPM cap");
            helper.assertBlockPresent(shaft, SHAFT);
            GearLimitManager.reload();
        })).thenSucceed();
    }

    private static void restoreRulesOnFailure(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | Error failure) {
            GearLimitManager.reload();
            throw failure;
        }
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_rpm", timeoutTicks = 320)
    public static void scriptedBareAndEncasedGearsUseOwnRpmAndVanillaGearsKeepCreateCap(GameTestHelper helper) {
        helper.assertTrue(AllConfigs.server().kinetics.maxRotationSpeed.get() == 256,
                "This gate requires Create's normal 256 RPM cap; it does not modify server config");
        Block large = scripted(helper, "steel_large_cogwheel");
        String[] targets = {"steel_cogwheel", "andesite_encased_steel_cogwheel", "brass_encased_steel_cogwheel"};
        for (int index = 0; index < targets.length; index++) {
            Block small = scripted(helper, targets[index]);
            var defaults = GearLimitDefaults.buildRules().get(small);
            helper.assertTrue(defaults != null && Integer.valueOf(512).equals(defaults.maxSpeed()),
                    targets[index] + " must have the supplied 512 RPM builder default");
            int start = index * 60;
            helper.runAfterDelay(start + 1, () -> place(helper, large, small));
            helper.runAfterDelay(start + 5, () -> motor(helper).generatedSpeed.setValue(256));
            helper.runAfterDelay(start + 25, () -> assertDriven(helper, large, small, 256, -512));
            helper.runAfterDelay(start + 30, () -> motor(helper).generatedSpeed.setValue(-256));
            helper.runAfterDelay(start + 50, () -> {
                assertDriven(helper, large, small, -256, 512);
                // A synchronous, isolated stricter rule proves the Create Builder JS limit remains active above Create's cap.
                try {
                    helper.assertTrue(GearLimitManager.reload(event ->
                                    event.addKineticDataWithDrops(small, 511, null, null)),
                            "Could not install temporary 511 RPM rule");
                    GearLimitManager.check((KineticBlockEntity) helper.getBlockEntity(SMALL));
                    helper.assertTrue(helper.getBlockState(SMALL).isAir(),
                            "512 RPM survived this gear's own 511 RPM rule");
                    helper.assertBlockPresent(large, LARGE);
                } finally {
                    // Restore native startup defaults and server overrides even when an assertion fails.
                    GearLimitManager.reload();
                }
            });
        }
        // Both mixed directions are required: exemption belongs to the target, not the propagator.
        Block customSmall = scripted(helper, "steel_cogwheel");
        helper.runAfterDelay(181, () -> place(helper, AllBlocks.LARGE_COGWHEEL.get(), customSmall));
        helper.runAfterDelay(185, () -> motor(helper).generatedSpeed.setValue(256));
        helper.runAfterDelay(210, () ->
                assertDriven(helper, AllBlocks.LARGE_COGWHEEL.get(), customSmall, 256, -512));

        helper.runAfterDelay(221, () -> place(helper, large, AllBlocks.COGWHEEL.get()));
        helper.runAfterDelay(225, () -> motor(helper).generatedSpeed.setValue(256));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(helper.getBlockState(LARGE).isAir(),
                    "A custom propagating large gear bypassed the ordinary target's Create RPM cap");
            helper.assertBlockPresent(AllBlocks.COGWHEEL.get(), SMALL);
        });

        helper.runAfterDelay(261, () -> place(helper, AllBlocks.LARGE_COGWHEEL.get(), AllBlocks.COGWHEEL.get()));
        helper.runAfterDelay(265, () -> motor(helper).generatedSpeed.setValue(256));
        helper.runAfterDelay(290, () -> {
            // Create destroys the propagating large gear when its ordinary neighbour would reach 512.
            helper.assertTrue(helper.getBlockState(LARGE).isAir(),
                    "Ordinary Create transmission bypassed the unchanged global RPM cap");
            helper.succeed();
        });
    }

    private static Block scripted(GameTestHelper helper, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("kubejs", path);
        helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(id), "Missing startup gear " + id);
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        helper.assertTrue(block instanceof MaterialGear, id + " is not a Create Builder JS scripted gear");
        return block;
    }

    private static void place(GameTestHelper helper, Block large, Block small) {
        // Disconnect the previous test network before constructing the next one.
        helper.setBlock(MOTOR, Blocks.AIR);
        helper.setBlock(LARGE, Blocks.AIR);
        helper.setBlock(SMALL, Blocks.AIR);
        helper.setBlock(SHAFT, Blocks.AIR);
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(LARGE, large.defaultBlockState().setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        helper.setBlock(SMALL, small.defaultBlockState().setValue(CogWheelBlock.AXIS, Direction.Axis.X));
    }

    private static void assertShaftSpeed(GameTestHelper helper, float speed) {
        helper.assertBlockPresent(AllBlocks.SHAFT.get(), SHAFT);
        helper.assertTrue(((KineticBlockEntity) helper.getBlockEntity(SHAFT)).getSpeed() == speed,
                "Ordinary shaft did not sustain " + speed + " RPM with a kinetic-data rule");
    }

    private static CreativeMotorBlockEntity motor(GameTestHelper helper) {
        return (CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR);
    }

    private static void assertDriven(GameTestHelper helper, Block large, Block small, float source, float output) {
        helper.assertBlockPresent(large, LARGE);
        helper.assertBlockPresent(small, SMALL);
        helper.assertTrue(((KineticBlockEntity) helper.getBlockEntity(LARGE)).getSpeed() == source,
                "Large source gear did not retain " + source + " RPM");
        helper.assertTrue(((KineticBlockEntity) helper.getBlockEntity(SMALL)).getSpeed() == output,
                "Target gear did not sustain transmitted " + output + " RPM under the global 256 cap");
    }

    private IndependentRpmGameTests() {
    }
}
