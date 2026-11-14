package net.quiwh.cbjs.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.compat.createchain.ConveyorChains;
import net.quiwh.cbjs.compat.createchain.ConveyorConnections;

import java.lang.ref.WeakReference;

/** Exercises actual transformed native attachment methods and server connection admission. */
@GameTestHolder(CBJS.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChainTankParityGameTests {
    @GameTest(template = "empty3x3x3", batch = "cbjs_registration")
    public static void tankAndVesselAttachmentRestrictionsAreIndependent(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        BlockPos tankPos = helper.absolutePos(relative);
        BlockPos attachmentPos = tankPos.above();
        var whistleState = AllBlocks.STEAM_WHISTLE.getDefaultState().setValue(WhistleBlock.WALL, false);
        var engineState = AllBlocks.STEAM_ENGINE.getDefaultState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
        for (String family : new String[]{"tank", "vessel"}) {
            for (String mode : new String[]{"normal", "no_engine", "no_whistle", "neither"}) {
                helper.setBlock(relative, fixture(helper, family + "_" + mode));
                var tank = (FluidTankBlockEntity)helper.getBlockEntity(relative);
                boolean engineAllowed = mode.equals("normal") || mode.equals("no_whistle");
                boolean whistleAllowed = mode.equals("normal") || mode.equals("no_engine");
                helper.assertTrue(SteamEngineBlock.canAttach(helper.getLevel(), attachmentPos, Direction.DOWN) == engineAllowed,
                        family + "/" + mode + " engine placement restriction differs");
                helper.assertTrue(whistleState.canSurvive(helper.getLevel(), attachmentPos) == whistleAllowed,
                        family + "/" + mode + " whistle placement restriction differs");
                var engine = AllBlockEntityTypes.STEAM_ENGINE.create(attachmentPos, engineState);
                engine.setLevel(helper.getLevel());
                engine.source = new WeakReference<>(tank);
                helper.assertTrue(engine.isValid() == engineAllowed,
                        family + "/" + mode + " saved engine validity differs");
                helper.assertTrue((engine.getTank() == tank) == engineAllowed,
                        family + "/" + mode + " saved engine source differs");
                var whistle = AllBlockEntityTypes.STEAM_WHISTLE.create(attachmentPos, whistleState);
                whistle.setLevel(helper.getLevel());
                whistle.source = new WeakReference<>(tank);
                helper.assertTrue((whistle.getTank() == tank) == whistleAllowed,
                        family + "/" + mode + " saved whistle source differs");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_registration")
    public static void serverChecksConnectionCapsOnBothEndpoints(GameTestHelper helper) {
        BlockPos from = new BlockPos(0, 1, 0);
        BlockPos to = new BlockPos(2, 1, 2);
        BlockPos absoluteFrom = helper.absolutePos(from);
        BlockPos absoluteTo = helper.absolutePos(to);
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.CREATIVE);
        try {
            helper.setBlock(from, fixture(helper, "conveyor_one"));
            helper.setBlock(to, fixture(helper, "conveyor_zero"));
            var source = initializedConveyor(helper, from);
            var target = initializedConveyor(helper, to);
            helper.assertTrue(!target.addConnectionTo(absoluteFrom),
                    "Native addConnectionTo may not bypass zero cap");
            ConveyorChains.apply(player, source, absoluteTo, new ItemStack(Items.CHAIN), true);
            helper.assertTrue(source.connections.isEmpty() && target.connections.isEmpty(),
                    "Zero target cap must reject connection on both endpoints");
            helper.setBlock(to, fixture(helper, "conveyor_unlimited"));
            target = initializedConveyor(helper, to);
            ConveyorChains.apply(player, source, absoluteTo, new ItemStack(Items.CHAIN), true);
            helper.assertTrue(source.connections.contains(absoluteTo.subtract(absoluteFrom))
                            && target.connections.contains(absoluteFrom.subtract(absoluteTo)),
                    "Permitted connection must be added symmetrically");
            helper.assertTrue(!source.addConnectionTo(absoluteFrom.above(4)),
                    "Native addConnectionTo may not bypass a full cap");
            helper.assertTrue(!ConveyorConnections.canAdd(source) && ConveyorConnections.canAdd(target),
                    "Per-endpoint caps must differ after connecting");
            ConveyorChains.apply(player, source, absoluteTo, new ItemStack(Items.CHAIN), true);
            helper.assertTrue(source.connections.size() == 1 && target.connections.size() == 1,
                    "Repeated packets may not bypass cap or duplicate edges");
            ConveyorChains.apply(player, source, absoluteTo, new ItemStack(Items.CHAIN), false);
            helper.assertTrue(source.connections.isEmpty() && target.connections.isEmpty(),
                    "Connection caps may not block disconnection");
            helper.setBlock(from, fixture(helper, "conveyor_zero"));
            source = initializedConveyor(helper, from);
            ConveyorChains.apply(player, source, absoluteTo, new ItemStack(Items.CHAIN), true);
            helper.assertTrue(source.connections.isEmpty() && target.connections.isEmpty(),
                    "Zero source cap must reject connection on both endpoints");
        } finally {
            player.setGameMode(GameType.SURVIVAL);
        }
        helper.succeed();
    }

    private static ChainConveyorBlockEntity initializedConveyor(GameTestHelper helper, BlockPos pos) {
        var conveyor = (ChainConveyorBlockEntity)helper.getBlockEntity(pos);
        // This test connects immediately after placement, before the first native tick.
        // Native tick() initializes its lazy connectionStats through prepareStats().
        conveyor.prepareStats();
        return conveyor;
    }

    private static Block fixture(GameTestHelper helper, String suffix) {
        var id = ResourceLocation.fromNamespaceAndPath("kubejs", "cbjs_parity_" + suffix);
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "Missing private regression fixture: " + id);
        return BuiltInRegistries.BLOCK.get(id);
    }

    private ChainTankParityGameTests() { }
}
