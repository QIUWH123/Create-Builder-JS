package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.quiwh.cbjs.content.ScriptedBlazeBurnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Custom burners retain their own entity type while satisfying the native keeper check. */
@Mixin(value = StockTickerBlockEntity.class, remap = false)
public abstract class BlazeBurnerStockTickerMixin {
    @WrapOperation(method = "isKeeperPresent", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntityEntry;is(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z"), require = 1, allow = 1)
    private boolean cbjs$scriptedKeeper(BlockEntityEntry<?> entry, BlockEntity entity, Operation<Boolean> original) {
        return entity instanceof ScriptedBlazeBurnerBlockEntity burner
                ? ((net.quiwh.cbjs.content.ScriptedBlazeBurnerBlock)burner.getBlockState().getBlock()).sourceBuilder().options().canBeStockKeeper
                : original.call(entry, entity);
    }
}
