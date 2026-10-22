package net.quiwh.cbjs.client;

import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.quiwh.cbjs.content.ScriptedPumpBlock;
import net.quiwh.cbjs.content.ScriptedFluidValveBlock;
import net.quiwh.cbjs.kubejs.FluidValveBuilder;
import net.quiwh.cbjs.kubejs.PumpBuilder;
import net.quiwh.cbjs.kubejs.SmartFluidPipeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Preserves Create's pump rotation and dynamic pipe attachments for custom materials. */
public final class FluidDeviceClientModels {
    // Flywheel's own PartialModel table uses weak values. Retain declarations across model
    // loading and reuse them while rendering, including before a block is first visible.
    private static final Map<ResourceLocation, PartialModel> PUMP_COGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, PartialModel> VALVE_POINTERS = new ConcurrentHashMap<>();
    private FluidDeviceClientModels() { }

    public static void register(PumpBuilder builder) {
        // Partial models must be declared before the model-loading event, not on first render.
        pumpCog(builder);
        registerAttachments(builder.get(), builder.pipeTexture());
    }

    public static void register(SmartFluidPipeBuilder builder) {
        registerAttachments(builder.get(), builder.pipeTexture());
    }

    public static void register(FluidValveBuilder builder) {
        valvePointer(builder);
        registerAttachments(builder.get(), builder.pipeTexture());
    }

    public static ResourceLocation valvePointerLocation(ResourceLocation blockId) {
        return ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath() + "/pointer");
    }

    /** Both native render paths keep Create's animation and select only the material's pointer. */
    public static PartialModel valvePointer(BlockState state, PartialModel nativePointer) {
        if (state.getBlock() instanceof ScriptedFluidValveBlock valve)
            return valvePointer(valve.sourceBuilder());
        return nativePointer;
    }

    private static PartialModel valvePointer(FluidValveBuilder builder) {
        return VALVE_POINTERS.computeIfAbsent(builder.id, id -> PartialModel.of(valvePointerLocation(id)));
    }

    private static void registerAttachments(Block block, String pipeTexture) {
        CreateRegistrate.blockModel(() -> model -> new DevicePipeModel(model, pipeTexture)).accept(block);
    }

    public static ResourceLocation pumpCogLocation(ResourceLocation blockId) {
        return ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath() + "/cog");
    }

    private static PartialModel pumpCog(PumpBuilder builder) {
        return PUMP_COGS.computeIfAbsent(builder.id, id -> PartialModel.of(pumpCogLocation(id)));
    }

    private static PartialModel pumpCog(BlockState state) {
        return pumpCog(((ScriptedPumpBlock) state.getBlock()).sourceBuilder());
    }

    public static final class MaterialPumpRenderer extends PumpRenderer {
        public MaterialPumpRenderer(BlockEntityRendererProvider.Context context) { super(context); }
        @Override protected SuperByteBuffer getRotatedModel(PumpBlockEntity entity, BlockState state) {
            return CachedBuffers.partialFacing(pumpCog(state), state);
        }
    }

    public static final class MaterialPumpVisual extends SingleAxisRotatingVisual<PumpBlockEntity> {
        public MaterialPumpVisual(VisualizationContext context, PumpBlockEntity entity, float partialTick) {
            super(context, entity, partialTick, Direction.SOUTH, Models.partial(pumpCog(entity.getBlockState())));
        }
    }

    private static final class DevicePipeModel extends PipeAttachmentModel {
        private static final ResourceLocation NATIVE_PIPE = ResourceLocation.parse("create:block/pipes");
        private final ResourceLocation pipeTexture;
        private final Map<BakedQuad, BakedQuad> retextured = new ConcurrentHashMap<>();

        private DevicePipeModel(BakedModel original, String pipeTexture) {
            super(original, true);
            this.pipeTexture = ResourceLocation.parse(pipeTexture);
        }

        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                ModelData data, RenderType renderType) {
            List<BakedQuad> original = super.getQuads(state, side, random, data, renderType);
            if (NATIVE_PIPE.equals(pipeTexture)) return original;
            List<BakedQuad> result = null;
            for (int i = 0; i < original.size(); i++) {
                BakedQuad quad = original.get(i);
                if (!NATIVE_PIPE.equals(quad.getSprite().contents().name())) continue;
                if (result == null) result = new ArrayList<>(original);
                result.set(i, retextured.computeIfAbsent(quad, key -> retexture(key,
                        Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(pipeTexture))));
            }
            return result == null ? original : result;
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
