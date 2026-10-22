package net.quiwh.cbjs.client;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.belt.BeltModel;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.HashMap;
import java.util.Map;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.kubejs.MechanicalBeltBuilder;
import net.quiwh.cbjs.content.MechanicalBeltShafts;

/** Selects material assets inside both native belt renderers without replacing their animation. */
public final class MechanicalBeltClientModels {
    // Flywheel's global PartialModel declarations hold weak values; retain the models until baking.
    private static final Map<Block, Material> MATERIALS = new HashMap<>();

    public static void register(MechanicalBeltBuilder builder) {
        MATERIALS.put(builder.get(), new Material(builder));
        CreateRegistrate.blockModel(() -> BeltModel::new).accept(builder.get());
    }

    public static PartialModel beltPartial(BeltBlockEntity entity, boolean diagonal, boolean start,
            boolean end, boolean bottom) {
        Material material = MATERIALS.get(entity.getBlockState().getBlock());
        if (material == null || !material.custom) return BeltRenderer.getBeltPartial(diagonal, start, end, bottom);
        return material.partials[(diagonal ? 6 : bottom ? 3 : 0) + (start ? 0 : end ? 2 : 1)];
    }

    public static SpriteShiftEntry spriteShift(BeltBlockEntity entity, DyeColor color, boolean diagonal, boolean bottom) {
        Material material = MATERIALS.get(entity.getBlockState().getBlock());
        if (material == null || !material.custom) return BeltRenderer.getSpriteShiftEntry(color, diagonal, bottom);
        int surface = diagonal ? 2 : bottom ? 1 : 0;
        return color == null ? material.shifts[surface] : material.dyedShifts[color.ordinal()][surface];
    }

    public static PartialModel pulley(BeltBlockEntity entity) {
        var shaft = MechanicalBeltShafts.get(entity);
        return shaft == null ? AllPartialModels.BELT_PULLEY : ShaftClientModels.beltPulley(shaft);
    }

    private static final class Material {
        private final boolean custom;
        private final PartialModel[] partials;
        private final SpriteShiftEntry[] shifts;
        private final SpriteShiftEntry[][] dyedShifts;

        private Material(MechanicalBeltBuilder builder) {
            custom = !builder.texturePath("belt").equals("create:block/belt")
                    || !builder.texturePath("belt_offset").equals("create:block/belt_offset")
                    || !builder.texturePath("belt_diagonal").equals("create:block/belt_diagonal")
                    || !builder.texturePath("belt_scroll").equals("create:block/belt_scroll")
                    || !builder.texturePath("belt_diagonal_scroll").equals("create:block/belt_diagonal_scroll");
            if (!custom) {
                partials = null;
                shifts = null;
                dyedShifts = null;
                return;
            }
            partials = new PartialModel[9];
            shifts = new SpriteShiftEntry[3];
            dyedShifts = new SpriteShiftEntry[DyeColor.values().length][3];
            String[] parts = {"start", "middle", "end", "start_bottom", "middle_bottom", "end_bottom",
                    "diagonal_start", "diagonal_middle", "diagonal_end"};
            for (int i = 0; i < parts.length; i++) partials[i] = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                    builder.id.getNamespace(), "block/" + builder.id.getPath() + "/" + parts[i]));
            String[] originals = {builder.texturePath("belt"), builder.texturePath("belt_offset"), builder.texturePath("belt_diagonal")};
            for (int i = 0; i < originals.length; i++) {
                ResourceLocation original = ResourceLocation.parse(originals[i]);
                shifts[i] = SpriteShifter.get(original, ResourceLocation.parse(builder.texturePath(
                        i == 2 ? "belt_diagonal_scroll" : "belt_scroll")));
                for (DyeColor color : DyeColor.values()) {
                    // Dyeing deliberately selects Create's installed dye appearance.
                    ResourceLocation dyed = ResourceLocation.fromNamespaceAndPath("create", "block/belt/"
                            + color.getSerializedName() + (i == 2 ? "_diagonal_scroll" : "_scroll"));
                    dyedShifts[color.ordinal()][i] = SpriteShifter.get(original, dyed);
                }
            }
        }
    }

    private MechanicalBeltClientModels() { }
}
