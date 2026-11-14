package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.client.BlazeBurnerClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Includes the native renderInContraption path, which delegates to renderShared. */
@Mixin(value = BlazeBurnerRenderer.class, remap = false)
public abstract class BlazeBurnerRendererModelMixin {
    @WrapOperation(method = "renderShared", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), require = 6, allow = 6)
    private static SuperByteBuffer cbjs$material(PartialModel model, BlockState state, Operation<SuperByteBuffer> original) {
        return original.call(BlazeBurnerClientModels.select(state, model), state);
    }
    @ModifyExpressionValue(method = "renderShared", at = {
            @At(value = "FIELD", target = "Lcom/simibubi/create/AllSpriteShifts;BURNER_FLAME:Lnet/createmod/catnip/render/SpriteShiftEntry;"),
            @At(value = "FIELD", target = "Lcom/simibubi/create/AllSpriteShifts;SUPER_BURNER_FLAME:Lnet/createmod/catnip/render/SpriteShiftEntry;") }, require = 2, allow = 2)
    private static SpriteShiftEntry cbjs$flame(SpriteShiftEntry original, @Local(argsOnly = true) BlockState state) {
        return BlazeBurnerClientModels.flame(state, original);
    }
}
