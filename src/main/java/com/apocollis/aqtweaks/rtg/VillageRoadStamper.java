package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * After village paste, lay appropriate paths or gravel on road columns whose surface is a configured
 * grass or dirt block.
 * - BOP loamy grass/dirt -> biomesoplenty:grass_path[variant=loamy]
 * - BOP sandy grass/dirt -> biomesoplenty:grass_path[variant=sandy]
 * - BOP silty grass/dirt -> biomesoplenty:grass_path[variant=silty]
 * - Vanilla grass/dirt -> Blocks.GRASS_PATH
 * - Vanilla sand/sandstone -> Blocks.GRAVEL
 * - Configured blocks in villageRoadGravelBlocks -> Blocks.GRAVEL
 */
public final class VillageRoadStamper {

    private static String[] cachedSource;
    private static Set<String> cachedIds = new HashSet<String>();

    private static Block bopGrassPathBlock;
    private static boolean bopGrassPathChecked = false;

    private VillageRoadStamper() {}

    public static void afterVillagePaste(World world, StructureStart start, StructureBoundingBox clip) {
        if (world == null || world.isRemote || start == null || clip == null) {
            return;
        }
        List<StructureComponent> components = start.getComponents();
        if (components == null || components.isEmpty()) return;

        int stamped = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (StructureComponent component : components) {
            if (component == null || !VillageLandHelper.isVillageRoad(component)) continue;
            StructureBoundingBox bb = component.getBoundingBox();
            if (bb == null) continue;
            if (bb.maxX < clip.minX || bb.minX > clip.maxX || bb.maxZ < clip.minZ || bb.minZ > clip.maxZ) {
                continue;
            }
            int minX = Math.max(bb.minX, clip.minX);
            int maxX = Math.min(bb.maxX, clip.maxX);
            int minZ = Math.max(bb.minZ, clip.minZ);
            int maxZ = Math.min(bb.maxZ, clip.maxZ);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (stampColumn(world, pos, x, z)) {
                        stamped++;
                    }
                }
            }
        }
        if (stamped > 0 && VillageDebug.enabled()) {
            VillageDebug.log("road-stamp clip=[%d,%d]x[%d,%d] columns=%d",
                    clip.minX, clip.maxX, clip.minZ, clip.maxZ, stamped);
        }
    }

    private static boolean stampColumn(World world, BlockPos.MutableBlockPos pos, int x, int z) {
        int surfaceY = world.getHeight(x, z);
        if (surfaceY < 1) return false;
        int top = Math.min(surfaceY + 4, 255);
        int bottom = Math.max(1, surfaceY - 32);

        for (int y = top; y >= bottom; y--) {
            pos.setPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == Blocks.AIR || state.getMaterial() == Material.AIR) {
                continue;
            }
            if (isPassableFoliage(state)) {
                continue;
            }
            if (state.getMaterial().isLiquid()) {
                break;
            }

            if (isRoadSurface(state)) {
                break;
            }

            IBlockState replacement = resolvePathState(state);
            if (replacement != null) {
                world.setBlockState(pos, replacement, 2);
                pos.setPos(x, y + 1, z);
                IBlockState above = world.getBlockState(pos);
                if (isPassableFoliage(above)) {
                    world.setBlockToAir(pos);
                    pos.setPos(x, y + 2, z);
                    if (world.getBlockState(pos).getBlock() == Blocks.DOUBLE_PLANT) {
                        world.setBlockToAir(pos);
                    }
                }
                return true;
            }

            break;
        }
        return false;
    }

    public static IBlockState resolvePathState(IBlockState surfaceState) {
        if (surfaceState == null || surfaceState.getBlock() == null) return null;
        Block block = surfaceState.getBlock();
        ResourceLocation reg = block.getRegistryName();
        if (reg == null) return null;

        String domain = reg.getNamespace();
        String path = reg.getPath();

        // 1. Biomes O' Plenty grass and dirt variants
        if ("biomesoplenty".equals(domain)) {
            if ("grass".equals(path) || "dirt".equals(path)) {
                String variant = getVariantName(surfaceState);
                if (variant != null) {
                    IBlockState bopPath = getBopGrassPathState(variant);
                    if (bopPath != null) {
                        return bopPath;
                    }
                }
            }
        }

        // 2. Vanilla grass or dirt
        if (block == Blocks.GRASS || block == Blocks.DIRT) {
            return Blocks.GRASS_PATH.getDefaultState();
        }

        // 3. Sand or sandstone -> gravel
        if (block == Blocks.SAND || block == Blocks.SANDSTONE) {
            return Blocks.GRAVEL.getDefaultState();
        }

        // 4. Configured gravel blocks (covers unmapped BOP dirt/grass variants or custom IDs)
        if (matches(surfaceState)) {
            return Blocks.GRAVEL.getDefaultState();
        }

        return null;
    }

    public static boolean isRoadSurface(IBlockState state) {
        if (state == null || state.getBlock() == null) return false;
        Block b = state.getBlock();
        if (b == Blocks.GRASS_PATH || b == Blocks.GRAVEL || b == Blocks.COBBLESTONE
                || b == Blocks.STONEBRICK || b == Blocks.PLANKS
                || b == Blocks.DOUBLE_STONE_SLAB || b == Blocks.STONE_SLAB) {
            return true;
        }
        return isBopGrassPath(b);
    }

    public static boolean isBopGrassPath(Block block) {
        if (block == null) return false;
        Block bopPath = getBopGrassPathBlock();
        if (bopPath != null && block == bopPath) return true;
        ResourceLocation reg = block.getRegistryName();
        return reg != null && "biomesoplenty".equals(reg.getNamespace()) && "grass_path".equals(reg.getPath());
    }

    public static Block getBopGrassPathBlock() {
        if (!bopGrassPathChecked) {
            bopGrassPathBlock = Block.getBlockFromName("biomesoplenty:grass_path");
            bopGrassPathChecked = true;
        }
        return bopGrassPathBlock;
    }

    public static IBlockState getBopGrassPathState(String variant) {
        Block bopPath = getBopGrassPathBlock();
        if (bopPath == null || variant == null) return null;
        int meta = -1;
        if ("loamy".equals(variant)) meta = 0;
        else if ("sandy".equals(variant)) meta = 1;
        else if ("silty".equals(variant)) meta = 2;

        if (meta >= 0) {
            try {
                return bopPath.getStateFromMeta(meta);
            } catch (Throwable ignored) {}
        }
        for (IBlockState state : bopPath.getBlockState().getValidStates()) {
            String v = getVariantName(state);
            if (variant.equalsIgnoreCase(v)) {
                return state;
            }
        }
        return bopPath.getDefaultState();
    }

    public static String getVariantName(IBlockState state) {
        if (state == null) return null;
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
            if ("variant".equalsIgnoreCase(entry.getKey().getName())) {
                Comparable<?> val = entry.getValue();
                if (val instanceof IStringSerializable) {
                    return ((IStringSerializable) val).getName().toLowerCase(Locale.ROOT);
                }
                if (val != null) {
                    return val.toString().toLowerCase(Locale.ROOT);
                }
            }
        }
        return null;
    }

    public static boolean isPassableFoliage(IBlockState state) {
        if (state == null) return false;
        Block block = state.getBlock();
        if (block == null || block == Blocks.AIR) return false;
        if (block == Blocks.TALLGRASS || block == Blocks.DOUBLE_PLANT
                || block == Blocks.YELLOW_FLOWER || block == Blocks.RED_FLOWER
                || block == Blocks.DEADBUSH || block == Blocks.SNOW_LAYER) {
            return true;
        }
        Material mat = state.getMaterial();
        return mat == Material.PLANTS || mat == Material.VINE || mat == Material.SNOW;
    }

    public static boolean matches(IBlockState state) {
        if (state == null || state.getBlock() == null) return false;
        String[] source = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageRoadGravelBlocks;
        if (source != cachedSource) {
            cachedSource = source;
            Set<String> next = new HashSet<String>();
            if (source != null) {
                for (String id : source) {
                    if (id != null && !id.isEmpty()) {
                        next.add(id.toLowerCase(Locale.ROOT));
                    }
                }
            }
            cachedIds = next;
        }
        ResourceLocation name = state.getBlock().getRegistryName();
        return name != null && cachedIds.contains(name.toString().toLowerCase(Locale.ROOT));
    }
}
