package net.quiwh.cbjs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.client.MechanicalBeltClientModels;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps native transported-item rendering, pulley rotation and belt UV scrolling. */
@Mixin(value = BeltRenderer.class, remap = false)
public abstract class MechanicalBeltRendererMixin {
    @Redirect(method = "renderSafe(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;BELT_PULLEY:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
            remap = false, require = 1, allow = 1)
    private PartialModel cbjs$originalShaft(BeltBlockEntity entity, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        return MechanicalBeltClientModels.pulley(entity);
    }

    @Redirect(method = "renderSafe(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            remap = false, require = 1, allow = 1)
    private boolean cbjs$renderCustomBelts(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state);
    }

    @Redirect(method = "renderSafe(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/belt/BeltRenderer;getBeltPartial(ZZZZ)Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
            remap = false, require = 1, allow = 1)
    private PartialModel cbjs$materialPartial(boolean diagonal, boolean start, boolean end, boolean bottom,
            BeltBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        return MechanicalBeltClientModels.beltPartial(entity, diagonal, start, end, bottom);
    }

    @Redirect(method = "renderSafe(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/belt/BeltRenderer;getSpriteShiftEntry(Lnet/minecraft/world/item/DyeColor;ZZ)Lnet/createmod/catnip/render/SpriteShiftEntry;"),
            remap = false, require = 1, allow = 1)
    private SpriteShiftEntry cbjs$materialShift(DyeColor color, boolean diagonal, boolean bottom,
            BeltBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        return MechanicalBeltClientModels.spriteShift(entity, color, diagonal, bottom);
    }
}
