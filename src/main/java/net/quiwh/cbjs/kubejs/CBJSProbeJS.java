package net.quiwh.cbjs.kubejs;

import com.mojang.datafixers.util.Pair;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import moe.wolfgirl.probejs.plugin.ProbeJSPlugin;
import moe.wolfgirl.probejs.plugin.builtins.events.Events;
import moe.wolfgirl.probejs.typescript.ClassPath;
import moe.wolfgirl.probejs.typescript.Documents;
import moe.wolfgirl.probejs.typescript.base.AliasRegistrar;
import moe.wolfgirl.probejs.typescript.base.DocumentRegistrar;
import moe.wolfgirl.probejs.typescript.document.ClassDecl;
import moe.wolfgirl.probejs.typescript.document.Types;
import moe.wolfgirl.probejs.typescript.document.base.CommentableCode;
import moe.wolfgirl.probejs.typescript.document.base.KindAware.Kind;
import moe.wolfgirl.probejs.typescript.document.base.Type;
import moe.wolfgirl.probejs.typescript.document.builders.ClassBuilder;
import moe.wolfgirl.probejs.typescript.document.builders.MethodBuilder;
import moe.wolfgirl.probejs.typescript.document.members.FieldDecl;
import moe.wolfgirl.probejs.typescript.document.members.MethodDecl;
import moe.wolfgirl.probejs.typescript.document.members.ParamDecl;
import moe.wolfgirl.probejs.typescript.document.types.ArrayType;
import moe.wolfgirl.probejs.typescript.document.types.ClassType;
import moe.wolfgirl.probejs.typescript.document.types.ParamType;

/** Optional ProbeJS 8 integration, loaded only by the conditional KubeJS plugin entry. */
public final class CBJSProbeJS extends ProbeJSPlugin {
    private static final String PACKAGE = "net.quiwh.cbjs.kubejs.";
    private static final String[] FACES = {"down", "up", "north", "south", "west", "east"};
    private static final String[] WINDOW_FACES = {"side", "end", "all"};
    private static final String[] WINDOW_STYLES = {"full", "none", "strip"};
    private static final Map<String, List<String>> BUILDERS = builders();
    private static final Map<String, String> ITEM_BUILDERS = Map.of("GogglesBuilder", "goggles", "WrenchBuilder", "wrench");
    private static final Set<String> SELECTOR_EVENTS = Set.of("fluidPipeFlow", "storageFluidChanged",
            "storageInventoryChanged", "beltItemEntered", "beltItemExited", "beltItemProcessed");

    @Override public void initialize() {
        // These events accept two selectors, beyond the stock compiler's single target.
        for (String event : SELECTOR_EVENTS) Events.SKIP_EVENTS.add(Pair.of("CBJSEvents", event));
    }

    @Override public void transformClass(Documents.ClassDocument entry) {
        Class<?> type = entry.classInfo().clazz();
        String name = type.getSimpleName();
        if (type.getName().startsWith(PACKAGE) || type == dev.latvian.mods.kubejs.block.BlockBuilder.class
                || type == dev.latvian.mods.kubejs.block.BlockModificationKubeEvent.BlockModifications.class
                || type.getName().equals("net.quiwh.cbjs.api.event.KineticDataModifyEvent$KineticDataModifier")
                || type.getName().equals("net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent$BurnerFuelModifier"))
            transformDocument(name, entry.document());
    }

    @Override public void addSidedDocuments(DocumentRegistrar registrar) {
        ClassBuilder namespace = new ClassBuilder("CBJSEvents").kind(Kind.NAMESPACE).noExport();
        selectorOverloads(namespace, "fluidPipeFlow", FluidPipeFlowEventJS.class, "selector", "fluid", "block",
                "Active fluid connections. Single selectors retain each entry's fluid/block role; paired positions match fluid then block.");
        selectorOverloads(namespace, "storageFluidChanged", StorageFluidChangedEventJS.class, "container", "container", "fluid",
                "Controller fluid changes, matching previous or current nonempty contents.");
        selectorOverloads(namespace, "storageInventoryChanged", StorageInventoryChangedEventJS.class, "container", "container", "item",
                "Controller inventory changes, matching previous or current changed stacks. One callback retains the full slot batch.");
        selectorOverloads(namespace, "beltItemEntered", BeltItemTransferEventJS.class, "belt", "belt", "item",
                "Completed native belt ingress, reported at server tick end. Simulation is silent.");
        selectorOverloads(namespace, "beltItemExited", BeltItemTransferEventJS.class, "belt", "belt", "item",
                "Completed native belt egress, reported at server tick end. Simulation is silent.");
        selectorOverloads(namespace, "beltItemProcessed", BeltItemProcessedEventJS.class, "belt", "belt", "item",
                "Committed native belt processing. Item selectors match input or outputs; notification cannot cancel it.");
        registrar.addGlobal((ClassPath) ClassPath.sided(ScriptType.SERVER, "events").append("CBJSSelectorEvents"), namespace.build());
    }

    private static void selectorOverloads(ClassBuilder namespace, String name, Class<?> payload,
            String single, String first, String second, String description) {
        Type selector = Types.union(Types.STRING, new ArrayType(Types.STRING), Types.NULL);
        Type callback = Types.lambda(builder -> builder.param("event", Types.clazz(payload)).returns(Types.VOID));
        for (int count = 0; count <= 2; count++) {
            MethodBuilder builder = new MethodBuilder(name).returnType(Types.VOID);
            if (count == 1) builder.param(single, selector);
            if (count == 2) builder.param(first, selector).param(second, selector);
            MethodDecl method = builder.param("handler", callback).build(Kind.NAMESPACE);
            method.addComments(description,
                    "Selectors accept an ID, #tag, mixed flat array, null or '*'. Arrays match OR; paired positions match AND. Empty arrays match nothing. Overlap produces one callback.");
            namespace.member(method);
        }
    }

    @Override public void addTypeAlias(AliasRegistrar registrar) {
        Api.componentAliases(registrar);
    }

