package com.apocollis.aqtweaks.comfort;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/** Cozy block item lines, and the Rested breakdown shown on the inventory Comfort tab. */
@SideOnly(Side.CLIENT)
public final class ComfortTooltips {

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) return;
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        ResourceLocation name = block.getRegistryName();
        if (name == null) return;
        PacketComfortCatalog.Row row = ComfortClientState.catalog.get(name.toString());
        if (row == null) return;
        List<String> lines = event.getToolTip();
        String counted = "";
        int[] counts = ComfortClientState.categories.get(row.category);
        if (counts != null) {
            counted = " " + counts[1] + " of " + counts[2] + " counted";
        }
        lines.add(TextFormatting.GOLD + capitalize(row.category) + " " + trim(row.weight) + counted);
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            lines.add(TextFormatting.DARK_GRAY + "Family " + row.family);
        }
    }

    /**
     * Lines for the Comfort tab hover box, below its on/off header. Rested shows whenever the effect is on the
     * player, at home or not. Hearthside shows only while the player is also resting at home.
     */
    public static List<String> tabLines() {
        List<String> text = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        PotionEffect rested = mc.player == null ? null : mc.player.getActivePotionEffect(PotionHomestead.INSTANCE);
        if (rested != null) {
            int seconds = Math.max(0, rested.getDuration() / 20);
            text.add(TextFormatting.GOLD + "Rested " + ComfortClientState.roman(rested.getAmplifier() + 1)
                    + TextFormatting.GRAY + String.format("  %d:%02d left", seconds / 60, seconds % 60));
            if (ComfortClientState.insight > 0.0f) {
                text.add(TextFormatting.YELLOW
                        + String.format("Insight: +%.0f%% XP from orbs", ComfortClientState.insight * 100.0f));
            }
            if (ComfortClientState.vigorInterval > 0 && ComfortClientState.vigorInterval < 30) {
                text.add(TextFormatting.GREEN + "Vigor: stamina recharges every "
                        + ComfortClientState.vigorInterval + " ticks (base 30)");
            }
            if (ComfortClientState.halfHearts > 0) {
                text.add(TextFormatting.RED + "Fortitude: +" + (ComfortClientState.halfHearts / 2) + " hearts, +"
                        + (ComfortClientState.halfFeathers / 2) + " feathers");
            }
        }
        if (ComfortClientState.state == 2 && ComfortClientState.granted > 0 && Loader.isModLoaded("thaumcraft")) {
            text.add(TextFormatting.AQUA + "\u2022 Hearthside: Cleansing warp (at home)");
        }
        if (rested == null && ComfortClientState.present) {
            text.add(TextFormatting.GRAY + ComfortHud.nextLine());
            text.add(TextFormatting.GRAY + ComfortHud.heldBack());
        }
        return text;
    }

    private static String capitalize(String category) {
        if (category == null || category.isEmpty()) return "";
        return Character.toUpperCase(category.charAt(0)) + category.substring(1);
    }

    private static String trim(float value) {
        if (value == Math.rint(value)) return Integer.toString((int) value);
        return String.format("%.1f", value);
    }
}
