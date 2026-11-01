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

/** Compact Rested panel on the right. Stays while a home status is present, then fades for 8s. */
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
        int panelHeight = 48;
        String anchor = ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.panelAnchor;
        int x = scaled.getScaledWidth() - panelWidth - 6 + ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.panelOffsetX;
        int y;
        if ("top".equalsIgnoreCase(anchor)) {
            y = 8;
        } else if ("bottom".equalsIgnoreCase(anchor)) {
            y = scaled.getScaledHeight() - panelHeight - 8;
        } else {
            y = scaled.getScaledHeight() / 2 - panelHeight / 2;
        }
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
        font.drawStringWithShadow(title, x + 4, y + 4, 0xFFE0A040);
        font.drawStringWithShadow(score, x + panelWidth - 4 - font.getStringWidth(score), y + 4, 0xFFE8E0C8);

        float span = Math.max(1.0f, ComfortClientState.threshold3);
        int barWidth = panelWidth - 8;
        int filled = (int) (barWidth * Math.min(1.0f, ComfortClientState.effective / span));
        drawRect(x + 4, y + 16, x + 4 + barWidth, y + 20, 0xFF3A3228);
        drawRect(x + 4, y + 16, x + 4 + filled, y + 20, 0xFFD09040);

        String held = heldBack();
        font.drawStringWithShadow(held, x + 4, y + 24, 0xFFE8D0C0);
        String nextLine = nextLine();
        font.drawStringWithShadow(nextLine, x + 4, y + 36, 0xFFC8C0B0);
    }

    private static String heldBack() {
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

    private static String nextLine() {
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
