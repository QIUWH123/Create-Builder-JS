package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.contraptions.bearing.BearingVisual;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.NativeGeneratorClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BearingVisual.class, remap = false)
public abstract class GeneratorBearingVisualMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;", ordinal = 0), require = 1, allow = 1)
    private static Model cbjs$shaft(PartialModel original, VisualizationContext context, KineticBlockEntity entity, float pt) {
        return Models.partial(NativeGeneratorClientModels.bearingPart(original, entity.getBlockState()));
    }
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;", ordinal = 1), require = 1, allow = 1)
    private Model cbjs$top(PartialModel original, VisualizationContext context, KineticBlockEntity entity, float pt) {
        return Models.partial(NativeGeneratorClientModels.bearingPart(original, entity.getBlockState()));
    }
}
