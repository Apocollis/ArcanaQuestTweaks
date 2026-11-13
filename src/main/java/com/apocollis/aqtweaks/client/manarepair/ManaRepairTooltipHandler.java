package com.apocollis.aqtweaks.client.manarepair;

import com.apocollis.aqtweaks.thaumcraft.ManaRepairTag;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** One tooltip line on Mana-Weave items. Registered only when Thaumcraft and Botania are loaded. */
@SideOnly(Side.CLIENT)
public class ManaRepairTooltipHandler {
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (ManaRepairTag.has(event.getItemStack())) {
            event.getToolTip().add("§b" + I18n.format("aqtweaks.manarepair.tooltip"));
        }
    }
}
