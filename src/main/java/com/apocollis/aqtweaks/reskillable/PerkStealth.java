package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.fantasticsource.dynamicstealth.server.event.attacks.StealthAttackEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Awareness cancels the stealth package. Spotting and threat still use the real sight check. */
public class PerkStealth {

    @SubscribeEvent
    public void onStealth(StealthAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer player)) return;
        if (PerkAccess.on(player, "aqtweaks:awareness",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.awareness.enable)) {
            event.setCanceled(true);
        }
    }
}
