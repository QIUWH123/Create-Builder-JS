package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.steamEngine.*;
import com.simibubi.create.content.decoration.steamWhistle.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.SteamClientModels;
import net.quiwh.cbjs.client.ShaftClientModels;
import java.util.function.Supplier;

public final class ScriptedSteamBlockEntities {
    private static final BlockEntityBuilder<SteamEngineBlockEntity, CreateRegistrate> ENGINE_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_steam_engine", SteamEngineBlockEntity::new)
            .visual(() -> SteamEngineVisual::new, false)
            .renderer(() -> SteamEngineRenderer::new);
    public static final BlockEntityEntry<SteamEngineBlockEntity> ENGINE = ENGINE_BUILDER.register();
    private static final BlockEntityBuilder<WhistleBlockEntity, CreateRegistrate> WHISTLE_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_steam_whistle", WhistleBlockEntity::new)
            .renderer(() -> WhistleRenderer::new);
    public static final BlockEntityEntry<WhistleBlockEntity> WHISTLE = WHISTLE_BUILDER.register();
    private static final BlockEntityBuilder<PoweredShaftBlockEntity, CreateRegistrate> SHAFT_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_powered_shaft", PoweredShaftBlockEntity::new)
            .visual(() -> SteamClientModels.PoweredShaftVisual::new, false)
            .renderer(() -> ShaftClientModels.ShaftRenderer::new);
    public static final BlockEntityEntry<PoweredShaftBlockEntity> POWERED_SHAFT = SHAFT_BUILDER.register();
    public static void addEngine(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(ENGINE::get, block);
    }
    public static void addWhistle(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(WHISTLE::get, block);
    }
    public static void addShaft(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(POWERED_SHAFT::get, block);
    }
    public static void register() { }
    private ScriptedSteamBlockEntities() { }
}
