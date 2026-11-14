package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import net.quiwh.cbjs.content.ScriptedMechanicalBeltBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keeps each belt material's queue spacing and entry cooldown in agreement. */
@Mixin(value = BeltInventory.class, remap = false)
public abstract class MechanicalBeltInventoryMixin {
    @Shadow @Final BeltBlockEntity belt;

    // The other 1f in tick() is an end-of-belt lookahead, not item spacing.
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 1, ordinal = 0), require = 1, allow = 1)
    private float cbjs$itemMovementSpacing(float original) {
        return cbjs$itemSpacing(original);
    }

    @ModifyConstant(method = "isBlocking", constant = @Constant(floatValue = 1), require = 2, allow = 2)
    private float cbjs$itemInsertionSpacing(float original) {
        return cbjs$itemSpacing(original);
    }

    @Unique
    private float cbjs$itemSpacing(float original) {
        if (belt.getBlockState().getBlock() instanceof ScriptedMechanicalBeltBlock scripted) {
            Float spacing = scripted.sourceBuilder().itemSpacing;
            if (spacing != null) return spacing;
        }
        return original;
    }
}
