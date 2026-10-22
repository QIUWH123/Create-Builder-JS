package net.quiwh.cbjs.compat.bnb.mixin;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.compat.bnb.BnbChainedCogwheelBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CogwheelChainBlock.class, remap = false)
public abstract class BnbChainBlockMixin {
    @Inject(method = "getChainState", at = @At("HEAD"), cancellable = true)
    private static void cbjs$retainSourceProperties(BlockState source, boolean large, Direction.Axis axis,
                                                        CallbackInfoReturnable<BlockState> callback) {
        var block = BnbChainedCogwheelBuilder.forSource(source);
        if (block != null)
            callback.setReturnValue(block.withPropertiesOf(source)
                    .setValue(CogwheelChainBlock.AXIS, axis));
    }
}
