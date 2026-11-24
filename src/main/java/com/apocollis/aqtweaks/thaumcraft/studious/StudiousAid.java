package com.apocollis.aqtweaks.thaumcraft.studious;

import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/**
 * A virtual research aid: it is never found next to a table. {@link StudiousModule#withAid} adds its key to a
 * Scholar's theory. Thaumcraft keys aids by class name; {@code checkSurroundingAids} only compares a block or an
 * item stack with the aid object, so the string placeholder here can never match anything.
 */
public final class StudiousAid implements ITheorycraftAid {

    public static final String KEY = StudiousAid.class.getName();

    @Override
    public Object getAidObject() {
        return "aqtweaks:studious_aid";
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class<TheorycraftCard>[] getCards() {
        return new Class[] {CardEureka.class, CardCrossReference.class, CardSecondOpinion.class};
    }
}
