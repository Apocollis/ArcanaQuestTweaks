package com.apocollis.aqtweaks.thaumadditions.client;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.ParticleEngine;

/**
 * Draws the Aegis shield: a translucent blue panel in front of a player who is holding it, carrying the runic
 * glyphs Thaumcraft's runic shielding uses (the same particle-atlas cells {@code RunicShieldHud} draws). One rune per
 * 5 points of pool left; a rune flashes and fades as a hit takes its share.
 */
@SideOnly(Side.CLIENT)
public class AegisShieldRenderer {
    private static final ResourceLocation PANEL = new ResourceLocation("aqtweaks", "textures/entity/aegis_shield.png");
    private static final ResourceLocation CRACKS = new ResourceLocation("aqtweaks",
            "textures/entity/aegis_shield_cracks.png");

    private static final double DISTANCE = 0.9D;
    private static final double HALF_WIDTH = 0.6D;
    private static final double BOTTOM = 0.05D;
    private static final double TOP = 1.85D;
    private static final float GLYPH_HALF = 0.11F;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Map<Integer, AegisClientState.Shield> shields = AegisClientState.shields();
        if (shields.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;
        float pt = event.getPartialTicks();
        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        mc.entityRenderer.disableLightmap();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();

        for (Map.Entry<Integer, AegisClientState.Shield> entry : shields.entrySet()) {
            Entity entity = mc.world.getEntityByID(entry.getKey());
            if (!(entity instanceof EntityLivingBase) || entity.isDead || entity.isInvisible()) continue;
            drawShield(mc, (EntityLivingBase) entity, entry.getValue(), pt, camX, camY, camZ);
        }

        GlStateManager.enableAlpha();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        mc.entityRenderer.enableLightmap();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private static void drawShield(Minecraft mc, EntityLivingBase entity, AegisClientState.Shield s, float pt, double camX,
                                   double camY, double camZ) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * pt - camX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * pt - camY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * pt - camZ;
        float yaw = entity.prevRotationYawHead
                + MathHelper.wrapDegrees(entity.rotationYawHead - entity.prevRotationYawHead) * pt;
        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        double rx = Math.cos(rad);
        double rz = Math.sin(rad);

        boolean firstPerson = entity == mc.player && mc.gameSettings.thirdPersonView == 0;
        float fraction = s.maxPool <= 0.0F ? 0.0F : MathHelper.clamp(s.pool / s.maxPool, 0.0F, 1.0F);
        float flash = s.flash > 0 ? s.flash / 6.0F : 0.0F;
        float red = 0.10F + 0.45F * (1.0F - fraction) + 0.45F * flash;
        float green = 0.55F + 0.35F * (1.0F - fraction) + 0.4F * flash;
        float blue = 1.0F;
        float alpha = (firstPerson ? 0.16F : 0.34F) + 0.25F * flash;

        double cx = x + dx * DISTANCE;
        double cz = z + dz * DISTANCE;
        Tessellator tess = Tessellator.getInstance();

        mc.getTextureManager().bindTexture(PANEL);
        GlStateManager.color(Math.min(1.0F, red), Math.min(1.0F, green), blue, Math.min(1.0F, alpha));
        quad(tess, cx, y + BOTTOM, cz, rx * HALF_WIDTH, rz * HALF_WIDTH, TOP - BOTTOM, 0.0F, 1.0F);

        if (fraction < 0.5F) {
            mc.getTextureManager().bindTexture(CRACKS);
            GlStateManager.color(1.0F, 1.0F, 1.0F, Math.min(1.0F, (0.5F - fraction) * 2.0F) * (firstPerson ? 0.5F : 0.8F));
            quad(tess, cx + dx * 0.004D, y + BOTTOM, cz + dz * 0.004D, rx * HALF_WIDTH, rz * HALF_WIDTH,
                    TOP - BOTTOM, 0.0F, 1.0F);
        }

        drawRunes(mc, tess, s, cx, y, cz, dx, dz, rx, rz);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** A quad standing at (x, y, z), spanning +-(rx, rz) sideways and {@code height} up, drawn double sided. */
    private static void quad(Tessellator tess, double x, double y, double z, double rx, double rz, double height,
                             float u0, float u1) {
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x - rx, y, z - rz).tex(u0, 1.0D).endVertex();
        buf.pos(x + rx, y, z + rz).tex(u1, 1.0D).endVertex();
        buf.pos(x + rx, y + height, z + rz).tex(u1, 0.0D).endVertex();
        buf.pos(x - rx, y + height, z - rz).tex(u0, 0.0D).endVertex();
        tess.draw();
    }

    private static void drawRunes(Minecraft mc, Tessellator tess, AegisClientState.Shield s, double cx, double y,
                                  double cz, double dx, double dz, double rx, double rz) {
        int slots = s.maxRunes();
        mc.getTextureManager().bindTexture(ParticleEngine.particleTexture);
        for (int i = 0; i < slots; i++) {
            float a = s.runeAlpha[i];
            if (a < 0.02F) continue;
            double across = ((i + 0.5D) / slots - 0.5D) * 0.8D;
            double norm = (i + 0.5D) / slots * 2.0D - 1.0D;
            double up = y + 1.05D + 0.14D * (1.0D - norm * norm);
            float pulse = MathHelper.sin(s.ticks / 4.0F + i) * 0.4F + 0.6F;
            float flash = s.runeFlash[i];
            float r = 1.0F;
            float g = 0.75F + 0.25F * flash;
            float b = 0.24F + 0.76F * flash;
            float alpha = Math.min(1.0F, a * (pulse + flash));
            GlStateManager.color(r, g, b, alpha);
            double px = cx + rx * across;
            double pz = cz + rz * across;
            glyph(tess, i, px + dx * 0.012D, up, pz + dz * 0.012D, rx, rz, false);
            glyph(tess, i, px - dx * 0.012D, up, pz - dz * 0.012D, rx, rz, true);
        }
    }

    /** One 16 px cell of Thaumcraft's particle atlas (a 1024 px sheet; cell {@code i} of the rune row at y 96). */
    private static void glyph(Tessellator tess, int index, double x, double y, double z, double rx, double rz,
                              boolean mirror) {
        float u0 = index / 64.0F;
        float u1 = u0 + 1.0F / 64.0F;
        float v0 = 24.0F / 256.0F;
        float v1 = v0 + 1.0F / 64.0F;
        if (mirror) {
            float t = u0;
            u0 = u1;
            u1 = t;
        }
        double hx = rx * GLYPH_HALF;
        double hz = rz * GLYPH_HALF;
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x - hx, y - GLYPH_HALF, z - hz).tex(u0, v1).endVertex();
        buf.pos(x + hx, y - GLYPH_HALF, z + hz).tex(u1, v1).endVertex();
        buf.pos(x + hx, y + GLYPH_HALF, z + hz).tex(u1, v0).endVertex();
        buf.pos(x - hx, y + GLYPH_HALF, z - hz).tex(u0, v0).endVertex();
        tess.draw();
    }
}
