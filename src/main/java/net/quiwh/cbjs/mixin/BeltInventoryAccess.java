package net.quiwh.cbjs.mixin;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value = BeltInventory.class, remap = false)
public interface BeltInventoryAccess {
    @Accessor("belt") BeltBlockEntity cbjs$getBelt();
}
