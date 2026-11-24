package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Chooses where a Battlemage blinks (plan 3.10 point 1). Pure scoring: no Minecraft types. The world checks (loaded chunk,
 * solid ground, box fits, no liquid or hazard) stay in {@link BattlemageBlink}, which walks this ranked list and takes the
 * first spot that passes them.
 *
 * <p><b>Extension points for later modes</b> (plan 19.8, Bombardier): add a value to {@link Mode} and a branch in
 * {@link #baseScore}; the ring, travel cap, sampling and the {@link Penalty} list are shared. A mode that lands on a band
 * around a target (8 to 14 blocks) sets {@code minRadius}/{@code maxRadius}/{@code preferRadius} on the {@link Request} and
 * puts the target in {@code center}. Hazard avoidance (own clouds as a hard reject, enemy clouds as a soft cost) is a
 * {@link Penalty}: a finite value is subtracted from the score, {@link Double#POSITIVE_INFINITY} rejects the point.
 */
public final class BlinkPicker {

    /** Why the mage blinks; decides the ring and the scoring. */
    public enum Mode {
        /** Land 1 to 2 blocks from an ally the mage wants to heal (plan 18.7). */
        TOWARD_ALLY,
        /**
         * Land on the volley band around the target (plan 19.8): {@code minRadius}/{@code maxRadius} are the band (8 to 14 by
         * default), the ring is centred on the target, and the score likes the middle of the band and a short hop. Cloud
         * avoidance and line of sight arrive as {@link Penalty} terms.
         */
        REPOSITION_BAND,
        /**
         * The strict defensive escape (plan 3.10): the ring is centred on the target and lies far out, so every candidate is
         * farther from the player than the mage is now; scored like the band, with an exposure penalty added by the caller.
         */
        AWAY
    }

    /** An extra scoring term (for example a cloud-avoidance cost). */
    public interface Penalty {
        /** @return a cost subtracted from the score (0 for none), or positive infinity to reject the point */
        double penalty(double x, double y, double z);
    }

    /** What to sample around, and how. */
    public static final class Request {
        public Mode mode = Mode.TOWARD_ALLY;
        /** Where the mage stands now. */
        public double fromX, fromY, fromZ;
        /** The point the ring is centred on: the ally for {@code TOWARD_ALLY}, the target for band modes. */
        public double centerX, centerY, centerZ;
        public double minRadius = 1.0D;
        public double maxRadius = 2.0D;
        /** The radius the score likes best; NaN means the middle of the ring. */
        public double preferRadius = Double.NaN;
        /** Candidates farther than this from {@code from} are dropped. */
        public double maxTravel = 24.0D;
        public int samples = 16;
        public final List<Penalty> penalties = new ArrayList<>();
    }

    /** A candidate landing point (feet position before the world checks walk it down to the ground). */
    public static final class Candidate {
        public final double x;
        public final double y;
        public final double z;
        public final double score;

        public Candidate(double x, double y, double z, double score) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.score = score;
        }
    }

    private BlinkPicker() {}

    /**
     * Samples {@code req.samples} points on the ring, scores them, and returns the best {@code limit}, best first. Evenly
     * spaced angles with a random jitter and a random radius, so repeated calls differ but cover the ring.
     */
    public static List<Candidate> rank(Request req, Random rnd, int limit) {
        List<Candidate> all = new ArrayList<>();
        int n = Math.max(1, req.samples);
        double minR = Math.max(0.0D, Math.min(req.minRadius, req.maxRadius));
        double maxR = Math.max(req.minRadius, req.maxRadius);
        double prefer = Double.isNaN(req.preferRadius) ? (minR + maxR) * 0.5D : req.preferRadius;
        for (int i = 0; i < n; i++) {
            double angle = (i + rnd.nextDouble()) * (2.0D * Math.PI / n);
            double radius = minR + rnd.nextDouble() * (maxR - minR);
            double x = req.centerX + Math.cos(angle) * radius;
            double z = req.centerZ + Math.sin(angle) * radius;
            double y = req.centerY;
            double travel = distance(req.fromX, req.fromY, req.fromZ, x, y, z);
            if (travel > req.maxTravel) {
                continue;
            }
            double score = baseScore(req, radius, prefer, travel);
            boolean rejected = false;
            for (Penalty p : req.penalties) {
                double cost = p.penalty(x, y, z);
                if (Double.isInfinite(cost) && cost > 0.0D) {
                    rejected = true;
                    break;
                }
                score -= cost;
            }
            if (!rejected) {
                all.add(new Candidate(x, y, z, score));
            }
        }
        Collections.sort(all, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                return Double.compare(b.score, a.score);
            }
        });
        if (limit > 0 && all.size() > limit) {
            return new ArrayList<>(all.subList(0, limit));
        }
        return all;
    }

    /** The mode's own scoring, before the penalties. Higher is better. */
    static double baseScore(Request req, double radius, double prefer, double travel) {
        switch (req.mode) {
            case AWAY:
            case REPOSITION_BAND:
                // The middle of the band is the best firing distance; a shorter hop is slightly better than a long one.
                return -Math.abs(radius - prefer) * 0.6D - travel * 0.04D;
            case TOWARD_ALLY:
            default:
                // Close to the preferred radius (inside the Healing Aura's reach) and a short hop for the mage.
                return -Math.abs(radius - prefer) * 2.0D - travel * 0.05D;
        }
    }

    static double distance(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
