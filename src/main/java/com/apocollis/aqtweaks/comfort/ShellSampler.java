package com.apocollis.aqtweaks.comfort;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Tags the shell around the player. Roles are a decay key and a discount, never a gate.
 * Roof, wall, window and floor are the blocks the rays and the foot sample hit.
 */
public final class ShellSampler {

    public static final byte ROOF = 1;
    public static final byte WALL = 2;
    public static final byte WINDOW = 3;
    public static final byte FLOOR = 4;

    private ShellSampler() {}

    public static Map<Long, Byte> sample(EntityPlayer player) {
        Map<Long, Byte> hits = new HashMap<>();
        World world = player.world;
        if (world == null) return hits;
        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;
        double eye = player.getEyeHeight();
        double[][] offsets = {{0, 0}, {0.4, 0}, {-0.4, 0}, {0, 0.4}, {0, -0.4}};
        for (double[] offset : offsets) {
            ray(world, hits, x + offset[0], y + eye, z + offset[1], 0, 1, 0, ROOF);
        }
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            ray(world, hits, x, y + 0.2, z, dx, 0, dz, WALL);
            ray(world, hits, x, y + eye, z, dx, 0, dz, WALL);
        }
        BlockPos feet = new BlockPos(player.posX, player.posY, player.posZ);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                markFloor(world, hits, feet.add(dx, -1, dz));
                BlockPos standing = feet.add(dx, 0, dz);
                if (isCarpet(world, standing)) {
                    put(hits, standing, FLOOR);
                }
            }
        }
        return hits;
    }

    public static String roleName(byte role) {
        switch (role) {
            case ROOF: return "roof";
            case WALL: return "wall";
            case WINDOW: return "window";
            case FLOOR: return "floor";
            default: return "other";
        }
    }

    private static void ray(World world, Map<Long, Byte> hits, double x, double y, double z,
            double dx, double dy, double dz, byte role) {
        for (int step = 1; step <= 20; step++) {
            double dist = step * 0.5;
            BlockPos pos = new BlockPos(x + dx * dist, y + dy * dist, z + dz * dist);
            if (!world.isBlockLoaded(pos)) return;
            IBlockState state = world.getBlockState(pos);
            if (state.getMaterial() == Material.AIR || state.getMaterial().isLiquid()) continue;
            byte tagged = isGlass(state) ? WINDOW : role;
            put(hits, pos, tagged);
            return;
        }
    }

    private static void markFloor(World world, Map<Long, Byte> hits, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) return;
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() == Material.AIR || state.getMaterial().isLiquid()) return;
        put(hits, pos, FLOOR);
    }

    private static boolean isCarpet(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) return false;
        ResourceLocation name = world.getBlockState(pos).getBlock().getRegistryName();
        return name != null && name.getPath().contains("carpet");
    }

    private static boolean isGlass(IBlockState state) {
        ResourceLocation name = state.getBlock().getRegistryName();
        if (name == null) return false;
        String path = name.getPath();
        return path.contains("glass") || path.contains("pane");
    }

    private static void put(Map<Long, Byte> hits, BlockPos pos, byte role) {
        long key = pos.toLong();
        Byte previous = hits.get(key);
        if (previous == null || role == WINDOW) {
            hits.put(key, role);
        }
    }
}
