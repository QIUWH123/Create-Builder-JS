package net.quiwh.cbjs.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.kinetics.GearLimitManager;

import java.nio.file.Files;

/** Integration gates for registration, Create propagation and identity-preserving casing. */
@GameTestHolder("cbjs")
@PrefixGameTestTemplate(false)
public final class CogwheelGameTests {
    private static final String[] MATERIALS = {"wooden", "andesite_alloy", "copper_cased", "copper",
            "cast_iron", "iron_cased", "wrought_iron", "steel_cased", "steel", "high_manganese_steel"};

    @GameTest(template = "empty3x3x3")
    public static void scriptedRegistrySuppliersKeepIdentityAndOwnEntities(GameTestHelper helper) {
        helper.assertTrue(!net.quiwh.cbjs.content.ModCogwheels.baseBlocks().isEmpty(),
                "No scripted cogwheels were collected");
        BlockPos pos = new BlockPos(1, 1, 1);
        for (var supplier : net.quiwh.cbjs.content.ModCogwheels.baseBlocks()) {
            var block = supplier.get();
            var id = ForgeRegistries.BLOCKS.getKey(block);
            helper.assertTrue(id != null && ForgeRegistries.BLOCKS.getValue(id) == block,
                    "Script supplier did not retain its registered block identity");
            helper.assertTrue(block.models() != null && block instanceof EncasableBlock,
                    id + " lost its model metadata or inherited casing behavior");
            helper.setBlock(pos, block.defaultBlockState());
            helper.assertTrue(helper.getBlockEntity(pos) != null
                            && helper.getBlockEntity(pos).getType() == net.quiwh.cbjs.content.ModBlockEntities.COGWHEEL.get()
                            && helper.getBlockEntity(pos).getType().isValid(block.defaultBlockState()),
                    id + " does not have a valid Create Builder JS owned bracketed entity");
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3")
    public static void everyMaterialHasValidOwnedKineticEntities(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        for (String material : MATERIALS) {
            for (String size : new String[]{"cogwheel", "large_cogwheel"}) {
                for (String casing : new String[]{"", "andesite_encased_", "brass_encased_"}) {
                    String id = casing + material + "_" + size;
                    Block block = gear(helper, id);
                    BlockState state = block.defaultBlockState().setValue(CogWheelBlock.AXIS, Direction.Axis.X);
                    helper.setBlock(pos, state);
                    helper.assertTrue(helper.getBlockEntity(pos) instanceof KineticBlockEntity,
                            id + " has no kinetic block entity");
                    helper.assertTrue(helper.getBlockEntity(pos).getType().isValid(state),
                            id + " is absent from its block entity's valid block set");
                    helper.assertTrue(block instanceof net.quiwh.cbjs.content.MaterialGear,
                            id + " was not registered by the scripted gear builder");
                    helper.assertTrue(block instanceof IWrenchable, id + " lost inherited wrench behavior");
                    helper.assertTrue(casing.isEmpty() ? block instanceof CogWheelBlock
                            && block instanceof EncasableBlock : block instanceof EncasedCogwheelBlock,
                            id + " lost inherited Create block behavior");
                    helper.assertTrue(size.equals("large_cogwheel") ? ICogWheel.isLargeCog(state)
                            : ICogWheel.isSmallCog(state), id + " has the wrong Create gear size");
                    helper.assertTrue(ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(helper.getBlockEntity(pos).getType())
                            .getNamespace().equals("cbjs"), id + " uses a foreign block entity type");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 100)
    public static void largeGearDrivesDifferentMaterialSmallGearAtDoubleReverseSpeed(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(0, 1, 1);
        BlockPos largePos = new BlockPos(1, 1, 1);
        BlockPos smallPos = new BlockPos(1, 2, 2);
        helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(largePos, gear(helper, "steel_large_cogwheel").defaultBlockState()
                .setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        helper.setBlock(smallPos, gear(helper, "high_manganese_steel_cogwheel").defaultBlockState()
                .setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        helper.runAfterDelay(5, () -> ((CreativeMotorBlockEntity) helper.getBlockEntity(motorPos))
                .generatedSpeed.setValue(32));
        helper.runAfterDelay(30, () -> {
            KineticBlockEntity large = (KineticBlockEntity) helper.getBlockEntity(largePos);
            KineticBlockEntity small = (KineticBlockEntity) helper.getBlockEntity(smallPos);
            helper.assertTrue(Math.abs(large.getSpeed()) == 32, "Large gear did not receive 32 RPM");
            helper.assertTrue(small.getSpeed() == -large.getSpeed() * 2,
                    "Material gears did not transmit the large-to-small 2:1 reverse ratio");
            helper.succeed();
        });
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 100)
    public static void suppliedKubeJsRulesBreakWoodenGearThroughRealTransmissionTicks(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("kubejs"),
                "This integration test requires the development KubeJS runtime dependency");
        helper.assertTrue(Files.isRegularFile(FMLPaths.GAMEDIR.get()
                        .resolve("kubejs/server_scripts/cbjs_gear_limits.js")),
                "Missing supplied gear-limit script; run installDevGearLimits before this integration test");
        GearLimitManager.reload();

        BlockPos motorPos = new BlockPos(0, 1, 1);
        BlockPos largePos = new BlockPos(1, 1, 1);
        BlockPos woodenPos = new BlockPos(1, 2, 2);
        Block largeBlock = gear(helper, "steel_large_cogwheel");
        Block woodenBlock = gear(helper, "wooden_cogwheel");
        helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(largePos, largeBlock.defaultBlockState()
                .setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        helper.setBlock(woodenPos, woodenBlock.defaultBlockState()
                .setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        helper.runAfterDelay(5, () -> ((CreativeMotorBlockEntity) helper.getBlockEntity(motorPos))
                .generatedSpeed.setValue(32));
        helper.runAfterDelay(25, () -> {
            helper.assertBlockPresent(woodenBlock, woodenPos);
            KineticBlockEntity large = (KineticBlockEntity) helper.getBlockEntity(largePos);
            KineticBlockEntity wooden = (KineticBlockEntity) helper.getBlockEntity(woodenPos);
            helper.assertTrue(Math.abs(large.getSpeed()) == 32 && wooden.getSpeed() == -large.getSpeed() * 2,
                    "Real material transmission did not reach the safe 64 RPM wooden-gear boundary");
            ((CreativeMotorBlockEntity) helper.getBlockEntity(motorPos)).generatedSpeed.setValue(64);
        });
        helper.runAfterDelay(50, () -> {
            helper.assertBlockPresent(largeBlock, largePos);
            helper.assertTrue(Math.abs(((KineticBlockEntity) helper.getBlockEntity(largePos)).getSpeed()) == 64,
                    "Steel large gear did not remain driven at 64 RPM");
            helper.assertTrue(helper.getBlockState(woodenPos).isAir(),
                    "Wooden gear survived transmitted 128 RPM; verify the supplied KubeJS startup script "
                            + "loaded its 64 RPM rule and the kinetic tick mixin is active");
            // Allow the drop to settle onto the test platform during the delayed assertion.
            helper.assertItemEntityCountIs(woodenBlock.asItem(), woodenPos, 4, 1);
            helper.succeed();
        });
    }

    @GameTest(template = "empty3x3x3")
    public static void casingAndUnwrappingPreserveEveryMaterialAndAxis(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos absolutePos = helper.absolutePos(pos);
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.EAST, absolutePos, false);
        for (String material : MATERIALS) {
            for (String size : new String[]{"cogwheel", "large_cogwheel"}) {
                String baseId = material + "_" + size;
                Block base = gear(helper, baseId);
                for (String casing : new String[]{"andesite", "brass"}) {
                    for (Direction.Axis axis : Direction.Axis.values()) {
                        BlockState original = base.defaultBlockState().setValue(CogWheelBlock.AXIS, axis);
                        helper.setBlock(pos, original);
                        ItemStack casingItem = casing.equals("andesite") ? AllBlocks.ANDESITE_CASING.asStack()
                                : AllBlocks.BRASS_CASING.asStack();
                        player.setItemInHand(InteractionHand.MAIN_HAND, casingItem);
                        ((EncasableBlock) base).tryEncase(original, helper.getLevel(), absolutePos,
                                casingItem, player, InteractionHand.MAIN_HAND, hit);
                        BlockState encased = helper.getBlockState(pos);
                        helper.assertTrue(encased.is(gear(helper, casing + "_encased_" + baseId)),
                                baseId + " lost its material during " + casing + " casing");
                        helper.assertTrue(encased.getValue(CogWheelBlock.AXIS) == axis, "Casing changed the gear axis");
                        ((IWrenchable) encased.getBlock()).onSneakWrenched(encased,
                                new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                        BlockState unwrapped = helper.getBlockState(pos);
                        helper.assertTrue(unwrapped.is(base), baseId + " unwrapped into a different gear");
                        helper.assertTrue(unwrapped.getValue(CogWheelBlock.AXIS) == axis,
                                "Unwrapping changed the gear axis");
                        helper.assertTrue(helper.getBlockEntity(pos).getType().isValid(unwrapped),
                                "Unwrapping left an invalid kinetic block entity");
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3")
    public static void pickingEncasedGearsMatchesCreateFacesAndPreservesMaterial(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos absolutePos = helper.absolutePos(pos);
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        for (String material : MATERIALS) {
            for (String size : new String[]{"cogwheel", "large_cogwheel"}) {
                Block base = gear(helper, material + "_" + size);
                for (String casing : new String[]{"andesite", "brass"}) {
                    String id = casing + "_encased_" + material + "_" + size;
                    Block block = gear(helper, id);
                    Block casingBlock = casing.equals("andesite") ? AllBlocks.ANDESITE_CASING.get()
                            : AllBlocks.BRASS_CASING.get();
                    for (Direction.Axis axis : Direction.Axis.values()) {
                        BlockState state = block.defaultBlockState().setValue(CogWheelBlock.AXIS, axis);
                        helper.setBlock(pos, state);
                        for (Direction face : Direction.values()) {
                            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), face,
                                    absolutePos, false);
                            ItemStack picked = block.getCloneItemStack(state, hit, helper.getLevel(),
                                    absolutePos, player);
                            Block expected = face.getAxis() == axis ? casingBlock : base;
                            helper.assertTrue(picked.is(expected.asItem()),
                                    id + " picked the wrong item for axis " + axis + " and face " + face);
                        }
                        ItemStack fallback = block.getCloneItemStack(state, new EntityHitResult(player),
                                helper.getLevel(), absolutePos, player);
                        helper.assertTrue(fallback.is(block.asItem()),
                                id + " lost its encased material for a non-block hit");
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", timeoutTicks = 120)
    public static void encasingRunningGearKeepsBothShaftConnections(GameTestHelper helper) {
        BlockPos motorPos = new BlockPos(0, 1, 1);
        BlockPos gearPos = new BlockPos(1, 1, 1);
        BlockPos shaftPos = new BlockPos(2, 1, 1);
        Block base = gear(helper, "steel_cogwheel");
        helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(gearPos, base.defaultBlockState().setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        helper.setBlock(shaftPos, AllBlocks.SHAFT.getDefaultState()
                .setValue(CogWheelBlock.AXIS, Direction.Axis.X));
        helper.runAfterDelay(5, () -> ((CreativeMotorBlockEntity) helper.getBlockEntity(motorPos))
                .generatedSpeed.setValue(32));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(Math.abs(((KineticBlockEntity) helper.getBlockEntity(shaftPos)).getSpeed()) == 32,
                    "Exposed gear did not drive its output shaft");
            BlockPos absolute = helper.absolutePos(gearPos);
            var player = FakePlayerFactory.getMinecraft(helper.getLevel());
            ItemStack casing = AllBlocks.BRASS_CASING.asStack();
            player.setItemInHand(InteractionHand.MAIN_HAND, casing);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
            ((EncasableBlock) base).tryEncase(helper.getBlockState(gearPos), helper.getLevel(), absolute,
                    casing, player, InteractionHand.MAIN_HAND, hit);
            BlockState encased = helper.getBlockState(gearPos);
            helper.assertTrue(encased.is(gear(helper, "brass_encased_steel_cogwheel")), "Wrong encased material");
            helper.assertTrue(encased.getValue(EncasedCogwheelBlock.TOP_SHAFT)
                    && encased.getValue(EncasedCogwheelBlock.BOTTOM_SHAFT),
                    "Encasing closed an existing input or output shaft");
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(Math.abs(((KineticBlockEntity) helper.getBlockEntity(shaftPos)).getSpeed()) == 32,
                    "Encased gear did not reconnect to its kinetic network");
            BlockPos absolute = helper.absolutePos(gearPos);
            var player = FakePlayerFactory.getMinecraft(helper.getLevel());
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
            BlockState state = helper.getBlockState(gearPos);
            ((IWrenchable) state.getBlock()).onSneakWrenched(state,
                    new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        });
        helper.runAfterDelay(75, () -> {
            helper.assertTrue(helper.getBlockState(gearPos).is(base), "Unwrapping changed the material");
            helper.assertTrue(Math.abs(((KineticBlockEntity) helper.getBlockEntity(shaftPos)).getSpeed()) == 32,
                    "Unwrapped gear did not reconnect to its kinetic network");
            helper.succeed();
        });
    }

    private static Block gear(GameTestHelper helper, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("cbjs", path);
        helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(id), "Missing material gear: " + id);
        return ForgeRegistries.BLOCKS.getValue(id);
    }

    private CogwheelGameTests() {
    }
}
