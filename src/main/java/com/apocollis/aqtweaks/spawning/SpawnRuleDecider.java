package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import com.apocollis.aqtweaks.spawning.rules.ConditionTable;
import com.apocollis.aqtweaks.spawning.rules.Decision;
import com.apocollis.aqtweaks.spawning.rules.GearTable;
import com.apocollis.aqtweaks.spawning.rules.ModifierPlan;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * With {@code Spawn Rules Decide} on, Tweaks decides every spawn from the three rule files and In Control's own spawn
 * rules do nothing (see MixinInControlSpawnRules). At the spawn check the conditions table sets the result; at join it
 * can cancel the mob, and a mob that stays gets its modifiers and gear once. Anything not loaded cleanly leaves the
 * decision to In Control.
 */
public final class SpawnRuleDecider {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    /** Set on a mob once its modifiers and gear were applied, so a chunk reload does not apply them again. */
    static final String DONE_TAG = "aqtweaks:spawnrules";
    private static final String IN_CONTROL_TAG = "incontrol:onjoinaction";
    private static boolean failureLogged;

    /** True when Tweaks, not In Control, is deciding spawns now. */
    public static boolean deciding() {
        return SpawningModuleConfig.general.enable && SpawningModuleConfig.general.spawnRulesDecide
                && ConditionShadow.conditions() != null && SpawnRuleTables.ready();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void spawnCheck(LivingSpawnEvent.CheckSpawn event) {
        if (!deciding() || event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        try {
            Decision decision = ConditionShadow.conditions().decide(ConditionTable.Phase.SPAWN, new LiveQuery.Check(event));
            if (decision.result != null) {
                event.setResult(ConditionShadow.toEvent(decision.result));
            }
        } catch (RuntimeException failure) {
            failed(failure);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void join(EntityJoinWorldEvent event) {
        if (!deciding() || !(event.getEntity() instanceof EntityLiving) || event.getWorld().isRemote
                || event.isCanceled()) {
            return;
        }
        try {
            LiveQuery.Join query = new LiveQuery.Join(event);
            Decision decision = ConditionShadow.conditions().decide(ConditionTable.Phase.JOIN, query);
            if (decision.result == com.apocollis.aqtweaks.spawning.rules.Decision.Result.DENY) {
                event.setCanceled(true);
                return;
            }
            NBTTagCompound data = event.getEntity().getEntityData();
            if (data.getBoolean(DONE_TAG) || data.getBoolean(IN_CONTROL_TAG)) {
                return;
            }
            ModifierPlan plan = SpawnRuleTables.modifiers().decide(query);
            GearTable.Loadout loadout = SpawnRuleTables.gear().decide(query);
            data.setBoolean(DONE_TAG, true);
            if (!plan.isEmpty() || !loadout.slots().isEmpty()) {
                SpawnRuleApplier.apply((EntityLiving) event.getEntity(), plan, loadout);
            }
        } catch (RuntimeException failure) {
            failed(failure);
        }
    }

    private static void failed(RuntimeException failure) {
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.warn("[AQTweaks] Deciding a spawn from the rule files failed; further failures are not logged.", failure);
        }
    }
}
