package com.apocollis.aqtweaks.reskillable.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/** Bright boxes around ores, drawn only for the player who prospected. */
public class ProspectorOutline {

    private static final List<BlockPos> marks = new ArrayList<>();
    private static long until;

    public static void show(List<BlockPos> ores) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || player.world == null) return;
        marks.clear();
        marks.addAll(ores);
        until = player.world.getTotalWorldTime() + 140;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (marks.isEmpty()) return;
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || player.world == null) return;
        if (player.world.getTotalWorldTime() > until) {
            marks.clear();
            return;
        }
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

        GlStateManager.pushMatrix();
        GlStateManager.translate(-px, -py, -pz);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(2.0f);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (BlockPos pos : marks) {
            box(buf, pos.getX(), pos.getY(), pos.getZ());
        }
        tess.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void box(BufferBuilder buf, int x, int y, int z) {
        double x0 = x;
        double y0 = y;
        double z0 = z;
        double x1 = x + 1.0;
        double y1 = y + 1.0;
        double z1 = z + 1.0;
        edge(buf, x0, y0, z0, x1, y0, z0);
        edge(buf, x1, y0, z0, x1, y0, z1);
        edge(buf, x1, y0, z1, x0, y0, z1);
        edge(buf, x0, y0, z1, x0, y0, z0);
        edge(buf, x0, y1, z0, x1, y1, z0);
        edge(buf, x1, y1, z0, x1, y1, z1);
        edge(buf, x1, y1, z1, x0, y1, z1);
        edge(buf, x0, y1, z1, x0, y1, z0);
        edge(buf, x0, y0, z0, x0, y1, z0);
        edge(buf, x1, y0, z0, x1, y1, z0);
        edge(buf, x1, y0, z1, x1, y1, z1);
        edge(buf, x0, y0, z1, x0, y1, z1);
    }

    private static void edge(BufferBuilder buf, double x1, double y1, double z1, double x2, double y2, double z2) {
        buf.pos(x1, y1, z1).color(255, 210, 40, 255).endVertex();
        buf.pos(x2, y2, z2).color(255, 210, 40, 255).endVertex();
    }
}
