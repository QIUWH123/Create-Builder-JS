package net.quiwh.cbjs.kubejs;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.CustomCasingBlockEntities;
import net.quiwh.cbjs.content.CustomEncasedCogwheelBlock;
import net.quiwh.cbjs.content.CustomEncasedPipeBlock;
import net.quiwh.cbjs.content.CustomEncasedShaftBlock;
import net.quiwh.cbjs.content.MaterialCogwheelBlock;
import net.quiwh.cbjs.content.MaterialEncasedCogwheelBlock;
import net.quiwh.cbjs.content.ModBlockEntities;
import net.quiwh.cbjs.content.ScriptedEncasedShaftBlock;
import net.quiwh.cbjs.content.ScriptedEncasedPipeBlock;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** Internal companions generated once for each requested part and each base material gear. */
final class CasingVariantBuilder extends BlockBuilder {
    enum Kind { SHAFT, COGWHEEL, LARGE_COGWHEEL, PIPE }
    final CasingBuilder casing;
    final Kind kind;
    final CogwheelBuilder gear;
    final FluidPipeBuilder pipe;
    ShaftBuilder shaft;
    private boolean additionalCreated;

    CasingVariantBuilder(ResourceLocation id, CasingBuilder casing, Kind kind, CogwheelBuilder gear) {
        this(id, casing, kind, gear, null);
    }

    CasingVariantBuilder(ResourceLocation id, CasingBuilder casing, Kind kind, CogwheelBuilder gear, FluidPipeBuilder pipe) {
        super(id);
        this.casing = casing;
        this.kind = kind;
        this.gear = gear;
        this.pipe = pipe;
        hardness = casing.hardness;
        resistance = casing.resistance;
        soundType = casing.soundType;
        lightLevel = casing.lightLevel;
        CompanionInheritance.copyMining(casing, this);
        // Create also uses noOcclusion for encased pipes, keeping light around their ports.
        opaque = false;
        suffocating = false;
        viewBlocking = false;
        notSolid = true;
        if (kind != Kind.PIPE) {
            renderType = "cutout_mipped";
        }
        if (gear != null) CompanionInheritance.copy(gear, this, true);
        else if (pipe != null) CompanionInheritance.copy(pipe, this, true);
    }

    void shaftSource(ShaftBuilder shaft) {
        this.shaft = shaft;
        CompanionInheritance.copy(shaft, this, true);
    }

    @Override public Block createObject() {
        if (gear != null)
            return new ScriptedEncasedCogwheelBlock(this, gear,
                    casing, () -> (MaterialCogwheelBlock) gear.get());
        return switch (kind) {
            case SHAFT -> shaft == null ? new CustomEncasedShaftBlock(createProperties(), casing)
                    : new ScriptedEncasedShaftBlock(this, casing, shaft);
            case COGWHEEL -> new CustomEncasedCogwheelBlock(createProperties(), false, casing);
            case LARGE_COGWHEEL -> new CustomEncasedCogwheelBlock(createProperties(), true, casing);
            case PIPE -> pipe == null ? new CustomEncasedPipeBlock(createProperties(), casing)
                    : new ScriptedEncasedPipeBlock(this, casing, () -> (FluidPipeBlock) pipe.get());
        };
    }

    Block originalBlock() {
        if (gear != null) return gear.get();
        if (pipe != null) return pipe.get();
        if (shaft != null) return shaft.get();
        return switch (kind) {
            case SHAFT -> AllBlocks.SHAFT.get();
            case COGWHEEL -> AllBlocks.COGWHEEL.get();
            case LARGE_COGWHEEL -> AllBlocks.LARGE_COGWHEEL.get();
            case PIPE -> AllBlocks.FLUID_PIPE.get();
        };
    }

    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        additionalCreated = true;
        super.createAdditionalObjects();
        if (gear != null) {
            ModBlockEntities.addEncasedValidBlock(this);
            GearLimitDefaults.register(this, gear.maxRpm, gear.maxStress, gear.independentRpmLimit, null);
        } else switch (kind) {
            case SHAFT -> CustomCasingBlockEntities.addShaft(this);
            case COGWHEEL -> CustomCasingBlockEntities.addCogwheel(this, false);
            case LARGE_COGWHEEL -> CustomCasingBlockEntities.addCogwheel(this, true);
            case PIPE -> CustomCasingBlockEntities.addPipe(this);
        }
        ModCasings.add(this);
        if (shaft != null)
            GearLimitDefaults.register(this, shaft.maxRpm, shaft.maxStress, shaft.independentRpmLimit, null);
    }

    @Override public void generateAssetJsons(AssetJsonGenerator generator) { CasingAssets.generate(this, generator); }

    @Override public void generateDataJsons(DataJsonGenerator generator) {
        if (shaft != null) { shaft.generateLoot(generator, id); return; }
        if (gear != null) {
            gear.generateLoot(generator, id);
            return;
        }
        if (pipe != null) {
            pipe.generateLoot(generator, id);
            return;
        }
        ResourceLocation original = switch (kind) {
            case SHAFT -> ResourceLocation.parse("create:shaft");
            case COGWHEEL -> ResourceLocation.parse("create:cogwheel");
            case LARGE_COGWHEEL -> ResourceLocation.parse("create:large_cogwheel");
            case PIPE -> ResourceLocation.parse("create:fluid_pipe");
        };
        generator.json(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "loot_tables/blocks/" + id.getPath()),
                GearAssets.defaultLoot(original));
    }
}
