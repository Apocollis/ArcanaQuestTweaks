package com.apocollis.aqtweaks.client.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.atum.AtumSounds;
import com.apocollis.aqtweaks.client.hazard.HazardLoopSound;
import com.apocollis.aqtweaks.nether.ClientAshClouds;
import com.apocollis.aqtweaks.nether.NetherHazards;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
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
 * Draws the ash clouds the server announced. Each tick it moves the cloud centres by their velocity, then spawns a
 * budgeted number of dark smoke particles and falling flakes, keeping a particle only where the cell is occupied
 * in the coarse bitmap and the block is air in the client's own chunk data, so ash pours through doorways and never
 * appears inside a sealed room. The budgets are spawn rates (particle total divided by lifetime), not live counts.
 */
@SideOnly(Side.CLIENT)
public class AshCloudRenderer {
    private static final int PARTICLE_LIFETIME_TICKS = 60;

    private final Random rand = new Random();
    private HazardLoopSound sound;
    private static float depth;

    /** 0..1: how deep (eased) the camera is inside a cloud. */
    public static float depth() {
        return depth;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientAshClouds.clear();
        depth = 0.0F;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        EntityPlayerSP player = mc.player;
        if (world == null || player == null || mc.isGamePaused()) return;
        if (!NetherHazards.isNether(world)) {
            ClientAshClouds.clear();
            depth = 0.0F;
            return;
        }
        ClientAshClouds.tick();

        boolean inside = ClientAshClouds.contains(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        depth += ((inside ? 1.0F : 0.0F) - depth) * 0.04F;

        if (ArcanaQuestTweaksConfig.NetherModuleConfig.enableAshSound) {
            if (depth > 0.02F && (sound == null || sound.isDonePlaying() || !mc.getSoundHandler().isSoundPlaying(sound))) {
                sound = new HazardLoopSound(player, AtumSounds.SANDSTORM_WIND, SoundCategory.WEATHER, 0.55F,
                        () -> depth * 0.5F);
                mc.getSoundHandler().playSound(sound);
            }
        }

        if (!ArcanaQuestTweaksConfig.NetherModuleConfig.enableAshParticles) return;
        int range = ArcanaQuestTweaksConfig.NetherModuleConfig.ashRenderDistance;
        int totalBudget = ArcanaQuestTweaksConfig.NetherModuleConfig.ashMaxParticlesTotal / PARTICLE_LIFETIME_TICKS;
        int perCloud = ArcanaQuestTweaksConfig.NetherModuleConfig.ashMaxParticlesPerCloud / PARTICLE_LIFETIME_TICKS;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (ClientAshClouds.Cloud cloud : ClientAshClouds.all()) {
            double dx = cloud.x - player.posX;
            double dz = cloud.z - player.posZ;
            if (dx * dx + dz * dz > (double) (range + cloud.reachH) * (range + cloud.reachH)) continue;
            int spawned = 0;
            int attempts = Math.max(1, perCloud) * 4;
            for (int i = 0; i < attempts && spawned < perCloud && totalBudget > 0; i++) {
                double x = cloud.x + (rand.nextDouble() * 2.0D - 1.0D) * cloud.reachH;
                double y = cloud.y + (rand.nextDouble() * 2.0D - 1.0D) * cloud.reachV;
                double z = cloud.z + (rand.nextDouble() * 2.0D - 1.0D) * cloud.reachH;
                double px = x - player.posX;
                double pz = z - player.posZ;
                if (px * px + pz * pz > (double) range * range) continue;
                if (!cloud.contains(x, y, z)) continue;
                pos.setPos(x, y, z);
                if (!world.isBlockLoaded(pos) || !world.isAirBlock(pos)) continue;
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, cloud.vx * 4.0D, 0.0D, cloud.vz * 4.0D);
                spawned++;
                totalBudget--;
            }
        }
        if (depth > 0.1F) {
            int flakes = ArcanaQuestTweaksConfig.NetherModuleConfig.ashFlakeRate;
            for (int i = 0; i < flakes; i++) {
                double x = player.posX + (rand.nextDouble() - 0.5D) * 16.0D;
                double y = player.posY + 1.0D + rand.nextDouble() * 7.0D;
                double z = player.posZ + (rand.nextDouble() - 0.5D) * 16.0D;
                if (!ClientAshClouds.contains(x, y, z)) continue;
                pos.setPos(x, y, z);
                if (!world.isBlockLoaded(pos) || !world.isAirBlock(pos)) continue;
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0D, -0.05D, 0.0D);
            }
        }
    }
}
