package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.latvian.mods.kubejs.level.CachedLevelBlock;
import dev.latvian.mods.kubejs.level.KubeLevelEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Objects;

/** Captures the pipe identity and location before a script can change the world. */
public abstract class FluidPipeEventJS implements KubeLevelEvent {
    private final String blockId;
    private final BlockPos pos;
    private final Level level;
    private final boolean encased;

    protected FluidPipeEventJS(SmartBlockEntity entity) {
        this(Objects.requireNonNull(entity.getLevel(), "Fluid pipe event requires a level"),
                entity.getBlockPos(), entity.getBlockState());
    }

    protected FluidPipeEventJS(Level level, BlockPos pos, BlockState state) {
        this.level = Objects.requireNonNull(level, "Fluid pipe event requires a level");
        this.pos = pos.immutable();
        blockId = Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(state.getBlock())).toString();
        encased = state.getBlock() instanceof EncasedPipeBlock;
    }

    public String getBlockId() { return blockId; }
    public CachedLevelBlock getBlock() { return new CachedLevelBlock(level, pos); }
    public BlockPos getPos() { return pos; }
    @Override public Level getLevel() { return level; }
    public boolean getIsEncased() { return encased; }

    protected static String fluidId(FluidStack fluid) {
        return fluid.isEmpty() ? "minecraft:empty"
                : Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(fluid.getFluid())).toString();
    }
}
