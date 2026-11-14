package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.content.MechanicalBeltLengths;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.foundation.utility.BlockHelper", remap = false)
public abstract class MechanicalBeltSchematicBlockMixin {
    @Inject(method = "placeSchematicBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$limitPastedBelt(Level level, BlockState state, BlockPos pos, ItemStack stack,
            CompoundTag data, CallbackInfo callback) {
        if (data != null && data.contains("Length", 3) && !MechanicalBeltLengths.allows(state, data.getInt("Length")))
            callback.cancel();
    }

    @Redirect(method = {"placeSchematicBlock"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 1)
    private static boolean cbjs$recognizeBelt(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state);
    }
}
