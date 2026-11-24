package com.apocollis.aqtweaks.thaumcraft;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Players whose runic shield must not refill right now (the Ley-Rupture surge, plan 6.17). The Twilight Forest handler
 * fills the set once a second; the Thaumcraft mixin only reads it. No Twilight Forest type here.
 */
public final class RunicRechargeHooks {
    private static final Set<UUID> BLOCKED = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private RunicRechargeHooks() {}

    public static boolean blocked(EntityPlayer player) {
        return player != null && !BLOCKED.isEmpty() && BLOCKED.contains(player.getUniqueID());
    }

    public static void set(UUID id, boolean blocked) {
        if (blocked) {
            BLOCKED.add(id);
        } else {
            BLOCKED.remove(id);
        }
    }

    public static void clear() {
        BLOCKED.clear();
    }
}
