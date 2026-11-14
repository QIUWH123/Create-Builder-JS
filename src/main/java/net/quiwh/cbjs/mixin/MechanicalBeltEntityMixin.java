package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import net.quiwh.cbjs.content.MechanicalBeltShaftAccess;
import net.quiwh.cbjs.content.MechanicalBeltShafts;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.simibubi.create.content.kinetics.belt.BeltBlockEntity", remap = false)
public abstract class MechanicalBeltEntityMixin implements MechanicalBeltShaftAccess {
    @Unique private CompoundTag cbjs$shaftTag;
    @Unique private BlockState cbjs$shaftState;

    @Override public BlockState cbjs$shaftMaterial(CompoundTag data) {
        // Forge replaces this tag on read and our setter replaces it on installation/migration.
        if (data != cbjs$shaftTag) {
            cbjs$shaftState = MechanicalBeltShafts.decode(data);
            cbjs$shaftTag = data;
        }
        return cbjs$shaftState;
    }

    @Redirect(method = {"tick", "hasPulley"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 1)
    private boolean cbjs$recognizeBelt(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state);
    }
}
