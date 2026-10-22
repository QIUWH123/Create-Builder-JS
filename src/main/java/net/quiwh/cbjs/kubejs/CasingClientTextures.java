package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Loaded only on the client: shares Create's casing connectivity across a script's companions. */
final class CasingClientTextures {
    static void register(List<CasingBuilder> casings, List<CasingVariantBuilder> variants) {
        Map<CasingBuilder, CTSpriteShiftEntry> shifts = new IdentityHashMap<>();
        for (var casing : casings) {
            if (casing.casingConnectedTexture() == null) continue;
            var shift = shift(AllCTTypes.OMNIDIRECTIONAL, casing.casingTexture(), casing.casingConnectedTexture());
            shifts.put(casing, shift);
            CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(shift)).accept(casing.get());
            CreateRegistrate.casingConnectivity((block, connectivity) -> connectivity.makeCasing(block, shift))
                    .accept(casing.get());
        }
        for (var variant : variants) {
            var casing = variant.casing;
            var shift = shifts.get(casing);
            if (shift == null) continue;
            CreateRegistrate.connectedTextures(() -> {
                if (variant.kind == CasingVariantBuilder.Kind.COGWHEEL
                        && casing.cogwheelSideTexture() != null && casing.cogwheelSideConnectedTexture() != null) {
                    var sides = Couple.create(
                            shift(AllCTTypes.VERTICAL, casing.cogwheelSideTexture(), casing.cogwheelSideConnectedTexture()),
                            shift(AllCTTypes.HORIZONTAL, casing.cogwheelSideTexture(), casing.cogwheelSideConnectedTexture()));
                    return new EncasedCogCTBehaviour(shift, sides);
                }
                return variant.kind == CasingVariantBuilder.Kind.COGWHEEL
                        || variant.kind == CasingVariantBuilder.Kind.LARGE_COGWHEEL
                        ? new EncasedCogCTBehaviour(shift) : new EncasedCTBehaviour(shift);
            }).accept(variant.get());
            CreateRegistrate.casingConnectivity((block, connectivity) -> connectivity.make(block, shift,
                    (state, face) -> switch (variant.kind) {
                        case SHAFT -> face.getAxis() != state.getValue(EncasedShaftBlock.AXIS);
                        case PIPE -> !state.getValue(EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(face));
                        case COGWHEEL, LARGE_COGWHEEL -> face.getAxis() == state.getValue(EncasedCogwheelBlock.AXIS)
                                && !state.getValue(face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                                ? EncasedCogwheelBlock.TOP_SHAFT : EncasedCogwheelBlock.BOTTOM_SHAFT);
                    })).accept(variant.get());
        }
    }

    private static CTSpriteShiftEntry shift(AllCTTypes type, String original, String connected) {
        return CTSpriteShifter.getCT(type, ResourceLocation.parse(original), ResourceLocation.parse(connected));
    }

    private CasingClientTextures() { }
}
