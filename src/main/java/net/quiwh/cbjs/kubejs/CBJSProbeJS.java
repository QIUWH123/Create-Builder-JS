package net.quiwh.cbjs.kubejs;

import com.mojang.logging.LogUtils;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.IEventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraftforge.fml.ModList;

/**
 * Optional ProbeJS 6 integration through its public document-generation event.
 * Reflection keeps ProbeJS out of the runtime linkage and the production compile classpath.
 * Registry and event overloads remain owned by ProbeJS's native compilers.
 */
public final class CBJSProbeJS {
    private static final String PACKAGE = "net.quiwh.cbjs.kubejs.";
    private static final IEventHandler DOCUMENT_LISTENER = CBJSProbeJS::generate;
    private static final String[] FACES = {"down", "up", "north", "south", "west", "east"};
    private static final String[] WINDOW_FACES = {"side", "end", "all"};
    private static final String[] WINDOW_STYLES = {"full", "none", "strip"};
    private static final Map<String, List<String>> BUILDERS = builders();
    private static final Map<String, String> ITEM_BUILDERS = Map.of("GogglesBuilder", "goggles", "WrenchBuilder", "wrench");

    private CBJSProbeJS() { }

    /** Called from SERVER registerBindings, after each script-manager listener reset. */
    public static void register() {
        if (!ModList.get().isLoaded("probejs")) return;
        try {
            Class<?> events = Class.forName("com.probejs.features.plugin.ProbeJSEvents");
            EventHandler handler = (EventHandler) events.getField("DOC_GEN").get(null);
            boolean[] present = {false};
            handler.forEachListener(ScriptType.SERVER, entry -> {
                if (entry.handler == DOCUMENT_LISTENER) present[0] = true;
            });
            if (!present[0]) handler.listenJava(ScriptType.SERVER, null, DOCUMENT_LISTENER);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LogUtils.getLogger().warn("Unable to register optional CBJS ProbeJS metadata", exception);
        }
    }

