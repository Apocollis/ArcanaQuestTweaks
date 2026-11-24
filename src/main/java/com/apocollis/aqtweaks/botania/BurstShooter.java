package com.apocollis.aqtweaks.botania;

import net.minecraft.entity.EntityLivingBase;

/**
 * Implemented onto Botania's {@code EntityManaBurst} by {@code MixinEntityManaBurstShooter}. Botania's player
 * burst never records who fired it (vanilla's {@code thrower} stays null), so Tweaks keeps it here. Vanilla types
 * only, so early mixins can use it without importing Botania.
 */
public interface BurstShooter {

    /** The player who fired this burst from a mana blaster, or null (spreaders, dispensers, reloaded bursts). */
    EntityLivingBase aqtweaks$getShooter();
}
