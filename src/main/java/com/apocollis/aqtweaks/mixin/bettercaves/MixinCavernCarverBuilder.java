package com.apocollis.aqtweaks.mixin.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.world.carver.cavern.CavernCarverBuilder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CavernCarverBuilder.class, remap = false)
public abstract class MixinCavernCarverBuilder {
    // Native builder settings for Pass 1.
}
