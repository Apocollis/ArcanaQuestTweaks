package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.fantasticsource.dynamicstealth.server.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;

import java.util.UUID;

/** Taunt ×2 and Low Profile ×0.75 on the six threat-generation stats. Decay stays at 1. */
public final class PerkThreat {

    private static final UUID UID = UUID.fromString("c3a1e5b0-7d44-4a1e-9c2f-6b8e0d4a11a7");
    private static final IAttribute[] STATS = {
            Attributes.THREATGEN_SPOTTED,
            Attributes.THREATGEN_ATTACK,
            Attributes.THREATGEN_DAMAGE_TAKEN,
            Attributes.THREATGEN_WARNED_AGAINST,
            Attributes.THREATGEN_KILL,
            Attributes.THREATGEN_VISIBLE
    };

    private PerkThreat() {}

    public static void apply(EntityPlayer player) {
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        double amount = 0.0;
        if (PerkAccess.on(player, "aqtweaks:taunt", perks.taunt.enable)) {
            amount = 1.0;
        } else if (PerkAccess.on(player, "aqtweaks:low_profile", perks.lowProfile.enable)) {
            amount = -0.25;
        }
        for (IAttribute stat : STATS) {
            IAttributeInstance instance = player.getEntityAttribute(stat);
            if (instance == null) continue;
            instance.removeModifier(UID);
            if (amount != 0.0) {
                instance.applyModifier(new AttributeModifier(UID, "aqtweaks.threat", amount, 0));
            }
        }
    }
}