    private static Object generate(dev.latvian.mods.kubejs.event.EventJS event) {
        try {
            Method transform = event.getClass().getMethod("transformDocument", Class.class, Consumer.class);
            installEventOverloads();
            for (String name : BUILDERS.keySet()) registerTransform(event, transform, PACKAGE + name);
            for (String name : ITEM_BUILDERS.keySet()) registerTransform(event, transform, PACKAGE + name);
            registerTransform(event, transform, "dev.latvian.mods.kubejs.block.BlockBuilder");
            registerTransform(event, transform, PACKAGE + "FluidDeviceBuilder");
            registerTransform(event, transform, PACKAGE + "ContraptionBlockBuilder");
            registerTransform(event, transform, "net.minecraft.network.chat.Component");
            registerTransform(event, transform, PACKAGE + "GogglesKineticInfo");
            registerTransform(event, transform, PACKAGE + "GogglesStorageInfo");
            // These classes are reached by ProbeJS from the actual registered event handlers.
            for (String name : List.of("FluidPipeEventJS",
                    "FluidPipeFlowChangedEventJS", "FluidPipePressureChangedEventJS", "FluidPipeCollisionEventJS",
                    "FluidPipeFlowEventJS", "KineticDataModifyEventJS", "KineticSpeedChangedEventJS", "KineticStressChangedEventJS",
                    "KineticOverloadEventJS", "KineticEventJS", "StorageEventJS", "StorageFluidChangedEventJS",
                    "StorageInventoryChangedEventJS", "StorageStructureEventJS", "StorageFormedEventJS",
                    "StorageResizedEventJS", "StorageSplitEventJS", "GogglesTooltipEventJS", "BlazeBurnerFuelEventJS",
                    "BeltItemEventJS", "BeltItemTransferEventJS", "BeltItemProcessedEventJS")) {
                registerTransform(event, transform, PACKAGE + name);
            }
            return null;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("CBJS metadata requires the ProbeJS 6 document API", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void installEventOverloads() throws ReflectiveOperationException {
        // ProbeJS's stock compiler knows zero/one extra argument. These runtime events
        // support two selectors; use its public override registry to retain every form.
        Class<?> compiler = Class.forName("com.probejs.docs.EventCompiler");
        Map<Object, Function<EventHandler, List<String>>> overrides =
                (Map<Object, Function<EventHandler, List<String>>>) compiler.getField("SPECIAL_EVENT_OVERRIDE").get(null);
        selectorOverloads(overrides, "fluidPipeFlow", "FluidPipeFlowEventJS", "selector: string | string[] | null",
                "fluid: string | string[] | null, block: string | string[] | null",
                "Active connections; paired fluid/block selectors match AND. A single selector or list retains each entry's scalar role: known fluid IDs match fluids first, block IDs match pipes, and #tags match fluid OR block. One callback even when entries overlap.");
        selectorOverloads(overrides, "storageFluidChanged", "StorageFluidChangedEventJS", "container: string | string[] | null",
                "container: string | string[] | null, fluid: string | string[] | null",
                "Container selector first, fluid selector second; one selector/list filters containers. Matches previous OR current nonempty contents; one callback even when entries overlap.");
        selectorOverloads(overrides, "storageInventoryChanged", "StorageInventoryChangedEventJS", "container: string | string[] | null",
                "container: string | string[] | null, item: string | string[] | null",
                "Container selector first, item selector second; one selector/list filters containers. Matches previous OR current nonempty changed stacks; one callback per batch retains all slot changes even when entries overlap.");
        for (String eventName : List.of("beltItemEntered", "beltItemExited", "beltItemProcessed")) {
            selectorOverloads(overrides, eventName,
                    eventName.equals("beltItemProcessed") ? "BeltItemProcessedEventJS" : "BeltItemTransferEventJS",
                    "belt: string | string[] | null", "belt: string | string[] | null, item: string | string[] | null",
                    "Completed native belt actions reported at server tick end; belt selector first, item second. Simulations are silent. Processing matches input or outputs. Notifications cannot cancel the action.");
        }
    }

    private static void selectorOverloads(Map<Object, Function<EventHandler, List<String>>> overrides,
            String eventName, String payload, String singleSelector, String pairedSelectors, String description) {
        overrides.put(com.mojang.datafixers.util.Pair.of("CBJSEvents", eventName), handler -> {
            try {
                Class<?> resolver = Class.forName("com.probejs.docs.formatter.NameResolver");
                Object resolved = resolver.getMethod("getResolvedName", String.class).invoke(null, PACKAGE + payload);
                String type = (String) resolved.getClass().getMethod("getFullName").invoke(resolved);
                String callback = "handler: (event: " + type + ") => void";
                return List.of("/** @at server; " + description + " Each selector accepts an ID, #tag, or flat array mixing IDs/#tags. Entries match with OR; paired positions match with AND. An empty array matches nothing. Top-level '*' and null are wildcards. */",
                        eventName + "(" + callback + "): void,",
                        eventName + "(" + singleSelector + ", " + callback + "): void,",
                        eventName + "(" + pairedSelectors + ", " + callback + "): void,");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot format CBJS " + eventName + " overloads", exception);
            }
        });
    }

    private static void registerTransform(Object event, Method transform, String className)
            throws ReflectiveOperationException {
        Class<?> target;
        try {
            target = Class.forName(className, false, CBJSProbeJS.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            // Optional payload classes may be absent in an older CBJS installation.
            return;
        }
        Consumer<Object> consumer = document -> transformDocument(target.getSimpleName(), document);
        transform.invoke(event, target, consumer);
    }

    /** Public for an offline verification using real ProbeJS DocumentClass and formatters. */
    public static void transformDocument(String name, Object document) {
        try {
            Api api = new Api(document);
            if (BUILDERS.containsKey(name) || ITEM_BUILDERS.containsKey(name) || name.equals("BlockBuilder") || name.equals("FluidDeviceBuilder"))
                api.preserveFluentReturns();
            if (BUILDERS.containsKey(name)) builder(name, api);
            if (ITEM_BUILDERS.containsKey(name)) equipment(name, api);
            if (name.equals("BlockBuilder") || name.equals("FluidDeviceBuilder")) api.hideImplementationMembers(name);
            if (name.equals("BlockBuilder") || name.equals("ContraptionBlockBuilder")) {
                contraptions(api);
                api.signature("replaceTexture", "this", "Replace one model texture slot; preserve the current geometry.",
                        new String[]{"role", "path"}, new String[]{"string", "string"});
            }
            if (name.endsWith("EventJS")) event(name, api);
            if (name.equals("GogglesKineticInfo") || name.equals("GogglesStorageInfo")) gogglesData(name, api);
            if (name.equals("Component")) api.componentAssignments();
            if (name.equals("FluidDeviceBuilder")) api.comment(document,
                    "Shared native Create device settings. Native devices own their block entity; blockEntity(...) is unsupported.");
            api.rehash();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot enrich ProbeJS document " + name, exception);
        }
    }

    private static Map<String, List<String>> builders() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("CasingBuilder", List.of("casing"));
        result.put("ShaftBuilder", List.of("shaft"));
        result.put("ChainBuilder", List.of("chain"));
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
        result.put("CogwheelBuilder", List.of("cogwheel", "large_cogwheel", "shaftless_cogwheel", "large_shaftless_cogwheel"));
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
        String[] textures = textureRoles(name);
        if (textures.length > 0) api.signature("replaceTexture", "this",
                "Choose a semantic texture role. Ordinary model face names are also accepted by KubeJS.",
                new String[]{"role", "path"}, new String[]{literals(textures) + " | (string & {})", "string"});
        String[] models = modelRoles(name);
        if (models.length > 0) {
            api.narrow("model", 2, 0, "role", literals(models));
            api.fluent("model", "Select a supported block/partial/item model resource. A one-argument model(path) selects the main model; model(role, path) replaces one supported part. Paths omit models/ and .json.");
        }
        switch (name) {
            case "CasingBuilder" -> {
                api.fluent("cogwheelCasing", "Enable Create cogwheel encasing for this casing. Default: false; no argument enables it.");
                api.fluent("shaftCasing", "Enable Create shaft encasing for this casing. Default: false; no argument enables it.");
                api.fluent("fluidCasing", "Enable Create fluid-pipe encasing for this casing. Default: false; no argument enables it.");
                api.fluent("casingTextures", "Set six Create casing roles from a namespace:block/prefix: _casing, _casing_connected, _encased_cogwheel_side, _encased_cogwheel_side_connected, _gearbox and _encased_pipe.");
            }
            case "CogwheelBuilder" -> {
                api.fluent("chainable", "Enable Bits 'n' Bobs chained-cogwheel compatibility when that optional mod is installed. Default: false; no argument enables it.");
                api.fluent("modelSet", "Select a shipped cogwheel material model set; preserves matching rotating parts and encased variants.");
            }
            case "ChainDriveBuilder" -> api.fluent("model", "A primary model supplies every stationary connection shape. Named single/end_horizontal/end_vertical/middle_horizontal/middle_vertical override one shape; shaft replaces the moving shaft, and item selects an independent item model. Omitted parts use native Create geometry.");
            case "TransmissionBuilder" -> {
                api.fluent("model", "Native clutch/gearshift/controller geometry by default. model(path) replaces the stationary body. Split transmissions support powered and shaft_half (shaft alias); the controller supports shaft and bracket. Item models may be selected separately.");
                api.comment(api.document, "Redstone, wrench interaction and speed-controller target behavior use Create's native mechanics. maxRpm/noMaxRPM change material failure limits; the controller target UI retains Create's config range.");
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
        api.fluent("noSteamEngine", "Disable Create boiler/steam-engine behavior. Default: enabled.");
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
        if (name.equals("BlazeBurnerFuelEventJS")) {
            api.comment(api.document, "CBJSEvents.blazeBurnerFuel(callback) in server_scripts. Rebuilt atomically on /reload; omitted entries return to native fuel rules.");
            api.fluent("addFuel", "Define normal burner fuel by item ID or #item tag, with positive integral burn ticks. Exact IDs take priority over tags. Uses Create's normal insertion and consumption behavior.");
            api.fluent("addSuperFuel", "Define superheated burner fuel by item ID or #item tag, with positive integral burn ticks. Exact IDs take priority over tags. Uses Create's normal insertion and consumption behavior.");
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
                if (api.matches(method, "getFluid")) api.nullableReturn(method, "net.minecraftforge.fluids.FluidStack");
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
            case "CasingBuilder" -> new String[]{"casing", "casing_connected", "cogwheel_side", "cogwheel_side_connected", "shaft", "pipe", "all", "north", "south", "west", "east", "up", "down"};
            case "ShaftBuilder" -> new String[]{"axis", "axis_top", "shaft", "shaft_top", "all"};
            case "CogwheelBuilder" -> new String[]{"gear", "shaft", "shaft_top"};
            case "GearboxBuilder" -> new String[]{"casing", "gearbox", "all"};
            case "ChainBuilder" -> new String[]{"chain", "item", "all"};
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
            case "CogwheelBuilder" -> new String[]{"cogwheel", "shaft", "shaft_half", "item"};
            case "GearboxBuilder" -> new String[]{"block", "body", "gearbox", "shaft", "shaft_half", "item"};
            case "ChainBuilder" -> new String[]{"block", "body", "chain", "item"};
            case "ChainDriveBuilder" -> new String[]{"block", "body", "single", "end_horizontal", "end_vertical", "middle_horizontal", "middle_vertical", "shaft", "item"};
            case "TransmissionBuilder" -> new String[]{"block", "body", "powered", "shaft_half", "shaft", "bracket", "item"};
            case "PumpBuilder" -> new String[]{"block", "body", "pump", "cog", "cogwheel", "item"};
            case "SmartFluidPipeBuilder" -> new String[]{"block", "body", "item"};
            default -> new String[0];
        };
    }

    private static String literals(String... values) {
        return java.util.Arrays.stream(values).map(value -> "\"" + value + "\"").collect(Collectors.joining(" | "));
    }

    /** The small reflective boundary uses only public ProbeJS 6 API members. */
    private static final class Api {
        final Object document;
        final Class<?> methodClass = Class.forName("com.probejs.jdoc.document.DocumentMethod");
        final Class<?> typeClass = Class.forName("com.probejs.jdoc.property.PropertyType");
        final Constructor<?> nativeType = Class.forName("com.probejs.jdoc.property.PropertyType$Native").getConstructor(String.class);
        final Constructor<?> param = Class.forName("com.probejs.jdoc.property.PropertyParam").getConstructor(String.class, typeClass, boolean.class);
        final Constructor<?> comment = Class.forName("com.probejs.jdoc.property.PropertyComment").getConstructor(String[].class);
        final Method addProperty = Class.forName("com.probejs.jdoc.document.AbstractDocumentBase")
                .getMethod("addProperty", Class.forName("com.probejs.jdoc.property.AbstractProperty"));
        final Field methodName = methodClass.getField("name");
        final Field methodParams = methodClass.getField("params");
        final Field methodReturns = methodClass.getField("returns");

        Api(Object document) throws ReflectiveOperationException { this.document = document; }

        @SuppressWarnings("unchecked") Set<Object> methods() throws ReflectiveOperationException {
            return (Set<Object>) document.getClass().getMethod("getMethods").invoke(document);
        }
        @SuppressWarnings("unchecked") List<Object> params(Object method) throws IllegalAccessException {
            return (List<Object>) methodParams.get(method);
        }
        void rehash() throws ReflectiveOperationException {
            // Method equality/hash codes include the signatures changed above.
            List<Object> updated = new ArrayList<>(methods());
            methods().clear();
            methods().addAll(updated);
        }
        String name(Object method) throws IllegalAccessException { return (String) methodName.get(method); }
        boolean matches(Object method, String expected) throws IllegalAccessException {
            String actual = name(method);
            return actual.equals(expected) || actual.startsWith(expected + "(");
        }
        void comment(Object target, String... lines) throws ReflectiveOperationException {
            addProperty.invoke(target, comment.newInstance((Object) lines));
        }
        void returns(Object method, String type) throws ReflectiveOperationException { methodReturns.set(method, nativeType.newInstance(type)); }
        void preserveFluentReturns() throws ReflectiveOperationException {
            for (Object method : methods()) {
                Object type = methodReturns.get(method);
                // BuilderBase<T> is the declared return of displayName and several
                // inherited helpers; inspect its base without flattening other generics.
                if (type.getClass().getName().equals("com.probejs.jdoc.property.PropertyType$Parameterized"))
                    type = type.getClass().getMethod("getBase").invoke(type);
                String serialized = type.getClass().getMethod("serialize").invoke(type).toString();
                // Match exact Java return types, leaving other objects and collections intact.
                for (String builder : List.of("dev.latvian.mods.kubejs.block.BlockBuilder",
                        "dev.latvian.mods.kubejs.registry.BuilderBase", "dev.latvian.mods.kubejs.item.ItemBuilder", PACKAGE + "FluidDeviceBuilder")) {
                    if (serialized.equals("{\"type\":\"type:class\",\"name\":\"" + builder + "\"}")) {
                        returns(method, "this");
                        break;
                    }
                }
            }
        }
        void methodComment(String name, String text) throws ReflectiveOperationException {
            for (Object method : methods()) if (matches(method, name)) comment(method, text);
        }
        void fluent(String name, String text) throws ReflectiveOperationException {
            for (Object method : methods()) if (matches(method, name)) { comment(method, text); returns(method, "this"); }
        }
        void narrow(String name, int count, int index, String paramName, String type) throws ReflectiveOperationException {
            for (Object method : methods()) if (matches(method, name) && params(method).size() == count)
                params(method).set(index, param.newInstance(paramName, nativeType.newInstance(type), false));
        }
        void narrowList(String name, String paramName, String type) throws ReflectiveOperationException {
            for (Object method : methods()) if (matches(method, name) && params(method).size() == 1) {
                Object parameter = params(method).get(0);
                Object parameterType = parameter.getClass().getMethod("getType").invoke(parameter);
                String javaType = parameterType.getClass().getMethod("serialize").invoke(parameterType).toString();
                if (javaType.contains("java.util.List"))
                    params(method).set(0, param.newInstance(paramName, nativeType.newInstance(type), false));
            }
        }
        void unsupportedStorageModels() throws ReflectiveOperationException {
            apiCommentStorageModels();
            List<Object> unsupported = new ArrayList<>();
            for (Object method : methods()) if (matches(method, "model")) unsupported.add(method);
            methods().removeIf(unsupported::contains);
            signature("model", "never", "Unsupported for storage world geometry. Customize texture roles, or use item(item => item.parentModel(path)) for an inventory model.",
                    new String[]{"path"}, new String[]{"never"});
            @SuppressWarnings("unchecked") Set<Object> fields = (Set<Object>) document.getClass().getMethod("getFields").invoke(document);
            List<Object> hidden = new ArrayList<>();
            for (Object field : fields) {
                String fieldName = (String) field.getClass().getMethod("getName").invoke(field);
                if (Set.of("modelJson", "blockstateJson", "model").contains(fieldName)) hidden.add(field);
            }
            fields.removeAll(hidden);
            // Removing a field here exposes BlockBuilder's inherited `any` field.
            // Shadow unsupported raw world overrides so completion rejects writes.
            Class<?> fieldClass = Class.forName("com.probejs.jdoc.document.DocumentField");
            for (String fieldName : List.of("modelJson", "blockstateJson")) {
                Object field = fieldClass.getConstructor().newInstance();
                com.google.gson.JsonObject definition = new com.google.gson.JsonObject();
                definition.add("properties", new com.google.gson.JsonArray());
                definition.addProperty("name", fieldName);
                definition.add("fieldType", (com.google.gson.JsonObject) typeClass.getMethod("serialize")
                        .invoke(nativeType.newInstance("never")));
                fieldClass.getMethod("deserialize", com.google.gson.JsonObject.class).invoke(field, definition);
                comment(field, "Unsupported for storage world geometry. Use texture roles or customize the inventory item through item(...).");
                fields.add(field);
            }
        }
        void apiCommentStorageModels() throws ReflectiveOperationException {
            comment(document, "Storage world geometry and blockstates are generated for multiblock connections; model(...), modelJson and blockstateJson are unsupported. Customize textures, or use item(item => item.parentModel(path)) for an inventory model.");
        }
        void hideImplementationMembers(String name) throws ReflectiveOperationException {
            Set<String> ownFields = name.equals("BlockBuilder") ? Set.of()
                    : java.util.Arrays.stream(Class.forName(PACKAGE + name, false,
                            CBJSProbeJS.class.getClassLoader()).getDeclaredFields())
                            .map(Field::getName).collect(Collectors.toSet());
            @SuppressWarnings("unchecked") Set<Object> fields = (Set<Object>) document.getClass().getMethod("getFields").invoke(document);
            List<Object> hidden = new ArrayList<>();
            for (Object field : fields)
                if (ownFields.contains((String) field.getClass().getMethod("getName").invoke(field))) hidden.add(field);
            fields.removeAll(hidden);
            // Keep user-facing query methods. Only registration/resource lifecycle helpers are hidden.
            Set<String> helpers = Set.of("createObject", "createAdditionalObjects", "generateAssetJsons",
                    "generateDataJsons", "createProperties", "transformObject", "getRegistryType",
                    "generateLang", "generateTags", "generateRecipes", "generateModels", "setType",
                    "modelOverrides", "independentWindowsEnabled", "enabledWindowFaces", "defaultWindowFaces", "maximumLength", "texture");
            List<Object> lifecycle = new ArrayList<>();
            for (Object method : methods()) {
                String member = name(method);
                if (member.equals("texture") && ITEM_BUILDERS.containsKey(name) && params(method).size() == 1) continue;
                if (helpers.stream().anyMatch(helper -> member.equals(helper) || member.startsWith(helper + "("))) lifecycle.add(method);
            }
            methods().removeIf(lifecycle::contains);
        }
        void componentInput(String name, boolean array) throws ReflectiveOperationException {
            for (Object method : methods()) if (matches(method, name) && params(method).size() == 1) {
                Object component = classType("net.minecraft.network.chat.Component");
                if (array) component = arrayType(component);
                params(method).set(0, param.newInstance(array ? "lines" : "text", component, false));
            }
        }
        void componentArgument(String name, int index) throws ReflectiveOperationException {
            for (Object method : methods()) if (matches(method, name) && params(method).size() > index)
                params(method).set(index, param.newInstance("text", classType("net.minecraft.network.chat.Component"), false));
        }
        void nullableReturn(Object method, String type) throws ReflectiveOperationException {
            methodReturns.set(method, union(classType(type), nativeType.newInstance("null")));
        }
        Object classType(String name) throws ReflectiveOperationException {
            return Class.forName("com.probejs.jdoc.property.PropertyType$Clazz").getConstructor(String.class).newInstance(name);
        }
        Object arrayType(Object component) throws ReflectiveOperationException {
            return Class.forName("com.probejs.jdoc.property.PropertyType$Array").getConstructor(typeClass).newInstance(component);
        }
        Object union(Object... types) throws ReflectiveOperationException {
            return Class.forName("com.probejs.jdoc.property.PropertyType$Union").getConstructor(List.class).newInstance(List.of(types));
        }
        void objectField(Object object, String name, Object type, boolean optional) throws ReflectiveOperationException {
            Class<?> keyClass = Class.forName("com.probejs.jdoc.property.PropertyType$JSObjectKey");
            Object key = keyClass.getConstructor().newInstance();
            keyClass.getMethod("withName", String.class).invoke(key, name);
            keyClass.getMethod("optional", boolean.class).invoke(key, optional);
            object.getClass().getMethod("add", keyClass, typeClass).invoke(object, key, type);
        }
        Object structuredComponent(boolean translated) throws ReflectiveOperationException {
            Object object = Class.forName("com.probejs.jdoc.property.PropertyType$JSObject").getConstructor().newInstance();
            Object text = classType("net.minecraft.network.chat.Component");
            objectField(object, translated ? "translate" : "text",
                    nativeType.newInstance(translated ? "string" : "string | number | boolean"), false);
            if (translated) objectField(object, "with", arrayType(text), true);
            objectField(object, "color", nativeType.newInstance("string | number"), true);
            for (String style : List.of("bold", "italic", "underlined", "strikethrough", "obfuscated"))
                objectField(object, style, nativeType.newInstance("boolean"), true);
            objectField(object, "insertion", nativeType.newInstance("string"), true);
            objectField(object, "font", nativeType.newInstance("string"), true);
            objectField(object, "click", nativeType.newInstance("string | {action: string; value: string}"), true);
            objectField(object, "hover", text, true);
            objectField(object, "extra", arrayType(text), true);
            return object;
        }
        void componentAssignments() throws ReflectiveOperationException {
            // Preserve class references until ProbeJS resolves final names. Recursion allows
            // structured extras/hover text and concatenated text arrays without using any.
            Class<?> assignmentClass = Class.forName("com.probejs.jdoc.property.PropertyAssign");
            for (Object type : List.of(nativeType.newInstance("string"), nativeType.newInstance("number"),
                    nativeType.newInstance("boolean"), classType("net.minecraft.network.chat.MutableComponent"),
                    structuredComponent(false), structuredComponent(true),
                    arrayType(classType("net.minecraft.network.chat.Component")))) {
                Object assignment = assignmentClass.getConstructor().newInstance();
                assignmentClass.getMethod("type", typeClass).invoke(assignment, type);
                addProperty.invoke(document, assignment);
            }
        }
        void signature(String name, String type, String text, String[] names, String[] types)
                throws ReflectiveOperationException {
            List<Object> replace = new ArrayList<>();
            for (Object method : methods()) if (name(method).equals(name) && params(method).size() == names.length) replace.add(method);
            methods().removeIf(replace::contains);
            Object method = methodClass.getConstructor().newInstance();
            methodName.set(method, name);
            returns(method, type);
            for (int i = 0; i < names.length; i++) params(method).add(param.newInstance(names[i], nativeType.newInstance(types[i]), false));
            comment(method, text);
            methods().add(method);
        }
    }
}
