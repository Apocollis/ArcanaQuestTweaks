package com.apocollis.aqtweaks.client.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.aurorian.ClientMoonfrost;
import com.apocollis.aqtweaks.aurorian.MoonfrostState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Frost vignette on the HUD. Alpha follows the rime stack (0..15) the server sends; drawn with gradient
 * rects only (no texture). Hidden while the chat is open, like other Tweaks HUD pieces.
 */
@SideOnly(Side.CLIENT)
public class AurorianFrostOverlay extends Gui {

    private static final int EDGE_FRACTION_DIVISOR = 4;
    private static final float MAX_ALPHA = 0.62F;

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.enableFrostOverlay) return;
        int rime = ClientMoonfrost.rime;
        if (rime <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || !MoonfrostState.isAurorian(mc.world) || mc.currentScreen instanceof GuiChat) return;

        float strength = Math.min(1.0F, rime / 15.0F);
        ScaledResolution res = event.getResolution();
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();
        int edgeX = Math.max(8, (int) (w / (float) EDGE_FRACTION_DIVISOR * (0.35F + 0.65F * strength)));
        int edgeY = Math.max(8, (int) (h / (float) EDGE_FRACTION_DIVISOR * (0.35F + 0.65F * strength)));
        int solid = argb((int) (MAX_ALPHA * strength * 255.0F), 0xD8, 0xEC, 0xFF);
        int clear = argb(0, 0xD8, 0xEC, 0xFF);

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        drawGradientRect(0, 0, w, edgeY, solid, clear);                 // top
        drawGradientRect(0, h - edgeY, w, h, clear, solid);             // bottom
        drawSide(0, 0, edgeX, h, solid, clear, true);                   // left
        drawSide(w - edgeX, 0, w, h, solid, clear, false);              // right
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
    }

    /** Vertical gradients cannot fade horizontally, so the side bands are built from thin strips. */
    private void drawSide(int left, int top, int right, int bottom, int solid, int clear, boolean solidOnLeft) {
        int width = right - left;
        int steps = Math.max(1, Math.min(24, width));
        int alphaSolid = solid >>> 24;
        for (int i = 0; i < steps; i++) {
            float t = solidOnLeft ? 1.0F - (i + 0.5F) / steps : (i + 0.5F) / steps;
            int a = (int) (alphaSolid * t * t);
            int x0 = left + (int) ((long) width * i / steps);
            int x1 = left + (int) ((long) width * (i + 1) / steps);
            if (x1 > x0) {
                drawRect(x0, top, x1, bottom, argb(a, 0xD8, 0xEC, 0xFF));
            }
        }
    }

    private static int argb(int a, int r, int g, int b) {
        return (Math.max(0, Math.min(255, a)) << 24) | (r << 16) | (g << 8) | b;
    }
}
