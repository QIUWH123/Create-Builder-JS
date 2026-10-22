package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.loot.LootBuilder;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.CustomEncasedPipeBlock;
import net.quiwh.cbjs.content.ScriptedFluidPipeBlock;
import net.quiwh.cbjs.content.ScriptedGlassFluidPipeBlock;
import net.quiwh.cbjs.content.ScriptedEncasedPipeBlock;

/** A Create pipe family registered with the usual KubeJS block builder settings. */
public final class FluidPipeBuilder extends BlockBuilder {
    public enum Kind { REGULAR, GLASS, ENCASED }

    public final Kind kind;
    public final FluidPipeBuilder base;
    String allTexture;
    private boolean encased = true;
    private boolean additionalCreated;
    private boolean dropsDisabled;
    private Consumer<LootBuilder> disabledLoot;
    private FluidPipeBuilder glass;
    private FluidPipeBuilder copperEncased;

    /** Replace one texture slot while preserving the family's default geometry. */
    public FluidPipeBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }

    public FluidPipeBuilder(ResourceLocation id) { this(id, Kind.REGULAR, null); }

    private FluidPipeBuilder(ResourceLocation id, Kind kind, FluidPipeBuilder base) {
        super(id);
        this.kind = kind;
        this.base = base;
        textures.entrySet().clear();
        allTexture = null;
        hardness = 3;
        resistance = 6;
        soundType = SoundType.COPPER;
        mapColor(MapColor.COLOR_ORANGE);
        notSolid = true;
        opaque = false;
        fullBlock = false;
        suffocating = false;
        viewBlocking = false;
        redstoneConductor = false;
        requiresTool = true;
        tagBlock(ResourceLocation.parse("minecraft:mineable/pickaxe"));
        if (kind == Kind.GLASS) renderType = "cutout_mipped";
        if (base != null) {
            CompanionInheritance.copy(base, this, false);
            if (kind == Kind.GLASS) renderType = "cutout_mipped";
            noItem();
        }
    }

    public FluidPipeBuilder source() { return base == null ? this : base; }

    /** Wrench-created glass and copper casing forms do not add separate inventory items. */
    public GlassFluidPipeBlock glassBlock() {
        return (GlassFluidPipeBlock) source().glass.get();
    }

    public CustomEncasedPipeBlock copperEncasedBlock() {
        FluidPipeBuilder variant = source().copperEncased;
        return variant == null ? null : (CustomEncasedPipeBlock) variant.get();
    }

    public FluidPipeBuilder noEncasedVariants() { encased = false; return this; }
    public boolean encasedVariantsEnabled() { return source().encased; }

    @Override public FluidPipeBuilder textureAll(String texture) {
        super.textureAll(texture);
        allTexture = texture;
        return this;
    }

    public String pipeTexture() { return textureOrDefault("pipe", "create:block/pipes"); }
    public String connectedTexture() { return textureOrDefault("pipe_connected", "create:block/pipes_connected"); }
    public String glassTexture() { return textureOrDefault("glass", "create:block/glass_fluid_pipe"); }

    private String textureOrDefault(String slot, String fallback) {
        FluidPipeBuilder source = source();
        if (source.textures.has(slot)) return source.textures.get(slot).getAsString();
        return source.allTexture == null ? fallback : source.allTexture;
    }

    @Override public FluidPipeBuilder noDrops() {
        super.noDrops();
        dropsDisabled = true;
        disabledLoot = lootTable;
        return this;
    }

    @Override public Block createObject() {
        validateBlockEntity();
        return switch (kind) {
            case REGULAR -> new ScriptedFluidPipeBlock(this);
            case GLASS -> new ScriptedGlassFluidPipeBlock(this);
            case ENCASED -> new ScriptedEncasedPipeBlock(this, AllBlocks.COPPER_CASING::get,
                    () -> (FluidPipeBlock) base.get());
        };
    }

    private void validateBlockEntity() {
        if (blockEntityInfo != null)
            throw new IllegalArgumentException("Fluid pipes own their Create block entity: " + id);
    }

    List<ResourceLocation> companionIds() {
        if (base != null) return List.of();
        var ids = new ArrayList<ResourceLocation>();
        ids.add(companionId("glass_"));
        if (encased) ids.add(companionId("copper_encased_"));
        return List.copyOf(ids);
    }

    private ResourceLocation companionId(String prefix) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), prefix + id.getPath());
    }

    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        validateBlockEntity();
        for (var child : companionIds()) {
            if (RegistryInfo.BLOCK.objects.containsKey(child) || RegistryInfo.ITEM.objects.containsKey(child))
                throw new IllegalArgumentException("Generated fluid pipe companion ID already exists: " + child);
        }
        additionalCreated = true;
        super.createAdditionalObjects();
        ModFluidPipes.add(this);
        if (base != null) return;
        glass = companion(Kind.GLASS, "glass_");
        if (encased) copperEncased = companion(Kind.ENCASED, "copper_encased_");
    }

    private FluidPipeBuilder companion(Kind kind, String prefix) {
        var child = new FluidPipeBuilder(companionId(prefix), kind, this);
        RegistryInfo.BLOCK.addBuilder(child);
        child.createAdditionalObjects();
        return child;
    }

    @Override public void generateAssetJsons(AssetJsonGenerator generator) { PipeAssets.generate(this, generator); }

    @Override public void generateDataJsons(DataJsonGenerator generator) { generateLoot(generator, id); }

    void generateLoot(DataJsonGenerator generator, ResourceLocation target) {
        FluidPipeBuilder source = source();
        JsonObject loot;
        if (source.lootTable != null && source.lootTable != source.disabledLoot) {
            var custom = new LootBuilder(null);
            custom.type = "minecraft:block";
            source.lootTable.accept(custom);
            loot = custom.toJson();
        } else loot = GearAssets.defaultLoot(source.dropsDisabled || source.itemBuilder == null ? null : source.id);
        generator.json(ResourceLocation.fromNamespaceAndPath(target.getNamespace(), "loot_tables/blocks/" + target.getPath()), loot);
    }
}
