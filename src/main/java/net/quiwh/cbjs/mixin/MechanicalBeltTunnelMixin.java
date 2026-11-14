package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import net.quiwh.cbjs.content.ScriptedBrassTunnelBlock;
import net.quiwh.cbjs.content.ScriptedTunnelBlock;
import net.quiwh.cbjs.kubejs.TunnelBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock", remap = false)
public abstract class MechanicalBeltTunnelMixin {
    @Redirect(method = {"isValidPositionForPlacement", "getTunnelState", "hasValidOutput"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 1)
    private boolean cbjs$recognizeBelt(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state);
    }

    /** Keep script properties while Create recalculates this placed tunnel's connections. */
    @Inject(method = "getTunnelState", at = @At("RETURN"), cancellable = true, require = 1)
    private void cbjs$retainScriptState(BlockGetter world, BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        TunnelBuilder builder = (Object) this instanceof ScriptedTunnelBlock tunnel ? tunnel.sourceBuilder()
                : (Object) this instanceof ScriptedBrassTunnelBlock tunnel ? tunnel.sourceBuilder() : null;
        if (builder == null || builder.blockStateProperties.isEmpty()) return;
        BlockState current = world.getBlockState(pos);
        BlockState result = cir.getReturnValue();
        if (current.getBlock() != (Object) this || result.getBlock() != current.getBlock()) return;
        for (Property<?> property : builder.blockStateProperties)
            if (property != BeltTunnelBlock.HORIZONTAL_AXIS && property != BeltTunnelBlock.SHAPE
                    && current.hasProperty(property) && result.hasProperty(property))
                result = cbjs$copyScriptValue(current, result, property);
        cir.setReturnValue(result);
    }

    private static <T extends Comparable<T>> BlockState cbjs$copyScriptValue(
            BlockState source, BlockState result, Property<T> property) {
        return result.setValue(property, source.getValue(property));
    }
}
