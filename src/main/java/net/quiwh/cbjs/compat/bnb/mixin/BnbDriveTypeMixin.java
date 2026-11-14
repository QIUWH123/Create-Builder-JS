package net.quiwh.cbjs.compat.bnb.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.BnbCogwheelChainTypes;
import dev.latvian.mods.kubejs.core.BlockKJS;
import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.MaterialGear;
import net.quiwh.cbjs.kubejs.CogwheelBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Script opt-ins distinguish drives while retaining custom native type predicates. */
@Mixin(value = CogwheelChainType.class, remap = false)
public abstract class BnbDriveTypeMixin {
    @Shadow @Final @Mutable private Predicate<Block> cogwheelPredicate;
    @Shadow @Final private CogwheelChainType.ChainRenderInfo chainRenderInfo;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cbjs$restrictScriptDrives(CallbackInfo callback) {
        Predicate<Block> nativePredicate = cogwheelPredicate;
        var renderInfo = chainRenderInfo;
        // Wrap once per native type, rather than allocate for every placement predicate query.
        cogwheelPredicate = block -> {
            if (block instanceof MaterialGear gear && gear.isEncased()) return false;
            if (!(block instanceof BlockKJS scripted)
                    || !(scripted.kjs$getBlockBuilder() instanceof CogwheelBuilder builder))
                return nativePredicate.test(block);
            boolean enabled = renderInfo == CogwheelChainType.ChainRenderInfo.BELT ? builder.beltEnabled()
                    : renderInfo == CogwheelChainType.ChainRenderInfo.ROPE ? builder.ropeEnabled()
                    : builder.chainEnabled();
            if (!enabled) return false;
            if (nativePredicate.test(block)) return true;
            // Only BnB's built-in rope/belt use a flange-only predicate. Explicit
            // CBJS opt-ins extend those types without relabeling ordinary gears.
            // Resolve holders at query time: constructors run before registration.
            var type = (CogwheelChainType) (Object) this;
            return renderInfo == CogwheelChainType.ChainRenderInfo.BELT
                    && BnbCogwheelChainTypes.BELT.isBound() && BnbCogwheelChainTypes.BELT.get() == type
                    || renderInfo == CogwheelChainType.ChainRenderInfo.ROPE
                    && BnbCogwheelChainTypes.ROPE_CHAIN.isBound() && BnbCogwheelChainTypes.ROPE_CHAIN.get() == type;
        };
    }
}
