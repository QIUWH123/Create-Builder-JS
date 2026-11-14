package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockItem;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.content.ScriptedBlazeBurnerBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlazeBurnerBlockItem.class, remap = false)
public abstract class BlazeBurnerItemMaterialMixin {
    @WrapOperation(method = {"useOn", "m_6225_", "interactLivingEntity", "m_6880_"}, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/burner/BlazeBurnerBlockItem;hasCapturedBlaze()Z"), require = 2, allow = 2)
    private boolean cbjs$skipCaptureWithoutFilledItem(BlazeBurnerBlockItem item, Operation<Boolean> original) {
        // The native filled-item branch skips capture but retains ordinary empty-cage placement.
        return original.call(item) || item.getBlock() instanceof ScriptedBlazeBurnerBlock
                && (item.getBlock().asItem() == net.minecraft.world.item.Items.AIR
                    || !((ScriptedBlazeBurnerBlock)item.getBlock()).sourceBuilder().options().canCaptureBlaze);
    }
    @Inject(method = "spawnCaptureEffects", at = @At("HEAD"), cancellable = true, require = 1)
    private void cbjs$captureEffects(net.minecraft.world.level.Level level, net.minecraft.world.phys.Vec3 position,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        var item = (BlazeBurnerBlockItem)(Object)this;
        if (item.getBlock() instanceof ScriptedBlazeBurnerBlock burner
                && !(level.isClientSide ? burner.sourceBuilder().options().haveParticles : burner.sourceBuilder().options().haveSound)) ci.cancel();
    }
    @WrapOperation(method = "giveBurnerItemTo", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;asStack()Lnet/minecraft/world/item/ItemStack;"), require = 1, allow = 1)
    private ItemStack cbjs$capturedMaterial(BlockEntry<?> entry, Operation<ItemStack> original) {
        var item = (BlazeBurnerBlockItem)(Object)this;
        return item.getBlock() instanceof ScriptedBlazeBurnerBlock ? new ItemStack(item.getBlock()) : original.call(entry);
    }
    @Inject(method = {"getDescriptionId", "m_5524_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void cbjs$emptyDescription(CallbackInfoReturnable<String> cir) {
        var item = (BlazeBurnerBlockItem)(Object)this;
        if (item.getBlock() instanceof ScriptedBlazeBurnerBlock && !item.hasCapturedBlaze()) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (id != null) cir.setReturnValue(net.minecraft.Util.makeDescriptionId("item", id));
        }
    }
}
