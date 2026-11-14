package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = KineticBlockEntity.class, remap = false)
public interface GogglesKineticDataAccess {
    @Accessor("stress") float cbjs$stress();
    @Accessor("capacity") float cbjs$capacity();
}
