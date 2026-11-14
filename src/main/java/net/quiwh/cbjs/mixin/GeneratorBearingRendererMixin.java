package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.contraptions.bearing.BearingRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.client.NativeGeneratorClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BearingRenderer.class, remap = false)
public abstract class GeneratorBearingRendererMixin {
    @Redirect(method = "renderSafe", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), require = 1)
    private SuperByteBuffer cbjs$top(PartialModel model, BlockState state) { return CachedBuffers.partial(NativeGeneratorClientModels.bearingPart(model, state), state); }
    @Redirect(method = "getRotatedModel", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/CachedBuffers;partialFacing(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Lnet/createmod/catnip/render/SuperByteBuffer;"), require = 1)
    private SuperByteBuffer cbjs$shaft(PartialModel model, BlockState state, Direction direction) { return CachedBuffers.partialFacing(NativeGeneratorClientModels.bearingPart(model, state), state, direction); }
}
