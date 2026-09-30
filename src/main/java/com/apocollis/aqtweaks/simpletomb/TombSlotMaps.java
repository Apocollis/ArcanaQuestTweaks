package com.apocollis.aqtweaks.simpletomb;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

public final class TombSlotMaps {

    public static final String TAG_GRAVE_SLOT = "aqtweaks_grave_slot";
    public static final String NBT_SLOT_MAP = "AQTweaks_SlotMap";

    public static final byte TYPE_MAIN = 0;
    public static final byte TYPE_ARMOR = 1;
    public static final byte TYPE_OFFHAND = 2;
    public static final byte TYPE_BAUBLE = 3;

    private TombSlotMaps() {}

    public static final class SlotMapping {
        public final int tombSlot;
        public final byte invType;
        public final int targetSlot;

        public SlotMapping(int tombSlot, byte invType, int targetSlot) {
            this.tombSlot = tombSlot;
            this.invType = invType;
            this.targetSlot = targetSlot;
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("TombSlot", tombSlot);
            tag.setByte("InvType", invType);
            tag.setInteger("TargetSlot", targetSlot);
            return tag;
        }

        public static SlotMapping readFromNBT(NBTTagCompound tag) {
            int tombSlot = tag.getInteger("TombSlot");
            byte invType = tag.getByte("InvType");
            int targetSlot = tag.getInteger("TargetSlot");
            return new SlotMapping(tombSlot, invType, targetSlot);
        }
    }

    public static void stampStack(ItemStack stack, byte type, int slot) {
        if (stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagCompound slotTag = new NBTTagCompound();
        slotTag.setByte("type", type);
        slotTag.setInteger("slot", slot);
        tag.setTag(TAG_GRAVE_SLOT, slotTag);
    }

    public static boolean hasGraveSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(TAG_GRAVE_SLOT, Constants.NBT.TAG_COMPOUND);
    }

    public static NBTTagCompound getGraveSlotTag(ItemStack stack) {
        if (!hasGraveSlot(stack)) {
            return null;
        }
        return stack.getTagCompound().getCompoundTag(TAG_GRAVE_SLOT);
    }

    public static void stripGraveSlotTag(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(TAG_GRAVE_SLOT)) {
            tag.removeTag(TAG_GRAVE_SLOT);
            if (tag.isEmpty()) {
                stack.setTagCompound(null);
            }
        }
    }

    public static NBTTagList writeSlotMap(List<SlotMapping> mappings) {
        NBTTagList list = new NBTTagList();
        if (mappings != null) {
            for (SlotMapping mapping : mappings) {
                list.appendTag(mapping.writeToNBT());
            }
        }
        return list;
    }

    public static List<SlotMapping> readSlotMap(NBTTagList list) {
        List<SlotMapping> mappings = new ArrayList<>();
        if (list != null) {
            for (int i = 0; i < list.tagCount(); i++) {
                mappings.add(SlotMapping.readFromNBT(list.getCompoundTagAt(i)));
            }
        }
        return mappings;
    }

    public static int findFirstEmptySlot(IItemHandler handler) {
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack redirectInsertItemStacked(IItemHandler inventory, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (hasGraveSlot(stack)) {
            NBTTagCompound slotTag = getGraveSlotTag(stack);
            byte type = slotTag.getByte("type");
            int targetSlot = slotTag.getInteger("slot");

            ItemStack stripped = stack.copy();
            stripGraveSlotTag(stripped);

            if (simulate) {
                int emptySlot = findFirstEmptySlot(inventory);
                return emptySlot == -1 ? stack : ItemStack.EMPTY;
            }

            int emptySlot = findFirstEmptySlot(inventory);
            if (emptySlot != -1) {
                inventory.insertItem(emptySlot, stripped, false);
                SimpleTombModule.recordCapturedStack(emptySlot, type, targetSlot, stripped);
                return ItemStack.EMPTY;
            }
            return stack;
        }

        ItemStack result = ItemHandlerHelper.insertItemStacked(inventory, stack, simulate);
        if (!simulate && (result.isEmpty() || result.getCount() < stack.getCount())) {
            ItemStack accepted = stack.copy();
            if (!result.isEmpty()) {
                accepted.setCount(stack.getCount() - result.getCount());
            }
            stripGraveSlotTag(accepted);
            SimpleTombModule.recordUnmappedStack(accepted);
        }
        return result;
    }

    public static boolean restoreItemToSlot(EntityPlayer player, ItemStack stack, byte invType, int targetSlot) {
        if (stack.isEmpty()) {
            return true;
        }

        switch (invType) {
            case TYPE_MAIN:
                if (targetSlot >= 0 && targetSlot < player.inventory.mainInventory.size()) {
                    ItemStack current = player.inventory.mainInventory.get(targetSlot);
                    if (current.isEmpty()) {
                        player.inventory.mainInventory.set(targetSlot, stack);
                    } else {
                        displaceCurrentAndSet(player, current, stack, () -> player.inventory.mainInventory.set(targetSlot, stack));
                    }
                    return true;
                }
                break;

            case TYPE_ARMOR:
                if (targetSlot >= 0 && targetSlot < player.inventory.armorInventory.size()) {
                    ItemStack current = player.inventory.armorInventory.get(targetSlot);
                    if (current.isEmpty()) {
                        player.inventory.armorInventory.set(targetSlot, stack);
                    } else if (net.minecraft.enchantment.EnchantmentHelper.hasBindingCurse(current)) {
                        ItemHandlerHelper.giveItemToPlayer(player, stack);
                    } else {
                        displaceCurrentAndSet(player, current, stack, () -> player.inventory.armorInventory.set(targetSlot, stack));
                    }
                    return true;
                }
                break;

            case TYPE_OFFHAND:
                if (player.inventory.offHandInventory.size() > 0) {
                    ItemStack current = player.inventory.offHandInventory.get(0);
                    if (current.isEmpty()) {
                        player.inventory.offHandInventory.set(0, stack);
                    } else {
                        displaceCurrentAndSet(player, current, stack, () -> player.inventory.offHandInventory.set(0, stack));
                    }
                    return true;
                }
                break;

            case TYPE_BAUBLE:
                if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
                    return TombBaubleSlots.restoreBauble(player, stack, targetSlot);
                }
                break;
        }

        ItemHandlerHelper.giveItemToPlayer(player, stack);
        return false;
    }

    private static void displaceCurrentAndSet(EntityPlayer player, ItemStack current, ItemStack replacement, Runnable setter) {
        ItemStack copy = current.copy();
        setter.run();
        boolean added = player.inventory.addItemStackToInventory(copy);
        if (!added || !copy.isEmpty()) {
            EntityItem dropped = player.dropItem(copy, false);
            if (dropped != null) {
                dropped.setNoPickupDelay();
            }
        }
    }
}
