package com.apocollis.aqtweaks.aether;

import java.util.ArrayList;
import java.util.List;

/** Client list of lightning strikes in their telegraph. Plain statics; the effects class ticks and renders them. */
public final class ClientBolts {
    private ClientBolts() {}

    public static final class Bolt {
        public final int x;
        public final int y;
        public final int z;
        public final int total;
        public int remaining;

        Bolt(int x, int y, int z, int ticks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.total = Math.max(1, ticks);
            this.remaining = ticks;
        }
    }

    private static final List<Bolt> BOLTS = new ArrayList<>();

    public static synchronized void add(int x, int y, int z, int ticks) {
        BOLTS.add(new Bolt(x, y, z, ticks));
    }

    public static synchronized List<Bolt> snapshot() {
        return new ArrayList<>(BOLTS);
    }

    public static synchronized void removeFinished() {
        BOLTS.removeIf(b -> b.remaining < -2);
    }

    public static synchronized void clear() {
        BOLTS.clear();
    }
}
