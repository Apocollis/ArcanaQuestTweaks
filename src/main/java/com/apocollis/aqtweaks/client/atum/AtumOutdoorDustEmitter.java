package com.apocollis.aqtweaks.client.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.atum.AtumWindHelper;
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
 * Enforces a strictly unified, dynamically veering wind vector across the entire dimension
 * so all particles (both large billowing dust clouds and small blowing sand flecks) stream in unison.
 * In sandstorms, increases particle density, speed, and volumetric coverage to greatly obscure player vision.
 * Also suppresses vanilla and Dynamic Surroundings weather rendering in Atum to prevent
 * conflicting diagonal/falling rain or dust motes.
 */
@SideOnly(Side.CLIENT)
public class AtumOutdoorDustEmitter {

    // Dynamic wind direction state (starts at East-Northeast, angle ~0.45 rad)
    private static double currentWindAngle = 0.45D;
    private static double targetWindAngle = 0.45D;
    private static double windDirX = Math.cos(0.45D);
    private static double windDirZ = Math.sin(0.45D);
    private static int windShiftTimer = 800; // Ticks until next direction change

    public static double getWindDirX() {
        return windDirX;
    }

    public static double getWindDirZ() {
        return windDirZ;
    }

    public static double getCurrentWindAngle() {
        return currentWindAngle;
    }

    private static double normalizeAngle(double angle) {
        while (angle < 0.0D) angle += 2.0D * Math.PI;
        while (angle >= 2.0D * Math.PI) angle -= 2.0D * Math.PI;
        return angle;
    }

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

        // Synchronized deterministic wind direction update
        Vec3d windVec = AtumWindHelper.getWindVector(world, isStorming);
        windDirX = windVec.x;
        windDirZ = windVec.z;
        currentWindAngle = Math.atan2(windDirZ, windDirX);

        // Clear weather atmospheric breeze oscillation (ebb and flow)
        long worldTime = world.getTotalWorldTime();
        double wave1 = Math.sin(worldTime * 0.0028D);
        double wave2 = Math.sin(worldTime * 0.0011D + 1.2D);
        double breezeFactor = Math.max(0.12D, Math.min(1.70D, 0.85D + 0.55D * wave1 + 0.30D * wave2));

        // Particle budget: scaled by breezeFactor in clear weather, maxSandstormDustParticles during sandstorms
        int particleLimit = isStorming
                ? config.maxSandstormDustParticles
                : (int) Math.max(35, config.maxOutdoorDustParticles * breezeFactor);
        if (ParticleAtumDustSheet.ACTIVE_COUNT.get() >= particleLimit) return;

        // Spawn density: a handful of attempts in clear weather; 600 to 1500 during sandstorms
        int attempts = isStorming
                ? (int) (600 + stormStrength * 900)
                : (int) Math.max(6, 32.0D * breezeFactor);

        Vec3d eyeVec = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        double baseAngle = Math.atan2(lookVec.z, lookVec.x);
        boolean playerOutdoors = world.canSeeSky(new BlockPos(eyeVec));

        // Wind speed: high-speed howling gale during storms, gentle fluctuating breeze in clear weather
        double windSpeed = isStorming
                ? (1.40D + stormStrength * 0.70D) // 1.40 - 2.10 blocks/tick in sandstorms
                : (0.16D + 0.16D * breezeFactor); // 0.18 - 0.43 blocks/tick in clear weather

        int lifetime = isStorming ? 38 : 55;

