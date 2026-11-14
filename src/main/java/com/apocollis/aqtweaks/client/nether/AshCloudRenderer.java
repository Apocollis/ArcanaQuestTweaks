package com.apocollis.aqtweaks.client.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.atum.AtumSounds;
import com.apocollis.aqtweaks.client.hazard.HazardLoopSound;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.nether.AshSwirlShape;
import com.apocollis.aqtweaks.nether.ClientAshClouds;
import com.apocollis.aqtweaks.nether.NetherHazards;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Draws the ash storm. In the warning, ash wisps and a few cinders start to stir around the player and the wind
 * rises. Once the server announces the clouds, each is drawn as a rounded body of {@link ParticleAshSwirl} motes: dense
 * core, feathered rim, turning as it drifts. A mote is only spawned where the cloud's coarse cell is occupied, the
 * block is loaded air, and (near the camera) nothing solid is in the way, so ash pours through doorways and never
 * appears inside a sealed room. The budgets are live caps, not spawn rates.
 */
@SideOnly(Side.CLIENT)
public class AshCloudRenderer {
    /** Mean mote lifetime, used to turn a live cap into a spawn rate per tick. */
    private static final int MEAN_LIFETIME_TICKS = 70;

    private final Random rand = new Random();
    private HazardLoopSound sound;
    private static float depth;
    private static float windScale;

