package net.quiwh.cbjs.kinetics;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.kubejs.CBJSEvents;
import net.quiwh.cbjs.kubejs.KineticOverloadEventJS;

/** Real registered entities and Create network mutation paths; requires Forge's mixin runtime. */
@GameTestHolder("cbjs")
@PrefixGameTestTemplate(false)
public final class NetworkStressGameTests {
    private static final BlockPos GEAR = new BlockPos(0, 1, 1);
    private static final BlockPos CONSUMER = new BlockPos(2, 1, 1);

    @GameTest(template = "empty3x3x3", batch = "cbjs_stress_cache")
    public static void removedConsumerDoesNotBreakSurvivingGear(GameTestHelper helper) {
        Block gear = AllBlocks.COGWHEEL.get();
        try {
            limit(helper, gear);
            Network fixture = network(helper, gear);
            demand(helper, fixture.gear, 8256, "Initial load must exceed the gear's 4096 SU limit");
            helper.getLevel().destroyBlock(fixture.consumer.getBlockPos(), false);
            // Check before the remaining entity gets a native dirty-network tick.
            GearLimitManager.check(fixture.gear);
            helper.assertBlockPresent(gear, GEAR);
            demand(helper, fixture.gear, 64, "Removal must invalidate the previously excessive demand");
            helper.succeed();
        } finally {
            GearLimitManager.reload();
        }
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_stress_cache")
    public static void sameTickSpeedAndPublicMapChangesRefreshDemand(GameTestHelper helper) {
        Network fixture = network(helper, AllBlocks.COGWHEEL.get());
        demand(helper, fixture.gear, 8256, "Initial demand");
        fixture.consumer.setSpeed(32);
        demand(helper, fixture.gear, 4160, "setSpeed must invalidate demand before another tick");
        fixture.consumer.setSpeed(-32);
        demand(helper, fixture.gear, 4160, "Demand uses absolute theoretical speed");
        fixture.network.members.put(fixture.consumer, 2f);
        demand(helper, fixture.gear, 128, "Direct registered-impact replacement must invalidate demand");
        fixture.network.members.entrySet().stream()
                .filter(entry -> entry.getKey() == fixture.consumer).findFirst().orElseThrow().setValue(3f);
        demand(helper, fixture.gear, 160, "Entry.setValue must invalidate demand");
        fixture.network.members = new HashMap<>(fixture.network.members);
        fixture.network.members.put(fixture.consumer, 4f);
        demand(helper, fixture.gear, 192, "Replacing the public members map must not retain the old snapshot");
        fixture.network.members.put(fixture.consumer, 5f);
        demand(helper, fixture.gear, 224, "The replacement map must track subsequent mutations too");
        fixture.network.updateStressFor(fixture.consumer, 6f);
        demand(helper, fixture.gear, 256, "A native demand recalculation must supply the new snapshot");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_stress_cache")
    public static void chunkUnloadLifecycleInvalidatesStaleMembers(GameTestHelper helper) {
        Network fixture = network(helper, AllBlocks.COGWHEEL.get());
        demand(helper, fixture.gear, 8256, "Initial demand");
        // Run the block-entity portion of chunk unloading without unloading the GameTest's chunk.
        // SmartBlockEntity then skips KineticBlockEntity.remove(), leaving native stale-member pruning.
        fixture.consumer.onChunkUnloaded();
        helper.getLevel().removeBlockEntity(fixture.consumer.getBlockPos());
        helper.assertTrue(helper.getLevel().getBlockEntity(fixture.consumer.getBlockPos()) != fixture.consumer,
                "The unloaded consumer must no longer be the world's current block entity");
        helper.assertTrue(fixture.network.members.containsKey(fixture.consumer),
                "This path must leave the stale member for native calculateStress pruning");
        demand(helper, fixture.gear, 64, "Chunk unload must invalidate demand before the next read");
        helper.assertFalse(fixture.network.members.containsKey(fixture.consumer),
                "The refreshed native calculation must prune the unloaded member");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_stress_cache")
    public static void removalCallbackDoesNotReuseDemandReadBetweenUnloadPhases(GameTestHelper helper) {
        Network fixture = network(helper, AllBlocks.COGWHEEL.get());
        demand(helper, fixture.gear, 8256, "Initial demand");
        int[] callbacks = {0};
        float[] callbackDemand = {-1};
        fixture.consumer.attachBehaviourLate(new BlockEntityBehaviour(fixture.consumer) {
            private final BehaviourType<BlockEntityBehaviour> type = new BehaviourType<>();

            @Override
            public BehaviourType<?> getType() {
                return type;
            }

            @Override
            public void unload() {
                callbacks[0]++;
                callbackDemand[0] = NetworkStressAccess.get(fixture.gear);
            }
        });
        fixture.consumer.onChunkUnloaded();
        // Forge marks every entity unloaded before calling setRemoved on them. Other callbacks
        // can therefore repopulate the cache while this consumer is still a current world entity.
        demand(helper, fixture.gear, 8256, "Demand read between chunk-unload phases");
        fixture.consumer.setRemoved();
        helper.assertTrue(callbacks[0] == 1 && callbackDemand[0] == 64,
                "A real behavior unload callback must see demand without the newly removed consumer");
        demand(helper, fixture.gear, 64, "Demand must remain fresh after removal callbacks finish");
        helper.assertFalse(fixture.network.members.containsKey(fixture.consumer),
                "The callback's native demand calculation must prune the removed member");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_stress_cache")
    public static void overloadCallbackCanRemoveDemandWithoutCancelling(GameTestHelper helper) {
        AtomicReference<CallbackState> live = new AtomicReference<>();
        // The event registration retains only this holder; finally releases its world/entity references.
        CBJSEvents.KINETIC_OVERLOAD.listenJava(ScriptType.SERVER, null, raw -> {
            CallbackState state = live.get();
            var event = (KineticOverloadEventJS) raw;
            if (state == null || event.getLevel() != state.fixture.gear.getLevel()
                    || !event.getPos().equals(state.fixture.gear.getBlockPos())) return null;
            state.calls++;
            state.reportedDemand = event.getStress();
            event.getLevel().destroyBlock(state.fixture.consumer.getBlockPos(), false);
            return null;
        });
        try {
            Block gear = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("kubejs:steel_cogwheel"));
            helper.assertTrue(gear != null && gear != Blocks.AIR, "Missing supplied scripted steel gear");
            limit(helper, gear);
            Network fixture = network(helper, gear);
            CallbackState state = new CallbackState(fixture);
            live.set(state);
            demand(helper, fixture.gear, 8256, "Initial cached demand must exceed the limit");
            GearLimitManager.check(fixture.gear);
            helper.assertTrue(state.calls == 1 && state.reportedDemand == 8256,
                    "The real overload listener must observe the excessive initial demand once");
            helper.assertTrue(helper.getBlockState(CONSUMER).isAir(), "The listener must remove the consumer");
            helper.assertBlockPresent(gear, GEAR);
            demand(helper, fixture.gear, 64, "The post-callback recheck must use repaired demand");
            helper.succeed();
        } finally {
            live.set(null);
            GearLimitManager.reload();
        }
    }

    private static void limit(GameTestHelper helper, Block gear) {
        helper.assertTrue(GearLimitManager.reload(event -> event.addKineticDataWithDrops(
                gear, null, 4096, List.of())), "Temporary stress limit rejected");
    }

    private static Network network(GameTestHelper helper, Block gearBlock) {
        KineticBlockEntity gear = place(helper, GEAR, gearBlock);
        KineticBlockEntity consumer = place(helper, CONSUMER, AllBlocks.SHAFT.get());
        Long id = gear.getBlockPos().asLong();
        gear.setNetwork(id);
        consumer.setNetwork(id);
        KineticNetwork network = gear.getOrCreateNetwork();
        helper.assertTrue(network == consumer.getOrCreateNetwork() && network instanceof NetworkStressAccess,
                "Both registered entities must share the required mixin-backed Create network");
        // Use Create's registered-impact API, then exercise actual speed/membership changes above.
        network.updateStressFor(gear, 1);
        network.updateStressFor(consumer, 128);
        return new Network(gear, consumer, network);
    }

    private static KineticBlockEntity place(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, Blocks.AIR);
        helper.setBlock(pos, block.defaultBlockState());
        KineticBlockEntity entity = (KineticBlockEntity) helper.getBlockEntity(pos);
        entity.updateSpeed = false;
        entity.setSpeed(64);
        return entity;
    }

    private static void demand(GameTestHelper helper, KineticBlockEntity entity, float expected, String message) {
        float actual = NetworkStressAccess.get(entity);
        helper.assertTrue(actual == expected, message + ": expected " + expected + ", got " + actual);
    }

    private record Network(KineticBlockEntity gear, KineticBlockEntity consumer, KineticNetwork network) {}

    private static final class CallbackState {
        final Network fixture;
        int calls;
        double reportedDemand;

        CallbackState(Network fixture) {
            this.fixture = fixture;
        }
    }

    private NetworkStressGameTests() {}
}
