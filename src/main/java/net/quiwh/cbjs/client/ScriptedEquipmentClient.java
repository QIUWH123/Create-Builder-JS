package net.quiwh.cbjs.client;

import com.simibubi.create.compat.curios.GogglesCurioRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.content.ScriptedGogglesItem;
import java.util.function.Supplier;

/** Once-per-client optional renderer registration, reusing Create's own Curios head positioning. */
@OnlyIn(Dist.CLIENT)
public final class ScriptedEquipmentClient {
    public static void registerCuriosGoggles() {
        try {
            var registry = Class.forName("top.theillusivec4.curios.api.client.CuriosRendererRegistry");
            var register = registry.getMethod("register", Item.class, Supplier.class);
            var renderer = Class.forName("com.simibubi.create.compat.curios.GogglesCurioRenderer").getConstructor(ModelPart.class);
            for (Item item : ForgeRegistries.ITEMS) {
                if (!(item instanceof ScriptedGogglesItem)) continue;
                Supplier<Object> factory = () -> {
                    try { return renderer.newInstance(Minecraft.getInstance().getEntityModels().bakeLayer(GogglesCurioRenderer.LAYER)); }
                    catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException("Cannot create native Create goggles Curios renderer", exception);
                    }
                };
                register.invoke(null, item, factory);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot register custom goggles with the installed Curios API", exception);
        }
    }
    private ScriptedEquipmentClient() { }
}
