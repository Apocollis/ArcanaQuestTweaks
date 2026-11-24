package com.apocollis.aqtweaks.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.ClientModuleConfig;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import dev.sapphic.smarthud.config.SlotsConfig;
import dev.sapphic.smarthud.item.SlotItem;
import dev.sapphic.smarthud.item.cache.InventoryCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Smart HUD's item slots drawn as two rows when the right strip (past the offhand slot and quiver) is too short for
 * all of them. Row 1 sits where Smart HUD draws its single row and holds as many slots as reach the screen edge;
 * row 2 holds the rest one frame height above, right-aligned to row 1's right end. The drawing copies Smart HUD
 * 2.1.1's {@code SlotRenderer.draw(Pre)}, with its own slot cap ({@code Smart HUD Max Slots}) instead of Smart HUD's {@code slotLimit} for a right-handed layout. Client side only.
 */
public final class SmartHudRows {

    private static final int SLOT = 20;
    private static final int FRAME_H = 22;

    private SmartHudRows() {}

    /** True when it drew the slots itself (the caller then cancels Smart HUD's own pass). */
    public static boolean tryDraw(ScaledResolution res, ResourceLocation sheet) {
        ImmutableCollection<SlotItem> cache = InventoryCache.get();
        int slots = Math.max(1, Math.min(ClientModuleConfig.hotbarSide.smartHudMaxSlots, cache.size()));
        if (cache.isEmpty()) {
            return false;
        }
        int width = res.getScaledWidth();
        int origin = width / 2 + 98 + HotbarSideLayout.smartHudShift();
        int perRow = (width - origin - 2) / SLOT;
        if (perRow < 1) {
            return false;
        }
        perRow = Math.min(perRow, slots);
        int second = Math.min(slots - perRow, perRow);
        ImmutableList<SlotItem> items = cache.asList();
        Minecraft mc = Minecraft.getMinecraft();
        int height = res.getScaledHeight();

        drawRow(mc, sheet, items, 0, perRow, origin, height, 0);
        if (second > 0) {
            drawRow(mc, sheet, items, perRow, second, origin + SLOT * (perRow - second), height, FRAME_H);
        }

        if (mc.gameSettings.attackIndicator == 2) {
            mc.gameSettings.attackIndicator = Integer.MIN_VALUE;
        }
        return true;
    }

    private static void drawRow(Minecraft mc, ResourceLocation sheet, ImmutableList<SlotItem> items, int from,
            int count, int origin, int height, int lift) {
        int v = SlotsConfig.style.getTextureV();
        if (SlotsConfig.style.isTextured()) {
            mc.getTextureManager().bindTexture(sheet);
            int y = height - FRAME_H - lift;
            mc.ingameGUI.drawTexturedModalRect(origin, y, 0, v, 11, FRAME_H);
            for (int i = 0; i < (count - 1) * 2; i++) {
                mc.ingameGUI.drawTexturedModalRect(origin + 11 + 10 * i, y, i % 2 == 0 ? 32 : 22, v, 10, FRAME_H);
            }
            mc.ingameGUI.drawTexturedModalRect(origin + SLOT * count - 9, y, 11, v, 11, FRAME_H);
        }
        FontRenderer font = mc.fontRenderer;
        for (int i = 0; i < count; i++) {
            SlotItem item = items.get(from + i);
            ItemStack stack = item.stack();
            int x = origin + 3 + SLOT * i;
            int y = height - 19 - lift;
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
            if (!stack.isStackable() && SlotsConfig.overlays) {
                mc.getRenderItem().renderItemOverlays(font, stack, x, y);
            }
            RenderHelper.disableStandardItemLighting();
            if (item.count() > 1 && SlotsConfig.counts) {
                String label = item.abbreviatedCount();
                int labelX = origin + SLOT - font.getStringWidth(label) + SLOT * i;
                GlStateManager.disableDepth();
                font.drawString(label, (float) labelX, (float) (height - font.FONT_HEIGHT - 1 - lift), -1, true);
            }
        }
    }
}
