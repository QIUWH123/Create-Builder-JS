package net.quiwh.cbjs.compat.bnb;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.registry.core.BnbResourceKeys;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.kubejs.ChainBuilder;

/** Each script chain material is a native BnB type, preserving its item and texture end to end. */
public final class BnbNativeChainTypes {
    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(BnbResourceKeys.COGWHEEL_CHAIN_TYPE)) return;
        event.register(BnbResourceKeys.COGWHEEL_CHAIN_TYPE, helper -> {
            for (var builder : RegistryObjectStorage.BLOCK) {
                if (!(builder instanceof ChainBuilder chain) || chain.itemBuilder == null) continue;
                helper.register(chain.id, new CogwheelChainType(1F, CogwheelChainType.ChainRenderInfo.CHAIN,
                        texture(chain), item -> item == chain.itemBuilder.get(), block -> true, true, chain::get) {
                    // Match mutable builder texture behavior used by native conveyor connections.
                    @Override public ResourceLocation getRenderTexture() { return texture(chain); }
                });
            }
        });
    }

    public static ResourceLocation texture(ChainBuilder chain) {
        ResourceLocation sprite = ResourceLocation.parse(chain.chainTexture());
        return ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
    }

    private BnbNativeChainTypes() { }
}
