package net.quiwh.cbjs.mixin;
import com.simibubi.create.content.kinetics.gauge.GaugeBlockEntity;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.ArmGaugeClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(value = com.simibubi.create.content.kinetics.gauge.GaugeVisual.class, remap = false)
public abstract class GaugeVisualModelMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), require = 1, allow = 1)
    private Model cbjs$part(PartialModel model, VisualizationContext context, GaugeBlockEntity entity, float tick) {
        return Models.partial(ArmGaugeClientModels.select(model, entity.getBlockState()));
    }
}
