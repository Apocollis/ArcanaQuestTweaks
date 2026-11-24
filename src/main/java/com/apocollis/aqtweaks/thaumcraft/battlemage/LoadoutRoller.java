package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Rolls a Battlemage's loadout (plan 3.15, 17). Pure: no Minecraft types, unit tested with a seeded {@link Random}.
 *
 * <p>The fixed foci (Heal Self, Knock Back, Blink, Rift) are always carried. Each named slot ({@code bolt},
 * {@code projectile}, {@code summon}, {@code aoe}) is rolled from its own pool, then {@link Config#wildcards} extra
 * foci are drawn from every rollable slot. Rules: a focus needs {@code minTier <= tier}; the summed complexity of the
 * rolled foci never exceeds the budget (each pick reserves the cheapest possible rest, so a heavy early pick pushes
 * the later ones toward cheap entries); at least one of the two direct slots is a fast filler; no role appears more
 * than {@code maxPerRole} times among the rolled foci; a focus tied to the current dimension is weighted up.
 *
 * <p>Heavy foci (plan 17.6 to 17.13): a focus is heavy at {@code complexity >= heavyComplexity}. A mage carries at most
 * {@code maxHeavy} of them. The aoe slot (every area focus is heavy) always keeps a heavy seat reserved, so a lighter slot
 * rolled before it can never use the last seat. With {@code minHeavyOther} one extra heavy non-area focus is forced, picked
 * uniformly by focus among the heavy bolt, projectile and summon foci, then placed in its own slot. With
 * {@code swarmChance} the Swarm focus leaves the normal pools and is added on a hit, using one wildcard seat and one heavy
 * seat (it does not satisfy the forced non-area heavy).
 */
public final class LoadoutRoller {

    public static final List<String> ROLLABLE_SLOTS = Arrays.asList("bolt", "projectile", "summon", "aoe");
    private static final Set<String> DIRECT_SLOTS = new HashSet<>(Arrays.asList("bolt", "projectile"));

    /** Per tier (or per variant) data from the library JSON. */
    public static final class Config {
        public int tier = 1;
        /** Foci carried by every mage of this tier, in order. */
        public List<String> fixed = new ArrayList<>();
        /** Rolled slots in order: bolt before projectile so the fast-filler rule can look back. */
        public List<String> slots = new ArrayList<>();
        public int wildcards;
        public int complexityBudget = 160;
        /** 0 is flat; positive favours high complexity as the tier rises, negative the opposite. */
        public double complexityBias;
        public int maxPerRole = 2;
        public double dimensionWeightBonus = 3.0D;
        /** A direct focus at or below this complexity counts as a fast filler. */
        public int fastFillerComplexity = 25;
        /** A focus at or above this complexity is heavy. */
        public int heavyComplexity = 50;
        /** Most heavy foci one mage carries (Tier 1 2, Tier 2 3). */
        public int maxHeavy = 99;
        /** Above 0 the aoe slot only offers heavy foci. */
        public int minHeavyArea;
        /** Above 0 one heavy non-area focus is forced (Tier 2). */
        public int minHeavyOther;
        /** Chance per mage that {@link #swarmId} is carried on top; 0 leaves Swarm an ordinary pool entry. */
        public double swarmChance;
        public String swarmId = "spell_bat_swarm";
    }

    private LoadoutRoller() {}

    /** @return focus ids: the fixed ones that exist in the pool, then the rolled ones */
    public static List<String> roll(Collection<FocusDef> pool, Config cfg, int dimension, Random rnd) {
        Map<String, FocusDef> byId = new LinkedHashMap<>();
        for (FocusDef d : pool) {
            byId.put(d.id, d);
        }
        Collection<FocusDef> all = byId.values();
        List<String> out = new ArrayList<>();
        for (String id : cfg.fixed) {
            if (byId.containsKey(id)) {
                out.add(id);
            }
        }
        List<FocusDef> rolled = new ArrayList<>();
        Set<String> taken = new HashSet<>(out);
        Set<String> excluded = new HashSet<>();
        int wildcardsLeft = cfg.wildcards;

        FocusDef swarm = cfg.swarmChance > 0.0D ? byId.get(cfg.swarmId) : null;
        if (swarm != null) {
            excluded.add(swarm.id);
            if (swarm.minTier <= cfg.tier && rnd.nextDouble() < cfg.swarmChance) {
                rolled.add(swarm);
                taken.add(swarm.id);
                if (wildcardsLeft > 0) {
                    wildcardsLeft--;
                }
            }
        }

        List<String> slots = cfg.slots;
        FocusDef forced = pickForcedOther(all, cfg, taken, excluded, rolled, dimension, rnd);
        int forcedIdx = forced != null ? slots.indexOf(forced.slot) : -1;
        if (forcedIdx < 0) {
            forced = null;
        }
        boolean aoeSeat = aoeNeedsHeavy(all, cfg, taken, excluded);

        for (int i = 0; i < slots.size(); i++) {
            String slot = slots.get(i);
            boolean direct = DIRECT_SLOTS.contains(slot);
            boolean forcedStep = forced != null && i == forcedIdx;
            boolean needFiller = false;
            if (direct && !forcedStep && !hasFiller(rolled, cfg)) {
                boolean laterFree = false;
                for (int j = i + 1; j < slots.size(); j++) {
                    if (DIRECT_SLOTS.contains(slots.get(j)) && !(forced != null && j == forcedIdx)) {
                        laterFree = true;
                        break;
                    }
                }
                needFiller = !laterFree;
            }
            List<FocusDef> cands;
            if (forcedStep && !taken.contains(forced.id) && roleCount(rolled, forced.role) < cfg.maxPerRole) {
                cands = new ArrayList<>();
                cands.add(forced);
            } else {
                cands = candidates(all, slot, false, cfg, taken, excluded, rolled);
                cands = heavyFilter(cands, cfg, rolled, reservedSeats(i, slot, slots, aoeSeat, forced, forcedIdx));
                if ("aoe".equals(slot) && cfg.minHeavyArea > 0) {
                    cands = keepIfAny(cands, true, cfg);
                }
                if (needFiller) {
                    List<FocusDef> filler = new ArrayList<>();
                    for (FocusDef d : cands) {
                        if (d.complexity <= cfg.fastFillerComplexity) {
                            filler.add(d);
                        }
                    }
                    if (!filler.isEmpty()) {
                        cands = filler;
                    }
                }
            }
            int reserve = reserveAfter(all, cfg, taken, excluded, slots, i, forced, forcedIdx, wildcardsLeft);
            FocusDef pick = pick(cands, spent(rolled), reserve, cfg, dimension, rnd);
            if (pick != null) {
                rolled.add(pick);
                taken.add(pick.id);
            }
        }
        for (int w = 0; w < wildcardsLeft; w++) {
            List<FocusDef> cands = candidates(all, null, true, cfg, taken, excluded, rolled);
            cands = heavyFilter(cands, cfg, rolled, 0);
            int reserve = (wildcardsLeft - w - 1) * cheapest(all, null, true, cfg, taken, excluded);
            FocusDef pick = pick(cands, spent(rolled), reserve, cfg, dimension, rnd);
            if (pick != null) {
                rolled.add(pick);
                taken.add(pick.id);
            }
        }
        for (FocusDef d : rolled) {
            out.add(d.id);
        }
        return out;
    }

    /** Summed complexity of the foci in {@code ids} that are not in {@code cfg.fixed}; used by tests and logging. */
    public static int rolledComplexity(List<String> ids, Collection<FocusDef> pool, Config cfg) {
        Map<String, FocusDef> byId = new LinkedHashMap<>();
        for (FocusDef d : pool) {
            byId.put(d.id, d);
        }
        int sum = 0;
        for (String id : ids) {
            FocusDef d = byId.get(id);
            if (d != null && !cfg.fixed.contains(id)) {
                sum += d.complexity;
            }
        }
        return sum;
    }

    /** True when the focus counts against {@link Config#maxHeavy}. */
    public static boolean isHeavy(FocusDef d, Config cfg) {
        return d.complexity >= cfg.heavyComplexity;
    }

    static double tierFactor(int complexity, double bias, int tier) {
        return Math.max(0.05D, 1.0D + bias * (tier - 1.5D) * (complexity - 35.0D) / 15.0D);
    }

    private static boolean hasFiller(List<FocusDef> rolled, Config cfg) {
        for (FocusDef d : rolled) {
            if (DIRECT_SLOTS.contains(d.slot) && d.complexity <= cfg.fastFillerComplexity) {
                return true;
            }
        }
        return false;
    }

    private static int spent(List<FocusDef> rolled) {
        int s = 0;
        for (FocusDef d : rolled) {
            s += d.complexity;
        }
        return s;
    }

    private static int countHeavy(List<FocusDef> rolled, Config cfg) {
        int n = 0;
        for (FocusDef d : rolled) {
            if (isHeavy(d, cfg)) {
                n++;
            }
        }
        return n;
    }

    /**
     * The forced heavy non-area focus (plan 17.9), picked by focus (not by slot) among the heavy bolt, projectile and summon
     * foci. Every focus has the same weight unless it is tied to the current dimension, which weights it up as elsewhere.
     */
    private static FocusDef pickForcedOther(Collection<FocusDef> all, Config cfg, Set<String> taken, Set<String> excluded,
            List<FocusDef> rolled, int dimension, Random rnd) {
        if (cfg.minHeavyOther <= 0) {
            return null;
        }
        List<FocusDef> opts = new ArrayList<>();
        for (FocusDef d : all) {
            if (taken.contains(d.id) || excluded.contains(d.id) || d.minTier > cfg.tier) {
                continue;
            }
            if (!isHeavy(d, cfg) || "aoe".equals(d.slot) || !cfg.slots.contains(d.slot)) {
                continue;
            }
            if (roleCount(rolled, d.role) >= cfg.maxPerRole) {
                continue;
            }
            opts.add(d);
        }
        if (opts.isEmpty()) {
            return null;
        }
        double total = 0.0D;
        double[] weights = new double[opts.size()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = weight(opts.get(i), cfg, dimension);
            total += weights[i];
        }
        if (total <= 0.0D) {
            return opts.get(rnd.nextInt(opts.size()));
        }
        double roll = rnd.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll <= 0.0D) {
                return opts.get(i);
            }
        }
        return opts.get(opts.size() - 1);
    }

    /** True when the aoe slot exists and can only produce heavy foci, so its seat must be held back for it. */
    private static boolean aoeNeedsHeavy(Collection<FocusDef> all, Config cfg, Set<String> taken, Set<String> excluded) {
        if (!cfg.slots.contains("aoe")) {
            return false;
        }
        if (cfg.minHeavyArea > 0) {
            return true;
        }
        boolean any = false;
        for (FocusDef d : all) {
            if (!"aoe".equals(d.slot) || d.minTier > cfg.tier || taken.contains(d.id) || excluded.contains(d.id)) {
                continue;
            }
            any = true;
            if (!isHeavy(d, cfg)) {
                return false;
            }
        }
        return any;
    }

    /** Heavy seats held for later steps: the aoe slot (when it comes after this one) and the forced heavy (same). */
    private static int reservedSeats(int i, String slot, List<String> slots, boolean aoeSeat, FocusDef forced, int forcedIdx) {
        int r = 0;
        if (aoeSeat && !"aoe".equals(slot)) {
            for (int j = i + 1; j < slots.size(); j++) {
                if ("aoe".equals(slots.get(j))) {
                    r++;
                    break;
                }
            }
        }
        if (forced != null && forcedIdx > i) {
            r++;
        }
        return r;
    }

    /** Drops heavy candidates when no seat is free after the held-back ones; keeps the list as it is if that would empty it. */
    private static List<FocusDef> heavyFilter(List<FocusDef> cands, Config cfg, List<FocusDef> rolled, int reserved) {
        if (countHeavy(rolled, cfg) + reserved < cfg.maxHeavy) {
            return cands;
        }
        return keepIfAny(cands, false, cfg);
    }

    /** The candidates that are heavy ({@code heavy} true) or light, or the whole list when none match. */
    private static List<FocusDef> keepIfAny(List<FocusDef> cands, boolean heavy, Config cfg) {
        List<FocusDef> kept = new ArrayList<>();
        for (FocusDef d : cands) {
            if (isHeavy(d, cfg) == heavy) {
                kept.add(d);
            }
        }
        return kept.isEmpty() ? cands : kept;
    }

    /** Foci that may be picked now: right slot (or any rollable slot for a wildcard), tier, not taken, role cap. */
    private static List<FocusDef> candidates(Collection<FocusDef> all, String slot, boolean wildcard, Config cfg,
            Set<String> taken, Set<String> excluded, List<FocusDef> rolled) {
        List<FocusDef> out = new ArrayList<>();
        for (FocusDef d : all) {
            if (taken.contains(d.id) || excluded.contains(d.id) || d.minTier > cfg.tier) {
                continue;
            }
            if (wildcard ? !ROLLABLE_SLOTS.contains(d.slot) : !d.slot.equals(slot)) {
                continue;
            }
            if (roleCount(rolled, d.role) >= cfg.maxPerRole) {
                continue;
            }
            out.add(d);
        }
        return out;
    }

    private static int roleCount(List<FocusDef> rolled, String role) {
        int n = 0;
        for (FocusDef d : rolled) {
            if (d.role.equals(role)) {
                n++;
            }
        }
        return n;
    }

    /** Cheapest possible cost of the picks still to come, so a pick never leaves the rest unaffordable. */
    private static int reserveAfter(Collection<FocusDef> all, Config cfg, Set<String> taken, Set<String> excluded,
            List<String> slots, int i, FocusDef forced, int forcedIdx, int wildcardsLeft) {
        int reserve = 0;
        for (int j = i + 1; j < slots.size(); j++) {
            if (forced != null && j == forcedIdx) {
                reserve += forced.complexity;
            } else {
                reserve += cheapest(all, slots.get(j), false, cfg, taken, excluded);
            }
        }
        if (wildcardsLeft > 0) {
            reserve += wildcardsLeft * cheapest(all, null, true, cfg, taken, excluded);
        }
        return reserve;
    }

    private static int cheapest(Collection<FocusDef> all, String slot, boolean wildcard, Config cfg, Set<String> taken,
            Set<String> excluded) {
        int best = Integer.MAX_VALUE;
        for (FocusDef d : all) {
            if (taken.contains(d.id) || excluded.contains(d.id) || d.minTier > cfg.tier) {
                continue;
            }
            if (wildcard ? !ROLLABLE_SLOTS.contains(d.slot) : !d.slot.equals(slot)) {
                continue;
            }
            best = Math.min(best, d.complexity);
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    private static FocusDef pick(List<FocusDef> cands, int spent, int reserve, Config cfg, int dimension, Random rnd) {
        if (cands.isEmpty()) {
            return null;
        }
        List<FocusDef> affordable = new ArrayList<>();
        for (FocusDef d : cands) {
            if (spent + d.complexity + reserve <= cfg.complexityBudget) {
                affordable.add(d);
            }
        }
        if (affordable.isEmpty()) {
            FocusDef cheapest = cands.get(0);
            for (FocusDef d : cands) {
                if (d.complexity < cheapest.complexity) {
                    cheapest = d;
                }
            }
            return cheapest;
        }
        double total = 0.0D;
        double[] weights = new double[affordable.size()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = weight(affordable.get(i), cfg, dimension);
            total += weights[i];
        }
        if (total <= 0.0D) {
            return affordable.get(rnd.nextInt(affordable.size()));
        }
        double roll = rnd.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll <= 0.0D) {
                return affordable.get(i);
            }
        }
        return affordable.get(affordable.size() - 1);
    }

    static double weight(FocusDef d, Config cfg, int dimension) {
        double w = Math.max(0.0D, d.baseWeight) * tierFactor(d.complexity, cfg.complexityBias, cfg.tier);
        for (int dim : d.dimensions) {
            if (dim == dimension) {
                w *= Math.max(1.0D, cfg.dimensionWeightBonus);
                break;
            }
        }
        return w;
    }
}
