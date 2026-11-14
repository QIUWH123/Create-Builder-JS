package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlockEntity;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.quiwh.cbjs.client.LogisticsClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.simibubi.create.content.logistics.tunnel.BeltTunnelVisual", remap = false)
public abstract class LogisticsBeltTunnelVisualMixin extends AbstractBlockEntityVisual<BeltTunnelBlockEntity> {
    protected LogisticsBeltTunnelVisualMixin(VisualizationContext context, BeltTunnelBlockEntity entity, float partialTick) { super(context, entity, partialTick); }
    @WrapOperation(method = "lambda$createFlaps$0", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), require = 1, allow = 1)
    private Model cbjs$flap(PartialModel model, Operation<Model> original) {
        return original.call(LogisticsClientModels.select(blockEntity.getBlockState(), model));
    }
}
