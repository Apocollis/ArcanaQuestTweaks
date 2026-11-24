package com.apocollis.aqtweaks.comfort.home;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The shared, cached area behind one or more home records (two beds inside the same build share one area).
 * Server thread only.
 */
public final class HomeArea {
    final int dimension;
    final List<HomeRecord> records = new ArrayList<>();
    HomeRegion region;
    HomeFiller filler;
    boolean dirty;
    long lastBuildStart = Long.MIN_VALUE / 2;
    long builtAt;
    int version;

    /** Comfort's cached furnishing list for this version; opaque to this package. */
    private Object scoreCache;
    private int scoreCacheVersion = -1;

    HomeArea(int dimension) {
        this.dimension = dimension;
    }

    public int dimension() {
        return dimension;
    }

    public HomeRegion region() {
        return region;
    }

    public boolean isReady() {
        return region != null;
    }

    public boolean isBuilding() {
        return filler != null;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int version() {
        return version;
    }

    public long builtAt() {
        return builtAt;
    }

    public List<HomeRecord> records() {
        return records;
    }

    public HomeRecord primary() {
        return records.isEmpty() ? null : records.get(0);
    }

    /** Stable id: the first owner's UUID. */
    public UUID id() {
        HomeRecord primary = primary();
        return primary == null ? new UUID(0L, 0L) : primary.owner;
    }

    public boolean anyHearthkeeper() {
        for (HomeRecord r : records) {
            if (r.hearthkeeper) {
                return true;
            }
        }
        return false;
    }

    public Object scoreCache(int forVersion) {
        return scoreCacheVersion == forVersion ? scoreCache : null;
    }

    public void putScoreCache(int forVersion, Object cache) {
        this.scoreCache = cache;
        this.scoreCacheVersion = forVersion;
    }
}
