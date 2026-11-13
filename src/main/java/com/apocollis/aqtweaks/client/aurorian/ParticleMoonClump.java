package com.apocollis.aqtweaks.client.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.aurorian.MoonSnowShape;

import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A heavy snow clump: a chunky crumb of the snow block texture, lit by the world light and depth tested. Vanilla
 * crumbs fall under gravity; these move with the storm wind and fall angle instead, and obey the same solid-block and
 * roof checks as the flakes so a clump cannot fly indoors.
 */
@SideOnly(Side.CLIENT)
public class ParticleMoonClump extends ParticleDigging {

    private final float hSpeed;
    private final float vSpeed;
    private boolean counted = true;

    public ParticleMoonClump(World world, double x, double y, double z, float hSpeed, float vSpeed, float scale, int lifetime) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D, Blocks.SNOW.getDefaultState());
        this.setBlockPos(new BlockPos(x, y, z));
        MoonSnowCounters.ACTIVE.incrementAndGet();
        this.hSpeed = hSpeed;
        this.vSpeed = vSpeed;
        this.canCollide = false;
        this.particleGravity = 0.0F;
        this.particleScale = scale;
        this.particleMaxAge = lifetime;
        this.particleRed = 0.88F;
        this.particleGreen = 0.94F;
        this.particleBlue = 1.0F;
        this.motionX = AurorianSnowEmitter.getWindDirX() * hSpeed;
        this.motionY = -vSpeed;
        this.motionZ = AurorianSnowEmitter.getWindDirZ() * hSpeed;
    }

    @Override
    public void setExpired() {
        super.setExpired();
        if (this.counted) {
            this.counted = false;
            MoonSnowCounters.ACTIVE.decrementAndGet();
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

        double gust = MoonSnowShape.gust(this.world.getTotalWorldTime());
        this.motionX = AurorianSnowEmitter.getWindDirX() * this.hSpeed * gust;
        this.motionZ = AurorianSnowEmitter.getWindDirZ() * this.hSpeed * gust;
        this.motionY = -this.vSpeed * gust;

        double ox = this.posX;
        double oy = this.posY;
        double oz = this.posZ;
        this.move(this.motionX, this.motionY, this.motionZ);

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
