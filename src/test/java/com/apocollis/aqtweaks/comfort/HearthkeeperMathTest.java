package com.apocollis.aqtweaks.comfort;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HearthkeeperMathTest {

    private static final double[] CURVE = {6.0D, 4.0D, 3.0D, 2.0D};

    @Test
    void companyBonusFollowsTheCurveThenTheTail() {
        assertEquals(0.0D, HearthkeeperMath.companyBonus(0, CURVE, 1.0D, 15.0D), 1.0e-9);
        assertEquals(6.0D, HearthkeeperMath.companyBonus(1, CURVE, 1.0D, 15.0D), 1.0e-9);
        assertEquals(10.0D, HearthkeeperMath.companyBonus(2, CURVE, 1.0D, 15.0D), 1.0e-9);
        assertEquals(15.0D, HearthkeeperMath.companyBonus(4, CURVE, 1.0D, 99.0D), 1.0e-9);
        assertEquals(16.0D, HearthkeeperMath.companyBonus(5, CURVE, 1.0D, 99.0D), 1.0e-9);
    }

    @Test
    void companyBonusIsCapped() {
        assertEquals(15.0D, HearthkeeperMath.companyBonus(4, CURVE, 1.0D, 15.0D), 1.0e-9);
        assertEquals(15.0D, HearthkeeperMath.companyBonus(9, CURVE, 1.0D, 15.0D), 1.0e-9);
        assertEquals(12.0D, HearthkeeperMath.companyBonus(3, CURVE, 1.0D, 12.0D), 1.0e-9);
    }

    @Test
    void zeroCapMeansNoCap() {
        assertEquals(20.0D, HearthkeeperMath.companyBonus(9, CURVE, 1.0D, 0.0D), 1.0e-9);
    }

    @Test
    void missingCurveUsesTheTailAlone() {
        assertEquals(3.0D, HearthkeeperMath.companyBonus(3, null, 1.0D, 15.0D), 1.0e-9);
    }

    @Test
    void boxBonusIsAShareOfTheCozyScore() {
        assertEquals(6.0D, HearthkeeperMath.boxBonus(40.0D, 0.15D), 1.0e-9);
        assertEquals(0.0D, HearthkeeperMath.boxBonus(-5.0D, 0.15D), 1.0e-9);
        assertEquals(0.0D, HearthkeeperMath.boxBonus(40.0D, -1.0D), 1.0e-9);
    }

    @Test
    void lingerLengthensTheDuration() {
        assertEquals(1800, HearthkeeperMath.lingerDuration(1200, 0.5D));
        assertEquals(1200, HearthkeeperMath.lingerDuration(1200, 0.0D));
        assertEquals(0, HearthkeeperMath.lingerDuration(0, 0.5D));
    }
}
