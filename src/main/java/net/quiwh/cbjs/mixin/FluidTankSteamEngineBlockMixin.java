package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.quiwh.cbjs.content.FluidTankCompat;
import net.quiwh.cbjs.content.ScriptedFluidTankBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SteamEngineBlock.class, remap = false)
public abstract class FluidTankSteamEngineBlockMixin {
    @Inject(method = {"canAttach", "m_53196_"}, at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void cbjs$customTankAttachment(LevelReader reader, BlockPos pos, Direction direction,
            CallbackInfoReturnable<Boolean> cir) {
        var state = reader.getBlockState(pos.relative(direction));
        if (state.getBlock() instanceof ScriptedFluidTankBlock)
            cir.setReturnValue(FluidTankCompat.steamAllowed(state, false));
    }
}
