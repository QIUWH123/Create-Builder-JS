package net.quiwh.cbjs.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.ContraptionMovementSetting;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.piston.PistonContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.quiwh.cbjs.CBJS;

/** Exercises startup block modifications through Create's native consumers. */
@GameTestHolder(CBJS.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContraptionModificationGameTests {
    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty3x3x3", batch = "cbjs_contraption")
    public static void modifiedSettingsPreserveDefaultsAndNativeRestrictions(GameTestHelper helper) {
        helper.assertTrue(ContraptionMovementSetting.get(Blocks.MAGENTA_WOOL)
                        == ContraptionMovementSetting.UNMOVABLE,
                "noContraptionAssembly did not reach Create's movement setting lookup");
        helper.assertTrue(ContraptionMovementSetting.get(Blocks.ORANGE_WOOL)
                        == ContraptionMovementSetting.NO_PICKUP,
                "noContraptionPickup did not reach Create's movement setting lookup");
        helper.assertTrue(ContraptionMovementSetting.get(Blocks.LIME_WOOL)
                        == ContraptionMovementSetting.NO_PICKUP,
                "Chained and repeated modification callbacks lost the pickup restriction");
        helper.assertTrue(ContraptionMovementSetting.get(Blocks.WHITE_WOOL) == null,
                "An unchanged vanilla block acquired a CBJS movement restriction");

        // This fixture's Create server config keeps obsidian UNMOVABLE. A weaker
        // scripted pickup restriction must not replace that native setting.
        helper.assertTrue(ContraptionMovementSetting.get(Blocks.OBSIDIAN)
                        == ContraptionMovementSetting.UNMOVABLE,
                "A pickup modification weakened Create's native UNMOVABLE setting");
        assertMovement(helper, Blocks.OBSIDIAN.defaultBlockState(), false, false);
        assertMovement(helper, Blocks.BEDROCK.defaultBlockState(), false, false);
        assertMovement(helper, Blocks.WHITE_WOOL.defaultBlockState(), true, true);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_contraption")
    public static void pushModificationBlocksPistonsAndAllowsBearings(GameTestHelper helper) {
        assertMovement(helper, Blocks.CYAN_WOOL.defaultBlockState(), false, true);
        assertMovement(helper, Blocks.LIME_WOOL.defaultBlockState(), false, true);
        assertMovement(helper, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), false, true);
        assertMovement(helper, Blocks.WHITE_WOOL.defaultBlockState(), true, true);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_contraption")
    public static void nativeAssemblyRejectsNoAssemblyAndRetainsPickupRestriction(GameTestHelper helper)
            throws AssemblyException {
        BearingContraption unchanged = assembleSingleBlock(helper, Blocks.WHITE_WOOL);
        helper.assertTrue(!ContraptionMovementSetting.isNoPickup(unchanged.getBlocks().values()),
                "An unchanged assembled block unexpectedly prevents pickup");

        BearingContraption pickupRestricted = assembleSingleBlock(helper, Blocks.ORANGE_WOOL);
        helper.assertTrue(ContraptionMovementSetting.isNoPickup(pickupRestricted.getBlocks().values()),
                "Create's assembled-block pickup check did not see the modification");

        helper.setBlock(TEST_POS, Blocks.MAGENTA_WOOL);
        BearingContraption blocked = new BearingContraption(false, Direction.EAST);
        boolean rejected = false;
        try {
            blocked.assemble(helper.getLevel(), helper.absolutePos(TEST_POS.west()));
        } catch (AssemblyException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected && blocked.getBlocks().isEmpty(),
                "Create's native assembly collected a noContraptionAssembly block");
        helper.assertBlockPresent(Blocks.MAGENTA_WOOL, TEST_POS);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_contraption")
    public static void builderAndModificationRestrictionsCombineWithoutAffectingCompanions(GameTestHelper helper) {
        Block base = fixtureBlock(helper, "modified_gear");
        helper.assertTrue(ContraptionMovementSetting.get(base) == ContraptionMovementSetting.NO_PICKUP,
                "Modifying the base gear lost its builder pickup restriction");
        assertMovement(helper, base.defaultBlockState(), false, true);

        for (String casing : new String[]{"andesite", "brass"}) {
            Block companion = fixtureBlock(helper, casing + "_encased_modified_gear");
            helper.assertTrue(ContraptionMovementSetting.get(companion) == ContraptionMovementSetting.NO_PICKUP,
                    casing + " companion lost its inherited builder pickup restriction");
            assertMovement(helper, companion.defaultBlockState(), true, true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_contraption")
    public static void nativeDisassemblyPreservesNoSquashWorldTargets(GameTestHelper helper)
            throws AssemblyException {
        placeSingleBlock(helper, Blocks.YELLOW_WOOL, Blocks.STONE);
        helper.assertBlockPresent(Blocks.YELLOW_WOOL, TEST_POS);
        placeSingleBlock(helper, Blocks.LIME_WOOL, Blocks.STONE);
        helper.assertBlockPresent(Blocks.LIME_WOOL, TEST_POS);

        placeSingleBlock(helper, Blocks.WHITE_WOOL, Blocks.STONE);
        helper.assertBlockPresent(Blocks.STONE, TEST_POS);
        // Protection belongs to the existing world block, not the arriving member.
        placeSingleBlock(helper, Blocks.WHITE_WOOL, Blocks.YELLOW_WOOL);
        helper.assertBlockPresent(Blocks.YELLOW_WOOL, TEST_POS);
        helper.succeed();
    }

    private static BearingContraption assembleSingleBlock(GameTestHelper helper, Block block)
            throws AssemblyException {
        helper.setBlock(TEST_POS, block);
        BearingContraption contraption = new BearingContraption(false, Direction.EAST);
        helper.assertTrue(contraption.assemble(helper.getLevel(), helper.absolutePos(TEST_POS.west())),
                "Native bearing assembly failed for " + BuiltInRegistries.BLOCK.getKey(block));
        helper.assertTrue(contraption.getBlocks().size() == 1
                        && contraption.getBlocks().get(BlockPos.ZERO).state().is(block),
                "Native assembly did not collect exactly the intended fixture block");
        return contraption;
    }

    private static void placeSingleBlock(GameTestHelper helper, Block target, Block incoming)
            throws AssemblyException {
        BearingContraption contraption = assembleSingleBlock(helper, incoming);
        contraption.removeBlocksFromWorld(helper.getLevel(), BlockPos.ZERO);
        // Normally onEntityCreated initializes storage after the block collection.
        // This direct placement fixture has no ticking contraption entity.
        contraption.getStorage().initialize();
        helper.setBlock(TEST_POS, target);
        contraption.addBlocksToWorld(helper.getLevel(),
                new StructureTransform(helper.absolutePos(TEST_POS), 0, 0, 0));
    }

    private static void assertMovement(GameTestHelper helper, BlockState state,
                                       boolean pistonAllowed, boolean bearingAllowed) {
        helper.setBlock(TEST_POS, state);
        BlockPos pos = helper.absolutePos(TEST_POS);
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        helper.assertTrue(new MovementProbePiston().allows(state, helper.getLevel(), pos) == pistonAllowed,
                id + " has incorrect native translating movement permission");
        helper.assertTrue(new MovementProbeBearing().allows(state, helper.getLevel(), pos) == bearingAllowed,
                id + " has incorrect native bearing movement permission");
    }

    private static Block fixtureBlock(GameTestHelper helper, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("cbjs_test", path);
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id),
                "Missing contraption startup fixture " + id);
        return BuiltInRegistries.BLOCK.get(id);
    }

    // These wrappers expose the real protected consumer without replacing it.
    private static final class MovementProbePiston extends PistonContraption {
        private MovementProbePiston() { super(Direction.EAST, false); }
        private boolean allows(BlockState state, Level level, BlockPos pos) {
            return super.movementAllowed(state, level, pos);
        }
    }

    private static final class MovementProbeBearing extends BearingContraption {
        private MovementProbeBearing() { super(false, Direction.EAST); }
        private boolean allows(BlockState state, Level level, BlockPos pos) {
            return super.movementAllowed(state, level, pos);
        }
    }

    private ContraptionModificationGameTests() { }
}
