package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageSeekPicker.Candidate;
import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageSeekPicker.CloudKind;
import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageSeekPicker.Priority;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattlemageSeekPickerTest {

    private static final List<Candidate> THREE = Arrays.asList(
            new Candidate(0, 4.0D, 20.0F, false),
            new Candidate(1, 25.0D, 5.0F, true),
            new Candidate(2, 9.0D, 12.0F, false));

    @Test
    void attackTargetFirstThenNearest() {
        assertEquals(Arrays.asList(1, 0, 2), BattlemageSeekPicker.order(THREE, Priority.ATTACK_TARGET));
    }

    @Test
    void nearestIgnoresTheAttackTarget() {
        assertEquals(Arrays.asList(0, 2, 1), BattlemageSeekPicker.order(THREE, Priority.NEAREST));
    }

    @Test
    void lowestHealthFirst() {
        assertEquals(Arrays.asList(1, 2, 0), BattlemageSeekPicker.order(THREE, Priority.LOWEST_HEALTH));
    }

    @Test
    void noAttackTargetFallsBackToNearest() {
        List<Candidate> none = Arrays.asList(new Candidate(0, 16.0D, 20.0F, false), new Candidate(1, 4.0D, 20.0F, false));
        assertEquals(1, BattlemageSeekPicker.pickOne(none, Priority.ATTACK_TARGET));
    }

    @Test
    void emptyListPicksNothing() {
        assertEquals(-1, BattlemageSeekPicker.pickOne(Collections.<Candidate>emptyList(), Priority.NEAREST));
        assertTrue(BattlemageSeekPicker.order(Collections.<Candidate>emptyList(), Priority.NEAREST).isEmpty());
    }

    @Test
    void priorityParsingDefaultsToAttackTarget() {
        assertEquals(Priority.NEAREST, Priority.parse("nearest"));
        assertEquals(Priority.LOWEST_HEALTH, Priority.parse(" LOWEST_HEALTH "));
        assertEquals(Priority.ATTACK_TARGET, Priority.parse("nonsense"));
        assertEquals(Priority.ATTACK_TARGET, Priority.parse(null));
    }

    @Test
    void cloudClassification() {
        assertEquals(CloudKind.SUPPORT, BattlemageSeekPicker.classifyCloud(true, false));
        assertEquals(CloudKind.HOSTILE, BattlemageSeekPicker.classifyCloud(false, true));
        assertEquals(CloudKind.HOSTILE, BattlemageSeekPicker.classifyCloud(true, true));
    }

    @Test
    void hostileCloudKeepsOnlyPlayersAndClouds() {
        assertTrue(BattlemageSeekPicker.keepInCloud(CloudKind.HOSTILE, false, true, false, false));
        assertFalse(BattlemageSeekPicker.keepInCloud(CloudKind.HOSTILE, false, false, true, false));
        assertFalse(BattlemageSeekPicker.keepInCloud(CloudKind.HOSTILE, false, false, false, true));
        assertTrue(BattlemageSeekPicker.keepInCloud(CloudKind.HOSTILE, true, false, false, false));
    }

    @Test
    void supportCloudKeepsOwnerAndFriendsNeverPlayers() {
        assertTrue(BattlemageSeekPicker.keepInCloud(CloudKind.SUPPORT, false, false, true, false));
        assertTrue(BattlemageSeekPicker.keepInCloud(CloudKind.SUPPORT, false, false, false, true));
        assertFalse(BattlemageSeekPicker.keepInCloud(CloudKind.SUPPORT, false, true, false, false));
        assertTrue(BattlemageSeekPicker.keepInCloud(CloudKind.SUPPORT, true, false, false, false));
    }
}
