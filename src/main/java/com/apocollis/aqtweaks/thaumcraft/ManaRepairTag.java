package com.apocollis.aqtweaks.thaumcraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** The root NBT byte that marks an item infused with Mana-Weave. Holds no mod types, so early mixins can reach it. */
public final class ManaRepairTag {
    private ManaRepairTag() {}

    public static final String KEY = "aqtweaks_mana_repair";

    public static boolean has(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean(KEY);
    }

    public static void set(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setBoolean(KEY, true);
    }

    /**
     * Called from the {@code ItemStack.damageItem} mixin. Returns true when mana paid for the damage and the stack must
     * not be damaged. Botania types are touched only when the stack is tagged.
     */
    public static boolean absorbDamage(net.minecraft.entity.EntityLivingBase entity, ItemStack stack, int amount) {
        if (!(entity instanceof net.minecraft.entity.player.EntityPlayer) || !has(stack)) return false;
        return ManaRepairBotania.payDamage((net.minecraft.entity.player.EntityPlayer) entity, stack, amount);
    }
}
