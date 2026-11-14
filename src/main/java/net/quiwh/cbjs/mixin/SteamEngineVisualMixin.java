package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineVisual;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.SteamClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SteamEngineVisual.class, remap = false)
public abstract class SteamEngineVisualMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), require = 3, allow = 3)
    private Model cbjs$customSteamPart(PartialModel model, VisualizationContext context, SteamEngineBlockEntity entity, float partialTick) {
        return Models.partial(SteamClientModels.select(model, entity.getBlockState()));
    }
}
