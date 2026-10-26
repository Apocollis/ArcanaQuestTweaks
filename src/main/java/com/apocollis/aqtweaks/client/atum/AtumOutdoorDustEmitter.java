package com.apocollis.aqtweaks.client.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Client-only emitter for localized outdoor dust sheet particles.
 * Enforces a strictly unified wind vector across the entire dimension so all particles
 * (both large billowing dust clouds and small blowing sand flecks) stream in unison.
 * Also suppresses vanilla and Dynamic Surroundings weather rendering in Atum to prevent
 * conflicting diagonal/falling rain or dust motes.
 */
@SideOnly(Side.CLIENT)
public class AtumOutdoorDustEmitter {

    // Strictly unified desert wind direction (East-Northeast blowing, angle ~0.45 rad)
    // Every outdoor particle moves along this exact vector.
    private static final double WIND_ANGLE = 0.45D;
    private static final double WIND_DIR_X = Math.cos(WIND_ANGLE);
    private static final double WIND_DIR_Z = Math.sin(WIND_ANGLE);

    private static final IRenderHandler ATUM_WEATHER_SUPPRESSOR = new IRenderHandler() {
        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {
            // No-op: suppress vanilla and Dynamic Surroundings weather rendering in Atum
        }
    };

    private final Random rand = new Random();
    private final BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (world != null && world.isRemote) {
            WorldProvider provider = world.provider;
            if (provider instanceof WorldProviderAtum || (provider != null && provider.getDimension() == 17)) {
                provider.setWeatherRenderer(ATUM_WEATHER_SUPPRESSOR);
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        ParticleAtumDustSheet.ACTIVE_COUNT.set(0);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused() || mc.world == null || mc.player == null) return;

        ArcanaQuestTweaksConfig.AtumFog config = ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog;
        if (!config.enableOutdoorDustParticles) return;

        World world = mc.world;
        WorldProvider provider = world.provider;
        if (provider == null) return;
        if (!(provider instanceof WorldProviderAtum) && provider.getDimension() != 17) return;

        // Ensure Dynamic Surroundings or vanilla weather renderer cannot override the weather handler in Atum
        if (provider.getWeatherRenderer() != ATUM_WEATHER_SUPPRESSOR) {
            provider.setWeatherRenderer(ATUM_WEATHER_SUPPRESSOR);
        }

        EntityPlayerSP player = mc.player;

        boolean isStorming = (provider instanceof WorldProviderAtum) &&
                (((WorldProviderAtum) provider).isStorming || ((WorldProviderAtum) provider).stormStrength > 0.05F);
        float stormStrength = (provider instanceof WorldProviderAtum) ? ((WorldProviderAtum) provider).stormStrength : 0.0F;

        // Particle budget: up to 300 in clear weather, and up to 1400 during sandstorms (2x+ density)
        int particleLimit = isStorming ? Math.min(1400, config.maxOutdoorDustParticles * 4) : Math.min(300, config.maxOutdoorDustParticles);
        if (ParticleAtumDustSheet.ACTIVE_COUNT.get() >= particleLimit) return;

        // Spawn density: 32 attempts/tick in clear weather, 160 attempts/tick during sandstorms (>2x boost)
        int attempts = isStorming ? 160 : 32;
        Vec3d eyeVec = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        double baseAngle = Math.atan2(lookVec.z, lookVec.x);
        boolean playerOutdoors = world.canSeeSky(new BlockPos(eyeVec));

        // Uniform wind speed: all particles stream at the exact same velocity
        double windSpeed = isStorming
                ? (0.90D + stormStrength * 0.40D) // High-speed sandstorm gale (0.90 - 1.30 blocks/tick)
                : 0.22D;                          // Steady clear-weather desert breeze

        double motX = WIND_DIR_X * windSpeed;
        double motY = 0.0D; // Strictly horizontal wind
        double motZ = WIND_DIR_Z * windSpeed;

        int lifetime = isStorming ? 40 : 50;

        for (int i = 0; i < attempts; i++) {
            if (ParticleAtumDustSheet.ACTIVE_COUNT.get() >= particleLimit) break;

            // When indoors, bias 80% into player's look cone; when outdoors, 50% in look cone and 50% around
            double angle;
            float biasChance = playerOutdoors ? 0.50F : 0.80F;
            if (rand.nextFloat() < biasChance) {
                angle = baseAngle + (rand.nextDouble() - 0.5) * 1.5;
            } else {
                angle = rand.nextDouble() * Math.PI * 2.0;
            }
            double dist = 2.5 + Math.pow(rand.nextDouble(), 1.3) * 20.0; // 2.5 to 22.5 blocks away
            double cx = player.posX + Math.cos(angle) * dist;
            double cy = player.posY + (rand.nextDouble() * 5.0 - 0.5);
            double cz = player.posZ + Math.sin(angle) * dist;

            checkPos.setPos(cx, cy, cz);

            // Must be air and MUST be outdoors under the open sky (NEVER inside buildings or caves)
            if (!world.isAirBlock(checkPos) || !world.canSeeSky(checkPos)) {
                continue;
            }

            // When indoors, verify open line of sight through opening
            if (!playerOutdoors) {
                Vec3d targetVec = new Vec3d(cx, cy, cz);
                RayTraceResult ray = world.rayTraceBlocks(eyeVec, targetVec, false, true, false);
                if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
                    if (ray.hitVec.squareDistanceTo(eyeVec) < targetVec.squareDistanceTo(eyeVec) - 2.0) {
                        continue;
                    }
                }
            }

            // Mix: ~75% large billowing dust clouds, ~25% small blowing sand grains
            // Both stream along the exact same unified wind vector
            boolean isSmallFleck = rand.nextFloat() < 0.25F;
            float particleScale;
            float particleAlpha;

            if (isSmallFleck) {
                // Small fast-moving sand grain / fleck
                particleScale = isStorming ? (0.8F + rand.nextFloat() * 0.7F) : (0.6F + rand.nextFloat() * 0.5F);
                particleAlpha = isStorming ? 0.65F : 0.45F;
            } else {
                // Large billowing dust sheet cloud
                particleScale = isStorming ? (5.0F + rand.nextFloat() * 2.5F) : (3.6F + rand.nextFloat() * 1.6F);
                particleAlpha = isStorming ? Math.min(0.55F, 0.28F + stormStrength * 0.22F) : 0.20F;
            }

            int particleLifetime = lifetime + rand.nextInt(10);

            ParticleAtumDustSheet particle = new ParticleAtumDustSheet(
                    world, cx, cy, cz, motX, motY, motZ, particleScale, particleAlpha, particleLifetime
            );
            mc.effectRenderer.addEffect(particle);
        }
    }
}
