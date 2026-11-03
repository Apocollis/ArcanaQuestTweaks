package com.apocollis.aqtweaks.rtg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure rule for which wet village roads stay (no Minecraft types, so it is unit tested).
 *
 * <p>A <b>plate</b> is a connected set of land boxes whose pads overlap. It counts as built-up only if one of
 * its boxes is a structure (not a road, not a lamp). A wet road is kept only if every water run along it lands
 * directly on a plate at both sides (the first and last wet column within {@code pad + 1} of a box), the run is
 * at most {@code maxSpan}, the two plates differ, both have a structure, and both are reachable from the well
 * by land or by other kept wet roads. A plate that was joined to the village only through a dropped wet road
 * is dropped with its pieces; plates that were never joined to the village are left alone.
 */
public final class VillageBridgeRule {

    /** A piece that makes land: its box (minX, maxX, minZ, maxZ) and what it is. */
    public static final class Land {
        public final Object tag;
        public final int[] box;
        public final boolean structure;
        public final boolean root;

        public Land(Object tag, int[] box, boolean structure, boolean root) {
            this.tag = tag;
            this.box = box;
            this.structure = structure;
            this.root = root;
        }
    }

    /** A road with water in it: per-slice wetness along its long axis. */
    public static final class Wet {
        public final Object tag;
        public final boolean alongX;
        public final int axisMin;
        public final int cross;
        public final boolean[] wet;
        public final boolean ocean;

        public Wet(Object tag, boolean alongX, int axisMin, int cross, boolean[] wet, boolean ocean) {
            this.tag = tag;
            this.alongX = alongX;
            this.axisMin = axisMin;
            this.cross = cross;
            this.wet = wet;
            this.ocean = ocean;
        }
    }

    public static final class Result {
        /** Wet roads to drop, with the reason (first failing rule). */
        public final Map<Object, String> droppedRoads = new LinkedHashMap<Object, String>();
        /** Land pieces (by tag) in plates that were joined to the village only through dropped roads. */
        public final Set<Object> orphans = new HashSet<Object>();
        public final Set<Object> keptRoads = new HashSet<Object>();
    }

    private VillageBridgeRule() {}

    public static Result evaluate(List<Land> lands, List<Wet> wets, int pad, int maxSpan) {
        Result result = new Result();
        int n = lands.size();
        int[] plate = plates(lands, pad);

        boolean[] structure = new boolean[n];
        int rootPlate = -1;
        for (int i = 0; i < n; i++) {
            if (lands.get(i).structure) structure[plate[i]] = true;
            if (lands.get(i).root) rootPlate = plate[i];
        }
        if (rootPlate < 0) {
            for (Wet w : wets) result.keptRoads.add(w.tag);
            return result;
        }

        // Per road: [plateA, plateB] for each run, or a reason it fails.
        List<int[]> edgesKept = new ArrayList<int[]>();
        List<int[]> edgesLaid = new ArrayList<int[]>();
        Map<Wet, List<int[]>> runEdges = new LinkedHashMap<Wet, List<int[]>>();
        Map<Wet, String> reasons = new LinkedHashMap<Wet, String>();
        for (Wet w : wets) {
            List<int[]> edges = new ArrayList<int[]>();
            String reason = null;
            boolean laid = true;
            int len = w.wet.length;
            if (w.ocean) {
                reason = "ocean";
                laid = false;
            } else if (len == 0 || w.wet[0] || w.wet[len - 1]) {
                reason = "wet-end";
                laid = false;
            } else {
                int i = 0;
                while (i < len) {
                    if (!w.wet[i]) {
                        i++;
                        continue;
                    }
                    int start = i;
                    while (i < len && w.wet[i]) i++;
                    int end = i - 1;
                    int a = landing(lands, plate, w, start, pad);
                    int b = landing(lands, plate, w, end, pad);
                    if (a < 0 || b < 0) {
                        if (reason == null) reason = a < 0 ? "no-near-plate" : "no-far-plate";
                        laid = false;
                        continue;
                    }
                    edges.add(new int[] {a, b});
                    if (reason == null) {
                        if (end - start + 1 > maxSpan) reason = "span";
                        else if (a == b) reason = "same-plate";
                        else if (!structure[a] || !structure[b]) reason = "no-structure";
                    }
                }
            }
            runEdges.put(w, edges);
            reasons.put(w, reason);
            if (laid) edgesLaid.addAll(edges);
            if (reason == null) edgesKept.addAll(edges);
        }

        boolean[] before = reach(n, rootPlate, edgesLaid, plate);
        boolean[] after = reach(n, rootPlate, edgesKept, plate);

        for (Wet w : wets) {
            String reason = reasons.get(w);
            if (reason == null) {
                for (int[] e : runEdges.get(w)) {
                    if (!after[e[0]] || !after[e[1]]) {
                        reason = "unreachable";
                        break;
                    }
                }
            }
            if (reason == null) result.keptRoads.add(w.tag);
            else result.droppedRoads.put(w.tag, reason);
        }
        for (int i = 0; i < n; i++) {
            if (before[plate[i]] && !after[plate[i]]) result.orphans.add(lands.get(i).tag);
        }
        return result;
    }

    /** Plate id (a representative land index) for each land piece. */
    static int[] plates(List<Land> lands, int pad) {
        int n = lands.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (boxDistance(lands.get(i).box, lands.get(j).box) <= 2.0 * Math.max(0, pad)) {
                    int a = find(parent, i);
                    int b = find(parent, j);
                    if (a != b) parent[Math.max(a, b)] = Math.min(a, b);
                }
            }
        }
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = find(parent, i);
        return out;
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    /** Plate whose pad touches the wet column at slice {@code index}, or -1. */
    private static int landing(List<Land> lands, int[] plate, Wet w, int index, int pad) {
        int a = w.axisMin + index;
        int x = w.alongX ? a : w.cross;
        int z = w.alongX ? w.cross : a;
        double best = Double.MAX_VALUE;
        int found = -1;
        for (int i = 0; i < lands.size(); i++) {
            double d = boxDistance(new int[] {x, x, z, z}, lands.get(i).box);
            if (d <= Math.max(0, pad) + 1 && d < best) {
                best = d;
                found = plate[i];
            }
        }
        return found;
    }

    private static boolean[] reach(int n, int rootPlate, List<int[]> edges, int[] plate) {
        boolean[] seen = new boolean[n];
        seen[rootPlate] = true;
        boolean grew = true;
        while (grew) {
            grew = false;
            for (int[] e : edges) {
                if (seen[e[0]] != seen[e[1]]) {
                    seen[e[0]] = true;
                    seen[e[1]] = true;
                    grew = true;
                }
            }
        }
        return seen;
    }

    private static double boxDistance(int[] a, int[] b) {
        int dx = 0;
        if (a[1] < b[0]) dx = b[0] - a[1];
        else if (a[0] > b[1]) dx = a[0] - b[1];
        int dz = 0;
        if (a[3] < b[2]) dz = b[2] - a[3];
        else if (a[2] > b[3]) dz = a[2] - b[3];
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }
}
