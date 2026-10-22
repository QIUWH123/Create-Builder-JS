package net.quiwh.cbjs.content;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.quiwh.cbjs.kubejs.FluidTankBuilder;
import net.quiwh.cbjs.storage.StorageEventDispatcher;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Keeps Create's multiblock lifecycle while supplying material-specific storage and limits. */
public class ScriptedFluidTankBlockEntity extends FluidTankBlockEntity
        implements com.simibubi.create.api.contraption.transformable.TransformableBlockEntity {
    private int migrationRetryTicks;
    private boolean migratingSavedGeometry;
    private static final String FACES_TAG = "WindowFaces";
    private FluidWindowFaces windowFaces;
    private boolean windowsSuppressed;
    private volatile FluidTankFaceLayout.Panel[] facePanels;
    private boolean readingStorage;
    private int syncedReadCapacity;
    private int configGeneration = StorageStructureLimits.configGeneration();
    private int configuredMaxWidth;
    private int configuredMaxLength;
    private int previousFluidSignal = -1;
    private int previousFluidLight = -1;
    private int previousLitHeight;
    private boolean previousGas;
    private PanelInput panelInput;
    private WindowSnapshot windowSnapshot;

    private record PanelInput(Direction.Axis axis, int width, int height, BlockPos relative,
                              boolean visible, FluidWindowFaces faces) { }
    private record WindowSnapshot(FluidWindowFaces faces, int width, int height,
                                  boolean window, boolean suppressed) { }

    public ScriptedFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        configuredMaxWidth = builder().maximumWidth();
        configuredMaxLength = builder().maximumLength(width);
        window = builder().defaultWindows();
        boiler = new ScriptedBoilerData();
        windowFaces = builder().defaultWindowFaces(getMainConnectionAxis());
        resolveWindows();
        rebuildFacePanels();
    }

    protected final FluidTankBuilder builder() {
        return ((ScriptedFluidTankBlock) getBlockState().getBlock()).sourceBuilder();
    }

    @Override
    protected SmartFluidTank createInventory() {
        return new Inventory(builder().capacityPerBlock(), this::onFluidStackChanged,
                stack -> builder().acceptsFluid(stack), this);
    }

    @Override
    public int getMaxWidth() {
        return StorageStructureLimits.loadedWidth(this, this, builder().maximumWidth());
    }

    @Override
    public int getMaxLength(Direction.Axis axis, int width) {
        return axis == getMainConnectionAxis() ? builder().maximumLength(width) : getMaxWidth();
    }

    @Override
    public int getTankSize(int tank) {
        return builder().capacityPerBlock();
    }

    @Override
    public void applyFluidTankSize(int blocks) {
        if (!readingStorage && blocks != (long) width * width * height)
            StorageEventDispatcher.structureChanging(this);
        long capacity = (long) blocks * builder().capacityPerBlock();
        if (blocks < 1 || capacity > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Fluid tank size exceeds its supported capacity: " + blocks);
        tankInventory.setCapacity((int) capacity);
        int overflow = tankInventory.getFluidAmount() - tankInventory.getCapacity();
        if (overflow > 0) tankInventory.drain(overflow, FluidAction.EXECUTE);
        forceFluidLevelUpdate = true;
    }

    @Override public void setWidth(int width) {
        if (!readingStorage && width != this.width) StorageEventDispatcher.structureChanging(this);
        if (width != this.width) invalidateFluidEffects();
        super.setWidth(width);
        configuredMaxLength = builder().maximumLength(width);
    }

    @Override public void setHeight(int height) {
        if (!readingStorage && height != this.height) StorageEventDispatcher.structureChanging(this);
        if (height != this.height) invalidateFluidEffects();
        super.setHeight(height);
    }

    @Override public void setController(BlockPos controller) {
        if (!readingStorage && !getController().equals(controller)) StorageEventDispatcher.structureChanging(this);
        if (!getController().equals(controller)) invalidateFluidEffects();
        super.setController(controller);
    }

    @Override public void remove() {
        StorageEventDispatcher.removed(this);
        super.remove();
    }

    @Override public void initialize() {
        super.initialize();
        if (level.isClientSide) return;
        if (isController()) refreshWindows();
        else if (level.hasChunkAt(getController()) && getControllerBE() instanceof ScriptedFluidTankBlockEntity vessel
                && vessel.getBlockState().is(getBlockState().getBlock())
                && vessel.getMainConnectionAxis() == getMainConnectionAxis()) {
            vessel.resolveWindows();
            vessel.applyMemberWindow(this);
            vessel.updateMemberFluidLight(this);
        } else rebuildFacePanels();
    }


    @Override public void tick() {
        if (level != null && !level.isClientSide && configGeneration != StorageStructureLimits.configGeneration())
            refreshStorageSettings();
        super.tick();
    }

    /** Apply reloaded Create values without truncating existing fluid. */
    protected void refreshStorageSettings() {
        configGeneration = StorageStructureLimits.configGeneration();
        int maxWidth = builder().maximumWidth();
        int maxLength = builder().maximumLength(width);
        boolean dimensionsChanged = configuredMaxWidth != maxWidth || configuredMaxLength != maxLength;
        configuredMaxWidth = maxWidth;
        configuredMaxLength = maxLength;
        int capacity = isController()
                ? StorageStructureLimits.boundedCapacity(builder().capacityPerBlock(), width, height)
                : builder().capacityPerBlock();
        if (tankInventory.getCapacity() != capacity) {
            if (isController()) {
                StorageEventDispatcher.structureChanging(this);
            }
            tankInventory.setCapacity(capacity);
            if (isController()) {
                forceFluidLevelUpdate = true;
                onFluidStackChanged(tankInventory.getFluid());
            }
        }
        if (dimensionsChanged) {
            updateConnectivity = true;
            migrationRetryTicks = 0;
        }
    }

    @Override
    protected void updateConnectivity() {
        if (!level.isClientSide && isController()) {
            // A smaller configured capacity leaves saved contents drainable. Native splitting would
            // discard that excess, so wait for normal extraction before changing the structure.
            if (tankInventory.getFluidAmount() > tankInventory.getCapacity()) return;
            if (width > builder().maximumWidth() || height > builder().maximumLength(width)) {
                if (migrationRetryTicks > 0) {
                    migrationRetryTicks--;
                    return;
                }
                migrationRetryTicks = 20;
                if (!savedMembersAvailable()) return;
                // The native splitter must still see the complete original saved footprint.
                migratingSavedGeometry = true;
                try {
                    ConnectivityHandler.splitMulti(this);
                } finally {
                    migratingSavedGeometry = false;
                }
            }
        }
        super.updateConnectivity();
    }

    /** Pending saved tanks cannot participate in a neighboring native rebuild. */
    public boolean connectivityMigrationPending() {
        return !migratingSavedGeometry && (width > builder().maximumWidth() || height > builder().maximumLength(width)
                || tankInventory.getFluidAmount() > StorageStructureLimits.boundedCapacity(
                        builder().capacityPerBlock(), width, height));
    }

    private boolean savedMembersAvailable() {
        int members = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    BlockPos pos = memberPos(y, x, z);
                    if (!level.hasChunkAt(pos)) return false;
                    FluidTankBlockEntity part = ConnectivityHandler.partAt(getType(), level, pos);
                    // A block can become available before its block entity has finished loading.
                    if (part == null) {
                        if (level.getBlockState(pos).is(getBlockState().getBlock())) return false;
                        continue;
                    }
                    if (part.getController().equals(worldPosition)) members++;
                }
            }
        }
        // Missing or replaced blocks must not cause the splitter to discard their share of fluid.
        return (long) members * builder().capacityPerBlock() >= tankInventory.getFluidAmount();
    }

    @Override
    public FluidTankBlockEntity getControllerBE() {
        FluidTankBlockEntity tank = super.getControllerBE();
        return tank != null && tank.getType() == getType() ? tank : null;
    }

    @Override
    public FluidTankBlockEntity getOtherFluidTankBlockEntity(Direction direction) {
        FluidTankBlockEntity tank = super.getOtherFluidTankBlockEntity(direction);
        return tank != null && tank.getType() == getType() ? tank : null;
    }

    /** The independent, persisted selection of all six exterior multiblock faces. */
    public FluidWindowFaces windowFaces() { return windowFaces; }

    /** Immutable panel records in Direction.get3DDataValue() order; no world lookup on render threads. */
    public FluidTankFaceLayout.Panel[] facePanels() { return facePanels.clone(); }

    private void resolveWindows() {
        if (windowFaces == null) windowFaces = builder().defaultWindowFaces(getMainConnectionAxis());
        if (!builder().windowsAllowed()) windowFaces = FluidWindowFaces.filled(FluidWindowMode.NONE);
        else for (Direction face : Direction.values()) {
            FluidWindowMode mode = windowFaces.get(face);
            // NONE remains valid for noDefaultWindow and old worlds even when omitted from the wrench cycle.
            if (mode != FluidWindowMode.NONE && !builder().allowedWindowStyles().contains(mode))
                windowFaces = windowFaces.with(face, builder().allowedWindowStyles().get(0));
        }
        // Only the controller owns boiler state. Members retain its effective visibility
        // snapshot, including on clients where the controller chunk has not arrived yet.
        if (isController()) windowsSuppressed = boiler.isActive();
        window = builder().windowsAllowed() && !windowsSuppressed && windowFaces.anyGlass();
    }

    /** Shared native toggle by default; independently edit the clicked face when opted in. */
    public void cycleWindowFace(Direction face) {
        if (face == null || hasLevel() && level.isClientSide) return;
        FluidTankBlockEntity controller = getControllerBE();
        if (!(controller instanceof ScriptedFluidTankBlockEntity tank)
                || !tank.builder().windowsAllowed() || tank.boiler.isActive()) return;
        if (!tank.builder().independentWindowsEnabled()) {
            tank.setWindows(!tank.window);
            return;
        }
        var modes = tank.builder().allowedWindowStyles();
        int current = modes.indexOf(tank.windowFaces.get(face));
        tank.windowFaces = tank.windowFaces.with(face, modes.get((current + 1) % modes.size()));
        tank.refreshWindows();
    }

    /** Native compatibility entry point toggles the complete configured layout. */
    @Override public void toggleWindows() {
        setWindows(!window);
    }

    @Override public void setWindows(boolean requested) {
        if (hasLevel() && !isController() && getControllerBE() instanceof ScriptedFluidTankBlockEntity tank) {
            tank.setWindows(requested);
            return;
        }
        // Create hides boiler windows through this hook; preserve the user's six choices for later.
        if (!boiler.isActive()) windowFaces = requested ? builder().enabledWindowFaces(getMainConnectionAxis())
                : FluidWindowFaces.filled(FluidWindowMode.NONE);
        refreshWindows();
    }

    @Override public void transform(net.minecraft.world.level.block.entity.BlockEntity entity,
            com.simibubi.create.content.contraptions.StructureTransform transform) {
        transformWindowFaces(transform);
    }

    public void transformWindowFaces(com.simibubi.create.content.contraptions.StructureTransform transform) {
        FluidWindowFaces changed = FluidWindowFaces.filled(FluidWindowMode.NONE);
        for (Direction face : Direction.values())
            changed = changed.with(transform.rotateFacing(transform.mirrorFacing(face)), windowFaces.get(face));
        windowFaces = changed;
        resolveWindows();
        rebuildFacePanels();
        setChanged();
    }

    private void refreshWindows() {
        resolveWindows();
        if (hasLevel()) {
            for (int length = 0; length < height; length++) for (int a = 0; a < width; a++) for (int b = 0; b < width; b++) {
                BlockPos pos = memberPos(length, a, b);
                if (!level.hasChunkAt(pos)) continue;
                FluidTankBlockEntity part = ConnectivityHandler.partAt(getType(), level, pos);
                if (part instanceof ScriptedFluidTankBlockEntity tank
                        && tank.getController().equals(worldPosition)
                        && tank.getBlockState().is(getBlockState().getBlock())
                        && tank.getMainConnectionAxis() == getMainConnectionAxis()) applyMemberWindow(tank);
            }
        } else applyMemberWindow(this);
    }

    private void applyMemberWindow(ScriptedFluidTankBlockEntity member) {
        WindowSnapshot next = new WindowSnapshot(windowFaces, width, height, window, windowsSuppressed);
        boolean snapshotChanged = !next.equals(member.windowSnapshot);
        member.windowFaces = windowFaces;
        member.windowsSuppressed = windowsSuppressed;
        member.width = width;
        member.height = height;
        member.window = window;
        BlockState state = member.getBlockState();
        BlockState updated = state.setValue(FluidTankBlock.SHAPE,
                window ? FluidTankBlock.Shape.WINDOW : FluidTankBlock.Shape.PLAIN);
        if (state != updated && hasLevel()) level.setBlock(member.worldPosition, updated, 22);
        boolean changed = member.rebuildFacePanels();
        if (changed && hasLevel()) level.getChunkSource().getLightEngine().checkBlock(member.worldPosition);
        member.windowSnapshot = next;
        // Every member persists the complete preference snapshot, even when its visible panels
        // did not change. Keep one immediate packet per changed member, without a second root packet.
        if (changed || snapshotChanged || state != updated) {
            member.setChanged();
            member.sendDataImmediately();
        }
    }

    private boolean rebuildFacePanels() {
        BlockPos relative = worldPosition.subtract(getController());
        Direction.Axis axis = getMainConnectionAxis();
        PanelInput input = new PanelInput(axis, width, height, relative, window, windowFaces);
        if (input.equals(panelInput)) return false;
        PanelInput previous = panelInput;
        panelInput = input;
        if (previous != null && previous.axis == axis && previous.width == width && previous.height == height
                && previous.relative.equals(relative) && previous.visible == window) {
            boolean localFaceChanged = false;
            if (window) for (Direction face : Direction.values()) {
                int coordinate = face.getAxis().choose(relative.getX(), relative.getY(), relative.getZ());
                int edge = face.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 0 : extent(face.getAxis()) - 1;
                if (coordinate == edge && previous.faces.get(face) != windowFaces.get(face)) {
                    localFaceChanged = true;
                    break;
                }
            }
            // Full preferences still synchronize to every member; only local exterior faces
            // contribute geometry. Interior/remote choices need no replacement panel array.
            if (!localFaceChanged) return false;
        }
        FluidWindowFaces visible = window ? windowFaces : FluidWindowFaces.filled(FluidWindowMode.NONE);
        boolean valid = relative.getX() >= 0 && relative.getX() < extent(Direction.Axis.X)
                && relative.getY() >= 0 && relative.getY() < extent(Direction.Axis.Y)
                && relative.getZ() >= 0 && relative.getZ() < extent(Direction.Axis.Z);
        // Old member saves do not include geometry. A moved/splitting member can also briefly
        // retain its previous controller position. Render an opaque single shell until the
        // loaded controller supplies a consistent snapshot; never crash the chunk load.
        FluidTankFaceLayout.Panel[] next = valid
                ? FluidTankFaceLayout.panels(axis, width, height, relative.getX(), relative.getY(), relative.getZ(), visible)
                : FluidTankFaceLayout.panels(axis, 1, 1, 0, 0, 0, FluidWindowFaces.filled(FluidWindowMode.NONE));
        if (java.util.Arrays.equals(facePanels, next)) return false;
        facePanels = next;
        if (hasLevel() && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 16);
        }
        return true;
    }


    @Override
    public Direction.Axis getMainConnectionAxis() {
        return builder().isVessel() ? getBlockState().getValue(ScriptedFluidVesselBlock.AXIS) : Direction.Axis.Y;
    }

    public BlockPos memberPos(int length, int a, int b) {
        return worldPosition.offset(FluidTankGeometry.offset(getMainConnectionAxis(), length, a, b));
    }

    public int extent(Direction.Axis dimension) {
        return FluidTankGeometry.extent(getMainConnectionAxis(), dimension, width, height);
    }

    public boolean hasWindow() { return window; }

    @Override
    protected net.minecraft.world.phys.AABB createRenderBoundingBox() {
        if (getMainConnectionAxis() == Direction.Axis.Y) return super.createRenderBoundingBox();
        return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(isController() ? extent(Direction.Axis.X) - 1 : 0,
                isController() ? extent(Direction.Axis.Y) - 1 : 0, isController() ? extent(Direction.Axis.Z) - 1 : 0);
    }

    private void invalidateFluidEffects() {
        previousFluidSignal = previousFluidLight = -1;
    }

    private static int fluidLight(FluidStack fluid) {
        return fluid.isEmpty() ? 0 : (int) (fluid.getFluid().getFluidType().getLightLevel(fluid) / 1.2f);
    }

    private int litHeight() {
        return (int) (Math.min(1, getFillState()) * width + 1);
    }

    private static int layerLight(int light, int litHeight, boolean gas, int y, int width) {
        boolean bright = gas ? width - y <= litHeight : y < litHeight;
        return bright ? light : light > 0 ? 1 : 0;
    }

    /** A member loaded after the latest cached update still needs the controller's current light. */
    private void updateMemberFluidLight(ScriptedFluidTankBlockEntity member) {
        if (!hasLevel() || level.isClientSide || getMainConnectionAxis() == Direction.Axis.Y) return;
        FluidStack fluid = tankInventory.getFluid();
        member.setLuminosity(layerLight(fluidLight(fluid), litHeight(),
                !fluid.isEmpty() && fluid.getFluid().getFluidType().isLighterThanAir(),
                member.worldPosition.getY() - worldPosition.getY(), width));
    }

    @Override
    protected void onFluidStackChanged(FluidStack fluid) {
        if (getMainConnectionAxis() == Direction.Axis.Y) { super.onFluidStackChanged(fluid); return; }
        if (!hasLevel()) return;
        if (!level.isClientSide) {
            int signal = com.simibubi.create.foundation.blockEntity.ComparatorUtil.fractionToRedstoneLevel(getFillState());
            if (signal != previousFluidSignal) {
                previousFluidSignal = signal;
                StorageComparatorUpdates.notifySurface(level, worldPosition,
                        extent(Direction.Axis.X), extent(Direction.Axis.Y), extent(Direction.Axis.Z));
            }
            int light = fluidLight(fluid);
            int litHeight = light == 0 ? 0 : litHeight();
            boolean gas = light != 0 && fluid.getFluid().getFluidType().isLighterThanAir();
            if (light != previousFluidLight || litHeight != previousLitHeight || gas != previousGas) {
                int sx = extent(Direction.Axis.X), sz = extent(Direction.Axis.Z);
                for (int y = 0; y < width; y++) {
                    int actual = layerLight(light, litHeight, gas, y, width);
                    if (previousFluidLight >= 0 && actual == layerLight(previousFluidLight, previousLitHeight, previousGas, y, width)) continue;
                    for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) {
                        BlockPos pos = worldPosition.offset(x, y, z);
                        if (!level.hasChunkAt(pos)) continue;
                        FluidTankBlockEntity part = ConnectivityHandler.partAt(getType(), level, pos);
                        if (part instanceof ScriptedFluidTankBlockEntity tank && tank.getController().equals(getController()))
                            tank.setLuminosity(actual);
                    }
                }
                previousFluidLight = light;
                previousLitHeight = litHeight;
                previousGas = gas;
            }
            setChanged();
            sendData();
        }
        if (isVirtual()) {
            if (getFluidLevel() == null) setFluidLevel(net.createmod.catnip.animation.LerpedFloat.linear().startWithValue(getFillState()));
            getFluidLevel().chase(getFillState(), .5, net.createmod.catnip.animation.LerpedFloat.Chaser.EXP);
        }
    }

    @Override
    public void notifyMultiUpdated() {
        invalidateFluidEffects();
        BlockState state = getBlockState();
        int coordinate = getMainConnectionAxis().choose(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        BlockPos origin = getController();
        int start = getMainConnectionAxis().choose(origin.getX(), origin.getY(), origin.getZ());
        level.setBlock(worldPosition, state.setValue(FluidTankBlock.BOTTOM, coordinate == start)
                .setValue(FluidTankBlock.TOP, coordinate == start + height - 1), 6);
        // The native implementation calls setWindows(window), which would flatten six independent choices.
        if (isController()) refreshWindows();
        onFluidStackChanged(tankInventory.getFluid());
        updateBoilerState();
        setChanged();
    }

    @Override
    public void updateBoilerState() {
        if (getMainConnectionAxis() == Direction.Axis.Y) {
            boolean active = boiler.isActive();
            super.updateBoilerState();
            if (active != boiler.isActive()) refreshWindows();
            return;
        }
        if (!isController()) return;
        boolean before = boiler.isActive();
        boolean changed = boiler.evaluate(this);
        if (before != boiler.isActive()) {
            refreshWindows();
            for (int length = 0; length < height; length++) for (int a = 0; a < width; a++) for (int b = 0; b < width; b++) {
                FluidTankBlockEntity part = ConnectivityHandler.partAt(getType(), level, memberPos(length, a, b));
                if (part instanceof ScriptedFluidTankBlockEntity tank && tank.getController().equals(worldPosition)) {
                    tank.fluidCapability.invalidate();
                    tank.updateCapability = true;
                }
            }
        }
        if (changed) { notifyUpdate(); boiler.checkPipeOrganAdvancement(this); }
    }

    @Override public void removeController(boolean keepFluids) {
        invalidateFluidEffects();
        if (!readingStorage) StorageEventDispatcher.structureChanging(this);
        super.removeController(keepFluids);
        if (!level.isClientSide) {
            // Native splitting calls removeController(true) before filling each destination.
            // Refresh here too: config reload/removal can precede a member's next tick,
            // including a member whose saved block entity has just become available.
            // setCapacity deliberately keeps any existing overflow drainable.
            tankInventory.setCapacity(builder().capacityPerBlock());
            refreshWindows();
        }
    }

    @Override public Object getExtraData() {
        return new FluidVesselWindowData(windowFaces);
    }

    @Override public void setExtraData(Object data) {
        if (data instanceof FluidVesselWindowData selection) windowFaces = selection.faces();
        else if (data instanceof Boolean enabled) windowFaces = enabled ? builder().defaultWindowFaces(getMainConnectionAxis())
                : FluidWindowFaces.filled(FluidWindowMode.NONE);
        resolveWindows();
        rebuildFacePanels();
    }

    @Override public Object modifyExtraData(Object data) {
        // Preserve the first selected multiblock's complete preference, as native Create merges its window flag.
        if (data instanceof FluidVesselWindowData previous)
            return previous.enabled() || !windowFaces.anyGlass() ? previous : getExtraData();
        return Boolean.TRUE.equals(data) ? new FluidVesselWindowData(builder().defaultWindowFaces(getMainConnectionAxis())) : getExtraData();
    }


    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        FluidWindowFaces savedFaces = readWindowFaces(tag);
        builder().validateConfiguration();
        int savedWidth = Math.max(tag.getInt("Size"), 1);
        int savedHeight = Math.max(tag.getInt("Height"), 1);
        // Saved geometry can outlive a capacity increase; do not reject the world before
        // the loaded structure can safely split under the new integer storage bound.
        long maximumBlocks = Integer.MAX_VALUE;
        if (maximumBlocks / savedWidth / savedWidth < savedHeight)
            throw new IllegalArgumentException("Saved fluid tank dimensions exceed supported capacity at " + worldPosition);
        boolean savedWindow = builder().windowsAllowed()
                && (tag.contains("Window") ? tag.getBoolean("Window") : builder().defaultWindows());
        CompoundTag input = tag;
        if (savedWidth != tag.getInt("Size") || savedHeight != tag.getInt("Height")
                || !tag.contains("Window") || savedWindow != tag.getBoolean("Window")) {
            input = tag.copy();
            input.putInt("Size", savedWidth);
            input.putInt("Height", savedHeight);
            input.putBoolean("Window", savedWindow);
        }
        // The scoped native read mixin supplies this registration's capacity while retaining
        // Create's controller, interpolation, light and render-update behavior.
        syncedReadCapacity = clientPacket && tag.contains("CBJSCapacity")
                ? Math.max(1, tag.getInt("CBJSCapacity")) : 0;
        readingStorage = true;
        try {
            super.read(input, clientPacket);
        } finally {
            readingStorage = false;
            syncedReadCapacity = 0;
        }
        windowFaces = savedFaces;
        windowsSuppressed = tag.getBoolean("WindowSuppressed");
        if (!isController()) {
            width = Math.max(1, tag.getInt("WindowWidth"));
            height = Math.max(1, tag.getInt("WindowLength"));
        }
        resolveWindows();
        rebuildFacePanels();
        migrationRetryTicks = 0;
        invalidateFluidEffects();
        configuredMaxLength = builder().maximumLength(width);
        if (!clientPacket && isController()
                && (width > builder().maximumWidth() || height > builder().maximumLength(width)
                        || tankInventory.getFluidAmount() > tankInventory.getCapacity()))
            updateConnectivity = true;
    }

    @Override public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        writeWindowSelection(tag);
        if (clientPacket) tag.putInt("CBJSCapacity", tankInventory.getCapacity());
    }

    @Override public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        writeWindowSelection(tag);
    }

    private void writeWindowSelection(CompoundTag tag) {
        CompoundTag faces = new CompoundTag();
        for (Direction face : Direction.values())
            faces.putString(face.getName(), windowFaces.get(face).getSerializedName());
        tag.put(FACES_TAG, faces);
        // Native Create only saves dimensions on the controller. Members need a render snapshot
        // even when the controller's chunk has not arrived on the client yet.
        tag.putInt("WindowWidth", width);
        tag.putInt("WindowLength", height);
        tag.putBoolean("WindowSuppressed", windowsSuppressed);
    }

    private FluidWindowFaces readWindowFaces(CompoundTag tag) {
        FluidWindowFaces result = builder().defaultWindowFaces(getMainConnectionAxis());
        if (tag.contains(FACES_TAG)) {
            CompoundTag faces = tag.getCompound(FACES_TAG);
            for (Direction face : Direction.values()) if (faces.contains(face.getName())) {
                try { result = result.with(face, FluidWindowMode.parse(faces.getString(face.getName()))); }
                catch (IllegalArgumentException ignored) { }
            }
            return result;
        }
        boolean enabled = tag.contains("VesselWindowEnabled") ? tag.getBoolean("VesselWindowEnabled")
                : tag.contains("Window") ? tag.getBoolean("Window") : builder().defaultWindows();
        if (!enabled) return FluidWindowFaces.filled(FluidWindowMode.NONE);
        if (!tag.contains("VesselWindowStyle")) return result;
        String legacy = tag.getString("VesselWindowStyle");
        result = FluidWindowFaces.filled(FluidWindowMode.NONE);
        if (legacy.startsWith("top_")) return result.with(Direction.UP,
                legacy.equals("top_strip") ? FluidWindowMode.STRIP : FluidWindowMode.FULL);
        FluidWindowMode mode = legacy.equals("side_wide") ? FluidWindowMode.FULL : FluidWindowMode.STRIP;
        for (Direction face : Direction.Plane.HORIZONTAL) result = result.with(face, mode);
        return result;
    }

    /** External intake is filtered; native multiblock redistribution preserves saved contents. */
    public static final class Inventory extends SmartFluidTank {
        private final Predicate<FluidStack> filter;
        private final ScriptedFluidTankBlockEntity owner;
        private boolean redistributing;
        private int mutationDepth;

        public Inventory(int capacity, Consumer<FluidStack> callback, Predicate<FluidStack> filter) {
            this(capacity, callback, filter, null);
        }

        private Inventory(int capacity, Consumer<FluidStack> callback, Predicate<FluidStack> filter,
                          ScriptedFluidTankBlockEntity owner) {
            super(capacity, callback);
            this.filter = filter;
            this.owner = owner;
        }

        @Override public Inventory setCapacity(int capacity) {
            if (owner != null && owner.readingStorage)
                capacity = owner.syncedReadCapacity > 0 ? owner.syncedReadCapacity
                        : StorageStructureLimits.boundedCapacity(owner.builder().capacityPerBlock(), owner.width, owner.height);
            super.setCapacity(capacity);
            return this;
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return redistributing || filter.test(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // Forge assumes amount <= capacity and otherwise returns a negative fill, destroying
            // saved overflow on execution. Capacity reductions must retain that fluid for draining.
            if (getFluidAmount() >= getCapacity()) return 0;
            FluidStack previous = beginChange(action);
            try {
                return super.fill(resource, action);
            } finally {
                endChange(previous);
            }
        }

        @Override public FluidStack drain(int amount, FluidAction action) {
            FluidStack previous = beginChange(action);
            try {
                return super.drain(amount, action);
            } finally {
                endChange(previous);
            }
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack previous = beginChange(action);
            try {
                return super.drain(resource, action);
            } finally {
                endChange(previous);
            }
        }

        @Override public void setFluid(FluidStack fluid) {
            FluidStack previous = beginChange(FluidAction.EXECUTE);
            try {
                super.setFluid(fluid);
            } finally {
                endChange(previous);
            }
        }

        private FluidStack beginChange(FluidAction action) {
            boolean outer = mutationDepth++ == 0;
            return outer && action.execute() && !redistributing && owner != null && !owner.readingStorage
                    && StorageEventDispatcher.needsFluidSnapshot(owner) ? getFluid().copy() : null;
        }

        private void endChange(FluidStack previous) {
            mutationDepth--;
            if (previous != null) StorageEventDispatcher.fluidChanged(owner, previous);
        }

        public int fillFromMultiblock(FluidStack resource, FluidAction action) {
            boolean previous = redistributing;
            redistributing = true;
            try {
                return fill(resource, action);
            } finally {
                redistributing = previous;
            }
        }
    }

    private final class ScriptedBoilerData extends BoilerData {
        @Override
        public boolean evaluate(FluidTankBlockEntity controller) {
            if (builder().steamEngineAllowed()) {
                if (getMainConnectionAxis() == Direction.Axis.Y) return super.evaluate(controller);
                int engines = attachedEngines, whistles = attachedWhistles;
                attachedEngines = attachedWhistles = 0;
                forEachAttachment((pos, state) -> {
                    if (com.simibubi.create.AllBlocks.STEAM_ENGINE.has(state)) attachedEngines++;
                    if (com.simibubi.create.AllBlocks.STEAM_WHISTLE.has(state)) attachedWhistles++;
                });
                needsHeatLevelUpdate = true;
                return engines != attachedEngines || whistles != attachedWhistles;
            }
            boolean active = isActive();
            clear();
            return active;
        }

        private void forEachAttachment(java.util.function.BiConsumer<BlockPos, BlockState> visitor) {
            if (!hasLevel()) return;
            for (int length = 0; length < height; length++) for (int a = 0; a < width; a++) for (int b = 0; b < width; b++) {
                BlockPos pos = memberPos(length, a, b);
                FluidTankBlockEntity member = ConnectivityHandler.partAt(getType(), level, pos);
                if (member == null || !member.getController().equals(worldPosition)) continue;
                for (Direction direction : Direction.values()) {
                    BlockPos attached = pos.relative(direction);
                    BlockState state = level.getBlockState(attached);
                    if (com.simibubi.create.AllBlocks.STEAM_ENGINE.has(state)
                            && com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock.getFacing(state) == direction
                            || com.simibubi.create.AllBlocks.STEAM_WHISTLE.has(state)
                            && com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.getAttachedDirection(state).getOpposite() == direction)
                        visitor.accept(attached, state);
                }
            }
        }

        @Override
        public boolean updateTemperature(FluidTankBlockEntity controller) {
            if (getMainConnectionAxis() == Direction.Axis.Y) return super.updateTemperature(controller);
            needsHeatLevelUpdate = false;
            int previousHeat = activeHeat; boolean previousPassive = passiveHeat;
            activeHeat = 0; passiveHeat = false;
            for (int x = 0; x < extent(Direction.Axis.X); x++) for (int z = 0; z < extent(Direction.Axis.Z); z++) {
                BlockPos pos = worldPosition.offset(x, -1, z);
                float heat = com.simibubi.create.api.boiler.BoilerHeater.findHeat(level, pos, level.getBlockState(pos));
                if (heat == 0) passiveHeat = true;
                else if (heat > 0) activeHeat += heat;
            }
            passiveHeat &= activeHeat == 0;
            return previousHeat != activeHeat || previousPassive != passiveHeat;
        }

        @Override
        public void checkPipeOrganAdvancement(FluidTankBlockEntity controller) {
            if (getMainConnectionAxis() == Direction.Axis.Y) { super.checkPipeOrganAdvancement(controller); return; }
            var owner = controller.getBehaviour(com.simibubi.create.foundation.advancement.AdvancementBehaviour.TYPE);
            if (owner == null || !owner.isOwnerPresent()) return;
            java.util.Set<Integer> pitches = new java.util.HashSet<>();
            forEachAttachment((pos, state) -> {
                if (level.getBlockEntity(pos) instanceof com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity whistle)
                    pitches.add(whistle.getPitchId());
            });
            if (pitches.size() >= 12) controller.award(com.simibubi.create.foundation.advancement.AllAdvancements.PIPE_ORGAN);
        }

        @Override
        public void updateOcclusion(FluidTankBlockEntity controller) {
            if (getMainConnectionAxis() == Direction.Axis.Y) { super.updateOcclusion(controller); return; }
            if (!level.isClientSide || !isActive()) return;
            for (Direction direction : net.createmod.catnip.data.Iterate.horizontalDirections) {
                double x = extent(Direction.Axis.X) / 2.0, z = extent(Direction.Axis.Z) / 2.0;
                double radius = extent(direction.getAxis()) / 2.0 + .25;
                var box = new net.minecraft.world.phys.AABB(worldPosition)
                        .move(x - .5, width / 2.0 - .5, z - .5).deflate(5 / 8.0)
                        .move(direction.getStepX() * radius, 0, direction.getStepZ() * radius)
                        .inflate(Math.abs(direction.getStepZ()) / 2.0, .25, Math.abs(direction.getStepX()) / 2.0);
                occludedDirections[direction.get2DDataValue()] = !level.noCollision(box);
            }
        }

        @Override
        public void read(CompoundTag tag, int boilerSize) {
            if (builder().steamEngineAllowed()) super.read(tag, boilerSize);
            else clear();
        }

        @Override
        public void clear() {
            super.clear();
            // Create clears engines but leaves the whistle count behind.
            attachedWhistles = 0;
        }

        @Override
        public BoilerFluidHandler createHandler() {
            return new BoilerFluidHandler() {
                @Override
                public boolean isFluidValid(int tank, FluidStack stack) {
                    return builder().steamEngineAllowed() && builder().acceptsFluid(stack)
                            && super.isFluidValid(tank, stack);
                }
            };
        }
    }
}
