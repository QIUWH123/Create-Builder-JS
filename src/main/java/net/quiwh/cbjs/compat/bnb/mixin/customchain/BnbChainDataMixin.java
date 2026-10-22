package net.quiwh.cbjs.compat.bnb.mixin.customchain;

import com.kipti.bnb.content.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.cogwheel_chain.graph.PlacingCogwheelChain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.compat.bnb.customchain.BnbChainMaterial;
import net.quiwh.cbjs.compat.bnb.customchain.BnbChainMaterialContext;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CogwheelChain.class, remap = false)
public abstract class BnbChainDataMixin implements BnbChainMaterial {
    @Unique private ResourceLocation cbjs$chainItem;

    @Override public ResourceLocation cbjs$chainMaterial() {
        return cbjs$chainItem == null ? ChainMaterials.VANILLA : cbjs$chainItem;
    }
    @Override public void cbjs$chainMaterial(ResourceLocation id) {
        cbjs$chainItem = ChainMaterials.normalize(id);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void cbjs$readMaterial(CompoundTag tag, CallbackInfo callback) {
        String key = tag.contains("CBJSChainItem") ? "CBJSChainItem" : "KineticJSChainItem";
        cbjs$chainMaterial(ResourceLocation.tryParse(tag.getString(key)));
    }
    @Inject(method = "write", at = @At("TAIL"))
    private void cbjs$writeMaterial(CompoundTag tag, CallbackInfo callback) {
        tag.remove("KineticJSChainItem");
        tag.putString("CBJSChainItem", cbjs$chainMaterial().toString());
    }
    @Inject(method = "placeInLevel", at = @At("HEAD"))
    private void cbjs$rememberMaterial(Level level, PlacingCogwheelChain source, CallbackInfo callback) {
        ResourceLocation selected = BnbChainMaterialContext.placing();
        if (selected != null) cbjs$chainMaterial(selected);
    }
}
