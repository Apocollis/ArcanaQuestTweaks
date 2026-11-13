package com.apocollis.aqtweaks.client.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.nether.ClientSpouts;

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

/** Client side of a spout: bubbling and smoke through the telegraph, then a column of flame, lava and smoke. */
@SideOnly(Side.CLIENT)
public class SpoutEffects {
    private final Random rand = new Random();

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientSpouts.clear();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (world == null || mc.isGamePaused()) return;
        if (!ArcanaQuestTweaksConfig.NetherModuleConfig.enableSpoutEffects) {
            ClientSpouts.clear();
            return;
        }
        for (ClientSpouts.Spout s : ClientSpouts.snapshot()) {
            double x = s.x + 0.5D;
            double z = s.z + 0.5D;
            double y = s.y + 1.0D;
            if (s.remaining == 60 || s.remaining == 59) {
                world.playSound(x, y, z, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 1.4F, 0.5F, false);
            }
            if (s.remaining > 0) {
                float t = 1.0F - s.remaining / 60.0F;
                int bubbles = 1 + (int) (t * 4.0F);
                for (int i = 0; i < bubbles; i++) {
                    world.spawnParticle(EnumParticleTypes.LAVA, x + (rand.nextDouble() - 0.5D) * 1.4D, y,
                            z + (rand.nextDouble() - 0.5D) * 1.4D, 0.0D, 0.0D, 0.0D);
                    if (rand.nextInt(2) == 0) {
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + (rand.nextDouble() - 0.5D),
                                y + 0.2D, z + (rand.nextDouble() - 0.5D), 0.0D, 0.05D + 0.1D * t, 0.0D);
                    }
                }
            } else if (s.remaining == 0) {
                for (int i = 0; i < 70; i++) {
                    double up = 0.4D + rand.nextDouble() * 0.9D;
                    double sx = (rand.nextDouble() - 0.5D) * 0.35D;
                    double sz = (rand.nextDouble() - 0.5D) * 0.35D;
                    world.spawnParticle(i % 3 == 0 ? EnumParticleTypes.LAVA : EnumParticleTypes.FLAME, x, y, z, sx, up, sz);
                }
                for (int i = 0; i < 24; i++) {
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x + (rand.nextDouble() - 0.5D) * 1.5D,
                            y + rand.nextDouble() * 6.0D, z + (rand.nextDouble() - 0.5D) * 1.5D, 0.0D, 0.2D, 0.0D);
                }
            } else if (s.remaining > -12) {
                // The column keeps burning for a moment after the burst.
                for (int i = 0; i < 6; i++) {
                    world.spawnParticle(EnumParticleTypes.FLAME, x + (rand.nextDouble() - 0.5D) * 2.0D,
                            y + rand.nextDouble() * 10.0D, z + (rand.nextDouble() - 0.5D) * 2.0D, 0.0D, 0.15D, 0.0D);
                }
            }
            s.remaining--;
        }
        ClientSpouts.removeFinished();
    }
}
