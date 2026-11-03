package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Compact Rested panel (37 px), bottom left beside the health bar by default. Stays while a home status is present, then fades for 8s. */
@SideOnly(Side.CLIENT)
public final class ComfortHud extends Gui {

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ComfortClientState.panelVisible()) return;
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        ScaledResolution scaled = new ScaledResolution(mc);
        int panelWidth = 168;
        // 37 px so the bottom-left panel (top at H - 39) sits below the lowest chat line, which ends at H - 40.
        int panelHeight = 37;
        int sw = scaled.getScaledWidth();
        int sh = scaled.getScaledHeight();
        int x;
        int y;
        switch (anchorName(ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.panelAnchor)) {
            case "top-left":
                x = 6;
                y = 8;
                break;
            case "top-right":
                x = sw - panelWidth - 6;
                y = 34;
                break;
            case "center-right":
                x = sw - panelWidth - 6;
                y = sh / 2 - panelHeight / 2;
                break;
            case "bottom-left":
                panelWidth = Math.min(panelWidth, Math.max(100, (sw / 2 - 91) - 12));
                x = 6;
                y = sh - panelHeight - 2;
                break;
            case "bottom-right":
                x = sw - panelWidth - 6;
                y = sh - panelHeight - 8;
                break;
            default: // top-center, beneath the Hwyla tooltip
                x = (sw - panelWidth) / 2;
                y = 44;
                break;
        }
        x += ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.panelOffsetX;
        y += ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.panelOffsetY;

        int alpha = 0xC0;
        if (!ComfortClientState.present) {
            long age = mc.player.ticksExisted - ComfortClientState.changedAt;
            alpha = (int) (0xC0 * (1.0f - age / 160.0f));
            if (alpha < 16) return;
        }
        drawRect(x, y, x + panelWidth, y + panelHeight, (alpha << 24));

        String title = ComfortClientState.granted > 0
                ? "Rested " + ComfortClientState.roman(ComfortClientState.granted)
                : "Comfort";
        float next = ComfortClientState.scoreTier <= 0 ? ComfortClientState.threshold1
                : ComfortClientState.scoreTier == 1 ? ComfortClientState.threshold2
                : ComfortClientState.threshold3;
        String score = String.format("%.0f / %.0f", ComfortClientState.effective, next);
        font.drawStringWithShadow(title, x + 4, y + 2, 0xFFE0A040);
        font.drawStringWithShadow(score, x + panelWidth - 4 - font.getStringWidth(score), y + 2, 0xFFE8E0C8);

        float span = Math.max(1.0f, ComfortClientState.threshold3);
        int barWidth = panelWidth - 8;
        int filled = (int) (barWidth * Math.min(1.0f, ComfortClientState.effective / span));
        drawRect(x + 4, y + 12, x + 4 + barWidth, y + 15, 0xFF3A3228);
        drawRect(x + 4, y + 12, x + 4 + filled, y + 15, 0xFFD09040);

        int textWidth = panelWidth - 8;
        font.drawStringWithShadow(font.trimStringToWidth(nextLine(), textWidth), x + 4, y + 17, 0xFFC8C0B0);
        font.drawStringWithShadow(font.trimStringToWidth(heldBack(), textWidth), x + 4, y + 27, 0xFFE8D0C0);
    }

    /** Maps the saved value to an anchor; the old top, center and bottom values still work. */
    private static String anchorName(String configured) {
        String name = configured == null ? "" : configured.trim().toLowerCase(java.util.Locale.ROOT);
        switch (name) {
            case "top-left":
            case "top-right":
            case "center-right":
            case "bottom-left":
            case "bottom-right":
                return name;
            case "bottom":
                return "bottom-right";
            default:
                return "top-center";
        }
    }

    static String heldBack() {
        if (ComfortClientState.missingFurniture) return "Held back by: furniture";
        if (ComfortClientState.hurtTicks > 0) return "Held back by: hurt";
        if (ComfortClientState.attackTicks > 0) return "Held back by: attack";
        StringBuilder text = new StringBuilder("Held back by:");
        int shown = 0;
        int extra = 0;
        for (PacketComfortStatus.Line line : ComfortClientState.lines) {
            if (line.kind == 6 || line.amount <= 0.0f) continue;
            if (shown >= 3) {
                extra++;
                continue;
            }
            text.append(' ').append(ComfortClientState.lineLabel(line))
                    .append(" -").append(Math.round(line.amount));
            shown++;
        }
        if (shown == 0) return "Held back by: nothing";
        if (extra > 0) text.append(" +").append(extra).append(" more");
        return text.toString();
    }

    static String nextLine() {
        int nextTier = ComfortClientState.scoreTier + 1;
        if (nextTier > 3) return "Rested III";
        float threshold = nextTier == 1 ? ComfortClientState.threshold1
                : nextTier == 2 ? ComfortClientState.threshold2
                : ComfortClientState.threshold3;
        float gap = Math.max(0.0f, threshold - ComfortClientState.effective);
        String promote = ComfortClientState.promoteTicks >= 0
                ? "  Promotes in " + formatTicks(ComfortClientState.promoteTicks)
                : "";
        return "Next: " + ComfortClientState.roman(nextTier) + " at " + Math.round(threshold)
                + " (+" + Math.round(gap) + ")" + promote;
    }

    private static String formatTicks(int ticks) {
        int seconds = Math.max(0, ticks / 20);
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
