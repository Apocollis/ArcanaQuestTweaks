package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EscortReleaseTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    @Test
    void anEntityWithoutTheTagIsNotAnEscort() {
        assertEquals(EscortRelease.Verdict.NOT_ESCORT, EscortRelease.verdict(null, new HashMap<UUID, Long>()));
    }

    @Test
    void anEscortOfALivingOrUnloadedOwnerIsDenied() {
        Map<UUID, Long> released = new HashMap<>();
        assertEquals(EscortRelease.Verdict.DENY, EscortRelease.verdict(OWNER.toString(), released));
        released.put(OTHER, 100L);
        assertEquals(EscortRelease.Verdict.DENY, EscortRelease.verdict(OWNER.toString(), released),
                "another mage's death does not release this escort");
        assertEquals(EscortRelease.Verdict.DENY, EscortRelease.verdict(OWNER.toString(), null));
    }

    @Test
    void anEscortOfADeadOwnerIsReleased() {
        Map<UUID, Long> released = new HashMap<>();
        released.put(OWNER, 500L);
        assertEquals(EscortRelease.Verdict.RELEASE, EscortRelease.verdict(OWNER.toString(), released));
    }

    @Test
    void anUnreadableTagIsReleasedSoItCannotLingerForever() {
        assertEquals(EscortRelease.Verdict.RELEASE, EscortRelease.verdict("not-a-uuid", new HashMap<UUID, Long>()));
        assertEquals(EscortRelease.Verdict.RELEASE, EscortRelease.verdict("", new HashMap<UUID, Long>()));
        assertNull(EscortRelease.parseOwner("not-a-uuid"));
        assertNull(EscortRelease.parseOwner(null));
        assertEquals(OWNER, EscortRelease.parseOwner(OWNER.toString()));
    }

    @Test
    void oldReleasedEntriesAreTrimmedAndNewOnesKept() {
        Map<UUID, Long> released = new HashMap<>();
        released.put(OWNER, 1000L);
        released.put(OTHER, 1000L + EscortRelease.MAX_AGE_TICKS);
        long now = 1000L + EscortRelease.MAX_AGE_TICKS + 5L;
        assertEquals(1, EscortRelease.trim(released, now, EscortRelease.MAX_AGE_TICKS));
        assertFalse(released.containsKey(OWNER));
        assertTrue(released.containsKey(OTHER));
        assertEquals(0, EscortRelease.trim(released, now, EscortRelease.MAX_AGE_TICKS));
    }

    @Test
    void onlyTheFourBattlemageEntitiesNeverDespawn() {
        assertTrue(EscortRelease.isBattlemagePath("aqtweaks", "cultist_battlemage"));
        assertTrue(EscortRelease.isBattlemagePath("aqtweaks", "cultist_master_battlemage"));
        assertTrue(EscortRelease.isBattlemagePath("aqtweaks", "cultist_swarm_mage"));
        assertTrue(EscortRelease.isBattlemagePath("aqtweaks", "cultist_bombardier_mage"));
        assertFalse(EscortRelease.isBattlemagePath("aqtweaks", "escort_portal"));
        assertFalse(EscortRelease.isBattlemagePath("thaumcraft", "cultist_battlemage"));
        assertFalse(EscortRelease.isBattlemagePath("aqtweaks", null));
    }
}
