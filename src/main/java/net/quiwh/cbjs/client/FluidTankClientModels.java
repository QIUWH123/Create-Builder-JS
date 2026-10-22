package net.quiwh.cbjs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.data.CreateRegistrate;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.quiwh.cbjs.content.FluidTankFaceLayout;
import net.quiwh.cbjs.content.FluidTankFaceLayout.Panel;
import net.quiwh.cbjs.content.FluidWindowMode;
import net.quiwh.cbjs.content.ScriptedFluidTankBlockEntity;
import net.quiwh.cbjs.kubejs.FluidTankFaceAssets;
import net.minecraftforge.client.model.data.ModelProperty;
import net.quiwh.cbjs.kubejs.FluidTankBuilder;

/** Native tank connected textures and fluid rendering with material-specific boiler gauges. */
public final class FluidTankClientModels {
    // Keep partials alive until model baking: Flywheel's global declarations hold weak values.
    public static final ModelProperty<Panel[]> FACE_PANELS = new ModelProperty<>();
    private static final Map<Block, Material> MATERIALS = new ConcurrentHashMap<>();

    public static void register(FluidTankBuilder builder) {
        Material material = new Material(builder);
        MATERIALS.put(builder.get(), material);
        CreateRegistrate.blockModel(() -> model -> new MaterialTankModel(model, material)).accept(builder.get());
    }

