package net.quiwh.cbjs.compat.bnb.mixin.customchain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Upgrade old custom-material NBT to native type/item fields before native decoding. */
@Mixin(value = CogwheelChain.class, remap = false)
public abstract class BnbChainDataMixin {
    @Inject(method = "placeInLevel", at = @At("HEAD"), cancellable = true)
    private void cbjs$preserveStandaloneOwner(net.minecraft.world.level.Level level,
            com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain placing,
            boolean creative, CallbackInfo callback) {
        for (var node : placing.getNodes()) {
            if (!level.hasChunkAt(node.pos()) || net.quiwh.cbjs.drives.DriveManager.hasDrive(level, node.pos())) {
                callback.cancel();
                return;
            }
        }
    }

    @Inject(method = "read", at = @At("HEAD"))
    private void cbjs$migrateMaterial(CompoundTag tag, CallbackInfo callback) {
        String key = tag.contains("CBJSChainItem") ? "CBJSChainItem" : "KineticJSChainItem";
        if (!tag.contains(key) || tag.contains("returned_item")) return;
        ResourceLocation id = ChainMaterials.normalize(ResourceLocation.tryParse(tag.getString(key)));
        var type = CogwheelChainType.COGWHEEL_TYPE_BY_ITEM.get(BuiltInRegistries.ITEM.get(id));
        if (type != null) {
            tag.putString("returned_item", id.toString());
            tag.putString("chain_type", type.getKey().toString());
        }
    }
}
