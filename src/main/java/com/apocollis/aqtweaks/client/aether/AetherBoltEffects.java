package com.apocollis.aqtweaks.client.aether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.aether.ClientBolts;

import net.minecraft.client.Minecraft;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** The dodgeable lightning telegraph: a crackling ground ring, a rising spark column and a charge hum. */
@SideOnly(Side.CLIENT)
public class AetherBoltEffects {
    private static final double RING_RADIUS = 2.5D;
    private final Random rand = new Random();

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientBolts.clear();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (world == null || mc.isGamePaused()) return;
        if (!ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableSquallParticles) {
            ClientBolts.clear();
            return;
        }
        for (ClientBolts.Bolt b : ClientBolts.snapshot()) {
            double x = b.x + 0.5D;
            double z = b.z + 0.5D;
            double y = b.y + 0.1D;
            if (b.remaining == b.total - 1 && ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableSquallSound) {
                world.playSound(x, y, z, SoundEvents.ENTITY_FIREWORK_TWINKLE, SoundCategory.WEATHER, 2.0F, 2.0F, false);
            }
            if (b.remaining > 0) {
                float t = 1.0F - (float) b.remaining / b.total;
                int ring = 6 + (int) (t * 10.0F);
                for (int i = 0; i < ring; i++) {
                    double a = rand.nextDouble() * Math.PI * 2.0D;
                    world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x + Math.cos(a) * RING_RADIUS, y,
                            z + Math.sin(a) * RING_RADIUS, 0.0D, 0.02D, 0.0D);
                }
                int column = 2 + (int) (t * 8.0F);
                for (int i = 0; i < column; i++) {
                    world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, x + (rand.nextDouble() - 0.5D) * 0.6D,
                            y + rand.nextDouble() * (2.0D + 6.0D * t), z + (rand.nextDouble() - 0.5D) * 0.6D, 0.0D,
                            0.3D, 0.0D);
                }
            } else if (b.remaining == 0) {
                for (int i = 0; i < 50; i++) {
                    world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x + (rand.nextDouble() - 0.5D) * 2.0D,
                            y + rand.nextDouble() * 8.0D, z + (rand.nextDouble() - 0.5D) * 2.0D,
                            (rand.nextDouble() - 0.5D) * 0.3D, 0.1D, (rand.nextDouble() - 0.5D) * 0.3D);
                }
            }
            b.remaining--;
        }
        ClientBolts.removeFinished();
    }
}
