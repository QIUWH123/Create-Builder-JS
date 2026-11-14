package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedTunnelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Keep andesite junctions splitting one item to each side on native and custom belts. */
@Mixin(targets = "com.simibubi.create.content.kinetics.belt.transport.BeltTunnelInteractionHandler", remap = false)
public abstract class LogisticsAndesiteTunnelMixin {
    @Redirect(method = "flapTunnelsAndCheckIfStuck", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1, allow = 1)
    private static boolean cbjs$andesite(BlockEntry<?> entry, BlockState state) {
        return state.getBlock() instanceof ScriptedTunnelBlock || entry.has(state);
    }
}
