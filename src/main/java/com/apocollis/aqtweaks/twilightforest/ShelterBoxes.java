package com.apocollis.aqtweaks.twilightforest;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Finds the Shelter Features structures around each player, for the client's particles to stop at. Twilight Forest
 * has no list of structure boxes, so each player's surroundings are sampled on a 16 block grid with
 * {@link LeyTerrain#shelterBoxNear}, at most once every 5 seconds (and when the player moves into a new group of four
 * chunks), skipping grid points already inside a known box. Boxes never move, so they are cached for the world and
 * only a changed list is sent to the player. Exposure and damage keep using the exact {@code structureShelter} check.
 */
public final class ShelterBoxes {
    /** Ticks between samples for one player. */
    public static final long INTERVAL_TICKS = 100L;
    private static final int STEP = 16;

    private final Map<Long, int[]> known = new HashMap<>();
    private final Map<UUID, Long> lastSample = new HashMap<>();
    private final Map<UUID, Long> lastRegion = new HashMap<>();
    private final Map<UUID, int[]> lastSent = new HashMap<>();

    /** The box list to send to this player if it changed since the last send, else null. Cheap when called every second. */
    public int[] refresh(WorldServer world, EntityPlayerMP player, long now, int radius) {
        UUID id = player.getUniqueID();
        long region = (((long) ((int) Math.floor(player.posX) >> 6)) << 32)
                | (((long) ((int) Math.floor(player.posZ) >> 6)) & 0xFFFFFFFFL);
        Long last = lastSample.get(id);
        Long lastReg = lastRegion.get(id);
        if (last != null && now - last < INTERVAL_TICKS && lastReg != null && lastReg == region) return null;
        lastSample.put(id, now);
        lastRegion.put(id, region);

        int px = (int) Math.floor(player.posX);
        int pz = (int) Math.floor(player.posZ);
        int ox = Math.floorDiv(px, STEP) * STEP + STEP / 2;
        int oz = Math.floorDiv(pz, STEP) * STEP + STEP / 2;
        for (int dx = -radius; dx <= radius; dx += STEP) {
            for (int dz = -radius; dz <= radius; dz += STEP) {
                int x = ox + dx;
                int z = oz + dz;
                if (insideKnown(x, z)) continue;
                int[] box = LeyTerrain.shelterBoxNear(world, x, z);
                if (box != null) known.put(key(box), box);
            }
        }

        List<int[]> near = new ArrayList<>();
        for (int[] box : known.values()) {
            if (box[3] >= px - radius && box[0] <= px + radius && box[5] >= pz - radius && box[2] <= pz + radius) {
                near.add(box);
            }
        }
        near.sort((a, b) -> Long.compare(key(a), key(b)));
        int[] flat = new int[near.size() * 6];
        for (int i = 0; i < near.size(); i++) {
            System.arraycopy(near.get(i), 0, flat, i * 6, 6);
        }
        int[] before = lastSent.get(id);
        if (before != null && Arrays.equals(before, flat)) return null;
        if (before == null && flat.length == 0) {
            lastSent.put(id, flat);
            return null;
        }
        lastSent.put(id, flat);
        return flat;
    }

    private boolean insideKnown(int x, int z) {
        for (int[] b : known.values()) {
            if (x >= b[0] && x <= b[3] && z >= b[2] && z <= b[5]) return true;
        }
        return false;
    }

    private static long key(int[] box) {
        return (((long) box[0]) << 32) ^ (box[2] & 0xFFFFFFFFL);
    }

    public void forget(UUID id) {
        lastSample.remove(id);
        lastRegion.remove(id);
        lastSent.remove(id);
    }

    public void clear() {
        known.clear();
        lastSample.clear();
        lastRegion.clear();
        lastSent.clear();
    }
}
