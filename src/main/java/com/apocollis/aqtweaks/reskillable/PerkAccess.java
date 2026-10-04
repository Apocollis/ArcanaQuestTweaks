package com.apocollis.aqtweaks.reskillable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.Loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Perk gate. {@link #has} is the single guarded entry into Reskillable's unlockables: callers on
 * always-on classes never touch a Reskillable type, and {@link ReskillableUnlockHelper} only loads
 * when the mod is present.
 */
public final class PerkAccess {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Reskillable");
    private static final boolean RESKILLABLE = Loader.isModLoaded("reskillable");
    private static boolean warned;

    private PerkAccess() {}

    /** True when Reskillable is loaded and the player has unlocked {@code registryId}. */
    public static boolean has(EntityPlayer player, String registryId) {
        if (!RESKILLABLE || player == null || registryId == null || registryId.isEmpty()) return false;
        try {
            return ReskillableUnlockHelper.hasUnlockable(player, registryId);
        } catch (RuntimeException | LinkageError t) {
            if (!warned) {
                warned = true;
                LOGGER.warn("[AQ-RESKILLABLE] unlockable lookup failed; perks read as locked (logged once)", t);
            }
            return false;
        }
    }

    public static boolean on(EntityPlayer player, String id, boolean enabled) {
        return enabled && player != null && !(player instanceof FakePlayer) && has(player, id);
    }
}
