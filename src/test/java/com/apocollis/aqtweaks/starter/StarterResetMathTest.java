package com.apocollis.aqtweaks.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StarterResetMathTest {

    /** A non-linear stand-in for the vanilla XP curve: points to reach a level. */
    private static int xp(int level) {
        return level * level + 6 * level;
    }

    private static int cost(int lvl) {
        return 2 + lvl;
    }

    @Test
    void baseLevelIsTheStarterTargetHeldToTheCapAndAtLeastOne() {
        assertEquals(4, StarterResetMath.baseLevel(4, 32));
        assertEquals(32, StarterResetMath.baseLevel(40, 32));
        assertEquals(1, StarterResetMath.baseLevel(0, 32));
    }

    @Test
    void onlyPurchasedLevelsRefund() {
        // base 4, current 6: the steps 4->5 and 5->6 were bought.
        assertEquals(xp(cost(4)) + xp(cost(5)),
                StarterResetMath.refundXpForSkill(6, 4, StarterResetMathTest::cost, StarterResetMathTest::xp));
    }

    @Test
    void atOrBelowTheBaseNothingRefunds() {
        assertEquals(0, StarterResetMath.refundXpForSkill(4, 4, StarterResetMathTest::cost, StarterResetMathTest::xp));
        assertEquals(0, StarterResetMath.refundXpForSkill(2, 4, StarterResetMathTest::cost, StarterResetMathTest::xp));
    }

    @Test
    void anUnknownPreviousClassRefundsEveryStepFromLevelOne() {
        assertEquals(xp(cost(1)) + xp(cost(2)),
                StarterResetMath.refundXpForSkill(3, 1, StarterResetMathTest::cost, StarterResetMathTest::xp));
    }

    @Test
    void purchasedLevelsNeverGoNegative() {
        assertEquals(0, StarterResetMath.purchasedLevels(2, 5));
        assertEquals(3, StarterResetMath.purchasedLevels(8, 5));
    }

    @Test
    void refundsClampAtTheIntRange() {
        assertEquals(Integer.MAX_VALUE,
                StarterResetMath.refundXpForSkill(10, 1, l -> l, l -> Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, StarterResetMath.addClamped(Integer.MAX_VALUE, 5));
    }
}
