package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.AllBlocks;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.custom.BasicKubeBlock;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.quiwh.cbjs.content.ScriptedGlassFluidPipeBlock;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** A normal script block with independently selectable Create encasing capabilities. */
public final class CasingBuilder extends CBJSBlockBuilder {
    private boolean cogwheels;
    private boolean shafts;
    private boolean fluidPipes;
    private boolean additionalCreated;

    /** Replace one texture slot while preserving the family's default geometry. */
    public CasingBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }

    public CasingBuilder(ResourceLocation id) {
        super(id);
        hardness = 1.5F;
        resistance = 6;
        soundType = SoundType.WOOD;
    }

    public CasingBuilder cogwheelCasing() { return cogwheelCasing(true); }

    public CasingBuilder cogwheelCasing(boolean enabled) {
        cogwheels = enabled;
        return this;
    }

    public CasingBuilder shaftCasing() { return shaftCasing(true); }

    public CasingBuilder shaftCasing(boolean enabled) {
        shafts = enabled;
        return this;
    }

    public CasingBuilder fluidCasing() { return fluidCasing(true); }

    public CasingBuilder fluidCasing(boolean enabled) {
        fluidPipes = enabled;
        return this;
    }

    /** The block role supplies the ordinary casing faces and its encased companions. */
    @Override public CasingBuilder texture(String type, String path) {
        if ("block".equals(type)) {
            super.textureAll(path);
        }
        super.texture(type, path);
        return this;
    }

    public String casingTexture() {
        if (textures.containsKey("all")) return textures.get("all");
        // KubeJS textureAll fills six named faces rather than an "all" slot.
        if (textures.containsKey("north")) return textures.get("north");
        return id.getNamespace() + ":block/" + id.getPath();
    }

    public String casingConnectedTexture() { return optionalTexture("connected"); }
    public String cogwheelSideTexture() { return optionalTexture("encased_cogwheel_side"); }
    public String cogwheelSideConnectedTexture() { return optionalTexture("encased_cogwheel_side_connected"); }
    public String gearboxTexture() {
        String value = optionalTexture("gearbox");
        return value == null ? "create:block/gearbox" : value;
    }
    public String encasedPipeTexture() {
        String value = optionalTexture("encased_pipe");
        return value == null ? "create:block/encased_pipe" : value;
    }

    private String optionalTexture(String name) {
        return textures.containsKey(name) ? textures.get(name) : null;
    }

    /** Pure planning keeps companion selection independent of startup-script declaration order. */
    List<CasingVariantBuilder> planVariants(List<CogwheelBuilder> gears) {
        return planVariants(gears, List.of());
    }

    List<CasingVariantBuilder> planVariants(List<CogwheelBuilder> gears, List<FluidPipeBuilder> pipes) {
        return planVariants(gears, pipes, List.of());
    }

    List<CasingVariantBuilder> planVariants(List<CogwheelBuilder> gears, List<FluidPipeBuilder> pipes, List<ShaftBuilder> shaftBuilders) {
        List<CasingVariantBuilder> result = new ArrayList<>();
        if (shafts) result.add(variant("shaft", CasingVariantBuilder.Kind.SHAFT, null));
        if (shafts) for (ShaftBuilder shaft : shaftBuilders) {
            if (!shaft.encasedVariantsEnabled()) continue;
            var variant = variant(shaft.id.getPath(), CasingVariantBuilder.Kind.SHAFT, null);
            variant.shaftSource(shaft);
            result.add(variant);
        }
        if (cogwheels) {
            result.add(variant("cogwheel", CasingVariantBuilder.Kind.COGWHEEL, null));
            result.add(variant("large_cogwheel", CasingVariantBuilder.Kind.LARGE_COGWHEEL, null));
            for (CogwheelBuilder gear : uniqueByIdentity(gears)) {
                if (gear.base != null || !gear.encasedVariantsEnabled()) continue;
                result.add(variant(gear.id.getPath(),
                        gear.large ? CasingVariantBuilder.Kind.LARGE_COGWHEEL : CasingVariantBuilder.Kind.COGWHEEL,
                        gear));
            }
        }
        if (fluidPipes) {
            result.add(variant("fluid_pipe", CasingVariantBuilder.Kind.PIPE, null));
            for (FluidPipeBuilder pipe : uniqueByIdentity(pipes)) {
                if (pipe.base != null || !pipe.encasedVariantsEnabled()) continue;
                result.add(new CasingVariantBuilder(companionId(pipe.id.getPath()), this,
                        CasingVariantBuilder.Kind.PIPE, null, pipe));
            }
        }
        Map<ResourceLocation, String> sources = new java.util.LinkedHashMap<>();
        for (var variant : result) {
            String source = variant.gear != null ? variant.gear.id.toString()
                    : variant.pipe != null ? variant.pipe.id.toString() : "<stock>";
            String previous = sources.putIfAbsent(variant.id, source);
            if (previous != null)
                throw new IllegalArgumentException("Generated casing companion ID collision: " + variant.id
                        + "; sources " + previous + " and " + source
                        + " must have distinct paths when encased by " + id);
        }
        return List.copyOf(result);
    }

    private static <T> List<T> uniqueByIdentity(List<T> builders) {
        var seen = new IdentityHashMap<T, Boolean>();
        var unique = new ArrayList<T>(builders.size());
        for (T builder : builders)
            if (seen.put(builder, Boolean.TRUE) == null) unique.add(builder);
        return unique;
    }

    private CasingVariantBuilder variant(String suffix, CasingVariantBuilder.Kind kind, CogwheelBuilder gear) {
        return new CasingVariantBuilder(companionId(suffix), this, kind, gear);
    }

    ResourceLocation companionId(String basePath) {
        String prefix = id.getPath();
        if (prefix.endsWith("_casing")) prefix = prefix.substring(0, prefix.length() - "_casing".length());
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), prefix + "_encased_" + basePath);
    }

    @Override public void createAdditionalObjects(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        if (additionalCreated) return;
        if ((cogwheels || shafts || fluidPipes) && itemBuilder == null)
            throw new IllegalArgumentException("Encasing casing " + id + " needs an item; remove noItem()");
        // Every script handler has run, but other builders' additional-object callbacks may not have.
        List<CogwheelBuilder> gears = new ArrayList<>();
        List<FluidPipeBuilder> pipes = new ArrayList<>();
        List<ShaftBuilder> shaftBuilders = new ArrayList<>();
        for (var builder : RegistryObjectStorage.BLOCK) {
            if (builder instanceof CogwheelBuilder gear && gear.base == null) gears.add(gear);
            if (builder instanceof FluidPipeBuilder pipe && pipe.base == null) pipes.add(pipe);
            if (builder instanceof ShaftBuilder shaft) shaftBuilders.add(shaft);
        }
        List<CasingVariantBuilder> variants = planVariants(gears, pipes, shaftBuilders);
        for (var variant : variants) {
            if (RegistryObjectStorage.BLOCK.objects.containsKey(variant.id) || RegistryObjectStorage.ITEM.objects.containsKey(variant.id))
                throw new IllegalArgumentException("Generated casing companion ID already exists: " + variant.id);
        }
        additionalCreated = true;
        super.createAdditionalObjects(registry);
        ModCasings.addCasing(this);
        for (var variant : variants) {
            registry.add(net.minecraft.core.registries.Registries.BLOCK, variant);
            variant.createAdditionalObjects(registry);
        }
    }

    @Override public Block createObject() {
        return blockEntityInfo == null ? new ScriptedCasingBlock(this) : new ScriptedEntityCasingBlock(this);
    }

    private static final class ScriptedCasingBlock extends BasicKubeBlock implements IWrenchable {
        ScriptedCasingBlock(BlockBuilder builder) { super(builder); }
        @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
            return InteractionResult.FAIL;
        }
    }

    private static final class ScriptedEntityCasingBlock extends BasicKubeBlock.WithEntity implements IWrenchable {
        ScriptedEntityCasingBlock(BlockBuilder builder) { super(builder); }
        @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
            return InteractionResult.FAIL;
        }
    }

    /** Axis/glass pipes bypass Create's general encasing registry and hardcode copper. */
    public static InteractionResult tryEncaseAxisPipe(BlockState state, Level level, BlockPos pos,
                                                      Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(state.getBlock() instanceof AxisPipeBlock) || !player.mayBuild()) return InteractionResult.PASS;
        var held = player.getItemInHand(hand);
        if (held.isEmpty()) return InteractionResult.PASS;
        FluidPipeBlock original = AllBlocks.FLUID_PIPE.get();
        if (state.getBlock() instanceof ScriptedGlassFluidPipeBlock glass) {
            original = glass.originalPipe();
            if (AllBlocks.COPPER_CASING.isIn(held)) {
                EncasedPipeBlock target = glass.sourceBuilder().copperEncasedBlock();
                // Stop AxisPipeBlock's stock copper path when the family opts out of encasing.
                return target == null ? InteractionResult.FAIL : encaseAxisPipe(state, level, pos, player, target);
            }
        }
        for (var variant : ModCasings.pipeVariants()) {
            if (variant.originalBlock() != original || variant.casing.get().asItem() != held.getItem()) continue;
            return encaseAxisPipe(state, level, pos, player, (EncasedPipeBlock) variant.get());
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult encaseAxisPipe(BlockState state, Level level, BlockPos pos,
                                                    Player player, EncasedPipeBlock target) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockState encased = CompanionInheritance.transfer(state, target.defaultBlockState());
        Direction.Axis axis = state.getValue(AxisPipeBlock.AXIS);
        for (Direction direction : Direction.values())
            encased = encased.setValue(EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(direction), direction.getAxis() == axis);
        FluidTransportBehaviour.cacheFlows(level, pos);
        level.setBlockAndUpdate(pos, encased);
        FluidTransportBehaviour.loadFlows(level, pos);
        SoundType sound = encased.getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
        return InteractionResult.SUCCESS;
    }
}
