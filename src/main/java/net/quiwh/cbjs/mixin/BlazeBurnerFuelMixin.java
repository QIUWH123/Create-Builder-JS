package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;
import com.simibubi.create.api.registry.CreateDataMaps;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent.Rule;
import net.quiwh.cbjs.fuels.BlazeBurnerFuelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Changes only classification/duration; Create retains insertion, simulation and heat logic. */
@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerFuelMixin {
    // Native data maps precede legacy tags and furnace burn time in Create 6.
    // Supplying the selected map keeps native insertion and simulation authoritative.
    @WrapOperation(method = "tryUpdateFuel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/Holder;getData(Lnet/neoforged/neoforge/registries/datamaps/DataMapType;)Ljava/lang/Object;"),
            require = 2, allow = 2)
    private Object cbjs$scriptFuel(Holder<Item> holder, DataMapType<Item, BlazeBurnerFuel> type,
            Operation<Object> original, @Local(argsOnly = true) ItemStack stack) {
        Rule rule = BlazeBurnerFuelManager.find(cbjs$block(), stack);
        if (rule == null) return BlazeBurnerFuelManager.usesVanillaFuels(cbjs$block()) ? original.call(holder, type) : null;
        if (type == CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS)
            return rule.superheated() ? new BlazeBurnerFuel(rule.burnTicks()) : null;
        if (type == CreateDataMaps.REGULAR_BLAZE_BURNER_FUELS)
            return rule.superheated() ? null : new BlazeBurnerFuel(rule.burnTicks());
        return original.call(holder, type);
    }
    @WrapOperation(method = "tryUpdateFuel", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/AllTags$AllItemTags;matches(Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 2, allow = 2)
    private boolean cbjs$nativeTags(com.simibubi.create.AllTags.AllItemTags tag, ItemStack stack,
            Operation<Boolean> original) {
        return BlazeBurnerFuelManager.usesVanillaFuels(cbjs$block()) && original.call(tag, stack);
    }
    @WrapOperation(method = "tryUpdateFuel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getBurnTime(Lnet/minecraft/world/item/crafting/RecipeType;)I"),
            require = 1, allow = 1)
    private int cbjs$nativeBurnTime(ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> recipeType,
            Operation<Integer> original) {
        return BlazeBurnerFuelManager.usesVanillaFuels(cbjs$block()) ? original.call(stack, recipeType) : 0;
    }
    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.world.level.block.Block cbjs$block() {
        var state = ((BlazeBurnerBlockEntity)(Object)this).getBlockState();
        return state == null ? null : state.getBlock();
    }
}
