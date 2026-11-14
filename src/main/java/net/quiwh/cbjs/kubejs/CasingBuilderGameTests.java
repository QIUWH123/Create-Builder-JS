package net.quiwh.cbjs.kubejs;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.content.CustomCasingBlockEntities;
import net.quiwh.cbjs.content.CustomEncasedCogwheelBlock;
import net.quiwh.cbjs.content.CustomEncasedPipeBlock;
import net.quiwh.cbjs.content.CustomEncasedShaftBlock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Runtime contracts that do not require a custom casing in the user's startup scripts. */
@GameTestHolder(CBJS.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CasingBuilderGameTests {
    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void encasingFlagsSelectOnlyRequestedCompanions(GameTestHelper helper) {
        var casing = new CasingBuilder(id("example", "test_casing"));
        int blocksBefore = RegistryObjectStorage.BLOCK.objects.size();
        int itemsBefore = RegistryObjectStorage.ITEM.objects.size();
        helper.assertTrue(casing.planVariants(List.of()).isEmpty(), "ordinary casings must not add companions");
        helper.assertTrue(casing.cogwheelCasing() == casing && casing.shaftCasing() == casing
                        && casing.fluidCasing() == casing,
                "each casing capability must support method chaining");
        for (boolean cogwheels : new boolean[]{false, true}) {
            for (boolean shafts : new boolean[]{false, true}) {
                for (boolean pipes : new boolean[]{false, true}) {
                    casing.cogwheelCasing(cogwheels).shaftCasing(shafts).fluidCasing(pipes);
                    Set<String> expected = new HashSet<>();
                    if (cogwheels) {
                        expected.add("example:test_encased_cogwheel");
                        expected.add("example:test_encased_large_cogwheel");
                    }
                    if (shafts) expected.add("example:test_encased_shaft");
                    if (pipes) expected.add("example:test_encased_fluid_pipe");
                    assertPaths(helper, casing.planVariants(List.of()), expected);
                }
            }
        }
        helper.assertTrue(RegistryObjectStorage.BLOCK.objects.size() == blocksBefore
                        && RegistryObjectStorage.ITEM.objects.size() == itemsBefore,
                "planning casing companions must not mutate live registries");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void materialCompanionsKeepNamespaceSizeAndBaseIdentity(GameTestHelper helper) {
        var small = new CogwheelBuilder(id("first", "copper_cogwheel"), false);
        var large = new CogwheelBuilder(id("second", "copper_large_cogwheel"), true);
        var casing = new CasingBuilder(id("example", "deep/custom_casing")).cogwheelCasing().shaftCasing().fluidCasing();
        var variants = casing.planVariants(List.of(small, large));
        assertPaths(helper, variants, Set.of(
                "example:deep/custom_encased_shaft", "example:deep/custom_encased_cogwheel",
                "example:deep/custom_encased_large_cogwheel", "example:deep/custom_encased_fluid_pipe",
                "example:deep/custom_encased_copper_cogwheel",
                "example:deep/custom_encased_copper_large_cogwheel"));
        for (var variant : variants) {
            helper.assertTrue(variant.casing == casing, "companion lost its source casing builder");
            if (variant.id.toString().equals("example:deep/custom_encased_copper_cogwheel")) {
                helper.assertTrue(variant.gear == small && variant.kind == CasingVariantBuilder.Kind.COGWHEEL,
                        "small material gear was planned as a large gear");
            } else if (variant.id.toString().equals("example:deep/custom_encased_copper_large_cogwheel")) {
                helper.assertTrue(variant.gear == large && variant.kind == CasingVariantBuilder.Kind.LARGE_COGWHEEL,
                        "large material gear was planned as a small gear");
            } else {
                helper.assertTrue(variant.gear == null, "companion substituted a material gear builder");
            }
        }
        for (boolean cogwheels : new boolean[]{false, true}) {
            for (boolean shafts : new boolean[]{false, true}) {
                for (boolean pipes : new boolean[]{false, true}) {
                    casing.cogwheelCasing(cogwheels).shaftCasing(shafts).fluidCasing(pipes);
                    Set<String> expected = new HashSet<>();
                    if (cogwheels) {
                        expected.add("example:deep/custom_encased_cogwheel");
                        expected.add("example:deep/custom_encased_large_cogwheel");
                        expected.add("example:deep/custom_encased_copper_cogwheel");
                        expected.add("example:deep/custom_encased_copper_large_cogwheel");
                    }
                    if (shafts) expected.add("example:deep/custom_encased_shaft");
                    if (pipes) expected.add("example:deep/custom_encased_fluid_pipe");
                    assertPaths(helper, casing.planVariants(List.of(small, large)), expected);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void companionNameCollisionsAreRejectedBeforeRegistration(GameTestHelper helper) {
        var casing = new CasingBuilder(id("kubejs", "stainless_steel_casing")).cogwheelCasing();
        var first = new CogwheelBuilder(id("first", "copper_cogwheel"), false);
        var second = new CogwheelBuilder(id("second", "copper_cogwheel"), false);
        int blocksBefore = RegistryObjectStorage.BLOCK.objects.size();
        int itemsBefore = RegistryObjectStorage.ITEM.objects.size();
        try {
            casing.planVariants(List.of(first, second));
            helper.fail("colliding companion IDs were accepted");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue(expected.getMessage().contains("kubejs:stainless_steel_encased_copper_cogwheel"),
                    "collision message omitted the conflicting generated ID");
            helper.assertTrue(expected.getMessage().contains("first:copper_cogwheel")
                            && expected.getMessage().contains("second:copper_cogwheel"),
                    "collision message omitted the source material IDs");
        }
        assertPaths(helper, casing.cogwheelCasing(false).shaftCasing().fluidCasing().planVariants(List.of(first, second)),
                Set.of("kubejs:stainless_steel_encased_shaft", "kubejs:stainless_steel_encased_fluid_pipe"));
        casing.cogwheelCasing().shaftCasing(false).fluidCasing(false);
        try {
            casing.planVariants(List.of(new CogwheelBuilder(id("other", "cogwheel"), false)));
            helper.fail("material gear collision with stock cogwheel ID was accepted");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue(expected.getMessage().contains("kubejs:stainless_steel_encased_cogwheel"),
                    "stock companion collision omitted its generated ID");
        }
        helper.assertTrue(RegistryObjectStorage.BLOCK.objects.size() == blocksBefore
                        && RegistryObjectStorage.ITEM.objects.size() == itemsBefore,
                "collision mutated live block or item registrations");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void startupCasingCanPlanAllRegisteredMaterialGears(GameTestHelper helper) {
        var casingId = id("cbjs_test", "fixture_casing");
        var registeredCasing = RegistryObjectStorage.BLOCK.objects.get(casingId);
        helper.assertTrue(registeredCasing instanceof CasingBuilder, "Missing startup casing fixture " + casingId);
        var casing = (CasingBuilder) registeredCasing;

        var gears = new java.util.ArrayList<CogwheelBuilder>();
        for (var builder : RegistryObjectStorage.BLOCK)
            if (builder instanceof CogwheelBuilder gear && gear.base == null) gears.add(gear);

        var variants = casing.planVariants(gears);
        var kubejsWooden = id("kubejs", "wooden_cogwheel");
        var fixtureWooden = id("cbjs", "legacy_fixture_wooden_cogwheel");
        helper.assertTrue(variants.stream().anyMatch(variant -> variant.gear != null && variant.gear.id.equals(kubejsWooden)),
                "the active KubeJS material gear was omitted from casing variants");
        helper.assertTrue(variants.stream().anyMatch(variant -> variant.gear != null && variant.gear.id.equals(fixtureWooden)),
                "the unique GameTest material gear must coexist with user examples in casing variants");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void casingRetainsOrdinaryKubeJsBlockHooks(GameTestHelper helper) {
        var fixtureId = id("cbjs_test", "hooks_casing");
        var registered = RegistryObjectStorage.BLOCK.objects.get(fixtureId);
        helper.assertTrue(registered instanceof CasingBuilder, "Missing startup casing hook fixture " + fixtureId);
        var builder = (CasingBuilder) registered;
        Block block = builder.get();
        var flag = (BooleanProperty) block.getStateDefinition().getProperty("script_flag");
        helper.assertTrue(block.defaultBlockState().getValue(flag), "casing ignored ordinary defaultState callback");
        helper.assertTrue(!block.defaultBlockState().rotate(Rotation.CLOCKWISE_90).getValue(flag),
                "casing ignored ordinary rotateState callback");
        helper.assertTrue(block.defaultBlockState().getDestroySpeed(helper.getLevel(), BlockPos.ZERO) == 7,
                "casing ignored ordinary hardness setting");
        helper.assertTrue(builder.itemBuilder.maxStackSize == 16, "casing ignored item settings after noItem/item");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void stockCompanionsCreateTheirOwnBlockEntityTypes(GameTestHelper helper) {
        var fixtureId = id("cbjs_test", "fixture_casing");
        var registered = RegistryObjectStorage.BLOCK.objects.get(fixtureId);
        helper.assertTrue(registered instanceof CasingBuilder, "Missing startup casing fixture " + fixtureId);
        var casing = (CasingBuilder) registered;
        for (var variant : casing.planVariants(List.of())) {
            Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(variant.id);
            var expected = switch (variant.kind) {
                case SHAFT -> CustomCasingBlockEntities.ENCASED_SHAFT.get();
                case COGWHEEL -> CustomCasingBlockEntities.ENCASED_COGWHEEL.get();
                case LARGE_COGWHEEL -> CustomCasingBlockEntities.ENCASED_LARGE_COGWHEEL.get();
                case PIPE -> CustomCasingBlockEntities.ENCASED_FLUID_PIPE.get();
            };
            helper.assertTrue(block instanceof EntityBlock, variant.id + " has no block entity factory");
            var entity = ((EntityBlock) block).newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            helper.assertTrue(entity != null && entity.getType() == expected,
                    variant.id + " created a foreign block entity type");
            if (block instanceof CustomEncasedCogwheelBlock cog) {
                helper.assertTrue(cog.isLargeCog() == (variant.kind == CasingVariantBuilder.Kind.LARGE_COGWHEEL),
                        variant.id + " lost its requested cogwheel size");
                helper.assertTrue(!block.defaultBlockState().canOcclude(),
                        variant.id + " blocks light to its moving parts");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void customKineticCasingsKeepConnectorsAndPickTheSuppliedCasing(GameTestHelper helper) {
        var casing = fixture(helper, "fixture_casing");
        var shaft = (CustomEncasedShaftBlock) fixture(helper, "fixture_encased_shaft");
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (Direction.Axis axis : Direction.Axis.values()) {
            var shaftState = shaft.defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
            for (Direction face : Direction.values()) {
                var hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
                helper.assertTrue(shaft.hasShaftTowards(helper.getLevel(), pos, shaftState, face)
                                == (face.getAxis() == axis), "custom shaft lost its axis connector");
                var picked = shaft.getCloneItemStack(shaftState, hit, helper.getLevel(), pos, player);
                Block expected = face.getAxis() == axis ? AllBlocks.SHAFT.get() : casing;
                helper.assertTrue(picked.is(expected.asItem()), "custom shaft picked a different casing or base");
            }
            for (boolean large : new boolean[]{false, true}) {
                var cog = (CustomEncasedCogwheelBlock) fixture(helper,
                        large ? "fixture_encased_large_cogwheel" : "fixture_encased_cogwheel");
                var state = cog.defaultBlockState().setValue(BlockStateProperties.AXIS, axis)
                        .setValue(EncasedCogwheelBlock.TOP_SHAFT, true)
                        .setValue(EncasedCogwheelBlock.BOTTOM_SHAFT, false);
                for (Direction face : Direction.values()) {
                    var hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
                    boolean connected = face.getAxis() == axis && face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                    helper.assertTrue(cog.hasShaftTowards(helper.getLevel(), pos, state, face) == connected,
                            "custom cogwheel lost independent shaft openings");
                    Block base = large ? AllBlocks.LARGE_COGWHEEL.get() : AllBlocks.COGWHEEL.get();
                    Block expected = face.getAxis() == axis ? casing : base;
                    helper.assertTrue(cog.getCloneItemStack(state, hit, helper.getLevel(), pos, player).is(expected.asItem()),
                            "custom cogwheel picked a different casing or gear size");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void customPipeSchedulesTransportUpdatesAtOpenConnections(GameTestHelper helper) {
        var pipe = (CustomEncasedPipeBlock) fixture(helper, "fixture_encased_fluid_pipe");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var state = pipe.defaultBlockState().setValue(BlockStateProperties.EAST, true);
        helper.assertTrue(!helper.getLevel().getBlockTicks().hasScheduledTick(pos, pipe),
                "test started with a pending custom pipe tick");
        pipe.neighborChanged(state, helper.getLevel(), pos, Blocks.AIR, pos.west(), false);
        helper.assertTrue(!helper.getLevel().getBlockTicks().hasScheduledTick(pos, pipe),
                "closed pipe connection scheduled transport work");
        pipe.neighborChanged(state, helper.getLevel(), pos, Blocks.AIR, pos.east(), false);
        helper.assertTrue(helper.getLevel().getBlockTicks().hasScheduledTick(pos, pipe),
                "custom casing suppressed an open pipe connection's transport update");
        helper.succeed();
    }

    private static void assertPaths(GameTestHelper helper, List<CasingVariantBuilder> variants, Set<String> expected) {
        Set<String> actual = variants.stream().map(variant -> variant.id.toString()).collect(Collectors.toSet());
        helper.assertTrue(actual.equals(expected) && variants.size() == expected.size(),
                "wrong casing companions; expected " + expected + ", got " + actual);
    }

    private static Block fixture(GameTestHelper helper, String path) {
        var fixtureId = id("cbjs_test", path);
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(fixtureId);
        helper.assertTrue(block != Blocks.AIR, "Missing startup casing fixture " + fixtureId);
        return block;
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private CasingBuilderGameTests() {
    }
}
