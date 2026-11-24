package com.apocollis.aqtweaks.starter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Drawing helpers shared by the two starter screens: nine-slice frames from the mod's own textures and scissoring. */
final class StarterDraw {

    private static final String DIR = "textures/gui/starter/";

    static final ResourceLocation PANEL = tex("panel");
    static final ResourceLocation OPTION = tex("option");
    static final ResourceLocation OPTION_HOVER = tex("option_hover");
    static final ResourceLocation OPTION_SELECTED = tex("option_selected");
    static final ResourceLocation BUTTON = tex("button");
    static final ResourceLocation BUTTON_HOVER = tex("button_hover");
    static final ResourceLocation BUTTON_DISABLED = tex("button_disabled");
    static final ResourceLocation DIVIDER = tex("divider");

    // Stoneborn palette (see scripts/generate_starter_textures.ps1).
    static final int GOLD = 0xE0B24A;
    static final int TEXT = 0xD8CDB8;
    static final int TEXT_INTRO = 0xC9BFAA;
    static final int TEXT_DIM = 0x8F8575;

    private StarterDraw() {}

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("aqtweaks", DIR + name + ".png");
    }

    /** A texture from the starter folder by short name (emblems). */
    static ResourceLocation emblem(String namespace, String path) {
        return new ResourceLocation(namespace, DIR + path + ".png");
    }

    /**
     * Draws {@code tex} (texW x texH) stretched to w x h, keeping a {@code corner}-pixel border at native size.
     */
    static void nineSlice(ResourceLocation tex, int x, int y, int w, int h, int texW, int texH, int corner, float alpha) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int c = Math.max(1, Math.min(corner, Math.min(w, h) / 2));
        Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1f, 1f, 1f, alpha);
        int mw = w - 2 * c;
        int mh = h - 2 * c;
        int sw = texW - 2 * corner;
        int sh = texH - 2 * corner;
        // Corners (source corner size is the texture's, destination may be clamped on tiny boxes).
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, corner, corner, c, c, texW, texH);
        Gui.drawScaledCustomSizeModalRect(x + w - c, y, texW - corner, 0, corner, corner, c, c, texW, texH);
        Gui.drawScaledCustomSizeModalRect(x, y + h - c, 0, texH - corner, corner, corner, c, c, texW, texH);
        Gui.drawScaledCustomSizeModalRect(x + w - c, y + h - c, texW - corner, texH - corner, corner, corner, c, c, texW, texH);
        if (mw > 0) {
            Gui.drawScaledCustomSizeModalRect(x + c, y, corner, 0, sw, corner, mw, c, texW, texH);
            Gui.drawScaledCustomSizeModalRect(x + c, y + h - c, corner, texH - corner, sw, corner, mw, c, texW, texH);
        }
        if (mh > 0) {
            Gui.drawScaledCustomSizeModalRect(x, y + c, 0, corner, corner, sh, c, mh, texW, texH);
            Gui.drawScaledCustomSizeModalRect(x + w - c, y + c, texW - corner, corner, corner, sh, c, mh, texW, texH);
        }
        if (mw > 0 && mh > 0) {
            Gui.drawScaledCustomSizeModalRect(x + c, y + c, corner, corner, sw, sh, mw, mh, texW, texH);
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** The 32x4 divider stretched across {@code w}. */
    static void divider(int x, int y, int w, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(DIVIDER);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, alpha);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 32, 4, w, 4, 32, 4);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Starts a GL scissor box given in GUI coordinates. Call {@link #endScissor()} afterwards. */
    static void beginScissor(Minecraft mc, int guiX, int guiY, int guiW, int guiH) {
        int f = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(guiX * f, mc.displayHeight - (guiY + guiH) * f, Math.max(0, guiW * f), Math.max(0, guiH * f));
    }

    static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** A button face from the button textures. */
    static void button(int x, int y, int w, int h, boolean enabled, boolean hover, float alpha) {
        ResourceLocation t = !enabled ? BUTTON_DISABLED : hover ? BUTTON_HOVER : BUTTON;
        nineSlice(t, x, y, w, h, 32, 20, 5, alpha);
    }
}
