package net.quiwh.cbjs.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.quiwh.cbjs.content.DirectionalGearboxBlockEntity;
import net.quiwh.cbjs.content.ScriptedDirectionalGearboxBlock;
import net.quiwh.cbjs.kinetics.GearboxTransmission;

/** Retains one GPU instance per exposed shaft until the block state changes. */
public final class DirectionalGearboxVisual extends KineticBlockEntityVisual<DirectionalGearboxBlockEntity> {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final RotatingInstance[] shafts = new RotatingInstance[DIRECTIONS.length];

    public DirectionalGearboxVisual(VisualizationContext context, DirectionalGearboxBlockEntity entity,
                                    float partialTick) {
        super(context, entity, partialTick);
        var kind = ((ScriptedDirectionalGearboxBlock) blockState.getBlock()).sourceBuilder().kind;
        Direction.Axis boxAxis = blockState.getValue(BlockStateProperties.AXIS);
        var model = GearboxClientModels.shaft(blockState, AllPartialModels.SHAFT_HALF);
        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model));
        for (Direction direction : DIRECTIONS) {
            if (!GearboxTransmission.hasPort(kind, boxAxis, direction)) continue;
            RotatingInstance shaft = instancer.createInstance();
            shaft.setup(entity, direction.getAxis(), entity.getOutputSpeed(direction))
                    .setPosition(getVisualPosition())
                    .rotateToFace(Direction.SOUTH, direction)
                    .setChanged();
            shafts[direction.ordinal()] = shaft;
        }
    }

    @Override
    public void update(float partialTick) {
        // Flywheel recreates the visual for block-state changes; data updates only
        // update speed/phase on existing instances, including source changes.
        for (Direction direction : DIRECTIONS) {
            RotatingInstance shaft = shafts[direction.ordinal()];
            if (shaft == null) continue;
            shaft.setup(blockEntity, direction.getAxis(), blockEntity.getOutputSpeed(direction)).setChanged();
        }
    }

    @Override
    public void updateLight(float partialTick) {
        // Flywheel accepts null entries; pass the retained array directly so a
        // varargs call does not allocate one temporary array for every shaft.
        relight(shafts);
    }

    @Override
    protected void _delete() {
        for (int index = 0; index < shafts.length; index++) {
            RotatingInstance shaft = shafts[index];
            if (shaft == null) continue;
            shaft.delete();
            shafts[index] = null;
        }
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (RotatingInstance shaft : shafts) {
            if (shaft != null) consumer.accept(shaft);
        }
    }
}
