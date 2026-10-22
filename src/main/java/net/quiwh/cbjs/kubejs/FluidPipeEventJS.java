package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.latvian.mods.kubejs.level.BlockContainerJS;
import dev.latvian.mods.kubejs.level.LevelEventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

/** Captures the pipe identity and location before a script can change the world. */
public abstract class FluidPipeEventJS extends LevelEventJS {
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
        blockId = Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(state.getBlock())).toString();
        encased = state.getBlock() instanceof EncasedPipeBlock;
    }

    public String getBlockId() { return blockId; }
    public BlockContainerJS getBlock() { return new BlockContainerJS(level, pos); }
    public BlockPos getPos() { return pos; }
    @Override public Level getLevel() { return level; }
    public boolean getIsEncased() { return encased; }

    protected static String fluidId(FluidStack fluid) {
        return fluid.isEmpty() ? "minecraft:empty"
                : Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(fluid.getFluid())).toString();
    }
}
