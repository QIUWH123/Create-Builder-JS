package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Basin recipes read native heat states independently of the boiler registry. */
@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BlazeBurnerBasinHeatMixin {
    @Inject(method = "getHeatLevelOf", at = @At("HEAD"), cancellable = true, require = 1)
    private static void cbjs$heatPermission(BlockState state, CallbackInfoReturnable<BlazeBurnerBlock.HeatLevel> cir) {
        var builder = BlazeBurnerFamilies.builder(state);
        if (builder == null) return;
        if (!builder.options().canHeatBasin) cir.setReturnValue(BlazeBurnerBlock.HeatLevel.NONE);
        else if (state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL))
            cir.setReturnValue(builder.options().clamp(state.getValue(BlazeBurnerBlock.HEAT_LEVEL)));
    }
}
