package net.quiwh.cbjs.mixin;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.FluidTankCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FluidTankItem.class, remap = false)
public abstract class FluidTankItemMixin {
    @Redirect(method = "tryMultiPlace", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/tank/FluidTankBlock;isTank(Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            require = 3, allow = 3)
    private boolean cbjs$bulkMaterial(BlockState state) {
        return FluidTankCompat.bulkTank(((FluidTankItem)(Object)this).getBlock(), state);
    }

    @Redirect(method = "tryMultiPlace", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/api/connectivity/ConnectivityHandler;partAt(Lnet/minecraft/world/level/block/entity/BlockEntityType;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"),
            require = 1, allow = 1)
    private net.minecraft.world.level.block.entity.BlockEntity cbjs$bulkEntityType(
            BlockEntityType<?> nativeType, BlockGetter level, BlockPos pos) {
        return ConnectivityHandler.partAt(FluidTankCompat.bulkType(((FluidTankItem)(Object)this).getBlock(), nativeType), level, pos);
    }

    @Redirect(method = {"updateCustomBlockEntityTag", "m_7274_"}, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/tank/FluidTankBlockEntity;getCapacityMultiplier()I"),
            require = 1, allow = 1)
    private int cbjs$customItemCapacity() {
        return FluidTankCompat.itemCapacity(((FluidTankItem)(Object)this).getBlock(), FluidTankBlockEntity.getCapacityMultiplier());
    }
}
