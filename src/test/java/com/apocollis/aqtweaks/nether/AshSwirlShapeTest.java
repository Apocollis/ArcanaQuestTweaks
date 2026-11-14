package com.apocollis.aqtweaks.nether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.nether.AshSwirlShape.Kind;

import java.util.Random;

import org.junit.jupiter.api.Test;

class AshSwirlShapeTest {
    @Test
    void densityIsDenseCoreAndFeatheredRim() {
        assertEquals(1.0D, AshSwirlShape.density(0.0D));
        assertEquals(1.0D, AshSwirlShape.density(AshSwirlShape.CORE));
        assertEquals(0.0D, AshSwirlShape.density(1.0D));
        assertEquals(0.0D, AshSwirlShape.density(1.4D));
        double mid = AshSwirlShape.density(0.8D);
        assertTrue(mid > 0.0D && mid < 1.0D);
        assertTrue(AshSwirlShape.density(0.7D) > AshSwirlShape.density(0.9D));
    }

    @Test
    void radiusScalesEachAxisByItsOwnReach() {
        assertEquals(1.0D, AshSwirlShape.radius(20.0D, 0.0D, 0.0D, 20.0D, 16.0D), 1e-9);
        assertEquals(1.0D, AshSwirlShape.radius(0.0D, 16.0D, 0.0D, 20.0D, 16.0D), 1e-9);
        assertEquals(0.5D, AshSwirlShape.radius(0.0D, 0.0D, 10.0D, 20.0D, 16.0D), 1e-9);
    }

    @Test
    void coreSpinsFasterAndRisesRimSinks() {
        assertTrue(AshSwirlShape.spinRate(0.0D, 1.0D) > AshSwirlShape.spinRate(1.0D, 1.0D));
        assertTrue(AshSwirlShape.roll(0.0D) > 0.0D);
        assertTrue(AshSwirlShape.roll(1.0D) < 0.0D);
        assertEquals(0.0D, AshSwirlShape.roll(0.5D), 1e-12);
        assertEquals(0.0D, AshSwirlShape.spinRate(0.3D, 0.0D));
    }

    @Test
    void kindMixIsAboutAThirdSheetsMostlyFlecksFewCinders() {
        Random rand = new Random(4L);
        int[] n = new int[3];
        int total = 30000;
        for (int i = 0; i < total; i++) n[AshSwirlShape.kind(rand).ordinal()]++;
        assertEquals(0.28D, (double) n[Kind.SHEET.ordinal()] / total, 0.02D);
        assertEquals(0.66D, (double) n[Kind.FLECK.ordinal()] / total, 0.02D);
        assertEquals(0.06D, (double) n[Kind.EMBER.ordinal()] / total, 0.01D);
    }

    @Test
    void sizesAndLifetimesStayInRange() {
        Random rand = new Random(8L);
        for (int i = 0; i < 2000; i++) {
            float s = AshSwirlShape.scale(Kind.SHEET, rand);
            assertTrue(s >= 3.2F && s <= 5.2F);
            float f = AshSwirlShape.scale(Kind.FLECK, rand);
            assertTrue(f >= 0.55F && f <= 1.15F);
            int life = AshSwirlShape.lifetime(Kind.SHEET, rand);
            assertTrue(life >= 70 && life < 120);
        }
    }

    @Test
    void warningWispsRampFromZero() {
        assertEquals(0, AshSwirlShape.warningWisps(0.0F));
        assertEquals(14, AshSwirlShape.warningWisps(1.0F));
        assertEquals(14, AshSwirlShape.warningWisps(2.0F));
    }
}
