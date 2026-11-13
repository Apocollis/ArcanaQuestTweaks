package com.apocollis.aqtweaks.hazard;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client copy of the events the server announced, per dimension and channel. Plain statics, any side may read. */
public final class ClientHazards {
    private ClientHazards() {}

    private static final Map<Long, HazardEvent> EVENTS = new ConcurrentHashMap<>();

    private static long key(int dimension, int channel) {
        return ((long) dimension << 32) ^ (channel & 0xFFFFFFFFL);
    }

    public static void accept(int dimension, HazardEvent event) {
        EVENTS.put(key(dimension, event.channel), event);
    }

    public static HazardEvent event(int dimension, int channel) {
        return EVENTS.get(key(dimension, channel));
    }

    public static void reset() {
        EVENTS.clear();
    }
}
