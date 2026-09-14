package com.apocollis.aqtweaks.bettermineshafts;

import net.minecraft.util.math.BlockPos;

/**
 * Accessor implemented by {@code MixinVerticalEntrance}. Locate uses the shaft
 * origin instead of the Start AABB center.
 */
public interface VerticalEntranceAccess {
    BlockPos aqtweaks$getCenterPos();
}
