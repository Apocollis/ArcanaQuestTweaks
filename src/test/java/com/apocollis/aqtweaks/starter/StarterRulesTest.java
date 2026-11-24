package com.apocollis.aqtweaks.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StarterRulesTest {

    @Test
    void aBrandNewCharacterIsPrompted() {
        assertTrue(StarterRules.shouldPrompt(false, false, false));
    }

    @Test
    void anExistingCharacterWithTheCsgFlagIsNeverPrompted() {
        assertFalse(StarterRules.shouldPrompt(false, false, true));
    }

    @Test
    void aPendingFlagKeepsPromptingEvenAfterCsgSetItsFlag() {
        assertTrue(StarterRules.shouldPrompt(true, false, true));
    }

    @Test
    void aChosenClassNeverPromptsAgain() {
        assertFalse(StarterRules.shouldPrompt(true, true, false));
        assertFalse(StarterRules.shouldPrompt(false, true, true));
    }

    @Test
    void theOpenPacketIsSentAtFortyTicksThenEveryHundred() {
        assertFalse(StarterRules.shouldSendOpen(0));
        assertFalse(StarterRules.shouldSendOpen(39));
        assertTrue(StarterRules.shouldSendOpen(40));
        assertFalse(StarterRules.shouldSendOpen(41));
        assertFalse(StarterRules.shouldSendOpen(139));
        assertTrue(StarterRules.shouldSendOpen(140));
        assertTrue(StarterRules.shouldSendOpen(240));
    }

    @Test
    void theRevealEndsOnTheClientAnswerOrTheTimeout() {
        long deadline = StarterRules.revealDeadline(1000L, 12);
        assertEquals(1000L + 17 * 20, deadline);
        assertFalse(StarterRules.shouldRelease(1100L, deadline, false));
        assertTrue(StarterRules.shouldRelease(1100L, deadline, true));
        assertTrue(StarterRules.shouldRelease(deadline, deadline, false));
        assertTrue(StarterRules.shouldRelease(deadline + 1, deadline, false));
        assertEquals(1000L + 5 * 20, StarterRules.revealDeadline(1000L, -3));
    }

    @Test
    void commandsGetThePlayerNameAndLoseTheLeadingSlash() {
        assertEquals("tc research Steve add FIRSTSTEPS",
                StarterRules.substitute("tc research %player% add FIRSTSTEPS", "Steve"));
        assertEquals("tc research Steve add FIRSTSTEPS",
                StarterRules.substitute("  /tc research %player% add FIRSTSTEPS ", "Steve"));
        assertEquals("say hi", StarterRules.substitute("say hi", "Steve"));
        assertEquals("", StarterRules.substitute(null, "Steve"));
    }

    @Test
    void lockdownMembershipFollowsLockAndUnlock() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        LockdownSet set = new LockdownSet();
        assertFalse(set.contains(a));
        set.lock(a, 1, 64, 1);
        assertTrue(set.contains(a));
        assertFalse(set.contains(b));
        assertEquals(1, set.size());
        assertEquals(64.0D, set.anchor(a)[1]);
        set.unlock(a);
        assertFalse(set.contains(a));
        assertEquals(0, set.size());
    }

    @Test
    void pickTextChoosesTheResetTextOnlyForAReawakening() {
        assertEquals("first", StarterRules.pickText(false, "first", "again"));
        assertEquals("again", StarterRules.pickText(true, "first", "again"));
    }

    @Test
    void tcResearchAddKeyReadsOnlyTheResearchAddCommand() {
        assertEquals("FIRSTSTEPS", StarterRules.tcResearchAddKey("tc research Steve add FIRSTSTEPS"));
        assertNull(StarterRules.tcResearchAddKey("tc research Steve remove FIRSTSTEPS"));
        assertNull(StarterRules.tcResearchAddKey("give Steve minecraft:apple"));
        assertNull(StarterRules.tcResearchAddKey(null));
    }
}
