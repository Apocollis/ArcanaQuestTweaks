package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * The pure parts of the Swarm Mage's escort arrival (plan 18.7): which entities arrive in which order, when each one
 * drops out of the portal, and in which order candidate portal spots are tried. The world work (checking the floor,
 * creating the entities, the portal entity) is in {@link EntityCultistSwarmMage}.
 */
public final class EscortSpawner {

    /** How long the cosmetic portal stays open, in ticks. */
    public static final int PORTAL_LIFE_TICKS = 70;
    /** Ticks after the portal opens until the first escort drops. */
    public static final int FIRST_DELAY_TICKS = 15;
    /** Ticks between two escorts. */
    public static final int STAGGER_TICKS = 10;
    /** The portal opens this far from the mage (plan 18.7: 4 to 6 blocks). */
    public static final double PORTAL_MIN_DISTANCE = 4.0D;
    public static final double PORTAL_MAX_DISTANCE = 6.0D;

    private EscortSpawner() {}

    /** The entity registry ids in arrival order: each entry repeated {@code count} times, entries in the order listed. */
    public static List<String> expand(List<EscortDef> defs) {
        List<String> out = new ArrayList<>();
        if (defs == null) {
            return out;
        }
        for (EscortDef d : defs) {
            for (int i = 0; i < Math.max(0, d.count); i++) {
                out.add(d.entity);
            }
        }
        return out;
    }

    /** True for a well-formed {@code modid:name} registry id (lower case letters, digits, underscore). */
    public static boolean isValidId(String id) {
        return id != null && id.matches("[a-z0-9_]+:[a-z0-9_]+");
    }

    /** The distinct escort entity ids that are malformed or that {@code registered} does not know, in list order. */
    public static List<String> unresolved(List<EscortDef> defs, Predicate<String> registered) {
        List<String> out = new ArrayList<>();
        if (defs == null) {
            return out;
        }
        for (EscortDef d : defs) {
            if (!out.contains(d.entity) && (!isValidId(d.entity) || !registered.test(d.entity))) {
                out.add(d.entity);
            }
        }
        return out;
    }

    /** Tick (counted from the portal opening) at which escort {@code index} drops. */
    public static int spawnTick(int index) {
        return FIRST_DELAY_TICKS + index * STAGGER_TICKS;
    }

    /** How many escorts are due now that {@code elapsed} ticks have passed and {@code alreadySpawned} are out (never over total). */
    public static int dueNow(int elapsed, int alreadySpawned, int total) {
        int due = 0;
        for (int i = alreadySpawned; i < total; i++) {
            if (spawnTick(i) <= elapsed) {
                due++;
            } else {
                break;
            }
        }
        return due;
    }

    /** True once every escort is out and the portal has had its full life. */
    public static boolean arrivalFinished(int elapsed, int spawned, int total) {
        return spawned >= total && elapsed >= PORTAL_LIFE_TICKS;
    }

    /** Angles (radians) to try for the portal spot, evenly spaced, in a random order starting from a random offset. */
    public static List<Double> portalAngles(Random rnd, int count) {
        int n = Math.max(1, count);
        List<Double> angles = new ArrayList<>();
        double offset = rnd.nextDouble() * 2.0D * Math.PI;
        for (int i = 0; i < n; i++) {
            angles.add(offset + i * (2.0D * Math.PI / n));
        }
        Collections.shuffle(angles, rnd);
        return angles;
    }

    /** A portal distance inside the 4 to 6 block ring. */
    public static double portalDistance(Random rnd) {
        return PORTAL_MIN_DISTANCE + rnd.nextDouble() * (PORTAL_MAX_DISTANCE - PORTAL_MIN_DISTANCE);
    }
}
