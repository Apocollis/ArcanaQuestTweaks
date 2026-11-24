package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which of the mage's live clouds came from which focus (plan 19.6). Thaumcraft's {@code EntityFocusCloud} only knows its
 * owner, not the focus that made it, so the mage attributes clouds by observation: a scan is taken just before a cast, the
 * cast runs, and every owned cloud that was not there before belongs to that cast. A short pending window after the cast
 * catches a cloud that spawns a few ticks later. All the clouds from one cast count as one live cast.
 *
 * <p>Pure: the entity feeds it the uuids of the owned clouds that are alive right now.
 */
public final class CloudLedger {

    private final Set<UUID> seen = new HashSet<>();
    private final Map<String, Set<UUID>> byFocus = new HashMap<>();
    private String pendingFocus;
    private long pendingUntil;

    /** Clouds that appear until {@code until} (entity ticks) are attributed to {@code focusId}. */
    public void expect(String focusId, long until) {
        pendingFocus = focusId;
        pendingUntil = until;
    }

    /**
     * Records the owned clouds alive at {@code now}. Unseen ones are attributed to the pending focus while the window is
     * open (otherwise they are remembered as seen but unattributed); clouds that are gone are forgotten.
     */
    public void observe(Collection<UUID> alive, long now) {
        if (pendingFocus != null && now > pendingUntil) {
            pendingFocus = null;
        }
        for (UUID id : alive) {
            if (seen.add(id) && pendingFocus != null) {
                Set<UUID> set = byFocus.get(pendingFocus);
                if (set == null) {
                    set = new HashSet<>();
                    byFocus.put(pendingFocus, set);
                }
                set.add(id);
            }
        }
        seen.retainAll(alive);
        Iterator<Map.Entry<String, Set<UUID>>> it = byFocus.entrySet().iterator();
        while (it.hasNext()) {
            Set<UUID> set = it.next().getValue();
            set.retainAll(alive);
            if (set.isEmpty()) {
                it.remove();
            }
        }
    }

    /** True while some cloud of this focus's previous cast is still alive. */
    public boolean hasLive(String focusId) {
        Set<UUID> set = byFocus.get(focusId);
        return set != null && !set.isEmpty();
    }

    /** Number of foci that have a live cast (all clouds of one cast count once). */
    public int liveCasts() {
        return byFocus.size();
    }

    /** Clears everything (a fresh fight, a reload). */
    public void clear() {
        seen.clear();
        byFocus.clear();
        pendingFocus = null;
    }
}
