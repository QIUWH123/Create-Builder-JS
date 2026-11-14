package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.quiwh.cbjs.client.BlazeBurnerClientModels;
import net.createmod.catnip.render.SpriteShiftEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlazeBurnerVisual.class, remap = false)
public abstract class BlazeBurnerVisualModelMixin extends AbstractBlockEntityVisual<BlazeBurnerBlockEntity> {
    protected BlazeBurnerVisualModelMixin(VisualizationContext context, BlazeBurnerBlockEntity entity, float partialTick) { super(context, entity, partialTick); }
    @WrapOperation(method = {"<init>", "animate", "setupFlameInstance"}, at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/Models;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;)Ldev/engine_room/flywheel/api/model/Model;"), require = 7, allow = 7)
    private Model cbjs$material(PartialModel model, Operation<Model> original) {
        return original.call(BlazeBurnerClientModels.select(blockEntity.getBlockState(), model));
    }
    @ModifyExpressionValue(method = "setupFlameInstance", at = {
            @At(value = "FIELD", target = "Lcom/simibubi/create/AllSpriteShifts;BURNER_FLAME:Lnet/createmod/catnip/render/SpriteShiftEntry;"),
            @At(value = "FIELD", target = "Lcom/simibubi/create/AllSpriteShifts;SUPER_BURNER_FLAME:Lnet/createmod/catnip/render/SpriteShiftEntry;") }, require = 2, allow = 2)
    private SpriteShiftEntry cbjs$flame(SpriteShiftEntry original) {
        return BlazeBurnerClientModels.flame(blockEntity.getBlockState(), original);
    }
}
