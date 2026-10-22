package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.contraption.ContraptionMovementSetting;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.TranslatingContraption;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.core.BlockBuilderProvider;
import dev.latvian.mods.rhino.util.HideFromJS;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Constant startup settings checked only by Create's existing assembly/placement paths. */
public final class ContraptionRules {
    public static final int NO_ASSEMBLY = 1;
    public static final int NO_PUSH = 2;
    public static final int NO_SQUASH = 4;
    public static final int NO_PICKUP = 8;
    // Optional companions register a source without forcing their addon classes to load here.
    private static final Map<BlockBuilder, BlockBuilder> OPTIONAL_SOURCES = new IdentityHashMap<>();

    @HideFromJS public static void inherit(BlockBuilder child, BlockBuilder source) {
        if (child == source) throw new IllegalArgumentException("A contraption builder cannot inherit itself");
        OPTIONAL_SOURCES.put(child, source);
    }

    @HideFromJS public static int flags(BlockBuilder builder) {
        if (builder == null) return 0;
        int flags = (Object)builder instanceof ContraptionBlockBuilder configured
                ? configured.cbjs$contraptionFlags() : 0;
        if (builder instanceof CogwheelBuilder cogwheel) flags |= flags(cogwheel.base);
        else if (builder instanceof FluidPipeBuilder pipe) flags |= flags(pipe.base);
        else if (builder instanceof ShaftEncasedBuilder shaft) flags |= flags(shaft.shaft);
        else if (builder instanceof CasingVariantBuilder variant) {
            flags |= flags(variant.casing) | flags(variant.gear) | flags(variant.pipe) | flags(variant.shaft);
        } else flags |= flags(OPTIONAL_SOURCES.get(builder));
        return flags;
    }

    @HideFromJS public static int flags(Block block) {
        return (Object)block instanceof BlockBuilderProvider provider ? flags(provider.kjs$getBlockBuilder()) : 0;
    }

    @HideFromJS public static boolean has(BlockState state, int flag) { return (flags(state.getBlock()) & flag) != 0; }

    /** Null preserves Create's own movement setting and all other mods' restrictions. */
    @HideFromJS public static ContraptionMovementSetting movementSetting(Block block) {
        int flags = flags(block);
        if ((flags & NO_ASSEMBLY) != 0) return ContraptionMovementSetting.UNMOVABLE;
        if ((flags & NO_PICKUP) != 0) return ContraptionMovementSetting.NO_PICKUP;
        return null;
    }

    @HideFromJS public static boolean deniesLinearMovement(Contraption contraption, BlockState state) {
        return contraption instanceof TranslatingContraption && has(state, NO_PUSH);
    }

    private ContraptionRules() { }
}
