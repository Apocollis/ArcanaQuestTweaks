package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Prospectus pick chances. Unknown variants keep the pick's own accuracy. */
public final class ProspectAccuracy {

    private static final int PERK_BONUS = 25;
    private static final Map<String, Integer> BASE;

    static {
        Map<String, Integer> bases = new HashMap<>();
        bases.put("wood", 10);
        bases.put("stone", 20);
        bases.put("tin", 30);
        bases.put("copper", 35);
        bases.put("bronze", 40);
        bases.put("gold", 45);
        bases.put("iron", 50);
        bases.put("aluminum", 55);
        bases.put("lead", 60);
        bases.put("silver", 65);
        bases.put("diamond", 70);
        bases.put("steel", 75);
        BASE = Collections.unmodifiableMap(bases);
    }

    private ProspectAccuracy() {}

    public static int chance(int fieldAccuracy, String variant, EntityPlayer player) {
        Integer base = variant == null ? null : BASE.get(variant);
        int accuracy = base != null ? base : fieldAccuracy;
        if (player != null && PerkAccess.on(player, "aqtweaks:prospector",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.prospector.enable)) {
            accuracy = Math.min(100, accuracy + PERK_BONUS);
        }
        return accuracy;
    }
}
