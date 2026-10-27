package com.apocollis.aqtweaks.atum;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Isolated helper for Baubles API interactions to ensure safe classloading
 * when Baubles is optionally loaded.
 */
public final class BaublesGogglesHelper {

    private BaublesGogglesHelper() {}

    /**
     * Checks if the player has valid protective goggles equipped in the Baubles head accessory slot.
     */
    public static boolean hasGogglesInHeadSlot(EntityPlayer player) {
        try {
            IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
            if (handler == null) return false;
            for (int i = 0; i < handler.getSlots(); i++) {
                if (BaubleType.HEAD.hasSlot(i)) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty() && AtumStormGameplayHandler.isGoggles(stack)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
