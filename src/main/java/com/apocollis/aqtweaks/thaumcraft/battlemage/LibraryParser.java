package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Parses and validates {@code cultist_battlemages.json}. Pure: no Minecraft types, so it is unit tested. Bad
 * entries are dropped and reported in {@link LibraryData#problems}; the library never throws on content.
 */
public final class LibraryParser {

    public static final Set<String> ROLES = new HashSet<>(Arrays.asList(
            "heal_self", "heal_ally", "ranged_primary", "ranged_heavy", "bolt", "bolt_heavy", "debuff", "peel",
            "summon_upkeep", "aoe", "area_denial", "finisher", "blink", "rift"));

    private LibraryParser() {}

    public static LibraryData parse(String json) {
        LibraryData out = new LibraryData();
        JsonObject root;
        try {
            JsonElement el = new JsonParser().parse(json);
            if (!el.isJsonObject()) {
                out.problems.add("root is not an object");
                return out;
            }
            root = el.getAsJsonObject();
        } catch (RuntimeException e) {
            out.problems.add("invalid JSON: " + e.getMessage());
            return out;
        }
        out.schemaVersion = i(root, "schemaVersion", 0);
        if (root.has("ai") && root.get("ai").isJsonObject()) {
            parseAi(root.getAsJsonObject("ai"), out.ai);
        }
        if (root.has("foci") && root.get("foci").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("foci")) {
                if (e.isJsonObject()) {
                    parseFocus(e.getAsJsonObject(), out);
                }
            }
        }
        Map<Integer, JsonObject> tierAi = new HashMap<>();
        if (root.has("tiers") && root.get("tiers").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("tiers")) {
                if (e.isJsonObject()) {
                    JsonObject o = e.getAsJsonObject();
                    parseTier(o, out);
                    int n = i(o, "tier", 0);
                    if (out.tiers.containsKey(n) && o.has("ai") && o.get("ai").isJsonObject()) {
                        tierAi.put(n, o.getAsJsonObject("ai"));
                    }
                }
            }
        }
        if (out.tiers.isEmpty()) {
            out.problems.add("no tiers defined");
        }
        for (TierDef t : out.tiers.values()) {
            t.ai = out.ai.copy();
            JsonObject override = tierAi.get(t.tier);
            if (override != null) {
                parseAi(override, t.ai);
            }
        }
        if (root.has("variants") && root.get("variants").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("variants")) {
                if (e.isJsonObject()) {
                    parseVariant(e.getAsJsonObject(), out);
                }
            }
        }
        return out;
    }

    private static void parseAi(JsonObject o, AiTuning ai) {
        ai.decisionIntervalTicks = Math.max(1, i(o, "decisionIntervalTicks", ai.decisionIntervalTicks));
        if (o.has("idealRange") && o.get("idealRange").isJsonObject()) {
            JsonObject r = o.getAsJsonObject("idealRange");
            ai.idealMin = d(r, "min", ai.idealMin);
            ai.idealMax = d(r, "max", ai.idealMax);
        }
        ai.globalCastGapTicks = Math.max(0, i(o, "globalCastGapTicks", ai.globalCastGapTicks));
        if (o.has("strafe") && o.get("strafe").isJsonObject()) {
            JsonObject s = o.getAsJsonObject("strafe");
            ai.strafeFlipMinTicks = Math.max(1, i(s, "flipMinTicks", ai.strafeFlipMinTicks));
            ai.strafeFlipMaxTicks = Math.max(ai.strafeFlipMinTicks, i(s, "flipMaxTicks", ai.strafeFlipMaxTicks));
            ai.strafeStep = d(s, "step", ai.strafeStep);
        }
        ai.leadShots = b(o, "leadShots", ai.leadShots);
        ai.repositionIntervalTicks = Math.max(1, i(o, "repositionIntervalTicks", ai.repositionIntervalTicks));
        ai.giveUpTicks = Math.max(1, i(o, "giveUpTicks", ai.giveUpTicks));
        ai.aoeMinRange = d(o, "aoeMinRange", ai.aoeMinRange);
        ai.followRange = d(o, "followRange", ai.followRange);
        ai.moveSpeed = d(o, "moveSpeed", ai.moveSpeed);
        ai.startupDelayTicks = Math.max(0, i(o, "startupDelayTicks", ai.startupDelayTicks));
        ai.projectileSpeed = Math.max(0.1D, d(o, "projectileSpeed", ai.projectileSpeed));
        ai.maxLeadTicks = Math.max(0, i(o, "maxLeadTicks", ai.maxLeadTicks));
        if (ai.idealMax < ai.idealMin) {
            ai.idealMax = ai.idealMin;
        }
        if (o.has("castBudget") && o.get("castBudget").isJsonObject()) {
            JsonObject c = o.getAsJsonObject("castBudget");
            ai.castBudgetEnabled = b(c, "enabled", ai.castBudgetEnabled);
            ai.budgetMax = Math.max(1.0D, d(c, "max", ai.budgetMax));
            ai.budgetRegenPerSecond = Math.max(0.0D, d(c, "regenPerSecond", ai.budgetRegenPerSecond));
            ai.costPerComplexity = Math.max(0.0D, d(c, "costPerComplexity", ai.costPerComplexity));
            ai.holdBelowFraction = d(c, "holdBelowFraction", ai.holdBelowFraction);
            ai.resumeFraction = d(c, "resumeFraction", ai.resumeFraction);
        }
        if (o.has("upkeep") && o.get("upkeep").isJsonObject()) {
            JsonObject u = o.getAsJsonObject("upkeep");
            ai.upkeepIntervalTicks = Math.max(1, i(u, "intervalTicks", ai.upkeepIntervalTicks));
            ai.upkeepRadius = Math.max(8.0D, d(u, "radius", ai.upkeepRadius));
            ai.desiredActiveSpellBats = Math.max(0, i(u, "desiredActiveSpellBats", ai.desiredActiveSpellBats));
        }
        if (o.has("melee") && o.get("melee").isJsonObject()) {
            JsonObject m = o.getAsJsonObject("melee");
            ai.meleeTriggerRange = Math.max(0.5D, d(m, "triggerRange", ai.meleeTriggerRange));
            ai.meleeReach = Math.max(0.5D, d(m, "reach", ai.meleeReach));
            ai.meleeAttackIntervalTicks = Math.max(1, i(m, "attackIntervalTicks", ai.meleeAttackIntervalTicks));
            ai.meleeBurstMin = Math.max(1, i(m, "burstMin", ai.meleeBurstMin));
            ai.meleeBurstMax = Math.max(ai.meleeBurstMin, i(m, "burstMax", ai.meleeBurstMax));
            ai.meleeBurstMaxTicks = Math.max(1, i(m, "burstMaxTicks", ai.meleeBurstMaxTicks));
            ai.meleeBurstCooldownTicks = Math.max(0, i(m, "burstCooldownTicks", ai.meleeBurstCooldownTicks));
            ai.meleeRetreatTicks = Math.max(0, i(m, "retreatTicks", ai.meleeRetreatTicks));
            ai.meleeCloseInChance = frac(m, "closeInChance", ai.meleeCloseInChance);
            ai.meleeCloseInMaxRange = Math.max(1.0D, d(m, "closeInMaxRange", ai.meleeCloseInMaxRange));
            ai.meleeStrikeBlinkChance = frac(m, "strikeBlinkChance", ai.meleeStrikeBlinkChance);
            ai.meleeStrikeBlinkMaxRange = Math.max(2.0D, d(m, "strikeBlinkMaxRange", ai.meleeStrikeBlinkMaxRange));
            ai.meleeStrikeInWindowTicks = Math.max(0, i(m, "strikeInWindowTicks", ai.meleeStrikeInWindowTicks));
            ai.meleeStrikeOutWindowTicks = Math.max(0, i(m, "strikeOutWindowTicks", ai.meleeStrikeOutWindowTicks));
        }
        if (o.has("stun") && o.get("stun").isJsonObject()) {
            JsonObject s = o.getAsJsonObject("stun");
            ai.stunDurationScale = Math.max(0.0D, d(s, "durationScale", ai.stunDurationScale));
            ai.stunImmunityTicks = Math.max(0, i(s, "immunityTicks", ai.stunImmunityTicks));
        }
        if (o.has("blink") && o.get("blink").isJsonObject()) {
            JsonObject bl = o.getAsJsonObject("blink");
            ai.blinkCooldownTicks = Math.max(0, i(bl, "cooldownTicks", ai.blinkCooldownTicks));
            ai.repositionBlinkIntervalTicks = Math.max(20, i(bl, "repositionIntervalTicks", ai.repositionBlinkIntervalTicks));
            ai.repositionBlinkChance = frac(bl, "repositionChance", ai.repositionBlinkChance);
            ai.blinkSamples = Math.max(1, i(bl, "samples", ai.blinkSamples));
            ai.blinkMaxTravel = Math.max(2.0D, d(bl, "maxTravel", ai.blinkMaxTravel));
            ai.blinkAllyMinDistance = Math.max(0.5D, d(bl, "allyMinDistance", ai.blinkAllyMinDistance));
            ai.blinkAllyMaxDistance = Math.max(ai.blinkAllyMinDistance, d(bl, "allyMaxDistance", ai.blinkAllyMaxDistance));
            ai.blinkBandMinDistance = Math.max(1.0D, d(bl, "bandMinDistance", ai.blinkBandMinDistance));
            ai.blinkBandMaxDistance = Math.max(ai.blinkBandMinDistance, d(bl, "bandMaxDistance", ai.blinkBandMaxDistance));
            ai.cloudAvoidMargin = Math.max(0.0D, d(bl, "cloudAvoidMargin", ai.cloudAvoidMargin));
            ai.enemyCloudCost = Math.max(0.0D, d(bl, "enemyCloudCost", ai.enemyCloudCost));
        }
        if (o.has("close") && o.get("close").isJsonObject()) {
            JsonObject c = o.getAsJsonObject("close");
            ai.closeMeleeMin = Math.max(1, i(c, "meleeMin", ai.closeMeleeMin));
            ai.closeMeleeMax = Math.max(ai.closeMeleeMin, i(c, "meleeMax", ai.closeMeleeMax));
            ai.closeSequenceMaxTicks = Math.max(20, i(c, "maxTicks", ai.closeSequenceMaxTicks));
        }
        ai.healAllyHpFraction = Math.max(0.0D, Math.min(1.0D, d(o, "healAllyHpFraction", ai.healAllyHpFraction)));
        ai.healAllyRange = Math.max(1.0D, d(o, "healAllyRange", ai.healAllyRange));
        ai.healAllyCastDistance = Math.max(0.5D, d(o, "healAllyCastDistance", ai.healAllyCastDistance));
        ai.healAllyCooldownTicks = Math.max(0, i(o, "healAllyCooldownTicks", ai.healAllyCooldownTicks));
        ai.healAllyBudgetMax = Math.max(0, i(o, "healAllyBudgetMax", ai.healAllyBudgetMax));
        ai.healAllyBudgetRegenTicks = Math.max(1, i(o, "healAllyBudgetRegenTicks", ai.healAllyBudgetRegenTicks));
        ai.cloudGuardRange = Math.max(0.5D, d(o, "cloudGuardRange", ai.cloudGuardRange));
        ai.cloudGuardNearRange = Math.max(1.0D, d(o, "cloudGuardNearRange", ai.cloudGuardNearRange));
        ai.cloudGuardCastTicks = Math.max(0, i(o, "cloudGuardCastTicks", ai.cloudGuardCastTicks));
        ai.cloudGuardMeleeMin = Math.max(1, i(o, "cloudGuardMeleeMin", ai.cloudGuardMeleeMin));
        ai.cloudGuardMeleeMax = Math.max(ai.cloudGuardMeleeMin, i(o, "cloudGuardMeleeMax", ai.cloudGuardMeleeMax));
        ai.cloudGuardCooldownTicks = Math.max(0, i(o, "cloudGuardCooldownTicks", ai.cloudGuardCooldownTicks));
        ai.cloudGuardCloseTicks = Math.max(0, i(o, "cloudGuardCloseTicks", ai.cloudGuardCloseTicks));
        parseStage3(o, ai);
        ai.volleyGapTicks = Math.max(1, i(o, "volleyGapTicks", ai.volleyGapTicks));
        ai.volleyPauseTicks = Math.max(0, i(o, "volleyPauseTicks", ai.volleyPauseTicks));
        ai.volleyMaxTicks = Math.max(1, i(o, "volleyMaxTicks", ai.volleyMaxTicks));
    }

    /** Stage 3 blocks: shield, healBudget, defense, rift, unstick. Absent blocks keep the {@link AiTuning} defaults. */
    private static void parseStage3(JsonObject o, AiTuning ai) {
        if (o.has("shield") && o.get("shield").isJsonObject()) {
            JsonObject s = o.getAsJsonObject("shield");
            ai.shieldRechargeIntervalTicks = Math.max(1, i(s, "rechargeIntervalTicks", ai.shieldRechargeIntervalTicks));
            ai.shieldBreakWaitTicks = Math.max(0, i(s, "breakWaitTicks", ai.shieldBreakWaitTicks));
            ai.shieldStartFull = b(s, "startFull", ai.shieldStartFull);
            ai.shieldBrokenBandBonus = Math.max(0.0D, d(s, "brokenBandBonus", ai.shieldBrokenBandBonus));
            ai.shieldResumeFraction = Math.max(0.0D, Math.min(1.0D, d(s, "resumeFraction", ai.shieldResumeFraction)));
            ai.staggerTicks = Math.max(0, i(s, "staggerTicks", ai.staggerTicks));
            ai.staggerDamageFraction = Math.max(0.0D, d(s, "staggerDamageFraction", ai.staggerDamageFraction));
        }
        if (o.has("healBudget") && o.get("healBudget").isJsonObject()) {
            JsonObject h = o.getAsJsonObject("healBudget");
            ai.healBudgetMax = Math.max(0, i(h, "max", ai.healBudgetMax));
            ai.healBudgetRegenTicks = Math.max(1, i(h, "regenTicks", ai.healBudgetRegenTicks));
            ai.healHpFraction = frac(h, "healHpFraction", ai.healHpFraction);
            ai.retreatHpFraction = frac(h, "retreatHpFraction", ai.retreatHpFraction);
            ai.retreatResumeFraction = frac(h, "retreatResumeFraction", ai.retreatResumeFraction);
            ai.retreatMaxTicks = Math.max(1, i(h, "retreatMaxTicks", ai.retreatMaxTicks));
            ai.retreatCooldownTicks = Math.max(0, i(h, "retreatCooldownTicks", ai.retreatCooldownTicks));
        }
        if (o.has("defense") && o.get("defense").isJsonObject()) {
            JsonObject f = o.getAsJsonObject("defense");
            ai.peelRange = Math.max(0.5D, d(f, "peelRange", ai.peelRange));
            ai.peelCooldownTicks = Math.max(0, i(f, "peelCooldownTicks", ai.peelCooldownTicks));
            ai.sidestepCooldownTicks = Math.max(0, i(f, "sidestepCooldownTicks", ai.sidestepCooldownTicks));
            ai.sidestepRange = Math.max(2.0D, d(f, "sidestepRange", ai.sidestepRange));
            ai.sidestepImpulse = Math.max(0.0D, d(f, "sidestepImpulse", ai.sidestepImpulse));
            ai.awayHpFraction = frac(f, "awayHpFraction", ai.awayHpFraction);
            ai.surgeLowHealthFraction = frac(f, "surgeLowHealthFraction", ai.surgeLowHealthFraction);
            ai.awayCloseRange = Math.max(1.0D, d(f, "awayCloseRange", ai.awayCloseRange));
            ai.awayPressureTicks = Math.max(0, i(f, "awayPressureTicks", ai.awayPressureTicks));
            ai.awayKnockBackWindowTicks = Math.max(0, i(f, "awayKnockBackWindowTicks", ai.awayKnockBackWindowTicks));
            ai.awayBandMinDistance = Math.max(2.0D, d(f, "awayBandMinDistance", ai.awayBandMinDistance));
            ai.awayBandMaxDistance = Math.max(ai.awayBandMinDistance, d(f, "awayBandMaxDistance", ai.awayBandMaxDistance));
            ai.blinkAfterMeleeChance = frac(f, "blinkAfterMeleeChance", ai.blinkAfterMeleeChance);
            ai.noLosBlinkTicks = Math.max(1, i(f, "noLosBlinkTicks", ai.noLosBlinkTicks));
            ai.targetRepickTicks = Math.max(1, i(f, "targetRepickTicks", ai.targetRepickTicks));
        }
        if (o.has("phases") && o.get("phases").isJsonObject()) {
            JsonObject p = o.getAsJsonObject("phases");
            ai.phaseEmpoweredBelow = frac(p, "empoweredBelow", ai.phaseEmpoweredBelow);
            ai.phaseDesperateBelow = Math.min(ai.phaseEmpoweredBelow, frac(p, "desperateBelow", ai.phaseDesperateBelow));
            ai.phaseEmpoweredCooldownScale = Math.max(0.1D, d(p, "empoweredCooldownScale", ai.phaseEmpoweredCooldownScale));
            ai.phaseDesperateCooldownScale = Math.max(0.1D, d(p, "desperateCooldownScale", ai.phaseDesperateCooldownScale));
            ai.phaseEmpoweredRegenScale = Math.max(0.0D, d(p, "empoweredRegenScale", ai.phaseEmpoweredRegenScale));
            ai.phaseDesperateRegenScale = Math.max(0.0D, d(p, "desperateRegenScale", ai.phaseDesperateRegenScale));
            ai.phaseEmpoweredExtraBats = Math.max(0, i(p, "empoweredExtraBats", ai.phaseEmpoweredExtraBats));
            ai.phaseDesperateExtraBats = Math.max(0, i(p, "desperateExtraBats", ai.phaseDesperateExtraBats));
            ai.phaseDesperateBlinkCooldownTicks = Math.max(0, i(p, "desperateBlinkCooldownTicks", ai.phaseDesperateBlinkCooldownTicks));
            ai.phaseDesperateHealRegenFactor = Math.max(0.1D, d(p, "desperateHealRegenFactor", ai.phaseDesperateHealRegenFactor));
            ai.resetRange = Math.max(0.0D, d(p, "resetRange", ai.resetRange));
            ai.resetAfterTicks = Math.max(1, i(p, "resetAfterTicks", ai.resetAfterTicks));
        }
        if (o.has("rift") && o.get("rift").isJsonObject()) {
            JsonObject r = o.getAsJsonObject("rift");
            ai.riftEnabled = b(r, "enabled", ai.riftEnabled);
            ai.riftMaxThickness = Math.max(1, i(r, "maxThickness", ai.riftMaxThickness));
            ai.riftCooldownMinTicks = Math.max(0, i(r, "cooldownMinTicks", ai.riftCooldownMinTicks));
            ai.riftCooldownMaxTicks = Math.max(ai.riftCooldownMinTicks, i(r, "cooldownMaxTicks", ai.riftCooldownMaxTicks));
            ai.riftMaxHolesPerCast = Math.max(2, i(r, "maxHolesPerCast", ai.riftMaxHolesPerCast));
            ai.riftDetourFactor = Math.max(1.0D, d(r, "detourFactor", ai.riftDetourFactor));
            ai.riftPathProbeIntervalTicks = Math.max(1, i(r, "pathProbeIntervalTicks", ai.riftPathProbeIntervalTicks));
            ai.riftHoleDurationTicks = Math.max(20, i(r, "holeDurationTicks", ai.riftHoleDurationTicks));
        }
        if (o.has("unstick") && o.get("unstick").isJsonObject()) {
            JsonObject u = o.getAsJsonObject("unstick");
            ai.suffocationTicks = Math.max(1, i(u, "suffocationTicks", ai.suffocationTicks));
            ai.emergencyRadius = Math.max(2.0D, d(u, "emergencyRadius", ai.emergencyRadius));
        }
    }

    private static double frac(JsonObject o, String key, double fallback) {
        return Math.max(0.0D, Math.min(1.0D, d(o, key, fallback)));
    }

    private static void parseFocus(JsonObject o, LibraryData out) {
        FocusDef f = new FocusDef();
        f.id = s(o, "id", null);
        if (f.id == null || f.id.isEmpty()) {
            out.problems.add("a focus has no id");
            return;
        }
        if (out.foci.containsKey(f.id)) {
            out.problems.add("duplicate focus id " + f.id);
            return;
        }
        f.role = s(o, "role", null);
        if (f.role == null || !ROLES.contains(f.role)) {
            out.problems.add("focus " + f.id + " has unknown role " + f.role);
            return;
        }
        f.minTier = Math.max(1, i(o, "minTier", 1));
        f.slot = s(o, "slot", "any");
        f.complexity = Math.max(0, i(o, "complexity", 0));
        f.baseWeight = Math.max(0.0D, d(o, "baseWeight", 1.0D));
        f.minRange = d(o, "minRange", 0.0D);
        f.maxRange = d(o, "maxRange", 24.0D);
        if (f.maxRange < f.minRange) {
            out.problems.add("focus " + f.id + " has maxRange below minRange");
            return;
        }
        f.requireLos = b(o, "requireLos", true);
        f.performed = b(o, "performed", false);
        if (o.has("dropWeight") && o.get("dropWeight").isJsonPrimitive()) {
            f.dropWeight = Math.max(0.0D, o.get("dropWeight").getAsDouble());
        }
        f.windupTicks = Math.max(0, i(o, "windupTicks", 0));
        f.summonCount = Math.max(1, i(o, "summonCount", 1));
        f.refireOnExpire = b(o, "refireOnExpire", "aoe".equals(f.role) || "area_denial".equals(f.role));
        if (o.has("cooldownTicks") && !o.get("cooldownTicks").isJsonNull()) {
            f.cooldownTicks = Math.max(0, o.get("cooldownTicks").getAsInt());
        }
        if (o.has("dimensions") && o.get("dimensions").isJsonArray()) {
            JsonArray a = o.getAsJsonArray("dimensions");
            f.dimensions = new int[a.size()];
            for (int k = 0; k < a.size(); k++) {
                f.dimensions[k] = a.get(k).getAsInt();
            }
        }
        if (o.has("tags") && o.get("tags").isJsonObject()) {
            JsonObject t = o.getAsJsonObject("tags");
            f.opener = s(t, "opener", null);
            if (t.has("followUp") && t.get("followUp").isJsonArray()) {
                for (JsonElement e : t.getAsJsonArray("followUp")) {
                    f.followUp.add(e.getAsString());
                }
            }
        }
        f.nbt = s(o, "nbt", null);
        if (!f.performed) {
            if (f.nbt == null || CtFocusLine.parse(f.nbt) == null) {
                out.problems.add("focus " + f.id + " has no usable nbt line");
                return;
            }
        }
        out.foci.put(f.id, f);
    }

    private static void parseTier(JsonObject o, LibraryData out) {
        TierDef t = new TierDef();
        t.tier = i(o, "tier", 0);
        if (t.tier < 1) {
            out.problems.add("a tier has no valid number");
            return;
        }
        t.health = d(o, "health", t.health);
        t.armor = d(o, "armor", t.armor);
        t.focusPower = Math.max(0.0D, d(o, "focusPower", t.focusPower));
        t.shieldMax = Math.max(0.0D, d(o, "shieldMax", t.shieldMax));
        if (o.has("loadout") && o.get("loadout").isJsonObject()) {
            t.loadout = parseLoadout(o.getAsJsonObject("loadout"), t.tier, out);
        }
        if (o.has("stage1Foci") && o.get("stage1Foci").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("stage1Foci")) {
                String id = e.getAsString();
                if (out.foci.containsKey(id)) {
                    t.stage1Foci.add(id);
                } else {
                    out.problems.add("tier " + t.tier + " lists unknown focus " + id);
                }
            }
        }
        if (o.has("equipment") && o.get("equipment").isJsonObject()) {
            parseEquipment(o.getAsJsonObject("equipment"), t.equipment, "tier " + t.tier, out);
        }
        parseDrops(o, "drops", t.drops);
        parseDrops(o, "baubleDrops", t.baubleDrops);
        out.tiers.put(t.tier, t);
    }

    private static void parseDrops(JsonObject o, String key, java.util.List<DropEntry> into) {
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            return;
        }
        for (JsonElement e : o.getAsJsonArray(key)) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject d = e.getAsJsonObject();
            if (!d.has("item")) {
                continue;
            }
            DropEntry entry = new DropEntry();
            entry.item = d.get("item").getAsString();
            entry.meta = Math.max(0, i(d, "meta", 0));
            entry.nbt = d.has("nbt") && !d.get("nbt").isJsonNull() ? d.get("nbt").getAsString() : null;
            entry.chance = frac(d, "chance", 1.0D);
            entry.min = Math.max(0, i(d, "min", 1));
            entry.max = Math.max(entry.min, i(d, "max", entry.min));
            entry.weight = Math.max(0.0D, d(d, "weight", 1.0D));
            into.add(entry);
        }
    }

    /** Slots are head, chest, legs, feet, mainhand, offhand; a value is an item id string or an object with item, nbt, enchantments. */
    private static void parseEquipment(JsonObject o, Map<String, EquipmentDef> into, String owner, LibraryData out) {
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            JsonElement v = e.getValue();
            if (v.isJsonPrimitive()) {
                into.put(e.getKey(), new EquipmentDef(v.getAsString()));
            } else if (v.isJsonObject()) {
                JsonObject eo = v.getAsJsonObject();
                String item = s(eo, "item", null);
                if (item == null || item.isEmpty()) {
                    out.problems.add(owner + " equipment " + e.getKey() + " has no item");
                    continue;
                }
                EquipmentDef def = new EquipmentDef(item);
                def.nbt = s(eo, "nbt", null);
                if (eo.has("enchantments") && eo.get("enchantments").isJsonArray()) {
                    for (JsonElement en : eo.getAsJsonArray("enchantments")) {
                        if (!en.isJsonObject()) {
                            continue;
                        }
                        String id = s(en.getAsJsonObject(), "id", null);
                        if (id == null || id.isEmpty()) {
                            out.problems.add(owner + " equipment " + e.getKey() + " has an enchantment with no id");
                            continue;
                        }
                        def.enchantments.add(new EquipmentDef.Ench(id, Math.max(1, i(en.getAsJsonObject(), "lvl", 1))));
                    }
                }
                into.put(e.getKey(), def);
            }
        }
    }

    /** A variant hangs off a tier (plan 18.8); its ai is the tier's resolved tuning plus its own overrides. */
    private static void parseVariant(JsonObject o, LibraryData out) {
        VariantDef v = new VariantDef();
        v.id = s(o, "id", null);
        if (v.id == null || v.id.isEmpty()) {
            out.problems.add("a variant has no id");
            return;
        }
        if (out.variants.containsKey(v.id)) {
            out.problems.add("duplicate variant id " + v.id);
            return;
        }
        v.tier = i(o, "tier", 1);
        TierDef base = out.tiers.get(v.tier);
        if (base == null) {
            out.problems.add("variant " + v.id + " names unknown tier " + v.tier);
            return;
        }
        if (o.has("fixedLoadout") && o.get("fixedLoadout").isJsonArray()) {
            v.fixedLoadout = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("fixedLoadout")) {
                String id = e.getAsString();
                if (out.foci.containsKey(id)) {
                    v.fixedLoadout.add(id);
                } else {
                    out.problems.add("variant " + v.id + " lists unknown focus " + id);
                }
            }
        }
        if (o.has("loadout") && o.get("loadout").isJsonObject()) {
            v.loadout = parseLoadout(o.getAsJsonObject("loadout"), v.tier, out);
        }
        if (o.has("health") && o.get("health").isJsonPrimitive()) {
            v.health = o.get("health").getAsDouble();
        }
        if (o.has("armor") && o.get("armor").isJsonPrimitive()) {
            v.armor = o.get("armor").getAsDouble();
        }
        if (o.has("focusPower") && o.get("focusPower").isJsonPrimitive()) {
            v.focusPower = Math.max(0.0D, o.get("focusPower").getAsDouble());
        }
        if (o.has("shieldMax") && o.get("shieldMax").isJsonPrimitive()) {
            v.shieldMax = Math.max(0.0D, o.get("shieldMax").getAsDouble());
        }
        if (o.has("weights") && o.get("weights").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("weights").entrySet()) {
                if (!ROLES.contains(e.getKey())) {
                    out.problems.add("variant " + v.id + " weights an unknown role " + e.getKey());
                } else if (e.getValue().isJsonPrimitive()) {
                    v.roleWeights.put(e.getKey(), Math.max(0.0D, e.getValue().getAsDouble()));
                }
            }
        }
        if (o.has("equipment") && o.get("equipment").isJsonObject()) {
            parseEquipment(o.getAsJsonObject("equipment"), v.equipment, "variant " + v.id, out);
        }
        if (o.has("escort") && o.get("escort").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("escort")) {
                if (!e.isJsonObject()) {
                    continue;
                }
                String entity = s(e.getAsJsonObject(), "entity", null);
                int count = Math.max(0, i(e.getAsJsonObject(), "count", 1));
                if (entity == null || entity.isEmpty()) {
                    out.problems.add("variant " + v.id + " has an escort entry with no entity");
                } else if (count > 0) {
                    v.escort.add(new EscortDef(entity, count));
                }
            }
        }
        v.ai = (base.ai != null ? base.ai : out.ai).copy();
        if (o.has("ai") && o.get("ai").isJsonObject()) {
            parseAi(o.getAsJsonObject("ai"), v.ai);
        }
        out.variants.put(v.id, v);
    }

    private static LoadoutRoller.Config parseLoadout(JsonObject o, int tier, LibraryData out) {
        LoadoutRoller.Config c = new LoadoutRoller.Config();
        c.tier = tier;
        if (o.has("fixed") && o.get("fixed").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("fixed")) {
                String id = e.getAsString();
                if (out.foci.containsKey(id)) {
                    c.fixed.add(id);
                } else {
                    out.problems.add("tier " + tier + " loadout lists unknown fixed focus " + id);
                }
            }
        }
        if (o.has("slots") && o.get("slots").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("slots")) {
                String slot = e.getAsString();
                if (LoadoutRoller.ROLLABLE_SLOTS.contains(slot)) {
                    c.slots.add(slot);
                } else {
                    out.problems.add("tier " + tier + " loadout has unknown slot " + slot);
                }
            }
        }
        c.wildcards = Math.max(0, i(o, "wildcards", 0));
        c.complexityBudget = Math.max(1, i(o, "complexityBudget", c.complexityBudget));
        c.complexityBias = d(o, "complexityBias", 0.0D);
        c.maxPerRole = Math.max(1, i(o, "maxPerRole", c.maxPerRole));
        c.dimensionWeightBonus = Math.max(1.0D, d(o, "dimensionWeightBonus", c.dimensionWeightBonus));
        c.fastFillerComplexity = Math.max(0, i(o, "fastFillerComplexity", c.fastFillerComplexity));
        c.heavyComplexity = Math.max(1, i(o, "heavyComplexity", c.heavyComplexity));
        c.maxHeavy = Math.max(0, i(o, "maxHeavy", c.maxHeavy));
        c.minHeavyArea = Math.max(0, i(o, "minHeavyArea", c.minHeavyArea));
        c.minHeavyOther = Math.max(0, i(o, "minHeavyOther", c.minHeavyOther));
        c.swarmChance = Math.max(0.0D, Math.min(1.0D, d(o, "swarmChance", c.swarmChance)));
        c.swarmId = s(o, "swarmId", c.swarmId);
        return c;
    }

    private static String s(JsonObject o, String k, String def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : def;
    }

    private static int i(JsonObject o, String k, int def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : def;
    }

    private static double d(JsonObject o, String k, double def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : def;
    }

    private static boolean b(JsonObject o, String k, boolean def) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsBoolean() : def;
    }
}
