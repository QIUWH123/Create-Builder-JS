package net.quiwh.cbjs.content;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import java.util.Locale;

/** Startup-only visual permissions and independent native burner capabilities. */
public final class BurnerOptions {
    public final boolean generic;
    public boolean haveBlaze = true;
    public boolean haveRods = true;
    public boolean haveFlame = true;
    public boolean haveGoggles = true;
    public boolean haveTrainHat = true;
    public boolean haveLogisticsHat = true;
    public boolean haveParticles = true;
    public boolean haveSound = true;
    public boolean canControlTrain = true;
    public boolean canBeStockKeeper = true;
    public boolean canCaptureBlaze = true;
    public boolean canHeatBoiler = true;
    public boolean canHeatBasin = true;
    public boolean canProcessFan = true;
    public boolean canUseCreativeFuel = true;
    public boolean canBeFedByArm = true;
    private HeatLevel maximumHeat = HeatLevel.SEETHING;

    public BurnerOptions(boolean generic) {
        this.generic = generic;
        if (generic) {
            haveBlaze = false;
            haveRods = false;
            canControlTrain = false;
            canBeStockKeeper = false;
            canCaptureBlaze = false;
        }
    }
    public void maxHeatLevel(String level) {
        if (level == null) throw invalidHeat();
        final HeatLevel selected;
        try { selected = HeatLevel.valueOf(level.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw invalidHeat(); }
        if (selected == HeatLevel.NONE) throw invalidHeat();
        maximumHeat = selected;
    }
    private static IllegalArgumentException invalidHeat() {
        return new IllegalArgumentException("Maximum burner heat must be smouldering, fading, kindled or seething; none is an empty cage without a fuel entity");
    }
    public HeatLevel clamp(HeatLevel heat) { return heat.ordinal() > maximumHeat.ordinal() ? maximumHeat : heat; }
    public HeatLevel nextCreativeLevel(HeatLevel current) {
        HeatLevel next = current.nextActiveLevel();
        if (next == HeatLevel.FADING && maximumHeat.ordinal() > HeatLevel.FADING.ordinal()) next = next.nextActiveLevel();
        return next.ordinal() > maximumHeat.ordinal() ? HeatLevel.SMOULDERING : next;
    }
    public boolean renders(String role) {
        return switch (role) {
            case "blaze", "blaze_active", "blaze_super", "blaze_super_active", "inert" -> haveBlaze;
            case "rods", "rods_2", "super_rods", "super_rods_2" -> haveRods;
            case "flame", "fire", "soul_fire" -> haveFlame;
            case "goggles", "goggles_small" -> haveGoggles;
            case "train_hat" -> haveTrainHat;
            case "logistics_hat" -> haveLogisticsHat;
            default -> true;
        };
    }
}
