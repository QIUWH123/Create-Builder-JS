package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.quiwh.cbjs.kubejs.CasingBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives custom casings the same glass-pipe entry point as Create's copper casing. */
@Mixin(value = AxisPipeBlock.class, remap = false)
public abstract class AxisPipeBlockMixin {
    // This inherited Minecraft method is named use in development and m_6227_ in Forge's runtime mappings.
    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 1, allow = 1)
    private void cbjs$tryCustomCasing(BlockState state, Level level, BlockPos pos, Player player,
                                        InteractionHand hand, BlockHitResult hit,
                                        CallbackInfoReturnable<InteractionResult> callback) {
        InteractionResult result = CasingBuilder.tryEncaseAxisPipe(state, level, pos, player, hand, hit);
        if (result != InteractionResult.PASS) {
            callback.setReturnValue(result);
        }
    }
}
