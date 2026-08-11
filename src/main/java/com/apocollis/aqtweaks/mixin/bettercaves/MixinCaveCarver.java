package com.apocollis.aqtweaks.mixin.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.world.carver.cave.CaveCarver;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Intentionally empty: Better Caves must keep its configured +Y bottomY.
 * Negative-Y carving is owned by AQTweaks (Depths primer + ChunkProviderServer pass).
 */
@Mixin(value = CaveCarver.class, remap = false)
public abstract class MixinCaveCarver {
}
