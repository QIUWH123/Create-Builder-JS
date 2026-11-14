package net.quiwh.cbjs.compat.createchain;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.quiwh.cbjs.content.ScriptedChainConveyorBlock;

/** Admission limits for new edges; existing saved connections remain intact when limits shrink. */
public final class ConveyorConnections {
    private ConveyorConnections() { }

    public static int maximum(ChainConveyorBlockEntity conveyor) {
        if (conveyor.getBlockState().getBlock() instanceof ScriptedChainConveyorBlock block) {
            Integer configured = block.sourceBuilder().maxConnections;
            if (configured != null) return configured;
        }
        return AllConfigs.server().kinetics.maxChainConveyorConnections.get();
    }

    public static boolean canAdd(ChainConveyorBlockEntity conveyor) {
        return conveyor.connections.size() < maximum(conveyor);
    }
}
