package com.apocollis.aqtweaks.comfort.home;

import java.util.UUID;

/**
 * Structure-shaped view of a home, the one record a display feature (titles) or a spawn rule reads.
 * Type is always {@code home}; the display name is the formatted owner name.
 */
public final class HomeStructure {
    public final UUID id;
    public final String type;
    public final String displayName;
    public final UUID ownerUuid;
    public final String ownerName;
    public final int dimension;
    public final int minX;
    public final int minY;
    public final int minZ;
    public final int maxX;
    public final int maxY;
    public final int maxZ;
    public final boolean hearthkeeper;
    public final boolean box;

    HomeStructure(HomeArea area, HomeRecord owner) {
        HomeRegion region = area.region();
        this.id = area.id();
        this.type = HomeNames.TYPE;
        this.ownerUuid = owner.owner;
        this.ownerName = owner.ownerName;
        this.displayName = HomeNames.format(HomeConfig.nameFormat, owner.ownerName);
        this.dimension = area.dimension();
        this.minX = region.minX();
        this.minY = region.minY();
        this.minZ = region.minZ();
        this.maxX = region.maxX();
        this.maxY = region.maxY();
        this.maxZ = region.maxZ();
        this.hearthkeeper = area.anyHearthkeeper();
        this.box = region.isBox();
    }
}
