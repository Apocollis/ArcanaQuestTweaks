package com.apocollis.aqtweaks.stamina;

import com.elenai.elenaidodge2.ModConfig;
import com.elenai.elenaidodge2.gui.DodgeGui;
import com.elenai.elenaidodge2.util.ClientStorage;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

/**
 * Extended's layered feather bar, with the sheet's own colors.
 * Layer 0 is the painted blue pair. Layer 1 and above is green on the light tint sprites.
 * Absorption is the painted gold pair, one row of 20.
 * Client-only: loaded from {@code MixinDodgeGui}.
 */
public final class ElenaiFeatherHudColors {

    private static final int ICON = 9;
    private static final int SLOTS = 10;
    private static final int UNITS = 20;
    /** Width in pixels of the green half of a slot where bonus stamina covers an iron feather. */
    private static final int SPLIT = 4;
    /** Painted blue full / half on the Extended sheet (256-space). */
    private static final int BLUE_FULL = 34;
    private static final int BLUE_HALF = 25;

    private ElenaiFeatherHudColors() {}

    public static void renderLayeredBar(
            GuiIngame gui,
            int width,
            int y,
            int amount,
            int vOffset,
            int uBackground,
            int uWhiteHalf,
            int uWhiteFull,
            int uTintHalf,
            int uTintFull,
            boolean healing,
            boolean failed,
            boolean drawBackground) {
        if (!drawBackground) {
            amount = Math.min(amount, UNITS);
        }

        int xBase = width / 2 + 82 + ModConfig.client.hud.xOffset;
        if (drawBackground) {
            int top = Math.max(0, MathHelper.ceil(amount / 20.0F) - 1);
            if (top >= 1) {
                green();
            } else {
                white();
            }
            for (int slot = 0; slot < SLOTS; slot++) {
                gui.drawTexturedModalRect(xBase - slot * 8, y, uBackground, vOffset, ICON, ICON);
            }
            white();
        }

        int layers = MathHelper.ceil(amount / 20.0F);
        for (int slot = 0; slot < SLOTS; slot++) {
            int x = xBase - slot * 8;
            int threshold = slot * 2 + 1;
            int chosenLayer = -1;
            int chosenFill = 0;
            boolean half = false;
            for (int layer = 0; layer < layers; layer++) {
                int fill = MathHelper.clamp(amount - layer * UNITS, 0, UNITS);
                if (threshold < fill) {
                    chosenLayer = layer;
                    chosenFill = fill;
                    half = false;
                } else if (threshold == fill) {
                    chosenLayer = layer;
                    chosenFill = fill;
                    half = true;
                }
            }
            if (chosenLayer < 0 || chosenFill <= 0) {
                continue;
            }
            int u;
            if (!drawBackground || chosenLayer == 0) {
                white();
                u = half ? uWhiteHalf : uWhiteFull;
            } else {
                green();
                u = half ? uTintHalf : uTintFull;
            }
            gui.drawTexturedModalRect(x, y, u, vOffset, ICON, ICON);
            white();
        }

        boolean flash = failed && ModConfig.client.hud.flash;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!drawBackground && amount <= slot * 2) {
                continue;
            }
            int x = xBase - slot * 8;
            if (healing) {
                gui.drawTexturedModalRect(x, y, 16, 9, ICON, ICON);
            } else if (flash) {
                gui.drawTexturedModalRect(x, y, 43, 9, ICON, ICON);
            }
        }
    }

    /**
     * Armor-weight bar. Usable feathers use the blue pair. Blocked slots stay the gray iron row.
     *
     * <p>Bonus stamina above 20 (the green layer of the unarmored bar) is drawn over this row from the left slot
     * to the right, in the sheet's green tint. Where it reaches an iron slot, that slot is split: the left
     * {@link #SPLIT} pixels green, the rest iron, so neither the bonus nor the blocked part is hidden.
     */
    public static void renderBaseWeightBar(
            GuiIngame gui,
            int width,
            int y,
            int usable,
            int cap,
            int blocked,
            int vPatron,
            int uOutline,
            int uTintHalf,
            int uTintFull,
            boolean healing,
            boolean failed) {
        int xBase = width / 2 + 82 + ModConfig.client.hud.xOffset;
        white();
        for (int slot = 0; slot < SLOTS; slot++) {
            gui.drawTexturedModalRect(xBase - slot * 8, y, uOutline, vPatron, ICON, ICON);
        }

        int vIron = vPatron + 9;
        int shown = Math.max(0, cap - blocked);
        boolean flash = failed && ModConfig.client.hud.flash;
        // Units of bonus stamina above the base row (the unarmored bar's first green layer), from current stamina.
        int bonus = MathHelper.clamp(ClientStorage.dodges - UNITS, 0, UNITS);
        for (int slot = 0; slot < SLOTS; slot++) {
            int x = xBase - (9 - slot) * 8;
            int odd = slot * 2 + 1;
            int remainder = shown - odd;
            boolean iron = false;
            if (odd <= shown && remainder < usable) {
                white();
                gui.drawTexturedModalRect(x, y, BLUE_FULL, vPatron, ICON, ICON);
            } else if (odd <= shown && remainder == usable) {
                white();
                gui.drawTexturedModalRect(x, y, BLUE_HALF, vPatron, ICON, ICON);
            } else if (odd < shown) {
                continue;
            } else if (odd == shown) {
                iron = true;
                gui.drawTexturedModalRect(x, y, BLUE_FULL, vIron, ICON, ICON);
            } else if (odd < cap) {
                iron = true;
                gui.drawTexturedModalRect(x, y, BLUE_HALF, vIron, ICON, ICON);
            } else if (odd == cap) {
                iron = true;
                gui.drawTexturedModalRect(x, y, BLUE_FULL, vIron, ICON, ICON);
            }
            if (odd <= bonus) {
                int u = odd < bonus ? uTintFull : uTintHalf;
                green();
                gui.drawTexturedModalRect(x, y, u, vPatron, iron ? SPLIT : ICON, ICON);
                white();
            }
            if (healing) {
                gui.drawTexturedModalRect(x, y, 16, 9, ICON, ICON);
            } else if (flash) {
                gui.drawTexturedModalRect(x, y, 43, 9, ICON, ICON);
            }
        }
    }

    private static void white() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, DodgeGui.alpha);
    }

    private static void green() {
        GlStateManager.color(0.0F, 1.0F, 0.0F, DodgeGui.alpha);
    }
}
