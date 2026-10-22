package net.quiwh.cbjs.client;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.quiwh.cbjs.kubejs.FluidPipeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Uses Create's attachment choices while replacing their pipe atlas for each material. */
public final class FluidPipeClientModels {
    private FluidPipeClientModels() { }

    @SuppressWarnings("removal")
    public static void register(FluidPipeBuilder builder) {
        register(builder.get(), builder.source());
        if (builder.kind == FluidPipeBuilder.Kind.GLASS)
            ItemBlockRenderTypes.setRenderLayer(builder.get(), RenderType.cutoutMipped());
        if (builder.kind == FluidPipeBuilder.Kind.ENCASED) {
            CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.COPPER_CASING)).accept(builder.get());
            CreateRegistrate.casingConnectivity((block, connectivity) -> connectivity.make(block, AllSpriteShifts.COPPER_CASING,
                    (state, face) -> !state.getValue(EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(face)))).accept(builder.get());
        }
    }

    /** Also used by a script casing's generated variant of the same material pipe. */
    public static void register(Block block, FluidPipeBuilder source) {
        CreateRegistrate.blockModel(() -> model -> new MaterialPipeModel(model, source)).accept(block);
    }

    private static final class MaterialPipeModel extends PipeAttachmentModel {
        private final Map<ResourceLocation, ResourceLocation> replacements;
        // Chunk meshes can be built concurrently. Each original baked quad is immutable and shared.
        private final Map<BakedQuad, BakedQuad> retextured = new ConcurrentHashMap<>();

        private MaterialPipeModel(BakedModel original, FluidPipeBuilder source) {
            super(original, true);
            var maps = new java.util.HashMap<ResourceLocation, ResourceLocation>();
            replacement(maps, "pipes", source.pipeTexture());
            replacement(maps, "pipes_connected", source.connectedTexture());
            replacement(maps, "glass_fluid_pipe", source.glassTexture());
            replacements = Map.copyOf(maps);
        }

        private static void replacement(Map<ResourceLocation, ResourceLocation> replacements, String original, String target) {
            ResourceLocation from = ResourceLocation.parse("create:block/" + original);
            ResourceLocation to = ResourceLocation.parse(target);
            if (!from.equals(to)) replacements.put(from, to);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            List<BakedQuad> original = super.getQuads(state, side, random, data, renderType);
            if (replacements.isEmpty()) return original;
            List<BakedQuad> result = null;
            for (int i = 0; i < original.size(); i++) {
                BakedQuad quad = original.get(i);
                ResourceLocation target = replacements.get(quad.getSprite().contents().name());
                if (target == null) continue;
                if (result == null) result = new ArrayList<>(original);
                result.set(i, retextured.computeIfAbsent(quad, key -> retexture(key, target)));
            }
            return result == null ? original : result;
        }

        private static BakedQuad retexture(BakedQuad original, ResourceLocation target) {
            TextureAtlasSprite to = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(target);
            return retexture(original, to);
        }

        private static BakedQuad retexture(BakedQuad original, TextureAtlasSprite to) {
            TextureAtlasSprite from = original.getSprite();
            int[] vertices = original.getVertices().clone();
            for (int vertex = 0; vertex < 4; vertex++) {
                float u = (BakedQuadHelper.getU(vertices, vertex) - from.getU0()) / (from.getU1() - from.getU0());
                float v = (BakedQuadHelper.getV(vertices, vertex) - from.getV0()) / (from.getV1() - from.getV0());
                BakedQuadHelper.setU(vertices, vertex, to.getU(u * 16));
                BakedQuadHelper.setV(vertices, vertex, to.getV(v * 16));
            }
            return new BakedQuad(vertices, original.getTintIndex(), original.getDirection(), to,
                    original.isShade(), original.hasAmbientOcclusion());
        }
    }
}
