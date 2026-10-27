package com.apocollis.aqtweaks.client.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client-only 3D billboard dust sheet particle for Atum.
 * Renders a soft, translucent drifting dust cloud in outdoor air.
 * Maintains uniform billboard size and dynamically moves strictly in unison with the wind.
 */
@SideOnly(Side.CLIENT)
public class ParticleAtumDustSheet extends Particle {

    public static final AtomicInteger ACTIVE_COUNT = new AtomicInteger(0);

    private final float speed;
    private final float baseAlpha;
    private final net.minecraft.util.math.BlockPos.MutableBlockPos posCheck = new net.minecraft.util.math.BlockPos.MutableBlockPos();

    public ParticleAtumDustSheet(World worldIn, double x, double y, double z,
                                float speed, float scale, float maxAlpha, int lifetime) {
        super(worldIn, x, y, z,
                AtumOutdoorDustEmitter.getWindDirX() * speed,
                0.0D,
                AtumOutdoorDustEmitter.getWindDirZ() * speed);
        ACTIVE_COUNT.incrementAndGet();

        this.speed = speed;
        this.motionX = AtumOutdoorDustEmitter.getWindDirX() * speed;
        this.motionY = 0.0D;
        this.motionZ = AtumOutdoorDustEmitter.getWindDirZ() * speed;
        this.canCollide = false;
        this.particleGravity = 0.0F;

        // Rich yellowish-sandy Atum desert sand tint: RGB ~ (0.92, 0.78, 0.44) with subtle natural variation
        float tintVar = (this.rand.nextFloat() - 0.5F) * 0.04F;
        this.particleRed = Math.max(0.0F, Math.min(1.0F, 0.92F + tintVar));
        this.particleGreen = Math.max(0.0F, Math.min(1.0F, 0.78F + tintVar * 0.8F));
        this.particleBlue = Math.max(0.0F, Math.min(1.0F, 0.44F + tintVar * 0.5F));

        this.baseAlpha = maxAlpha;
        this.particleAlpha = 0.0F;

        // Full scale throughout lifetime: no tiny motes
        this.particleScale = scale;

        this.particleMaxAge = lifetime;
        this.particleAge = 0;

        // Soft cloud sprite from particles.png
        this.setParticleTextureIndex(7);
    }

    @Override
    public boolean shouldDisableDepth() {
        // Disables depth writes so multiple translucent dust sheets blend smoothly
        return true;
    }

    private boolean counted = true;

    @Override
    public void setExpired() {
        super.setExpired();
        if (this.counted) {
            this.counted = false;
            ACTIVE_COUNT.decrementAndGet();
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

        // Dynamic wind following: particle dynamically steers as wind direction veers
        if (ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog.enableDynamicWindDirection) {
            this.motionX = AtumOutdoorDustEmitter.getWindDirX() * this.speed;
            this.motionZ = AtumOutdoorDustEmitter.getWindDirZ() * this.speed;
        }

        // Smooth alpha envelope: fade in over first 15% of life, fade out over last 25%
        float lifeFraction = (float) this.particleAge / (float) this.particleMaxAge;
        if (lifeFraction < 0.15F) {
            this.particleAlpha = (lifeFraction / 0.15F) * this.baseAlpha;
        } else if (lifeFraction > 0.75F) {
            this.particleAlpha = ((1.0F - lifeFraction) / 0.25F) * this.baseAlpha;
        } else {
            this.particleAlpha = this.baseAlpha;
        }

        this.move(this.motionX, this.motionY, this.motionZ);

        // Spawn still requires open sky. Once airborne, sand may blow through doorways and windows.
        // It dies when it hits a solid block.
        if ((this.particleAge & 1) == 0 && this.world.isBlockLoaded(this.posCheck.setPos(this.posX, this.posY, this.posZ))) {
            IBlockState state = this.world.getBlockState(this.posCheck);
            if (state.getMaterial().isSolid()) {
                this.setExpired();
            }
        }
    }
}
