package com.apocollis.aqtweaks.client.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.atum.AtumSounds;
import com.apocollis.aqtweaks.aurorian.AurorianWindHelper;
import com.apocollis.aqtweaks.aurorian.ClientMoonfrost;
import com.apocollis.aqtweaks.aurorian.MoonSnowShape;
import com.apocollis.aqtweaks.aurorian.MoonSnowShape.SnowClass;
import com.apocollis.aqtweaks.aurorian.MoonfrostState;
import com.apocollis.aqtweaks.aurorian.StormVisuals;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.weather.StormShelter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumParticleTypes;
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
 * Client tick for the Moonfrost storm: blizzard snow particles and the wind loop. Holds the shared client wind
 * direction the particles follow, and clears the synced storm data on disconnect.
 *
 * <p>Particles and sound scale with {@code max(strength, lead-in)} (see {@link StormVisuals}) so the warning (snow
 * and wind) is felt before the damage starts. There is no fog: the view is limited by a dense veil of snow on a
 * perimeter around the player and by the band of snow upwind and across.
 */
@SideOnly(Side.CLIENT)
public class AurorianSnowEmitter {

    private static double windDirX = Math.cos(AurorianWindHelper.DEFAULT_WIND_ANGLE);
    private static double windDirZ = Math.sin(AurorianWindHelper.DEFAULT_WIND_ANGLE);
    private static AurorianStormSound activeSound;

    private final Random rand = new Random();
    private final BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
    /** Particle-only indoor/outdoor ease: the outdoor band fades into the opening-only view instead of popping. */
    private float exposure;

    public static double getWindDirX() {
        return windDirX;
    }

