package com.apocollis.aqtweaks.thaumcraft;

import baubles.api.BaublesApi;
import hellfirepvp.astralsorcery.common.event.RunicShieldingCalculateEvent;
import mod.emt.thaumictweaker.events.RunicShieldingHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.GuiIngameForge;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.lib.events.PlayerEvents;

/**
 * Client HUD for Thaumcraft runic shielding while Thaumic Tweaker stores the charge as absorption.
 * Not {@code @SideOnly}: the client mixin class references this type, and the dedicated-server
 * side transformer must not strip it.
 */
public final class RunicShieldHud {
    private static final int ROW = 10;
    private static final int ICON = 8;
    private static final float TEX = 0.00390625F;

    private static boolean active;
    private static int screenWidth;
    private static int screenHeight;
    private static int leftHeightAtHealth;

    private RunicShieldHud() {}

    public static void prepare(int width, int height) {
        active = false;
        screenWidth = width;
        screenHeight = height;
        leftHeightAtHealth = GuiIngameForge.left_height;
    }

    /** Absorption reported to {@code GuiIngameForge.renderHealth}. Zero hides the gold hearts. */
    public static float absorptionForHealth(EntityPlayer player) {
        active = replacing(player);
        if (active) {
            return 0F;
        }
        return player.getAbsorptionAmount();
    }

    public static boolean skipTweakerOverlay() {
        return active;
    }

    public static void draw() {
        if (!active) {
            return;
        }
        active = false;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) {
            return;
        }
        int cap = cap(player);
        if (cap <= 0) {
            return;
        }
        float absorption = player.getAbsorptionAmount();
        float shield = Math.min(absorption, cap);
        float surplus = Math.max(0F, absorption - cap);
        int icons = shield > 0F ? MathHelper.ceil(shield) : 0;
        int extraRuneRows = icons > ROW ? MathHelper.ceil(icons / (float) ROW) - 1 : 0;
        float goldPool = MathHelper.ceil(surplus);
        int goldHearts = goldPool > 0F ? MathHelper.ceil(goldPool / 2F) : 0;
        int goldRows = goldHearts > 0 ? MathHelper.ceil(goldHearts / (float) ROW) : 0;

        int left = screenWidth / 2 - 91;
        int healthRowY = screenHeight - leftHeightAtHealth;
        int above = screenHeight - GuiIngameForge.left_height;
        int hardcoreV = player.world.getWorldInfo().isHardcoreModeEnabled() ? 45 : 0;
        int margin = 16;
        if (player.isPotionActive(MobEffects.POISON)) {
            margin += 36;
        } else if (player.isPotionActive(MobEffects.WITHER)) {
            margin += 72;
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        mc.getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.color(1F, 1F, 1F, 1F);
        for (int i = ROW; i < icons; i++) {
            int row = i / ROW - 1;
            blit(left + (i % ROW) * ICON, above - row * ROW, 16, hardcoreV);
        }
        float goldLeft = goldPool;
        for (int i = goldHearts - 1; i >= 0; i--) {
            int row = i / ROW;
            int x = left + (i % ROW) * ICON;
            int y = above - extraRuneRows * ROW - row * ROW;
            blit(x, y, 16, hardcoreV);
            boolean half = goldLeft == goldPool && goldPool % 2F == 1F;
            blit(x, y, margin + (half ? 153 : 144), hardcoreV);
            goldLeft -= half ? 1F : 2F;
        }

        if (icons > 0) {
            mc.getTextureManager().bindTexture(ParticleEngine.particleTexture);
            GlStateManager.disableAlpha();
            for (int i = 0; i < icons; i++) {
                int row = i / ROW;
                int y = row == 0 ? healthRowY : above - (row - 1) * ROW;
                drawRune(left + (i % ROW) * ICON, y, i, player.ticksExisted);
            }
            GlStateManager.enableAlpha();
        }

        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.disableBlend();
        GuiIngameForge.left_height += (extraRuneRows + goldRows) * ROW;
    }

    private static void drawRune(int pixelX, int pixelY, int index, int ticks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(pixelX, pixelY, 0F);
        GlStateManager.scale(4F, 4F, 4F);
        GlStateManager.color(1F, 1F, 1F, 1F);
        UtilsFX.drawTexturedQuad(0F, 0F, 40F, 4F, 2.25F, 2.25F, 0D);
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        float pulse = MathHelper.sin(ticks / 4F + index) * 0.4F + 0.6F;
        GlStateManager.color(1F, 0.75F, 0.24F, pulse);
        UtilsFX.drawTexturedQuad(0F, 0F, index * 4F, 24F, 4F, 4F, 0D);
        GlStateManager.popMatrix();
        GlStateManager.popMatrix();
    }

    private static void blit(int x, int y, int u, int v) {
        float fu = u * TEX;
        float fv = v * TEX;
        float fw = 9F * TEX;
        float fh = 9F * TEX;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x, y + 9, 0D).tex(fu, fv + fh).endVertex();
        buf.pos(x + 9, y + 9, 0D).tex(fu + fw, fv + fh).endVertex();
        buf.pos(x + 9, y, 0D).tex(fu + fw, fv).endVertex();
        buf.pos(x, y, 0D).tex(fu, fv).endVertex();
        tess.draw();
    }

    private static boolean replacing(EntityPlayer player) {
        return player != null && cap(player) > 0 && !RunicShieldingHandler.ENABLE_NEW_RUNIC_SHIELDING;
    }

    /**
     * Same total {@code handleRunicArmor} stores in {@code runicInfo}, which that method only fills on the server.
     * Armor and baubles, then Astral's calculate event (its listener is safe on the client).
     */
    private static int cap(EntityPlayer player) {
        int total = 0;
        for (int slot = 0; slot < 4; slot++) {
            total += PlayerEvents.getRunicCharge(player.inventory.armorInventory.get(slot));
        }
        IInventory baubles = BaublesApi.getBaubles(player);
        if (baubles != null) {
            for (int slot = 0; slot < baubles.getSizeInventory(); slot++) {
                total += PlayerEvents.getRunicCharge(baubles.getStackInSlot(slot));
            }
        }
        return RunicShieldingCalculateEvent.fire(player, total);
    }
}
