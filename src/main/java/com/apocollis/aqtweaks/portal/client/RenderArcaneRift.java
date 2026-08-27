package com.apocollis.aqtweaks.portal.client;

import com.apocollis.aqtweaks.portal.EntityArcaneRift;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderArcaneRift extends Render<EntityArcaneRift> {

    private static final int SEGS = 8;
    private static final int VSEGS = 4;
    private static final float HEIGHT = 2.4F;

    public RenderArcaneRift(RenderManager renderManager) {
        super(renderManager);
        shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntityArcaneRift entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.lastClientFxTick != entity.ticksExisted) {
            entity.lastClientFxTick = entity.ticksExisted;
            RiftParticles.emit(entity);
        }

        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                .getAtlasSprite("minecraft:blocks/portal");
        int remaining = entity.getRemainingTicks();
        float life = remaining > 100 ? 1.0F : Math.max(0.0F, remaining / 100.0F);
        float alpha = 0.25F + 0.50F * life;
        float scale = 0.9F + 0.1F * life;
        float time = entity.ticksExisted + partialTicks;
        float vScroll = time * 0.012F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        float cr = 1.0F;
        float cg = 1.0F;
        float cb = 1.0F;
        if (entity.isWild()) {
            cr = 1.0F;
            cg = 0.22F;
            cb = 0.18F;
        }

        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();
        float uSpan = maxU - minU;
        float vSpan = maxV - minV;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buf = tessellator.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < SEGS; i++) {
            float a0 = (float) (i * Math.PI * 2.0 / SEGS);
            float a1 = (float) ((i + 1) * Math.PI * 2.0 / SEGS);
            float u0 = minU + (i / (float) SEGS) * uSpan;
            float u1 = minU + ((i + 1) / (float) SEGS) * uSpan;
            for (int k = 0; k < VSEGS; k++) {
                float n0 = k / (float) VSEGS;
                float n1 = (k + 1) / (float) VSEGS;
                if (n1 >= 1.0F) {
                    n1 = 0.999F;
                }
                float y0 = n0 * HEIGHT;
                float y1 = n1 * HEIGHT;
                float vf0 = wrapUnit(n0 - vScroll);
                float vf1 = wrapUnit(n1 - vScroll);
                if (vf1 < vf0) {
                    float splitN = n0 + (n1 - n0) * ((1.0F - vf0) / (1.0F - vf0 + vf1));
                    float splitY = splitN * HEIGHT;
                    putRing(buf, a0, a1, u0, u1, y0, splitY, n0, splitN, vf0, 1.0F, minV, vSpan, time, cr, cg, cb, alpha);
                    putRing(buf, a0, a1, u0, u1, splitY, y1, splitN, n1, 0.0F, vf1, minV, vSpan, time, cr, cg, cb, alpha);
                } else {
                    putRing(buf, a0, a1, u0, u1, y0, y1, n0, n1, vf0, vf1, minV, vSpan, time, cr, cg, cb, alpha);
                }
            }
        }
        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private static void putRing(BufferBuilder buf, float a0, float a1, float u0, float u1,
            float y0, float y1, float n0, float n1, float vf0, float vf1, float minV, float vSpan, float time,
            float cr, float cg, float cb, float alpha) {
        float v0 = minV + (1.0F - vf0) * vSpan;
        float v1 = minV + (1.0F - vf1) * vSpan;
        ringVert(buf, a0, y0, n0, u0, v0, time, cr, cg, cb, alpha);
        ringVert(buf, a1, y0, n0, u1, v0, time, cr, cg, cb, alpha);
        ringVert(buf, a1, y1, n1, u1, v1, time, cr, cg, cb, alpha);
        ringVert(buf, a0, y1, n1, u0, v1, time, cr, cg, cb, alpha);
    }

    private static void ringVert(BufferBuilder buf, float angle, float y, float yNorm, float u, float v, float time,
            float cr, float cg, float cb, float alpha) {
        float radius = 0.8F
                + 0.14F * (float) Math.sin(angle * 3.0 + time * 0.12)
                + 0.10F * (float) Math.sin(yNorm * 4.0 + time * 0.16);
        float px = (float) Math.cos(angle) * radius;
        float pz = (float) Math.sin(angle) * radius;
        buf.pos(px, y, pz).tex(u, v).color(cr, cg, cb, alpha).endVertex();
    }

    private static float wrapUnit(float value) {
        return value - (float) Math.floor(value);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityArcaneRift entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
