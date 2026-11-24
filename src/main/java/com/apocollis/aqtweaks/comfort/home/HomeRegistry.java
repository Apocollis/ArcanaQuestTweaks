package com.apocollis.aqtweaks.comfort.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Server-side registry of player homes. A home is established automatically when a player sleeps in a bed or
 * sets their spawn there; its region is a cached flood fill of the build (box fallback for open-air or
 * oversized builds), rebuilt lazily after a restart or a block change.
 *
 * <p>{@link #structureAt} is the single entry point for the vanilla structure adapter, the titles feature
 * and spawn rules. Server thread only.
 */
public final class HomeRegistry {

    private static final Map<UUID, HomeRecord> RECORDS = new LinkedHashMap<>();
    private static final List<HomeArea> AREAS = new ArrayList<>();
    private static HomeSavedData saved;

    private HomeRegistry() {}

    // ----- lifecycle -------------------------------------------------------------------------------------

    /** Loads the persisted anchors; call when the overworld loads on the server. */
    public static void load(World overworld) {
        clear();
        saved = HomeSavedData.get(overworld);
        if (saved == null) {
            return;
        }
        for (HomeRecord record : new ArrayList<>(saved.loaded)) {
            attach(record, null);
        }
    }

    public static void clear() {
        RECORDS.clear();
        AREAS.clear();
        saved = null;
    }

    static List<HomeRecord> allRecords() {
        return new ArrayList<>(RECORDS.values());
    }

    public static List<HomeArea> areas() {
        return Collections.unmodifiableList(AREAS);
    }

    private static void save() {
        if (saved != null) {
            saved.markDirty();
        }
    }

    // ----- establish / remove ----------------------------------------------------------------------------

    /** Idempotent. Sets (or moves) the player's home to the bed at {@code bed}. */
    public static HomeRecord establish(World world, EntityPlayer owner, BlockPos bed) {
        if (!HomeConfig.enabled || world == null || world.isRemote) {
            return null;
        }
        int dim = world.provider.getDimension();
        HomeRecord existing = RECORDS.get(owner.getUniqueID());
        if (existing != null && existing.dimension == dim && existing.bed.equals(bed)) {
            if (!existing.ownerName.equals(owner.getName())) {
                existing.ownerName = owner.getName();
                save();
            }
            return existing;
        }
        if (existing != null) {
            detach(existing);
        }
        HomeRecord record = new HomeRecord(owner.getUniqueID(), owner.getName(), dim, bed,
                com.apocollis.aqtweaks.comfort.HearthkeeperHooks.keeper(owner));
        HomeArea join = null;
        for (HomeArea area : AREAS) {
            HomeRegion region = area.region;
            if (area.dimension == dim && region != null && !region.isBox()
                    && region.contains(bed.getX(), bed.getY() + 1, bed.getZ())) {
                join = area;
                break;
            }
        }
        attach(record, join);
        save();
        return record;
    }

    /** The owner set their spawn elsewhere (or the bed is gone): drop their home. */
    public static void removeOwner(UUID owner) {
        HomeRecord record = RECORDS.get(owner);
        if (record != null) {
            detach(record);
            save();
        }
    }

    public static void refreshName(EntityPlayer player) {
        HomeRecord record = RECORDS.get(player.getUniqueID());
        if (record != null && !record.ownerName.equals(player.getName())) {
            record.ownerName = player.getName();
            save();
        }
    }

    /** Set by the Hearthkeeper perk (HearthkeeperHooks.syncFlag on the comfort pass, StarterReward on grant); also read at establish. */
    public static void setHearthkeeper(UUID owner, boolean value) {
        HomeRecord record = RECORDS.get(owner);
        if (record != null && record.hearthkeeper != value) {
            record.hearthkeeper = value;
            save();
        }
    }

    public static HomeRecord recordOf(UUID owner) {
        return RECORDS.get(owner);
    }

    private static void attach(HomeRecord record, HomeArea join) {
        RECORDS.put(record.owner, record);
        HomeArea area = join;
        if (area == null) {
            area = new HomeArea(record.dimension);
            AREAS.add(area);
        }
        area.records.add(record);
        record.area = area;
    }

    private static void detach(HomeRecord record) {
        RECORDS.remove(record.owner);
        HomeArea area = record.area;
        record.area = null;
        if (area == null) {
            return;
        }
        area.records.remove(record);
        if (area.records.isEmpty()) {
            AREAS.remove(area);
        } else if (area.region != null && area.region.isBox()) {
            area.dirty = true; // the box was centred on a bed that is gone
        }
    }

    // ----- change tracking -------------------------------------------------------------------------------

    /** Marks every home whose bounds touch {@code pos} dirty. Bounding-box test first, so this is cheap. */
    public static void markDirty(World world, BlockPos pos) {
        if (AREAS.isEmpty() || world == null || world.isRemote) {
            return;
        }
        int dim = world.provider.getDimension();
        for (HomeArea area : AREAS) {
            if (area.dimension == dim && area.region != null
                    && area.region.affectedBy(pos.getX(), pos.getY(), pos.getZ())) {
                area.dirty = true;
            }
        }
    }

    /** Forces a rebuild now (operator command), ignoring the cooldown. */
    public static boolean rescan(World world, HomeArea area) {
        if (area == null || area.dimension != world.provider.getDimension()) {
            return false;
        }
        area.dirty = true;
        area.lastBuildStart = Long.MIN_VALUE / 2;
        return startBuild(world, area);
    }

    // ----- queries ---------------------------------------------------------------------------------------

    /** The ready area that contains the player's feet or head block, preferring a precise region over a box. */
    public static HomeArea areaAtPlayer(EntityPlayer player) {
        if (AREAS.isEmpty() || player == null || player.world == null || player.world.isRemote) {
            return null;
        }
        int x = (int) Math.floor(player.posX);
        int y = (int) Math.floor(player.getEntityBoundingBox().minY + 1.0E-3D);
        int z = (int) Math.floor(player.posZ);
        return areaAt(player.world.provider.getDimension(), x, y, z, true);
    }

    private static HomeArea areaAt(int dim, int x, int y, int z, boolean occupy) {
        HomeArea box = null;
        for (HomeArea area : AREAS) {
            HomeRegion region = area.region;
            if (area.dimension != dim || region == null) {
                continue;
            }
            boolean in = occupy ? region.occupies(x, y, z) : region.contains(x, y, z);
            if (!in) {
                continue;
            }
            if (!region.isBox()) {
                return area;
            }
            if (box == null) {
                box = area;
            }
        }
        return box;
    }

    /** Structure-shaped record of the home at {@code pos}, or null. The one lookup other features use. */
    public static HomeStructure structureAt(World world, BlockPos pos) {
        if (AREAS.isEmpty() || world == null || world.isRemote) {
            return null;
        }
        HomeArea area = areaAt(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), false);
        if (area == null || area.primary() == null) {
            return null;
        }
        return new HomeStructure(area, area.primary());
    }

    // ----- building --------------------------------------------------------------------------------------

    /** Called from the comfort state pass for each player (every 40 ticks). */
    public static void touch(EntityPlayer player) {
        if (AREAS.isEmpty() || !HomeConfig.enabled) {
            return;
        }
        World world = player.world;
        int dim = world.provider.getDimension();
        long now = world.getTotalWorldTime();
        for (HomeArea area : new ArrayList<>(AREAS)) {
            if (area.dimension != dim || area.filler != null || area.records.isEmpty()) {
                continue;
            }
            if (!near(player, area)) {
                continue;
            }
            if (area.region == null) {
                startBuild(world, area);
            } else if (area.dirty && now - area.lastBuildStart >= HomeConfig.rebuildCooldownTicks) {
                startBuild(world, area);
            } else if (now - area.builtAt >= HomeConfig.recheckTicks()) {
                area.dirty = true;
                startBuild(world, area);
            }
        }
    }

    private static boolean near(EntityPlayer player, HomeArea area) {
        int px = (int) Math.floor(player.posX);
        int py = (int) Math.floor(player.posY);
        int pz = (int) Math.floor(player.posZ);
        if (area.region != null && area.region.boundsContain(px, py, pz, 4)) {
            return true;
        }
        BlockPos bed = area.records.get(0).bed;
        int range = Math.max(1, HomeConfig.triggerRange);
        return Math.abs(px - bed.getX()) <= range && Math.abs(pz - bed.getZ()) <= range
                && Math.abs(py - bed.getY()) <= 16;
    }

    private static boolean startBuild(World world, HomeArea area) {
        if (area.filler != null || area.records.isEmpty()) {
            return false;
        }
        // Drop records whose bed is gone. An unloaded bed is left alone and retried on the next touch.
        for (HomeRecord record : new ArrayList<>(area.records)) {
            if (!world.isBlockLoaded(record.bed)) {
                continue;
            }
            if (!(world.getBlockState(record.bed).getBlock() instanceof BlockBed)) {
                detach(record);
                save();
            }
        }
        if (area.records.isEmpty()) {
            return false;
        }
        BlockPos bed = area.records.get(0).bed;
        if (!world.isBlockLoaded(bed)) {
            return false;
        }
        area.dirty = false;
        area.lastBuildStart = world.getTotalWorldTime();
        area.filler = new HomeFiller(new WorldProbe(world), bed.getX(), bed.getY(), bed.getZ(),
                HomeConfig.maxCells, HomeConfig.fallbackRadius, HomeConfig.fallbackDown, HomeConfig.fallbackUp,
                HomeConfig.doorSettings());
        return true;
    }

    /** Advances running fills; call once per world tick on the server. */
    public static void tick(World world) {
        if (AREAS.isEmpty()) {
            return;
        }
        int dim = world.provider.getDimension();
        for (HomeArea area : new ArrayList<>(AREAS)) {
            if (area.dimension != dim || area.filler == null) {
                continue;
            }
            if (area.filler.step(HomeConfig.fillCellsPerTick)) {
                finish(world, area);
            }
        }
    }

    private static void finish(World world, HomeArea area) {
        area.region = area.filler.result();
        area.filler = null;
        area.version++;
        area.builtAt = world.getTotalWorldTime();
        if (area.region.isBox()) {
            // visible in /aqhome info; the reason says why the build fell back
        } else {
            absorbOverlapping(area);
        }
    }

    /** A second bed that sits inside this filled region belongs to the same home: merge its record in. */
    private static void absorbOverlapping(HomeArea area) {
        for (HomeArea other : new ArrayList<>(AREAS)) {
            if (other == area || other.dimension != area.dimension) {
                continue;
            }
            boolean inside = false;
            for (HomeRecord r : other.records) {
                if (area.region.contains(r.bed.getX(), r.bed.getY() + 1, r.bed.getZ())) {
                    inside = true;
                    break;
                }
            }
            if (!inside) {
                continue;
            }
            for (HomeRecord r : other.records) {
                r.area = area;
                area.records.add(r);
            }
            other.records.clear();
            other.filler = null;
            AREAS.remove(other);
        }
    }

    /** True when any home exists in the dimension (fast exit for hot paths). */
    public static boolean hasHomes(int dimension) {
        for (HomeArea area : AREAS) {
            if (area.dimension == dimension) {
                return true;
            }
        }
        return false;
    }
}
