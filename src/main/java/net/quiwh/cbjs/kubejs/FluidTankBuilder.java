package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.fluids.tank.FluidTankItem;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.content.ScriptedFluidTankBlock;
import net.minecraft.core.Direction;
import net.quiwh.cbjs.content.FluidWindowMode;
import net.quiwh.cbjs.content.FluidWindowFaces;

/** Native Create tanks with per-registration storage, dimensions and artwork. */
public final class FluidTankBuilder extends FluidDeviceBuilder {
    public static final Set<String> TEXTURE_ROLES = Set.of("fluid_tank", "fluid_tank_connected",
            "fluid_tank_top", "fluid_tank_top_connected", "fluid_tank_inner", "fluid_tank_inner_connected",
            "fluid_tank_window", "fluid_tank_window_single", "boiler_gauge");
    private final boolean vessel;
    private Integer blockCapacity;
    private Integer maxWidth;
    private Integer maxHeight;
    private boolean windows = true;
    private boolean initialWindows = true;
    private boolean independentWindows;
    private List<FluidWindowMode> windowStyles = List.of(FluidWindowMode.STRIP, FluidWindowMode.FULL, FluidWindowMode.NONE);
    private FluidWindowMode sideWindow = FluidWindowMode.STRIP;
    private FluidWindowMode endWindow = FluidWindowMode.NONE;
    private boolean steamEngine = true;
    private List<FluidSelector> fluidSelectors = List.of();

    public FluidTankBuilder(ResourceLocation id) { this(id, false); }

    public FluidTankBuilder(ResourceLocation id, boolean vessel) {
        super(id);
        this.vessel = vessel;
        renderType = "cutout_mipped";
        getOrCreateItemBuilder();
    }

    public FluidTankBuilder capacity(int value) { blockCapacity = positive("capacity", value); return this; }
    public FluidTankBuilder maxSize(int value) { maxWidth = positive("maxSize", value); return this; }
    /** Remove the width cap; integer storage bounds still apply. */
    public FluidTankBuilder noMaxSize() { maxWidth = 0; return this; }
    public boolean isVessel() { return vessel; }
    public FluidTankBuilder maxLength(int value) {
        if (!vessel) throw new IllegalArgumentException("maxLength is only available for fluid_vessel; use maxHeight for fluid_tank");
        maxHeight = positive("maxLength", value); return this;
    }
    public FluidTankBuilder maxHeight(int value) { maxHeight = positive("maxHeight", value); return this; }
    public FluidTankBuilder noMaxHeight() { maxHeight = 0; return this; }
    public FluidTankBuilder noMaxLength() {
        if (!vessel) throw new IllegalArgumentException("noMaxLength is only available for fluid_vessel; use noMaxHeight for fluid_tank");
        return noMaxHeight();
    }
    public FluidTankBuilder noWindow() { windows = false; return this; }
    public FluidTankBuilder noDefaultWindow() { initialWindows = false; return this; }
    /** Opt into selecting each exterior multiblock face separately with a wrench. */
    public FluidTankBuilder independentWindows() { independentWindows = true; return this; }
    public boolean independentWindowsEnabled() { return independentWindows; }
    /** Wrench cycle order used when independent windows are enabled. */
    public FluidTankBuilder windowStyles(List<String> styles) {
        if (styles == null || styles.isEmpty())
            throw new IllegalArgumentException("windowStyles requires at least one of full, none or strip");
        List<FluidWindowMode> parsed = styles.stream().map(FluidWindowMode::parse).toList();
        if (Set.copyOf(parsed).size() != parsed.size())
            throw new IllegalArgumentException("windowStyles may not contain duplicate styles");
        windowStyles = parsed;
        return this;
    }
    /** Set the starting design of all six faces. */
    public FluidTankBuilder windowStyle(String style) {
        return windowStyle("all", style);
    }
    /** Set the four longitudinal sides, two axial ends, or all six faces. */
    public FluidTankBuilder windowStyle(String group, String style) {
        FluidWindowMode mode = FluidWindowMode.parse(style);
        if (group == null) throw new IllegalArgumentException("Unknown window group: null; use side, end or all");
        switch (group) {
            case "side" -> sideWindow = mode;
            case "end" -> endWindow = mode;
            case "all" -> { sideWindow = mode; endWindow = mode; }
            default -> throw new IllegalArgumentException("Unknown window group: " + group + "; use side, end or all");
        }
        return this;
    }
    public List<FluidWindowMode> allowedWindowStyles() { return windowStyles; }
    public FluidWindowFaces defaultWindowFaces() {
        return defaultWindowFaces(vessel ? Direction.Axis.X : Direction.Axis.Y);
    }
    public FluidWindowFaces defaultWindowFaces(Direction.Axis axis) {
        return initialWindows ? enabledWindowFaces(axis) : FluidWindowFaces.filled(FluidWindowMode.NONE);
    }
    /** The enabled layout is independent from whether new blocks start with windows. */
    public FluidWindowFaces enabledWindowFaces() {
        return enabledWindowFaces(vessel ? Direction.Axis.X : Direction.Axis.Y);
    }
    public FluidWindowFaces enabledWindowFaces(Direction.Axis axis) {
        java.util.Objects.requireNonNull(axis, "axis");
        if (!windows) return FluidWindowFaces.filled(FluidWindowMode.NONE);
        FluidWindowFaces result = FluidWindowFaces.filled(sideWindow);
        for (Direction face : Direction.values()) if (face.getAxis() == axis) result = result.with(face, endWindow);
        for (Direction face : Direction.values())
            if (result.get(face) != FluidWindowMode.NONE && !windowStyles.contains(result.get(face)))
                result = result.with(face, windowStyles.get(0));
        return result;
    }
    public FluidTankBuilder noSteamEngine() { steamEngine = false; return this; }
    public boolean windowsAllowed() { return windows; }
    public boolean defaultWindows() { return defaultWindowFaces().anyGlass(); }
    public boolean steamEngineAllowed() { return steamEngine; }
    public int maximumWidth() {
        int safe = net.quiwh.cbjs.content.StorageStructureLimits.maximumWidth(capacityPerBlock());
        return maxWidth == null ? Math.min(3, safe) : maxWidth == 0 ? safe : Math.min(maxWidth, safe);
    }

