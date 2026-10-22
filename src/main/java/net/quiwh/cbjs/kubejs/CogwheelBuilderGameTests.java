package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
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
import net.minecraftforge.gametest.GameTestHolder;
import net.quiwh.cbjs.CBJS;
import java.util.HashMap;

/** Runtime coverage for hooks requiring Forge/KubeJS bootstrap and mixins. */
@GameTestHolder(CBJS.MOD_ID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
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
    public static void customShapesAreBuiltOnceAndKeepNativeDefaults(GameTestHelper helper) {
        VoxelShape expected = Shapes.or(Shapes.box(0, 0, 0, 0.25, 1, 1),
                Shapes.box(0.75, 0, 0, 1, 0.5, 1));
        for (boolean large : new boolean[]{false, true}) {
            for (boolean encased : new boolean[]{false, true}) {
                var builder = new CogwheelBuilder(ResourceLocation.parse("example:shape_test"), large);
                builder.box(0, 0, 0, 4, 16, 16).box(12, 0, 0, 16, 8, 16);
                Block block = shapeBlock(builder, encased);
                var state = block.defaultBlockState();
                VoxelShape first = block.getShape(state, helper.getLevel(), BlockPos.ZERO, CollisionContext.empty());
                helper.assertTrue(!Shapes.joinIsNotEmpty(first, expected, BooleanOp.NOT_SAME),
                        "custom gear shape lost or added volume");
                for (Direction.Axis axis : Direction.Axis.values()) {
                    var rotated = state.setValue(BlockStateProperties.AXIS, axis);
                    helper.assertTrue(block.getShape(rotated, helper.getLevel(), BlockPos.ZERO, CollisionContext.empty()) == first,
                            "custom gear shape is rebuilt for each query or state");
                }

                var defaults = new CogwheelBuilder(ResourceLocation.parse("example:native_shape_test"), large);
                Block plain = shapeBlock(defaults, encased);
                var nativeBlock = encased
                        ? large ? com.simibubi.create.AllBlocks.ANDESITE_ENCASED_LARGE_COGWHEEL.get()
                                : com.simibubi.create.AllBlocks.ANDESITE_ENCASED_COGWHEEL.get()
                        : large ? com.simibubi.create.AllBlocks.LARGE_COGWHEEL.get()
                                : com.simibubi.create.AllBlocks.COGWHEEL.get();
                for (Direction.Axis axis : Direction.Axis.values()) {
                    VoxelShape actual = plain.getShape(plain.defaultBlockState().setValue(BlockStateProperties.AXIS, axis),
                            helper.getLevel(), BlockPos.ZERO, CollisionContext.empty());
                    VoxelShape original = nativeBlock.getShape(nativeBlock.defaultBlockState().setValue(BlockStateProperties.AXIS, axis),
                            helper.getLevel(), BlockPos.ZERO, CollisionContext.empty());
                    helper.assertTrue(!Shapes.joinIsNotEmpty(actual, original, BooleanOp.NOT_SAME),
                            "empty custom shape replaced Create's native geometry");
                }
            }
        }
        helper.succeed();
    }

    private static Block shapeBlock(CogwheelBuilder builder, boolean encased) {
        return encased ? new ScriptedEncasedCogwheelBlock(builder, () -> Blocks.IRON_BLOCK,
                () -> (net.quiwh.cbjs.content.MaterialCogwheelBlock) builder.get()) : builder.createObject();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void oneModelPathSelectsMatchingGearParts(GameTestHelper helper) {
        var builder = new CogwheelBuilder(ResourceLocation.fromNamespaceAndPath("example", "custom_gear"), false);
        builder.model("example:block/custom").model("kubejs:block/cogwheel/steel/large_cogwheel");
        helper.assertTrue("steel".equals(builder.importedModel), "full model path selects preset for either size");
        helper.assertTrue(builder.model.isEmpty() && builder.itemBuilder.parentModel.isEmpty(), "known model clears static parents");
        var explicit = new com.google.gson.JsonObject(); explicit.addProperty("parent", "example:block/explicit");
        builder.modelJson = explicit;
        builder.model("kubejs:block/cogwheel/copper/cogwheel");
        helper.assertTrue(builder.modelJson == explicit, "explicit model JSON retained");
        builder.model("example:block/custom");
        helper.assertTrue("copper".equals(builder.importedModel) && "example:block/custom".equals(builder.model)
                && "example:block/custom".equals(builder.itemBuilder.parentModel), "ordinary static model preserves partial preset");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs")
    public static void encasedCogwheelsDoNotBlockTheirOwnLight(GameTestHelper helper) {
        int checked = 0;
        for (var block : net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValues()) {
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
        var builder = new CogwheelBuilder(ResourceLocation.fromNamespaceAndPath("example", "test_gear"), false);
        var flag = BooleanProperty.create("script_flag");
        builder.maxRpm(0);
        builder.hardness(7).resistance(19).property(flag)
                .defaultState(event -> event.set(flag, true))
                .rotateState(event -> event.set(BlockStateProperties.AXIS, "z"));
        builder.replaceTexture("gear", "example:block/custom_gear")
                .replaceTexture("shaft", "example:block/custom_shaft")
                .replaceTexture("shaft_top", "example:block/custom_top");
        builder.noItem().item(item -> item.maxStackSize(16));
        var block = builder.createObject();
        helper.assertTrue(block.defaultBlockState().getValue(flag), "ordinary defaultState callback");
        helper.assertTrue(block.rotate(block.defaultBlockState(), Rotation.CLOCKWISE_90)
                .getValue(BlockStateProperties.AXIS) == Direction.Axis.Z, "ordinary rotateState callback");
        helper.assertTrue(block.defaultBlockState().getDestroySpeed(helper.getLevel(), BlockPos.ZERO) == 7,
                "ordinary hardness");
        helper.assertTrue(builder.itemBuilder.maxStackSize == 16, "ordinary item settings after noItem/item");
        try { builder.maxRpm(-1); throw new AssertionError("negative RPM accepted"); }
        catch (IllegalArgumentException expected) { }
        try { builder.maxStress(-1); throw new AssertionError("negative stress accepted"); }
        catch (IllegalArgumentException expected) { }
        var generated = new HashMap<ResourceLocation, GeneratedData>();
        builder.generateAssetJsons(new AssetJsonGenerator(generated));
        helper.assertTrue(generated.size() == 7, "four moving parts, static, item and blockstate");
        for (String model : new String[]{"cogwheel_shaftless", "large_cogwheel_shaftless", "shaft", "shaft_half"}) {
            var data = generated.get(ResourceLocation.fromNamespaceAndPath("example", "models/block/test_gear/" + model + ".json"));
            String json = new String(data.data().get(), java.nio.charset.StandardCharsets.UTF_8);
            helper.assertTrue(json.contains("example:block/custom_"), "custom textures reach " + model);
        }
        helper.succeed();
    }
}
