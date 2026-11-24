package com.apocollis.aqtweaks.reskillable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ForagerLumberjackTest {

    @Test
    void freeBlockPatternsUseWildcardsAndIgnoreCase() {
        String[] patterns = {"rustic:*", "Harvestcraft:pam*crop", "minecraft:deadbush"};
        assertTrue(ForagerBlocks.matchesAny("rustic:grape_stem", patterns));
        assertTrue(ForagerBlocks.matchesAny("harvestcraft:pamcorncrop", patterns));
        assertTrue(ForagerBlocks.matchesAny("minecraft:deadbush", patterns));
        assertFalse(ForagerBlocks.matchesAny("minecraft:stone", patterns));
        assertFalse(ForagerBlocks.matchesAny("minecraft:deadbush2", patterns));
        assertFalse(ForagerBlocks.matchesAny("minecraft:stone", new String[0]));
        assertFalse(ForagerBlocks.matchesAny("minecraft:stone", null));
        assertFalse(ForagerBlocks.matchesAny("minecraft:stone", new String[] {"", "  "}));
    }

    @Test
    void lumberjackMultipliesTheSpeedOnlyAboveOne() {
        assertEquals(6.0F, LumberjackSpeed.apply(3.0F, 2.0D), 1e-6F);
        assertEquals(3.0F, LumberjackSpeed.apply(3.0F, 1.0D), 1e-6F);
        assertEquals(3.0F, LumberjackSpeed.apply(3.0F, 0.5D), 1e-6F);
    }

    @Test
    void halvingTheSpeedDividedByTheLogCountKeepsTheTreeScaling() {
        // Tree Chopper sets original / logCount; Lumberjack then doubles it: a 10 log tree digs as 5 logs.
        float original = 6.0F;
        float treeChopper = original / 10.0F;
        assertEquals(original / 5.0F, LumberjackSpeed.apply(treeChopper, 2.0D), 1e-6F);
    }
}
