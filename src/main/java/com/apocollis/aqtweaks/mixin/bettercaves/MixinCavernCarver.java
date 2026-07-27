package com.apocollis.aqtweaks.mixin.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.world.carver.cavern.CavernCarver;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CavernCarver.class, remap = false)
public abstract class MixinCavernCarver {
    // Reverted bottomY overrides so Pass 1 (Positive Y) runs natively from Y = 1 to Y = 255.
}
