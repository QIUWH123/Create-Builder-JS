package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.world.item.DyeColor;
import net.quiwh.cbjs.client.MechanicalBeltClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.simibubi.create.foundation.render.AllInstanceTypes;

/** Material substitution retains Create's native Flywheel instance updates and disposal. */
@Mixin(value = BeltVisual.class, remap = false)
public abstract class MechanicalBeltVisualMixin extends KineticBlockEntityVisual<BeltBlockEntity> {
    @Shadow @Final protected RotatingInstance pulley;
    @Shadow private Model getPulleyModel() { throw new AssertionError(); }
    @Unique private PartialModel cbjs$pulleyMaterial;

    protected MechanicalBeltVisualMixin(VisualizationContext context, BeltBlockEntity entity, float partialTick) {
        super(context, entity, partialTick);
    }

    @Redirect(method = "getPulleyModel", at = @At(value = "FIELD",
            target = "Lcom/simibubi/create/AllPartialModels;BELT_PULLEY:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
            remap = false, require = 1, allow = 1)
    private PartialModel cbjs$originalShaft() {
        cbjs$pulleyMaterial = MechanicalBeltClientModels.pulley(blockEntity);
        return cbjs$pulleyMaterial;
    }

    @Inject(method = "update(F)V", at = @At("HEAD"), remap = false)
    private void cbjs$updatePulleyMaterial(float partialTick, CallbackInfo callback) {
        if (pulley != null && MechanicalBeltClientModels.pulley(blockEntity) != cbjs$pulleyMaterial)
            instancerProvider().instancer(AllInstanceTypes.ROTATING, getPulleyModel()).stealInstance(pulley);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/BeltRenderer;getBeltPartial(ZZZZ)Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
            remap = false, require = 1, allow = 1)
    private PartialModel cbjs$materialPartial(boolean diagonal, boolean start, boolean end, boolean bottom) {
        return MechanicalBeltClientModels.beltPartial(blockEntity, diagonal, start, end, bottom);
    }

    @Redirect(method = {"<init>", "update(F)V"}, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/BeltRenderer;getSpriteShiftEntry(Lnet/minecraft/world/item/DyeColor;ZZ)Lnet/createmod/catnip/render/SpriteShiftEntry;"),
            remap = false, require = 2, allow = 2)
    private SpriteShiftEntry cbjs$materialShift(DyeColor color, boolean diagonal, boolean bottom) {
        return MechanicalBeltClientModels.spriteShift(blockEntity, color, diagonal, bottom);
    }
}
