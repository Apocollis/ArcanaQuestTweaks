package com.apocollis.aqtweaks.atum;

/**
 * Implemented by the Atum world provider through {@code MixinWorldProviderAtum}: the duration, in ticks, that the next
 * storm must get when its warning countdown ends (0 = none). It lives on the provider so it dies with its world.
 */
public interface PendingStorm {
    int aqtweaks$getPendingStormTicks();

    void aqtweaks$setPendingStormTicks(int ticks);
}
