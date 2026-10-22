package net.quiwh.cbjs.client;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import net.createmod.catnip.data.Couple;

/** Native Create housing models use Create's own connected texture shifts. */
public final class ModSpriteShifts {
    public static EncasedCogCTBehaviour encasedCogwheel(boolean brass, boolean large) {
        var casing = brass ? AllSpriteShifts.BRASS_CASING : AllSpriteShifts.ANDESITE_CASING;
        return large ? new EncasedCogCTBehaviour(casing) : new EncasedCogCTBehaviour(casing,
                Couple.create(brass ? AllSpriteShifts.BRASS_ENCASED_COGWHEEL_SIDE : AllSpriteShifts.ANDESITE_ENCASED_COGWHEEL_SIDE,
                        brass ? AllSpriteShifts.BRASS_ENCASED_COGWHEEL_OTHERSIDE : AllSpriteShifts.ANDESITE_ENCASED_COGWHEEL_OTHERSIDE));
    }

    private ModSpriteShifts() { }
}