    /** Public for an offline verification using real ProbeJS DocumentClass and formatters. */
    public static void transformDocument(String name, Object document) {
        try {
            Api api = new Api((ClassDecl) document);
            if (BUILDERS.containsKey(name) || ITEM_BUILDERS.containsKey(name) || name.equals("BlockBuilder") || name.equals("CBJSBlockBuilder") || isSharedDeviceBuilder(name))
                api.preserveFluentReturns();
            if (BUILDERS.containsKey(name)) builder(name, api);
            if (ITEM_BUILDERS.containsKey(name)) equipment(name, api);
            if (name.equals("BlockBuilder") || isSharedDeviceBuilder(name)) api.hideImplementationMembers(name);
            if (name.equals("BlockBuilder") || name.equals("ContraptionBlockBuilder")) {
                contraptions(api);
                api.signature("replaceTexture", "this", "Replace one model texture slot; preserve the current geometry.",
                        new String[]{"role", "path"}, new String[]{"string", "string"});
            }
            if (name.equals("BlockModifications") || name.equals("ContraptionBlockModification")) {
                contraptions(api);
                api.comment(document, "BlockEvents.modification in startup_scripts: event.modify(id, block => block.noContraptionAssembly()).",
                        "Adds restrictions to this registered block. Existing builder restrictions and Create's native rules still apply.");
            }
            if (name.endsWith("EventJS")) event(name, api);
            if (name.equals("GogglesKineticInfo") || name.equals("GogglesStorageInfo")) gogglesData(name, api);
            
            if (name.equals("KineticDataModifier")) {
                api.preserveFluentReturns();
                api.fluent("stressImpact", "Nonnegative finite base stress demand in SU per RPM. Native machine factors still apply. Omission restores the native live default on the next successful reload.");
                api.fluent("stressCapacity", "Nonnegative finite base generated capacity in SU per RPM. Native generator speed and efficiency factors still apply. Steam-engine overrides target the engine block ID.");
            }
            if (name.equals("BurnerFuelModifier")) {
                burnerFuels(api);
                api.comment(document, "One exact burner block profile. Chain settings from a single modifyBurner(blockId) call per event layer.");
            }
            if (name.equals("FluidDeviceBuilder")) api.comment(document,
                    "Shared native Create device settings. Native devices own their block entity; blockEntity(...) is unsupported.");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot enrich ProbeJS document " + name, exception);
        }
    }

