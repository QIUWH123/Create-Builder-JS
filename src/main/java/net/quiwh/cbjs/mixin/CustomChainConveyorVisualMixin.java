package net.quiwh.cbjs.mixin;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.ChainConveyorClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Change only geometry; native package animation, lighting and instance disposal remain intact. */
@Mixin(value = ChainConveyorVisual.class, remap = false)
public abstract class CustomChainConveyorVisualMixin extends SingleAxisRotatingVisual<ChainConveyorBlockEntity> {
    protected CustomChainConveyorVisualMixin(VisualizationContext context, ChainConveyorBlockEntity entity, float partialTick) {
        super(context, entity, partialTick, Models.partial(AllPartialModels.CHAIN_CONVEYOR_SHAFT));
    }
    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;CHAIN_CONVEYOR_SHAFT:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"), require = 1, allow = 1)
    private static PartialModel cbjs$shaft(VisualizationContext context, ChainConveyorBlockEntity entity, float partialTick) {
        return ChainConveyorClientModels.select(entity.getBlockState(), AllPartialModels.CHAIN_CONVEYOR_SHAFT);
    }
    @Redirect(method = "setupGuards", at = @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;CHAIN_CONVEYOR_WHEEL:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"), require = 1, allow = 1)
    private PartialModel cbjs$wheel() {
        return ChainConveyorClientModels.select(blockEntity.getBlockState(), AllPartialModels.CHAIN_CONVEYOR_WHEEL);
    }
    @Redirect(method = "setupGuards", at = @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;CHAIN_CONVEYOR_GUARD:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"), require = 1, allow = 1)
    private PartialModel cbjs$guard() {
        return ChainConveyorClientModels.select(blockEntity.getBlockState(), AllPartialModels.CHAIN_CONVEYOR_GUARD);
    }
}
