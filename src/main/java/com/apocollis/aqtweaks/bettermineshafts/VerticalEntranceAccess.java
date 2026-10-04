package com.apocollis.aqtweaks.bettermineshafts;

import net.minecraft.util.math.BlockPos;

/**
 * Accessor implemented by {@code MixinVerticalEntrance}. Locate uses the shaft
 * origin instead of the Start AABB center.
 */
public interface VerticalEntranceAccess {
    BlockPos aqtweaks$getCenterPos();

    /** True once Better Mineshafts refused to build this entrance and Tweaks kept a stub instead. */
    boolean aqtweaks$isFallback();
}
