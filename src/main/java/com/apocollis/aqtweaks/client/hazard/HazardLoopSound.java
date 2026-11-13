package com.apocollis.aqtweaks.client.hazard;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Supplier;

/**
 * A looping, non-attenuated sound that follows the player and eases its volume toward a target supplied each
 * tick. When the target stays at zero the loop ends itself; the owner restarts it when the target rises again.
 */
@SideOnly(Side.CLIENT)
public class HazardLoopSound extends MovingSound {
    private final EntityPlayerSP player;
    private final Supplier<Float> target;

    public HazardLoopSound(EntityPlayerSP player, SoundEvent sound, SoundCategory category, float pitch,
                           Supplier<Float> target) {
        super(sound, category);
        this.player = player;
        this.target = target;
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = ISound.AttenuationType.NONE;
        this.volume = 0.0F;
        this.pitch = pitch;
    }

    @Override
    public void update() {
        if (player == null || player.isDead) {
            this.donePlaying = true;
            return;
        }
        this.xPosF = (float) player.posX;
        this.yPosF = (float) player.posY;
        this.zPosF = (float) player.posZ;
        float t = Math.max(0.0F, target.get());
        this.volume += (t - this.volume) * 0.1F;
        if (t <= 0.005F && this.volume < 0.01F) {
            this.volume = 0.0F;
            this.donePlaying = true;
        }
    }
}
