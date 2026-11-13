package com.apocollis.aqtweaks.client.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.aurorian.AurorianWindHelper;
import com.apocollis.aqtweaks.aurorian.MoonfrostHandler;
import com.apocollis.aqtweaks.aurorian.MoonfrostState;
import com.apocollis.aqtweaks.hazard.HazardState;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Looping wind for the Moonfrost storm. It reuses the {@code aqtweaks:sandstorm.wind} event at a lower pitch
 * (no new sound asset yet). The volume rises through the warning to 30%, then follows the storm strength and
 * fades with it, muffled indoors and behind a windbreak.
 */
@SideOnly(Side.CLIENT)
public class AurorianStormSound extends MovingSound {

    private static final float WARNING_PEAK = 0.30F;
    private static final int SHELTER_REFRESH_TICKS = 10;

    private final EntityPlayerSP player;
    private boolean outdoors = true;
    private boolean sheltered;
    private int sinceShelterCheck = SHELTER_REFRESH_TICKS;

    public AurorianStormSound(EntityPlayerSP player, SoundEvent sound) {
        super(sound, SoundCategory.WEATHER);
        this.player = player;
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = ISound.AttenuationType.NONE;
        this.volume = 0.05F;
        this.pitch = 0.75F;
    }

    @Override
    public void update() {
        if (player == null || player.isDead || player.world == null || !MoonfrostState.isAurorian(player.world)) {
            this.donePlaying = true;
            return;
        }
        World world = player.world;
        HazardState state = MoonfrostState.get(world);
        float target = targetVolume(state, world);

        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.enableStormSound || target <= 0.005F) {
            this.volume -= 0.05F;
            if (this.volume <= 0.0F) {
                this.volume = 0.0F;
                this.donePlaying = true;
            }
            return;
        }

        this.xPosF = (float) player.posX;
        this.yPosF = (float) player.posY;
        this.zPosF = (float) player.posZ;

        if (++sinceShelterCheck >= SHELTER_REFRESH_TICKS) {
            sinceShelterCheck = 0;
            outdoors = com.apocollis.aqtweaks.weather.StormShelter.openSky(world,
                    new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ));
            Vec3d wind = AurorianWindHelper.getWindVector(world, true);
            sheltered = outdoors && MoonfrostHandler.isShelteredHeld(player, world, wind);
        }
        if (!outdoors) {
            target *= 0.25F;
            this.pitch = 0.65F;
        } else if (sheltered) {
            target *= 0.50F;
            this.pitch = 0.70F;
        } else {
            this.pitch = 0.74F + state.strength * 0.08F;
        }
        target *= (float) ArcanaQuestTweaksConfig.AurorianModuleConfig.stormWindVolume;
        this.volume += (target - this.volume) * 0.08F;
    }

    private static float targetVolume(HazardState state, World world) {
        if (state.storming) {
            float ramp = Math.max(1, ArcanaQuestTweaksConfig.AurorianModuleConfig.rampTicks);
            float tail = Math.min(1.0F, state.ticksToEnd / ramp);
            tail = Math.max(0.0F, tail);
            return (WARNING_PEAK + (1.0F - WARNING_PEAK) * state.strength) * tail;
        }
        return WARNING_PEAK * state.warning;
    }
}
