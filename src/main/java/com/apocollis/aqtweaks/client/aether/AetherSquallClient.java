package com.apocollis.aqtweaks.client.aether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.aether.AetherChannels;
import com.apocollis.aqtweaks.aether.AetherSquallGear;
import com.apocollis.aqtweaks.aether.AetherWindHelper;
import com.apocollis.aqtweaks.atum.AtumSounds;
import com.apocollis.aqtweaks.aurorian.StormVisuals;
import com.apocollis.aqtweaks.client.hazard.HazardLoopSound;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.weather.GearTierRule;
import com.apocollis.aqtweaks.weather.StormShelter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Client side of the gust: the crosswind push for the local player (added like the Atum sandstorm push, so it also
 * moves gliding and flying players), cloud wisps streaming along the coming heading from the warning, and the rising
 * wind loop. The heading comes from the event seed, so it matches the server and every other client.
 */
@SideOnly(Side.CLIENT)
public class AetherSquallClient {
    private final Random rand = new Random();
    private HazardLoopSound sound;
    private float soundScale;

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        sound = null;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        EntityPlayerSP player = mc.player;
        soundScale = 0.0F;
        if (world == null || player == null || mc.isGamePaused() || !AetherChannels.isAether(world)) return;
        if (!ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableSqualls) return;
        HazardState state = Hazards.state(world, AetherChannels.GUST);
        if (state.event == null || state.phase == com.apocollis.aqtweaks.hazard.HazardEvent.Phase.CLEAR) return;

        long now = world.getTotalWorldTime();
        double angle = AetherWindHelper.headingAt(state.event.seed, state.event.start, now,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.crosswindShiftTicks);
        Vec3d wind = new Vec3d(Math.cos(angle), 0.0D, Math.sin(angle));

        float visual = StormVisuals.scale(state, 0.5F);
        soundScale = visual;
        if (ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableSquallSound && visual > 0.02F
                && (sound == null || sound.isDonePlaying() || !mc.getSoundHandler().isSoundPlaying(sound))) {
            sound = new HazardLoopSound(player, AtumSounds.SANDSTORM_WIND, SoundCategory.WEATHER, 1.2F,
                    () -> soundScale * 0.6F);
            mc.getSoundHandler().playSound(sound);
        }
        if (ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableSquallParticles) {
            wisps(world, player, wind, visual);
        }
        if (state.strength >= 0.05F) {
            push(player, world, wind, state.strength);
        }
    }

    private void wisps(World world, EntityPlayerSP player, Vec3d wind, float scale) {
        int attempts = (int) (scale * 40.0F);
        double speed = 0.5D + scale * 0.9D;
        for (int i = 0; i < attempts; i++) {
            double along = rand.nextDouble() * 48.0D - 36.0D;
            double cross = (rand.nextDouble() - 0.5D) * 40.0D;
            double x = player.posX + wind.x * along - wind.z * cross;
            double z = player.posZ + wind.z * along + wind.x * cross;
            double y = player.posY + (rand.nextDouble() * 16.0D - 6.0D);
            world.spawnParticle(EnumParticleTypes.CLOUD, x, y, z, wind.x * speed, 0.0D, wind.z * speed);
        }
    }

    private void push(EntityPlayerSP player, World world, Vec3d wind, float strength) {
        if (StormShelter.isSheltered(player, world, wind,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.shelterWindShadowDistance)) {
            return;
        }
        boolean insulated = GearTierRule.allSlotsOnList(player, ArcanaQuestTweaksConfig.AetherModuleConfig.squall.insulatedGear);
        boolean conductive = GearTierRule.anyOnList(player, ArcanaQuestTweaksConfig.AetherModuleConfig.squall.conductiveGear);
        double force = ArcanaQuestTweaksConfig.AetherModuleConfig.squall.crosswindPushForce * strength
                * AetherSquallGear.pushFactor(insulated, conductive);
        if (player.isSneaking()) {
            force *= ArcanaQuestTweaksConfig.AetherModuleConfig.squall.sneakAnchorFactor;
        }
        player.motionX += wind.x * force;
        player.motionZ += wind.z * force;
        double max = ArcanaQuestTweaksConfig.AetherModuleConfig.squall.maxGustSpeed;
        double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        if (speed > max) {
            double s = max / speed;
            player.motionX *= s;
            player.motionZ *= s;
        }
    }
}