    private static Map<String, List<String>> builders() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("CasingBuilder", List.of("casing"));
        result.put("ShaftBuilder", List.of("shaft"));
        result.put("ChainBuilder", List.of("chain"));
        result.put("ChainConveyorBuilder", List.of("chain_conveyor"));
        result.put("ChainDriveBuilder", List.of("encased_chain_drive"));
        result.put("TransmissionBuilder", List.of("clutch", "gearshift", "rotation_speed_controller"));
        result.put("MechanicalBeltBuilder", List.of("mechanical_belt"));
        result.put("GearboxBuilder", List.of("gearbox", "vertical_gearbox", "six_way_gearbox"));
        result.put("FluidPipeBuilder", List.of("fluid_pipe"));
        result.put("FluidTankBuilder", List.of("fluid_tank", "fluid_vessel"));
        result.put("ItemVaultBuilder", List.of("item_vault", "item_silo"));
        result.put("PumpBuilder", List.of("mechanical_pump"));
        result.put("SmartFluidPipeBuilder", List.of("smart_pipe"));
        result.put("FluidValveBuilder", List.of("fluid_valve"));
        result.put("BlazeBurnerBuilder", List.of("blaze_burner", "burner"));
        result.put("SteamEngineBuilder", List.of("steam_engine"));
        result.put("SteamWhistleBuilder", List.of("steam_whistle"));
        result.put("MechanicalArmBuilder", List.of("mechanical_arm"));
        result.put("ChuteBuilder", List.of("chute", "smart_chute"));
        result.put("FunnelBuilder", List.of("andesite_funnel", "brass_funnel"));
        result.put("TunnelBuilder", List.of("andesite_tunnel", "brass_tunnel"));
        result.put("WaterWheelBuilder", List.of("water_wheel", "large_water_wheel"));
        result.put("WindmillBearingBuilder", List.of("windmill_bearing"));
        result.put("HandCrankBuilder", List.of("hand_crank"));
        result.put("CreativeMotorBuilder", List.of("creative_motor"));
        result.put("BasinBuilder", List.of("basin"));
        result.put("FlywheelBuilder", List.of("flywheel"));
        result.put("GaugeBuilder", List.of("speedometer", "stressometer"));
        result.put("CogwheelBuilder", List.of("cogwheel", "large_cogwheel", "shaftless_cogwheel", "large_shaftless_cogwheel",
                "flanged_cogwheel", "large_flanged_cogwheel"));
        return result;
    }

    private static void builder(String name, Api api) throws ReflectiveOperationException {
        api.hideImplementationMembers(name);
        api.comment(api.document, "StartupEvents.registry('block'): event.create(id, "
                + BUILDERS.get(name).stream().map(value -> "'" + value + "'").collect(Collectors.joining(" | ")) + ").",
                "IDs and asset paths use namespace:path. Texture paths omit textures/ and .png; model paths omit models/ and .json.");
        contraptions(api);
        api.fluent("maxRpm", name.equals("ChainBuilder")
                ? "Nonnegative RPM rating for mechanical connections using this chain material. Omitted: no material RPM cap; endpoint blocks retain their own limits. Zero permits only stopped connections."
                : "Nonnegative RPM cap for this registered family; zero stops rotation. Omitted: inherit Create's configured limit. Setting an explicit cap selects an independent limit.");
        api.fluent("noMaxRPM", "Remove this family's RPM cap and opt out of Create's default cap. maxStress remains independent.");
        api.fluent("maxStress", "Nonnegative network stress cap in SU (stress units). Omitted: no CBJS stress cap. Zero permits only an unloaded network.");
        api.fluent("noEncasedVariants", "Disable generated encased companions for this registration. Default: companions enabled.");
        api.fluent("noDrops", "Disable the normal block drop. A later explicit lootTable(...) can provide custom drops.");
        if (!name.equals("MechanicalBeltBuilder")) api.fluent("textureAll", name.equals("CasingBuilder")
                ? "Set the casing shell texture; connected/opening roles remain independently configurable with replaceTexture(role, path)."
                : "Use one texture resource for this family's semantic texture roles. Omitted: native artwork.");
        api.fluent("noItem", "Remove only this registration's item. Automatically generated encased companion items remain available. Where supported, noEncasedVariants() disables those blocks too.");
        if (isNativeMachineBuilder(name)) api.fluent("noItem", "Remove this registration's item. Generated internal placement variants do not gain separate items.");
        String[] textures = textureRoles(name);
        if (textures.length > 0) api.signature("replaceTexture", "this",
                "Choose a supported semantic texture role. Arbitrary KubeJS model slots are accepted, but affect only models that reference that slot; unknown names do not change generated companion models.",
                new String[]{"role", "path"}, new String[]{literals(textures) + " | (string & {})", "string"});
        String[] models = modelRoles(name);
        if (models.length > 0) {
            api.narrow("model", 2, 0, "role", literals(models));
            if (!name.equals("CogwheelBuilder"))
                api.fluent("model", "Select a supported block/partial/item model resource. A one-argument model(path) selects the main model; model(role, path) replaces one supported part. Paths omit models/ and .json.");
        }
        switch (name) {
            case "CasingBuilder" -> {
                api.signature("texture", "this",
                        "Alias of replaceTexture(role, path). Select block, connected, encased_cogwheel_side, encased_cogwheel_side_connected, gearbox or encased_pipe. Other KubeJS slots only affect models referencing that exact slot.",
                        new String[]{"role", "path"}, new String[]{literals(textures) + " | (string & {})", "string"});
                api.fluent("cogwheelCasing", "Enable Create cogwheel encasing for this casing. Default: false; no argument enables it.");
                api.fluent("shaftCasing", "Enable Create shaft encasing for this casing. Default: false; no argument enables it.");
                api.fluent("fluidCasing", "Enable Create fluid-pipe encasing for this casing. Default: false; no argument enables it.");
            }
            case "CogwheelBuilder" -> {
                api.cogwheelModels();
                api.fluent("chain", "Opt into Bits 'n' Bobs chain drives, including CBJS chain materials. Default false; no arguments. Independent of rope() and belt(). Without BnB the cogwheel still works, but drive connections are unavailable.");
                api.fluent("rope", "Opt this cogwheel into Bits 'n' Bobs rope drives. Available on ordinary, shaftless and flanged types. Default false; no arguments. Does not enable chain or belt drives.");
                api.fluent("belt", "Opt this cogwheel into Bits 'n' Bobs belt drives. Available on ordinary, shaftless and flanged types. Default false; no arguments. Does not enable chain or rope drives; distinct from mechanical_belt item transport.");
            }
            case "ChainDriveBuilder" -> api.fluent("model", "A primary model supplies every stationary connection shape. Named single/end_horizontal/end_vertical/middle_horizontal/middle_vertical override one shape; shaft replaces the moving shaft, and item selects an independent item model. Omitted parts use native Create geometry.");
            case "TransmissionBuilder" -> {
                api.fluent("model", "Native clutch/gearshift/controller geometry by default. model(path) replaces the stationary body. Split transmissions support powered and shaft_half (shaft alias); the controller supports shaft and bracket. Item models may be selected separately.");
                api.comment(api.document, "Redstone, wrench interaction and speed-controller target behavior use Create's native mechanics. maxRpm/noMaxRPM change material failure limits; the controller target UI retains Create's config range.");
            }
            case "BlazeBurnerBuilder" -> {
                burnerFuels(api);
                api.narrow("maxHeatLevel", 1, 0, "level", literals("smouldering", "fading", "kindled", "seething"));
                api.fluent("maxHeatLevel", "Cap actual burner heat; default seething. none is not a valid fueled burner heat.");
                for (String method : List.of("haveBlaze", "haveRods", "haveFlame", "haveGoggles", "haveTrainHat", "haveLogisticsHat", "haveParticles", "haveSound"))
                    api.fluent(method, "Select this visual/audio feature. No argument enables it; boolean explicitly enables or disables it. blaze_burner enables all by default; burner starts without blaze and rods.");
                for (String method : List.of("canControlTrain", "canBeStockKeeper", "canCaptureBlaze", "canHeatBoiler", "canHeatBasin", "canProcessFan", "canUseCreativeFuel", "canBeFedByArm"))
                    api.fluent(method, "Select this independent native capability. No argument enables it; boolean explicitly enables or disables it. Generic burner disables train control, stock keeping and blaze capture by default.");
                api.comment(api.document,
                        "Native burner family: blaze_burner uses blaze visuals/capabilities; burner uses generic defaults. Generates matching empty_<name> item and lit_<name> block in the same namespace and folder.",
                        "Builder fuel defaults are overridden by startup then server burnerFuel events. Existing blazeBurnerFuel server handlers remain supported.");
            }
            case "SteamEngineBuilder" -> api.comment(api.document,
                    "Native boiler attachment and piston animation. Supports native/custom shafts and enabled custom tanks/vessels. Powered custom shafts retain their source material. Tune generation with kineticData.stressCapacity on this engine ID.");
            case "SteamWhistleBuilder" -> api.comment(api.document,
                    "Native pitch, redstone and sound. Generates an itemless <name>_extension companion matching this whistle's material.");
            case "ChainConveyorBuilder" -> {
                api.fluent("maxConnections", "Nonnegative maximum attached chains for this conveyor. Zero forbids new connections. Omitted: follow Create's live server setting. Both endpoints must have space; existing chains remain intact.");
                api.fluent("noMaxConnections", "Remove this conveyor's connection-count cap. Other endpoint, chain length and package limits remain independent.");
                api.comment(api.document,
                        "Native package transport, routing and ports with mixed native/custom endpoints and registered chain materials. Connection count defaults to Create's live config unless overridden; length and package limits retain native settings.");
            }
            case "MechanicalBeltBuilder" -> {
                api.fluent("maxLength", "Maximum total belt blocks, including both endpoint pulleys. Integer of at least 2. Omitted: inherit the live Create maxBeltLength setting. An explicit value overrides it.");
                api.fluent("noMaxLength", "Remove the belt length cap independently of Create's config. World and integer representation bounds still apply. A later maxLength sets a fixed cap.");
                api.fluent("itemSpacing", "Minimum distance between transported stack centers in belt segments. Finite positive number representable as a positive float. Omitted: Create's normal spacing, 1.0; smaller values make items denser.");
            }
            case "FluidTankBuilder" -> {
                tank(api);
                api.unsupportedStorageModels();
            }
            case "ItemVaultBuilder" -> {
                api.unsupportedStorageModels();
                api.fluent("capacity", "Positive inventory slots per block. Omitted: inherit the live Create vaultCapacity server setting (normally 20). This is a slot count, not an item count. Existing contents are preserved if capacity shrinks.");
                api.fluent("maxSize", "Positive square cross-section width in blocks. Omitted: Create's native width cap of 3. An explicit value overrides it.");
                api.fluent("maxLength", "Positive multiblock length in blocks (vertical height for item_silo). Omitted: Create's native cap of 3 times the formed width. An explicit value overrides it.");
                api.fluent("maxHeight", "Alias for maxLength; positive vertical height in blocks for item_silo. Omitted: Create's native cap of 3 times the formed width.");
                api.fluent("noMaxSize", "Remove the cross-section width cap. Loaded structure and integer slot capacity bounds still apply. A later maxSize sets a fixed cap.");
                api.fluent("noMaxLength", "Remove the longitudinal cap. Alias of noMaxHeight; integer slot capacity bounds still apply. A later maxLength or maxHeight sets a fixed cap.");
                api.fluent("noMaxHeight", "Remove the longitudinal cap. Alias of noMaxLength; integer slot capacity bounds still apply. A later maxHeight or maxLength sets a fixed cap.");
                selector(api, "itemFilter", "item", "insertion", "Saved contents stay intact and can still be extracted.");
            }
            default -> { }
        }
    }

    private static boolean isSharedDeviceBuilder(String name) {
        return Set.of("FluidDeviceBuilder", "NativeMachineBuilder", "NativeKineticMachineBuilder").contains(name);
    }
    private static boolean isNativeMachineBuilder(String name) {
        return Set.of("MechanicalArmBuilder", "ChuteBuilder", "FunnelBuilder", "TunnelBuilder", "WaterWheelBuilder",
                "WindmillBearingBuilder", "HandCrankBuilder", "CreativeMotorBuilder", "BasinBuilder", "FlywheelBuilder", "GaugeBuilder").contains(name);
    }

    private static void equipment(String name, Api api) throws ReflectiveOperationException {
        api.hideImplementationMembers(name);
        api.comment(api.document, "StartupEvents.registry('item'): event.create(id, '" + ITEM_BUILDERS.get(name) + "').",
                "Uses Create's native " + ITEM_BUILDERS.get(name) + " behavior and supports ordinary KubeJS item properties.");
        String[] roles = name.equals("GogglesBuilder") ? new String[]{"frame", "lens", "goggles", "layer0"}
                : new String[]{"wrench"};
        api.signature("replaceTexture", "this", "Replace a texture slot on the default Create geometry. Explicit parentModel or modelJson owns the complete model.",
                new String[]{"role", "path"}, new String[]{literals(roles) + " | (string & {})", "string"});
    }

    private static void tank(Api api) throws ReflectiveOperationException {
        api.fluent("capacity", "Positive fluid capacity per block in mB (1000 mB = one bucket). Omitted: inherit the live Create fluidTankCapacity setting times 1000 (normally 8000 mB). Existing contents are preserved if capacity shrinks.");
        api.fluent("maxSize", "Positive square cross-section width in blocks. Omitted: Create's native width cap of 3. An explicit value overrides it.");
        api.fluent("maxHeight", "Positive height in blocks for fluid_tank; axis length for fluid_vessel. Omitted: inherit the live Create fluidTankMaxHeight setting. An explicit value overrides it.");
        api.fluent("maxLength", "Fluid vessels only: positive horizontal axis length in blocks. Use maxHeight for fluid tanks. Omitted: inherit the live Create fluidTankMaxHeight setting.");
        api.fluent("noMaxSize", "Remove the cross-section width cap. Loaded structure and integer fluid capacity bounds still apply. A later maxSize sets a fixed cap.");
        api.fluent("noMaxHeight", "Remove the height/axis length cap. Integer fluid capacity bounds still apply. A later maxHeight sets a fixed cap.");
        api.fluent("noMaxLength", "Fluid vessels only: alias for noMaxHeight. Remove the horizontal axis length cap. A later maxLength or maxHeight sets a fixed cap.");
        api.fluent("noWindow", "Disable windows and wrench window cycling. Default: windows allowed.");
        api.fluent("noDefaultWindow", "Place with all faces opaque while keeping wrench window changes and the configured enabled layout. Default: strip on the four sides, none on the two ends.");
        api.fluent("independentWindows", "Enable per-face wrench cycling through windowStyles. Takes no arguments. Omitted: shared glass/no-glass toggle of the configured windowStyle layout, using native tank/vessel window designs by default.");
        api.fluent("noSteamEngine", "Disable steam-engine attachment and engine tank access. Whistles remain independent. Default: engines enabled.");
        api.fluent("noWhistle", "Disable steam-whistle attachment and whistle tank access. Steam engines remain independent. Default: whistles enabled.");
        api.narrow("windowStyles", 1, 0, "styles", "(" + literals(WINDOW_STYLES) + ")[]");
        api.fluent("windowStyles", "Nonempty, duplicate-free allowed styles. With independentWindows enabled, wrench cycles each clicked exterior face through this list; shared mode toggles the configured layout on/off. Default: ['strip', 'full', 'none'].");
        api.narrow("windowStyle", 1, 0, "style", literals(WINDOW_STYLES));
        api.narrow("windowStyle", 2, 0, "face", literals(WINDOW_FACES));
        api.narrow("windowStyle", 2, 1, "style", literals(WINDOW_STYLES));
        api.fluent("windowStyle", "Set the enabled layout on side (four longitudinal faces), end (two axis caps) or all six faces, in either shared or independent mode. One argument sets all. Groups follow the tank/vessel orientation. noDefaultWindow starts opaque without changing this layout. Default: side strip, end none.");
        selector(api, "fluidFilter", "fluid", "filling", "Saved contents stay intact and can still be drained.");
    }

    private static void selector(Api api, String method, String kind, String action, String retained)
            throws ReflectiveOperationException {
        api.fluent(method, "Whitelist " + action + " by one " + kind + " ID or #" + kind + " tag, or a nonempty array. Entries match with OR. Omitted: accept every "
                + kind + ". " + retained + (kind.equals("fluid")
                ? " Example: 'minecraft:water' or '#forge:water'."
                : " Example: 'minecraft:iron_ingot' or '#forge:ingots/iron'."));
        // Java List<String> is supported by Rhino's array conversion; expose the natural JS array.
        api.narrowList(method, "selectors", "string[]");
    }

    private static void contraptions(Api api) throws ReflectiveOperationException {
        api.signature("noContraptionAssembly", "this", "Prevent direct assembly and slime/glue dragging. Default: allow Create's normal assembly rules.", new String[0], new String[0]);
        api.signature("noContraptionPush", "this", "Prevent assembly/movement by translating contraptions such as pistons and gantries. Bearings retain their normal rules. Default: Create's normal movement rules.", new String[0], new String[0]);
        api.signature("noContraptionSquash", "this", "Protect this existing world block from replacement when a contraption disassembles over it. Default: Create's normal disassembly rules.", new String[0], new String[0]);
        api.signature("noContraptionPickup", "this", "Prevent picking up a minecart contraption containing this block. Default: Create's normal minecart pickup rules.", new String[0], new String[0]);
    }

    private static void burnerFuels(Api api) {
        api.fluent("addFuel", "Declare normal fuel by namespaced item ID or #item tag and positive integral burn ticks. Exact IDs beat tags within the same profile; later layers override earlier layers.");
        api.fluent("addSuperFuel", "Declare superheated fuel by namespaced item ID or #item tag and positive integral burn ticks. Native insertion/consumption and heat caps still apply.");
        api.fluent("useVanillaFuels", "Enable or disable native Create/vanilla fallback fuels for this burner profile. Default: enabled.");
        api.fluent("useGlobalFuels", "Enable or disable shared CBJS fuel rules for this burner profile. Default: enabled. Local profile rules are still active.");
    }

    private static void event(String name, Api api) throws ReflectiveOperationException {
        api.comment(api.document, name.equals("GogglesTooltipEventJS")
                ? "CBJSEvents.gogglesTooltip([blockId,] callback), in client_scripts. Runs once per inspected game tick; appends to Create's normal goggles readout. clear() replaces its current lines."
                : name.equals("FluidPipeFlowEventJS")
                ? "CBJSEvents.fluidPipeFlow callback in server_scripts. Each selector accepts an ID/#tag or a flat mixed array; OR within arrays, AND between paired fluid/block selectors."
                : name.equals("StorageFluidChangedEventJS") || name.equals("StorageInventoryChangedEventJS")
                ? "CBJSEvents storage content callback in server_scripts. Optional selectors accept an ID/#tag or a flat mixed array; OR within arrays, AND between paired container/content selectors."
                : name.startsWith("BeltItem")
                ? "CBJSEvents belt callback in server_scripts. Optional selectors are belt first, item second; IDs, #tags, mixed arrays, null and '*' are supported. Actions are reported after completion at server tick end."
                : name.equals("KineticDataModifyEventJS") ? "CBJSEvents.kineticData(callback), in server_scripts; modify defaults through modifyKineticData(blockId)."
                : "CBJSEvents callback in server_scripts. EventHandler ID filters use the exact namespace:block_id; omit the filter to listen to every matching block.");
        for (Object method : api.methods()) {
            String member = api.name(method);
            if (member.toLowerCase().contains("rpm")) api.comment(method, "Rotation speed in RPM; signed speeds retain rotation direction.");
            if (member.toLowerCase().contains("stress") || member.toLowerCase().contains("capacity"))
                api.comment(method, "Kinetic stress and network capacity use SU (stress units).");
            if (name.contains("Overload") && (member.equals("getMaxRpm") || member.equals("getMaxStress"))) {
                api.returns(method, "number | null");
                api.comment(method, member.equals("getMaxRpm")
                        ? "Effective RPM limit for this check, including Create's configured cap when inherited; null means unlimited RPM."
                        : "Network demand limit in SU for this check; null means no CBJS stress cap.");
            }
            if (member.equals("getSide")) api.returns(method, literals(FACES));
            if (member.equals("getAxis") || member.equals("getOldAxis") || member.equals("getNewAxis")) api.returns(method, literals("x", "y", "z"));
            if (member.equals("getReason") && name.contains("Overload")) api.returns(method, literals("rpm", "stress", "both"));
        }
        if (name.equals("BlazeBurnerFuelEventJS") || name.equals("BurnerFuelEventJS")) {
            api.comment(api.document, "CBJSEvents.burnerFuel(callback) supports startup and server scripts; blazeBurnerFuel remains available in server_scripts. Rebuilt atomically on /reload, layering builder defaults, startup rules and server rules.");
            burnerFuels(api);
            api.methodComment("modifyBurner", "Configure one exact registered burner block ID; chain per-burner setters from one call per event layer. Native Create burners are supported too.");
        }
        if (name.contains("Overload")) {
            api.comment(api.document, "Posted before a scripted RPM/stress limit destroys the block. event.cancel() prevents that destruction for this check; it does not remove the limit.");
            api.methodComment("cancel", "Prevent destruction from this overload check.");
        }
        if (name.equals("KineticStressChangedEventJS")) api.comment(api.document,
                "Network stress/capacity transition. oldStress/stress and oldCapacity/capacity use SU; previouslyOverstressed/overstressed describe the previous/current network state.");
        if (name.equals("KineticEventJS")) api.comment(api.document,
                "Shared kinetic block context. isLarge and isEncased describe cogwheel variants; other kinetic families return false.");
        if (name.equals("FluidPipeCollisionEventJS")) api.comment(api.document, "event.cancel() prevents Create's default fluid-collision interaction.");
        if (name.equals("FluidPipeFlowEventJS")) api.comment(api.document,
                "Flow event fires for active fluid connections. Overloads: fluidPipeFlow(callback), fluidPipeFlow(selector, callback), fluidPipeFlow(fluid, block, callback). Each selector accepts an ID/#tag or a flat array mixing IDs/#tags; OR within arrays, AND between paired positions. Empty arrays match nothing; top-level '*' and null are wildcards in both single and paired forms. In the single form each entry retains its scalar role: known fluid IDs match fluids first, block IDs match pipes, and #tags match fluid OR block. Overlapping entries cause one callback.");
        if (name.equals("StorageFluidChangedEventJS")) api.comment(api.document, "Controller fluid snapshot changed. Optional selectors are container first, fluid second; one selector/list filters containers. Each selector accepts an ID/#tag or a flat array mixing IDs/#tags; OR within arrays, AND between positions. Empty arrays match nothing; top-level '*' and null are wildcards. Match old OR current nonempty fluid, including removal; overlapping entries cause one callback. Amounts and capacities are in mB (1000 mB = one bucket). Returned fluid snapshots are safe to inspect.");
        if (name.equals("StorageInventoryChangedEventJS")) api.comment(api.document, "Controller inventory snapshot changed. Optional selectors are container first, item second; one selector/list filters containers. Each selector accepts an ID/#tag or a flat array mixing IDs/#tags; OR within arrays, AND between positions. Empty arrays match nothing; top-level '*' and null are wildcards. Match old OR current nonempty changed stacks, including removal. One callback per batch retains all slot changes even when entries overlap. Slot counts are inventory slots; item counts count individual items.");
        if (name.equals("BeltItemEventJS")) api.comment(api.document,
                "Completed native belt action queued to server tick end. pos identifies the affected segment; controllerPos identifies its belt controller. item is a detached snapshot; count is the snapshot stack count. Notifications cannot cancel completed actions.");
        if (name.equals("BeltItemTransferEventJS")) {
            api.comment(api.document, "Successful ingress or egress only; simulation is silent. sourcePos/destinationPos/direction may be null when native code does not expose them.");
            for (Object method : api.methods()) {
                if (api.matches(method, "getReason"))
                    api.returns(method, literals("insertion", "extraction", "ejection", "funnel", "crusher", "tunnel"));
                if (api.matches(method, "getDirection")) api.returns(method, literals(FACES) + " | null");
                if (api.matches(method, "getSourcePos") || api.matches(method, "getDestinationPos"))
                    api.nullableReturn(method, "net.minecraft.core.BlockPos");
            }
        }
        if (name.equals("BeltItemProcessedEventJS")) {
            for (Object method : api.methods()) {
                if (api.matches(method, "getProcessorId")) api.returns(method, "string | null");
                if (api.matches(method, "getProcessorPos")) api.nullableReturn(method, "net.minecraft.core.BlockPos");
            }
            api.comment(api.document,
                    "One committed press/deployer/spout/fan result. item is the entire pre-process input, outputs are detached results, and heldItem is the unprocessed remainder. Item filters match input or outputs; count does not claim how many were consumed. processorPos/processorId may be null.");
        }
        if (name.equals("StorageStructureEventJS") || name.equals("StorageFormedEventJS")
                || name.equals("StorageResizedEventJS") || name.equals("StorageSplitEventJS"))
            api.comment(api.document, "storageFormed/storageResized/storageSplit describe controller structure transitions. Dimensions are in blocks. Zero new dimensions mean the controller was removed.");
        if (name.equals("GogglesTooltipEventJS")) {
            api.componentInput("add", false);
            api.componentInput("addAll", true);
            for (String member : List.of("setLine", "insertLine", "addItem", "addFluid"))
                api.componentArgument(member, 1);
            api.componentArgument("addProgress", 2);
            for (String member : List.of("position", "offset", "scale", "lineSpacing", "background", "border", "noBackground"))
                api.fluent(member, "Configure this inspected readout. Pixel positions use GUI coordinates; colors accept signed or unsigned 32-bit ARGB. Native goggles visibility and fade remain active.");
            for (Object method : api.methods()) {
                if (api.matches(method, "getKinetic")) api.nullableReturn(method, PACKAGE + "GogglesKineticInfo");
                if (api.matches(method, "getStorage")) api.nullableReturn(method, PACKAGE + "GogglesStorageInfo");
                if (api.matches(method, "getLine")) api.nullableReturn(method, "net.minecraft.network.chat.Component");
            }
            api.methodComment("add", "Append one line through KubeJS Component conversion: strings, numbers, booleans, Components, text/translate objects and arrays of text parts are accepted. Null is rejected.");
            api.methodComment("addAll", "Append an array/list of text components as separate goggles tooltip lines.");
            api.methodComment("clear", "Clear the current goggles tooltip lines before adding a replacement readout.");
            api.methodComment("getLines", "Read-only view of the current tooltip lines.");
            api.methodComment("getFace", "Inspected Minecraft Direction (down, up, north, south, west, or east).");
            api.methodComment("getLine", "Return one line, or null for an invalid index. Indexes start at zero.");
            for (String member : List.of("setLine", "insertLine", "removeLine"))
                api.methodComment(member, "Edit lines using a zero-based index; return false for an invalid index. Setting a rich row replaces it with ordinary text.");
            api.methodComment("addItem", "Append an item icon and label; omit the label for the item name. The stack is copied.");
            api.methodComment("addFluid", "Append a fluid icon and label; omit the label for the fluid name. The stack is copied.");
            api.methodComment("addProgress", "Append a progress row. Finite value is clamped to [0, maximum]; maximum must be positive. Default width is 120 pixels; explicit width is 1..4096. Color is ARGB.");
            api.methodComment("getKinetic", "Client-synchronized kinetic readout, or null for a non-kinetic source. Does not scan or create a network.");
            api.methodComment("getStorage", "Client-visible tank/vessel/vault/silo dimensions and capacity, or null. Fluid contents are copied; unsynchronized vault items remain unavailable.");
        }
    }

    private static void gogglesData(String name, Api api) throws ReflectiveOperationException {
        api.comment(api.document, "Client-visible snapshot for the inspected information source. No additional server synchronization or network/inventory scanning.");
        for (Object method : api.methods()) {
            if (name.equals("GogglesKineticInfo") && (api.matches(method, "getStress") || api.matches(method, "getCapacity")))
                api.returns(method, "number | null");
            if (name.equals("GogglesStorageInfo")) {
                if (api.matches(method, "getAmount")) api.returns(method, "number | null");
                if (api.matches(method, "getFluid")) api.nullableReturn(method, "net.neoforged.neoforge.fluids.FluidStack");
                if (api.matches(method, "getAxis")) api.returns(method, literals("x", "y", "z"));
                if (api.matches(method, "getType")) api.returns(method, literals("fluid_tank", "fluid_vessel", "item_vault", "item_silo"));
            }
        }
        if (name.equals("GogglesKineticInfo")) api.comment(api.document,
                "RPM is signed; stress and capacity are SU. Unavailable synchronized stress/capacity returns null.");
        else api.comment(api.document,
                "Dimensions use blocks. capacity is fluid mB for tanks/vessels or item slots for vaults/silos. amount/fluid are null for item storage; inventoryAvailable is false because Create does not sync vault contents.");
    }

    private static String[] textureRoles(String name) {
        return switch (name) {
            case "MechanicalArmBuilder" -> new String[]{"all", "body", "item", "cog", "base", "lower_body", "upper_body", "claw", "claw_goggles", "grip_upper", "grip_lower"};
            case "ChuteBuilder", "BasinBuilder" -> new String[]{"all", "body", "item"};
            case "FunnelBuilder" -> new String[]{"all", "body", "item", "flap", "belt_flap"};
            case "TunnelBuilder" -> new String[]{"all", "body", "item", "flap"};
            case "WaterWheelBuilder" -> new String[]{"all", "body", "item", "wheel", "wheel_extension"};
            case "WindmillBearingBuilder" -> new String[]{"all", "body", "item", "top", "shaft"};
            case "HandCrankBuilder" -> new String[]{"all", "body", "item", "base", "handle"};
            case "CreativeMotorBuilder" -> new String[]{"all", "body", "item", "shaft"};
            case "FlywheelBuilder" -> new String[]{"all", "body", "item", "wheel", "shaft"};
            case "GaugeBuilder" -> new String[]{"all", "body", "item", "head", "dial", "shaft"};
            case "CasingBuilder" -> new String[]{"block", "connected", "encased_cogwheel_side", "encased_cogwheel_side_connected", "gearbox", "encased_pipe", "all", "north", "south", "west", "east", "up", "down"};
            case "ShaftBuilder" -> new String[]{"axis", "axis_top", "shaft", "shaft_top", "all"};
            case "CogwheelBuilder" -> new String[]{"gear", "shaft", "shaft_top"};
            case "GearboxBuilder" -> new String[]{"casing", "gearbox", "all"};
            case "ChainBuilder" -> new String[]{"chain", "item", "all"};
            case "ChainConveyorBuilder" -> new String[]{"casing", "guard", "wheel", "axis", "axis_top", "all"};
            case "BlazeBurnerBuilder" -> new String[]{"brazier", "brazier_soul", "blaze", "blaze_active", "blaze_super", "blaze_super_active", "inert", "rods", "rods_2", "super_rods", "super_rods_2", "flame", "flame_scroll", "super_flame_scroll", "fire", "soul_fire", "goggles", "goggles_small", "train_hat", "logistics_hat", "particle", "all"};
            case "SteamEngineBuilder" -> new String[]{"engine", "linkage", "all"};
            case "SteamWhistleBuilder" -> new String[]{"whistle", "plate", "powered", "all"};
            case "ChainDriveBuilder" -> new String[]{"side", "end", "middle", "gearbox", "axis", "axis_top"};
            case "TransmissionBuilder" -> new String[]{"side", "powered", "gearbox", "frame", "axis", "axis_top", "top", "brass", "casing", "bracket"};
            case "MechanicalBeltBuilder" -> new String[]{"belt", "belt_offset", "belt_diagonal", "belt_scroll", "belt_diagonal_scroll", "item"};
            case "FluidPipeBuilder" -> new String[]{"pipe", "pipe_connected", "glass"};
            case "PumpBuilder" -> new String[]{"pump", "cog", "pipe"};
            case "SmartFluidPipeBuilder" -> new String[]{"smart_pipe_1", "smart_pipe_2", "smart_pipe_3", "body", "body_1", "body_2", "body_3", "pipe"};
            case "FluidValveBuilder" -> new String[]{"fluid_valve", "pipe"};
            case "FluidTankBuilder" -> new String[]{"fluid_tank", "fluid_tank_connected", "fluid_tank_top", "fluid_tank_top_connected", "fluid_tank_inner", "fluid_tank_inner_connected", "fluid_tank_window", "fluid_tank_window_single", "boiler_gauge"};
            case "ItemVaultBuilder" -> new String[]{"vault_front_small", "vault_front_medium", "vault_front_large", "vault_side_small", "vault_side_medium", "vault_side_large", "vault_top_small", "vault_top_medium", "vault_top_large", "vault_bottom_small", "vault_bottom_medium", "vault_bottom_large"};
            default -> new String[0];
        };
    }

    private static String[] modelRoles(String name) {
        return switch (name) {
            case "ShaftBuilder" -> new String[]{"block", "body", "shaft", "item"};
            case "CogwheelBuilder" -> new String[]{"cogwheel", "shaft", "shaft_half"};
            case "GearboxBuilder" -> new String[]{"block", "body", "gearbox", "shaft", "shaft_half", "item"};
            case "ChainBuilder" -> new String[]{"block", "body", "chain", "item"};
            case "ChainConveyorBuilder" -> new String[]{"block", "body", "shaft", "wheel", "guard", "item"};
            case "BlazeBurnerBuilder" -> java.util.stream.Stream.concat(java.util.stream.Stream.of("block", "body", "burner", "item"), BlazeBurnerBuilder.PARTS.stream().sorted()).toArray(String[]::new);
            case "SteamEngineBuilder" -> new String[]{"block", "body", "engine", "piston", "linkage", "connector", "item"};
            case "SteamWhistleBuilder" -> java.util.stream.Stream.concat(java.util.stream.Stream.of("block", "body", "whistle", "item"), SteamAssets.WHISTLE_PARTS.stream().sorted()).toArray(String[]::new);
            case "ChainDriveBuilder" -> new String[]{"block", "body", "single", "end_horizontal", "end_vertical", "middle_horizontal", "middle_vertical", "shaft", "item"};
            case "TransmissionBuilder" -> new String[]{"block", "body", "powered", "shaft_half", "shaft", "bracket", "item"};
            case "PumpBuilder" -> new String[]{"block", "body", "pump", "cog", "cogwheel", "item"};
            case "SmartFluidPipeBuilder" -> new String[]{"block", "body", "item"};
            case "MechanicalArmBuilder" -> new String[]{"block", "body", "mechanical_arm", "cog", "base", "lower_body", "upper_body", "claw", "claw_goggles", "grip_upper", "grip_lower", "item"};
            case "ChuteBuilder" -> new String[]{"block", "body", "chute", "smart_chute", "item"};
            case "FunnelBuilder" -> new String[]{"block", "body", "andesite_funnel", "brass_funnel", "belt", "flap", "belt_flap", "item"};
            case "TunnelBuilder" -> new String[]{"block", "body", "andesite_tunnel", "brass_tunnel", "flap", "item"};
            case "WaterWheelBuilder" -> new String[]{"block", "body", "water_wheel", "large_water_wheel", "wheel", "wheel_extension", "item"};
            case "WindmillBearingBuilder" -> new String[]{"block", "body", "windmill_bearing", "top", "shaft", "item"};
            case "HandCrankBuilder" -> new String[]{"block", "body", "hand_crank", "base", "handle", "item"};
            case "CreativeMotorBuilder" -> new String[]{"block", "body", "creative_motor", "shaft", "item"};
            case "BasinBuilder" -> new String[]{"block", "body", "basin", "item"};
            case "FlywheelBuilder" -> new String[]{"block", "body", "flywheel", "wheel", "shaft", "item"};
            case "GaugeBuilder" -> new String[]{"block", "body", "speedometer", "stressometer", "head", "dial", "shaft", "item"};
            default -> new String[0];
        };
    }

    private static String literals(String... values) {
        return java.util.Arrays.stream(values).map(value -> "\"" + value + "\"").collect(Collectors.joining(" | "));
    }

    /** Maps the shared metadata policy to ProbeJS 8's public document model. */
    private static final class Api {
        final ClassDecl document;
        Api(ClassDecl document) { this.document = document; }

        List<MethodDecl> methods() {
            // A filtered mutable view keeps signature removals/additions in the actual document.
            return new AbstractList<>() {
                private List<MethodDecl> current() {
                    return document.members.stream().filter(MethodDecl.class::isInstance).map(MethodDecl.class::cast).toList();
                }
                @Override public MethodDecl get(int index) { return current().get(index); }
                @Override public int size() { return current().size(); }
                @Override public MethodDecl remove(int index) {
                    MethodDecl old = get(index); document.members.remove(old); return old;
                }
                @Override public boolean add(MethodDecl method) { return document.members.add(method); }
            };
        }
        List<ParamDecl> params(Object method) { return ((MethodDecl) method).params; }
        String name(Object method) { return ((MethodDecl) method).name; }
        boolean matches(Object method, String expected) { return name(method).equals(expected); }
        void comment(Object target, String... lines) { ((CommentableCode) target).addComments(lines); }
        void returns(Object method, String type) { ((MethodDecl) method).returnType = Types.raw(type); }
        void preserveFluentReturns() {
            for (MethodDecl method : methods()) {
                Object type = method.returnType;
                if (type instanceof ParamType parameterized) type = parameterized.baseType;
                if (!(type instanceof ClassType clazz)) continue;
                String path = clazz.classPath.asJavaPath();
                if (path.equals("dev.latvian.mods.kubejs.registry.BuilderBase")
                        || path.equals("dev.latvian.mods.kubejs.registry.ModelledBuilderBase")
                        || path.equals("dev.latvian.mods.kubejs.block.BlockBuilder")
                        || path.equals("dev.latvian.mods.kubejs.item.ItemBuilder")
                        || path.startsWith(PACKAGE) && path.endsWith("Builder"))
                    method.returnType = Types.THIS;
            }
        }
        void methodComment(String name, String text) {
            for (MethodDecl method : methods()) if (matches(method, name)) comment(method, text);
        }
        void fluent(String name, String text) {
            for (MethodDecl method : methods()) if (matches(method, name)) { comment(method, text); returns(method, "this"); }
        }
        void narrow(String name, int count, int index, String paramName, String type) {
            for (MethodDecl method : methods()) if (matches(method, name) && params(method).size() == count)
                params(method).set(index, new ParamDecl(paramName, Types.raw(type)));
        }
        void narrowList(String name, String paramName, String type) {
            for (MethodDecl method : methods()) if (matches(method, name) && params(method).size() == 1)
                params(method).set(0, new ParamDecl(paramName, Types.raw(type)));
        }
        void unsupportedStorageModels() {
            comment(document, "Storage world geometry and blockstates are generated for multiblock connections; model(...), modelJson and blockstateJson are unsupported. Customize textures, or use item(item => item.parentModel(path)) for an inventory model.");
            methods().removeIf(method -> matches(method, "model"));
            signature("model", "never", "Unsupported for storage world geometry. Customize textures or the inventory item through item(...).",
                    new String[]{"path"}, new String[]{"never"});
            document.members.removeIf(member -> member instanceof FieldDecl field && Set.of("modelJson", "blockstateJson", "model").contains(field.name));
            for (String name : List.of("modelJson", "blockstateJson")) {
                FieldDecl field = new FieldDecl(name, Types.NEVER, false);
                field.addComments("Unsupported for storage world geometry. Use texture roles or customize the inventory item through item(...).");
                document.members.add(field);
            }
        }
        void cogwheelModels() {
            methods().removeIf(method -> matches(method, "model") || matches(method, "modelSet")
                    || matches(method, "chainable"));
            signature("model", "this", "Select shaftless cogwheel geometry or a shaft/shaft_half model. Omitted parts use Create's original models. Customize the inventory through item(item => item.parentModel(path)). Paths omit models/ and .json.",
                    new String[]{"role", "path"}, new String[]{literals("cogwheel", "shaft", "shaft_half"), "string"});
            // A subclass declaration shadows the inherited complete-model overloads.
            signature("parentModel", "never", "Complete block model replacement is unsupported for cogwheels. Select named parts, or set the inventory model through item(item => item.parentModel(path)).",
                    new String[]{"path"}, new String[]{"never"});
            methods().removeIf(method -> matches(method, "modelGenerator"));
            signature("modelGenerator", "never", "Complete block model generation is unsupported for cogwheels. Use named part resources or customize the inventory through item(...).",
                    new String[]{"generator"}, new String[]{"never"});
            document.members.removeIf(member -> member instanceof FieldDecl field && field.name.equals("modelJson"));
            FieldDecl modelJson = new FieldDecl("modelJson", Types.NEVER, false);
            modelJson.addComments("Complete block model JSON is unsupported for cogwheels. Use named part resources or customize the inventory through item(...).");
            document.members.add(modelJson);
        }
        void hideImplementationMembers(String name) throws ReflectiveOperationException {
            Set<String> ownFields = name.equals("BlockBuilder") ? Set.of()
                    : java.util.Arrays.stream(Class.forName(PACKAGE + name, false, CBJSProbeJS.class.getClassLoader()).getDeclaredFields())
                            .map(Field::getName).collect(Collectors.toSet());
            document.members.removeIf(member -> member instanceof FieldDecl field && ownFields.contains(field.name));
            Set<String> helpers = Set.of("createObject", "createAdditionalObjects", "generateAssets", "generateData", "createProperties",
                    "transformObject", "getRegistryType", "generateLang", "generateTags", "generateRecipes", "generateModels", "setType",
                    "modelOverrides", "nativeBlockId", "independentWindowsEnabled", "enabledWindowFaces", "defaultWindowFaces", "maximumLength", "texture");
            methods().removeIf(method -> helpers.contains(method.name)
                    && !(method.name.equals("texture") && ITEM_BUILDERS.containsKey(name) && method.params.size() == 1));
        }
        void componentInput(String name, boolean array) {
            for (MethodDecl method : methods()) if (matches(method, name) && method.params.size() == 1) {
                Type component = Types.clazz(net.minecraft.network.chat.Component.class).asInput();
                method.params.set(0, new ParamDecl(array ? "lines" : "text", array ? new ArrayType(component) : component));
            }
        }
        void componentArgument(String name, int index) {
            for (MethodDecl method : methods()) if (matches(method, name) && method.params.size() > index)
                method.params.set(index, new ParamDecl("text", Types.clazz(net.minecraft.network.chat.Component.class).asInput()));
        }
        void nullableReturn(Object method, String type) {
            ((MethodDecl) method).returnType = Types.union(Types.clazz(type), Types.NULL);
        }
        static Type structuredComponent(boolean translated) {
            return Types.object(object -> {
                Type text = Types.clazz(net.minecraft.network.chat.Component.class).asInput();
                object.param(translated ? "translate" : "text", Types.raw(translated ? "string" : "string | number | boolean"));
                if (translated) object.param("with", true, new ArrayType(text));
                object.param("color", true, Types.raw("string | number"));
                for (String style : List.of("bold", "italic", "underlined", "strikethrough", "obfuscated")) object.param(style, true, Types.BOOLEAN);
                object.param("insertion", true, Types.STRING).param("font", true, Types.STRING)
                        .param("click", true, Types.raw("string | {action: string; value: string}"))
                        .param("hover", true, text).param("extra", true, new ArrayType(text));
            });
        }
        static void componentAliases(AliasRegistrar registrar) {
            for (Type type : List.of(Types.STRING, Types.NUMBER, Types.BOOLEAN,
                    Types.clazz(net.minecraft.network.chat.MutableComponent.class), structuredComponent(false), structuredComponent(true),
                    new ArrayType(Types.clazz(net.minecraft.network.chat.Component.class).asInput())))
                registrar.addInputAlias(net.minecraft.network.chat.Component.class, type);
        }
        void signature(String name, String type, String text, String[] names, String[] types) {
            methods().removeIf(method -> method.name.equals(name) && method.params.size() == names.length);
            MethodBuilder builder = new MethodBuilder(name).returnType(Types.raw(type));
            for (int i = 0; i < names.length; i++) builder.param(names[i], Types.raw(types[i]));
            MethodDecl method = builder.build(); method.addComments(text); methods().add(method);
        }
    }
}
