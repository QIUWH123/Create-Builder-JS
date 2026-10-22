package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** A custom casing around Create's ordinary small or large cogwheel. */
public final class CustomEncasedCogwheelBlock extends EncasedCogwheelBlock {
    public CustomEncasedCogwheelBlock(Properties properties, boolean large, Supplier<Block> casing) {
        super(properties, large, casing);
    }

    @Override
    public BlockEntityType<? extends SimpleKineticBlockEntity> getBlockEntityType() {
        return isLarge ? CustomCasingBlockEntities.ENCASED_LARGE_COGWHEEL.get()
                : CustomCasingBlockEntities.ENCASED_COGWHEEL.get();
    }
}
