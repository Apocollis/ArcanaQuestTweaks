package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * A wet road (water along its axis) is kept only as a bridge between two village plates that each hold a
 * structure and are reachable from the well; see {@link VillageBridgeRule}. After a village is fully laid out, a
 * wet road that fails the rule is dropped, and so is every piece that was joined to the village only through it.
 * Dry roads and every other piece are never judged. A pure function of the kept pieces and the landscape, so every
 * chunk that lays the village out gets the same result. The village itself is never vetoed.
 */
public final class VillageBridgePrune {

    private VillageBridgePrune() {}

    public static void prune(World world, Object start) {
        if (world == null || start == null
                || !ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBridgeRequireBothEnds
                || !ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageRiverBridges) {
            return;
        }
        VillageLandHelper.pushWorld(world);
        try {
            if (apply(start)) {
                StructureAccess.updateStructureStartBoundingBox(start);
            }
        } finally {
            VillageLandHelper.popWorld();
        }
    }

    private static boolean apply(Object start) {
        List<Object> pieces = new ArrayList<Object>(StructureAccess.getStructureStartComponents(start));
        List<VillageBridgeRule.Land> lands = new ArrayList<VillageBridgeRule.Land>();
        List<VillageBridgeRule.Wet> wets = new ArrayList<VillageBridgeRule.Wet>();
        VillageLandHelper.pushColumnLandscapeCache();
        try {
            for (Object piece : pieces) {
                if (VillageLandHelper.isVillagePlatePad(piece)) continue;
                int[] box = StructureAccess.getStructureComponentBoxXZ(piece);
                if (box == null) continue;
                boolean road = VillageLandHelper.isVillageRoad(piece);
                if (road) {
                    VillageBridgeRule.Wet wet = VillageLandHelper.wetProfile(start, piece);
                    if (wet != null) {
                        wets.add(wet);
                        continue;
                    }
                }
                if (VillageLandHelper.isPasteSkippedPiece(start, piece)) continue;
                boolean root = VillageLandHelper.isVillageWellOrStart(piece);
                boolean structure = !road && !VillageLandHelper.isVillageLamp(piece);
                lands.add(new VillageBridgeRule.Land(piece, box, structure, root));
            }
        } finally {
            VillageLandHelper.popColumnLandscapeCache();
        }
        if (wets.isEmpty()) return false;

        int pad = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageComponentPad;
        int span = Math.max(1, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBridgeMaxSpan);
        VillageBridgeRule.Result result = VillageBridgeRule.evaluate(lands, wets, pad, span);

        boolean changed = false;
        for (VillageBridgeRule.Wet wet : wets) {
            int[] box = StructureAccess.getStructureComponentBoxXZ(wet.tag);
            String reason = result.droppedRoads.get(wet.tag);
            if (reason == null) {
                VillageDebug.log("bridge keep box=%s", boxText(box));
                continue;
            }
            VillageLandHelper.removeVillagePiece(start, null, wet.tag);
            VillageDebug.log("bridge drop reason=%s box=%s", reason, boxText(box));
            changed = true;
        }
        for (Object orphan : result.orphans) {
            VillageLandHelper.removeVillagePiece(start, null, orphan);
            VillageDebug.log("bridge drop orphan box=%s", boxText(StructureAccess.getStructureComponentBoxXZ(orphan)));
            changed = true;
        }
        return changed;
    }

    private static String boxText(int[] box) {
        return box == null ? "?" : "[" + box[0] + "," + box[1] + "]x[" + box[2] + "," + box[3] + "]";
    }
}