    public static double getWindDirZ() {
        return windDirZ;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientMoonfrost.reset();
        com.apocollis.aqtweaks.hazard.ClientHazards.reset();
        MoonSnowCounters.reset();
        activeSound = null;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() != null && event.getWorld().isRemote) MoonSnowCounters.reset();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused() || mc.world == null || mc.player == null) return;
        World world = mc.world;
        if (!MoonfrostState.isAurorian(world)) {
            MoonSnowCounters.reset();
            if (activeSound != null) {
                mc.getSoundHandler().stopSound(activeSound);
                activeSound = null;
            }
            return;
        }

        HazardState state = MoonfrostState.get(world);
        float scale = StormVisuals.scale(state, (float) ArcanaQuestTweaksConfig.AurorianModuleConfig.warningParticleFraction);
        EntityPlayerSP player = mc.player;

        updateSound(mc, player, scale);

        if (scale <= 0.02F || !ArcanaQuestTweaksConfig.AurorianModuleConfig.enableStormParticles) return;

        Vec3d wind = AurorianWindHelper.getWindVector(world, state.storming);
        windDirX = wind.x;
        windDirZ = wind.z;

        int limit = (int) (ArcanaQuestTweaksConfig.AurorianModuleConfig.maxStormSnowParticles * scale);
        if (limit <= 0 || MoonSnowCounters.ACTIVE.get() >= limit) return;
        int attempts = (int) (scale * (900.0F + 1200.0F * state.strength));
        if (attempts <= 0) return;

        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        double baseAngle = Math.atan2(look.z, look.x);
        boolean outdoors = StormShelter.openSky(world, new BlockPos(eye));
        exposure = MoonSnowShape.easeExposure(exposure, outdoors ? 1.0F : 0.0F);

        double angle = ArcanaQuestTweaksConfig.AurorianModuleConfig.snowFallAngle;
        double h0 = MoonSnowShape.horizontalSpeed(scale, ArcanaQuestTweaksConfig.AurorianModuleConfig.snowSpeedMultiplier);
        double strength = Math.max(state.strength, scale);
        boolean streaks = ArcanaQuestTweaksConfig.AurorianModuleConfig.snowStreaks;

        // 1. The perimeter: a veil on a sphere around the player (peaking at the configured radius), first, with
        // its own share of the budget so the band cannot starve it. Independent of the indoor ease; gated by sky.
        double radius = ArcanaQuestTweaksConfig.AurorianModuleConfig.visibilityPerimeterRadius;
        if (radius > 0.0D) {
            int shellAttempts = (int) (attempts * 0.30D * ArcanaQuestTweaksConfig.AurorianModuleConfig.perimeterDensity);
            int shellLimit = (int) (limit * 0.35D);
            for (int i = 0; i < shellAttempts; i++) {
                if (MoonSnowCounters.PERIMETER.get() >= shellLimit || MoonSnowCounters.ACTIVE.get() >= limit) break;
                SnowClass cls = MoonSnowShape.pickShell(rand);
                double h = h0 * MoonSnowShape.speedFactor(cls) * 0.9D;
                double v = MoonSnowShape.fallPerTick(h, angle, rand.nextDouble());
                int life = MoonSnowShape.lifetime(cls, rand, true);
                double r = MoonSnowShape.perimeterRadius(rand, radius);
                double[] d = MoonSnowShape.perimeterDirection(rand);
                double cx = eye.x + d[0] * r;
                double cy = eye.y + d[1] * r;
                double cz = eye.z + d[2] * r;
                if (!openCell(world, cx, cy, cz, angle)) continue;
                spawn(mc, world, cx, cy, cz, cls, h, v, life, true, strength);
            }
        }

        // 2. The band (outdoors) and the look cone (under cover), blended by the eased exposure.
        int bandAttempts = (int) (attempts * exposure);
        int coneAttempts = attempts - bandAttempts;
        for (int i = 0; i < bandAttempts; i++) {
            if (MoonSnowCounters.ACTIVE.get() >= limit) break;
            SnowClass cls = MoonSnowShape.pick(rand, strength, streaks);
            double h = h0 * MoonSnowShape.speedFactor(cls);
            double v = MoonSnowShape.fallPerTick(h, angle, rand.nextDouble());
            int life = MoonSnowShape.lifetime(cls, rand, false);
            double cx;
            double cy;
            double cz;
            if (cls == SnowClass.CLUMP) {
                // Heavy clumps come in close, where they read as chunky.
                double[] d = MoonSnowShape.perimeterDirection(rand);
                double r = 3.0D + rand.nextDouble() * 5.0D;
                cx = eye.x + d[0] * r;
                cy = eye.y + d[1] * r;
                cz = eye.z + d[2] * r;
            } else {
                double alongWind = rand.nextDouble() * 66.0D - 50.0D; // [-50, +16]
                double crossWind = (rand.nextDouble() - 0.5D) * 70.0D;
                cx = player.posX + windDirX * alongWind - windDirZ * crossWind;
                cz = player.posZ + windDirZ * alongWind + windDirX * crossWind;
                // About -5 to +40 blocks, raised by half the fall so the space around the player stays filled.
                cy = player.posY + (rand.nextDouble() * 45.0D - 5.0D) + 0.5D * v * life;
            }
            if (cy > 300.0D) continue;
            if (!openCell(world, cx, cy, cz, angle)) continue;
            double ex = cx - eye.x;
            double ey = cy - eye.y;
            double ez = cz - eye.z;
            double distSq = ex * ex + ey * ey + ez * ez;
            if (distSq < 9.0D) continue; // nothing in the camera
            if (distSq <= 36.0D && blockedFromEye(world, eye, cx, cy, cz)) continue;
            spawn(mc, world, cx, cy, cz, cls, h, v, life, false, strength);
        }
        for (int i = 0; i < coneAttempts; i++) {
            if (MoonSnowCounters.ACTIVE.get() >= limit) break;
            SnowClass cls = MoonSnowShape.pick(rand, strength, streaks);
            if (cls == SnowClass.CLUMP) cls = SnowClass.MEDIUM;
            double h = h0 * MoonSnowShape.speedFactor(cls);
            double v = MoonSnowShape.fallPerTick(h, angle, rand.nextDouble());
            int life = MoonSnowShape.lifetime(cls, rand, false);
            // Under cover: bias into the look cone so the snow shows through openings.
            double a = rand.nextFloat() < 0.80F
                    ? baseAngle + (rand.nextDouble() - 0.5D) * 1.5D
                    : rand.nextDouble() * Math.PI * 2.0D;
            double dist = 2.5D + rand.nextDouble() * 20.0D;
            double cx = player.posX + Math.cos(a) * dist;
            double cz = player.posZ + Math.sin(a) * dist;
            double cy = player.posY + (rand.nextDouble() * 30.0D - 10.0D) + 0.5D * v * life;
            if (cy > 300.0D) continue;
            if (!openCell(world, cx, cy, cz, angle)) continue;
            if (blockedFromEye(world, eye, cx, cy, cz)) continue;
            spawn(mc, world, cx, cy, cz, cls, h, v, life, false, strength);
        }

        // 3. Powder puffs where the snow lands near the player, and in the gusts.
        if (scale > 0.4F && rand.nextFloat() < 0.35F * scale) {
            double px = player.posX + (rand.nextDouble() - 0.5D) * 24.0D;
            double pz = player.posZ + (rand.nextDouble() - 0.5D) * 24.0D;
            checkPos.setPos(px, 64.0D, pz);
            if (world.isBlockLoaded(checkPos)) {
                int y = world.getPrecipitationHeight(checkPos).getY();
                checkPos.setPos(px, y, pz);
                if (StormShelter.openSky(world, checkPos)) {
                    double sp = h0 * 0.25D;
                    world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, px, y + 0.15D, pz, windDirX * sp,
                            0.04D + rand.nextDouble() * 0.05D, windDirZ * sp);
                }
            }
        }
    }

    /**
     * The cell is air and sees the sky (leaves ignored), and so does the cell upwind and up along the flight line,
     * which also has no block in between. A point above the build limit counts as open sky.
     */
    private boolean openCell(World world, double cx, double cy, double cz, double angleDeg) {
        checkPos.setPos(cx, cy, cz);
        if (!world.isAirBlock(checkPos) || !StormShelter.openSky(world, checkPos)) return false;
        double a = Math.toRadians(Math.max(MoonSnowShape.MIN_ANGLE, Math.min(MoonSnowShape.MAX_ANGLE, angleDeg)));
        double upX = cx - windDirX * Math.cos(a) * 6.5D;
        double upY = cy + Math.sin(a) * 6.5D;
        double upZ = cz - windDirZ * Math.cos(a) * 6.5D;
        if (upY > 255.0D) return true;
        checkPos.setPos(upX, upY, upZ);
        if (!StormShelter.openSky(world, checkPos)) return false;
        RayTraceResult windRay = world.rayTraceBlocks(new Vec3d(upX, upY, upZ), new Vec3d(cx, cy, cz), false, true, false);
        return windRay == null || windRay.typeOfHit != RayTraceResult.Type.BLOCK;
    }

    private static boolean blockedFromEye(World world, Vec3d eye, double cx, double cy, double cz) {
        Vec3d target = new Vec3d(cx, cy, cz);
        RayTraceResult ray = world.rayTraceBlocks(eye, target, false, true, false);
        return ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK
                && ray.hitVec.squareDistanceTo(eye) < target.squareDistanceTo(eye) - 1.5D;
    }

    private void spawn(Minecraft mc, World world, double cx, double cy, double cz, SnowClass cls, double h, double v,
                       int life, boolean perimeter, double strength) {
        float scale = MoonSnowShape.scale(cls, rand);
        if (cls == SnowClass.CLUMP) {
            mc.effectRenderer.addEffect(new ParticleMoonClump(world, cx, cy, cz, (float) h, (float) v, scale, life));
            return;
        }
        float alpha = MoonSnowShape.alpha(cls, rand, strength);
        mc.effectRenderer.addEffect(new ParticleMoonSnow(world, cx, cy, cz, cls, (float) h, (float) v, scale, alpha, life,
                perimeter));
    }

    private static void updateSound(Minecraft mc, EntityPlayerSP player, float scale) {
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.enableStormSound || scale <= 0.02F) return;
        if (activeSound != null && !activeSound.isDonePlaying() && mc.getSoundHandler().isSoundPlaying(activeSound)) {
            return;
        }
        if (ArcanaQuestTweaksConfig.AurorianModuleConfig.suppressExternalWindInAurorian) {
            mc.getSoundHandler().stop("dsurround:desert.wind", null);
            mc.getSoundHandler().stop("dsurround:wind", null);
            mc.getSoundHandler().stop("ambientsounds:wind.howling-wind", null);
            mc.getSoundHandler().stop("ambientsounds:wind.wind", null);
        }
        activeSound = new AurorianStormSound(player, AtumSounds.SANDSTORM_WIND);
        mc.getSoundHandler().playSound(activeSound);
    }
}
