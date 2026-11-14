package net.quiwh.cbjs.client;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MaterialGear;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MaterialCogwheelVisual<T extends KineticBlockEntity> extends SingleAxisRotatingVisual<T> {
    private final List<RotatingInstance> shafts = new ArrayList<>();

    public MaterialCogwheelVisual(VisualizationContext context, T entity, float partialTick) {
        super(context, entity, partialTick, Models.partial(gearModel(entity.getBlockState())));
        updateFlangedPhase();
        createShafts();
    }

    private static PartialModel gearModel(BlockState state) {
        MaterialGear gear = (MaterialGear) state.getBlock();
        ModPartialModels.GearModels models = ModPartialModels.of(gear.models());
        return models.rotating(gear.isLargeCog());
    }

    private void createShafts() {
        BlockState state = blockState;
        MaterialGear gear = (MaterialGear) state.getBlock();
        if (gear.isShaftless()) {
            return;
        }
        ModPartialModels.GearModels models = ModPartialModels.of(gear.models());
        if (!gear.isEncased()) {
            addShaft(models.shaft(), Direction.UP,
                    Direction.fromAxisAndDirection(rotationAxis(), Direction.AxisDirection.POSITIVE), gear);
        } else {
            IRotate rotating = (IRotate) state.getBlock();
            for (Direction direction : Direction.values()) {
                if (direction.getAxis() == rotationAxis() && rotating.hasShaftTowards(blockEntity.getLevel(),
                        blockEntity.getBlockPos(), state, direction)) {
                    addShaft(models.shaftHalf(), Direction.SOUTH, direction, gear);
                }
            }
        }
    }

    private void addShaft(PartialModel model, Direction modelFacing, Direction facing, MaterialGear gear) {
        RotatingInstance shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(model))
                .createInstance().rotateToFace(modelFacing, facing).setup(blockEntity).setPosition(getVisualPosition());
        if (gear.isFlanged()) {
            shaft.setRotationOffset(flangedOffset());
        } else if (gear.isLargeCog()) {
            shaft.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos));
        }
        shaft.setChanged();
        shafts.add(shaft);
    }

    @Override
    public void update(float partialTick) {
        super.update(partialTick);
        updateFlangedPhase();
        // Flywheel recreates the visual for block-state changes, so its shaft topology
        // and orientation stay fixed while speed and network data update in place.
        MaterialGear gear = (MaterialGear) blockState.getBlock();
        for (RotatingInstance shaft : shafts) {
            shaft.setup(blockEntity);
            if (gear.isFlanged()) {
                shaft.setRotationOffset(flangedOffset());
            } else if (gear.isLargeCog()) {
                shaft.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos));
            }
            shaft.setChanged();
        }
    }

    private float flangedOffset() {
        return BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos)
                + blockEntity.getRotationAngleOffset(rotationAxis());
    }

    private void updateFlangedPhase() {
        if (((MaterialGear) blockState.getBlock()).isFlanged()) {
            rotatingModel.setRotationOffset(flangedOffset());
            rotatingModel.setChanged();
        }
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        for (RotatingInstance shaft : shafts) {
            relight(shaft);
        }
    }

    @Override
    protected void _delete() {
        super._delete();
        shafts.forEach(RotatingInstance::delete);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        shafts.forEach(consumer);
    }
}
