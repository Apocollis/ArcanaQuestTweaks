package com.apocollis.aqtweaks.starter;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The pure membership behind {@link StarterLockdown}: who is held, and where. No Minecraft types. */
final class LockdownSet {

    private final Set<UUID> locked = ConcurrentHashMap.newKeySet();
    private final Map<UUID, double[]> anchors = new ConcurrentHashMap<>();

    void lock(UUID id, double x, double y, double z) {
        locked.add(id);
        anchors.put(id, new double[] {x, y, z});
    }

    void unlock(UUID id) {
        locked.remove(id);
        anchors.remove(id);
    }

    boolean contains(UUID id) {
        return id != null && locked.contains(id);
    }

    double[] anchor(UUID id) {
        return anchors.get(id);
    }

    int size() {
        return locked.size();
    }

    void clear() {
        locked.clear();
        anchors.clear();
    }
}
