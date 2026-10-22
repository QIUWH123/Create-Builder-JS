package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** A custom casing around Create's ordinary shaft. */
public class CustomEncasedShaftBlock extends EncasedShaftBlock {
    private final net.quiwh.cbjs.kubejs.ShaftBuilder shaft;
    public CustomEncasedShaftBlock(Properties properties, Supplier<Block> casing) {
        this(properties, casing, null);
    }
    public CustomEncasedShaftBlock(Properties properties, Supplier<Block> casing,
                                   net.quiwh.cbjs.kubejs.ShaftBuilder shaft) {
        super(properties, casing);
        this.shaft = shaft;
    }

    public net.quiwh.cbjs.kubejs.ShaftBuilder sourceBuilder() { return shaft; }
    private Block original() { return shaft == null ? com.simibubi.create.AllBlocks.SHAFT.get() : shaft.get(); }

    @Override public net.minecraft.world.InteractionResult onSneakWrenched(
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.item.context.UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            context.getLevel().levelEvent(2001, context.getClickedPos(), Block.getId(state));
            KineticBlockEntity.switchToBlockState(context.getLevel(), context.getClickedPos(),
                    net.quiwh.cbjs.kubejs.CompanionInheritance.transfer(state, original().defaultBlockState())
                            .setValue(AXIS, state.getValue(AXIS)));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override public void handleEncasing(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.item.ItemStack heldItem, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        KineticBlockEntity.switchToBlockState(level, pos,
                net.quiwh.cbjs.kubejs.CompanionInheritance.transfer(state, defaultBlockState())
                        .setValue(AXIS, state.getValue(AXIS)));
    }

    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.HitResult target,
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player) {
        if (target instanceof net.minecraft.world.phys.BlockHitResult hit
                && hit.getDirection().getAxis() == getRotationAxis(state)) return new net.minecraft.world.item.ItemStack(original());
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override public com.simibubi.create.content.schematics.requirement.ItemRequirement getRequiredItems(
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.entity.BlockEntity entity) {
        return com.simibubi.create.content.schematics.requirement.ItemRequirement.of(original().defaultBlockState(), entity);
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return CustomCasingBlockEntities.ENCASED_SHAFT.get();
    }
}