    public int capacityPerBlock() {
        return configuredCapacityPerBlock();
    }

    private int configuredCapacityPerBlock() {
        if (blockCapacity != null) return blockCapacity;
        var config = AllConfigs.server();
        // Startup scripts run before the world/server configuration is loaded.
        return config.specification == null || !config.specification.isLoaded()
                ? 8000 : (int) Math.min(Integer.MAX_VALUE, (long) config.fluids.fluidTankCapacity.get() * 1000);
    }

    public int maximumHeight() {
        return maximumLength(1);
    }
    public int maximumLength(int width) {
        int safe = net.quiwh.cbjs.content.StorageStructureLimits.maximumLength(capacityPerBlock(), width);
        return maxHeight == null ? Math.min(configuredMaximumHeight(), safe)
                : maxHeight == 0 ? safe : Math.min(maxHeight, safe);
    }

    private int configuredMaximumHeight() {
        var config = AllConfigs.server();
        return config.specification == null || !config.specification.isLoaded()
                ? 32 : config.fluids.fluidTankMaxHeight.get();
    }

    /** Validate explicit storage settings after the world configuration is available. */
    void resolveServerSettings() { validateConfiguration(); }

    public FluidTankBuilder fluidFilter(String selector) { return fluidFilter(java.util.Collections.singletonList(selector)); }

    public FluidTankBuilder fluidFilter(List<String> selectors) {
        if (selectors == null || selectors.isEmpty())
            throw new IllegalArgumentException("fluidFilter requires at least one fluid ID or #fluid tag");
        fluidSelectors = selectors.stream().map(FluidSelector::parse).toList();
        return this;
    }

    public boolean acceptsFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty() || fluidSelectors.isEmpty()) return true;
        for (FluidSelector selector : fluidSelectors)
            if (selector.matches(stack)) return true;
        return false;
    }

    public String texturePath(String role) {
        if (!TEXTURE_ROLES.contains(role)) throw new IllegalArgumentException("Unknown fluid tank texture role: " + role);
        return textureOrDefault(role, "create:block/" + role);
    }

    /** Separate atlas identities preserve CT roles even when several roles share one PNG. */
    public String spritePath(String role) {
        if (!TEXTURE_ROLES.contains(role)) throw new IllegalArgumentException("Unknown fluid tank texture role: " + role);
        return id.getNamespace() + ":block/" + id.getPath() + "/texture/" + role;
    }

    /** Validate the largest possible capacity without overflowing intermediate products. */
    public void validateConfiguration() {
        validateConfiguration(capacityPerBlock());
    }

    private void validateConfiguration(int capacity) {
        if (blockCapacity == null || maxWidth == null || maxHeight == null || maxWidth == 0 || maxHeight == 0) return;
        long remaining = Integer.MAX_VALUE / (long) capacity;
        remaining /= maxWidth;
        remaining /= maxWidth;
        if (remaining < maxHeight)
            throw new IllegalArgumentException("Fluid tank " + id
                    + " exceeds 2147483647 mB at its maximum dimensions; reduce capacity, maxSize or maxHeight");
    }

    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (itemBuilder == null || !(itemBuilder instanceof TankItemBuilder)) {
            itemBuilder = new TankItemBuilder(id);
            itemBuilder.blockBuilder = this;
        }
        return itemBuilder;
    }

    @Override public Block createObject() {
        validateBlockEntity();
        validateConfiguration();
        return vessel ? new net.quiwh.cbjs.content.ScriptedFluidVesselBlock(this) : new ScriptedFluidTankBlock(this);
    }

    @Override public FluidTankBuilder model(String path) {
        throw StorageAssetOverrides.unsupported(this, "model");
    }

    @Override protected void validateBlockEntity() {
        super.validateBlockEntity();
        StorageAssetOverrides.validate(this);
    }

    @Override protected void registerDevice() {
        validateConfiguration();
        ModFluidTanks.add(this);
    }

    @Override public void generateAssetJsons(AssetJsonGenerator generator) { FluidTankAssets.generate(this, generator); }

    private static int positive(String name, int value) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private record FluidSelector(ResourceLocation id, TagKey<net.minecraft.world.level.material.Fluid> tag) {
        static FluidSelector parse(String text) {
            var selector = RegistryFilterSelector.parse("fluidFilter", text);
            return new FluidSelector(selector.id(), selector.tagged() ? TagKey.create(Registries.FLUID, selector.id()) : null);
        }
        boolean matches(FluidStack stack) {
            return tag != null ? stack.getFluid().is(tag) : id.equals(ForgeRegistries.FLUIDS.getKey(stack.getFluid()));
        }
    }

    private final class TankItemBuilder extends BlockItemBuilder {
        TankItemBuilder(ResourceLocation id) { super(id); }
        @Override public Item createObject() { return vessel ? new net.quiwh.cbjs.content.ScriptedFluidVesselItem(blockBuilder.get(), createItemProperties())
                : new FluidTankItem(blockBuilder.get(), createItemProperties()); }
    }
}
