package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps a reposition blink out of clouds (plan 19.8). Pure, no Minecraft types: the runtime builds the zones from the
 * Focus Clouds and mines near the target, this class turns them into a {@link BlinkPicker.Penalty}.
 *
 * <p>Two kinds of zone. An <b>own</b> zone (a cloud this mage cast) is a hard reject: the penalty is positive infinity, so
 * the spot is never offered. An <b>enemy</b> zone (a player's cloud, a mine) is a soft cost: the spot stays a candidate but
 * scores lower, so when every candidate is inside some enemy cloud the least bad one still wins and the mage still blinks.
 */
public final class CloudAvoidance implements BlinkPicker.Penalty {

    /** A sphere-ish zone: a cloud lies flat on its block, so the vertical reach is the radius plus a block. */
    public static final class Zone {
        public final double x;
        public final double y;
        public final double z;
        public final double radius;

        public Zone(double x, double y, double z, double radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = Math.max(0.0D, radius);
        }

        /** True when the point is inside the zone grown by {@code margin} blocks. */
        public boolean contains(double px, double py, double pz, double margin) {
            double reach = radius + Math.max(0.0D, margin);
            double dx = px - x;
            double dz = pz - z;
            return dx * dx + dz * dz <= reach * reach && Math.abs(py - y) <= reach + 1.0D;
        }
    }

    private final List<Zone> own = new ArrayList<>();
    private final List<Zone> enemy = new ArrayList<>();
    private final double margin;
    private final double enemyCost;

    /** @param margin blocks added to every zone's radius @param enemyCost cost per enemy zone the spot lies in */
    public CloudAvoidance(double margin, double enemyCost) {
        this.margin = margin;
        this.enemyCost = Math.max(0.0D, enemyCost);
    }

    public CloudAvoidance addOwn(Zone zone) {
        own.add(zone);
        return this;
    }

    public CloudAvoidance addEnemy(Zone zone) {
        enemy.add(zone);
        return this;
    }

    public int ownCount() {
        return own.size();
    }

    public int enemyCount() {
        return enemy.size();
    }

    @Override
    public double penalty(double x, double y, double z) {
        for (Zone zone : own) {
            if (zone.contains(x, y, z, margin)) {
                return Double.POSITIVE_INFINITY;
            }
        }
        double cost = 0.0D;
        for (Zone zone : enemy) {
            if (zone.contains(x, y, z, margin)) {
                cost += enemyCost;
            }
        }
        return cost;
    }
}
