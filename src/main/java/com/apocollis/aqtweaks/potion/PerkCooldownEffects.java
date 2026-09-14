package com.apocollis.aqtweaks.potion;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public final class PerkCooldownEffects {

    private PerkCooldownEffects() {}

    public static void apply(EntityPlayer player, Potion potion, int ticks) {
        if (player == null || player.world.isRemote || potion == null || ticks <= 0) return;
        player.addPotionEffect(new PotionEffect(potion, ticks, 0, false, false));
    }
}