    public static ResourceLocation gaugeLocation(ResourceLocation blockId, boolean dial) {
        return ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath()
                + (dial ? "/gauge_dial" : "/gauge"));
    }

    private static final class Material {
        private final CTSpriteShiftEntry side;
        private final CTSpriteShiftEntry top;
        private final CTSpriteShiftEntry inner;
        private final PartialModel gauge;
        private final PartialModel dial;
        private final boolean customGauge;
        private final boolean vessel;
        private final Map<Panel, PartialModel> panels;

        private Material(FluidTankBuilder builder) {
            vessel = builder.isVessel();
            Map<Panel, PartialModel> parts = new java.util.HashMap<>();
            for (Panel panel : FluidTankFaceLayout.templates()) {
                if (vessel == (panel.mainAxis() == Direction.Axis.Y)) continue;
                parts.put(panel, PartialModel.of(FluidTankFaceAssets.modelLocation(builder.id, panel)));
            }
            panels = Map.copyOf(parts);
            side = shift(builder, "fluid_tank");
            top = shift(builder, "fluid_tank_top");
            inner = shift(builder, "fluid_tank_inner");
            gauge = PartialModel.of(gaugeLocation(builder.id, false));
            dial = PartialModel.of(gaugeLocation(builder.id, true));
            customGauge = !builder.texturePath("boiler_gauge").equals("create:block/boiler_gauge");
        }

        private static CTSpriteShiftEntry shift(FluidTankBuilder builder, String role) {
            return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, ResourceLocation.parse(builder.spritePath(role)),
                    ResourceLocation.parse(builder.spritePath(role + "_connected")));
        }
    }

    /** Select fragments before CT rewrites UVs; glass and reveals retain outer-face cull groups. */
    private static final class FacePanelModel extends BakedModelWrapper<BakedModel> {
        private final Material material;
        private FacePanelModel(BakedModel original, Material material) { super(original); this.material = material; }

        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                ModelData data, RenderType renderType) {
            if (!data.has(FACE_PANELS)) return super.getQuads(state, side, random, data, renderType);
            if (side == null) return List.of();
            Panel panel = data.get(FACE_PANELS)[side.get3DDataValue()];
            if (panel == null) return List.of();
            PartialModel model = material.panels.get(panel);
            if (model == null) return List.of();
            return model.get().getQuads(state, side, random, ModelData.EMPTY, renderType);
        }
    }

    private static final class MaterialTankModel extends CTModel {
        private MaterialTankModel(BakedModel original, Material material) {
            super(new FacePanelModel(original, material), material.vessel
                    ? new FluidVesselCTBehaviour(material.side, material.top, material.inner)
                    : new SurfaceTankCTBehaviour(material.side, material.top, material.inner));
        }

        @Override protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world,
                BlockPos pos, BlockState state, ModelData blockEntityData) {
            super.gatherModelData(builder, world, pos, state, blockEntityData);
            if (world.getBlockEntity(pos) instanceof ScriptedFluidTankBlockEntity tank)
                builder.with(FACE_PANELS, tank.facePanels());
            return builder;
        }

        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                ModelData data, RenderType renderType) {
            if (side != null) return List.of();
            List<BakedQuad> quads = new ArrayList<>();
            for (Direction direction : Iterate.directions)
                quads.addAll(super.getQuads(state, direction, random, data, renderType));
            quads.addAll(super.getQuads(state, null, random, data, renderType));
            return quads;
        }
    }

    /** Window reveals can face up/down while carrying the lateral sheet (and vice versa). */
    private static final class SurfaceTankCTBehaviour extends FluidTankCTBehaviour {
        private final CTSpriteShiftEntry side, top, inner;

        private SurfaceTankCTBehaviour(CTSpriteShiftEntry side, CTSpriteShiftEntry top, CTSpriteShiftEntry inner) {
            super(side, top, inner);
            this.side = side; this.top = top; this.inner = inner;
        }

        @Override public CTSpriteShiftEntry getShift(BlockState state, Direction face,
                net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
            if (sprite == null) return super.getShift(state, face, null);
            if (sprite == side.getOriginal()) return side;
            if (sprite == top.getOriginal()) return top;
            if (sprite == inner.getOriginal()) return inner;
            return null;
        }
    }

    /** Horizontal hulls keep fluid surfaces aligned with gravity. */
    public static final class MaterialVesselRenderer extends MaterialTankRenderer {
        public MaterialVesselRenderer(BlockEntityRendererProvider.Context context) { super(context); }

        @Override protected void renderSafe(FluidTankBlockEntity entity, float partialTick, PoseStack pose,
                MultiBufferSource buffers, int light, int overlay) {
            if (!(entity instanceof net.quiwh.cbjs.content.ScriptedFluidTankBlockEntity tank)
                    || tank.getMainConnectionAxis() == Direction.Axis.Y) {
                super.renderSafe(entity, partialTick, pose, buffers, light, overlay);
                return;
            }
            if (!tank.isController()) return;
            if (!tank.hasWindow()) {
                if (tank.boiler.isActive()) renderAsBoiler(tank, partialTick, pose, buffers, light, overlay);
                return;
            }
            if (tank.getFluidLevel() == null || tank.getTankInventory().getFluid().isEmpty()) return;
            float level = net.minecraft.util.Mth.clamp(tank.getFluidLevel().getValue(partialTick), 0, 1);
            if (level <= 0) return;
            var fluid = tank.getTankInventory().getFluid();
            float shell = 1 / 16f + 1 / 128f;
            float xMin = fluidInset(tank, Direction.WEST, tank.getMainConnectionAxis() == Direction.Axis.X ? .25f : shell);
            float xMax = tank.extent(Direction.Axis.X)
                    - fluidInset(tank, Direction.EAST, tank.getMainConnectionAxis() == Direction.Axis.X ? .25f : shell);
            float zMin = fluidInset(tank, Direction.NORTH, tank.getMainConnectionAxis() == Direction.Axis.Z ? .25f : shell);
            float zMax = tank.extent(Direction.Axis.Z)
                    - fluidInset(tank, Direction.SOUTH, tank.getMainConnectionAxis() == Direction.Axis.Z ? .25f : shell);
            float volumeHeight = tank.getWidth() - 2 * shell;
            float filledHeight = Math.min(volumeHeight, 1 / 16f + level * (volumeHeight - 1 / 16f));
            boolean gas = fluid.getFluid().getFluidType().isLighterThanAir();
            float y = gas
                    ? tank.getWidth() - shell - filledHeight : shell;
            renderVisibleFluidBox(tank, fluid, gas,
                    xMin, y, zMin, xMax, y + filledHeight, zMax, buffers, pose, light);
        }

    }

    /** Windowed caps use native side-window clearance; opaque caps retain their original inset. */
    private static float fluidInset(ScriptedFluidTankBlockEntity tank, Direction face, float opaqueInset) {
        return tank.windowFaces().get(face) != FluidWindowMode.NONE ? 1 / 16f + 1 / 128f : opaqueInset;
    }

    /** Cull solid boundary faces before native tiling creates their vertices. Keep the free surface. */
    @SuppressWarnings("unchecked")
    private static void renderVisibleFluidBox(ScriptedFluidTankBlockEntity tank, FluidStack fluid, boolean gas,
            float xMin, float yMin, float zMin, float xMax, float yMax, float zMax,
            MultiBufferSource buffers, PoseStack pose, int light) {
        var windows = tank.windowFaces();
        var helper = (net.createmod.catnip.platform.services.ModFluidHelper<FluidStack>)
                net.createmod.catnip.platform.CatnipServices.FLUID_HELPER;
        var sprite = helper.getStillTextureOrMissing(fluid);
        int color = helper.getColor(fluid, null, null);
        int fluidLight = light & 0xF00000 | Math.max(light >> 4 & 15, helper.getLuminosity(fluid)) << 4;
        int mask = 0;
        for (Direction face : Iterate.directions)
            if (windows.get(face) != FluidWindowMode.NONE) mask |= 1 << face.get3DDataValue();
        var key = new FluidSurfaceCache.Key(sprite, color, fluidLight, mask, gas,
                xMin, yMin, zMin, xMax, yMax, zMax);
        boolean stable = tank.getFluidLevel().getValue(0) == tank.getFluidLevel().getValue(1);
        FluidSurfaceCache.render(key, stable, pose, net.createmod.catnip.render.FluidRenderHelper.getFluidBuilder(buffers),
                (localPose, vertices) -> {
            if (windows.get(Direction.NORTH) != FluidWindowMode.NONE
                    && windows.get(Direction.SOUTH) != FluidWindowMode.NONE
                    && windows.get(Direction.WEST) != FluidWindowMode.NONE
                    && windows.get(Direction.EAST) != FluidWindowMode.NONE) {
                boolean bottom = windows.get(gas ? Direction.UP : Direction.DOWN) != FluidWindowMode.NONE;
                net.createmod.catnip.platform.ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid,
                        xMin, yMin, zMin, xMax, yMax, zMax, vertices, localPose, fluidLight, bottom, true);
                return;
            }
            localPose.pushPose();
            if (gas) {
                float centerX = xMin + (xMax - xMin) / 2;
                float centerY = yMin + (yMax - yMin) / 2;
                float centerZ = zMin + (zMax - zMin) / 2;
                localPose.translate(centerX, centerY, centerZ);
                localPose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
                localPose.translate(-centerX, -centerY, -centerZ);
            }
            for (Direction local : Iterate.directions) {
                Direction world = gas && local.getAxis() != Direction.Axis.X ? local.getOpposite() : local;
                if (local != Direction.UP && windows.get(world) == FluidWindowMode.NONE) continue;
                boolean positive = local.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                switch (local.getAxis()) {
                    case X -> net.createmod.catnip.render.FluidRenderHelper.renderStillTiledFace(local,
                            zMin, yMin, zMax, yMax, positive ? xMax : xMin, vertices, localPose, fluidLight, color, sprite);
                    case Y -> net.createmod.catnip.render.FluidRenderHelper.renderStillTiledFace(local,
                            xMin, zMin, xMax, zMax, positive ? yMax : yMin, vertices, localPose, fluidLight, color, sprite);
                    case Z -> net.createmod.catnip.render.FluidRenderHelper.renderStillTiledFace(local,
                            xMin, yMin, xMax, yMax, positive ? zMax : zMin, vertices, localPose, fluidLight, color, sprite);
                }
            }
            localPose.popPose();
        });
    }

    /** Native fluid animation with bounds following each configurable glass face. */
    public static class MaterialTankRenderer extends FluidTankRenderer {
        public MaterialTankRenderer(BlockEntityRendererProvider.Context context) { super(context); }

        @Override protected void renderSafe(FluidTankBlockEntity entity, float partialTick, PoseStack pose,
                MultiBufferSource buffers, int light, int overlay) {
            if (!(entity instanceof ScriptedFluidTankBlockEntity tank)) {
                super.renderSafe(entity, partialTick, pose, buffers, light, overlay);
                return;
            }
            if (!tank.isController()) return;
            if (!tank.hasWindow()) {
                if (tank.boiler.isActive()) renderAsBoiler(tank, partialTick, pose, buffers, light, overlay);
                return;
            }
            if (tank.getFluidLevel() == null) return;
            float capHeight = 1 / 4f;
            float shell = 1 / 16f + 1 / 128f;
            float minPuddleHeight = 1 / 16f;
            float lowerInset = fluidInset(tank, Direction.DOWN, capHeight + minPuddleHeight);
            float upperInset = fluidInset(tank, Direction.UP, capHeight);
            float totalHeight = tank.getHeight() - lowerInset - upperInset;
            float level = tank.getFluidLevel().getValue(partialTick);
            if (level < 1 / (512f * totalHeight)) return;
            float filledHeight = net.minecraft.util.Mth.clamp(level * totalHeight, 0, totalHeight);
            var fluid = tank.getTankInventory().getFluid();
            if (fluid.isEmpty()) return;
            boolean gas = fluid.getFluid().getFluidType().isLighterThanAir();
            // Preserve Create's interpolation and gas placement within the visible hull.
            float yMin = totalHeight + lowerInset - filledHeight;
            float yMax = yMin + filledHeight;
            if (gas) {
                yMin += totalHeight - filledHeight;
                yMax += totalHeight - filledHeight;
            }
            pose.pushPose();
            pose.translate(0, filledHeight - totalHeight, 0);
            renderVisibleFluidBox(tank, fluid, gas,
                    shell, yMin, shell, shell + tank.getWidth() - 2 * shell, yMax,
                    shell + tank.getWidth() - 2 * shell, buffers, pose, light);
            pose.popPose();
        }

        @Override protected void renderAsBoiler(FluidTankBlockEntity entity, float partialTick, PoseStack pose,
                MultiBufferSource buffers, int light, int overlay) {
            Material material = MATERIALS.get(entity.getBlockState().getBlock());
            boolean vessel = entity.getMainConnectionAxis() != Direction.Axis.Y;
            if (material == null || !material.customGauge && !vessel) {
                super.renderAsBoiler(entity, partialTick, pose, buffers, light, overlay);
                return;
            }
            BlockState state = entity.getBlockState();
            VertexConsumer vertices = buffers.getBuffer(RenderType.cutout());
            float width = entity.getWidth();
            float progress = entity.boiler.gauge.getValue(partialTick);
            float pivotY = 6f / 16;
            float pivotZ = 8f / 16;
            pose.pushPose();
            var scripted = (net.quiwh.cbjs.content.ScriptedFluidTankBlockEntity)entity;
            pose.translate(vessel ? scripted.extent(Direction.Axis.X) / 2f : width / 2f,
                    vessel ? width / 2f : .5, vessel ? scripted.extent(Direction.Axis.Z) / 2f : width / 2f);
            for (Direction direction : Iterate.horizontalDirections) {
                if (entity.boiler.occludedDirections[direction.get2DDataValue()]) continue;
                pose.pushPose();
                float rotation = -direction.toYRot() - 90;
                float radius = (vessel ? scripted.extent(direction.getAxis()) : width) / 2f - 6 / 16f;
                CachedBuffers.partial(material.gauge, state)
                        .rotateYDegrees(rotation).uncenter().translate(radius, 0, 0)
                        .light(light).renderInto(pose, vertices);
                CachedBuffers.partial(material.dial, state)
                        .rotateYDegrees(rotation).uncenter().translate(radius, 0, 0)
                        .translate(0, pivotY, pivotZ).rotateXDegrees(-145 * progress + 90)
                        .translate(0, -pivotY, -pivotZ).light(light).renderInto(pose, vertices);
                pose.popPose();
            }
            pose.popPose();
        }
    }

    private FluidTankClientModels() { }
}
