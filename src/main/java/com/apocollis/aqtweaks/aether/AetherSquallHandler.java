package com.apocollis.aqtweaks.aether;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardEvent;
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
import java.util.List;
import java.util.Random;

/**
 * Server side of the Aether Silver Squalls. Gusts are pushed client-side (see {@code AetherSquallClient}); the
 * server adds the headwind cost. Lightning runs on its own hazard channel: when its warning window opens a
 * strike point is locked and telegraphed, and when the event starts the bolt lands as a non-destructive visual
 * plus damage and a short shock to everything inside {@link #STRIKE_RADIUS}.
 */
public class AetherSquallHandler {

    public static final DamageSource DAMAGE_LIGHTNING = new DamageSource("aqtweaks_aether_lightning")
            .setDamageBypassesArmor().setMagicDamage();

    private static final double STRIKE_RADIUS = 2.5D;

    private final Random rand = new Random();
    private long lightningEventId = Long.MIN_VALUE;
    private BlockPos strikePos;
    private long strikeAt = -1L;
    private boolean struck;

    public boolean strikePending() {
        return strikePos != null && !struck;
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
        if (state.event == null) return;
        if (state.event.id != lightningEventId) {
            lightningEventId = state.event.id;
            strikePos = null;
            strikeAt = -1L;
            struck = false;
        }
        if (strikePos == null && !struck && state.phase != HazardEvent.Phase.CLEAR) {
            long telegraph = ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningTelegraphTicks;
            strikeAt = state.phase == HazardEvent.Phase.WARNING ? state.event.start : now + telegraph;
            strikePos = pickTarget(world);
            if (strikePos == null) {
                struck = true; // nobody exposed: skip this strike
                return;
            }
            int ticks = (int) Math.max(1L, strikeAt - now);
            ArcanaQuestTweaks.NETWORK.sendToAllAround(
                    new PacketAetherBoltTelegraph(strikePos.getX(), strikePos.getY(), strikePos.getZ(), ticks),
                    new NetworkRegistry.TargetPoint(world.provider.getDimension(), strikePos.getX() + 0.5D,
                            strikePos.getY(), strikePos.getZ() + 0.5D, 64.0D));
        }
        if (strikePos != null && !struck && now >= strikeAt) {
            struck = true;
            strike(world, strikePos);
        }
    }

    private static boolean exposedToSky(EntityPlayer player) {
        return player.world.canSeeSky(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ));
    }

    private BlockPos pickTarget(WorldServer world) {
        List<EntityPlayer> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (EntityPlayer player : world.playerEntities) {
            if (player.isCreative() || player.isSpectator() || player.isDead || !exposedToSky(player)) continue;
            boolean insulated = GearTierRule.allSlotsOnList(player, ArcanaQuestTweaksConfig.AetherModuleConfig.squall.insulatedGear);
            boolean conductive = GearTierRule.anyOnList(player, ArcanaQuestTweaksConfig.AetherModuleConfig.squall.conductiveGear);
            int w = AetherSquallGear.attractionWeight(insulated, conductive);
            candidates.add(player);
            weights.add(w);
            total += w;
        }
        if (candidates.isEmpty()) return null;
        int roll = rand.nextInt(total);
        EntityPlayer chosen = candidates.get(0);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                chosen = candidates.get(i);
                break;
            }
        }
        boolean insulated = GearTierRule.allSlotsOnList(chosen, ArcanaQuestTweaksConfig.AetherModuleConfig.squall.insulatedGear);
        boolean conductive = GearTierRule.anyOnList(chosen, ArcanaQuestTweaksConfig.AetherModuleConfig.squall.conductiveGear);
        double x = chosen.posX;
        double z = chosen.posZ;
        if (AetherSquallGear.locksOn(insulated, conductive)) {
            x += (rand.nextDouble() - 0.5D) * 4.0D;
            z += (rand.nextDouble() - 0.5D) * 4.0D;
        } else {
            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double dist = 6.0D + rand.nextDouble() * 8.0D;
            x += Math.cos(angle) * dist;
            z += Math.sin(angle) * dist;
        }
        return groundNear(world, (int) Math.floor(x), (int) Math.floor(chosen.posY), (int) Math.floor(z));
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

    private void strike(WorldServer world, BlockPos pos) {
        world.addWeatherEffect(new EntityLightningBolt(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, true));
        AxisAlignedBB box = new AxisAlignedBB(pos.getX() + 0.5D - STRIKE_RADIUS, pos.getY() - 2.0D,
                pos.getZ() + 0.5D - STRIKE_RADIUS, pos.getX() + 0.5D + STRIKE_RADIUS, pos.getY() + 4.0D,
                pos.getZ() + 0.5D + STRIKE_RADIUS);
        for (EntityLivingBase entity : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double dx = entity.posX - (pos.getX() + 0.5D);
            double dz = entity.posZ - (pos.getZ() + 0.5D);
            if (dx * dx + dz * dz > STRIKE_RADIUS * STRIKE_RADIUS) continue;
            boolean insulated = false;
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (player.isCreative() || player.isSpectator() || !exposedToSky(player)) continue;
                insulated = GearTierRule.allSlotsOnList(player, ArcanaQuestTweaksConfig.AetherModuleConfig.squall.insulatedGear);
            }
            float damage = (float) AetherSquallGear.shockDamage(insulated,
                    ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningDamage,
                    ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningInsulatedDamage);
            entity.attackEntityFrom(DAMAGE_LIGHTNING, damage);
            entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 1, false, true));
        }
    }

    /** Cancels a pending strike (the {@code clear} command). */
    public void reset() {
        strikePos = null;
        strikeAt = -1L;
        struck = true;
    }
}
