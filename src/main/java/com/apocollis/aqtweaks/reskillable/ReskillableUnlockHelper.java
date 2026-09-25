package com.apocollis.aqtweaks.reskillable;

import codersafterdark.reskillable.api.ReskillableRegistries;
import codersafterdark.reskillable.api.data.PlayerData;
import codersafterdark.reskillable.api.data.PlayerDataHandler;
import codersafterdark.reskillable.api.data.PlayerSkillInfo;
import codersafterdark.reskillable.api.unlockable.Unlockable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public final class ReskillableUnlockHelper {
    private ReskillableUnlockHelper() {}

    public static boolean hasUnlockable(EntityPlayer player, String registryId) {
        if (registryId == null || registryId.isEmpty()) return false;
        Unlockable unlockable = ReskillableRegistries.UNLOCKABLES.getValue(new ResourceLocation(registryId));
        if (unlockable == null) return false;
        PlayerData data = PlayerDataHandler.get(player);
        if (data == null) return false;
        PlayerSkillInfo info = data.getSkillInfo(unlockable.getParentSkill());
        return info != null && info.isUnlocked(unlockable);
    }
}
