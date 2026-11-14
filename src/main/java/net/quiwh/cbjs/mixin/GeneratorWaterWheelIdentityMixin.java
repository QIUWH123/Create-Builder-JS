package net.quiwh.cbjs.mixin;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Adjacent scripted wheels obey the same native collision exclusion as native wheels. */
@Mixin(value = WaterWheelBlock.class, remap = false)
public abstract class GeneratorWaterWheelIdentityMixin {
    @Redirect(method = {"canSurvive", "m_7898_"}, at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1)
    private boolean cbjs$wheel(BlockEntry<?> entry, BlockState state) { return entry == AllBlocks.WATER_WHEEL && state.getBlock() instanceof WaterWheelBlock || entry.has(state); }
}
