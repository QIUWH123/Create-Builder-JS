package net.quiwh.cbjs.kubejs;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.fluids.PipeConnectionState;

import java.util.Objects;

/** Active completed flow at one pipe side this tick; the sample stack does not measure throughput. */
public final class FluidPipeFlowEventJS extends FluidPipeEventJS {
    private final String side;
    private final Block filterBlock;
    private final PipeConnectionState state;

    public FluidPipeFlowEventJS(SmartBlockEntity entity, Direction side, PipeConnectionState state) {
        super(entity);
        this.side = side.getName();
        this.filterBlock = entity.getBlockState().getBlock();
        this.state = state;
    }

    public String getSide() { return side; }
    public String getFluidId() { return Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(state.fluidType())).toString(); }
    public FluidStack getFluid() { return state.fluid(); }
    public boolean getIsInbound() { return state.inbound(); }

    @HideFromJS public Block getFilterBlock() { return filterBlock; }
    @HideFromJS public Fluid getFilterFluid() { return state.fluidType(); }
}
