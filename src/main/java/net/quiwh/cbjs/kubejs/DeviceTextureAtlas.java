package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** One additive block atlas per generation pass, shared by all scripted storage families. */
public final class DeviceTextureAtlas {
    public static void generate(FluidDeviceBuilder source, AssetJsonGenerator generator) {
        var fluids = ModFluidTanks.builders();
        var items = ModItemVaults.builders();
        boolean registered = fluids.contains(source) || items.contains(source);
        if (registered) {
            Object first = fluids.isEmpty() ? items.get(0) : fluids.get(0);
            if (source != first) return;
        }
        Map<String, String> entries = new LinkedHashMap<>();
        if (!registered) {
            if (source instanceof FluidTankBuilder tank)
                for (String role : FluidTankBuilder.TEXTURE_ROLES) entries.put(tank.spritePath(role), tank.texturePath(role));
            if (source instanceof ItemVaultBuilder vault)
                for (String role : ItemVaultBuilder.TEXTURE_ROLES) entries.put(vault.spritePath(role), vault.texturePath(role));
        } else {
            for (var tank : fluids) for (String role : FluidTankBuilder.TEXTURE_ROLES)
                entries.put(tank.spritePath(role), tank.texturePath(role));
            for (var vault : items) for (String role : ItemVaultBuilder.TEXTURE_ROLES)
                entries.put(vault.spritePath(role), vault.texturePath(role));
        }
        JsonArray sources = new JsonArray();
        entries.forEach((alias, texture) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("type", "minecraft:single"); entry.addProperty("resource", texture); entry.addProperty("sprite", alias);
            sources.add(entry);
        });
        JsonObject atlas = new JsonObject(); atlas.add("sources", sources);
        generator.json(ResourceLocation.parse("minecraft:atlases/blocks"), atlas);
    }
    private DeviceTextureAtlas() { }
}
