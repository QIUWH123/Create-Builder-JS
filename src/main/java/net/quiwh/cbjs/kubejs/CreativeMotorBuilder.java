package net.quiwh.cbjs.kubejs;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.*;

/** Native adjustable creative motor and computer peripheral. */
public final class CreativeMotorBuilder extends NativeKineticMachineBuilder {
    public CreativeMotorBuilder(ResourceLocation id) {
        super(id, "creative_motor", Set.of("shaft"));
        mapColor(MapColor.COLOR_PURPLE);
        tagBlock(ResourceLocation.parse("create:safe_nbt"));
        if (itemBuilder != null) itemBuilder.rarity = Rarity.EPIC;
    }
    @Override protected Map<String, String> nativeParts() { return Map.of("shaft", "create:block/shaft_half"); }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedCreativeMotorBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { ScriptedGeneratorBlockEntities.add(this); ModNativeGenerators.add(this); registerKineticDefaults(); }
}
