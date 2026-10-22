package net.quiwh.cbjs.mixin;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.TransmissionClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SplitShaftVisual.class, remap = false)
public abstract class TransmissionVisualModelMixin {
    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;SHAFT_HALF:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"), require = 1, allow = 1)
    private PartialModel cbjs$material(VisualizationContext context, SplitShaftBlockEntity entity, float partialTick) {
        return TransmissionClientModels.shaft(entity.getBlockState(), AllPartialModels.SHAFT_HALF);
    }
}
