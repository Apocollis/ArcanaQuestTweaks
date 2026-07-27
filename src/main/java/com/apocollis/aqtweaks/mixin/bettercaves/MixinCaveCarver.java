package com.apocollis.aqtweaks.mixin.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.world.carver.cave.CaveCarver;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CaveCarver.class, remap = false)
public abstract class MixinCaveCarver {
    // Reverted bottomY overrides so Pass 1 (Positive Y) runs natively from Y = 1 to Y = 255.
}
