package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A bridge exists only to connect two village pieces across water. After a village is fully laid out, a bridge
 * road (a road that crosses water, {@link VillageLandHelper#bridgeRun} above 0) whose either end touches no other
 * kept piece is dropped, and so is every piece that was reachable from the well only through it. A bridge that
 * runs out into the sea ends on nothing, so it fails the same test. A pure function of the kept pieces, so every
 * chunk that lays the village out gets the same result. The village itself is never vetoed.
 */
public final class VillageBridgePrune {

    /** Blocks between a piece's box and the bridge's end slice that still count as touching. */
    private static final int TOUCH = 2;
    private static final int MAX_DROPS = 8;

    private VillageBridgePrune() {}

    public static void prune(World world, Object start) {
        if (world == null || start == null
                || !ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBridgeRequireBothEnds
                || !ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageRiverBridges) {
            return;
        }
        VillageLandHelper.pushWorld(world);
        try {
            boolean changed = false;
            for (int drops = 0; drops < MAX_DROPS; drops++) {
                if (!dropOne(start)) break;
                changed = true;
            }
            if (changed) {
                StructureAccess.updateStructureStartBoundingBox(start);
            }
        } finally {
            VillageLandHelper.popWorld();
        }
    }

    /** Drops the first dead-end bridge and what only it connected; false when none fails. */
    private static boolean dropOne(Object start) {
        List<Object> pieces = pieces(start);
        for (Object piece : pieces) {
            if (!VillageLandHelper.isVillageRoad(piece)) continue;
            int[] box = StructureAccess.getStructureComponentBoxXZ(piece);
            if (box == null) continue;
            if (VillageLandHelper.bridgeRun(start, piece) <= 0) continue;
            String reason = deadEndReason(pieces, piece, box);
            if (reason == null) continue;

            Set<Object> before = reachable(pieces, null);
            Set<Object> after = reachable(pieces, piece);
            VillageLandHelper.removeVillagePiece(start, null, piece);
            int orphans = 0;
            for (Object other : pieces) {
                if (other == piece || !before.contains(other) || after.contains(other)) continue;
                VillageLandHelper.removeVillagePiece(start, null, other);
                orphans++;
            }
            VillageDebug.log("bridge drop dead-end reason=%s box=[%d,%d]x[%d,%d] orphans=%d",
                    reason, box[0], box[1], box[2], box[3], orphans);
            return true;
        }
        return false;
    }

    private static String deadEndReason(List<Object> pieces, Object bridge, int[] box) {
        boolean alongX = box[1] - box[0] >= box[3] - box[2];
        int[] low = alongX ? new int[] {box[0], box[0], box[2], box[3]} : new int[] {box[0], box[1], box[2], box[2]};
        int[] high = alongX ? new int[] {box[1], box[1], box[2], box[3]} : new int[] {box[0], box[1], box[3], box[3]};
        if (!touchesOther(pieces, bridge, low)) return "no-near-piece";
        if (!touchesOther(pieces, bridge, high)) return "no-far-piece";
        return null;
    }

    private static boolean touchesOther(List<Object> pieces, Object bridge, int[] slice) {
        for (Object other : pieces) {
            if (other == bridge || VillageLandHelper.isVillagePlatePad(other)) continue;
            int[] o = StructureAccess.getStructureComponentBoxXZ(other);
            if (o != null && near(o, slice)) return true;
        }
        return false;
    }

    private static boolean near(int[] a, int[] b) {
        return a[0] <= b[1] + TOUCH && a[1] >= b[0] - TOUCH
                && a[2] <= b[3] + TOUCH && a[3] >= b[2] - TOUCH;
    }

    /** Pieces reachable from the well or Start by touching boxes, ignoring {@code skip}. */
    private static Set<Object> reachable(List<Object> pieces, Object skip) {
        List<Object> nodes = new ArrayList<Object>();
        List<int[]> boxes = new ArrayList<int[]>();
        Object root = null;
        for (Object piece : pieces) {
            if (piece == skip || VillageLandHelper.isVillagePlatePad(piece)) continue;
            int[] box = StructureAccess.getStructureComponentBoxXZ(piece);
            if (box == null) continue;
            if (root == null && VillageLandHelper.isVillageWellOrStart(piece)) root = piece;
            nodes.add(piece);
            boxes.add(box);
        }
        Set<Object> seen = new HashSet<Object>();
        if (root == null) return seen;
        List<Object> queue = new ArrayList<Object>();
        queue.add(root);
        seen.add(root);
        for (int head = 0; head < queue.size(); head++) {
            int[] from = boxes.get(nodes.indexOf(queue.get(head)));
            for (int i = 0; i < nodes.size(); i++) {
                Object next = nodes.get(i);
                if (seen.contains(next) || !near(boxes.get(i), from)) continue;
                seen.add(next);
                queue.add(next);
            }
        }
        return seen;
    }

    private static List<Object> pieces(Object start) {
        return StructureAccess.getStructureStartComponents(start);
    }
}
