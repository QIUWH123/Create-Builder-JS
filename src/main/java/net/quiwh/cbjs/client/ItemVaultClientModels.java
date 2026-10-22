package net.quiwh.cbjs.client;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.logistics.vault.ItemVaultBlock;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.kubejs.ItemVaultBuilder;
import org.jetbrains.annotations.Nullable;

/** Native rectangular CT atlases, with material-local identity and a vertical silo frame. */
public final class ItemVaultClientModels {
    public static void register(ItemVaultBuilder builder) {
        var behaviour = new Behaviour(builder);
        CreateRegistrate.blockModel(() -> model -> new CTModel(model, behaviour)).accept(builder.get());
    }
    public static final class Behaviour extends ConnectedTextureBehaviour.Base {
        private final ItemVaultBuilder builder;
        private final Map<String, CTSpriteShiftEntry> shifts = new HashMap<>();
        public Behaviour(ItemVaultBuilder builder) {
            this.builder = builder;
            for (String side : new String[]{"front", "side", "top", "bottom"})
                for (String size : new String[]{"medium", "large"})
                    shifts.put(side + "_" + size, CTSpriteShifter.getCT(AllCTTypes.RECTANGLE,
                            ResourceLocation.parse(builder.spritePath("vault_" + side + "_small")),
                            ResourceLocation.parse(builder.spritePath("vault_" + side + "_" + size))));
        }
        @Override public CTSpriteShiftEntry getShift(BlockState state, Direction face, @Nullable TextureAtlasSprite sprite) {
            String side;
            if (builder.vertical()) side = face == Direction.DOWN ? "bottom" : face == Direction.UP ? "front" : "top";
            else {
                Direction.Axis axis = state.getValue(ItemVaultBlock.HORIZONTAL_AXIS);
                side = face.getAxis() == axis ? "front" : face == Direction.UP ? "top" : face == Direction.DOWN ? "bottom" : "side";
            }
            return shifts.get(side + (state.getValue(ItemVaultBlock.LARGE) ? "_large" : "_medium"));
        }
        @Override protected Direction getUpDirection(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
            if (builder.vertical()) return super.getUpDirection(level, pos, state, face);
            Direction.Axis axis = state.getValue(ItemVaultBlock.HORIZONTAL_AXIS);
            boolean x = axis == Direction.Axis.X;
            if (face.getAxis().isVertical() && x) return super.getUpDirection(level, pos, state, face).getClockWise();
            if (face.getAxis() == axis || face.getAxis().isVertical()) return super.getUpDirection(level, pos, state, face);
            return Direction.fromAxisAndDirection(axis, x ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        }
        @Override protected Direction getRightDirection(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
            if (builder.vertical()) return super.getRightDirection(level, pos, state, face);
            Direction.Axis axis = state.getValue(ItemVaultBlock.HORIZONTAL_AXIS);
            if (face.getAxis().isVertical() && axis == Direction.Axis.X)
                return super.getRightDirection(level, pos, state, face).getClockWise();
            if (face.getAxis() == axis || face.getAxis().isVertical()) return super.getRightDirection(level, pos, state, face);
            return Direction.fromAxisAndDirection(Direction.Axis.Y, face.getAxisDirection());
        }
        @Override public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter level,
                BlockPos pos, BlockPos otherPos, Direction face) {
            return state == other && state.is(builder.get()) && ConnectivityHandler.isConnected(level, pos, otherPos);
        }
    }
    private ItemVaultClientModels() { }
}
