package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.client.LogisticsClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.simibubi.create.content.logistics.funnel.FunnelRenderer", remap = false)
public abstract class LogisticsFunnelRendererMixin {
    @WrapOperation(method = "renderSafe", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), require = 1, allow = 1)
    private SuperByteBuffer cbjs$flap(PartialModel model, BlockState state, Operation<SuperByteBuffer> original) {
        return original.call(LogisticsClientModels.select(state, model), state);
    }
}
