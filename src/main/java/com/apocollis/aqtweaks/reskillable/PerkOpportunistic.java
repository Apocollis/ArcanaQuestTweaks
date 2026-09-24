package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Recent living-source hits. Opportunistic reads this when Dynamic Stealth asks if the victim can see the attacker. */
public final class PerkOpportunistic {

    private static final Map<Integer, Hit> LAST = new ConcurrentHashMap<>();

    private PerkOpportunistic() {}

    public static void record(EntityLivingBase victim, EntityLivingBase attacker) {
        if (victim == null || attacker == null || victim.world == null) return;
        LAST.put(victim.getEntityId(), new Hit(attacker.getEntityId(), victim.world.getTotalWorldTime()));
        if (LAST.size() > 256) {
            long now = victim.world.getTotalWorldTime();
            LAST.entrySet().removeIf(entry -> now - entry.getValue().time > 80);
        }
    }

    public static boolean qualifies(EntityLivingBase victim, Entity attacker) {
        if (!(attacker instanceof EntityPlayer player) || victim == null || victim.world == null) return false;
        if (!PerkAccess.on(player, "aqtweaks:opportunistic",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.opportunistic.enable)) {
            return false;
        }
        Hit hit = LAST.get(victim.getEntityId());
        if (hit == null) return false;
        if (hit.attackerId == player.getEntityId()) return false;
        return victim.world.getTotalWorldTime() - hit.time <= 60;
    }

    private static final class Hit {
        final int attackerId;
        final long time;

        Hit(int attackerId, long time) {
            this.attackerId = attackerId;
            this.time = time;
        }
    }
}
