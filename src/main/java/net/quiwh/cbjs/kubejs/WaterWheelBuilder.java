package net.quiwh.cbjs.kubejs;

import java.util.Map;
import java.util.Set;
import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.*;
import com.simibubi.create.content.kinetics.waterwheel.LargeWaterWheelBlockItem;

/** Native small/large wheels, including flow scoring, wood application and structural proxies. */
public final class WaterWheelBuilder extends NativeKineticMachineBuilder {
    public final boolean large;
    public WaterWheelBuilder(ResourceLocation id, boolean large) {
        super(id, large ? "large_water_wheel" : "water_wheel", large ? Set.of("wheel", "wheel_extension") : Set.of("wheel"));
        this.large = large;
        soundType = SoundType.WOOD;
        hardness = 2; resistance = 2;
        requiresTool = false;
        mapColor(MapColor.DIRT);
        tagBlock(ResourceLocation.parse("minecraft:mineable/axe"));
        if (large) getOrCreateItemBuilder();
    }
    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (!large) return super.getOrCreateItemBuilder();
        if (!(itemBuilder instanceof LargeWheelItemBuilder)) {
            itemBuilder = new LargeWheelItemBuilder(id);
            ((dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder).blockBuilder = this;
        }
        return (BlockItemBuilder) itemBuilder;
    }
    private static final class LargeWheelItemBuilder extends CBJSBlockItemBuilder {
        LargeWheelItemBuilder(ResourceLocation id) { super(id); }
        @Override public Item createObject() { return new LargeWaterWheelBlockItem(blockBuilder.get(), createItemProperties()); }
    }
    @Override protected boolean primaryPart(String role) { return large && role.equals("wheel"); }
    @Override protected boolean primaryStaticModel() { return !large; }
    @Override protected Map<String, String> nativeParts() {
        return large ? Map.of("wheel", "create:block/large_water_wheel/block", "wheel_extension", "create:block/large_water_wheel/block_extension")
                : Map.of("wheel", "create:block/water_wheel/wheel");
    }
    @Override public Block createObject() { validateBlockEntity(); return large ? new ScriptedLargeWaterWheelBlock(this) : new ScriptedWaterWheelBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        ScriptedGeneratorBlockEntities.add(this); ModNativeGenerators.add(this); registerKineticDefaults();
    }
}
