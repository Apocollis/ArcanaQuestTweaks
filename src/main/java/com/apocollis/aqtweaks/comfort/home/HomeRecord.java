package com.apocollis.aqtweaks.comfort.home;

import java.util.UUID;
import net.minecraft.util.math.BlockPos;

/** One player's home: the bed that anchors it. Persisted (see {@link HomeSavedData}). */
public final class HomeRecord {
    public final UUID owner;
    public String ownerName;
    public final int dimension;
    public final BlockPos bed;
    /** Set when the owner holds the Homesteader perk; refreshed by the perk when it exists. */
    public boolean hearthkeeper;
    HomeArea area;

    public HomeRecord(UUID owner, String ownerName, int dimension, BlockPos bed, boolean hearthkeeper) {
        this.owner = owner;
        this.ownerName = ownerName == null ? "" : ownerName;
        this.dimension = dimension;
        this.bed = bed.toImmutable();
        this.hearthkeeper = hearthkeeper;
    }

    public HomeArea area() {
        return area;
    }
}
