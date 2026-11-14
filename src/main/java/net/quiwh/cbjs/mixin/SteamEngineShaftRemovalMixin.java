package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.SteamFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock.class, remap = false)
public abstract class SteamEngineShaftRemovalMixin {
    @Redirect(method = {"onRemove", "m_6810_"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1)
    private boolean cbjs$steamFamily(BlockEntry<?> entry, BlockState state) { return SteamFamilies.has(entry, state); }
}
