package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.player.EntityPlayer;

/** Underground body temperature moves 4 points toward 11. 10 and 11 stay put. */
public final class SpelunkerComfort {

    private SpelunkerComfort() {}

    public static int pull(EntityPlayer player, int temp) {
        if (!PerkAccess.on(player, "aqtweaks:spelunker",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.spelunker.enable)) {
            return temp;
        }
        if (!underground(player)) return temp;
        if (temp == 10 || temp == 11) return temp;
        if (temp < 11) return Math.min(11, temp + 4);
        return Math.max(11, temp - 4);
    }

    public static boolean underground(EntityPlayer player) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableUndergroundExposure) return false;
        int y = (int) Math.floor(player.posY);
        int yMin = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureUndergroundYMin;
        int yMax = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureUndergroundYMax;
        return y < yMin || (y >= yMin && y <= yMax);
    }
}
