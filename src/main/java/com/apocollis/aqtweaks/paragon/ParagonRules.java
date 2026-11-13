package com.apocollis.aqtweaks.paragon;

/** Pure rules of absorbing and of set perks. No Minecraft types. */
public final class ParagonRules {
    private ParagonRules() {}

    public enum Absorb {
        /** Fresh item: take the trait. */
        ACCEPT,
        /** The item holds another absorbed trait; swap it. */
        REPLACE,
        /** The item already holds this trait; nothing to gain. */
        REJECT_SAME,
        /** Metallurgy's own Paragon is on the item; one Paragon at a time. */
        REJECT_METALLURGY,
        /** The trait does not apply to this kind of item. */
        REJECT_KIND
    }

    public static Absorb absorb(String existingTrait, boolean hasMetallurgyParagon, String incomingTrait,
                                boolean kindAllowed) {
        if (!kindAllowed) return Absorb.REJECT_KIND;
        if (hasMetallurgyParagon) return Absorb.REJECT_METALLURGY;
        if (existingTrait == null || existingTrait.isEmpty()) return Absorb.ACCEPT;
        return existingTrait.equals(incomingTrait) ? Absorb.REJECT_SAME : Absorb.REPLACE;
    }

    /** The trait shared by all four worn pieces, or {@code null} when any slot is empty, plain or different. */
    public static String fullSet(String helmet, String chest, String legs, String boots) {
        if (helmet == null || helmet.isEmpty()) return null;
        return helmet.equals(chest) && helmet.equals(legs) && helmet.equals(boots) ? helmet : null;
    }
}
