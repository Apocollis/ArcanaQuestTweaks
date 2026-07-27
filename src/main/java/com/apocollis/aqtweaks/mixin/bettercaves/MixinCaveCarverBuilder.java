package com.apocollis.aqtweaks.mixin.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.world.carver.cave.CaveCarverBuilder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CaveCarverBuilder.class, remap = false)
public abstract class MixinCaveCarverBuilder {
    // Native builder settings for Pass 1.
}
