package net.quiwh.cbjs.mixin;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.client.ArmGaugeClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(value = com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer.class, remap = false)
public abstract class ArmRendererModelMixin {
    @Redirect(method = {"renderArm", "getRotatedModel"}, at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), require = 7, allow = 7)
    private SuperByteBuffer cbjs$part(PartialModel model, BlockState state) { return CachedBuffers.partial(ArmGaugeClientModels.select(model, state), state); }
}
