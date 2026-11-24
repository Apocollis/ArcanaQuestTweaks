package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure ordering and filtering shared by the Battlemage target mixins (plan 2.2). Entities are mapped to
 * {@link Candidate}s by the caller, so this class has no Minecraft types.
 */
public final class BattlemageSeekPicker {

    public enum Priority {
        ATTACK_TARGET, NEAREST, LOWEST_HEALTH;

        public static Priority parse(String s) {
            if (s != null) {
                for (Priority p : values()) {
                    if (p.name().equalsIgnoreCase(s.trim())) {
                        return p;
                    }
                }
            }
            return ATTACK_TARGET;
        }
    }

    public enum CloudKind { HOSTILE, SUPPORT }

    /** A potential target. {@code index} points back into the caller's own list. */
    public static final class Candidate {
        public final int index;
        public final double distanceSq;
        public final float health;
        public final boolean attackTarget;

        public Candidate(int index, double distanceSq, float health, boolean attackTarget) {
            this.index = index;
            this.distanceSq = distanceSq;
            this.health = health;
            this.attackTarget = attackTarget;
        }
    }

    private BattlemageSeekPicker() {}

    /** Candidate indices in priority order. Stable for ties (input order, which stock sorts nearest first). */
    public static List<Integer> order(List<Candidate> candidates, Priority priority) {
        List<Candidate> copy = new ArrayList<>(candidates);
        Comparator<Candidate> byAttack = Comparator.comparing((Candidate c) -> !c.attackTarget);
        switch (priority) {
            case NEAREST:
                copy.sort(Comparator.comparingDouble(c -> c.distanceSq));
                break;
            case LOWEST_HEALTH:
                copy.sort(Comparator.comparingDouble((Candidate c) -> c.health).thenComparingDouble(c -> c.distanceSq));
                break;
            case ATTACK_TARGET:
            default:
                copy.sort(byAttack.thenComparingDouble(c -> c.distanceSq));
                break;
        }
        List<Integer> out = new ArrayList<>(copy.size());
        for (Candidate c : copy) {
            out.add(c.index);
        }
        return out;
    }

    /** The first candidate a Spell Bat search should take, or -1. Attack target wins under ATTACK_TARGET if in range. */
    public static int pickOne(List<Candidate> candidates, Priority priority) {
        List<Integer> ordered = order(candidates, priority);
        return ordered.isEmpty() ? -1 : ordered.get(0);
    }

    /** A cloud heals (SUPPORT) only when its package has Heal and no damage effect; mixed packages count as HOSTILE. */
    public static CloudKind classifyCloud(boolean hasHeal, boolean hasOtherEffect) {
        return hasHeal && !hasOtherEffect ? CloudKind.SUPPORT : CloudKind.HOSTILE;
    }

    /**
     * Whether an entity stays in a Battlemage cloud's target list.
     *
     * @param isCloud         the entity is another focus cloud (kept so stock cloud separation still works)
     * @param validPlayer     alive survival or adventure player
     * @param isOwner         the cloud's owner
     * @param friendlyNonPlayer a non-player the stock friendly rule says is friendly to the owner
     */
    public static boolean keepInCloud(CloudKind kind, boolean isCloud, boolean validPlayer, boolean isOwner,
            boolean friendlyNonPlayer) {
        if (isCloud) {
            return true;
        }
        return kind == CloudKind.HOSTILE ? validPlayer : (isOwner || friendlyNonPlayer);
    }
}
