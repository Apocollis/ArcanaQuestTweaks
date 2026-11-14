package com.apocollis.aqtweaks.nether;

import java.util.ArrayList;
import java.util.List;

/** Client list of spouts in their telegraph and of lava landings in their warning. Plain statics; the effects class ticks and renders them. */
public final class ClientSpouts {
    private ClientSpouts() {}

    public static final class Spout {
        public final int x;
        public final int y;
        public final int z;
        public int remaining;

        Spout(int x, int y, int z, int remaining) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.remaining = remaining;
        }
    }

    /** One spot where thrown lava is about to land. */
    public static final class Landing {
        public final int x;
        public final int y;
        public final int z;
        public final int total;
        public int remaining;

        Landing(int x, int y, int z, int total) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.total = Math.max(1, total);
            this.remaining = this.total;
        }
    }

    private static final List<Spout> SPOUTS = new ArrayList<>();
    private static final List<Landing> LANDINGS = new ArrayList<>();

    public static synchronized void add(int x, int y, int z, int ticks) {
        SPOUTS.add(new Spout(x, y, z, ticks));
    }

    public static synchronized List<Spout> snapshot() {
        return new ArrayList<>(SPOUTS);
    }

    public static synchronized void removeFinished() {
        SPOUTS.removeIf(s -> s.remaining < -2);
        LANDINGS.removeIf(l -> l.remaining < -2);
    }

    /** {@code coords} is x, y, z for each landing, back to back. */
    public static synchronized void addLandings(int[] coords, int ticks) {
        for (int i = 0; i + 2 < coords.length; i += 3) {
            LANDINGS.add(new Landing(coords[i], coords[i + 1], coords[i + 2], ticks));
        }
    }

    public static synchronized List<Landing> landings() {
        return new ArrayList<>(LANDINGS);
    }

    public static synchronized void clear() {
        SPOUTS.clear();
        LANDINGS.clear();
    }
}
