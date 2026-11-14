package net.quiwh.cbjs.mixin;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.GearboxClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Substitutes the material partial while retaining the native gearbox animation. */
@Mixin(value = GearboxVisual.class, remap = false)
public abstract class GearboxVisualModelMixin {
    @Redirect(method = "<init>", at = @At(value = "FIELD",
            target = "Lcom/simibubi/create/AllPartialModels;SHAFT_HALF:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
            remap = false, require = 1, allow = 1)
    private PartialModel cbjs$shaftModel(VisualizationContext context, GearboxBlockEntity entity, float partialTick) {
        return GearboxClientModels.shaft(entity.getBlockState(), AllPartialModels.SHAFT_HALF);
    }
}
