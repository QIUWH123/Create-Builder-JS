package net.quiwh.cbjs.mixin;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.ArmGaugeClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(value = com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual.class, remap = false)
public abstract class ArmVisualModelMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", ordinal = 0, target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), require = 1, allow = 1)
    private static Model cbjs$part(PartialModel model, VisualizationContext context, ArmBlockEntity entity, float tick) {
        return Models.partial(ArmGaugeClientModels.select(model, entity.getBlockState()));
    }
    @Redirect(method = "<init>", slice = @org.spongepowered.asm.mixin.injection.Slice(from = @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;ARM_BASE:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;")), at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), require = 6, allow = 6)
    private Model cbjs$movingPart(PartialModel model, VisualizationContext context, ArmBlockEntity entity, float tick) {
        return Models.partial(ArmGaugeClientModels.select(model, entity.getBlockState()));
    }
}
