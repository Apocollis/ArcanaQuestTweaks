package com.apocollis.aqtweaks.starter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure comparison of a CSG kit with the default starting inventory. Slots are CSG strings {@code category:slot}
 * (0 main, 1 armor, 2 off hand); stacks are compared by their CSG reference string.
 */
public final class StarterKitDiff {

    private StarterKitDiff() {}

    /** Main-inventory slots that every kit must copy from the default inventory (plan 13.1): 8-11, 15-17, 27-35. */
    public static boolean isUtilitySlot(String slot) {
        int[] parsed = parse(slot);
        if (parsed == null || parsed[0] != 0) return false;
        int s = parsed[1];
        return (s >= 8 && s <= 11) || (s >= 15 && s <= 17) || (s >= 27 && s <= 35);
    }

    /** Utility slots the default inventory fills that the kit leaves empty or fills with something else. */
    public static List<String> drift(Map<String, String> defaults, Map<String, String> kit) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : defaults.entrySet()) {
            if (!isUtilitySlot(e.getKey())) continue;
            String have = kit.get(e.getKey());
            if (have == null || !have.equals(e.getValue())) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /** Kit slots that hold something other than the default (or nothing in the default): armor first, off hand, main. */
    public static List<String> gearSlots(Map<String, String> defaults, Map<String, String> kit) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : kit.entrySet()) {
            String def = defaults.get(e.getKey());
            if (def == null || !def.equals(e.getValue())) {
                out.add(e.getKey());
            }
        }
        out.sort((a, b) -> {
            int[] pa = parse(a);
            int[] pb = parse(b);
            if (pa == null || pb == null) return a.compareTo(b);
            int ra = rank(pa[0]);
            int rb = rank(pb[0]);
            if (ra != rb) return Integer.compare(ra, rb);
            return Integer.compare(pa[1], pb[1]);
        });
        return out;
    }

    private static int rank(int category) {
        // armor (1) first, then off hand (2), then main (0)
        return category == 1 ? 0 : category == 2 ? 1 : 2;
    }

    private static int[] parse(String slot) {
        if (slot == null) return null;
        int colon = slot.indexOf(':');
        if (colon <= 0) return null;
        try {
            return new int[] {Integer.parseInt(slot.substring(0, colon)), Integer.parseInt(slot.substring(colon + 1))};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
