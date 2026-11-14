package net.quiwh.cbjs.mixin;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.client.SteamClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = com.simibubi.create.content.kinetics.steamEngine.SteamEngineRenderer.class, remap = false)
public abstract class SteamEngineRendererMixin {
    @Redirect(method = "transformed", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), require = 1)
    private SuperByteBuffer cbjs$customSteamPart(PartialModel model, BlockState state) {
        return CachedBuffers.partial(SteamClientModels.select(model, state), state);
    }
}
