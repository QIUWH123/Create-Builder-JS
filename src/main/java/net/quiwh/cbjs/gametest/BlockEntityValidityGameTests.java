package net.quiwh.cbjs.gametest;

import dev.latvian.mods.kubejs.core.BlockKJS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.quiwh.cbjs.CBJS;

/** Ensures every registered KubeJS entity block is accepted by its native type. */
@GameTestHolder(CBJS.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlockEntityValidityGameTests {
    @GameTest(template = "empty3x3x3", batch = "cbjs_registration")
    public static void scriptedEntityBlocksHaveValidNativeTypes(GameTestHelper helper) {
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            // KubeJS mixes BlockKJS into every Block, including vanilla blocks. Only
            // blocks backed by a builder are scripted registrations for this check.
            if (!(block instanceof BlockKJS scripted)
                    || scripted.kjs$getBlockBuilder() == null
                    || !(block instanceof EntityBlock entityBlock)) continue;
            boolean createdEntity = false;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BlockEntity entity = entityBlock.newBlockEntity(BlockPos.ZERO, state);
                if (entity == null) continue;
                helper.assertTrue(entity.getType().isValid(state),
                        BuiltInRegistries.BLOCK.getKey(block) + " is missing from its block entity type");
                createdEntity = true;
                break;
            }
            helper.assertTrue(createdEntity,
                    BuiltInRegistries.BLOCK.getKey(block) + " never creates a block entity in any state");
            checked++;
        }
        helper.assertTrue(checked > 0, "No scripted entity blocks were registered for the regression test");
        helper.succeed();
    }

    private BlockEntityValidityGameTests() {}
}
