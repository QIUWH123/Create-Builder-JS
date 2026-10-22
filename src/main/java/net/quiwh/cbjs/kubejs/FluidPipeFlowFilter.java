package net.quiwh.cbjs.kubejs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;

/** Parsed listener selectors. Holder membership stays live when datapack tags reload. */
public final class FluidPipeFlowFilter {
    private final RegistrySelector<Fluid> fluid;
    private final RegistrySelector<Block> block;
    private final boolean either;

    private FluidPipeFlowFilter(RegistrySelector<Fluid> fluid, RegistrySelector<Block> block, boolean either) {
        this.fluid = fluid;
        this.block = block;
        this.either = either;
    }

    public static FluidPipeFlowFilter of(Object fluid, Object block) {
        return new FluidPipeFlowFilter(RegistrySelector.parse(fluid, Registries.FLUID, "fluidPipeFlow fluid"),
                RegistrySelector.parse(block, Registries.BLOCK, "fluidPipeFlow block"), false);
    }

    /** Single selectors preserve fluid-ID priority; tags can match fluids or pipe blocks. */
    public static FluidPipeFlowFilter either(Object selector) {
        // Validate the entire input before partitioning, including members after a wildcard.
        RegistrySelector.parse(selector, Registries.FLUID, "fluidPipeFlow fluid or block");
        var fluids = new ArrayList<Object>();
        var blocks = new ArrayList<Object>();
        for (Object entry : RegistrySelector.values(selector)) {
            Object value = RegistrySelector.scalarValue(entry);
            if (value == null || "*".equals(value) || value instanceof String text && text.startsWith("#")) {
                fluids.add(value == null ? "*" : value);
                blocks.add(value == null ? "*" : value);
            } else {
                ResourceLocation id = value instanceof ResourceLocation location ? location
                        : ResourceLocation.parse((String) value);
                if (!ForgeRegistries.FLUIDS.containsKey(id) && ForgeRegistries.BLOCKS.containsKey(id)) blocks.add(id);
                else fluids.add(id);
            }
        }
        return new FluidPipeFlowFilter(RegistrySelector.parse(fluids, Registries.FLUID, "fluidPipeFlow fluid"),
                RegistrySelector.parse(blocks, Registries.BLOCK, "fluidPipeFlow block"), true);
    }

    public boolean matchesBlock(Block block) {
        // A fluid alternative may match a connection on any pipe; block-only lists stay selective.
        return either ? !fluid.isEmpty() || this.block.matches(block.builtInRegistryHolder())
                : !fluid.isEmpty() && this.block.matches(block.builtInRegistryHolder());
    }

    public boolean matches(Block block, Fluid fluid) {
        return either
                ? this.fluid.matches(fluid.builtInRegistryHolder()) || this.block.matches(block.builtInRegistryHolder())
                : this.fluid.matches(fluid.builtInRegistryHolder()) && this.block.matches(block.builtInRegistryHolder());
    }
}
