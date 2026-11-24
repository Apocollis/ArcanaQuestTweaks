package com.apocollis.aqtweaks.starter;

import com.brandon3055.csg.DataManager;
import com.brandon3055.csg.lib.PlayerSlot;
import com.brandon3055.csg.lib.StackReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;

/** Reads the CustomStartingGear kits as plain strings for the pure diff code, and builds the gear preview. */
final class StarterCsg {

    private StarterCsg() {}

    static Map<String, String> defaults() {
        return flatten(DataManager.spawnInventory);
    }

    static Map<String, String> kit(String name) {
        if (DataManager.kits == null) return new LinkedHashMap<>();
        return flatten(DataManager.kits.get(name));
    }

    static List<String> drift(String kit) {
        return StarterKitDiff.drift(defaults(), kit(kit));
    }

    /** The kit minus the shared default items, armor first, at most {@code cap} stacks. */
    static List<ItemStack> gear(String kitName, int cap) {
        List<ItemStack> out = new ArrayList<>();
        Map<PlayerSlot, StackReference> kit = DataManager.kits == null ? null : DataManager.kits.get(kitName);
        if (kit == null) return out;
        Map<String, String> flatDefaults = defaults();
        Map<String, String> flatKit = flatten(kit);
        Map<String, StackReference> bySlot = new LinkedHashMap<>();
        for (Map.Entry<PlayerSlot, StackReference> e : kit.entrySet()) {
            bySlot.put(e.getKey().toString(), e.getValue());
        }
        for (String slot : StarterKitDiff.gearSlots(flatDefaults, flatKit)) {
            if (out.size() >= cap) break;
            StackReference ref = bySlot.get(slot);
            if (ref == null) continue;
            ItemStack stack = ref.createStack();
            if (stack != null && !stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out;
    }

    private static Map<String, String> flatten(Map<PlayerSlot, StackReference> source) {
        Map<String, String> out = new LinkedHashMap<>();
        if (source == null) return out;
        for (Map.Entry<PlayerSlot, StackReference> e : source.entrySet()) {
            out.put(e.getKey().toString(), e.getValue().toString());
        }
        return out;
    }
}
