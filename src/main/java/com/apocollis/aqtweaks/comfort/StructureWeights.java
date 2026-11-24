package com.apocollis.aqtweaks.comfort;

/** Pure weight rule for structure blocks that are not listed under {@code categories.structure}. */
final class StructureWeights {

    private StructureWeights() {}

    /**
     * Weight an unlisted solid block earns as a structure piece. Stairs (vanilla or modded) use
     * {@code stairsWeight} when it is above zero, so a stair-roofed starter house scores; every other unlisted
     * block uses {@code unlistedWeight}. Zero means the block does not score.
     */
    static float unlisted(boolean stairs, float stairsWeight, float unlistedWeight) {
        if (stairs && stairsWeight > 0.0f) {
            return stairsWeight;
        }
        return Math.max(0.0f, unlistedWeight);
    }
}
