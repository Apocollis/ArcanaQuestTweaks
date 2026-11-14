package com.apocollis.aqtweaks.client.twilightforest;

import com.apocollis.aqtweaks.twilightforest.ClientLey;
import com.apocollis.aqtweaks.twilightforest.LeyEruptionShape;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Ley-Rupture rune or eruption mote. It starts at the spawn floor and climbs at a steady speed to its own peak
 * (Y50 to Y55): runes (enchant glyphs) zigzag sideways, the other shapes (wisps, sparks, dots) burst with a chaotic
 * jitter. It passes through ordinary blocks (stone, dirt, trees, leaves) and stops only inside the box of a sheltered
 * structure; it is full bright, fades in at the start and from Y50 dissolves at its own peak. Spawned straight into
 * the particle manager (so never distance culled) and counted against {@code Max Ley Particles}; tinted 70 percent
 * violet and 30 percent emerald; depth tested.
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

    private final boolean rune;
    private final boolean vent;
    private final float baseScale;
    private final float snap;
    private final int snapEvery;
    private final double rise;
    private final double peakY;
    private boolean counted = true;

    /**
     * @param rune          an enchant glyph (true) or a wisp, spark or dot (false)
     * @param peakY         the height this particle dissolves at, 50 to 55
     * @param rise          blocks per tick, steady
     */
    public ParticleLeyEruption(World world, double x, double y, double z, boolean rune, boolean vent,
                               float distanceScale, double peakY, double rise) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        ACTIVE_COUNT.incrementAndGet();
        if (vent) VENT_COUNT.incrementAndGet();
        this.rune = rune;
        this.vent = vent;
        this.peakY = peakY;
        this.rise = rise;
        this.canCollide = false;
        this.particleGravity = 0.0F;

        int rgb = LeyEruptionShape.colour(this.rand);
        this.particleRed = ((rgb >> 16) & 0xFF) / 255.0F;
        this.particleGreen = ((rgb >> 8) & 0xFF) / 255.0F;
        this.particleBlue = (rgb & 0xFF) / 255.0F;
        this.particleAlpha = 0.0F;
        this.particleMaxAge = LeyEruptionShape.maxAge(y, peakY, rise);
        this.motionY = rise;

        if (rune) {
            this.setParticleTextureIndex(LeyEruptionShape.glyphSprite(this.rand));
            this.motionX = (this.rand.nextDouble() - 0.5D) * 0.16D;
            this.motionZ = (this.rand.nextDouble() - 0.5D) * 0.16D;
            this.baseScale = (1.0F + this.rand.nextFloat() * 0.5F) * distanceScale;
            this.snap = 0.08F + this.rand.nextFloat() * 0.06F;
            this.snapEvery = 3 + this.rand.nextInt(3);
        } else {
            this.setParticleTextureIndex(LeyEruptionShape.eruptionSprite(this.rand));
            double kick = 0.10D + this.rand.nextDouble() * 0.14D;
            double ang = this.rand.nextDouble() * Math.PI * 2.0D;
            this.motionX = Math.cos(ang) * kick;
            this.motionZ = Math.sin(ang) * kick;
            this.baseScale = (0.8F + this.rand.nextFloat() * 0.6F) * distanceScale;
            this.snap = 0.08F;
            this.snapEvery = 2 + this.rand.nextInt(2);
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

        if (this.particleAge++ >= this.particleMaxAge || this.posY >= this.peakY) {
            this.setExpired();
            return;
        }

        if (this.particleAge % this.snapEvery == 0) {
            // A sharp sideways snap: the zigzag.
            this.motionX = (this.rand.nextDouble() * 2.0D - 1.0D) * this.snap;
            this.motionZ = (this.rand.nextDouble() * 2.0D - 1.0D) * this.snap;
        }
        if (this.rune) {
            this.motionX *= 0.92D;
            this.motionZ *= 0.92D;
        } else {
            this.motionX = this.motionX * 0.86D + (this.rand.nextDouble() - 0.5D) * 0.08D;
            this.motionZ = this.motionZ * 0.86D + (this.rand.nextDouble() - 0.5D) * 0.08D;
            this.particleScale = this.baseScale * (0.85F + this.rand.nextFloat() * 0.3F);
        }
        // The climb is steady: no drag, so every particle reaches its peak.
        this.motionY = this.rise;
        this.move(this.motionX, this.motionY, this.motionZ);
        this.particleAlpha = LeyEruptionShape.alpha(this.particleAge, this.particleMaxAge, this.posY, this.peakY);

        // Ordinary blocks do not stop a column; the box of a sheltered structure does.
        if ((this.particleAge & 1) == 0 && ClientLey.isInShelter(this.posX, this.posY, this.posZ)) {
            this.setExpired();
        }
    }
}
