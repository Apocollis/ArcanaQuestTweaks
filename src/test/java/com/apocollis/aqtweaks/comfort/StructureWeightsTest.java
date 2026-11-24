package com.apocollis.aqtweaks.comfort;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StructureWeightsTest {

    @Test
    void stairsUseTheirOwnWeightWhenEnabled() {
        assertEquals(1.0f, StructureWeights.unlisted(true, 1.0f, 0.25f));
    }

    @Test
    void stairsFallBackToTheUnlistedWeightWhenTheirWeightIsZero() {
        assertEquals(0.25f, StructureWeights.unlisted(true, 0.0f, 0.25f));
    }

    @Test
    void stairsStillScoreWhenUnlistedBlocksDoNot() {
        assertEquals(1.0f, StructureWeights.unlisted(true, 1.0f, 0.0f));
    }

    @Test
    void otherBlocksKeepTheUnlistedWeight() {
        assertEquals(0.25f, StructureWeights.unlisted(false, 1.0f, 0.25f));
        assertEquals(0.0f, StructureWeights.unlisted(false, 1.0f, 0.0f));
        assertEquals(0.0f, StructureWeights.unlisted(false, 1.0f, -3.0f));
    }
}
