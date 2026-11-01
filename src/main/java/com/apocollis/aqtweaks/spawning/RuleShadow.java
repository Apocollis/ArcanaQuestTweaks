package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import com.apocollis.aqtweaks.spawning.rules.GearTable;
import com.apocollis.aqtweaks.spawning.rules.ModifierPlan;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Compares what In Control gave a mob (stats, potions, gear) with what the modifiers and gear tables would give it,
 * without changing anything. Before In Control acts a mob is snapshotted; after it has acted, the tables are applied
 * to the snapshot on paper and the two are compared. Mobs where a 'random' row was in play are not compared, and
 * neither is 'angry' (it depends on who is nearby). Runs under the same switch as the conditions comparison.
 */
public final class RuleShadow {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    private static final int MAX_KEPT = 300;
    private static final double TOLERANCE = 1e-4;
    private static final String IN_CONTROL_TAG = "incontrol:onjoinaction";

    private static final class Mismatch {
        final String example;
        int count = 1;

        Mismatch(String example) {
            this.example = example;
        }
    }

    private static final Map<net.minecraft.entity.Entity, SpawnRuleApplier.Snapshot> BEFORE =
            new WeakHashMap<net.minecraft.entity.Entity, SpawnRuleApplier.Snapshot>();
    private static final Map<String, Mismatch> MISMATCHES = new LinkedHashMap<String, Mismatch>();
    private static long same;
    private static long different;
    private static long skippedRandom;
    private static long skippedOther;
    private static long unlogged;
    private static boolean failureLogged;

