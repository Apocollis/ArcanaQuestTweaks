package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.mixin.comfort.AccessorGuiContainer;
import com.apocollis.aqtweaks.mixin.comfort.InvokerGuiScreen;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/** Item lines, and the Rested hover box anchored to the top effect row. */
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

    @SubscribeEvent
    public void onDraw(net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiInventory)) return;
        if (ComfortClientState.restedTicks <= 0 && ComfortClientState.granted <= 0) return;
        GuiInventory gui = (GuiInventory) event.getGui();
        int left = ((AccessorGuiContainer) gui).aqtweaks$getGuiLeft();
        int top = ((AccessorGuiContainer) gui).aqtweaks$getGuiTop();
        int x = left - 124;
        int y = top;
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        if (mouseX < x || mouseX > x + 120 || mouseY < y || mouseY > y + 32) return;
        Minecraft mc = Minecraft.getMinecraft();
        java.util.List<String> text = new java.util.ArrayList<>();
        int tier = Math.max(ComfortClientState.granted, ComfortClientState.scoreTier);
        text.add(TextFormatting.GOLD + "Rested " + ComfortClientState.roman(Math.max(1, tier))
                + TextFormatting.RESET + String.format("  %.0f", ComfortClientState.effective));
        float next = ComfortClientState.scoreTier <= 0 ? ComfortClientState.threshold1
                : ComfortClientState.scoreTier == 1 ? ComfortClientState.threshold2
                : ComfortClientState.threshold3;
        if (ComfortClientState.scoreTier < 3) {
            text.add("Next at " + Math.round(next));
        }
        for (PacketComfortStatus.Line line : ComfortClientState.lines) {
            if (line.amount == 0.0f) continue;
            String sign = line.kind == 6 ? "+" : "-";
            text.add(ComfortClientState.lineLabel(line) + " " + sign + trim(Math.abs(line.amount)));
        }
        if (ComfortClientState.insight > 0.0f) {
            text.add(String.format("Insight +%.0f%% XP", ComfortClientState.insight * 100.0f));
        }
        if (ComfortClientState.vigorInterval > 0 && ComfortClientState.granted > 0) {
            text.add("Vigor " + ComfortClientState.vigorInterval + " ticks");
        }
        if (ComfortClientState.halfHearts > 0) {
            text.add("Fortitude +" + (ComfortClientState.halfHearts / 2) + " hearts, +"
                    + (ComfortClientState.halfFeathers / 2) + " feathers");
        }
        text.add(TextFormatting.AQUA + "At home: Clarity");
        ((InvokerGuiScreen) gui).aqtweaks$drawHoveringText(text, mouseX, mouseY);
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
