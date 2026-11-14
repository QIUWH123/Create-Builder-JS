package net.quiwh.cbjs.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.content.ScriptedCreativeMotorBlock;
import net.quiwh.cbjs.content.ScriptedGaugeBlock;
import net.quiwh.cbjs.content.ScriptedSpeedControllerBlock;

/** Exercises real peripheral discovery while remaining loadable without CC:Tweaked. */
@GameTestHolder(CBJS.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ComputerCraftCapabilitiesGameTests {
    private static final String PERIPHERALS = "com.simibubi.create.compat.computercraft.implementation.peripherals.";

    @GameTest(template = "empty3x3x3", batch = "cbjs_registration")
    @SuppressWarnings("unchecked")
    public static void scriptedMachinesExposeNativePeripherals(GameTestHelper helper) {
        if (!ModList.get().isLoaded("computercraft")) {
            helper.succeed();
            return;
        }
        var id = ResourceLocation.parse("computercraft:peripheral");
        var capability = (BlockCapability<Object, Direction>) BlockCapability.getAll().stream()
                .filter(candidate -> candidate.name().equals(id)).findFirst().orElseThrow();
        Map<String, Block> families = new LinkedHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof ScriptedGaugeBlock gauge)
                families.putIfAbsent(gauge.sourceBuilder().stress ? "StressGaugePeripheral" : "SpeedGaugePeripheral", block);
            else if (block instanceof ScriptedSpeedControllerBlock)
                families.putIfAbsent("SpeedControllerPeripheral", block);
            else if (block instanceof ScriptedCreativeMotorBlock)
                families.putIfAbsent("CreativeMotorPeripheral", block);
        }
        helper.assertTrue(families.size() == 4, "Register all four scripted computer-capable machine families for this test");
        Map<BlockPos, String> expected = new LinkedHashMap<>();
        int index = 0;
        for (var family : families.entrySet()) {
            var pos = new BlockPos(index % 2 * 2, 1, index / 2 * 2);
            helper.setBlock(pos, family.getValue().defaultBlockState());
            expected.put(pos, family.getKey());
            index++;
        }
        helper.runAfterDelay(2, () -> {
            expected.forEach((pos, family) -> {
                var absolute = helper.absolutePos(pos);
                Object first = helper.getLevel().getCapability(capability, absolute, Direction.NORTH);
                helper.assertTrue(first != null, family + " is missing its peripheral capability");
                helper.assertTrue(first.getClass().getName().equals(PERIPHERALS + family),
                        family + " did not expose Create's native peripheral");
                for (Direction side : Direction.values())
                    helper.assertTrue(helper.getLevel().getCapability(capability, absolute, side) == first,
                            family + " did not retain its native cached peripheral on " + side);
                helper.assertTrue(helper.getLevel().getCapability(capability, absolute, null) == first,
                        family + " rejected an unsided peripheral query");
            });
            helper.succeed();
        });
    }

    private ComputerCraftCapabilitiesGameTests() {}
}
