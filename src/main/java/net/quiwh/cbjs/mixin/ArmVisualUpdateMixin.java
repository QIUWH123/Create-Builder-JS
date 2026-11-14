package net.quiwh.cbjs.mixin;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.ArmGaugeClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(value = com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual.class, remap = false)
public abstract class ArmVisualUpdateMixin extends KineticBlockEntityVisual<ArmBlockEntity> {
    protected ArmVisualUpdateMixin(VisualizationContext context, ArmBlockEntity entity, float tick) { super(context, entity, tick); }
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), require = 1, allow = 1)
    private Model cbjs$goggles(PartialModel model) { return Models.partial(ArmGaugeClientModels.select(model, blockState)); }
}
