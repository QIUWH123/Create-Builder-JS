package net.quiwh.cbjs.mixin;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.crank.*;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.NativeGeneratorClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = HandCrankVisual.class, remap = false)
public abstract class GeneratorHandCrankVisualMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), require = 2, allow = 2)
    private Model cbjs$part(PartialModel original, VisualizationContext context, HandCrankBlockEntity entity, float pt) {
        return Models.partial(NativeGeneratorClientModels.part(entity.getBlockState(), original == AllPartialModels.HAND_CRANK_HANDLE ? "handle" : "base", original));
    }
}
