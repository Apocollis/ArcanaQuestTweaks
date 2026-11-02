package com.apocollis.aqtweaks.comfort;

import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/** Item lines, and the Rested and Hearthside hover boxes on the top effect rows. */
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

    /** The order the inventory effect column uses: Rested first, Hearthside second, the rest as vanilla sorts them. */
    public static List<PotionEffect> pinned(List<PotionEffect> sorted) {
        List<PotionEffect> out = new ArrayList<>(sorted.size());
        PotionEffect rested = null;
        PotionEffect hearthside = null;
        for (PotionEffect effect : sorted) {
            if (effect.getPotion() == PotionHomestead.INSTANCE) {
                rested = effect;
            } else if (effect.getPotion() == PotionClarity.INSTANCE) {
                hearthside = effect;
            } else {
                out.add(effect);
            }
        }
        if (hearthside != null) out.add(0, hearthside);
        if (rested != null) out.add(0, rested);
        return out;
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiInventory gui)) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        Collection<PotionEffect> active = mc.player.getActivePotionEffects();
        if (active.isEmpty()) return;
        List<PotionEffect> order = pinned(Ordering.natural().sortedCopy(active));
        int step = order.size() > 5 ? 132 / (order.size() - 1) : 33;
        int x = gui.getGuiLeft() - 124;
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        if (mouseX < x || mouseX > x + 120) return;
        for (int i = 0; i < order.size(); i++) {
            int y = gui.getGuiTop() + i * step;
            int height = i == order.size() - 1 ? 32 : Math.min(32, step);
            if (mouseY < y || mouseY >= y + height) continue;
            PotionEffect effect = order.get(i);
            if (effect.getPotion() == PotionHomestead.INSTANCE) {
                gui.drawHoveringText(restedLines(effect), mouseX, mouseY);
            } else if (effect.getPotion() == PotionClarity.INSTANCE) {
                gui.drawHoveringText(hearthsideLines(effect), mouseX, mouseY);
            }
            return;
        }
    }

    private static List<String> restedLines(PotionEffect effect) {
        List<String> text = new ArrayList<>();
        int tier = effect.getAmplifier() + 1;
        int seconds = Math.max(0, effect.getDuration() / 20);
        text.add(TextFormatting.GOLD + "Rested " + ComfortClientState.roman(tier)
                + TextFormatting.GRAY + String.format("  %d:%02d left", seconds / 60, seconds % 60));
        if (ComfortClientState.insight > 0.0f) {
            text.add(TextFormatting.YELLOW + String.format("Insight: +%.0f%% XP from orbs", ComfortClientState.insight * 100.0f));
        }
        if (ComfortClientState.vigorInterval > 0 && ComfortClientState.vigorInterval < 30) {
            text.add(TextFormatting.GREEN + "Vigor: stamina recharges every " + ComfortClientState.vigorInterval + " ticks (base 30)");
        }
        if (ComfortClientState.halfHearts > 0) {
            text.add(TextFormatting.RED + "Fortitude: +" + (ComfortClientState.halfHearts / 2) + " hearts, +"
                    + (ComfortClientState.halfFeathers / 2) + " feathers");
        }
        if (Loader.isModLoaded("thaumcraft")) {
            text.add(TextFormatting.AQUA + "At home: Hearthside (clears warp)");
        }
        return text;
    }

    private static List<String> hearthsideLines(PotionEffect effect) {
        List<String> text = new ArrayList<>();
        int tier = effect.getAmplifier() + 1;
        text.add(TextFormatting.AQUA + "Hearthside " + ComfortClientState.roman(tier));
        text.add(TextFormatting.AQUA + "Active while resting at home: clears temporary warp");
        text.add(TextFormatting.GRAY + "Clearing 1 temporary warp every " + PotionClarity.secondsPerWarp(effect.getAmplifier()) + " s");
        if (ComfortClientState.present) {
            float next = ComfortClientState.scoreTier <= 0 ? ComfortClientState.threshold1
                    : ComfortClientState.scoreTier == 1 ? ComfortClientState.threshold2
                    : ComfortClientState.threshold3;
            text.add(String.format("Comfort: %.0f", ComfortClientState.effective));
            if (ComfortClientState.scoreTier < 3) {
                text.add("Next tier at " + Math.round(next));
            }
            for (PacketComfortStatus.Line line : ComfortClientState.lines) {
                if (line.amount == 0.0f) continue;
                String sign = line.kind == 6 ? "+" : "-";
                text.add(ComfortClientState.lineLabel(line) + " " + sign + trim(Math.abs(line.amount)));
            }
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
