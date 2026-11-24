package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The pure decisions of the "never despawn" rule (plan 21). Battlemages never despawn. An escort carries an
 * {@link #TAG} entity-data string holding its owner mage's UUID and is denied despawning while that owner is alive (or
 * simply unloaded); once the owner has died, its UUID is in the released set and the escort despawns like any mob.
 * No Minecraft types here, unit tested.
 */
public final class EscortRelease {

    /** Entity-data key on an escort; the value is the owner mage's UUID as a string. */
    public static final String TAG = "aqtweaks_escort";
    /** Released owner ids older than this many world ticks are forgotten (about 5 in-game days). */
    public static final long MAX_AGE_TICKS = 120000L;

    /** What the despawn guard does with an escort. */
    public enum Verdict {
        /** Not an escort (no tag): leave vanilla alone. */
        NOT_ESCORT,
        /** Owner alive or unknown: block the despawn. */
        DENY,
        /** Owner is dead: drop the tag and let vanilla despawn it. */
        RELEASE
    }

    private EscortRelease() {}

    /** @return the owner UUID, or null for a missing or malformed value */
    public static UUID parseOwner(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @param tagValue the escort tag value, null when the entity has no tag
     * @param released owner UUID to the world tick at which it was released (the owner died)
     */
    public static Verdict verdict(String tagValue, Map<UUID, Long> released) {
        if (tagValue == null) {
            return Verdict.NOT_ESCORT;
        }
        UUID owner = parseOwner(tagValue);
        if (owner == null) {
            // A tag we cannot read protects nothing useful: treat the escort as released so it cannot linger forever.
            return Verdict.RELEASE;
        }
        return released != null && released.containsKey(owner) ? Verdict.RELEASE : Verdict.DENY;
    }

    /** Removes entries released more than {@code maxAge} ticks before {@code now}; returns how many were dropped. */
    public static int trim(Map<UUID, Long> released, long now, long maxAge) {
        int removed = 0;
        Iterator<Map.Entry<UUID, Long>> it = released.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (now - e.getValue() > maxAge) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    /** True for the four registered battlemage entities (namespace and path), which never despawn. */
    public static boolean isBattlemagePath(String namespace, String path) {
        if (!"aqtweaks".equals(namespace) || path == null) {
            return false;
        }
        return "cultist_battlemage".equals(path) || "cultist_master_battlemage".equals(path)
                || "cultist_swarm_mage".equals(path) || "cultist_bombardier_mage".equals(path);
    }
}
