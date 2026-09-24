package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;

public final class PerkAccess {

    private PerkAccess() {}

    public static boolean on(EntityPlayer player, String id, boolean enabled) {
        return enabled && player != null && !(player instanceof FakePlayer) && Reflect.hasUnlockable(player, id);
    }
}
