package net.quiwh.cbjs.kinetics;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.quiwh.cbjs.api.KineticData;
import net.quiwh.cbjs.api.event.KineticDataModifyEvent;

/** Tests real world destruction, Create network demand, script validation and reload replacement. */
@GameTestHolder("cbjs")
@PrefixGameTestTemplate(false)
public final class GearLimitGameTests {
    @GameTest(template = "empty3x3x3")
    public static void builderDefaultsCanBeOverriddenOnceAndReturnOnReload(GameTestHelper helper) {
        Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.parse("kubejs:wooden_cogwheel"));
        helper.assertTrue(block != null && block != net.minecraft.world.level.block.Blocks.AIR,
                "The supplied startup script did not register its wooden cogwheel");
        var defaults = GearLimitDefaults.buildRules();
        helper.assertTrue(defaults.get(block).maxSpeed() == 64,
                "Wooden builder RPM default was not recorded");
        KineticDataModifyEvent override = new KineticDataModifyEvent(defaults);
        override.modifyKineticData("kubejs:wooden_cogwheel").maxRpm(128);
        KineticData overridden = override.buildRules().get(block);
        helper.assertTrue(overridden.maxSpeed() == 128
                        && overridden.maxNetworkStress().equals(defaults.get(block).maxNetworkStress())
                        && overridden.independentRpmLimit()
                        && java.util.Objects.equals(overridden.list(), defaults.get(block).list()),
                "Explicit rule could not replace a builder default");
        helper.assertTrue(new KineticDataModifyEvent(GearLimitDefaults.buildRules())
                        .buildRules().get(block).maxSpeed() == 64,
                "A prior override mutated the next reload's builder defaults");
        try {
            override.modifyKineticData("kubejs:wooden_cogwheel").maxRpm(256);
            helper.fail("Duplicate explicit override was accepted");
        } catch (IllegalArgumentException expected) {
            try {
                override.buildRules();
                helper.fail("A caught duplicate error permitted partial rule publication");
            } catch (IllegalStateException rejected) {
                helper.succeed();
            }
        }
    }

    @GameTest(template = "empty3x3x3")
    public static void rpmBoundaryDropsReloadAndTickInjection(GameTestHelper helper) {
        Block block = AllBlocks.COGWHEEL.get();
        BlockPos pos = new BlockPos(1, 1, 1);
        var tileDrops = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
        boolean previousTileDrops = tileDrops.get();
        try {
            helper.assertTrue(GearLimitManager.reload(event -> event.addKineticDataWithDrops(
                    block, 64, null, List.of(new ItemStack(Items.STICK, 3)))), "Valid rules rejected");
            KineticBlockEntity entity = place(helper, pos, 64);
            GearLimitManager.check(entity);
            helper.assertBlockPresent(block, pos);

            // Even a script engine that swallows an invalid API call cannot publish a partial set.
            helper.assertFalse(GearLimitManager.reload(event -> {
                event.addKineticDataWithDrops(block, 1000, null, List.of());
                try {
                    event.modifyKineticData("cbjs:missing_gear").maxRpm(64);
                } catch (IllegalArgumentException expected) {
                    // Emulate KubeJS reporting a handler exception and continuing dispatch.
                }
            }), "A malformed reload replaced working limits");
            tileDrops.set(false, helper.getLevel().getServer());
            entity.setSpeed(-65);
            GearLimitManager.check(entity);
            helper.assertTrue(helper.getBlockState(pos).isAir(), "Reverse overspeed did not break the gear");
            helper.assertItemEntityCountIs(Items.STICK, pos, 2, 3);
            helper.assertItemEntityNotPresent(block.asItem(), pos, 2);
            helper.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
            tileDrops.set(true, helper.getLevel().getServer());

            GearLimitManager.reload(event -> event.addKineticDataWithDrops(block, 64, null, List.of()));
            GearLimitManager.check(place(helper, pos, 65));
            helper.assertTrue(helper.getBlockState(pos).isAir(), "Empty-drop rule did not break the gear");
            helper.assertEntityNotPresent(EntityType.ITEM);

            GearLimitManager.reload(event -> event.addKineticDataWithDrops(block, 64, null, null));
            GearLimitManager.check(place(helper, pos, 65));
            helper.assertItemEntityCountIs(block.asItem(), pos, 2, 1);
            helper.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);

            GearLimitManager.reload(event -> {});
            entity = place(helper, pos, 65);
            GearLimitManager.check(entity);
            helper.assertBlockPresent(block, pos);

            GearLimitManager.reload(event -> event.addKineticDataWithDrops(AllBlocks.CREATIVE_MOTOR.get(), 0, null, List.of()));
            // Run Create's actual tick to prove the mixin delegates to the manager.
            helper.setBlock(pos, AllBlocks.CREATIVE_MOTOR.getDefaultState());
            entity = (KineticBlockEntity) helper.getBlockEntity(pos);
            entity.updateSpeed = false;
            entity.setSpeed(65);
            entity.tick();
            helper.assertTrue(helper.getBlockState(pos).isAir(), "Kinetic tick mixin did not enforce the rule");
            helper.succeed();
        } finally {
            tileDrops.set(previousTileDrops, helper.getLevel().getServer());
            GearLimitManager.reload();
        }
    }

    @GameTest(template = "empty3x3x3")
    public static void totalNetworkDemandBreaksEvenWhenCreateHasStoppedRotation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        try {
            GearLimitManager.reload(event -> event.addKineticDataWithDrops(AllBlocks.COGWHEEL.get(),
                    null, 8192, List.of()));
            KineticBlockEntity entity = place(helper, pos, 64);
            entity.setNetwork(helper.absolutePos(pos).asLong());
            var network = entity.getOrCreateNetwork();
            network.members.put(entity, 128f);
            entity.updateFromNetwork(0, 8192, 1);
            helper.assertTrue(entity.getSpeed() == 0, "Test network must be ordinarily overstressed");
            helper.assertTrue(network.calculateStress() == 8192, "Unexpected connected demand");
            GearLimitManager.check(entity);
            helper.assertBlockPresent(AllBlocks.COGWHEEL.get(), pos);
            network.members.put(entity, 129f);
            GearLimitManager.check(entity);
            helper.assertTrue(helper.getBlockState(pos).isAir(), "Excess network demand did not break the gear");
            helper.succeed();
        } finally {
            GearLimitManager.reload();
        }
    }

    @GameTest(template = "empty3x3x3")
    public static void apiRejectsInvalidLimitsAndDefensivelyCopiesDrops(GameTestHelper helper) {
        ItemStack original = new ItemStack(Items.STICK, 3);
        KineticData data = new KineticData(AllBlocks.COGWHEEL.get(), 64, 8192, List.of(original));
        original.setCount(20);
        data.list().get(0).setCount(1);
        helper.assertTrue(data.list().get(0).getCount() == 3, "Rule drop stacks can be modified externally");
        KineticDataModifyEvent event = new KineticDataModifyEvent();
        try {
            event.addKineticDataWithDrops(AllBlocks.COGWHEEL.get(), -1, null, null);
            helper.fail("Negative RPM accepted");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    private static KineticBlockEntity place(GameTestHelper helper, BlockPos pos, float speed) {
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(pos, AllBlocks.COGWHEEL.getDefaultState());
        KineticBlockEntity entity = (KineticBlockEntity) helper.getBlockEntity(pos);
        entity.setSpeed(speed);
        return entity;
    }

    private GearLimitGameTests() {
    }
}
