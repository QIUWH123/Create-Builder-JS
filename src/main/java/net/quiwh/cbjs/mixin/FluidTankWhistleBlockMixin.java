package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.FluidTankCompat;
import net.quiwh.cbjs.content.ScriptedFluidTankBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WhistleBlock.class, remap = false)
public abstract class FluidTankWhistleBlockMixin {
    @Inject(method = {"canSurvive", "m_7898_"}, at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void cbjs$customTankAttachment(BlockState whistle, LevelReader reader, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        var state = reader.getBlockState(pos.relative(WhistleBlock.getAttachedDirection(whistle)));
        if (state.getBlock() instanceof ScriptedFluidTankBlock)
            cir.setReturnValue(FluidTankCompat.steamAllowed(state, false));
    }
}
