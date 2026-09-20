package com.apocollis.aqtweaks.qualitytools;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Loot/drop stamp and Dawnstone rune register. Construct only when {@code qualitytools} is loaded.
 */
public class QualityToolsModule {

    public static void postInit() {
        if (!ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general.enable) {
            return;
        }
        if (Loader.isModLoaded("embers")) {
            QualityRuneAnvilRecipe.registerAll();
        }
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!QualityStamp.enabled()) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        var container = event.getContainer();
        if (container == null || container instanceof ContainerMerchant) {
            return;
        }
        for (Slot slot : container.inventorySlots) {
            if (slot == null || slot.inventory == null) {
                continue;
            }
            if (slot.inventory instanceof InventoryPlayer
                    || slot.inventory instanceof InventoryCrafting
                    || slot.inventory instanceof InventoryCraftResult) {
                continue;
            }
            QualityStamp.stampIfUntagged(slot.getStack());
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (!QualityStamp.enabled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity == null || entity.world == null || entity.world.isRemote) {
            return;
        }
        if (entity instanceof EntityItem item) {
            ItemStack stack = item.getItem();
            QualityStamp.stampIfUntagged(stack);
            return;
        }
        if (entity instanceof EntityPlayer) {
            return;
        }
        if (entity instanceof EntityLivingBase living) {
            for (var slot : net.minecraft.inventory.EntityEquipmentSlot.values()) {
                QualityStamp.stampIfUntagged(living.getItemStackFromSlot(slot));
            }
        }
    }
}
