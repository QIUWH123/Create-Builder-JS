package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.quiwh.cbjs.content.ScriptedFluidTankBlockEntity;
import net.quiwh.cbjs.content.ScriptedItemVaultBlockEntity;

/** Controller dimensions and fluid contents available on the client, without inventory scanning. */
public final class GogglesStorageInfo {
    private final String type, axis;
    private final BlockPos controllerPos;
    private final int width, length;
    private final long capacity;
    private final FluidStack fluid;
    private GogglesStorageInfo(String type, Direction.Axis axis, BlockPos controllerPos, int width, int length, long capacity, FluidStack fluid) {
        this.type = type; this.axis = axis.getName(); this.controllerPos = controllerPos.immutable();
        this.width = width; this.length = length; this.capacity = capacity;
        this.fluid = fluid == null ? null : fluid.copy();
    }
    @dev.latvian.mods.rhino.util.HideFromJS
    public static GogglesStorageInfo of(BlockEntity entity) {
        if (entity instanceof FluidTankBlockEntity part) {
            FluidTankBlockEntity tank = part.isController() ? part : part.getControllerBE();
            if (tank == null) return null;
            Direction.Axis axis = tank instanceof ScriptedFluidTankBlockEntity scripted ? scripted.getMainConnectionAxis() : Direction.Axis.Y;
            return new GogglesStorageInfo(axis == Direction.Axis.Y ? "fluid_tank" : "fluid_vessel", axis, tank.getBlockPos(),
                    tank.getWidth(), tank.getHeight(), tank.getTankInventory().getCapacity(), tank.getTankInventory().getFluid());
        }
        if (entity instanceof ItemVaultBlockEntity part) {
            ItemVaultBlockEntity vault = part.isController() ? part : part.getControllerBE();
            if (vault == null) return null;
            Direction.Axis axis = vault.getMainConnectionAxis();
            long blocks = (long) vault.getWidth() * vault.getWidth() * vault.getHeight();
            int slots = vault instanceof ScriptedItemVaultBlockEntity scripted
                    ? scripted.builder().capacityPerBlock() : vault.getInventoryOfBlock().getSlots();
            return new GogglesStorageInfo(axis == Direction.Axis.Y ? "item_silo" : "item_vault", axis, vault.getBlockPos(),
                    vault.getWidth(), vault.getHeight(), blocks * slots, null);
        }
        return null;
    }
    public String getType() { return type; }
    public String getAxis() { return axis; }
    public BlockPos getControllerPos() { return controllerPos; }
    public int getWidth() { return width; }
    public int getLength() { return length; }
    public long getBlocks() { return (long) width * width * length; }
    /** Fluid capacity in mB or item capacity in slots. */
    public long getCapacity() { return capacity; }
    public FluidStack getFluid() { return fluid == null ? null : fluid.copy(); }
    public Integer getAmount() { return fluid == null ? null : fluid.getAmount(); }
    /** Vault contents are intentionally not synchronized by Create. */
    public boolean getInventoryAvailable() { return false; }
}
