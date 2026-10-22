package net.quiwh.cbjs.mixin;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.FluidDeviceClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Selects the material pointer once when Flywheel creates the native valve visual. */
@Mixin(value = FluidValveVisual.class, remap = false)
public abstract class FluidValveVisualMixin {
    @Redirect(method = "<init>", at = @At(value = "FIELD",
            target = "Lcom/simibubi/create/AllPartialModels;FLUID_VALVE_POINTER:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
            remap = false, require = 1, allow = 1)
    private PartialModel cbjs$materialPointer(VisualizationContext context, FluidValveBlockEntity entity,
            float partialTick) {
        return FluidDeviceClientModels.valvePointer(entity.getBlockState(), AllPartialModels.FLUID_VALVE_POINTER);
    }
}
