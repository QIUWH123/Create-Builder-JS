package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedBrassTunnelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Preserve native cross-material brass groups and distribution rather than narrowing by exact ID. */
@Mixin(value = BrassTunnelBlockEntity.class, remap = false)
public abstract class LogisticsBrassTunnelMixin {
    @Redirect(method = {"hasDistributionBehaviour", "addValidOutputsOf", "getAdjacent"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 4, allow = 4)
    private boolean cbjs$brass(BlockEntry<?> entry, BlockState state) {
        return state.getBlock() instanceof ScriptedBrassTunnelBlock || entry.has(state);
    }
}
