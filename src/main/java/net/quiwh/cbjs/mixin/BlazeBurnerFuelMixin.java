package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent.Rule;
import net.quiwh.cbjs.fuels.BlazeBurnerFuelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Changes only classification/duration; Create retains insertion, simulation and heat logic. */
@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerFuelMixin {
    @WrapOperation(method = "tryUpdateFuel", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/AllTags$AllItemTags;matches(Lnet/minecraft/world/item/ItemStack;)Z",
            ordinal = 0), require = 1, allow = 1)
    private boolean cbjs$specialFuel(AllItemTags tag, ItemStack stack, Operation<Boolean> original,
            @Share("cbjs$fuel") LocalRef<Rule> selected) {
        Rule rule = BlazeBurnerFuelManager.find(stack);
        selected.set(rule);
        return rule == null ? original.call(tag, stack) : rule.superheated();
    }

    @ModifyConstant(method = "tryUpdateFuel", constant = @Constant(intValue = 3200), require = 1, allow = 1)
    private int cbjs$specialBurnTicks(int original, @Share("cbjs$fuel") LocalRef<Rule> selected) {
        Rule rule = selected.get();
        return rule == null ? original : rule.burnTicks();
    }

    @WrapOperation(method = "tryUpdateFuel", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeHooks;getBurnTime(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/RecipeType;)I"),
            require = 1, allow = 1)
    private int cbjs$normalBurnTicks(ItemStack stack, RecipeType<?> recipeType, Operation<Integer> original,
            @Share("cbjs$fuel") LocalRef<Rule> selected) {
        Rule rule = selected.get();
        return rule == null ? original.call(stack, recipeType) : rule.burnTicks();
    }
}