    private static boolean active() {
        return SpawningModuleConfig.general.enable && SpawningModuleConfig.general.shadowSpawnConditions
                && SpawnRuleTables.ready() && !SpawnRuleDecider.deciding();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void joinBefore(EntityJoinWorldEvent event) {
        if (active() && event.getEntity() instanceof EntityLiving && !event.getWorld().isRemote
                && !event.getEntity().getEntityData().getBoolean(IN_CONTROL_TAG)) {
            remember((EntityLiving) event.getEntity());
        }
    }

    /**
     * Taken at join, before In Control's join handler, not at the spawn check: the game and other mods equip and buff a
     * mob between the two (onInitialSpawn: a skeleton's bow, a zombie's armour, a Gaia mob's weapon), and none of that
     * is In Control's doing. The one rule In Control applies at the spawn check (the Crimson cultists' health and
     * damage) therefore shows up as a difference for those mobs.
     */
    private static synchronized void remember(EntityLiving mob) {
        BEFORE.put(mob, SpawnRuleApplier.snapshot(mob));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void joinAfter(EntityJoinWorldEvent event) {
        if (!active() || !(event.getEntity() instanceof EntityLiving) || event.getWorld().isRemote) {
            return;
        }
        EntityLiving mob = (EntityLiving) event.getEntity();
        SpawnRuleApplier.Snapshot before;
        synchronized (RuleShadow.class) {
            before = BEFORE.remove(mob);
        }
        if (before == null) {
            return;
        }
        if (event.isCanceled()) {
            return;
        }
        try {
            LiveQuery.Join query = new LiveQuery.Join(event);
            ModifierPlan plan = SpawnRuleTables.modifiers().decide(query);
            GearTable.Loadout loadout = SpawnRuleTables.gear().decide(query);
            if (plan.usedRandom() || loadout.usedRandom()) {
                count(0, 0, 1);
                return;
            }
            List<String> differences = new ArrayList<String>();
            compareStats(mob, before, plan, differences);
            comparePotions(mob, before, plan, differences);
            compareGear(mob, before, loadout, differences);
            String id = String.valueOf(EntityList.getKey(mob.getClass()));
            String example = "dimension " + mob.world.provider.getDimension() + ", y " + mob.getPosition().getY()
                    + ", modifiers rows " + plan.rows() + ", gear row " + loadout.row();
            if (differences.isEmpty()) {
                count(1, 0, 0);
            } else {
                count(0, 1, 0);
                for (String difference : differences) {
                    record(id, id + " " + difference, example);
                }
            }
        } catch (RuntimeException failure) {
            synchronized (RuleShadow.class) {
                skippedOther++;
            }
            if (!failureLogged) {
                failureLogged = true;
                LOGGER.warn("[AQTweaks] Modifier and gear comparison failed; further failures are counted only.", failure);
            }
        }
    }

    private static void compareStats(EntityLiving mob, SpawnRuleApplier.Snapshot before, ModifierPlan plan,
                                     List<String> differences) {
        for (Map.Entry<String, net.minecraft.entity.ai.attributes.IAttribute> entry : SpawnRuleApplier.ATTRIBUTES.entrySet()) {
            String stat = entry.getKey();
            Double start = before.stats.get(stat);
            IAttributeInstance attribute = mob.getEntityAttribute(entry.getValue());
            if (start == null || attribute == null) {
                continue;
            }
            double expected = plan.expected(stat, start);
            double actual = attribute.getBaseValue();
            if (Math.abs(expected - actual) > TOLERANCE * Math.max(1.0, Math.abs(expected))) {
                differences.add(stat + ": In Control " + number(actual) + ", Tweaks " + number(expected)
                        + " (from " + number(start) + ")");
            }
        }
    }

    private static void comparePotions(EntityLiving mob, SpawnRuleApplier.Snapshot before, ModifierPlan plan,
                                       List<String> differences) {
        Map<String, Integer> expected = new LinkedHashMap<String, Integer>();
        for (String potion : plan.potions()) {
            String[] parts = potion.split(",");
            if (!before.potions.containsKey(parts[0])) {
                expected.put(parts[0], Integer.parseInt(parts[2]));
            }
        }
        Map<String, Integer> actual = new LinkedHashMap<String, Integer>();
        for (PotionEffect effect : mob.getActivePotionEffects()) {
            String id = SpawnRuleApplier.potionId(effect.getPotion());
            if (!before.potions.containsKey(id)) {
                actual.put(id, effect.getAmplifier());
            }
        }
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            Integer got = actual.get(entry.getKey());
            if (got == null) {
                differences.add("potion " + entry.getKey() + ": In Control none, Tweaks level " + entry.getValue());
            } else if (!got.equals(entry.getValue())) {
                differences.add("potion " + entry.getKey() + ": In Control level " + got + ", Tweaks level " + entry.getValue());
            }
        }
        for (Map.Entry<String, Integer> entry : actual.entrySet()) {
            if (!expected.containsKey(entry.getKey())) {
                differences.add("potion " + entry.getKey() + ": In Control level " + entry.getValue() + ", Tweaks none");
            }
        }
    }

    private static void compareGear(EntityLiving mob, SpawnRuleApplier.Snapshot before, GearTable.Loadout loadout,
                                    List<String> differences) {
        for (String slot : SpawnRuleApplier.SLOTS.keySet()) {
            String start = before.items.get(slot);
            String given = loadout.slots().get(slot);
            String expected = given == null ? start : SpawnRuleApplier.specId(given);
            String actual = SpawnRuleApplier.heldId(mob, slot);
            if (!expected.equals(actual)) {
                differences.add(slot + ": In Control " + shown(actual) + ", Tweaks " + shown(expected));
            }
        }
    }

    private static String shown(String id) {
        return id.isEmpty() ? "empty" : id;
    }

    private static String number(double value) {
        return String.format(java.util.Locale.ROOT, "%.4g", value);
    }

    private static synchronized void count(long sameDelta, long differentDelta, long randomDelta) {
        same += sameDelta;
        different += differentDelta;
        skippedRandom += randomDelta;
    }

    private static synchronized void record(String id, String key, String example) {
        SpawnTrace.escalate(id, key + " at " + example);
        Mismatch known = MISMATCHES.get(key);
        if (known != null) {
            known.count++;
        } else if (MISMATCHES.size() < MAX_KEPT) {
            MISMATCHES.put(key, new Mismatch(example));
            LOGGER.info("[AQTweaks] modifiers or gear differ from In Control: {} at {}", key, example);
        } else {
            unlogged++;
        }
    }

    public static synchronized void reset() {
        MISMATCHES.clear();
        same = 0;
        different = 0;
        skippedRandom = 0;
        skippedOther = 0;
        unlogged = 0;
    }

    /** The comparison so far, one line each. */
    public static synchronized List<String> summary() {
        List<String> lines = new ArrayList<String>();
        lines.add("Modifiers and gear " + (SpawnRuleTables.ready() ? "loaded (" + SpawnRuleTables.status() + ")" : "not loaded ("
                + SpawnRuleTables.status() + ")") + ", comparison "
                + (SpawningModuleConfig.general.shadowSpawnConditions ? "on" : "off") + ": " + same + " mobs same, "
                + different + " different, " + skippedRandom + " skipped (random rows), " + skippedOther
                + " skipped (other). 'angry' is not compared.");
        for (Map.Entry<String, Mismatch> entry : MISMATCHES.entrySet()) {
            lines.add("  " + entry.getValue().count + "x " + entry.getKey() + "; first at " + entry.getValue().example);
        }
        if (unlogged > 0) {
            lines.add("  " + unlogged + " more differences of kinds not listed (" + MAX_KEPT + " kinds kept)");
        }
        return lines;
    }
}
