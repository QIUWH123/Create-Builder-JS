package net.quiwh.cbjs.kubejs;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.*;

/** Native sail-driven contraption bearing, retaining assembly and direction controls. */
public final class WindmillBearingBuilder extends NativeKineticMachineBuilder {
    public WindmillBearingBuilder(ResourceLocation id) {
        super(id, "windmill_bearing", Set.of("top", "shaft"));
        mapColor(MapColor.PODZOL);
        tagBlock(ResourceLocation.parse("minecraft:mineable/axe"));
        tagBlock(ResourceLocation.parse("create:safe_nbt"));
    }
    @Override protected Map<String, String> nativeParts() { return Map.of("top", "create:block/bearing/top_wooden", "shaft", "create:block/shaft_half"); }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedWindmillBearingBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { ScriptedGeneratorBlockEntities.add(this); ModNativeGenerators.add(this); registerKineticDefaults(); }
}
