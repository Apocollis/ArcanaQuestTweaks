package com.apocollis.aqtweaks.aether;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.aether.AetherSquallGear.GearClass;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.network.PacketAetherBoltTelegraph;
import com.apocollis.aqtweaks.weather.GearTierRule;
import com.apocollis.aqtweaks.weather.StormShelter;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Server side of the Aether Silver Squalls. Gusts are pushed client-side (see {@code AetherSquallClient}); the
 * server adds the headwind cost. Lightning runs on its own hazard channel as a storm of strike salvos
 * ({@link AetherLightningSchedule}): each salvo locks onto the exposed players around one target (by gear class, see
 * {@link AetherSquallGear}), telegraphs every bolt for {@code Lightning Telegraph Ticks}, then lands a primary bolt on
 * each marked spot followed by secondary bolts around it and, for conductive and channeling players, extra bolts
 * that re-aim at where the player now stands. Every bolt is a non-destructive visual plus damage and a short shock to
 * everything inside {@link #STRIKE_RADIUS}.
 */
public class AetherSquallHandler {

    public static final DamageSource DAMAGE_LIGHTNING = new DamageSource("aqtweaks_aether_lightning")
            .setDamageBypassesArmor().setMagicDamage();

    private static final double STRIKE_RADIUS = 2.5D;

    private final Random rand = new Random();
    private long lightningEventId = Long.MIN_VALUE;
    private long[] waveStrikes = new long[0];
    private int nextWave;
    private final List<Bolt> bolts = new ArrayList<>();

    /** One scheduled bolt. A retarget bolt has no position until its telegraph opens (it reads the player then). */
    private static final class Bolt {
        BlockPos pos;
        long telegraphAt;
        long strikeAt;
        boolean telegraphed;
        final boolean secondary;
        final UUID follow;

        Bolt(BlockPos pos, long telegraphAt, long strikeAt, boolean secondary, UUID follow) {
            this.pos = pos;
            this.telegraphAt = telegraphAt;
            this.strikeAt = strikeAt;
            this.secondary = secondary;
            this.follow = follow;
        }
    }

    public boolean strikePending() {
        return !bolts.isEmpty() || nextWave < waveStrikes.length;
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !(event.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) event.world;
        if (!AetherChannels.isAether(world)) return;
        long now = world.getTotalWorldTime();
        if (ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableLightning) {
            lightning(world, now);
        }
        if (ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableSqualls && now % 20L == 0L) {
            headwind(world, now);
        }
    }

    // ---- gust: headwind cost ----

    private void headwind(WorldServer world, long now) {
        if (!ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableHeadwindStaminaDrain) return;
        HazardState state = Hazards.state(world, AetherChannels.GUST);
        if (state.event == null || state.strength < 0.15F) return;
        double angle = AetherWindHelper.headingAt(state.event.seed, state.event.start, now,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.crosswindShiftTicks);
        Vec3d wind = new Vec3d(Math.cos(angle), 0.0D, Math.sin(angle));
        for (EntityPlayer player : world.playerEntities) {
            if (player.isCreative() || player.isSpectator() || !(player instanceof EntityPlayerMP)) continue;
            if (StormShelter.isSheltered(player, world, wind,
                    ArcanaQuestTweaksConfig.AetherModuleConfig.squall.shelterWindShadowDistance)) {
                continue;
            }
            double mx = player.posX - player.prevPosX;
            double mz = player.posZ - player.prevPosZ;
            double dot = (mx * wind.x + mz * wind.z) * 20.0D; // per second
            if (player.isSprinting() && dot < -0.8D) {
                player.addExhaustion(0.05F * state.strength);
                if (Loader.isModLoaded("elenaidodge2")) {
                    drainFeather((EntityPlayerMP) player);
                }
            }
        }
    }

    private static void drainFeather(EntityPlayerMP player) {
        try {
            com.elenai.elenaidodge2.api.FeathersHelper.decreaseFeathers(player, 1);
        } catch (Throwable ignored) {
        }
    }

    // ---- lightning ----

    private void lightning(WorldServer world, long now) {
        HazardState state = Hazards.state(world, AetherChannels.LIGHTNING);
        if (state.event != null && state.phase != HazardEvent.Phase.CLEAR && state.event.id != lightningEventId) {
            lightningEventId = state.event.id;
            int lo = ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningWavesMin;
            int hi = Math.max(lo, ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningWavesMax);
            int count = lo + rand.nextInt(hi - lo + 1);
            waveStrikes = AetherLightningSchedule.planWaves(now, state.event.start, state.event.end,
                    telegraphTicks(), count, rand);
            nextWave = 0;
        }
        int telegraph = telegraphTicks();
        while (nextWave < waveStrikes.length && now >= waveStrikes[nextWave] - telegraph) {
            launchWave(world, now);
            nextWave++;
        }
        runBolts(world, now);
    }

    private static int telegraphTicks() {
        return Math.max(1, ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningTelegraphTicks);
    }

    private static boolean exposedToSky(EntityPlayer player) {
        return player.world.canSeeSky(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ));
    }

    private static GearClass gearClass(EntityPlayer player) {
        ArcanaQuestTweaksConfig.AetherHazardGear gearCfg = ArcanaQuestTweaksConfig.HazardGearConfig.aether;
        boolean insulated = GearTierRule.allSlotsOnList(player, gearCfg.insulatedGear);
        boolean conductive = GearTierRule.anyOnListExcluding(player, gearCfg.conductiveGear,
                gearCfg.insulatedGear, gearCfg.channelingGear);
        boolean channeling = GearTierRule.allSlotsOnList(player, gearCfg.channelingGear);
        return AetherSquallGear.classify(insulated, conductive, channeling);
    }

    /** One salvo: lock every exposed player around a weighted-random primary target, then queue their bolts. */
    private void launchWave(WorldServer world, long now) {
        ArcanaQuestTweaksConfig.AetherSquall c = ArcanaQuestTweaksConfig.AetherModuleConfig.squall;
        List<EntityPlayer> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (EntityPlayer player : world.playerEntities) {
            if (player.isCreative() || player.isSpectator() || player.isDead || !exposedToSky(player)) continue;
            int w = AetherSquallGear.attractionWeight(gearClass(player));
            candidates.add(player);
            weights.add(w);
            total += w;
        }
        if (candidates.isEmpty()) return;
        int roll = rand.nextInt(total);
        EntityPlayer primary = candidates.get(0);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                primary = candidates.get(i);
                break;
            }
        }
        // Everyone exposed within 32 blocks of the primary target is part of the salvo; conductive and channeling
        // players are locked first, insulated players last.
        List<EntityPlayer> group = new ArrayList<>();
        for (EntityPlayer player : candidates) {
            if (player == primary || player.getDistanceSq(primary) <= 1024.0D) group.add(player);
        }
        group.sort((a, b) -> Integer.compare(AetherSquallGear.priority(gearClass(a)),
                AetherSquallGear.priority(gearClass(b))));

        int telegraph = telegraphTicks();
        long primaryStrike = now + telegraph;
        for (EntityPlayer player : group) {
            GearClass gear = gearClass(player);
            BlockPos marked = markSpot(world, player, gear);
            bolts.add(new Bolt(marked, now, primaryStrike, false, null));

            int lo = c.lightningClusterMin;
            int hi = Math.max(lo, c.lightningClusterMax);
            int cluster = lo + rand.nextInt(hi - lo + 1);
            for (int i = 0; i < cluster; i++) {
                double angle = rand.nextDouble() * Math.PI * 2.0D;
                double dist = 2.0D + rand.nextDouble() * Math.max(0.0D, c.lightningClusterRadius - 2.0D);
                BlockPos at = groundNear(world, (int) Math.floor(marked.getX() + 0.5D + Math.cos(angle) * dist),
                        marked.getY(), (int) Math.floor(marked.getZ() + 0.5D + Math.sin(angle) * dist));
                long strike = primaryStrike + 20 + rand.nextInt(41);
                bolts.add(new Bolt(at, strike - telegraph, strike, true, null));
            }
            if (AetherSquallGear.retargeted(gear)) {
                for (int i = 0; i < c.lightningConductiveExtraBolts; i++) {
                    long open = primaryStrike + 10 + rand.nextInt(16) + i * 20L;
                    bolts.add(new Bolt(null, open, open + telegraph, false, player.getUniqueID()));
                }
            }
        }
    }

    /** The ground spot to mark for a player: their own, or a few blocks off for a full insulated set. */
    private BlockPos markSpot(WorldServer world, EntityPlayer player, GearClass gear) {
        double x = player.posX;
        double z = player.posZ;
        if (!AetherSquallGear.locksOn(gear)) {
            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double dist = AetherSquallGear.divertDistance(rand);
            x += Math.cos(angle) * dist;
            z += Math.sin(angle) * dist;
        }
        return groundNear(world, (int) Math.floor(x), (int) Math.floor(player.posY), (int) Math.floor(z));
    }

    private void broadcast(WorldServer world, BlockPos pos, int ticks) {
        ArcanaQuestTweaks.NETWORK.sendToAllAround(
                new PacketAetherBoltTelegraph(pos.getX(), pos.getY(), pos.getZ(), Math.max(1, ticks)),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        64.0D));
    }

    /** Opens telegraphs and lands bolts that are due. Bolts already telegraphed still land after the storm ends. */
    private void runBolts(WorldServer world, long now) {
        if (bolts.isEmpty()) return;
        int telegraph = telegraphTicks();
        Iterator<Bolt> it = bolts.iterator();
        while (it.hasNext()) {
            Bolt bolt = it.next();
            if (!bolt.telegraphed && now >= bolt.telegraphAt) {
                if (bolt.follow != null) {
                    EntityPlayer target = world.getPlayerEntityByUUID(bolt.follow);
                    if (target == null || target.isDead || target.isCreative() || target.isSpectator()
                            || !exposedToSky(target)) {
                        it.remove();
                        continue;
                    }
                    bolt.pos = groundNear(world, (int) Math.floor(target.posX), (int) Math.floor(target.posY),
                            (int) Math.floor(target.posZ));
                    bolt.strikeAt = now + telegraph;
                }
                bolt.telegraphed = true;
                broadcast(world, bolt.pos, (int) Math.max(1L, bolt.strikeAt - now));
            }
            if (bolt.telegraphed && now >= bolt.strikeAt) {
                strike(world, bolt.pos, bolt.secondary);
                it.remove();
            }
        }
    }

    /** First standing surface at or below {@code y + 8} within 24 blocks down; falls back to the given Y. */
    private static BlockPos groundNear(World world, int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cy = y + 8; cy >= y - 16; cy--) {
            pos.setPos(x, cy, z);
            if (!world.isBlockLoaded(pos)) break;
            if (world.getBlockState(pos).getMaterial().blocksMovement()) {
                return new BlockPos(x, cy + 1, z);
            }
        }
        return new BlockPos(x, y, z);
    }

    private void strike(WorldServer world, BlockPos pos, boolean secondary) {
        world.addWeatherEffect(new EntityLightningBolt(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, true));
        AxisAlignedBB box = new AxisAlignedBB(pos.getX() + 0.5D - STRIKE_RADIUS, pos.getY() - 2.0D,
                pos.getZ() + 0.5D - STRIKE_RADIUS, pos.getX() + 0.5D + STRIKE_RADIUS, pos.getY() + 4.0D,
                pos.getZ() + 0.5D + STRIKE_RADIUS);
        double factor = secondary ? ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningSecondaryDamageFactor : 1.0D;
        for (EntityLivingBase entity : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double dx = entity.posX - (pos.getX() + 0.5D);
            double dz = entity.posZ - (pos.getZ() + 0.5D);
            if (dx * dx + dz * dz > STRIKE_RADIUS * STRIKE_RADIUS) continue;
            boolean insulated = false;
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (player.isCreative() || player.isSpectator() || !exposedToSky(player)) continue;
                GearClass gear = gearClass(player);
                if (AetherSquallGear.immuneToBolt(gear)) continue;
                insulated = gear == GearClass.INSULATED;
            }
            float damage = (float) (factor * AetherSquallGear.shockDamage(insulated,
                    ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningDamage,
                    ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningInsulatedDamage));
            if (damage > 0.0F) entity.attackEntityFrom(DAMAGE_LIGHTNING, damage);
            if (!secondary) entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 1, false, true));
        }
    }

    /** Cancels every pending salvo and bolt (the {@code clear} command). */
    public void reset() {
        bolts.clear();
        waveStrikes = new long[0];
        nextWave = 0;
    }
}
