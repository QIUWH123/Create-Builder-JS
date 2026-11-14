package net.quiwh.cbjs.content;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Native fuel timers and synchronization with independent startup capabilities. */
public final class ScriptedBlazeBurnerBlockEntity extends BlazeBurnerBlockEntity {
    public ScriptedBlazeBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    private BurnerOptions options() { return ((ScriptedBlazeBurnerBlock)getBlockState().getBlock()).sourceBuilder().options(); }
    @Override protected HeatLevel getHeatLevel() { return options().clamp(super.getHeatLevel()); }
    @Override protected void setBlockHeat(HeatLevel heat) { super.setBlockHeat(options().clamp(heat)); }
    @Override public HeatLevel getHeatLevelForRender() { return options().clamp(super.getHeatLevelForRender()); }
    @Override public void tick() {
        applyCosmeticPermissions();
        super.tick();
        if (level != null && !level.isClientSide) setBlockHeat(super.getHeatLevelFromBlock());
    }
    @Override public void lazyTick() {
        super.lazyTick();
        if (!options().canBeStockKeeper) stockKeeper = false;
    }
    @Override protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        applyCosmeticPermissions();
    }
    private void applyCosmeticPermissions() {
        if (!options().haveGoggles) goggles = false;
        if (!options().haveTrainHat) hat = false;
        if (!options().canBeStockKeeper) stockKeeper = false;
    }
    @Override protected void applyCreativeFuel() {
        activeFuel = FuelType.NONE;
        remainingBurnTime = 0;
        isCreative = true;
        HeatLevel next = options().nextCreativeLevel(getHeatLevelFromBlock());
        if (level.isClientSide) { spawnParticleBurst(next == HeatLevel.SEETHING); return; }
        playSound();
        setBlockHeat(next);
    }
    @Override public boolean isCreativeFuel(ItemStack stack) { return options().canUseCreativeFuel && super.isCreativeFuel(stack); }
    @Override protected void spawnParticles(HeatLevel heat, double multiplier) { if (options().haveParticles) super.spawnParticles(options().clamp(heat), multiplier); }
    @Override public void spawnParticleBurst(boolean soulFlame) { if (options().haveParticles) super.spawnParticleBurst(soulFlame && options().clamp(HeatLevel.SEETHING) == HeatLevel.SEETHING); }
    @Override protected void playSound() { if (options().haveSound) super.playSound(); }
}
