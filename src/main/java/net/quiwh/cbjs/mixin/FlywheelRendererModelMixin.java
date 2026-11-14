package net.quiwh.cbjs.mixin;
import com.simibubi.create.AllPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.client.ArmGaugeClientModels;
import net.quiwh.cbjs.content.ScriptedFlywheelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(value = com.simibubi.create.content.kinetics.flywheel.FlywheelRenderer.class, remap = false)
public abstract class FlywheelRendererModelMixin {
    @Redirect(method = "renderFlywheel", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/CachedBuffers;block(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"), require = 1, allow = 1)
    private SuperByteBuffer cbjs$wheel(BlockState state) {
        if (!(state.getBlock() instanceof ScriptedFlywheelBlock wheel)) return CachedBuffers.block(state);
        return CachedBuffers.partialFacingVertical(ArmGaugeClientModels.select(AllPartialModels.FLYWHEEL, state), state,
                Direction.fromAxisAndDirection(wheel.getRotationAxis(state), Direction.AxisDirection.POSITIVE));
    }
}
