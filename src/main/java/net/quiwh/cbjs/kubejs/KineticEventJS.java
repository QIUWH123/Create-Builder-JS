package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import dev.latvian.mods.kubejs.level.BlockContainerJS;
import dev.latvian.mods.kubejs.level.LevelEventJS;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.content.MaterialGear;

/** Identity and location captured before a kinetic callback can change the world. */
public abstract class KineticEventJS extends LevelEventJS {
    private final String blockId;
    private final BlockPos pos;
    private final Level level;
    private final boolean large;
    private final boolean encased;

    protected KineticEventJS(KineticBlockEntity entity) {
        blockId = Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(entity.getBlockState().getBlock())).toString();
        pos = entity.getBlockPos().immutable();
        level = Objects.requireNonNull(entity.getLevel(), "Kinetic event requires a level");
        var block = entity.getBlockState().getBlock();
        large = block instanceof ICogWheel cogwheel && cogwheel.isLargeCog();
        encased = block instanceof EncasedCogwheelBlock || block instanceof MaterialGear gear && gear.isEncased();
    }

    public String getBlockId() { return blockId; }
    public BlockContainerJS getBlock() { return new BlockContainerJS(level, pos); }
    public BlockPos getPos() { return pos; }
    public boolean getIsLarge() { return large; }
    public boolean getIsEncased() { return encased; }
    @Override public Level getLevel() { return level; }
}
