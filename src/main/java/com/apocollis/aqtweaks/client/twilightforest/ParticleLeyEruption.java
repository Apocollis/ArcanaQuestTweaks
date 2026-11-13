package com.apocollis.aqtweaks.client.twilightforest;

import com.apocollis.aqtweaks.twilightforest.LeyEruptionShape;

import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Ley-Rupture glyph or eruption mote. Glyphs rise and zigzag for about two seconds and fade; eruption bolts burst
 * upward with a chaotic jitter. Spawned straight into the particle manager (so never distance culled), counted
 * against {@code Max Ley Particles}, full-bright, tinted 70 percent violet and 30 percent emerald, depth tested.
 */
@SideOnly(Side.CLIENT)
public class ParticleLeyEruption extends Particle {

    public static final AtomicInteger ACTIVE_COUNT = new AtomicInteger(0);
    public static final AtomicInteger VENT_COUNT = new AtomicInteger(0);

    public static int active() {
        return ACTIVE_COUNT.get();
    }

    public static int vent() {
        return VENT_COUNT.get();
    }

    /** The particle manager clears particles without calling {@code setExpired}, so the counters are reset by hand. */
    public static void resetCounts() {
        ACTIVE_COUNT.set(0);
        VENT_COUNT.set(0);
    }

    private final boolean eruption;
    private final boolean vent;
    private final float baseScale;
    private final float snap;
    private final int snapEvery;
    private boolean counted = true;
    private final BlockPos.MutableBlockPos posCheck = new BlockPos.MutableBlockPos();

    public ParticleLeyEruption(World world, double x, double y, double z, boolean eruption, boolean vent,
                               boolean activePhase, float distanceScale) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        ACTIVE_COUNT.incrementAndGet();
        if (vent) VENT_COUNT.incrementAndGet();
        this.eruption = eruption;
        this.vent = vent;
        this.canCollide = false;
        this.particleGravity = 0.0F;

        int rgb = LeyEruptionShape.colour(this.rand);
        this.particleRed = ((rgb >> 16) & 0xFF) / 255.0F;
        this.particleGreen = ((rgb >> 8) & 0xFF) / 255.0F;
        this.particleBlue = (rgb & 0xFF) / 255.0F;
        this.particleAlpha = 0.0F;

        if (eruption) {
            this.setParticleTextureIndex(LeyEruptionShape.eruptionSprite(this.rand));
            this.particleMaxAge = LeyEruptionShape.eruptionLifetime(this.rand);
            this.motionY = LeyEruptionShape.eruptionRise(this.rand);
            double kick = 0.10D + this.rand.nextDouble() * 0.14D;
            double ang = this.rand.nextDouble() * Math.PI * 2.0D;
            this.motionX = Math.cos(ang) * kick;
            this.motionZ = Math.sin(ang) * kick;
            this.baseScale = (0.8F + this.rand.nextFloat() * 0.6F) * distanceScale;
            this.snap = 0.08F;
            this.snapEvery = 2 + this.rand.nextInt(2);
        } else {
            this.setParticleTextureIndex(LeyEruptionShape.glyphSprite(this.rand));
            this.particleMaxAge = LeyEruptionShape.glyphLifetime(this.rand);
            this.motionY = LeyEruptionShape.glyphRise(this.rand, activePhase);
            this.motionX = (this.rand.nextDouble() - 0.5D) * 0.16D;
            this.motionZ = (this.rand.nextDouble() - 0.5D) * 0.16D;
            this.baseScale = (1.0F + this.rand.nextFloat() * 0.5F) * distanceScale;
            this.snap = 0.08F + this.rand.nextFloat() * 0.06F;
            this.snapEvery = 3 + this.rand.nextInt(3);
        }
        this.particleScale = this.baseScale;
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return 15728880;
    }

    @Override
    public void setExpired() {
        super.setExpired();
        if (this.counted) {
            this.counted = false;
            ACTIVE_COUNT.decrementAndGet();
            if (this.vent) VENT_COUNT.decrementAndGet();
        }
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        if (this.particleAge % this.snapEvery == 0) {
            // A sharp sideways snap: the zigzag.
            this.motionX = (this.rand.nextDouble() * 2.0D - 1.0D) * this.snap;
            this.motionZ = (this.rand.nextDouble() * 2.0D - 1.0D) * this.snap;
        }
        if (this.eruption) {
            this.motionX = this.motionX * 0.86D + (this.rand.nextDouble() - 0.5D) * 0.08D;
            this.motionZ = this.motionZ * 0.86D + (this.rand.nextDouble() - 0.5D) * 0.08D;
            this.motionY = this.motionY * 0.96D + 0.012D;
            this.particleScale = this.baseScale * (0.85F + this.rand.nextFloat() * 0.3F);
        } else {
            this.motionX *= 0.92D;
            this.motionZ *= 0.92D;
            this.motionY *= 0.985D;
        }
        this.move(this.motionX, this.motionY, this.motionZ);
        this.particleAlpha = LeyEruptionShape.alpha(this.particleAge, this.particleMaxAge);

        // Stops cleanly on a roof or overhang; the first two ticks are exempt so a ground spawn does not die at once.
        // The fastest bolt moves under 0.8 blocks a tick, so a one block ceiling cannot be stepped over.
        if (this.particleAge > 2 && this.world.isBlockLoaded(this.posCheck.setPos(this.posX, this.posY, this.posZ))) {
            if (this.world.getBlockState(this.posCheck).getMaterial().isSolid()) {
                this.setExpired();
            }
        }
    }
}