        for (int i = 0; i < attempts; i++) {
            if (ParticleAtumDustSheet.ACTIVE_COUNT.get() >= particleLimit) break;

            double cx, cz;
            double dist;
            if (!playerOutdoors) {
                // When indoors, bias 80% into player's look cone so particles are visible through openings
                double angle;
                if (rand.nextFloat() < 0.80F) {
                    angle = baseAngle + (rand.nextDouble() - 0.5D) * 1.5D;
                } else {
                    angle = rand.nextDouble() * Math.PI * 2.0D;
                }
                dist = 2.5D + rand.nextDouble() * 20.0D;
                cx = player.posX + Math.cos(angle) * dist;
                cz = player.posZ + Math.sin(angle) * dist;
            } else {
                // Outdoors: wind-aligned band. Storms reach 50 blocks upwind so looking into the gale is a wall of sand.
                // alongWind storm [-50, +16], calm [-25, +25]; crossWind [-35, +35] storm, [-25, +25] calm
                double alongWind = isStorming
                        ? rand.nextDouble() * 66.0D - 50.0D
                        : rand.nextDouble() * 50.0D - 25.0D;
                double crossWidth = isStorming ? 70.0D : 50.0D;
                double crossWind = (rand.nextDouble() - 0.5D) * crossWidth;

                cx = player.posX + windDirX * alongWind - windDirZ * crossWind;
                cz = player.posZ + windDirZ * alongWind + windDirX * crossWind;
                dist = Math.sqrt((cx - player.posX) * (cx - player.posX) + (cz - player.posZ) * (cz - player.posZ));
            }

            // Elevation: appear above and below player elevation
            // Clear weather: -6 to +14 blocks; Storm: -10 to +20 blocks (covering dunes, pits, cliffs, sky)
            double elevationSpan = isStorming ? 30.0D : 20.0D;
            double elevationOffset = isStorming ? 10.0D : 6.0D;
            double cy = player.posY + (rand.nextDouble() * elevationSpan - elevationOffset);

            checkPos.setPos(cx, cy, cz);

            // Must be air and MUST be outdoors under the open sky (NEVER inside buildings or caves)
            if (!world.isAirBlock(checkPos) || !world.canSeeSky(checkPos)) {
                continue;
            }

            // Upwind solid obstruction check: do not originate within 6-7 blocks downwind of a solid obstacle
            double upwindDist = isStorming ? 7.0D : 6.0D;
            double upX = cx - windDirX * upwindDist;
            double upZ = cz - windDirZ * upwindDist;
            checkPos.setPos(upX, cy, upZ);
            if (!world.canSeeSky(checkPos)) {
                continue; // Upwind position is under a roof or underground
            }
            Vec3d upVec = new Vec3d(upX, cy, upZ);
            Vec3d targetVec = new Vec3d(cx, cy, cz);
            RayTraceResult windRay = world.rayTraceBlocks(upVec, targetVec, false, true, false);
            if (windRay != null && windRay.typeOfHit == RayTraceResult.Type.BLOCK) {
                continue; // Solid wall/obstacle is directly upwind; candidate is in wind shadow
            }

            // Line-of-sight check: prevent spawning behind solid obstacles relative to player
            if (!playerOutdoors || dist <= 6.0) {
                RayTraceResult ray = world.rayTraceBlocks(eyeVec, targetVec, false, true, false);
                if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
                    if (ray.hitVec.squareDistanceTo(eyeVec) < targetVec.squareDistanceTo(eyeVec) - 1.5) {
                        continue;
                    }
                }
            }

            // Mix: 85% small sand grains / 15% moderate sheets in storm; 65/35 in clear weather
            boolean isSmallFleck = isStorming ? (rand.nextFloat() < 0.85F) : (rand.nextFloat() < 0.65F);
            float particleScale;
            float particleAlpha;
            float particleSpeed = (float) (windSpeed * (isSmallFleck ? 1.15D : 1.0D));

            if (isSmallFleck) {
                particleScale = 0.6F + rand.nextFloat() * 0.5F;
                particleAlpha = isStorming ? Math.min(0.95F, 0.55F + stormStrength * 0.40F) : 0.45F;
            } else {
                particleScale = 3.6F + rand.nextFloat() * 1.6F;
                particleAlpha = isStorming ? Math.min(0.85F, 0.35F + stormStrength * 0.50F) : 0.20F;
            }

            int particleLifetime = lifetime + rand.nextInt(isStorming ? 8 : 12);

            ParticleAtumDustSheet particle = new ParticleAtumDustSheet(
                    world, cx, cy, cz, particleSpeed, particleScale, particleAlpha, particleLifetime
            );
            mc.effectRenderer.addEffect(particle);
        }
    }
}
