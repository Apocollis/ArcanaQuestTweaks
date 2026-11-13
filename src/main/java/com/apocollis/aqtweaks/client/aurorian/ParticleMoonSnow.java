package com.apocollis.aqtweaks.client.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.aurorian.MoonSnowShape;
import com.apocollis.aqtweaks.aurorian.MoonSnowShape.SnowClass;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Blizzard snow for the Moonfrost storm. One class per sprite family (specks, flakes, pellets, soft puffs, streaks),
 * falling diagonally down with the shared wind, pulsing with the gusts, dying on solid blocks (end of the step and
 * points along it) and under roofs. Only the soft puffs draw with the depth test off.
 */
@SideOnly(Side.CLIENT)
public class ParticleMoonSnow extends Particle {

    private final SnowClass snowClass;
    private final float hSpeed;
    private final float vSpeed;
    private final float baseAlpha;
    private final boolean perimeter;
    private final float wobblePhase;
    private boolean counted = true;

    public ParticleMoonSnow(World world, double x, double y, double z, SnowClass snowClass, float hSpeed, float vSpeed,
                            float scale, float maxAlpha, int lifetime, boolean perimeter) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        MoonSnowCounters.ACTIVE.incrementAndGet();
        if (perimeter) MoonSnowCounters.PERIMETER.incrementAndGet();
        this.snowClass = snowClass;
        this.hSpeed = hSpeed;
        this.vSpeed = vSpeed;
        this.perimeter = perimeter;
        this.wobblePhase = this.rand.nextFloat() * 6.28F;
        this.canCollide = false;
        this.particleGravity = 0.0F;

        float tint = (this.rand.nextFloat() - 0.5F) * 0.05F;
        this.particleRed = clamp(0.82F + tint);
        this.particleGreen = clamp(0.91F + tint);
        this.particleBlue = 1.0F;

        this.baseAlpha = maxAlpha;
        this.particleAlpha = 0.0F;
        this.particleScale = scale;
        this.particleMaxAge = lifetime;
        this.particleAge = 0;
        this.setParticleTextureIndex(MoonSnowShape.sprite(snowClass, this.rand));
        this.motionX = AurorianSnowEmitter.getWindDirX() * hSpeed;
        this.motionY = -vSpeed;
        this.motionZ = AurorianSnowEmitter.getWindDirZ() * hSpeed;
    }

    private static float clamp(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    @Override
    public boolean shouldDisableDepth() {
        return MoonSnowShape.disablesDepth(this.snowClass);
    }

    @Override
    public void setExpired() {
        super.setExpired();
        if (this.counted) {
            this.counted = false;
            MoonSnowCounters.ACTIVE.decrementAndGet();
            if (this.perimeter) MoonSnowCounters.PERIMETER.decrementAndGet();
        }
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX,
                               float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (this.snowClass == SnowClass.STREAK) {
            // Roll the dash to the screen direction of motion. Camera right = (rotationX, 0, rotationYZ),
            // up = (rotationXY, rotationZ, rotationXZ); the particle roll turns clockwise about the view axis, hence the minus.
            double vr = this.motionX * rotationX + this.motionZ * rotationYZ;
            double vu = this.motionX * rotationXY + this.motionY * rotationZ + this.motionZ * rotationXZ;
            float angle = -(float) Math.atan2(vu, vr);
            this.prevParticleAngle = angle;
            this.particleAngle = angle;
        }
        super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
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

        double gust = MoonSnowShape.gust(this.world.getTotalWorldTime());
        double wx = AurorianSnowEmitter.getWindDirX();
        double wz = AurorianSnowEmitter.getWindDirZ();
        this.motionX = wx * this.hSpeed * gust;
        this.motionZ = wz * this.hSpeed * gust;
        this.motionY = -this.vSpeed * gust;
        if (this.snowClass == SnowClass.SPECK || this.snowClass == SnowClass.SMALL) {
            // A gentle swirl across the wind.
            double sway = Math.sin((this.particleAge + this.wobblePhase) * 0.5D) * this.hSpeed * 0.06D;
            this.motionX += -wz * sway;
            this.motionZ += wx * sway;
        }

        float life = (float) this.particleAge / (float) this.particleMaxAge;
        if (life < 0.15F) {
            this.particleAlpha = (life / 0.15F) * this.baseAlpha;
        } else if (life > 0.75F) {
            this.particleAlpha = ((1.0F - life) / 0.25F) * this.baseAlpha;
        } else {
            this.particleAlpha = this.baseAlpha;
        }

        double ox = this.posX;
        double oy = this.posY;
        double oz = this.posZ;
        this.move(this.motionX, this.motionY, this.motionZ);

        // A fast flake must not step through a thin roof: look at the end of the step and, when it is long, at the
        // points a third and two thirds along it.
        if (MoonSnowRoof.solidAt(this.world, this.posX, this.posY, this.posZ)) {
            this.setExpired();
            return;
        }
        double dx = this.posX - ox;
        double dy = this.posY - oy;
        double dz = this.posZ - oz;
        if (dx * dx + dy * dy + dz * dz > 1.0D
                && (MoonSnowRoof.solidAt(this.world, ox + dx / 3.0D, oy + dy / 3.0D, oz + dz / 3.0D)
                || MoonSnowRoof.solidAt(this.world, ox + dx * 2.0D / 3.0D, oy + dy * 2.0D / 3.0D, oz + dz * 2.0D / 3.0D))) {
            this.setExpired();
            return;
        }
        if (ArcanaQuestTweaksConfig.AurorianModuleConfig.cullSnowUnderRoofs) {
            int cover = MoonSnowRoof.cover(this.world, (int) Math.floor(this.posX), (int) Math.floor(this.posZ));
            if (MoonSnowShape.underRoof(this.posY, cover)) {
                this.setExpired();
            }
        }
    }
}
