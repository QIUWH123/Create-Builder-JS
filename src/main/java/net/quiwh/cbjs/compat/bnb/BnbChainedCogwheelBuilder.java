package net.quiwh.cbjs.compat.bnb;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;
import net.quiwh.cbjs.kubejs.CogwheelBuilder;
import net.minecraftforge.registries.RegisterEvent;

/** A chain-created form of an exposed gear; it has no separate inventory item. */
public final class BnbChainedCogwheelBuilder extends BlockBuilder {
    private static final Map<CogwheelBuilder, BnbChainedCogwheelBuilder> COMPANIONS = new IdentityHashMap<>();
    private static final Map<Block, BnbChainedCogwheelBlock> BY_SOURCE = new IdentityHashMap<>();
    private final CogwheelBuilder source;
    private boolean additionalCreated;
    private static boolean hooksRegistered;

    public BnbChainedCogwheelBuilder(CogwheelBuilder source) {
        super(ResourceLocation.fromNamespaceAndPath(source.id.getNamespace(), "chained_" + source.id.getPath()));
        this.source = source;
        net.quiwh.cbjs.kubejs.ContraptionRules.inherit(this, source);
        renderType = source.renderType;
        noItem();
    }

    static void add(CogwheelBuilder source) {
        if (COMPANIONS.containsKey(source)) return;
        var companion = new BnbChainedCogwheelBuilder(source);
        if (RegistryInfo.BLOCK.objects.containsKey(companion.id) || RegistryInfo.ITEM.objects.containsKey(companion.id))
            throw new IllegalArgumentException("Generated chained cogwheel ID already exists: " + companion.id);
        COMPANIONS.put(source, companion);
        RegistryInfo.BLOCK.addBuilder(companion);
        // KubeJS has already copied its initial collection when it invokes this callback.
        companion.createAdditionalObjects();
    }

    public CogwheelBuilder source() { return source; }

    public static BnbChainedCogwheelBlock forSource(BlockState state) {
        return BY_SOURCE.get(state.getBlock());
    }

    static void onRegister(RegisterEvent event) {
        if (hooksRegistered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (var builder : COMPANIONS.values()) BlockStressValues.IMPACTS.register(builder.get(), () -> 0);
        hooksRegistered = true;
    }

    @Override public Block createObject() {
        var block = new BnbChainedCogwheelBlock(source);
        BY_SOURCE.put(source.get(), block);
        return block;
    }

    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        additionalCreated = true;
        super.createAdditionalObjects();
        BnbChainBlockEntities.add(this);
        GearLimitDefaults.register(this, source.maxRpm, source.maxStress, source.independentRpmLimit, null);
    }

    @Override public void generateAssetJsons(AssetJsonGenerator generator) {
        BnbChainedCogwheelAssets.generate(this, generator);
    }

    @Override public void generateDataJsons(DataJsonGenerator generator) {
        // The block uses its source's exact loot-table ID, including noDrops and custom loot.
    }
}
