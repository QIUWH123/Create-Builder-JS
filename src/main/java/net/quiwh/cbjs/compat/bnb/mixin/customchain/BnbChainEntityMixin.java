package net.quiwh.cbjs.compat.bnb.mixin.customchain;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.compat.bnb.customchain.BnbChainMaterialContext;
import net.quiwh.cbjs.content.ChainMaterials;
import net.quiwh.cbjs.kinetics.KineticEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CogwheelChainBlockEntity.class, remap = false)
public abstract class BnbChainEntityMixin {
    @Inject(method = "lazyTick", at = @At("TAIL"))
    private void cbjs$checkMaterialLimits(CallbackInfo callback) {
        var entity = (CogwheelChainBlockEntity) (Object) this;
        if (!KineticEventDispatcher.isCurrent(entity)) return;
        var controller = entity;
        if (!entity.isController()) {
            var offset = entity.getControllerOffset();
            if (offset == null) return;
            var controllerPos = entity.getBlockPos().offset(offset);
            var level = entity.getLevel();
            // Limit checks must not load a controller chunk just to inspect the chain material.
            if (!level.hasChunkAt(controllerPos)
                    || !(level.getBlockEntity(controllerPos) instanceof CogwheelChainBlockEntity resolved)
                    || !resolved.isController() || resolved.isRemoved()) return;
            controller = resolved;
        }
        if (controller.getChain() == null) return;
        // Every member is checked: small and large gears can have different RPM on the same loop.
        if (ChainMaterials.isOverloaded(BnbChainMaterialContext.material(controller), entity))
            entity.destroyChain(true);
    }

    @WrapMethod(method = "destroyChain")
    private ItemStack cbjs$refundScope(boolean drop, Operation<ItemStack> original) {
        try (var ignored = BnbChainMaterialContext.refunding(
                BnbChainMaterialContext.material((CogwheelChainBlockEntity)(Object)this))) {
            return original.call(drop);
        }
    }
    @WrapMethod(method = "getRequiredItems")
    private ItemRequirement cbjs$requirementScope(BlockState state, Operation<ItemRequirement> original) {
        try (var ignored = BnbChainMaterialContext.refunding(
                BnbChainMaterialContext.material((CogwheelChainBlockEntity)(Object)this))) {
            return original.call(state);
        }
    }
    @Group(name = "cbjs$refundMaterial", min = 2, max = 2)
    @Redirect(method = {"destroyChain", "getRequiredItems"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;"), require = 0)
    private ItemStack cbjs$refundMaterial(ItemStack vanilla, int count) {
        return ChainMaterials.stack(BnbChainMaterialContext.refunding(), count);
    }
    @Group(name = "cbjs$refundMaterial", min = 2, max = 2)
    @Redirect(method = {"destroyChain", "getRequiredItems"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;m_255036_(I)Lnet/minecraft/world/item/ItemStack;"), require = 0)
    private ItemStack cbjs$refundMaterialSrg(ItemStack vanilla, int count) {
        return ChainMaterials.stack(BnbChainMaterialContext.refunding(), count);
    }
}
