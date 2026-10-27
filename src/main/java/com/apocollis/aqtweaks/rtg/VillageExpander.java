package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * When village layout finishes below a configured minimum building count (e.g. 10),
 * attempts to expand roads/paths outward from open road ends and missing well directions
 * to lay out more buildings on available dry land within the 112-block village cap.
 */
public final class VillageExpander {

    private static final int MAX_PASSES = 8;

    private VillageExpander() {}

    public static void expandVillageIfNeeded(StructureStart start, Random rand, World world) {
        int minBuildings = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageMinBuildingCount;
        if (minBuildings <= 0 || start == null || rand == null) {
            return;
        }

        List<StructureComponent> components = start.getComponents();
        if (components == null || components.isEmpty()) return;

        StructureComponent first = components.get(0);
        if (!(first instanceof StructureVillagePieces.Start)) return;
        StructureVillagePieces.Start wellStart = (StructureVillagePieces.Start) first;

        int buildingCount = countBuildings(components);
        if (buildingCount >= minBuildings) {
            return;
        }

        int initialCount = buildingCount;
        StructureBoundingBox wellBb = wellStart.getBoundingBox();
        int wellX = wellBb != null ? (wellBb.minX + wellBb.maxX) / 2 : 0;
        int wellZ = wellBb != null ? (wellBb.minZ + wellBb.maxZ) / 2 : 0;

        VillageDebug.log("village-expand start well=%d,%d current=%d target=%d", wellX, wellZ, buildingCount, minBuildings);

        // Ensure weights are not capped if limits were reached
        ensurePieceWeightsAvailable(wellStart);

        for (int pass = 0; pass < MAX_PASSES && buildingCount < minBuildings; pass++) {
            boolean addedAny = false;

            // Phase 1: Try missing directions from the well
            if (tryExpandFromWell(wellStart, components, rand)) {
                addedAny = true;
                drainPending(wellStart, components, rand);
                buildingCount = countBuildings(components);
                if (buildingCount >= minBuildings) break;
            }

            // Phase 2: Try dead-end roads
            List<StructureComponent> snapshot = new ArrayList<StructureComponent>(components);
            for (StructureComponent c : snapshot) {
                if (c == null || !VillageLandHelper.isVillageRoad(c)) continue;
                if (isRoadEndConnected(c, components)) continue;

                if (tryExpandRoadEnd(wellStart, c, components, rand)) {
                    addedAny = true;
                    drainPending(wellStart, components, rand);
                    buildingCount = countBuildings(components);
                    if (buildingCount >= minBuildings) break;
                }
            }

            if (!addedAny) {
                // No more roads could fit in any direction
                break;
            }
        }

        StructureAccess.updateStructureStartBoundingBox(start);
        VillageDebug.log("village-expand finished well=%d,%d initial=%d final=%d minTarget=%d",
                wellX, wellZ, initialCount, buildingCount, minBuildings);
    }

    public static int countBuildings(List<StructureComponent> components) {
        if (components == null) return 0;
        int count = 0;
        for (StructureComponent c : components) {
            if (c == null) continue;
            if (!VillageLandHelper.isVillageRoad(c) && !VillageLandHelper.isVillageWellOrStart(c)) {
                count++;
            }
        }
        return count;
    }

    private static void ensurePieceWeightsAvailable(StructureVillagePieces.Start wellStart) {
        if (wellStart.structureVillageWeightedPieceList == null) return;
        for (StructureVillagePieces.PieceWeight pw : wellStart.structureVillageWeightedPieceList) {
            if (pw != null && pw.villagePiecesSpawned >= pw.villagePiecesLimit) {
                pw.villagePiecesLimit += 2;
            }
        }
    }

    private static boolean tryExpandFromWell(StructureVillagePieces.Start well, List<StructureComponent> components, Random rand) {
        StructureBoundingBox wellBb = well.getBoundingBox();
        if (wellBb == null) return false;

        boolean added = false;
        EnumFacing[] facings = new EnumFacing[] {EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH};
        for (EnumFacing facing : facings) {
            if (hasRoadFromWell(well, components, facing)) continue;

            int x = 0;
            int y = wellBb.maxY - 4;
            int z = 0;

            switch (facing) {
                case WEST:
                    x = wellBb.minX - 1;
                    z = wellBb.minZ + 1;
                    break;
                case EAST:
                    x = wellBb.maxX + 1;
                    z = wellBb.minZ + 1;
                    break;
                case NORTH:
                    x = wellBb.minX + 1;
                    z = wellBb.minZ - 1;
                    break;
                case SOUTH:
                    x = wellBb.minX + 1;
                    z = wellBb.maxZ + 1;
                    break;
            }

            StructureComponent road = tryAddRoad(well, components, rand, x, y, z, facing, 1);
            if (road != null) {
                added = true;
            }
        }
        return added;
    }

    private static boolean hasRoadFromWell(StructureVillagePieces.Start well, List<StructureComponent> components, EnumFacing facing) {
        StructureBoundingBox wellBb = well.getBoundingBox();
        if (wellBb == null) return true;
        for (StructureComponent c : components) {
            if (c == null || !VillageLandHelper.isVillageRoad(c)) continue;
            StructureBoundingBox bb = c.getBoundingBox();
            if (bb == null) continue;
            switch (facing) {
                case WEST:
                    if (bb.maxX == wellBb.minX - 1 && bb.minZ <= wellBb.maxZ && bb.maxZ >= wellBb.minZ) return true;
                    break;
                case EAST:
                    if (bb.minX == wellBb.maxX + 1 && bb.minZ <= wellBb.maxZ && bb.maxZ >= wellBb.minZ) return true;
                    break;
                case NORTH:
                    if (bb.maxZ == wellBb.minZ - 1 && bb.minX <= wellBb.maxX && bb.maxX >= wellBb.minX) return true;
                    break;
                case SOUTH:
                    if (bb.minZ == wellBb.maxZ + 1 && bb.minX <= wellBb.maxX && bb.maxX >= wellBb.minX) return true;
                    break;
            }
        }
        return false;
    }

