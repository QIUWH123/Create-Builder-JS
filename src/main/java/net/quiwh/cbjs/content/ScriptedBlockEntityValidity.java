package net.quiwh.cbjs.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

/**
 * Defers native block-entity valid-block updates until after KubeJS has registered
 * its startup blocks. Registrate snapshots validBlock calls when its entry is built,
 * which is earlier than KubeJS's dynamic block registration callback.
 */
public final class ScriptedBlockEntityValidity {
    private static final List<Pending> PENDING = new ArrayList<>();

    private ScriptedBlockEntityValidity() {}

    public static synchronized void add(Supplier<? extends BlockEntityType<?>> type,
                                        Supplier<? extends Block> block) {
        PENDING.add(new Pending(Objects.requireNonNull(type, "type"), Objects.requireNonNull(block, "block")));
    }

    public static void apply(BlockEntityTypeAddBlocksEvent event) {
        List<Pending> pending;
        synchronized (ScriptedBlockEntityValidity.class) {
            pending = List.copyOf(PENDING);
        }

        // NeoForge memoizes the common superclass for each type. Calling modify once
        // per block lets the first scripted block become that cached superclass when
        // the type starts with no valid blocks, rejecting sibling block classes that
        // belong to the same native block-entity implementation. Resolve and batch
        // all additions per type so the event validates the complete set together.
        Map<BlockEntityType<?>, List<Block>> additions = new LinkedHashMap<>();
        for (Pending registration : pending) {
            additions.computeIfAbsent(registration.type().get(), ignored -> new ArrayList<>())
                    .add(registration.block().get());
        }
        for (Map.Entry<BlockEntityType<?>, List<Block>> entry : additions.entrySet()) {
            event.modify(entry.getKey(), entry.getValue().toArray(Block[]::new));
        }
    }

    private record Pending(Supplier<? extends BlockEntityType<?>> type,
                           Supplier<? extends Block> block) {}
}
