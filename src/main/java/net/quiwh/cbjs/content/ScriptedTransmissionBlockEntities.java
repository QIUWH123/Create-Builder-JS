package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.transmission.*;
import com.simibubi.create.content.kinetics.speedController.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.TransmissionClientModels;
import net.quiwh.cbjs.kubejs.TransmissionBuilder;

/** Each native block entity type explicitly accepts its scripted transmission blocks. */
public final class ScriptedTransmissionBlockEntities {
    private static final BlockEntityBuilder<ClutchBlockEntity, CreateRegistrate> CLUTCH_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_clutch", ClutchBlockEntity::new)
            .visual(() -> SplitShaftVisual::new, false).renderer(() -> SplitShaftRenderer::new);
    private static final BlockEntityBuilder<GearshiftBlockEntity, CreateRegistrate> GEARSHIFT_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_gearshift", GearshiftBlockEntity::new)
            .visual(() -> SplitShaftVisual::new, false).renderer(() -> SplitShaftRenderer::new);
    private static final BlockEntityBuilder<SpeedControllerBlockEntity, CreateRegistrate> CONTROLLER_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_rotation_speed_controller", SpeedControllerBlockEntity::new)
            .visual(() -> TransmissionClientModels.ControllerVisual::new)
            .renderer(() -> SpeedControllerRenderer::new);
    public static final BlockEntityEntry<ClutchBlockEntity> CLUTCH = CLUTCH_BUILDER.register();
    public static final BlockEntityEntry<GearshiftBlockEntity> GEARSHIFT = GEARSHIFT_BUILDER.register();
    public static final BlockEntityEntry<SpeedControllerBlockEntity> SPEED_CONTROLLER = CONTROLLER_BUILDER.register();
    public static void add(TransmissionBuilder builder) {
        switch (builder.kind) {
            case CLUTCH -> CLUTCH_BUILDER.validBlock(builder::get);
            case GEARSHIFT -> GEARSHIFT_BUILDER.validBlock(builder::get);
            case SPEED_CONTROLLER -> CONTROLLER_BUILDER.validBlock(builder::get);
        }
    }
    public static void register() { }
    private ScriptedTransmissionBlockEntities() { }
}
