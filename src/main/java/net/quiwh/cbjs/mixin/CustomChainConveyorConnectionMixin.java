package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChainConveyorConnectionHandler.class, remap = false)
public abstract class CustomChainConveyorConnectionMixin {
    @Shadow private static BlockPos firstPos;
    @Shadow private static ResourceKey<Level> firstDim;
    @Unique private static ResourceLocation cbjs$firstMaterial;

    @Inject(method = "isChain", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$recognizeChain(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(ChainMaterials.isChain(stack));
    }

    @WrapMethod(method = "onItemUsedOnBlock", remap = false)
    private static void cbjs$selectMaterial(PlayerInteractEvent.RightClickBlock event, Operation<Void> original) {
        if (!event.getLevel().isClientSide) { original.call(event); return; }
        ItemStack stack = event.getItemStack();
        if (firstPos != null && (firstDim != event.getLevel().dimension() || !ChainMaterials.isChain(stack)
                || cbjs$firstMaterial != null && !cbjs$firstMaterial.equals(ChainMaterials.id(stack))))
            cbjs$clearSelection();
        original.call(event);
        if (firstPos == null) cbjs$firstMaterial = null;
        else if (cbjs$firstMaterial == null) cbjs$firstMaterial = ChainMaterials.id(stack);
    }

    @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$clearChangedSelection(CallbackInfo ci) {
        if (firstPos == null) { cbjs$firstMaterial = null; return; }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || firstDim != minecraft.level.dimension()) {
            cbjs$clearSelection();
            ci.cancel();
            return;
        }
        ItemStack held = minecraft.player.getMainHandItem();
        if (!ChainMaterials.isChain(held)) held = minecraft.player.getOffhandItem();
        if (!ChainMaterials.isChain(held) || cbjs$firstMaterial != null
                && !cbjs$firstMaterial.equals(ChainMaterials.id(held))) {
            cbjs$clearSelection();
            ci.cancel();
        }
    }

    @Inject(method = "validateAndConnect", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$validateSelectedMaterial(LevelAccessor level, BlockPos pos, Player player,
            ItemStack stack, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (firstPos == null || !ChainMaterials.isChain(stack) || cbjs$firstMaterial != null
                && !cbjs$firstMaterial.equals(ChainMaterials.id(stack))) {
            cbjs$clearSelection();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onRightClick", at = @At("RETURN"), remap = false)
    private static void cbjs$clearCompletedSelection(CallbackInfoReturnable<Boolean> cir) {
        if (firstPos == null) cbjs$firstMaterial = null;
    }

    @Unique private static void cbjs$clearSelection() {
        firstPos = null;
        firstDim = null;
        cbjs$firstMaterial = null;
    }
}
