package net.quiwh.cbjs.compat.bnb.mixin.customchain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.behaviour.CogwheelChainBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.registries.BuiltInRegistries;
import net.quiwh.cbjs.content.ChainMaterials;
import net.quiwh.cbjs.kinetics.KineticEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Check each member's actual RPM while using BnB's native returned material. */
@Mixin(value = CogwheelChainBehaviour.class, remap = false)
public abstract class BnbChainEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void cbjs$checkMaterialLimits(CallbackInfo callback) {
        var behaviour = (CogwheelChainBehaviour) (Object) this;
        if (!(behaviour.blockEntity instanceof KineticBlockEntity entity)
                || !KineticEventDispatcher.isCurrent(entity) || !behaviour.isPartOfChain()) return;
        var controller = behaviour;
        if (!behaviour.isController()) {
            var offset = behaviour.getControllerOffset();
            if (offset == null) return;
            var pos = entity.getBlockPos().offset(offset);
            var level = entity.getLevel();
            // Inspect only loaded controllers; limit checks never load chunks.
            if (level == null || !level.hasChunkAt(pos)) return;
            controller = BlockEntityBehaviour.get(level, pos, CogwheelChainBehaviour.TYPE);
            if (controller == null || !controller.isController() || controller.blockEntity.isRemoved()) return;
        }
        var chain = controller.getControlledChain();
        if (chain != null && ChainMaterials.isOverloaded(BuiltInRegistries.ITEM.getKey(chain.getReturnedItem()), entity))
            behaviour.destroyChain(true, true);
    }
}
