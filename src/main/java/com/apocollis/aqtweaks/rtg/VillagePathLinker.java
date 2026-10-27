package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;

/**
 * After village paste, joins a road that ends in the open to the nearest other road or the well, so a player can walk
 * through the village without leaving a path. A one-wide trodden path (grass path on grass or dirt, gravel on sand or
 * the configured BOP blocks) is laid along an L from the road end. It never crosses a village piece, water, or a
 * column outside the pasted clip, and it is a pure function of the kept pieces, so every clip draws the same line.
 */
public final class VillagePathLinker {

    private VillagePathLinker() {}

    public static void afterVillagePaste(World world, StructureStart start, StructureBoundingBox clip) {
        int gap = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villagePathLinkGap;
        if (gap <= 0 || world == null || world.isRemote || start == null || clip == null) return;
        List<StructureComponent> components = start.getComponents();
        if (components == null || components.size() < 2) return;

        List<StructureBoundingBox> roads = new ArrayList<StructureBoundingBox>();
        List<StructureBoundingBox> targets = new ArrayList<StructureBoundingBox>();
        List<StructureBoundingBox> occupied = new ArrayList<StructureBoundingBox>();
        for (StructureComponent component : components) {
            if (component == null || component.getBoundingBox() == null) continue;
            StructureBoundingBox bb = component.getBoundingBox();
            boolean road = VillageLandHelper.isVillageRoad(component);
            if (road) {
                roads.add(bb);
                targets.add(bb);
            } else if (VillageLandHelper.isVillageWellOrStart(component)) {
                targets.add(bb);
            }
            if (!road) occupied.add(bb);
        }
        if (roads.isEmpty()) return;

        int placed = 0;
        for (StructureBoundingBox road : roads) {
            boolean alongX = road.maxX - road.minX >= road.maxZ - road.minZ;
            for (int sign = -1; sign <= 1; sign += 2) {
                placed += linkEnd(world, road, alongX, sign, targets, occupied, clip, gap);
            }
        }
        if (placed > 0 && VillageDebug.enabled()) {
            VillageDebug.log("path-link clip=[%d,%d]x[%d,%d] columns=%d",
                    clip.minX, clip.maxX, clip.minZ, clip.maxZ, placed);
        }
    }

    private static int linkEnd(World world, StructureBoundingBox road, boolean alongX, int sign,
                               List<StructureBoundingBox> targets, List<StructureBoundingBox> occupied,
                               StructureBoundingBox clip, int gap) {
        // First open cell beyond the road end, on its centre line.
        int ex;
        int ez;
        if (alongX) {
            ex = sign < 0 ? road.minX - 1 : road.maxX + 1;
            ez = (road.minZ + road.maxZ) / 2;
        } else {
            ex = (road.minX + road.maxX) / 2;
            ez = sign < 0 ? road.minZ - 1 : road.maxZ + 1;
        }
        // Already joined: that cell touches another road or the well.
        for (StructureBoundingBox t : targets) {
            if (t != road && contains(t, ex, ez, 1)) return 0;
        }
        if (inside(occupied, ex, ez)) return 0;

        StructureBoundingBox best = null;
        int bestDist = Integer.MAX_VALUE;
        int bestX = 0;
        int bestZ = 0;
        for (StructureBoundingBox t : targets) {
            if (t == road) continue;
            int tx = Math.max(t.minX, Math.min(ex, t.maxX));
            int tz = Math.max(t.minZ, Math.min(ez, t.maxZ));
            int ahead = alongX ? (tx - ex) * sign : (tz - ez) * sign;
            if (ahead < 0) continue; // the other road is behind this end
            int dist = Math.abs(tx - ex) + Math.abs(tz - ez);
            if (dist > gap || dist >= bestDist) continue;
            best = t;
            bestDist = dist;
            bestX = tx;
            bestZ = tz;
        }
        if (best == null) return 0;

        int placed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = ex;
        int z = ez;
        int guard = bestDist + 2;
        // Straight along the road's own axis first, then across.
        while (guard-- > 0) {
            if (inside(occupied, x, z) && !contains(best, x, z, 0)) return placed;
            if (contains(best, x, z, 0)) break;
            if (pathColumn(world, pos, x, z, clip)) placed++;
            if (alongX) {
                if (x != bestX) x += Integer.signum(bestX - x);
                else if (z != bestZ) z += Integer.signum(bestZ - z);
                else break;
            } else {
                if (z != bestZ) z += Integer.signum(bestZ - z);
                else if (x != bestX) x += Integer.signum(bestX - x);
                else break;
            }
        }
        return placed;
    }

    private static boolean contains(StructureBoundingBox bb, int x, int z, int pad) {
        return x >= bb.minX - pad && x <= bb.maxX + pad && z >= bb.minZ - pad && z <= bb.maxZ + pad;
    }

    private static boolean inside(List<StructureBoundingBox> boxes, int x, int z) {
        for (StructureBoundingBox bb : boxes) {
            if (contains(bb, x, z, 0)) return true;
        }
        return false;
    }

    private static boolean pathColumn(World world, BlockPos.MutableBlockPos pos, int x, int z,
                                      StructureBoundingBox clip) {
        if (x < clip.minX || x > clip.maxX || z < clip.minZ || z > clip.maxZ) return false;
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
            if (VillageRoadStamper.isPassableFoliage(state)) {
                continue;
            }
            if (state.getMaterial().isLiquid()) {
                return false;
            }
            if (VillageRoadStamper.isRoadSurface(state)) {
                return false;
            }

            IBlockState replacement = VillageRoadStamper.resolvePathState(state);
            if (replacement != null) {
                world.setBlockState(pos, replacement, 2);
                pos.setPos(x, y + 1, z);
                IBlockState above = world.getBlockState(pos);
                if (VillageRoadStamper.isPassableFoliage(above)) {
                    world.setBlockToAir(pos);
                    pos.setPos(x, y + 2, z);
                    if (world.getBlockState(pos).getBlock() == Blocks.DOUBLE_PLANT) {
                        world.setBlockToAir(pos);
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }
}
