package com.apocollis.aqtweaks.client.aether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.aether.AetherChannels;
import com.apocollis.aqtweaks.aether.AetherSquallGear;
import com.apocollis.aqtweaks.aether.AetherSquallGear.GearClass;
import com.apocollis.aqtweaks.aether.StormWarningMath;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.weather.GearTierRule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The warning of a whole lightning storm, before the first strike marker appears: distant thunder that builds, static
 * charge crackling on a player who can see the sky (more on conductive gear, most on channeling gear), and one
 * action-bar line. All client side, driven by the synced hazard state.
 */
@SideOnly(Side.CLIENT)
public class AetherStormWarning {
    private final Random rand = new Random();
    private long messagedEvent = Long.MIN_VALUE;
    private int thunderCooldown;

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        messagedEvent = Long.MIN_VALUE;
        thunderCooldown = 0;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        EntityPlayerSP player = mc.player;
        if (world == null || player == null || mc.isGamePaused() || !AetherChannels.isAether(world)) return;
        ArcanaQuestTweaksConfig.AetherSquall c = ArcanaQuestTweaksConfig.AetherModuleConfig.squall;
        if (!c.enableLightning) return;
        HazardState state = Hazards.state(world, AetherChannels.LIGHTNING);
        if (state.event == null || state.phase != HazardEvent.Phase.WARNING) {
            thunderCooldown = 0;
            return;
        }
        if (!world.canSeeSky(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ))) return;

        float w = state.warning;
        if (c.lightningWarningMessage && state.event.id != messagedEvent) {
            messagedEvent = state.event.id;
            mc.ingameGUI.setOverlayMessage(I18n.format("aqtweaks.aether.lightning_warning"), false);
        }

        if (c.enableSquallSound) {
            if (--thunderCooldown <= 0) {
                thunderCooldown = StormWarningMath.thunderGapTicks(w) + rand.nextInt(20);
                double angle = rand.nextDouble() * Math.PI * 2.0D;
                double dist = 30.0D + rand.nextDouble() * 30.0D;
                world.playSound(player.posX + Math.cos(angle) * dist, player.posY + 20.0D,
                        player.posZ + Math.sin(angle) * dist, SoundEvents.ENTITY_LIGHTNING_THUNDER,
                        SoundCategory.WEATHER, StormWarningMath.thunderVolume(w), 0.5F + rand.nextFloat() * 0.2F, false);
            }
            if (w > 0.3F && player.ticksExisted % 20 == 0) {
                world.playSound(player.posX, player.posY + 1.5D, player.posZ, SoundEvents.ENTITY_FIREWORK_TWINKLE,
                        SoundCategory.WEATHER, 0.25F * w, 1.8F, false);
            }
        }

        if (c.enableSquallParticles) {
            ArcanaQuestTweaksConfig.AetherHazardGear gearCfg = ArcanaQuestTweaksConfig.HazardGearConfig.aether;
            GearClass gear = AetherSquallGear.classify(
                    GearTierRule.allSlotsOnList(player, gearCfg.insulatedGear),
                    GearTierRule.anyOnListExcluding(player, gearCfg.conductiveGear, gearCfg.insulatedGear,
                            gearCfg.channelingGear),
                    GearTierRule.allSlotsOnList(player, gearCfg.channelingGear));
            int motes = StormWarningMath.moteCount(w, gear);
            for (int i = 0; i < motes; i++) {
                double x = player.posX + (rand.nextDouble() - 0.5D) * 0.9D;
                double y = player.posY + 1.2D + rand.nextDouble() * 0.9D;
                double z = player.posZ + (rand.nextDouble() - 0.5D) * 0.9D;
                world.spawnParticle(i % 3 == 0 ? EnumParticleTypes.FIREWORKS_SPARK : EnumParticleTypes.CRIT_MAGIC, x, y,
                        z, (rand.nextDouble() - 0.5D) * 0.06D, 0.04D + rand.nextDouble() * 0.06D,
                        (rand.nextDouble() - 0.5D) * 0.06D);
            }
        }
    }
}
