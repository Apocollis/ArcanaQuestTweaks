package com.apocollis.aqtweaks.portal.client;

import com.apocollis.aqtweaks.portal.EntityArcaneRift;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RiftParticles {

    private static final int BURST = 2;
    private static final double HEIGHT = 2.4;
    private static final double MAX_RADIUS = 0.70;

    private RiftParticles() {}

    public static void emit(EntityArcaneRift rift) {
        int remaining = rift.getRemainingTicks();
        double cx = rift.posX;
        double cy = rift.posY;
        double cz = rift.posZ;
        var rand = rift.world.rand;
        boolean wild = rift.isWild();
        float r = wild ? 0.95F : 0.55F;
        float g = wild ? 0.12F : 0.2F;
        float b = wild ? 0.08F : 0.85F;
        for (int i = 0; i < BURST; i++) {
            double radius = rand.nextDouble() * MAX_RADIUS;
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            double y = cy + rand.nextDouble() * HEIGHT;
            spawnTinted(EnumParticleTypes.DRAGON_BREATH,
                    cx + ox, y, cz + oz,
                    ox * -0.008, 0.02 + rand.nextDouble() * 0.03, oz * -0.008,
                    r, g, b);
        }
        if (remaining <= 1) {
            for (int i = 0; i < 4; i++) {
                double radius = rand.nextDouble() * MAX_RADIUS;
                double angle = rand.nextDouble() * Math.PI * 2.0;
                rift.world.spawnParticle(EnumParticleTypes.CLOUD,
                        cx + Math.cos(angle) * radius,
                        cy + 1.0,
                        cz + Math.sin(angle) * radius,
                        0.0, 0.05, 0.0);
            }
        }
    }

    private static void spawnTinted(EnumParticleTypes type, double x, double y, double z,
            double mx, double my, double mz, float red, float green, float blue) {
        Particle particle = Minecraft.getMinecraft().effectRenderer.spawnEffectParticle(
                type.getParticleID(), x, y, z, mx, my, mz);
        if (particle != null) {
            particle.setRBGColorF(red, green, blue);
        }
    }
}
