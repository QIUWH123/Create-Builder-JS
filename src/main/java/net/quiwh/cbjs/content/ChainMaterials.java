package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.api.KineticData;
import net.quiwh.cbjs.kinetics.GearLimitManager;
import net.quiwh.cbjs.kinetics.NetworkStressAccess;

/** Shared material validation for native conveyor edges and optional cogwheel chain loops. */
public final class ChainMaterials {
    public static final ResourceLocation VANILLA = ResourceLocation.parse("minecraft:chain");
    private static final ResourceLocation VANILLA_TEXTURE = ResourceLocation.parse("minecraft:textures/block/chain.png");
    // One entry per registered chain block, shared by both native conveyor and BnB renderers.
    private static final java.util.Map<ScriptedChainBlock, RenderTexture> TEXTURES = new java.util.concurrent.ConcurrentHashMap<>();
    private record RenderTexture(String source, ResourceLocation file) { }

    public static boolean isChain(ItemStack stack) {
        return stack != null && !stack.isEmpty() && isChain(stack.getItem());
    }

    private static boolean isChain(Item item) {
        return item == Items.CHAIN || item instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ScriptedChainBlock;
    }

    public static ResourceLocation id(ItemStack stack) {
        return isChain(stack) ? BuiltInRegistries.ITEM.getKey(stack.getItem()) : VANILLA;
    }

    /** Old saves and removed registrations degrade to a vanilla chain, never an arbitrary refund. */
    public static ResourceLocation normalize(ResourceLocation id) {
        return id != null && isChain(BuiltInRegistries.ITEM.get(id)) ? id : VANILLA;
    }

    public static ItemStack stack(ResourceLocation id, int count) {
        return count <= 0 ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(normalize(id)), count);
    }

    public static BlockState blockState(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(normalize(id));
        return item instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : Blocks.CHAIN.defaultBlockState();
    }

    /** Only registered custom chain items carry additional connection ratings. */
    public static KineticData limits(ResourceLocation id) {
        if (id == null || VANILLA.equals(id)) return null;
        Item item = BuiltInRegistries.ITEM.get(id);
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ScriptedChainBlock
                ? GearLimitManager.rule(blockItem.getBlock()) : null;
    }

    public static long limitsRevision() { return GearLimitManager.rulesVersion(); }

    /** Actual endpoint RPM and the shared network-demand cache, never a per-connection network scan. */
    public static boolean isOverloaded(ResourceLocation material, KineticBlockEntity endpoint) {
        KineticData data = limits(material);
        if (data == null) return false;
        if (data.maxSpeed() != null && Math.abs(endpoint.getSpeed()) > data.maxSpeed()) return true;
        return data.maxNetworkStress() != null && NetworkStressAccess.get(endpoint) > data.maxNetworkStress();
    }

    /** Native chain renderers bind a PNG directly instead of using the block atlas. */
    public static ResourceLocation texture(ResourceLocation id) {
        if (id == null || VANILLA.equals(id)) return VANILLA_TEXTURE;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (!(item instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ScriptedChainBlock chain))
            return VANILLA_TEXTURE;
        String source = chain.sourceBuilder().chainTexture();
        RenderTexture cached = TEXTURES.get(chain);
        if (cached != null && cached.source.equals(source)) return cached.file;
        // Builders remain mutable: refresh only the affected material after its texture changes.
        ResourceLocation sprite = ResourceLocation.parse(source);
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
        TEXTURES.put(chain, new RenderTexture(source, file));
        return file;
    }

    private ChainMaterials() { }
}
