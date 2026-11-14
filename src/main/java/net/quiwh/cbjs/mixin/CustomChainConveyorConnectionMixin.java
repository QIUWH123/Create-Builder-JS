package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlock;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionHandler;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.quiwh.cbjs.content.ChainMaterials;
import net.quiwh.cbjs.compat.createchain.ConveyorConnections;
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
    @Shadow private static boolean fail(String message) { throw new AssertionError(); }
    @Unique private static ResourceLocation cbjs$firstMaterial;

    @WrapOperation(method = "onItemUsedOnBlock", at = @At(value = "INVOKE", target =
            "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1, allow = 1)
    private static boolean cbjs$family(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        return state.getBlock() instanceof ChainConveyorBlock;
    }

    @WrapOperation(method = "onItemUsedOnBlock", at = @At(value = "INVOKE", target =
            "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;"), require = 1, allow = 1)
    private static Object cbjs$selectionMaximum(ConfigBase.ConfigInt config, Operation<Object> original,
            PlayerInteractEvent.RightClickBlock event) {
        return event.getLevel().getBlockEntity(event.getPos()) instanceof ChainConveyorBlockEntity conveyor
                ? ConveyorConnections.maximum(conveyor) : original.call(config);
    }

    @WrapOperation(method = "validateAndConnect", at = @At(value = "INVOKE", target =
            "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;", ordinal = 1), require = 1, allow = 1)
    private static Object cbjs$targetMaximum(ConfigBase.ConfigInt config, Operation<Object> original,
            LevelAccessor level, BlockPos pos, Player player, ItemStack chain, boolean simulate) {
        return level.getBlockEntity(pos) instanceof ChainConveyorBlockEntity conveyor
                ? ConveyorConnections.maximum(conveyor) : original.call(config);
    }

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
            return;
        }
        // Both endpoints can change after selection. Keep preview and the eventual server
        // packet consistent, and reject removed endpoints before Create dereferences them.
        if (!(level.getBlockEntity(firstPos) instanceof ChainConveyorBlockEntity source)
                || !(level.getBlockEntity(pos) instanceof ChainConveyorBlockEntity target)) {
            cir.setReturnValue(fail("chain_conveyor.blocks_invalid"));
            return;
        }
        if (!ConveyorConnections.canAdd(source) || !ConveyorConnections.canAdd(target))
            cir.setReturnValue(fail("chain_conveyor.cannot_add_more_connections"));
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
