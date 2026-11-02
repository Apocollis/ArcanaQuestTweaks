package com.apocollis.aqtweaks.client.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.atum.AtumStormGameplayHandler;
import com.apocollis.aqtweaks.atum.AtumWindHelper;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Continuous client-side howling wind audio loop for Atum sandstorms.
 * Modulates volume and pitch based on:
 * - Current storm strength (fades in as storm builds, roars during peak gale, fades out as storm subsides).
 * - Shelter status: muffled/lower-volume indoors/underground or behind solid wind barriers.
 */
@SideOnly(Side.CLIENT)
public class AtumStormSound extends MovingSound {

    private final EntityPlayerSP player;

    public AtumStormSound(EntityPlayerSP player, SoundEvent sound) {
        super(sound, SoundCategory.WEATHER);
        this.player = player;
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = ISound.AttenuationType.NONE;
        this.volume = 0.05F; // Start soft and fade in
        this.pitch = 1.0F;
    }

    @Override
    public void update() {
        if (this.player == null || this.player.isDead || this.player.world == null) {
            this.donePlaying = true;
            return;
        }

        World world = this.player.world;
        WorldProvider provider = world.provider;
        if (!(provider instanceof WorldProviderAtum) && (provider == null || provider.getDimension() != 17)) {
            this.donePlaying = true;
            return;
        }

        boolean isStorming = (provider instanceof WorldProviderAtum) &&
                (((WorldProviderAtum) provider).isStorming || ((WorldProviderAtum) provider).stormStrength > 0.05F);
        float stormStrength = (provider instanceof WorldProviderAtum) ? ((WorldProviderAtum) provider).stormStrength : 0.0F;

        if (!isStorming || stormStrength <= 0.02F || !ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog.enableSandstormWindSound) {
            // Smoothly fade out before terminating
            this.volume -= 0.05F;
            if (this.volume <= 0.0F) {
                this.volume = 0.0F;
                this.donePlaying = true;
            }
            return;
        }

        this.xPosF = (float) this.player.posX;
        this.yPosF = (float) this.player.posY;
        this.zPosF = (float) this.player.posZ;

        // Base volume scales with storm strength: reaches full volume at stormStrength ~0.8-1.0
        float targetVolume = (float) Math.min(1.0F, 0.25F + stormStrength * 0.75F);

        // Shelter & indoor acoustic modulation
        BlockPos eyePos = new BlockPos(this.player.posX, this.player.posY + this.player.getEyeHeight(), this.player.posZ);
        boolean canSeeSky = world.canSeeSky(eyePos);
        Vec3d windVec = AtumWindHelper.getWindVector(world, true);
        boolean sheltered = AtumStormGameplayHandler.isSheltered(this.player, world, windVec);

        if (!canSeeSky) {
            // Underground or indoors: deeply muffled distant howl
            targetVolume *= 0.25F;
            this.pitch = 0.85F;
        } else if (sheltered) {
            // Behind a dune or wall: wind roaring overhead but partially shielded
            targetVolume *= 0.50F;
            this.pitch = 0.92F;
        } else {
            // Fully exposed in the open gale: roaring howling gale
            this.pitch = 0.96F + (stormStrength * 0.08F);
        }

        // Apply client config volume multiplier
        targetVolume *= (float) ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog.sandstormWindVolume;

        // Smooth volume interpolation (ease-in/ease-out)
        this.volume += (targetVolume - this.volume) * 0.08F;
    }
}
