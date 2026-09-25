package com.apocollis.aqtweaks.mixin.thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import thaumcraft.common.lib.events.PlayerEvents;

import java.util.HashMap;

/** {@code runicInfo} is the gear cap Thaumcraft already computed, including Astral's add. */
@Mixin(value = PlayerEvents.class, remap = false)
public interface MixinPlayerEventsRunicInfo {

    @Accessor("runicInfo")
    static HashMap<Integer, Integer> aqtweaks$runicInfo() {
        throw new AssertionError();
    }
}
