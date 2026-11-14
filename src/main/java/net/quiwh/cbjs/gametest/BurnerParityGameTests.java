package net.quiwh.cbjs.gametest;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.interaction.ConductorBlockInteractionBehavior;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.content.ScriptedBlazeBurnerBlockEntity;
import net.quiwh.cbjs.fuels.BlazeBurnerFuelManager;

/** Exercises actual Create fuel insertion and independent burner capability consumers. */
@GameTestHolder(CBJS.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BurnerParityGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static BlockState fixture(String name) {
        var id = ResourceLocation.parse("kubejs:burner_parity_" + name);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new AssertionError("Missing burner fixture: " + id);
        return BuiltInRegistries.BLOCK.get(id).defaultBlockState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING);
    }
    private static ScriptedBlazeBurnerBlockEntity place(GameTestHelper helper, String name) {
        helper.setBlock(POS, fixture(name));
        return (ScriptedBlazeBurnerBlockEntity)helper.getBlockEntity(POS);
    }
    private static boolean insert(BlazeBurnerBlockEntity burner, ItemStack stack, boolean simulate) throws Exception {
        var method = BlazeBurnerBlockEntity.class.getDeclaredMethod("tryUpdateFuel", ItemStack.class, boolean.class, boolean.class);
        method.setAccessible(true);
        return (boolean)method.invoke(burner, stack, false, simulate);
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_burner")
    public static void perBurnerServerFuelsOverrideStartupAndBuilderDefaults(GameTestHelper helper) throws Exception {
        var burner = place(helper, "layered");
        var stack = new ItemStack(Items.COAL);
        var rule = BlazeBurnerFuelManager.find(burner.getBlockState().getBlock(), stack);
        helper.assertTrue(rule != null && rule.burnTicks() == 300, "Server burnerFuel did not override startup and builder fuel layers");
        helper.assertTrue(insert(burner, stack, true), "Native burner rejected a simulated registered custom fuel");
        helper.assertTrue(burner.getRemainingBurnTime() == 0, "Fuel simulation mutated the burner timer");
        helper.assertTrue(insert(burner, stack, false), "Native burner rejected registered custom fuel");
        helper.assertTrue(burner.getRemainingBurnTime() == 300, "Native insertion did not use the selected per-burner duration");
        helper.assertTrue(burner.getHeatLevelFromBlock() == HeatLevel.KINDLED, "Native regular fuel heat changed unexpectedly");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_burner")
    public static void disabledNativeFallbackRejectsFurnaceAndSuperheatedDefaults(GameTestHelper helper) throws Exception {
        var burner = place(helper, "restricted");
        helper.assertTrue(!insert(burner, new ItemStack(Items.STICK), true), "useVanillaFuels(false) allowed native furnace fuel");
        helper.assertTrue(!insert(burner, AllItems.BLAZE_CAKE.asStack(), true), "useVanillaFuels(false) allowed native superheated data-map/tag fuel");
        helper.assertTrue(!insert(burner, new ItemStack(Items.IRON_NUGGET), true), "useGlobalFuels(false) allowed shared CBJS fuel");
        helper.assertTrue(!burner.isCreativeFuel(AllItems.CREATIVE_BLAZE_CAKE.asStack()), "Creative fuel capability did not reach the native fuel predicate");
        helper.assertTrue(insert(burner, new ItemStack(Items.DIAMOND), false), "Explicit per-burner super fuel was lost when native fallback was disabled");
        helper.assertTrue(burner.getHeatLevelFromBlock() == HeatLevel.KINDLED, "Configured maxHeatLevel did not clamp native superheated fuel");
        helper.assertTrue(burner.getHeatLevelForRender() == HeatLevel.KINDLED, "Render heat did not obey the configured maximum");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_burner")
    public static void independentHeatFanAndConductorCapabilitiesReachCreate(GameTestHelper helper) {
        var restricted = fixture("restricted").setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING);
        var enabled = fixture("layered").setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED);
        helper.setBlock(POS, restricted);
        var level = helper.getLevel(); var pos = helper.absolutePos(POS);
        helper.assertTrue(BasinBlockEntity.getHeatLevelOf(restricted) == HeatLevel.NONE, "canHeatBasin(false) left basin heat enabled");
        helper.assertTrue(BoilerHeater.findHeat(level, pos, restricted) == BoilerHeater.NO_HEAT, "canHeatBoiler(false) left boiler heat enabled");
        helper.assertTrue(!AllFanProcessingTypes.BLASTING.isValidAt(level, pos), "canProcessFan(false) left blasting enabled");
        helper.assertTrue(!(MovingInteractionBehaviour.REGISTRY.get(restricted.getBlock()) instanceof ConductorBlockInteractionBehavior),
                "Generic burner retained native train control by default");
        helper.setBlock(POS, enabled);
        helper.assertTrue(BasinBlockEntity.getHeatLevelOf(enabled) == HeatLevel.KINDLED, "Enabling visual options should preserve basin heating");
        helper.assertTrue(BoilerHeater.findHeat(level, pos, enabled) > BoilerHeater.NO_HEAT, "Enabled boiler heating was lost");
        helper.assertTrue(MovingInteractionBehaviour.REGISTRY.get(enabled.getBlock()) instanceof ConductorBlockInteractionBehavior,
                "Explicitly enabled train control did not reach native registry");
        helper.succeed();
    }
    private BurnerParityGameTests() { }
}