    /** 0..1: how deep (eased) the camera is inside a cloud. */
    public static float depth() {
        return depth;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientAshClouds.clear();
        depth = 0.0F;
        windScale = 0.0F;
        ParticleAshSwirl.resetCounts();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            ParticleAshSwirl.resetCounts();
        }
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
            windScale = 0.0F;
            ParticleAshSwirl.resetCounts();
            return;
        }
        ClientAshClouds.tick();

        HazardState state = Hazards.state(world, NetherHazards.ASH);
        float warning = state.event != null && state.phase == HazardEvent.Phase.WARNING ? state.warning : 0.0F;

        boolean inside = ClientAshClouds.contains(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        // Fog and wind respond within about a second of entering a cloud, and clear a little more slowly.
        depth += ((inside ? 1.0F : 0.0F) - depth) * (inside ? 0.08F : 0.05F);
        windScale = Math.max(depth * 0.5F, warning * 0.25F);

        if (ArcanaQuestTweaksConfig.NetherModuleConfig.enableAshSound) {
            if (windScale > 0.02F && (sound == null || sound.isDonePlaying() || !mc.getSoundHandler().isSoundPlaying(sound))) {
                sound = new HazardLoopSound(player, AtumSounds.SANDSTORM_WIND, SoundCategory.WEATHER, 0.55F,
                        () -> windScale);
                mc.getSoundHandler().playSound(sound);
            }
        }

        if (!ArcanaQuestTweaksConfig.NetherModuleConfig.enableAshParticles) return;
        Vec3d eye = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (warning > 0.0F) {
            warningWisps(mc, world, player, eye, pos, state, warning);
        }
        spawnClouds(mc, world, player, eye, pos);

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

    /** Ash wisps and a few cinders stirring in the open air near the player, more as the warning runs down. */
    private void warningWisps(Minecraft mc, World world, EntityPlayerSP player, Vec3d eye,
                              BlockPos.MutableBlockPos pos, HazardState state, float warning) {
        int totalCap = ArcanaQuestTweaksConfig.NetherModuleConfig.ashMaxParticlesTotal;
        double wind = AshSwirlShape.warningAngle(state.event.seed);
        double windX = Math.cos(wind) * 0.12D;
        double windZ = Math.sin(wind) * 0.12D;
        int wisps = AshSwirlShape.warningWisps(warning);
        for (int i = 0; i < wisps && ParticleAshSwirl.active() < totalCap; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0D;
            double r = 3.0D + rand.nextDouble() * 19.0D;
            double x = player.posX + Math.cos(a) * r;
            double z = player.posZ + Math.sin(a) * r;
            double y = player.posY - 1.0D + rand.nextDouble() * 9.0D;
            pos.setPos(x, y, z);
            if (!world.isBlockLoaded(pos) || !world.isAirBlock(pos)) continue;
            if (blocked(world, eye, x, y, z)) continue;
            AshSwirlShape.Kind kind = rand.nextFloat() < 0.15F ? AshSwirlShape.Kind.EMBER : AshSwirlShape.Kind.FLECK;
            mc.effectRenderer.addEffect(new ParticleAshSwirl(world, x, y, z, kind, windX, windZ));
        }
    }

    private void spawnClouds(Minecraft mc, World world, EntityPlayerSP player, Vec3d eye, BlockPos.MutableBlockPos pos) {
        int range = ArcanaQuestTweaksConfig.NetherModuleConfig.ashRenderDistance;
        int totalCap = ArcanaQuestTweaksConfig.NetherModuleConfig.ashMaxParticlesTotal;
        int cloudCap = ArcanaQuestTweaksConfig.NetherModuleConfig.ashMaxParticlesPerCloud;
        double swirl = ArcanaQuestTweaksConfig.NetherModuleConfig.ashSwirlSpeed;
        for (ClientAshClouds.Cloud cloud : ClientAshClouds.all()) {
            double dx = cloud.x - player.posX;
            double dz = cloud.z - player.posZ;
            double far = (double) range + cloud.reachH;
            if (dx * dx + dz * dz > far * far) continue;
            int live = ParticleAshSwirl.active();
            int liveCloud = ParticleAshSwirl.activeFor(cloud.id);
            if (live >= totalCap || liveCloud >= cloudCap) continue;
            int rate = Math.max(1, Math.min(totalCap, cloudCap) / MEAN_LIFETIME_TICKS);
            int attempts = Math.min(900, rate * 5);
            boolean near = dx * dx + dz * dz < (cloud.reachH + 28.0D) * (cloud.reachH + 28.0D);
            int spawned = 0;
            for (int i = 0; i < attempts && spawned < rate && live < totalCap && liveCloud < cloudCap; i++) {
                double x;
                double y;
                double z;
                if (near && rand.nextFloat() < 0.65F) {
                    // Dense around the viewer, so visibility right around the player is limited.
                    x = player.posX + (rand.nextDouble() * 2.0D - 1.0D) * 28.0D;
                    y = player.posY + rand.nextDouble() * 24.0D - 8.0D;
                    z = player.posZ + (rand.nextDouble() * 2.0D - 1.0D) * 28.0D;
                } else {
                    x = cloud.x + (rand.nextDouble() * 2.0D - 1.0D) * cloud.reachH;
                    y = cloud.y + (rand.nextDouble() * 2.0D - 1.0D) * cloud.reachV;
                    z = cloud.z + (rand.nextDouble() * 2.0D - 1.0D) * cloud.reachH;
                }
                double ox = x - cloud.x;
                double oy = y - cloud.y;
                double oz = z - cloud.z;
                double d = AshSwirlShape.radius(ox, oy, oz, cloud.reachH, cloud.reachV);
                if (d >= 1.0D || rand.nextDouble() > AshSwirlShape.density(d)) continue;
                double px = x - player.posX;
                double pz = z - player.posZ;
                if (px * px + pz * pz > (double) range * range) continue;
                if (!cloud.contains(x, y, z)) continue;
                pos.setPos(x, y, z);
                if (!world.isBlockLoaded(pos) || !world.isAirBlock(pos)) continue;
                if (blocked(world, eye, x, y, z)) continue;
                mc.effectRenderer.addEffect(new ParticleAshSwirl(world, cloud, ox, oy, oz,
                        AshSwirlShape.kind(rand), swirl));
                spawned++;
                live++;
                liveCloud++;
            }
        }
    }

    /** True if a solid block lies between the camera and a point within 24 blocks (so a sealed room stays clear). */
    private static boolean blocked(World world, Vec3d eye, double x, double y, double z) {
        double dx = x - eye.x;
        double dy = y - eye.y;
        double dz = z - eye.z;
        double dist2 = dx * dx + dy * dy + dz * dz;
        if (dist2 > 576.0D) return false;
        Vec3d target = new Vec3d(x, y, z);
        RayTraceResult ray = world.rayTraceBlocks(eye, target, false, true, false);
        return ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK
                && ray.hitVec.squareDistanceTo(eye) < dist2 - 1.5D;
    }
}
