package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared village pad height: flatten writes, structure detection reads.
 */
public final class VillagePlate {

    private static final Map<String, Float> HEIGHTS = new ConcurrentHashMap<>();

    private VillagePlate() {}

    public static String key(long seed, int[] box) {
        return seed + ":" + box[0] + "," + box[1] + "," + box[2] + "," + box[3];
    }

    public static void put(long seed, int[] box, float height) {
        if (box == null) return;
        HEIGHTS.put(key(seed, box), height);
    }

    public static Float get(long seed, int[] box) {
        if (box == null) return null;
        return HEIGHTS.get(key(seed, box));
    }

    public static int[] padded(int[] box, int pad) {
        if (box == null) return null;
        if (pad <= 0) return box;
        return new int[] {box[0] - pad, box[1] + pad, box[2] - pad, box[3] + pad};
    }

    public static boolean containsXZ(int x, int z, int[] box) {
        return box != null && x >= box[0] && x <= box[1] && z >= box[2] && z <= box[3];
    }

    public static boolean yInSlab(int y, float plateHeight, int heightAbove) {
        int padY = Math.round(plateHeight);
        int maxY = padY + Math.max(0, heightAbove);
        return y >= padY && y <= maxY;
    }

    /**
     * Cached plate height, or world surface / start minY if this village was never flattened this session.
     */
    public static float resolve(World world, Object start, int[] box) {
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        Float cached = get(seed, box);
        if (cached != null) return cached;

        float height = sampleWorldSurface(world, box);
        if (Float.isNaN(height)) {
            int minY = Reflect.getStructureStartMinY(start);
            height = minY > Integer.MIN_VALUE ? minY : 64.0F;
        }
        put(seed, box, height);
        return height;
    }

    private static float sampleWorldSurface(World world, int[] box) {
        if (world == null || box == null) return Float.NaN;
        int x = (box[0] + box[1]) >> 1;
        int z = (box[2] + box[3]) >> 1;
        try {
            int y = world.getHeight(x, z);
            if (y > 0) return y;
        } catch (Throwable ignored) {}
        return Float.NaN;
    }
}
