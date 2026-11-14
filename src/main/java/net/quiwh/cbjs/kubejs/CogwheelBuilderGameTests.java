package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.script.data.GeneratedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.quiwh.cbjs.CBJS;
import java.util.HashMap;

/** Runtime coverage for hooks requiring Forge/KubeJS bootstrap and mixins. */
@GameTestHolder(CBJS.MOD_ID)
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class CogwheelBuilderGameTests {
    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void omittedLimitsAndEncasedOptOut(GameTestHelper helper) {
        var defaults = new CogwheelBuilder(ResourceLocation.parse("example:default_limits"), false);
        helper.assertTrue(defaults.maxRpm == null && !defaults.independentRpmLimit,
                "omitted maxRpm must inherit Create's cap");
        helper.assertTrue(defaults.maxStress == null, "omitted maxStress must have no cap");
        helper.assertTrue(defaults.encasedVariantsEnabled(), "encased companions must be enabled by default");
        var optedOut = new CogwheelBuilder(ResourceLocation.parse("example:no_encased_gear"), true)
                .noEncasedVariants();
        helper.assertTrue(!optedOut.encasedVariantsEnabled(), "encased opt-out must disable companions");
        var casing = new CasingBuilder(ResourceLocation.parse("example:policy_casing")).cogwheelCasing();
        var variants = casing.planVariants(java.util.List.of(defaults, optedOut));
        helper.assertTrue(variants.size() == 3, "custom casing must create vanilla and enabled material companions");
        helper.assertTrue(variants.stream().anyMatch(variant -> variant.gear == defaults)
                        && variants.stream().noneMatch(variant -> variant.gear == optedOut),
                "custom casing ignored material cogwheel opt-out");
        defaults.maxRpm(512).maxStress(4096);
        helper.assertTrue(defaults.maxRpm == 512 && defaults.independentRpmLimit && defaults.maxStress == 4096,
                "explicit RPM must use the material's independent cap");
        defaults.noMaxRPM();
        helper.assertTrue(defaults.maxRpm == null && defaults.independentRpmLimit,
                "noMaxRPM must ignore Create's cap");
        defaults.maxRpm(0);
        helper.assertTrue(defaults.maxRpm == 0 && defaults.independentRpmLimit,
                "zero must remain a valid independent cap after noMaxRPM");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void cogwheelItemsMaterializeAndReuseTheirBlock(GameTestHelper helper) {
        var id = ResourceLocation.parse("cbjs_test:hooks_gear");
        var registered = dev.latvian.mods.kubejs.registry.RegistryObjectStorage.BLOCK.objects.get(id);
        helper.assertTrue(registered instanceof CogwheelBuilder, "Missing registered cogwheel fixture " + id);
        var builder = (CogwheelBuilder) registered;
        var item = builder.itemBuilder.get();
        helper.assertTrue(item instanceof CogwheelBlockItem, "material cogwheel lost Create's specialized placement item");
        var block = builder.get();
        helper.assertTrue(block != null && ((net.minecraft.world.item.BlockItem) item).getBlock() == block,
                "Create cogwheel item was not bound to the materialized block");
        helper.assertTrue(builder.createTransformedObject() == block,
                "later block registry creation replaced the block already bound to its item");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void customShapesAreBuiltOnceAndKeepNativeDefaults(GameTestHelper helper) {
        VoxelShape expected = Shapes.or(Shapes.box(0, 0, 0, 0.25, 1, 1),
                Shapes.box(0.75, 0, 0, 1, 0.5, 1));
        for (boolean large : new boolean[]{false, true}) {
            for (boolean encased : new boolean[]{false, true}) {
                Block block = shapeBlock(helper, "shape_", large, encased);
                var state = block.defaultBlockState();
                VoxelShape first = state.getShape(helper.getLevel(), BlockPos.ZERO, CollisionContext.empty());
                helper.assertTrue(!Shapes.joinIsNotEmpty(first, expected, BooleanOp.NOT_SAME),
                        "custom gear shape lost or added volume");
                for (Direction.Axis axis : Direction.Axis.values()) {
                    var rotated = state.setValue(BlockStateProperties.AXIS, axis);
                    helper.assertTrue(rotated.getShape(helper.getLevel(), BlockPos.ZERO, CollisionContext.empty()) == first,
                            "custom gear shape is rebuilt for each query or state");
                }

                Block plain = shapeBlock(helper, "native_shape_", large, encased);
                var nativeBlock = encased
                        ? large ? com.simibubi.create.AllBlocks.ANDESITE_ENCASED_LARGE_COGWHEEL.get()
                                : com.simibubi.create.AllBlocks.ANDESITE_ENCASED_COGWHEEL.get()
                        : large ? com.simibubi.create.AllBlocks.LARGE_COGWHEEL.get()
                                : com.simibubi.create.AllBlocks.COGWHEEL.get();
                for (Direction.Axis axis : Direction.Axis.values()) {
                    VoxelShape actual = plain.defaultBlockState().setValue(BlockStateProperties.AXIS, axis).getShape(
                            helper.getLevel(), BlockPos.ZERO, CollisionContext.empty());
                    VoxelShape original = nativeBlock.defaultBlockState().setValue(BlockStateProperties.AXIS, axis).getShape(
                            helper.getLevel(), BlockPos.ZERO, CollisionContext.empty());
                    helper.assertTrue(!Shapes.joinIsNotEmpty(actual, original, BooleanOp.NOT_SAME),
                            "empty custom shape replaced Create's native geometry");
                }
            }
        }
        helper.succeed();
    }

    private static Block shapeBlock(GameTestHelper helper, String prefix, boolean large, boolean encased) {
        var id = ResourceLocation.fromNamespaceAndPath("cbjs_test",
                (encased ? "andesite_encased_" : "") + prefix + (large ? "large" : "small"));
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
        helper.assertTrue(block instanceof net.quiwh.cbjs.content.MaterialGear, "Missing startup shape fixture " + id);
        return block;
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void namedModelsKeepShaftDefaultsIndependent(GameTestHelper helper) {
        var builder = new CogwheelBuilder(ResourceLocation.parse("example:custom_gear"), false);
        builder.model("cogwheel", "example:block/gear_only");
        helper.assertTrue(builder.models().shaft().equals(net.quiwh.cbjs.content.GearModelSet.COGWHEEL_SHAFT),
                "custom gear keeps native full shaft");
        builder.model("shaft", "example:block/full_shaft");
        helper.assertTrue(builder.models().shaft().equals(ResourceLocation.parse("example:block/custom_gear/shaft")),
                "full shaft override resolves its generated alias");
        helper.assertTrue(builder.models().shaftHalf().equals(net.quiwh.cbjs.content.GearModelSet.SHAFT_HALF),
                "full shaft override does not replace native half shaft");
        builder.item(item -> item.parentModel(ResourceLocation.parse("example:item/gear")));
        helper.assertTrue("example:item/gear".equals(CBJSAssets.parent(builder.itemBuilder)),
                "item model follows native KubeJS item customization");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void encasedCogwheelsDoNotBlockTheirOwnLight(GameTestHelper helper) {
        int checked = 0;
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (!(block instanceof net.quiwh.cbjs.content.MaterialEncasedCogwheelBlock)) continue;
            for (var axis : Direction.Axis.values()) {
                var state = block.defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
                helper.assertTrue(!state.canOcclude(), "encased cogwheel must not occlude light: " + block);
                helper.assertTrue(state.getLightBlock(helper.getLevel(), BlockPos.ZERO) <= 1,
                        "encased cogwheel must admit light to its rotating parts: " + block);
            }
            checked++;
        }
        helper.assertTrue(checked > 0, "no scripted encased cogwheels found");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void ordinaryBuilderHooksAndGeneratedAssets(GameTestHelper helper) {
        var fixtureId = ResourceLocation.parse("cbjs_test:hooks_gear");
        var registered = dev.latvian.mods.kubejs.registry.RegistryObjectStorage.BLOCK.objects.get(fixtureId);
        helper.assertTrue(registered instanceof CogwheelBuilder, "Missing startup gear hook fixture " + fixtureId);
        var builder = (CogwheelBuilder) registered;
        var block = builder.get();
        var flag = (BooleanProperty) block.getStateDefinition().getProperty("script_flag");
        helper.assertTrue(block.defaultBlockState().getValue(flag), "ordinary defaultState callback");
        helper.assertTrue(block.defaultBlockState().rotate(Rotation.CLOCKWISE_90)
                .getValue(BlockStateProperties.AXIS) == Direction.Axis.Z, "ordinary rotateState callback");
        helper.assertTrue(block.defaultBlockState().getDestroySpeed(helper.getLevel(), BlockPos.ZERO) == 7,
                "ordinary hardness");
        helper.assertTrue(builder.itemBuilder.maxStackSize == 16, "ordinary item settings after noItem/item");
        try { builder.maxRpm(-1); throw new AssertionError("negative RPM accepted"); }
        catch (IllegalArgumentException expected) { }
        try { builder.maxStress(-1); throw new AssertionError("negative stress accepted"); }
        catch (IllegalArgumentException expected) { }
        var generated = new HashMap<ResourceLocation, GeneratedData>();
        builder.generateAssets(new KubeAssetGenerator() {
            @Override public dev.latvian.mods.kubejs.util.RegistryAccessContainer getRegistries() { return dev.latvian.mods.kubejs.util.RegistryAccessContainer.current; }
            @Override public void add(GeneratedData data) { generated.put(data.id(), data); }
            @Override public GeneratedData getGenerated(ResourceLocation id) { return generated.get(id); }
        });
        helper.assertTrue(generated.size() == 7, "four moving parts, static, item and blockstate");
        for (String model : new String[]{"cogwheel_shaftless", "large_cogwheel_shaftless", "shaft", "shaft_half"}) {
            var data = generated.get(fixtureId.withPath(path -> "models/block/" + path + "/" + model + ".json"));
            String json = new String(data.data().get(), java.nio.charset.StandardCharsets.UTF_8);
            helper.assertTrue(json.contains("example:block/custom_"), "custom textures reach " + model);
        }
        helper.succeed();
    }
}
