package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.rhino.util.HideFromJS;
import com.simibubi.create.content.fluids.pipes.*;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/** Explicit family policy; native casing geometry, models and block entities remain independent. */
@HideFromJS
public final class CompanionInheritance {
    public static void copy(BlockBuilder source, BlockBuilder target, boolean casingOwnsMaterial) {
        target.blockStateProperties.addAll(source.blockStateProperties);
        target.defaultStateModification = source.defaultStateModification;
        target.placementStateModification = source.placementStateModification;
        target.canBeReplacedFunction = source.canBeReplacedFunction;
        target.stepOnCallback = source.stepOnCallback;
        target.fallOnCallback = source.fallOnCallback;
        target.afterFallenOnCallback = source.afterFallenOnCallback;
        target.explodedCallback = source.explodedCallback;
        target.rotateStateModification = source.rotateStateModification;
        target.mirrorStateModification = source.mirrorStateModification;
        target.rightClick = source.rightClick;
        target.randomTickCallback = source.randomTickCallback;
        target.pickBlockCallback = source.pickBlockCallback;
        if (!casingOwnsMaterial) {
            copyMining(source, target);
            target.hardness = source.hardness;
            target.resistance = source.resistance;
            target.soundType = source.soundType;
            target.mapColorFn = source.mapColorFn;
            target.lightLevel = source.lightLevel;
            target.noCollision = source.noCollision;
            target.slipperiness = source.slipperiness;
            target.speedFactor = source.speedFactor;
            target.jumpFactor = source.jumpFactor;
            target.noValidSpawns = source.noValidSpawns;
            target.instrument = source.instrument;
            target.transparent = source.transparent;
        }
        // noItem() applies to the declared block only. Companion inventory identities stay stable.
        if (source.itemBuilder == null || target.itemBuilder == null) return;
        var from = source.itemBuilder;
        var to = target.itemBuilder;
        to.maxStackSize = from.maxStackSize;
        to.maxDamage = from.maxDamage;
        to.burnTime = from.burnTime;
        to.rarity = from.rarity;
        to.fireResistant = from.fireResistant;
        to.foodBuilder = from.foodBuilder;
        to.subtypes = from.subtypes;
        copyContainerItem(from, to);
        to.glow = from.glow;
        to.tooltip.addAll(from.tooltip);
        to.tint = from.tint;
        to.barColor = from.barColor;
        to.barWidth = from.barWidth;
        to.nameGetter = from.nameGetter;
        to.attributes = from.attributes == null ? null : com.google.common.collect.HashMultimap.create(from.attributes);
        to.anim = from.anim;
        to.useDuration = from.useDuration;
        to.use = from.use;
        to.finishUsing = from.finishUsing;
        to.releaseUsing = from.releaseUsing;
        to.hurtEnemy = from.hurtEnemy;
    }

    /** Mining follows the material owner; recipe/compatibility tags belong to the declared block. */
    static void copyMining(BlockBuilder source, BlockBuilder target) {
        target.requiresTool = source.requiresTool;
        for (ResourceLocation tag : source.defaultTags) {
            // Forge's sorted tier list is populated after startup registration. Copy the tier
            // tag naming convention directly, including namespaced mod tiers.
            if (tag.equals(BlockTags.MINEABLE_WITH_PICKAXE.location())
                    || tag.equals(BlockTags.MINEABLE_WITH_AXE.location())
                    || tag.equals(BlockTags.MINEABLE_WITH_SHOVEL.location())
                    || tag.equals(BlockTags.MINEABLE_WITH_HOE.location())
                    || tag.getPath().startsWith("needs_") && tag.getPath().endsWith("_tool"))
                target.tagBlock(tag);
        }
    }

    /** Copy only shared script-defined state, leaving all native connection decisions intact. */
    public static BlockState transfer(BlockState from, BlockState to) {
        if (from.getBlock() instanceof dev.latvian.mods.kubejs.core.BlockKJS scripted
                && scripted.kjs$getBlockBuilder() != null)
            return transfer(scripted.kjs$getBlockBuilder(), from, to);
        return to;
    }

    public static BlockState transfer(BlockBuilder source, BlockState from, BlockState to) {
        for (Property<?> property : source.blockStateProperties)
            if (!isNativeProperty(from, property) && !isNativeProperty(to, property)
                    && from.hasProperty(property) && to.hasProperty(property)) to = copyValue(from, to, property);
        return to;
    }

    private static boolean isNativeProperty(BlockState state, Property<?> scriptedProperty) {
        // Compare the actual block definition's property identities. A gear may legitimately
        // declare "north" or "alt", and a pipe may declare "top_shaft" as script state.
        Property<?> property = state.getBlock().getStateDefinition().getProperty(scriptedProperty.getName());
        var block = state.getBlock();
        if (property == BlockStateProperties.WATERLOGGED && block instanceof SimpleWaterloggedBlock) return true;
        if (property == BlockStateProperties.AXIS && (block instanceof IRotate || block instanceof AxisPipeBlock)) return true;
        if (block instanceof GlassFluidPipeBlock && property == GlassFluidPipeBlock.ALT) return true;
        if (block instanceof EncasedCogwheelBlock
                && (property == EncasedCogwheelBlock.TOP_SHAFT || property == EncasedCogwheelBlock.BOTTOM_SHAFT)) return true;
        if (block instanceof FluidPipeBlock || block instanceof EncasedPipeBlock)
            for (Property<?> connection : FluidPipeBlock.PROPERTY_BY_DIRECTION.values())
                if (property == connection) return true;
        return false;
    }

    private static void copyContainerItem(ItemBuilder from, ItemBuilder to) {
        try {
            // Preserve the ID before item registration. Resolving createItemProperties() here
            // would discard remainders pointing to another startup-script item.
            to.containerItem((ResourceLocation) ContainerItemField.VALUE.get(from));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot inherit KubeJS ItemBuilder.containerItem", exception);
        }
    }

    /** Lazy, startup-only compatibility access for the pinned KubeJS 2001.6.5 API. */
    private static final class ContainerItemField {
        private static final java.lang.reflect.Field VALUE = find();
        private static java.lang.reflect.Field find() {
            try {
                var field = ItemBuilder.class.getDeclaredField("containerItem");
                if (field.getType() != ResourceLocation.class)
                    throw new NoSuchFieldException("ItemBuilder.containerItem must be ResourceLocation");
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Companion items require KubeJS 2001.6.5 ItemBuilder.containerItem", exception);
            }
        }
    }

    private static <T extends Comparable<T>> BlockState copyValue(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    private CompanionInheritance() {}
}
