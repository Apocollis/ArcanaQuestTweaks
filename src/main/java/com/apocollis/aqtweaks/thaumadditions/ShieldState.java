package com.apocollis.aqtweaks.thaumadditions;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * One reading of "is this entity shielding, and for how long" that treats a vanilla-style shield and a raised Aegis
 * the same, for perks that care about shield timing. Server side.
 */
public final class ShieldState {
    private ShieldState() {}

    /** A shield is up: Aegis raised, or the entity is using an item that blocks (vanilla's own test). */
    public static boolean isBlocking(EntityLivingBase entity) {
        return AegisChannels.isRaised(entity) || entity.isActiveItemStackBlocking();
    }

    /** Ticks since the shield was raised, or -1 when none is. */
    public static int ticksRaised(EntityPlayer player) {
        AegisChannels.Channel channel = AegisChannels.get(player);
        if (channel != null) {
            return channel.held;
        }
        if (player.isActiveItemStackBlocking()) {
            return player.getActiveItemStack().getMaxItemUseDuration() - player.getItemInUseCount();
        }
        return -1;
    }
}
