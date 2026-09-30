package com.apocollis.aqtweaks.simpletomb;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketSync;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public final class TombBaubleSlots {

    private TombBaubleSlots() {}

    public static void stampBaubles(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) {
            return;
        }
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                TombSlotMaps.stampStack(stack, TombSlotMaps.TYPE_BAUBLE, i);
            }
        }
    }

    public static void cleanUndroppedBaubles(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) {
            return;
        }
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                TombSlotMaps.stripGraveSlotTag(stack);
            }
        }
    }

    public static boolean restoreBauble(EntityPlayer player, ItemStack stack, int targetSlot) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null || targetSlot < 0 || targetSlot >= handler.getSlots()) {
            ItemHandlerHelper.giveItemToPlayer(player, stack);
            return false;
        }

        ItemStack current = handler.getStackInSlot(targetSlot);
        if (current.isEmpty()) {
            handler.setStackInSlot(targetSlot, stack);
        } else if (EnchantmentHelper.hasBindingCurse(current)) {
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        } else {
            ItemStack copy = current.copy();
            handler.setStackInSlot(targetSlot, stack);
            boolean added = player.inventory.addItemStackToInventory(copy);
            if (!added || !copy.isEmpty()) {
                EntityItem dropped = player.dropItem(copy, false);
                if (dropped != null) {
                    dropped.setNoPickupDelay();
                }
            }
        }

        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP mp = (EntityPlayerMP) player;
            PacketHandler.INSTANCE.sendTo(PacketSync.S2CPack(player, targetSlot, handler.getStackInSlot(targetSlot), 0), mp);
        }
        return true;
    }
}
