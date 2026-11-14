package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.simibubi.create.content.contraptions.Contraption", remap = false)
public abstract class MechanicalBeltContraptionMixin {
    @Redirect(method = {"moveBlock"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 1)
    private boolean cbjs$recognizeBelt(BlockEntry<?> entry, BlockState state) {
        return entry == com.simibubi.create.AllBlocks.WINDMILL_BEARING
                && state.getBlock() instanceof net.quiwh.cbjs.content.ScriptedWindmillBearingBlock
                || MechanicalBeltCompat.recognizeBelt(entry, state);
    }
}
