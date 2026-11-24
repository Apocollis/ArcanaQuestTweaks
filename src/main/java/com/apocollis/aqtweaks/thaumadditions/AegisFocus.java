package com.apocollis.aqtweaks.thaumadditions;

import com.apocollis.aqtweaks.thaumadditions.foci.FocusEffectAegis;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.items.casters.ItemCaster;
import thaumcraft.common.items.casters.ItemFocus;

/** Finds the Aegis effect in the focus a caster item has selected. Remembers the last answer (this runs per frame). */
public final class AegisFocus {
    private AegisFocus() {}

    private static NBTTagCompound lastTag;
    private static FocusEffectAegis lastEffect;

    /** The Aegis effect in the focus selected on this caster stack, or null. */
    public static synchronized FocusEffectAegis find(ItemStack caster) {
        if (caster == null || caster.isEmpty() || !(caster.getItem() instanceof ItemCaster)) return null;
        NBTTagCompound tag = caster.getTagCompound();
        if (tag == null || !tag.hasKey("focus", 10)) return null;
        NBTTagCompound focusTag = tag.getCompoundTag("focus");
        if (lastTag != null && lastTag.equals(focusTag)) return lastEffect;
        FocusEffectAegis found = null;
        ItemStack focus = new ItemStack(focusTag);
        if (!focus.isEmpty() && focus.getItem() instanceof ItemFocus) {
            FocusPackage pkg = ItemFocus.getPackage(focus);
            if (pkg != null) {
                for (FocusEffect effect : pkg.getFocusEffects()) {
                    if (effect instanceof FocusEffectAegis) {
                        found = (FocusEffectAegis) effect;
                        break;
                    }
                }
            }
        }
        lastTag = focusTag.copy();
        lastEffect = found;
        return found;
    }

    public static boolean has(ItemStack caster) {
        return find(caster) != null;
    }
}
