package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Chooses a wall to rift through (plan 3.14): walks lines from the mage, finds the first solid run, checks that every block
 * of it is one a real Rift could turn into a hole, and finds a standing spot beyond it. Pure: the world is behind
 * {@link Probe}, so it is tested on a grid.
 *
 * <p>The tunnel is two blocks tall (feet and head column) along a straight line at the mage's own level. The exit may be one
 * block higher or lower. Same refusals as the real Rift: anything {@link Probe#riftable} rejects (bedrock, hardness -1,
 * blacklisted blocks, tile entities) makes that direction invalid.
 */
public final class RiftPicker {

    /** Why the mage rifts. */
    public enum Mode {
        /** Escape: land out of the target's sight and farther from it than now. */
        DEFENSIVE,
        /** Pursue: land on the target's side, inside the firing band. */
        OFFENSIVE
    }

    /** The world, as the picker needs it. Cells are block coordinates; {@code y} is the feet level. */
    public interface Probe {
        /** A mage can occupy the block: no collision box, no liquid. */
        boolean open(int x, int y, int z);

        /** A solid block a real Rift could open: no tile entity, not bedrock or hardness -1, not blacklisted. */
        boolean riftable(int x, int y, int z);

        /** Feet cell where the mage may stand: feet and head open, solid floor right below, no hazard. */
        boolean standable(int x, int y, int z);

        /** True when a player at the target could see a mage standing at this feet cell. */
        boolean seenByTarget(int x, int y, int z);
    }

    public static final class Request {
        public Mode mode = Mode.DEFENSIVE;
        public int fromX;
        public int fromY;
        public int fromZ;
        public double targetX;
        public double targetZ;
        public int maxThickness = 12;
        public int maxHoles = 24;
        /** How far a line is followed from the mage, in blocks. */
        public int maxReach = 28;
        /** The wall must start within this many blocks of the mage. */
        public int startWithin = 4;
        /** Offensive: the exit lands between these distances of the target. */
        public double bandMin = 3.0D;
        public double bandMax = 14.0D;
        public int directions = 16;
    }

    /** A chosen tunnel and exit. {@code holes} are the cells to open (feet and head level). */
    public static final class Plan {
        public int exitX;
        public int exitY;
        public int exitZ;
        public int thickness;
        public double score;
        /** Unit direction of the line, for the hole facing. */
        public double dirX;
        public double dirZ;
        public final List<int[]> holes = new ArrayList<>();
    }

    /** An offensive rift that ends outside the band must still bring the mage this much closer to the target. */
    static final double PROGRESS_BLOCKS = 4.0D;

    private RiftPicker() {}

    /** The best plan, or null when no direction works. */
    public static Plan pick(Request req, Probe probe, Random rnd) {
        Plan best = null;
        double fromDist = distance(req.fromX + 0.5D, req.fromZ + 0.5D, req.targetX, req.targetZ);
        double bearing = Math.atan2(req.targetZ - (req.fromZ + 0.5D), req.targetX - (req.fromX + 0.5D));
        int n = Math.max(4, req.directions);
        double offset = rnd.nextDouble() * (2.0D * Math.PI / n);
        for (int i = 0; i < n; i++) {
            double angle = offset + i * (2.0D * Math.PI / n);
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            if (req.mode == Mode.OFFENSIVE) {
                double dot = dx * Math.cos(bearing) + dz * Math.sin(bearing);
                if (dot < 0.3D) {
                    continue;
                }
            }
            Plan plan = trace(req, probe, dx, dz);
            if (plan == null) {
                continue;
            }
            double exitDist = distance(plan.exitX + 0.5D, plan.exitZ + 0.5D, req.targetX, req.targetZ);
            if (req.mode == Mode.OFFENSIVE) {
                // Inside the band is best; a wall that leaves the exit still outside it is fine when it gains real ground.
                if (exitDist < req.bandMin || (exitDist > req.bandMax && exitDist > fromDist - PROGRESS_BLOCKS)) {
                    continue;
                }
                double prefer = (req.bandMin + req.bandMax) * 0.5D;
                double miss = exitDist > req.bandMax ? (exitDist - req.bandMax) : Math.abs(exitDist - prefer);
                plan.score = -miss * 0.6D - plan.thickness * 0.2D;
            } else {
                if (exitDist < fromDist + 2.0D || probe.seenByTarget(plan.exitX, plan.exitY, plan.exitZ)) {
                    continue;
                }
                plan.score = (exitDist - fromDist) * 0.5D - plan.thickness * 0.2D;
            }
            plan.score += rnd.nextDouble() * 0.05D;
            if (best == null || plan.score > best.score) {
                best = plan;
            }
        }
        return best;
    }

    /** Follows one line; returns the plan for it, or null when the line is invalid. */
    static Plan trace(Request req, Probe probe, double dx, double dz) {
        List<int[]> cells = rayCells(req.fromX + 0.5D, req.fromZ + 0.5D, dx, dz, req.maxReach);
        int y = req.fromY;
        Plan plan = new Plan();
        plan.dirX = dx;
        plan.dirZ = dz;
        boolean inWall = false;
        int index = 0;
        for (int[] c : cells) {
            index++;
            boolean feetOpen = probe.open(c[0], y, c[1]);
            boolean headOpen = probe.open(c[0], y + 1, c[1]);
            boolean both = feetOpen && headOpen;
            if (!inWall) {
                if (both) {
                    if (index > req.startWithin) {
                        return null;
                    }
                    continue;
                }
                if (index > req.startWithin) {
                    return null;
                }
                inWall = true;
            }
            if (both) {
                int exitY = standingLevel(probe, c[0], y, c[1]);
                if (exitY == Integer.MIN_VALUE) {
                    return null;
                }
                plan.exitX = c[0];
                plan.exitY = exitY;
                plan.exitZ = c[1];
                return plan.thickness > 0 ? plan : null;
            }
            if (!feetOpen) {
                if (!probe.riftable(c[0], y, c[1])) {
                    return null;
                }
                plan.holes.add(new int[] {c[0], y, c[1]});
            }
            if (!headOpen) {
                if (!probe.riftable(c[0], y + 1, c[1])) {
                    return null;
                }
                plan.holes.add(new int[] {c[0], y + 1, c[1]});
            }
            plan.thickness++;
            if (plan.thickness > req.maxThickness || plan.holes.size() > req.maxHoles) {
                return null;
            }
        }
        return null;
    }

    /** The feet level of a standing spot in this column (the line's level, one up, or one down), or MIN_VALUE. */
    private static int standingLevel(Probe probe, int x, int y, int z) {
        if (probe.standable(x, y, z)) {
            return y;
        }
        if (probe.standable(x, y - 1, z)) {
            return y - 1;
        }
        if (probe.standable(x, y + 1, z)) {
            return y + 1;
        }
        return Integer.MIN_VALUE;
    }

    /** The distinct block columns a line passes through, nearest first, starting beyond the mage's own cell. */
    static List<int[]> rayCells(double ox, double oz, double dx, double dz, int reach) {
        List<int[]> cells = new ArrayList<>();
        int lastX = (int) Math.floor(ox);
        int lastZ = (int) Math.floor(oz);
        for (double t = 0.5D; t <= reach; t += 0.5D) {
            int cx = (int) Math.floor(ox + dx * t);
            int cz = (int) Math.floor(oz + dz * t);
            if (cx != lastX || cz != lastZ) {
                cells.add(new int[] {cx, cz});
                lastX = cx;
                lastZ = cz;
            }
        }
        return cells;
    }

    private static double distance(double ax, double az, double bx, double bz) {
        double dx = ax - bx;
        double dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
