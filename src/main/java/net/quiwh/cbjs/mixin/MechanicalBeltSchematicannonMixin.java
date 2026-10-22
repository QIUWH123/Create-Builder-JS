package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import net.quiwh.cbjs.content.MechanicalBeltLengths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity", remap = false)
public abstract class MechanicalBeltSchematicannonMixin {
    @Inject(method = "shouldIgnoreBlockState", at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$skipOverlongSchematicBelt(BlockState state, BlockEntity blockEntity,
            CallbackInfoReturnable<Boolean> callback) {
        // The cannon checks this before taking the belt's connector and pulley shafts.
        if (blockEntity instanceof BeltBlockEntity belt && !MechanicalBeltLengths.allows(state, belt.beltLength))
            callback.setReturnValue(true);
    }

    @Redirect(method = {"shouldIgnoreBlockState", "launchBlockOrBelt"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 1)
    private boolean cbjs$recognizeBelt(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state);
    }

    @Redirect(method = "launchBelt", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;asStack()Lnet/minecraft/world/item/ItemStack;"),
            remap = false, require = 1, allow = 1)
    private net.minecraft.world.item.ItemStack cbjs$launchMatchingConnector(
            com.tterrag.registrate.util.entry.ItemEntry<?> entry, net.minecraft.core.BlockPos target,
            BlockState state, int length, com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType[] casings) {
        return MechanicalBeltCompat.connector(state);
    }
}