    private static boolean isRoadEndConnected(StructureComponent road, List<StructureComponent> components) {
        StructureBoundingBox bb = road.getBoundingBox();
        EnumFacing facing = road.getCoordBaseMode();
        if (bb == null || facing == null) return true;

        int testX1 = bb.minX;
        int testX2 = bb.maxX;
        int testZ1 = bb.minZ;
        int testZ2 = bb.maxZ;

        switch (facing) {
            case NORTH:
                testZ1 = bb.minZ - 1;
                testZ2 = bb.minZ - 1;
                break;
            case SOUTH:
                testZ1 = bb.maxZ + 1;
                testZ2 = bb.maxZ + 1;
                break;
            case WEST:
                testX1 = bb.minX - 1;
                testX2 = bb.minX - 1;
                break;
            case EAST:
                testX1 = bb.maxX + 1;
                testX2 = bb.maxX + 1;
                break;
        }

        for (StructureComponent other : components) {
            if (other == null || other == road || !VillageLandHelper.isVillageRoad(other)) continue;
            StructureBoundingBox obb = other.getBoundingBox();
            if (obb == null) continue;
            if (obb.minX <= testX2 && obb.maxX >= testX1 && obb.minZ <= testZ2 && obb.maxZ >= testZ1) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryExpandRoadEnd(
            StructureVillagePieces.Start well,
            StructureComponent road,
            List<StructureComponent> components,
            Random rand) {
        StructureBoundingBox bb = road.getBoundingBox();
        EnumFacing facing = road.getCoordBaseMode();
        if (bb == null || facing == null) return false;

        Candidate[] candidates = getEndCandidates(bb, facing);
        for (Candidate c : candidates) {
            StructureComponent added = tryAddRoad(well, components, rand, c.x, c.y, c.z, c.facing, 1);
            if (added != null) {
                return true;
            }
        }
        return false;
    }

    private record Candidate(int x, int y, int z, EnumFacing facing) {}

    private static Candidate[] getEndCandidates(StructureBoundingBox bb, EnumFacing facing) {
        switch (facing) {
            case NORTH:
                return new Candidate[] {
                        new Candidate(bb.minX, bb.minY, bb.minZ - 1, EnumFacing.NORTH),
                        new Candidate(bb.minX - 1, bb.minY, bb.minZ, EnumFacing.WEST),
                        new Candidate(bb.maxX + 1, bb.minY, bb.minZ, EnumFacing.EAST)
                };
            case SOUTH:
                return new Candidate[] {
                        new Candidate(bb.minX, bb.minY, bb.maxZ + 1, EnumFacing.SOUTH),
                        new Candidate(bb.maxX + 1, bb.minY, bb.maxZ - 2, EnumFacing.EAST),
                        new Candidate(bb.minX - 1, bb.minY, bb.maxZ - 2, EnumFacing.WEST)
                };
            case WEST:
                return new Candidate[] {
                        new Candidate(bb.minX - 1, bb.minY, bb.minZ, EnumFacing.WEST),
                        new Candidate(bb.minX, bb.minY, bb.maxZ + 1, EnumFacing.SOUTH),
                        new Candidate(bb.minX, bb.minY, bb.minZ - 1, EnumFacing.NORTH)
                };
            case EAST:
                return new Candidate[] {
                        new Candidate(bb.maxX + 1, bb.minY, bb.minZ, EnumFacing.EAST),
                        new Candidate(bb.maxX - 2, bb.minY, bb.minZ - 1, EnumFacing.NORTH),
                        new Candidate(bb.maxX - 2, bb.minY, bb.maxZ + 1, EnumFacing.SOUTH)
                };
            default:
                return new Candidate[0];
        }
    }

    private static StructureComponent tryAddRoad(
            StructureVillagePieces.Start start,
            List<StructureComponent> components,
            Random rand,
            int x, int y, int z,
            EnumFacing facing,
            int type) {
        if (!VillageLandHelper.withinVillageCap(start, x, z)) {
            return null;
        }
        StructureBoundingBox box = StructureVillagePieces.Path.findPieceBox(start, components, rand, x, y, z, facing);
        if (box != null && box.minY > 10) {
            if (!VillageLandHelper.withinVillageCap(start, box.minX, box.minZ)
                    || !VillageLandHelper.withinVillageCap(start, box.maxX, box.maxZ)) {
                return null;
            }
            StructureVillagePieces.Path path = new StructureVillagePieces.Path(start, type, rand, box, facing);
            components.add(path);
            start.pendingRoads.add(path);
            return path;
        }
        return null;
    }

    private static void drainPending(
            StructureVillagePieces.Start start,
            List<StructureComponent> components,
            Random rand) {
        List<StructureComponent> roads = start.pendingRoads;
        List<StructureComponent> houses = start.pendingHouses;
        while (!roads.isEmpty() || !houses.isEmpty()) {
            if (roads.isEmpty()) {
                int idx = rand.nextInt(houses.size());
                StructureComponent house = houses.remove(idx);
                house.buildComponent(start, components, rand);
            } else {
                int idx = rand.nextInt(roads.size());
                StructureComponent road = roads.remove(idx);
                road.buildComponent(start, components, rand);
            }
        }
    }
}
