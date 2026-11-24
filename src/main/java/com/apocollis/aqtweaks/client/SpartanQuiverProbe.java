package com.apocollis.aqtweaks.client;

import com.oblivioussp.spartanweaponry.event.EventHandlerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/**
 * Mirrors the conditions under which Spartan Weaponry's {@code HudQuiverAmmo.render} draws: a non-empty quiver
 * stack that carries NBT, and a player who is not a spectator. Only called when Spartan Weaponry is loaded.
 */
final class SpartanQuiverProbe {

    private SpartanQuiverProbe() {}

    static boolean shown() {
        ItemStack quiver = EventHandlerClient.getActiveQuiverStack();
        Minecraft mc = Minecraft.getMinecraft();
        return quiver != null && !quiver.isEmpty() && quiver.hasTagCompound()
                && mc.player != null && !mc.player.isSpectator();
    }
}
