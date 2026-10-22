package net.quiwh.cbjs.client;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.quiwh.cbjs.content.MaterialGear;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Uses the same notification-driven instance lifecycle as Create's EncasedCogVisual. */
public final class MaterialEncasedCogwheelVisual<T extends KineticBlockEntity> extends KineticBlockEntityVisual<T> {
    private final boolean large;
    private final RotatingInstance rotatingModel;
    private final List<RotatingInstance> shafts = new ArrayList<>(2);

    public MaterialEncasedCogwheelVisual(VisualizationContext context, T entity, float partialTick) {
        super(context, entity, partialTick);
        MaterialGear gear = (MaterialGear) blockState.getBlock();
        ModPartialModels.GearModels models = ModPartialModels.of(gear.models());
        large = gear.isLargeCog();

        rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING,
                        Models.partial(large ? models.large() : models.small()))
                .createInstance().rotateToFace(rotationAxis()).setup(blockEntity).setPosition(getVisualPosition());
        rotatingModel.setChanged();

        IRotate rotating = (IRotate) blockState.getBlock();
        for (Direction direction : Iterate.directionsInAxis(rotationAxis())) {
            if (!rotating.hasShaftTowards(entity.getLevel(), entity.getBlockPos(), blockState, direction)) {
                continue;
            }
            RotatingInstance shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING,
                            Models.partial(models.shaftHalf()))
                    .createInstance().rotateToFace(Direction.SOUTH, direction).setup(blockEntity)
                    .setPosition(getVisualPosition());
            if (large) {
                shaft.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos));
            }
            shaft.setChanged();
            shafts.add(shaft);
        }
    }

    @Override
    public void update(float partialTick) {
        rotatingModel.setup(blockEntity).setChanged();
        // Flywheel recreates visuals for block-state changes; only kinetic data changes here.
        for (RotatingInstance shaft : shafts) {
            shaft.setup(blockEntity);
            if (large) {
                shaft.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos));
            }
            shaft.setChanged();
        }
    }

    @Override
    public void updateLight(float partialTick) {
        relight(rotatingModel);
        for (RotatingInstance shaft : shafts) {
            relight(shaft);
        }
    }

    @Override
    protected void _delete() {
        rotatingModel.delete();
        shafts.forEach(RotatingInstance::delete);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotatingModel);
        shafts.forEach(consumer);
    }
}
